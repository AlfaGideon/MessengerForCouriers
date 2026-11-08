package ru.ekip.couriers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android UI. The APK contains no WebView and renders every screen with Android Views. */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int PICK_VIDEO_REQUEST = 4102;
    private static final int UPDATE_PERMISSION_REQUEST = 4103;
    private static final int UPDATE_INSTALL_REQUEST = 4104;
    private static final String UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/update.json";
    private static final String UPDATE_APK_PREFIX = "https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/ekip-debug.apk";
    private static final int INK = Color.rgb(16, 24, 32);
    private static final int SURFACE = Color.rgb(27, 37, 48);
    private static final int SURFACE_2 = Color.rgb(36, 48, 61);
    private static final int MUTED = Color.rgb(157, 171, 184);
    private static final int ACCENT = Color.rgb(80, 210, 192);
    private static final int ACCENT_2 = Color.rgb(143, 161, 255);
    private static final int RED = Color.rgb(255, 105, 125);
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
        getWindow().setNavigationBarColor(Color.rgb(10, 16, 22));
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarDividerColor(Color.rgb(43, 58, 71));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        loadLocalState();
        buildShell();
        renderPage();
    }

    private void loadLocalState() {
        SharedPreferences prefs = getSharedPreferences("ekip_native", MODE_PRIVATE);
        profileName = prefs.getString("profile_name", "");
        String saved = prefs.getString("videos", "[]");
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String path = object.optString("path", "");
                File file = new File(path);
                if (file.isFile()) videos.add(new VideoItem(path, object.optString("name", file.getName()), object.optLong("date", file.lastModified())));
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
        bar.setBackground(round(Color.rgb(18, 28, 38), 27));
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
                halo.setBackground(round(0x3350D2C0, 36));
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
                    joinedRoomId = null;
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
                joinedRoomId = null;
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
            tab.setBackground(ripple(round(selected ? 0x1F50D2C0 : 0x00000000, 16)));
        }
    }

    private void renderPage() {
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
    }

    private void renderFeed() {
        FrameLayout feed = new FrameLayout(this);
        feed.setBackgroundColor(Color.rgb(8, 9, 12));
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
            @Override public boolean onSingleTapUp(MotionEvent event) { togglePlayback(); return true; }
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dy) > dp(50) && Math.abs(velocityY) > dp(120)) {
                    showVideoAt(currentVideoIndex + (dy < 0 ? 1 : -1));
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
        if (player == null) { playOverlay.setText("▶"); playOverlay.setVisibility(View.VISIBLE); }
    }

    private void renderEmptyFeed(FrameLayout feed) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(26), dp(24), dp(26));
        empty.setBackground(round(Color.rgb(17, 27, 37), 28));
        empty.setElevation(dp(4));

        FrameLayout iconTile = new FrameLayout(this);
        iconTile.setBackground(round(0x2250D2C0, 23));
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
        TextView subtitle = text("Добавь ролик с телефона — он останется только у тебя и будет доступен после перезапуска.", 13, 0xFFB4C0CA, false);
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
        gradientTop.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xB8000000, 0x00000000}));
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(-1, dp(135), Gravity.TOP);
        feed.addView(gradientTop, top);
        View gradientBottom = new View(this);
        gradientBottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xE8000000, 0x00000000}));
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, dp(240), Gravity.BOTTOM);
        feed.addView(gradientBottom, bottom);
    }

    private void addFeedVideoDetails(FrameLayout feed) {
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(18), 0, dp(84), dp(16));
        TextView badge = text("●  МОЯ МЕДИАТЕКА", 10, ACCENT, true);
        badge.setLetterSpacing(.08f);
        info.addView(badge);
        TextView title = text(videos.get(currentVideoIndex).name, 16, Color.WHITE, true);
        title.setMaxLines(2);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        info.addView(title, titleParams);
        TextView local = text("Сохранено на этом телефоне", 11, 0xFFE0E0E4, false);
        LinearLayout.LayoutParams localParams = new LinearLayout.LayoutParams(-1, -2);
        localParams.topMargin = dp(5);
        info.addView(local, localParams);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
        params.bottomMargin = dp(104);
        feed.addView(info, params);
    }

    private void addFeedControls(FrameLayout feed) {
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER_HORIZONTAL);
        side.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView sound = roundIconButton(muted ? "mute" : "sound", 0x990D0E12, Color.WHITE, 46);
        sound.setContentDescription(muted ? "Включить звук" : "Выключить звук");
        sound.setOnClickListener(view -> {
            muted = !muted;
            if (player != null) player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
            sound.setText(muted ? "×" : "♪");
        });
        side.addView(sound);
        TextView delete = roundIconButton("more", 0x990D0E12, Color.WHITE, 46);
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
        playback.setOrientation(LinearLayout.HORIZONTAL);
        playback.setGravity(Gravity.CENTER_VERTICAL);
        playback.setPadding(dp(15), 0, dp(15), 0);
        playback.setBackground(round(0x66101218, 20));
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
        });
        return card;
    }

    private void renderCallRoom() {
        RoomItem room = findRoom(joinedRoomId);
        if (room == null) { joinedRoomId = null; renderRooms(); return; }
        LinearLayout page = pageColumn();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(18));
        TextView back = roundIconButton("back", SURFACE_2, Color.WHITE, 46);
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        back.setOnClickListener(view -> { joinedRoomId = null; renderPage(); });
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

        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER);
        stage.setBackground(round(SURFACE, 26));
        stage.setPadding(dp(18), dp(20), dp(18), dp(20));
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        stageParams.bottomMargin = dp(14);
        page.addView(stage, stageParams);
        TextView avatar = text("Я", 35, INK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(ACCENT, 32));
        stage.addView(avatar, new LinearLayout.LayoutParams(dp(104), dp(104)));
        TextView self = text(profileName.isEmpty() ? "Вы" : profileName, 19, Color.WHITE, true);
        LinearLayout.LayoutParams selfParams = new LinearLayout.LayoutParams(-2, -2);
        selfParams.topMargin = dp(18);
        stage.addView(self, selfParams);
        TextView micState = text("Микрофон не подключён", 12, MUTED, false);
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(-2, -2);
        micParams.topMargin = dp(6);
        stage.addView(micState, micParams);
        TextView waiting = text("Вы вошли в комнату.\nДругие участники появятся после подключения голосового сервера.", 13, MUTED, false);
        waiting.setGravity(Gravity.CENTER);
        waiting.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams waitingParams = new LinearLayout.LayoutParams(-1, -2);
        waitingParams.topMargin = dp(34);
        stage.addView(waiting, waitingParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        TextView mic = pillButton("⊘  Микрофон недоступен", SURFACE_2, MUTED, 12);
        mic.setEnabled(false);
        controls.addView(mic, new LinearLayout.LayoutParams(0, dp(54), 1f));
        TextView leave = pillButton("‹  Выйти", RED, Color.WHITE, 14);
        LinearLayout.LayoutParams leaveParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        leaveParams.leftMargin = dp(10);
        controls.addView(leave, leaveParams);
        leave.setOnClickListener(view -> { joinedRoomId = null; renderPage(); });
        page.addView(controls);
        TextView notice = text("Голосовая связь между телефонами ещё не подключена.", 10, MUTED, false);
        notice.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
        noticeParams.topMargin = dp(12);
        page.addView(notice, noticeParams);
        contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
    }

    private void renderProfile() {
        LinearLayout page = pageColumn();
        addPageHeader(page, "Профиль", "Личные данные на этом телефоне");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(round(SURFACE, 22));
        TextView avatar = text(profileName.isEmpty() ? "?" : profileName.substring(0, 1).toUpperCase(Locale.getDefault()), 28, INK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(ACCENT, 28));
        card.addView(avatar, new LinearLayout.LayoutParams(dp(76), dp(76)));
        TextView title = text(profileName.isEmpty() ? "Как к вам обращаться?" : profileName, 19, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(15);
        title.setGravity(Gravity.CENTER);
        card.addView(title, titleParams);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(profileName);
        input.setHint("Ваше имя");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(MUTED);
        input.setTextSize(15);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(SURFACE_2, 14));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(52));
        inputParams.topMargin = dp(22);
        card.addView(input, inputParams);
        TextView save = pillButton("Сохранить профиль", ACCENT, INK, 14);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(50));
        saveParams.topMargin = dp(12);
        card.addView(save, saveParams);
        save.setOnClickListener(view -> {
            profileName = input.getText().toString().trim();
            getSharedPreferences("ekip_native", MODE_PRIVATE).edit().putString("profile_name", profileName).apply();
            Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show();
            renderPage();
        });
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(16);
        page.addView(card, cardParams);
        contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
    }

    private void renderSettings() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout page = pageColumn();
        addPageHeader(page, "Настройки", "Курьерка под ваш город и маршрут");

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

        addSectionLabel(page, "ПРОФИЛЬ");
        LinearLayout profileCard = settingsCard();
        addSettingRow(profileCard, "Мой профиль", profileName.isEmpty() ? "Добавить имя" : profileName, "◉", view -> {
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
        TextView voiceDetail = text("В списке будут две комнаты: общий канал и «" + cityName() + "». Выбор города меняет доступную городскую комнату.", 12, MUTED, false);
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
            row.setBackground(ripple(round(selected ? 0x2250D2C0 : Color.TRANSPARENT, 15)));
            TextView marker = text(selected ? "●" : "○", 18, selected ? ACCENT : MUTED, true);
            marker.setGravity(Gravity.CENTER);
            row.addView(marker, new LinearLayout.LayoutParams(dp(30), dp(46)));
            TextView label = text(name, 14, selected ? Color.WHITE : 0xFFE0E6EC, selected);
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
        custom.setBackground(ripple(round(0x1A50D2C0, 15)));
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
        if (joinedRoomId != null && !"common".equals(joinedRoomId) && !joinedRoomId.equals(cityId)) joinedRoomId = null;
        Toast.makeText(this, "Город: " + cityName(), Toast.LENGTH_SHORT).show();
        renderPage();
    }

    private void addSettingRow(LinearLayout parent, String title, String detail, String glyph, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(68));
        row.setPadding(0, dp(12), 0, dp(12));
        TextView icon = text(glyph, 21, ACCENT, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(0x2250D2C0, 12));
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
        divider.setBackgroundColor(0xFF30414F);
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
        Uri uri = data.getData();
        String name = queryDisplayName(uri);
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
                    videos.add(0, new VideoItem(finalOutput.getAbsolutePath(), name, System.currentTimeMillis()));
                    currentVideoIndex = 0;
                    persistVideos();
                    copyingVideo = false;
                    renderPage();
                    Toast.makeText(this, "Видео добавлено в медиатеку", Toast.LENGTH_SHORT).show();
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

    private void showLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setTag("copying_video_overlay");
        overlay.setBackgroundColor(0xBB08090C);
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

    private String queryDisplayName(Uri uri) {
        String name = "Видео";
        try (android.database.Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) name = value;
            }
        } catch (Exception ignored) { }
        return name;
    }

    private void persistVideos() {
        JSONArray array = new JSONArray();
        for (VideoItem item : videos) {
            JSONObject object = new JSONObject();
            try {
                object.put("path", item.path);
                object.put("name", item.name);
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
                .setMessage("«" + item.name + "» будет удалено с этого устройства.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new File(item.path).delete();
                    videos.remove(currentVideoIndex);
                    if (currentVideoIndex >= videos.size()) currentVideoIndex = Math.max(0, videos.size() - 1);
                    persistVideos();
                    renderPage();
                }).show();
    }

    private void showVideoAt(int index) {
        if (videos.isEmpty()) return;
        int next = index;
        if (next < 0) next = videos.size() - 1;
        if (next >= videos.size()) next = 0;
        if (next == currentVideoIndex && player != null) return;
        currentVideoIndex = next;
        userWantsPlayback = true;
        persistVideos();
        renderPage();
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
                return true;
            });
            player.prepareAsync();
        } catch (Exception error) {
            releasePlayer();
            Toast.makeText(this, "Не удалось открыть видео", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyVideoTransform() {
        if (textureView == null || videoWidth <= 0 || videoHeight <= 0) return;
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;
        float scale = Math.max(viewWidth / (float) videoWidth, viewHeight / (float) videoHeight);
        float scaledWidth = videoWidth * scale;
        float scaledHeight = videoHeight * scale;
        float dx = (viewWidth - scaledWidth) / 2f;
        float dy = (viewHeight - scaledHeight) / 2f;
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        textureView.setTransform(matrix);
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
                case "chatline":
                    canvas.drawRoundRect(new RectF(3, 4, 21, 19), 5, 5, paint); path.moveTo(7, 19); path.lineTo(5, 22); path.lineTo(12, 19); canvas.drawPath(path, paint); break;
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
            joinedRoomId = null;
            renderPage();
        } else if (!"feed".equals(activeTab)) {
            activeTab = "feed";
            renderPage();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null && userWantsPlayback) {
            try { player.start(); handler.post(updateProgress); } catch (Exception ignored) { }
        }
    }

    @Override protected void onPause() {
        if (player != null && player.isPlaying()) {
            try { player.pause(); userWantsPlayback = true; } catch (Exception ignored) { }
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        releasePlayer();
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface UpdateDownloadListener {
        void onProgress(long downloaded, long total);
    }

    private static class VideoItem {
        final String path;
        final String name;
        final long date;
        VideoItem(String path, String name, long date) { this.path = path; this.name = name; this.date = date; }
    }

    private static class RoomItem {
        final String id, name, detail;
        final int tint;
        final boolean global;
        RoomItem(String id, String name, String detail, int tint, boolean global) { this.id = id; this.name = name; this.detail = detail; this.tint = tint; this.global = global; }
    }
}
