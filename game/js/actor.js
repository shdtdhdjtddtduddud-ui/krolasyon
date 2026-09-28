// Tüm canlıların ortak temeli: konum, fizik, durum etkileri, vuruş parlaması, animasyon bağlantısı
import * as THREE from 'three';
import { heightAt, WATER_Y, BORDER } from './terrain.js';
import { resolve } from './collision.js';
import { animate } from './anim.js';
import { clamp } from './util.js';

const TMP = new THREE.Color(), FLASH = new THREE.Color(1, 1, 1), FROST = new THREE.Color(0.3, 0.6, 1), POISON = new THREE.Color(0.3, 0.9, 0.2), BURN = new THREE.Color(1, 0.35, 0.05);
export class Actor {
  constructor(game, model) {
    this.game = game; this.model = model; this.root = model.root;
    this.pos = new THREE.Vector3(); this.vel = new THREE.Vector3(); this.kb = new THREE.Vector3();
    this.yaw = 0; this.radius = 0.5; this.alive = true; this.onGround = true; this.vy = 0;
    this.st = { time: Math.random() * 10, phase: 0, speed: 0, act: null, hurt: 0, dead: null };
    this.status = { burn: 0, poison: 0, freeze: 0, slow: 0, stun: 0 }; this.dot = { burn: 0, poison: 0 };
    this.flash = 0; this.invuln = 0;
    game.scene.add(this.root);
  }
  get y() { return this.pos.y; }
  groundAt() { return heightAt(this.pos.x, this.pos.z); }
  hitFlash(v = 1) { this.flash = Math.max(this.flash, v); }
  applyStatus(k, t, dps = 0) { this.status[k] = Math.max(this.status[k], t); if (dps) this.dot[k] = Math.max(this.dot[k] * (this.status[k] > 0 ? 1 : 0), dps); }
  tickStatus(dt) {
    const s = this.status;
    for (const k in s) if (s[k] > 0) s[k] = Math.max(0, s[k] - dt);
    this.dotAcc = (this.dotAcc || 0) + dt;
    if (this.dotAcc >= 0.5) {
      this.dotAcc -= 0.5;
      if (s.burn > 0 && this.dot.burn) { this.takeDot(this.dot.burn * 0.5, 'burn'); this.game.fx.burst(this.center(), 4, { speed: 2, up: 1, life: 0.5, size: 0.4, size2: 0.1, color: [1, 0.5, 0.1], color2: [0.6, 0.1, 0] }); }
      if (s.poison > 0 && this.dot.poison) { this.takeDot(this.dot.poison * 0.5, 'poison'); this.game.fx.burst(this.center(), 3, { speed: 1.2, up: 1, life: 0.7, size: 0.3, color: [0.4, 1, 0.2], normal: true, alpha: 0.7 }); }
    }
  }
  takeDot() {}
  center() { return new THREE.Vector3(this.pos.x, this.pos.y + (this.model.height || 1.6) * 0.55, this.pos.z); }
  updateFlash(dt) {
    this.flash = Math.max(0, this.flash - dt * 7);
    const s = this.status; let tint = null, ti = 0;
    if (s.freeze > 0) { tint = FROST; ti = 0.7; } else if (s.burn > 0) { tint = BURN; ti = 0.25 + Math.sin(this.st.time * 20) * 0.1; } else if (s.poison > 0) { tint = POISON; ti = 0.25; } else if (s.slow > 0) { tint = FROST; ti = 0.2; }
    const f = this.flash, active = f > 0 || !!tint;
    if (!active && !this._tinted) return;
    for (const m of this.model.mats) {
      const be = m.userData.baseEmissive, bi = m.userData.baseEI ?? 0; if (!be) continue;
      if (!active) { m.emissive.copy(be); m.emissiveIntensity = bi; continue; }
      TMP.copy(be).multiplyScalar(bi);
      if (tint) TMP.lerp(tint, ti);
      if (f > 0) TMP.lerp(FLASH, Math.min(1, f));
      m.emissive.copy(TMP); m.emissiveIntensity = 1;
    }
    this._tinted = active;
  }
  // Hareket ve çarpışma (hedef hız vx,vz)
  physics(dt, wantX, wantZ, accel = 12, opts = {}) {
    const k = 1 - Math.exp(-accel * dt);
    this.vel.x += (wantX - this.vel.x) * k; this.vel.z += (wantZ - this.vel.z) * k;
    const kbd = Math.exp(-6 * dt); this.kb.multiplyScalar(kbd);
    const ox = this.pos.x, oz = this.pos.z, oh = heightAt(ox, oz);
    this.pos.x += (this.vel.x + this.kb.x) * dt; this.pos.z += (this.vel.z + this.kb.z) * dt;
    if (!opts.flying) {
      const nh = heightAt(this.pos.x, this.pos.z); const step = Math.hypot(this.pos.x - ox, this.pos.z - oz);
      if (step > 1e-4 && (nh - oh) / step > 1.15 && nh > oh + 0.05) { this.pos.x = ox; this.pos.z = oz; this.vel.x *= 0.3; this.vel.z *= 0.3; }
    }
    if (!opts.noCollide) resolve(this.pos, this.radius, this.pos.y + 0.3);
    this.pos.x = clamp(this.pos.x, -BORDER, BORDER); this.pos.z = clamp(this.pos.z, -BORDER, BORDER);
    const g = heightAt(this.pos.x, this.pos.z);
    this.swimming = false;
    if (opts.flying) { this.onGround = false; return; }
    this.vy -= 26 * dt; this.pos.y += this.vy * dt;
    let floor = g;
    if (g < WATER_Y - 1.2 && !opts.noSwim) { floor = WATER_Y - 1.05; this.swimming = true; }
    if (this.pos.y <= floor) { if (!this.onGround && this.vy < -6) this.onLand?.(-this.vy); this.pos.y = floor; this.vy = 0; this.onGround = true; }
    else this.onGround = this.pos.y - floor < 0.05;
  }
  sync(dt) {
    this.root.position.copy(this.pos); this.root.rotation.y = this.yaw;
    this.st.time += dt; this.st.hurt = Math.max(0, this.st.hurt - dt * 4);
    animate(this.model, this.st, dt);
    this.updateFlash(dt);
  }
  dispose() {
    this.game.scene.remove(this.root);
    for (const m of this.model.mats) m.dispose();
  }
}
