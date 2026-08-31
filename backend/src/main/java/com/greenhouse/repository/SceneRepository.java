package com.greenhouse.repository;

import com.greenhouse.entity.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 场景联动数据访问层
 */
@Repository
public interface SceneRepository extends JpaRepository<Scene, Long> {

    /** 按大棚查询所有场景 */
    List<Scene> findByGreenhouseId(Long greenhouseId);

    /** 按大棚查询已启用的场景 */
    List<Scene> findByGreenhouseIdAndEnabledTrue(Long greenhouseId);

    /** 检查名称是否已存在 */
    boolean existsByGreenhouseIdAndName(Long greenhouseId, String name);

    /** 统计大棚下场景数量 */
    long countByGreenhouseId(Long greenhouseId);


    void deleteByGreenhouseId(Long greenhouseId);
}
