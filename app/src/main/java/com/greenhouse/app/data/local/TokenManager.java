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

    // ===== 清除 =====

    public static void clear() {
        prefs.edit().clear().apply();
    }
}
