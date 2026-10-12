'use strict';
const express = require('express');
const path = require('path');
const fs = require('fs');
const session = require('express-session');
const helmet = require('helmet');
const config = require('./config');
require('./db');
const SqliteStore = require('./session-store');
const { locals, csrf } = require('./middleware');

const app = express();
if (config.TRUST_PROXY) app.set('trust proxy', 1);
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '..', 'views'));
app.disable('x-powered-by');

// ad networks (e.g. AdSense) inject scripts/iframes from many domains, so scripts/frames are allowed over https.
// user content is always sanitised, so it can never add scripts itself.
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", 'https:'],
      styleSrc: ["'self'", "'unsafe-inline'", 'https:'],
      imgSrc: ["'self'", 'data:', 'https:', 'http:'],
      frameSrc: ['https:'],
      connectSrc: ["'self'", 'https:'],
      fontSrc: ["'self'", 'https:', 'data:'],
      objectSrc: ["'none'"],
      upgradeInsecureRequests: config.SECURE_COOKIES ? [] : null,
    },
  },
  crossOriginEmbedderPolicy: false,
  crossOriginResourcePolicy: { policy: 'cross-origin' },
}));

app.use('/static', express.static(path.join(__dirname, '..', 'public'), { maxAge: '7d' }));
app.use('/media', express.static(config.MEDIA_DIR, { maxAge: '30d', dotfiles: 'deny', setHeaders: res => res.setHeader('X-Content-Type-Options', 'nosniff') }));

app.use(express.urlencoded({ extended: false, limit: '200kb', parameterLimit: 200 }));
app.use(session({
  name: 'sid',
  store: new SqliteStore(),
  secret: config.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  rolling: true,
  cookie: { httpOnly: true, sameSite: 'lax', secure: config.SECURE_COOKIES, maxAge: 7 * 86400000 },
}));
app.use(locals);
// multipart forms check their token inside the route, after the upload is parsed
app.use((req, res, next) => (req.is('multipart/form-data') ? next() : csrf(req, res, next)));

app.use(require('./routes/api'));
app.use(require('./routes/auth'));
app.use(require('./routes/projects'));
app.use(require('./routes/dashboard'));
app.use(require('./routes/admin'));

app.get('/terms', (req, res) => res.render('page', { page: 'terms' }));
app.get('/privacy', (req, res) => res.render('page', { page: 'privacy' }));
app.get('/rules', (req, res) => res.render('page', { page: 'rules' }));
app.get('/monetization', (req, res) => res.render('page', { page: 'monetization' }));

app.use((req, res) => res.status(404).render('error', { code: 404, message: res.locals.t('error.not_found') }));
app.use((err, req, res, next) => {
  console.error(err);
  if (res.headersSent) return next(err);
  res.status(500).render('error', { code: 500, message: res.locals.t ? res.locals.t('error.server') : 'Server error' });
});

// remove abandoned uploads
const TMP = path.join(config.DATA_DIR, 'tmp');
setInterval(() => {
  if (!fs.existsSync(TMP)) return;
  for (const f of fs.readdirSync(TMP)) {
    const p = path.join(TMP, f);
    try {
      if (Date.now() - fs.statSync(p).mtimeMs > 3600 * 1000) fs.unlinkSync(p);
    } catch { /* ignore */ }
  }
}, 30 * 60 * 1000).unref();

if (require.main === module) {
  app.listen(config.PORT, () => console.log(`${config.SITE_NAME} listening on :${config.PORT} (data: ${config.DATA_DIR})`));
}

module.exports = app;
