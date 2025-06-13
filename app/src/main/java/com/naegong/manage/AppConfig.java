package com.naegong.manage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.*;

public class AppConfig {

    public static final String PREF_NAME = "admin_settings";
    public static final String BLOCKED_APPS_KEY = "blocked_apps";
    public static final String UTILITY_APPS_KEY = "allowed_utility_apps";

    // 고정 허용 앱 (필수/학습용)
    public static final List<String> allowedEssentialApps = Arrays.asList(
            "com.samsung.android.calendar",
            "com.sec.android.app.clockpackage",
            "com.nhn.android.naverdic"
    );

    public static final List<String> allowedLearningApps = Arrays.asList(
            "com.ds.mimacstudy.smartnplayer",
            "com.etoos.etoosstudyapp",
            "net.megastudy.smartplay.main",
            "kr.co.ebs.middle",
            "com.coden.android.ebs",
            "com.cdn.aquanmanager"
    );

    // ✅ 디폴트 유틸리티 앱
    public static final List<String> defaultUtilityApps = Arrays.asList(
            "com.naegongstudy.app"
    );

    // ✅ 기본 차단 앱
    public static final Set<String> defaultBlockedApps = new HashSet<>(Arrays.asList(
            "com.google.android.youtube",
            "com.android.chrome",
            "com.instagram.android",
            "com.kakao.talk",
            "com.sec.android.app.sbrowser"
    ));

    // ✅ SharedPreferences가격 저장된 유틸리티 앱 목록 가져오기
    public static List<String> getAllowedUtilityApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSet = prefs.getStringSet(UTILITY_APPS_KEY, new HashSet<>(defaultUtilityApps));
        return new ArrayList<>(savedSet);
    }
    public static List<String> getDefaultUtilityApps() {
        return new ArrayList<>(defaultUtilityApps);
    }

    // ✅ 최종 허용 앱 리스트 반환
    public static List<String> getAllowedApps(Context context) {
        List<String> utility = getAllowedUtilityApps(context);
        List<String> all = new ArrayList<>();
        all.addAll(allowedEssentialApps);
        all.addAll(allowedLearningApps);
        all.addAll(utility);
        return all;
    }
    // ✅ RemoteConfig 차단 앱 저장 (쉼표로 전달됨)
    public static void setBlockedApps(Context context, String csvApps) {
        Set<String> blockedSet = new HashSet<>(Arrays.asList(csvApps.split(",")));
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(BLOCKED_APPS_KEY, blockedSet).apply();
    }

    // ✅ 최종 차단 앱 반환 (기본 + RemoteConfig)
    public static Set<String> getBlockedApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(BLOCKED_APPS_KEY, new HashSet<>());
        Set<String> all = new HashSet<>(defaultBlockedApps);
        all.addAll(saved);
        return all;
    }

    // ✅ RemoteConfig 버전 정보 저장
    public static void setLatestVersion(Context context, String version) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("latest_version", version).apply();
    }

    public static String getLatestVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString("latest_version", "1.0.0"); // 기본값
    }

    // ✅ RemoteConfig APK 다운로드 URL 저장
    public static void setApkUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("apk_url", url).apply();
    }

    public static String getApkUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString("apk_url", "");
    }
}