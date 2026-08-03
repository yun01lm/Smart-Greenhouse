package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.ChatConversation;
import com.greenhouse.entity.ChatMessage;
import com.greenhouse.entity.DataAuthorization;
import com.greenhouse.entity.ExpertAvailability;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员专家工作台服务
 * <p>
 * 提供全量专家列表、在线状态管理、授权记录查询和统计功能。
 * 与 ExpertService 的区别：ADMIN 可查看所有专家和授权，不限制用户范围。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExpertService {

    private final UserRepository userRepository;
    private final ExpertAvailabilityRepository availabilityRepository;
    private final DataAuthorizationRepository authorizationRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 获取专家列表（含在线状态和咨询数）
     */
    public List<Map<String, Object>> listExperts() {
        List<User> experts = userRepository.findByRole(User.Role.EXPERT);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User expert : experts) {
            ExpertAvailability av = availabilityRepository.findByExpertId(expert.getId())
                    .orElse(ExpertAvailability.builder()
                            .expertId(expert.getId()).isOnline(0).maxConcurrent(5).build());

            long consultCount = conversationRepository.countByExpertId(expert.getId());

            result.add(Map.of(
                    "id", expert.getId(),
                    "name", expert.getUsername(),
                    "phone", expert.getPhone() != null ? expert.getPhone() : "",
                    "isOnline", av.getIsOnline(),
                    "maxConcurrent", av.getMaxConcurrent(),
                    "lastActiveAt", av.getLastActiveAt() != null
                            ? av.getLastActiveAt().toString() : "",
                    "consultCount", consultCount,
                    "status", expert.getStatus()
            ));
        }
        return result;
    }

    /**
     * 切换专家在线状态
     */
    @Transactional
    public Map<String, Object> toggleOnline(Long expertId, boolean isOnline) {
        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "专家不存在"));
        if (expert.getRole() != User.Role.EXPERT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户不是专家");
        }

        ExpertAvailability av = availabilityRepository.findByExpertId(expertId)
                .orElse(ExpertAvailability.builder()
                        .expertId(expertId).maxConcurrent(5).build());

        av.setIsOnline(isOnline ? 1 : 0);
        av.setLastActiveAt(LocalDateTime.now());
        availabilityRepository.save(av);

        log.info("[ADMIN] 专家在线状态已切换: expertId={}, online={}", expertId, isOnline);

        return Map.of("id", expertId, "isOnline", isOnline);
    }

    /**
     * 全量授权记录（分页）
     */
    public Page<Map<String, Object>> listAuthorizations(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<DataAuthorization> authPage;

        if (status != null && !status.isEmpty()) {
            DataAuthorization.AuthorizationStatus s =
                    DataAuthorization.AuthorizationStatus.valueOf(status.toUpperCase());
            authPage = authorizationRepository.findByStatus(s, pageable);
        } else {
            authPage = authorizationRepository.findAll(pageable);
        }

        return authPage.map(auth -> {
            User expert = userRepository.findById(auth.getExpertId()).orElse(null);
            User user = userRepository.findById(auth.getUserId()).orElse(null);
            Greenhouse gh = greenhouseRepository.findById(auth.getGreenhouseId()).orElse(null);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", auth.getId());
            map.put("expertName", expert != null ? expert.getUsername() : "未知");
            map.put("userName", user != null ? user.getUsername() : "未知");
            map.put("greenhouseName", gh != null ? gh.getName() : "未知");
            map.put("status", auth.getStatus().name());
            map.put("reason", auth.getReason());
            map.put("requestedAt", auth.getRequestedAt());
            map.put("approvedAt", auth.getApprovedAt());
            map.put("expiresAt", auth.getExpiresAt());
            // 计算剩余天数
            long remaining = 0;
            if (auth.getExpiresAt() != null &&
                    auth.getStatus() == DataAuthorization.AuthorizationStatus.APPROVED) {
                remaining = java.time.Duration.between(LocalDateTime.now(), auth.getExpiresAt()).toDays();
                if (remaining < 0) remaining = 0;
            }
            map.put("remainingDays", remaining);
            return map;
        });
    }

    /**
     * 专家工作台统计数据
     */
    public Map<String, Object> getStats() {
        List<User> experts = userRepository.findByRole(User.Role.EXPERT);
        long onlineCount = availabilityRepository.countByIsOnline(1);
        long authTotal = authorizationRepository.count();
        long convTotal = conversationRepository.count();

        return Map.of(
                "expertTotal", experts.size(),
                "onlineCount", onlineCount,
                "authTotal", authTotal,
                "convTotal", convTotal
        );
    }

    // ===== 咨询记录（R9 新增） =====

    /**
     * 咨询记录分页查询（R9）
     * <p>按专家/用户/时间筛选；userKeyword 按用户名模糊匹配；startTime/endTime 为 epoch 毫秒。</p>
     */
    public Page<Map<String, Object>> listConversations(Long expertId, Long userId, String userKeyword,
                                                       Long startTime, Long endTime, int page, int size) {
        Specification<ChatConversation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (expertId != null) {
                predicates.add(cb.equal(root.get("expertId"), expertId));
            }
            List<Long> userFilterIds = resolveUserIds(userId, userKeyword);
            if (userFilterIds != null) {
                if (userFilterIds.isEmpty()) {
                    predicates.add(cb.equal(cb.literal(0), cb.literal(1)));
                } else {
                    predicates.add(root.get("userId").in(userFilterIds));
                }
            }
            if (startTime != null || endTime != null) {
                LocalDateTime start = (startTime != null && startTime > 0)
                        ? Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
                LocalDateTime end = (endTime != null && endTime > 0)
                        ? Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
                if (start != null && end != null) {
                    predicates.add(cb.between(root.get("createdAt"), start, end));
                } else if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                } else if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatConversation> convPage = conversationRepository.findAll(spec, pageable);

        Map<Long, String> nameMap = loadUserNames(convPage.getContent());
        Map<Long, String> ghNameMap = loadGreenhouseNames(convPage.getContent());
        Map<Long, Long> msgCountMap = loadMessageCounts(convPage.getContent());
        Map<Long, String> lastMsgMap = loadLastMessages(convPage.getContent());

        return convPage.map(conv -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", conv.getId());
            map.put("expertId", conv.getExpertId());
            map.put("expertName", nameMap.getOrDefault(conv.getExpertId(), "未知"));
            map.put("userId", conv.getUserId());
            map.put("userName", nameMap.getOrDefault(conv.getUserId(), "未知"));
            map.put("greenhouseId", conv.getGreenhouseId());
            map.put("greenhouseName", conv.getGreenhouseId() != null
                    ? ghNameMap.getOrDefault(conv.getGreenhouseId(), "未知") : "");
            map.put("subject", conv.getSubject());
            map.put("status", conv.getStatus().name());
            map.put("messageCount", msgCountMap.getOrDefault(conv.getId(), 0L));
            map.put("lastMessage", lastMsgMap.getOrDefault(conv.getId(), ""));
            map.put("createdAt", conv.getCreatedAt());
            map.put("closedAt", conv.getClosedAt());
            return map;
        });
    }

    /**
     * 对话消息明细（R9）
     */
    public List<Map<String, Object>> getConversationMessages(Long conversationId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        User expert = userRepository.findById(conv.getExpertId()).orElse(null);
        User user = userRepository.findById(conv.getUserId()).orElse(null);

        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            boolean fromExpert = m.getSenderType() == ChatMessage.SenderType.EXPERT;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("senderType", m.getSenderType().name());
            map.put("senderName", fromExpert
                    ? (expert != null ? expert.getUsername() : "专家")
                    : (user != null ? user.getUsername() : "用户"));
            map.put("messageType", m.getMessageType().name());
            map.put("content", m.getContent());
            map.put("filePath", m.getFilePath());
            map.put("createdAt", m.getCreatedAt());
            result.add(map);
        }
        return result;
    }

    /**
     * 咨询记录导出 Excel（R9）
     */
    public byte[] exportConversations(Long expertId, Long userId, String userKeyword,
                                      Long startTime, Long endTime) {
        Page<Map<String, Object>> page = listConversations(expertId, userId, userKeyword,
                startTime, endTime, 0, 10000);
        List<Map<String, Object>> rows = page.getContent();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("咨询记录");
            String[] headers = {"对话ID", "专家", "用户", "大棚", "咨询主题", "状态", "消息数", "最后消息", "创建时间", "关闭时间"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Map<String, Object> row : rows) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(((Number) row.get("id")).longValue());
                r.createCell(1).setCellValue(String.valueOf(row.get("expertName")));
                r.createCell(2).setCellValue(String.valueOf(row.get("userName")));
                r.createCell(3).setCellValue(String.valueOf(row.get("greenhouseName")));
                r.createCell(4).setCellValue(String.valueOf(row.get("subject")));
                r.createCell(5).setCellValue(String.valueOf(row.get("status")));
                r.createCell(6).setCellValue(((Number) row.get("messageCount")).longValue());
                r.createCell(7).setCellValue(String.valueOf(row.get("lastMessage")));
                r.createCell(8).setCellValue(String.valueOf(row.get("createdAt")));
                r.createCell(9).setCellValue(String.valueOf(row.get("closedAt")));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("咨询记录导出失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "咨询记录导出失败");
        }
    }

    // ===== 咨询记录辅助 =====

    /** 解析用户筛选：exact userId 优先；否则按用户名模糊匹配；返回 null 表示未设置用户筛选 */
    private List<Long> resolveUserIds(Long userId, String userKeyword) {
        if (userId != null) {
            return List.of(userId);
        }
        if (userKeyword != null && !userKeyword.isBlank()) {
            List<User> users = userRepository.findByUsernameContaining(userKeyword.trim());
            return users.stream().map(User::getId).toList();
        }
        return null;
    }

    /** 批量加载用户/专家名称 */
    private Map<Long, String> loadUserNames(List<ChatConversation> convs) {
        Set<Long> ids = new HashSet<>();
        for (ChatConversation c : convs) {
            ids.add(c.getUserId());
            ids.add(c.getExpertId());
        }
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    /** 批量加载大棚名称 */
    private Map<Long, String> loadGreenhouseNames(List<ChatConversation> convs) {
        Set<Long> ids = convs.stream().map(ChatConversation::getGreenhouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return greenhouseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Greenhouse::getId, Greenhouse::getName, (a, b) -> a));
    }

    /** 批量统计对话消息数 */
    private Map<Long, Long> loadMessageCounts(List<ChatConversation> convs) {
        List<Long> ids = convs.stream().map(ChatConversation::getId).toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : messageRepository.countByConversationIds(ids)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    /** 批量加载最后一条消息摘要 */
    private Map<Long, String> loadLastMessages(List<ChatConversation> convs) {
        Map<Long, String> result = new HashMap<>();
        for (ChatConversation c : convs) {
            result.put(c.getId(), lastMessagePreview(c.getId()));
        }
        return result;
    }

    private String lastMessagePreview(Long conversationId) {
        List<ChatMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (msgs.isEmpty()) return "";
        ChatMessage last = msgs.get(msgs.size() - 1);
        String content = last.getContent();
        if (content == null || content.isBlank()) {
            return "[" + last.getMessageType().name() + "]";
        }
        return content.length() > 50 ? content.substring(0, 50) + "…" : content;
    }
}
