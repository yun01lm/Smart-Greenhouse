package com.greenhouse.repository;

import com.greenhouse.entity.ControlLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 控制日志数据访问层
 */
@Repository
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {

    /** 按设备ID查询控制日志 */
    List<ControlLog> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    /** 按大棚ID查询（通过设备关联） */
    Page<ControlLog> findByDeviceIdInOrderByCreatedAtDesc(List<Long> deviceIds, Pageable pageable);

    /** 按用户ID查询 */
    List<ControlLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按场景ID查询 */
    List<ControlLog> findBySceneId(Long sceneId);
}
