package com.greenhouse.repository;

import com.greenhouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
