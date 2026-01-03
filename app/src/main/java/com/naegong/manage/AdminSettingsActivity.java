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

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminSettingsActivity extends AppCompatActivity {

    private static final String PREF_NAME = "admin_settings";
    private static final String BLOCKED_APPS_KEY = "blocked_apps";
    private static final String ALLOWED_UTILITY_APPS_KEY = "allowed_utility_apps";

    private String correctPin = "4711"; // ✅ 기본값 (RemoteConfig 로딩 전 대체용)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Remote Config 설정
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        // ✅ Remote Config fetch & 적용 → PIN 가져옴
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Remote Config 키 : admin_pin_code
                String fetchedPin = remoteConfig.getString("admin_pin_code");
                if (fetchedPin != null && !fetchedPin.isEmpty()) {
                    correctPin = fetchedPin;
                } else {
                    correctPin = "4711"; // ✅ RemoteConfig에 값이 없으면 fallback
                }
            } else {
                correctPin = "4711"; // fetch 실패 시 fallback
            }

            // ✅ PIN 다이얼로그 호출 (RemoteConfig 완료 후)
            showPinDialog();
        });
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
                    if (correctPin.equals(enteredPin)) {
                        loadAdminSettingsUI(); // ✅ 인증 성공 시 관리자 UI 표시
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

        // ⬇️ AdminSettingsActivity.java - loadAdminSettingsUI() 안의 resetButton 클릭 리스너를 다음으로 교체
        resetButton.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE); // DPM 가져오기
            ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);                      // DO 컴포넌트
            if (dpm == null) {
                Toast.makeText(this, "DevicePolicyManager를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) 초기화 대상 패키지 구성: 허용앱 전체 + 추가 후보(Provider/시계/캘린더 등)
            List<String> targetList = new ArrayList<>();
            targetList.addAll(AppConfig.getAllowedApps(this));     // 기존 허용 앱 전체
            targetList.addAll(AppConfig.extraResetCandidates);     // 우리가 추가한 후보들
            // 중복 제거
            Set<String> uniqueTargets = new LinkedHashSet<>(targetList);

            // 2) 초기화 실행 전 안내(운영 가이드): 동기화가 즉시 재주입하지 않도록 알림
            Toast.makeText(this, "초기화 실행 중… (캘린더 동기화가 켜져 있으면 데이터가 다시 채워질 수 있습니다)", Toast.LENGTH_LONG).show();

            // 3) 패키지 설치 여부 확인 후 초기화
            for (String pkg : uniqueTargets) {
                try {
                    // 설치 여부 사전 확인: 미설치면 스킵
                    getPackageManager().getPackageInfo(pkg, 0); // 없으면 예외 발생
                } catch (Exception notInstalled) {
                    // 미설치 패키지 로그만 찍고 넘어감
                    android.util.Log.w("Reset", "패키지 미설치, 스킵: " + pkg);
                    continue;
                }

                try {
                    // Device Owner 권한으로 사용자 데이터 초기화 요청
                    dpm.clearApplicationUserData(admin, pkg, getMainExecutor(), (pkgName, succeeded) -> {
                        String msg = (succeeded ? "초기화 완료: " : "초기화 실패: ") + pkgName;
                        android.util.Log.d("Reset", msg);
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    // 권한/보호 패키지/제조사 커스텀 제한 등으로 실패 가능
                    android.util.Log.e("Reset", "초기화 중 예외: " + pkg, e);
                    Toast.makeText(this, "오류: " + pkg, Toast.LENGTH_SHORT).show();
                }
            }

            // 4) 정책 일관성: 크롬/유튜브 차단·유틸리티 반영(기존 로직 유지)
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
                Toast.makeText(this, "크롬/유튜브 정책 갱신 완료", Toast.LENGTH_SHORT).show();

                // UI 갱신 브로드캐스트
                androidx.localbroadcastmanager.content.LocalBroadcastManager
                        .getInstance(this)
                        .sendBroadcast(new Intent("ACTION_REFRESH_APPS"));
            }
        });

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
