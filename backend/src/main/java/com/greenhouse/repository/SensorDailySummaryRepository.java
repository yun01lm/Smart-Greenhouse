package com.greenhouse.repository;

import com.greenhouse.entity.SensorDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 传感器日汇总数据访问层
 */
@Repository
public interface SensorDailySummaryRepository extends JpaRepository<SensorDailySummary, Long> {

    /** 按大棚+传感器类型+日期区间查询（7天/30天历史趋势图读取） */
    List<SensorDailySummary> findByGreenhouseIdAndSensorTypeAndStatDateBetween(
            Long greenhouseId, String sensorType, LocalDate start, LocalDate end);

    /** 查询某日某大棚某类型全部设备汇总（回填/校验用） */
    List<SensorDailySummary> findByGreenhouseIdAndSensorTypeAndStatDate(
            Long greenhouseId, String sensorType, LocalDate statDate);

    /** 查询日期区间内所有大棚/类型/设备汇总（按设备回填时用） */
    List<SensorDailySummary> findByStatDateBetween(LocalDate start, LocalDate end);

    /** 幂等检查：同日同设备同类型是否已生成 */
    boolean existsByGreenhouseIdAndDeviceIdAndSensorTypeAndStatDate(
            Long greenhouseId, Long deviceId, String sensorType, LocalDate statDate);

    /** 幂等检查：某日某大棚某类型是否已存在任意设备汇总 */
    boolean existsByGreenhouseIdAndSensorTypeAndStatDate(
            Long greenhouseId, String sensorType, LocalDate statDate);

    /** 查询某设备某日汇总（单设备回填） */
    Optional<SensorDailySummary> findByGreenhouseIdAndDeviceIdAndSensorTypeAndStatDate(
            Long greenhouseId, Long deviceId, String sensorType, LocalDate statDate);
}