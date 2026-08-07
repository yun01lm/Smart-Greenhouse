package com.greenhouse.app.util;

import com.greenhouse.app.data.local.TokenManager;

/**
 * 角色适配工具类 (F11)
 * <p>
 * 根据登录用户的角色（OWNER / WORKER）提供功能可见性和权限判断。
 * APP 端仅存在 OWNER 和 WORKER 两种角色（无 ADMIN / EXPERT）。
 * </p>
 *
 * <h3>权限模型</h3>
 * <ul>
 *   <li>OWNER（棚主）：拥有全部功能，无需限制</li>
 *   <li>WORKER（员工）：功能可见性由棚主分配的 6 项权限控制</li>
 * </ul>
 *
 * <h3>员工权限位</h3>
 * <table>
 *   <tr><td>can_view_data</td><td>查看实时/历史数据</td></tr>
 *   <tr><td>can_control_device</td><td>控制设备/执行场景</td></tr>
 *   <tr><td>can_diagnose</td><td>病虫害诊断</td></tr>
 *   <tr><td>can_ask_expert</td><td>专家咨询</td></tr>
 *   <tr><td>can_view_alerts</td><td>查看预警</td></tr>
 *   <tr><td>can_view_history</td><td>查看历史数据</td></tr>
 * </table>
 */
public class RoleAdapter {

    // ===== 角色常量 =====

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_WORKER = "WORKER";
    public static final String ROLE_TECHNICIAN = "TECHNICIAN";

    // 员工权限 Key（与后端 employee_permissions 表字段对应）
    public static final String PERM_VIEW_DATA = "can_view_data";
    public static final String PERM_CONTROL_DEVICE = "can_control_device";
    public static final String PERM_DIAGNOSE = "can_diagnose";
    public static final String PERM_ASK_EXPERT = "can_ask_expert";
    public static final String PERM_VIEW_ALERTS = "can_view_alerts";
    public static final String PERM_VIEW_HISTORY = "can_view_history";

    // ===== 角色判断 =====

    /**
     * 当前用户是否为棚主
     */
    public static boolean isOwner() {
        return ROLE_OWNER.equals(TokenManager.getRole());
    }

    /**
     * 当前用户是否为普通员工
     */
    public static boolean isWorker() {
        return ROLE_WORKER.equals(TokenManager.getRole());
    }

    /**
     * 当前用户是否为技术员（R23：默认拥有全部权限，可被棚主收紧）
     */
    public static boolean isTechnician() {
        return ROLE_TECHNICIAN.equals(TokenManager.getRole());
    }

    /**
     * 获取当前角色
     */
    public static String getCurrentRole() {
        return TokenManager.getRole();
    }

    // ===== 权限判断 =====

    /**
     * 判断当前用户是否拥有某项权限。
     * <p>
     * 棚主默认拥有所有权限；员工根据 SharedPreferences 中缓存的权限位判断。
     * 权限数据由员工登录后通过 GET /worker/permissions 接口获取并缓存。
     * </p>
     *
     * @param permission 权限 Key（使用本类中的 PERM_xxx 常量）
     * @return true 表示有权限
     */
    public static boolean hasPermission(String permission) {
        // 棚主/技术员默认拥有所有权限（技术员权限可被棚主收紧，后端强制校验）
        if (isOwner() || isTechnician()) return true;

        // 员工：检查缓存的权限位
        return TokenManager.getBoolean(permission, false);
    }

    /**
     * 是否有查看数据权限
     */
    public static boolean canViewData() {
        return hasPermission(PERM_VIEW_DATA);
    }

    /**
     * 是否有控制设备权限
     */
    public static boolean canControlDevice() {
        return hasPermission(PERM_CONTROL_DEVICE);
    }

    /**
     * 是否有诊断权限
     */
    public static boolean canDiagnose() {
        return hasPermission(PERM_DIAGNOSE);
    }

    /**
     * 是否有专家咨询权限
     */
    public static boolean canAskExpert() {
        return hasPermission(PERM_ASK_EXPERT);
    }

    /**
     * 是否有查看预警权限
     */
    public static boolean canViewAlerts() {
        return hasPermission(PERM_VIEW_ALERTS);
    }

    /**
     * 是否有查看历史数据权限
     */
    public static boolean canViewHistory() {
        return hasPermission(PERM_VIEW_HISTORY);
    }

    // ===== 功能可见性 =====

    /**
     * AI 助手 Tab 是否可见
     * <p>
     * AI 助手包含诊断和问答两个子功能，任一有权限即可见。
     * </p>
     */
    public static boolean isAiAssistantVisible() {
        return canDiagnose() || canAskExpert();
    }

    /**
     * 设备控制 Tab 是否可见
     */
    public static boolean isControlVisible() {
        return canControlDevice();
    }

    /**
     * 员工管理入口是否可见（仅棚主）
     */
    public static boolean isEmployeeManagementVisible() {
        return isOwner();
    }
}
