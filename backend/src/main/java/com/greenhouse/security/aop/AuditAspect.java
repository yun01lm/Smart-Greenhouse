package com.greenhouse.security.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.AuditLog;
import com.greenhouse.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 审计日志 AOP 切面
 * <p>
 * 自动拦截标注了 {@link org.springframework.web.bind.annotation.PostMapping}、
 * {@link org.springframework.web.bind.annotation.PutMapping}、
 * {@link org.springframework.web.bind.annotation.DeleteMapping} 的 Controller 方法，
 * 记录操作日志到 audit_logs 表。
 * </p>
 *
 * <h3>记录内容</h3>
 * <ul>
 *   <li>操作用户（ID + 用户名）</li>
 *   <li>操作类型（方法名自动推断）</li>
 *   <li>操作目标（从方法参数提取）</li>
 *   <li>请求IP、HTTP方法、URI</li>
 *   <li>操作结果（SUCCESS/FAILURE）</li>
 *   <li>耗时（毫秒）</li>
 * </ul>
 *
 * <h3>排除项</h3>
 * <p>
 * 以下请求不记录审计日志：
 * <ul>
 *   <li>登录请求（/auth/login）</li>
 *   <li>GET 请求（查询类操作）</li>
 *   <li>WebSocket 升级请求</li>
 * </ul>
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /** 切点：所有 Controller 类中的方法 */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    /** 切点：写操作（POST/PUT/DELETE） */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void writeOperations() {}

    /**
     * 环绕通知：记录写操作的审计日志
     */
    @Around("controllerMethods() && writeOperations()")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String result = "SUCCESS";
        String failReason = null;

        try {
            Object returnValue = joinPoint.proceed();
            return returnValue;
        } catch (Throwable e) {
            result = "FAILURE";
            failReason = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "未知错误";
            throw e;
        } finally {
            try {
                long elapsedMs = System.currentTimeMillis() - startTime;
                saveAuditLog(joinPoint, result, failReason, elapsedMs);
            } catch (Exception e) {
                // 审计日志记录失败不影响主业务流程
                log.warn("审计日志记录失败: {}", e.getMessage());
            }
        }
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, String result,
                               String failReason, long elapsedMs) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String methodName = method.getName();
            String className = signature.getDeclaringType().getSimpleName();

            // 跳过登录请求
            if (methodName.contains("login") || methodName.contains("Login")) {
                return;
            }

            // 提取用户信息
            Long userId = null;
            String username = "anonymous";
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof Long) {
                        userId = (Long) principal;
                    }
                    username = auth.getName();
                }
            } catch (Exception ignored) {}

            // 推断操作类型
            String action = inferAction(methodName, className);

            // 提取操作目标
            String target = extractTarget(joinPoint.getArgs());

            // 提取请求信息
            String ipAddress = getClientIp();
            String httpMethod = getHttpMethod();
            String requestUri = getRequestUri();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .target(target)
                    .detail(buildDetail(joinPoint))
                    .ipAddress(ipAddress)
                    .httpMethod(httpMethod)
                    .requestUri(requestUri)
                    .result(result)
                    .failReason(failReason)
                    .elapsedMs(elapsedMs)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("审计日志保存失败: {}", e.getMessage());
        }
    }

    /**
     * 从方法名和类名推断操作类型
     */
    private String inferAction(String methodName, String className) {
        String lower = methodName.toLowerCase();
        if (lower.contains("recognize") || lower.contains("diagnos")) return "DIAGNOSIS";
        if (lower.contains("ask") || lower.contains("qa")) return "QA";
        if (lower.contains("control") || lower.contains("actuator")) return "CONTROL";
        if (lower.contains("upload") || lower.contains("import")) return "UPLOAD";
        if (lower.contains("delete") || lower.contains("remove")) return "DELETE";
        if (lower.contains("update") || lower.contains("modify")) return "UPDATE";
        if (lower.contains("create") || lower.contains("add") || lower.contains("register")) return "CREATE";
        if (lower.contains("scene") || lower.contains("execute")) return "CONTROL";
        if (lower.contains("index")) return "INDEX";
        return "UPDATE";
    }

    /**
     * 从方法参数中提取操作目标
     */
    private String extractTarget(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            // 尝试提取第一个参数的 ID 字段
            Object firstArg = args[0];
            if (firstArg instanceof Long) {
                return "id=" + firstArg;
            }
            // 尝试序列化参数对象（截断）
            String json = objectMapper.writeValueAsString(firstArg);
            if (json.length() > 200) {
                json = json.substring(0, 200) + "...";
            }
            return json;
        } catch (JsonProcessingException e) {
            return args[0].getClass().getSimpleName();
        }
    }

    /**
     * 构建操作详情
     */
    private String buildDetail(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";

            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getHttpMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getMethod() : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getRequestUri() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getRequestURI() : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
