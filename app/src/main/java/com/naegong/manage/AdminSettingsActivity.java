package com.naegong.manage;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class AdminSettingsActivity extends AppCompatActivity {

    private static final String CORRECT_PIN = "0810" +
            ""; // 실제 운영 시 외부 저장으로 관리 권장
    private static final String PREF_NAME = "admin_settings";
    private static final String BLOCKED_APPS_KEY = "blocked_apps";
    private static final String ALLOWED_UTILITY_APPS_KEY = "allowed_utility_apps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPinDialog(); // PIN 입력 후 UI 로드
    }

    private void showPinDialog() {
        EditText input = new EditText(this);
        input.setHint("PIN 입력");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("관리자 인증")
                .setMessage("설정에 접근하려면 PIN을 입력하세요.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("확인", (dialog, which) -> {
                    String enteredPin = input.getText().toString().trim();
                    if (CORRECT_PIN.equals(enteredPin)) {
                        loadAdminSettingsUI(); // 인증 성공 시 관리자 UI 표시
                    } else {
                        Toast.makeText(this, "잘못된 PIN입니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> finish())
                .show();
    }

    private void loadAdminSettingsUI() {
        setContentView(R.layout.activity_admin);

        Button chromeButton = findViewById(R.id.buttonEnableChrome);
        Button youtubeButton = findViewById(R.id.buttonEnableYouTube);
        Button resetButton = findViewById(R.id.buttonResetData);

        chromeButton.setOnClickListener(v -> toggleBlockedApp("com.android.chrome"));
        youtubeButton.setOnClickListener(v -> toggleBlockedApp("com.google.android.youtube"));

        resetButton.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);

            for (String pkg : AppConfig.getAllowedApps(this)) {
                try {
                    dpm.clearApplicationUserData(admin, pkg, getMainExecutor(), (pkgName, succeeded) -> {
                        String msg = pkgName + (succeeded ? " 초기화 완료" : " 초기화 실패");
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "오류: " + pkg, Toast.LENGTH_SHORT).show();
                }
            }

            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            Set<String> blockedApps = new HashSet<>(prefs.getStringSet(BLOCKED_APPS_KEY, new HashSet<>()));
            Set<String> allowedUtilityApps = new HashSet<>(prefs.getStringSet(ALLOWED_UTILITY_APPS_KEY, new HashSet<>(AppConfig.getDefaultUtilityApps())));
            boolean updated = false;

            if (!blockedApps.contains("com.android.chrome")) {
                blockedApps.add("com.android.chrome");
                updated = true;
            }
            if (!blockedApps.contains("com.google.android.youtube")) {
                blockedApps.add("com.google.android.youtube");
                updated = true;
            }
            if (!allowedUtilityApps.contains("com.android.chrome")) {
                allowedUtilityApps.add("com.android.chrome");
                updated = true;
            }
            if (!allowedUtilityApps.contains("com.google.android.youtube")) {
                allowedUtilityApps.add("com.google.android.youtube");
                updated = true;
            }

            if (updated) {
                prefs.edit()
                        .putStringSet(BLOCKED_APPS_KEY, blockedApps)
                        .putStringSet(ALLOWED_UTILITY_APPS_KEY, allowedUtilityApps)
                        .apply();
                Toast.makeText(this, "크롬/유튜브가 차단 및 유틸리티 목록에 추가되었습니다.", Toast.LENGTH_SHORT).show();

                // ✅ UI 갱신 브로드캐스트 전송
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("ACTION_REFRESH_APPS"));
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (!prefs.contains(BLOCKED_APPS_KEY)) {
            prefs.edit().putStringSet(BLOCKED_APPS_KEY, new HashSet<>(AppConfig.defaultBlockedApps)).apply();
        }
    }

    private void toggleBlockedApp(String packageName) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        Set<String> blockedApps = new HashSet<>(prefs.getStringSet(BLOCKED_APPS_KEY, new HashSet<>()));
        Set<String> allowedUtilityApps = new HashSet<>(prefs.getStringSet(ALLOWED_UTILITY_APPS_KEY, new HashSet<>(AppConfig.getDefaultUtilityApps())));

        boolean isBlocked = blockedApps.contains(packageName);
        String message;

        if (isBlocked) {
            blockedApps.remove(packageName);
            allowedUtilityApps.add(packageName); // 차단 해제 → 유틸리티로 추가
            message = packageName + " 허용됨";
        } else {
            blockedApps.add(packageName);
            allowedUtilityApps.remove(packageName); // 차단 시 유틸리티에서 제거
            message = packageName + " 차단됨";
        }

        prefs.edit()
                .putStringSet(BLOCKED_APPS_KEY, blockedApps)
                .putStringSet(ALLOWED_UTILITY_APPS_KEY, allowedUtilityApps)
                .apply();

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // ✅ UI 갱신 브로드캐스트 전송
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                new Intent("ACTION_REFRESH_APPS")
        );
    }

}