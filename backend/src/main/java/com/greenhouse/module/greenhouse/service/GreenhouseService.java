package com.greenhouse.module.greenhouse.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.greenhouse.dto.GreenhouseRequest;
import com.greenhouse.module.greenhouse.dto.GreenhouseResponse;
import com.greenhouse.module.greenhouse.dto.RegionStatsResponse;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 大棚管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;

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
                // TODO: 步骤7（C18权限模块）实现后，从employee_permissions表查询被授权的大棚
                greenhouses = List.of();
                break;
            default:
                greenhouses = List.of();
        }

        return greenhouses.stream()
                .map(GreenhouseResponse::fromEntity)
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
