package com.greenhouse.repository;

import com.greenhouse.entity.Firmware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 固件数据访问层
 */
@Repository
public interface FirmwareRepository extends JpaRepository<Firmware, String> {

    /** 按状态查询固件 */
    List<Firmware> findByStatus(Firmware.Status status);

    /** 按状态分页查询固件 */
    Page<Firmware> findByStatus(Firmware.Status status, Pageable pageable);

    /** 按状态统计 */
    long countByStatus(Firmware.Status status);

    /** 查询当前最大固件ID（用于批量预注册生成下一个ID） */
    @Query("SELECT MAX(f.firmwareId) FROM Firmware f")
    Optional<String> findMaxFirmwareId();

    /** 检查固件ID是否已被某设备绑定（排除指定设备，用于解绑/重绑场景） */
    boolean existsByBoundDeviceIdAndFirmwareIdNot(Long boundDeviceId, String firmwareId);
}
