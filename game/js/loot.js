// Yere düşen ganimetler: altın, eşya, malzeme
import * as THREE from 'three';
import { heightAt } from './terrain.js';
import { RARITY, MATERIALS } from './data.js';
import { tex } from './textures.js';
import { rand } from './util.js';

const beamGeo = new THREE.CylinderGeometry(0.08, 0.25, 4, 8, 1, true).translate(0, 2, 0);
const orbGeo = new THREE.OctahedronGeometry(0.22, 0);
const coinGeo = new THREE.CylinderGeometry(0.14, 0.14, 0.04, 12).rotateX(Math.PI / 2);
export class Loot {
  constructor(game, pos, kind, data) {
    this.game = game; this.kind = kind; this.data = data; this.t = 0;
    this.pos = pos.clone(); this.vel = new THREE.Vector3(rand(-3, 3), rand(5, 8), rand(-3, 3));
    const g = new THREE.Group(); this.obj = g;
    let color = 0xffd040;
    if (kind === 'item') color = RARITY[data.rarity].hex; if (kind === 'mat') color = 0x80ffb0; if (kind === 'potion') color = data === 'hp' ? 0xff3040 : 0x3070ff;
    if (kind === 'gold') { const m = new THREE.Mesh(coinGeo, new THREE.MeshStandardMaterial({ color: 0xffc830, metalness: 1, roughness: 0.25, emissive: 0x805000, emissiveIntensity: 0.6 })); g.add(m); this.spin = m; }
    else { const m = new THREE.Mesh(orbGeo, new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 2.5, roughness: 0.2 })); g.add(m); this.spin = m; }
    if (kind === 'item' || kind === 'mat') { const b = new THREE.Mesh(beamGeo, new THREE.MeshBasicMaterial({ color: new THREE.Color(color).multiplyScalar(1.5), transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending, depthWrite: false })); g.add(b); this.beam = b; }
    const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex('glow'), color, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.8 })); s.scale.setScalar(kind === 'gold' ? 0.6 : 1.2); g.add(s);
    g.position.copy(this.pos); game.scene.add(g); this.alive = true;
  }
  update(dt) {
    const G = this.game, P = G.player; this.t += dt;
    const gy = heightAt(this.pos.x, this.pos.z) + 0.35;
    if (!this.landed) { this.vel.y -= 20 * dt; this.pos.addScaledVector(this.vel, dt); if (this.pos.y < gy) { this.pos.y = gy; this.vel.multiplyScalar(0.3); this.vel.y = Math.abs(this.vel.y) * 0.3; if (this.vel.length() < 0.8) this.landed = true; } }
    else this.pos.y = gy + Math.sin(this.t * 3) * 0.08;
    const d = this.pos.distanceTo(P.center());
    if (P.alive && this.t > 0.5 && d < 5 && this.t >= 0) { const dir = P.center().sub(this.pos).normalize(); this.pos.addScaledVector(dir, dt * (6 + (5 - d) * 4)); this.landed = true; }
    if (P.alive && this.t > 0.5 && d < 1.2) { if (G.pickup(this)) { this.remove(); return false; } this.t = -2.5; }
    this.obj.position.copy(this.pos); this.spin.rotation.y += dt * 3;
    if (this.t > 120) { this.remove(); return false; }
    return true;
  }
  remove() { this.alive = false; this.game.scene.remove(this.obj); this.obj.traverse((m) => m.material?.dispose()); }
}
