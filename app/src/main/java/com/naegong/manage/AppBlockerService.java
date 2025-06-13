package com.naegong.manage;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {

    private static final String PREF_NAME = "admin_settings";
    private static final String BLOCKED_APPS_KEY = "blocked_apps";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = String.valueOf(event.getPackageName());

            // ✅ 설정 앱 감지 시 PIN 인증 체크
            if (packageName.equals("com.android.settings")) {
                SharedPreferences pinPrefs = getSharedPreferences("pin_prefs", MODE_PRIVATE);
                long lastSuccessTime = pinPrefs.getLong("last_success_time", 0);
                long currentTime = System.currentTimeMillis();

                // ⏱️ 10분 이내는 통과
                if (currentTime - lastSuccessTime < 600_000) {
                    return;
                }

                // 🔒 인증 필요 → PIN 입력 화면 띄움
                Intent pinIntent = new Intent(this, PinLockActivity.class);
                pinIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(pinIntent);
                return;
            }

            // ✅ 차단 앱 목록 불러오기
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            Set<String> blockedApps = prefs.getStringSet(BLOCKED_APPS_KEY, null);

            // ⛔ blocked_apps가 없다면 기본 차단 리스트 적용
            if (blockedApps == null) {
                blockedApps = new HashSet<>(AppConfig.defaultBlockedApps);
                prefs.edit().putStringSet(BLOCKED_APPS_KEY, blockedApps).apply();
            }

            // 🚫 차단된 앱이면 강제 홈 이동
            if (blockedApps.contains(packageName)) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                Toast.makeText(this, "차단된 앱입니다: " + packageName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInterrupt() {
        // 필수 구현 – 무시해도 무방
    }
}
