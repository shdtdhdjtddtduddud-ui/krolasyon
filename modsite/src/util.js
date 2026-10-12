'use strict';
const crypto = require('crypto');
const fs = require('fs');
const { marked } = require('marked');
const sanitizeHtml = require('sanitize-html');
const config = require('./config');

const CATEGORIES = ['adventure', 'combat', 'decoration', 'equipment', 'food', 'game-mechanics', 'library', 'magic', 'mobs',
  'optimization', 'storage', 'technology', 'transportation', 'utility', 'worldgen', 'cursed'];
const LOADERS = ['forge', 'neoforge', 'fabric', 'quilt'];
const GAME_VERSIONS = ['1.21.4', '1.21.3', '1.21.1', '1.21', '1.20.6', '1.20.4', '1.20.2', '1.20.1', '1.20', '1.19.4', '1.19.2', '1.18.2',
  '1.17.1', '1.16.5', '1.12.2', '1.7.10'];
const LICENSES = ['ARR', 'MIT', 'Apache-2.0', 'GPL-3.0', 'LGPL-3.0', 'MPL-2.0', 'CC-BY-4.0', 'CC-BY-NC-4.0', 'CC0-1.0'];
const CHANNELS = ['release', 'beta', 'alpha'];
const SIDES = ['required', 'optional', 'unsupported'];

const TR_MAP = { ç: 'c', ğ: 'g', ı: 'i', İ: 'i', ö: 'o', ş: 's', ü: 'u', Ç: 'c', Ğ: 'g', Ö: 'o', Ş: 's', Ü: 'u' };

function slugify(s) {
  return String(s || '')
    .replace(/[çğıİöşüÇĞÖŞÜ]/g, c => TR_MAP[c])
    .toLowerCase()
    .normalize('NFKD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48);
}

const SANITIZE = {
  allowedTags: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'br', 'hr', 'blockquote', 'ul', 'ol', 'li', 'strong', 'em', 'del', 'code', 'pre',
    'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'details', 'summary', 'span', 'sup', 'sub', 'kbd'],
  allowedAttributes: { a: ['href', 'title'], img: ['src', 'alt', 'title', 'width', 'height'], th: ['align'], td: ['align'], code: ['class'] },
  allowedSchemes: ['http', 'https', 'mailto'],
  allowedSchemesByTag: { img: ['https', 'http'] },
  transformTags: {
    a: (tag, attribs) => ({ tagName: 'a', attribs: { ...attribs, rel: 'nofollow ugc noopener', target: '_blank' } }),
  },
};

function markdown(src) {
  const html = marked.parse(String(src || ''), { gfm: true, breaks: false });
  return sanitizeHtml(html, SANITIZE);
}

/** plain text, line breaks kept, for comments */
function plain(src) {
  return sanitizeHtml(String(src || ''), { allowedTags: [], allowedAttributes: {} });
}

function csv(list) {
  return (Array.isArray(list) ? list : list ? [list] : []).map(s => String(s).trim()).filter(Boolean).join(',');
}

function pickList(input, allowed) {
  const arr = Array.isArray(input) ? input : input ? [input] : [];
  return arr.filter(v => allowed.includes(v));
}

function split(s) {
  return String(s || '').split(',').filter(Boolean);
}

function fileHashes(path) {
  const data = fs.readFileSync(path);
  return {
    sha1: crypto.createHash('sha1').update(data).digest('hex'),
    sha512: crypto.createHash('sha512').update(data).digest('hex'),
  };
}

function readHead(path, n = 16) {
  const fd = fs.openSync(path, 'r');
  try {
    const b = Buffer.alloc(n);
    fs.readSync(fd, b, 0, n, 0);
    return b;
  } finally {
    fs.closeSync(fd);
  }
}

/** jar / zip archives start with PK\x03\x04 */
function isZip(path) {
  const b = readHead(path, 4);
  return b[0] === 0x50 && b[1] === 0x4b && b[2] === 0x03 && b[3] === 0x04;
}

/** returns the image extension or null */
function imageType(path) {
  const b = readHead(path, 12);
  if (b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47) return 'png';
  if (b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'jpg';
  if (b.slice(0, 4).toString() === 'GIF8') return 'gif';
  if (b.slice(0, 4).toString() === 'RIFF' && b.slice(8, 12).toString() === 'WEBP') return 'webp';
  return null;
}

function safeFileName(name) {
  const base = String(name || 'file').split(/[\\/]/).pop();
  return base.replace(/[^A-Za-z0-9._+\-]/g, '_').replace(/^\.+/, '').slice(0, 120) || 'file';
}

function visitorHash(req) {
  const ua = String(req.headers['user-agent'] || '').slice(0, 200);
  return crypto.createHmac('sha256', config.SESSION_SECRET).update(`${req.ip}|${ua}`).digest('hex').slice(0, 32);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function formatNumber(n) {
  n = Number(n) || 0;
  if (n >= 1e6) return (n / 1e6).toFixed(n >= 1e7 ? 0 : 1).replace(/\.0$/, '') + 'M';
  if (n >= 1e3) return (n / 1e3).toFixed(n >= 1e4 ? 0 : 1).replace(/\.0$/, '') + 'K';
  return String(n);
}

function formatBytes(n) {
  if (n >= 1 << 20) return (n / (1 << 20)).toFixed(1) + ' MB';
  if (n >= 1 << 10) return (n / (1 << 10)).toFixed(0) + ' KB';
  return n + ' B';
}

function money(n, currency) {
  return `${(Math.round((Number(n) || 0) * 100) / 100).toFixed(2)} ${currency || ''}`.trim();
}

function validUrl(u) {
  if (!u) return '';
  try {
    const x = new URL(u);
    return x.protocol === 'https:' || x.protocol === 'http:' ? x.toString() : '';
  } catch {
    return '';
  }
}

module.exports = {
  CATEGORIES, LOADERS, GAME_VERSIONS, LICENSES, CHANNELS, SIDES,
  slugify, markdown, plain, csv, pickList, split, fileHashes, isZip, imageType, safeFileName, visitorHash, today,
  formatNumber, formatBytes, money, validUrl,
};
