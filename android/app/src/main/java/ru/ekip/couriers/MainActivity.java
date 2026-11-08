package ru.ekip.couriers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
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
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android UI. The APK contains no WebView and renders every screen with Android Views. */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int PICK_VIDEO_REQUEST = 4102;
    private static final int INK = Color.rgb(20, 21, 25);
    private static final int SURFACE = Color.rgb(29, 30, 35);
    private static final int SURFACE_2 = Color.rgb(39, 40, 46);
    private static final int MUTED = Color.rgb(158, 160, 168);
    private static final int YELLOW = Color.rgb(255, 211, 50);
    private static final int RED = Color.rgb(234, 82, 86);
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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 12, 15));
        getWindow().setNavigationBarColor(Color.rgb(10, 11, 14));
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarDividerColor(Color.rgb(38, 39, 44));
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
        rooms.add(new RoomItem("common", "Общая комната", "Для всех городов", YELLOW, true));
        rooms.add(new RoomItem("spb", "Санкт-Петербург", "Городская голосовая", 0xFF80A8FF, false));
        rooms.add(new RoomItem("moscow", "Москва", "Городская голосовая", 0xFFF48EAA, false));
        rooms.add(new RoomItem("kazan", "Казань", "Городская голосовая", 0xFF76C79A, false));
        rooms.add(new RoomItem("ekb", "Екатеринбург", "Городская голосовая", 0xFFFFD45C, false));
        rooms.add(new RoomItem("novosibirsk", "Новосибирск", "Городская голосовая", 0xFF83A8FF, false));
    }

    private void buildShell() {
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(INK);
        contentFrame = new FrameLayout(this);
        appRoot.addView(contentFrame, new LinearLayout.LayoutParams(-1, 0, 1f));
        bottomNavigation = buildBottomNavigation();
        appRoot.addView(bottomNavigation, new LinearLayout.LayoutParams(-1, dp(68)));
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
        bar.setPadding(dp(6), dp(7), dp(6), dp(5));
        bar.setBackgroundColor(0xFF111216);
        String[] ids = {"feed", "chats", "rooms", "profile", "more"};
        String[] labels = {"Лента", "Чаты", "Голос", "Профиль", "Ещё"};
        String[] icons = {"home", "chat", "mic", "profile", "more"};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(4), 0, dp(1));
            IconView icon = new IconView(this, icons[i], MUTED, dp(21));
            TextView label = text(labels[i], 10, MUTED, true);
            item.addView(icon, new LinearLayout.LayoutParams(dp(25), dp(26)));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
            labelParams.topMargin = dp(2);
            item.addView(label, labelParams);
            item.setBackground(ripple(round(0x00000000, 14)));
            item.setOnClickListener(view -> {
                joinedRoomId = null;
                activeTab = id;
                renderPage();
            });
            bar.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            item.setTag(id);
        }
        return bar;
    }

    private void updateNavigationSelection() {
        if (bottomNavigation == null) return;
        for (int i = 0; i < bottomNavigation.getChildCount(); i++) {
            LinearLayout item = (LinearLayout) bottomNavigation.getChildAt(i);
            String id = (String) item.getTag();
            int color = activeTab.equals(id) ? YELLOW : MUTED;
            ((IconView) item.getChildAt(0)).setTint(color);
            ((TextView) item.getChildAt(1)).setTextColor(color);
            item.setBackground(ripple(round(activeTab.equals(id) ? 0x22FFD332 : 0x00000000, 14)));
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
        empty.setPadding(dp(34), dp(32), dp(34), dp(34));
        ImageView mark = new ImageView(this);
        mark.setImageResource(ru.ekip.couriers.R.mipmap.ic_launcher);
        mark.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mark.setBackground(round(YELLOW, 22));
        mark.setClipToOutline(true);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(62), dp(62));
        markParams.bottomMargin = dp(22);
        empty.addView(mark, markParams);
        TextView eyebrow = text("ТВОЯ ВИДЕОМЕДИАТЕКА", 11, YELLOW, true);
        eyebrow.setLetterSpacing(.12f);
        eyebrow.setGravity(Gravity.CENTER);
        empty.addView(eyebrow);
        TextView title = text("Здесь пока пусто", 24, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(12);
        empty.addView(title, titleParams);
        TextView subtitle = text("Выбери ролик — он сохранится в приложении и будет доступен после перезапуска.", 14, 0xFFB4B6BE, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(9);
        subtitleParams.bottomMargin = dp(24);
        empty.addView(subtitle, subtitleParams);
        TextView choose = pillButton("＋   Выбрать видео", YELLOW, INK, 15);
        choose.setOnClickListener(view -> openVideoPicker());
        empty.addView(choose, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView privacy = text("ХРАНИТСЯ ТОЛЬКО НА ЭТОМ ТЕЛЕФОНЕ", 9, MUTED, true);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(17);
        empty.addView(privacy, privacyParams);
        feed.addView(empty, centered());
        addFeedTopBar(feed);
    }

    private void addFeedTopBar(FrameLayout feed) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(12), dp(16), dp(8));
        ImageView logo = new ImageView(this);
        try { logo.setImageResource(ru.ekip.couriers.R.mipmap.ic_launcher); } catch (Exception ignored) { }
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setBackground(round(YELLOW, 12));
        logo.setClipToOutline(true);
        top.addView(logo, new LinearLayout.LayoutParams(dp(37), dp(37)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        TextView name = text("экип", 18, Color.WHITE, true);
        TextView sub = text("СВОИ НА МАРШРУТЕ", 8, 0xFFE0E0E4, true);
        sub.setLetterSpacing(.13f);
        brand.addView(name);
        brand.addView(sub);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        if (videos.size() > 0) {
            TextView add = roundIconButton("add", YELLOW, INK, 48);
            add.setContentDescription("Добавить видео");
            add.setOnClickListener(view -> openVideoPicker());
            top.addView(add);
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
        TextView badge = text("●  МОЯ МЕДИАТЕКА", 10, YELLOW, true);
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
        seekBar.setProgressTintList(ColorStateList.valueOf(YELLOW));
        seekBar.setThumbTintList(ColorStateList.valueOf(YELLOW));
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
        IconView art = new IconView(this, icon, YELLOW, dp(32));
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
        contentFrame.addView(page);
    }

    private void renderRooms() {
        LinearLayout page = pageColumn();
        addPageHeader(page, "Голосовые", "Общая и городские комнаты");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        TextView tip = text("Комнаты доступны всегда — зайти можно первым.", 13, MUTED, false);
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(-1, -2);
        tipParams.bottomMargin = dp(15);
        list.addView(tip, tipParams);
        for (RoomItem room : rooms) list.addView(roomCard(room));
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        contentFrame.addView(page);
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
        TextView status = text("Постоянная комната", 11, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(4);
        copy.addView(kind);
        copy.addView(name, nameParams);
        copy.addView(status, statusParams);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView join = pillButton("Зайти", YELLOW, INK, 12);
        card.addView(join, new LinearLayout.LayoutParams(dp(76), dp(42)));
        join.setOnClickListener(view -> {
            joinedRoomId = room.id;
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
        TextView count = pillButton("1 участник", SURFACE_2, Color.WHITE, 10);
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
        avatar.setBackground(round(YELLOW, 32));
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
        contentFrame.addView(page);
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
        avatar.setBackground(round(YELLOW, 28));
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
        TextView save = pillButton("Сохранить профиль", YELLOW, INK, 14);
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
        contentFrame.addView(page);
    }

    private void renderSettings() {
        LinearLayout page = pageColumn();
        addPageHeader(page, "Ещё", "Настройки приложения");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));
        card.setBackground(round(SURFACE, 20));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(18);
        page.addView(card, cardParams);
        addSettingRow(card, "Медиатека", "Видео хранятся на телефоне", "▣", view -> new AlertDialog.Builder(this).setTitle("Локальная медиатека").setMessage("Выбранные видео копируются в приватное хранилище приложения и не отправляются в сеть.").setPositiveButton("Понятно", null).show());
        addSettingRow(card, "Обновить приложение", "Скачать актуальный APK", "↻", view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/ekip-debug.apk"));
            try { startActivity(intent); } catch (Exception ignored) { Toast.makeText(this, "Не удалось открыть ссылку на APK", Toast.LENGTH_SHORT).show(); }
        });
        addSettingRow(card, "О приложении", "Экип · Android", "ⓘ", view -> new AlertDialog.Builder(this).setTitle("Экип").setMessage("Нативное Android-приложение. Видео хранятся только на этом устройстве.").setPositiveButton("Понятно", null).show());
        TextView note = text("Android попросит подтвердить установку обновления. Голосовые звонки и публикация видео требуют сервера.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(18);
        page.addView(note, noteParams);
        contentFrame.addView(page);
    }

    private void addSettingRow(LinearLayout parent, String title, String detail, String glyph, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        TextView icon = text(glyph, 21, YELLOW, true);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(text(title, 14, Color.WHITE, true));
        TextView sub = text(detail, 11, MUTED, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(3);
        copy.addView(sub, subParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(new IconView(this, "next", MUTED, dp(18)));
        row.setOnClickListener(action);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        View divider = new View(this);
        divider.setBackgroundColor(0xFF36373D);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
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
        progress.setIndeterminateTintList(ColorStateList.valueOf(YELLOW));
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

    private RoomItem findRoom(String id) {
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
