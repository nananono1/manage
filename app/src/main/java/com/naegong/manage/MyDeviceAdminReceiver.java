package com.naegong.manage;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * 디바이스 관리자 권한을 부여받기 위한 기본 리시버
 */
public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    // 관리자 권한이 활성화되었을 때 호출됨
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "✅ Device Admin 권한이 활성화되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 관리자 권한이 비활성화되었을 때 호출됨
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "❌ Device Admin 권한이 비활성화되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
