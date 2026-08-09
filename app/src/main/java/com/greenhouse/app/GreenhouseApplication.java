package com.greenhouse.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.local.TokenManager;
import com.greenhouse.app.ui.login.LoginActivity;

/**
 * ???? Application
 * <p>
 * ????????????Token????
 * </p>
 */
public class GreenhouseApplication extends Application {

    private static GreenhouseApplication instance;

    /** ???? Activity????????? */
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // ??? Token ???
        TokenManager.init(this);

        // ??? API ???
        ApiClient.init();

        // ?????????Token ???????????????
        ApiClient.setSessionExpiredListener(() -> {
            TokenManager.clear();
            Activity top = currentActivity;
            if (top != null && !top.isFinishing()) {
                top.runOnUiThread(() -> {
                    Intent intent = new Intent(top, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    top.startActivity(intent);
                });
            }
        });

        // ?????? Activity??????????
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override
            public void onActivityStarted(Activity activity) { }
            @Override
            public void onActivityResumed(Activity activity) { currentActivity = activity; }
            @Override
            public void onActivityPaused(Activity activity) { }
            @Override
            public void onActivityStopped(Activity activity) { }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override
            public void onActivityDestroyed(Activity activity) {
                if (currentActivity == activity) currentActivity = null;
            }
        });
    }

    public static GreenhouseApplication getInstance() {
        return instance;
    }
}
