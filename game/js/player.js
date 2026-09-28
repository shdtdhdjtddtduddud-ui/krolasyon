// Oyuncu karakteri: hareket, kombo saldırılar, kaçınma, yetenekler, istatistikler
import * as THREE from 'three';
import { Actor } from './actor.js';
import { buildHumanoid, makeWeapon } from './models.js';
import { CLASSES, SKILLS, RARITY } from './data.js';
import { damage, enemiesInArc, Projectile } from './combat.js';
import { castSkill } from './skills.js';
import { heightAt, WATER_Y, biomeWeights } from './terrain.js';
import { Trail } from './fx.js';
import { clamp, dampAngle, wrapAngle, rand } from './util.js';

export class Player extends Actor {
  constructor(game, save) {
    const C = CLASSES[save.cls];
    const model = buildHumanoid({ ...C.look });
    super(game, model);
    this.isPlayer = true; this.cls = save.cls; this.C = C; this.radius = 0.45;
    this.level = save.level || 1; this.xp = save.xp || 0; this.gold = save.gold ?? 20;
    this.inv = save.inv || []; this.eq = save.eq || {}; this.mats = save.mats || {}; this.potions = save.potions || { hp: 3, mp: 2 };
    this.upgrade = save.upgrade || 0;
    this.skills = SKILLS[this.cls]; this.cds = [0, 0, 0, 0];
    this.stamina = 100; this.buff = 0; this.combo = 0; this.queued = false; this.action = null; this.rollT = -1; this.deadT = 0;
    this.potionCd = 0; this.team = 'player';
    this.pos.set(save.x ?? 4, 0, save.z ?? 8); this.pos.y = heightAt(this.pos.x, this.pos.z);
    this.yaw = save.yaw ?? Math.PI;
    this.stats = {}; this.recalc(); this.hp = save.hp ?? this.stats.maxHp; this.mp = save.mp ?? this.stats.maxMp;
    this.attachWeapon();
    this.trail = new Trail(game.scene, C.color, 14);
    this.st.holdPose = C.weapon === 'sword' ? 'sword' : C.weapon === 'staff' ? 'staff' : 'bow';
    this.lastStep = 0; this.target = null;
  }
  attachWeapon() {
    const p = this.model.p; if (this.weapon) this.weapon.parent.remove(this.weapon); if (this.shield) this.shield.parent.remove(this.shield);
    const w = this.eq.weapon; const R = w ? RARITY[w.rarity] : RARITY[0];
    const glow = w ? w.rarity * 0.6 + this.upgrade * 0.15 : 0;
    const col = w && w.rarity > 0 ? R.hex : this.cls === 'mage' ? 0x80a0ff : 0xc8d0d8;
    this.weapon = makeWeapon(this.C.weapon, col, glow);
    if (this.C.weapon === 'bow') { this.weapon.rotation.set(Math.PI / 2, 0, 0); p.lSocket.add(this.weapon); }
    else { this.weapon.rotation.set(Math.PI / 2, 0, 0); if (this.C.weapon === 'staff') this.weapon.position.set(0, 0, -0.5); p.rSocket.add(this.weapon); }
    if (this.cls === 'warrior') { this.shield = makeWeapon('shield'); this.shield.rotation.set(0, Math.PI / 2, 0); this.shield.position.set(0.08, 0.05, 0); p.lElbow.add(this.shield); this.shield.position.set(0.1, -0.18, 0); }
    if (this.trail) this.trail.color.setHex(w && w.rarity > 0 ? R.hex : this.C.color);
  }
  recalc() {
    const C = this.C, L = this.level - 1, s = { hp: 0, mp: 0, atk: 0, armor: 0, crit: 0, critDmg: 0, speed: 0 };
    for (const k in this.eq) { const it = this.eq[k]; if (it) for (const st in it.stats) s[st] += it.stats[st]; }
    const S = this.stats;
    S.maxHp = Math.round(C.hp + C.hpL * L + s.hp); S.maxMp = Math.round(C.mp + C.mpL * L + s.mp);
    S.atk = Math.round((C.atk + C.atkL * L + s.atk) * (1 + this.upgrade * 0.08)); S.armor = Math.round(C.armor + L * 1.2 + s.armor);
    S.crit = Math.min(0.75, C.crit + s.crit); S.critDmg = 1.6 + s.critDmg; S.speed = C.speed * (1 + s.speed);
    if (this.hp > S.maxHp) this.hp = S.maxHp; if (this.mp > S.maxMp) this.mp = S.maxMp;
  }
  xpNext() { return Math.round(90 * Math.pow(this.level, 1.55)); }
  gainXp(n) {
    if (this.level >= 20) return; this.xp += n; const G = this.game;
    while (this.xp >= this.xpNext() && this.level < 20) {
      this.xp -= this.xpNext(); this.level++; this.recalc(); this.hp = this.stats.maxHp; this.mp = this.stats.maxMp;
      G.audio.play('levelup'); G.ui.banner(`Seviye ${this.level}!`, 'Güçlerin artıyor'); G.fx.burst(this.pos.clone().setY(this.pos.y + 0.2), 60, { speed: 3, up: 3, life: 1.5, size: 0.25, color: [1, 0.85, 0.3], spread: 0.6, drag: 0.5 });
      G.fx.ring(this.pos, 4, [1, 0.8, 0.3], 0.8); G.fx.light(this.center(), [1, 0.8, 0.3], 30, 0.8);
      const s = this.skills.find((k) => k.lvl === this.level); if (s) G.ui.toast(`Yeni yetenek: ${s.icon} ${s.name}`, 'gold');
    }
  }
  // ----- nişan alma -----
  aimDir() {
    const G = this.game; const d = new THREE.Vector3();
    if (G.input.touch || !G.input.locked) d.set(Math.sin(this.yaw), 0, Math.cos(this.yaw)); else { G.camera.getWorldDirection(d); d.y = 0; d.normalize(); }
    return d;
  }
  findTarget(range, cone = 40, dir = this.aimDir()) {
    const G = this.game; if (G.lockTarget && G.lockTarget.alive && G.lockTarget.pos.distanceTo(this.pos) < range + 6) return G.lockTarget;
    let best = null, bs = 1e9; const cc = Math.cos(cone * Math.PI / 180);
    for (const e of G.enemies) {
      if (!e.alive) continue; const dx = e.pos.x - this.pos.x, dz = e.pos.z - this.pos.z, d = Math.hypot(dx, dz); if (d > range + e.radius) continue;
      const c = (dx * dir.x + dz * dir.z) / (d || 1); const touchOK = G.input.touch && d < 12;
      if (c < cc && !touchOK) continue; const score = d * (2 - c);
      if (score < bs) { bs = score; best = e; }
    }
    return best;
  }
  faceTo(target, snap = false) { const y = Math.atan2(target.x - this.pos.x, target.z - this.pos.z); this.yaw = snap ? y : dampAngle(this.yaw, y, 25, 0.016); this.faceYaw = y; }
  // ----- aksiyon sistemi -----
  startAction(a) { this.action = { t: 0, fired: new Set(), speed: 1, move: 0.15, ...a }; this.st.act = { name: a.anim, t: 0 }; }
  canAct() { return this.alive && this.rollT < 0 && !this.swimming && !(this.status.stun > 0) && !(this.status.freeze > 0); }
  attack() {
    if (!this.canAct()) return;
    if (this.action) { if (this.action.combo && this.action.t / this.action.dur > 0.3) this.queued = true; return; }
    const G = this.game, cls = this.cls;
    const tgt = this.findTarget(cls === 'warrior' ? 6 : 26);
    if (tgt) this.faceTo(tgt.pos, true); else if (!G.input.touch && G.input.locked) { const d = this.aimDir(); this.yaw = Math.atan2(d.x, d.z); }
    if (cls === 'warrior') {
      const i = this.combo % 3; this.combo++;
      const def = [{ anim: 'slash1', dur: 0.46, hit: 0.27, mult: 1, arc: 130 }, { anim: 'slash2', dur: 0.46, hit: 0.27, mult: 1.1, arc: 130 }, { anim: 'overhead', dur: 0.66, hit: 0.4, mult: 1.8, arc: 80, heavy: true }][i];
      this.startAction({ ...def, combo: true, trail: [def.hit - 0.12, def.hit + 0.06], move: 0.1, lunge: i === 2 ? 5 : 3.5, events: [[def.hit - 0.1, () => G.audio.play(i === 2 ? 'heavySwing' : 'swing', this.pos, { pitch: rand(0.9, 1.1) })], [def.hit, () => this.meleeHit(3.0, def.arc, def.mult, def.heavy)]] });
    } else if (cls === 'mage') {
      this.startAction({ anim: 'staffBolt', dur: 0.42, combo: true, move: 0.5, events: [[0.16, () => this.fireBolt(tgt)]] });
    } else {
      this.startAction({ anim: 'bow', dur: 0.5, combo: true, move: 0.45, draw: true, events: [[0.27, () => this.fireArrow(tgt)]] });
    }
  }
  meleeHit(range, arc, mult, heavy) {
    const G = this.game; const dir = new THREE.Vector3(Math.sin(this.yaw), 0, Math.cos(this.yaw));
    const hits = enemiesInArc(G, this.pos, dir, range, arc);
    for (const e of hits) damage(G, this, e, this.stats.atk * mult, { melee: true, kb: heavy ? 9 : 4.5, heavy, dir: e.pos.clone().sub(this.pos).setY(0).normalize(), lift: heavy ? 4 : 0 });
    if (heavy) { const p = this.pos.clone().addScaledVector(dir, 2); G.fx.dust(p, 10); G.fx.ring(p, 2.2, [1, 0.9, 0.7], 0.3); G.fx.addShake(0.15); }
    if (!hits.length) this.combo = this.combo % 3 === 0 ? 0 : this.combo;
  }
  handPos(side = 'r') { const v = new THREE.Vector3(); this.model.p[side + 'Hand'].getWorldPosition(v); return v; }
  weaponTip() { const v = new THREE.Vector3(); this.weapon.userData.tip.getWorldPosition(v); return v; }
  shotDir(tgt, from) {
    if (tgt) { const c = tgt.center(); return c.sub(from).normalize(); }
    const G = this.game; if (!G.input.touch && G.input.locked) { const d = new THREE.Vector3(); G.camera.getWorldDirection(d); d.y = Math.max(-0.3, d.y + 0.06); return d.normalize(); }
    return new THREE.Vector3(Math.sin(this.yaw), 0.02, Math.cos(this.yaw));
  }
  fireBolt(tgt) {
    const G = this.game; const from = this.weaponTip(); const dir = this.shotDir(tgt, from);
    G.projectiles.push(new Projectile(G, { type: 'bolt', pos: from, dir, speed: 30, team: 'player', src: this, color: 0x9a80ff, pcolor: [0.6, 0.5, 1], radius: 0.4, life: 1.6, homing: tgt, homingStr: 4,
      onHit: (pr, e) => damage(G, this, e, this.stats.atk * 1.0, { kb: 2 }) }));
    G.audio.play('magic', from, { pitch: rand(0.9, 1.2) }); G.fx.burst(from, 8, { speed: 2, life: 0.3, size: 0.2, color: [0.6, 0.5, 1] });
  }
  fireArrow(tgt, o = {}) {
    const G = this.game; const from = this.handPos('l').add(new THREE.Vector3(0, 0.1, 0)); const dir = o.dir || this.shotDir(tgt, from);
    G.projectiles.push(new Projectile(G, { type: 'arrow', pos: from, dir, speed: o.speed || 48, team: 'player', src: this, radius: 0.35, life: 1.4, grav: 2, pierce: o.pierce || 0, color: o.color, pcolor: o.pcolor,
      onHit: o.onHit || ((pr, e) => damage(G, this, e, this.stats.atk * (o.mult || 1), { kb: 2.5 })), onExplode: o.onExplode }));
    if (!o.silent) G.audio.play('bow', from, { pitch: rand(0.95, 1.1) });
  }
  dodge(dirx, dirz) {
    if (!this.canAct() && !(this.action && this.action.cancel !== false)) return;
    if (this.stamina < 25 || this.rollT >= 0 || !this.alive || this.swimming) return;
    const G = this.game; this.stamina -= 25; this.action = null; this.st.act = null; this.queued = false; this.combo = 0;
    let dx = dirx, dz = dirz; if (Math.hypot(dx, dz) < 0.1) { dx = -Math.sin(this.yaw); dz = -Math.cos(this.yaw); }
    const l = Math.hypot(dx, dz); dx /= l; dz /= l;
    if (this.C.dodge === 'blink') {
      const from = this.center(); let n = 0; const p = this.pos.clone();
      for (let i = 0; i < 20; i++) { p.x += dx * 0.45; p.z += dz * 0.45; n++; }
      G.fx.burst(from, 30, { speed: 3, life: 0.5, size: 0.25, color: [0.6, 0.5, 1], spread: 0.4 });
      this.pos.x = p.x; this.pos.z = p.z; this.pos.y = Math.max(this.pos.y, heightAt(p.x, p.z)); this.invuln = 0.3;
      G.fx.burst(this.center(), 30, { speed: 3, life: 0.5, size: 0.25, color: [0.6, 0.5, 1], spread: 0.4 }); G.audio.play('blink', this.pos);
      this.yaw = Math.atan2(dx, dz); return;
    }
    this.rollT = 0; this.rollDir = [dx, dz]; this.yaw = Math.atan2(dx, dz); this.invuln = 0.42; G.audio.play('roll', this.pos);
  }
  jump() { if (this.onGround && this.canAct() && !this.action) { this.vy = 9; this.onGround = false; this.game.audio.play('jump', this.pos); } }
  onLand(v) { this.game.audio.play('land', this.pos); this.game.fx.dust(this.pos.clone().setY(this.pos.y + 0.1), 6); }
  useSkill(i) {
    const G = this.game, s = this.skills[i]; if (!s || !this.canAct() || this.action) return;
    if (this.level < s.lvl) { G.ui.toast(`${s.name} seviye ${s.lvl}'de açılır`); G.audio.play('deny'); return; }
    if (this.cds[i] > 0) { G.audio.play('deny'); return; }
    if (this.mp < s.mp) { G.ui.toast('Yetersiz mana'); G.audio.play('deny'); return; }
    this.mp -= s.mp; this.cds[i] = s.cd; castSkill(this, G, s.id);
  }
  drink(kind) {
    const G = this.game; if (this.potionCd > 0 || !this.alive) return;
    if ((this.potions[kind] || 0) <= 0) { G.ui.toast(kind === 'hp' ? 'Can iksirin kalmadı' : 'Mana iksirin kalmadı'); G.audio.play('deny'); return; }
    this.potions[kind]--; this.potionCd = 1.2;
    if (kind === 'hp') { this.healOver = { amt: this.stats.maxHp * 0.5, t: 1.5 }; G.fx.burst(this.center(), 25, { speed: 2, up: 1.5, life: 1, size: 0.2, color: [1, 0.3, 0.35], spread: 0.5 }); }
    else { this.mp = Math.min(this.stats.maxMp, this.mp + this.stats.maxMp * 0.6); G.fx.burst(this.center(), 25, { speed: 2, up: 1.5, life: 1, size: 0.2, color: [0.3, 0.5, 1], spread: 0.5 }); }
    G.audio.play('potion', this.pos); if (!this.action) this.startAction({ anim: 'drink', dur: 0.6, move: 0.6 });
  }
  takeDot(amt, type) { damage(this.game, null, this, amt, { type, silent: true, noFlinch: true }); }
  heal(n, silent = false) { const G = this.game; const h = Math.min(n, this.stats.maxHp - this.hp); if (h <= 0) return; this.hp += h; if (silent) { this.healAcc = (this.healAcc || 0) + h; if (this.healAcc >= 15) { G.fx.number(this.center(), '+' + Math.round(this.healAcc), 'heal'); this.healAcc = 0; } } else G.fx.number(this.center(), '+' + Math.round(h), 'heal'); }
  die() {
    if (!this.alive) return; this.alive = false; this.hp = 0; this.st.dead = 0; this.action = null; this.st.act = null; this.rollT = -1;
    this.game.audio.play('die', this.pos, { pitch: 0.8 }); this.game.onPlayerDeath();
  }
  respawn(x, z) { this.alive = true; this.hp = this.stats.maxHp; this.mp = this.stats.maxMp; this.st.dead = null; this.pos.set(x, heightAt(x, z), z); this.vel.set(0, 0, 0); this.kb.set(0, 0, 0); for (const k in this.status) this.status[k] = 0; this.model.p.body.rotation.set(0, 0, 0); }
  update(dt) {
    const G = this.game, I = G.input, S = this.stats;
    this.invuln = Math.max(0, this.invuln - dt); this.potionCd = Math.max(0, this.potionCd - dt);
    for (let i = 0; i < 4; i++) this.cds[i] = Math.max(0, this.cds[i] - dt);
    if (!this.alive) { this.st.dead += dt; this.st.speed = 0; this.physics(dt, 0, 0); this.sync(dt); return; }
    this.tickStatus(dt);
    if (this.buff > 0) { this.buff -= dt; this.heal(S.maxHp * 0.025 * dt, true); if (Math.random() < 0.4) G.fx.burst(this.pos.clone().setY(this.pos.y + 0.2), 1, { speed: 1, up: 3, life: 0.8, size: 0.2, color: [1, 0.4, 0.1], spread: 0.5 }); }
    if (this.healOver) { const k = Math.min(dt, this.healOver.t); this.heal(this.healOver.amt * k / 1.5, true); this.healOver.t -= dt; if (this.healOver.t <= 0) this.healOver = null; }
    this.mp = Math.min(S.maxMp, this.mp + (1.5 + this.level * 0.25 + (this.cls === 'mage' ? 2 : 0)) * dt);
    if (!G.inCombat) this.hp = Math.min(S.maxHp, this.hp + S.maxHp * 0.012 * dt);
    // girdi → istenen hareket
    const cy = G.camYaw; let mx = I.mx, mz = I.mz; const ml = Math.hypot(mx, mz); if (ml > 1) { mx /= ml; mz /= ml; }
    const fx = -Math.sin(cy), fz = -Math.cos(cy), rx = Math.cos(cy), rz = -Math.sin(cy);
    let wx = fx * mz + rx * mx, wz = fz * mz + rz * mx;
    const inputMag = Math.hypot(wx, wz);
    const stunned = this.status.stun > 0 || this.status.freeze > 0;
    let sprint = I.sprint && inputMag > 0.2 && this.stamina > 1 && !this.action && !this.swimming;
    let spd = S.speed * (this.buff > 0 ? 1.2 : 1) * (this.status.slow > 0 ? 0.55 : 1) * (sprint ? 1.5 : 1) * (this.swimming ? 0.55 : 1);
    if (sprint) this.stamina = Math.max(0, this.stamina - 16 * dt); else if (this.rollT < 0) this.stamina = Math.min(100, this.stamina + 28 * dt);
    if (stunned) spd = 0;
    let tx = wx * spd, tz = wz * spd, accel = this.onGround ? 14 : 4;
    // aksiyon güncelle
    const a = this.action;
    if (a) {
      a.t += dt * (a.speed || 1); const k = a.t / a.dur; this.st.act = { name: a.anim, t: Math.min(1, k) };
      if (a.face) this.yaw = dampAngle(this.yaw, a.face, 20, dt);
      tx *= a.move; tz *= a.move;
      if (a.lunge && a.hit && a.t > a.hit - 0.14 && a.t < a.hit) { tx += Math.sin(this.yaw) * a.lunge; tz += Math.cos(this.yaw) * a.lunge; }
      if (a.vel) { tx = a.vel[0]; tz = a.vel[1]; accel = 30; }
      for (const [et, fn] of a.events || []) if (a.t >= et && !a.fired.has(et)) { a.fired.add(et); fn(); }
      if (a.draw && this.weapon.userData.setDraw) this.weapon.userData.setDraw(clamp(a.t / (a.dur * 0.5), 0, 1) * (a.t < a.dur * 0.55 ? 1 : 0));
      a.update?.(dt, a);
      if (a.combo && this.queued && k > 0.62) { this.action = null; this.queued = false; this.attack(); }
      else if (a.t >= a.dur) { this.action = null; this.st.act = null; a.end?.(); if (!a.combo) this.combo = 0; if (this.queued) { this.queued = false; this.attack(); } else this.comboReset = 0.5; }
    } else if (this.comboReset > 0) { this.comboReset -= dt; if (this.comboReset <= 0) this.combo = 0; }
    if (this.weapon.userData.setDraw && (!a || !a.draw)) this.weapon.userData.setDraw(0);
    // takla
    if (this.rollT >= 0) {
      this.rollT += dt; const k = this.rollT / 0.55; tx = this.rollDir[0] * 12 * (1 - k * 0.5); tz = this.rollDir[1] * 12 * (1 - k * 0.5); accel = 40;
      this.st.roll = Math.min(1, k); if (k >= 1) { this.rollT = -1; this.st.roll = null; }
    } else this.st.roll = null;
    // yöne dön
    if (!a && this.rollT < 0 && inputMag > 0.1 && !stunned) this.yaw = dampAngle(this.yaw, Math.atan2(wx, wz), 14, dt);
    else if (a && a.turn && inputMag > 0.1) this.yaw = dampAngle(this.yaw, Math.atan2(wx, wz), 6, dt);
    this.physics(dt, tx, tz, accel);
    // düşmanlarla itişme
    for (const e of G.enemies) { if (!e.alive) continue; const dx = this.pos.x - e.pos.x, dz = this.pos.z - e.pos.z, d = Math.hypot(dx, dz), m = this.radius + e.radius * 0.9; if (d < m && d > 0.001 && Math.abs(e.pos.y - this.pos.y) < 2) { this.pos.x += dx / d * (m - d); this.pos.z += dz / d * (m - d); } }
    this.st.speed = Math.hypot(this.vel.x, this.vel.z) * (stunned ? 0 : 1); this.st.air = !this.onGround && !this.swimming && this.rollT < 0; this.st.vy = this.vy; this.st.swim = this.swimming;
    // adım sesi
    const ph = Math.floor(this.st.phase / Math.PI); if (ph !== this.lastStep && this.onGround && this.st.speed > 1) { this.lastStep = ph; const w = biomeWeights(this.pos.x, this.pos.z); G.audio.play(w.snow > 0.5 ? 'snowstep' : 'step', this.pos, { pitch: rand(0.8, 1.2) }); if (sprint || w.desert > 0.5 || w.snow > 0.5) G.fx.dust(this.pos.clone().setY(this.pos.y + 0.05), 2, w.snow > 0.5 ? [0.95, 0.97, 1] : w.desert > 0.5 ? [0.85, 0.7, 0.5] : [0.55, 0.5, 0.4]); }
    if (this.swimming && Math.random() < 0.3 && this.st.speed > 0.5) G.fx.burst(this.pos.clone().setY(WATER_Y + 0.05), 2, { speed: 1.5, up: 1, life: 0.6, size: 0.15, color: [0.8, 0.9, 1], normal: true, alpha: 0.7, grav: 6 });
    this.sync(dt);
    const tr = a?.trail; const on = !!tr && a.t >= tr[0] && a.t <= tr[1];
    const wb = new THREE.Vector3(), wt = new THREE.Vector3(); this.weapon.userData.base.getWorldPosition(wb); this.weapon.userData.tip.getWorldPosition(wt);
    this.trail.update(dt, wb, wt, on || (this.spinTrail > 0));
    if (this.spinTrail > 0) this.spinTrail -= dt;
    if (this.cls === 'mage' && this.weapon.userData.orb && Math.random() < 0.3) G.fx.burst(wt, 1, { speed: 0.3, life: 0.5, size: 0.12, color: [0.5, 0.6, 1] });
  }
}
