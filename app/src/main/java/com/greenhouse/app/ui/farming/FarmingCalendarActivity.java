package com.greenhouse.app.ui.farming;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.databinding.ActivityFarmingCalendarBinding;
import com.greenhouse.app.data.model.CropCycleData;
import com.greenhouse.app.data.repository.GrowthRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 农事日历（F3）
 * <p>
 * 展示作物生长周期（定植/阶段/天数），支持添加本地农事提醒（AlarmManager 当天 8 点通知）。
 * </p>
 */
public class FarmingCalendarActivity extends AppCompatActivity {

    private static final String PREFS = "farming_reminders";
    private static final String KEY_REMINDERS = "reminders";
    public static final String EXTRA_REMINDER_TEXT = "reminder_text";
    public static final String ACTION_REMINDER = "com.greenhouse.app.ACTION_FARMING_REMINDER";

    private ActivityFarmingCalendarBinding binding;
    private final List<String> reminders = new ArrayList<>();
    private final List<CropCycleData> cycles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFarmingCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadReminders();
        renderReminders();

        binding.btnAddReminder.setOnClickListener(v -> showAddDialog());
        loadCropCycles();
    }

    private void loadCropCycles() {
        long ghId = getIntent().getLongExtra("greenhouse_id", 1);
        new GrowthRepository().getCropCycles(ghId, new com.greenhouse.app.data.repository.BaseRepository.Callback<List<CropCycleData>>() {
            @Override
            public void onSuccess(List<CropCycleData> data) {
                cycles.clear();
                if (data != null) cycles.addAll(data);
                runOnUiThread(FarmingCalendarActivity.this::renderCycles);
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(FarmingCalendarActivity.this,
                        "加载作物周期失败: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderCycles() {
        StringBuilder sb = new StringBuilder();
        if (cycles.isEmpty()) {
            sb.append("暂无作物周期数据\n（在 Web 端维护定植信息后自动显示）");
        } else {
            for (CropCycleData c : cycles) {
                sb.append("▪ ").append(c.getCropType() == null ? "未知作物" : c.getCropType())
                        .append(c.getVariety() == null ? "" : "（" + c.getVariety() + "）")
                        .append("\n    定植: ").append(c.getPlantingDate() == null ? "--" : c.getPlantingDate())
                        .append("  ·  阶段: ").append(c.getCurrentStage() == null ? "--" : c.getCurrentStage())
                        .append("  ·  已生长 ").append(c.getDaysSincePlanting()).append(" 天\n");
            }
        }
        binding.tvCycles.setText(sb.toString());
    }

    private void loadReminders() {
        reminders.clear();
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_REMINDERS, "");
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split("\\|\\|")) {
                if (!s.isEmpty()) reminders.add(s);
            }
        }
    }

    private void renderReminders() {
        StringBuilder sb = new StringBuilder();
        if (reminders.isEmpty()) {
            sb.append("暂无提醒\n（点击下方按钮添加浇水/施肥/打药提醒）");
        } else {
            for (String r : reminders) sb.append("▪ ").append(r).append("\n");
        }
        binding.tvReminders.setText(sb.toString());
    }

    private void showAddDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("例如：8:00 给番茄浇水");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("添加农事提醒")
                .setView(et)
                .setPositiveButton("添加", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "请输入提醒内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String today = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
                            .format(new java.util.Date());
                    String entry = today + " " + text;
                    reminders.add(entry);
                    saveReminders();
                    renderReminders();
                    scheduleReminder(text);
                    Toast.makeText(this, "已添加，明日 8:00 提醒", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveReminders() {
        String joined = String.join("||", reminders);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_REMINDERS, joined).apply();
    }

    /** 明天的 8:00 发一条提醒通知 */
    private void scheduleReminder(String text) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class)
                .setAction(ACTION_REMINDER)
                .putExtra(EXTRA_REMINDER_TEXT, text);
        PendingIntent pi = PendingIntent.getBroadcast(this, text.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        c.set(java.util.Calendar.HOUR_OF_DAY, 8);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
