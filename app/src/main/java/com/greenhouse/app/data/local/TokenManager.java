package com.greenhouse.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Token 管理器（安全加固版）
 * <p>
 * 使用 EncryptedSharedPreferences 存储 JWT Token 和用户敏感信息。
 * 数据在写入时自动 AES-256 加密，读取时自动解密。
 * 替代原有明文 SharedPreferences，防止 root 设备或恶意应用窃取 Token。
 * </p>
 *
 * <p>加密方案：
 * <ul>
 *   <li>主密钥：AES256-GCM（Android Keystore 硬件保护）</li>
 *   <li>Key 加密：AES256-SIV</li>
 *   <li>Value 加密：AES256-GCM</li>
 * </ul>
 * </p>
 */
public class TokenManager {

    private static final String PREF_NAME = "greenhouse_secure_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_REAL_NAME = "real_name";

    private static SharedPreferences prefs;

    /**
     * 初始化加密存储。必须在 Application.onCreate() 中调用。
     *
     * @param context 应用上下文
     * @throws RuntimeException 如果设备不支持加密或 Keystore 初始化失败
     */
    public static void init(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("无法初始化加密存储，设备可能不支持", e);
        }
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
     * 员工登录后通过 GET /worker/permissions 接口获取权限数据，
     * 调用此方法缓存到加密存储，供 RoleAdapter 使用。
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
