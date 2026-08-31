package com.greenhouse.repository;

import com.greenhouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 根据用户名查找 */
    Optional<User> findByUsername(String username);

    /** 根据手机号查找 */
    Optional<User> findByPhone(String phone);

    /** 按用户名模糊查询（R9 咨询记录用户筛选用） */
    List<User> findByUsernameContaining(String keyword);

    /** 检查用户名是否存在 */
    boolean existsByUsername(String username);

    /** 检查手机号是否存在 */
    boolean existsByPhone(String phone);

    /** 根据角色查找 */
    List<User> findByRole(User.Role role);

    /** 查找某棚主名下的所有员工 */
    List<User> findByOwnerIdAndRole(Long ownerId, User.Role role);

    /** 查找在线专家 */
    List<User> findByRoleAndExpertStatus(User.Role role, User.ExpertStatus expertStatus);

    /** 查找某角色下某棚主的所有用户 */
    List<User> findByRoleAndOwnerId(User.Role role, Long ownerId);

    /** 统计某角色下某棚主的用户数量 */
    long countByRoleAndOwnerId(User.Role role, Long ownerId);

    /** 统计某角色的用户总数 */
    long countByRole(User.Role role);

    // ===== 用户地区聚合（R46.2：地区选项 = 大棚表 ∪ 用户表） =====

    /** 全部省份（用户表，按用户自身填写地区聚合） */
    @Query("SELECT DISTINCT u.province FROM User u WHERE u.province IS NOT NULL AND u.province <> '' ORDER BY u.province")
    List<String> findDistinctUserProvinces();

    /** 某省下的城市（用户表） */
    @Query("SELECT DISTINCT u.city FROM User u WHERE u.city IS NOT NULL AND u.city <> '' AND u.province = :province ORDER BY u.city")
    List<String> findDistinctUserCities(@Param("province") String province);

    /** 某省市下的区县（用户表） */
    @Query("SELECT DISTINCT u.district FROM User u WHERE u.district IS NOT NULL AND u.district <> '' AND u.province = :province AND u.city = :city ORDER BY u.district")
    List<String> findDistinctUserDistricts(@Param("province") String province, @Param("city") String city);

    /** 某省市县下的乡镇（用户表） */
    @Query("SELECT DISTINCT u.town FROM User u WHERE u.town IS NOT NULL AND u.town <> '' AND u.province = :province AND u.city = :city AND u.district = :district ORDER BY u.town")
    List<String> findDistinctUserTowns(@Param("province") String province, @Param("city") String city, @Param("district") String district);

    /** 某省市县镇下的村（用户表） */
    @Query("SELECT DISTINCT u.village FROM User u WHERE u.village IS NOT NULL AND u.village <> '' AND u.province = :province AND u.city = :city AND u.district = :district AND u.town = :town ORDER BY u.village")
    List<String> findDistinctUserVillages(@Param("province") String province, @Param("city") String city, @Param("district") String district, @Param("town") String town);
}
