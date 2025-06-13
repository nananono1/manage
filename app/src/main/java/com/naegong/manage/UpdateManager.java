package com.naegong.manage;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String SUBAPP_PACKAGE = "com.naegongstudy.app";

    // ✅ 1. 서브앱 설치 여부 및 버전 체크
    public static void checkSubAppUpdate(Context context, String remoteVersion, String apkUrl) {
        String installedVersion = getInstalledSubAppVersion(context);

        if (installedVersion == null) {
            Log.d(TAG, "서브앱 미설치됨 → 설치 유도");
            downloadAndInstallApk(context, apkUrl);
        } else if (!installedVersion.equals(remoteVersion)) {
            Log.d(TAG, "서브앱 버전 불일치: " + installedVersion + " → " + remoteVersion);
            downloadAndInstallApk(context, apkUrl);
        } else {
            Log.d(TAG, "서브앱 최신 버전 유지 중: " + installedVersion);
        }
    }

    // ✅ 2. 설치된 서브앱 버전 확인
    private static String getInstalledSubAppVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(SUBAPP_PACKAGE, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null; // 설치 안 되어 있음
        }
    }

    // ✅ 3. APK 다운로드 및 설치 트리거
    private static void downloadAndInstallApk(Context context, String apkUrl) {
        try {
            // 🔹 3-1. URL에서 파일명 자동 추출
            String fileName = Uri.parse(apkUrl).getLastPathSegment(); // 예: subApp_v1.2.0.apk

            // 🔹 3-2. 저장 경로 설정
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (file.exists()) file.delete(); // 기존 파일 제거

            // 🔹 3-3. 다운로드 요청 설정
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("서브앱 업데이트 중");
            request.setDescription("최신 버전 설치 파일을 다운로드합니다.");
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // 🔹 3-4. 다운로드 시작
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            Log.d(TAG, "✅ APK 다운로드 시작됨: " + fileName);

            // 설치는 별도 BroadcastReceiver를 쓰거나, 딜레이 후 시도 가능

        } catch (Exception e) {
            Log.e(TAG, "❌ APK 다운로드 실패", e);
        }
    }

    // ✅ 4. APK 설치 유도 함수 (다운로드 후 수동 또는 자동 호출)
    public static void installApk(Context context, File apkFile) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0) {
                Log.e(TAG, "❌ 설치할 파일이 존재하지 않음");
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "❌ 설치 인텐트를 실행할 수 없습니다", e);
        }
    }
}