'use strict';
const express = require('express');
const rateLimit = require('express-rate-limit');
const { db, settings, now } = require('../db');
const revenue = require('../revenue');
const { flash, requireUser } = require('../middleware');

const router = express.Router();
const payoutLimiter = rateLimit({ windowMs: 60 * 60 * 1000, limit: 5, standardHeaders: true, legacyHeaders: false });

router.get('/dashboard', requireUser, (req, res) => {
  const projects = db.prepare('SELECT * FROM projects WHERE owner_id = ? ORDER BY updated_at DESC').all(req.user.id);
  const totals = projects.reduce((t, p) => ({ downloads: t.downloads + p.downloads, views: t.views + p.views, follows: t.follows + p.follows }),
    { downloads: 0, views: 0, follows: 0 });
  const daily = revenue.dailyForOwner(req.user.id, 30);
  res.render('dashboard/index', { projects, totals, daily, bal: revenue.balance(req.user.id) });
});

router.get('/dashboard/earnings', requireUser, (req, res) => {
  const earnings = db.prepare(`SELECT e.*, p.name, p.slug FROM earnings e LEFT JOIN projects p ON p.id = e.project_id
    WHERE e.user_id = ? ORDER BY e.month DESC, e.amount DESC`).all(req.user.id);
  const payouts = db.prepare('SELECT * FROM payouts WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.render('dashboard/earnings', { earnings, payouts, bal: revenue.balance(req.user.id), current: revenue.currentMonthForOwner(req.user.id) });
});

router.post('/dashboard/payout', requireUser, payoutLimiter, (req, res) => {
  const t = res.locals.t;
  const s = settings();
  const bal = revenue.balance(req.user.id);
  const amount = Math.round((parseFloat(req.body.amount) || 0) * 100) / 100;
  const min = parseFloat(s.min_payout) || 5;
  if (!req.user.payout_method || !req.user.payout_details) flash(req, 'error', t('earn.need_method'));
  else if (amount < min) flash(req, 'error', t('earn.min', min, s.currency));
  else if (amount > bal.available) flash(req, 'error', t('earn.too_much'));
  else if (db.prepare("SELECT 1 FROM payouts WHERE user_id = ? AND status = 'pending'").get(req.user.id)) flash(req, 'error', t('earn.pending_exists'));
  else {
    db.prepare('INSERT INTO payouts(user_id, amount, method, details, created_at) VALUES (?, ?, ?, ?, ?)')
      .run(req.user.id, amount, req.user.payout_method, req.user.payout_details, now());
    flash(req, 'success', t('earn.requested'));
  }
  res.redirect('/dashboard/earnings');
});

router.get('/notifications', requireUser, (req, res) => {
  const items = db.prepare('SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 100').all(req.user.id);
  db.prepare('UPDATE notifications SET read = 1 WHERE user_id = ?').run(req.user.id);
  res.render('notifications', { items });
});

module.exports = router;
