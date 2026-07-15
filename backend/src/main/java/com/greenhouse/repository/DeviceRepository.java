package com.greenhouse.repository;

import com.greenhouse.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 设备数据访问层
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /** 按大棚查询所有设备 */
    List<Device> findByGreenhouseId(Long greenhouseId);

    /** 按大棚和设备类型查询 */
    List<Device> findByGreenhouseIdAndDeviceType(Long greenhouseId, Device.DeviceType deviceType);

    /** 按大棚和设备状态查询 */
    List<Device> findByGreenhouseIdAndStatus(Long greenhouseId, Device.DeviceStatus status);

    /** 按大棚和设备编号查询（设备编号在同一大棚下唯一） */
    Optional<Device> findByGreenhouseIdAndDeviceSn(Long greenhouseId, String deviceSn);

    /** 检查设备编号是否已存在（创建时去重） */
    boolean existsByGreenhouseIdAndDeviceSn(Long greenhouseId, String deviceSn);

    /** 检查名称是否已存在 */
    boolean existsByGreenhouseIdAndName(Long greenhouseId, String name);

    /** 统计大棚下设备数量 */
    long countByGreenhouseId(Long greenhouseId);

    /** 按状态统计大棚设备数量 */
    long countByGreenhouseIdAndStatus(Long greenhouseId, Device.DeviceStatus status);

    /** 按设备类型统计大棚设备数量 */
    @Query("SELECT d.deviceType, COUNT(d) FROM Device d WHERE d.greenhouseId = :greenhouseId GROUP BY d.deviceType")
    List<Object[]> countByDeviceTypeGrouped(@Param("greenhouseId") Long greenhouseId);

    /** 批量查询指定ID的设备 */
    List<Device> findByIdIn(List<Long> ids);

    /** 按状态统计全部设备数量（ADMIN 监控用） */
    long countByStatus(Device.DeviceStatus status);
}
