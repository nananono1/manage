package com.naegong.manage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockAppListActivity extends AppCompatActivity {

    private static final String PREF_NAME = "admin_settings";
    private static final String BLOCKED_APPS_KEY = "blocked_apps";

    private ListView listView;
    private AppAdapter adapter;
    private List<AppItem> appList;
    private Set<String> blockedPackages;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 간단한 리스트뷰 레이아웃 생성 (XML 없이 코드 레벨에서 처리 가능)
        listView = new ListView(this);
        listView.setBackgroundColor(Color.WHITE);
        setContentView(listView);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        blockedPackages = new HashSet<>(prefs.getStringSet(BLOCKED_APPS_KEY, new HashSet<>()));

        // 설치된 앱 로딩
        loadInstalledApps();
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        appList = new ArrayList<>();

        for (ApplicationInfo appInfo : packages) {
            // 시스템 앱 제외하고 싶은 경우 아래 주석 해제 (단, 크롬/유튜브는 시스템 앱일 수 있으므로 주의)
            // if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            // 자기 자신(Manage 앱)은 리스트에서 제외
            if (appInfo.packageName.equals(getPackageName())) continue;

            AppItem item = new AppItem();
            item.packageName = appInfo.packageName;
            item.label = pm.getApplicationLabel(appInfo).toString();
            item.icon = pm.getApplicationIcon(appInfo);
            item.isBlocked = blockedPackages.contains(item.packageName);
            appList.add(item);
        }

        // 이름순 정렬
        Collections.sort(appList, (o1, o2) -> o1.label.compareTo(o2.label));

        adapter = new AppAdapter(this, appList);
        listView.setAdapter(adapter);
    }

    // 변경사항 저장 및 UI 갱신 요청
    private void updateBlockedState(String packageName, boolean isBlocked) {
        if (isBlocked) {
            blockedPackages.add(packageName);
        } else {
            blockedPackages.remove(packageName);
        }

        prefs.edit().putStringSet(BLOCKED_APPS_KEY, blockedPackages).apply();

        // 메인 화면(홈 그리드) 갱신 알림
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("ACTION_REFRESH_APPS"));
    }

    // --- 내부 클래스: 데이터 모델 ---
    private static class AppItem {
        String label;
        String packageName;
        Drawable icon;
        boolean isBlocked;
    }

    // --- 내부 클래스: 어댑터 ---
    private class AppAdapter extends ArrayAdapter<AppItem> {
        public AppAdapter(Context context, List<AppItem> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.activity_list_item, parent, false);
            }

            AppItem item = getItem(position);

            // 기본 리스트 아이템 뷰 활용
            ImageView iconView = convertView.findViewById(android.R.id.icon);
            TextView titleView = convertView.findViewById(android.R.id.text1);

            // 체크박스 기능을 위해 커스텀 레이아웃을 쓰는 게 좋지만,
            // 여기서는 text1에 상태를 표시하거나, 클릭 시 토글되도록 구현
            iconView.setImageDrawable(item.icon);
            titleView.setText(item.label + (item.isBlocked ? " [⛔ 차단됨]" : ""));
            titleView.setTextColor(item.isBlocked ? Color.RED : Color.BLACK);

            convertView.setOnClickListener(v -> {
                item.isBlocked = !item.isBlocked;
                updateBlockedState(item.packageName, item.isBlocked);
                notifyDataSetChanged(); // 리스트 UI 갱신

                String msg = item.isBlocked ? "차단됨: " : "해제됨: ";
                Toast.makeText(BlockAppListActivity.this, msg + item.label, Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }
}