package com.greenhouse.module.greenhouse.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.DataAuthorization;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.repository.DataAuthorizationRepository;
import com.greenhouse.entity.User;
import com.greenhouse.module.greenhouse.dto.GreenhouseRequest;
import com.greenhouse.module.greenhouse.dto.GreenhouseResponse;
import com.greenhouse.module.greenhouse.dto.RegionStatsResponse;
import com.greenhouse.repository.EmployeePermissionRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;
    private final EmployeePermissionRepository permissionRepository;
    private final DataAuthorizationRepository dataAuthorizationRepository;

    /** 每个棚主最多创建的大棚数量 */
    private static final long MAX_GREENHOUSES_PER_OWNER = 10;

    /**
     * 创建大棚（仅棚主可操作）
     */
    @Transactional
    public GreenhouseResponse createGreenhouse(Long userId, GreenhouseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 校验角色：只有棚主可以创建大棚
        if (user.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NOT_OWNER);
        }

        // 校验大棚数量上限
        long count = greenhouseRepository.countByOwnerId(userId);
        if (count >= MAX_GREENHOUSES_PER_OWNER) {
            throw new BusinessException(ErrorCode.GREENHOUSE_LIMIT_EXCEEDED);
        }

        // 校验重名
        if (greenhouseRepository.existsByOwnerIdAndName(userId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚名称已存在");
        }

        Greenhouse greenhouse = Greenhouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .cropType(request.getCropType())
                .ownerId(userId)
                .province(request.getProvince())
                .city(request.getCity())
                .district(request.getDistrict())
                .town(request.getTown())
                .village(request.getVillage())
                .status(true)
                .build();

        greenhouse = greenhouseRepository.save(greenhouse);
        log.info("大棚创建成功: id={}, name={}, ownerId={}", greenhouse.getId(), greenhouse.getName(), userId);

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
     * 更新大棚（仅棚主可操作自己的大棚）
     */
    @Transactional
    public GreenhouseResponse updateGreenhouse(Long userId, Long greenhouseId, GreenhouseRequest request) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        // 校验归属
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验重名（排除自己）
        if (!greenhouse.getName().equals(request.getName())
                && greenhouseRepository.existsByOwnerIdAndName(userId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚名称已存在");
        }

        greenhouse.setName(request.getName());
        greenhouse.setLocation(request.getLocation());
        greenhouse.setCropType(request.getCropType());
        greenhouse.setProvince(request.getProvince());
        greenhouse.setCity(request.getCity());
        greenhouse.setDistrict(request.getDistrict());
        greenhouse.setTown(request.getTown());
        greenhouse.setVillage(request.getVillage());

        greenhouse = greenhouseRepository.save(greenhouse);
        log.info("大棚更新成功: id={}, name={}", greenhouse.getId(), greenhouse.getName());

        return GreenhouseResponse.fromEntity(greenhouse);
    }

    /**
     * 删除大棚（仅棚主可操作自己的大棚）
     */
    @Transactional
    public void deleteGreenhouse(Long userId, Long greenhouseId) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        greenhouseRepository.delete(greenhouse);
        log.info("大棚删除成功: id={}, name={}", greenhouse.getId(), greenhouse.getName());
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
