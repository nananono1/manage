package com.naegong.manage;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.List;

public class NotificationBlockerService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        // ✅ 현재 context를 기반으로 허용 앱 목록을 가져옴
        List<String> allowedApps = AppConfig.getAllowedApps(this);

        if (allowedApps.contains(packageName)) {
            return;
        }

        // 나머지는 알림 제거
        cancelNotification(sbn.getKey());
    }
}
