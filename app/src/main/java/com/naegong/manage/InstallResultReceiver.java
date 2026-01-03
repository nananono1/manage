package com.naegong.manage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class InstallResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra("android.content.pm.extra.STATUS", -1);
        String msg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");

        if (status == 0) {
            Toast.makeText(context, "앱 설치가 완료되었습니다.", Toast.LENGTH_SHORT).show();
            Log.d("InstallReceiver", "✅ 설치 성공");

            // ✅ 설치 완료 알림을 MainActivity에 전송
            Intent updateIntent = new Intent("com.naegong.ACTION_INSTALL_SUCCESS");
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);

        } else {
            Toast.makeText(context, "설치 실패: " + msg, Toast.LENGTH_LONG).show();
            Log.e("InstallReceiver", "❌ 설치 실패: " + msg);
        }
    }
}
