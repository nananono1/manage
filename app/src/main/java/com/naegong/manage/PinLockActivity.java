package com.naegong.manage;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class PinLockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Remote Config 설정 및 fetch
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        // 최소 fetch 간격 0초 (개발용)
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        // 🔄 Remote Config fetch → UI 구성 실행
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            // 서버에서 가져온 PIN 코드 (없으면 기본값)
            String correctPin = remoteConfig.getString("admin_pin_code");
            if (correctPin == null || correctPin.isEmpty()) {
                correctPin = "1234";
            }

            // 🔲 다이얼로그 느낌의 창 설정
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            getWindow().setGravity(Gravity.CENTER);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            // 🧱 레이아웃 구성
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 50, 50, 50);
            layout.setGravity(Gravity.CENTER_HORIZONTAL);

            // 🔐 PIN 입력창
            EditText pinInput = new EditText(this);
            pinInput.setHint("설정변경을 원할 경우 관리자에게 문의해 주세요");
            pinInput.setInputType(0x00000012); // TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD
            pinInput.setTextSize(24);
            pinInput.setPadding(30, 30, 30, 30);
            layout.addView(pinInput);
            pinInput.requestFocus();

            // ✅ 확인 버튼
            Button submitButton = new Button(this);
            submitButton.setText("확인");
            layout.addView(submitButton);

            setContentView(layout);

            // 🔐 PIN 확인 로직
            String finalCorrectPin = correctPin;
            submitButton.setOnClickListener(v -> {
                if (pinInput.getText().toString().equals(finalCorrectPin)) {
                    // ✅ PIN 일치: SharedPreferences에 현재 시간 저장
                    SharedPreferences prefs = getSharedPreferences("pin_prefs", MODE_PRIVATE);
                    prefs.edit().putLong("last_success_time", System.currentTimeMillis()).apply();

                    finish(); // 설정 앱 진입 허용
                } else {
                    Toast.makeText(this, "잘못된 PIN입니다", Toast.LENGTH_SHORT).show();

                    // ❌ PIN 틀림 → 홈으로 이동
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    // ⛔ 뒤로 가기 눌러도 설정 앱 강제 종료
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
