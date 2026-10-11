// Araçlar: traktör, kamyonet, yelkenli kayık, at arabası. Ön yön +z.
import * as THREE from 'three';
import * as T from './textures.js';
import { mat, tmat, box, cyl, sphere, cone, rod, beam, dynamic, bake, mergeChildren } from './kit.js';
import { spokedWheel } from './buildings.js';
import { buildAnimalModel } from './animals.js';

const glass = () => mat('#a9c8d6', { transparent: true, opacity: 0.25, roughness: 0.05, metalness: 0.3, side: THREE.DoubleSide });

function tireWheel(r, w, rimColor, rimR) {
  const g = new THREE.Group();
  const tread = T.tire().clone(); tread.needsUpdate = true; tread.repeat.set(Math.round(r * 6), 1);
  const tm = new THREE.MeshStandardMaterial({ map: tread, roughness: 0.95 });
  const tire = new THREE.Mesh(new THREE.CylinderGeometry(r, r, w, 22, 1, true), tm);
  tire.rotation.z = Math.PI / 2; g.add(tire);
  const side = mat('#1d1c1b', { roughness: 0.9 });
  for (const s of [-1, 1]) {
    const ring = new THREE.Mesh(new THREE.RingGeometry(rimR, r, 22), side);
    ring.rotation.y = s * Math.PI / 2; ring.position.x = s * w / 2; g.add(ring);
    const rim = new THREE.Mesh(new THREE.CircleGeometry(rimR, 18), mat(rimColor, { roughness: 0.45, metalness: 0.3 }));
    rim.rotation.y = s * Math.PI / 2; rim.position.x = s * (w / 2 - 0.04); g.add(rim);
    const hub = cyl(g, rimR * 0.35, rimR * 0.4, 0.12, 10, mat(rimColor, { roughness: 0.4, metalness: 0.4 }), s * (w / 2 - 0.02), 0, 0, 0, 0, Math.PI / 2);
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * Math.PI * 2;
      sphere(g, 0.025, mat('#555', { metalness: 0.7 }), s * (w / 2), Math.cos(a) * rimR * 0.55, Math.sin(a) * rimR * 0.55, 1, 1, 1, 0);
    }
  }
  mergeChildren(g);
  return g;
}

// ----------------------------------------------------------------- TRAKTÖR (referans 1)
export function tractor() {
  const g = new THREE.Group();
  const green = tmat(T.paintedMetal('#2f8a3a'), '#ffffff', { roughness: 0.4, metalness: 0.15 });
  const yellow = mat('#f2c230', { roughness: 0.45 });
  const dark = mat('#2c2e2f', { roughness: 0.6, metalness: 0.4 });
  const steel = mat('#8f969c', { roughness: 0.35, metalness: 0.8 });
  // şasi
  box(g, 0.8, 0.45, 3.0, dark, 0, 0.8, 0.2);
  // motor kaputu
  box(g, 1.0, 0.8, 1.7, green, 0, 1.3, 0.95);
  const top = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.5, 1.7, 16, 1, false, -Math.PI / 2, Math.PI), green);
  top.rotation.x = Math.PI / 2; top.position.set(0, 1.7, 0.95); top.scale.set(1, 1, 0.35); g.add(top);
  // ızgara
  box(g, 0.86, 0.72, 0.08, steel, 0, 1.3, 1.81);
  for (let i = 0; i < 7; i++) box(g, 0.05, 0.62, 0.04, dark, -0.33 + i * 0.11, 1.3, 1.86);
  // farlar
  for (const s of [-1, 1]) {
    cyl(g, 0.12, 0.12, 0.1, 14, steel, s * 0.55, 1.45, 1.7, Math.PI / 2);
    const lens = cyl(g, 0.1, 0.1, 0.02, 14, mat('#fffbe6', { emissive: '#fff0b0', emissiveIntensity: 0.2 }), s * 0.55, 1.45, 1.76, Math.PI / 2);
    lens.userData.headlight = true;
    box(g, 0.04, 0.08, 1.6, yellow, s * 0.51, 1.5, 0.95);
  }
  // egzoz
  rod(g, [0.47, 1.7, 1.3], [0.47, 2.7, 1.3], 0.055, steel, 10);
  cyl(g, 0.07, 0.07, 0.14, 10, dark, 0.47, 2.75, 1.3);
  // kabin
  box(g, 1.5, 0.12, 1.5, dark, 0, 1.15, -0.55);
  for (const [x, z] of [[-0.72, 0.18], [0.72, 0.18], [-0.72, -1.28], [0.72, -1.28]]) box(g, 0.08, 1.55, 0.08, green, x, 1.95, z);
  box(g, 1.75, 0.16, 1.75, yellow, 0, 2.78, -0.55);
  box(g, 1.6, 0.06, 1.6, mat('#e0b020'), 0, 2.88, -0.55);
  const gm = glass();
  box(g, 1.38, 1.3, 0.02, gm, 0, 2.0, 0.18);
  box(g, 1.38, 1.3, 0.02, gm, 0, 2.0, -1.28);
  for (const s of [-1, 1]) box(g, 0.02, 1.3, 1.38, gm, s * 0.72, 2.0, -0.55);
  // ayna
  rod(g, [0.75, 2.3, 0.15], [1.0, 2.3, 0.3], 0.02, dark);
  box(g, 0.05, 0.25, 0.18, dark, 1.02, 2.3, 0.3);
  // koltuk ve direksiyon
  box(g, 0.6, 0.12, 0.55, mat('#2a2a2a'), 0, 1.45, -0.75);
  box(g, 0.6, 0.6, 0.1, mat('#2a2a2a'), 0, 1.75, -1.02);
  rod(g, [0, 1.25, -0.05], [0, 1.75, -0.3], 0.03, dark);
  const sw = dynamic(new THREE.Group());
  sw.position.set(0, 1.78, -0.32); sw.rotation.x = -0.9;
  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.2, 0.025, 6, 20), mat('#1a1a1a'));
  sw.add(ring);
  box(sw, 0.38, 0.03, 0.03, mat('#1a1a1a'), 0, 0, 0);
  g.add(sw);
  // basamak
  box(g, 0.25, 0.05, 0.4, dark, 0.85, 0.8, -0.1);
  // çamurluklar
  for (const s of [-1, 1]) {
    const f = new THREE.Mesh(new THREE.CylinderGeometry(0.95, 0.95, 0.62, 16, 1, true, -Math.PI / 2, Math.PI), mat('#2f8a3a', { side: THREE.DoubleSide, roughness: 0.4 }));
    f.rotation.z = Math.PI / 2; f.rotation.y = 0;
    f.position.set(s * 0.98, 0.95, -0.75);
    f.rotation.set(0, 0, Math.PI / 2);
    g.add(f);
    const ff = new THREE.Mesh(new THREE.CylinderGeometry(0.56, 0.56, 0.38, 12, 1, true, -Math.PI / 2, Math.PI * 0.8), mat('#2f8a3a', { side: THREE.DoubleSide, roughness: 0.4 }));
    ff.rotation.set(0, 0, Math.PI / 2); ff.position.set(s * 0.8, 0.55, 1.35);
    g.add(ff);
  }
  // arka çapa ekipmanı
  const hitch = new THREE.Group();
  box(hitch, 1.9, 0.12, 0.25, dark, 0, 0.55, -1.95);
  for (let i = 0; i < 6; i++) {
    const d = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.22, 0.03, 12), steel);
    d.rotation.z = Math.PI / 2; d.position.set(-0.8 + i * 0.32, 0.3, -2.0); d.rotation.y = 0.3;
    hitch.add(d);
  }
  rod(hitch, [-0.3, 0.9, -1.6], [-0.5, 0.55, -1.95], 0.04, dark);
  rod(hitch, [0.3, 0.9, -1.6], [0.5, 0.55, -1.95], 0.04, dark);
  g.add(hitch);
  // tekerlekler
  const wheels = [];
  const rear = [-1, 1].map((s) => { const w = tireWheel(0.85, 0.5, '#f2c230', 0.55); w.position.set(s * 0.98, 0.85, -0.75); g.add(w); return dynamic(w); });
  const front = [-1, 1].map((s) => {
    const piv = dynamic(new THREE.Group()); piv.position.set(s * 0.8, 0.48, 1.35); g.add(piv);
    const w = tireWheel(0.48, 0.3, '#f2c230', 0.3); piv.add(w); return { piv, w };
  });
  // ön ağırlık
  box(g, 0.9, 0.25, 0.25, dark, 0, 0.7, 2.0);
  bake(g);
  const headlights = [];
  g.traverse((o) => { if (o.userData.headlight) headlights.push(o); });
  return {
    group: g, kind: 'tractor', name: 'Traktör',
    seat: new THREE.Vector3(0, 2.25, -0.65),
    radius: 1.6, maxSpeed: 7.5, accel: 4, turn: 0.9, reverse: 3,
    tiller: true,
    exitOffset: new THREE.Vector3(1.8, 0, -0.5),
    animate(dist, steer) {
      for (const w of rear) w.rotation.x += dist / 0.85;
      for (const f of front) { f.w.rotation.x += dist / 0.48; f.piv.rotation.y = steer * 0.5; }
      sw.rotation.z = -steer * 1.5;
    },
  };
}

// ----------------------------------------------------------------- KAMYONET (referans 5)
export function truck() {
  const g = new THREE.Group();
  const paint = tmat(T.rustyPaint('#8fc1b5'), '#ffffff', { roughness: 0.6 });
  const chrome = mat('#c9cfd3', { metalness: 0.85, roughness: 0.25 });
  const dark = mat('#2b2a28', { roughness: 0.7 });
  const wood = tmat(T.planks('#8a5a3a', 6, { weathered: true }));
  // şasi
  box(g, 1.2, 0.3, 5.2, dark, 0, 0.75, -0.2);
  // kaput
  box(g, 1.2, 0.75, 1.5, paint, 0, 1.35, 1.75);
  const hoodTop = new THREE.Mesh(new THREE.CylinderGeometry(0.6, 0.6, 1.5, 14, 1, false, -Math.PI / 2, Math.PI), paint);
  hoodTop.rotation.x = Math.PI / 2; hoodTop.position.set(0, 1.72, 1.75); hoodTop.scale.set(1, 1, 0.3); g.add(hoodTop);
  // ızgara
  box(g, 1.0, 0.8, 0.1, dark, 0, 1.3, 2.52);
  for (let i = 0; i < 9; i++) box(g, 0.05, 0.75, 0.05, chrome, -0.42 + i * 0.105, 1.3, 2.58);
  // tampon
  box(g, 2.2, 0.16, 0.15, paint, 0, 0.7, 2.7);
  // çamurluklar (yuvarlak)
  for (const s of [-1, 1]) {
    const f = new THREE.Mesh(new THREE.CylinderGeometry(0.62, 0.62, 0.42, 14, 1, false, -Math.PI / 2, Math.PI), paint);
    f.rotation.set(0, 0, Math.PI / 2); f.position.set(s * 0.82, 0.75, 1.75); g.add(f);
    box(g, 0.42, 0.06, 1.2, paint, s * 0.82, 0.78, 1.1);
    // farlar
    cyl(g, 0.16, 0.13, 0.18, 14, chrome, s * 0.72, 1.45, 2.42, Math.PI / 2);
    const lens = cyl(g, 0.13, 0.13, 0.02, 14, mat('#fffbe6', { emissive: '#fff0b0', emissiveIntensity: 0.2 }), s * 0.72, 1.45, 2.52, Math.PI / 2);
    lens.userData.headlight = true;
    // basamak
    box(g, 0.3, 0.05, 1.1, dark, s * 1.02, 0.85, 0.3);
  }
  // kabin (panellerden: içeriden görünür)
  const cz = 0.45, cw = 1.8, ch = 1.25, cl = 1.4, cy = 1.25;
  box(g, cw, 0.08, cl, dark, 0, cy, cz);
  box(g, cw + 0.08, 0.1, cl + 0.1, paint, 0, cy + ch + 0.55, cz);
  box(g, cw, ch + 0.5, 0.08, paint, 0, cy + (ch + 0.5) / 2, cz - cl / 2);
  for (const s of [-1, 1]) {
    box(g, 0.08, 0.7, cl, paint, s * cw / 2, cy + 0.35, cz);
    for (const z of [cz - cl / 2 + 0.05, cz + cl / 2 - 0.05]) box(g, 0.08, ch - 0.2 + 0.5, 0.1, paint, s * cw / 2, cy + 0.7 + (ch + 0.3) / 2 - 0.1, z);
    box(g, 0.02, 0.6, cl - 0.2, glass(), s * cw / 2, cy + 1.0, cz);
  }
  box(g, cw, 0.6, 0.05, glass(), 0, cy + 1.0, cz + cl / 2);
  box(g, cw, 0.12, 0.1, paint, 0, cy + 0.68, cz + cl / 2);
  box(g, 0.1, 0.65, 0.1, paint, 0, cy + 1.0, cz + cl / 2);
  box(g, cw, 0.5, 0.06, dark, 0, cy + 0.45, cz + cl / 2 - 0.1);
  // koltuk, direksiyon
  box(g, 1.5, 0.15, 0.55, mat('#5a3a2a'), 0, cy + 0.42, cz - 0.3);
  box(g, 1.5, 0.6, 0.12, mat('#5a3a2a'), 0, cy + 0.8, cz - 0.6);
  const sw = dynamic(new THREE.Group());
  sw.position.set(-0.4, cy + 0.95, cz + 0.35); sw.rotation.x = -1.0;
  sw.add(new THREE.Mesh(new THREE.TorusGeometry(0.2, 0.02, 6, 20), mat('#151515')));
  box(sw, 0.38, 0.025, 0.025, mat('#151515'), 0, 0, 0);
  g.add(sw);
  // yazı (kapı)
  for (const s of [-1, 1]) {
    const p = new THREE.Mesh(new THREE.PlaneGeometry(0.8, 0.25), tmat(T.boardText(['ÇİFTLİK'], '#8fc1b5', '#b0332a'), '#ffffff', { transparent: false }));
    p.position.set(s * (cw / 2 + 0.045), cy + 0.38, cz); p.rotation.y = s * Math.PI / 2;
    g.add(p);
  }
  // kasa
  const bz = -1.55, bl = 2.6;
  box(g, 2.0, 0.12, bl, wood, 0, 1.05, bz);
  for (const s of [-1, 1]) {
    for (const y of [1.3, 1.65]) box(g, 0.06, 0.16, bl, wood, s * 0.98, y, bz);
    for (let i = 0; i <= 4; i++) box(g, 0.09, 0.85, 0.09, wood, s * 0.98, 1.45, bz - bl / 2 + (i * bl) / 4);
  }
  for (const y of [1.3, 1.65]) box(g, 2.0, 0.16, 0.06, wood, 0, y, bz - bl / 2);
  box(g, 2.0, 1.0, 0.06, wood, 0, 1.55, bz + bl / 2);
  // tekerlekler
  const rearW = [-1, 1].map((s) => { const w = dynamic(tireWheel(0.5, 0.35, '#3a3a38', 0.3)); w.position.set(s * 0.85, 0.5, -1.7); g.add(w); return w; });
  const front = [-1, 1].map((s) => {
    const piv = dynamic(new THREE.Group()); piv.position.set(s * 0.82, 0.5, 1.75); g.add(piv);
    const w = tireWheel(0.5, 0.3, '#3a3a38', 0.3); piv.add(w); return { piv, w };
  });
  bake(g);
  return {
    group: g, kind: 'truck', name: 'Kamyonet',
    seat: new THREE.Vector3(-0.4, 2.25, 0.25),
    radius: 2.0, maxSpeed: 17, accel: 6, turn: 0.75, reverse: 4,
    exitOffset: new THREE.Vector3(-2.0, 0, 0.3),
    animate(dist, steer) {
      for (const w of rearW) w.rotation.x += dist / 0.5;
      for (const f of front) { f.w.rotation.x += dist / 0.5; f.piv.rotation.y = steer * 0.45; }
      sw.rotation.z = -steer * 1.8;
    },
  };
}

// ----------------------------------------------------------------- YELKENLİ KAYIK (referans 4)
export function boat() {
  const g = new THREE.Group();
  const hullM = tmat(T.planks('#9a6a3e', 8), '#ffffff', { side: THREE.DoubleSide });
  const blue = mat('#2f7fc4', { roughness: 0.5 });
  const white = mat('#efe9dc');
  // gövde (bölümlerden örülmüş)
  const sections = [
    [-2.3, 0.75, 0.45], [-1.6, 0.95, 0.55], [-0.6, 1.05, 0.6], [0.6, 1.0, 0.6], [1.6, 0.75, 0.55], [2.3, 0.35, 0.5], [2.7, 0.02, 0.45],
  ];
  const pos = [], uv = [], idx = [];
  const ring = (z, w, d) => [
    [-w, 0.35, z], [-w * 0.85, -d * 0.55, z], [0, -d, z], [w * 0.85, -d * 0.55, z], [w, 0.35, z],
  ];
  sections.forEach(([z, w, d], i) => {
    ring(z, w, d).forEach((p, k) => { pos.push(...p); uv.push(k / 2, z / 1.2); });
  });
  for (let i = 0; i < sections.length - 1; i++) {
    for (let k = 0; k < 4; k++) {
      const a = i * 5 + k, b = (i + 1) * 5 + k;
      idx.push(a, b, a + 1, b, b + 1, a + 1);
    }
  }
  // kıç ayna
  idx.push(0, 1, 4, 1, 3, 4, 1, 2, 3);
  const hg = new THREE.BufferGeometry();
  hg.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  hg.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  hg.setIndex(idx); hg.computeVertexNormals();
  g.add(new THREE.Mesh(hg, hullM));
  // mavi küpeşte
  for (let i = 0; i < sections.length - 1; i++) {
    for (const s of [-1, 1]) {
      const a = sections[i], b = sections[i + 1];
      beam(g, [s * a[1], 0.4, a[0]], [s * b[1], 0.4, b[0]], 0.1, 0.12, blue);
    }
  }
  beam(g, [-0.75, 0.4, -2.3], [0.75, 0.4, -2.3], 0.1, 0.12, blue);
  box(g, 1.7, 0.06, 4.2, tmat(T.planks('#b88a5a', 8, { nails: false })), 0, 0.05, 0.0);
  // oturak ve kabin
  box(g, 1.8, 0.08, 0.35, tmat(T.planks('#8a5a34', 4)), 0, 0.35, 0.9);
  box(g, 1.2, 0.7, 1.0, white, 0, 0.45, -1.3);
  box(g, 1.3, 0.12, 1.1, blue, 0, 0.85, -1.3);
  for (let i = 0; i < 5; i++) box(g, 0.08, 0.6, 0.04, blue, -0.48 + i * 0.24, 0.45, -0.78);
  // direk + seren + yelken
  const wood = mat('#c99a5a');
  cyl(g, 0.07, 0.09, 4.6, 8, wood, 0, 2.3, 0.4);
  const sailG = dynamic(new THREE.Group());
  sailG.position.set(0, 0, 0.4);
  rod(sailG, [-1.6, 4.3, 0], [1.7, 3.5, 0], 0.05, wood);
  rod(sailG, [-1.4, 1.2, 0], [1.6, 1.3, 0], 0.04, wood);
  const sgeo = new THREE.BufferGeometry();
  sgeo.setAttribute('position', new THREE.Float32BufferAttribute([-1.55, 4.25, 0.02, 1.65, 3.48, 0.02, 1.55, 1.35, 0.15, -1.35, 1.25, 0.15], 3));
  sgeo.setAttribute('uv', new THREE.Float32BufferAttribute([0, 1, 1, 1, 1, 0, 0, 0], 2));
  sgeo.setIndex([0, 3, 1, 1, 3, 2]); sgeo.computeVertexNormals();
  const sail = new THREE.Mesh(sgeo, tmat(T.sail(), '#ffffff', { side: THREE.DoubleSide, roughness: 1 }));
  sailG.add(sail);
  g.add(sailG);
  // kürekler
  for (const s of [-1, 1]) {
    rod(g, [s * 0.9, 0.45, 0.2], [s * 2.0, -0.1, -0.6], 0.035, wood);
    box(g, 0.08, 0.04, 0.5, wood, s * 2.05, -0.12, -0.75, 0, s * 0.6, 0);
  }
  // fener
  rod(g, [0.7, 0.4, -2.2], [0.9, 1.3, -2.5], 0.03, mat('#3a3a3a'));
  const lan = dynamic(box(g, 0.2, 0.28, 0.2, mat('#ffd27a', { emissive: '#ffb347', emissiveIntensity: 0 }), 0.92, 1.2, -2.55));
  const light = new THREE.PointLight(0xffc070, 0, 12, 1.8);
  light.position.set(0.92, 1.1, -2.55); g.add(light);
  bake(g);
  return {
    group: g, kind: 'boat', name: 'Yelkenli Kayık',
    seat: new THREE.Vector3(0, 1.35, -0.2),
    radius: 1.6, maxSpeed: 6, accel: 2, turn: 0.7, reverse: 2,
    water: true,
    exitOffset: new THREE.Vector3(1.8, 0, 0),
    animate(dist, steer, ctx) {
      sailG.rotation.y = Math.sin((ctx?.time || 0) * 0.5) * 0.1 + steer * 0.3;
      sail.scale.z = 1 + Math.sin((ctx?.time || 0) * 2) * 0.05;
      lan.material.emissiveIntensity = ctx?.night ? 2 : 0;
      light.intensity = ctx?.night ? 4 : 0;
    },
  };
}

// ----------------------------------------------------------------- AT ARABASI (referans 9)
export function horseCart() {
  const g = new THREE.Group();
  const wood = tmat(T.planks('#9a7a58', 6, { weathered: true }));
  const dark = tmat(T.planks('#6e5034', 4));
  // kasa
  const cz = -1.6;
  box(g, 1.6, 0.1, 2.6, wood, 0, 1.05, cz);
  for (const s of [-1, 1]) for (const y of [1.25, 1.5, 1.75]) box(g, 0.06, 0.2, 2.6, wood, s * 0.8, y, cz);
  for (const y of [1.25, 1.5, 1.75]) box(g, 1.6, 0.2, 0.06, wood, 0, y, cz - 1.3);
  box(g, 1.6, 0.8, 0.06, wood, 0, 1.45, cz + 1.3);
  box(g, 1.5, 0.12, 0.45, dark, 0, 1.55, cz + 0.95);
  // oklar (ata bağlanan çubuklar)
  for (const s of [-1, 1]) rod(g, [s * 0.5, 1.0, cz + 1.3], [s * 0.45, 1.05, 2.0], 0.05, dark);
  rod(g, [-0.5, 1.0, cz + 1.3], [0.5, 1.0, cz + 1.3], 0.05, dark);
  const wheels = [-1, 1].map((s) => {
    const w = dynamic(spokedWheel(0.85, '#8a6a48'));
    w.position.set(s * 0.95, 0.85, cz);
    w.rotation.y = Math.PI / 2;
    g.add(w);
    return w;
  });
  // at
  const horse = buildAnimalModel('at');
  horse.root.position.set(0, 0, 1.3);
  dynamic(horse.root);
  g.add(horse.root);
  // koşum
  box(horse.body, 0.7, 0.12, 0.25, mat('#3a2a1e'), 0, 0.38, 0.35);
  box(horse.body, 0.4, 0.06, 0.4, mat('#d9b04a'), 0, 0.4, 0.0);
  bake(g);
  let phase = 0;
  return {
    group: g, kind: 'horsecart', name: 'At Arabası',
    seat: new THREE.Vector3(0, 2.4, cz + 0.85),
    radius: 1.8, maxSpeed: 9, accel: 3, turn: 0.9, reverse: 1.5,
    exitOffset: new THREE.Vector3(1.8, 0, -1),
    animate(dist, steer, ctx, speed) {
      for (const w of wheels) w.rotation.z -= dist / 0.85;
      phase += dist * 2.2;
      const P = horse;
      const sw = Math.abs(speed) > 0.1 ? Math.sin(phase) : 0;
      P.legs[0].rotation.x = sw * 0.6; P.legs[3].rotation.x = sw * 0.6;
      P.legs[1].rotation.x = -sw * 0.6; P.legs[2].rotation.x = -sw * 0.6;
      P.neck.rotation.x = -0.85 + Math.sin(phase * 2) * 0.06;
      P.tail.rotation.z = Math.sin((ctx?.time || 0) * 2) * 0.3;
    },
  };
}
