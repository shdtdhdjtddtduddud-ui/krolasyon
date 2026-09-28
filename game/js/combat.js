// Hasar hesaplama, mermiler (ok, ateş topu, büyü), alan etkileri
import * as THREE from 'three';
import { heightAt } from './terrain.js';
import { rand, clamp } from './util.js';
import { tex } from './textures.js';

export function damage(game, src, tgt, base, o = {}) {
  if (!tgt || !tgt.alive || tgt.invuln > 0) return 0;
  const P = game.player;
  let crit = false, d = base * rand(0.9, 1.1);
  if (src === P) {
    crit = o.canCrit !== false && Math.random() < P.stats.crit; if (crit) d *= P.stats.critDmg;
    if (P.buff > 0) d *= 1.4;
  }
  if (tgt === P) { d *= 100 / (100 + P.stats.armor * 2.5); if (P.blocking) d *= 0.4; }
  else if (tgt.def?.boss && tgt.phase2) d *= 0.9;
  d = Math.max(1, Math.round(d));
  tgt.hp -= d; tgt.hitFlash(1); tgt.st.hurt = o.noFlinch ? tgt.st.hurt : 1;
  const c = tgt.center();
  game.fx.number(c, d, tgt === P ? 'taken' : crit ? 'crit' : o.type === 'poison' ? 'poison' : o.type === 'burn' ? 'burn' : 'dmg');
  if (o.kb && !tgt.def?.boss) { const dir = o.dir || tgt.pos.clone().sub(src?.pos || tgt.pos).setY(0).normalize(); tgt.kb.addScaledVector(dir, o.kb * (tgt.def?.heavy ? 0.35 : 1)); if (o.lift && tgt.onGround) tgt.vy = o.lift; }
  if (o.status) for (const k in o.status) { const v = o.status[k]; tgt.applyStatus(k, v[0], v[1] || 0); }
  if (!o.silent) {
    if (o.melee) { game.fx.sparks(c.clone().lerp(src.center(), 0.3), crit ? [1, 0.9, 0.3] : [1, 0.7, 0.3], crit ? 22 : 12); if (tgt.def?.model === 'humanoid' && tgt.def.look?.head !== 'skull' && tgt.def.look?.head !== 'golem') game.fx.blood(c); }
    game.audio.play(o.heavy ? 'hitHeavy' : 'hit', c, { pitch: tgt.def?.boss ? 0.7 : 1 });
    if (crit) game.audio.play('crit', c);
  }
  if (src === P && o.melee) { game.hitstop(crit ? 0.09 : 0.055); game.fx.addShake(crit ? 0.22 : 0.12); }
  if (tgt === P) { game.fx.addShake(clamp(d / P.stats.maxHp * 3, 0.15, 0.6)); game.ui.hurtFlash(); game.audio.play('hurt', null); }
  tgt.onDamaged?.(src, d);
  if (tgt.hp <= 0) tgt.die(src);
  return d;
}

// Hedef tarama yardımcıları
export function enemiesInArc(game, pos, dir, range, arcDeg, extra = 0) {
  const out = []; const cosA = Math.cos((arcDeg / 2) * Math.PI / 180);
  for (const e of game.enemies) {
    if (!e.alive) continue; const dx = e.pos.x - pos.x, dz = e.pos.z - pos.z; const d = Math.hypot(dx, dz);
    if (d > range + e.radius + extra) continue; if (Math.abs(e.pos.y - pos.y) > 4 + e.radius) continue;
    if (arcDeg < 360 && d > e.radius) { const c = (dx * dir.x + dz * dir.z) / d; if (c < cosA) continue; }
    out.push(e);
  }
  return out;
}
export function enemiesInRadius(game, pos, r) { return enemiesInArc(game, pos, null, r, 360); }

// ---------------- MERMİLER ----------------
const geoCache = {};
function glowSprite(color, size) {
  const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex('glow'), color, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true }));
  s.scale.setScalar(size); return s;
}
export class Projectile {
  // o: {type, pos, dir, speed, team, dmg, src, radius, life, grav, pierce, homing, color, onHit, aoe, status}
  constructor(game, o) {
    this.game = game; Object.assign(this, { radius: 0.4, life: 3, grav: 0, pierce: 0, hit: new Set() }, o);
    this.pos = o.pos.clone(); this.vel = o.dir.clone().normalize().multiplyScalar(o.speed);
    const g = new THREE.Group(); this.obj = g;
    const col = new THREE.Color(o.color ?? 0xffffff);
    if (o.type === 'arrow') {
      const shaft = new THREE.Mesh(geoCache.shaft ||= new THREE.CylinderGeometry(0.02, 0.02, 0.9, 4).rotateX(Math.PI / 2), new THREE.MeshStandardMaterial({ color: 0x8a6a4a }));
      const head = new THREE.Mesh(geoCache.head ||= new THREE.ConeGeometry(0.05, 0.16, 4).rotateX(Math.PI / 2).translate(0, 0, 0.5), new THREE.MeshStandardMaterial({ color: o.color ?? 0xdddddd, emissive: o.color ?? 0, emissiveIntensity: o.color ? 2 : 0 }));
      g.add(shaft, head); if (o.color) g.add(glowSprite(col, 0.6));
    } else if (o.type === 'fireball' || o.type === 'meteor') {
      const sz = o.type === 'meteor' ? 1.2 : 0.35;
      g.add(new THREE.Mesh(new THREE.SphereGeometry(sz, 12, 10), new THREE.MeshBasicMaterial({ color: new THREE.Color(4, 1.6, 0.4) })));
      g.add(glowSprite(new THREE.Color(1, 0.5, 0.15), sz * 6));
    } else if (o.type === 'web') {
      g.add(new THREE.Mesh(new THREE.IcosahedronGeometry(0.35, 0), new THREE.MeshStandardMaterial({ color: 0xeeeeee, transparent: true, opacity: 0.8 })));
    } else if (o.type === 'ice') {
      g.add(new THREE.Mesh(new THREE.OctahedronGeometry(0.3, 0).scale(0.6, 0.6, 2), new THREE.MeshStandardMaterial({ color: 0xbfe6ff, emissive: 0x3aa0ff, emissiveIntensity: 1.5 })));
      g.add(glowSprite(new THREE.Color(0.4, 0.8, 1), 1.5));
    } else {
      g.add(new THREE.Mesh(new THREE.SphereGeometry(0.16, 10, 8), new THREE.MeshBasicMaterial({ color: col.clone().multiplyScalar(3) })));
      g.add(glowSprite(col, 1.6));
    }
    g.position.copy(this.pos); game.scene.add(g);
    this.alive = true; this.t = 0;
  }
  update(dt) {
    const G = this.game; this.t += dt; this.life -= dt;
    if (this.homing && this.homing.alive) {
      const to = this.homing.center().sub(this.pos).normalize().multiplyScalar(this.speed);
      this.vel.lerp(to, 1 - Math.exp(-this.homingStr * dt));
    }
    this.vel.y -= this.grav * dt;
    this.pos.addScaledVector(this.vel, dt);
    this.obj.position.copy(this.pos);
    if (this.type === 'arrow' || this.type === 'ice') this.obj.lookAt(this.pos.clone().add(this.vel));
    // iz partikülleri
    const fx = G.fx;
    if (this.type === 'fireball' || this.type === 'meteor') { const s = this.type === 'meteor' ? 3 : 1; fx.burst(this.pos, 3 * s, { speed: 1, life: 0.45, size: 0.5 * s, size2: 0.1, color: [1, 0.6, 0.2], color2: [0.6, 0.1, 0], spread: 0.15 * s }); if (Math.random() < 0.3) fx.burst(this.pos, 1, { speed: 0.5, life: 1, size: 0.6 * s, size2: 1.5 * s, color: [0.15, 0.13, 0.12], normal: true, alpha: 0.4 }); }
    else if (this.type === 'bolt') fx.burst(this.pos, 2, { speed: 0.6, life: 0.35, size: 0.28, size2: 0.02, color: this.pcolor || [0.6, 0.5, 1], spread: 0.05 });
    else if (this.type === 'arrow' && this.color) fx.burst(this.pos, 1, { speed: 0.3, life: 0.3, size: 0.18, size2: 0.02, color: this.pcolor || [0.4, 1, 0.3] });
    else if (this.type === 'ice') fx.burst(this.pos, 1, { speed: 0.5, life: 0.4, size: 0.15, color: [0.6, 0.9, 1] });
    // çarpışma
    const targets = this.team === 'player' ? G.enemies : [G.player];
    for (const e of targets) {
      if (!e.alive || this.hit.has(e) || e.invuln > 0) continue;
      const c = e.center(); const r = this.radius + e.radius + (e.model.height > 3 ? e.model.height * 0.25 : 0);
      if (c.distanceToSquared(this.pos) < r * r || (Math.hypot(e.pos.x - this.pos.x, e.pos.z - this.pos.z) < this.radius + e.radius && this.pos.y > e.pos.y - 0.2 && this.pos.y < e.pos.y + e.model.height)) {
        this.hit.add(e); this.onHit?.(this, e);
        if (this.pierce-- <= 0) { this.explode(e); return; }
      }
    }
    const gh = heightAt(this.pos.x, this.pos.z);
    if (this.pos.y < gh || this.life <= 0) { if (this.pos.y < gh) this.pos.y = gh + 0.1; this.explode(null); }
  }
  explode(target) {
    if (!this.alive) return; this.alive = false; this.onExplode?.(this, target);
    const fx = this.game.fx;
    if (this.type === 'arrow') { fx.burst(this.pos, 5, { speed: 3, life: 0.3, size: 0.08, color: [1, 0.9, 0.7], grav: 8 }); this.game.audio.play('arrowHit', this.pos); }
    else if (this.type === 'bolt') { fx.burst(this.pos, 12, { speed: 4, life: 0.4, size: 0.2, size2: 0.02, color: this.pcolor || [0.6, 0.5, 1] }); fx.light(this.pos, this.pcolor || [0.6, 0.5, 1], 8, 0.2); }
    else if (this.type === 'web') { fx.burst(this.pos, 10, { speed: 3, life: 0.5, size: 0.15, color: [0.95, 0.95, 0.95], normal: true, grav: 6 }); }
    else if (this.type === 'ice') fx.burst(this.pos, 12, { speed: 4, life: 0.5, size: 0.15, color: [0.7, 0.95, 1], grav: 8 });
    this.game.scene.remove(this.obj);
    this.obj.traverse((m) => { if (m.material) m.material.dispose(); if (m.geometry && !Object.values(geoCache).includes(m.geometry)) m.geometry.dispose(); });
  }
}
// Alan hasarı
export function aoe(game, src, pos, radius, dmg, o = {}) {
  const team = src === game.player ? 'player' : 'enemy'; let n = 0;
  if (team === 'player') { for (const e of enemiesInRadius(game, pos, radius)) { damage(game, src, e, dmg, { dir: e.pos.clone().sub(pos).setY(0).normalize(), ...o }); n++; } }
  else { const P = game.player; if (P.alive && Math.hypot(P.pos.x - pos.x, P.pos.z - pos.z) < radius + P.radius && Math.abs(P.pos.y - pos.y) < 3.5 + (o.tall || 0)) { damage(game, src, P, dmg, { dir: P.pos.clone().sub(pos).setY(0).normalize(), ...o }); n++; } }
  return n;
}
export function inCone(px, pz, ox, oz, dirYaw, range, arc) {
  const dx = px - ox, dz = pz - oz, d = Math.hypot(dx, dz); if (d > range) return false; if (d < 0.5) return true;
  const fx = Math.sin(dirYaw), fz = Math.cos(dirYaw); return (dx * fx + dz * fz) / d > Math.cos(arc / 2);
}
export function inLine(px, pz, ox, oz, dirYaw, len, halfW) {
  const fx = Math.sin(dirYaw), fz = Math.cos(dirYaw); const dx = px - ox, dz = pz - oz;
  const along = dx * fx + dz * fz, side = Math.abs(-dx * fz + dz * fx); return along > -1 && along < len && side < halfW;
}
