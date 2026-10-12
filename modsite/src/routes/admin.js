'use strict';
const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { db, settings, setSetting, now, notify } = require('../db');
const revenue = require('../revenue');
const util = require('../util');
const { flash, requireStaff, requireAdmin } = require('../middleware');

const router = express.Router();
router.use('/admin', requireStaff);

router.get('/admin', (req, res) => {
  const c = q => db.prepare(q).get().c;
  const stats = {
    review: c("SELECT COUNT(*) c FROM projects WHERE status = 'review'"),
    reports: c("SELECT COUNT(*) c FROM reports WHERE status = 'open'"),
    payouts: c("SELECT COUNT(*) c FROM payouts WHERE status = 'pending'"),
    users: c('SELECT COUNT(*) c FROM users'),
    projects: c("SELECT COUNT(*) c FROM projects WHERE status = 'approved'"),
    downloads: db.prepare('SELECT COALESCE(SUM(downloads), 0) c FROM projects').get().c,
  };
  res.render('admin/index', { stats });
});

// ---------------- moderation ----------------

router.get('/admin/review', (req, res) => {
  const items = db.prepare(`SELECT p.*, u.username owner, (SELECT COUNT(*) FROM versions v WHERE v.project_id = p.id) nversions
    FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.status = 'review' ORDER BY p.updated_at`).all();
  res.render('admin/review', { items });
});

router.post('/admin/review/:id', (req, res) => {
  const t = res.locals.t;
  const p = db.prepare('SELECT * FROM projects WHERE id = ?').get(req.params.id);
  if (!p) return res.redirect('/admin/review');
  const note = String(req.body.note || '').slice(0, 2000);
  if (req.body.action === 'approve') {
    db.prepare("UPDATE projects SET status = 'approved', moderation_note = ?, published_at = COALESCE(published_at, ?), updated_at = ? WHERE id = ?").run(note, now(), now(), p.id);
    notify(p.owner_id, `✅ ${t('mod.approved_msg', p.name)}`, `/mod/${p.slug}`);
  } else if (req.body.action === 'reject') {
    db.prepare("UPDATE projects SET status = 'rejected', moderation_note = ?, updated_at = ? WHERE id = ?").run(note, now(), p.id);
    notify(p.owner_id, `❌ ${t('mod.rejected_msg', p.name)}${note ? ': ' + note.slice(0, 120) : ''}`, `/mod/${p.slug}/edit`);
  }
  flash(req, 'success', t('common.saved'));
  res.redirect('/admin/review');
});

router.get('/admin/projects', (req, res) => {
  const items = db.prepare('SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id ORDER BY p.created_at DESC LIMIT 500').all();
  res.render('admin/projects', { items });
});

router.post('/admin/projects/:id', (req, res) => {
  const p = db.prepare('SELECT * FROM projects WHERE id = ?').get(req.params.id);
  if (p) {
    if (['approved', 'unlisted', 'rejected', 'draft', 'review'].includes(req.body.status)) {
      db.prepare('UPDATE projects SET status = ?, published_at = CASE WHEN ? = \'approved\' THEN COALESCE(published_at, ?) ELSE published_at END WHERE id = ?')
        .run(req.body.status, req.body.status, now(), p.id);
    }
    if (req.body.monetized !== undefined) db.prepare('UPDATE projects SET monetized = ? WHERE id = ?').run(req.body.monetized === '1' ? 1 : 0, p.id);
  }
  res.redirect('/admin/projects');
});

router.get('/admin/reports', (req, res) => {
  const items = db.prepare(`SELECT r.*, u.username reporter FROM reports r LEFT JOIN users u ON u.id = r.reporter_id
    ORDER BY r.status = 'open' DESC, r.created_at DESC LIMIT 300`).all();
  for (const r of items) {
    if (r.target_type === 'project') r.target = db.prepare('SELECT name label, slug FROM projects WHERE id = ?').get(r.target_id);
    else if (r.target_type === 'comment') r.target = db.prepare('SELECT c.body label, p.slug FROM comments c JOIN projects p ON p.id = c.project_id WHERE c.id = ?').get(r.target_id);
    else r.target = db.prepare('SELECT username label FROM users WHERE id = ?').get(r.target_id);
  }
  res.render('admin/reports', { items });
});

router.post('/admin/reports/:id', (req, res) => {
  const r = db.prepare('SELECT * FROM reports WHERE id = ?').get(req.params.id);
  if (r) {
    if (req.body.action === 'remove') {
      if (r.target_type === 'comment') db.prepare('UPDATE comments SET deleted = 1 WHERE id = ?').run(r.target_id);
      if (r.target_type === 'project') db.prepare("UPDATE projects SET status = 'unlisted' WHERE id = ?").run(r.target_id);
    }
    db.prepare("UPDATE reports SET status = 'closed' WHERE id = ?").run(r.id);
  }
  res.redirect('/admin/reports');
});

// ---------------- users (admin only) ----------------

router.get('/admin/users', requireAdmin, (req, res) => {
  const q = String(req.query.q || '').trim();
  const items = q
    ? db.prepare('SELECT * FROM users WHERE username LIKE ? OR email LIKE ? ORDER BY created_at DESC LIMIT 200').all(`%${q}%`, `%${q}%`)
    : db.prepare('SELECT * FROM users ORDER BY created_at DESC LIMIT 200').all();
  res.render('admin/users', { items, q });
});

router.post('/admin/users/:id', requireAdmin, async (req, res) => {
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!u) return res.redirect('/admin/users');
  if (u.id === req.user.id && req.body.action !== 'reset') {
    flash(req, 'error', res.locals.t('admin.not_self'));
    return res.redirect('/admin/users');
  }
  if (req.body.action === 'role' && ['user', 'moderator', 'admin'].includes(req.body.role)) db.prepare('UPDATE users SET role = ? WHERE id = ?').run(req.body.role, u.id);
  if (req.body.action === 'ban') {
    db.prepare('UPDATE users SET banned = 1 WHERE id = ?').run(u.id);
    db.prepare("UPDATE projects SET status = 'unlisted' WHERE owner_id = ? AND status = 'approved'").run(u.id);
  }
  if (req.body.action === 'unban') db.prepare('UPDATE users SET banned = 0 WHERE id = ?').run(u.id);
  if (req.body.action === 'reset') {
    const pw = crypto.randomBytes(6).toString('base64url');
    db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(await bcrypt.hash(pw, 11), u.id);
    flash(req, 'success', `${u.username}: ${pw}`);
  }
  res.redirect('/admin/users');
});

// ---------------- revenue sharing (admin only) ----------------

router.get('/admin/revenue', requireAdmin, (req, res) => {
  const month = /^\d{4}-\d{2}$/.test(req.query.month || '') ? req.query.month : new Date(Date.now() - 15 * 86400000).toISOString().slice(0, 7);
  const preview = revenue.monthStats(month);
  preview.rows.sort((a, b) => b.points - a.points);
  const history = db.prepare('SELECT * FROM revenue_months ORDER BY month DESC').all();
  const done = history.find(h => h.month === month);
  res.render('admin/revenue', { month, preview, history, done });
});

router.post('/admin/revenue', requireAdmin, (req, res) => {
  const t = res.locals.t;
  const month = String(req.body.month || '');
  const gross = parseFloat(String(req.body.gross || '').replace(',', '.'));
  if (!(gross >= 0)) flash(req, 'error', t('rev.bad_amount'));
  else {
    try {
      const r = revenue.distribute(month, gross, String(req.body.note || '').slice(0, 300));
      flash(req, 'success', t('rev.done', util.money(r.pool, settings().currency), r.projects));
    } catch (e) {
      flash(req, 'error', e.message === 'already distributed' ? t('rev.already') : e.message);
    }
  }
  res.redirect(`/admin/revenue?month=${encodeURIComponent(month)}`);
});

router.get('/admin/payouts', requireAdmin, (req, res) => {
  const items = db.prepare(`SELECT po.*, u.username, u.email FROM payouts po JOIN users u ON u.id = po.user_id
    ORDER BY po.status = 'pending' DESC, po.created_at DESC LIMIT 300`).all();
  res.render('admin/payouts', { items });
});

router.post('/admin/payouts/:id', requireAdmin, (req, res) => {
  const t = res.locals.t;
  const po = db.prepare('SELECT * FROM payouts WHERE id = ?').get(req.params.id);
  if (po && po.status === 'pending' && ['paid', 'rejected'].includes(req.body.action)) {
    const note = String(req.body.note || '').slice(0, 300);
    db.prepare('UPDATE payouts SET status = ?, note = ?, processed_at = ? WHERE id = ?').run(req.body.action, note, now(), po.id);
    notify(po.user_id, req.body.action === 'paid' ? t('earn.paid_msg', util.money(po.amount, settings().currency)) : `${t('earn.rejected_msg')} ${note}`, '/dashboard/earnings');
  }
  res.redirect('/admin/payouts');
});

// ---------------- site settings (admin only) ----------------

const SETTING_KEYS = ['creator_share_percent', 'view_weight', 'download_weight', 'currency', 'min_payout', 'ad_head', 'ad_top', 'ad_sidebar',
  'ad_project', 'ads_txt', 'announcement', 'registrations_open'];

router.get('/admin/settings', requireAdmin, (req, res) => res.render('admin/settings'));

router.post('/admin/settings', requireAdmin, (req, res) => {
  for (const k of SETTING_KEYS) {
    if (k === 'registrations_open') setSetting(k, req.body[k] ? '1' : '0');
    else if (req.body[k] !== undefined) setSetting(k, String(req.body[k]).slice(0, 20000));
  }
  flash(req, 'success', res.locals.t('common.saved'));
  res.redirect('/admin/settings');
});

module.exports = router;
