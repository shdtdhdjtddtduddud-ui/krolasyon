'use strict';
const express = require('express');
const bcrypt = require('bcryptjs');
const rateLimit = require('express-rate-limit');
const { db, settings, now } = require('../db');
const { flash, requireUser } = require('../middleware');

const router = express.Router();
const limiter = rateLimit({ windowMs: 15 * 60 * 1000, limit: 15, standardHeaders: true, legacyHeaders: false });

function safeNext(n) {
  return typeof n === 'string' && n.startsWith('/') && !n.startsWith('//') ? n : '/';
}

router.get('/register', (req, res) => {
  if (req.user) return res.redirect('/');
  res.render('auth/register', { form: {} });
});

router.post('/register', limiter, async (req, res) => {
  const t = res.locals.t;
  if (settings().registrations_open !== '1') {
    flash(req, 'error', t('auth.closed'));
    return res.redirect('/register');
  }
  const username = String(req.body.username || '').trim();
  const email = String(req.body.email || '').trim().toLowerCase();
  const password = String(req.body.password || '');
  const errors = [];
  if (!/^[A-Za-z0-9_\-]{3,24}$/.test(username)) errors.push(t('auth.bad_username'));
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.push(t('auth.bad_email'));
  if (password.length < 8) errors.push(t('auth.bad_password'));
  if (!req.body.accept) errors.push(t('auth.accept_terms'));
  if (db.prepare('SELECT 1 FROM users WHERE username = ? OR email = ?').get(username, email)) errors.push(t('auth.taken'));
  if (errors.length) return res.status(400).render('auth/register', { form: { username, email }, errors });
  const hash = await bcrypt.hash(password, 11);
  const first = db.prepare('SELECT COUNT(*) c FROM users').get().c === 0;
  const r = db.prepare('INSERT INTO users(username, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)')
    .run(username, email, hash, first ? 'admin' : 'user', now());
  req.session.regenerate(() => {
    req.session.uid = r.lastInsertRowid;
    flash(req, 'success', first ? t('auth.welcome_admin') : t('auth.welcome'));
    res.redirect('/');
  });
});

router.get('/login', (req, res) => {
  if (req.user) return res.redirect('/');
  res.render('auth/login', { next: safeNext(req.query.next) });
});

router.post('/login', limiter, async (req, res) => {
  const t = res.locals.t;
  const id = String(req.body.login || '').trim();
  const user = db.prepare('SELECT * FROM users WHERE username = ? OR email = ?').get(id, id.toLowerCase());
  const ok = user && await bcrypt.compare(String(req.body.password || ''), user.password_hash);
  if (!ok) return res.status(401).render('auth/login', { next: safeNext(req.body.next), errors: [t('auth.invalid')], login: id });
  if (user.banned) return res.status(403).render('auth/login', { next: '/', errors: [t('auth.banned')], login: id });
  const next = safeNext(req.body.next);
  req.session.regenerate(() => {
    req.session.uid = user.id;
    if (req.body.remember) req.session.cookie.maxAge = 30 * 86400000;
    res.redirect(next);
  });
});

router.post('/logout', (req, res) => {
  req.session.destroy(() => res.redirect('/'));
});

router.get('/settings', requireUser, (req, res) => res.render('auth/settings'));

router.post('/settings/profile', requireUser, (req, res) => {
  const bio = String(req.body.bio || '').slice(0, 500);
  db.prepare('UPDATE users SET bio = ? WHERE id = ?').run(bio, req.user.id);
  flash(req, 'success', res.locals.t('common.saved'));
  res.redirect('/settings');
});

router.post('/settings/payout', requireUser, (req, res) => {
  const method = ['paypal', 'papara', 'iban'].includes(req.body.payout_method) ? req.body.payout_method : '';
  const details = String(req.body.payout_details || '').slice(0, 300);
  db.prepare('UPDATE users SET payout_method = ?, payout_details = ? WHERE id = ?').run(method, details, req.user.id);
  flash(req, 'success', res.locals.t('common.saved'));
  res.redirect('/settings');
});

router.post('/settings/password', requireUser, limiter, async (req, res) => {
  const t = res.locals.t;
  const ok = await bcrypt.compare(String(req.body.current || ''), req.user.password_hash);
  const pw = String(req.body.password || '');
  if (!ok) flash(req, 'error', t('auth.invalid'));
  else if (pw.length < 8) flash(req, 'error', t('auth.bad_password'));
  else {
    db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(await bcrypt.hash(pw, 11), req.user.id);
    db.prepare('DELETE FROM sessions WHERE sess LIKE ? AND sid != ?').run(`%"uid":${req.user.id}%`, req.sessionID);
    flash(req, 'success', t('common.saved'));
  }
  res.redirect('/settings');
});

router.get('/lang/:l', (req, res) => {
  const l = req.params.l === 'en' ? 'en' : 'tr';
  res.cookie('lang', l, { maxAge: 365 * 86400000, sameSite: 'lax' });
  res.redirect(safeNext(req.query.back));
});

module.exports = router;
