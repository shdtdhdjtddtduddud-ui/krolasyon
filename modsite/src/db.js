'use strict';
const Database = require('better-sqlite3');
const config = require('./config');

const db = new Database(config.DB_FILE);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');
db.pragma('busy_timeout = 5000');

const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  email TEXT NOT NULL UNIQUE COLLATE NOCASE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'user',          -- user | moderator | admin
  bio TEXT NOT NULL DEFAULT '',
  avatar TEXT,
  banned INTEGER NOT NULL DEFAULT 0,
  payout_method TEXT NOT NULL DEFAULT '',     -- paypal | papara | iban
  payout_details TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
  id INTEGER PRIMARY KEY,
  slug TEXT NOT NULL UNIQUE COLLATE NOCASE,
  owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  summary TEXT NOT NULL DEFAULT '',
  description TEXT NOT NULL DEFAULT '',
  icon TEXT,
  license TEXT NOT NULL DEFAULT 'ARR',
  source_url TEXT NOT NULL DEFAULT '',
  issues_url TEXT NOT NULL DEFAULT '',
  discord_url TEXT NOT NULL DEFAULT '',
  categories TEXT NOT NULL DEFAULT '',        -- comma separated keys
  loaders TEXT NOT NULL DEFAULT '',
  game_versions TEXT NOT NULL DEFAULT '',
  client_side TEXT NOT NULL DEFAULT 'required',
  server_side TEXT NOT NULL DEFAULT 'required',
  status TEXT NOT NULL DEFAULT 'draft',       -- draft | review | approved | rejected | unlisted
  moderation_note TEXT NOT NULL DEFAULT '',
  monetized INTEGER NOT NULL DEFAULT 1,
  downloads INTEGER NOT NULL DEFAULT 0,
  views INTEGER NOT NULL DEFAULT 0,
  follows INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  published_at INTEGER
);
CREATE INDEX IF NOT EXISTS projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS projects_owner ON projects(owner_id);

CREATE VIRTUAL TABLE IF NOT EXISTS projects_fts USING fts5(name, summary, description, content='projects', content_rowid='id');
CREATE TRIGGER IF NOT EXISTS projects_ai AFTER INSERT ON projects BEGIN
  INSERT INTO projects_fts(rowid, name, summary, description) VALUES (new.id, new.name, new.summary, new.description);
END;
CREATE TRIGGER IF NOT EXISTS projects_ad AFTER DELETE ON projects BEGIN
  INSERT INTO projects_fts(projects_fts, rowid, name, summary, description) VALUES ('delete', old.id, old.name, old.summary, old.description);
END;
CREATE TRIGGER IF NOT EXISTS projects_au AFTER UPDATE OF name, summary, description ON projects BEGIN
  INSERT INTO projects_fts(projects_fts, rowid, name, summary, description) VALUES ('delete', old.id, old.name, old.summary, old.description);
  INSERT INTO projects_fts(rowid, name, summary, description) VALUES (new.id, new.name, new.summary, new.description);
END;

CREATE TABLE IF NOT EXISTS versions (
  id INTEGER PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  version_number TEXT NOT NULL,
  changelog TEXT NOT NULL DEFAULT '',
  channel TEXT NOT NULL DEFAULT 'release',    -- release | beta | alpha
  loaders TEXT NOT NULL DEFAULT '',
  game_versions TEXT NOT NULL DEFAULT '',
  file_name TEXT NOT NULL,
  file_path TEXT NOT NULL,
  file_size INTEGER NOT NULL,
  sha1 TEXT NOT NULL,
  sha512 TEXT NOT NULL,
  downloads INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS versions_project ON versions(project_id);

CREATE TABLE IF NOT EXISTS gallery (
  id INTEGER PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  path TEXT NOT NULL,
  title TEXT NOT NULL DEFAULT '',
  featured INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS comments (
  id INTEGER PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  parent_id INTEGER REFERENCES comments(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS comments_project ON comments(project_id);

CREATE TABLE IF NOT EXISTS follows (
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, project_id)
);

CREATE TABLE IF NOT EXISTS reports (
  id INTEGER PRIMARY KEY,
  reporter_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  target_type TEXT NOT NULL,                  -- project | comment | user
  target_id INTEGER NOT NULL,
  reason TEXT NOT NULL,
  details TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'open',        -- open | closed
  created_at INTEGER NOT NULL
);

-- one row per (project, kind, day, visitor) so views/downloads are counted once per visitor per day
CREATE TABLE IF NOT EXISTS events (
  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  kind TEXT NOT NULL,                         -- view | download
  day TEXT NOT NULL,                          -- YYYY-MM-DD (UTC)
  visitor TEXT NOT NULL,                      -- salted hash of ip + user agent
  PRIMARY KEY (project_id, kind, day, visitor)
) WITHOUT ROWID;
CREATE INDEX IF NOT EXISTS events_day ON events(day);

CREATE TABLE IF NOT EXISTS revenue_months (
  month TEXT PRIMARY KEY,                     -- YYYY-MM
  gross REAL NOT NULL,
  share_percent REAL NOT NULL,
  pool REAL NOT NULL,
  total_points REAL NOT NULL,
  note TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS earnings (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  project_id INTEGER REFERENCES projects(id) ON DELETE SET NULL,
  month TEXT NOT NULL,
  views INTEGER NOT NULL,
  downloads INTEGER NOT NULL,
  points REAL NOT NULL,
  amount REAL NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS earnings_user ON earnings(user_id);

CREATE TABLE IF NOT EXISTS payouts (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount REAL NOT NULL,
  method TEXT NOT NULL,
  details TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',     -- pending | paid | rejected
  note TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL,
  processed_at INTEGER
);

CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  link TEXT NOT NULL DEFAULT '',
  read INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
  sid TEXT PRIMARY KEY,
  sess TEXT NOT NULL,
  expires INTEGER NOT NULL
);
`;
db.exec(SCHEMA);

const DEFAULT_SETTINGS = {
  creator_share_percent: '75',
  view_weight: '1',
  download_weight: '5',
  currency: 'USD',
  min_payout: '5',
  ad_head: '',
  ad_top: '',
  ad_sidebar: '',
  ad_project: '',
  ads_txt: '',
  announcement: '',
  registrations_open: '1',
};
const insertSetting = db.prepare('INSERT OR IGNORE INTO settings(key, value) VALUES (?, ?)');
for (const [k, v] of Object.entries(DEFAULT_SETTINGS)) insertSetting.run(k, v);

let settingsCache = null;

function settings() {
  if (!settingsCache) {
    settingsCache = {};
    for (const r of db.prepare('SELECT key, value FROM settings').all()) settingsCache[r.key] = r.value;
  }
  return settingsCache;
}

function setSetting(key, value) {
  db.prepare('INSERT INTO settings(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value').run(key, String(value));
  settingsCache = null;
}

function now() {
  return Math.floor(Date.now() / 1000);
}

function notify(userId, body, link = '') {
  db.prepare('INSERT INTO notifications(user_id, body, link, created_at) VALUES (?, ?, ?, ?)').run(userId, body, link, now());
}

module.exports = { db, settings, setSetting, now, notify };
