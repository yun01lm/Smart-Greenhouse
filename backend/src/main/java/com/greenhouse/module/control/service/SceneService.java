package com.greenhouse.module.control.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.*;
import com.greenhouse.module.control.dto.ControlLogResponse;
import com.greenhouse.module.control.dto.ControlRequest;
import com.greenhouse.module.control.dto.SceneRequest;
import com.greenhouse.module.control.dto.SceneResponse;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 场景联动服务
 * <p>
 * 负责场景的增删改查和手动执行。
 * Phase 1 只做手动执行，自动触发条件留给 Phase 2 预警引擎。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneService {

    private final SceneRepository sceneRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final DeviceRepository deviceRepository;
    private final ControlLogRepository controlLogRepository;
    private final ControlService controlService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** 每个大棚最多场景数量 */
    private static final long MAX_SCENES_PER_GREENHOUSE = 20;

    /**
     * 创建场景
     */
    @Transactional
    public SceneResponse createScene(Long userId, Long greenhouseId, SceneRequest request) {
        // 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验数量上限
        if (sceneRepository.countByGreenhouseId(greenhouseId) >= MAX_SCENES_PER_GREENHOUSE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "大棚场景数量已达上限(" + MAX_SCENES_PER_GREENHOUSE + "个)");
        }

        // 校验名称唯一性
        if (sceneRepository.existsByGreenhouseIdAndName(greenhouseId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景名称已存在");
        }

        // 校验所有设备属于该大棚
        validateDevicesBelongToGreenhouse(greenhouseId, request.getActions());

        // 序列化动作列表为 JSON
        String actionsJson;
        try {
            actionsJson = objectMapper.writeValueAsString(request.getActions());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "动作列表格式错误");
        }

        Scene scene = Scene.builder()
                .name(request.getName())
                .description(request.getDescription())
                .greenhouseId(greenhouseId)
                .actionsJson(actionsJson)
                .enabled(true)
                .build();

        scene = sceneRepository.save(scene);
        log.info("场景创建成功: id={}, name={}, greenhouseId={}, actionCount={}",
                scene.getId(), scene.getName(), greenhouseId, request.getActions().size());

        return toSceneResponse(scene);
    }

    /**
     * 获取大棚场景列表
     */
    public List<SceneResponse> listScenes(Long greenhouseId) {
        List<Scene> scenes = sceneRepository.findByGreenhouseId(greenhouseId);
        return scenes.stream()
                .map(this::toSceneResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取场景详情
     */
    public SceneResponse getScene(Long sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "场景不存在"));
        return toSceneResponse(scene);
    }

    /**
     * 更新场景
     */
    @Transactional
    public SceneResponse updateScene(Long userId, Long sceneId, SceneRequest request) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "场景不存在"));

        // 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(scene.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验名称唯一性
        if (!scene.getName().equals(request.getName())
                && sceneRepository.existsByGreenhouseIdAndName(scene.getGreenhouseId(), request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景名称已存在");
        }

        // 校验设备归属
        validateDevicesBelongToGreenhouse(scene.getGreenhouseId(), request.getActions());

        String actionsJson;
        try {
            actionsJson = objectMapper.writeValueAsString(request.getActions());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "动作列表格式错误");
        }

        scene.setName(request.getName());
        scene.setDescription(request.getDescription());
        scene.setActionsJson(actionsJson);

        scene = sceneRepository.save(scene);
        log.info("场景更新成功: id={}, name={}", scene.getId(), scene.getName());

        return toSceneResponse(scene);
    }

    /**
     * 删除场景
     */
    @Transactional
    public void deleteScene(Long userId, Long sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "场景不存在"));

        Greenhouse greenhouse = greenhouseRepository.findById(scene.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        sceneRepository.delete(scene);
        log.info("场景删除成功: id={}, name={}", scene.getId(), scene.getName());
    }

    /**
     * 执行场景（手动触发）
     * <p>
     * 按顺序执行场景中的每个控制动作。
     * </p>
     */
    @Transactional
    public List<ControlLogResponse> executeScene(Long userId, User.Role role, Long sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "场景不存在"));

        if (!scene.getEnabled()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景已禁用");
        }

        // 校验大棚权限
        Greenhouse greenhouse = greenhouseRepository.findById(scene.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        controlService.checkControlPermission(userId, role, greenhouse, null);

        // 解析动作列表
        List<SceneRequest.SceneAction> actions;
        try {
            actions = objectMapper.readValue(scene.getActionsJson(),
                    new TypeReference<List<SceneRequest.SceneAction>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "场景动作数据格式错误");
        }

        // 逐一执行动作
        List<ControlLogResponse> results = new ArrayList<>();
        for (SceneRequest.SceneAction action : actions) {
            try {
                ControlRequest controlRequest = ControlRequest.builder()
                        .deviceId(action.getDeviceId())
                        .action(action.getAction())
                        .build();

                ControlLogResponse result = controlService.controlDevice(userId, role, controlRequest);

                // 标记为场景触发
                ControlLog log = controlLogRepository.findById(result.getId()).orElse(null);
                if (log != null) {
                    log.setSource("SCENE");
                    log.setSceneId(sceneId);
                    controlLogRepository.save(log);
                }
                result.setSource("SCENE");
                result.setSceneId(sceneId);

                results.add(result);
            } catch (Exception e) {
                log.warn("场景执行中设备控制失败: sceneId={}, deviceId={}, error={}",
                        sceneId, action.getDeviceId(), e.getMessage());
                results.add(ControlLogResponse.builder()
                        .deviceId(action.getDeviceId())
                        .action(action.getAction())
                        .source("SCENE")
                        .sceneId(sceneId)
                        .success(false)
                        .failReason(e.getMessage())
                        .build());
            }
        }

        log.info("场景执行完成: sceneId={}, name={}, successCount={}/{}, totalActions={}",
                sceneId, scene.getName(),
                results.stream().filter(ControlLogResponse::getSuccess).count(),
                results.size(), actions.size());

        return results;
    }

    /**
     * 预警联动自动执行场景（系统触发，无用户上下文）
     * <p>
     * 由 AlertEngine 在预警触发时调用；跳过用户权限校验，
     * 保留设备在线/类型校验，控制日志来源标记为 ALERT（预警联动）。
     * </p>
     */
    @Transactional
    public List<ControlLogResponse> executeSceneByAlert(Long sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "场景不存在"));

        if (!scene.getEnabled()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景已禁用");
        }

        // 解析动作列表
        List<SceneRequest.SceneAction> actions;
        try {
            actions = objectMapper.readValue(scene.getActionsJson(),
                    new TypeReference<List<SceneRequest.SceneAction>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "场景动作数据格式错误");
        }

        // 逐一执行动作
        List<ControlLogResponse> results = new ArrayList<>();
        for (SceneRequest.SceneAction action : actions) {
            try {
                ControlLogResponse result = controlService.controlDeviceBySystem(
                        action.getDeviceId(), action.getAction(), sceneId);
                results.add(result);
            } catch (Exception e) {
                log.warn("预警联动场景执行中设备控制失败: sceneId={}, deviceId={}, error={}",
                        sceneId, action.getDeviceId(), e.getMessage());
                results.add(ControlLogResponse.builder()
                        .deviceId(action.getDeviceId())
                        .action(action.getAction())
                        .source("ALERT")
                        .sceneId(sceneId)
                        .success(false)
                        .failReason(e.getMessage())
                        .build());
            }
        }

        log.info("预警联动场景执行完成: sceneId={}, name={}, successCount={}/{}, totalActions={}",
                sceneId, scene.getName(),
                results.stream().filter(ControlLogResponse::getSuccess).count(),
                results.size(), actions.size());

        return results;
    }

    // ===== 辅助方法 =====

    /**
     * 将 Scene 实体转为 SceneResponse（含设备名称）
     */
    private SceneResponse toSceneResponse(Scene scene) {
        List<SceneResponse.SceneActionInfo> actionInfos = new ArrayList<>();
        try {
            List<SceneRequest.SceneAction> actions = objectMapper.readValue(
                    scene.getActionsJson(), new TypeReference<List<SceneRequest.SceneAction>>() {});
            for (SceneRequest.SceneAction action : actions) {
                String deviceName = deviceRepository.findById(action.getDeviceId())
                        .map(Device::getName).orElse("未知设备");
                actionInfos.add(SceneResponse.SceneActionInfo.builder()
                        .deviceId(action.getDeviceId())
                        .deviceName(deviceName)
                        .action(action.getAction())
                        .build());
            }
        } catch (Exception e) {
            log.warn("解析场景动作失败: sceneId={}", scene.getId(), e);
        }

        return SceneResponse.fromEntity(scene, actionInfos);
    }

    /**
     * 校验动作列表中的设备是否都属于指定大棚
     */
    private void validateDevicesBelongToGreenhouse(Long greenhouseId,
                                                    List<SceneRequest.SceneAction> actions) {
        for (SceneRequest.SceneAction action : actions) {
            Device device = deviceRepository.findById(action.getDeviceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

            if (!device.getGreenhouseId().equals(greenhouseId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "设备「" + device.getName() + "」不属于该大棚");
            }

            if (device.getDeviceType() != Device.DeviceType.CONTROLLER) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "设备「" + device.getName() + "」不是控制器，无法加入场景");
            }
        }
    }
}
