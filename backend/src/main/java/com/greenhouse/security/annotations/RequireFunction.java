package com.greenhouse.security.annotations;

import java.lang.annotation.*;

/**
 * 功能权限注解
 * <p>
 * 标注在 Controller 方法上，AOP 切面自动校验当前用户是否有该功能的操作权限。
 * 主要用于员工权限控制（棚主可配置员工能使用哪些功能）。
 * 步骤7（C18多角色权限模块）实现AOP切面。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code @RequireFunction("CONTROL_DEVICE")}
 * {@code @PostMapping("/api/v1/control/actuator")}
 * public ApiResponse<?> controlDevice(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFunction {

    /** 功能标识：VIEW_DATA / CONTROL_DEVICE / DIAGNOSE / ASK_EXPERT / VIEW_ALERTS / VIEW_HISTORY */
    String value();
}
