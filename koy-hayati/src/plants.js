// Saksı bitkileri (referans 10) ve ekin modelleri
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import * as T from './textures.js';
import { mat, tmat, box, cyl, sphere, cone, rod, bake } from './kit.js';
import { mulberry32 } from './util.js';

// Kavisli yaprak geometrisi: taban orijinde, +y boyunca uzanır
export function leafGeo(w, l, bend = 0.3, segs = 5, heart = false) {
  const g = new THREE.PlaneGeometry(w, l, 2, segs);
  g.translate(0, l / 2, 0);
  const p = g.attributes.position;
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i);
    const t = y / l;
    let width = Math.sin(Math.PI * Math.min(1, t * (heart ? 1.0 : 1.0))) ;
    if (heart) width = Math.sin(Math.PI * (0.15 + t * 0.85)) * (1 - t * 0.2);
    p.setX(i, x * Math.max(0.05, width));
    p.setZ(i, bend * t * t * l + Math.abs(x) * 0.25);
  }
  g.computeVertexNormals();
  return g;
}

function mergeMeshes(list) {
  const geos = list.map((m) => {
    m.updateMatrix();
    let g = m.geometry.index ? m.geometry.toNonIndexed() : m.geometry.clone();
    for (const k of Object.keys(g.attributes)) if (!['position', 'normal', 'uv'].includes(k)) g.deleteAttribute(k);
    g.applyMatrix4(m.matrix);
    return g;
  });
  return mergeGeometries(geos, false);
}

export function pottedPlant(type, seed = 1) {
  const r = mulberry32(seed + type.length * 13);
  const g = new THREE.Group();
  const potM = mat('#f4f2ee', { roughness: 0.5 });
  const potH = type === 'cactus' || type === 'opuntia' ? 0.28 : 0.32;
  if (type === 'caladium') {
    cyl(g, 0.24, 0.16, 0.3, 14, potM, 0, 0.15, 0);
    for (let i = 0; i < 3; i++) {
      const tr = new THREE.Mesh(new THREE.TorusGeometry(0.2 + i * 0.015, 0.008, 4, 20), mat('#e2dfda'));
      tr.rotation.x = Math.PI / 2; tr.position.y = 0.08 + i * 0.08; g.add(tr);
    }
  } else if (type === 'snake' || type === 'rosemary') {
    box(g, 0.32, potH, 0.32, potM, 0, potH / 2, 0);
  } else {
    cyl(g, 0.19, 0.14, potH, 14, potM, 0, potH / 2, 0);
  }
  const soil = new THREE.Mesh(new THREE.CircleGeometry(0.17, 12), mat('#3e2a1c'));
  soil.rotation.x = -Math.PI / 2; soil.position.y = potH - 0.02; g.add(soil);
  const top = potH - 0.02;
  const side = THREE.DoubleSide;
  const leafMesh = (geo, color, x, y, z, rx, ry, rz, s = 1) => {
    const m = new THREE.Mesh(geo, mat(color, { side, roughness: 0.6 }));
    m.position.set(x, y, z); m.rotation.set(rx, ry, rz, 'YXZ'); m.scale.setScalar(s);
    g.add(m); return m;
  };
  switch (type) {
    case 'monstera': {
      const lg = leafGeo(0.42, 0.42, -0.2, 5, true);
      for (let i = 0; i < 7; i++) {
        const a = (i / 7) * Math.PI * 2 + r();
        const h = 0.25 + r() * 0.35;
        rod(g, [0, top, 0], [Math.cos(a) * 0.2, top + h, Math.sin(a) * 0.2], 0.012, mat('#4c7a32'));
        leafMesh(lg, '#2f6b2a', Math.cos(a) * 0.2, top + h, Math.sin(a) * 0.2, 0.9 + r() * 0.4, -a + Math.PI / 2, 0, 0.9 + r() * 0.4);
      }
      break;
    }
    case 'palm': {
      const lg = leafGeo(0.12, 0.75, 0.6, 6);
      for (let i = 0; i < 9; i++) {
        const a = (i / 9) * Math.PI * 2 + r() * 0.4;
        const h = 0.3 + r() * 0.3;
        rod(g, [0, top, 0], [Math.cos(a) * 0.06, top + h, Math.sin(a) * 0.06], 0.012, mat('#8aa83a'));
        for (let k = 0; k < 2; k++) leafMesh(lg, '#5f9e3a', Math.cos(a) * 0.06, top + h, Math.sin(a) * 0.06, 0.6 + k * 0.4, -a + Math.PI / 2 + (k - 0.5) * 0.3, 0);
      }
      break;
    }
    case 'cactus': {
      const cm = mat('#5a8a4a', { flat: true, roughness: 0.6 });
      cyl(g, 0.09, 0.1, 0.8, 8, cm, 0, top + 0.4, 0);
      sphere(g, 0.09, cm, 0, top + 0.8, 0, 1, 0.6, 1, 1);
      for (const [s, h] of [[-1, 0.35], [1, 0.5]]) {
        cyl(g, 0.055, 0.06, 0.18, 8, cm, s * 0.13, top + h, 0, 0, 0, s * Math.PI / 2);
        cyl(g, 0.055, 0.06, 0.3, 8, cm, s * 0.21, top + h + 0.15, 0);
        sphere(g, 0.055, cm, s * 0.21, top + h + 0.3, 0, 1, 0.6, 1, 1);
      }
      for (let i = 0; i < 20; i++) sphere(g, 0.012, mat('#f0e6c0'), Math.cos(i) * 0.1, top + 0.1 + (i / 20) * 0.7, Math.sin(i) * 0.1, 1, 1, 1, 0);
      break;
    }
    case 'opuntia': {
      const cm = mat('#7aa060', { flat: true, roughness: 0.6 });
      const pads = [[0, 0.22, 0, 0, 0.18], [0.12, 0.48, 0.02, 0.5, 0.14], [-0.1, 0.5, -0.02, -0.4, 0.13], [0.2, 0.7, 0, 0.8, 0.1], [-0.05, 0.75, 0.02, -0.1, 0.11]];
      for (const [x, y, z, rz, s] of pads) {
        const p = sphere(g, s, cm, x, top + y, z, 0.85, 1.25, 0.28, 1);
        p.rotation.z = rz;
      }
      break;
    }
    case 'fern': {
      const lg = leafGeo(0.14, 0.55, 0.55, 6);
      for (let i = 0; i < 16; i++) {
        const a = (i / 16) * Math.PI * 2 + r() * 0.3;
        leafMesh(lg, i % 2 ? '#5aa83f' : '#4f9a38', 0, top + 0.02, 0, 0.5 + r() * 0.5, -a, 0);
      }
      break;
    }
    case 'bonsai': {
      const bm = tmat(T.bark('#6b4a2f'));
      rod(g, [0, top, 0], [0.08, top + 0.25, 0.02], 0.045, bm);
      rod(g, [0.08, top + 0.25, 0.02], [-0.08, top + 0.45, 0], 0.04, bm);
      rod(g, [-0.08, top + 0.45, 0], [0.05, top + 0.62, 0], 0.03, bm);
      const lm = mat('#4f8a3a', { flat: true });
      for (const [x, y, z, s] of [[0.18, 0.3, 0, 0.12], [-0.18, 0.48, 0.03, 0.13], [0.08, 0.68, 0, 0.14], [0.0, 0.42, 0.15, 0.1]]) sphere(g, s, lm, x, top + y, z, 1.4, 0.6, 1.2, 1);
      break;
    }
    case 'snake': {
      const lg = leafGeo(0.09, 0.7, 0.05, 4);
      for (let i = 0; i < 9; i++) {
        const a = r() * Math.PI * 2;
        leafMesh(lg, i % 3 ? '#3f6b2f' : '#6a8a3a', Math.cos(a) * 0.06, top, Math.sin(a) * 0.06, 0.05 + r() * 0.15, a, 0, 0.7 + r() * 0.5);
      }
      break;
    }
    case 'caladium': {
      const lg = leafGeo(0.32, 0.34, -0.15, 5, true);
      for (let i = 0; i < 9; i++) {
        const a = (i / 9) * Math.PI * 2 + r() * 0.3;
        const h = 0.15 + r() * 0.15;
        rod(g, [0, top, 0], [Math.cos(a) * 0.15, top + h, Math.sin(a) * 0.15], 0.01, mat('#b84a6a'));
        leafMesh(lg, i % 2 ? '#d86a8a' : '#c25a7a', Math.cos(a) * 0.15, top + h, Math.sin(a) * 0.15, 0.9 + r() * 0.4, -a + Math.PI / 2, 0);
      }
      break;
    }
    case 'olive': {
      rod(g, [0, top, 0], [0.02, top + 0.7, 0], 0.025, tmat(T.bark('#8a7a64')));
      const lm = mat('#8a9a6a', { flat: true });
      for (let i = 0; i < 7; i++) sphere(g, 0.1, lm, (r() - 0.5) * 0.3, top + 0.55 + r() * 0.35, (r() - 0.5) * 0.3, 1, 0.8, 1, 1);
      break;
    }
    case 'rosemary': {
      const lm = mat('#4a6a4a', { flat: true });
      for (let i = 0; i < 22; i++) {
        const x = (r() - 0.5) * 0.25, z = (r() - 0.5) * 0.25;
        cone(g, 0.03, 0.5 + r() * 0.3, 4, lm, x, top + 0.3, z, (r() - 0.5) * 0.3, 0, (r() - 0.5) * 0.3);
      }
      break;
    }
  }
  g.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } });
  bake(g);
  return g;
}

// ---------------- Ekin modelleri: { parts: [{geo, mat, kind:'leaf'|'fruit'}] }
// Bütün parçalar 2m karelik tarla hücresinin merkezine göre tanımlanır.
const leafCol = '#ffffff';
function grid(fn, n = 3, spacing = 0.55) {
  const out = [];
  for (let i = 0; i < n; i++) for (let j = 0; j < n; j++) out.push(fn((i - (n - 1) / 2) * spacing, (j - (n - 1) / 2) * spacing, i * n + j));
  return out;
}
const Mm = (geo, x = 0, y = 0, z = 0, sx = 1, sy = 1, sz = 1, rx = 0, ry = 0, rz = 0) => {
  const m = new THREE.Mesh(geo); m.position.set(x, y, z); m.scale.set(sx, sy, sz); m.rotation.set(rx, ry, rz); return m;
};

export function cropModels() {
  const r = mulberry32(5);
  const ico = new THREE.IcosahedronGeometry(1, 0);
  const ico1 = new THREE.IcosahedronGeometry(1, 1);
  const L = (w, l, b) => leafGeo(w, l, b, 3);
  const models = {};
  models.havuc = {
    leaf: mergeMeshes(grid((x, z) => [0, 1, 2, 3, 4].map((k) => Mm(L(0.06, 0.35, 0.15), x, 0, z, 1, 1, 1, 0.3, k * 1.25, 0))).flat()),
    fruit: mergeMeshes(grid((x, z) => Mm(new THREE.ConeGeometry(0.06, 0.12, 6), x, 0.02, z, 1, 1, 1, Math.PI, 0, 0))),
    fruitColor: '#e8742a', ripeLeaf: '#4f9a38',
  };
  models.patates = {
    leaf: mergeMeshes(grid((x, z) => [Mm(ico, x, 0.18, z, 0.22, 0.16, 0.22), Mm(ico, x + 0.08, 0.26, z, 0.14, 0.1, 0.14)], 3, 0.6).flat()),
    fruit: mergeMeshes(grid((x, z) => [0, 1, 2].map((k) => Mm(ico, x + Math.cos(k * 2) * 0.1, 0.33, z + Math.sin(k * 2) * 0.1, 0.03, 0.03, 0.03)), 3, 0.6).flat()),
    fruitColor: '#ffffff', ripeLeaf: '#5a8a3a',
  };
  models.bugday = {
    leaf: mergeMeshes(grid((x, z) => [0, 1, 2, 3, 4, 5].map((k) => Mm(new THREE.BoxGeometry(0.015, 0.8, 0.015), x + Math.cos(k) * 0.07, 0.4, z + Math.sin(k * 1.3) * 0.07, 1, 1, 1, Math.cos(k) * 0.08, 0, Math.sin(k) * 0.08)), 4, 0.42).flat()),
    fruit: mergeMeshes(grid((x, z) => [0, 1, 2, 3, 4, 5].map((k) => Mm(new THREE.BoxGeometry(0.04, 0.16, 0.04), x + Math.cos(k) * 0.07 + Math.cos(k) * 0.06, 0.86, z + Math.sin(k * 1.3) * 0.07 + Math.sin(k) * 0.06, 1, 1, 1, Math.cos(k) * 0.08, 0, Math.sin(k) * 0.08)), 4, 0.42).flat()),
    fruitColor: '#e8c25a', ripeLeaf: '#d9b04a',
  };
  models.domates = {
    leaf: mergeMeshes(grid((x, z) => [Mm(new THREE.CylinderGeometry(0.015, 0.015, 0.8, 4), x, 0.4, z), Mm(ico1, x, 0.45, z, 0.25, 0.4, 0.25), Mm(ico1, x + 0.08, 0.7, z, 0.18, 0.22, 0.18)], 2, 0.9).flat()),
    fruit: mergeMeshes(grid((x, z) => [0, 1, 2, 3, 4, 5].map((k) => Mm(ico1, x + Math.cos(k * 1.1) * 0.22, 0.3 + (k % 3) * 0.17, z + Math.sin(k * 1.1) * 0.22, 0.065, 0.06, 0.065)), 2, 0.9).flat()),
    fruitColor: '#d6342a', ripeLeaf: '#3f8a2f',
  };
  models.lahana = {
    leaf: mergeMeshes(grid((x, z) => [0, 1, 2, 3, 4].map((k) => Mm(L(0.3, 0.3, -0.25), x, 0.02, z, 1, 1, 1, 1.2, k * 1.25, 0)), 2, 0.9).flat()),
    fruit: mergeMeshes(grid((x, z) => Mm(ico1, x, 0.2, z, 0.24, 0.21, 0.24), 2, 0.9)),
    fruitColor: '#b7da8f', ripeLeaf: '#7ab85a',
  };
  models.misir = {
    leaf: mergeMeshes(grid((x, z) => [Mm(new THREE.CylinderGeometry(0.025, 0.035, 1.9, 5), x, 0.95, z), ...[0, 1, 2, 3].map((k) => Mm(L(0.1, 0.7, 0.4), x, 0.5 + k * 0.32, z, 1, 1, 1, 0.9, k * 1.9, 0))], 2, 0.9).flat()),
    fruit: mergeMeshes(grid((x, z) => [Mm(new THREE.CylinderGeometry(0.05, 0.04, 0.3, 6), x + 0.07, 1.05, z, 1, 1, 1, 0, 0, -0.35), Mm(new THREE.CylinderGeometry(0.05, 0.04, 0.3, 6), x - 0.07, 1.25, z + 0.03, 1, 1, 1, 0, 0, 0.35)], 2, 0.9).flat()),
    fruitColor: '#f2cf4a', ripeLeaf: '#6a9a3a',
  };
  models.karpuz = {
    leaf: mergeMeshes(Array.from({ length: 12 }, (_, k) => Mm(L(0.28, 0.32, -0.3), Math.cos(k) * 0.6, 0.02, Math.sin(k * 1.7) * 0.6, 1, 1, 1, 1.3, k * 0.9, 0))),
    fruit: mergeMeshes([Mm(ico1, 0.1, 0.22, 0.05, 0.3, 0.24, 0.42, 0, 0.4, 0)]),
    fruitColor: '#3f8a35', ripeLeaf: '#4a8a35', fruitTex: true,
  };
  models.kabak = {
    leaf: mergeMeshes(Array.from({ length: 10 }, (_, k) => Mm(L(0.32, 0.34, -0.3), Math.cos(k * 1.3) * 0.6, 0.02, Math.sin(k * 1.9) * 0.6, 1, 1, 1, 1.3, k * 1.1, 0))),
    fruit: mergeMeshes([...Array.from({ length: 8 }, (_, k) => Mm(ico1, Math.cos(k * 0.785) * 0.12, 0.24, Math.sin(k * 0.785) * 0.12, 0.17, 0.22, 0.17)), Mm(new THREE.CylinderGeometry(0.03, 0.04, 0.12, 5), 0, 0.48, 0)]),
    fruitColor: '#e8802a', ripeLeaf: '#5a8a35',
  };
  return models;
}
