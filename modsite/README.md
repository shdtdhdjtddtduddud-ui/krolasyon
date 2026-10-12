# Krolasyon Mods

Self-hosted Minecraft mod sharing platform (Modrinth-style): accounts, mod pages with versions, gallery and Markdown descriptions,
search & filters, comments with replies, follows, notifications, reports, a moderation queue, ad slots and **creator revenue sharing**
(monthly ad revenue split between creators by unique views and downloads, manual payouts). Turkish and English UI, light/dark theme.

- **Stack:** Node.js 20+, Express 5, SQLite (better-sqlite3), EJS, no build step
- **Data:** everything lives in `data/` (database, uploaded files, images)
- **Deploy:** `docker compose up -d` with Caddy for automatic HTTPS - see [DEPLOY.md](DEPLOY.md) (Turkish, step by step)

## Local development

```bash
npm install
npm start        # http://localhost:3000 - the first account you register becomes admin
npm test         # end-to-end flow test
```

## Configuration (`.env`)

| Variable | Default | |
|---|---|---|
| `SITE_NAME` | Krolasyon Mods | shown everywhere |
| `SITE_URL` | http://localhost:3000 | public URL (https enables secure cookies) |
| `CONTACT_EMAIL` | admin@example.com | legal pages / footer |
| `SESSION_SECRET` | generated into `data/.session-secret` | |
| `DATA_DIR` | `./data` | |
| `PORT` | 3000 | |
| `MAX_FILE_MB` / `MAX_IMAGE_MB` | 100 / 5 | upload limits |

Ads, revenue share %, view/download weights, currency, minimum payout, announcement bar and registrations are edited in **Admin → Site settings**.

## Revenue sharing

1. Approved, monetized projects collect points from unique daily views (1 point) and downloads (5 points); owners' own visits and bots are ignored.
2. After a month ends the admin enters that month's ad revenue in **Admin → Revenue sharing**.
3. `pool = revenue × creator share %` is split between projects by their share of points and credited to creator balances.
4. Creators request payouts (PayPal / Papara / IBAN); the admin pays manually and marks them paid.

## Public API

- `GET /api/v1/search?q=&limit=`
- `GET /api/v1/project/:slug` (includes versions, file hashes and download URLs)
