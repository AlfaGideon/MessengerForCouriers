# Курьерка backend (prototype)

This service is a first network backend for the app: account registration/login, the seven chat categories shown in the Android UI, persistent plain-text chat messages, report submission, voice-room presence/mute state, and optional Socket.IO/WebRTC signaling events. The current Android app uses authenticated HTTP polling for chat and voice presence/mute state.

**This is a prototype, not a production service.** The current Android client can register/login and exchange text messages through it; it also sends voice-room presence and mute state. The server's WebRTC signaling endpoints are only a foundation: this process does **not** transmit or relay audio, and the Android client does not yet negotiate peer audio. Do not advertise network voice as working yet.

## One-click Windows launcher

Install Docker Desktop and run the repository-root `StartCourierka.bat`. It uses the backend files already in this repository (fetching the required files only if the adjacent `backend` folder is missing), downloads `cloudflared` when needed, creates a random JWT secret, starts the Docker service, opens a temporary public Quick Tunnel and copies its URL. Set that URL in **Ещё → Сервер сообщества** on every phone. The tunnel address changes on restart and is only suitable for testing.

## Run locally

Requirements: Node.js 20+ and npm.

```sh
cd backend
npm ci
JWT_SECRET="$(openssl rand -hex 48)" npm start
```

The server listens on `http://0.0.0.0:3000`. Check it with:

```sh
curl http://localhost:3000/api/health
curl http://localhost:3000/api/categories
npm run test:smoke
```

If `JWT_SECRET` is omitted, a temporary random secret is used. Accounts/messages remain in `backend/data/store.json`, but all issued tokens stop working after restart. For a persistent deployment set the same random secret and keep the data directory on persistent storage. Never commit `.env`, the secret, or the user-data directory.

## Docker

```sh
cd backend
cp .env.example .env
# Edit .env and set JWT_SECRET to a random value from: openssl rand -hex 48
docker compose up -d --build
```

The container publishes port 3000 only on localhost, suitable for a local Cloudflare Tunnel origin. The named Docker volume keeps the JSON store across container rebuilds. Back up that volume.

For a temporary remote test, Cloudflare Tunnel can publish the local HTTP service under a public hostname; a quick tunnel address is temporary. A stable named tunnel generally requires control of a hostname/domain. The computer and tunnel process must stay on. Cloudflare's docs describe publishing local services through a tunnel: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/routing-to-tunnel/.

## HTTP API

- `GET /api/health` — health and explicit capability flags.
- `GET /api/categories` — public category metadata.
- `POST /api/auth/register` — JSON `{ "username", "displayName", "password" }`; returns a bearer token and public user. Password is hashed with Node scrypt.
- `POST /api/auth/login` — JSON `{ "username", "password" }`.
- `GET /api/me` — requires `Authorization: Bearer <token>`.
- `GET /api/messages?category=earnings&limit=50` — authenticated message history.
- `POST /api/messages` — authenticated JSON `{ "categoryId", "text" }`; emits `chat:message` to subscribed sockets.
- `POST /api/reports` — authenticated JSON `{ "messageId", "reason" }`.

Socket.IO clients authenticate with `auth: { token }` and can use `chat:join`, `chat:send`, `chat:leave`, `voice:join`, `voice:mic`, `voice:signal`, and `voice:leave`. Voice signal payloads relay SDP/ICE data only; audio stays outside this server.

## Before inviting the public

This prototype uses a single-process JSON file: it is not safe to run multiple replicas and is not a substitute for PostgreSQL backups/migrations. There is no admin/moderation dashboard, password reset, account recovery, user blocking, privacy/retention workflow, abuse prevention beyond a basic in-memory IP limiter, or production monitoring. Add these, persistent database storage, HTTPS/public hostname, secret management, and Android client integration before using it as a public community service.
