package com.greenhouse.repository;

import com.greenhouse.entity.EmployeePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 员工权限数据访问层
 */
@Repository
public interface EmployeePermissionRepository extends JpaRepository<EmployeePermission, Long> {

    /** 查询员工在指定大棚下的权限 */
    Optional<EmployeePermission> findByEmployeeIdAndGreenhouseId(Long employeeId, Long greenhouseId);

    /** 查询员工的所有权限记录 */
    List<EmployeePermission> findByEmployeeId(Long employeeId);

    /** 查询棚主分配的所有权限 */
    List<EmployeePermission> findByOwnerId(Long ownerId);

    /** 查询某大棚下所有被授权的员工权限 */
    List<EmployeePermission> findByGreenhouseId(Long greenhouseId);

    /** 检查员工是否已有该大棚的权限 */
    boolean existsByEmployeeIdAndGreenhouseId(Long employeeId, Long greenhouseId);

    /** 删除员工在某大棚下的权限 */
    void deleteByEmployeeIdAndGreenhouseId(Long employeeId, Long greenhouseId);

    /** 删除棚主给某员工分配的所有权限 */
    void deleteByEmployeeIdAndOwnerId(Long employeeId, Long ownerId);
}
