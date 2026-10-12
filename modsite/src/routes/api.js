'use strict';
const express = require('express');
const config = require('../config');
const { db, settings } = require('../db');
const util = require('../util');

const router = express.Router();

function pub(p) {
  return {
    slug: p.slug, name: p.name, summary: p.summary, author: p.owner, icon: p.icon ? config.SITE_URL + p.icon : null,
    categories: util.split(p.categories), loaders: util.split(p.loaders), game_versions: util.split(p.game_versions),
    license: p.license, downloads: p.downloads, follows: p.follows, client_side: p.client_side, server_side: p.server_side,
    published: p.published_at, updated: p.updated_at, url: `${config.SITE_URL}/mod/${p.slug}`,
  };
}

router.get('/api/v1/search', (req, res) => {
  const q = String(req.query.q || '').trim().replace(/["*^():]/g, ' ').split(/\s+/).filter(Boolean).map(w => `"${w}"*`).join(' ');
  const limit = Math.min(100, parseInt(req.query.limit, 10) || 20);
  const rows = q
    ? db.prepare(`SELECT p.*, u.username owner FROM projects p JOIN projects_fts f ON f.rowid = p.id JOIN users u ON u.id = p.owner_id
        WHERE p.status = 'approved' AND projects_fts MATCH ? ORDER BY bm25(projects_fts, 10.0, 4.0, 1.0) LIMIT ?`).all(q, limit)
    : db.prepare(`SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.status = 'approved'
        ORDER BY p.downloads DESC LIMIT ?`).all(limit);
  res.json({ hits: rows.map(pub) });
});

router.get('/api/v1/project/:slug', (req, res) => {
  const p = db.prepare("SELECT p.*, u.username owner FROM projects p JOIN users u ON u.id = p.owner_id WHERE p.slug = ? AND p.status = 'approved'").get(req.params.slug);
  if (!p) return res.status(404).json({ error: 'not_found' });
  const versions = db.prepare('SELECT * FROM versions WHERE project_id = ? ORDER BY created_at DESC').all(p.id).map(v => ({
    id: v.id, name: v.name, version_number: v.version_number, channel: v.channel, loaders: util.split(v.loaders),
    game_versions: util.split(v.game_versions), downloads: v.downloads, published: v.created_at,
    file: { name: v.file_name, size: v.file_size, sha1: v.sha1, sha512: v.sha512, url: `${config.SITE_URL}/download/${v.id}` },
  }));
  res.json({ ...pub(p), description: p.description, versions });
});

// ads.txt, robots.txt and sitemap for search engines / ad networks
router.get('/ads.txt', (req, res) => res.type('text/plain').send(settings().ads_txt || ''));
router.get('/robots.txt', (req, res) => res.type('text/plain').send(`User-agent: *\nDisallow: /admin\nDisallow: /dashboard\nDisallow: /download/\nSitemap: ${config.SITE_URL}/sitemap.xml\n`));
router.get('/sitemap.xml', (req, res) => {
  const rows = db.prepare("SELECT slug, updated_at FROM projects WHERE status = 'approved' ORDER BY updated_at DESC LIMIT 5000").all();
  const urls = [`<url><loc>${config.SITE_URL}/</loc></url>`, `<url><loc>${config.SITE_URL}/mods</loc></url>`]
    .concat(rows.map(r => `<url><loc>${config.SITE_URL}/mod/${r.slug}</loc><lastmod>${new Date(r.updated_at * 1000).toISOString().slice(0, 10)}</lastmod></url>`));
  res.type('application/xml').send(`<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${urls.join('')}</urlset>`);
});

module.exports = router;
