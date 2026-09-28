// Doğa objeleri: ağaçlar, kayalar, kristaller... (instanced çizim için geometri + malzeme grupları)
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { M } from './mats.js';
import { noise } from './noise.js';
import { mulberry32 } from './util.js';

export function prep(g, m) {
  let geo = g.index ? g.toNonIndexed() : g;
  if (m) geo.applyMatrix4(m);
  if (!geo.attributes.uv) geo.setAttribute('uv', new THREE.Float32BufferAttribute(new Float32Array(geo.attributes.position.count * 2), 2));
  for (const k of Object.keys(geo.attributes)) if (!['position', 'normal', 'uv'].includes(k)) geo.deleteAttribute(k);
  return geo;
}
export function T(x = 0, y = 0, z = 0, rx = 0, ry = 0, rz = 0, sx = 1, sy = sx, sz = sx) {
  return new THREE.Matrix4().compose(new THREE.Vector3(x, y, z), new THREE.Quaternion().setFromEuler(new THREE.Euler(rx, ry, rz)), new THREE.Vector3(sx, sy, sz));
}
export function jitter(geo, amt, freq = 1.3, seed = 0) {
  const p = geo.attributes.position;
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i), z = p.getZ(i);
    const n = noise.n2(x * freq + seed, z * freq + y * freq * 0.7 - seed);
    const k = 1 + n * amt; p.setXYZ(i, x * k, y * k, z * k);
  }
  geo.computeVertexNormals(); return geo;
}
export function blob(r, amt = 0.25, seed = 0, detail = 1) { return jitter(new THREE.IcosahedronGeometry(r, detail), amt, 1.5 / r, seed); }
function merge(list) { const g = mergeGeometries(list.map((x) => (x.index ? x.toNonIndexed() : x)), false); g.computeBoundingSphere(); return g; }
// parts: [[geoList, matName], ...]
function build(parts, opts = {}) {
  const geos = [], mats = [];
  for (const [list, mat] of parts) { geos.push(merge(list)); mats.push(M(mat)); }
  const geo = mergeGeometries(geos, true); geo.computeBoundingSphere();
  return { geo, mats, ...opts };
}

function oak(leafMat = 'leaves') {
  const r = mulberry32(11);
  const trunk = [prep(new THREE.CylinderGeometry(0.28, 0.45, 3.4, 7, 3), T(0, 1.7, 0))];
  trunk.push(prep(new THREE.CylinderGeometry(0.1, 0.18, 1.8, 5), T(0.5, 2.8, 0, 0, 0, -0.9)));
  trunk.push(prep(new THREE.CylinderGeometry(0.1, 0.18, 1.6, 5), T(-0.45, 3.0, 0.2, 0.2, 0, 0.9)));
  const leaves = [];
  const pts = [[0, 4.6, 0, 1.9], [1.2, 4.0, 0.4, 1.3], [-1.1, 4.2, -0.3, 1.4], [0.2, 4.1, 1.2, 1.3], [-0.3, 4.3, -1.2, 1.2], [0.3, 5.5, 0.2, 1.2]];
  pts.forEach((p, i) => leaves.push(prep(blob(p[3], 0.28, i * 3.1), T(p[0], p[1], p[2], r(), r(), 0))));
  return build([[trunk, 'bark'], [leaves, leafMat]], { col: 0.55 });
}
function pine(snow = false, dark = false) {
  const trunk = [prep(new THREE.CylinderGeometry(0.18, 0.34, 3, 6), T(0, 1.5, 0))];
  const leaves = [], caps = [];
  const tiers = 5;
  for (let i = 0; i < tiers; i++) {
    const rr = 2.2 - i * 0.38, y = 1.7 + i * 1.15;
    leaves.push(prep(jitter(new THREE.ConeGeometry(rr, 1.9, 8, 1), 0.12, 1, i), T(0, y, 0, 0, i * 0.7, 0)));
    if (snow) caps.push(prep(new THREE.ConeGeometry(rr * 0.7, 0.8, 8, 1), T(0, y + 0.62, 0, 0, i * 0.7, 0)));
  }
  const parts = [[trunk, dark ? 'darkBark' : 'bark'], [leaves, dark ? 'darkLeaves' : 'pineLeaves']];
  if (snow) parts.push([caps, 'snowCap']);
  return build(parts, { col: 0.45 });
}
function deadTree() {
  const t = [prep(new THREE.CylinderGeometry(0.2, 0.5, 4.5, 6, 3), T(0, 2.2, 0))];
  const r = mulberry32(5);
  for (let i = 0; i < 6; i++) {
    const y = 2 + r() * 2.4, a = r() * 6.28, l = 1 + r() * 1.6;
    t.push(prep(new THREE.CylinderGeometry(0.04, 0.14, l, 4), T(Math.cos(a) * l * 0.4, y + l * 0.3, Math.sin(a) * l * 0.4, Math.sin(a) * 0.9, 0, -Math.cos(a) * 0.9)));
  }
  return build([[t, 'charBark']], { col: 0.45 });
}
function cactus() {
  const g = [prep(new THREE.CapsuleGeometry(0.4, 3.4, 4, 10), T(0, 2.1, 0))];
  g.push(prep(new THREE.CapsuleGeometry(0.26, 1.0, 4, 8), T(0.75, 2.1, 0, 0, 0, Math.PI / 2)));
  g.push(prep(new THREE.CapsuleGeometry(0.26, 1.3, 4, 8), T(1.1, 2.8, 0)));
  g.push(prep(new THREE.CapsuleGeometry(0.22, 0.8, 4, 8), T(-0.6, 2.6, 0, 0, 0, Math.PI / 2)));
  g.push(prep(new THREE.CapsuleGeometry(0.22, 1.0, 4, 8), T(-0.9, 3.1, 0)));
  return build([[g, 'cactus']], { col: 0.5 });
}
function palm() {
  const trunk = []; let x = 0, y = 0;
  for (let i = 0; i < 7; i++) { trunk.push(prep(new THREE.CylinderGeometry(0.22 - i * 0.012, 0.3 - i * 0.012, 1.0, 7), T(x, y + 0.5, 0, 0, 0, -0.05 - i * 0.03))); x += 0.08 + i * 0.03; y += 0.97; }
  const fr = [];
  for (let i = 0; i < 8; i++) {
    const a = (i / 8) * Math.PI * 2; const g = new THREE.PlaneGeometry(0.9, 3.4, 1, 4);
    const p = g.attributes.position; for (let k = 0; k < p.count; k++) { const yy = p.getY(k); p.setZ(k, -(((yy + 1.7) / 3.4) ** 2) * 1.2); }
    g.computeVertexNormals();
    fr.push(prep(g, new THREE.Matrix4().makeTranslation(x, y, 0).multiply(new THREE.Matrix4().makeRotationY(a)).multiply(new THREE.Matrix4().makeRotationX(-1.1)).multiply(new THREE.Matrix4().makeTranslation(0, 1.7, 0))));
  }
  return build([[trunk, 'bark'], [fr, 'palmLeaves']], { col: 0.4 });
}
function bush(mat = 'leaves') {
  const g = []; const pts = [[0, 0.5, 0, 0.8], [0.6, 0.4, 0.2, 0.6], [-0.5, 0.45, -0.2, 0.65], [0.1, 0.4, -0.6, 0.55]];
  pts.forEach((p, i) => g.push(prep(blob(p[3], 0.3, i * 7), T(p[0], p[1], p[2]))));
  return build([[g, mat]], { col: 0, shadow: false });
}
function rock(mat = 'rock', big = false) {
  const g = [prep(jitter(new THREE.DodecahedronGeometry(1, 1), 0.3, 1.2, big ? 3 : 1), T(0, 0.25, 0, 0, 0, 0, 1.3, 0.8, 1.1))];
  if (big) g.push(prep(jitter(new THREE.DodecahedronGeometry(0.7, 1), 0.3, 1.5, 9), T(0.9, 0.2, 0.5)));
  return build([[g, mat]], { col: big ? 1.3 : 1.0 });
}
function pillarRock(mat = 'sandRock') {
  const g = [prep(jitter(new THREE.CylinderGeometry(1.2, 1.8, 6, 7, 4), 0.2, 0.8, 4), T(0, 3, 0)), prep(jitter(new THREE.CylinderGeometry(1.9, 1.3, 1.4, 7, 1), 0.2, 0.8, 8), T(0, 6.4, 0))];
  return build([[g, mat]], { col: 1.6 });
}
function mushrooms(capMat) {
  const stems = [], caps = [];
  [[0, 0, 1], [0.6, 0.3, 0.7], [-0.4, 0.5, 0.55], [0.2, -0.6, 0.5]].forEach(([x, z, s]) => {
    stems.push(prep(new THREE.CylinderGeometry(0.08 * s, 0.12 * s, 0.9 * s, 6), T(x, 0.45 * s, z)));
    caps.push(prep(new THREE.SphereGeometry(0.42 * s, 10, 6, 0, Math.PI * 2, 0, Math.PI / 2), T(x, 0.85 * s, z, 0, 0, 0, 1, 0.7, 1)));
  });
  return build([[stems, 'stem'], [caps, capMat]], { col: 0, shadow: false });
}
function crystals(mat) {
  const g = []; const r = mulberry32(21);
  for (let i = 0; i < 6; i++) {
    const h = 1 + r() * 2.2, a = r() * 6.28, tilt = 0.2 + r() * 0.5;
    const c = new THREE.CylinderGeometry(0, 0.3 + r() * 0.2, h, 6); c.translate(0, h / 2, 0);
    g.push(prep(c, T(Math.cos(a) * 0.3, 0, Math.sin(a) * 0.3, Math.sin(a) * tilt, 0, -Math.cos(a) * tilt)));
  }
  return build([[g, mat]], { col: 0.6 });
}
function logProp() {
  const g = [prep(new THREE.CylinderGeometry(0.35, 0.4, 4, 8), T(0, 0.35, 0, 0, 0, Math.PI / 2))];
  return build([[g, 'bark']], { col: 0 });
}
function ribs() {
  const g = [];
  for (let i = 0; i < 6; i++) g.push(prep(new THREE.TorusGeometry(2.2 - Math.abs(i - 2.5) * 0.25, 0.12, 5, 10, Math.PI), T(i * 0.9 - 2.2, 0, 0, 0, Math.PI / 2, 0)));
  g.push(prep(new THREE.CylinderGeometry(0.18, 0.18, 6.5, 6), T(0, 0.2, 0, 0, 0, Math.PI / 2)));
  g.push(prep(new THREE.SphereGeometry(0.9, 8, 6), T(3.6, 0.5, 0, 0, 0, 0, 1.4, 0.8, 0.8)));
  return build([[g, 'bone']], { col: 1.8 });
}
function grassClump() {
  const pos = [], col = [];
  const r = mulberry32(3);
  for (let b = 0; b < 5; b++) {
    const a = r() * Math.PI, h = 0.32 + r() * 0.36, w = 0.06, ox = (r() - 0.5) * 0.4, oz = (r() - 0.5) * 0.4, lean = (r() - 0.5) * 0.3;
    const dx = Math.cos(a) * w, dz = Math.sin(a) * w;
    const v = [[ox - dx, 0, oz - dz, 0], [ox + dx, 0, oz + dz, 0], [ox - dx * 0.6 + lean * 0.5, h * 0.55, oz - dz * 0.6, 0.55], [ox + dx * 0.6 + lean * 0.5, h * 0.55, oz + dz * 0.6, 0.55], [ox + lean, h, oz, 1]];
    const tris = [[0, 1, 3], [0, 3, 2], [2, 3, 4]];
    for (const t of tris) for (const k of t) { pos.push(v[k][0], v[k][1], v[k][2]); const c = 0.35 + v[k][3] * 0.75; col.push(c, c, c); }
  }
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setAttribute('color', new THREE.Float32BufferAttribute(col, 3));
  g.computeVertexNormals();
  // normalleri yukarı doğru eğ (daha yumuşak ışık)
  const n = g.attributes.normal; for (let i = 0; i < n.count; i++) n.setXYZ(i, n.getX(i) * 0.25, 1, n.getZ(i) * 0.25); g.normalizeNormals();
  return g;
}
function flowerGeo() {
  const g = [];
  const stem = new THREE.CylinderGeometry(0.02, 0.02, 0.5, 3); stem.translate(0, 0.25, 0);
  const head = new THREE.IcosahedronGeometry(0.09, 0); head.translate(0, 0.52, 0);
  const s = prep(stem), h = prep(head);
  const cs = new Float32Array(s.attributes.position.count * 3).fill(0.25); const ch = new Float32Array(h.attributes.position.count * 3).fill(1);
  s.setAttribute('color', new THREE.BufferAttribute(cs, 3)); h.setAttribute('color', new THREE.BufferAttribute(ch, 3));
  return mergeGeometries([s, h]);
}

let PROPS = null;
export function getProps() {
  if (PROPS) return PROPS;
  PROPS = {
    oak: oak(), autumnOak: oak('autumnLeaves'), pine: pine(), darkPine: pine(false, true), snowPine: pine(true), deadTree: deadTree(), cactus: cactus(), palm: palm(),
    bush: bush(), darkBush: bush('darkLeaves'), rock: rock(), bigRock: rock('rock', true), darkRock: rock('darkRock', true), sandRock: rock('sandRock'), snowRock: rock('snowRock', true),
    obsidian: rock('obsidian'), hoodoo: pillarRock(), mushBlue: mushrooms('mushroom'), mushPink: mushrooms('mushroomPink'), iceCrystal: crystals('ice'), fireCrystal: crystals('fireCrystal'),
    log: logProp(), ribs: ribs(),
  };
  return PROPS;
}
export const GRASS_GEO = () => grassClump();
export const FLOWER_GEO = () => flowerGeo();
