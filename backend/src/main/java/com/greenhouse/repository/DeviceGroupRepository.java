package com.greenhouse.repository;

import com.greenhouse.entity.DeviceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备分组数据访问层
 */
@Repository
public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {

    /** 按大棚查询所有分组 */
    List<DeviceGroup> findByGreenhouseId(Long greenhouseId);

    /** 检查分组名称是否已存在 */
    boolean existsByGreenhouseIdAndName(Long greenhouseId, String name);

    /** 统计大棚下分组数量 */
    long countByGreenhouseId(Long greenhouseId);
}
