package com.naegong.manage;  // helpers 없이!

import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.InputStream;

public class SetupHelper {
    public static void initSetup(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) {
                Log.w("SetupHelper", "Device Owner 아님 - 잠금화면 설정 생략");
                return;
            }

            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            InputStream inputStream = context.getAssets().open("wallpaper.jpg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setStream(inputStream, null, true, WallpaperManager.FLAG_LOCK);
                Log.i("SetupHelper", "잠금화면 배경 설정 완료");
            } else {
                Log.w("SetupHelper", "Android 7.0(API 24)+ 필요");
            }

            inputStream.close();
        } catch (Exception e) {
            Log.e("SetupHelper", "잠금화면 설정 실패", e);
        }
    }
}
