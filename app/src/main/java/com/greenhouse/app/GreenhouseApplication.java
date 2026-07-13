package com.greenhouse.app;

import android.app.Application;

import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.local.TokenManager;

/**
 * 智慧大棚 Application
 * <p>
 * 全局初始化：网络客户端、Token管理器等
 * </p>
 */
public class GreenhouseApplication extends Application {

    private static GreenhouseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化 Token 管理器
        TokenManager.init(this);

        // 初始化 API 客户端
        ApiClient.init();
    }

    public static GreenhouseApplication getInstance() {
        return instance;
    }
}
