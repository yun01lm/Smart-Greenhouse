package com.greenhouse.security.annotations;

import java.lang.annotation.*;

/**
 * 大棚访问权限注解
 * <p>
 * 标注在 Controller 方法上，AOP 切面自动校验当前用户是否有该大棚的访问权限。
 * 步骤7（C18多角色权限模块）实现AOP切面。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code @RequireGreenhouseAccess}
 * {@code @GetMapping("/api/v1/greenhouses/{greenhouseId}/sensors")}
 * public ApiResponse<?> getSensors(@PathVariable Long greenhouseId) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireGreenhouseAccess {
}
