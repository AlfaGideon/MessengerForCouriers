const assert = require('node:assert/strict');
const { io } = require('socket.io-client');

const base = process.env.BACKEND_URL || 'http://127.0.0.1:3000';
const timeout = (promise, ms = 5000) => Promise.race([
  promise,
  new Promise((_, reject) => setTimeout(() => reject(new Error(`Timed out after ${ms}ms`)), ms))
]);
function waitEvent(socket, event) {
  return timeout(new Promise(resolve => socket.once(event, resolve)));
}
function emitAck(socket, event, data) {
  return timeout(new Promise(resolve => socket.emit(event, data, resolve)));
}
async function register(n) {
  const response = await fetch(`${base}/api/auth/register`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: `smoke_${Date.now()}_${n}`, displayName: `Тест ${n}`, password: 'demo-password-123' })
  });
  assert.equal(response.status, 201);
  return response.json();
}
async function api(token, route, method = 'GET', body) {
  const response = await fetch(`${base}${route}`, {
    method, headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const json = await response.json();
  assert.equal(response.ok, true, JSON.stringify(json));
  return json;
}
async function main() {
  const health = await (await fetch(`${base}/api/health`)).json();
  assert.equal(health.ok, true);
  assert.equal(health.categories, 7);
  assert.equal(health.features.voiceAudioRelay, false);

  const [a, b] = await Promise.all([register(1), register(2)]);
  await api(a.token, '/api/voice/join', 'POST', { roomId: 'global' });
  await api(b.token, '/api/voice/join', 'POST', { roomId: 'global' });
  let participants = await api(a.token, '/api/voice/participants?room=global');
  assert.equal(participants.participants.length, 1);
  await api(b.token, '/api/voice/mic', 'POST', { enabled: true });
  participants = await api(a.token, '/api/voice/participants?room=global');
  assert.equal(participants.participants[0].micEnabled, true);
  await api(b.token, '/api/voice/leave', 'POST', {});

  const first = io(base, { auth: { token: a.token }, reconnection: false });
  const second = io(base, { auth: { token: b.token }, reconnection: false });
  try {
    await Promise.all([waitEvent(first, 'session:ready'), waitEvent(second, 'session:ready')]);
    const joinChat = await emitAck(first, 'chat:join', { categoryId: 'earnings' });
    assert.ok(Array.isArray(joinChat.messages));
    const messageText = `Проверка связи ${Date.now()}`;
    const receivedMessage = waitEvent(first, 'chat:message');
    const sent = await emitAck(first, 'chat:send', { categoryId: 'earnings', text: messageText });
    assert.equal(sent.message.text, messageText);
    assert.equal((await receivedMessage).id, sent.message.id);

    const firstVoiceJoin = await emitAck(first, 'voice:join', { roomId: 'global' });
    assert.equal(firstVoiceJoin.audioTransport, 'webrtc-peer-signaling-only');
    const participantJoined = waitEvent(first, 'voice:participant-joined');
    const secondVoiceJoin = await emitAck(second, 'voice:join', { roomId: 'global' });
    assert.equal(secondVoiceJoin.participants.length, 1);
    await participantJoined;

    const muteUpdate = waitEvent(first, 'voice:mic');
    assert.deepEqual(await emitAck(second, 'voice:mic', { enabled: true }), { ok: true });
    const mute = await muteUpdate;
    assert.equal(mute.userId, b.user.id);
    assert.equal(mute.enabled, true);

    const signal = waitEvent(second, 'voice:signal');
    const signalAck = await emitAck(first, 'voice:signal', {
      targetUserId: b.user.id,
      description: { type: 'offer', sdp: 'test-sdp' }
    });
    assert.deepEqual(signalAck, { ok: true });
    assert.equal((await signal).description.type, 'offer');
    console.log('Backend smoke test passed: auth, categories, persistent/realtime chat, HTTP voice presence/mute, Socket.IO presence/mute and signaling relay. Audio is not relayed.');
  } finally {
    first.close();
    second.close();
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
