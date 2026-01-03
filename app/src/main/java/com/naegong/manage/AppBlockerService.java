package com.naegong.manage; // 패키지 선언: 앱의 고유 네임스페이스

import android.accessibilityservice.AccessibilityService;                  // 접근성 서비스 상속을 위한 임포트
import android.content.Intent;                                             // 액티비티 실행 등 인텐트 사용
import android.content.SharedPreferences;                                  // SharedPreferences 접근
import android.os.SystemClock;                                             // 디바운스를 위한 시간 함수
import android.view.accessibility.AccessibilityEvent;                      // 접근성 이벤트 타입
import android.widget.Toast;                                               // 사용자 피드백 토스트

import java.util.HashSet;                                                  // 기본 차단 리스트 생성 시 사용
import java.util.Set;                                                      // 차단 앱 목록 자료구조
import java.util.concurrent.atomic.AtomicBoolean;                          // 스레드 안전 플래그(중복 실행 방지)

public class AppBlockerService extends AccessibilityService {              // 접근성 서비스 클래스 시작

    private static final String PREF_NAME = "admin_settings";              // 차단앱 SharedPreferences 이름 상수
    private static final String BLOCKED_APPS_KEY = "blocked_apps";         // 차단앱 키 상수

    // ───────────────────────────────────────────────────────────
    // 정책 상수(중앙화)
    // ───────────────────────────────────────────────────────────
    public static final long GRACE_MS = 180_000L;                          // ✅ 유예시간: 3분(180,000ms)로 설정
    private static final long SETTINGS_DEBOUNCE_MS = 1_000L;               // 설정앱 이벤트 디바운스: 1초
    private static final String PIN_PREFS = "pin_prefs";                   // PIN 성공 시각 저장소 이름
    private static final String KEY_LAST_SUCCESS = "last_success_time";    // PIN 성공 시각 키

    // ───────────────────────────────────────────────────────────
    // 중복 실행 방지 플래그/상태
    // ───────────────────────────────────────────────────────────
    private static final AtomicBoolean sPinShowing = new AtomicBoolean(false); // 현재 PIN 창 표시 중인지 여부(프로세스 전역)
    private long lastSettingsLaunchTs = 0L;                                     // 마지막 설정앱 감지 시각(디바운스용)
    private CharSequence lastClassName = null;                                  // 직전 클래스명(내부 화면 전환 중복 억제)

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {           // 접근성 이벤트 콜백
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return; // 다른 이벤트는 무시(창 전환만 관심)
        if (event.getPackageName() == null) return;                        // 패키지명이 없으면 무시

        String packageName = String.valueOf(event.getPackageName());       // 현재 포그라운드 패키지명 문자열화
        CharSequence className = event.getClassName();                     // 현재 화면 클래스명(서브액티비티 식별용)

        // ───────────────────────────────────────────────────────────
        // 설정앱 감지(벤더 다양성 고려)
        // ───────────────────────────────────────────────────────────
        boolean isSettings =
                "com.android.settings".equals(packageName)                 // AOSP 기본
                        || "com.google.android.settings".equals(packageName)       // 구글 변형
                        || packageName.endsWith(".settings");                      // 그 외 벤더 안전장치

        if (isSettings) {                                                  // 설정앱이 맞다면
            long now = SystemClock.elapsedRealtime();                      // 부팅 이후 경과시간(ms)
            if (now - lastSettingsLaunchTs < SETTINGS_DEBOUNCE_MS) return; // 1초 내 연속 이벤트는 무시(디바운스)
            if (lastClassName != null && lastClassName.equals(className)) return; // 같은 클래스 반복 전환도 무시
            lastSettingsLaunchTs = now;                                    // 최근 시각 업데이트
            lastClassName = className;                                     // 최근 클래스명 업데이트

            SharedPreferences pinPrefs = getSharedPreferences(PIN_PREFS, MODE_PRIVATE); // PIN 성공 시각 저장소
            long lastSuccessTime = pinPrefs.getLong(KEY_LAST_SUCCESS, 0L);              // 마지막 성공 시각(ms)
            long currentTime = System.currentTimeMillis();                               // 현재 UTC 시각(ms)

            if (currentTime - lastSuccessTime < GRACE_MS) {                 // ✅ 3분 유예 내라면
                return;                                                     // 그냥 통과(추가 PIN 요구 안 함)
            }

            if (sPinShowing.get()) return;                                  // 이미 PIN 창을 띄우는 중이면 재호출 금지

            sPinShowing.set(true);                                          // 이제부터 띄우는 중으로 마킹
            Intent pinIntent = new Intent(this, PinLockActivity.class);     // PIN 입력 액티비티 인텐트
            pinIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);              // 서비스에서 액티비티 실행이므로 NEW_TASK 필수
            startActivity(pinIntent);                                       // PIN 액티비티 실행
            return;                                                         // 설정앱 분기 처리 종료
        }
        // ✅ 설정앱에서 벗어난 순간, "같은 클래스 반복" 억제 상태를 리셋하여
        //    다음에 설정앱에 재진입할 때 PIN 화면이 정상적으로 다시 뜨도록 한다.
        lastClassName = null;                                              // 직전 설정 화면 클래스명 기록 초기화

        // ───────────────────────────────────────────────────────────
        // 차단 앱 정책 (설정앱 외의 경우)
        // ───────────────────────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE); // 차단앱 저장소 로드
        Set<String> blockedApps = prefs.getStringSet(BLOCKED_APPS_KEY, null);    // 차단 목록 조회

        if (blockedApps == null) {                                         // 차단 목록이 비어있다면
            blockedApps = new HashSet<>(AppConfig.defaultBlockedApps);     // 기본 차단 리스트로 초기화
            prefs.edit().putStringSet(BLOCKED_APPS_KEY, blockedApps).apply(); // 저장
        }

        if (blockedApps.contains(packageName)) {                           // 현재 앱이 차단 대상이면
            Intent intent = new Intent(Intent.ACTION_MAIN);                // 홈으로 이동하는 인텐트 생성
            intent.addCategory(Intent.CATEGORY_HOME);                      // 홈 카테고리 지정
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);                // 새로운 태스크로 실행
            startActivity(intent);                                         // 홈으로 이동(사실상 해당 앱 퇴출)
            Toast.makeText(this, "차단된 앱입니다: " + packageName, Toast.LENGTH_SHORT).show(); // 사용자 안내
        }
    }

    @Override
    public void onInterrupt() {                                            // 필수 오버라이드(특별한 처리 없음)
        // no-op
    }

    // ───────────────────────────────────────────────────────────
    // PinLockActivity 종료 시 호출하여 "표시 중" 상태를 해제
    // ───────────────────────────────────────────────────────────
    public static void notifyPinClosed() {                                 // 정적 메서드: 액티비티에서 호출
        sPinShowing.set(false);                                            // PIN 표시 중 플래그 해제
    }
}
