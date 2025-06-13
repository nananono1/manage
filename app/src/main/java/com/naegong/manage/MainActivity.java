package com.naegong.manage;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.accessibility.AccessibilityManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.naegong.manage.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.view.View;

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            binding.essentialGrid.removeAllViews();
            binding.learningGrid.removeAllViews();
            binding.utilityGrid.removeAllViews();
            populateAllowedApps();
        }
    };

    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        SharedPreferences adminPrefs = getSharedPreferences("admin_settings", MODE_PRIVATE);
        if (!adminPrefs.contains("blocked_apps")) {
            Set<String> defaultBlocked = new HashSet<>(AppConfig.defaultBlockedApps);
            adminPrefs.edit().putStringSet("blocked_apps", defaultBlocked).apply();
        }
        if (!adminPrefs.contains("allowed_utility_apps")) {
            Set<String> defaultUtilities = new HashSet<>(AppConfig.defaultUtilityApps);
            adminPrefs.edit().putStringSet("allowed_utility_apps", defaultUtilities).apply();
        }

        if (!isAccessibilityServiceEnabled(this, AppBlockerService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "접근성 권한을 활성화해주세요.", Toast.LENGTH_LONG).show();
        }

        requestNotificationListenerPermissionIfNeeded();
        checkAndSetDoNotDisturbPermission();
        populateAllowedApps();

        binding.settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminSettingsActivity.class);
            startActivity(intent);
        });
        RemoteConfigManager remoteConfigManager = new RemoteConfigManager();
        remoteConfigManager.fetchAndApplyRemoteConfig(getApplicationContext());

    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled()) return false;

        String expectedService = context.getPackageName() + "/" + service.getName();
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null || enabledServices.isEmpty()) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServices);
        for (String enabledService : colonSplitter) {
            if (enabledService.equalsIgnoreCase(expectedService)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private void requestNotificationListenerPermissionIfNeeded() {
        if (!isNotificationListenerEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "알림 접근 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
        }
    }

    private void checkAndSetDoNotDisturbPermission() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                NotificationManager.Policy policy = new NotificationManager.Policy(
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                        NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY
                );
                notificationManager.setNotificationPolicy(policy);
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            }
        }
    }

    private void clearAllowedAppsUserData() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);

        for (String packageName : AppConfig.getAllowedApps(this)) {
            try {
                dpm.clearApplicationUserData(
                        admin,
                        packageName,
                        getMainExecutor(),
                        (pkgName, succeeded) -> {
                            String msg = pkgName + (succeeded ? " 초기화 완료" : " 초기화 실패");
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                );
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "오류: " + packageName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void populateAllowedApps() {
        PackageManager pm = getPackageManager();
        SharedPreferences prefs = getSharedPreferences("admin_settings", MODE_PRIVATE);
        Set<String> blockedApps = prefs.getStringSet("blocked_apps", new HashSet<>());

        for (String packageName : AppConfig.getAllowedApps(this)) {
            if (blockedApps.contains(packageName)) continue;

            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                Drawable icon = pm.getApplicationIcon(appInfo);
                String label = pm.getApplicationLabel(appInfo).toString();

                LinearLayout appItem = new LinearLayout(this);
                appItem.setOrientation(LinearLayout.VERTICAL);
                appItem.setGravity(Gravity.CENTER);
                appItem.setPadding(16, 16, 16, 16);

                ImageView iconView = new ImageView(this);
                int iconSize = (int) (getResources().getDisplayMetrics().density * 72);
                iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
                iconView.setImageDrawable(icon);

                TextView labelView = new TextView(this);
                labelView.setText(label);
                labelView.setTextSize(12);
                labelView.setTextColor(Color.WHITE);
                labelView.setGravity(Gravity.CENTER);

                appItem.addView(iconView);
                appItem.addView(labelView);

                appItem.setOnClickListener(v -> {
                    Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                });

                if (AppConfig.allowedEssentialApps.contains(packageName)) {
                    binding.essentialGrid.addView(appItem);
                } else if (AppConfig.allowedLearningApps.contains(packageName)) {
                    binding.learningGrid.addView(appItem);
                } else if (AppConfig.getAllowedUtilityApps(this).contains(packageName)) {
                    binding.utilityGrid.addView(appItem);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

//    private void installSubAppFromHosting() {
//        new Thread(() -> {
//            try {
//                URL url = new URL("ht tps://naegong-student-qgbwcp.web.app/subappV2.apk");
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.connect();
//
//                File apkFile = new File(getFilesDir(), "subappV2.apk");
//
//                try (InputStream in = conn.getInputStream();
//                     OutputStream out = new FileOutputStream(apkFile)) {
//                    byte[] buffer = new byte[4096];
//                    int len;
//                    while ((len = in.read(buffer)) != -1) {
//                        out.write(buffer, 0, len);
//                    }
//                }
//
//                runOnUiThread(() -> {
//                    try {
//                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
//                        InputStream is = getAssets().open("wallpaper.jpg");
//                        Bitmap bitmap = BitmapFactory.decodeStream(is);
//                        wallpaperManager.setBitmap(bitmap);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Toast.makeText(this, "배경화면 설정 실패", Toast.LENGTH_SHORT).show();
//                    }
//
//                    try {
//                        Uri apkUri = FileProvider.getUriForFile(
//                                this,
//                                getPackageName() + ".provider",
//                                apkFile
//                        );
//
//                        Intent intent = new Intent(Intent.ACTION_VIEW);
//                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        startActivity(intent);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Toast.makeText(this, "앱 설치 유도 실패", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() ->
//                        Toast.makeText(this, "APK 다운로드 또는 설치 실패", Toast.LENGTH_LONG).show()
//                );
//            }
//        }).start();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(refreshReceiver, new IntentFilter("ACTION_REFRESH_APPS"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshReceiver);
    }

    @Override
    public void onBackPressed() {
        // 아무 동작도 하지 않음
    }
}
