package com.greenhouse.module.permission.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.EmployeePermission;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.permission.dto.*;
import com.greenhouse.repository.EmployeePermissionRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工权限管理服务
 * <p>
 * 负责棚主管理员工及其权限的全部业务逻辑。
 * 核心规则：
 * 1. 员工只能归属一个棚主
 * 2. 一个员工可以对多个大棚拥有不同权限
 * 3. 棚主只能管理自己的员工
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final EmployeePermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final GreenhouseRepository greenhouseRepository;

    /** 每个棚主最多管理的员工数 */
    private static final long MAX_EMPLOYEES_PER_OWNER = 20;

    /**
     * 添加员工（邀请）
     * <p>
     * 通过用户名或手机号查找员工账号，将其归属到当前棚主名下，
     * 并分配对指定大棚的权限。
     * </p>
     */
    @Transactional
    public PermissionResponse addEmployee(Long ownerId, AddEmployeeRequest request) {
        // 1. 校验棚主身份
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NOT_OWNER);
        }

        // 2. 通过用户名或手机号查找员工
        User employee = userRepository.findByUsername(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR,
                        "未找到该用户，请确认用户名或手机号正确"));

        // 3. 校验员工角色
        if (employee.getRole() != User.Role.WORKER) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该用户不是员工角色（角色: " + employee.getRole() + "），只有 WORKER 角色才能被添加为员工");
        }

        // 4. 校验员工归属：如果已有归属棚主，且不是当前棚主，则拒绝
        if (employee.getOwnerId() != null && !employee.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该员工已归属其他棚主，不能重复添加");
        }

        // 5. 首次添加时绑定棚主关系
        if (employee.getOwnerId() == null) {
            // 校验员工数量上限
            long employeeCount = userRepository.countByRoleAndOwnerId(User.Role.WORKER, ownerId);
            if (employeeCount >= MAX_EMPLOYEES_PER_OWNER) {
                throw new BusinessException(ErrorCode.EMPLOYEE_LIMIT_EXCEEDED);
            }
            employee.setOwnerId(ownerId);
            userRepository.save(employee);
            log.info("员工归属绑定: employeeId={}, ownerId={}", employee.getId(), ownerId);
        }

        // 6. 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 7. 检查是否已有该大棚的权限
        if (permissionRepository.existsByEmployeeIdAndGreenhouseId(employee.getId(), request.getGreenhouseId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该员工已拥有此大棚的权限，请使用更新权限接口");
        }

        // 8. 创建权限记录
        EmployeePermission permission = EmployeePermission.builder()
                .employeeId(employee.getId())
                .ownerId(ownerId)
                .greenhouseId(request.getGreenhouseId())
                .canViewData(request.getCanViewData())
                .canControlDevice(request.getCanControlDevice())
                .canDiagnose(request.getCanDiagnose())
                .canAskExpert(request.getCanAskExpert())
                .canViewAlerts(request.getCanViewAlerts())
                .canViewHistory(request.getCanViewHistory())
                .build();

        permission = permissionRepository.save(permission);
        log.info("员工权限分配成功: employeeId={}, greenhouseId={}, ownerId={}",
                employee.getId(), request.getGreenhouseId(), ownerId);

        return PermissionResponse.fromEntity(permission, greenhouse.getName());
    }

    /**
     * 获取员工列表（棚主视角）
     */
    public List<EmployeeResponse> listEmployees(Long ownerId) {
        List<User> employees = userRepository.findByRoleAndOwnerId(User.Role.WORKER, ownerId);
        return employees.stream()
                .map(EmployeeResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * 获取员工权限列表（棚主查看某个员工的所有权限）
     */
    public List<PermissionResponse> getEmployeePermissions(Long ownerId, Long employeeId) {
        // 校验该员工确实归属当前棚主
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!ownerId.equals(employee.getOwnerId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<EmployeePermission> permissions = permissionRepository.findByEmployeeId(employeeId);
        return permissions.stream()
                .map(p -> {
                    Greenhouse gh = greenhouseRepository.findById(p.getGreenhouseId())
                            .orElse(null);
                    String ghName = gh != null ? gh.getName() : "未知大棚";
                    return PermissionResponse.fromEntity(p, ghName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新员工权限
     */
    @Transactional
    public PermissionResponse updatePermission(Long ownerId, Long employeeId, UpdatePermissionRequest request) {
        // 校验员工归属
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!ownerId.equals(employee.getOwnerId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 查找权限记录
        EmployeePermission permission = permissionRepository
                .findByEmployeeIdAndGreenhouseId(employeeId, request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR,
                        "该员工没有此大棚的权限记录"));

        // 更新非空字段
        if (request.getCanViewData() != null) {
            permission.setCanViewData(request.getCanViewData());
        }
        if (request.getCanControlDevice() != null) {
            permission.setCanControlDevice(request.getCanControlDevice());
        }
        if (request.getCanDiagnose() != null) {
            permission.setCanDiagnose(request.getCanDiagnose());
        }
        if (request.getCanAskExpert() != null) {
            permission.setCanAskExpert(request.getCanAskExpert());
        }
        if (request.getCanViewAlerts() != null) {
            permission.setCanViewAlerts(request.getCanViewAlerts());
        }
        if (request.getCanViewHistory() != null) {
            permission.setCanViewHistory(request.getCanViewHistory());
        }

        permission = permissionRepository.save(permission);
        log.info("员工权限更新成功: employeeId={}, greenhouseId={}", employeeId, request.getGreenhouseId());

        Greenhouse gh = greenhouseRepository.findById(permission.getGreenhouseId()).orElse(null);
        String ghName = gh != null ? gh.getName() : "未知大棚";
        return PermissionResponse.fromEntity(permission, ghName);
    }

    /**
     * 删除员工（移除所有权限 + 解除归属关系）
     */
    @Transactional
    public void removeEmployee(Long ownerId, Long employeeId) {
        // 校验员工归属
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!ownerId.equals(employee.getOwnerId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 删除所有权限记录
        permissionRepository.deleteByEmployeeIdAndOwnerId(employeeId, ownerId);

        // 解除归属关系
        employee.setOwnerId(null);
        userRepository.save(employee);

        log.info("员工已移除: employeeId={}, ownerId={}", employeeId, ownerId);
    }

    // ===== 员工端方法 =====

    /**
     * 员工查看自己的权限
     */
    public List<PermissionResponse> getMyPermissions(Long workerId) {
        List<EmployeePermission> permissions = permissionRepository.findByEmployeeId(workerId);
        return permissions.stream()
                .map(p -> {
                    Greenhouse gh = greenhouseRepository.findById(p.getGreenhouseId()).orElse(null);
                    String ghName = gh != null ? gh.getName() : "未知大棚";
                    return PermissionResponse.fromEntity(p, ghName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 员工查看自己可访问的大棚ID列表
     */
    public List<Long> getMyGreenhouseIds(Long workerId) {
        List<EmployeePermission> permissions = permissionRepository.findByEmployeeId(workerId);
        return permissions.stream()
                .map(EmployeePermission::getGreenhouseId)
                .distinct()
                .collect(Collectors.toList());
    }
}
