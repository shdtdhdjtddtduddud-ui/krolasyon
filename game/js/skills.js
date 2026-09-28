// Oyuncu yetenekleri (her sınıf için 4)
import * as THREE from 'three';
import { damage, aoe, enemiesInRadius, enemiesInArc, Projectile, inLine } from './combat.js';
import { heightAt } from './terrain.js';
import { rand } from './util.js';

function targetPoint(P, G, range) {
  const t = P.findTarget(range, 50);
  if (t) return { x: t.pos.x, z: t.pos.z, t };
  const d = P.aimDir(); const dist = Math.min(range, 12);
  return { x: P.pos.x + d.x * dist, z: P.pos.z + d.z * dist, t: null };
}
function face(P, x, z) { P.yaw = Math.atan2(x - P.pos.x, z - P.pos.z); }

export function castSkill(P, G, id) {
  const fx = G.fx, au = G.audio, A = P.stats.atk;
  switch (id) {
    // ================= SAVAŞÇI =================
    case 'whirl': {
      let spin = 0;
      P.spinTrail = 1.0;
      P.startAction({ anim: 'spin', dur: 1.0, move: 0.75, turn: true, trail: [0, 1],
        update: (dt, a) => { spin += dt * 20; P.st.spin = spin; if (Math.random() < 0.5) fx.burst(P.center(), 2, { speed: 4, life: 0.3, size: 0.2, color: [1, 0.5, 0.4] }); },
        end: () => { P.st.spin = null; P.model.p.body.rotation.y = 0; },
        events: [0.15, 0.48, 0.81].map((t) => [t, () => { au.play('swing', P.pos, { pitch: 1.3 }); for (const e of enemiesInRadius(G, P.pos, 3.8)) damage(G, P, e, A * 0.75, { melee: true, kb: 5 }); }]) });
      break;
    }
    case 'slam': {
      const tp = targetPoint(P, G, 9); face(P, tp.x, tp.z);
      const dist = Math.min(8, Math.hypot(tp.x - P.pos.x, tp.z - P.pos.z));
      P.vy = 10; P.onGround = false;
      const v = [Math.sin(P.yaw) * dist / 0.55, Math.cos(P.yaw) * dist / 0.55];
      P.startAction({ anim: 'slam', dur: 0.85, move: 0, vel: v, trail: [0.4, 0.6],
        update: (dt, a) => { if (a.t > 0.55) a.vel = [0, 0]; },
        events: [[0.55, () => {
          const c = P.pos.clone().add(new THREE.Vector3(Math.sin(P.yaw) * 1.5, 0, Math.cos(P.yaw) * 1.5));
          aoe(G, P, c, 6.5, A * 2.3, { kb: 10, lift: 7, heavy: true, status: { stun: [1.6] } });
          fx.ring(c, 7, [1, 0.6, 0.3], 0.5); fx.ring(c, 4, [1, 0.9, 0.6], 0.35); fx.dust(c, 30); fx.burst(c, 40, { speed: 10, up: 2, life: 0.8, size: 0.25, color: [0.55, 0.45, 0.35], grav: 20, normal: true, spread: 2 });
          fx.burst(c, 30, { speed: 8, up: 1, life: 0.4, size: 0.2, color: [1, 0.7, 0.3] }); fx.light(c, [1, 0.6, 0.3], 40, 0.4);
          fx.addShake(0.7); G.hitstop(0.12); au.play('shock', c); au.play('hitHeavy', c);
        }]] });
      au.play('jump', P.pos);
      break;
    }
    case 'charge': {
      const tp = targetPoint(P, G, 14); face(P, tp.x, tp.z); const hit = new Set(); const yaw = P.yaw;
      P.invuln = 0.5;
      P.startAction({ anim: 'thrust', dur: 0.5, move: 0, vel: [Math.sin(yaw) * 26, Math.cos(yaw) * 26], trail: [0, 0.5],
        update: (dt, a) => { if (a.t > 0.42) a.vel = [0, 0]; fx.dust(P.pos, 2); for (const e of enemiesInRadius(G, P.pos, 2.6)) if (!hit.has(e)) { hit.add(e); const side = Math.sign((e.pos.x - P.pos.x) * Math.cos(yaw) - (e.pos.z - P.pos.z) * Math.sin(yaw)) || 1; damage(G, P, e, A * 1.4, { melee: true, kb: 12, lift: 5, dir: new THREE.Vector3(Math.sin(yaw) + Math.cos(yaw) * side, 0, Math.cos(yaw) - Math.sin(yaw) * side).normalize(), status: { stun: [0.8] } }); } } });
      au.play('heavySwing', P.pos); fx.burst(P.center(), 20, { speed: 3, life: 0.4, size: 0.3, color: [1, 0.6, 0.3] });
      break;
    }
    case 'warcry': {
      P.buff = 10;
      P.startAction({ anim: 'roar', dur: 0.9, move: 0,
        events: [[0.3, () => { au.play('buff', P.pos); fx.ring(P.pos, 8, [1, 0.4, 0.1], 0.7); fx.burst(P.center(), 60, { speed: 7, life: 0.8, size: 0.3, color: [1, 0.5, 0.15], spread: 0.3 }); fx.light(P.center(), [1, 0.5, 0.2], 40, 0.8); fx.addShake(0.3); for (const e of enemiesInRadius(G, P.pos, 8)) { e.applyStatus('stun', 1); e.kb.add(e.pos.clone().sub(P.pos).setY(0).normalize().multiplyScalar(8)); } }]] });
      break;
    }
    // ================= BÜYÜCÜ =================
    case 'fireball': {
      const t = P.findTarget(30); if (t) P.faceTo(t.pos, true);
      P.startAction({ anim: 'cast', dur: 0.55, move: 0.3, events: [[0.28, () => {
        const from = P.weaponTip(); const dir = P.shotDir(t, from);
        au.play('fireball', from);
        G.projectiles.push(new Projectile(G, { type: 'fireball', pos: from, dir, speed: 26, team: 'player', src: P, radius: 0.6, life: 2, homing: t, homingStr: 2.5,
          onExplode: (pr) => { fx.explosion(pr.pos, 3.5); au.play('explode', pr.pos); aoe(G, P, pr.pos, 4.2, A * 2.1, { kb: 8, lift: 4, status: { burn: [4, A * 0.25] } }); } }));
      }]] });
      break;
    }
    case 'nova': {
      P.startAction({ anim: 'castUp', dur: 0.8, move: 0, events: [[0.45, () => {
        au.play('ice', P.pos); fx.ring(P.pos, 8, [0.5, 0.85, 1], 0.6); fx.ring(P.pos, 5, [0.8, 0.95, 1], 0.4);
        for (let i = 0; i < 60; i++) { const a = (i / 60) * Math.PI * 2; fx.add.add({ x: P.pos.x, y: P.pos.y + 0.4, z: P.pos.z, vx: Math.cos(a) * 14, vy: rand(0, 2), vz: Math.sin(a) * 14, life: 0.55, size: 0.35, size2: 0.05, r: 0.6, g: 0.9, b: 1, drag: 2 }); }
        fx.light(P.center(), [0.4, 0.8, 1], 40, 0.6); fx.addShake(0.2);
        for (const e of enemiesInRadius(G, P.pos, 7.5)) { damage(G, P, e, A * 1.3, { kb: 3, status: { freeze: [e.def.boss ? 0.8 : 2.8], slow: [5] } }); spawnIce(G, e.pos); }
      }]] });
      break;
    }
    case 'chain': {
      const t = P.findTarget(22, 60);
      P.startAction({ anim: 'cast', dur: 0.5, move: 0.2, events: [[0.25, () => {
        let from = P.weaponTip(); let cur = t; const hit = new Set();
        au.play('zap', from);
        if (!cur) { const d = P.aimDir(); lightning(G, from, from.clone().add(d.multiplyScalar(14)).setY(from.y)); return; }
        for (let i = 0; i < 5 && cur; i++) {
          const to = cur.center(); lightning(G, from, to); hit.add(cur);
          damage(G, P, cur, A * 1.6 * (1 - i * 0.1), { kb: 2, status: { stun: [0.35] } }); fx.sparks(to, [0.6, 0.8, 1], 10);
          from = to; let best = null, bd = 12;
          for (const e of G.enemies) { if (!e.alive || hit.has(e)) continue; const d = e.center().distanceTo(from); if (d < bd) { bd = d; best = e; } }
          cur = best;
        }
      }]] });
      break;
    }
    case 'meteor': {
      const tp = targetPoint(P, G, 26); face(P, tp.x, tp.z);
      P.startAction({ anim: 'castUp', dur: 0.9, move: 0, events: [[0.45, () => {
        au.play('fireball', P.pos, { pitch: 0.6 }); fx.telegraph(tp.x, tp.z, 7, 1.8, 0xff8030);
        for (let i = 0; i < 7; i++) {
          const x = tp.x + (i ? rand(-5, 5) : 0), z = tp.z + (i ? rand(-5, 5) : 0), y = heightAt(x, z);
          const from = new THREE.Vector3(x - 12 + rand(-3, 3), y + 30, z - 8); const dir = new THREE.Vector3(x, y, z).sub(from).normalize();
          G.later(i * 0.28, () => G.projectiles.push(new Projectile(G, { type: 'meteor', pos: from, dir, speed: 38, team: 'player', src: P, radius: 1.2, life: 3,
            onExplode: (pr) => { fx.explosion(pr.pos, 3.8); au.play('explode', pr.pos); aoe(G, P, pr.pos, 3.8, A * 1.9, { kb: 9, lift: 5, status: { burn: [3, A * 0.2] } }); } })));
        }
      }]] });
      break;
    }
    // ================= OKÇU =================
    case 'multishot': {
      const t = P.findTarget(26); if (t) P.faceTo(t.pos, true);
      P.startAction({ anim: 'bow', dur: 0.55, move: 0.2, draw: true, events: [[0.3, () => {
        const base = P.shotDir(t, P.handPos('l')); const by = Math.atan2(base.x, base.z);
        for (let i = -3; i <= 3; i++) { const y = by + i * 0.13; P.fireArrow(null, { dir: new THREE.Vector3(Math.sin(y), base.y, Math.cos(y)), mult: 0.85, silent: i !== 0, color: 0xffd060, pcolor: [1, 0.8, 0.3] }); }
        au.play('bow', P.pos, { pitch: 0.8 });
      }]] });
      break;
    }
    case 'poison': {
      const t = P.findTarget(28); if (t) P.faceTo(t.pos, true);
      P.startAction({ anim: 'bow', dur: 0.65, move: 0.2, draw: true, events: [[0.36, () => {
        P.fireArrow(t, { pierce: 4, color: 0x60ff30, pcolor: [0.4, 1, 0.2], speed: 55, mult: 1.6,
          onHit: (pr, e) => { damage(G, P, e, A * 1.6, { kb: 2, status: { poison: [6, A * 0.45] } }); poisonCloud(G, P, e.pos.clone()); } });
      }]] });
      break;
    }
    case 'leap': {
      const back = [-Math.sin(P.yaw), -Math.cos(P.yaw)]; const trap = P.pos.clone();
      P.vy = 8.5; P.onGround = false; P.invuln = 0.6;
      P.startAction({ anim: 'roar', dur: 0.6, move: 0, vel: [back[0] * 14, back[1] * 14], update: (dt, a) => { P.st.roll = Math.min(1, a.t / 0.6); P.st.rollDir = -1; }, end: () => { P.st.roll = null; } });
      au.play('roll', P.pos);
      const m = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.6, 0.2, 8), new THREE.MeshStandardMaterial({ color: 0x5a4030, emissive: 0xff6010, emissiveIntensity: 1.5 }));
      m.position.set(trap.x, heightAt(trap.x, trap.z) + 0.1, trap.z); G.scene.add(m);
      G.later(0.9, () => { G.scene.remove(m); m.material.dispose(); m.geometry.dispose(); fx.explosion(trap.clone().setY(trap.y + 0.5), 3.5); au.play('explode', trap); aoe(G, P, trap, 4.5, A * 2, { kb: 10, lift: 6, status: { stun: [1] } }); });
      break;
    }
    case 'rain': {
      const tp = targetPoint(P, G, 28); face(P, tp.x, tp.z);
      P.startAction({ anim: 'castUp', dur: 0.7, move: 0, events: [[0.4, () => {
        au.play('bow', P.pos, { pitch: 0.7 }); const dec = fx.telegraph(tp.x, tp.z, 7, 4, 0x80ff60);
        for (let i = 0; i < 16; i++) G.later(0.3 + i * 0.25, () => {
          for (let k = 0; k < 4; k++) { const x = tp.x + rand(-6, 6), z = tp.z + rand(-6, 6), y = heightAt(x, z); const from = new THREE.Vector3(x - 3, y + 22, z - 2); const dir = new THREE.Vector3(x, y, z).sub(from).normalize(); G.projectiles.push(new Projectile(G, { type: 'arrow', pos: from, dir, speed: 45, team: 'player', src: P, radius: 0.5, life: 1.5, color: 0xa0ff80, pcolor: [0.6, 1, 0.4], onHit: (pr, e) => damage(G, P, e, A * 0.55, { status: { slow: [2] }, silent: true }) })); }
          if (i % 3 === 0) au.play('bow', new THREE.Vector3(tp.x, heightAt(tp.x, tp.z), tp.z), { pitch: 1.3 });
        });
      }]] });
      break;
    }
  }
}
function lightning(G, a, b) {
  const pts = []; const n = 10;
  for (let i = 0; i <= n; i++) { const p = a.clone().lerp(b, i / n); if (i > 0 && i < n) p.add(new THREE.Vector3(rand(-0.6, 0.6), rand(-0.6, 0.6), rand(-0.6, 0.6))); pts.push(p); }
  const g = new THREE.BufferGeometry().setFromPoints(pts);
  const l = new THREE.Line(g, new THREE.LineBasicMaterial({ color: new THREE.Color(2, 2.6, 4), transparent: true, blending: THREE.AdditiveBlending }));
  G.scene.add(l); pts.forEach((p) => G.fx.burst(p, 2, { speed: 1, life: 0.3, size: 0.4, size2: 0.05, color: [0.6, 0.8, 1] })); G.fx.light(b, [0.5, 0.7, 1], 30, 0.25);
  let t = 0; G.effects.push({ update: (dt) => { t += dt; l.material.opacity = 1 - t / 0.3; if (t > 0.3) { G.scene.remove(l); g.dispose(); l.material.dispose(); return false; } return true; } });
}
function spawnIce(G, pos) {
  const m = new THREE.Mesh(new THREE.IcosahedronGeometry(1.1, 0), new THREE.MeshStandardMaterial({ color: 0xbfe6ff, emissive: 0x3aa0ff, emissiveIntensity: 0.6, transparent: true, opacity: 0.6, roughness: 0.1, flatShading: true }));
  m.position.set(pos.x, pos.y + 0.8, pos.z); m.scale.set(1, 1.4, 1); G.scene.add(m); let t = 0;
  G.effects.push({ update: (dt) => { t += dt; m.material.opacity = 0.6 * (1 - Math.max(0, t - 1.8) / 0.6); if (t > 2.4) { G.scene.remove(m); m.geometry.dispose(); m.material.dispose(); G.fx.burst(m.position, 12, { speed: 4, life: 0.5, size: 0.15, color: [0.7, 0.95, 1], grav: 10 }); return false; } return true; } });
}
function poisonCloud(G, P, pos) {
  let t = 0, tick = 0;
  G.effects.push({ update: (dt) => { t += dt; tick -= dt; if (Math.random() < 0.6) G.fx.burst(pos.clone().setY(pos.y + 0.6), 2, { speed: 1, life: 1.2, size: 1, size2: 2.4, color: [0.3, 0.8, 0.2], normal: true, alpha: 0.35, spread: 1.5 }); if (tick <= 0) { tick = 0.5; for (const e of enemiesInRadius(G, pos, 3.5)) e.applyStatus('poison', 3, P.stats.atk * 0.3); } return t < 4; } });
}
