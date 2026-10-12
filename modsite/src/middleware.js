'use strict';
const crypto = require('crypto');
const { db, settings } = require('./db');
const { t, pickLang } = require('./i18n');
const util = require('./util');
const config = require('./config');

/** loads the logged-in user, language, csrf token and template helpers */
function locals(req, res, next) {
  const lang = pickLang(req);
  req.lang = lang;
  if (!req.session.csrf) req.session.csrf = crypto.randomBytes(24).toString('hex');
  let user = null;
  if (req.session.uid) {
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.session.uid) || null;
    if (!user || user.banned) {
      user = null;
      delete req.session.uid;
    }
  }
  req.user = user;
  res.locals.user = user;
  res.locals.lang = lang;
  res.locals.t = (k, ...a) => t(lang, k, ...a);
  res.locals.csrf = req.session.csrf;
  res.locals.u = util;
  res.locals.site = { name: config.SITE_NAME, url: config.SITE_URL, contact: config.CONTACT_EMAIL };
  res.locals.settings = settings();
  res.locals.path = req.path;
  res.locals.query = req.query;
  res.locals.flash = req.session.flash || [];
  req.session.flash = [];
  res.locals.unread = user ? db.prepare('SELECT COUNT(*) c FROM notifications WHERE user_id = ? AND read = 0').get(user.id).c : 0;
  res.locals.isStaff = !!user && (user.role === 'admin' || user.role === 'moderator');
  res.locals.isAdmin = !!user && user.role === 'admin';
  next();
}

function flash(req, type, msg) {
  (req.session.flash = req.session.flash || []).push({ type, msg });
}

/** checks the csrf token on every state-changing request (call after body parsing) */
function csrf(req, res, next) {
  if (req.method === 'GET' || req.method === 'HEAD') return next();
  const token = (req.body && req.body._csrf) || req.headers['x-csrf-token'];
  if (!token || !req.session.csrf || token.length !== req.session.csrf.length ||
      !crypto.timingSafeEqual(Buffer.from(token), Buffer.from(req.session.csrf))) {
    return res.status(403).render('error', { code: 403, message: res.locals.t('error.csrf') });
  }
  next();
}

function requireUser(req, res, next) {
  if (!req.user) {
    flash(req, 'info', res.locals.t('auth.login_required'));
    return res.redirect('/login?next=' + encodeURIComponent(req.originalUrl));
  }
  next();
}

function requireStaff(req, res, next) {
  if (!req.user || (req.user.role !== 'admin' && req.user.role !== 'moderator')) return res.status(403).render('error', { code: 403, message: res.locals.t('error.forbidden') });
  next();
}

function requireAdmin(req, res, next) {
  if (!req.user || req.user.role !== 'admin') return res.status(403).render('error', { code: 403, message: res.locals.t('error.forbidden') });
  next();
}

/** owner of the project or staff */
function canEdit(user, project) {
  return !!user && !!project && (project.owner_id === user.id || user.role === 'admin' || user.role === 'moderator');
}

module.exports = { locals, flash, csrf, requireUser, requireStaff, requireAdmin, canEdit };
