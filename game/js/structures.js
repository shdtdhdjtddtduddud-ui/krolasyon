// Yapılar: köy, orman harabeleri, piramit, buz kalesi, ejderha ini, ork kampı, ışınlanma taşları
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { M, addWind } from './mats.js';
import { prep, T, jitter, blob } from './props.js';
import { heightAt, LOC, WAYSTONES } from './terrain.js';
import { addBox, addCircle } from './collision.js';
import { mulberry32 } from './util.js';

class SB {
  constructor(scene, q) { this.parts = {}; this.scene = scene; this.q = q; }
  add(geo, mat, m) { (this.parts[mat] ||= []).push(prep(geo, m)); }
  box(w, h, d, mat, m) { this.add(new THREE.BoxGeometry(w, h, d), mat, m); }
  build(noShadow = []) {
    const out = [];
    for (const mat in this.parts) {
      const g = mergeGeometries(this.parts[mat]); g.computeBoundingSphere();
      const mesh = new THREE.Mesh(g, M(mat)); mesh.castShadow = this.q.shadows && !noShadow.includes(mat); mesh.receiveShadow = true;
      this.scene.add(mesh); out.push(mesh);
    }
    return out;
  }
}
// yerel dönüşüm zinciri
const W = (x, z, ry, lx = 0, ly = 0, lz = 0, rx = 0, rry = 0, rz = 0, sx = 1, sy = sx, sz = sx) => {
  const base = new THREE.Matrix4().makeTranslation(x, heightAt(x, z), z).multiply(new THREE.Matrix4().makeRotationY(ry));
  return base.multiply(T(lx, ly, lz, rx, rry, rz, sx, sy, sz));
};

function house(sb, x, z, ry, w, d, h, roofMat = 'roof') {
  const y0 = heightAt(x, z);
  const L = (lx, ly, lz, rx = 0, rry = 0, rz = 0, sx = 1, sy = sx, sz = sx) => new THREE.Matrix4().makeTranslation(x, y0, z).multiply(new THREE.Matrix4().makeRotationY(ry)).multiply(T(lx, ly, lz, rx, rry, rz, sx, sy, sz));
  sb.box(w + 0.4, 1.2, d + 0.4, 'stone', L(0, -0.1, 0));
  sb.box(w, h, d, 'plaster', L(0, 0.5 + h / 2, 0));
  const top = 0.5 + h;
  for (const sx of [-1, 1]) for (const sz of [-1, 1]) sb.box(0.28, h, 0.28, 'darkWood', L(sx * w / 2, 0.5 + h / 2, sz * d / 2));
  for (const sz of [-1, 1]) sb.box(w + 0.1, 0.22, 0.22, 'darkWood', L(0, 0.5 + h * 0.55, sz * (d / 2 + 0.02)));
  for (const sx of [-1, 1]) sb.box(0.22, 0.22, d + 0.1, 'darkWood', L(sx * (w / 2 + 0.02), 0.5 + h * 0.55, 0));
  // çatı
  const rh = d * 0.45, half = d / 2 + 0.55, len = Math.hypot(half, rh), ang = Math.atan2(rh, half);
  for (const s of [-1, 1]) sb.box(w + 1.0, 0.18, len, roofMat, L(0, top + rh / 2, s * half / 2, s * ang, 0, 0));
  sb.box(w + 1.1, 0.28, 0.35, 'darkWood', L(0, top + rh + 0.02, 0));
  const tri = new THREE.Shape(); tri.moveTo(-d / 2, 0); tri.lineTo(0, rh); tri.lineTo(d / 2, 0); tri.closePath();
  for (const s of [-1, 1]) sb.add(new THREE.ShapeGeometry(tri), 'plaster', L(s * w / 2, top, 0, 0, s * Math.PI / 2, 0));
  // kapı ve pencereler
  sb.box(1.2, 2.1, 0.14, 'darkWood', L(0, 0.5 + 1.05, d / 2 + 0.05));
  sb.box(1.5, 0.2, 0.3, 'darkWood', L(0, 0.5 + 2.2, d / 2 + 0.1));
  for (const wx of [-w / 3.2, w / 3.2]) { sb.box(0.8, 0.8, 0.1, 'windowGlow', L(wx, 0.5 + h * 0.62, d / 2 + 0.04)); sb.box(1.0, 0.14, 0.2, 'darkWood', L(wx, 0.5 + h * 0.62 - 0.46, d / 2 + 0.1)); }
  sb.box(0.1, 0.8, 0.8, 'windowGlow', L(w / 2 + 0.04, 0.5 + h * 0.62, 0)); sb.box(0.1, 0.8, 0.8, 'windowGlow', L(-w / 2 - 0.04, 0.5 + h * 0.62, 0));
  sb.box(0.8, 2.6, 0.8, 'stone', L(w / 3, top + rh * 0.7, -d / 5));
  addBox(x, z, w / 2 + 0.3, d / 2 + 0.3, ry);
  const cw = new THREE.Vector3(w / 3, top + rh * 0.7 + 1.4, -d / 5).applyAxisAngle(new THREE.Vector3(0, 1, 0), ry);
  return { chimney: new THREE.Vector3(x + cw.x, y0 + cw.y, z + cw.z) };
}

export function buildStructures(scene, world, q) {
  const animated = [], interact = [], out = { animated, interact };
  const r = mulberry32(77);
  // ================= KÖY =================
  const sb = new SB(scene, q);
  const houses = [
    [-30, -22, 7, 6, 4], [30, -24, 8, 6, 4.5], [-36, 14, 6, 6, 3.8], [38, 20, 7, 7, 4.2], [-16, 36, 6, 5, 3.6], [18, 40, 7, 6, 4],
    [-44, -4, 6, 5, 3.6], [-18, -42, 6, 6, 4],
  ];
  const chimneys = [];
  houses.forEach(([x, z, w, d, h], i) => {
    const ry = Math.atan2(-x, -z);
    chimneys.push(house(sb, x, z, ry, w, d, h, i % 3 === 2 ? 'roofBlue' : i % 4 === 3 ? 'thatch' : 'roof').chimney);
  });
  chimneys.forEach((c) => world.emitters.push({ type: 'smoke', pos: c, rate: 3 }));
  // kuyu
  {
    const x = 6, z = 6;
    sb.add(new THREE.CylinderGeometry(1.6, 1.7, 1.1, 14, 1, true), 'stone', W(x, z, 0, 0, 0.55, 0));
    sb.add(new THREE.CylinderGeometry(1.35, 1.35, 0.2, 14), 'water', W(x, z, 0, 0, 0.7, 0));
    sb.add(new THREE.TorusGeometry(1.65, 0.18, 6, 16), 'stone', W(x, z, 0, 0, 1.1, 0, Math.PI / 2));
    for (const s of [-1, 1]) sb.box(0.2, 2.6, 0.2, 'darkWood', W(x, z, 0, s * 1.5, 1.3, 0));
    sb.box(3.4, 0.14, 1.6, 'roof', W(x, z, 0, 0, 2.7, 0.4, 0.5)); sb.box(3.4, 0.14, 1.6, 'roof', W(x, z, 0, 0, 2.7, -0.4, -0.5));
    sb.add(new THREE.CylinderGeometry(0.15, 0.15, 3, 6), 'wood', W(x, z, 0, 0, 2.1, 0, 0, 0, Math.PI / 2));
    addCircle(x, z, 1.9);
  }
  // kamp ateşi
  {
    const x = -8, z = 4, y = heightAt(x, z);
    for (let i = 0; i < 9; i++) { const a = (i / 9) * Math.PI * 2; sb.add(jitter(new THREE.DodecahedronGeometry(0.35, 0), 0.2, 2, i), 'rock', W(x, z, 0, Math.cos(a) * 1.3, 0.15, Math.sin(a) * 1.3)); }
    for (let i = 0; i < 4; i++) sb.add(new THREE.CylinderGeometry(0.12, 0.14, 1.8, 6), 'charBark' , W(x, z, i * 0.8, 0, 0.35, 0, 0.9, 0, 0));
    world.emitters.push({ type: 'fire', pos: new THREE.Vector3(x, y + 0.4, z), rate: 30, size: 1 });
    const l = new THREE.PointLight(0xff8a3a, 30, 28, 1.6); l.position.set(x, y + 1.5, z); scene.add(l); world.lights.push({ light: l, base: 30, flicker: true, seed: 1 });
    addCircle(x, z, 1.4, y + 0.5);
    // kütük banklar
    for (let i = 0; i < 3; i++) { const a = i * 2.1 + 0.4; const bx = x + Math.cos(a) * 3.4, bz = z + Math.sin(a) * 3.4; sb.add(new THREE.CylinderGeometry(0.3, 0.3, 2.2, 8), 'bark', W(bx, bz, -a, 0, 0.3, 0, 0, 0, Math.PI / 2)); }
  }
  // pazar tezgahları
  const stall = (x, z, ry, mat) => {
    for (const sx of [-1.4, 1.4]) for (const sz of [-1, 1]) sb.box(0.15, 2.6, 0.15, 'darkWood', W(x, z, ry, sx, 1.3, sz));
    sb.box(3.2, 0.12, 2.6, mat, W(x, z, ry, 0, 2.7, 0, 0.18));
    sb.box(3, 0.9, 0.9, 'wood', W(x, z, ry, 0, 0.45, 0.6));
    for (let i = 0; i < 5; i++) sb.add(new THREE.SphereGeometry(0.18, 8, 6), i % 2 ? 'autumnLeaves' : 'fireCrystal', W(x, z, ry, -1 + i * 0.5, 1.05, 0.6));
    addBox(x, z, 1.6, 1.2, ry);
  };
  stall(16, -8, -1.3, 'stripes'); stall(14, 12, -1.9, 'cloth');
  // demirci
  {
    const x = -22, z = -6, ry = 1.2, y = heightAt(x, z);
    for (const sx of [-2.4, 2.4]) for (const sz of [-2, 2]) sb.box(0.3, 3.4, 0.3, 'darkWood', W(x, z, ry, sx, 1.7, sz));
    sb.box(5.6, 0.2, 5, 'thatch', W(x, z, ry, 0, 3.5, 0, 0.1));
    sb.box(1.6, 1.2, 1.6, 'stone', W(x, z, ry, -1.4, 0.6, -1.2)); sb.box(1.2, 0.2, 1.2, 'coal', W(x, z, ry, -1.4, 1.25, -1.2));
    sb.box(0.8, 3.4, 0.8, 'stone', W(x, z, ry, -1.4, 2.4, -1.6));
    sb.box(0.5, 0.6, 0.4, 'darkIron', W(x, z, ry, 1, 0.3, 0.4)); sb.box(1.0, 0.25, 0.45, 'darkIron', W(x, z, ry, 1, 0.72, 0.4));
    const fp = new THREE.Vector3(-1.4, 1.4, -1.2).applyAxisAngle(new THREE.Vector3(0, 1, 0), ry);
    world.emitters.push({ type: 'ember', pos: new THREE.Vector3(x + fp.x, y + fp.y, z + fp.z), rate: 6 });
    const cp = new THREE.Vector3(-1.4, 4.2, -1.6).applyAxisAngle(new THREE.Vector3(0, 1, 0), ry);
    world.emitters.push({ type: 'smoke', pos: new THREE.Vector3(x + cp.x, y + cp.y, z + cp.z), rate: 4 });
    if (q.lights > 1) { const l = new THREE.PointLight(0xff6020, 14, 14, 1.8); l.position.set(x + fp.x, y + 2, z + fp.z); scene.add(l); world.lights.push({ light: l, base: 14, flicker: true, seed: 5 }); }
    addBox(x, z, 2.6, 2.2, ry, y + 1.5);
    // silah rafı
    for (let i = 0; i < 3; i++) sb.box(0.08, 1.4, 0.2, 'iron', W(x, z, ry, 2.6, 1.0, -1 + i * 0.6, 0, 0, 0.1));
  }
  // yel değirmeni
  {
    const x = 44, z = -46, y = heightAt(x, z);
    sb.add(new THREE.CylinderGeometry(2.2, 3.2, 10, 10), 'plaster', W(x, z, 0, 0, 5, 0));
    sb.add(new THREE.ConeGeometry(3, 3, 10), 'thatch', W(x, z, 0, 0, 11.4, 0));
    sb.box(1.2, 2, 0.2, 'darkWood', W(x, z, Math.atan2(-x, -z), 0, 1, 3.05));
    addCircle(x, z, 3.2);
    const hub = new THREE.Group(); hub.position.set(x, y + 9, z); hub.rotation.y = Math.atan2(-x, -z);
    const blades = new THREE.Group(); blades.position.z = 3; hub.add(blades);
    for (let i = 0; i < 4; i++) {
      const arm = new THREE.Mesh(new THREE.BoxGeometry(0.25, 7, 0.2), M('darkWood')); arm.position.y = 3.5;
      const sail = new THREE.Mesh(new THREE.PlaneGeometry(1.8, 5.5), M('cloth')); sail.position.set(1, 4, 0.12); sail.material = M('stripes');
      const g = new THREE.Group(); g.rotation.z = (i * Math.PI) / 2; g.add(arm, sail); blades.add(g);
      arm.castShadow = sail.castShadow = q.shadows;
    }
    scene.add(hub); animated.push((dt) => (blades.rotation.z += dt * 0.6));
  }
  // çit ve fenerler
  for (let a = 0; a < Math.PI * 2; a += 0.08) {
    const skip = [0, Math.PI / 2, Math.PI, -Math.PI / 2, (3 * Math.PI) / 2, 2.43].some((g) => Math.abs(Math.atan2(Math.sin(a - g), Math.cos(a - g))) < 0.16);
    if (skip) continue;
    const R = 58, x = Math.cos(a) * R, z = Math.sin(a) * R;
    sb.box(0.2, 1.4, 0.2, 'darkWood', W(x, z, 0, 0, 0.7, 0));
    sb.box(0.1, 0.14, R * 0.085, 'wood', W(x, z, -a, 0, 1.0, R * 0.04)); sb.box(0.1, 0.14, R * 0.085, 'wood', W(x, z, -a, 0, 0.55, R * 0.04));
    addCircle(x, z, 0.5, heightAt(x, z) + 1.2);
  }
  for (let i = 0; i < 8; i++) {
    const a = (i / 8) * Math.PI * 2 + 0.2, x = Math.cos(a) * 22, z = Math.sin(a) * 22;
    sb.box(0.18, 3.2, 0.18, 'darkIron', W(x, z, 0, 0, 1.6, 0)); sb.box(0.9, 0.12, 0.12, 'darkIron', W(x, z, a, 0.35, 3.1, 0));
    sb.box(0.35, 0.5, 0.35, 'lantern', W(x, z, a, 0.7, 2.75, 0)); addCircle(x, z, 0.3);
  }
  // fıçı, sandık, saman
  for (let i = 0; i < 26; i++) {
    const a = r() * Math.PI * 2, d = 12 + r() * 40, x = Math.cos(a) * d, z = Math.sin(a) * d;
    if (houses.some((h) => Math.hypot(h[0] - x, h[1] - z) < 7) || Math.hypot(x - 6, z - 6) < 4 || Math.hypot(x + 8, z - 4) < 5.5 || Math.hypot(x + 22, z + 6) < 5 || Math.hypot(x - 16, z + 8) < 3.5 || Math.hypot(x - 14, z - 12) < 3.5) continue;
    const k = r();
    if (k < 0.4) { sb.add(new THREE.CylinderGeometry(0.45, 0.45, 1.1, 10), 'wood', W(x, z, r() * 6, 0, 0.55, 0)); sb.add(new THREE.TorusGeometry(0.46, 0.04, 4, 12), 'darkIron', W(x, z, 0, 0, 0.3, 0, Math.PI / 2)); sb.add(new THREE.TorusGeometry(0.46, 0.04, 4, 12), 'darkIron', W(x, z, 0, 0, 0.8, 0, Math.PI / 2)); addCircle(x, z, 0.5, heightAt(x, z) + 1.1); }
    else if (k < 0.75) { sb.box(0.9, 0.9, 0.9, 'wood', W(x, z, r() * 6, 0, 0.45, 0)); addCircle(x, z, 0.6, heightAt(x, z) + 0.9); }
    else { sb.add(new THREE.CylinderGeometry(0.6, 0.6, 1.3, 10), 'hay', W(x, z, r() * 6, 0, 0.6, 0, 0, 0, Math.PI / 2)); addCircle(x, z, 0.7, heightAt(x, z) + 1.2); }
  }
  // sancaklar
  for (const [x, z] of [[-6, -24], [6, -24], [-24, 6], [24, 6]]) {
    sb.box(0.16, 6, 0.16, 'darkWood', W(x, z, 0, 0, 3, 0));
    const flag = new THREE.Mesh(new THREE.PlaneGeometry(1.4, 2.6, 4, 8), addWind(new THREE.MeshStandardMaterial({ color: 0x7a1f2a, side: THREE.DoubleSide, roughness: 1 }), 3, 1));
    flag.geometry.translate(0.7, -1.3, 0); flag.position.set(x + 0.08, heightAt(x, z) + 5.8, z); flag.rotation.y = Math.atan2(-x, -z) + Math.PI / 2; scene.add(flag);
  }
  sb.build(['windowGlow', 'lantern', 'water', 'coal']);

  // ================= ORK KAMPI =================
  {
    const o = new SB(scene, q), c = LOC.orcCamp, y0 = heightAt(c.x, c.z);
    for (let a = 0; a < Math.PI * 2; a += 0.13) {
      if (Math.abs(Math.atan2(Math.sin(a - 0.4), Math.cos(a - 0.4))) < 0.3) continue;
      const x = c.x + Math.cos(a) * 24, z = c.z + Math.sin(a) * 24;
      o.add(new THREE.CylinderGeometry(0.25, 0.3, 4 + r() * 1.5, 6), 'bark', W(x, z, 0, 0, 2, 0, (r() - 0.5) * 0.2, 0, (r() - 0.5) * 0.2));
      o.add(new THREE.ConeGeometry(0.28, 0.8, 6), 'bark', W(x, z, 0, 0, 4.6, 0));
      addCircle(x, z, 0.6);
    }
    for (let i = 0; i < 4; i++) {
      const a = i * 1.6 + 1.2, x = c.x + Math.cos(a) * 13, z = c.z + Math.sin(a) * 13;
      o.add(new THREE.ConeGeometry(3.2, 4.5, 7, 1, true), 'thatch', W(x, z, r(), 0, 2.25, 0));
      o.add(new THREE.CylinderGeometry(0.1, 0.1, 5.5, 5), 'bark', W(x, z, 0, 0, 2.75, 0));
      addCircle(x, z, 3);
    }
    o.add(new THREE.CylinderGeometry(0.35, 0.45, 5, 7), 'bark', W(c.x + 4, c.z - 3, 0, 0, 2.5, 0));
    o.add(new THREE.SphereGeometry(0.5, 10, 8), 'bone', W(c.x + 4, c.z - 3, 0, 0, 5.2, 0));
    o.add(new THREE.ConeGeometry(0.15, 0.9, 5), 'bone', W(c.x + 4, c.z - 3, 0, 0.45, 5.6, 0, 0, 0, -0.6)); o.add(new THREE.ConeGeometry(0.15, 0.9, 5), 'bone', W(c.x + 4, c.z - 3, 0, -0.45, 5.6, 0, 0, 0, 0.6));
    addCircle(c.x + 4, c.z - 3, 0.6);
    for (let i = 0; i < 8; i++) { const a = (i / 8) * 6.28; o.add(new THREE.DodecahedronGeometry(0.4, 0), 'rock', W(c.x, c.z, 0, Math.cos(a) * 1.6, 0.2, Math.sin(a) * 1.6)); }
    world.emitters.push({ type: 'fire', pos: new THREE.Vector3(c.x, y0 + 0.4, c.z), rate: 34, size: 1.3 });
    if (q.lights > 2) { const l = new THREE.PointLight(0xff7a2a, 26, 26, 1.6); l.position.set(c.x, y0 + 2, c.z); scene.add(l); world.lights.push({ light: l, base: 26, flicker: true, seed: 9 }); }
    addCircle(c.x, c.z, 1.6, y0 + 0.5);
    o.build();
  }
  // ================= ORMAN HARABELERİ =================
  {
    const o = new SB(scene, q), c = LOC.forestBoss;
    for (let i = 0; i < 12; i++) {
      const a = (i / 12) * Math.PI * 2, x = c.x + Math.cos(a) * 32, z = c.z + Math.sin(a) * 32;
      const hgt = i % 3 === 0 ? 2 + r() * 2 : 6 + r() * 3;
      o.add(new THREE.CylinderGeometry(1, 1.15, hgt, 10), i % 2 ? 'mossStone' : 'stone', W(x, z, 0, 0, hgt / 2, 0));
      o.box(2.6, 0.6, 2.6, 'stone', W(x, z, a, 0, 0.3, 0));
      if (hgt > 5) o.box(2.4, 0.6, 2.4, 'mossStone', W(x, z, a, 0, hgt + 0.3, 0));
      addCircle(x, z, 1.4);
    }
    o.box(4, 1.2, 2.5, 'mossStone', W(c.x, c.z - 8, 0, 0, 0.6, 0));
    o.add(new THREE.CylinderGeometry(1.4, 1.4, 10, 10), 'mossStone', W(c.x + 18, c.z + 10, 0.4, 0, 1, 0, 0, 0, Math.PI / 2));
    // kemer girişi
    for (const s of [-1, 1]) o.box(1.8, 8, 1.8, 'mossStone', W(c.x + s * 4.5, c.z + 42, 0, 0, 4, 0));
    o.box(11, 1.6, 2, 'mossStone', W(c.x, c.z + 42, 0, 0, 8.6, 0));
    addCircle(c.x - 4.5, c.z + 42, 1.3); addCircle(c.x + 4.5, c.z + 42, 1.3);
    o.build();
  }
  // ================= PİRAMİT =================
  {
    const o = new SB(scene, q), c = LOC.pyramid;
    for (let i = 0; i < 8; i++) { const s = 36 - i * 4.4; o.box(s, 3.2, s, 'sandstone', W(c.x, c.z, 0, 0, 1.6 + i * 3.2, 0)); }
    o.box(3, 5, 3, 'gold', W(c.x, c.z, 0, 0, 27, 0, 0, Math.PI / 4));
    o.box(5, 6, 3, 'darkWood', W(c.x, c.z, 0, -18.2, 3, 0, 0, Math.PI / 2));
    addBox(c.x, c.z, 18.5, 18.5, 0);
    for (const s of [-1, 1]) {
      const x = LOC.desertBoss.x - 10, z = LOC.desertBoss.z + s * 30;
      o.add(new THREE.CylinderGeometry(0.4, 1.3, 14, 4), 'sandstone', W(x, z, Math.PI / 4, 0, 7, 0));
      o.add(new THREE.ConeGeometry(0.55, 1.4, 4), 'gold', W(x, z, Math.PI / 4, 0, 14.6, 0));
      addCircle(x, z, 1.4);
    }
    for (let i = 0; i < 10; i++) {
      const a = (i / 10) * Math.PI * 2, x = LOC.desertBoss.x + Math.cos(a) * 40, z = LOC.desertBoss.z + Math.sin(a) * 40;
      if (Math.cos(a) > 0.6) continue;
      o.box(2, 3 + r() * 3, 2, 'sandstone', W(x, z, a, 0, 1.5, 0, (r() - 0.5) * 0.2)); addCircle(x, z, 1.3);
    }
    // yıkık heykel başı
    o.add(new THREE.SphereGeometry(2.5, 10, 8), 'sandstone', W(LOC.desertBoss.x - 22, LOC.desertBoss.z - 22, 0.7, 0, 1.5, 0, 0.3, 0, 0.4, 1, 1.25, 1));
    o.box(3, 1.2, 1.4, 'sandstone', W(LOC.desertBoss.x - 22, LOC.desertBoss.z - 22, 0.7, 0, 2.2, 2.2));
    addCircle(LOC.desertBoss.x - 22, LOC.desertBoss.z - 22, 2.8);
    o.build();
  }
  // ================= BUZ KALESİ =================
  {
    const o = new SB(scene, q), c = LOC.snowBoss;
    for (let i = 0; i < 14; i++) {
      const a = (i / 14) * Math.PI * 2, x = c.x + Math.cos(a) * 38, z = c.z + Math.sin(a) * 38;
      if (Math.cos(a) > 0.8) continue;
      const hh = 5 + r() * 9; const g = new THREE.CylinderGeometry(0, 1.4 + r(), hh, 6); g.translate(0, hh / 2, 0);
      o.add(g, 'ice', W(x, z, r(), 0, 0, 0, (r() - 0.5) * 0.4, 0, (r() - 0.5) * 0.4)); addCircle(x, z, 1.5);
    }
    for (let i = 0; i < 6; i++) { const a = i * 1.05 + 0.5, x = c.x + Math.cos(a) * 48, z = c.z + Math.sin(a) * 48; o.box(8, 5 + r() * 3, 2, 'snowRock', W(x, z, -a + Math.PI / 2, 0, 2.5, 0)); addBox(x, z, 4, 1, -a + Math.PI / 2); }
    for (const s of [-1, 1]) { o.box(3, 12, 3, 'snowRock', W(c.x + 44, c.z + s * 7, 0, 0, 6, 0)); o.add(new THREE.ConeGeometry(2.4, 4, 4), 'ice', W(c.x + 44, c.z + s * 7, Math.PI / 4, 0, 14, 0)); addBox(c.x + 44, c.z + s * 7, 1.5, 1.5, 0); }
    o.build(['ice']);
  }
  // ================= EJDERHA İNİ =================
  {
    const o = new SB(scene, q), c = LOC.dragonLair;
    for (let i = 0; i < 22; i++) {
      const a = (i / 22) * Math.PI * 2; if (Math.abs(Math.atan2(Math.sin(a + Math.PI / 2), Math.cos(a + Math.PI / 2))) < 0.3) continue;
      const x = c.x + Math.cos(a) * 46, z = c.z + Math.sin(a) * 46; const hh = 8 + r() * 12;
      const g = jitter(new THREE.CylinderGeometry(0, 2 + r() * 1.5, hh, 5, 3), 0.15, 0.5, i); g.translate(0, hh / 2, 0);
      o.add(g, 'obsidian', W(x, z, r() * 3, 0, 0, 0, (r() - 0.5) * 0.3, 0, (r() - 0.5) * 0.3)); addCircle(x, z, 2.2);
    }
    // dev kemikler
    o.add(new THREE.TorusGeometry(5, 0.4, 6, 12, Math.PI), 'bone', W(c.x + 22, c.z + 18, 0.6, 0, 0, 0));
    o.add(new THREE.TorusGeometry(4.2, 0.35, 6, 12, Math.PI), 'bone', W(c.x + 24, c.z + 21, 0.6, 0, 0, 0));
    o.add(new THREE.SphereGeometry(2.5, 10, 8), 'bone', W(c.x - 20, c.z + 22, 1, 0, 1, 0, 0, 0, 0, 1.6, 0.8, 1));
    // giriş kapıları
    for (const s of [-1, 1]) { o.box(3, 16, 3, 'darkRock', W(c.x + s * 9, c.z - 46, 0, 0, 8, 0)); o.add(new THREE.ConeGeometry(2, 5, 4), 'obsidian', W(c.x + s * 9, c.z - 46, 0, 0, 18.5, 0)); addBox(c.x + s * 9, c.z - 46, 1.5, 1.5, 0); }
    o.build();
    const bar = new THREE.Mesh(new THREE.PlaneGeometry(15, 14, 1, 1), M('barrier'));
    bar.position.set(c.x, heightAt(c.x, c.z - 46) + 7, c.z - 46); scene.add(bar);
    out.barrier = { mesh: bar, x: c.x, z: c.z - 46, w: 7.5 };
    animated.push((dt, t) => { bar.material.opacity = 0.25 + Math.sin(t * 3) * 0.1; });
  }
  // ================= IŞINLANMA TAŞLARI =================
  for (const w of WAYSTONES) {
    const o = new SB(scene, q), y = heightAt(w.x, w.z);
    o.add(new THREE.CylinderGeometry(2.6, 3, 0.6, 8), 'stone', W(w.x, w.z, 0, 0, 0.1, 0));
    o.add(new THREE.CylinderGeometry(0.45, 0.7, 3.4, 6), 'stone', W(w.x, w.z, 0, 0, 1.9, 0));
    for (let i = 0; i < 4; i++) { const a = (i / 4) * Math.PI * 2 + 0.4; o.box(0.5, 1.6, 0.5, 'stone', W(w.x, w.z, a, 2.1, 0.9, 0)); o.box(0.3, 0.3, 0.3, 'rune', W(w.x, w.z, a, 2.1, 1.85, 0)); }
    o.build(['rune']);
    const cr = new THREE.Mesh(new THREE.OctahedronGeometry(0.55, 0), M('rune')); cr.scale.y = 1.6; cr.position.set(w.x, y + 4.4, w.z); scene.add(cr);
    animated.push((dt, t) => { cr.rotation.y += dt * 1.2; cr.position.y = y + 4.4 + Math.sin(t * 2 + w.x) * 0.25; });
    addCircle(w.x, w.z, 0.9);
    world.emitters.push({ type: 'rune', pos: new THREE.Vector3(w.x, y + 4.4, w.z), rate: 3 });
    interact.push({ kind: 'waystone', id: w.id, name: w.name, x: w.x, z: w.z, r: 4, crystal: cr });
  }
  // ================= SANDIKLAR =================
  const chestSpots = [[LOC.orcCamp.x - 6, LOC.orcCamp.z + 8], [LOC.forestBoss.x + 6, LOC.forestBoss.z - 12], [LOC.pyramid.x - 22, LOC.pyramid.z + 6], [LOC.snowBoss.x - 10, LOC.snowBoss.z + 16], [LOC.oasis.x + 10, LOC.oasis.z + 30], [150, -210], [-200, -160], [-180, 210], [200, 190], [60, -150]];
  chestSpots.forEach(([x, z], i) => {
    const g = new THREE.Group(); const y = heightAt(x, z);
    const base = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.7, 0.8), M('wood')); base.position.y = 0.35;
    const lid = new THREE.Group(); lid.position.set(0, 0.7, -0.4);
    const lm = new THREE.Mesh(new THREE.CylinderGeometry(0.4, 0.4, 1.2, 10, 1, false, 0, Math.PI), M('wood')); lm.rotation.z = Math.PI / 2; lm.position.z = 0.4; lid.add(lm);
    const band = new THREE.Mesh(new THREE.BoxGeometry(1.25, 0.1, 0.85), M('gold')); band.position.y = 0.55;
    const lock = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.25, 0.08), M('gold')); lock.position.set(0, 0.55, 0.42);
    g.add(base, lid, band, lock); g.position.set(x, y, z); g.rotation.y = r() * 6; g.traverse((m) => { if (m.isMesh) { m.castShadow = q.shadows; } });
    scene.add(g); addCircle(x, z, 0.8, y + 0.8);
    interact.push({ kind: 'chest', id: 'chest' + i, x, z, r: 2.6, lid, level: Math.max(1, Math.round(Math.hypot(x, z) / 40)), mesh: g });
  });
  return out;
}
