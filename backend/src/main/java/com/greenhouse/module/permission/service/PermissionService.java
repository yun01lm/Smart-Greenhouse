package com.greenhouse.module.permission.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.common.PasswordPolicy;
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

import java.util.ArrayList;
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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** 每个棚主最多管理的员工数 */
    private static final long MAX_EMPLOYEES_PER_OWNER = 20;

    /**
     * 添加员工（创建或邀请，R23）
     * <p>
     * 创建模式：棚主直接注册员工账号（WORKER/TECHNICIAN），自动归属当前棚主；
     * 邀请模式：按用户名/手机号绑定已存在员工账号。
     * 权限字段为空时按角色默认值填充（WORKER：看数据+控设备+看预警；TECHNICIAN：全部权限）。
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

        // 2. 解析员工账号：创建 or 邀请
        User employee;
        if (request.getIdentifier() != null && !request.getIdentifier().isBlank()) {
            // ---- 邀请模式 ----
            employee = userRepository.findByUsername(request.getIdentifier().trim())
                    .or(() -> userRepository.findByPhone(request.getIdentifier().trim()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR,
                            "未找到该用户，请确认用户名或手机号正确"));
            if (employee.getRole() != User.Role.WORKER && employee.getRole() != User.Role.TECHNICIAN) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "该用户不是员工角色（角色: " + employee.getRole() + "），只有 WORKER/TECHNICIAN 角色才能被添加为员工");
            }
            if (employee.getOwnerId() != null && !employee.getOwnerId().equals(ownerId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该员工已归属其他棚主，不能重复添加");
            }
            if (employee.getOwnerId() == null) {
                employee.setOwnerId(ownerId);
                userRepository.save(employee);
                log.info("员工归属绑定: employeeId={}, ownerId={}", employee.getId(), ownerId);
            }
        } else {
            // ---- 创建模式 ----
            User.Role roleType = request.getRoleType();
            if (roleType != User.Role.WORKER && roleType != User.Role.TECHNICIAN) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "员工类型只能是 WORKER（普通员工）或 TECHNICIAN（技术员）");
            }
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "创建员工必须填写用户名");
            }
            if (userRepository.existsByUsername(request.getUsername().trim())) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS);
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()
                    && userRepository.existsByPhone(request.getPhone().trim())) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "创建员工必须填写初始密码");
            }
            PasswordPolicy.validate(request.getPassword());

            long employeeCount = userRepository.countByRoleAndOwnerId(User.Role.WORKER, ownerId)
                    + userRepository.countByRoleAndOwnerId(User.Role.TECHNICIAN, ownerId);
            if (employeeCount >= MAX_EMPLOYEES_PER_OWNER) {
                throw new BusinessException(ErrorCode.EMPLOYEE_LIMIT_EXCEEDED);
            }

            employee = User.builder()
                    .username(request.getUsername().trim())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                    .realName(request.getRealName())
                    .role(roleType)
                    .ownerId(ownerId)
                    .status(true)
                    .build();
            employee = userRepository.save(employee);
            log.info("棚主创建员工账号: employeeId={}, username={}, role={}",
                    employee.getId(), employee.getUsername(), roleType);
        }

        // 3. 大棚归属校验与权限记录
        if (request.getGreenhouseId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择授权大棚");
        }
        Greenhouse greenhouse = greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }
        if (permissionRepository.existsByEmployeeIdAndGreenhouseId(employee.getId(), request.getGreenhouseId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该员工已拥有此大棚的权限，请使用更新权限接口");
        }

        User.Role role = employee.getRole();
        EmployeePermission permission = EmployeePermission.builder()
                .employeeId(employee.getId())
                .ownerId(ownerId)
                .greenhouseId(request.getGreenhouseId())
                .canViewData(permOrDefault(request.getCanViewData(), true, role))
                .canControlDevice(permOrDefault(request.getCanControlDevice(), true, role))
                .canDiagnose(permOrDefault(request.getCanDiagnose(), false, role))
                .canAskExpert(permOrDefault(request.getCanAskExpert(), false, role))
                .canViewAlerts(permOrDefault(request.getCanViewAlerts(), true, role))
                .canViewHistory(permOrDefault(request.getCanViewHistory(), false, role))
                .build();

        permission = permissionRepository.save(permission);
        log.info("员工权限分配成功: employeeId={}, greenhouseId={}, ownerId={}",
                employee.getId(), request.getGreenhouseId(), ownerId);

        return PermissionResponse.fromEntity(permission, greenhouse.getName());
    }

    /**
     * 权限取值：显式指定用之；否则技术员默认全部开放，普通员工按 workerDefault
     */
    private boolean permOrDefault(Boolean value, boolean workerDefault, User.Role role) {
        if (value != null) {
            return value;
        }
        if (role == User.Role.TECHNICIAN) {
            return true;
        }
        return workerDefault;
    }

    /**
     * 棚主重置员工密码（R23）
     */
    @Transactional
    public void resetEmployeePassword(Long ownerId, Long employeeId, String newPassword) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "员工不存在"));
        if (!ownerId.equals(employee.getOwnerId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        PasswordPolicy.validate(newPassword);
        employee.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(employee);
        log.info("棚主重置员工密码: employeeId={}, ownerId={}", employeeId, ownerId);
    }

    /**
     * 获取员工列表（棚主视角）
     */
    public List<EmployeeResponse> listEmployees(Long ownerId) {
        List<User> employees = new ArrayList<>();
        employees.addAll(userRepository.findByRoleAndOwnerId(User.Role.WORKER, ownerId));
        employees.addAll(userRepository.findByRoleAndOwnerId(User.Role.TECHNICIAN, ownerId));
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
     * 更新员工基本信息（姓名、手机号）
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long ownerId, Long employeeId, UpdateEmployeeRequest request) {
        // 校验员工归属
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!ownerId.equals(employee.getOwnerId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 更新非空字段
        if (request.getRealName() != null && !request.getRealName().isBlank()) {
            employee.setRealName(request.getRealName());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            // 校验手机号唯一性
            if (!request.getPhone().equals(employee.getPhone())
                    && userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已被其他用户使用");
            }
            employee.setPhone(request.getPhone());
        }

        employee = userRepository.save(employee);
        log.info("员工信息更新成功: employeeId={}, ownerId={}", employeeId, ownerId);

        return EmployeeResponse.fromUser(employee);
    }
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
