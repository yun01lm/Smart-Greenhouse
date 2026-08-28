package com.greenhouse.app.ui.farming;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.greenhouse.app.R;
import com.greenhouse.app.ui.common.MainActivity;

/**
 * 农事提醒通知接收器（F3）：AlarmManager 触发后发系统通知
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "greenhouse_farming";

    @Override
    public void onReceive(Context context, Intent intent) {
        String text = intent.getStringExtra(FarmingCalendarActivity.EXTRA_REMINDER_TEXT);
        if (text == null) text = "农事提醒";

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "农事提醒", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_growth)
                .setContentTitle("农事提醒")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify(text.hashCode(), builder.build());
    }
}
