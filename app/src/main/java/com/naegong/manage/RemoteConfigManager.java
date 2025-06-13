package com.naegong.manage;

import android.content.Context;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class RemoteConfigManager {

    private static final String TAG = "RemoteConfigManager";

    // Remote Config 키
    private static final String KEY_LATEST_VERSION_SUBAPP = "latest_version_subapp";
    private static final String KEY_APK_URL_SUBAPP = "apk_url_subapp";

    private final FirebaseRemoteConfig remoteConfig;

    public RemoteConfigManager() {
        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 개발 중에는 0초 (운영 시엔 3600)
                .build();

        remoteConfig.setConfigSettingsAsync(configSettings);
    }

    public void fetchAndApplyRemoteConfig(Context context) {
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "✅ Remote Config fetch 성공");

                // 버전 및 URL 가져오기
                String latestVersion = remoteConfig.getString(KEY_LATEST_VERSION_SUBAPP);
                String apkUrl = remoteConfig.getString(KEY_APK_URL_SUBAPP);

                Log.d(TAG, "📦 latest_version_subapp = " + latestVersion);
                Log.d(TAG, "📎 apk_url_subapp = " + apkUrl);

                // 자동 업데이트 트리거
                UpdateManager.checkSubAppUpdate(context, latestVersion, apkUrl);

            } else {
                Log.e(TAG, "❌ Remote Config fetch 실패", task.getException());
            }
        });
    }
}
