const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { Server } = require('socket.io');

const HOST = process.env.HOST || '0.0.0.0';
const PORT = Number(process.env.PORT || 3000);
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, 'data');
const DATA_FILE = path.join(DATA_DIR, 'store.json');
const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_BODY_BYTES = 64 * 1024;
const MAX_MESSAGE_LENGTH = 2000;
const CATEGORIES = [
  { id: 'earnings', title: 'Заработок, бонусы и выплаты', description: 'Вопросы о выплатах и бонусах', icon: '💸' },
  { id: 'incidents', title: 'ДТП, угоны и помощь на смене', description: 'Ситуации на маршруте и взаимопомощь', icon: '🚨' },
  { id: 'news', title: 'Новости и слухи', description: 'Новости курьерской работы', icon: '📰' },
  { id: 'repair', title: 'Байки, аренда и ремонт', description: 'Транспорт, аренда и ремонт', icon: '🛠️' },
  { id: 'tips', title: 'Лайфхаки и полезные советы', description: 'Полезный опыт на каждый день', icon: '💡' },
  { id: 'market', title: 'Купить, продать, обменять', description: 'Объявления курьеров', icon: '🛒' },
  { id: 'gear', title: 'Экипировка и аксессуары', description: 'Снаряжение для работы', icon: '🦺' }
];

const ephemeralSecret = !process.env.JWT_SECRET;
const JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(48).toString('hex');
if (JWT_SECRET.length < 32) throw new Error('JWT_SECRET must contain at least 32 characters');
if (ephemeralSecret) console.warn('WARNING: JWT_SECRET is temporary. Set JWT_SECRET before using accounts beyond this test session.');

fs.mkdirSync(DATA_DIR, { recursive: true });
let store;
try {
  store = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  if (!Array.isArray(store.users) || !Array.isArray(store.messages) || !Array.isArray(store.reports)) throw new Error('Invalid data file');
} catch (error) {
  if (error.code !== 'ENOENT') console.warn('Could not load backend store; starting with an empty store:', error.message);
  store = { users: [], messages: [], reports: [] };
}
function saveStore() {
  const temp = DATA_FILE + '.tmp';
  fs.writeFileSync(temp, JSON.stringify(store), { mode: 0o600 });
  fs.renameSync(temp, DATA_FILE);
}

function json(res, status, value) {
  const body = Buffer.from(JSON.stringify(value));
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': body.length,
    'cache-control': 'no-store',
    'x-content-type-options': 'nosniff',
    'x-frame-options': 'DENY',
    'referrer-policy': 'no-referrer'
  });
  res.end(body);
}
function error(res, status, code, message) { json(res, status, { error: code, message }); }
function parseBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', chunk => {
      size += chunk.length;
      if (size > MAX_BODY_BYTES) {
        reject(Object.assign(new Error('Request body is too large'), { status: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(Object.assign(new Error('Expected a JSON request body'), { status: 400 })); }
    });
    req.on('error', reject);
  });
}
function tokenFor(user) {
  const payload = Buffer.from(JSON.stringify({ sub: user.id, exp: Date.now() + TOKEN_TTL_MS })).toString('base64url');
  const signature = crypto.createHmac('sha256', JWT_SECRET).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}
function userFromToken(token) {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 2) return null;
  const expected = crypto.createHmac('sha256', JWT_SECRET).update(parts[0]).digest();
  let actual;
  try { actual = Buffer.from(parts[1], 'base64url'); } catch { return null; }
  if (actual.length !== expected.length || !crypto.timingSafeEqual(actual, expected)) return null;
  try {
    const payload = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    if (!payload.sub || payload.exp < Date.now()) return null;
    return store.users.find(user => user.id === payload.sub) || null;
  } catch { return null; }
}
function bearerUser(req) {
  const header = req.headers.authorization || '';
  return userFromToken(header.startsWith('Bearer ') ? header.slice(7) : '');
}
function publicUser(user) { return { id: user.id, username: user.username, displayName: user.displayName, createdAt: user.createdAt }; }
function categoryById(id) { return CATEGORIES.find(category => category.id === id); }
function cleanMessage(message) {
  return { id: message.id, categoryId: message.categoryId, userId: message.userId,
    displayName: message.displayName, text: message.text, createdAt: message.createdAt };
}
function makeMessage(user, categoryId, rawText) {
  const text = typeof rawText === 'string' ? rawText.trim() : '';
  if (!categoryById(categoryId)) throw Object.assign(new Error('Unknown chat category'), { status: 400, code: 'unknown_category' });
  if (!text || text.length > MAX_MESSAGE_LENGTH) throw Object.assign(new Error(`Message must contain 1–${MAX_MESSAGE_LENGTH} characters`), { status: 400, code: 'invalid_message' });
  const message = { id: crypto.randomUUID(), categoryId, userId: user.id,
    displayName: user.displayName, text, createdAt: new Date().toISOString() };
  store.messages.push(message);
  if (store.messages.length > 10000) store.messages.splice(0, store.messages.length - 10000);
  saveStore();
  return cleanMessage(message);
}

const limits = new Map();
function rateLimited(req) {
  const key = req.socket.remoteAddress || 'unknown';
  const now = Date.now();
  const current = limits.get(key);
  if (!current || now - current.startedAt > 60_000) {
    limits.set(key, { startedAt: now, count: 1 });
    return false;
  }
  current.count++;
  return current.count > 180;
}

const server = http.createServer(async (req, res) => {
  const origin = req.headers.origin;
  res.setHeader('access-control-allow-origin', process.env.CORS_ORIGIN || '*');
  res.setHeader('access-control-allow-headers', 'authorization, content-type');
  res.setHeader('access-control-allow-methods', 'GET, POST, OPTIONS');
  if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }
  if (rateLimited(req)) return error(res, 429, 'rate_limited', 'Too many requests; try again in a minute.');

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  try {
    if (req.method === 'GET' && url.pathname === '/') {
      return json(res, 200, { name: 'Курьерка API', status: 'running', docs: '/api/health' });
    }
    if (req.method === 'GET' && url.pathname === '/api/health') {
      return json(res, 200, { ok: true, service: 'courierka-backend', categories: CATEGORIES.length,
        storage: 'single-process-json-prototype', persistentTokenSecret: !ephemeralSecret,
        features: { accounts: true, persistentTextChat: true, voicePresenceAndSignaling: true, voiceAudioRelay: false },
        warning: 'Prototype backend. Voice signaling only: this server does not relay or record audio.' });
    }
    if (req.method === 'GET' && url.pathname === '/api/categories') return json(res, 200, { categories: CATEGORIES });

    if (req.method === 'POST' && url.pathname === '/api/auth/register') {
      const body = await parseBody(req);
      const username = typeof body.username === 'string' ? body.username.trim().toLowerCase() : '';
      const displayName = typeof body.displayName === 'string' ? body.displayName.trim() : '';
      const password = typeof body.password === 'string' ? body.password : '';
      if (!/^[a-z0-9_.-]{3,24}$/.test(username)) return error(res, 400, 'invalid_username', 'Username must be 3–24 characters: letters, digits, dot, underscore or dash.');
      if (displayName.length < 2 || displayName.length > 40) return error(res, 400, 'invalid_display_name', 'Display name must be 2–40 characters.');
      if (password.length < 10 || password.length > 128) return error(res, 400, 'weak_password', 'Password must contain 10–128 characters.');
      if (store.users.some(user => user.username === username)) return error(res, 409, 'username_taken', 'This username is already registered.');
      const salt = crypto.randomBytes(16).toString('hex');
      const passwordHash = crypto.scryptSync(password, salt, 64).toString('hex');
      const user = { id: crypto.randomUUID(), username, displayName, salt, passwordHash, createdAt: new Date().toISOString() };
      store.users.push(user);
      saveStore();
      return json(res, 201, { token: tokenFor(user), user: publicUser(user) });
    }
    if (req.method === 'POST' && url.pathname === '/api/auth/login') {
      const body = await parseBody(req);
      const username = typeof body.username === 'string' ? body.username.trim().toLowerCase() : '';
      const password = typeof body.password === 'string' ? body.password : '';
      const user = store.users.find(item => item.username === username);
      if (!user) return error(res, 401, 'invalid_credentials', 'Username or password is incorrect.');
      const candidate = crypto.scryptSync(password, user.salt, 64);
      const expected = Buffer.from(user.passwordHash, 'hex');
      if (candidate.length !== expected.length || !crypto.timingSafeEqual(candidate, expected)) return error(res, 401, 'invalid_credentials', 'Username or password is incorrect.');
      return json(res, 200, { token: tokenFor(user), user: publicUser(user) });
    }

    const user = bearerUser(req);
    if (!user && (url.pathname === '/api/me' || url.pathname.startsWith('/api/messages') || url.pathname === '/api/reports')) {
      return error(res, 401, 'unauthorized', 'Sign in to continue.');
    }
    if (req.method === 'GET' && url.pathname === '/api/me') return json(res, 200, { user: publicUser(user) });
    if (req.method === 'GET' && url.pathname === '/api/messages') {
      const categoryId = url.searchParams.get('category');
      if (!categoryById(categoryId)) return error(res, 400, 'unknown_category', 'Unknown chat category.');
      const limit = Math.max(1, Math.min(100, Number(url.searchParams.get('limit') || 50)));
      const messages = store.messages.filter(item => item.categoryId === categoryId).slice(-limit).map(cleanMessage);
      return json(res, 200, { messages });
    }
    if (req.method === 'POST' && url.pathname === '/api/messages') {
      const body = await parseBody(req);
      const message = makeMessage(user, body.categoryId, body.text);
      io.to(`chat:${message.categoryId}`).emit('chat:message', message);
      return json(res, 201, { message });
    }
    if (req.method === 'POST' && url.pathname === '/api/reports') {
      const body = await parseBody(req);
      if (typeof body.messageId !== 'string' || !store.messages.some(item => item.id === body.messageId)) return error(res, 404, 'message_not_found', 'Message not found.');
      const reason = typeof body.reason === 'string' ? body.reason.trim().slice(0, 500) : '';
      if (!reason) return error(res, 400, 'invalid_reason', 'Provide a short report reason.');
      const report = { id: crypto.randomUUID(), messageId: body.messageId, reporterId: user.id, reason, createdAt: new Date().toISOString() };
      store.reports.push(report);
      saveStore();
      return json(res, 201, { accepted: true, reportId: report.id });
    }
    return error(res, 404, 'not_found', 'Route not found.');
  } catch (err) {
    if (!res.headersSent) return error(res, err.status || 500, err.code || 'server_error', err.status ? err.message : 'Unexpected server error.');
    res.destroy();
  }
});

const io = new Server(server, {
  cors: { origin: process.env.CORS_ORIGIN || '*', methods: ['GET', 'POST'] },
  maxHttpBufferSize: 128 * 1024,
  pingInterval: 25_000,
  pingTimeout: 20_000
});
const userSockets = new Map();
const voiceRooms = new Map();
function addSocket(userId, socketId) {
  if (!userSockets.has(userId)) userSockets.set(userId, new Set());
  userSockets.get(userId).add(socketId);
}
function removeSocket(userId, socketId) {
  const sockets = userSockets.get(userId);
  if (!sockets) return;
  sockets.delete(socketId);
  if (!sockets.size) userSockets.delete(userId);
}
function voiceSnapshot(roomId, excludingUserId) {
  const room = voiceRooms.get(roomId);
  return room ? [...room.values()].filter(person => person.userId !== excludingUserId).map(({ userId, displayName, micEnabled }) => ({ userId, displayName, micEnabled })) : [];
}
function leaveVoice(socket) {
  const roomId = socket.data.voiceRoomId;
  if (!roomId) return;
  const room = voiceRooms.get(roomId);
  if (room) {
    const person = room.get(socket.data.user.id);
    if (person) {
      person.sockets.delete(socket.id);
      if (!person.sockets.size) {
        room.delete(socket.data.user.id);
        socket.to(`voice:${roomId}`).emit('voice:participant-left', { userId: socket.data.user.id });
      }
    }
    if (!room.size) voiceRooms.delete(roomId);
  }
  socket.leave(`voice:${roomId}`);
  socket.data.voiceRoomId = null;
}

io.use((socket, next) => {
  const user = userFromToken(socket.handshake.auth && socket.handshake.auth.token);
  if (!user) return next(new Error('unauthorized'));
  socket.data.user = user;
  next();
});
io.on('connection', socket => {
  const user = socket.data.user;
  addSocket(user.id, socket.id);
  socket.emit('session:ready', { user: publicUser(user) });

  socket.on('chat:join', ({ categoryId } = {}, ack = () => {}) => {
    if (!categoryById(categoryId)) return ack({ error: 'unknown_category' });
    socket.join(`chat:${categoryId}`);
    const messages = store.messages.filter(item => item.categoryId === categoryId).slice(-50).map(cleanMessage);
    ack({ messages });
  });
  socket.on('chat:leave', ({ categoryId } = {}) => {
    if (categoryById(categoryId)) socket.leave(`chat:${categoryId}`);
  });
  socket.on('chat:send', ({ categoryId, text } = {}, ack = () => {}) => {
    try {
      const message = makeMessage(user, categoryId, text);
      io.to(`chat:${categoryId}`).emit('chat:message', message);
      ack({ message });
    } catch (err) { ack({ error: err.code || 'invalid_message', message: err.message }); }
  });
  socket.on('voice:join', ({ roomId } = {}, ack = () => {}) => {
    if (typeof roomId !== 'string' || !/^[a-z0-9:_-]{1,80}$/i.test(roomId)) return ack({ error: 'invalid_room' });
    if (socket.data.voiceRoomId && socket.data.voiceRoomId !== roomId) leaveVoice(socket);
    if (!voiceRooms.has(roomId)) voiceRooms.set(roomId, new Map());
    const room = voiceRooms.get(roomId);
    const existing = room.get(user.id);
    const participants = voiceSnapshot(roomId, user.id);
    if (!existing) room.set(user.id, { userId: user.id, displayName: user.displayName, micEnabled: false, sockets: new Set([socket.id]) });
    else existing.sockets.add(socket.id);
    socket.join(`voice:${roomId}`);
    socket.data.voiceRoomId = roomId;
    ack({ roomId, participants, audioTransport: 'webrtc-peer-signaling-only' });
    if (!existing) socket.to(`voice:${roomId}`).emit('voice:participant-joined', { userId: user.id, displayName: user.displayName, micEnabled: false });
  });
  socket.on('voice:leave', () => leaveVoice(socket));
  socket.on('voice:mic', ({ enabled } = {}, ack = () => {}) => {
    const roomId = socket.data.voiceRoomId;
    if (!roomId || typeof enabled !== 'boolean') return ack({ error: 'not_in_voice_room' });
    const room = voiceRooms.get(roomId);
    const person = room && room.get(user.id);
    if (!person) return ack({ error: 'not_in_voice_room' });
    person.micEnabled = enabled;
    io.to(`voice:${roomId}`).emit('voice:mic', { userId: user.id, enabled });
    ack({ ok: true });
  });
  socket.on('voice:signal', ({ targetUserId, description, candidate } = {}, ack = () => {}) => {
    const roomId = socket.data.voiceRoomId;
    const room = roomId && voiceRooms.get(roomId);
    if (!room || !room.has(targetUserId) || targetUserId === user.id) return ack({ error: 'target_not_in_room' });
    const data = { fromUserId: user.id };
    if (description && JSON.stringify(description).length <= 96 * 1024) data.description = description;
    else if (candidate && JSON.stringify(candidate).length <= 8 * 1024) data.candidate = candidate;
    else return ack({ error: 'invalid_signal' });
    const target = room.get(targetUserId);
    for (const socketId of target.sockets) io.to(socketId).emit('voice:signal', data);
    ack({ ok: true });
  });
  socket.on('disconnect', () => {
    leaveVoice(socket);
    removeSocket(user.id, socket.id);
  });
});

server.listen(PORT, HOST, () => {
  console.log(`Courierka backend listening on http://${HOST}:${PORT}`);
  console.log(`Health: http://localhost:${PORT}/api/health`);
});

function shutdown(signal) {
  console.log(`${signal}: shutting down`);
  io.close(() => server.close(() => process.exit(0)));
  setTimeout(() => process.exit(1), 5000).unref();
}
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
