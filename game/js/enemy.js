// Düşmanlar: yapay zeka, saldırı repertuarı, boss mekanikleri
import * as THREE from 'three';
import { Actor } from './actor.js';
import { buildHumanoid, buildQuadruped, buildArachnid, buildSlime, buildDragon, makeWeapon } from './models.js';
import { ENEMIES } from './data.js';
import { damage, aoe, Projectile, inCone, inLine } from './combat.js';
import { heightAt } from './terrain.js';
import { rand, dampAngle, wrapAngle, clamp, pick } from './util.js';

function buildModel(def) {
  const L = def.look || {};
  let m;
  switch (def.model) {
    case 'humanoid': m = buildHumanoid(L); break;
    case 'quad': m = buildQuadruped(L); break;
    case 'spider': m = buildArachnid(L); break;
    case 'scorpion': m = buildArachnid({ ...L, scorpion: true }); break;
    case 'slime': m = buildSlime(L); break;
    case 'dragon': m = buildDragon(L); break;
  }
  if (def.weapon) {
    const w = makeWeapon(def.weapon, def.weaponColor ?? 0xa0a4a8, def.weaponColor ? 1.5 : 0);
    w.rotation.set(Math.PI / 2, 0, 0); if (def.weaponScale) w.scale.setScalar(def.weaponScale);
    if (def.weapon === 'staff' || def.weapon === 'spear') w.position.set(0, 0, -0.5);
    m.p.rSocket.add(w); m.weapon = w; w.userData.mats.forEach((x) => { x.userData.baseEmissive = x.emissive.clone(); x.userData.baseEI = x.emissiveIntensity; m.mats.push(x); });
  }
  if (def.shield) { const s = makeWeapon('shield'); s.rotation.set(0, Math.PI / 2, 0); s.position.set(0.1, -0.18, 0); m.p.lElbow.add(s); }
  return m;
}

// Saldırı tanımları
// anim, dur, hit (sn), range, arc(derece), mult, cd, lunge, tele:{shape, r, at, arc, len, w}, proj, status, custom
const A = {
  slimeSlam: { anim: 'slam', dur: 1.0, hit: 0.72, range: 2, arc: 360, mult: 1, cd: 1.6, lunge: 3.5 },
  bite: { anim: 'bite', dur: 0.8, hit: 0.44, range: 2.3, arc: 100, mult: 1, cd: 1.3, lunge: 5 },
  pounce: { anim: 'pounce', dur: 1.1, hit: 0.6, range: 7, minRange: 3.5, arc: 110, mult: 1.4, cd: 5, leap: 10 },
  stab: { anim: 'thrust', dur: 0.62, hit: 0.36, range: 1.9, arc: 90, mult: 1, cd: 1, lunge: 4 },
  stab2: { anim: 'slash1', dur: 0.62, hit: 0.36, range: 1.9, arc: 120, mult: 1, cd: 1, lunge: 3 },
  boltGreen: { anim: 'staffBolt', dur: 0.95, hit: 0.5, range: 18, arc: 360, mult: 1, cd: 2.2, proj: { type: 'bolt', color: 0x80ff40, pcolor: [0.5, 1, 0.3], speed: 16 } },
  spiderBite: { anim: 'bite', dur: 0.85, hit: 0.5, range: 2.4, arc: 100, mult: 1, cd: 1.4, lunge: 5, status: { poison: [4, 3] } },
  web: { anim: 'spit', dur: 0.9, hit: 0.48, range: 15, minRange: 5, arc: 360, mult: 0.5, cd: 6, proj: { type: 'web', speed: 18, status: { slow: [3] } } },
  slash1: { anim: 'slash1', dur: 0.9, hit: 0.52, range: 2.3, arc: 130, mult: 1, cd: 1.4, lunge: 3 },
  overhead: { anim: 'overhead', dur: 1.15, hit: 0.7, range: 2.6, arc: 70, mult: 1.5, cd: 2.2, lunge: 3, heavy: true },
  sweep: { anim: 'sweep', dur: 1.1, hit: 0.65, range: 2.8, arc: 200, mult: 1.2, cd: 2.5 },
  pinch: { anim: 'pinch', dur: 0.9, hit: 0.5, range: 2.8, arc: 100, mult: 1, cd: 1.3, lunge: 3 },
  sting: { anim: 'sting', dur: 1.1, hit: 0.62, range: 3.2, arc: 70, mult: 1.3, cd: 3, status: { poison: [5, 4] } },
  punch: { anim: 'punch', dur: 0.95, hit: 0.54, range: 2.6, arc: 100, mult: 1, cd: 1.4, lunge: 3 },
  punch2: { anim: 'punch2', dur: 0.95, hit: 0.54, range: 2.6, arc: 100, mult: 1, cd: 1.4, lunge: 3 },
  slam: { anim: 'slam', dur: 1.5, hit: 0.9, range: 3.6, arc: 360, mult: 1.6, cd: 5, tele: { shape: 'circle', r: 3.6, at: 'front', off: 1.6 }, heavy: true, shake: 0.3 },
  fireBolt: { anim: 'cast', dur: 1.0, hit: 0.52, range: 20, arc: 360, mult: 1, cd: 2.4, proj: { type: 'fireball', speed: 17, aoe: 2.2, status: { burn: [3, 4] } } },
  fireBreathSmall: { anim: 'bite', dur: 1.4, hit: 0.5, range: 5, minRange: 0, arc: 360, mult: 0.35, cd: 6, tele: { shape: 'cone', r: 6, arc: 0.9, at: 'self' }, ticks: 4, status: { burn: [3, 5] }, breath: true },
  // ---- boss saldırıları ----
  bossStomp: { anim: 'stomp', dur: 1.6, hit: 0.9, range: 7, arc: 360, mult: 1.3, cd: 7, tele: { shape: 'circle', r: 7.5, at: 'self' }, heavy: true, shake: 0.5, status: { stun: [0.8] } },
  bossCharge: { anim: 'overhead', dur: 1.9, hit: 1.3, range: 18, minRange: 6, arc: 360, mult: 1.6, cd: 8, tele: { shape: 'line', len: 18, w: 3, at: 'self' }, charge: true, heavy: true },
  bossSlam: { anim: 'slam', dur: 1.7, hit: 1.0, range: 5.5, arc: 360, mult: 1.7, cd: 5, tele: { shape: 'circle', r: 5.5, at: 'front', off: 3.5 }, heavy: true, shake: 0.55, wave: true },
  rootLine: { anim: 'castUp', dur: 1.8, hit: 1.1, range: 20, arc: 360, mult: 1.3, cd: 9, custom: 'rootLine' },
  summonSaplings: { anim: 'roar', dur: 1.6, hit: 0.8, range: 30, arc: 360, mult: 0, cd: 22, custom: 'summon' },
  bossSting: { anim: 'sting', dur: 1.6, hit: 1.05, range: 9, arc: 360, mult: 1.6, cd: 5, tele: { shape: 'circle', r: 3.4, at: 'target' }, status: { poison: [6, 10] }, heavy: true, shake: 0.4 },
  sandSpray: { anim: 'slam', dur: 1.5, hit: 0.9, range: 12, arc: 360, mult: 1.2, cd: 8, tele: { shape: 'cone', r: 13, arc: 1.1, at: 'self' }, custom: 'sandSpray' },
  burrow: { anim: 'slam', dur: 3.2, hit: 2.8, range: 30, minRange: 8, arc: 360, mult: 2, cd: 14, custom: 'burrow' },
  iceSpikes: { anim: 'castUp', dur: 1.9, hit: 1.25, range: 22, arc: 360, mult: 1.2, cd: 9, custom: 'iceSpikes' },
  frostBreath: { anim: 'roar', dur: 2.2, hit: 0.6, range: 12, arc: 360, mult: 0.35, cd: 10, tele: { shape: 'cone', r: 13, arc: 1.2, at: 'self' }, ticks: 7, status: { slow: [3], freeze: [0.15] }, breath: 'ice' },
  dBite: { anim: 'bite', dur: 1.1, hit: 0.58, range: 8, arc: 70, mult: 1.2, cd: 2, lunge: 4 },
  dClaw: { anim: 'claw', dur: 1.2, hit: 0.62, range: 7, arc: 140, mult: 1.1, cd: 2.5, tele: { shape: 'cone', r: 7.5, arc: 2.3, at: 'self' } },
  dTail: { anim: 'tail', dur: 1.5, hit: 0.72, range: 10, arc: 360, mult: 1.3, cd: 6, tele: { shape: 'circle', r: 10, at: 'self' }, heavy: true, shake: 0.4 },
  dBreath: { anim: 'breath', dur: 2.8, hit: 0.9, range: 20, arc: 360, mult: 0.28, cd: 8, tele: { shape: 'cone', r: 20, arc: 0.9, at: 'self' }, ticks: 9, status: { burn: [4, 12] }, breath: 'fire' },
  dFly: { anim: 'roar', dur: 9, hit: 8.4, range: 60, minRange: 10, arc: 360, mult: 1.8, cd: 25, custom: 'fly' },
};

export class Enemy extends Actor {
  constructor(game, type, x, z, lvlBonus = 0, camp = null) {
    const def = ENEMIES[type]; const model = buildModel(def);
    super(game, model);
    this.type = type; this.def = def; this.camp = camp; this.team = 'enemy';
    this.level = def.lvl + lvlBonus; const sc = 1 + lvlBonus * 0.18;
    this.maxHp = Math.round(def.hp * sc); this.hp = this.maxHp; this.dmg = def.dmg * (1 + lvlBonus * 0.12);
    this.radius = def.radius; this.speed = def.speed; this.home = new THREE.Vector3(x, 0, z);
    this.pos.set(x, heightAt(x, z), z); this.yaw = rand(0, Math.PI * 2);
    this.state = 'idle'; this.cd = rand(0.5, 1.5); this.atk = null; this.wanderT = rand(1, 4); this.wanderTo = null; this.aggroT = 0;
    this.st.heavy = def.heavy ? 1 : 0; this.boss = !!def.boss; this.phase2 = false;
    if (def.flying) this.hover = 2.2;
    this.sync(0.016);
  }
  dist() { const P = this.game.player; return Math.hypot(P.pos.x - this.pos.x, P.pos.z - this.pos.z); }
  onDamaged(src) { if (src === this.game.player) { this.aggroT = 12; if (this.state === 'idle' || this.state === 'return') this.state = 'chase'; this.alertGroup(); } }
  alertGroup() { if (!this.camp) return; for (const e of this.game.enemies) if (e.camp === this.camp && e !== this && e.alive && e.state === 'idle') { e.state = 'chase'; e.aggroT = 8; } }
  takeDot(amt, type) { damage(this.game, this.game.player, this, amt, { type, silent: true, noFlinch: true, canCrit: false }); }
  die(src) {
    if (!this.alive) return; this.alive = false; this.hp = 0; this.st.dead = 0; this.st.act = null; this.atk?.decal && (this.atk.decal.t = 99);
    this.game.audio.play(this.def.boss ? 'roar' : 'die', this.pos, { pitch: this.def.pitch || 1 });
    if (this.def.model === 'slime') this.game.fx.burst(this.center(), 30, { speed: 5, life: 0.8, size: 0.3, color: [0.4, 0.9, 0.3], grav: 12, normal: true, floor: true });
    this.game.onKill(this);
    this.deadT = 0;
  }
  start(name) {
    const a = A[name]; const P = this.game.player;
    this.atk = { name, def: a, t: 0, done: false, ticks: 0, target: P.pos.clone(), yaw: this.yaw };
    this.st.act = { name: a.anim, t: 0 };
    if (a.tele) this.makeTele(a);
    if (this.def.boss || a.heavy) this.game.audio.play(this.def.sound === 'roar' ? 'growl' : 'growl', this.pos, { pitch: (this.def.pitch || 1), vol: 0.7 });
    if (a.custom) this.customStart?.(a.custom);
  }
  makeTele(a) {
    const T = a.tele, fx = this.game.fx, time = a.hit * this.atkSpeed();
    let x = this.pos.x, z = this.pos.z;
    const P = this.game.player;
    const yaw = Math.atan2(P.pos.x - this.pos.x, P.pos.z - this.pos.z); this.yaw = yaw; this.atk.yaw = yaw;
    if (T.at === 'front') { x += Math.sin(yaw) * T.off * (this.model.scale > 1 ? 1 : 1); z += Math.cos(yaw) * T.off; }
    if (T.at === 'target') { x = P.pos.x; z = P.pos.z; }
    this.atk.cx = x; this.atk.cz = z;
    if (T.shape === 'circle') this.atk.decal = fx.telegraph(x, z, T.r, time);
    else if (T.shape === 'cone') this.atk.decal = fx.telegraph(x, z, T.r, time, 0xff4020, 'cone', yaw, T.arc);
    else if (T.shape === 'line') this.atk.decal = fx.telegraph(x, z, T.len, time, 0xff4020, 'line', yaw, T.w);
  }
  atkSpeed() { return this.phase2 ? 0.8 : 1; }
  chooseAttack(d) {
    const list = this.def.attacks.filter((n) => { const a = A[n]; return d <= a.range + this.radius && d >= (a.minRange || 0) && !(this.cdMap?.[n] > 0); });
    if (!list.length) return null;
    // boss: özel saldırıları tercih et
    if (this.boss) { const special = list.filter((n) => A[n].cd >= 5); if (special.length && Math.random() < 0.55) return pick(special); }
    return pick(list);
  }
  update(dt) {
    const G = this.game, P = G.player;
    if (!this.alive) {
      this.deadT += dt; this.st.dead = this.deadT; this.st.speed = 0;
      if (this.deadT > 2.2) this.pos.y -= dt * 1.2 * (this.model.height > 3 ? 1.5 : 1);
      this.vel.set(0, 0, 0); this.sync(dt); return this.deadT < (this.boss ? 5 : 3.6);
    }
    this.tickStatus(dt); this.cdMap ||= {}; for (const k in this.cdMap) this.cdMap[k] -= dt;
    const d = this.dist(); const stunned = this.status.stun > 0 || this.status.freeze > 0;
    const slow = this.status.slow > 0 ? 0.5 : 1; const def = this.def;
    this.aggroT -= dt;
    // durum makinesi
    const homeD = Math.hypot(this.pos.x - this.home.x, this.pos.z - this.home.z);
    if (P.alive && this.state === 'idle' && d < def.aggro) { this.state = 'chase'; this.alertGroup(); if (!this.boss) G.audio.play(def.sound || 'growl', this.pos, { pitch: def.pitch || 1, vol: 0.6 }); else this.bossIntro(); }
    if (this.state === 'chase' && (!P.alive || (homeD > (this.boss ? 45 : 55) && this.aggroT <= 0) || d > 70)) { this.state = 'return'; this.atk = null; this.st.act = null; }
    if (this.state === 'return' && homeD < 3) { this.state = 'idle'; this.hp = this.maxHp; }
    if (this.boss && !this.phase2 && this.hp < this.maxHp * 0.5) { this.phase2 = true; this.speed *= 1.2; G.ui.toast(`${def.name} öfkeleniyor!`, 'red'); this.start(def.model === 'dragon' ? 'dFly' : 'summonSaplings' in A && def.attacks.includes('summonSaplings') ? 'summonSaplings' : def.attacks[def.attacks.length - 1]); }
    let tx = 0, tz = 0; let faceP = false;
    if (stunned) { if (this.atk) { this.atk.decal && (this.atk.decal.t = 99); this.atk = null; this.st.act = null; } }
    else if (this.atk) this.updateAttack(dt);
    else if (this.state === 'chase') {
      this.cd -= dt;
      const want = def.ranged ? Math.min(def.range * 0.6, 12) : 0;
      if (d > this.radius + P.radius + 0.4 && (!def.ranged || d > want)) { const k = this.speed * slow; tx = (P.pos.x - this.pos.x) / d * k; tz = (P.pos.z - this.pos.z) / d * k; }
      else if (def.ranged && d < want * 0.6) { const k = this.speed * 0.7; tx = -(P.pos.x - this.pos.x) / d * k; tz = -(P.pos.z - this.pos.z) / d * k; }
      else if (def.ranged) { const k = this.speed * 0.5, s = Math.sin(this.st.time * 0.7 + this.home.x) > 0 ? 1 : -1; tx = (P.pos.z - this.pos.z) / d * k * s; tz = -(P.pos.x - this.pos.x) / d * k * s; }
      faceP = true;
      if (this.cd <= 0 && P.alive) { const n = this.chooseAttack(d); if (n) { this.start(n); this.cd = A[n].cd * (this.phase2 ? 0.7 : 1) * rand(0.8, 1.2); this.cdMap[n] = A[n].cd * 1.2; } }
    } else if (this.state === 'return') { const k = this.speed * 0.8; tx = (this.home.x - this.pos.x) / homeD * k; tz = (this.home.z - this.pos.z) / homeD * k; this.hp = Math.min(this.maxHp, this.hp + this.maxHp * 0.2 * dt); }
    else {
      this.wanderT -= dt;
      if (this.wanderT <= 0) { this.wanderT = rand(3, 7); this.wanderTo = Math.random() < 0.6 ? new THREE.Vector3(this.home.x + rand(-9, 9), 0, this.home.z + rand(-9, 9)) : null; }
      if (this.wanderTo && !this.boss) { const wx = this.wanderTo.x - this.pos.x, wz = this.wanderTo.z - this.pos.z, wd = Math.hypot(wx, wz); if (wd > 0.8) { tx = wx / wd * this.speed * 0.3; tz = wz / wd * this.speed * 0.3; } else this.wanderTo = null; }
    }
    // ayrışma
    for (const o of G.enemies) { if (o === this || !o.alive) continue; const dx = this.pos.x - o.pos.x, dz = this.pos.z - o.pos.z, dd = Math.hypot(dx, dz), m = this.radius + o.radius; if (dd < m && dd > 0.001) { tx += dx / dd * 3; tz += dz / dd * 3; } }
    if (this.atk?.move) { tx = this.atk.move[0]; tz = this.atk.move[1]; }
    const flying = !!this.hover || this.flying;
    this.physics(dt, tx, tz, this.atk?.move ? 20 : 8, { flying, noCollide: this.burrowed, noSwim: this.boss });
    if (this.hover && !this.flying) { const gy = heightAt(this.pos.x, this.pos.z) + this.hover + Math.sin(this.st.time * 2) * 0.3; this.pos.y += (gy - this.pos.y) * Math.min(1, dt * 3); }
    if (faceP || this.atk?.track) this.yaw = dampAngle(this.yaw, Math.atan2(P.pos.x - this.pos.x, P.pos.z - this.pos.z), this.boss ? 4 : 8, dt);
    else if (Math.hypot(tx, tz) > 0.5) this.yaw = dampAngle(this.yaw, Math.atan2(tx, tz), 6, dt);
    this.st.speed = stunned ? 0 : Math.hypot(this.vel.x, this.vel.z); this.st.aggro = this.state === 'chase'; this.st.air = !this.onGround && !flying && !this.burrowed;
    this.st.vy = this.vy; this.st.lookY = 0;
    this.sync(dt);
    return true;
  }
  updateAttack(dt) {
    const G = this.game, P = G.player, at = this.atk, a = at.def; const sp = this.atkSpeed();
    at.t += dt / sp; const k = at.t / a.dur; this.st.act = { name: a.anim, t: Math.min(1, k) };
    at.move = null; at.track = at.t < a.hit * 0.6 && !a.tele;
    if (a.lunge && at.t > a.hit - 0.18 && at.t < a.hit) at.move = [Math.sin(this.yaw) * a.lunge, Math.cos(this.yaw) * a.lunge];
    if (a.leap && at.t > a.hit - 0.35 && !at.leapt) { at.leapt = true; this.vy = 7; const dd = Math.min(this.dist(), 9); at.leapV = [Math.sin(this.yaw) * dd * 1.8, Math.cos(this.yaw) * dd * 1.8]; }
    if (at.leapV && !this.onGround) at.move = at.leapV;
    if (a.charge && at.t > a.hit - 0.45 && at.t < a.hit + 0.1) { at.move = [Math.sin(at.yaw) * 38, Math.cos(at.yaw) * 38]; this.yaw = at.yaw; if (inLine(P.pos.x, P.pos.z, this.pos.x, this.pos.z, at.yaw, 2.5, 2.5) && !at.done) { at.done = true; damage(G, this, P, this.dmg * a.mult, { kb: 14, lift: 5, heavy: true }); } if (Math.random() < 0.5) G.fx.dust(this.pos, 3); }
    if (a.custom) this.customUpdate?.(dt, at, a);
    // vuruş anı
    if (!at.done && at.t >= a.hit && !a.charge) {
      at.done = true;
      if (a.ticks) { at.tickT = 0; at.ticking = a.ticks; }
      else this.resolveHit(a, at);
    }
    if (at.ticking > 0) {
      at.tickT -= dt; this.breathFx(a, at);
      if (at.tickT <= 0) { at.tickT = (a.dur - a.hit) / (a.ticks + 1); at.ticking--; if (inCone(P.pos.x, P.pos.z, this.pos.x, this.pos.z, this.yaw, a.tele?.r || a.range, a.tele?.arc || 1)) damage(G, this, P, this.dmg * a.mult, { status: a.status, noFlinch: true }); }
    }
    if (at.t >= a.dur) { this.atk = null; this.st.act = null; }
  }
  breathFx(a, at) {
    const G = this.game; const p = this.model.p; const from = new THREE.Vector3();
    (p.mouth || p.head || p.body).getWorldPosition(from);
    const fire = a.breath !== 'ice'; const r = a.tele?.r || 6;
    for (let i = 0; i < (this.boss ? 6 : 3); i++) {
      const sp = rand(0.7, 1) * r * 1.6; const ang = this.yaw + rand(-0.35, 0.35) * (a.tele?.arc || 1);
      (fire ? G.fx.add : G.fx.add).add({ x: from.x, y: from.y, z: from.z, vx: Math.sin(ang) * sp, vy: rand(-2, 1), vz: Math.cos(ang) * sp, life: 0.65, size: 0.4, size2: this.boss ? 2.8 : 1.4, r: fire ? 1 : 0.6, g: fire ? 0.7 : 0.85, b: fire ? 0.25 : 1, r2: fire ? 0.8 : 0.3, g2: fire ? 0.15 : 0.5, b2: fire ? 0.02 : 1, drag: 1.2 });
    }
    if (Math.random() < 0.2) G.audio.play('breath', from, { min: 1.2 });
    if (fire) G.fx.light(from, [1, 0.5, 0.15], this.boss ? 30 : 10, 0.15);
  }
  resolveHit(a, at) {
    const G = this.game, P = G.player;
    if (a.proj) {
      const from = new THREE.Vector3(); (this.model.weapon?.userData.tip || this.model.p.head || this.model.p.body).getWorldPosition(from);
      const to = P.center(); const dir = to.clone().sub(from).normalize();
      const pr = a.proj;
      G.projectiles.push(new Projectile(G, { type: pr.type, pos: from, dir, speed: pr.speed, team: 'enemy', src: this, color: pr.color, pcolor: pr.pcolor, radius: 0.45, life: 3,
        onHit: (p, e) => damage(G, this, e, this.dmg * a.mult, { status: pr.status }),
        onExplode: pr.aoe ? (p) => { G.fx.explosion(p.pos, pr.aoe * 0.7); G.audio.play('explode', p.pos); aoe(G, this, p.pos, pr.aoe, this.dmg * a.mult * 0.5, { status: pr.status }); } : null }));
      G.audio.play(pr.type === 'fireball' ? 'fireball' : pr.type === 'web' ? 'web' : 'magic', from);
      return;
    }
    if (a.custom) { this.customHit?.(a.custom, at, a); return; }
    if (a.tele && a.tele.shape === 'circle') {
      const c = new THREE.Vector3(at.cx, heightAt(at.cx, at.cz), at.cz);
      aoe(G, this, c, a.tele.r, this.dmg * a.mult, { kb: a.heavy ? 10 : 5, lift: a.heavy ? 5 : 0, status: a.status, heavy: a.heavy });
      G.fx.ring(c, a.tele.r, [1, 0.8, 0.5], 0.5); G.fx.dust(c, 16, [0.6, 0.55, 0.45]); G.fx.burst(c, 20, { speed: 7, up: 1.5, life: 0.7, size: 0.2, color: [0.5, 0.45, 0.4], grav: 15, normal: true, spread: a.tele.r * 0.5 });
      G.fx.addShake(a.shake || 0.25); G.audio.play('shock', c);
      if (a.wave) this.spawnWave(c);
      return;
    }
    if (a.tele && a.tele.shape === 'cone') { if (inCone(P.pos.x, P.pos.z, this.pos.x, this.pos.z, at.yaw, a.tele.r, a.tele.arc)) damage(G, this, P, this.dmg * a.mult, { kb: 7, status: a.status }); G.audio.play('heavySwing', this.pos, { pitch: 0.6 }); return; }
    // yakın dövüş yayı
    G.audio.play(this.def.weapon ? 'swing' : 'swing', this.pos, { pitch: this.boss ? 0.6 : 1 });
    const d = this.dist();
    if (d < a.range + this.radius + P.radius && Math.abs(P.pos.y - this.pos.y) < 2.5 + this.model.height * 0.3) {
      const ang = Math.abs(wrapAngle(Math.atan2(P.pos.x - this.pos.x, P.pos.z - this.pos.z) - this.yaw));
      if (a.arc >= 360 || ang < (a.arc / 2) * Math.PI / 180) damage(G, this, P, this.dmg * a.mult, { kb: a.heavy ? 9 : 4, lift: a.heavy ? 4 : 0, status: a.status, heavy: a.heavy, melee: !!this.def.weapon && false });
    }
    if (a.shake) G.fx.addShake(a.shake);
  }
  spawnWave(c) {
    const G = this.game; let r = 2; const t0 = G.time;
    const wave = { update: (dt) => { r += dt * 14; G.fx.ring(c, r, [1, 0.6, 0.3], 0.15); const P = G.player; const d = Math.hypot(P.pos.x - c.x, P.pos.z - c.z); if (!wave.hit && Math.abs(d - r) < 1 && P.onGround && P.invuln <= 0) { wave.hit = true; damage(G, this, P, this.dmg * 0.6, { kb: 6 }); } return r < 14; } };
    G.effects.push(wave);
  }
  bossIntro() { const G = this.game; G.ui.bossIntro(this); G.audio.play('roar', this.pos, { pitch: this.def.pitch || 0.6 }); this.start(this.def.model === 'dragon' ? 'dTail' : this.def.attacks.includes('summonSaplings') ? 'summonSaplings' : this.def.attacks[0]); this.cd = 2; }
  // ------- özel boss saldırıları -------
  customStart(kind) {
    const G = this.game, P = G.player, at = this.atk;
    if (kind === 'rootLine') {
      at.lines = []; const base = Math.atan2(P.pos.x - this.pos.x, P.pos.z - this.pos.z);
      for (const off of this.phase2 ? [-0.5, -0.25, 0, 0.25, 0.5] : [-0.35, 0, 0.35]) { const y = base + off; at.lines.push(y); G.fx.telegraph(this.pos.x, this.pos.z, 20, at.def.hit, 0x60ff30, 'line', y, 2.2); }
    }
    if (kind === 'iceSpikes') { at.spots = []; const n = this.phase2 ? 12 : 8; for (let i = 0; i < n; i++) { const x = P.pos.x + (i === 0 ? 0 : rand(-8, 8)), z = P.pos.z + (i === 0 ? 0 : rand(-8, 8)); at.spots.push([x, z]); G.fx.telegraph(x, z, 2.4, at.def.hit, 0x60c0ff); } }
    if (kind === 'sandSpray') at.yaw = this.yaw;
    if (kind === 'burrow') { at.phase = 0; }
    if (kind === 'fly') { at.phase = 0; at.drops = 0; this.st.wingsOpen = true; G.audio.play('roar', this.pos); }
    if (kind === 'summon') { }
  }
  customUpdate(dt, at, a) {
    const G = this.game, P = G.player;
    if (a.custom === 'burrow') {
      if (at.t < 0.6) { G.fx.dust(this.pos, 3, [0.85, 0.7, 0.5]); }
      else if (at.t < a.hit - 0.8) { this.burrowed = true; this.root.visible = false; this.invuln = 1; const dx = P.pos.x - this.pos.x, dz = P.pos.z - this.pos.z, dd = Math.hypot(dx, dz); if (dd > 0.5) at.move = [dx / dd * 14, dz / dd * 14]; G.fx.dust(this.pos, 2, [0.85, 0.7, 0.5]); if (!at.decal) at.decal = G.fx.telegraph(this.pos.x, this.pos.z, 5, 99); at.decal.m.position.set(this.pos.x, heightAt(this.pos.x, this.pos.z) + 0.2, this.pos.z); at.decal.inner.position.copy(at.decal.m.position); }
      else if (at.t < a.hit) { at.move = [0, 0]; if (at.decal) { at.decal.inner.scale.set(10 * (1 - (a.hit - at.t) / 0.8), 1, 10 * (1 - (a.hit - at.t) / 0.8)); } }
      else { this.burrowed = false; this.root.visible = true; this.invuln = 0; if (at.decal) { at.decal.t = 999; at.decal = null; } }
    }
    if (a.custom === 'fly') {
      const fly = at.t > 0.8 && at.t < a.hit;
      this.flying = fly; this.st.fly = fly ? 1 : Math.max(0, (this.st.fly || 0) - dt);
      const gy = heightAt(this.pos.x, this.pos.z);
      if (fly) {
        const targetY = gy + 14; this.pos.y += (targetY - this.pos.y) * Math.min(1, dt * 1.5);
        const ang = G.time * 0.6; const cx = this.home.x + Math.cos(ang) * 22, cz = this.home.z + Math.sin(ang) * 22;
        at.move = [(cx - this.pos.x) * 1.5, (cz - this.pos.z) * 1.5];
        if (Math.floor(at.t * 4) % 2 === 0 && Math.random() < dt * 20) G.audio.play('flap', this.pos, { min: 0.5 });
        at.dropT = (at.dropT ?? 1) - dt;
        if (at.dropT <= 0) { at.dropT = this.phase2 ? 0.45 : 0.65; this.dropFireball(P.pos.x + rand(-4, 4) * (at.drops % 3 ? 1 : 0), P.pos.z + rand(-4, 4) * (at.drops % 3 ? 1 : 0)); at.drops++; }
      } else if (at.t >= a.hit - 0.6 || at.t < 0.8) {
        if (at.t > 1 && !at.landTele) { at.landTele = true; at.landX = P.pos.x; at.landZ = P.pos.z; G.fx.telegraph(at.landX, at.landZ, 8, a.hit - at.t); }
        if (at.landTele) { at.move = [(at.landX - this.pos.x) * 3, (at.landZ - this.pos.z) * 3]; this.pos.y += (gy - this.pos.y) * Math.min(1, dt * 5); }
      }
    }
  }
  dropFireball(x, z) {
    const G = this.game; const y = heightAt(x, z);
    G.fx.telegraph(x, z, 3.2, 1.1);
    const from = new THREE.Vector3(x + rand(-6, 6), y + 26, z + rand(-6, 6)); const dir = new THREE.Vector3(x, y, z).sub(from).normalize();
    G.projectiles.push(new Projectile(G, { type: 'meteor', pos: from, dir, speed: 24, team: 'enemy', src: this, radius: 1, life: 4, onExplode: (p) => { G.fx.explosion(p.pos, 3); G.audio.play('explode', p.pos); aoe(G, this, p.pos, 3.2, this.dmg * 0.8, { kb: 6, status: { burn: [3, 8] } }); } }));
  }
  customHit(kind, at, a) {
    const G = this.game, P = G.player;
    if (kind === 'rootLine') {
      for (const y of at.lines) {
        for (let i = 1; i <= 10; i++) { const x = this.pos.x + Math.sin(y) * i * 2, z = this.pos.z + Math.cos(y) * i * 2; this.spike(x, z, 0x5a4030, i * 0.03, 'bark'); }
        if (inLine(P.pos.x, P.pos.z, this.pos.x, this.pos.z, y, 20, 1.3)) damage(G, this, P, this.dmg * a.mult, { kb: 6, lift: 6, status: { slow: [2] } });
      }
      G.audio.play('shock', this.pos); G.fx.addShake(0.35);
    }
    if (kind === 'iceSpikes') {
      at.spots.forEach(([x, z], i) => { this.spike(x, z, 0x9ad8ff, i * 0.04, 'ice'); if (Math.hypot(P.pos.x - x, P.pos.z - z) < 2.4) damage(G, this, P, this.dmg * a.mult, { kb: 4, lift: 7, status: { slow: [2.5] } }); });
      G.audio.play('ice', P.pos); G.fx.addShake(0.3);
    }
    if (kind === 'sandSpray') {
      for (let i = 0; i < 60; i++) { const ang = at.yaw + rand(-0.55, 0.55), sp = rand(8, 22); G.fx.norm.add({ x: this.pos.x, y: this.pos.y + 1.5, z: this.pos.z, vx: Math.sin(ang) * sp, vy: rand(0, 4), vz: Math.cos(ang) * sp, life: 1.1, size: 0.8, size2: 2.5, r: 0.85, g: 0.7, b: 0.45, a: 0.6, drag: 1.5 }); }
      if (inCone(P.pos.x, P.pos.z, this.pos.x, this.pos.z, at.yaw, 13, 1.1)) damage(G, this, P, this.dmg * a.mult, { kb: 10, status: { slow: [2] } });
      G.audio.play('heavySwing', this.pos, { pitch: 0.4 });
    }
    if (kind === 'burrow') { const c = this.pos.clone(); aoe(G, this, c, 5, this.dmg * a.mult, { kb: 12, lift: 9, heavy: true }); G.fx.explosion(c.setY(c.y + 0.5), 4, [0.9, 0.75, 0.5], [0.4, 0.3, 0.2]); G.fx.dust(c, 30, [0.85, 0.7, 0.5]); G.audio.play('shock', c); }
    if (kind === 'fly') { this.flying = false; this.st.fly = 0; this.st.wingsOpen = false; const c = this.pos.clone(); aoe(G, this, c, 8, this.dmg * a.mult, { kb: 14, lift: 7, heavy: true }); G.fx.ring(c, 9, [1, 0.6, 0.3], 0.6); G.fx.dust(c, 40); G.fx.addShake(0.8); G.audio.play('shock', c); G.audio.play('explode', c); }
    if (kind === 'summon') {
      const n = this.phase2 ? 4 : 3;
      for (let i = 0; i < n; i++) { const ang = (i / n) * Math.PI * 2; const x = this.pos.x + Math.cos(ang) * 7, z = this.pos.z + Math.sin(ang) * 7; const type = this.type === 'treant' ? 'sapling' : 'orc'; const e = G.spawnEnemy(type, x, z, 0, null); if (e) { e.state = 'chase'; e.aggroT = 30; G.fx.burst(e.center(), 25, { speed: 4, life: 0.7, size: 0.3, color: [0.4, 0.9, 0.3] }); } }
      G.audio.play('roar', this.pos, { pitch: this.def.pitch || 0.5 });
    }
  }
  spike(x, z, color, delay, texName) {
    const G = this.game; const y = heightAt(x, z);
    const m = new THREE.Mesh(G.spikeGeo ||= new THREE.ConeGeometry(0.6, 3, 5).translate(0, 1.5, 0), new THREE.MeshStandardMaterial({ color, roughness: 0.7, emissive: texName === 'ice' ? 0x3080ff : 0, emissiveIntensity: texName === 'ice' ? 0.8 : 0, flatShading: true }));
    m.position.set(x, y - 3, z); m.rotation.set(rand(-0.3, 0.3), rand(0, 6), rand(-0.3, 0.3)); m.castShadow = true; G.scene.add(m);
    let t = -delay;
    G.effects.push({ update: (dt) => { t += dt; if (t < 0) return true; const up = t < 0.12 ? t / 0.12 : t < 1.2 ? 1 : 1 - (t - 1.2) / 0.5; m.position.y = y - 3 + 3 * Math.max(0, up); if (t < dt * 1.5) G.fx.dust(new THREE.Vector3(x, y, z), 4, texName === 'ice' ? [0.9, 0.95, 1] : [0.45, 0.35, 0.25]); if (t > 1.7) { G.scene.remove(m); m.material.dispose(); return false; } return true; } });
  }
}
ENEMIES.sapling = { name: 'Kök Filizi', model: 'humanoid', look: { head: 'treant', s: 1.1, bulk: 1.2, skin: 0x6a5030, skinTex: 'bark', cloth: 0x4a7a20, shirtless: true, bareFeet: true, noBelt: true, pants: 0x5a4020 }, hp: 90, dmg: 14, speed: 4.5, range: 2, aggro: 30, xp: 20, gold: [0, 2], radius: 0.6, attacks: ['punch', 'punch2'], lvl: 7, sound: 'growl', pitch: 1.2 };
