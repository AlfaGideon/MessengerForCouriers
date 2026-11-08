const DB_NAME = 'ekip-local-media';
const STORE_NAME = 'videos';
const DB_VERSION = 1;

function openDatabase() {
  return new Promise((resolve, reject) => {
    if (!('indexedDB' in window)) {
      reject(new Error('На этом устройстве недоступно локальное хранилище видео.'));
      return;
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const store = database.createObjectStore(STORE_NAME, { keyPath: 'id' });
        store.createIndex('createdAt', 'createdAt');
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('Не удалось открыть хранилище.'));
  });
}

export async function listSavedVideos() {
  const db = await openDatabase();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readonly');
    const request = transaction.objectStore(STORE_NAME).getAll();
    request.onsuccess = () => resolve(request.result.sort((a, b) => b.createdAt - a.createdAt));
    request.onerror = () => reject(request.error || new Error('Не удалось загрузить сохранённые видео.'));
    transaction.oncomplete = () => db.close();
    transaction.onerror = () => { db.close(); reject(transaction.error); };
  });
}

export async function saveVideo(file, caption) {
  const db = await openDatabase();
  const item = {
    id: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    name: file.name || 'video',
    type: file.type || 'video/mp4',
    size: file.size,
    caption: caption.trim(),
    createdAt: Date.now(),
    blob: file,
  };
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite');
    transaction.objectStore(STORE_NAME).put(item);
    transaction.oncomplete = () => { db.close(); resolve(item); };
    transaction.onerror = () => {
      db.close();
      const error = transaction.error;
      reject(error?.name === 'QuotaExceededError'
        ? new Error('На устройстве не хватает места. Удалите старые ролики или выберите видео поменьше.')
        : new Error('Не удалось сохранить видео на устройстве.'));
    };
    transaction.onabort = () => { db.close(); reject(transaction.error || new Error('Сохранение отменено.')); };
  });
}

export async function deleteSavedVideo(id) {
  const db = await openDatabase();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite');
    transaction.objectStore(STORE_NAME).delete(id);
    transaction.oncomplete = () => { db.close(); resolve(); };
    transaction.onerror = () => { db.close(); reject(transaction.error || new Error('Не удалось удалить видео.')); };
  });
}
