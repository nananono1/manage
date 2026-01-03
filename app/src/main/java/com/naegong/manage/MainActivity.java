package com.naegong.manage; // ✅ 패키지 선언

import android.app.NotificationManager; // ✅ DND 정책 적용에 필요
import android.app.usage.UsageStats; // ✅ 포그라운드 앱 확인용
import android.app.usage.UsageStatsManager; // ✅ 포그라운드 앱 확인용
import android.content.BroadcastReceiver; // ✅ 브로드캐스트 수신기
import android.content.Context; // ✅ Context
import android.content.Intent; // ✅ Intent
import android.content.IntentFilter; // ✅ IntentFilter
import android.content.SharedPreferences; // ✅ SharedPreferences
import android.content.pm.ApplicationInfo; // ✅ 앱 정보 조회
import android.content.pm.PackageInfo; // ✅ 설치된 앱 버전 조회
import android.content.pm.PackageManager; // ✅ 패키지 매니저
import android.graphics.Color; // ✅ UI 색상
import android.graphics.drawable.Drawable; // ✅ 앱 아이콘
import android.os.Build; // ✅ API 33 분기(registerReceiver)
import android.os.Bundle; // ✅ Activity 생명주기
import android.provider.Settings; // ✅ 설정 화면 이동
import android.text.TextUtils; // ✅ 문자열 유틸
import android.view.Gravity; // ✅ 레이아웃 정렬
import android.view.View; // ✅ View
import android.view.accessibility.AccessibilityManager; // ✅ 접근성 서비스 체크
import android.widget.ImageView; // ✅ 앱 아이콘 뷰
import android.widget.LinearLayout; // ✅ 앱 버튼 컨테이너
import android.widget.TextView; // ✅ 앱 라벨/상태 텍스트
import android.widget.Toast; // ✅ 토스트

import androidx.appcompat.app.AlertDialog; // ✅ 업데이트 다이얼로그
import androidx.appcompat.app.AppCompatActivity; // ✅ Activity
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // ✅ 로컬 브로드캐스트

import com.google.firebase.remoteconfig.FirebaseRemoteConfig; // ✅ RemoteConfig
import com.naegong.manage.databinding.ActivityMainBinding; // ✅ ViewBinding

import java.util.HashSet; // ✅ Set 기본값
import java.util.List; // ✅ 리스트
import java.util.Set; // ✅ 셋
import java.util.SortedMap; // ✅ 정렬 맵
import java.util.TreeMap; // ✅ 정렬 맵 구현

public class MainActivity extends AppCompatActivity { // ✅ 메인 런처 액티비티

    private ActivityMainBinding binding; // ✅ 바인딩

    // =====================================================
    // ✅ (추가) 외부 앱(com.naegongstudy.app) → Manage 학생정보 전달 수신(서명권한 보호)
    // =====================================================

    private static final String ACTION_STUDENT_INFO = "com.naegong.ACTION_STUDENT_INFO"; // ✅ 발신 앱 action
    private static final String PERM_SEND_STUDENT_INFO = "com.naegong.permission.SEND_STUDENT_INFO"; // ✅ signature permission(Manage manifest에 선언)

    private static final String PREFS_TABLET_DISPLAY = "tablet_display"; // ✅ 수신값 저장 prefs
    private static final String KEY_DISPLAY_NAME = "display_name"; // ✅ 학생명 저장키
    private static final String KEY_SEAT_NO = "seatNo"; // ✅ 좌석번호 저장키
    private static final String KEY_SPOT = "spot"; // ✅ 지점 저장키
    private static final String KEY_SCHOOL = "school"; // ✅ 학교 저장키
    private static final String KEY_SCHEDULE_NOW_JSON = "scheduleNowJson"; // ✅ scheduleNow JSON 저장키

    private static final String EXTRA_DISPLAY_NAME = "display_name"; // ✅ intent extra(학생명)
    private static final String EXTRA_SEAT_NO = "seatNo"; // ✅ intent extra(좌석)
    private static final String EXTRA_SPOT = "spot"; // ✅ intent extra(지점)
    private static final String EXTRA_SCHOOL = "school"; // ✅ intent extra(학교)
    private static final String EXTRA_SCHEDULE_NOW_JSON = "scheduleNowJson"; // ✅ intent extra(scheduleNow JSON)

    private static final String STUDENT_MARKER = "\n\n[STUDENT_INFO]\n"; // ✅ base텍스트와 학생정보 분리 마커

    // ✅ 학생정보 수신기(시스템 브로드캐스트)
    private final BroadcastReceiver studentInfoReceiver = new BroadcastReceiver() { // ✅ 수신기
        @Override
        public void onReceive(Context context, Intent intent) { // ✅ 수신 콜백
            if (intent == null) return; // ✅ null 방지
            if (!ACTION_STUDENT_INFO.equals(intent.getAction())) return; // ✅ action 다르면 무시

            String displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME); // ✅ 학생명
            String seatNo = intent.getStringExtra(EXTRA_SEAT_NO); // ✅ 좌석
            String spot = intent.getStringExtra(EXTRA_SPOT); // ✅ 지점
            String school = intent.getStringExtra(EXTRA_SCHOOL); // ✅ 학교
            String scheduleNowJson = intent.getStringExtra(EXTRA_SCHEDULE_NOW_JSON); // ✅ scheduleNow JSON

            saveStudentInfoToPrefs(displayName, seatNo, spot, school, scheduleNowJson); // ✅ 저장
            renderStudentInfoFromPrefs(); // ✅ 화면 반영
        }
    };

    // ✅ (추가) 학생정보 prefs 저장
    private void saveStudentInfoToPrefs(String displayName, String seatNo, String spot, String school, String scheduleNowJson) { // ✅ 저장 함수
        SharedPreferences prefs = getSharedPreferences(PREFS_TABLET_DISPLAY, MODE_PRIVATE); // ✅ prefs 열기
        prefs.edit() // ✅ 편집 시작
                .putString(KEY_DISPLAY_NAME, displayName != null ? displayName : "") // ✅ null 방지
                .putString(KEY_SEAT_NO, seatNo != null ? seatNo : "") // ✅ null 방지
                .putString(KEY_SPOT, spot != null ? spot : "") // ✅ null 방지
                .putString(KEY_SCHOOL, school != null ? school : "") // ✅ null 방지
                .putString(KEY_SCHEDULE_NOW_JSON, scheduleNowJson != null ? scheduleNowJson : "") // ✅ null 방지
                .apply(); // ✅ 저장
    }

    // ✅ (추가) base 상태 텍스트를 갱신하되, 학생정보가 이미 있으면 유지
    private void setBaseStatusTextPreservingStudent(String baseText) { // ✅ base 갱신 함수
        if (binding == null || binding.versionStatusText == null) return; // ✅ 바인딩 체크
        binding.versionStatusText.setVisibility(View.VISIBLE); // ✅ 보이기

        String existing = binding.versionStatusText.getText() != null ? binding.versionStatusText.getText().toString() : ""; // ✅ 기존 텍스트
        String studentPart = ""; // ✅ 학생 파트

        int idx = existing.indexOf(STUDENT_MARKER); // ✅ 마커 위치
        if (idx >= 0) { // ✅ 학생정보가 붙어있다면
            studentPart = existing.substring(idx); // ✅ 마커부터 끝까지(학생정보) 보존
        }

        String merged = (baseText != null ? baseText : "") + studentPart; // ✅ base + (기존 학생정보)
        binding.versionStatusText.setText(merged); // ✅ 적용
    }

    // ✅ (추가) prefs에서 학생정보를 읽어 “학생파트만” 업데이트(버전 메시지 덮어쓰기 방지)
    private void renderStudentInfoFromPrefs() { // ✅ 학생정보 표시 함수
        SharedPreferences prefs = getSharedPreferences(PREFS_TABLET_DISPLAY, MODE_PRIVATE); // ✅ prefs 열기

        String displayName = prefs.getString(KEY_DISPLAY_NAME, ""); // ✅ 학생명
        String seatNo = prefs.getString(KEY_SEAT_NO, ""); // ✅ 좌석
        String spot = prefs.getString(KEY_SPOT, ""); // ✅ 지점
        String school = prefs.getString(KEY_SCHOOL, ""); // ✅ 학교
        String scheduleNowJson = prefs.getString(KEY_SCHEDULE_NOW_JSON, ""); // ✅ scheduleNow JSON

        String hasSchedule = (scheduleNowJson != null && !scheduleNowJson.isEmpty()) ? "O" : "X"; // ✅ scheduleNow 유무

        String studentLine = "학생: " + (displayName.isEmpty() ? "(미지정)" : displayName) // ✅ 학생명 라인
                + " | 좌석: " + (seatNo.isEmpty() ? "(미지정)" : seatNo) // ✅ 좌석 라인
                + " | 지점: " + (spot.isEmpty() ? "(미지정)" : spot) // ✅ 지점 라인
                + " | 학교: " + (school.isEmpty() ? "(미지정)" : school) // ✅ 학교 라인
                + " | scheduleNow: " + hasSchedule; // ✅ scheduleNow 라인

        if (binding == null || binding.versionStatusText == null) return; // ✅ 바인딩 체크
        binding.versionStatusText.setVisibility(View.VISIBLE); // ✅ 보이기

        String existing = binding.versionStatusText.getText() != null ? binding.versionStatusText.getText().toString() : ""; // ✅ 기존 텍스트
        String base = existing; // ✅ base(버전/업데이트) 영역

        int idx = existing.indexOf(STUDENT_MARKER); // ✅ 마커 위치
        if (idx >= 0) { // ✅ 이미 학생정보가 붙어있으면
            base = existing.substring(0, idx); // ✅ base만 분리
        }

        String merged = base + STUDENT_MARKER + studentLine; // ✅ base + 학생파트(항상 최신)
        binding.versionStatusText.setText(merged); // ✅ 적용
    }

    // =====================================================
    // ✅ 기존 기능: 관리자 설정 변경 시 홈 그리드 갱신 수신기
    // =====================================================

    private final BroadcastReceiver refreshAppsReceiver = new BroadcastReceiver() { // ✅ 기존 리시버
        @Override
        public void onReceive(Context context, Intent intent) { // ✅ 수신
            if (binding != null) { // ✅ 바인딩 체크
                binding.essentialGrid.removeAllViews(); // ✅ essential 초기화
                binding.learningGrid.removeAllViews(); // ✅ learning 초기화
                binding.utilityGrid.removeAllViews(); // ✅ utility 초기화
            }
            populateAllowedApps(); // ✅ 다시 채우기
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) { // ✅ onCreate
        super.onCreate(savedInstanceState); // ✅ 상위 호출
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // ✅ 바인딩 inflate
        setContentView(binding.getRoot()); // ✅ 뷰 세팅

        // 시스템 UI 설정(기존)
        getWindow().setStatusBarColor(Color.TRANSPARENT); // ✅ 상태바 투명
        getWindow().setNavigationBarColor(Color.TRANSPARENT); // ✅ 네비바 투명
        getWindow().getDecorView().setSystemUiVisibility( // ✅ 레이아웃 플래그
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | // ✅ 안정
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | // ✅ 풀스크린 레이아웃
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // ✅ 네비게이션 숨김 레이아웃
        );

        // SharedPreferences 초기 설정(기존)
        SharedPreferences adminPrefs = getSharedPreferences("admin_settings", MODE_PRIVATE); // ✅ admin settings
        if (!adminPrefs.contains("blocked_apps")) { // ✅ 기본값 없으면
            Set<String> defaultBlocked = new HashSet<>(AppConfig.defaultBlockedApps); // ✅ 기본 차단 앱
            adminPrefs.edit().putStringSet("blocked_apps", defaultBlocked).apply(); // ✅ 저장
        }
        if (!adminPrefs.contains("allowed_utility_apps")) { // ✅ 기본값 없으면
            Set<String> defaultUtilities = new HashSet<>(AppConfig.defaultUtilityApps); // ✅ 기본 유틸앱
            adminPrefs.edit().putStringSet("allowed_utility_apps", defaultUtilities).apply(); // ✅ 저장
        }

        // ✅ 항상 com.naegongstudy.app이 유틸리티 앱에 포함되도록 보장(기존)
        Set<String> currentUtilities = new HashSet<>(adminPrefs.getStringSet("allowed_utility_apps", new HashSet<>())); // ✅ 현재 유틸앱
        if (!currentUtilities.contains("com.naegongstudy.app")) { // ✅ 없으면
            currentUtilities.add("com.naegongstudy.app"); // ✅ 추가
            adminPrefs.edit().putStringSet("allowed_utility_apps", currentUtilities).apply(); // ✅ 저장
        }

        // ✅ 잠금화면 배경 설정 (최초 1회만)(기존)
        SharedPreferences prefs = getSharedPreferences("setup_prefs", MODE_PRIVATE); // ✅ setup prefs
        if (!prefs.getBoolean("wallpaperSet", false)) { // ✅ 최초 1회
            SetupHelper.initSetup(this); // ✅ 잠금화면 배경 설정(프로젝트에 실제 존재)
            prefs.edit().putBoolean("wallpaperSet", true).apply(); // ✅ 완료 표시
        }

        // 접근성 권한 확인(기존)
        if (!isAccessibilityServiceEnabled(this, AppBlockerService.class)) { // ✅ 접근성 서비스 체크
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS); // ✅ 접근성 설정으로 이동
            startActivity(intent); // ✅ 실행
            Toast.makeText(this, "접근성 권한을 활성화해주세요.", Toast.LENGTH_LONG).show(); // ✅ 안내
        }

        // 알림 권한 및 DND 설정 확인(기존)
        requestNotificationListenerPermissionIfNeeded(); // ✅ 알림 리스너 권한 유도
        checkAndSetDoNotDisturbPermission(); // ✅ DND 정책 적용

        // 앱 목록 표시(기존)
        populateAllowedApps(); // ✅ 홈 아이콘 구성

        // 설정 버튼(기존)
        binding.settingsButton.setOnClickListener(v -> { // ✅ 클릭 리스너
            Intent intent = new Intent(MainActivity.this, AdminSettingsActivity.class); // ✅ 관리자 설정 화면
            startActivity(intent); // ✅ 실행
        });

        // 업데이트 확인 버튼(기존: RemoteConfig 키/UpdateManager.checkSubAppUpdate 유지)
        binding.checkUpdateButton.setOnClickListener(v -> { // ✅ 클릭 리스너
            FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance(); // ✅ RemoteConfig
            remoteConfig.fetchAndActivate().addOnCompleteListener(task -> { // ✅ fetch+activate
                if (task.isSuccessful()) { // ✅ 성공
                    String remoteVersion = remoteConfig.getString("latest_version_subapp"); // ✅ 최신 버전(서브앱)
                    String apkUrl = remoteConfig.getString("apk_url_subapp"); // ✅ APK URL(서브앱)
                    String localVersion = getInstalledSubAppVersion(this); // ✅ 현재 설치 버전

                    String message = "현재 버전: " + (localVersion != null ? localVersion : "미설치됨") + "\n최신 버전: " + remoteVersion; // ✅ base 메시지
                    setBaseStatusTextPreservingStudent(message); // ✅ base 갱신(학생정보 보존)

                    if (localVersion == null || !localVersion.equals(remoteVersion)) { // ✅ 업데이트 필요
                        new AlertDialog.Builder(this) // ✅ 다이얼로그
                                .setTitle("업데이트 가능") // ✅ 제목
                                .setMessage("새로운 버전이 있습니다.\n업데이트 하시겠습니까?") // ✅ 메시지
                                .setPositiveButton("예", (dialog, which) -> { // ✅ 예
                                    UpdateManager.checkSubAppUpdate(this, remoteVersion, apkUrl); // ✅ 프로젝트에 실제 존재하는 메서드
                                })
                                .setNegativeButton("아니요", null) // ✅ 아니요
                                .show(); // ✅ 표시
                    } else { // ✅ 최신
                        Toast.makeText(this, "최신 버전입니다.", Toast.LENGTH_SHORT).show(); // ✅ 안내
                    }
                } else { // ✅ 실패
                    Toast.makeText(this, "버전 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show(); // ✅ 안내
                }
            });
        });

        // ✅ 설치 완료 브로드캐스트 수신기 등록(기존)
        LocalBroadcastManager.getInstance(this).registerReceiver( // ✅ 등록
                installSuccessReceiver, // ✅ 리시버
                new IntentFilter("com.naegong.ACTION_INSTALL_SUCCESS") // ✅ 필터
        );

        // ✅ 관리자 설정 변경 시 그리드 갱신(기존)
        LocalBroadcastManager.getInstance(this).registerReceiver( // ✅ 등록
                refreshAppsReceiver, // ✅ 리시버
                new IntentFilter("ACTION_REFRESH_APPS") // ✅ 필터
        );

        // =====================================================
        // ✅ (추가) 학생정보 브로드캐스트 수신 등록(signature permission 강제)
        // =====================================================

        IntentFilter studentFilter = new IntentFilter(ACTION_STUDENT_INFO); // ✅ 학생정보 필터

        if (Build.VERSION.SDK_INT >= 33) { // ✅ Android 13(API 33)+
            registerReceiver( // ✅ 시스템 브로드캐스트 등록
                    studentInfoReceiver, // ✅ 수신기
                    studentFilter, // ✅ 필터
                    PERM_SEND_STUDENT_INFO, // ✅ signature permission 요구
                    null, // ✅ handler
                    Context.RECEIVER_EXPORTED // ✅ 외부 앱 브로드캐스트 수신 허용
            );
        } else { // ✅ Android 12 이하(당신 9~11 포함)
            registerReceiver( // ✅ 구버전 시그니처 사용
                    studentInfoReceiver, // ✅ 수신기
                    studentFilter, // ✅ 필터
                    PERM_SEND_STUDENT_INFO, // ✅ signature permission 요구
                    null // ✅ handler
            );
        }

        renderStudentInfoFromPrefs(); // ✅ 앱 시작 시: 마지막 학생정보 표시(있다면)

        PolicyEnforcer.syncGoogleAccountPolicy(this); // ✅ 기존: 계정 정책 동기화
    }

    // ✅ 특정 앱 포그라운드에 따라 orientation 강제 변경(기존)
    @Override
    protected void onResume() { // ✅ onResume
        super.onResume(); // ✅ 상위 호출
        PolicyEnforcer.syncGoogleAccountPolicy(this); // ✅ 기존
        String foregroundApp = getForegroundAppPackageName(); // ✅ 포그라운드 앱 확인

        if ("com.naegongstudy.app".equals(foregroundApp)) { // ✅ 학생앱이 포그라운드면
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // ✅ 앱이 원하는 방향 허용
        } else { // ✅ 그 외에는
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // ✅ 가로 고정
        }
    }

    // ✅ 현재 포그라운드 앱 패키지명 가져오기(기존)
    private String getForegroundAppPackageName() { // ✅ 포그라운드 앱 가져오기
        String currentApp = "unknown"; // ✅ 기본값
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) { // ✅ L+에서만 가능
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE); // ✅ 매니저
            long time = System.currentTimeMillis(); // ✅ 현재 시간
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time); // ✅ 최근 10초
            if (appList != null && !appList.isEmpty()) { // ✅ 데이터 존재
                SortedMap<Long, UsageStats> sortedMap = new TreeMap<>(); // ✅ 정렬 맵
                for (UsageStats usageStats : appList) { // ✅ 순회
                    sortedMap.put(usageStats.getLastTimeUsed(), usageStats); // ✅ lastUsed 기준 정렬
                }
                if (!sortedMap.isEmpty()) { // ✅ 비어있지 않으면
                    currentApp = sortedMap.get(sortedMap.lastKey()).getPackageName(); // ✅ 가장 최근 앱
                }
            }
        }
        return currentApp; // ✅ 반환
    }

    // ✅ 설치 완료 후 UI 업데이트 수신기(기존 + 학생정보 보존 적용)
    private final BroadcastReceiver installSuccessReceiver = new BroadcastReceiver() { // ✅ 리시버
        @Override
        public void onReceive(Context context, Intent intent) { // ✅ 수신
            String newVersion = getInstalledSubAppVersion(MainActivity.this); // ✅ 새 버전
            String msg = "✅ 앱이 최신 버전(" + newVersion + ")으로 설치되었습니다."; // ✅ base 메시지
            setBaseStatusTextPreservingStudent(msg); // ✅ base 갱신(학생정보 보존)
        }
    };

    // ✅ 설치된 서브앱 버전 확인(기존)
    private String getInstalledSubAppVersion(Context context) { // ✅ 버전 조회
        try { // ✅ 예외 처리
            PackageManager pm = context.getPackageManager(); // ✅ PM
            PackageInfo info = pm.getPackageInfo("com.naegongstudy.app", 0); // ✅ 서브앱 정보
            return info.versionName; // ✅ 버전명 반환
        } catch (PackageManager.NameNotFoundException e) { // ✅ 미설치
            return null; // ✅ null
        }
    }

    // ✅ 접근성 권한 확인 함수(기존)
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) { // ✅ 접근성 체크
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE); // ✅ 매니저
        if (am == null || !am.isEnabled()) return false; // ✅ 비활성화면 false

        String expectedService = context.getPackageName() + "/" + service.getName(); // ✅ 기대 서비스 문자열
        String enabledServices = Settings.Secure.getString( // ✅ 활성화된 서비스 목록
                context.getContentResolver(), // ✅ resolver
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES // ✅ key
        );

        if (enabledServices == null || enabledServices.isEmpty()) return false; // ✅ 없으면 false

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':'); // ✅ ':' 분리
        colonSplitter.setString(enabledServices); // ✅ 대상 설정
        for (String enabledService : colonSplitter) { // ✅ 순회
            if (enabledService.equalsIgnoreCase(expectedService)) { // ✅ 일치하면
                return true; // ✅ true
            }
        }
        return false; // ✅ 불일치
    }

    // ✅ 알림 리스너 권한 요청(기존)
    private void requestNotificationListenerPermissionIfNeeded() { // ✅ 알림 리스너 체크
        String pkgName = getPackageName(); // ✅ 내 패키지
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners"); // ✅ 활성 리스너
        if (flat == null || !flat.contains(pkgName)) { // ✅ 없으면
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); // ✅ 설정 화면
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // ✅ 새 태스크
            startActivity(intent); // ✅ 이동
            Toast.makeText(this, "알림 관리 권한을 허용해주세요.", Toast.LENGTH_LONG).show(); // ✅ 안내
        }
    }

    // ✅ 방해금지 모드 설정(기존)
    private void checkAndSetDoNotDisturbPermission() { // ✅ DND 적용
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // ✅ NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) { // ✅ P+
            if (notificationManager.isNotificationPolicyAccessGranted()) { // ✅ 권한 있으면
                NotificationManager.Policy policy = new NotificationManager.Policy( // ✅ 정책
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS, // ✅ 알람만
                        NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON | // ✅ 화면 켬 억제
                                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF | // ✅ 화면 끔 억제
                                NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR | // ✅ 상태바 억제
                                NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE | // ✅ 뱃지 억제
                                NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT | // ✅ ambient 억제
                                NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST, // ✅ 알림 리스트 억제
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY // ✅ 발신자 제한 없음(카테고리로 제어)
                );
                notificationManager.setNotificationPolicy(policy); // ✅ 정책 적용
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE); // ✅ 완전 무음
            }
        }
    }

    // ✅ 앱 아이콘들 구성(기존)
    private void populateAllowedApps() { // ✅ 앱 버튼 구성
        PackageManager pm = getPackageManager(); // ✅ PM
        SharedPreferences prefs = getSharedPreferences("admin_settings", MODE_PRIVATE); // ✅ prefs
        Set<String> blockedApps = prefs.getStringSet("blocked_apps", new HashSet<>()); // ✅ 차단 앱

        for (String packageName : AppConfig.getAllowedApps(this)) { // ✅ 허용 앱 순회
            if (blockedApps.contains(packageName)) continue; // ✅ 차단이면 스킵

            try { // ✅ 예외 처리
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0); // ✅ 앱 정보
                Drawable icon = pm.getApplicationIcon(appInfo); // ✅ 아이콘
                String label = pm.getApplicationLabel(appInfo).toString(); // ✅ 라벨

                LinearLayout appItem = new LinearLayout(this); // ✅ 컨테이너
                appItem.setOrientation(LinearLayout.VERTICAL); // ✅ 세로
                appItem.setGravity(Gravity.CENTER); // ✅ 가운데
                appItem.setPadding(16, 16, 16, 16); // ✅ 패딩

                ImageView iconView = new ImageView(this); // ✅ 아이콘 뷰
                int iconSize = (int) (getResources().getDisplayMetrics().density * 72); // ✅ dp→px
                iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize)); // ✅ 크기
                iconView.setImageDrawable(icon); // ✅ 아이콘 적용

                TextView labelView = new TextView(this); // ✅ 라벨 뷰
                labelView.setText(label); // ✅ 라벨
                labelView.setTextSize(12); // ✅ 글씨 크기
                labelView.setTextColor(Color.WHITE); // ✅ 흰색
                labelView.setGravity(Gravity.CENTER); // ✅ 가운데

                appItem.addView(iconView); // ✅ 아이콘 추가
                appItem.addView(labelView); // ✅ 라벨 추가

                appItem.setOnClickListener(v -> { // ✅ 클릭 시 실행
                    Intent launchIntent = pm.getLaunchIntentForPackage(packageName); // ✅ 런치 인텐트
                    if (launchIntent != null) { // ✅ 존재하면
                        startActivity(launchIntent); // ✅ 실행
                    }
                });

                if (AppConfig.allowedEssentialApps.contains(packageName)) { // ✅ essential
                    binding.essentialGrid.addView(appItem); // ✅ 추가
                } else if (AppConfig.allowedLearningApps.contains(packageName)) { // ✅ learning
                    binding.learningGrid.addView(appItem); // ✅ 추가
                } else if (AppConfig.getAllowedUtilityApps(this).contains(packageName)) { // ✅ utility
                    binding.utilityGrid.addView(appItem); // ✅ 추가
                }

            } catch (PackageManager.NameNotFoundException e) { // ✅ 앱 없음
                e.printStackTrace(); // ✅ 로그
            }
        }
    }

    @Override
    protected void onDestroy() { // ✅ 종료
        super.onDestroy(); // ✅ 상위 호출
        LocalBroadcastManager.getInstance(this).unregisterReceiver(installSuccessReceiver); // ✅ 로컬 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshAppsReceiver); // ✅ 로컬 해제

        try { // ✅ 예외 방지
            unregisterReceiver(studentInfoReceiver); // ✅ 시스템 브로드캐스트 해제
        } catch (Exception ignored) { // ✅ 이미 해제됐거나 미등록이면 무시
        }
    }

    @Override
    public void onBackPressed() { // ✅ 뒤로가기
        // 뒤로가기 비활성화
    }
}
