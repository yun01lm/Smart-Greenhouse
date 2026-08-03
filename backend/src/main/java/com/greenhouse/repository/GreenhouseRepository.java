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

    // ===== 地区聚合（管理员功能，R2） =====

    /** 全部省份 */
    @Query("SELECT DISTINCT g.province FROM Greenhouse g WHERE g.province IS NOT NULL AND g.province <> '' ORDER BY g.province")
    List<String> findDistinctProvinces();

    /** 某省下的城市 */
    @Query("SELECT DISTINCT g.city FROM Greenhouse g WHERE g.city IS NOT NULL AND g.city <> '' AND g.province = :province ORDER BY g.city")
    List<String> findDistinctCities(@Param("province") String province);

    /** 某省市下的区县 */
    @Query("SELECT DISTINCT g.district FROM Greenhouse g WHERE g.district IS NOT NULL AND g.district <> '' AND g.province = :province AND g.city = :city ORDER BY g.district")
    List<String> findDistinctDistricts(@Param("province") String province, @Param("city") String city);

    /** 某省市县下的乡镇 */
    @Query("SELECT DISTINCT g.town FROM Greenhouse g WHERE g.town IS NOT NULL AND g.town <> '' AND g.province = :province AND g.city = :city AND g.district = :district ORDER BY g.town")
    List<String> findDistinctTowns(@Param("province") String province, @Param("city") String city, @Param("district") String district);

    /** 某省市县镇下的村 */
    @Query("SELECT DISTINCT g.village FROM Greenhouse g WHERE g.village IS NOT NULL AND g.village <> '' AND g.province = :province AND g.city = :city AND g.district = :district AND g.town = :town ORDER BY g.village")
    List<String> findDistinctVillages(@Param("province") String province, @Param("city") String city, @Param("district") String district, @Param("town") String town);

    /** 按五级地区筛选大棚（各级参数均可空） */
    @Query("SELECT g FROM Greenhouse g WHERE (:province IS NULL OR g.province = :province) AND (:city IS NULL OR g.city = :city) AND (:district IS NULL OR g.district = :district) AND (:town IS NULL OR g.town = :town) AND (:village IS NULL OR g.village = :village)")
    List<Greenhouse> findByRegion(@Param("province") String province, @Param("city") String city, @Param("district") String district, @Param("town") String town, @Param("village") String village);
}
