package com.greenhouse.repository;

import com.greenhouse.entity.Greenhouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 大棚数据访问层
 */
@Repository
public interface GreenhouseRepository extends JpaRepository<Greenhouse, Long> {

    /** 根据棚主查找 */
    List<Greenhouse> findByOwnerId(Long ownerId);

    /** 根据棚主和状态查找 */
    List<Greenhouse> findByOwnerIdAndStatus(Long ownerId, Boolean status);

    /** 检查棚主名下是否有重名大棚 */
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    /** 统计某棚主的大棚数量 */
    long countByOwnerId(Long ownerId);

    // ===== 地区统计（管理员功能） =====

    /** 按省份统计 */
    @Query("SELECT g.province, COUNT(g) FROM Greenhouse g WHERE g.province IS NOT NULL GROUP BY g.province")
    List<Object[]> countByProvince();

    /** 按城市统计 */
    @Query("SELECT g.province, g.city, COUNT(g) FROM Greenhouse g WHERE g.city IS NOT NULL GROUP BY g.province, g.city")
    List<Object[]> countByCity();

    /** 按区县统计 */
    @Query("SELECT g.district, COUNT(g) FROM Greenhouse g WHERE g.district IS NOT NULL GROUP BY g.district")
    List<Object[]> countByDistrict();

    /** 根据地区筛选大棚 */
    List<Greenhouse> findByProvinceAndCityAndDistrict(String province, String city, String district);
}
