package com.greenhouse.module.greenhouse.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.common.RegionNormalizer;
import com.greenhouse.entity.*;
import com.greenhouse.module.greenhouse.dto.GreenhouseRequest;
import com.greenhouse.module.greenhouse.dto.GreenhouseResponse;
import com.greenhouse.module.greenhouse.dto.RegionStatsResponse;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 大棚管理服务
 * <p>
 * R45：支持棚主自主管理 + 管理员代管（代建/代改/代删）；
 * 删除大棚时级联清理全部关联数据（设备/固件解绑/分组/预警/场景/作物/诊断/评估/授权/权限/时序数据）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;
    private final EmployeePermissionRepository permissionRepository;
    private final DataAuthorizationRepository dataAuthorizationRepository;
    private final RegionNormalizer regionNormalizer;

    // R45 级联清理依赖
    private final DeviceRepository deviceRepository;
    private final FirmwareRepository firmwareRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final UserAlertThresholdRepository userAlertThresholdRepository;
    private final SceneRepository sceneRepository;
    private final CropCycleRepository cropCycleRepository;
    private final DiagnosticRecordRepository diagnosticRecordRepository;
    private final HealthAssessmentRepository healthAssessmentRepository;
    private final GrowthAssessmentRepository growthAssessmentRepository;
    private final SensorDailySummaryRepository sensorDailySummaryRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SensorDataService sensorDataService;

    /** 每个棚主最多创建的大棚数量 */
    private static final long MAX_GREENHOUSES_PER_OWNER = 10;

    /**
     * 创建大棚
     * <p>OWNER：创建自己的大棚；ADMIN：代棚主创建（request.ownerId 必填，目标必须是棚主）。</p>
     */
    @Transactional
    public GreenhouseResponse createGreenhouse(Long userId, User.Role role, GreenhouseRequest request) {
        Long ownerId = userId;
        if (role == User.Role.ADMIN) {
            if (request.getOwnerId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "管理员代建大棚必须指定棚主(ownerId)");
            }
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if (owner.getRole() != User.Role.OWNER) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "目标用户不是棚主，无法代建大棚");
            }
            ownerId = owner.getId();
        } else if (role != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NOT_OWNER);
        }

        // 校验大棚数量上限
        long count = greenhouseRepository.countByOwnerId(ownerId);
        if (count >= MAX_GREENHOUSES_PER_OWNER) {
            throw new BusinessException(ErrorCode.GREENHOUSE_LIMIT_EXCEEDED);
        }

        // 校验重名
        if (greenhouseRepository.existsByOwnerIdAndName(ownerId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚名称已存在");
        }

        String normProvince = regionNormalizer.normalizeProvince(request.getProvince());
        Greenhouse greenhouse = Greenhouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .cropType(request.getCropType())
                .ownerId(ownerId)
                .province(normProvince)
                .city(regionNormalizer.normalizeCity(normProvince, request.getCity()))
                .district(regionNormalizer.normalizeDistrict(request.getDistrict()))
                .town(request.getTown())
                .village(request.getVillage())
                .status(true)
                .build();

        greenhouse = greenhouseRepository.save(greenhouse);
        log.info("大棚创建成功: id={}, name={}, ownerId={}, by={}({})", greenhouse.getId(), greenhouse.getName(), ownerId, role, userId);

        return GreenhouseResponse.fromEntity(greenhouse);
    }

    /**
     * 获取大棚列表（按角色过滤）
     * ADMIN → 全部大棚
     * OWNER → 自己的大棚
     * WORKER → 被授权的大棚（权限过滤在C18模块实现，此处返回空列表）
     */
    public List<GreenhouseResponse> listGreenhouses(Long userId, User.Role role) {
        List<Greenhouse> greenhouses;

        switch (role) {
            case ADMIN:
                greenhouses = greenhouseRepository.findAll();
                break;
            case OWNER:
                greenhouses = greenhouseRepository.findByOwnerId(userId);
                break;
            case WORKER:
                // 从 employee_permissions 表查询被授权的大棚ID列表
                List<Long> authorizedIds = permissionRepository.findByEmployeeId(userId)
                        .stream()
                        .map(p -> p.getGreenhouseId())
                        .distinct()
                        .toList();
                if (authorizedIds.isEmpty()) {
                    greenhouses = List.of();
                } else {
                    greenhouses = greenhouseRepository.findAllById(authorizedIds);
                }
                break;
            case TECHNICIAN:
                // 技术员与普通员工同走权限表（默认权限全开，可被棚主收紧）
                List<Long> techIds = permissionRepository.findByEmployeeId(userId)
                        .stream()
                        .map(p -> p.getGreenhouseId())
                        .distinct()
                        .toList();
                if (techIds.isEmpty()) {
                    greenhouses = List.of();
                } else {
                    greenhouses = greenhouseRepository.findAllById(techIds);
                }
                break;
            case EXPERT:
                // 专家：仅返回已授权（APPROVED 且未过期）的大棚
                List<Long> authIds = dataAuthorizationRepository
                        .findByExpertIdAndStatusOrderByApprovedAtDesc(userId, DataAuthorization.AuthorizationStatus.APPROVED)
                        .stream()
                        .filter(a -> a.getExpiresAt() != null && a.getExpiresAt().isAfter(LocalDateTime.now()))
                        .map(DataAuthorization::getGreenhouseId)
                        .distinct()
                        .toList();
                greenhouses = authIds.isEmpty() ? List.of() : greenhouseRepository.findAllById(authIds);
                break;
            default:
                greenhouses = List.of();
        }

        return greenhouses.stream()
                .map(gh -> {
                    GreenhouseResponse resp = GreenhouseResponse.fromEntity(gh);
                    resp.setOwnerName(userRepository.findById(gh.getOwnerId())
                            .map(User::getRealName)
                            .orElse(""));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取大棚详情
     */
    public GreenhouseResponse getGreenhouse(Long greenhouseId) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        return GreenhouseResponse.fromEntity(greenhouse);
    }

    /**
     * 更新大棚（OWNER 操作自己的大棚；ADMIN 可代管任意大棚）
     */
    @Transactional
    public GreenhouseResponse updateGreenhouse(Long userId, User.Role role, Long greenhouseId, GreenhouseRequest request) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        // 校验归属（ADMIN 代管跳过）
        if (role != User.Role.ADMIN && !greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验重名（排除自己）
        if (!greenhouse.getName().equals(request.getName())
                && greenhouseRepository.existsByOwnerIdAndName(greenhouse.getOwnerId(), request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚名称已存在");
        }

        greenhouse.setName(request.getName());
        greenhouse.setLocation(request.getLocation());
        greenhouse.setCropType(request.getCropType());
        String normProvince = regionNormalizer.normalizeProvince(request.getProvince());
        greenhouse.setProvince(normProvince);
        greenhouse.setCity(regionNormalizer.normalizeCity(normProvince, request.getCity()));
        greenhouse.setDistrict(regionNormalizer.normalizeDistrict(request.getDistrict()));
        greenhouse.setTown(request.getTown());
        greenhouse.setVillage(request.getVillage());

        greenhouse = greenhouseRepository.save(greenhouse);
        log.info("大棚更新成功: id={}, name={}", greenhouse.getId(), greenhouse.getName());

        return GreenhouseResponse.fromEntity(greenhouse);
    }

    /**
     * 删除大棚（OWNER 操作自己的大棚；ADMIN 可代管任意大棚）
     * <p>R45：级联清理全部关联数据。</p>
     */
    @Transactional
    public void deleteGreenhouse(Long userId, User.Role role, Long greenhouseId) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        if (role != User.Role.ADMIN && !greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // ===== 级联清理（R45）=====

        // 1. 咨询会话及其消息
        List<ChatConversation> conversations = chatConversationRepository.findByGreenhouseId(greenhouseId);
        if (!conversations.isEmpty()) {
            List<Long> convIds = conversations.stream().map(ChatConversation::getId).collect(Collectors.toList());
            chatMessageRepository.deleteByConversationIdIn(convIds);
            chatConversationRepository.deleteAll(conversations);
        }

        // 2. 设备（逐个固件解绑）
        List<Device> devices = deviceRepository.findByGreenhouseId(greenhouseId);
        for (Device device : devices) {
            if (device.getFirmwareId() != null) {
                firmwareRepository.findById(device.getFirmwareId()).ifPresent(fw -> {
                    fw.setStatus(Firmware.Status.UNBOUND);
                    fw.setBoundDeviceId(null);
                    firmwareRepository.save(fw);
                });
            }
        }
        deviceRepository.deleteAll(devices);

        // 3. 设备分组（实体删除，连带 device_group_members）
        List<DeviceGroup> groups = deviceGroupRepository.findByGreenhouseId(greenhouseId);
        if (!groups.isEmpty()) {
            deviceGroupRepository.deleteAll(groups);
        }

        // 4. 预警规则 / 自定义阈值 / 预警记录
        alertRuleRepository.deleteByGreenhouseId(greenhouseId);
        userAlertThresholdRepository.deleteByGreenhouseId(greenhouseId);
        alertRepository.deleteByGreenhouseId(greenhouseId);

        // 5. 场景联动
        sceneRepository.deleteByGreenhouseId(greenhouseId);

        // 6. 作物生长周期
        cropCycleRepository.deleteByGreenhouseId(greenhouseId);

        // 7. 病虫害诊断记录
        diagnosticRecordRepository.deleteByGreenhouseId(greenhouseId);

        // 8. 健康评估 / 长势评估
        healthAssessmentRepository.deleteByGreenhouseId(greenhouseId);
        growthAssessmentRepository.deleteByGreenhouseId(greenhouseId);

        // 9. 传感器日汇总
        sensorDailySummaryRepository.deleteByGreenhouseId(greenhouseId);

        // 10. 数据授权 / 员工权限
        dataAuthorizationRepository.deleteByGreenhouseId(greenhouseId);
        permissionRepository.deleteByGreenhouseId(greenhouseId);

        // 11. InfluxDB 时序数据
        sensorDataService.deleteGreenhouseData(greenhouseId);

        // 12. 删除大棚
        greenhouseRepository.delete(greenhouse);
        log.info("大棚删除成功(含级联清理): id={}, name={}, ownerId={}, by={}({})",
                greenhouse.getId(), greenhouse.getName(), greenhouse.getOwnerId(), role, userId);
    }

    /**
     * 地区分布统计（管理员功能）
     */
    public List<RegionStatsResponse> getRegionStats() {
        List<Object[]> provinceStats = greenhouseRepository.countByProvince();
        List<RegionStatsResponse> result = new ArrayList<>();

        for (Object[] row : provinceStats) {
            String province = (String) row[0];
            Long count = (Long) row[1];
            result.add(RegionStatsResponse.builder()
                    .name(province)
                    .greenhouseCount(count)
                    .build());
        }

        return result;
    }
}
