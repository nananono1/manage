package com.naegong.manage;                                               // 패키지 선언

import android.app.Activity;                                               // 액티비티 기반
import android.content.Intent;                                             // 홈 이동/인텐트
import android.content.SharedPreferences;                                   // PIN 성공 시각 저장
import android.os.Bundle;                                                  // 생명주기 번들
import android.text.InputType;                                             // 입력 타입 상수
import android.view.Gravity;                                               // 다이얼로그 위치
import android.view.MotionEvent;                                           // ✅ 바깥 터치/외부 이벤트 차단
import android.view.WindowManager;                                         // 창 속성
import android.widget.Button;                                              // 버튼 위젯
import android.widget.EditText;                                            // 입력 위젯
import android.widget.LinearLayout;                                        // 레이아웃
import android.widget.Toast;                                               // 토스트

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;              // Remote Config
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;       // RC 설정

public class PinLockActivity extends Activity {                            // PIN 입력 액티비티 시작

    private static final String PIN_PREFS = "pin_prefs";                   // PIN 성공 시각 저장소 이름 상수
    private static final String KEY_LAST_SUCCESS = "last_success_time";    // PIN 성공 시각 키 상수

    private boolean pinVerified = false;                                   // ✅ PIN 성공 여부(바깥 터치/강제 종료 시 우회 방지)

    private void goHomeAndFinish() {                                       // ✅ 홈으로 보내고 종료(설정 우회 차단)
        Intent intent = new Intent(Intent.ACTION_MAIN);                    // 홈 이동 인텐트
        intent.addCategory(Intent.CATEGORY_HOME);                          // 홈 카테고리
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);                    // 새로운 태스크
        startActivity(intent);                                             // 홈으로 이동
        finish();                                                          // 현재 액티비티 종료
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {                   // onCreate: 최초 생성 시 호출
        super.onCreate(savedInstanceState);                                // 부모 초기화

        setFinishOnTouchOutside(false);                                    // ✅ 박스(윈도우) 바깥 터치로 액티비티가 닫히는 버그 차단

        // ✅ Remote Config 인스턴스 획득
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance(); // RC 인스턴스

        // ✅ Remote Config 설정(최소 fetch 간격)
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder() // 빌더 시작
                .setMinimumFetchIntervalInSeconds(3600)                    // 최소 1시간 간격으로 fetch
                .build();                                                  // 설정 객체 생성
        remoteConfig.setConfigSettingsAsync(configSettings);               // 비동기 적용

        // 🔄 Remote Config fetch & activate 후 UI 구성
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {    // fetch+activate 완료 리스너
            String correctPin = remoteConfig.getString("admin_pin_code");  // 서버에서 PIN 가져오기
            if (correctPin == null || correctPin.isEmpty()) {              // 값이 없을 경우
                correctPin = "4711";                                       // 기본 PIN(로컬 안전망)
            }

            // 🔲 다이얼로그 느낌의 창 배치(가운데, 키보드 자동)
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, // 너비: 화면 가득
                    WindowManager.LayoutParams.WRAP_CONTENT); // 높이: 내용만
            getWindow().setGravity(Gravity.CENTER);                         // 가운데 정렬
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE); // 키보드 자동

            // 🧱 수직 레이아웃 구성
            LinearLayout layout = new LinearLayout(this);                   // 루트 레이아웃
            layout.setOrientation(LinearLayout.VERTICAL);                   // 세로 방향
            layout.setPadding(50, 50, 50, 50);                              // 패딩
            layout.setGravity(Gravity.CENTER_HORIZONTAL);                   // 수평 중앙 정렬

            // 🔐 PIN 입력창 구성
            EditText pinInput = new EditText(this);                         // 입력 필드 생성
            pinInput.setHint("설정변경을 원할 경우 관리자에게 문의해 주세요"); // 힌트 문구
            pinInput.setInputType(InputType.TYPE_CLASS_NUMBER               // 숫자 키패드
                    | InputType.TYPE_NUMBER_VARIATION_PASSWORD);            // 입력값 숨김 처리
            pinInput.setTextSize(24);                                       // 글자 크기
            pinInput.setPadding(30, 30, 30, 30);                            // 내부 패딩
            layout.addView(pinInput);                                       // 레이아웃에 추가
            pinInput.requestFocus();                                        // 포커스 요청

            // ✅ 확인 버튼 구성
            Button submitButton = new Button(this);                         // 버튼 생성
            submitButton.setText("확인");                                    // 라벨
            layout.addView(submitButton);                                   // 레이아웃에 추가

            setContentView(layout);                                         // 화면에 레이아웃 적용

            // 🔐 PIN 확인 로직
            String finalCorrectPin = correctPin;                            // 로컬 상수로 캡처
            submitButton.setOnClickListener(v -> {                          // 클릭 리스너
                String typed = pinInput.getText().toString();               // 입력값 문자열
                if (typed.equals(finalCorrectPin)) {                        // PIN 일치 시
                    SharedPreferences prefs = getSharedPreferences(PIN_PREFS, MODE_PRIVATE); // 성공 시각 저장소
                    prefs.edit().putLong(KEY_LAST_SUCCESS, System.currentTimeMillis()).apply(); // ✅ 지금 시각 저장
                    // PIN 인증 성공한 경우
                    PolicyEnforcer.allowTemporarily(this);

                    Toast.makeText(this, "3분간 잠금이 해제됩니다.", Toast.LENGTH_SHORT).show(); // ✅ 사용자 안내 토스트

                    pinVerified = true;                                    // ✅ 성공 플래그(이후 onStop/onDestroy에서 홈 강제 이동하지 않게)
                    finish();                                               // 액티비티 종료(설정앱 진입 허용)
                } else {                                                    // PIN 불일치 시
                    Toast.makeText(this, "잘못된 PIN입니다", Toast.LENGTH_SHORT).show(); // 경고 토스트

                    goHomeAndFinish();                                     // ✅ 즉시 홈 이동(설정 우회 차단)
                }
            });
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {                        // ✅ 바깥 터치로 닫히거나 뒤로 전달되는 이벤트 방지
        if (event != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) { // 바깥 터치 이벤트라면
            return true;                                                    // 이벤트 소비(아무 동작 없음)
        }
        return super.onTouchEvent(event);                                   // 기본 처리
    }

    @Override
    public void onBackPressed() {                                          // 뒤로가기 차단 + 홈 이동
        goHomeAndFinish();                                                 // ✅ 홈으로 보내고 종료(설정 우회 차단)
    }

    @Override
    protected void onStop() {                                               // ✅ 화면이 사라질 때(바깥 터치/시스템 행동 등)
        super.onStop();                                                     // 부모 호출

        if (!pinVerified) {                                                 // PIN 성공이 아닌 상태로 사라지면
            goHomeAndFinish();                                              // ✅ 설정앱에 남아있지 못하도록 홈으로 강제
        }
    }

    @Override
    protected void onDestroy() {                                           // 액티비티 파괴 시
        super.onDestroy();                                                 // 부모 호출
        AppBlockerService.notifyPinClosed();                               // ✅ 서비스 측 "표시 중" 플래그 해제
    }
}
