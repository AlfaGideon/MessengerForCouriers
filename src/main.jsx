import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity, ArrowDownLeft, ArrowLeft, ArrowUpRight, Bell, Bookmark, Check, ChevronDown, ChevronRight,
  CircleHelp, Compass, FileVideo, HardDrive, Headphones, Heart, Home, ImagePlus, MapPin, MessageCircle,
  Mic, MicOff, MoreHorizontal, Plus, Search, Send, Settings, Share2, ShieldCheck,
  SlidersHorizontal, Sparkles, ThumbsUp, Trash2, Users, Video, Volume2, X, Zap
} from 'lucide-react';
import './style.css';
import { deleteSavedVideo, listSavedVideos, saveVideo } from './storage.js';

const people = [];
const initialChats = [];
const rooms = [];

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
  const [showUpload, setShowUpload] = useState(false);
  const [toast, setToast] = useState('');
  const [activeChat, setActiveChat] = useState(0);
  const [chatMessage, setChatMessage] = useState('');
  const [sentMessages, setSentMessages] = useState([]);
  const [muted, setMuted] = useState(false);
  const [joinedRoom, setJoinedRoom] = useState('');
  const [search, setSearch] = useState('');
  const [settings, setSettings] = useState({ notifications: true, darkMode: false, location: true });
  const [videos, setVideos] = useState([]);
  const [videosLoading, setVideosLoading] = useState(true);

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
    setShowUpload(false);
    notify('Видео сохранено на этом телефоне');
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
  const joinRoom = (title) => {
    setJoinedRoom(joinedRoom === title ? '' : title);
    notify(joinedRoom === title ? 'Вы вышли из комнаты' : `Вы в комнате «${title}»`);
  };

  return <div className={`app-shell ${settings.darkMode ? 'dark-mode' : ''}`}>
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
      <button className="side-room side-room-empty" onClick={() => navigate('rooms')}><span>Пока нет комнат — создать</span><Plus size={14}/></button>
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
        {page === 'feed' && <FeedPage {...{ filter, setFilter, videos, videosLoading, onDeleteVideo: removeLocalVideo, setShowUpload, notify }} />}
        {page === 'chats' && <ChatsPage {...{ activeChat, setActiveChat, search, setSearch, sentMessages, chatMessage, setChatMessage, sendMessage, notify }} />}
        {page === 'rooms' && <RoomsPage {...{ joinedRoom, joinRoom, muted, setMuted, notify }} />}
        {page === 'profile' && <ProfilePage {...{ navigate, notify }} />}
        {page === 'settings' && <SettingsPage {...{ settings, setSettings, notify }} />}
      </div>
      <nav className="mobile-nav">
        {nav.map(({id,label,icon:Icon,badge}) => <button key={id} className={page === id ? 'active' : ''} onClick={() => navigate(id)}><span className="mobile-icon-wrap"><Icon size={20}/>{badge && <i>{badge}</i>}</span><small>{label}</small></button>)}
        <button className={page === 'settings' ? 'active' : ''} onClick={() => navigate('settings')}><span className="mobile-icon-wrap"><Settings size={20}/></span><small>Ещё</small></button>
      </nav>
    </main>

    <aside className="rightbar"><div className="local-storage-panel"><div className="empty-module-icon"><HardDrive size={24}/></div><span className="eyebrow">ЛОКАЛЬНЫЙ РЕЖИМ</span><h3>Ваши видео — на вашем устройстве</h3><p>Файлы остаются в хранилище приложения. Они не загружаются в общую сеть и не видны другим пользователям.</p></div></aside>

    {showUpload && <UploadModal onClose={() => setShowUpload(false)} onDone={publishLocalVideo} />}
    {toast && <div className="toast"><span className="toast-check"><Check size={14}/></span>{toast}</div>}
  </div>;
}

function FeedPage({ videos, videosLoading, onDeleteVideo, setShowUpload }) {
  return <div className="feed-layout">
    <div className="feed-column">
      <div className="welcome-row"><div><div className="eyebrow"><span className="welcome-dot"/> ЭКИП · МОЯ МЕДИАТЕКА</div><h1>Мои видео</h1><p>Добавьте ролик с телефона — он останется на устройстве.</p></div><button className="button-primary create-video" onClick={() => setShowUpload(true)}><Plus size={17}/> Добавить видео</button></div>
      <div className="route-status-strip"><span className="route-status-icon"><HardDrive size={16}/></span><span className="route-status-copy"><b>Локальное хранение</b><small><i/> Видео сохраняются в памяти приложения</small></span><span className="local-only-badge">ТОЛЬКО ВАМ</span></div>
      <div className="feed-filter-row"><div className="feed-tabs"><button className="selected">Медиатека <span className="video-count">{videos.length}</span></button></div><span className="local-feed-hint"><HardDrive size={13}/> Без облачной публикации</span></div>
      {videosLoading ? <div className="empty-feed loading-feed"><div className="storage-spinner"/><b>Проверяю сохранённые видео…</b></div> : videos.length === 0 ? <section className="empty-feed"><div className="empty-feed-art"><div className="empty-feed-orbit orbit-one"/><div className="empty-feed-orbit orbit-two"/><div className="empty-feed-icon"><FileVideo size={31}/></div></div><div className="eyebrow">ВАША МЕДИАТЕКА</div><h2>Здесь пока пусто</h2><p>Выберите видео из галереи телефона. Оно будет сохранено в приложении и останется доступно после перезапуска.</p><button className="button-primary empty-upload-button" onClick={() => setShowUpload(true)}><Video size={17}/> Выбрать видео</button><div className="local-privacy-note"><ShieldCheck size={15}/> Файл хранится только на этом устройстве</div></section> : <div className="saved-video-list">{videos.map(video => <article className="saved-video-card" key={video.id}><video className="saved-video-player" controls playsInline preload="metadata" src={video.url}/><div className="saved-video-copy"><div className="saved-video-meta"><span className="saved-video-badge"><HardDrive size={12}/> НА УСТРОЙСТВЕ</span><button className="saved-video-delete" aria-label="Удалить видео" onClick={() => { if (window.confirm('Удалить видео с этого устройства?')) onDeleteVideo(video); }}><Trash2 size={16}/></button></div><h2>{video.caption || video.name}</h2><p>{video.name} · {formatBytes(video.size)} · {new Date(video.createdAt).toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' })}</p></div></article>)}</div>}
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

function RoomsPage() {
  return <div className="empty-section-page"><div className="page-heading"><div><div className="eyebrow"><span className="live-dot"/> ГОЛОСОВАЯ СВЯЗЬ</div><h1>Комнаты<span>.</span></h1><p>Голосовые комнаты для общения во время смены.</p></div></div><section className="empty-module"><div className="empty-module-icon"><Headphones size={27}/></div><h2>Комнат пока нет</h2><p>Список комнат будет здесь, когда подключится сообщество.</p><span className="module-note">Для настоящих голосовых звонков нужен сервер сигнализации/WebRTC.</span></section></div>;
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

function SettingsPage({ settings, setSettings, notify }) {
  const rows = [
    { key:'notifications', icon:Bell, title:'Уведомления', desc:'Сообщения, реакции и эфиры' },
    { key:'darkMode', icon:Activity, title:'Тёмная тема', desc:'Бережёт глаза в вечернюю смену' },
    { key:'location', icon:MapPin, title:'Показывать город', desc:'Чтобы находить курьеров рядом' },
  ];
  return <div className="settings-page"><div className="page-heading"><div><div className="eyebrow">ПОД ТВОЙ РИТМ</div><h1>Настройки<span>.</span></h1><p>Настрой экип так, как удобно тебе.</p></div></div><div className="settings-layout"><div className="settings-menu"><button className="selected"><Settings size={17}/> Основные</button><button onClick={() => notify('Настройки приватности')}><ShieldCheck size={17}/> Приватность</button><button onClick={() => notify('Настройки аккаунта')}><Users size={17}/> Аккаунт</button><button onClick={() => notify('Справка появится скоро')}><CircleHelp size={17}/> Помощь</button></div><section className="settings-card"><div className="settings-card-head"><div><h2>Основные</h2><p>Общие настройки приложения</p></div></div>{rows.map(({key,icon:Icon,title,desc})=><div className="setting-row" key={key}><div className="setting-icon"><Icon size={18}/></div><div className="setting-copy"><b>{title}</b><span>{desc}</span></div><button className={`toggle ${settings[key]?'on':''}`} onClick={() => { setSettings({...settings,[key]:!settings[key]}); notify(`${title}: ${settings[key]?'выключено':'включено'}`); }} aria-label={`Переключить ${title}`}><i/></button></div>)}<div className="settings-divider"/><button className="setting-link" onClick={() => notify('Версия 1.0 · Экип')}><span>О приложении</span><small>Версия 1.0.0</small><ChevronRight size={16}/></button><button className="setting-link" onClick={() => notify('Условия использования')}><span>Правила экипа</span><ChevronRight size={16}/></button></section></div></div>;
}

function UploadModal({ onClose, onDone }) {
  const [file, setFile] = useState(null);
  const [caption, setCaption] = useState('');
  const [previewUrl, setPreviewUrl] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!file) { setPreviewUrl(''); return; }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const submit = async () => {
    if (!file) { setError('Сначала выберите видео из галереи.'); return; }
    setSaving(true);
    setError('');
    try {
      await onDone({ file, caption });
    } catch (err) {
      setError(err.message || 'Не получилось сохранить видео.');
      setSaving(false);
    }
  };

  return <div className="modal-backdrop" onClick={onClose}><div className="upload-modal" onClick={e=>e.stopPropagation()}>
    <div className="modal-head"><div><span className="eyebrow">ЛОКАЛЬНОЕ СОХРАНЕНИЕ</span><h2>Добавить видео<span>.</span></h2></div><button className="icon-button" onClick={onClose} aria-label="Закрыть"><X size={19}/></button></div>
    <p className="modal-subtitle">Выберите видео из памяти телефона. Оно сохранится в этой копии приложения и будет доступно после перезапуска.</p>
    <label className={`upload-drop ${file?'has-file':''}`}><input type="file" accept="video/*" onChange={e=>{setFile(e.target.files?.[0]||null);setError('');}}/>
      {file && previewUrl ? <video className="upload-preview" src={previewUrl} muted playsInline/> : <span className="upload-icon"><Video size={23}/></span>}
      <b>{file?.name||'Выберите видео на телефоне'}</b><small>{file?`${formatBytes(file.size)} · ${file.type||'видеофайл'}`:'MP4, MOV и другие видеоформаты'}</small><span className="button-secondary upload-choose"><ImagePlus size={15}/> {file?'Выбрать другое':'Открыть галерею'}</span>
    </label>
    <label className="upload-caption">Подпись<textarea value={caption} onChange={e=>setCaption(e.target.value)} placeholder="Добавьте заметку к видео (необязательно)" rows="3"/></label>
    <div className="upload-local-note"><HardDrive size={14}/> Видео останется на этом телефоне и не появится у других пользователей.</div>
    {error&&<div className="upload-error" role="alert">{error}</div>}
    <div className="upload-modal-actions"><button className="button-secondary" onClick={onClose} disabled={saving}>Отмена</button><button className="button-primary" onClick={submit} disabled={!file||saving}>{saving?'Сохраняю…':<><HardDrive size={16}/> Сохранить видео</>}</button></div>
  </div></div>;
}

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));
}

createRoot(document.getElementById('root')).render(<App/>);
