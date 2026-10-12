'use strict';
/*
 * Creator revenue sharing (Modrinth style):
 *  - every approved, monetized project collects "points" from unique daily page views and downloads
 *    (one view and one download per visitor per project per day; the owner's own visits never count)
 *  - at the end of a month the admin enters the ad revenue the site earned that month
 *  - creator pool = revenue * share% (default 75%), split between projects by their share of all points
 *  - the amounts land in each creator's balance; creators request payouts, the admin pays them manually
 */
const { db, settings, now, notify } = require('./db');
const util = require('./util');

const insertEvent = db.prepare('INSERT OR IGNORE INTO events(project_id, kind, day, visitor) VALUES (?, ?, ?, ?)');

/** records a view/download; returns true when it was a new unique event */
function track(req, project, kind) {
  if (!project || project.status !== 'approved') return false;
  if (req.user && req.user.id === project.owner_id) return false;
  const ua = String(req.headers['user-agent'] || '');
  if (/bot|crawl|spider|slurp|curl|wget|python|headless/i.test(ua)) return false;
  const r = insertEvent.run(project.id, kind, util.today(), util.visitorHash(req));
  return r.changes > 0;
}

function monthRange(month) {
  return [`${month}-01`, `${month}-31`];
}

/** per project unique views/downloads + points for a month (approved & monetized only) */
function monthStats(month) {
  const s = settings();
  const vw = parseFloat(s.view_weight) || 1;
  const dw = parseFloat(s.download_weight) || 5;
  const [a, b] = monthRange(month);
  const rows = db.prepare(`
    SELECT p.id project_id, p.owner_id, p.name, p.slug,
      SUM(CASE WHEN e.kind = 'view' THEN 1 ELSE 0 END) views,
      SUM(CASE WHEN e.kind = 'download' THEN 1 ELSE 0 END) downloads
    FROM events e JOIN projects p ON p.id = e.project_id
    WHERE e.day BETWEEN ? AND ? AND p.monetized = 1
    GROUP BY p.id`).all(a, b);
  for (const r of rows) r.points = r.views * vw + r.downloads * dw;
  const total = rows.reduce((t, r) => t + r.points, 0);
  return { rows, total };
}

/** closes a month: distributes the creator pool. Throws if the month was already distributed. */
function distribute(month, gross, note = '') {
  if (!/^\d{4}-\d{2}$/.test(month)) throw new Error('bad month');
  if (db.prepare('SELECT 1 FROM revenue_months WHERE month = ?').get(month)) throw new Error('already distributed');
  const share = parseFloat(settings().creator_share_percent) || 75;
  const pool = Math.max(0, gross) * share / 100;
  const { rows, total } = monthStats(month);
  const ins = db.prepare('INSERT INTO earnings(user_id, project_id, month, views, downloads, points, amount, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
  const currency = settings().currency;
  db.transaction(() => {
    db.prepare('INSERT INTO revenue_months(month, gross, share_percent, pool, total_points, note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)')
      .run(month, gross, share, pool, total, note, now());
    for (const r of rows) {
      const amount = total > 0 ? Math.floor(pool * r.points / total * 10000) / 10000 : 0;
      ins.run(r.owner_id, r.project_id, month, r.views, r.downloads, r.points, amount, now());
      if (amount > 0) notify(r.owner_id, `+${util.money(amount, currency)} (${month}) - ${r.name}`, '/dashboard/earnings');
    }
  })();
  return { pool, total, projects: rows.length };
}

function balance(userId) {
  const earned = db.prepare('SELECT COALESCE(SUM(amount), 0) s FROM earnings WHERE user_id = ?').get(userId).s;
  const paidOrPending = db.prepare("SELECT COALESCE(SUM(amount), 0) s FROM payouts WHERE user_id = ? AND status != 'rejected'").get(userId).s;
  return { earned, withdrawn: paidOrPending, available: Math.max(0, Math.round((earned - paidOrPending) * 100) / 100) };
}

/** daily views/downloads of one creator for the last n days (for charts) */
function dailyForOwner(ownerId, days = 30) {
  const from = new Date(Date.now() - (days - 1) * 86400000).toISOString().slice(0, 10);
  const rows = db.prepare(`
    SELECT e.day, SUM(e.kind = 'view') views, SUM(e.kind = 'download') downloads
    FROM events e JOIN projects p ON p.id = e.project_id
    WHERE p.owner_id = ? AND e.day >= ? GROUP BY e.day`).all(ownerId, from);
  const map = Object.fromEntries(rows.map(r => [r.day, r]));
  const out = [];
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000).toISOString().slice(0, 10);
    out.push({ day: d, views: map[d] ? map[d].views : 0, downloads: map[d] ? map[d].downloads : 0 });
  }
  return out;
}

/** estimated earnings this month if the site earned `rpm` per 1000 points (shown as a rough guide) */
function currentMonthForOwner(ownerId) {
  const month = new Date().toISOString().slice(0, 7);
  const { rows, total } = monthStats(month);
  const mine = rows.filter(r => r.owner_id === ownerId);
  const points = mine.reduce((t, r) => t + r.points, 0);
  return { month, points, total, share: total > 0 ? points / total : 0, projects: mine };
}

module.exports = { track, monthStats, distribute, balance, dailyForOwner, currentMonthForOwner };
