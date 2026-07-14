package com.greenhouse.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Token 管理器
 * <p>
 * 使用 SharedPreferences 存储 JWT Token 和用户基本信息。
 * 登录成功时保存，退出登录时清除。
 * </p>
 */
public class TokenManager {

    private static final String PREF_NAME = "greenhouse_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_REAL_NAME = "real_name";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===== Token =====

    public static void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public static boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    // ===== 用户信息 =====

    public static void saveUserInfo(long userId, String username, String role, String realName) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .putString(KEY_REAL_NAME, realName)
                .apply();
    }

    public static long getUserId() {
        return prefs.getLong(KEY_USER_ID, 0);
    }

    public static String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public static String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public static String getRealName() {
        return prefs.getString(KEY_REAL_NAME, "");
    }

    // ===== 员工权限位缓存（F11 角色适配） =====

    /**
     * 缓存员工权限位。
     * <p>
     * 员工登录后通过 GET /worker/permissions 接口获取权限数据，
     * 调用此方法缓存到 SharedPreferences，供 RoleAdapter 使用。
     * </p>
     *
     * @param canViewData      是否可查看数据
     * @param canControlDevice 是否可控制设备
     * @param canDiagnose      是否可诊断
     * @param canAskExpert     是否可咨询专家
     * @param canViewAlerts    是否可查看预警
     * @param canViewHistory   是否可查看历史
     */
    public static void savePermissions(boolean canViewData, boolean canControlDevice,
                                       boolean canDiagnose, boolean canAskExpert,
                                       boolean canViewAlerts, boolean canViewHistory) {
        prefs.edit()
                .putBoolean("can_view_data", canViewData)
                .putBoolean("can_control_device", canControlDevice)
                .putBoolean("can_diagnose", canDiagnose)
                .putBoolean("can_ask_expert", canAskExpert)
                .putBoolean("can_view_alerts", canViewAlerts)
                .putBoolean("can_view_history", canViewHistory)
                .apply();
    }

    /**
     * 读取缓存的布尔值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // ===== 清除 =====

    public static void clear() {
        prefs.edit().clear().apply();
    }
}
