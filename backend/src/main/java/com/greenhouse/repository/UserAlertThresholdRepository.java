package com.greenhouse.repository;

import com.greenhouse.entity.UserAlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户自定义预警阈值数据访问层
 */
@Repository
public interface UserAlertThresholdRepository extends JpaRepository<UserAlertThreshold, Long> {

    /** 查询用户对某大棚某传感器的自定义阈值 */
    Optional<UserAlertThreshold> findByUserIdAndGreenhouseIdAndSensorType(
            Long userId, Long greenhouseId, String sensorType);

    /** 查询用户的所有自定义阈值 */
    List<UserAlertThreshold> findByUserId(Long userId);

    /** 查询大棚下所有用户的自定义阈值 */
    List<UserAlertThreshold> findByGreenhouseId(Long greenhouseId);

    /** 查询用户对某大棚的所有自定义阈值 */
    List<UserAlertThreshold> findByUserIdAndGreenhouseId(Long userId, Long greenhouseId);
}
