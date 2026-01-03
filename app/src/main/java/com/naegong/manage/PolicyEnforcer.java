package com.naegong.manage; // 패키지 경로는 프로젝트와 동일하게

import android.app.admin.DevicePolicyManager;          // DPM 정책 적용용
import android.content.ComponentName;                  // Device Admin 컴포넌트
import android.content.Context;                        // 컨텍스트
import android.content.SharedPreferences;              // PIN 성공 시각 읽기
import android.util.Log;                               // 로깅

/**
 * Google 계정 추가/삭제 차단 정책을 현재 시간/유예시간에 맞춰 '강제 동기화'하는 유틸.
 * - 기본: 차단 ON (사용자 계정 추가/삭제 못함)
 * - PIN 성공 후 GRACE_MS(3분) 동안: 차단 OFF (추가/삭제 허용)
 * - GRACE_MS 지나면: 자동으로 다시 차단 ON
 */
public final class PolicyEnforcer {

    private static final String TAG = "PolicyEnforcer";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google"; // 구글 계정 타입
    private static final String PIN_PREFS = "pin_prefs";            // PIN 시간 저장 SP
    private static final String KEY_LAST_SUCCESS = "last_success_time"; // 마지막 PIN 성공 시각(ms)

    private PolicyEnforcer() { /* no-op */ }

    /**
     * 현재 시각과 마지막 PIN 성공 시각을 비교하여
     * - 유예시간 내면: 차단 해제(false)
     * - 유예시간 밖이면: 차단 적용(true)
     */
    public static void syncGoogleAccountPolicy(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE); // PIN 시간 저장소
            long last = sp.getLong(KEY_LAST_SUCCESS, 0L);                                         // 마지막 PIN 성공 시각
            long now = System.currentTimeMillis();                                                // 현재 시각(ms)

            // AppBlockerService의 상수를 그대로 사용(3분 = 180,000ms)
            long graceMs = AppBlockerService.GRACE_MS; // public static final long GRACE_MS = 180_000L;

            boolean withinGrace = (now - last) < graceMs; // 유예시간 내 여부
            // withinGrace == true  → 차단 해제(사용자 추가/삭제 허용)
            // withinGrace == false → 차단 적용(사용자 추가/삭제 금지)

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE); // DPM
            if (dpm == null) {
                Log.w(TAG, "DevicePolicyManager is null");
                return;
            }
            ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class); // DO 컴포넌트

            boolean disabled = !withinGrace; // disabled=true → 관리(추가/삭제) 금지, disabled=false → 허용
            dpm.setAccountManagementDisabled(admin, GOOGLE_ACCOUNT_TYPE, disabled);

            Log.d(TAG, "syncGoogleAccountPolicy → withinGrace=" + withinGrace + ", set disabled=" + disabled);
        } catch (Exception e) {
            Log.e(TAG, "syncGoogleAccountPolicy failed", e);
        }
    }

    /**
     * PIN 성공 직후 '즉시' 차단을 해제하고(=3분간 허용), 이후는 MainActivity 등에서 sync가 복귀시킴.
     * - 별도 저장은 PinLockActivity에서 이미 하고 있으므로 여기서는 DPM에 즉시 반영만 수행.
     */
    public static void allowTemporarily(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE); // DPM
            if (dpm == null) return;
            ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class); // DO 컴포넌트
            // disabled=false → 추가/삭제 허용
            dpm.setAccountManagementDisabled(admin, GOOGLE_ACCOUNT_TYPE, false);
            Log.d(TAG, "allowTemporarily → disabled=false (3분 유예 적용)");
        } catch (Exception e) {
            Log.e(TAG, "allowTemporarily failed", e);
        }
    }
}
