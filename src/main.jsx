import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity, ArrowDownLeft, ArrowLeft, ArrowUpRight, Bell, Bookmark, Check, ChevronDown, ChevronRight,
  CircleHelp, CloudDownload, Compass, FileVideo, HardDrive, Headphones, Heart, Home, MapPin, MessageCircle,
  Mic, MicOff, MoreHorizontal, Pause, Play, Plus, Search, Send, Settings, Share2, ShieldCheck,
  RefreshCw, Radio, SlidersHorizontal, Sparkles, ThumbsUp, Trash2, Users, Video, Volume2, VolumeX, X, Zap
} from 'lucide-react';
import './style.css';
import { deleteSavedVideo, listSavedVideos, saveVideo } from './storage.js';
import { fetchOtaManifest } from './ota.js';

const rooms = [
  { id: 'common', title: 'Общая комната', city: 'Для всех городов', detail: 'Общий голосовой зал экипа', tone: 'room-yellow', global: true },
  { id: 'spb', title: 'Санкт-Петербург', city: 'Санкт-Петербург', detail: 'Голосовая для курьеров города', tone: 'room-blue' },
  { id: 'moscow', title: 'Москва', city: 'Москва', detail: 'Голосовая для курьеров города', tone: 'room-pink' },
  { id: 'kazan', title: 'Казань', city: 'Казань', detail: 'Голосовая для курьеров города', tone: 'room-green' },
  { id: 'ekb', title: 'Екатеринбург', city: 'Екатеринбург', detail: 'Голосовая для курьеров города', tone: 'room-yellow' },
  { id: 'novosibirsk', title: 'Новосибирск', city: 'Новосибирск', detail: 'Голосовая для курьеров города', tone: 'room-blue' },
];

function Avatar({ name, tone = 'blue', size = '' }) {
  return <span className={`avatar avatar-${tone} ${size}`}>{name}</span>;
}
function Logo({ small = false }) {
  return <div className={`brand-mark ${small ? 'brand-mark-small' : ''}`}><img src="/ekip-icon.png" alt="" /></div>;
}
function App() {
  const [page, setPage] = useState('feed');
  const [filter, setFilter] = useState('Для тебя');
  const [liked, setLiked] = useState(false);
  const [saved, setSaved] = useState(false);
  const [following, setFollowing] = useState(false);
  const videoPickerRef = useRef(null);
  const [savingVideo, setSavingVideo] = useState(false);
  const [toast, setToast] = useState('');
  const [activeChat, setActiveChat] = useState(0);
  const [chatMessage, setChatMessage] = useState('');
  const [sentMessages, setSentMessages] = useState([]);
  const [joinedRoom, setJoinedRoom] = useState('');
  const [search, setSearch] = useState('');
  const [settings, setSettings] = useState({ notifications: true, darkMode: false, location: true });
  const [videos, setVideos] = useState([]);
  const [videosLoading, setVideosLoading] = useState(true);
  const [updateInfo, setUpdateInfo] = useState(null);
  const [otaStatus, setOtaStatus] = useState('idle');

  useEffect(() => {
    let active = true;
    listSavedVideos().then(items => {
      if (active) setVideos(items.map(item => ({ ...item, url: URL.createObjectURL(item.blob) })));
    }).catch(() => {
      if (active) setVideos([]);
    }).finally(() => { if (active) setVideosLoading(false); });
    return () => { active = false; };
  }, []);

  const publishLocalVideo = async ({ file, caption }) => {
    if (navigator.storage?.persist) await navigator.storage.persist().catch(() => false);
    const item = await saveVideo(file, caption);
    const video = { ...item, url: URL.createObjectURL(item.blob) };
    setVideos(current => [video, ...current]);
    notify('Видео добавлено в медиатеку на телефоне');
  };

  const removeLocalVideo = async (video) => {
    await deleteSavedVideo(video.id);
    URL.revokeObjectURL(video.url);
    setVideos(current => current.filter(item => item.id !== video.id));
    notify('Видео удалено с устройства');
  };

  const notify = (message) => {
    setToast(message);
    window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(() => setToast(''), 2600);
  };
  const checkForUpdates = async (autoApply = false) => {
    if (!window.EkipNative?.getWebVersion) {
      setOtaStatus('native-only');
      return;
    }
    setOtaStatus('checking');
    try {
      const manifest = await fetchOtaManifest();
      const current = Number(window.EkipNative.getWebVersion());
      if (Number(manifest.webVersion) > current) {
        setUpdateInfo(manifest);
        setOtaStatus('available');
        if (autoApply) {
          setOtaStatus('downloading');
          window.EkipNative.installWebUpdate(manifest.webUpdateUrl, manifest.webUpdateSha256, String(manifest.webVersion));
        }
      } else {
        setUpdateInfo(manifest);
        setOtaStatus('current');
      }
    } catch (error) {
      setOtaStatus('error');
    }
  };
  const applyWebUpdate = () => {
    if (!updateInfo || !window.EkipNative?.installWebUpdate) return;
    setOtaStatus('downloading');
    window.EkipNative.installWebUpdate(updateInfo.webUpdateUrl, updateInfo.webUpdateSha256, String(updateInfo.webVersion));
  };

  useEffect(() => {
    window.__ekipOtaResult = (success) => {
      if (success) {
        setOtaStatus('current');
        setUpdateInfo(null);
        notify('Экип обновлён по воздуху');
      } else {
        setOtaStatus('error');
        notify('Не удалось установить обновление. Проверьте интернет и попробуйте ещё раз.');
      }
    };
    if (window.EkipNative?.getWebVersion) checkForUpdates(true);
    return () => { delete window.__ekipOtaResult; };
  }, []);

  const chooseVideo = () => { if (!savingVideo) videoPickerRef.current?.click(); };
  const handleVideoPicked = async (event) => {
    const input = event.currentTarget;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    setSavingVideo(true);
    try {
      await publishLocalVideo({ file, caption: '' });
    } catch (error) {
      notify(error.message || 'Не удалось сохранить видео');
    } finally {
      setSavingVideo(false);
    }
  };

  const navigate = (next) => { setPage(next); setToast(''); };
  const nav = [
    { id: 'feed', label: 'Лента', icon: Home },
    { id: 'chats', label: 'Чаты', icon: MessageCircle },
    { id: 'rooms', label: 'Голосовые', icon: Headphones },
    { id: 'profile', label: 'Профиль', icon: Users },
  ];

  const sendMessage = (e) => {
    e.preventDefault();
    if (!chatMessage.trim()) return;
    setSentMessages([...sentMessages, { chat: activeChat, text: chatMessage.trim() }]);
    setChatMessage('');
  };
  const joinRoom = (roomId) => {
    setJoinedRoom(roomId);
    setPage('rooms');
    const room = rooms.find(item => item.id === roomId);
    notify(`Открыта комната: ${room?.title || 'комната экипа'}`);
  };
  const leaveRoom = () => {
    setJoinedRoom('');
    notify('Вы вышли из комнаты');
  };

  return <div className={`app-shell ${settings.darkMode ? 'dark-mode' : ''} ${page === 'feed' ? 'feed-screen' : ''}`}>
    <aside className="sidebar">
      <button className="brand-lockup" onClick={() => navigate('feed')} aria-label="Экип, на главную">
        <Logo /><span className="brand-name">экип<span className="brand-dot">.</span><small>СВОИ НА МАРШРУТЕ</small></span>
      </button>
      <div className="side-caption">МЕНЮ</div>
      <nav className="side-nav">
        {nav.map(({ id, label, icon: Icon, badge }) => <button key={id} className={`nav-item ${page === id ? 'active' : ''}`} onClick={() => navigate(id)}>
          <Icon size={19} strokeWidth={page === id ? 2.35 : 1.9} /><span>{label}</span>{badge && <b className="nav-badge">{badge}</b>}
        </button>)}
      </nav>
      <div className="sidebar-room-head"><span className="side-caption">КОМНАТЫ РЯДОМ</span><button onClick={() => navigate('rooms')}><Plus size={16}/></button></div>
      {rooms.slice(0,4).map(room => <button className={`side-room ${joinedRoom===room.id?'active-room':''}`} key={room.id} onClick={() => joinRoom(room.id)}><i className="empty-indicator"/><span>{room.title}</span><small>⋯</small></button>)}
      <div className="sidebar-bottom">
        <div className="shift-card"><div className="shift-icon"><HardDrive size={16}/></div><div><b>Медиа на устройстве</b><span>Локальное сохранение</span></div></div>
        <button className="sidebar-profile" onClick={() => navigate('profile')}><Avatar name="?" tone="yellow"/><span><b>Ваш профиль</b><small>Заполните данные</small></span><MoreHorizontal size={19}/></button>
      </div>
    </aside>

    <main className="main-area">
      <header className="topbar">
        <div className="mobile-brand"><Logo small/><b>экип<span className="brand-dot">.</span></b></div>
        <div className="breadcrumb"><span>Сообщество</span><ChevronRight size={14}/><b>{nav.find(n => n.id === page)?.label || 'Настройки'}</b></div>
        <div className="top-actions"><label className="searchbox"><Search size={17}/><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Поиск по экипу"/><kbd>⌘ K</kbd></label><button className="icon-button notification-button" aria-label="Уведомления" onClick={() => notify('Пока новых уведомлений нет')}><Bell size={19}/><i/></button><Avatar name="?" tone="yellow" size="avatar-top"/></div>
      </header>

      <div className="page-content">
        {page === 'feed' && <FeedPage {...{ filter, setFilter, videos, videosLoading, onDeleteVideo: removeLocalVideo, onChooseVideo: chooseVideo, savingVideo }} />}
        {page === 'chats' && <ChatsPage {...{ activeChat, setActiveChat, search, setSearch, sentMessages, chatMessage, setChatMessage, sendMessage, notify }} />}
        {page === 'rooms' && <RoomsPage joinedRoom={joinedRoom} onJoinRoom={joinRoom} onLeaveRoom={leaveRoom} />}
        {page === 'profile' && <ProfilePage {...{ navigate, notify }} />}
        {page === 'settings' && <SettingsPage {...{ settings, setSettings, notify, otaStatus, updateInfo, onCheckUpdates: () => checkForUpdates(false), onApplyUpdate: applyWebUpdate }} />}
      </div>
      <nav className="mobile-nav">
        {nav.map(({id,label,icon:Icon,badge}) => <button key={id} className={page === id ? 'active' : ''} onClick={() => navigate(id)}><span className="mobile-icon-wrap"><Icon size={20}/>{badge && <i>{badge}</i>}</span><small>{label}</small></button>)}
        <button className={page === 'settings' ? 'active' : ''} onClick={() => navigate('settings')}><span className="mobile-icon-wrap"><Settings size={20}/></span><small>Ещё</small></button>
      </nav>
    </main>

    <aside className="rightbar"><div className="local-storage-panel"><div className="empty-module-icon"><HardDrive size={24}/></div><span className="eyebrow">ЛОКАЛЬНЫЙ РЕЖИМ</span><h3>Ваши видео — на вашем устройстве</h3><p>Файлы остаются в хранилище приложения. Они не загружаются в общую сеть и не видны другим пользователям.</p></div></aside>

    <input ref={videoPickerRef} className="native-video-picker" type="file" accept="video/*" onChange={handleVideoPicked} aria-label="Выбрать видео на телефоне" />
    {toast && <div className="toast"><span className="toast-check"><Check size={14}/></span>{toast}</div>}
  </div>;
}

function formatVideoTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const minutes = Math.floor(seconds / 60);
  const remainder = Math.floor(seconds % 60).toString().padStart(2, '0');
  return `${minutes}:${remainder}`;
}

function SavedVideoPlayer({ video }) {
  const player = useRef(null);
  const [playing, setPlaying] = useState(false);
  const [muted, setMuted] = useState(false);
  const [time, setTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const holdTimer = useRef(null);
  const longPressReset = useRef(null);
  const ignoreLongPressClick = useRef(false);
  useEffect(() => {
    const element = player.current;
    if (!element || !window.matchMedia('(max-width: 760px)').matches || !('IntersectionObserver' in window)) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && entry.intersectionRatio >= 0.72) {
        element.muted = true;
        setMuted(true);
        element.play().catch(() => {});
      } else {
        element.pause();
      }
    }, { threshold: [0, 0.72, 1] });
    observer.observe(element);
    return () => { observer.disconnect(); element.pause(); clearTimeout(holdTimer.current); clearTimeout(longPressReset.current); };
  }, [video.url]);
  const togglePlayback = () => {
    const element = player.current;
    if (!element) return;
    if (element.paused) element.play().catch(() => {});
    else element.pause();
  };
  const startTouchHold = () => {
    clearTimeout(holdTimer.current);
    ignoreLongPressClick.current = false;
    holdTimer.current = window.setTimeout(() => { ignoreLongPressClick.current = true; }, 450);
  };
  const endTouchHold = () => {
    clearTimeout(holdTimer.current);
    clearTimeout(longPressReset.current);
    longPressReset.current = window.setTimeout(() => { ignoreLongPressClick.current = false; }, 650);
  };
  const handleVideoTap = () => {
    if (ignoreLongPressClick.current) { ignoreLongPressClick.current = false; return; }
    togglePlayback();
  };
  const toggleMute = () => {
    const element = player.current;
    if (!element) return;
    element.muted = !element.muted;
    setMuted(element.muted);
  };
  return <div className={`video-player-shell ${playing ? 'video-is-playing' : ''}`}>
    <video ref={player} className="saved-video-player" playsInline preload="metadata" src={video.url}
      onClick={handleVideoTap} onTouchStart={startTouchHold} onTouchEnd={endTouchHold} onTouchCancel={endTouchHold} onContextMenu={event => event.preventDefault()} onDragStart={event => event.preventDefault()} onPlay={() => setPlaying(true)} onPause={() => setPlaying(false)}
      onTimeUpdate={event => setTime(event.currentTarget.currentTime)}
      onLoadedMetadata={event => setDuration(event.currentTarget.duration || 0)}
      onEnded={() => setPlaying(false)} />
    {!playing && <button className="video-big-play" onClick={handleVideoTap} onTouchStart={startTouchHold} onTouchEnd={endTouchHold} onContextMenu={event => event.preventDefault()} aria-label="Воспроизвести видео"><Play size={25} fill="currentColor"/></button>}
    <div className="video-controls" onClick={event => event.stopPropagation()}>
      <button className="video-control-button" onClick={togglePlayback} aria-label={playing ? 'Пауза' : 'Воспроизвести'}>{playing ? <Pause size={17} fill="currentColor"/> : <Play size={17} fill="currentColor"/>}</button>
      <span className="video-current-time">{formatVideoTime(time)}</span>
      <input className="video-seek" type="range" min="0" max={duration || 0} step="0.1" value={Math.min(time, duration || 0)} onChange={event => { if (player.current) player.current.currentTime = Number(event.target.value); }} aria-label="Перемотать видео" style={{ '--seek-progress': `${duration ? (time / duration) * 100 : 0}%` }}/>
      <span className="video-duration">{formatVideoTime(duration)}</span>
      <button className="video-control-button video-volume" onClick={toggleMute} aria-label={muted ? 'Включить звук' : 'Выключить звук'}>{muted ? <VolumeX size={17}/> : <Volume2 size={17}/>}</button>
    </div>
  </div>;
}

function FeedPage({ videos, videosLoading, onDeleteVideo, onChooseVideo, savingVideo }) {
  return <div className="feed-layout">
    <div className="feed-column">
      <div className="welcome-row"><div><div className="eyebrow"><span className="welcome-dot"/> ЭКИП · МОЯ МЕДИАТЕКА</div><h1>Мои видео</h1><p>Добавьте ролик с телефона — он останется на устройстве.</p></div>{videos.length > 0 && <button className="button-primary create-video" onClick={onChooseVideo} disabled={savingVideo} aria-label="Добавить видео" title="Добавить видео"><Plus size={17}/><span>{savingVideo ? 'Сохраняю…' : 'Добавить видео'}</span></button>}</div>
      <div className="route-status-strip"><span className="route-status-icon"><HardDrive size={16}/></span><span className="route-status-copy"><b>Локальное хранение</b><small><i/> Видео сохраняются в памяти приложения</small></span><span className="local-only-badge">ТОЛЬКО ВАМ</span></div>
      <div className="feed-filter-row"><div className="feed-tabs"><button className="selected">Медиатека <span className="video-count">{videos.length}</span></button></div><span className="local-feed-hint"><HardDrive size={13}/> Без облачной публикации</span></div>
      {videosLoading ? <div className="empty-feed loading-feed"><div className="storage-spinner"/><b>Проверяю сохранённые видео…</b></div> : videos.length === 0 ? <section className="empty-feed"><div className="empty-feed-art"><div className="empty-feed-orbit orbit-one"/><div className="empty-feed-orbit orbit-two"/><div className="empty-feed-icon"><FileVideo size={31}/></div></div><div className="eyebrow">ВАША МЕДИАТЕКА</div><h2>Здесь пока пусто</h2><p>Выберите ролик — он сразу добавится в медиатеку и останется на этом телефоне.</p><button className="button-primary empty-upload-button" onClick={onChooseVideo} disabled={savingVideo}><Video size={17}/> {savingVideo ? 'Сохраняю видео…' : 'Выбрать видео'}</button><div className="local-privacy-note"><ShieldCheck size={15}/> Файл хранится только на этом устройстве</div></section> : <div className="saved-video-list shorts-feed">{videos.map(video => <article className="saved-video-card shorts-card" key={video.id}><SavedVideoPlayer video={video}/><div className="saved-video-copy"><div className="saved-video-meta"><span className="saved-video-badge"><HardDrive size={12}/> МОЯ МЕДИАТЕКА</span><button className="saved-video-delete" aria-label="Удалить видео" onClick={() => { if (window.confirm('Удалить видео с этого устройства?')) onDeleteVideo(video); }}><Trash2 size={16}/></button></div><h2>{video.caption || video.name}</h2><p>{formatBytes(video.size)} · {new Date(video.createdAt).toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' })}</p></div></article>)}</div>}
    </div>
  </div>;
}

function formatBytes(bytes) {
  if (!bytes) return '0 Б';
  const units = ['Б', 'КБ', 'МБ', 'ГБ'];
  const unit = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** unit).toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

function ChatsPage() {
  return <div className="empty-section-page"><div className="page-heading"><div><div className="eyebrow">СООБЩЕНИЯ</div><h1>Чаты<span>.</span></h1><p>Личные и групповые переписки.</p></div></div><section className="empty-module"><div className="empty-module-icon"><MessageCircle size={27}/></div><h2>Чатов пока нет</h2><p>Здесь появятся ваши личные и групповые переписки.</p><span className="module-note">Сообщения между пользователями появятся после подключения сервера и аккаунтов.</span></section></div>;
}

function RoomsPage({ joinedRoom, onJoinRoom, onLeaveRoom }) {
  const micMuted = true;
  const current = rooms.find(room => room.id === joinedRoom);
  if (current) {
    const participants = [{ id: 'self', name: 'Вы', speaking: false, muted: micMuted }]
      .sort((left, right) => Number(right.speaking) - Number(left.speaking));
    return <div className="call-room-screen">
      <header className="call-room-header">
        <button className="call-back-button" onClick={onLeaveRoom} aria-label="Вернуться к комнатам"><ArrowLeft size={19}/></button>
        <div><span>ГОЛОСОВАЯ КОМНАТА</span><h1>{current.title}</h1></div>
        <span className="call-participant-count"><Users size={15}/> {participants.length}</span>
      </header>
      <section className="call-stage" aria-label="Участники голосовой комнаты">
        <div className="participant-grid">
          {participants.map(person => <article className={`participant-tile ${person.speaking ? 'participant-speaking' : ''}`} key={person.id}>
            <div className={`participant-avatar ${person.muted ? 'participant-muted' : ''}`}>{person.name === 'Вы' ? 'Я' : person.name.slice(0, 1)}</div>
            <span className="participant-name">{person.name}</span>
            <span className="participant-mic-state">{person.muted ? <MicOff size={14}/> : <Mic size={14}/>} {person.muted ? 'Микрофон выключен' : 'Микрофон включён'}</span>
          </article>)}
        </div>
        <div className="call-empty-note"><span className="call-status-pulse"/> Вы в комнате. Другие участники пока не подключены.</div>
      </section>
      <footer className="call-control-dock">
        <button className="call-mic-button is-muted" disabled aria-label="Микрофон недоступен без голосового сервера"><span><MicOff size={21}/></span><small>Микрофон недоступен</small></button>
        <button className="call-leave-button" onClick={onLeaveRoom}><ArrowDownLeft size={19}/> Выйти</button>
      </footer>
      <div className="call-backend-notice"><Headphones size={16}/><span>Передача голоса между телефонами пока не подключена. Нужен сервер голосовых комнат.</span></div>
    </div>;
  }
  return <div className="rooms-page">
    <div className="page-heading"><div><div className="eyebrow"><span className="live-dot"/> КОМНАТЫ ВСЕГДА ОТКРЫТЫ</div><h1>Голосовые<span>.</span></h1><p>Заходи в общую или выбери свой город.</p></div></div>
    <div className="rooms-section-title"><div><h2>Постоянные комнаты</h2><p>Войти можно в любой момент — не нужно ждать инициатора.</p></div></div>
    <div className="persistent-room-list">{rooms.map(room => <article className="persistent-room" key={room.id}><div className={`persistent-room-icon ${room.tone}`}>{room.global?<Radio size={22}/>:<MapPin size={22}/>}</div><div className="persistent-room-details"><div className="persistent-room-heading"><span className="persistent-room-type">{room.global?'ОБЩАЯ':'ГОРОДСКАЯ'}</span></div><h3>{room.title}</h3><p>{room.detail}</p><span className="persistent-room-status"><span className="empty-indicator"/> Комната доступна для входа</span></div><button className="room-enter-button" onClick={() => onJoinRoom(room.id)}>Зайти<ArrowUpRight size={15}/></button></article>)}</div>
    <div className="voice-requirement-note"><Headphones size={16}/><span>Здесь пока нет подключённого голосового сервера; экран комнаты показывает только локальный вход.</span></div>
  </div>;
}

function ProfilePage({ notify }) {
  const [name, setName] = useState(() => localStorage.getItem('ekip-profile-name') || '');
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(name);
  const save = () => {
    const clean = draft.trim();
    localStorage.setItem('ekip-profile-name', clean);
    setName(clean);
    setEditing(false);
    notify('Профиль сохранён на устройстве');
  };
  return <div className="profile-page"><div className="page-heading"><div><div className="eyebrow">ЛОКАЛЬНЫЙ ПРОФИЛЬ</div><h1>Профиль<span>.</span></h1><p>Данные сохраняются только на этом телефоне.</p></div></div><section className="empty-module profile-empty-module"><div className="profile-placeholder-avatar">{name ? name.slice(0,1).toUpperCase() : '?'}</div>{editing?<div className="profile-edit-local"><label htmlFor="profile-name">Как к вам обращаться?</label><input id="profile-name" maxLength={40} value={draft} onChange={e=>setDraft(e.target.value)} placeholder="Ваше имя"/><div><button className="button-secondary" onClick={() => setEditing(false)}>Отмена</button><button className="button-primary" onClick={save}>Сохранить</button></div></div>:<><h2>{name || 'Профиль не заполнен'}</h2><p>{name ? 'Имя сохранено на этом устройстве.' : 'Добавьте имя, чтобы видеть его рядом со своими видео.'}</p><button className="button-primary" onClick={() => { setDraft(name); setEditing(true); }}>{name?'Изменить имя':'Добавить имя'}</button></>}</section></div>;
}

function SettingsPage({ settings, setSettings, notify, otaStatus, updateInfo, onCheckUpdates, onApplyUpdate }) {
  const rows = [
    { key:'notifications', icon:Bell, title:'Уведомления', desc:'Сообщения, реакции и эфиры' },
    { key:'darkMode', icon:Activity, title:'Тёмная тема', desc:'Бережёт глаза в вечернюю смену' },
    { key:'location', icon:MapPin, title:'Показывать город', desc:'Чтобы находить курьеров рядом' },
  ];
  return <div className="settings-page"><div className="page-heading"><div><div className="eyebrow">ПОД ТВОЙ РИТМ</div><h1>Настройки<span>.</span></h1><p>Настрой экип так, как удобно тебе.</p></div></div><div className="settings-layout"><div className="settings-menu"><button className="selected"><Settings size={17}/> Основные</button><button onClick={() => notify('Настройки приватности')}><ShieldCheck size={17}/> Приватность</button><button onClick={() => notify('Настройки аккаунта')}><Users size={17}/> Аккаунт</button><button onClick={() => notify('Справка появится скоро')}><CircleHelp size={17}/> Помощь</button></div><section className="settings-card"><div className="settings-card-head"><div><h2>Основные</h2><p>Общие настройки приложения</p></div></div>{rows.map(({key,icon:Icon,title,desc})=><div className="setting-row" key={key}><div className="setting-icon"><Icon size={18}/></div><div className="setting-copy"><b>{title}</b><span>{desc}</span></div><button className={`toggle ${settings[key]?'on':''}`} onClick={() => { setSettings({...settings,[key]:!settings[key]}); notify(`${title}: ${settings[key]?'выключено':'включено'}`); }} aria-label={`Переключить ${title}`}><i/></button></div>)}<div className="settings-divider"/><section className="ota-card"><div className="ota-card-heading"><span className="setting-icon"><CloudDownload size={18}/></span><div><b>Обновление приложения</b><span>Загружать свежую версию по воздуху</span></div></div><div className={`ota-status ota-${otaStatus}`} role="status">{otaStatus === 'checking' ? 'Проверяю обновления…' : otaStatus === 'downloading' ? 'Загружаю и устанавливаю…' : otaStatus === 'available' ? `Доступно обновление ${updateInfo?.webVersion ?? ''}` : otaStatus === 'current' ? 'Установлена актуальная версия' : otaStatus === 'error' ? 'Проверка не удалась. Попробуйте ещё раз.' : otaStatus === 'native-only' ? 'Автообновление доступно в Android-приложении' : 'Проверить наличие обновлений'}</div><div className="ota-actions"><button className="button-secondary" onClick={onCheckUpdates} disabled={otaStatus === 'checking' || otaStatus === 'downloading'}><RefreshCw size={14}/> Проверить</button>{otaStatus === 'available'&&<button className="button-primary" onClick={onApplyUpdate}><CloudDownload size={14}/> Обновить</button>}</div><small>Обновляется интерфейс приложения. Для исправлений Android-части нужно установить новый APK.</small></section><button className="setting-link" onClick={() => notify('Экип · версия 1.0.0')}><span>О приложении</span><small>Версия 1.0.0</small><ChevronRight size={16}/></button><button className="setting-link" onClick={() => notify('Условия использования')}><span>Правила экипа</span><ChevronRight size={16}/></button></section></div></div>;
}

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));
}

createRoot(document.getElementById('root')).render(<App/>);
