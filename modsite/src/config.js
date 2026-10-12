'use strict';
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

// optional .env file (KEY=value lines) next to package.json
const envFile = path.join(__dirname, '..', '.env');
if (fs.existsSync(envFile)) {
  for (const line of fs.readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}

const DATA_DIR = path.resolve(process.env.DATA_DIR || path.join(__dirname, '..', 'data'));
fs.mkdirSync(DATA_DIR, { recursive: true });

function secret() {
  if (process.env.SESSION_SECRET) return process.env.SESSION_SECRET;
  // generate once and keep it in the data dir so sessions survive restarts
  const f = path.join(DATA_DIR, '.session-secret');
  if (!fs.existsSync(f)) fs.writeFileSync(f, crypto.randomBytes(48).toString('hex'), { mode: 0o600 });
  return fs.readFileSync(f, 'utf8').trim();
}

module.exports = {
  PORT: parseInt(process.env.PORT || '3000', 10),
  SITE_NAME: process.env.SITE_NAME || 'Krolasyon Mods',
  SITE_URL: (process.env.SITE_URL || 'http://localhost:3000').replace(/\/$/, ''),
  DATA_DIR,
  FILES_DIR: path.join(DATA_DIR, 'files'),
  MEDIA_DIR: path.join(DATA_DIR, 'media'),
  DB_FILE: path.join(DATA_DIR, 'site.db'),
  SESSION_SECRET: secret(),
  MAX_FILE_MB: parseInt(process.env.MAX_FILE_MB || '100', 10),
  MAX_IMAGE_MB: parseInt(process.env.MAX_IMAGE_MB || '5', 10),
  TRUST_PROXY: process.env.TRUST_PROXY !== 'false',
  SECURE_COOKIES: (process.env.SITE_URL || '').startsWith('https://'),
  CONTACT_EMAIL: process.env.CONTACT_EMAIL || 'admin@example.com',
};
