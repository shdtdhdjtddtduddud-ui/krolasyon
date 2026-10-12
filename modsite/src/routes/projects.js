'use strict';
const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const rateLimit = require('express-rate-limit');
const config = require('../config');
const { db, now, notify } = require('../db');
const util = require('../util');
const revenue = require('../revenue');
const { flash, csrf, requireUser, canEdit } = require('../middleware');

const router = express.Router();
const TMP = path.join(config.DATA_DIR, 'tmp');
fs.mkdirSync(TMP, { recursive: true });
fs.mkdirSync(config.FILES_DIR, { recursive: true });
fs.mkdirSync(config.MEDIA_DIR, { recursive: true });

const uploadFile = multer({ dest: TMP, limits: { fileSize: config.MAX_FILE_MB * 1048576, files: 1, fields: 40 } });
const uploadImage = multer({ dest: TMP, limits: { fileSize: config.MAX_IMAGE_MB * 1048576, files: 1, fields: 40 } });
const commentLimiter = rateLimit({ windowMs: 10 * 60 * 1000, limit: 20, standardHeaders: true, legacyHeaders: false });
const uploadLimiter = rateLimit({ windowMs: 60 * 60 * 1000, limit: 40, standardHeaders: true, legacyHeaders: false });

/** multer wrapper that turns size errors into a flash message instead of a crash */
function multipart(mw, field) {
  return (req, res, next) => mw.single(field)(req, res, err => {
    if (err) {
      flash(req, 'error', err.code === 'LIMIT_FILE_SIZE' ? res.locals.t('upload.too_big') : err.message);
      return res.redirect(req.originalUrl.replace(/\/(versions|gallery)$/, '/edit'));
    }
    next();
  });
}

function cleanup(req) {
  if (req.file && fs.existsSync(req.file.path)) fs.unlink(req.file.path, () => {});
}

function getProject(slug) {
  return db.prepare('SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.slug = ?').get(slug);
}

function visible(req, p) {
  return p && (p.status === 'approved' || p.status === 'unlisted' || canEdit(req.user, p));
}

function notFound(res) {
  return res.status(404).render('error', { code: 404, message: res.locals.t('error.not_found') });
}

/** stores an uploaded image in media/<sub>/ and returns its public path */
function saveImage(file, sub) {
  const ext = util.imageType(file.path);
  if (!ext) {
    fs.unlinkSync(file.path);
    return null;
  }
  const dir = path.join(config.MEDIA_DIR, sub);
  fs.mkdirSync(dir, { recursive: true });
  const name = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}.${ext}`;
  fs.renameSync(file.path, path.join(dir, name));
  return `/media/${sub}/${name}`;
}

function removeMedia(p) {
  if (!p || !p.startsWith('/media/')) return;
  const f = path.join(config.MEDIA_DIR, p.slice('/media/'.length));
  if (f.startsWith(config.MEDIA_DIR) && fs.existsSync(f)) fs.unlink(f, () => {});
}

const SORTS = {
  downloads: 'p.downloads DESC',
  follows: 'p.follows DESC',
  newest: 'p.published_at DESC',
  updated: 'p.updated_at DESC',
  relevance: null,
};

// ---------------- browse ----------------

function browse(q) {
  const where = ["p.status = 'approved'"];
  const args = [];
  let join = '';
  let order = SORTS[q.sort] || null;
  const text = String(q.q || '').trim();
  if (text) {
    const terms = text.replace(/["*^():]/g, ' ').split(/\s+/).filter(Boolean).map(w => `"${w}"*`).join(' ');
    if (terms) {
      join = 'JOIN projects_fts f ON f.rowid = p.id';
      where.push('projects_fts MATCH ?');
      args.push(terms);
      if (!order) order = 'bm25(projects_fts, 10.0, 4.0, 1.0)';
    }
  }
  for (const c of util.pickList(q.c, util.CATEGORIES)) {
    where.push("(',' || p.categories || ',') LIKE ?");
    args.push(`%,${c},%`);
  }
  for (const l of util.pickList(q.l, util.LOADERS)) {
    where.push("(',' || p.loaders || ',') LIKE ?");
    args.push(`%,${l},%`);
  }
  for (const v of util.pickList(q.v, util.GAME_VERSIONS)) {
    where.push("(',' || p.game_versions || ',') LIKE ?");
    args.push(`%,${v},%`);
  }
  if (!order) order = 'p.downloads DESC';
  const page = Math.max(1, parseInt(q.page, 10) || 1);
  const per = 20;
  const base = `FROM projects p ${join} JOIN users u ON u.id = p.owner_id WHERE ${where.join(' AND ')}`;
  const total = db.prepare(`SELECT COUNT(*) c ${base}`).get(...args).c;
  const rows = db.prepare(`SELECT p.*, u.username owner ${base} ORDER BY ${order} LIMIT ? OFFSET ?`).all(...args, per, (page - 1) * per);
  return { rows, total, page, pages: Math.max(1, Math.ceil(total / per)) };
}

router.get('/', (req, res) => {
  const popular = db.prepare("SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.status = 'approved' ORDER BY p.downloads DESC LIMIT 8").all();
  const fresh = db.prepare("SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.status = 'approved' ORDER BY p.published_at DESC LIMIT 8").all();
  const stats = db.prepare("SELECT COUNT(*) projects, COALESCE(SUM(downloads), 0) downloads FROM projects WHERE status = 'approved'").get();
  stats.users = db.prepare('SELECT COUNT(*) c FROM users').get().c;
  res.render('home', { popular, fresh, stats });
});

router.get('/mods', (req, res) => {
  const result = browse(req.query);
  res.render('browse', { ...result, q: req.query });
});

// ---------------- project page ----------------

router.get('/mod/:slug', (req, res) => {
  const p = getProject(req.params.slug);
  if (!visible(req, p)) return notFound(res);
  if (revenue.track(req, p, 'view')) db.prepare('UPDATE projects SET views = views + 1 WHERE id = ?').run(p.id);
  const tab = ['description', 'gallery', 'versions', 'changelog', 'comments'].includes(req.query.tab) ? req.query.tab : 'description';
  const versions = db.prepare('SELECT * FROM versions WHERE project_id = ? ORDER BY created_at DESC').all(p.id);
  const gallery = db.prepare('SELECT * FROM gallery WHERE project_id = ? ORDER BY featured DESC, created_at').all(p.id);
  const comments = db.prepare(`SELECT c.*, u.username, u.role FROM comments c JOIN users u ON u.id = c.user_id
    WHERE c.project_id = ? ORDER BY c.created_at`).all(p.id);
  const following = req.user ? !!db.prepare('SELECT 1 FROM follows WHERE user_id = ? AND project_id = ?').get(req.user.id, p.id) : false;
  res.render('project', {
    p, tab, versions, gallery, comments, following,
    descriptionHtml: util.markdown(p.description),
    editable: canEdit(req.user, p),
  });
});

router.get('/download/:id', (req, res) => {
  const v = db.prepare('SELECT * FROM versions WHERE id = ?').get(req.params.id);
  if (!v) return notFound(res);
  const p = db.prepare('SELECT * FROM projects WHERE id = ?').get(v.project_id);
  if (!visible(req, p)) return notFound(res);
  const file = path.join(config.FILES_DIR, v.file_path);
  if (!file.startsWith(config.FILES_DIR) || !fs.existsSync(file)) return notFound(res);
  if (revenue.track(req, p, 'download')) {
    db.prepare('UPDATE versions SET downloads = downloads + 1 WHERE id = ?').run(v.id);
    db.prepare('UPDATE projects SET downloads = downloads + 1 WHERE id = ?').run(p.id);
  }
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.download(file, v.file_name);
});

router.post('/mod/:slug/follow', requireUser, (req, res) => {
  const p = getProject(req.params.slug);
  if (!visible(req, p)) return notFound(res);
  const has = db.prepare('SELECT 1 FROM follows WHERE user_id = ? AND project_id = ?').get(req.user.id, p.id);
  if (has) db.prepare('DELETE FROM follows WHERE user_id = ? AND project_id = ?').run(req.user.id, p.id);
  else db.prepare('INSERT INTO follows(user_id, project_id, created_at) VALUES (?, ?, ?)').run(req.user.id, p.id, now());
  db.prepare('UPDATE projects SET follows = (SELECT COUNT(*) FROM follows WHERE project_id = ?) WHERE id = ?').run(p.id, p.id);
  res.redirect(`/mod/${p.slug}`);
});

// ---------------- comments & reports ----------------

router.post('/mod/:slug/comment', requireUser, commentLimiter, (req, res) => {
  const p = getProject(req.params.slug);
  if (!p || p.status !== 'approved') return notFound(res);
  const body = String(req.body.body || '').trim().slice(0, 2000);
  let parent = parseInt(req.body.parent_id, 10) || null;
  if (parent && !db.prepare('SELECT 1 FROM comments WHERE id = ? AND project_id = ?').get(parent, p.id)) parent = null;
  if (body.length >= 2) {
    const r = db.prepare('INSERT INTO comments(project_id, user_id, parent_id, body, created_at) VALUES (?, ?, ?, ?, ?)').run(p.id, req.user.id, parent, body, now());
    if (p.owner_id !== req.user.id) notify(p.owner_id, `${req.user.username}: ${body.slice(0, 80)}`, `/mod/${p.slug}?tab=comments#c${r.lastInsertRowid}`);
    if (parent) {
      const pc = db.prepare('SELECT user_id FROM comments WHERE id = ?').get(parent);
      if (pc && pc.user_id !== req.user.id && pc.user_id !== p.owner_id) notify(pc.user_id, `${req.user.username}: ${body.slice(0, 80)}`, `/mod/${p.slug}?tab=comments#c${r.lastInsertRowid}`);
    }
  }
  res.redirect(`/mod/${p.slug}?tab=comments`);
});

router.post('/comment/:id/delete', requireUser, (req, res) => {
  const c = db.prepare('SELECT c.*, p.owner_id, p.slug FROM comments c JOIN projects p ON p.id = c.project_id WHERE c.id = ?').get(req.params.id);
  if (!c) return notFound(res);
  const staff = req.user.role === 'admin' || req.user.role === 'moderator';
  if (c.user_id === req.user.id || c.owner_id === req.user.id || staff) db.prepare('UPDATE comments SET deleted = 1 WHERE id = ?').run(c.id);
  res.redirect(`/mod/${c.slug}?tab=comments`);
});

router.post('/report', requireUser, commentLimiter, (req, res) => {
  const type = ['project', 'comment', 'user'].includes(req.body.target_type) ? req.body.target_type : null;
  const id = parseInt(req.body.target_id, 10);
  const reason = ['spam', 'malware', 'copyright', 'inappropriate', 'other'].includes(req.body.reason) ? req.body.reason : 'other';
  if (type && id) {
    db.prepare('INSERT INTO reports(reporter_id, target_type, target_id, reason, details, created_at) VALUES (?, ?, ?, ?, ?, ?)')
      .run(req.user.id, type, id, reason, String(req.body.details || '').slice(0, 1000), now());
    flash(req, 'success', res.locals.t('report.sent'));
  }
  res.redirect(String(req.body.back || '/').startsWith('/') ? String(req.body.back) : '/');
});

// ---------------- create / edit ----------------

router.get('/new', requireUser, (req, res) => res.render('project-form', { p: null, form: {} }));

function readProjectForm(body) {
  return {
    name: String(body.name || '').trim().slice(0, 64),
    summary: String(body.summary || '').trim().slice(0, 256),
    description: String(body.description || '').slice(0, 65000),
    license: util.LICENSES.includes(body.license) ? body.license : 'ARR',
    source_url: util.validUrl(body.source_url),
    issues_url: util.validUrl(body.issues_url),
    discord_url: util.validUrl(body.discord_url),
    categories: util.csv(util.pickList(body.categories, util.CATEGORIES).slice(0, 4)),
    loaders: util.csv(util.pickList(body.loaders, util.LOADERS)),
    game_versions: util.csv(util.pickList(body.game_versions, util.GAME_VERSIONS)),
    client_side: util.SIDES.includes(body.client_side) ? body.client_side : 'required',
    server_side: util.SIDES.includes(body.server_side) ? body.server_side : 'required',
  };
}

router.post('/new', requireUser, uploadLimiter, multipart(uploadImage, 'icon'), csrf, (req, res) => {
  const t = res.locals.t;
  const f = readProjectForm(req.body);
  const slug = util.slugify(req.body.slug || f.name);
  const errors = [];
  if (f.name.length < 3) errors.push(t('project.err_name'));
  if (f.summary.length < 10) errors.push(t('project.err_summary'));
  if (slug.length < 3 || ['new', 'edit', 'admin', 'api'].includes(slug)) errors.push(t('project.err_slug'));
  else if (db.prepare('SELECT 1 FROM projects WHERE slug = ?').get(slug)) errors.push(t('project.err_slug_taken'));
  if (errors.length) {
    cleanup(req);
    return res.status(400).render('project-form', { p: null, form: { ...f, slug }, errors });
  }
  const icon = req.file ? saveImage(req.file, 'icons') : null;
  const ts = now();
  db.prepare(`INSERT INTO projects(slug, owner_id, name, summary, description, icon, license, source_url, issues_url, discord_url, categories,
      loaders, game_versions, client_side, server_side, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
    .run(slug, req.user.id, f.name, f.summary, f.description, icon, f.license, f.source_url, f.issues_url, f.discord_url, f.categories,
      f.loaders, f.game_versions, f.client_side, f.server_side, ts, ts);
  flash(req, 'success', t('project.created'));
  res.redirect(`/mod/${slug}/edit`);
});

router.get('/mod/:slug/edit', requireUser, (req, res) => {
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) return notFound(res);
  const versions = db.prepare('SELECT * FROM versions WHERE project_id = ? ORDER BY created_at DESC').all(p.id);
  const gallery = db.prepare('SELECT * FROM gallery WHERE project_id = ? ORDER BY featured DESC, created_at').all(p.id);
  res.render('project-edit', { p, versions, gallery, form: p });
});

router.post('/mod/:slug/edit', requireUser, uploadLimiter, multipart(uploadImage, 'icon'), csrf, (req, res) => {
  const t = res.locals.t;
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) {
    cleanup(req);
    return notFound(res);
  }
  const f = readProjectForm(req.body);
  if (f.name.length < 3 || f.summary.length < 10) {
    cleanup(req);
    flash(req, 'error', f.name.length < 3 ? t('project.err_name') : t('project.err_summary'));
    return res.redirect(`/mod/${p.slug}/edit`);
  }
  let icon = p.icon;
  if (req.file) {
    const saved = saveImage(req.file, 'icons');
    if (saved) {
      removeMedia(p.icon);
      icon = saved;
    } else flash(req, 'error', t('upload.bad_image'));
  }
  db.prepare(`UPDATE projects SET name = ?, summary = ?, description = ?, icon = ?, license = ?, source_url = ?, issues_url = ?, discord_url = ?,
      categories = ?, loaders = ?, game_versions = ?, client_side = ?, server_side = ?, updated_at = ? WHERE id = ?`)
    .run(f.name, f.summary, f.description, icon, f.license, f.source_url, f.issues_url, f.discord_url, f.categories, f.loaders,
      f.game_versions, f.client_side, f.server_side, now(), p.id);
  flash(req, 'success', t('common.saved'));
  res.redirect(`/mod/${p.slug}/edit`);
});

router.post('/mod/:slug/versions', requireUser, uploadLimiter, multipart(uploadFile, 'file'), csrf, (req, res) => {
  const t = res.locals.t;
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) {
    cleanup(req);
    return notFound(res);
  }
  const back = `/mod/${p.slug}/edit#versions`;
  if (!req.file) {
    flash(req, 'error', t('upload.no_file'));
    return res.redirect(back);
  }
  if (!util.isZip(req.file.path)) {
    cleanup(req);
    flash(req, 'error', t('upload.bad_file'));
    return res.redirect(back);
  }
  const number = String(req.body.version_number || '').trim().slice(0, 32);
  if (!number) {
    cleanup(req);
    flash(req, 'error', t('version.err_number'));
    return res.redirect(back);
  }
  const name = String(req.body.name || '').trim().slice(0, 64) || `${p.name} ${number}`;
  const loaders = util.csv(util.pickList(req.body.loaders, util.LOADERS));
  const gv = util.csv(util.pickList(req.body.game_versions, util.GAME_VERSIONS));
  const channel = util.CHANNELS.includes(req.body.channel) ? req.body.channel : 'release';
  const hashes = util.fileHashes(req.file.path);
  const fileName = util.safeFileName(req.file.originalname);
  const ts = now();
  const r = db.prepare(`INSERT INTO versions(project_id, name, version_number, changelog, channel, loaders, game_versions, file_name, file_path, file_size,
      sha1, sha512, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '', ?, ?, ?, ?)`)
    .run(p.id, name, number, String(req.body.changelog || '').slice(0, 20000), channel, loaders, gv, fileName, req.file.size, hashes.sha1, hashes.sha512, ts);
  const rel = path.join(String(p.id), String(r.lastInsertRowid), fileName);
  const dest = path.join(config.FILES_DIR, rel);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.renameSync(req.file.path, dest);
  db.prepare('UPDATE versions SET file_path = ? WHERE id = ?').run(rel, r.lastInsertRowid);
  // the project's supported loaders / versions follow its files
  const all = db.prepare('SELECT loaders, game_versions FROM versions WHERE project_id = ?').all(p.id);
  const L = new Set(), V = new Set();
  for (const v of all) {
    util.split(v.loaders).forEach(x => L.add(x));
    util.split(v.game_versions).forEach(x => V.add(x));
  }
  db.prepare('UPDATE projects SET loaders = ?, game_versions = ?, updated_at = ? WHERE id = ?')
    .run([...L].join(','), util.GAME_VERSIONS.filter(x => V.has(x)).join(','), ts, p.id);
  for (const f of db.prepare('SELECT user_id FROM follows WHERE project_id = ?').all(p.id)) notify(f.user_id, `${p.name} ${number}`, `/mod/${p.slug}?tab=versions`);
  flash(req, 'success', t('version.uploaded'));
  res.redirect(back);
});

router.post('/version/:id/delete', requireUser, (req, res) => {
  const v = db.prepare('SELECT v.*, p.slug, p.owner_id FROM versions v JOIN projects p ON p.id = v.project_id WHERE v.id = ?').get(req.params.id);
  if (!v || !canEdit(req.user, v)) return notFound(res);
  const f = path.join(config.FILES_DIR, v.file_path);
  if (f.startsWith(config.FILES_DIR) && fs.existsSync(f)) fs.rmSync(path.dirname(f), { recursive: true, force: true });
  db.prepare('DELETE FROM versions WHERE id = ?').run(v.id);
  res.redirect(`/mod/${v.slug}/edit#versions`);
});

router.post('/mod/:slug/gallery', requireUser, uploadLimiter, multipart(uploadImage, 'image'), csrf, (req, res) => {
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) {
    cleanup(req);
    return notFound(res);
  }
  if (req.file) {
    const saved = saveImage(req.file, `gallery/${p.id}`);
    if (saved) {
      const featured = req.body.featured ? 1 : 0;
      if (featured) db.prepare('UPDATE gallery SET featured = 0 WHERE project_id = ?').run(p.id);
      db.prepare('INSERT INTO gallery(project_id, path, title, featured, created_at) VALUES (?, ?, ?, ?, ?)')
        .run(p.id, saved, String(req.body.title || '').slice(0, 100), featured, now());
    } else flash(req, 'error', res.locals.t('upload.bad_image'));
  }
  res.redirect(`/mod/${p.slug}/edit#gallery`);
});

router.post('/gallery/:id/delete', requireUser, (req, res) => {
  const g = db.prepare('SELECT g.*, p.slug, p.owner_id FROM gallery g JOIN projects p ON p.id = g.project_id WHERE g.id = ?').get(req.params.id);
  if (!g || !canEdit(req.user, g)) return notFound(res);
  removeMedia(g.path);
  db.prepare('DELETE FROM gallery WHERE id = ?').run(g.id);
  res.redirect(`/mod/${g.slug}/edit#gallery`);
});

router.post('/mod/:slug/submit', requireUser, (req, res) => {
  const t = res.locals.t;
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) return notFound(res);
  const hasVersion = db.prepare('SELECT 1 FROM versions WHERE project_id = ?').get(p.id);
  if (!hasVersion) flash(req, 'error', t('project.need_version'));
  else if (p.description.trim().length < 100) flash(req, 'error', t('project.need_description'));
  else if (p.status === 'draft' || p.status === 'rejected') {
    db.prepare("UPDATE projects SET status = 'review', updated_at = ? WHERE id = ?").run(now(), p.id);
    for (const s of db.prepare("SELECT id FROM users WHERE role IN ('admin', 'moderator')").all()) notify(s.id, `${t('admin.new_review')}: ${p.name}`, '/admin/review');
    flash(req, 'success', t('project.submitted'));
  }
  res.redirect(`/mod/${p.slug}/edit`);
});

router.post('/mod/:slug/delete', requireUser, (req, res) => {
  const p = getProject(req.params.slug);
  if (!canEdit(req.user, p)) return notFound(res);
  if (req.body.confirm !== p.slug) {
    flash(req, 'error', res.locals.t('project.delete_confirm_wrong'));
    return res.redirect(`/mod/${p.slug}/edit`);
  }
  fs.rmSync(path.join(config.FILES_DIR, String(p.id)), { recursive: true, force: true });
  fs.rmSync(path.join(config.MEDIA_DIR, 'gallery', String(p.id)), { recursive: true, force: true });
  removeMedia(p.icon);
  db.prepare('DELETE FROM projects WHERE id = ?').run(p.id);
  flash(req, 'success', res.locals.t('project.deleted'));
  res.redirect('/dashboard');
});

// ---------------- users ----------------

router.get('/user/:name', (req, res) => {
  const u = db.prepare('SELECT id, username, bio, role, created_at FROM users WHERE username = ?').get(req.params.name);
  if (!u) return notFound(res);
  const own = req.user && (req.user.id === u.id || req.user.role !== 'user');
  const projects = db.prepare(`SELECT p.*, ? owner FROM projects p WHERE owner_id = ? ${own ? '' : "AND status = 'approved'"} ORDER BY downloads DESC`).all(u.username, u.id);
  const downloads = projects.reduce((t, p) => t + p.downloads, 0);
  res.render('user', { profile: u, projects, downloads });
});

module.exports = router;
