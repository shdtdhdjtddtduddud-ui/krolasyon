'use strict';
// End-to-end flow: register -> create mod -> upload version -> review -> download -> comment -> revenue -> payout
const { test, before, after } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');

const DATA = fs.mkdtempSync(path.join(os.tmpdir(), 'modsite-test-'));
process.env.DATA_DIR = DATA;
process.env.SITE_URL = 'http://localhost';
const app = require('../src/server');
let server, base;

before(() => new Promise(r => { server = app.listen(0, () => { base = `http://127.0.0.1:${server.address().port}`; r(); }); }));
after(() => { server.close(); fs.rmSync(DATA, { recursive: true, force: true }); });

class Client {
  constructor(ua = 'Mozilla/5.0 test') { this.cookies = {}; this.ua = ua; }
  header() { return Object.entries(this.cookies).map(([k, v]) => `${k}=${v}`).join('; '); }
  async req(method, url, body) {
    const headers = { cookie: this.header(), 'user-agent': this.ua };
    let payload = body;
    if (body && !(body instanceof FormData)) {
      headers['content-type'] = 'application/x-www-form-urlencoded';
      payload = new URLSearchParams(body).toString();
    }
    const res = await fetch(base + url, { method, headers, body: payload, redirect: 'manual' });
    for (const c of res.headers.getSetCookie()) {
      const [kv] = c.split(';');
      const i = kv.indexOf('=');
      this.cookies[kv.slice(0, i)] = kv.slice(i + 1);
    }
    res.text_ = await res.text();
    return res;
  }
  get(url) { return this.req('GET', url); }
  async csrf(url = '/') {
    const r = await this.get(url);
    const m = /name="_csrf" value="([0-9a-f]+)"/.exec(r.text_);
    assert.ok(m, 'csrf token on ' + url);
    return m[1];
  }
  async post(url, body, from) {
    const token = await this.csrf(from || url);
    if (body instanceof FormData) { body.append('_csrf', token); return this.req('POST', url, body); }
    return this.req('POST', url, { ...body, _csrf: token });
  }
}

const admin = new Client();
const creator = new Client();
const visitor = new Client('Mozilla/5.0 visitor');

function zipBytes() {
  // minimal valid empty zip with a local header signature at the start
  return Buffer.concat([Buffer.from([0x50, 0x4b, 0x03, 0x04]), Buffer.alloc(60, 1)]);
}
function png() {
  return Buffer.from('89504e470d0a1a0a0000000d4948445200000001000000010806000000' + '1f15c4890000000d49444154789c6360000002000154a24f5d0000000049454e44ae426082', 'hex');
}

test('pages render', async () => {
  for (const u of ['/', '/mods', '/login', '/register', '/terms', '/privacy', '/rules', '/monetization', '/robots.txt', '/sitemap.xml', '/api/v1/search']) {
    const r = await visitor.get(u);
    assert.strictEqual(r.status, 200, u);
  }
  assert.strictEqual((await visitor.get('/nope')).status, 404);
});

test('csrf is enforced', async () => {
  const r = await visitor.req('POST', '/login', { login: 'x', password: 'y' });
  assert.strictEqual(r.status, 403);
});

test('register: first user becomes admin', async () => {
  let r = await admin.post('/register', { username: 'boss', email: 'boss@example.com', password: 'secret123', accept: '1' });
  assert.strictEqual(r.status, 302);
  r = await admin.get('/admin');
  assert.strictEqual(r.status, 200);
  r = await creator.post('/register', { username: 'maker', email: 'maker@example.com', password: 'secret123', accept: '1' });
  assert.strictEqual(r.status, 302);
  r = await creator.get('/admin');
  assert.strictEqual(r.status, 403);
  r = await creator.post('/register', { username: 'maker', email: 'other@example.com', password: 'secret123', accept: '1' }, '/');
  assert.notStrictEqual(r.status, 302);
});

test('create, upload, submit, approve, download, comment', async () => {
  let fd = new FormData();
  fd.append('name', 'Ben 10 Omnitrix');
  fd.append('summary', 'Transform into nine aliens with the Omnitrix.');
  fd.append('description', '# Omnitrix\n\n' + 'Lots of aliens and abilities. '.repeat(10));
  fd.append('categories', 'adventure');
  fd.append('categories', 'magic');
  fd.append('license', 'ARR');
  fd.append('icon', new Blob([png()], { type: 'image/png' }), 'icon.png');
  let r = await creator.post('/new', fd, '/new');
  assert.strictEqual(r.status, 302);
  assert.strictEqual(r.headers.get('location'), '/mod/ben-10-omnitrix/edit');

  // non-zip upload is rejected
  fd = new FormData();
  fd.append('version_number', '1.0.0');
  fd.append('file', new Blob([Buffer.from('not a jar')]), 'evil.exe');
  await creator.post('/mod/ben-10-omnitrix/versions', fd, '/mod/ben-10-omnitrix/edit');
  r = await creator.get('/mod/ben-10-omnitrix/edit');
  assert.ok(!r.text_.includes('evil.exe'));

  fd = new FormData();
  fd.append('version_number', '1.0.0');
  fd.append('loaders', 'forge');
  fd.append('game_versions', '1.20.1');
  fd.append('changelog', 'Initial release');
  fd.append('file', new Blob([zipBytes()]), 'ben10_omnitrix-1.20.1-forge-1.0.0.jar');
  r = await creator.post('/mod/ben-10-omnitrix/versions', fd, '/mod/ben-10-omnitrix/edit');
  assert.strictEqual(r.status, 302);

  // not public before approval
  assert.strictEqual((await visitor.get('/mod/ben-10-omnitrix')).status, 404);
  r = await creator.post('/mod/ben-10-omnitrix/submit', {}, '/mod/ben-10-omnitrix/edit');
  assert.strictEqual(r.status, 302);
  r = await admin.get('/admin/review');
  assert.ok(r.text_.includes('Ben 10 Omnitrix'));
  const id = /action="\/admin\/review\/(\d+)"/.exec(r.text_)[1];
  r = await admin.post(`/admin/review/${id}`, { action: 'approve', note: 'nice' }, '/admin/review');
  assert.strictEqual(r.status, 302);

  r = await visitor.get('/mod/ben-10-omnitrix');
  assert.strictEqual(r.status, 200);
  assert.ok(r.text_.includes('Omnitrix'));
  const vid = /href="\/download\/(\d+)"/.exec(r.text_)[1];
  r = await visitor.get(`/download/${vid}`);
  assert.strictEqual(r.status, 200);
  assert.match(r.headers.get('content-disposition'), /ben10_omnitrix-1.20.1-forge-1.0.0.jar/);
  await visitor.get(`/download/${vid}`); // same visitor same day: counted once

  r = await visitor.get('/api/v1/project/ben-10-omnitrix');
  const api = JSON.parse(r.text_);
  assert.strictEqual(api.downloads, 1);
  assert.deepStrictEqual(api.game_versions, ['1.20.1']);

  r = await visitor.get('/mods?q=omni');
  assert.ok(r.text_.includes('Ben 10 Omnitrix'));
  r = await visitor.get('/mods?c=mobs');
  assert.ok(!r.text_.includes('Ben 10 Omnitrix'));

  // comments (with xss attempt)
  r = await admin.post('/mod/ben-10-omnitrix/comment', { body: 'Great mod <script>alert(1)</script>' }, '/mod/ben-10-omnitrix?tab=comments');
  assert.strictEqual(r.status, 302);
  r = await visitor.get('/mod/ben-10-omnitrix?tab=comments');
  assert.ok(r.text_.includes('Great mod &lt;script&gt;'));
  assert.ok(!r.text_.includes('<script>alert(1)'));
  r = await creator.get('/notifications');
  assert.ok(r.text_.includes('boss'));
});

test('revenue sharing and payouts', async () => {
  const month = new Date().toISOString().slice(0, 7);
  let r = await admin.get(`/admin/revenue?month=${month}`);
  assert.ok(r.text_.includes('Ben 10 Omnitrix'));
  r = await admin.post('/admin/revenue', { month, gross: '100', note: 'test' }, `/admin/revenue?month=${month}`);
  assert.strictEqual(r.status, 302);
  r = await admin.post('/admin/revenue', { month, gross: '100' }, `/admin/revenue?month=${month}`);
  r = await creator.get('/dashboard/earnings');
  assert.ok(r.text_.includes('75.00 USD'), 'creator gets the 75% pool');

  r = await creator.post('/dashboard/payout', { amount: '10' }, '/dashboard/earnings');
  r = await admin.get('/admin/payouts');
  assert.ok(!r.text_.includes('maker@example.com'), 'no payout without payout details');
  await creator.post('/settings/payout', { payout_method: 'papara', payout_details: '1234567890' }, '/settings');
  await creator.post('/dashboard/payout', { amount: '80' }, '/dashboard/earnings');
  r = await admin.get('/admin/payouts');
  assert.ok(!r.text_.includes('80.00 USD'), 'cannot exceed balance');
  await creator.post('/dashboard/payout', { amount: '50' }, '/dashboard/earnings');
  r = await admin.get('/admin/payouts');
  assert.ok(r.text_.includes('50.00 USD'));
  const pid = /action="\/admin\/payouts\/(\d+)"/.exec(r.text_)[1];
  await admin.post(`/admin/payouts/${pid}`, { action: 'paid', note: 'tx1' }, '/admin/payouts');
  r = await creator.get('/dashboard/earnings');
  assert.ok(r.text_.includes('25.00 USD'), 'remaining balance');
});

test('settings: ads appear on pages', async () => {
  const fd = { creator_share_percent: '75', view_weight: '1', download_weight: '5', currency: 'USD', min_payout: '5', ad_head: '', ad_top: '<div id="AD-TOP">ad</div>',
    ad_sidebar: '', ad_project: '', ads_txt: 'google.com, pub-1, DIRECT', announcement: 'Hello', registrations_open: '1' };
  await admin.post('/admin/settings', fd, '/admin/settings');
  const r = await visitor.get('/');
  assert.ok(r.text_.includes('AD-TOP'));
  assert.strictEqual((await visitor.get('/ads.txt')).text_, 'google.com, pub-1, DIRECT');
});
