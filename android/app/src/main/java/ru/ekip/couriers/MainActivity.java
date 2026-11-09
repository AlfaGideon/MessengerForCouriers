package ru.ekip.couriers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.app.Dialog;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Icon;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Rational;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native Android UI. The APK contains no WebView and renders every screen with Android Views. */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int PICK_VIDEO_REQUEST = 4102;
    private static final int UPDATE_PERMISSION_REQUEST = 4103;
    private static final int UPDATE_INSTALL_REQUEST = 4104;
    private static final int PICK_AVATAR_REQUEST = 4105;
    private static final int MICROPHONE_PERMISSION_REQUEST = 4106;
    private static final String ACTION_PIP_PLAY_PAUSE = "ru.ekip.couriers.action.PIP_PLAY_PAUSE";
    private static final String ACTION_PIP_MIC_TOGGLE = "ru.ekip.couriers.action.PIP_MIC_TOGGLE";
    private static final String UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/update.json";
    private static final String UPDATE_APK_PREFIX = "https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/ekip-debug.apk";
    private static final int INK = Color.rgb(14, 13, 11);
    private static final int SURFACE = Color.rgb(28, 27, 23);
    private static final int SURFACE_2 = Color.rgb(41, 39, 32);
    private static final int MUTED = Color.rgb(174, 169, 155);
    private static final int ACCENT = Color.rgb(255, 207, 34);
    private static final int ACCENT_2 = Color.rgb(255, 158, 20);
    private static final int RED = Color.rgb(235, 89, 68);
    private static final String[] CITY_NAMES = {
            "Абакан", "Архангельск", "Астрахань", "Балаково", "Балашиха", "Барнаул", "Белгород", "Бийск", "Благовещенск", "Братск", "Брянск",
            "Великий Новгород", "Владивосток", "Владикавказ", "Владимир", "Волгоград", "Вологда", "Воронеж", "Грозный", "Дзержинск", "Домодедово",
            "Екатеринбург", "Иваново", "Ижевск", "Иркутск", "Йошкар-Ола", "Казань", "Калининград", "Калуга", "Каменск-Уральский", "Кемерово", "Киров",
            "Кисловодск", "Королёв", "Кострома", "Краснодар", "Красноярск", "Красногорск", "Курган", "Курск", "Липецк", "Люберцы", "Магадан",
            "Магнитогорск", "Махачкала", "Миасс", "Москва", "Мурманск", "Муром", "Мытищи", "Набережные Челны", "Нальчик", "Находка", "Нефтекамск",
            "Нижневартовск", "Нижний Новгород", "Нижний Тагил", "Новокузнецк", "Новомосковск", "Новороссийск", "Новосибирск", "Норильск", "Одинцово",
            "Омск", "Орёл", "Оренбург", "Орск", "Пенза", "Пермь", "Петрозаводск", "Подольск", "Псков", "Пушкино", "Пятигорск", "Реутов",
            "Ростов-на-Дону", "Рубцовск", "Рязань", "Самара", "Санкт-Петербург", "Саранск", "Саратов", "Севастополь", "Симферополь", "Смоленск",
            "Сочи", "Ставрополь", "Старый Оскол", "Сургут", "Сыктывкар", "Таганрог", "Тамбов", "Тверь", "Тольятти", "Томск", "Тула", "Тюмень",
            "Улан-Удэ", "Ульяновск", "Уфа", "Хабаровск", "Ханты-Мансийск", "Химки", "Чебоксары", "Челябинск", "Череповец", "Чита", "Шахты",
            "Элиста", "Электросталь", "Южно-Сахалинск", "Якутск", "Ярославль"
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver pictureInPictureActionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT < 26 || !isInPictureInPictureMode()) return;
            if (ACTION_PIP_PLAY_PAUSE.equals(intent.getAction())) togglePlayback();
            else if (ACTION_PIP_MIC_TOGGLE.equals(intent.getAction())) toggleMicrophone();
        }
    };
    private boolean pictureInPictureActionReceiverRegistered;
    private boolean enteringPictureInPicture;
    private View pipFeedTopBar, pipFeedGradientTop, pipFeedGradientBottom, pipFeedDetails, pipFeedSideControls, pipFeedPlaybackControls;
    private View pipRoomHeader, pipRoomControls;
    private LinearLayout pipRoomPage;
    private LinearLayout.LayoutParams pipRoomStageParams;
    private FrameLayout pipRoomMicButton;
    private IconView pipRoomMicIcon;
    private ImageView pendingVideoTransitionView;
    private Bitmap pendingVideoTransitionBitmap;
    private int pendingVideoTransitionDirection;
    private boolean preservingVideoTransitionForRender;
    private boolean videoTransitionRunning;
    private final ArrayList<VideoItem> videos = new ArrayList<>();
    private final ArrayList<RoomItem> rooms = new ArrayList<>();
    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (player != null && player.isPlaying() && seekBar != null) {
                int duration = Math.max(1, player.getDuration());
                seekBar.setMax(duration);
                seekBar.setProgress(player.getCurrentPosition());
                if (videoTime != null) videoTime.setText(formatTime(player.getCurrentPosition()) + "  /  " + formatTime(duration));
                handler.postDelayed(this, 300);
            }
        }
    };

    private LinearLayout appRoot;
    private FrameLayout contentFrame;
    private LinearLayout bottomNavigation;
    private TextureView textureView;
    private MediaPlayer player;
    private Surface videoSurface;
    private volatile AudioRecord microphoneRecorder;
    private volatile NoiseSuppressor microphoneNoiseSuppressor;
    private volatile Thread microphoneMeterThread;
    private final AtomicBoolean microphoneCaptureRunning = new AtomicBoolean(false);
    private boolean microphoneEnabled = false;
    private boolean microphoneWasStoppedForPause = false;
    private VoiceArenaView voiceArenaView;
    private AudioTrack feedbackAudioTrack;
    private final Runnable releaseFeedbackAudioTrack = () -> {
        if (feedbackAudioTrack != null) {
            try { feedbackAudioTrack.stop(); } catch (Exception ignored) { }
            try { feedbackAudioTrack.release(); } catch (Exception ignored) { }
            feedbackAudioTrack = null;
        }
    };
    private TextView playOverlay;
    private TextView videoTime;
    private SeekBar seekBar;
    private String activeTab = "feed";
    private String joinedRoomId = null;
    private int currentVideoIndex = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private boolean userWantsPlayback = true;
    private boolean muted = false;
    private boolean copyingVideo = false;
    private String profileName = "";
    private String profileBio = "";
    private String profileTelegram = "";
    private String profileAvatarPath = "";
    private String courierService = "";
    private String cityId = "city:санкт-петербург";
    private String customCityName = "";
    private Dialog updateProgressDialog;
    private TextView updateProgressTitle;
    private TextView updateProgressDetail;
    private ProgressBar updateProgressBar;
    private boolean checkingForUpdate = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(INK);
        getWindow().setNavigationBarColor(Color.rgb(8, 8, 7));
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarDividerColor(Color.rgb(58, 53, 42));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        registerPictureInPictureActionReceiver();
        loadLocalState();
        buildShell();
        renderPage();
    }

    private void registerPictureInPictureActionReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_PIP_PLAY_PAUSE);
        filter.addAction(ACTION_PIP_MIC_TOGGLE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(pictureInPictureActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(pictureInPictureActionReceiver, filter);
        pictureInPictureActionReceiverRegistered = true;
    }

    private void loadLocalState() {
        SharedPreferences prefs = getSharedPreferences("ekip_native", MODE_PRIVATE);
        profileName = prefs.getString("profile_name", "");
        profileBio = prefs.getString("profile_bio", "");
        profileTelegram = prefs.getString("profile_telegram", "");
        profileAvatarPath = prefs.getString("profile_avatar_path", "");
        courierService = prefs.getString("courier_service", "");
        if (!profileAvatarPath.isEmpty() && !new File(profileAvatarPath).isFile()) profileAvatarPath = "";
        String saved = prefs.getString("videos", "[]");
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String path = object.optString("path", "");
                File file = new File(path);
                String title = object.optString("title", object.optString("caption", "")).trim();
                if (title.isEmpty()) title = "Моё видео";
                if (file.isFile()) videos.add(new VideoItem(path, title, object.optLong("date", file.lastModified())));
            }
        } catch (Exception ignored) { }
        if (videos.size() > 0) currentVideoIndex = Math.max(0, Math.min(prefs.getInt("video_index", 0), videos.size() - 1));
        String savedCity = prefs.getString("city_id", "spb");
        if (savedCity.startsWith("custom:")) {
            customCityName = prefs.getString("custom_city_name", savedCity.substring("custom:".length()));
            if (customCityName == null || customCityName.trim().isEmpty()) customCityName = savedCity.substring("custom:".length());
            cityId = customCityKey(customCityName);
        } else {
            int index = cityIndex(savedCity);
            if (index < 0) index = cityIndex("spb");
            cityId = cityKey(index < 0 ? 0 : index);
            prefs.edit().putString("city_id", cityId).apply();
        }
        rebuildAccessibleRooms();
    }

    private void buildShell() {
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(INK);
        contentFrame = new FrameLayout(this);
        appRoot.addView(contentFrame, new LinearLayout.LayoutParams(-1, 0, 1f));
        bottomNavigation = buildBottomNavigation();
        appRoot.addView(bottomNavigation, new LinearLayout.LayoutParams(-1, dp(88)));
        if (Build.VERSION.SDK_INT >= 30) {
            appRoot.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
                return insets;
            });
        }
        setContentView(appRoot);
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(7), dp(5), dp(7), dp(5));
        bar.setBackground(round(Color.rgb(20, 19, 16), 27));
        bar.setElevation(dp(10));
        bar.setClipChildren(false);
        bar.setClipToPadding(false);
        String[] ids = {"feed", "chats", "rooms", "profile", "more"};
        String[] labels = {"Лента", "Чаты", "Голос", "Профиль", "Ещё"};
        String[] icons = {"home", "chat", "mic", "profile", "more"};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            if ("rooms".equals(id)) {
                FrameLayout slot = new FrameLayout(this);
                slot.setTag(id);
                LinearLayout center = new LinearLayout(this);
                center.setOrientation(LinearLayout.VERTICAL);
                center.setGravity(Gravity.CENTER);
                center.setTranslationY(-dp(3));
                FrameLayout halo = new FrameLayout(this);
                halo.setBackground(round(0x33FFCF22, 36));
                FrameLayout circle = new FrameLayout(this);
                circle.setTag("voice-button");
                circle.setContentDescription("Открыть голосовые комнаты");
                circle.setBackground(ripple(round(ACCENT, 32)));
                circle.setElevation(dp(8));
                IconView mic = new IconView(this, "mic", INK, dp(28));
                circle.addView(mic, centered());
                FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER);
                halo.addView(circle, circleParams);
                center.addView(halo, new LinearLayout.LayoutParams(dp(66), dp(66)));
                TextView label = text(labels[i], 10, MUTED, true);
                label.setTag("nav-label");
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
                labelParams.topMargin = dp(1);
                center.addView(label, labelParams);
                slot.addView(center, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
                slot.setOnClickListener(view -> {
                    if (joinedRoomId != null) closeVoiceRoom();
                    activeTab = id;
                    renderPage();
                });
                bar.addView(slot, new LinearLayout.LayoutParams(0, -1, 1f));
                continue;
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(7), 0, dp(4));
            IconView icon = new IconView(this, icons[i], MUTED, dp(21));
            icon.setTag("nav-icon");
            TextView label = text(labels[i], 10, MUTED, true);
            label.setTag("nav-label");
            item.addView(icon, new LinearLayout.LayoutParams(dp(25), dp(26)));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
            labelParams.topMargin = dp(2);
            item.addView(label, labelParams);
            item.setBackground(ripple(round(0x00000000, 16)));
            item.setOnClickListener(view -> {
                if (joinedRoomId != null) closeVoiceRoom();
                activeTab = id;
                renderPage();
            });
            item.setTag(id);
            bar.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        return bar;
    }

    private void updateNavigationSelection() {
        if (bottomNavigation == null) return;
        for (int i = 0; i < bottomNavigation.getChildCount(); i++) {
            View item = bottomNavigation.getChildAt(i);
            String id = (String) item.getTag();
            boolean selected = activeTab.equals(id);
            if ("rooms".equals(id)) {
                TextView label = item.findViewWithTag("nav-label");
                View button = item.findViewWithTag("voice-button");
                if (label != null) label.setTextColor(selected ? ACCENT : MUTED);
                if (button != null) {
                    float scale = selected ? 1.06f : 1f;
                    button.setScaleX(scale);
                    button.setScaleY(scale);
                }
                continue;
            }
            LinearLayout tab = (LinearLayout) item;
            int color = selected ? ACCENT : MUTED;
            ((IconView) tab.getChildAt(0)).setTint(color);
            ((TextView) tab.getChildAt(1)).setTextColor(color);
            tab.setBackground(ripple(round(selected ? 0x1FFFCE22 : 0x00000000, 16)));
        }
    }

    private void renderPage() {
        if (!preservingVideoTransitionForRender) clearPendingVideoTransition();
        pipRoomHeader = pipRoomControls = pipRoomPage = null;
        pipRoomStageParams = null;
        pipRoomMicButton = null;
        pipRoomMicIcon = null;
        if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
        voiceArenaView = null;
        releasePlayer();
        if (contentFrame == null) return;
        contentFrame.removeAllViews();
        contentFrame.setBackgroundColor(INK);
        updateNavigationSelection();
        if ("feed".equals(activeTab)) renderFeed();
        else if ("rooms".equals(activeTab) && joinedRoomId != null) renderCallRoom();
        else if ("rooms".equals(activeTab)) renderRooms();
        else if ("chats".equals(activeTab)) renderEmptyPage("Чаты", "Сообщения появятся после подключения сервера.", "chat");
        else if ("profile".equals(activeTab)) renderProfile();
        else renderSettings();
        refreshPictureInPictureEligibility();
    }

    private void renderFeed() {
        FrameLayout feed = new FrameLayout(this);
        feed.setBackgroundColor(Color.rgb(9, 9, 8));
        contentFrame.addView(feed, new FrameLayout.LayoutParams(-1, -1));
        if (videos.isEmpty()) {
            renderEmptyFeed(feed);
            return;
        }
        currentVideoIndex = Math.max(0, Math.min(currentVideoIndex, videos.size() - 1));
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        feed.addView(textureView, new FrameLayout.LayoutParams(-1, -1));
        GestureDetector gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent event) {
                if (Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode()) return true;
                togglePlayback();
                return true;
            }
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dy) > dp(50) && Math.abs(velocityY) > dp(120)) {
                    if (Build.VERSION.SDK_INT < 26 || !isInPictureInPictureMode()) {
                        int direction = dy < 0 ? 1 : -1;
                        showVideoAt(currentVideoIndex + direction, direction);
                    }
                    return true;
                }
                return false;
            }
        });
        textureView.setOnTouchListener((view, event) -> gestures.onTouchEvent(event));
        addFeedGradient(feed);
        addFeedTopBar(feed);
        addFeedVideoDetails(feed);
        addFeedControls(feed);
        if (pendingVideoTransitionBitmap != null) installPendingVideoTransition(feed);
        if (player == null) { playOverlay.setText("▶"); playOverlay.setVisibility(View.VISIBLE); }
    }

    private void renderEmptyFeed(FrameLayout feed) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(26), dp(24), dp(26));
        empty.setBackground(round(Color.rgb(26, 24, 20), 28));
        empty.setElevation(dp(4));

        FrameLayout iconTile = new FrameLayout(this);
        iconTile.setBackground(round(0x22FFCF22, 23));
        TextView plus = roundIconButton("add", ACCENT, INK, 52);
        plus.setTextSize(30);
        plus.setBackground(ripple(round(ACCENT, 20)));
        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER);
        iconTile.addView(plus, plusParams);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        tileParams.bottomMargin = dp(22);
        empty.addView(iconTile, tileParams);
        plus.setOnClickListener(view -> openVideoPicker());

        TextView eyebrow = text("ТВОЯ ВИДЕОМЕДИАТЕКА", 10, ACCENT, true);
        eyebrow.setLetterSpacing(.12f);
        eyebrow.setGravity(Gravity.CENTER);
        empty.addView(eyebrow);
        TextView title = text("Первое видео\nначинается здесь", 23, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(11);
        empty.addView(title, titleParams);
        TextView subtitle = text("Добавь ролик с телефона — он останется только у тебя и будет доступен после перезапуска.", 13, 0xFFD1CCBF, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(10);
        subtitleParams.bottomMargin = dp(21);
        empty.addView(subtitle, subtitleParams);
        TextView choose = pillButton("＋   Выбрать видео", ACCENT, INK, 15);
        choose.setOnClickListener(view -> openVideoPicker());
        empty.addView(choose, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView privacy = text("СОХРАНЯЕТСЯ НА ЭТОМ ТЕЛЕФОНЕ", 9, MUTED, true);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(16);
        empty.addView(privacy, privacyParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        cardParams.leftMargin = dp(22);
        cardParams.rightMargin = dp(22);
        feed.addView(empty, cardParams);
        addFeedTopBar(feed);
    }

    private void addFeedTopBar(FrameLayout feed) {
        LinearLayout top = new LinearLayout(this);
        pipFeedTopBar = top;
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(12), dp(16), dp(8));
        ImageView logo = new ImageView(this);
        try { logo.setImageResource(ru.ekip.couriers.R.mipmap.ic_launcher); } catch (Exception ignored) { }
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackgroundColor(Color.TRANSPARENT);
        top.addView(logo, new LinearLayout.LayoutParams(dp(37), dp(37)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        TextView name = text("Курьерка", 18, Color.WHITE, true);
        TextView sub = text("СВОИ НА МАРШРУТЕ", 8, 0xFFE0E0E4, true);
        sub.setLetterSpacing(.13f);
        brand.addView(name);
        brand.addView(sub);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        if (videos.size() > 0) {
            TextView add = roundIconButton("add", ACCENT, INK, 48);
            add.setContentDescription("Добавить видео");
            add.setOnClickListener(view -> openVideoPicker());
            top.addView(add, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, dp(70), Gravity.TOP);
        feed.addView(top, params);
        top.bringToFront();
    }

    private void addFeedGradient(FrameLayout feed) {
        View gradientTop = new View(this);
        pipFeedGradientTop = gradientTop;
        gradientTop.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xB8000000, 0x00000000}));
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(-1, dp(135), Gravity.TOP);
        feed.addView(gradientTop, top);
        View gradientBottom = new View(this);
        pipFeedGradientBottom = gradientBottom;
        gradientBottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xE8000000, 0x00000000}));
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, dp(240), Gravity.BOTTOM);
        feed.addView(gradientBottom, bottom);
    }

    private void addFeedVideoDetails(FrameLayout feed) {
        VideoItem video = videos.get(currentVideoIndex);
        LinearLayout info = new LinearLayout(this);
        pipFeedDetails = info;
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(18), 0, dp(84), dp(16));

        LinearLayout uploader = new LinearLayout(this);
        uploader.setOrientation(LinearLayout.HORIZONTAL);
        uploader.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout avatar = createAvatarView(dp(38));
        avatar.setContentDescription("Открыть профиль " + (profileName.isEmpty() ? "Курьер" : profileName));
        uploader.addView(avatar, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout authorDetails = new LinearLayout(this);
        authorDetails.setOrientation(LinearLayout.VERTICAL);
        authorDetails.setPadding(dp(10), 0, 0, 0);
        TextView author = text(profileName.isEmpty() ? "Курьер" : profileName, 13, Color.WHITE, true);
        authorDetails.addView(author);
        TextView uploadedAt = text(formatUploadTime(video.date), 10, 0xFFD1CCBF, false);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(3);
        authorDetails.addView(uploadedAt, dateParams);
        uploader.addView(authorDetails, new LinearLayout.LayoutParams(0, -2, 1f));
        View.OnClickListener openProfile = view -> openCurrentProfile();
        avatar.setOnClickListener(openProfile);
        authorDetails.setOnClickListener(openProfile);
        uploader.setContentDescription("Автор видео. Открыть профиль");
        info.addView(uploader);

        TextView title = text(video.title, 15, Color.WHITE, false);
        title.setMaxLines(4);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(9);
        info.addView(title, titleParams);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
        params.bottomMargin = dp(104);
        feed.addView(info, params);
    }

    private String formatUploadTime(long timestamp) {
        return new SimpleDateFormat("d MMMM, HH:mm", new Locale("ru", "RU")).format(new Date(timestamp));
    }

    private void openCurrentProfile() {
        joinedRoomId = null;
        activeTab = "profile";
        renderPage();
    }

    private void addFeedControls(FrameLayout feed) {
        LinearLayout side = new LinearLayout(this);
        pipFeedSideControls = side;
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER_HORIZONTAL);
        side.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView sound = roundIconButton(muted ? "mute" : "sound", 0x990D0C09, Color.WHITE, 46);
        sound.setContentDescription(muted ? "Включить звук" : "Выключить звук");
        sound.setOnClickListener(view -> {
            muted = !muted;
            if (player != null) player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
            sound.setText(muted ? "×" : "♪");
        });
        side.addView(sound);
        TextView delete = roundIconButton("more", 0x990D0C09, Color.WHITE, 46);
        delete.setContentDescription("Действия с видео");
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        deleteParams.topMargin = dp(13);
        side.addView(delete, deleteParams);
        delete.setOnClickListener(view -> showDeleteCurrentVideo());
        FrameLayout.LayoutParams sideParams = new FrameLayout.LayoutParams(dp(64), -2, Gravity.BOTTOM | Gravity.END);
        sideParams.bottomMargin = dp(149);
        sideParams.rightMargin = dp(6);
        feed.addView(side, sideParams);

        LinearLayout playback = new LinearLayout(this);
        pipFeedPlaybackControls = playback;
        playback.setOrientation(LinearLayout.HORIZONTAL);
        playback.setGravity(Gravity.CENTER_VERTICAL);
        playback.setPadding(dp(15), 0, dp(15), 0);
        playback.setBackground(round(0x6613110D, 20));
        TextView play = roundIconButton("pause", 0x00000000, Color.WHITE, 38);
        playOverlay = play;
        play.setOnClickListener(view -> togglePlayback());
        playback.addView(play, new LinearLayout.LayoutParams(dp(38), dp(42)));
        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seekBar.setThumbTintList(ColorStateList.valueOf(ACCENT));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(0x77FFFFFF));
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, dp(36), 1f);
        seekParams.leftMargin = dp(7);
        playback.addView(seekBar, seekParams);
        videoTime = text("0:00  /  0:00", 10, Color.WHITE, true);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(dp(68), -2);
        timeParams.leftMargin = dp(7);
        videoTime.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        playback.addView(videoTime, timeParams);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) {
                    int position = (int) (player.getDuration() * (progress / (float) bar.getMax()));
                    player.seekTo(position);
                    if (videoTime != null) videoTime.setText(formatTime(position) + "  /  " + formatTime(player.getDuration()));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        FrameLayout.LayoutParams playbackParams = new FrameLayout.LayoutParams(-1, dp(50), Gravity.BOTTOM);
        playbackParams.leftMargin = dp(14);
        playbackParams.rightMargin = dp(14);
        playbackParams.bottomMargin = dp(18);
        feed.addView(playback, playbackParams);
    }

    private void renderEmptyPage(String title, String message, String icon) {
        LinearLayout page = pageColumn();
        addPageHeader(page, title, "");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(30), dp(24), dp(30));
        card.setBackground(round(SURFACE, 24));
        IconView art = new IconView(this, icon, ACCENT, dp(32));
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        artParams.bottomMargin = dp(18);
        card.addView(art, artParams);
        String emptyTitle = "Чаты".equals(title) ? "Чатов пока нет" : title + " пока пусто";
        TextView titleView = text(emptyTitle, 19, Color.WHITE, true);
        titleView.setGravity(Gravity.CENTER);
        card.addView(titleView);
        TextView detail = text(message, 14, MUTED, false);
        detail.setGravity(Gravity.CENTER);
        detail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(9);
        card.addView(detail, detailParams);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        cardParams.topMargin = dp(18);
        cardParams.bottomMargin = dp(18);
        page.addView(card, cardParams);
        contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
    }

    private void renderRooms() {
        LinearLayout page = pageColumn();
        addPageHeader(page, "Голосовые", cityName() + " · свои на связи");
        LinearLayout accessCard = new LinearLayout(this);
        accessCard.setOrientation(LinearLayout.VERTICAL);
        accessCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        accessCard.setBackground(round(SURFACE, 20));
        TextView accessTitle = text("Две комнаты для вас", 16, Color.WHITE, true);
        accessCard.addView(accessTitle);
        TextView accessDetail = text("Общий голосовой канал и голосовой канал вашего города. Другие города не показываются.", 12, MUTED, false);
        accessDetail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams accessDetailParams = new LinearLayout.LayoutParams(-1, -2);
        accessDetailParams.topMargin = dp(5);
        accessCard.addView(accessDetail, accessDetailParams);
        LinearLayout.LayoutParams accessParams = new LinearLayout.LayoutParams(-1, -2);
        accessParams.topMargin = dp(12);
        accessParams.bottomMargin = dp(16);
        page.addView(accessCard, accessParams);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (RoomItem room : rooms) list.addView(roomCard(room));
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
    }

    private View roomCard(RoomItem room) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(12), dp(14));
        card.setBackground(round(SURFACE, 18));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        TextView icon = text(room.global ? "◎" : "⌖", 24, INK, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(room.tint, 15));
        card.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, dp(6), 0);
        TextView kind = text(room.global ? "ОБЩАЯ" : "ГОРОДСКАЯ", 9, MUTED, true);
        kind.setLetterSpacing(.08f);
        TextView name = text(room.name, 15, Color.WHITE, true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(4);
        TextView status = text(room.global ? "Открыта всем" : "Только ваш город", 11, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(4);
        copy.addView(kind);
        copy.addView(name, nameParams);
        copy.addView(status, statusParams);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView join = pillButton("Открыть", ACCENT, INK, 12);
        card.addView(join, new LinearLayout.LayoutParams(dp(76), dp(42)));
        join.setOnClickListener(view -> {
            if (!room.global && !room.id.equals(cityId)) {
                Toast.makeText(this, "Доступна только городская комната «" + cityName() + "»", Toast.LENGTH_SHORT).show();
                return;
            }
            joinedRoomId = room.id;
            activeTab = "rooms";
            renderPage();
            playVoiceCue(true);
            requestMicrophonePermission();
        });
        return card;
    }

    private void renderCallRoom() {
        RoomItem room = findRoom(joinedRoomId);
        if (room == null) { joinedRoomId = null; renderRooms(); return; }
        LinearLayout page = pageColumn();
        pipRoomPage = page;
        LinearLayout header = new LinearLayout(this);
        pipRoomHeader = header;
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));
        TextView back = roundIconButton("back", SURFACE_2, Color.WHITE, 46);
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        back.setOnClickListener(view -> leaveVoiceRoom());
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.setPadding(dp(12), 0, 0, 0);
        titleGroup.addView(text("ГОЛОСОВАЯ КОМНАТА", 9, MUTED, true));
        TextView roomTitle = text(room.name, 17, Color.WHITE, true);
        LinearLayout.LayoutParams roomTitleParams = new LinearLayout.LayoutParams(-1, -2);
        roomTitleParams.topMargin = dp(3);
        titleGroup.addView(roomTitle, roomTitleParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView count = pillButton(room.global ? "ОБЩАЯ" : "ВАШ ГОРОД", SURFACE_2, ACCENT, 9);
        header.addView(count, new LinearLayout.LayoutParams(-2, dp(38)));
        page.addView(header);

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(round(SURFACE, 26));
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        stageParams.bottomMargin = dp(14);
        pipRoomStageParams = stageParams;
        page.addView(stage, stageParams);
        voiceArenaView = new VoiceArenaView(this);
        stage.addView(voiceArenaView, new FrameLayout.LayoutParams(-1, -1));
        pipRoomMicButton = new FrameLayout(this);
        pipRoomMicButton.setContentDescription(microphoneEnabled ? "Выключить микрофон" : "Включить микрофон");
        pipRoomMicButton.setBackground(ripple(round(microphoneEnabled ? ACCENT : SURFACE_2, 28)));
        pipRoomMicButton.setElevation(dp(5));
        pipRoomMicButton.setVisibility(View.GONE);
        pipRoomMicIcon = new IconView(this, microphoneEnabled ? "mic" : "mute", microphoneEnabled ? INK : ACCENT, dp(22));
        pipRoomMicButton.addView(pipRoomMicIcon, centered());
        pipRoomMicButton.setOnClickListener(view -> toggleMicrophone());
        FrameLayout.LayoutParams pipMicParams = new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pipMicParams.bottomMargin = dp(14);
        stage.addView(pipRoomMicButton, pipMicParams);

        LinearLayout controls = new LinearLayout(this);
        pipRoomControls = controls;
        controls.setGravity(Gravity.CENTER);
        TextView mic = pillButton(microphoneEnabled ? "Выключить микрофон" : hasMicrophonePermission() ? "Включить микрофон" : "Разрешить микрофон",
                microphoneEnabled ? SURFACE_2 : ACCENT, microphoneEnabled ? Color.WHITE : INK, 12);
        controls.addView(mic, new LinearLayout.LayoutParams(0, dp(54), 1f));
        mic.setOnClickListener(view -> toggleMicrophone());
        TextView leave = pillButton("‹  Выйти", RED, Color.WHITE, 14);
        LinearLayout.LayoutParams leaveParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        leaveParams.leftMargin = dp(10);
        controls.addView(leave, leaveParams);
        leave.setOnClickListener(view -> leaveVoiceRoom());
        page.addView(controls);
        contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
    }

    private void renderProfile() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout page = pageColumn();
        addPageHeader(page, "Профиль", "Ваша карточка курьера");

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.HORIZONTAL);
        preview.setGravity(Gravity.CENTER_VERTICAL);
        preview.setPadding(dp(16), dp(16), dp(14), dp(16));
        preview.setBackground(round(SURFACE, 23));
        FrameLayout avatar = createAvatarView(dp(72));
        preview.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setPadding(dp(13), 0, dp(8), 0);
        TextView displayName = text(profileName.isEmpty() ? "Ваш никнейм" : profileName, 17, Color.WHITE, true);
        displayName.setMaxLines(1);
        displayName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        identity.addView(displayName, new LinearLayout.LayoutParams(-1, -2));
        TextView local = text(profileBio.isEmpty() ? cityName() + " · Курьер" : profileBio, 11, MUTED, false);
        local.setMaxLines(2);
        LinearLayout.LayoutParams localParams = new LinearLayout.LayoutParams(-1, -2);
        localParams.topMargin = dp(5);
        identity.addView(local, localParams);
        LinearLayout workRow = new LinearLayout(this);
        workRow.setGravity(Gravity.CENTER_VERTICAL);
        int serviceLogo = courierServiceLogo(courierService);
        if (!courierService.isEmpty()) {
            if (serviceLogo != 0) {
                ImageView logo = new ImageView(this);
                logo.setImageResource(serviceLogo);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.setContentDescription("Логотип " + courierService);
                workRow.addView(logo, new LinearLayout.LayoutParams(dp(24), dp(24)));
            } else {
                FrameLayout genericLogo = new FrameLayout(this);
                genericLogo.setBackground(round(SURFACE_2, 6));
                genericLogo.addView(new IconView(this, "delivery", ACCENT, dp(17)), centered());
                genericLogo.setContentDescription("Значок курьерской службы");
                workRow.addView(genericLogo, new LinearLayout.LayoutParams(dp(24), dp(24)));
            }
        }
        TextView workService = text(courierService.isEmpty() ? "Курьерская служба не указана" : "Работает · " + courierService,
                10, courierService.isEmpty() ? MUTED : ACCENT, true);
        workService.setMaxLines(1);
        workService.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams workTextParams = new LinearLayout.LayoutParams(0, -2, 1f);
        workTextParams.leftMargin = courierService.isEmpty() ? 0 : dp(5);
        workRow.addView(workService, workTextParams);
        LinearLayout.LayoutParams workServiceParams = new LinearLayout.LayoutParams(-1, -2);
        workServiceParams.topMargin = dp(4);
        identity.addView(workRow, workServiceParams);
        preview.addView(identity, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!profileTelegram.isEmpty()) {
            FrameLayout telegram = new FrameLayout(this);
            telegram.setContentDescription("Открыть Telegram");
            telegram.setBackground(ripple(round(SURFACE_2, 17)));
            telegram.addView(new IconView(this, "telegram", ACCENT, dp(24)), centered());
            preview.addView(telegram, new LinearLayout.LayoutParams(dp(48), dp(48)));
            telegram.setOnClickListener(view -> openTelegramProfile());
        }
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.topMargin = dp(15);
        page.addView(preview, previewParams);

        addSectionLabel(page, "НАСТРОИТЬ ПРОФИЛЬ");
        LinearLayout editor = settingsCard();
        editor.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout avatarActions = new LinearLayout(this);
        avatarActions.setOrientation(LinearLayout.HORIZONTAL);
        TextView chooseAvatar = pillButton(profileAvatarPath.isEmpty() ? "＋  Выбрать аватар" : "Изменить фото", SURFACE_2, ACCENT, 13);
        avatarActions.addView(chooseAvatar, new LinearLayout.LayoutParams(0, dp(46), 1f));
        chooseAvatar.setOnClickListener(view -> openAvatarPicker());
        if (!profileAvatarPath.isEmpty()) {
            TextView removeAvatar = pillButton("Удалить", SURFACE_2, MUTED, 12);
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(88), dp(46));
            removeParams.leftMargin = dp(8);
            avatarActions.addView(removeAvatar, removeParams);
            removeAvatar.setOnClickListener(view -> clearProfileAvatar());
        }
        editor.addView(avatarActions);

        String[] selectedCourierService = {courierService};
        addInlineLabel(editor, "КУРЬЕРСКАЯ СЛУЖБА");
        addCourierServiceSelector(editor, selectedCourierService);

        addInlineLabel(editor, "НИКНЕЙМ");
        EditText nickname = profileInput("Например, КурьерПитер", profileName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nickname.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        LinearLayout.LayoutParams nicknameParams = new LinearLayout.LayoutParams(-1, dp(52));
        nicknameParams.topMargin = dp(7);
        editor.addView(nickname, nicknameParams);

        addInlineLabel(editor, "О СЕБЕ / СТАТУС");
        EditText bio = new EditText(this);
        bio.setText(profileBio);
        bio.setHint("Например, на маршруте каждый день");
        bio.setTextColor(Color.WHITE);
        bio.setHintTextColor(MUTED);
        bio.setTextSize(14);
        bio.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        bio.setMinLines(2);
        bio.setMaxLines(3);
        bio.setGravity(Gravity.TOP | Gravity.START);
        bio.setPadding(dp(14), dp(12), dp(14), dp(10));
        bio.setBackground(round(SURFACE_2, 14));
        bio.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        LinearLayout.LayoutParams bioParams = new LinearLayout.LayoutParams(-1, dp(82));
        bioParams.topMargin = dp(7);
        editor.addView(bio, bioParams);

        addInlineLabel(editor, "TELEGRAM");
        EditText telegram = profileInput("@username или t.me/username", profileTelegram.isEmpty() ? "" : "@" + profileTelegram,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        telegram.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams telegramParams = new LinearLayout.LayoutParams(-1, dp(52));
        telegramParams.topMargin = dp(7);
        editor.addView(telegram, telegramParams);
        TextView telegramHint = text("В карточке будет только значок самолётика — ссылка не показывается текстом.", 11, MUTED, false);
        telegramHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(6);
        editor.addView(telegramHint, hintParams);

        TextView save = pillButton("СОХРАНИТЬ ПРОФИЛЬ", ACCENT, INK, 13);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(52));
        saveParams.topMargin = dp(17);
        editor.addView(save, saveParams);
        TextView localInfo = text("Профиль сохранён на этом телефоне. Telegram откроется по значку самолётика.", 10, MUTED, false);
        localInfo.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams localInfoParams = new LinearLayout.LayoutParams(-1, -2);
        localInfoParams.topMargin = dp(11);
        editor.addView(localInfo, localInfoParams);
        save.setOnClickListener(view -> {
            String newName = nickname.getText().toString().trim().replaceAll("\\s+", " ");
            String newTelegram = normalizeTelegramHandle(telegram.getText().toString());
            if (newTelegram == null) {
                telegram.setError("Введите @username или ссылку t.me/username");
                return;
            }
            profileName = newName;
            profileBio = bio.getText().toString().trim();
            profileTelegram = newTelegram;
            courierService = selectedCourierService[0] == null ? "" : selectedCourierService[0].trim();
            getSharedPreferences("ekip_native", MODE_PRIVATE).edit()
                    .putString("profile_name", profileName)
                    .putString("profile_bio", profileBio)
                    .putString("profile_telegram", profileTelegram)
                    .putString("courier_service", courierService)
                    .apply();
            Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show();
            renderPage();
        });
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(-1, -2);
        editorParams.topMargin = dp(4);
        editorParams.bottomMargin = dp(20);
        page.addView(editor, editorParams);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        contentFrame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addCourierServiceSelector(LinearLayout parent, String[] selectedService) {
        TextView hint = text("Выберите службу доставки", 11, MUTED, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(5);
        parent.addView(hint, hintParams);

        String[] labels = {"Не указано", "Яндекс Еда", "Магнит Доставка", "Купер", "Яндекс Доставка", "Самокат", "Достависта", "Ozon", "Wildberries", "Другая служба"};
        String[] values = {"", "Яндекс Еда", "Магнит Доставка", "Купер", "Яндекс Доставка", "Самокат", "Достависта", "Ozon", "Wildberries", ""};
        ArrayList<LinearLayout> choices = new ArrayList<>();
        ArrayList<TextView> choiceLabels = new ArrayList<>();
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(4);
        parent.addView(grid, gridParams);

        Runnable refreshSelection = () -> {
            boolean custom = !selectedService[0].isEmpty() && !isPopularCourierService(selectedService[0]);
            for (int i = 0; i < choices.size(); i++) {
                boolean selected = i == labels.length - 1 ? custom : values[i].equals(selectedService[0]);
                choices.get(i).setBackground(ripple(round(selected ? 0x33FFCF22 : SURFACE_2, 14)));
                choiceLabels.get(i).setTextColor(selected ? ACCENT : Color.WHITE);
            }
        };

        for (int i = 0; i < labels.length; i++) {
            if (i % 2 == 0) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                if (i > 0) rowParams.topMargin = dp(6);
                grid.addView(row, rowParams);
            }
            LinearLayout row = (LinearLayout) grid.getChildAt(grid.getChildCount() - 1);
            final int choiceIndex = i;
            LinearLayout chip = new LinearLayout(this);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(6), dp(5), dp(6), dp(5));
            int logoResource = courierServiceLogo(values[i]);
            if (logoResource != 0) {
                ImageView logo = new ImageView(this);
                logo.setImageResource(logoResource);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.setBackground(round(SURFACE, 8));
                logo.setClipToOutline(true);
                logo.setContentDescription("Логотип " + labels[i]);
                chip.addView(logo, new LinearLayout.LayoutParams(dp(34), dp(34)));
            } else {
                FrameLayout logoBox = new FrameLayout(this);
                logoBox.setBackground(round(SURFACE, 8));
                String iconName = choiceIndex == 0 ? "delivery" : choiceIndex == labels.length - 1 ? "add" : "delivery";
                logoBox.addView(new IconView(this, iconName, MUTED, dp(20)), centered());
                chip.addView(logoBox, new LinearLayout.LayoutParams(dp(34), dp(34)));
            }
            TextView name = text(labels[i], 11, Color.WHITE, true);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
            nameParams.leftMargin = dp(6);
            chip.addView(name, nameParams);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            chipParams.leftMargin = dp(3);
            chipParams.rightMargin = dp(3);
            row.addView(chip, chipParams);
            choices.add(chip);
            choiceLabels.add(name);
            chip.setOnClickListener(view -> {
                if (choiceIndex == labels.length - 1) {
                    showCustomCourierServiceDialog(selectedService, refreshSelection);
                } else {
                    selectedService[0] = values[choiceIndex];
                    refreshSelection.run();
                }
            });
        }
        if (labels.length % 2 != 0) {
            LinearLayout lastRow = (LinearLayout) grid.getChildAt(grid.getChildCount() - 1);
            lastRow.addView(new View(this), new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        refreshSelection.run();
    }

    private int courierServiceLogo(String value) {
        if ("Яндекс Еда".equals(value)) return R.drawable.logo_yandex_eda;
        if ("Магнит Доставка".equals(value)) return R.drawable.logo_magnit_delivery;
        if ("Купер".equals(value)) return R.drawable.logo_kuper;
        if ("Яндекс Доставка".equals(value)) return R.drawable.logo_yandex_delivery;
        if ("Самокат".equals(value)) return R.drawable.logo_samokat;
        if ("Достависта".equals(value)) return R.drawable.logo_dostavista;
        if ("Ozon".equals(value)) return R.drawable.logo_ozon;
        if ("Wildberries".equals(value)) return R.drawable.logo_wildberries;
        return 0;
    }

    private boolean isPopularCourierService(String value) {
        return "Яндекс Еда".equals(value) || "Магнит Доставка".equals(value) || "Купер".equals(value)
                || "Яндекс Доставка".equals(value) || "Самокат".equals(value) || "Достависта".equals(value)
                || "Ozon".equals(value) || "Wildberries".equals(value);
    }

    private void showCustomCourierServiceDialog(String[] selectedService, Runnable onSaved) {
        String initialValue = isPopularCourierService(selectedService[0]) ? "" : selectedService[0];
        EditText input = profileInput("Название службы", initialValue, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        new AlertDialog.Builder(this)
                .setTitle("Ваша курьерская служба")
                .setView(input)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Готово", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) selectedService[0] = value;
                    onSaved.run();
                })
                .show();
    }

    private void addInlineLabel(LinearLayout parent, String value) {
        TextView label = text(value, 9, ACCENT, true);
        label.setLetterSpacing(.1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(18);
        parent.addView(label, params);
    }

    private EditText profileInput(String hint, String value, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(MUTED);
        input.setTextSize(14);
        input.setInputType(inputType);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(SURFACE_2, 14));
        return input;
    }

    private String normalizeTelegramHandle(String value) {
        String handle = value == null ? "" : value.trim();
        if (handle.isEmpty()) return "";
        if (handle.toLowerCase(Locale.ROOT).startsWith("https://") || handle.toLowerCase(Locale.ROOT).startsWith("http://")) {
            Uri uri = Uri.parse(handle);
            String host = uri.getHost();
            if (host == null || !(host.equalsIgnoreCase("t.me") || host.equalsIgnoreCase("telegram.me") || host.equalsIgnoreCase("www.t.me") || host.equalsIgnoreCase("www.telegram.me"))) return null;
            if (uri.getPathSegments().isEmpty()) return null;
            handle = uri.getPathSegments().get(0);
        } else {
            handle = handle.replaceFirst("(?i)^(https?://)?(www\\.)?(t\\.me|telegram\\.me)/", "");
            if (handle.contains("/")) handle = handle.substring(0, handle.indexOf('/'));
            if (handle.contains("?")) handle = handle.substring(0, handle.indexOf('?'));
            if (handle.startsWith("@")) handle = handle.substring(1);
        }
        return handle.matches("[A-Za-z0-9_]{5,32}") ? handle : null;
    }

    private void openTelegramProfile() {
        if (profileTelegram.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + profileTelegram));
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(Intent.createChooser(intent, "Выберите аватар"), PICK_AVATAR_REQUEST); }
        catch (Exception error) { Toast.makeText(this, "Не удалось открыть выбор изображения", Toast.LENGTH_SHORT).show(); }
    }

    private void copyProfileAvatar(Uri uri) {
        ioExecutor.execute(() -> {
            File temp = new File(getFilesDir(), "profile-avatar.tmp");
            File destination = new File(getFilesDir(), "profile-avatar.img");
            boolean saved = false;
            try (InputStream input = getContentResolver().openInputStream(uri); FileOutputStream output = new FileOutputStream(temp)) {
                if (input == null) throw new IllegalStateException("empty image stream");
                byte[] buffer = new byte[16 * 1024];
                int count;
                long total = 0;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > 12L * 1024L * 1024L) throw new IllegalStateException("image too large");
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
                if (destination.exists() && !destination.delete()) throw new IllegalStateException("cannot replace image");
                saved = temp.renameTo(destination);
                if (!saved) throw new IllegalStateException("cannot save image");
            } catch (Exception error) {
                temp.delete();
            }
            final boolean result = saved;
            handler.post(() -> {
                if (result) {
                    profileAvatarPath = destination.getAbsolutePath();
                    getSharedPreferences("ekip_native", MODE_PRIVATE).edit().putString("profile_avatar_path", profileAvatarPath).apply();
                    Toast.makeText(this, "Аватар обновлён", Toast.LENGTH_SHORT).show();
                    renderPage();
                } else Toast.makeText(this, "Не удалось сохранить это изображение", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void clearProfileAvatar() {
        if (!profileAvatarPath.isEmpty()) new File(profileAvatarPath).delete();
        profileAvatarPath = "";
        getSharedPreferences("ekip_native", MODE_PRIVATE).edit().remove("profile_avatar_path").apply();
        renderPage();
    }

    private FrameLayout createAvatarView(int size) {
        FrameLayout outer = new FrameLayout(this);
        outer.setPadding(dp(3), dp(3), dp(3), dp(3));
        outer.setBackground(oval(ACCENT));
        FrameLayout inner = new FrameLayout(this);
        inner.setBackground(oval(SURFACE_2));
        TextView fallback = text(profileName.isEmpty() ? "К" : profileName.substring(0, 1).toUpperCase(Locale.getDefault()), 28, ACCENT, true);
        fallback.setGravity(Gravity.CENTER);
        inner.addView(fallback, centered());
        File avatarFile = profileAvatarPath.isEmpty() ? null : new File(profileAvatarPath);
        if (avatarFile != null && avatarFile.isFile()) {
            Bitmap bitmap = decodeAvatar(avatarFile.getAbsolutePath(), size);
            if (bitmap != null) {
                ImageView photo = new ImageView(this);
                photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                photo.setImageBitmap(bitmap);
                photo.setBackground(oval(SURFACE_2));
                photo.setClipToOutline(true);
                inner.addView(photo, centered());
            }
        }
        FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams(-1, -1);
        outer.addView(inner, innerParams);
        outer.setContentDescription("Аватар профиля");
        return outer;
    }

    private Bitmap decodeAvatar(String path, int targetSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int sample = 1;
            int target = Math.max(1, targetSize);
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception ignored) { return null; }
    }

    private void renderSettings() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout page = pageColumn();
        addPageHeader(page, "Ещё", "Профиль, город и настройки Курьерки");

        addSectionLabel(page, "ВАШ ГОРОД");
        LinearLayout cityCard = new LinearLayout(this);
        cityCard.setOrientation(LinearLayout.HORIZONTAL);
        cityCard.setGravity(Gravity.CENTER_VERTICAL);
        cityCard.setPadding(dp(16), dp(16), dp(14), dp(16));
        cityCard.setBackground(ripple(round(SURFACE, 20)));
        TextView cityIcon = text("⌖", 25, INK, true);
        cityIcon.setGravity(Gravity.CENTER);
        cityIcon.setBackground(round(ACCENT, 16));
        cityCard.addView(cityIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout cityCopy = new LinearLayout(this);
        cityCopy.setOrientation(LinearLayout.VERTICAL);
        cityCopy.setPadding(dp(13), 0, dp(8), 0);
        cityCopy.addView(text(cityName(), 16, Color.WHITE, true));
        TextView cityDetail = text("Нажмите, чтобы сменить город", 12, MUTED, false);
        LinearLayout.LayoutParams cityDetailParams = new LinearLayout.LayoutParams(-1, -2);
        cityDetailParams.topMargin = dp(4);
        cityCopy.addView(cityDetail, cityDetailParams);
        cityCard.addView(cityCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        cityCard.addView(new IconView(this, "next", MUTED, dp(19)), new LinearLayout.LayoutParams(dp(24), dp(24)));
        cityCard.setOnClickListener(view -> showCityPicker());
        LinearLayout.LayoutParams cityCardParams = new LinearLayout.LayoutParams(-1, -2);
        cityCardParams.topMargin = dp(9);
        page.addView(cityCard, cityCardParams);

        addSectionLabel(page, "ВАША СТРАНИЦА");
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.HORIZONTAL);
        profileCard.setGravity(Gravity.CENTER_VERTICAL);
        profileCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        profileCard.setBackground(ripple(round(SURFACE, 21)));
        FrameLayout settingsAvatar = createAvatarView(dp(54));
        profileCard.addView(settingsAvatar, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout profileCopy = new LinearLayout(this);
        profileCopy.setOrientation(LinearLayout.VERTICAL);
        profileCopy.setPadding(dp(12), 0, dp(8), 0);
        profileCopy.addView(text(profileName.isEmpty() ? "Настроить профиль" : profileName, 15, Color.WHITE, true));
        TextView profileHint = text(profileTelegram.isEmpty() ? "Фото, никнейм и Telegram" : "Фото, никнейм · Telegram подключён", 11, MUTED, false);
        LinearLayout.LayoutParams profileHintParams = new LinearLayout.LayoutParams(-1, -2);
        profileHintParams.topMargin = dp(5);
        profileCopy.addView(profileHint, profileHintParams);
        profileCard.addView(profileCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        profileCard.addView(new IconView(this, "next", ACCENT, dp(18)), new LinearLayout.LayoutParams(dp(22), dp(22)));
        profileCard.setOnClickListener(view -> {
            activeTab = "profile";
            renderPage();
        });
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, -2);
        profileParams.topMargin = dp(9);
        page.addView(profileCard, profileParams);

        addSectionLabel(page, "ГОЛОСОВЫЕ КОМНАТЫ");
        LinearLayout voiceCard = settingsCard();
        voiceCard.setPadding(dp(18), dp(17), dp(18), dp(17));
        voiceCard.addView(text("Только общий и ваш город", 15, Color.WHITE, true));
        String voiceServerNote = "Голосовой сервер пока не подключён — реальные участники не отображаются. Кружки в комнате работают как локальная демо-арена; голос не передаётся другим пользователям.";
        String noiseNote = isNoiseSuppressorAvailable() ? "Системное шумоподавление включается вместе с микрофоном." : "На этом телефоне системное шумоподавление недоступно.";
        TextView voiceDetail = text("В списке будут две комнаты: общий канал и «" + cityName() + "». Выбор города меняет городскую комнату.\n\n" + voiceServerNote + "\n" + noiseNote, 12, MUTED, false);
        voiceDetail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams voiceDetailParams = new LinearLayout.LayoutParams(-1, -2);
        voiceDetailParams.topMargin = dp(6);
        voiceCard.addView(voiceDetail, voiceDetailParams);
        LinearLayout.LayoutParams voiceParams = new LinearLayout.LayoutParams(-1, -2);
        voiceParams.topMargin = dp(9);
        page.addView(voiceCard, voiceParams);

        addSectionLabel(page, "ПРИЛОЖЕНИЕ");
        LinearLayout appCard = settingsCard();
        addSettingRow(appCard, "Медиатека", videos.size() == 0 ? "Видео хранятся только на телефоне" : videos.size() + " видео · только на телефоне", "▣", view -> new AlertDialog.Builder(this).setTitle("Локальная медиатека").setMessage("Выбранные видео хранятся в приватной памяти этого телефона и не публикуются на сервере.").setPositiveButton("Понятно", null).show());
        addSettingRow(appCard, "Обновить приложение", "Проверить новую версию", "↻", view -> checkForUpdates());
        addSettingRow(appCard, "О приложении", "Курьерка · Android", "ⓘ", view -> new AlertDialog.Builder(this).setTitle("Курьерка").setMessage("Нативное Android-приложение. Голосовые соединения между телефонами пока требуют серверной части.").setPositiveButton("Понятно", null).show());
        LinearLayout.LayoutParams appParams = new LinearLayout.LayoutParams(-1, -2);
        appParams.topMargin = dp(9);
        appParams.bottomMargin = dp(18);
        page.addView(appCard, appParams);

        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        contentFrame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    }

    private LinearLayout settingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));
        card.setBackground(round(SURFACE, 20));
        return card;
    }

    private void addSectionLabel(LinearLayout parent, String label) {
        TextView title = text(label, 10, MUTED, true);
        title.setLetterSpacing(.11f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(20);
        params.leftMargin = dp(4);
        parent.addView(title, params);
    }

    private void showCityPicker() {
        Dialog dialog = new Dialog(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(16));
        card.setBackground(round(SURFACE, 26));
        TextView eyebrow = text("КУРЬЕРКА  ·  ГОРОД", 10, ACCENT, true);
        eyebrow.setLetterSpacing(.1f);
        card.addView(eyebrow);
        TextView title = text("Выберите свой город", 20, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        card.addView(title, titleParams);
        TextView subtitle = text("Городская голосовая будет только для вашего города", 12, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        subtitleParams.bottomMargin = dp(14);
        card.addView(subtitle, subtitleParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("Найти город");
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(round(SURFACE_2, 15));
        card.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, dp(390));
        scrollParams.topMargin = dp(10);
        card.addView(scroll, scrollParams);
        populateCityRows(list, "", dialog);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) { populateCityRows(list, value.toString(), dialog); }
            @Override public void afterTextChanged(Editable value) { }
        });
        dialog.setContentView(card);
        dialog.setCancelable(true);
        dialog.show();
        styleDialogWindow(dialog);
    }

    private void populateCityRows(LinearLayout list, String query, Dialog picker) {
        list.removeAllViews();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        int found = 0;
        for (int i = 0; i < CITY_NAMES.length; i++) {
            final int cityIndex = i;
            String name = CITY_NAMES[i];
            if (!normalized.isEmpty() && !name.toLowerCase(Locale.ROOT).replace('ё', 'е').contains(normalized)) continue;
            found++;
            boolean selected = cityKey(i).equals(cityId);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), 0, dp(12), 0);
            row.setBackground(ripple(round(selected ? 0x22FFCF22 : Color.TRANSPARENT, 15)));
            TextView marker = text(selected ? "●" : "○", 18, selected ? ACCENT : MUTED, true);
            marker.setGravity(Gravity.CENTER);
            row.addView(marker, new LinearLayout.LayoutParams(dp(30), dp(46)));
            TextView label = text(name, 14, selected ? Color.WHITE : 0xFFE6E0D4, selected);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1f);
            labelParams.leftMargin = dp(9);
            row.addView(label, labelParams);
            row.setOnClickListener(view -> {
                setCity(cityKey(cityIndex));
                picker.dismiss();
            });
            list.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        if (found == 0) {
            TextView empty = text("Такого города нет в списке — укажите его вручную ниже.", 13, MUTED, false);
            empty.setPadding(dp(12), dp(14), dp(12), dp(14));
            list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        }
        TextView custom = text("＋   Указать другой город", 14, ACCENT, true);
        custom.setGravity(Gravity.CENTER_VERTICAL);
        custom.setPadding(dp(12), dp(4), dp(12), dp(4));
        custom.setBackground(ripple(round(0x1AFFCF22, 15)));
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(-1, dp(50));
        customParams.topMargin = dp(6);
        list.addView(custom, customParams);
        custom.setOnClickListener(view -> {
            picker.dismiss();
            showCustomCityDialog();
        });
    }

    private void showCustomCityDialog() {
        LinearLayout card = updateDialogCard("ВАШ ГОРОД", "Укажите город", "Название сохранится на этом устройстве.");
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(MUTED);
        input.setHint("Например, Тверь");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(SURFACE_2, 14));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(52));
        inputParams.topMargin = dp(16);
        card.addView(input, inputParams);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = pillButton("Отмена", SURFACE_2, Color.WHITE, 13);
        TextView save = pillButton("Сохранить", ACCENT, INK, 13);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        saveParams.leftMargin = dp(10);
        buttons.addView(save, saveParams);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.topMargin = dp(16);
        card.addView(buttons, buttonParams);
        Dialog dialog = createRoundedDialog(card);
        cancel.setOnClickListener(view -> dialog.dismiss());
        save.setOnClickListener(view -> {
            String name = input.getText().toString().trim().replaceAll("\\s+", " ");
            if (name.length() < 2) {
                input.setError("Введите название города");
                return;
            }
            if (name.length() > 48) {
                input.setError("Слишком длинное название");
                return;
            }
            dialog.dismiss();
            setCustomCity(name);
        });
    }

    private void setCity(String newCityId) {
        int index = cityIndex(newCityId);
        if (index < 0) return;
        String canonicalId = cityKey(index);
        if (canonicalId.equals(cityId)) return;
        cityId = canonicalId;
        customCityName = "";
        getSharedPreferences("ekip_native", MODE_PRIVATE).edit().putString("city_id", cityId).remove("custom_city_name").apply();
        onCityChanged();
    }

    private void setCustomCity(String name) {
        String normalized = name.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return;
        int catalogIndex = cityIndex(normalized);
        if (catalogIndex >= 0) {
            setCity(cityKey(catalogIndex));
            return;
        }
        cityId = customCityKey(normalized);
        customCityName = normalized;
        getSharedPreferences("ekip_native", MODE_PRIVATE).edit()
                .putString("city_id", cityId)
                .putString("custom_city_name", customCityName)
                .apply();
        onCityChanged();
    }

    private void onCityChanged() {
        rebuildAccessibleRooms();
        if (joinedRoomId != null && !"common".equals(joinedRoomId) && !joinedRoomId.equals(cityId)) closeVoiceRoom();
        Toast.makeText(this, "Город: " + cityName(), Toast.LENGTH_SHORT).show();
        renderPage();
    }

    private boolean isNoiseSuppressorAvailable() {
        try { return NoiseSuppressor.isAvailable(); } catch (Exception ignored) { return false; }
    }

    private boolean hasMicrophonePermission() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        if (hasMicrophonePermission()) return;
        SharedPreferences prefs = getSharedPreferences("ekip_native", MODE_PRIVATE);
        boolean requestedBefore = prefs.getBoolean("microphone_permission_requested", false);
        boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
        if (requestedBefore && !showRationale) {
            showUpdateInfoDialog("Микрофон выключен", "Разрешение было отклонено. Включите доступ к микрофону в настройках Android, чтобы использовать голосовые функции.", "Открыть настройки", this::openAppPermissionSettings);
            return;
        }
        if (showRationale) {
                showUpdateInfoDialog("Нужен доступ к микрофону", "Разрешите микрофон для голосовых комнат. Курьерка не включает запись в фоне.", "Продолжить", () -> {
                prefs.edit().putBoolean("microphone_permission_requested", true).apply();
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_REQUEST);
            });
            return;
        }
        prefs.edit().putBoolean("microphone_permission_requested", true).apply();
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_REQUEST);
    }

    private void openAppPermissionSettings() {
        try {
            Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(settings);
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть настройки приложения", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isInPictureInPicture() {
        return Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode();
    }

    private void toggleMicrophone() {
        if (microphoneEnabled) {
            microphoneEnabled = false;
            stopMicrophoneCapture();
            playMicCue(false);
            if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
            if (isInPictureInPicture()) {
                updatePictureInPictureMicButton();
                refreshPictureInPictureActions();
            } else renderPage();
            return;
        }
        if (!hasMicrophonePermission()) {
            requestMicrophonePermission();
            return;
        }
        if (!startMicrophoneCapture()) {
            Toast.makeText(this, "Не удалось включить микрофон. Проверьте разрешение и настройки телефона.", Toast.LENGTH_LONG).show();
            return;
        }
        microphoneEnabled = true;
        playMicCue(true);
        if (isInPictureInPicture()) {
            updatePictureInPictureMicButton();
            refreshPictureInPictureActions();
        } else renderPage();
    }

    private boolean startMicrophoneCapture() {
        if (!hasMicrophonePermission()) return false;
        int minBuffer;
        try { minBuffer = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); }
        catch (Exception error) { return false; }
        if (minBuffer <= 0) return false;
        int bufferBytes = Math.max(minBuffer, 4096);
        AudioRecord recorder = null;
        NoiseSuppressor suppressor = null;
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes * 2);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                recorder.release();
                return false;
            }
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                recorder.release();
                return false;
            }
            if (NoiseSuppressor.isAvailable()) {
                try {
                    suppressor = NoiseSuppressor.create(recorder.getAudioSessionId());
                    if (suppressor != null) suppressor.setEnabled(true);
                } catch (Exception ignored) { suppressor = null; }
            }
            microphoneRecorder = recorder;
            microphoneNoiseSuppressor = suppressor;
            microphoneCaptureRunning.set(true);
            AudioRecord activeRecorder = recorder;
            NoiseSuppressor activeSuppressor = suppressor;
            Thread meter = new Thread(() -> {
                short[] samples = new short[Math.max(1024, bufferBytes / 2)];
                long lastUiUpdate = 0;
                float noiseFloor = 180f;
                int speechFrames = 0;
                int quietFrames = 0;
                boolean speechDetected = false;
                try {
                    while (microphoneCaptureRunning.get()) {
                        int read = activeRecorder.read(samples, 0, samples.length);
                        if (read < 0) break;
                        if (read == 0) continue;
                        double sum = 0;
                        for (int i = 0; i < read; i++) {
                            double value = samples[i];
                            sum += value * value;
                        }
                        float rms = (float) Math.sqrt(sum / read);
                        float startThreshold = Math.max(850f, noiseFloor * 2.6f);
                        float stopThreshold = Math.max(520f, noiseFloor * 1.6f);
                        if (!speechDetected) {
                            if (rms >= startThreshold) speechFrames++;
                            else {
                                speechFrames = 0;
                                noiseFloor = noiseFloor * .96f + Math.min(rms, 900f) * .04f;
                            }
                            if (speechFrames >= 2) {
                                speechDetected = true;
                                quietFrames = 0;
                            }
                        } else if (rms < stopThreshold) {
                            if (++quietFrames >= 2) {
                                speechDetected = false;
                                speechFrames = 0;
                            }
                        } else quietFrames = 0;
                        float level = speechDetected ? Math.min(1f, .18f + (rms - stopThreshold) / 6500f) : 0f;
                        long now = SystemClock.uptimeMillis();
                        if (now - lastUiUpdate >= 70) {
                            lastUiUpdate = now;
                            final boolean speakingNow = speechDetected;
                            final float levelNow = level;
                            handler.post(() -> {
                                if (microphoneEnabled && microphoneRecorder == activeRecorder && voiceArenaView != null) voiceArenaView.setSpeechLevel(speakingNow, levelNow);
                            });
                        }
                    }
                } catch (Exception ignored) { }
                finally {
                    try {
                        if (activeRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) activeRecorder.stop();
                    } catch (Exception ignored) { }
                    try { activeRecorder.release(); } catch (Exception ignored) { }
                    if (activeSuppressor != null) try { activeSuppressor.release(); } catch (Exception ignored) { }
                    if (microphoneNoiseSuppressor == activeSuppressor) microphoneNoiseSuppressor = null;
                    if (microphoneRecorder == activeRecorder) {
                        microphoneRecorder = null;
                        microphoneCaptureRunning.set(false);
                        handler.post(() -> {
                            if (microphoneEnabled) {
                                microphoneEnabled = false;
                                if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
                                Toast.makeText(MainActivity.this, "Микрофон остановлен системой", Toast.LENGTH_SHORT).show();
                                if (isInPictureInPicture()) {
                                    updatePictureInPictureMicButton();
                                    refreshPictureInPictureActions();
                                } else renderPage();
                            }
                        });
                    }
                }
            }, "courier-microphone-meter");
            microphoneMeterThread = meter;
            meter.start();
            return true;
        } catch (Exception error) {
            microphoneCaptureRunning.set(false);
            microphoneRecorder = null;
            microphoneNoiseSuppressor = null;
            if (suppressor != null) try { suppressor.release(); } catch (Exception ignored) { }
            if (recorder != null) try { recorder.release(); } catch (Exception ignored) { }
            return false;
        }
    }

    private void stopMicrophoneCapture() {
        microphoneCaptureRunning.set(false);
        AudioRecord recorder = microphoneRecorder;
        microphoneRecorder = null;
        microphoneMeterThread = null;
        if (recorder != null) {
            try {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) recorder.stop();
            } catch (Exception ignored) { }
        }
    }

    private void closeVoiceRoom() {
        boolean wasInRoom = joinedRoomId != null;
        if (microphoneEnabled) microphoneEnabled = false;
        stopMicrophoneCapture();
        if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
        joinedRoomId = null;
        if (wasInRoom) playVoiceCue(false);
    }

    private void leaveVoiceRoom() {
        closeVoiceRoom();
        activeTab = "rooms";
        renderPage();
    }

    private void playVoiceCue(boolean connected) {
        playChime(connected ? new int[]{349, 392, 440, 523} : new int[]{523, 440, 392, 330}, connected ? 1150 : 1250);
    }

    private void playMicCue(boolean enabled) {
        playChime(enabled ? new int[]{440, 494, 587} : new int[]{587, 494, 370}, enabled ? 850 : 920);
    }

    private void playChime(int[] notes, int durationMs) {
        handler.removeCallbacks(releaseFeedbackAudioTrack);
        releaseFeedbackAudioTrack.run();
        int sampleRate = 24000;
        int sampleCount = sampleRate * durationMs / 1000;
        short[] pcm = new short[sampleCount];
        double phase = 0;
        float secondsTotal = durationMs / 1000f;
        for (int i = 0; i < sampleCount; i++) {
            float seconds = i / (float) sampleRate;
            float notePosition = seconds / secondsTotal * notes.length;
            int noteIndex = Math.min(notes.length - 1, (int) notePosition);
            float withinNote = notePosition - noteIndex;
            float glide = Math.max(0f, Math.min(1f, (withinNote - .68f) / .32f));
            glide = glide * glide * (3f - 2f * glide);
            float frequency = notes[noteIndex];
            if (noteIndex + 1 < notes.length) {
                frequency += (notes[noteIndex + 1] - frequency) * glide;
            }
            phase += 2.0 * Math.PI * frequency / sampleRate;
            float attack = Math.max(0f, Math.min(1f, seconds / .16f));
            attack = attack * attack * (3f - 2f * attack);
            float release = Math.max(0f, Math.min(1f, (secondsTotal - seconds) / .3f));
            release = release * release * (3f - 2f * release);
            float envelope = attack * release;
            double tone = Math.sin(phase) + .075 * Math.sin(phase * 2.0) + .018 * Math.sin(phase * 3.0);
            pcm[i] = (short) (tone * envelope * 7600f);
        }
        AudioTrack track = null;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();
            track = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            int written = 0;
            while (written < pcm.length) {
                int count = track.write(pcm, written, pcm.length - written);
                if (count <= 0) throw new IllegalStateException("Audio cue could not be written");
                written += count;
            }
            track.setVolume(.48f);
            feedbackAudioTrack = track;
            track.play();
            handler.postDelayed(releaseFeedbackAudioTrack, durationMs + 100L);
        } catch (Exception ignored) {
            if (feedbackAudioTrack == track) feedbackAudioTrack = null;
            if (track != null) try { track.release(); } catch (Exception ignoredAgain) { }
        }
    }

    private void addSettingRow(LinearLayout parent, String title, String detail, String glyph, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(68));
        row.setPadding(0, dp(12), 0, dp(12));
        TextView icon = text(glyph, 21, ACCENT, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(0x22FFCF22, 12));
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        copy.setPadding(dp(10), 0, dp(4), 0);
        TextView titleView = text(title, 15, Color.WHITE, true);
        copy.addView(titleView, new LinearLayout.LayoutParams(-1, -2));
        TextView sub = text(detail, 12, MUTED, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(3);
        copy.addView(sub, subParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        IconView arrow = new IconView(this, "next", MUTED, dp(18));
        row.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(24)));
        row.setOnClickListener(action);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        View divider = new View(this);
        divider.setBackgroundColor(0xFF403A2E);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void checkForUpdates() {
        if (checkingForUpdate) return;
        checkingForUpdate = true;
        showUpdateProgressDialog("Проверяем версию", "Подключаемся к серверу обновлений", true);
        ioExecutor.execute(() -> {
            try {
                JSONObject metadata = fetchJson(UPDATE_MANIFEST_URL + "?t=" + System.currentTimeMillis());
                if (!metadata.optBoolean("persistentSigning", false)) {
                    throw new IllegalStateException("Для безопасного обновления ещё не настроена постоянная подпись.");
                }
                long latestCode = metadata.getLong("versionCode");
                PackageInfo installed = getPackageManager().getPackageInfo(getPackageName(), 0);
                long installedCode = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
                if (latestCode <= installedCode) {
                    handler.post(() -> {
                        checkingForUpdate = false;
                        dismissUpdateProgressDialog();
                        showUpdateInfoDialog("У вас актуальная версия", "Курьерка уже обновлена.", "Понятно", null);
                    });
                    return;
                }
                String apkUrl = metadata.getString("apkUrl");
                String expectedSha = metadata.getString("sha256").toLowerCase(Locale.ROOT);
                if (!apkUrl.startsWith(UPDATE_APK_PREFIX) || !expectedSha.matches("[0-9a-f]{64}")) {
                    throw new IllegalStateException("Сервер вернул некорректные данные обновления.");
                }
                String versionName = metadata.optString("versionName", "новая версия");
                handler.post(() -> setUpdateProgressStage("Скачиваем Курьерку " + versionName, "Подготавливаем безопасную загрузку", true));
                File apk = downloadUpdate(apkUrl, expectedSha, (downloaded, total) -> handler.post(() -> updateDownloadProgress(downloaded, total)));
                handler.post(() -> {
                    checkingForUpdate = false;
                    dismissUpdateProgressDialog();
                    showUpdateReadyDialog(apk, versionName);
                });
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Проверьте соединение и попробуйте ещё раз." : error.getMessage();
                handler.post(() -> {
                    checkingForUpdate = false;
                    dismissUpdateProgressDialog();
                    showUpdateInfoDialog("Не удалось проверить обновление", message, "Понятно", null);
                });
            }
        });
    }

    private void showUpdateProgressDialog(String title, String detail, boolean indeterminate) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(20));
        card.setBackground(round(SURFACE, 26));
        TextView brand = text("КУРЬЕРКА  ·  ОБНОВЛЕНИЕ", 10, ACCENT, true);
        brand.setLetterSpacing(.1f);
        card.addView(brand);
        updateProgressTitle = text(title, 19, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(10);
        card.addView(updateProgressTitle, titleParams);
        updateProgressDetail = text(detail, 12, MUTED, false);
        updateProgressDetail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(6);
        detailParams.bottomMargin = dp(17);
        card.addView(updateProgressDetail, detailParams);
        updateProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        updateProgressBar.setMax(100);
        updateProgressBar.setIndeterminate(indeterminate);
        updateProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        updateProgressBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        updateProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(SURFACE_2));
        card.addView(updateProgressBar, new LinearLayout.LayoutParams(-1, dp(6)));
        updateProgressDialog = new Dialog(this);
        updateProgressDialog.setContentView(card);
        updateProgressDialog.setCancelable(false);
        updateProgressDialog.show();
        styleDialogWindow(updateProgressDialog);
    }

    private void setUpdateProgressStage(String title, String detail, boolean indeterminate) {
        if (updateProgressTitle == null || updateProgressDetail == null || updateProgressBar == null) return;
        updateProgressTitle.setText(title);
        updateProgressDetail.setText(detail);
        updateProgressBar.setIndeterminate(indeterminate);
        if (!indeterminate) updateProgressBar.setProgress(0);
    }

    private void updateDownloadProgress(long downloaded, long total) {
        if (updateProgressBar == null || updateProgressDetail == null) return;
        if (total > 0) {
            updateProgressBar.setIndeterminate(false);
            updateProgressBar.setProgress((int) Math.min(100, downloaded * 100 / total));
            updateProgressDetail.setText(formatBytes(downloaded) + " из " + formatBytes(total));
        } else {
            updateProgressBar.setIndeterminate(true);
            updateProgressDetail.setText("Загружено " + formatBytes(downloaded));
        }
    }

    private String formatBytes(long value) {
        if (value < 1024L * 1024L) return Math.max(1, value / 1024L) + " КБ";
        return String.format(Locale.getDefault(), "%.1f МБ", value / (1024f * 1024f));
    }

    private void dismissUpdateProgressDialog() {
        if (updateProgressDialog != null) {
            try { updateProgressDialog.dismiss(); } catch (Exception ignored) { }
        }
        updateProgressDialog = null;
        updateProgressTitle = null;
        updateProgressDetail = null;
        updateProgressBar = null;
    }

    private void showUpdateReadyDialog(File apk, String versionName) {
        LinearLayout card = updateDialogCard("ОБНОВЛЕНИЕ ГОТОВО", "Курьерка " + versionName,
                "Файл загружен и проверен. Android попросит подтвердить установку.");
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView later = pillButton("Позже", SURFACE_2, Color.WHITE, 13);
        TextView install = pillButton("Установить", ACCENT, INK, 13);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        buttons.addView(later, buttonParams);
        LinearLayout.LayoutParams installParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        installParams.leftMargin = dp(10);
        buttons.addView(install, installParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(20);
        card.addView(buttons, buttonsParams);
        Dialog dialog = createRoundedDialog(card);
        later.setOnClickListener(view -> dialog.dismiss());
        install.setOnClickListener(view -> {
            dialog.dismiss();
            requestUpdateInstallPermission(apk);
        });
    }

    private LinearLayout updateDialogCard(String eyebrow, String title, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(20));
        card.setBackground(round(SURFACE, 26));
        TextView label = text(eyebrow, 10, ACCENT, true);
        label.setLetterSpacing(.1f);
        card.addView(label);
        TextView heading = text(title, 19, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(10);
        card.addView(heading, headingParams);
        TextView detail = text(message, 13, MUTED, false);
        detail.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(7);
        card.addView(detail, detailParams);
        return card;
    }

    private void showUpdateInfoDialog(String title, String message, String buttonText, Runnable action) {
        LinearLayout card = updateDialogCard("КУРЬЕРКА", title, message);
        TextView button = pillButton(buttonText, ACCENT, INK, 13);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(48));
        buttonParams.topMargin = dp(20);
        card.addView(button, buttonParams);
        Dialog dialog = createRoundedDialog(card);
        button.setOnClickListener(view -> {
            dialog.dismiss();
            if (action != null) action.run();
        });
    }

    private Dialog createRoundedDialog(LinearLayout card) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(card);
        dialog.setCancelable(true);
        dialog.show();
        styleDialogWindow(dialog);
        return dialog;
    }

    private void styleDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(390));
        window.setLayout(Math.max(dp(280), width), WindowManager.LayoutParams.WRAP_CONTENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = .48f;
        window.setAttributes(attributes);
    }

    private void requestUpdateInstallPermission(File apk) {
        if (apk == null || !apk.isFile()) {
            showUpdateInfoDialog("Файл не найден", "Скачайте обновление ещё раз.", "Понятно", null);
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            showUpdateInfoDialog("Разрешите установку", "Android один раз попросит разрешить Курьерке устанавливать обновления. После этого вернитесь в приложение.", "Открыть настройки", () -> {
                try {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(settings, UPDATE_PERMISSION_REQUEST);
                } catch (Exception error) {
                    showUpdateInfoDialog("Не удалось открыть настройки", "Разрешите установку APK для Курьерки в настройках Android.", "Понятно", null);
                }
            });
            return;
        }
        launchPackageInstaller(apk);
    }

    private void launchPackageInstaller(File apk) {
        if (apk == null || !apk.isFile()) {
            showUpdateInfoDialog("Файл не найден", "Проверьте обновление ещё раз.", "Понятно", null);
            return;
        }
        try {
            Uri packageUri = Uri.parse("content://ru.ekip.couriers.updates/ekip-update.apk");
            Intent installer = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            installer.setDataAndType(packageUri, "application/vnd.android.package-archive");
            installer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installer.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(installer, UPDATE_INSTALL_REQUEST);
        } catch (Exception error) {
            String message = error.getMessage() == null ? "Не удалось открыть установщик Android." : error.getMessage();
            showUpdateInfoDialog("Не удалось установить", message, "Понятно", null);
        }
    }

    private JSONObject fetchJson(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-cache");
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > 64 * 1024) throw new IllegalStateException("Слишком большой файл обновления");
                output.write(buffer, 0, count);
            }
            return new JSONObject(new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private File downloadUpdate(String address, String expectedSha, UpdateDownloadListener listener) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Сервер не отдал APK (HTTP " + responseCode + ").");
        }
        long expectedLength = connection.getContentLengthLong();
        File apk = new File(getCacheDir(), "ekip-update.apk");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(apk)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            long total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > 100L * 1024L * 1024L) throw new IllegalStateException("Размер APK превышает допустимый лимит.");
                digest.update(buffer, 0, count);
                output.write(buffer, 0, count);
                if (listener != null) listener.onProgress(total, expectedLength);
            }
            output.getFD().sync();
        } catch (Exception error) {
            apk.delete();
            throw error;
        } finally {
            connection.disconnect();
        }
        StringBuilder actualSha = new StringBuilder();
        for (byte value : digest.digest()) actualSha.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        if (!expectedSha.equals(actualSha.toString())) {
            apk.delete();
            throw new IllegalStateException("Контрольная сумма APK не совпала; установка отменена.");
        }
        return apk;
    }

    private LinearLayout pageColumn() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        page.setBackgroundColor(INK);
        return page;
    }

    private void addPageHeader(LinearLayout parent, String title, String subtitle) {
        TextView titleView = text(title, 29, Color.WHITE, true);
        parent.addView(titleView);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = text(subtitle, 13, MUTED, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(6);
            params.bottomMargin = dp(6);
            parent.addView(sub, params);
        }
    }

    private void openVideoPicker() {
        if (copyingVideo) return;
        Intent intent;
        if (Build.VERSION.SDK_INT >= 33) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("video/*");
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            if (Build.VERSION.SDK_INT >= 33) startActivityForResult(intent, PICK_VIDEO_REQUEST);
            else startActivityForResult(Intent.createChooser(intent, "Выберите видео"), PICK_VIDEO_REQUEST);
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть выбор видео", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            copyProfileAvatar(data.getData());
            return;
        }
        if (requestCode == UPDATE_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls()) {
                launchPackageInstaller(new File(getCacheDir(), "ekip-update.apk"));
            } else {
                showUpdateInfoDialog("Разрешение не включено", "Без этого разрешения Android не позволит Курьерке открыть установщик.", "Понятно", null);
            }
            return;
        }
        if (requestCode == UPDATE_INSTALL_REQUEST) {
            if (resultCode == RESULT_OK) showUpdateInfoDialog("Обновление установлено", "Курьерка обновлена.", "Готово", null);
            else if (resultCode == RESULT_CANCELED) showUpdateInfoDialog("Установка не завершена", "Установщик Android был закрыт или отменён.", "Понятно", null);
            return;
        }
        if (requestCode != PICK_VIDEO_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        promptVideoTitle(data.getData());
    }

    private void promptVideoTitle(Uri uri) {
        LinearLayout card = updateDialogCard("НОВОЕ ВИДЕО", "Добавьте подпись", "Название обязательно. Можно добавить хэштеги — до 200 символов.");
        EditText titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setTextSize(15);
        titleInput.setTextColor(Color.WHITE);
        titleInput.setHintTextColor(MUTED);
        titleInput.setHint("Напишите название видео");
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        titleInput.setPadding(dp(14), 0, dp(14), 0);
        titleInput.setBackground(round(SURFACE_2, 14));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(52));
        inputParams.topMargin = dp(16);
        card.addView(titleInput, inputParams);
        TextView count = text("0 / 200", 10, MUTED, false);
        count.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-1, dp(24));
        card.addView(count, countParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = pillButton("Отмена", SURFACE_2, Color.WHITE, 13);
        TextView upload = pillButton("Добавить видео", ACCENT, INK, 13);
        upload.setEnabled(false);
        upload.setAlpha(.45f);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        uploadParams.leftMargin = dp(9);
        buttons.addView(upload, uploadParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(10);
        card.addView(buttons, buttonsParams);

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int countValue) {
                count.setText(value.length() + " / 200");
                boolean valid = !value.toString().trim().isEmpty();
                upload.setEnabled(valid);
                upload.setAlpha(valid ? 1f : .45f);
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        Dialog dialog = createRoundedDialog(card);
        cancel.setOnClickListener(view -> dialog.dismiss());
        upload.setOnClickListener(view -> {
            String title = titleInput.getText().toString().trim().replaceAll("\\s+", " ");
            if (title.isEmpty()) {
                titleInput.setError("Название обязательно");
                return;
            }
            dialog.dismiss();
            startVideoImport(uri, title);
        });
    }

    private void startVideoImport(Uri uri, String title) {
        final String videoTitle = title;
        final long uploadedAt = System.currentTimeMillis();
        copyingVideo = true;
        showLoadingOverlay();
        ioExecutor.execute(() -> {
            File output = null;
            try {
                File folder = new File(getFilesDir(), "videos");
                if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Не удалось создать папку видео");
                output = new File(folder, "ekip-" + System.currentTimeMillis() + ".video");
                try (InputStream input = getContentResolver().openInputStream(uri); FileOutputStream target = new FileOutputStream(output)) {
                    if (input == null) throw new IllegalStateException("Не удалось прочитать выбранное видео");
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    long total = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > 1024L * 1024L * 1024L) throw new IllegalStateException("Размер видео превышает 1 ГБ");
                        target.write(buffer, 0, count);
                    }
                }
                File finalOutput = output;
                handler.post(() -> {
                    videos.add(0, new VideoItem(finalOutput.getAbsolutePath(), videoTitle, uploadedAt));
                    currentVideoIndex = 0;
                    persistVideos();
                    copyingVideo = false;
                    renderPage();
                    Toast.makeText(this, "Видео добавлено", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                if (output != null) output.delete();
                handler.post(() -> {
                    copyingVideo = false;
                    removeLoadingOverlay();
                    Toast.makeText(this, "Не удалось сохранить видео", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MICROPHONE_PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        getSharedPreferences("ekip_native", MODE_PRIVATE).edit().putBoolean("microphone_permission_requested", true).apply();
        if (granted) {
            Toast.makeText(this, "Доступ к микрофону разрешён", Toast.LENGTH_SHORT).show();
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            showUpdateInfoDialog("Микрофон выключен", "Разрешите доступ в настройках Android, чтобы использовать микрофон в голосовых комнатах.", "Открыть настройки", this::openAppPermissionSettings);
        } else {
            Toast.makeText(this, "Без доступа к микрофону говорить в голосовой комнате нельзя", Toast.LENGTH_LONG).show();
        }
        if ("rooms".equals(activeTab)) {
            if (isInPictureInPicture()) updatePictureInPictureMicButton();
            else renderPage();
        }
    }

    private void showLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setTag("copying_video_overlay");
        overlay.setBackgroundColor(0xBB090806);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackground(round(SURFACE, 22));
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        box.addView(progress, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView label = text("Сохраняю видео…", 14, Color.WHITE, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.topMargin = dp(14);
        box.addView(label, labelParams);
        overlay.addView(box, centered());
        contentFrame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void removeLoadingOverlay() {
        View overlay = contentFrame.findViewWithTag("copying_video_overlay");
        if (overlay != null) contentFrame.removeView(overlay);
    }

    private void persistVideos() {
        JSONArray array = new JSONArray();
        for (VideoItem item : videos) {
            JSONObject object = new JSONObject();
            try {
                object.put("path", item.path);
                object.put("title", item.title);
                object.put("date", item.date);
                array.put(object);
            } catch (Exception ignored) { }
        }
        getSharedPreferences("ekip_native", MODE_PRIVATE).edit().putString("videos", array.toString()).putInt("video_index", currentVideoIndex).apply();
    }

    private void showDeleteCurrentVideo() {
        if (videos.isEmpty()) return;
        VideoItem item = videos.get(currentVideoIndex);
        new AlertDialog.Builder(this)
                .setTitle("Удалить видео?")
                .setMessage("«" + item.title + "» будет удалено с этого устройства.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new File(item.path).delete();
                    videos.remove(currentVideoIndex);
                    if (currentVideoIndex >= videos.size()) currentVideoIndex = Math.max(0, videos.size() - 1);
                    persistVideos();
                    renderPage();
                }).show();
    }

    private Bitmap captureCurrentFeedFrame() {
        if (textureView == null || textureView.getWidth() <= 0 || textureView.getHeight() <= 0) return null;
        try { return textureView.getBitmap(); } catch (Exception ignored) { return null; }
    }

    private void showVideoAt(int index, int direction) {
        if (videos.isEmpty() || pendingVideoTransitionBitmap != null) return;
        int next = index;
        if (next < 0) next = videos.size() - 1;
        if (next >= videos.size()) next = 0;
        if (next == currentVideoIndex && player != null) return;
        Bitmap outgoingFrame = captureCurrentFeedFrame();
        currentVideoIndex = next;
        userWantsPlayback = true;
        persistVideos();
        if (outgoingFrame != null) {
            pendingVideoTransitionBitmap = outgoingFrame;
            pendingVideoTransitionDirection = direction;
            preservingVideoTransitionForRender = true;
        }
        renderPage();
        preservingVideoTransitionForRender = false;
        if (outgoingFrame != null && pendingVideoTransitionView == null) clearPendingVideoTransition();
    }

    private void installPendingVideoTransition(FrameLayout feed) {
        if (pendingVideoTransitionBitmap == null) return;
        ImageView outgoing = new ImageView(this);
        outgoing.setImageBitmap(pendingVideoTransitionBitmap);
        outgoing.setScaleType(ImageView.ScaleType.FIT_XY);
        outgoing.setContentDescription("Переход к следующему видео");
        int viewportWidth = Math.max(1, contentFrame == null ? feed.getWidth() : contentFrame.getWidth());
        int viewportHeight = Math.max(1, contentFrame == null ? feed.getHeight() : contentFrame.getHeight());
        float scale = Math.min(viewportWidth / (float) pendingVideoTransitionBitmap.getWidth(),
                viewportHeight / (float) pendingVideoTransitionBitmap.getHeight());
        int width = Math.max(1, Math.round(pendingVideoTransitionBitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(pendingVideoTransitionBitmap.getHeight() * scale));
        feed.addView(outgoing, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
        outgoing.bringToFront();
        pendingVideoTransitionView = outgoing;
    }

    private void animatePendingVideoTransition() {
        ImageView outgoing = pendingVideoTransitionView;
        if (outgoing == null || videoTransitionRunning) return;
        videoTransitionRunning = true;
        outgoing.post(() -> {
            if (pendingVideoTransitionView != outgoing) return;
            int height = Math.max(outgoing.getHeight(), contentFrame == null ? 0 : contentFrame.getHeight());
            if (height <= 0) height = dp(400);
            outgoing.animate()
                    .translationY(-pendingVideoTransitionDirection * height)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f))
                    .withEndAction(this::clearPendingVideoTransition)
                    .start();
        });
    }

    private void clearPendingVideoTransition() {
        ImageView outgoing = pendingVideoTransitionView;
        pendingVideoTransitionView = null;
        if (outgoing != null) {
            outgoing.animate().cancel();
            if (outgoing.getParent() instanceof ViewGroup) ((ViewGroup) outgoing.getParent()).removeView(outgoing);
            outgoing.setImageDrawable(null);
        }
        Bitmap snapshot = pendingVideoTransitionBitmap;
        pendingVideoTransitionBitmap = null;
        if (snapshot != null && !snapshot.isRecycled()) snapshot.recycle();
        pendingVideoTransitionDirection = 0;
        videoTransitionRunning = false;
    }

    private void togglePlayback() {
        if (player == null) { userWantsPlayback = true; return; }
        try {
            if (player.isPlaying()) {
                player.pause();
                userWantsPlayback = false;
                if (playOverlay != null) { playOverlay.setText("▶"); playOverlay.setVisibility(View.VISIBLE); }
            } else {
                player.start();
                userWantsPlayback = true;
                if (playOverlay != null) playOverlay.setVisibility(View.GONE);
                handler.removeCallbacks(updateProgress);
                handler.post(updateProgress);
            }
        } catch (Exception ignored) { }
        refreshPictureInPictureEligibility();
    }

    private void preparePlayer(SurfaceTexture surfaceTexture) {
        if (videos.isEmpty() || textureView == null) return;
        videoSurface = new Surface(surfaceTexture);
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.setDataSource(videos.get(currentVideoIndex).path);
            player.setSurface(videoSurface);
            player.setOnPreparedListener(mediaPlayer -> {
                videoWidth = mediaPlayer.getVideoWidth();
                videoHeight = mediaPlayer.getVideoHeight();
                applyVideoTransform();
                mediaPlayer.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
                if (seekBar != null) seekBar.setMax(Math.max(1, mediaPlayer.getDuration()));
                if (videoTime != null) videoTime.setText("0:00  /  " + formatTime(mediaPlayer.getDuration()));
                if (userWantsPlayback) {
                    mediaPlayer.start();
                    if (playOverlay != null) playOverlay.setVisibility(View.GONE);
                    handler.removeCallbacks(updateProgress);
                    handler.post(updateProgress);
                } else if (playOverlay != null) {
                    playOverlay.setText("▶");
                    playOverlay.setVisibility(View.VISIBLE);
                }
                refreshPictureInPictureEligibility();
                animatePendingVideoTransition();
            });
            player.setOnVideoSizeChangedListener((mediaPlayer, width, height) -> {
                videoWidth = width;
                videoHeight = height;
                applyVideoTransform();
            });
            player.setLooping(true);
            player.setOnErrorListener((mediaPlayer, what, extra) -> {
                if (playOverlay != null) { playOverlay.setText("!"); playOverlay.setVisibility(View.VISIBLE); }
                Toast.makeText(this, "Не удалось воспроизвести этот формат видео", Toast.LENGTH_SHORT).show();
                animatePendingVideoTransition();
                return true;
            });
            player.prepareAsync();
        } catch (Exception error) {
            releasePlayer();
            Toast.makeText(this, "Не удалось открыть видео", Toast.LENGTH_SHORT).show();
            animatePendingVideoTransition();
        }
    }

    private void applyVideoTransform() {
        if (textureView == null || videoWidth <= 0 || videoHeight <= 0) return;
        if (!(textureView.getParent() instanceof FrameLayout)) return;
        FrameLayout viewport = (FrameLayout) textureView.getParent();
        int viewportWidth = viewport.getWidth();
        int viewportHeight = viewport.getHeight();
        if (viewportWidth <= 0 || viewportHeight <= 0) return;

        int targetWidth = viewportWidth;
        int targetHeight = viewportHeight;
        if (!isInPictureInPicture()) {
            float scale = Math.min(viewportWidth / (float) videoWidth, viewportHeight / (float) videoHeight);
            targetWidth = Math.max(1, Math.round(videoWidth * scale));
            targetHeight = Math.max(1, Math.round(videoHeight * scale));
        }
        ViewGroup.LayoutParams current = textureView.getLayoutParams();
        FrameLayout.LayoutParams fitted = new FrameLayout.LayoutParams(targetWidth, targetHeight, Gravity.CENTER);
        if (current == null || current.width != targetWidth || current.height != targetHeight
                || !(current instanceof FrameLayout.LayoutParams) || ((FrameLayout.LayoutParams) current).gravity != Gravity.CENTER) {
            textureView.setLayoutParams(fitted);
        }
        textureView.setTransform(new Matrix());
        textureView.invalidate();
    }

    private void releasePlayer() {
        handler.removeCallbacks(updateProgress);
        if (player != null) {
            try { player.setOnPreparedListener(null); player.setOnVideoSizeChangedListener(null); player.setOnCompletionListener(null); player.setOnErrorListener(null); player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        if (videoSurface != null) {
            try { videoSurface.release(); } catch (Exception ignored) { }
            videoSurface = null;
        }
        textureView = null;
        playOverlay = null;
        seekBar = null;
        videoTime = null;
        pipFeedTopBar = pipFeedGradientTop = pipFeedGradientBottom = pipFeedDetails = pipFeedSideControls = pipFeedPlaybackControls = null;
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if ("feed".equals(activeTab) && !videos.isEmpty()) preparePlayer(surface);
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { applyVideoTransform(); }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { releasePlayer(); return true; }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }

    private void rebuildAccessibleRooms() {
        rooms.clear();
        rooms.add(new RoomItem("common", "Общий голосовой", "Открыт для всех городов", ACCENT, true));
        rooms.add(new RoomItem(cityId, cityName(), "Только для курьеров вашего города", ACCENT_2, false));
    }

    private int cityIndex(String id) {
        if (id == null) return -1;
        String value = id.trim();
        if (value.startsWith("city:")) value = value.substring("city:".length());
        else if (value.startsWith("city-")) {
            try {
                int index = Integer.parseInt(value.substring("city-".length()));
                return index >= 0 && index < CITY_NAMES.length ? index : -1;
            } catch (NumberFormatException ignored) { return -1; }
        }
        if ("spb".equals(value)) value = "Санкт-Петербург";
        else if ("moscow".equals(value)) value = "Москва";
        else if ("kazan".equals(value)) value = "Казань";
        else if ("ekb".equals(value)) value = "Екатеринбург";
        else if ("novosibirsk".equals(value)) value = "Новосибирск";
        else if ("krasnodar".equals(value)) value = "Краснодар";
        else if ("nizhny".equals(value)) value = "Нижний Новгород";
        else if ("vladivostok".equals(value)) value = "Владивосток";
        for (int i = 0; i < CITY_NAMES.length; i++) if (CITY_NAMES[i].equalsIgnoreCase(value)) return i;
        return -1;
    }

    private String cityKey(int index) { return "city:" + CITY_NAMES[index].toLowerCase(Locale.ROOT); }

    private String customCityKey(String name) {
        return "custom:" + name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String cityName() {
        if (cityId != null && cityId.startsWith("custom:") && customCityName != null && !customCityName.trim().isEmpty()) return customCityName;
        int index = cityIndex(cityId);
        if (index >= 0) return CITY_NAMES[index];
        int defaultIndex = cityIndex("spb");
        return CITY_NAMES[defaultIndex < 0 ? 0 : defaultIndex];
    }

    private RoomItem findRoom(String id) {
        if (id == null) return null;
        for (RoomItem room : rooms) if (room.id.equals(id)) return room;
        return null;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView pillButton(String label, int background, int foreground, float size) {
        TextView view = text(label, size, foreground, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(ripple(round(background, 15)));
        return view;
    }

    private TextView roundIconButton(String glyph, int background, int foreground, int sizeDp) {
        TextView view = new TextView(this);
        view.setGravity(Gravity.CENTER);
        view.setBackground(ripple(round(background, sizeDp / 2)));
        view.setTag(glyph);
        view.setTextColor(foreground);
        view.setText(glyph.equals("add") ? "+" : glyph.equals("pause") ? "Ⅱ" : glyph.equals("back") ? "‹" : glyph.equals("more") ? "···" : glyph.equals("mute") ? "×" : glyph.equals("sound") ? "♪" : "");
        view.setTextSize(glyph.equals("more") ? 20 : 18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setVisibility(View.VISIBLE);
        view.setContentDescription(glyph);
        return view;
    }

    private class VoiceArenaView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final ArrayList<ArenaBubble> bubbles = new ArrayList<>();
        private final float mainRadius = dp(37);
        private Bitmap mainAvatar;
        private boolean speaking;
        private boolean compactPictureInPicture;
        private float voiceLevel;
        private long speechAnimationStarted = SystemClock.uptimeMillis();
        private long previousFrameTime;
        private long lastTouchTime;
        private float lastTouchX, lastTouchY;
        private float dragVelocityX, dragVelocityY;
        private ArenaBubble draggedBubble;
        private boolean animating;

        VoiceArenaView(Activity context) {
            super(context);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStrokeCap(Paint.Cap.ROUND);
            if (!profileAvatarPath.isEmpty()) mainAvatar = decodeAvatar(profileAvatarPath, dp(78));
            setContentDescription("Локальная игровая арена с аватарами. Перетаскивайте маленькие кружочки.");
        }

        void setCompactPictureInPicture(boolean compact) {
            compactPictureInPicture = compact;
            invalidate();
        }

        void setSpeechLevel(boolean voiceDetected, float value) {
            if (!speaking && voiceDetected) speechAnimationStarted = SystemClock.uptimeMillis();
            speaking = voiceDetected;
            voiceLevel = speaking ? Math.max(0f, Math.min(1f, value)) : 0f;
            invalidate();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animating = true;
            previousFrameTime = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }

        @Override protected void onDetachedFromWindow() {
            animating = false;
            draggedBubble = null;
            super.onDetachedFromWindow();
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            initBubbles(width, height);
        }

        private void initBubbles(float width, float height) {
            bubbles.clear();
            float cx = width / 2f, cy = height / 2f;
            float radius = dp(16);
            float orbitX = Math.min(width * .34f, dp(130));
            float orbitY = Math.min(height * .32f, dp(126));
            bubbles.add(new ArenaBubble(cx, cy - orbitY, radius, dp(42), dp(20), ACCENT_2));
            bubbles.add(new ArenaBubble(cx + orbitX, cy + orbitY * .55f, radius, -dp(32), -dp(40), Color.rgb(216, 165, 68)));
            bubbles.add(new ArenaBubble(cx - orbitX, cy + orbitY * .55f, radius, dp(18), -dp(47), Color.rgb(183, 151, 91)));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            if (bubbles.isEmpty()) initBubbles(getWidth(), getHeight());
            updatePhysics(cx, cy);
            float boundaryRadius = Math.min(getWidth(), getHeight()) * .48f;
            if (!compactPictureInPicture) {
                float inset = dp(8);
                RectF arenaBounds = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(ACCENT);
                paint.setAlpha(34);
                canvas.drawRoundRect(arenaBounds, dp(24), dp(24), paint);
                arenaBounds.inset(dp(7), dp(7));
                paint.setAlpha(18);
                canvas.drawRoundRect(arenaBounds, dp(19), dp(19), paint);
            }

            float avatarRadius = compactPictureInPicture
                    ? Math.min(mainRadius, Math.max(dp(8), (getWidth() - dp(14)) / 2f))
                    : mainRadius;
            if (speaking) drawSpeechWaves(canvas, cx, cy, boundaryRadius, avatarRadius);
            if (!compactPictureInPicture) for (ArenaBubble bubble : bubbles) drawGameBubble(canvas, bubble);
            drawMainAvatar(canvas, cx, cy, avatarRadius);

            if (animating) postInvalidateOnAnimation();
        }

        private void drawSpeechWaves(Canvas canvas, float cx, float cy, float boundaryRadius, float avatarRadius) {
            float inner = avatarRadius + dp(5);
            float maxRadius = Math.min(boundaryRadius - dp(4), inner + dp(49));
            float elapsed = (SystemClock.uptimeMillis() - speechAnimationStarted) / 1450f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(ACCENT);
            for (int i = 0; i < 3; i++) {
                float phase = (elapsed + i / 3f) % 1f;
                paint.setStrokeWidth(dp(1.5f + voiceLevel * 1.8f));
                paint.setAlpha((int) (165 * (1f - phase) * (.5f + .5f * voiceLevel)));
                float radius = inner + phase * (maxRadius - inner) + voiceLevel * dp(5);
                canvas.drawCircle(cx, cy, radius, paint);
            }
        }

        private void drawMainAvatar(Canvas canvas, float cx, float cy, float avatarRadius) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(SURFACE_2);
            canvas.drawCircle(cx, cy, avatarRadius, paint);
            if (mainAvatar != null && !mainAvatar.isRecycled()) {
                Path clip = new Path();
                clip.addCircle(cx, cy, avatarRadius - dp(2), Path.Direction.CW);
                canvas.save();
                canvas.clipPath(clip);
                canvas.drawBitmap(mainAvatar, null, new RectF(cx - avatarRadius, cy - avatarRadius, cx + avatarRadius, cy + avatarRadius), photoPaint);
                canvas.restore();
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ACCENT);
                paint.setTextSize(compactPictureInPicture ? dp(20) : dp(29));
                String initial = profileName.isEmpty() ? "К" : profileName.substring(0, 1).toUpperCase(Locale.getDefault());
                canvas.drawText(initial, cx, cy + dp(10), paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(ACCENT);
            paint.setAlpha(245);
            paint.setStrokeWidth(dp(2.5f));
            canvas.drawCircle(cx, cy, avatarRadius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(compactPictureInPicture ? dp(11) : dp(15));
            String displayName = profileName.isEmpty() ? "Вы" : profileName;
            int maxLength = compactPictureInPicture ? 12 : 20;
            if (displayName.length() > maxLength) displayName = displayName.substring(0, maxLength - 1) + "…";
            canvas.drawText(displayName, cx, cy + avatarRadius + dp(compactPictureInPicture ? 16 : 21), paint);
        }

        private void drawGameBubble(Canvas canvas, ArenaBubble bubble) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(bubble.color);
            paint.setAlpha(235);
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFE6A0);
            paint.setAlpha(190);
            paint.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(INK);
            canvas.drawCircle(bubble.x, bubble.y - bubble.radius * .18f, bubble.radius * .22f, paint);
            canvas.drawRoundRect(new RectF(bubble.x - bubble.radius * .42f, bubble.y + bubble.radius * .08f,
                    bubble.x + bubble.radius * .42f, bubble.y + bubble.radius * .58f), bubble.radius * .3f, bubble.radius * .3f, paint);
        }

        private void updatePhysics(float cx, float cy) {
            long now = SystemClock.uptimeMillis();
            float dt = previousFrameTime == 0 ? .016f : Math.min(.04f, Math.max(.001f, (now - previousFrameTime) / 1000f));
            previousFrameTime = now;
            float wall = dp(10);
            float minX = wall, maxX = getWidth() - wall;
            float minY = wall, maxY = getHeight() - wall;
            for (ArenaBubble bubble : bubbles) {
                if (bubble == draggedBubble) continue;
                bubble.x += bubble.vx * dt;
                bubble.y += bubble.vy * dt;
                float left = minX + bubble.radius, right = maxX - bubble.radius;
                float top = minY + bubble.radius, bottom = maxY - bubble.radius;
                if (bubble.x < left) { bubble.x = left; bubble.vx = Math.abs(bubble.vx); }
                if (bubble.x > right) { bubble.x = right; bubble.vx = -Math.abs(bubble.vx); }
                if (bubble.y < top) { bubble.y = top; bubble.vy = Math.abs(bubble.vy); }
                if (bubble.y > bottom) { bubble.y = bottom; bubble.vy = -Math.abs(bubble.vy); }
                bounceFromMain(bubble, cx, cy);
            }
            for (int i = 0; i < bubbles.size(); i++) {
                for (int j = i + 1; j < bubbles.size(); j++) resolveBubbleCollision(bubbles.get(i), bubbles.get(j));
            }
        }

        private void bounceFromMain(ArenaBubble bubble, float cx, float cy) {
            float dx = bubble.x - cx, dy = bubble.y - cy;
            float minDistance = bubble.radius + mainRadius;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance >= minDistance) return;
            if (distance < .001f) { dx = 1f; dy = 0f; distance = 1f; }
            float nx = dx / distance, ny = dy / distance;
            bubble.x = cx + nx * minDistance;
            bubble.y = cy + ny * minDistance;
            float velocityAlongNormal = bubble.vx * nx + bubble.vy * ny;
            if (velocityAlongNormal < 0) {
                bubble.vx -= 1.8f * velocityAlongNormal * nx;
                bubble.vy -= 1.8f * velocityAlongNormal * ny;
            }
        }

        private void resolveBubbleCollision(ArenaBubble first, ArenaBubble second) {
            float dx = second.x - first.x, dy = second.y - first.y;
            float minimum = first.radius + second.radius;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared >= minimum * minimum) return;
            float distance = (float) Math.sqrt(Math.max(.001f, distanceSquared));
            float nx = dx / distance, ny = dy / distance;
            float overlap = (minimum - distance) / 2f;
            first.x -= nx * overlap; first.y -= ny * overlap;
            second.x += nx * overlap; second.y += ny * overlap;
            float relative = (second.vx - first.vx) * nx + (second.vy - first.vy) * ny;
            if (relative < 0) {
                float impulse = -(1.72f * relative) / 2f;
                first.vx -= impulse * nx; first.vy -= impulse * ny;
                second.vx += impulse * nx; second.vy += impulse * ny;
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX(), y = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    draggedBubble = null;
                    float nearest = Float.MAX_VALUE;
                    for (ArenaBubble bubble : bubbles) {
                        float dx = x - bubble.x, dy = y - bubble.y;
                        float distance = dx * dx + dy * dy;
                        float hit = bubble.radius + dp(9);
                        if (distance <= hit * hit && distance < nearest) { nearest = distance; draggedBubble = bubble; }
                    }
                    if (draggedBubble == null) return false;
                    lastTouchX = x; lastTouchY = y; lastTouchTime = SystemClock.uptimeMillis();
                    dragVelocityX = dragVelocityY = 0;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (draggedBubble == null) return false;
                    long now = SystemClock.uptimeMillis();
                    float seconds = Math.max(.01f, (now - lastTouchTime) / 1000f);
                    float dx = x - lastTouchX, dy = y - lastTouchY;
                    draggedBubble.x += dx; draggedBubble.y += dy;
                    dragVelocityX = dx / seconds; dragVelocityY = dy / seconds;
                    lastTouchX = x; lastTouchY = y; lastTouchTime = now;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (draggedBubble == null) return false;
                    draggedBubble.vx = Math.max(-dp(260), Math.min(dp(260), dragVelocityX * .75f));
                    draggedBubble.vy = Math.max(-dp(260), Math.min(dp(260), dragVelocityY * .75f));
                    draggedBubble = null;
                    performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    draggedBubble = null;
                    return true;
            }
            return true;
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    private static class ArenaBubble {
        float x, y, vx, vy, radius;
        final int color;
        ArenaBubble(float x, float y, float radius, float vx, float vy, int color) {
            this.x = x; this.y = y; this.radius = radius; this.vx = vx; this.vy = vy; this.color = color;
        }
    }

    private class IconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String name;
        private final float iconSize;
        private int tint;
        IconView(Activity context, String name, int tint, float iconSize) {
            super(context);
            this.name = name;
            this.tint = tint;
            this.iconSize = iconSize;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        void setTint(int color) { tint = color; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(iconSize, Math.min(getWidth(), getHeight()) * .78f);
            float left = (getWidth() - size) / 2f;
            float top = (getHeight() - size) / 2f;
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(size / 24f, size / 24f);
            paint.setColor(tint);
            paint.setStrokeWidth(2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            Path path = new Path();
            switch (name) {
                case "home":
                    path.moveTo(3, 10); path.lineTo(12, 3); path.lineTo(21, 10); path.moveTo(5, 9); path.lineTo(5, 21); path.lineTo(10, 21); path.lineTo(10, 15); path.lineTo(14, 15); path.lineTo(14, 21); path.lineTo(19, 21); path.lineTo(19, 9); canvas.drawPath(path, paint); break;
                case "chat":
                    canvas.drawRoundRect(new RectF(3, 4, 21, 18), 5, 5, paint); path.moveTo(7, 18); path.lineTo(5, 22); path.lineTo(12, 18); canvas.drawPath(path, paint); break;
                case "mic":
                    canvas.drawRoundRect(new RectF(9, 3, 15, 15), 3, 3, paint); canvas.drawArc(new RectF(5, 7, 19, 20), 0, 180, false, paint); path.moveTo(12, 20); path.lineTo(12, 23); path.moveTo(9, 23); path.lineTo(15, 23); canvas.drawPath(path, paint); break;
                case "mute":
                    canvas.drawRoundRect(new RectF(9, 3, 15, 15), 3, 3, paint); path.moveTo(5, 10); path.lineTo(5, 14); path.quadTo(12, 23, 19, 14); path.moveTo(12, 20); path.lineTo(12, 23); path.moveTo(9, 23); path.lineTo(15, 23); path.moveTo(4, 4); path.lineTo(20, 20); canvas.drawPath(path, paint); break;
                case "profile":
                    canvas.drawCircle(12, 8, 4, paint); canvas.drawArc(new RectF(4, 13, 20, 25), 180, 180, false, paint); break;
                case "more":
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(5, 12, 1.8f, paint); canvas.drawCircle(12, 12, 1.8f, paint); canvas.drawCircle(19, 12, 1.8f, paint); break;
                case "add":
                    path.moveTo(12, 4); path.lineTo(12, 20); path.moveTo(4, 12); path.lineTo(20, 12); canvas.drawPath(path, paint); break;
                case "back":
                    path.moveTo(15, 4); path.lineTo(7, 12); path.lineTo(15, 20); canvas.drawPath(path, paint); break;
                case "next":
                    path.moveTo(9, 4); path.lineTo(17, 12); path.lineTo(9, 20); canvas.drawPath(path, paint); break;
                case "play":
                    paint.setStyle(Paint.Style.FILL); path.moveTo(6, 3); path.lineTo(21, 12); path.lineTo(6, 21); path.close(); canvas.drawPath(path, paint); break;
                case "pause":
                    paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(new RectF(6, 4, 10, 20), 1, 1, paint); canvas.drawRoundRect(new RectF(14, 4, 18, 20), 1, 1, paint); break;
                case "sound":
                    path.moveTo(3, 9); path.lineTo(7, 9); path.lineTo(12, 5); path.lineTo(12, 19); path.lineTo(7, 15); path.lineTo(3, 15); path.close(); canvas.drawPath(path, paint); canvas.drawArc(new RectF(11, 6, 21, 18), -55, 110, false, paint); break;
                case "delivery":
                    canvas.drawRoundRect(new RectF(3, 5, 21, 21), 3, 3, paint);
                    path.moveTo(3, 10); path.lineTo(21, 10); path.moveTo(12, 10); path.lineTo(12, 21);
                    canvas.drawPath(path, paint);
                    break;
                case "chatline":
                    canvas.drawRoundRect(new RectF(3, 4, 21, 19), 5, 5, paint); path.moveTo(7, 19); path.lineTo(5, 22); path.lineTo(12, 19); canvas.drawPath(path, paint); break;
                case "telegram":
                    paint.setStyle(Paint.Style.FILL);
                    path.moveTo(2, 10); path.lineTo(22, 2); path.lineTo(16, 22); path.lineTo(11, 15); path.lineTo(7, 19); path.lineTo(7, 13); path.close();
                    canvas.drawPath(path, paint);
                    paint.setColor(INK);
                    paint.setStrokeWidth(1.2f);
                    paint.setStyle(Paint.Style.STROKE);
                    path.reset(); path.moveTo(7, 13); path.lineTo(15, 14); path.lineTo(22, 2); canvas.drawPath(path, paint);
                    break;
                default:
                    canvas.drawCircle(12, 12, 8, paint);
            }
            canvas.restore();
        }
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private android.graphics.drawable.Drawable ripple(GradientDrawable background) {
        if (Build.VERSION.SDK_INT >= 21) return new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), background, null);
        return background;
    }

    private FrameLayout.LayoutParams centered() { return new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER); }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
    private String formatTime(int milliseconds) {
        int seconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override public void onBackPressed() {
        if (joinedRoomId != null) {
            leaveVoiceRoom();
        } else if (!"feed".equals(activeTab)) {
            activeTab = "feed";
            renderPage();
        } else {
            super.onBackPressed();
        }
    }

    private boolean shouldEnterPictureInPicture() {
        boolean playingFeed = "feed".equals(activeTab) && player != null && userWantsPlayback;
        boolean inVoiceRoom = "rooms".equals(activeTab) && joinedRoomId != null;
        return playingFeed || inVoiceRoom;
    }

    private PictureInPictureParams pictureInPictureParams() {
        float ratio = "rooms".equals(activeTab) && joinedRoomId != null ? 9f / 16f : 16f / 9f;
        if ("feed".equals(activeTab) && videoWidth > 0 && videoHeight > 0) {
            ratio = videoWidth / (float) videoHeight;
            if (ratio < 0.42f || ratio > 2.39f) ratio = 16f / 9f;
        }
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(Math.max(1, Math.round(ratio * 1000)), 1000));
        if (Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(shouldEnterPictureInPicture());
        if ("feed".equals(activeTab) && player != null) {
            boolean playing = player.isPlaying();
            Intent actionIntent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 4110, actionIntent, flags);
            int iconResource = playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            String actionTitle = playing ? "Пауза" : "Воспроизвести";
            RemoteAction playPause = new RemoteAction(Icon.createWithResource(this, iconResource), actionTitle, actionTitle, actionPendingIntent);
            builder.setActions(Collections.singletonList(playPause));
        } else if ("rooms".equals(activeTab) && joinedRoomId != null) {
            Intent actionIntent = new Intent(ACTION_PIP_MIC_TOGGLE).setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 4111, actionIntent, flags);
            String actionTitle = microphoneEnabled ? "Выключить микрофон" : "Включить микрофон";
            RemoteAction micToggle = new RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_btn_speak_now), actionTitle, actionTitle, actionPendingIntent);
            builder.setActions(Collections.singletonList(micToggle));
        }
        return builder.build();
    }

    private void refreshPictureInPictureEligibility() {
        if (Build.VERSION.SDK_INT >= 26) {
            try { setPictureInPictureParams(pictureInPictureParams()); } catch (Exception ignored) { }
        }
    }

    private void refreshPictureInPictureActions() {
        if (Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode()
                && ("feed".equals(activeTab) || ("rooms".equals(activeTab) && joinedRoomId != null))) {
            try { setPictureInPictureParams(pictureInPictureParams()); } catch (Exception ignored) { }
        }
    }

    private void setPictureInPictureFeedUi(boolean inPictureInPicture) {
        View[] overlays = {pipFeedTopBar, pipFeedGradientTop, pipFeedGradientBottom, pipFeedDetails, pipFeedSideControls, pipFeedPlaybackControls};
        for (View overlay : overlays) {
            if (overlay != null) overlay.setVisibility(inPictureInPicture ? View.GONE : View.VISIBLE);
        }
    }

    private void updatePictureInPictureMicButton() {
        if (pipRoomMicButton == null || pipRoomMicIcon == null) return;
        pipRoomMicButton.setContentDescription(microphoneEnabled ? "Выключить микрофон" : "Включить микрофон");
        pipRoomMicButton.setBackground(ripple(round(microphoneEnabled ? ACCENT : SURFACE_2, 28)));
        pipRoomMicIcon.setTint(microphoneEnabled ? INK : ACCENT);
    }

    private void setPictureInPictureRoomUi(boolean inPictureInPicture) {
        if (pipRoomHeader != null) pipRoomHeader.setVisibility(inPictureInPicture ? View.GONE : View.VISIBLE);
        if (pipRoomControls != null) pipRoomControls.setVisibility(inPictureInPicture ? View.GONE : View.VISIBLE);
        if (pipRoomPage != null) {
            int padding = inPictureInPicture ? 0 : dp(18);
            pipRoomPage.setPadding(padding, padding, padding, padding);
        }
        if (pipRoomStageParams != null) {
            pipRoomStageParams.bottomMargin = inPictureInPicture ? 0 : dp(14);
            if (pipRoomPage != null && pipRoomPage.getChildCount() > 1) pipRoomPage.getChildAt(1).setLayoutParams(pipRoomStageParams);
        }
        if (pipRoomMicButton != null) pipRoomMicButton.setVisibility(inPictureInPicture ? View.VISIBLE : View.GONE);
        if (voiceArenaView != null) voiceArenaView.setCompactPictureInPicture(inPictureInPicture);
        updatePictureInPictureMicButton();
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= 26 && shouldEnterPictureInPicture()) {
            enteringPictureInPicture = true;
            try {
                if (!enterPictureInPictureMode(pictureInPictureParams())) enteringPictureInPicture = false;
            } catch (Exception ignored) { enteringPictureInPicture = false; }
        }
    }

    @Override public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        enteringPictureInPicture = false;
        if (bottomNavigation != null) bottomNavigation.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        if ("feed".equals(activeTab)) {
            setPictureInPictureFeedUi(isInPictureInPictureMode);
            if (isInPictureInPictureMode) {
                if (player != null && userWantsPlayback && !player.isPlaying()) {
                    try { player.start(); } catch (Exception ignored) { }
                }
                if (textureView != null) textureView.post(this::applyVideoTransform);
                handler.post(updateProgress);
                refreshPictureInPictureActions();
            }
        } else if ("rooms".equals(activeTab) && joinedRoomId != null) {
            setPictureInPictureRoomUi(isInPictureInPictureMode);
            if (isInPictureInPictureMode) refreshPictureInPictureActions();
        }
        refreshPictureInPictureEligibility();
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null && userWantsPlayback) {
            try { player.start(); handler.post(updateProgress); } catch (Exception ignored) { }
        }
        if (microphoneWasStoppedForPause) {
            microphoneWasStoppedForPause = false;
            renderPage();
        }
    }

    @Override protected void onPause() {
        boolean inPictureInPicture = Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode();
        if (!inPictureInPicture && !enteringPictureInPicture && player != null && player.isPlaying()) {
            try { player.pause(); userWantsPlayback = true; } catch (Exception ignored) { }
        }
        if (!inPictureInPicture && !enteringPictureInPicture && microphoneEnabled) {
            microphoneEnabled = false;
            stopMicrophoneCapture();
            microphoneWasStoppedForPause = true;
            if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        microphoneEnabled = false;
        stopMicrophoneCapture();
        if (voiceArenaView != null) voiceArenaView.setSpeechLevel(false, 0f);
        handler.removeCallbacks(releaseFeedbackAudioTrack);
        releaseFeedbackAudioTrack.run();
        releasePlayer();
        if (pictureInPictureActionReceiverRegistered) {
            try { unregisterReceiver(pictureInPictureActionReceiver); } catch (Exception ignored) { }
            pictureInPictureActionReceiverRegistered = false;
        }
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface UpdateDownloadListener {
        void onProgress(long downloaded, long total);
    }

    private static class VideoItem {
        final String path;
        final String title;
        final long date;
        VideoItem(String path, String title, long date) { this.path = path; this.title = title; this.date = date; }
    }

    private static class RoomItem {
        final String id, name, detail;
        final int tint;
        final boolean global;
        RoomItem(String id, String name, String detail, int tint, boolean global) { this.id = id; this.name = name; this.detail = detail; this.tint = tint; this.global = global; }
    }
}
