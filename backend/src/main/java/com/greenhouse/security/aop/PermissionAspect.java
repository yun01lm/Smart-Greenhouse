package com.greenhouse.security.aop;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.EmployeePermission;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.entity.DataAuthorization;
import com.greenhouse.repository.DataAuthorizationRepository;
import com.greenhouse.repository.EmployeePermissionRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.security.annotations.RequireFunction;
import com.greenhouse.security.annotations.RequireGreenhouseAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * 权限校验 AOP 切面
 * <p>
 * 拦截带 @RequireGreenhouseAccess 和 @RequireFunction 注解的方法，
 * 自动校验当前用户是否有对应大棚和功能的访问权限。
 * </p>
 *
 * <h3>权限校验规则</h3>
 * <ul>
 *   <li>ADMIN：全部放行</li>
 *   <li>OWNER：检查大棚是否归属自己</li>
 *   <li>WORKER：检查 employee_permissions 表中是否有对应授权</li>
 *   <li>EXPERT：暂不处理（后续在 C17 专家授权模块实现）</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final GreenhouseRepository greenhouseRepository;
    private final EmployeePermissionRepository permissionRepository;
    private final DataAuthorizationRepository dataAuthorizationRepository;

    /**
     * 校验大棚访问权限
     * <p>
     * 从方法参数中自动提取 greenhouseId（支持 @PathVariable 和 @RequestParam）。
     * 支持的参数名：greenhouseId、id（当方法只有一个路径变量时）。
     * </p>
     */
    @Before("@annotation(com.greenhouse.security.annotations.RequireGreenhouseAccess)")
    public void checkGreenhouseAccess(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) auth.getPrincipal();
        String roleStr = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_OWNER");
        User.Role role = User.Role.valueOf(roleStr.replace("ROLE_", ""));

        // ADMIN 全部放行
        if (role == User.Role.ADMIN) {
            return;
        }

        // 提取 greenhouseId
        Long greenhouseId = extractGreenhouseId(joinPoint);
        if (greenhouseId == null) {
            log.warn("@RequireGreenhouseAccess 无法从方法参数中提取 greenhouseId: {}",
                    joinPoint.getSignature().toShortString());
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法确定大棚ID");
        }

        // 校验权限
        switch (role) {
            case OWNER -> {
                Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
                if (!greenhouse.getOwnerId().equals(userId)) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
            }
            case WORKER -> {
                Optional<EmployeePermission> perm = permissionRepository
                        .findByEmployeeIdAndGreenhouseId(userId, greenhouseId);
                if (perm.isEmpty()) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                // 员工至少需要 canViewData 权限才能访问
                if (!perm.get().getCanViewData()) {
                    throw new BusinessException(ErrorCode.FUNCTION_DENIED);
                }
            }
            case TECHNICIAN -> {
                // 技术员与普通员工同走权限表校验（默认权限全开，可被棚主收紧）
                Optional<EmployeePermission> techPerm = permissionRepository
                        .findByEmployeeIdAndGreenhouseId(userId, greenhouseId);
                if (techPerm.isEmpty()) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                if (!techPerm.get().getCanViewData()) {
                    throw new BusinessException(ErrorCode.FUNCTION_DENIED);
                }
            }
            case EXPERT -> {
                // 检查是否有有效的数据授权（APPROVED + 未过期）
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.util.Optional<com.greenhouse.entity.DataAuthorization> dataAuth =
                        dataAuthorizationRepository.findTopByExpertIdAndGreenhouseIdAndStatusAndExpiresAtAfterOrderByApprovedAtDesc(
                                userId, greenhouseId, com.greenhouse.entity.DataAuthorization.AuthorizationStatus.APPROVED, now);
                if (dataAuth.isEmpty()) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
            }
        }
    }

    /**
     * 校验功能权限
     * <p>
     * 仅对 WORKER 角色生效，OWNER 和 ADMIN 直接放行。
     * </p>
     */
    @Before("@annotation(requireFunction)")
    public void checkFunctionAccess(JoinPoint joinPoint, RequireFunction requireFunction) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) auth.getPrincipal();
        String roleStr = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_OWNER");
        User.Role role = User.Role.valueOf(roleStr.replace("ROLE_", ""));

        // ADMIN 和 OWNER 全部放行
        if (role == User.Role.ADMIN || role == User.Role.OWNER) {
            return;
        }
        if (role == User.Role.EXPERT) {
            return; // 专家权限已由 checkGreenhouseAccess 校验
        }

        // 仅员工层级（WORKER/TECHNICIAN）需要校验权限位
        if (role == User.Role.WORKER || role == User.Role.TECHNICIAN) {
            Long greenhouseId = extractGreenhouseId(joinPoint);
            if (greenhouseId == null) {
                log.warn("@RequireFunction 无法从方法参数中提取 greenhouseId: {}",
                        joinPoint.getSignature().toShortString());
                throw new BusinessException(ErrorCode.PARAM_ERROR, "无法确定大棚ID");
            }

            Optional<EmployeePermission> perm = permissionRepository
                    .findByEmployeeIdAndGreenhouseId(userId, greenhouseId);
            if (perm.isEmpty()) {
                throw new BusinessException(ErrorCode.FUNCTION_DENIED);
            }

            // 根据功能标识校验对应权限
            String function = requireFunction.value();
            boolean hasPermission = switch (function) {
                case "VIEW_DATA" -> perm.get().getCanViewData();
                case "CONTROL_DEVICE" -> perm.get().getCanControlDevice();
                case "DIAGNOSE" -> perm.get().getCanDiagnose();
                case "ASK_EXPERT" -> perm.get().getCanAskExpert();
                case "VIEW_ALERTS" -> perm.get().getCanViewAlerts();
                case "VIEW_HISTORY" -> perm.get().getCanViewHistory();
                default -> {
                    log.warn("未知的功能标识: {}", function);
                    yield false;
                }
            };

            if (!hasPermission) {
                throw new BusinessException(ErrorCode.FUNCTION_DENIED);
            }
        }
    }

    /**
     * 从方法参数中提取 greenhouseId
     * <p>
     * 策略：
     * 1. 优先查找名为 "greenhouseId" 的参数
     * 2. 如果没有，查找名为 "id" 的参数（适用于单一路径变量的情况）
     * 3. 支持 @PathVariable 和 @RequestParam 注解
     * </p>
     */
    private Long extractGreenhouseId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 第一轮：精确匹配 "greenhouseId"
        for (int i = 0; i < parameters.length; i++) {
            String paramName = getParameterName(parameters[i]);
            if ("greenhouseId".equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        // 第二轮：匹配 "id"（当只有一个 Long 类型参数时更可靠）
        for (int i = 0; i < parameters.length; i++) {
            String paramName = getParameterName(parameters[i]);
            if ("id".equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        // 第三轮：任何 Long 类型的第一个参数（兜底）
        for (int i = 0; i < parameters.length; i++) {
            if (args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        return null;
    }

    /**
     * 获取参数名（优先从注解获取，其次用反射名）
     */
    private String getParameterName(Parameter parameter) {
        // 优先从 @PathVariable 获取
        org.springframework.web.bind.annotation.PathVariable pathVar =
                parameter.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
        if (pathVar != null) {
            String value = pathVar.value();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // 其次从 @RequestParam 获取
        org.springframework.web.bind.annotation.RequestParam requestParam =
                parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
        if (requestParam != null) {
            String value = requestParam.value();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // 兜底使用反射参数名（需要 -parameters 编译参数）
        return parameter.getName();
    }
}
