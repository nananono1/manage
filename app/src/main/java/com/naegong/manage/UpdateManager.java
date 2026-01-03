package com.naegong.manage;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String SUBAPP_PACKAGE = "com.naegongstudy.app";

    public static void checkSubAppUpdate(Context context, String remoteVersion, String apkUrl) {
        String installedVersion = getInstalledSubAppVersion(context);

        if (installedVersion == null || !installedVersion.equals(remoteVersion)) {
            Log.d(TAG, "📥 업데이트 필요 → 다운로드 및 설치 시작");
            downloadAndInstallApk(context, apkUrl);
        } else {
            Toast.makeText(context, "이미 최신 버전입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private static String getInstalledSubAppVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(SUBAPP_PACKAGE, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static void downloadAndInstallApk(Context context, String apkUrl) {
        try {
            String fileName = Uri.parse(apkUrl).getLastPathSegment();
            File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (apkFile.exists()) apkFile.delete();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("업데이트 다운로드 중");
            request.setDescription("최신 버전 설치 파일을 받고 있습니다...");
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        context.unregisterReceiver(this);
                        installApkWithInstaller(ctx, apkFile);
                    }
                }
            };

            context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            Log.e(TAG, "❌ APK 다운로드 실패", e);
            Toast.makeText(context, "APK 다운로드 실패", Toast.LENGTH_LONG).show();
        }
    }

    // ✅ PackageInstaller 기반 설치
    private static void installApkWithInstaller(Context context, File apkFile) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "설치 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            try (FileInputStream in = new FileInputStream(apkFile);
                 OutputStream out = session.openWrite("subapp_session", 0, -1)) {
                byte[] buffer = new byte[65536];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
            }

            // ✅ 설치 결과를 수신할 리시버 설정
            Intent intent = new Intent(context, InstallResultReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            session.commit(pendingIntent.getIntentSender());
            session.close();

        } catch (Exception e) {
            Log.e(TAG, "❌ PackageInstaller 설치 실패", e);
            Toast.makeText(context, "설치 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
