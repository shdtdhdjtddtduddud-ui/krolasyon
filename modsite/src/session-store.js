'use strict';
const session = require('express-session');
const { db } = require('./db');

/** express-session store backed by the sessions table */
class SqliteStore extends session.Store {
  constructor() {
    super();
    this.getStmt = db.prepare('SELECT sess FROM sessions WHERE sid = ? AND expires > ?');
    this.setStmt = db.prepare('INSERT INTO sessions(sid, sess, expires) VALUES (?, ?, ?) ON CONFLICT(sid) DO UPDATE SET sess = excluded.sess, expires = excluded.expires');
    this.delStmt = db.prepare('DELETE FROM sessions WHERE sid = ?');
    this.touchStmt = db.prepare('UPDATE sessions SET expires = ? WHERE sid = ?');
    setInterval(() => db.prepare('DELETE FROM sessions WHERE expires <= ?').run(Date.now()), 3600 * 1000).unref();
  }

  expiry(sess) {
    const maxAge = sess && sess.cookie && sess.cookie.maxAge ? sess.cookie.maxAge : 86400000;
    return Date.now() + maxAge;
  }

  get(sid, cb) {
    try {
      const r = this.getStmt.get(sid, Date.now());
      cb(null, r ? JSON.parse(r.sess) : null);
    } catch (e) {
      cb(e);
    }
  }

  set(sid, sess, cb) {
    try {
      this.setStmt.run(sid, JSON.stringify(sess), this.expiry(sess));
      cb && cb(null);
    } catch (e) {
      cb && cb(e);
    }
  }

  destroy(sid, cb) {
    try {
      this.delStmt.run(sid);
      cb && cb(null);
    } catch (e) {
      cb && cb(e);
    }
  }

  touch(sid, sess, cb) {
    try {
      this.touchStmt.run(this.expiry(sess), sid);
      cb && cb(null);
    } catch (e) {
      cb && cb(e);
    }
  }
}

module.exports = SqliteStore;
