package com.greenhouse.module.firmware.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Device;
import com.greenhouse.entity.Firmware;
import com.greenhouse.module.firmware.dto.FirmwareBatchRequest;
import com.greenhouse.module.firmware.dto.FirmwareResponse;
import com.greenhouse.repository.FirmwareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 固件管理服务
 * <p>
 * 负责出厂固件的批量预注册、查询。
 * 固件绑定/解绑在设备创建/删除时由 {@code DeviceService} 联动处理。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareService {

    private final FirmwareRepository firmwareRepository;

    /** 固件ID固定8位数字 */
    public static final int FIRMWARE_ID_LENGTH = 8;

    /**
     * 批量预注册固件
     * <p>
     * 从当前最大固件ID +1 开始生成，8位数字零填充（如 00000001）。
     * </p>
     */
    @Transactional
    public List<FirmwareResponse> batchRegister(FirmwareBatchRequest request) {
        // 传感器类固件必须指定传感器类型
        if (request.getDeviceType() == Device.DeviceType.SENSOR && request.getSensorType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "传感器类固件必须指定传感器类型");
        }

        // 计算起始ID
        long start = 0;
        String maxId = firmwareRepository.findMaxFirmwareId().orElse(null);
        if (maxId != null && maxId.matches("\\d+")) {
            start = Long.parseLong(maxId);
        }

        List<Firmware> saved = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            long seq = start + 1 + i;
            String firmwareId = String.format("%0" + FIRMWARE_ID_LENGTH + "d", seq);

            Firmware firmware = Firmware.builder()
                    .firmwareId(firmwareId)
                    .deviceType(request.getDeviceType())
                    .sensorType(request.getSensorType())
                    .firmwareVersion(request.getFirmwareVersion())
                    .batchNo(request.getBatchNo())
                    .status(Firmware.Status.UNBOUND)
                    .build();
            saved.add(firmwareRepository.save(firmware));
        }

        log.info("固件批量预注册成功: count={}, startId={}, endId={}, deviceType={}, batchNo={}",
                request.getCount(),
                String.format("%0" + FIRMWARE_ID_LENGTH + "d", start + 1),
                String.format("%0" + FIRMWARE_ID_LENGTH + "d", start + request.getCount()),
                request.getDeviceType(), request.getBatchNo());

        return saved.stream().map(FirmwareResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * 分页查询固件列表（可按状态筛选）
     *
     * @return {records: List<FirmwareResponse>, total: 总数, page: 页码, size: 每页条数}
     */
    public Map<String, Object> list(Firmware.Status status, int page, int size) {
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Firmware> result = status != null
                ? firmwareRepository.findByStatus(status, pageable)
                : firmwareRepository.findAll(pageable);

        Map<String, Object> body = new HashMap<>();
        body.put("records", result.getContent().stream()
                .map(FirmwareResponse::fromEntity)
                .collect(Collectors.toList()));
        body.put("total", result.getTotalElements());
        body.put("page", page);
        body.put("size", safeSize);
        return body;
    }

    /**
     * 查询固件详情
     */
    public FirmwareResponse get(String firmwareId) {
        Firmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "固件不存在"));
        return FirmwareResponse.fromEntity(firmware);
    }

    /**
     * 统计未绑定固件数量（Web 固件管理页角标用）
     */
    public long countUnbound() {
        return firmwareRepository.countByStatus(Firmware.Status.UNBOUND);
    }
}
