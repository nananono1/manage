package com.naegong.manage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 부팅 완료 시 동작
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // SharedPreferences에서 부트리시버 활성화 여부 확인
            SharedPreferences prefs = context.getSharedPreferences("admin_settings", Context.MODE_PRIVATE);
            boolean bootReceiverEnabled = prefs.getBoolean("boot_receiver_enabled", false);

            if (bootReceiverEnabled) {
                // 예시: 앱 차단 서비스 또는 필요한 초기화 작업 수행
                Intent serviceIntent = new Intent(context, AppBlockerService.class);
                serviceIntent.setAction("BOOT_START"); // 선택 사항
                context.startForegroundService(serviceIntent); // Android 8.0 이상에서 필요

                Toast.makeText(context, "Manage 앱이 부팅 후 자동 실행되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
