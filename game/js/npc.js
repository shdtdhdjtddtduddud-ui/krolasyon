// Köylüler ve görev veren karakterler
import * as THREE from 'three';
import { buildHumanoid, makeWeapon } from './models.js';
import { animate } from './anim.js';
import { heightAt } from './terrain.js';
import { dampAngle, rand } from './util.js';
import { NPCS, VILLAGERS } from './data.js';

export class NPC {
  constructor(game, def) {
    this.game = game; this.def = def; this.id = def.id; this.name = def.name;
    this.model = buildHumanoid(def.look); this.root = this.model.root;
    if (def.weapon) { const w = makeWeapon(def.weapon, def.weaponColor ?? 0xa0a4a8, def.weaponColor ? 1 : 0); w.rotation.set(Math.PI / 2, 0, 0); if (def.weapon === 'staff' || def.weapon === 'spear') w.position.set(0, 0, -0.5); this.model.p.rSocket.add(w); }
    this.pos = new THREE.Vector3(def.x, heightAt(def.x, def.z), def.z); this.home = this.pos.clone();
    this.yaw = Math.atan2(-def.x, -def.z); this.baseYaw = this.yaw;
    this.st = { time: rand(0, 10), speed: 0, phase: 0, act: null, holdPose: def.weapon === 'staff' ? 'staff' : null };
    this.actT = rand(0, 5); this.wander = def.wander;
    game.scene.add(this.root); this.sync(0);
    this.marker = document.createElement('div'); this.marker.className = 'npcMark'; document.getElementById('dmgLayer').appendChild(this.marker);
  }
  sync(dt) { this.root.position.copy(this.pos); this.root.rotation.y = this.yaw; this.st.time += dt; animate(this.model, this.st, dt); }
  update(dt) {
    const P = this.game.player; const d = P.pos.distanceTo(this.pos);
    if (d > 90) { this.marker.style.display = 'none'; return; }
    let tx = 0, tz = 0;
    if (d < 6) { this.yaw = dampAngle(this.yaw, Math.atan2(P.pos.x - this.pos.x, P.pos.z - this.pos.z), 5, dt); const dy = (P.pos.y + 1.6) - (this.pos.y + 1.6); this.st.lookX = -dy * 0.1; }
    else if (this.wander) {
      this.wt = (this.wt ?? 0) - dt;
      if (this.wt <= 0) { this.wt = rand(4, 10); this.to = Math.random() < 0.7 ? new THREE.Vector3(this.home.x + rand(-14, 14), 0, this.home.z + rand(-14, 14)) : null; }
      if (this.to) { const dx = this.to.x - this.pos.x, dz = this.to.z - this.pos.z, l = Math.hypot(dx, dz); if (l > 0.6) { tx = dx / l * 1.4; tz = dz / l * 1.4; this.yaw = dampAngle(this.yaw, Math.atan2(dx, dz), 4, dt); } else this.to = null; }
    } else this.yaw = dampAngle(this.yaw, this.baseYaw, 2, dt);
    this.pos.x += tx * dt; this.pos.z += tz * dt; this.pos.y = heightAt(this.pos.x, this.pos.z);
    this.st.speed = Math.hypot(tx, tz);
    // boşta animasyonları
    this.actT -= dt;
    if (this.def.idle === 'hammer' && d > 6) { this.st.act = { name: 'hammer', t: (this.st.time * 0.9) % 1 }; if (this.st.act.t > 0.68 && this.st.act.t < 0.7 && d < 30) { this.game.audio.play('metal', this.pos, { pitch: 1.3, min: 0.5 }); const hp = new THREE.Vector3(); this.model.p.rHand.getWorldPosition(hp); this.game.fx.sparks(hp, [1, 0.7, 0.3], 6); } }
    else if (this.talking) { this.st.act = { name: 'talk', t: (this.st.time * 0.5) % 1 }; }
    else if (this.actT < 0 && this.def.idle) { this.st.act = { name: this.def.idle, t: 0 }; this.actT = rand(5, 10); this.actDur = 2.5; }
    if (this.st.act && !this.talking && this.def.idle !== 'hammer') { this.st.act.t += dt / (this.actDur || 2); if (this.st.act.t >= 1) this.st.act = null; }
    if (this.def.idle === 'hammer' && d <= 6) this.st.act = null;
    this.sync(dt);
    // görev işareti (! / ?)
    const mk = this.game.questMarker(this.id);
    if (mk && d < 60) {
      const v = this.pos.clone(); v.y += 2.5 + Math.sin(this.st.time * 3) * 0.1; v.project(this.game.camera);
      if (v.z < 1) { this.marker.style.display = 'block'; this.marker.textContent = mk; this.marker.className = 'npcMark ' + (mk === '?' ? 'turnin' : ''); this.marker.style.transform = `translate(${(v.x * 0.5 + 0.5) * innerWidth}px, ${(-v.y * 0.5 + 0.5) * innerHeight}px) translate(-50%,-100%)`; }
      else this.marker.style.display = 'none';
    } else this.marker.style.display = 'none';
  }
}
export function spawnNPCs(game) {
  const list = NPCS.map((d) => new NPC(game, d));
  VILLAGERS.forEach((v, i) => { const a = i * 1.7 + 0.5; list.push(new NPC(game, { id: 'v' + i, name: ['Çiftçi Ali', 'Ayşe', 'Zeynep', 'Küçük Mert'][i], x: Math.cos(a) * 14, z: Math.sin(a) * 14, look: v.look, wander: true, idle: 'wave' })); });
  return list;
}
