package com.greenhouse.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.greenhouse.app.R;
import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.api.GreenhouseApiService;
import com.greenhouse.app.data.model.ApiResponse;
import com.greenhouse.app.data.model.DeviceInfo;
import com.greenhouse.app.ui.common.MainActivity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * 离线/异常主动推送（F2）
 * <p>
 * 后台定时轮询（15 分钟）：设备离线数 + 未读告警数，状态变化时发系统通知。
 * 无 WorkManager 依赖，用 Service + Handler 实现（离线构建兼容）。
 * </p>
 */
public class MonitorService extends Service {

    private static final String TAG = "MonitorService";
    private static final String CHANNEL_ID = "greenhouse_monitor";
    private static final long POLL_INTERVAL_MS = 15 * 60 * 1000L; // 15 分钟

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            poll();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private int lastUnreadCount = -1;
    private int lastOfflineCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        handler.post(pollTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 前台服务在 Android 8+ 需要通知（降低优先级展示，避免保活复杂化）
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollTask);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "大棚监控提醒", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("设备离线与告警提醒");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void poll() {
        ApiClient.getExecutor().execute(() -> {
            try {
                long ghId = 1; // 默认大棚（演示）
                GreenhouseApiService api = ApiClient.getApiService();

                // 未读告警数
                int unread = -1;
                try {
                    Response<ApiResponse<Map<String, Object>>> r =
                            api.getUnreadAlertCount(ghId).execute();
                    if (r.isSuccessful() && r.body() != null && r.body().isSuccess()) {
                        Object data = r.body().getData();
                        if (data instanceof Number) {
                            unread = ((Number) data).intValue();
                        } else if (data instanceof Map) {
                            Object c = ((Map<?, ?>) data).get("count");
                            if (c instanceof Number) unread = ((Number) c).intValue();
                        }
                    }
                } catch (IOException ignored) {
                }

                // 离线设备数（控制器为关键设备）
                int offline = -1;
                try {
                    Response<ApiResponse<List<DeviceInfo>>> r =
                            api.getDevices(ghId, "CONTROLLER").execute();
                    if (r.isSuccessful() && r.body() != null && r.body().isSuccess()
                            && r.body().getData() != null) {
                        offline = 0;
                        for (DeviceInfo d : r.body().getData()) {
                            if (d.getStatus() != null && "OFFLINE".equalsIgnoreCase(d.getStatus())) {
                                offline++;
                            }
                        }
                    }
                } catch (IOException ignored) {
                }

                // 状态变化才通知
                if (unread >= 0 && lastUnreadCount >= 0 && unread > lastUnreadCount) {
                    notifyUser("新告警提醒", "有 " + unread + " 条未处理告警，请及时查看");
                }
                if (offline >= 0 && lastOfflineCount >= 0 && offline > lastOfflineCount) {
                    notifyUser("设备离线提醒", "有 " + offline + " 台设备离线，请检查连接");
                }
                if (unread >= 0) lastUnreadCount = unread;
                if (offline >= 0) lastOfflineCount = offline;
            } catch (Exception e) {
                Log.w(TAG, "轮询异常: " + e.getMessage());
            }
        });
    }

    private void notifyUser(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        getSystemService(NotificationManager.class).notify((int) System.currentTimeMillis(), notification);
    }
}
