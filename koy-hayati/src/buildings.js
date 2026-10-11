// Binalar ve sabit eşyalar. Her kurucu { group, colliders, interacts, update } döndürür.
// colliders: yerel koordinatta [minX, maxX, minZ, maxZ]; ön yüz +z yönüne bakar.
import * as THREE from 'three';
import * as T from './textures.js';
import { mat, tmat, box, cyl, sphere, cone, rod, beam, prism, dynamic, bake, mergeChildren } from './kit.js';
import { mulberry32 } from './util.js';

const glassMat = () => mat('#9fc4d8', { roughness: 0.15, metalness: 0.2, transparent: true, opacity: 0.55 });
const glowGlass = () => mat('#ffd88a', { emissive: '#ffb347', emissiveIntensity: 0, roughness: 0.3 });
export const windowGlowMats = [];

function windowFrame(g, x, y, z, w, h, ry = 0, frameColor = '#efe9dc', glow = true) {
  const fm = mat(frameColor);
  const grp = new THREE.Group();
  grp.position.set(x, y, z); grp.rotation.y = ry;
  box(grp, w + 0.2, 0.12, 0.12, fm, 0, h / 2 + 0.04, 0);
  box(grp, w + 0.3, 0.1, 0.22, fm, 0, -h / 2 - 0.04, 0.05);
  box(grp, 0.1, h, 0.12, fm, -w / 2 - 0.04, 0, 0);
  box(grp, 0.1, h, 0.12, fm, w / 2 + 0.04, 0, 0);
  box(grp, 0.05, h, 0.08, fm, 0, 0, 0.02);
  box(grp, w, 0.05, 0.08, fm, 0, 0, 0.02);
  const gm = glow ? glowGlass() : glassMat();
  if (glow && !windowGlowMats.includes(gm)) windowGlowMats.push(gm);
  box(grp, w, h, 0.04, gm, 0, 0, -0.03);
  g.add(grp);
  return grp;
}

function shutters(g, x, y, z, w, h, color) {
  const m = tmat(T.planks(color, 4, { nails: false }));
  box(g, w * 0.5, h, 0.05, m, x - w * 0.78, y, z);
  box(g, w * 0.5, h, 0.05, m, x + w * 0.78, y, z);
}

// ----------------------------------------------------------------- ÇİFTLİK EVİ
export function farmhouse() {
  const g = new THREE.Group();
  const W = 12, D = 9, H1 = 3.0, H2 = 2.6, t = 0.22, RH = 2.6;
  const wall = tmat(T.planks('#8b5a3c', 8));
  const trim = mat('#4e3322');
  const stoneM = tmat(T.stone('#8d8a83'));
  const roofM = tmat(T.roofTiles('#8c5a45'));
  const floorM = tmat(T.planks('#b07a48', 6, { nails: false }));
  const inner = tmat(T.plaster('#efe2c8'));

  // temel
  box(g, W + 0.3, 0.45, D + 0.3, stoneM, 0, -0.2, 0, 0, 0, 0, 2);
  box(g, W - 0.3, 0.1, D - 0.3, floorM, 0, 0.04, 0, 0, 0, 0, 2.5);
  // zemin kat duvarları (dış + iç sıva)
  const walls = [
    [W, H1, t, 0, H1 / 2, -D / 2 + t / 2],
    [t, H1, D, -W / 2 + t / 2, H1 / 2, 0],
    [t, H1, D, W / 2 - t / 2, H1 / 2, 0],
    [W / 2 - 0.7, H1, t, -(0.7 + (W / 2 - 0.7) / 2), H1 / 2, D / 2 - t / 2],
    [W / 2 - 0.7, H1, t, 0.7 + (W / 2 - 0.7) / 2, H1 / 2, D / 2 - t / 2],
    [1.4, H1 - 2.3, t, 0, 2.3 + (H1 - 2.3) / 2, D / 2 - t / 2],
  ];
  for (const w of walls) box(g, w[0], w[1], w[2], wall, w[3], w[4], w[5], 0, 0, 0, 2.2);
  // iç sıva yüzleri
  box(g, W - 2 * t, H1 - 0.1, 0.02, inner, 0, H1 / 2, -D / 2 + t + 0.01, 0, 0, 0, 2);
  box(g, 0.02, H1 - 0.1, D - 2 * t, inner, -W / 2 + t + 0.01, H1 / 2, 0, 0, 0, 0, 2);
  box(g, 0.02, H1 - 0.1, D - 2 * t, inner, W / 2 - t - 0.01, H1 / 2, 0, 0, 0, 0, 2);
  // köşe dikmeleri
  for (const sx of [-1, 1]) for (const sz of [-1, 1]) box(g, 0.3, H1 + H2, 0.3, trim, sx * W / 2, (H1 + H2) / 2, sz * D / 2);
  // üst kat (kapalı kutu, alt yüzü tavan)
  box(g, W, H2, D, wall, 0, H1 + H2 / 2, 0, 0, 0, 0, 2.2);
  box(g, W - 2 * t, 0.08, D - 2 * t, tmat(T.planks('#9a6a3c', 8, { nails: false })), 0, H1 - 0.04, 0, 0, 0, 0, 2);
  box(g, W + 0.25, 0.18, D + 0.25, trim, 0, H1, 0);
  // kalkan duvarlar
  for (const sx of [-1, 1]) {
    prism(g, [[-D / 2, 0], [D / 2, 0], [0, RH]], t, wall, sx * (W / 2 - t / 2), H1 + H2, 0, Math.PI / 2, 2);
  }
  // çatı
  const half = D / 2 + 0.6;
  const ang = Math.atan2(RH, half);
  const len = Math.hypot(half, RH) + 0.1;
  for (const s of [-1, 1]) {
    const r = box(g, W + 1.0, 0.16, len, roofM, 0, H1 + H2 + RH / 2 - 0.05, s * half / 2, s * ang, 0, 0, 1.6);
    r.rotation.x = s * ang;
  }
  box(g, W + 1.1, 0.22, 0.3, trim, 0, H1 + H2 + RH + 0.02, 0);
  // baca
  const brick = tmat(T.bricks('#8e4a35'));
  box(g, 0.9, 3.4, 0.9, brick, W / 2 - 2, H1 + H2 + 1.6, -1.2, 0, 0, 0, 1.5);
  box(g, 1.1, 0.18, 1.1, trim, W / 2 - 2, H1 + H2 + 3.3, -1.2);

  // pencereler
  for (const x of [-3.6, 3.6]) windowFrame(g, x, 1.7, D / 2 + 0.03, 1.3, 1.3);
  for (const x of [-3.6, 0, 3.6]) windowFrame(g, x, H1 + 1.3, D / 2 + 0.03, 1.1, 1.2);
  for (const z of [-2, 2]) {
    windowFrame(g, -W / 2 - 0.03, 1.7, z, 1.2, 1.2, -Math.PI / 2);
    windowFrame(g, W / 2 + 0.03, 1.7, z, 1.2, 1.2, Math.PI / 2);
  }
  windowFrame(g, 0, 1.7, -D / 2 - 0.03, 1.2, 1.2, Math.PI);
  for (const x of [-3.6, 3.6]) shutters(g, x, 1.7, D / 2 + 0.06, 1.3, 1.4, '#3f6b4a');
  // tabela
  const sign = box(g, 1.6, 0.5, 0.06, tmat(T.boardText(['Çiftlik Evi'])), -2, 2.55, D / 2 + 3.0);
  sign.userData.noBake = false;

  // veranda
  const deck = tmat(T.planks('#9a6a3f', 6));
  box(g, W + 0.4, 0.2, 3.2, deck, 0, 0.0, D / 2 + 1.6, 0, 0, 0, 2);
  box(g, 1.6, 0.12, 0.6, deck, 0, -0.1, D / 2 + 3.5);
  const postX = [-W / 2, -W / 4 - 0.5, -1.0, 1.0, W / 4 + 0.5, W / 2];
  for (const x of postX) box(g, 0.2, 2.7, 0.2, trim, x, 1.35, D / 2 + 3.05);
  const pr = box(g, W + 0.8, 0.12, 3.6, roofM, 0, 2.85, D / 2 + 1.7, 0.12, 0, 0, 1.6);
  // korkuluk
  const rail = mat('#e6dccb');
  for (const [a, b] of [[-W / 2, -1.0], [1.0, W / 2]]) {
    box(g, b - a, 0.08, 0.1, rail, (a + b) / 2, 0.95, D / 2 + 3.05);
    box(g, b - a, 0.06, 0.08, rail, (a + b) / 2, 0.3, D / 2 + 3.05);
    for (let x = a + 0.25; x < b; x += 0.32) box(g, 0.05, 0.65, 0.05, rail, x, 0.62, D / 2 + 3.05);
  }
  for (const sx of [-1, 1]) {
    box(g, 0.1, 0.08, 3.1, rail, sx * W / 2, 0.95, D / 2 + 1.55);
    for (let z = D / 2 + 0.2; z < D / 2 + 3; z += 0.32) box(g, 0.05, 0.65, 0.05, rail, sx * W / 2, 0.62, z);
  }
  // veranda sallanan sandalye ve saksılar
  const chairM = tmat(T.planks('#7a4a2a', 4, { nails: false }));
  box(g, 0.6, 0.06, 0.6, chairM, -4.2, 0.5, D / 2 + 1.8);
  box(g, 0.6, 0.8, 0.06, chairM, -4.2, 0.9, D / 2 + 1.5, -0.15);

  // --- İç mekân ---
  const rugM = tmat(T.rug());
  box(g, 3.4, 0.02, 2.4, rugM, 0.8, 0.1, 0.6);
  // yatak
  const bedWood = tmat(T.planks('#6b3f22', 3, { nails: false }));
  box(g, 1.6, 0.4, 2.3, bedWood, -4.5, 0.3, -2.9);
  box(g, 1.5, 0.22, 2.2, mat('#f3efe6'), -4.5, 0.6, -2.9);
  box(g, 1.55, 0.12, 1.5, tmat(T.stripes('#a33a2c', '#e8d4a8', 10)), -4.5, 0.74, -2.4);
  box(g, 1.1, 0.16, 0.45, mat('#ffffff'), -4.5, 0.78, -3.75);
  box(g, 1.7, 1.2, 0.1, bedWood, -4.5, 0.6, -4.05);
  // komodin + lamba
  box(g, 0.6, 0.6, 0.5, bedWood, -3.2, 0.35, -3.9);
  cyl(g, 0.08, 0.1, 0.3, 8, mat('#c9b48a'), -3.2, 0.8, -3.9);
  cone(g, 0.22, 0.25, 8, mat('#f2e2b5', { emissive: '#ffcf7a', emissiveIntensity: 0.4 }), -3.2, 1.05, -3.9);
  // soba
  const iron = mat('#2b2a29', { roughness: 0.6, metalness: 0.4 });
  box(g, 1.1, 0.9, 0.8, iron, 4.7, 0.5, -3.6);
  box(g, 1.2, 0.08, 0.9, iron, 4.7, 0.98, -3.6);
  const fire = box(g, 0.5, 0.3, 0.02, mat('#ff8a2a', { emissive: '#ff6a00', emissiveIntensity: 1.5 }), 4.7, 0.45, -3.19);
  cyl(g, 0.12, 0.12, 2.0, 8, iron, 4.9, 2.0, -3.7);
  // tencere
  cyl(g, 0.2, 0.18, 0.22, 10, mat('#8a8f94', { metalness: 0.6, roughness: 0.4 }), 4.5, 1.13, -3.5);
  // mutfak tezgâhı + raf
  box(g, 2.2, 0.9, 0.6, bedWood, 2.6, 0.45, -4.05);
  box(g, 2.2, 0.06, 0.65, mat('#d8cdb8'), 2.6, 0.92, -4.05);
  box(g, 2.0, 0.05, 0.3, bedWood, 2.6, 1.9, -4.2);
  const jarCols = ['#c0392b', '#e67e22', '#f1c40f', '#8e44ad', '#27ae60'];
  jarCols.forEach((c, i) => cyl(g, 0.08, 0.08, 0.2, 8, mat(c, { roughness: 0.4 }), 1.8 + i * 0.38, 2.03, -4.2));
  // dolap
  box(g, 1.0, 1.9, 0.6, bedWood, -5.4, 0.95, 1.0);
  // masa (referans 11)
  const tbl = table();
  tbl.position.set(1.0, 0.05, 0.8);
  g.add(tbl);
  for (const [x, z, r] of [[1.0, 1.75, Math.PI], [1.0, -0.15, 0]]) {
    const ch = chair();
    ch.position.set(x, 0.05, z); ch.rotation.y = r;
    g.add(ch);
  }
  // duvar resmi
  box(g, 1.2, 0.8, 0.04, tmat(T.boardText(['Yuvam'], '#3d5a3b')), -1.6, 1.8, -D / 2 + t + 0.04);

  // kapı (dinamik)
  const doorPivot = dynamic(new THREE.Group());
  doorPivot.position.set(-0.7, 0, D / 2 - t / 2);
  box(doorPivot, 1.38, 2.28, 0.08, tmat(T.planks('#5c3a22', 4)), 0.69, 1.16, 0);
  sphere(doorPivot, 0.05, mat('#c9a34a', { metalness: 0.8, roughness: 0.3 }), 1.2, 1.1, 0.08);
  g.add(doorPivot);

  // iç ışık
  const lamp = new THREE.PointLight(0xffc27a, 0, 14, 1.6);
  lamp.position.set(0.5, 2.5, 0);
  g.add(lamp);

  bake(g);

  const colliders = [
    [-W / 2, W / 2, -D / 2, -D / 2 + t],
    [-W / 2, -W / 2 + t, -D / 2, D / 2],
    [W / 2 - t, W / 2, -D / 2, D / 2],
    [-W / 2, -0.7, D / 2 - t, D / 2],
    [0.7, W / 2, D / 2 - t, D / 2],
    [-W / 2 - 0.1, -1.0, D / 2 + 3.0, D / 2 + 3.12],
    [1.0, W / 2 + 0.1, D / 2 + 3.0, D / 2 + 3.12],
    [-W / 2 - 0.1, -W / 2 + 0.1, D / 2, D / 2 + 3.1],
    [W / 2 - 0.1, W / 2 + 0.1, D / 2, D / 2 + 3.1],
    [-5.5, -3.6, -4.3, -1.7],
    [4.1, 5.8, -4.3, -3.1],
    [1.5, 3.7, -4.4, -3.7],
    [0.5, 1.5, 0.3, 1.3],
    [-5.9, -4.9, 0.6, 1.4],
  ];
  const interacts = [
    { id: 'bed', x: -4.5, y: 0.8, z: -2.6, r: 1.6, label: 'Uyu (günü bitir)' },
    { id: 'kitchen', x: 3.6, y: 1.0, z: -3.4, r: 1.6, label: 'Mutfak / Soba' },
  ];
  const plantSlots = [
    [-5.4, 0.05, 2.8], [-5.4, 0.05, 3.8], [5.4, 0.05, 1.8], [5.4, 0.05, 3.6],
    [-1.4, 0.05, -3.9], [-2.4, 0.05, -3.9], [-5.6, 0.1, D / 2 + 2.6], [5.6, 0.1, D / 2 + 2.6],
    [2.6, 0.95, -4.15], [-3.2, 0.65, -3.9],
  ];
  let doorOpen = 0;
  return {
    group: g, colliders, interacts, plantSlots, lamp,
    door: { x: 0, z: D / 2 },
    update(dt, ctx) {
      const near = ctx.localPlayerDist(0, D / 2) < 3.2;
      doorOpen += ((near ? 1 : 0) - doorOpen) * Math.min(1, dt * 4);
      doorPivot.rotation.y = -doorOpen * 1.7;
      lamp.intensity = ctx.night ? 7 : (ctx.dark ? 4 : 2.2);
      fire.material.emissiveIntensity = ctx.season === 3 || ctx.night ? 2.2 + Math.sin(ctx.time * 9) * 0.4 : 0.6;
    },
  };
}

export function table() {
  const g = new THREE.Group();
  const wood = mat('#7a4024', { roughness: 0.55 });
  cyl(g, 0.6, 0.6, 0.06, 10, wood, 0, 0.78, 0);
  const top = new THREE.Mesh(new THREE.CircleGeometry(0.55, 10), tmat(T.doily(), '#ffffff', { roughness: 0.7 }));
  top.rotation.x = -Math.PI / 2; top.position.y = 0.815;
  g.add(top);
  cyl(g, 0.07, 0.09, 0.72, 8, wood, 0, 0.4, 0);
  for (let i = 0; i < 4; i++) {
    const a = (i / 4) * Math.PI * 2 + Math.PI / 4;
    beam(g, [0, 0.12, 0], [Math.cos(a) * 0.42, 0.02, Math.sin(a) * 0.42], 0.08, 0.06, wood);
  }
  return g;
}

export function chair() {
  const g = new THREE.Group();
  const w = tmat(T.planks('#6b3f22', 3, { nails: false }));
  box(g, 0.45, 0.05, 0.45, w, 0, 0.45, 0);
  for (const sx of [-1, 1]) for (const sz of [-1, 1]) box(g, 0.05, 0.45, 0.05, w, sx * 0.19, 0.22, sz * 0.19);
  box(g, 0.45, 0.5, 0.05, w, 0, 0.72, -0.2);
  return g;
}

// ----------------------------------------------------------------- AHIR
export function barn() {
  const g = new THREE.Group();
  const W = 12, D = 14, H = 4.2;
  const red = tmat(T.planks('#9c3328', 8, { weathered: true }));
  const white = mat('#f1ece2');
  const roofM = tmat(T.roofTiles('#7d2f26'));
  const stoneM = tmat(T.stone());
  const prof = [[-6, 0], [6, 0], [6, 4.2], [4.3, 7.0], [0, 8.6], [-4.3, 7.0], [-6, 4.2]];
  prism(g, prof, 0.25, red, 0, 0, D / 2 - 0.125, 0, 2);
  prism(g, prof, 0.25, red, 0, 0, -D / 2 + 0.125, 0, 2);
  for (const sx of [-1, 1]) box(g, 0.25, H, D, red, sx * (W / 2 - 0.125), H / 2, 0, 0, 0, 0, 2);
  box(g, W + 0.3, 0.4, D + 0.3, stoneM, 0, 0.0, 0, 0, 0, 0, 2);
  // çatı panelleri
  const pts = [[6.35, 4.0], [4.35, 7.15], [0, 8.85]];
  for (const s of [-1, 1]) {
    beam(g, [s * pts[0][0], pts[0][1], 0], [s * pts[1][0], pts[1][1], 0], 0.18, D + 0.8, roofM, 2);
    beam(g, [s * pts[1][0], pts[1][1], 0], [s * pts[2][0], pts[2][1], 0], 0.18, D + 0.8, roofM, 2);
  }
  // beyaz kenar süsleri
  for (const z of [D / 2 + 0.02, -D / 2 - 0.02]) {
    for (let i = 0; i < prof.length; i++) {
      const a = prof[i], b = prof[(i + 1) % prof.length];
      if (i === 0) continue;
      beam(g, [a[0], a[1], z], [b[0], b[1], z], 0.18, 0.06, white);
    }
  }
  for (const sx of [-1, 1]) for (const sz of [-1, 1]) box(g, 0.22, H, 0.22, white, sx * W / 2, H / 2, sz * D / 2);
  // büyük X kapılar
  const doorM = tmat(T.planks('#8c2c22', 6));
  for (const sx of [-1, 1]) {
    const cx = sx * 1.25;
    box(g, 2.4, 3.4, 0.1, doorM, cx, 1.9, D / 2 + 0.08);
    const z = D / 2 + 0.15;
    box(g, 2.5, 0.16, 0.06, white, cx, 3.55, z);
    box(g, 2.5, 0.16, 0.06, white, cx, 0.25, z);
    box(g, 0.16, 3.4, 0.06, white, cx - 1.17, 1.9, z);
    box(g, 0.16, 3.4, 0.06, white, cx + 1.17, 1.9, z);
    beam(g, [cx - 1.1, 0.3, z], [cx + 1.1, 3.5, z], 0.16, 0.06, white);
    beam(g, [cx + 1.1, 0.3, z], [cx - 1.1, 3.5, z], 0.16, 0.06, white);
  }
  // samanlık kapağı
  box(g, 1.8, 1.6, 0.1, doorM, 0, 5.6, D / 2 + 0.08);
  box(g, 2.0, 0.14, 0.06, white, 0, 6.45, D / 2 + 0.15);
  box(g, 2.0, 0.14, 0.06, white, 0, 4.78, D / 2 + 0.15);
  beam(g, [-0.85, 4.85, D / 2 + 0.15], [0.85, 6.38, D / 2 + 0.15], 0.14, 0.06, white);
  // makara kirişi
  box(g, 0.2, 0.2, 1.2, mat('#4a3020'), 0, 7.4, D / 2 + 0.5);
  // yan pencereler
  for (const z of [-4, 0, 4]) for (const sx of [-1, 1]) windowFrame(g, sx * (W / 2 + 0.05), 2.6, z, 0.9, 0.9, sx * Math.PI / 2, '#f1ece2', false);
  // yemlik ve saman
  const hayM = tmat(T.hay());
  box(g, 2.4, 0.5, 0.7, tmat(T.planks('#6b4a2f', 4)), 4.5, 0.25, D / 2 + 1.2);
  box(g, 2.2, 0.25, 0.55, hayM, 4.5, 0.55, D / 2 + 1.2);
  const tag = box(g, 2.2, 0.6, 0.06, tmat(T.boardText(['Ahır'], '#f1ece2', '#7d2f26')), 0, 7.6, D / 2 + 0.2);
  bake(g);
  return {
    group: g,
    colliders: [[-W / 2 - 0.2, W / 2 + 0.2, -D / 2 - 0.2, D / 2 + 0.2], [3.2, 5.8, D / 2 + 0.8, D / 2 + 1.6]],
    interacts: [{ id: 'barn', x: 0, y: 1.5, z: D / 2 + 0.8, r: 2.6, label: 'Ahır (hayvan durumu, yem)' }],
  };
}

// ----------------------------------------------------------------- HANGAR / DEPO
export function shed() {
  const g = new THREE.Group();
  const W = 12, D = 10, H = 4.6, RH = 2.2;
  const metal = tmat(T.corrugated('#b9bec2'));
  const roofM = tmat(T.corrugated('#9aa1a6'));
  const frame = mat('#5a4230');
  box(g, W, 0.12, D, tmat(T.stone('#9a958c')), 0, 0.02, 0, 0, 0, 0, 3);
  box(g, W, H, 0.12, metal, 0, H / 2, -D / 2, 0, 0, 0, 1.5);
  for (const sx of [-1, 1]) box(g, 0.12, H, D, metal, sx * W / 2, H / 2, 0, 0, 0, 0, 1.5);
  for (const sx of [-1, 1]) prism(g, [[-D / 2, 0], [D / 2, 0], [0, RH]], 0.12, metal, sx * W / 2, H, 0, Math.PI / 2, 1.5);
  prism(g, [[-W / 2, 0], [W / 2, 0], [0, RH]], 0.12, metal, 0, H, -D / 2, 0, 1.5);
  const half = D / 2 + 0.5, ang = Math.atan2(RH, half), len = Math.hypot(half, RH);
  for (const s of [-1, 1]) {
    box(g, W + 0.8, 0.1, len, roofM, 0, H + RH / 2, s * half / 2, s * ang, 0, 0, 1.5);
  }
  // ön açık: dikmeler
  for (const x of [-W / 2, 0, W / 2]) box(g, 0.25, H, 0.25, frame, x, H / 2, D / 2);
  box(g, W, 0.3, 0.25, frame, 0, H - 0.15, D / 2);
  prism(g, [[-W / 2, 0], [W / 2, 0], [0, RH]], 0.12, metal, 0, H, D / 2, 0, 1.5);
  // tezgâh ve aletler
  const wood = tmat(T.planks('#8a5a34', 5));
  box(g, 3.5, 0.9, 0.9, wood, -3, 0.45, -D / 2 + 0.6);
  box(g, 3.6, 0.08, 1.0, wood, -3, 0.92, -D / 2 + 0.6);
  box(g, 3.6, 1.4, 0.06, wood, -3, 2.0, -D / 2 + 0.1);
  for (let i = 0; i < 5; i++) {
    rod(g, [-4.4 + i * 0.6, 1.4, -D / 2 + 0.2], [-4.4 + i * 0.6, 2.5, -D / 2 + 0.2], 0.03, mat('#7a5a3a'));
    box(g, 0.25, 0.18, 0.04, mat('#6e7378', { metalness: 0.6 }), -4.4 + i * 0.6, 1.35, -D / 2 + 0.2);
  }
  // varil
  for (const [x, z] of [[4.5, -3.5], [3.6, -3.8], [4.4, -2.6]]) {
    cyl(g, 0.4, 0.4, 1.1, 12, mat('#2f6b8a', { roughness: 0.5, metalness: 0.3 }), x, 0.6, z);
  }
  // saman balyası yığını içeride
  const hb = hayStack();
  hb.position.set(3.0, 0, 1.0); hb.rotation.y = 0.4;
  g.add(hb);
  bake(g);
  return {
    group: g,
    colliders: [[-W / 2, W / 2, -D / 2 - 0.1, -D / 2 + 0.1], [-W / 2 - 0.1, -W / 2 + 0.1, -D / 2, D / 2], [W / 2 - 0.1, W / 2 + 0.1, -D / 2, D / 2],
      [-4.8, -1.2, -D / 2, -D / 2 + 1.1], [3.1, 5.0, -4.0, -2.1], [1.7, 4.3, 0, 2.0]],
    interacts: [{ id: 'workbench', x: -3, y: 1, z: -D / 2 + 1.2, r: 1.8, label: 'Alet Tezgâhı (aletlerin)' }],
  };
}

// ----------------------------------------------------------------- SİLO
export function silo() {
  const g = new THREE.Group();
  const r = 2.8, h = 12;
  const m = tmat(T.corrugated('#d9d6cf'));
  cyl(g, r, r, h, 24, m, 0, h / 2, 0, 0, 0, 0, 2);
  const dome = new THREE.Mesh(new THREE.SphereGeometry(r, 24, 10, 0, Math.PI * 2, 0, Math.PI / 2), mat('#c9c5bd', { metalness: 0.3, roughness: 0.5 }));
  dome.position.y = h; g.add(dome);
  for (let y = 1.5; y < h; y += 2.2) {
    const tr = new THREE.Mesh(new THREE.TorusGeometry(r + 0.03, 0.06, 6, 32), mat('#9e9a93'));
    tr.rotation.x = Math.PI / 2; tr.position.y = y; g.add(tr);
  }
  const lm = mat('#6e6a64', { metalness: 0.5 });
  for (const s of [-1, 1]) rod(g, [s * 0.25, 0.5, r + 0.15], [s * 0.25, h + 0.5, r + 0.15], 0.035, lm);
  for (let y = 0.8; y < h + 0.5; y += 0.4) rod(g, [-0.25, y, r + 0.15], [0.25, y, r + 0.15], 0.025, lm);
  box(g, 6.2, 0.3, 6.2, tmat(T.stone()), 0, 0.05, 0);
  bake(g);
  return { group: g, colliders: [[-r, r, -r, r]], interacts: [{ id: 'silo', x: 0, y: 1, z: r + 0.4, r: 2, label: 'Silo (yem deposu)' }] };
}

// ----------------------------------------------------------------- SU KULESİ
export function waterTower() {
  const g = new THREE.Group();
  const legM = mat('#d8d8d4', { metalness: 0.4, roughness: 0.5 });
  const tankM = mat('#3e4752', { metalness: 0.5, roughness: 0.45 });
  const H = 9;
  const legs = [[-2, -2], [2, -2], [2, 2], [-2, 2]];
  for (const [x, z] of legs) rod(g, [x, 0, z], [x * 0.7, H, z * 0.7], 0.12, legM);
  for (let i = 0; i < 4; i++) {
    const a = legs[i], b = legs[(i + 1) % 4];
    for (const y of [3, 6]) {
      const f = 1 - (y / H) * 0.3;
      rod(g, [a[0] * f, y, a[1] * f], [b[0] * f, y, b[1] * f], 0.05, legM);
    }
    rod(g, [a[0], 0.2, a[1]], [b[0] * 0.8, 6, b[1] * 0.8], 0.035, legM);
  }
  cyl(g, 2.4, 2.4, 3.6, 20, tankM, 0, H + 1.8, 0);
  cone(g, 2.7, 1.4, 20, tankM, 0, H + 4.3, 0);
  const ring = new THREE.Mesh(new THREE.TorusGeometry(2.7, 0.05, 6, 32), legM);
  ring.rotation.x = Math.PI / 2; ring.position.y = H + 0.9; g.add(ring);
  cyl(g, 2.75, 2.75, 0.1, 20, legM, 0, H + 0.05, 0);
  for (let i = 0; i < 16; i++) {
    const a = (i / 16) * Math.PI * 2;
    rod(g, [Math.cos(a) * 2.7, H, Math.sin(a) * 2.7], [Math.cos(a) * 2.7, H + 0.9, Math.sin(a) * 2.7], 0.025, legM);
  }
  bake(g);
  return { group: g, colliders: legs.map(([x, z]) => [x - 0.2, x + 0.2, z - 0.2, z + 0.2]), interacts: [] };
}

// ----------------------------------------------------------------- RÜZGÂR POMPASI
export function windPump() {
  const g = new THREE.Group();
  const m = mat('#cfcfcb', { metalness: 0.5, roughness: 0.5 });
  const H = 9.5;
  const legs = [[-1.3, -1.3], [1.3, -1.3], [1.3, 1.3], [-1.3, 1.3]];
  for (const [x, z] of legs) rod(g, [x, 0, z], [x * 0.2, H, z * 0.2], 0.07, m);
  for (let i = 0; i < 4; i++) {
    const a = legs[i], b = legs[(i + 1) % 4];
    for (let y = 1.5; y < H; y += 2) {
      const f = 1 - (y / H) * 0.8;
      rod(g, [a[0] * f, y, a[1] * f], [b[0] * f, y, b[1] * f], 0.03, m);
    }
  }
  cyl(g, 1.4, 1.4, 0.08, 16, mat('#2d2f31'), 0, H - 0.3, 0);
  const hub = dynamic(new THREE.Group());
  hub.position.set(0, H + 0.3, 0.6);
  const bladeM = mat('#e9e7e1', { metalness: 0.3, roughness: 0.5, side: THREE.DoubleSide });
  for (let i = 0; i < 18; i++) {
    const a = (i / 18) * Math.PI * 2;
    const b = new THREE.Mesh(new THREE.BoxGeometry(0.28, 1.4, 0.02), bladeM);
    b.position.set(Math.cos(a) * 1.1, Math.sin(a) * 1.1, 0);
    b.rotation.z = a - Math.PI / 2;
    b.rotation.y = 0.35;
    hub.add(b);
  }
  const rim = new THREE.Mesh(new THREE.TorusGeometry(1.8, 0.03, 4, 32), m); hub.add(rim);
  cyl(hub, 0.15, 0.15, 0.3, 8, mat('#3a3a3a'), 0, 0, 0, Math.PI / 2);
  g.add(hub);
  box(g, 0.1, 0.1, 2.6, m, 0, H + 0.3, -0.7);
  const vane = box(g, 0.04, 1.0, 1.4, mat('#e9e7e1'), 0, H + 0.45, -2.0);
  bake(g);
  return {
    group: g, colliders: legs.map(([x, z]) => [x - 0.15, x + 0.15, z - 0.15, z + 0.15]), interacts: [],
    update(dt, ctx) { hub.rotation.z -= dt * (1.2 + ctx.wind * 3); },
  };
}

// ----------------------------------------------------------------- YEL DEĞİRMENİ (tuğla)
export function windmill() {
  const g = new THREE.Group();
  const brick = tmat(T.bricks('#8c5442'));
  cyl(g, 2.1, 3.0, 9, 10, brick, 0, 4.5, 0, 0, 0, 0, 2.5);
  const capM = tmat(T.planks('#6b4a35', 6));
  cone(g, 2.6, 2.6, 10, capM, 0, 10.3, 0);
  cyl(g, 2.5, 2.5, 0.4, 10, mat('#4a3426'), 0, 9.1, 0);
  // kapı ve pencereler
  box(g, 1.2, 2.1, 0.3, tmat(T.planks('#4e3322', 4)), 0, 1.05, 2.85, -0.1);
  windowFrame(g, 0, 5.2, 2.45, 0.6, 0.8, 0, '#e8dfcf', false);
  windowFrame(g, 2.4, 3.5, 0, 0.6, 0.8, Math.PI / 2, '#e8dfcf', false);
  const hub = dynamic(new THREE.Group());
  hub.position.set(0, 8.6, 2.6);
  const frameM = mat('#d9c9a8');
  const cloth = mat('#efe6d0', { side: THREE.DoubleSide, roughness: 1 });
  cyl(hub, 0.3, 0.3, 0.6, 10, mat('#4a3426'), 0, 0, 0, Math.PI / 2);
  for (let i = 0; i < 4; i++) {
    const arm = new THREE.Group();
    arm.rotation.z = (i / 4) * Math.PI * 2;
    box(arm, 0.18, 6.8, 0.14, frameM, 0, 3.4, 0.1);
    box(arm, 1.4, 5.4, 0.02, cloth, 0.75, 3.9, 0.15);
    for (let k = 0; k < 6; k++) box(arm, 1.6, 0.06, 0.06, frameM, 0.8, 1.3 + k * 1.05, 0.18);
    box(arm, 0.06, 5.6, 0.06, frameM, 1.55, 3.9, 0.18);
    hub.add(arm);
  }
  g.add(hub);
  const tag = box(g, 2.0, 0.55, 0.06, tmat(T.boardText(['Değirmen'])), 0, 2.6, 2.95, -0.1);
  bake(g);
  return {
    group: g, colliders: [[-2.9, 2.9, -2.9, 2.6]],
    interacts: [{ id: 'mill', x: 0, y: 1, z: 3.4, r: 2.2, label: 'Değirmen (un öğüt / çalış)' }],
    update(dt, ctx) { hub.rotation.z += dt * (0.25 + ctx.wind * 0.6); },
  };
}

// ----------------------------------------------------------------- KÜMES
export function coop() {
  const g = new THREE.Group();
  const wall = tmat(T.planks('#c9a26b', 6, { weathered: true }));
  const roofM = tmat(T.roofTiles('#a8462e'));
  const legM = mat('#5a3d28');
  for (const sx of [-1, 1]) for (const sz of [-1, 1]) box(g, 0.15, 0.7, 0.15, legM, sx * 1.5, 0.35, sz * 1.1);
  box(g, 3.2, 1.6, 2.4, wall, 0, 1.5, 0, 0, 0, 0, 1.5);
  prism(g, [[-1.2, 0], [1.2, 0], [0, 1.0]], 3.2, wall, 0, 2.3, 0, Math.PI / 2, 1.5);
  const ang = Math.atan2(1.0, 1.45), len = Math.hypot(1.45, 1.0) + 0.1;
  for (const s of [-1, 1]) box(g, 3.6, 0.1, len, roofM, 0, 2.82, s * 0.7, s * ang, 0, 0, 1.2);
  box(g, 0.7, 0.8, 0.05, mat('#3a2a1e'), 0.6, 1.2, 1.23);
  beam(g, [0.6, 0.8, 1.25], [0.6, 0, 2.6], 0.6, 0.05, tmat(T.planks('#8a6a45', 3)));
  box(g, 0.9, 0.5, 0.6, wall, -1.95, 1.3, 0.3);
  windowFrame(g, -0.7, 1.7, 1.23, 0.5, 0.4, 0, '#f1ece2', false);
  bake(g);
  return { group: g, colliders: [[-1.8, 2.4, -1.3, 1.3]], interacts: [{ id: 'coop', x: 0.6, y: 0.8, z: 2.2, r: 1.8, label: 'Kümes (yumurta topla)' }] };
}

// ----------------------------------------------------------------- AÇIK BARINAK
export function shelter() {
  const g = new THREE.Group();
  const post = mat('#5a3d28');
  const wall = tmat(T.planks('#d9c6a0', 6));
  const roofM = tmat(T.roofTiles('#8a3a2c'));
  for (const x of [-3, 0, 3]) for (const z of [-2, 2]) box(g, 0.2, 2.8, 0.2, post, x, 1.4, z);
  box(g, 6.2, 2.2, 0.1, wall, 0, 1.4, -2, 0, 0, 0, 1.5);
  for (const sx of [-1, 1]) box(g, 0.1, 1.4, 4, wall, sx * 3, 1.9, 0, 0, 0, 0, 1.5);
  box(g, 6.8, 0.12, 5, roofM, 0, 3.0, 0, -0.18, 0, 0, 1.5);
  const tr = tmat(T.planks('#6b4a2f', 4));
  box(g, 3, 0.5, 0.6, tr, 0, 0.3, -1.4);
  box(g, 2.8, 0.2, 0.45, tmat(T.hay()), 0, 0.58, -1.4);
  // su yalağı
  box(g, 1.4, 0.45, 0.6, mat('#6e7378', { metalness: 0.5 }), 2.0, 0.25, -1.4);
  box(g, 1.3, 0.04, 0.5, mat('#4a8aa8', { roughness: 0.1 }), 2.0, 0.44, -1.4);
  bake(g);
  return { group: g, colliders: [[-3.1, 3.1, -2.1, -1.0]], interacts: [] };
}

// ----------------------------------------------------------------- ÇİT
// gaps: [{side:'n'|'s'|'e'|'w', a, b}] (kenar boyunca dünya koordinatı aralığı)
export function fenceRect(x0, x1, z0, z1, gaps = [], color = '#e9e2d3') {
  const g = new THREE.Group();
  const pm = mat(color);
  const colliders = [];
  const sides = [
    { side: 'n', fixed: z0, from: x0, to: x1, horiz: true },
    { side: 's', fixed: z1, from: x0, to: x1, horiz: true },
    { side: 'w', fixed: x0, from: z0, to: z1, horiz: false },
    { side: 'e', fixed: x1, from: z0, to: z1, horiz: false },
  ];
  for (const s of sides) {
    const runs = [];
    let cur = s.from;
    const gs = gaps.filter((q) => q.side === s.side).sort((a, b) => a.a - b.a);
    for (const q of gs) { runs.push([cur, q.a]); cur = q.b; }
    runs.push([cur, s.to]);
    for (const [a, b] of runs) {
      if (b - a < 0.1) continue;
      const len = b - a, mid = (a + b) / 2;
      const n = Math.max(1, Math.round(len / 2));
      for (let i = 0; i <= n; i++) {
        const p = a + (len * i) / n;
        if (s.horiz) box(g, 0.14, 1.2, 0.14, pm, p, 0.6, s.fixed);
        else box(g, 0.14, 1.2, 0.14, pm, s.fixed, 0.6, p);
      }
      for (const y of [0.45, 0.95]) {
        if (s.horiz) box(g, len, 0.1, 0.06, pm, mid, y, s.fixed);
        else box(g, 0.06, 0.1, len, pm, s.fixed, y, mid);
      }
      if (s.horiz) colliders.push([a, b, s.fixed - 0.12, s.fixed + 0.12]);
      else colliders.push([s.fixed - 0.12, s.fixed + 0.12, a, b]);
    }
  }
  bake(g);
  return { group: g, colliders, interacts: [], worldSpace: true };
}

// ----------------------------------------------------------------- KUYU
export function well() {
  const g = new THREE.Group();
  const st = tmat(T.stone('#9a958c'));
  cyl(g, 1.0, 1.05, 0.9, 14, st, 0, 0.45, 0, 0, 0, 0, 1.5);
  const water = new THREE.Mesh(new THREE.CircleGeometry(0.85, 14), mat('#1d3f4f', { roughness: 0.1 }));
  water.rotation.x = -Math.PI / 2; water.position.y = 0.7; g.add(water);
  const w = mat('#5a3d28');
  for (const sx of [-1, 1]) box(g, 0.14, 2.2, 0.14, w, sx * 0.95, 1.5, 0);
  const roofM = tmat(T.roofTiles('#8a3a2c'));
  for (const s of [-1, 1]) box(g, 2.4, 0.08, 1.0, roofM, 0, 2.75, s * 0.4, s * 0.6, 0, 0, 1);
  rod(g, [-1.1, 2.0, 0], [1.2, 2.0, 0], 0.06, w);
  rod(g, [1.2, 2.0, 0], [1.2, 1.7, 0.25], 0.03, w);
  rod(g, [0, 2.0, 0], [0, 1.2, 0], 0.012, mat('#b9a27a'));
  cyl(g, 0.16, 0.12, 0.25, 10, mat('#6e5034'), 0, 1.1, 0);
  bake(g);
  return { group: g, colliders: [[-1.1, 1.1, -1.1, 1.1]], interacts: [{ id: 'well', x: 0, y: 1, z: 0, r: 2.0, label: 'Kuyu (sulama kabını doldur)' }] };
}

export function woodpile() {
  const g = new THREE.Group();
  const logM = tmat(T.bark('#6b4a2f'));
  const capM = mat('#c9a271');
  for (let row = 0; row < 4; row++) {
    const n = 6 - row;
    for (let i = 0; i < n; i++) {
      const x = (i - (n - 1) / 2) * 0.32;
      const y = 0.16 + row * 0.27;
      cyl(g, 0.15, 0.15, 1.3, 7, logM, x, y, 0, Math.PI / 2);
      for (const s of [-1, 1]) {
        const c = new THREE.Mesh(new THREE.CircleGeometry(0.14, 7), capM);
        c.position.set(x, y, s * 0.651); c.rotation.y = s > 0 ? 0 : Math.PI; g.add(c);
      }
    }
  }
  // kütük ve balta
  cyl(g, 0.35, 0.4, 0.55, 10, logM, 1.6, 0.27, 0.2);
  const axe = new THREE.Group();
  rod(axe, [0, 0, 0], [0, 0.8, 0], 0.03, mat('#8a6a45'));
  box(axe, 0.22, 0.14, 0.03, mat('#8d9399', { metalness: 0.7, roughness: 0.3 }), 0.08, 0.75, 0);
  axe.position.set(1.6, 0.55, 0.2); axe.rotation.z = 0.5;
  g.add(axe);
  bake(g);
  return { group: g, colliders: [[-1.0, 2.0, -0.7, 0.7]], interacts: [] };
}

// ----------------------------------------------------------------- SAMAN BALYALARI + YABA (referans 2)
export function hayRoll(parent, x, y, z, ry = 0, r = 0.65, l = 1.2) {
  const grp = new THREE.Group();
  const side = cyl(grp, r, r, l, 16, tmat(T.hay()), 0, 0, 0, 0, 0, Math.PI / 2, 1, true);
  for (const s of [-1, 1]) {
    const c = new THREE.Mesh(new THREE.CircleGeometry(r, 16), tmat(T.haySpiral()));
    c.position.x = s * l / 2; c.rotation.y = s * Math.PI / 2; grp.add(c);
  }
  grp.position.set(x, y, z); grp.rotation.y = ry;
  parent.add(grp);
  return grp;
}
export function pitchfork(parent, x, y, z, rz = 0.3, ry = 0) {
  const p = new THREE.Group();
  rod(p, [0, 0, 0], [0, 1.6, 0], 0.03, mat('#8a6a45'));
  box(p, 0.08, 0.12, 0.04, mat('#c3a35b'), 0, 1.66, 0);
  const steel = mat('#b8bec4', { metalness: 0.8, roughness: 0.3 });
  box(p, 0.42, 0.04, 0.04, steel, 0, -0.02, 0);
  for (let i = 0; i < 4; i++) {
    const tx = -0.18 + i * 0.12;
    rod(p, [tx, -0.02, 0], [tx * 1.1, -0.5, 0.06], 0.012, steel);
  }
  p.position.set(x, y, z); p.rotation.set(0, ry, rz);
  parent.add(p);
  return p;
}
export function hayStack() {
  const g = new THREE.Group();
  hayRoll(g, -0.65, 0.65, 0);
  hayRoll(g, 0.65, 0.65, 0);
  hayRoll(g, 0, 1.75, 0);
  pitchfork(g, 1.35, 0.55, 0.3, -0.35);
  return g;
}

// ----------------------------------------------------------------- KORKULUK (referans 2)
export function scarecrow() {
  const g = new THREE.Group();
  const w = mat('#7a5a3a');
  box(g, 0.1, 2.4, 0.1, w, 0, 1.2, 0);
  box(g, 1.8, 0.08, 0.08, w, 0, 1.7, 0);
  const shirt = mat('#3c6fae');
  box(g, 0.6, 0.75, 0.32, shirt, 0, 1.45, 0);
  box(g, 0.2, 0.2, 0.02, mat('#d9a845'), 0.15, 1.3, 0.17);
  for (const s of [-1, 1]) {
    box(g, 0.6, 0.22, 0.24, shirt, s * 0.55, 1.7, 0);
    cone(g, 0.1, 0.3, 6, mat('#e2b85a'), s * 0.95, 1.7, 0, 0, 0, s * Math.PI / 2);
  }
  const tie = box(g, 0.14, 0.5, 0.02, tmat(T.stripes('#c0392b', '#f4efe6', 6)), 0, 1.5, 0.17);
  tie.rotation.z = 0.1;
  sphere(g, 0.25, mat('#d8bf86', { flat: true }), 0, 2.08, 0, 1, 1.05, 1);
  box(g, 0.06, 0.06, 0.02, mat('#222'), -0.08, 2.12, 0.24);
  box(g, 0.06, 0.06, 0.02, mat('#222'), 0.08, 2.12, 0.24);
  box(g, 0.14, 0.03, 0.02, mat('#7a2a1a'), 0, 2.0, 0.24);
  const hat = mat('#36609c');
  cyl(g, 0.48, 0.48, 0.04, 12, hat, 0, 2.3, 0, 0.12);
  cyl(g, 0.22, 0.26, 0.3, 10, hat, 0, 2.45, 0, 0.1);
  cone(g, 0.1, 0.35, 5, mat('#e2b85a'), 0, 1.0, 0, Math.PI);
  bake(g);
  return { group: g, colliders: [[-0.2, 0.2, -0.2, 0.2]], interacts: [] };
}

// ----------------------------------------------------------------- A TABELA (referans 2)
export function signA(color = '#8a4f2a') {
  const g = new THREE.Group();
  const fr = tmat(T.planks(color, 3, { nails: false }));
  const panel = tmat(T.signCarrot());
  for (const s of [-1, 1]) {
    const side = new THREE.Group();
    side.position.set(0, 0, s * 0.02);
    side.rotation.x = s * 0.22;
    box(side, 0.08, 1.3, 0.06, fr, -0.42, 0.62, 0);
    box(side, 0.08, 1.3, 0.06, fr, 0.42, 0.62, 0);
    box(side, 0.92, 0.1, 0.06, fr, 0, 1.18, 0);
    box(side, 0.92, 0.1, 0.06, fr, 0, 0.35, 0);
    box(side, 0.6, 0.18, 0.06, fr, 0, 1.3, 0);
    const p = new THREE.Mesh(new THREE.PlaneGeometry(0.76, 0.76), panel);
    p.position.set(0, 0.76, s * 0.035); if (s < 0) p.rotation.y = Math.PI;
    side.add(p);
    g.add(side);
  }
  bake(g);
  return { group: g, colliders: [[-0.5, 0.5, -0.35, 0.35]], interacts: [{ id: 'farmstand', x: 0, y: 0.8, z: 0.4, r: 1.8, label: 'Çiftlik Tezgâhı (yerinde sat %75)' }] };
}

// ----------------------------------------------------------------- TARLA ÇADIRI (referans 8)
export function fieldStall() {
  const g = new THREE.Group();
  const post = tmat(T.bark('#8a6a45'));
  const pts = [[-1.6, -1.1, 2.2], [1.6, -1.1, 2.4], [1.6, 1.1, 1.9], [-1.6, 1.1, 1.8]];
  for (const [x, z, h] of pts) cyl(g, 0.08, 0.1, h, 6, post, x, h / 2, z);
  const cloth = mat('#d06a33', { side: THREE.DoubleSide, roughness: 1 });
  const roof = box(g, 3.6, 0.04, 2.6, cloth, 0, 2.1, 0, 0.12, 0, 0.05);
  rod(g, [-1.7, 2.15, -1.2], [1.7, 2.3, -1.2], 0.05, post);
  const crate = tmat(T.planks('#a07a4a', 4));
  for (const [x, z, c] of [[-0.8, 0.2, '#d9472b'], [0.4, 0.4, '#e8892c'], [0.9, -0.5, '#5a9b3c']]) {
    box(g, 0.8, 0.45, 0.6, crate, x, 0.23, z);
    for (let i = 0; i < 9; i++) sphere(g, 0.09, mat(c, { flat: true }), x - 0.25 + (i % 3) * 0.25, 0.5, z - 0.17 + Math.floor(i / 3) * 0.17, 1, 1, 1, 0);
  }
  const basket = cyl(g, 0.35, 0.28, 0.35, 10, tmat(T.hay(), '#c08a50'), -1.6, 0.18, 1.6);
  for (let i = 0; i < 8; i++) sphere(g, 0.07, mat('#a3122a'), -1.6 + Math.cos(i) * 0.18, 0.38, 1.6 + Math.sin(i) * 0.18, 1, 1, 1, 0);
  for (const [x, z, s] of [[1.9, 1.3, 0.35], [-2.0, -0.8, 0.3], [2.0, -1.2, 0.25]]) sphere(g, s, mat('#8f8c86', { flat: true }), x, s * 0.5, z, 1.2, 0.7, 1, 0);
  bake(g);
  return { group: g, colliders: [[-1.7, 1.7, -1.2, 1.2]], interacts: [{ id: 'farmstand', x: 0, y: 0.8, z: 1.4, r: 2.2, label: 'Tarla Tezgâhı (yerinde sat %75)' }] };
}

// ----------------------------------------------------------------- PAZAR ARABASI (referans 13)
export function marketCart(produce = ['melon', 'tomato', 'cabbage']) {
  const g = new THREE.Group();
  const wood = tmat(T.planks('#b0763f', 5));
  const dark = tmat(T.planks('#7a4a28', 4));
  box(g, 2.4, 0.08, 1.4, dark, 0, 0.75, 0);
  for (const s of [-1, 1]) box(g, 2.4, 0.5, 0.08, wood, 0, 1.0, s * 0.7);
  for (const s of [-1, 1]) box(g, 0.08, 0.5, 1.4, wood, s * 1.2, 1.0, 0);
  for (const s of [-0.4, 0.4]) box(g, 0.06, 0.45, 1.3, dark, s, 1.0, 0);
  wheel(g, -0.75, 0.45, 0.75, 0.45);
  wheel(g, 0.75, 0.45, 0.75, 0.45);
  wheel(g, -0.75, 0.45, -0.75, 0.45);
  wheel(g, 0.75, 0.45, -0.75, 0.45);
  for (const s of [-1, 1]) rod(g, [-1.2, 0.8, s * 0.5], [-2.7, 0.35, s * 0.6], 0.05, dark);
  // ürünler
  const kinds = {
    melon: () => ({ m: tmat(T.melonStripes()), r: 0.17, sy: 0.85 }),
    tomato: () => ({ m: mat('#d23a2a', { roughness: 0.4 }), r: 0.08, sy: 0.9 }),
    cabbage: () => ({ m: mat('#a9cf86', { flat: true }), r: 0.14, sy: 0.9 }),
    carrot: () => ({ m: mat('#e8742a'), r: 0.07, sy: 2 }),
    apple: () => ({ m: mat('#c8302a', { roughness: 0.4 }), r: 0.07, sy: 1 }),
    pumpkin: () => ({ m: mat('#e2802a', { flat: true }), r: 0.16, sy: 0.75 }),
  };
  produce.forEach((k, i) => {
    const P = kinds[k]();
    const cx = -0.8 + i * 0.8;
    const n = P.r > 0.12 ? 6 : 14;
    for (let j = 0; j < n; j++) {
      const s = sphere(g, P.r, P.m, cx + ((j % 3) - 1) * P.r * 1.9, 1.0 + Math.floor(j / 6) * P.r * 0.9, -0.35 + ((Math.floor(j / 3)) % 2) * 0.6 + (j % 2) * 0.1, 1, P.sy, 1, 1);
    }
  });
  // tente
  const pole = tmat(T.bark('#7a5a3a'));
  for (const x of [-1.15, 1.15]) {
    cyl(g, 0.05, 0.05, 1.7, 6, pole, x, 1.6, -0.75);
    cyl(g, 0.05, 0.05, 1.3, 6, pole, x, 1.4, 0.75);
  }
  const aw = box(g, 2.7, 0.03, 1.9, tmat(T.stripes('#f1e6cf', '#2b3566', 10)), 0, 2.25, 0, 0.25);
  aw.material.side = THREE.DoubleSide;
  bake(g);
  return { group: g, colliders: [[-1.3, 1.3, -0.8, 0.8]], interacts: [] };
}

function wheel(g, x, y, z, r, spokes = 0) {
  const w = tmat(T.planks('#8a5a34', 4, { nails: false }));
  cyl(g, r, r, 0.12, 14, w, x, y, z, Math.PI / 2);
  const rim = new THREE.Mesh(new THREE.TorusGeometry(r, 0.035, 5, 18), mat('#3d3d3d', { metalness: 0.6 }));
  rim.position.set(x, y, z + Math.sign(z || 1) * 0.02); g.add(rim);
  cyl(g, 0.08, 0.08, 0.2, 8, mat('#3d3d3d', { metalness: 0.6 }), x, y, z, Math.PI / 2);
}

// Tahta parmaklıklı tekerlek (dinamik dönebilir)
export function spokedWheel(r, color = '#7a5a3a') {
  const g = new THREE.Group();
  const m = mat(color);
  const rim = new THREE.Mesh(new THREE.TorusGeometry(r, 0.06, 6, 20), m); g.add(rim);
  const iron = new THREE.Mesh(new THREE.TorusGeometry(r + 0.04, 0.025, 4, 20), mat('#3a3a3a', { metalness: 0.6 })); g.add(iron);
  for (let i = 0; i < 10; i++) {
    const a = (i / 10) * Math.PI * 2;
    const s = box(g, 0.04, r, 0.04, m, Math.cos(a) * r / 2, Math.sin(a) * r / 2, 0);
    s.rotation.z = a - Math.PI / 2;
  }
  cyl(g, 0.1, 0.1, 0.2, 8, m, 0, 0, 0, Math.PI / 2);
  mergeChildren(g);
  return g;
}

// ----------------------------------------------------------------- EL ARABASI (referans 6)
export function handCart() {
  const g = new THREE.Group();
  const wood = tmat(T.planks('#8a6a48', 5));
  const iron = mat('#4a4a4a', { metalness: 0.6, roughness: 0.5 });
  box(g, 1.8, 0.08, 1.1, wood, 0, 0.6, 0);
  for (const s of [-1, 1]) {
    for (const y of [0.75, 0.95]) box(g, 1.8, 0.1, 0.05, wood, 0, y, s * 0.55);
    for (const x of [-0.85, 0, 0.85]) box(g, 0.07, 0.45, 0.07, wood, x, 0.8, s * 0.55);
  }
  for (const y of [0.75, 0.95]) box(g, 0.05, 0.1, 1.1, wood, 0.9, y, 0);
  for (const s of [-1, 1]) {
    const w = new THREE.Group();
    cyl(w, 0.58, 0.58, 0.1, 14, tmat(T.planks('#6e5034', 4, { nails: false })), 0, 0, 0, Math.PI / 2);
    const rim = new THREE.Mesh(new THREE.TorusGeometry(0.58, 0.04, 5, 18), iron); w.add(rim);
    cyl(w, 0.12, 0.12, 0.16, 8, iron, 0, 0, 0, Math.PI / 2);
    w.position.set(0, 0.58, s * 0.66);
    g.add(w);
  }
  for (const s of [-1, 1]) {
    rod(g, [-0.9, 0.62, s * 0.4], [-2.2, 0.55, s * 0.25], 0.05, wood);
    box(g, 0.25, 0.08, 0.08, mat('#c9a271'), -2.15, 0.55, s * 0.25);
  }
  box(g, 0.08, 0.5, 0.08, wood, -1.9, 0.3, 0);
  // yük: sandık, fıçı, peynir
  const crate = tmat(T.planks('#9a7346', 4));
  box(g, 0.7, 0.5, 0.6, crate, 0.4, 0.9, -0.1);
  box(g, 0.6, 0.45, 0.5, crate, 0.45, 1.35, 0.1, 0, 0.2);
  const barrel = cyl(g, 0.28, 0.25, 0.6, 12, tmat(T.planks('#7a5232', 8)), -0.45, 0.65 + 0.3, 0.15, Math.PI / 2, 0.4);
  const ch = cyl(g, 0.25, 0.25, 0.14, 12, tmat(T.cheese()), 0.4, 1.65, 0.05);
  pitchfork(g, -0.2, 1.4, -0.3, 0.9, 0.4);
  bake(g);
  return { group: g, colliders: [[-2.2, 1.0, -0.8, 0.8]], interacts: [] };
}

// ----------------------------------------------------------------- EL ARABASI (TEKERLEKLİ) + KÜREK (referans 14)
export function wheelbarrow() {
  const g = new THREE.Group();
  const galv = mat('#8a9096', { metalness: 0.65, roughness: 0.55 });
  const tub = new THREE.Mesh(new THREE.CylinderGeometry(0.62, 0.38, 0.42, 4, 1, true), mat('#8a9096', { metalness: 0.65, roughness: 0.55, side: THREE.DoubleSide }));
  tub.rotation.y = Math.PI / 4; tub.scale.set(1.35, 1, 1);
  tub.position.set(0, 0.62, 0); g.add(tub);
  const bottom = box(g, 0.7, 0.03, 0.5, galv, 0, 0.42, 0);
  const tube = mat('#9aa0a6', { metalness: 0.7, roughness: 0.4 });
  for (const s of [-1, 1]) {
    rod(g, [0.85, 0.3, s * 0.08], [-0.6, 0.45, s * 0.3], 0.025, tube);
    rod(g, [-0.6, 0.45, s * 0.3], [-1.25, 0.62, s * 0.34], 0.025, tube);
    rod(g, [-0.3, 0.42, s * 0.25], [-0.4, 0.0, s * 0.28], 0.022, tube);
    box(g, 0.2, 0.05, 0.05, mat('#2a2a2a'), -1.3, 0.63, s * 0.34);
  }
  const wheelG = new THREE.Group();
  cyl(wheelG, 0.26, 0.26, 0.1, 16, mat('#1f1f1f'), 0, 0, 0, Math.PI / 2);
  cyl(wheelG, 0.14, 0.14, 0.11, 12, mat('#9aa0a6', { metalness: 0.6 }), 0, 0, 0, Math.PI / 2);
  wheelG.position.set(0.85, 0.26, 0); g.add(wheelG);
  // kürek
  const sh = new THREE.Group();
  rod(sh, [0, 0, 0], [0, 1.25, 0], 0.025, mat('#6b4a2f'));
  box(sh, 0.18, 0.04, 0.04, mat('#6b4a2f'), 0, 1.3, 0);
  const blade = box(sh, 0.3, 0.36, 0.02, mat('#9a7a6a', { metalness: 0.5, roughness: 0.6 }), 0, -0.15, 0);
  sh.position.set(-0.5, 0.35, 0.45); sh.rotation.set(0.05, 0, -0.3);
  g.add(sh);
  bake(g);
  return { group: g, colliders: [[-1.3, 1.0, -0.5, 0.6]], interacts: [] };
}

// ----------------------------------------------------------------- KASABA EVİ (referans 12)
export function townHouse(seed = 1, opts = {}) {
  const r = mulberry32(seed);
  const g = new THREE.Group();
  const W = opts.w ?? 7 + Math.floor(r() * 3), D = opts.d ?? 6 + Math.floor(r() * 2);
  const H1 = 3.0, H2 = 2.7;
  const beamC = ['#5b3a24', '#4a3222', '#6b4a2f'][Math.floor(r() * 3)];
  const wallC = ['#f1e8d6', '#efe2c4', '#e9dccb', '#f4eee2'][Math.floor(r() * 4)];
  const roofC = ['#8a3a2c', '#5a5f66', '#7d4a35', '#4c535b'][Math.floor(r() * 4)];
  const ground = tmat(T.stone('#a49e92'));
  const upper = tmat(T.halfTimber(wallC, beamC));
  const roofM = tmat(T.roofTiles(roofC));
  box(g, W, H1, D, opts.shop ? tmat(T.plaster(wallC)) : ground, 0, H1 / 2, 0, 0, 0, 0, 2.5);
  box(g, W + 0.4, H2, D + 0.4, upper, 0, H1 + H2 / 2, 0, 0, 0, 0, 2.7);
  box(g, W + 0.5, 0.16, D + 0.5, mat(beamC), 0, H1, 0);
  const RH = 3.2;
  for (const s of [-1, 1]) prism(g, [[-(D + 0.4) / 2, 0], [(D + 0.4) / 2, 0], [0, RH]], 0.2, upper, s * (W / 2 + 0.1), H1 + H2, 0, Math.PI / 2, 2.7);
  const half = D / 2 + 0.7, ang = Math.atan2(RH, half), len = Math.hypot(half, RH) + 0.1;
  for (const s of [-1, 1]) box(g, W + 1.2, 0.16, len, roofM, 0, H1 + H2 + RH / 2, s * half / 2, s * ang, 0, 0, 1.6);
  if (r() < 0.7) box(g, 0.7, 2.6, 0.7, tmat(T.bricks('#7d4535')), W / 2 - 1.3, H1 + H2 + 2.2, -0.6, 0, 0, 0, 1.5);
  // kapı
  box(g, 1.2, 2.2, 0.1, tmat(T.planks('#4e3322', 4)), 0, 1.1, D / 2 + 0.05);
  box(g, 1.5, 0.15, 0.2, mat(beamC), 0, 2.3, D / 2 + 0.08);
  // pencereler
  for (const x of [-W / 2 + 1.4, W / 2 - 1.4]) {
    windowFrame(g, x, 1.6, D / 2 + 0.03, 1.0, 1.1);
    shutters(g, x, 1.6, D / 2 + 0.06, 1.0, 1.2, ['#3f6b4a', '#2f5a8a', '#8a3a2c'][Math.floor(r() * 3)]);
    // çiçek kutusu
    box(g, 1.1, 0.2, 0.25, tmat(T.planks('#6b4a2f', 3)), x, 0.95, D / 2 + 0.2);
    for (let i = 0; i < 5; i++) sphere(g, 0.08, mat(['#e74c3c', '#f1c40f', '#ecf0f1', '#e84393'][Math.floor(r() * 4)]), x - 0.4 + i * 0.2, 1.12, D / 2 + 0.22, 1, 1, 1, 0);
  }
  for (const x of [-W / 2 + 1.4, 0, W / 2 - 1.4]) windowFrame(g, x, H1 + 1.3, D / 2 + 0.23, 0.9, 1.0);
  for (const z of [-D / 4, D / 4]) {
    windowFrame(g, W / 2 + 0.23, H1 + 1.3, z, 0.8, 0.9, Math.PI / 2);
    windowFrame(g, -W / 2 - 0.23, H1 + 1.3, z, 0.8, 0.9, -Math.PI / 2);
  }
  if (opts.sign) {
    box(g, 3.2, 0.8, 0.08, tmat(T.boardText(opts.sign, '#3d2a1c', '#f6e3b0')), 0, 2.75, D / 2 + 0.3);
  }
  bake(g);
  return { group: g, colliders: [[-W / 2 - 0.2, W / 2 + 0.2, -D / 2 - 0.2, D / 2 + 0.2]], interacts: [], w: W, d: D };
}

// Dükkân tezgâhı (ön tarafa) + satıcı
export function shopCounter(awningA = '#f1e6cf', awningB = '#8a2a2a') {
  const g = new THREE.Group();
  const wood = tmat(T.planks('#8a5a34', 5));
  box(g, 3.0, 1.0, 0.8, wood, 0, 0.5, 0);
  box(g, 3.2, 0.08, 1.0, tmat(T.planks('#6b4a2f', 5)), 0, 1.04, 0);
  for (const x of [-1.5, 1.5]) cyl(g, 0.06, 0.06, 2.6, 6, mat('#5a3d28'), x, 1.3, 0.4);
  const aw = box(g, 3.4, 0.03, 1.6, tmat(T.stripes(awningA, awningB, 10)), 0, 2.55, 0.1, 0.25);
  aw.material.side = THREE.DoubleSide;
  for (let i = 0; i < 6; i++) {
    box(g, 0.35, 0.25, 0.3, tmat(T.planks('#a07a4a', 3)), -1.1 + i * 0.44, 1.2, -0.1);
  }
  bake(g);
  return g;
}

// ----------------------------------------------------------------- NPC (satıcı)
export function npc(seed = 1) {
  const r = mulberry32(seed * 7 + 3);
  const g = new THREE.Group();
  const skin = mat(['#e8b896', '#c98e6a', '#a86f4c', '#f1c7a8'][Math.floor(r() * 4)]);
  const shirt = mat(['#6a8caf', '#a0522d', '#5c7a4a', '#8e5a8a', '#c9a24a'][Math.floor(r() * 5)]);
  const pants = mat(['#3a3f4a', '#5a4632', '#2f3a2a'][Math.floor(r() * 3)]);
  const hair = mat(['#2a1d14', '#5a3a22', '#8a8a8a', '#c9a35a'][Math.floor(r() * 4)]);
  for (const s of [-1, 1]) box(g, 0.2, 0.85, 0.22, pants, s * 0.13, 0.43, 0);
  const body = new THREE.Group(); body.position.y = 0.85; g.add(body);
  box(body, 0.55, 0.7, 0.3, shirt, 0, 0.35, 0);
  box(body, 0.5, 0.55, 0.02, mat('#f1ead8'), 0, 0.28, 0.16);
  const head = new THREE.Group(); head.position.y = 0.85; body.add(head);
  box(head, 0.32, 0.36, 0.32, skin, 0, 0.12, 0);
  box(head, 0.34, 0.12, 0.34, hair, 0, 0.32, 0);
  box(head, 0.34, 0.25, 0.08, hair, 0, 0.18, -0.15);
  for (const s of [-1, 1]) box(head, 0.05, 0.05, 0.02, mat('#1a1a1a'), s * 0.07, 0.15, 0.165);
  if (r() < 0.5) box(head, 0.1, 0.04, 0.02, mat('#4a2a1a'), 0, 0.04, 0.165);
  if (r() < 0.5) {
    cyl(head, 0.3, 0.3, 0.03, 10, mat('#c9a35a'), 0, 0.33, 0);
    cyl(head, 0.18, 0.2, 0.18, 10, mat('#c9a35a'), 0, 0.42, 0);
  }
  const arms = [];
  for (const s of [-1, 1]) {
    const a = new THREE.Group(); a.position.set(s * 0.36, 0.65, 0);
    box(a, 0.15, 0.6, 0.17, shirt, 0, -0.28, 0);
    box(a, 0.12, 0.14, 0.13, skin, 0, -0.62, 0);
    body.add(a); arms.push(a);
  }
  shadowAll(g);
  mergeChildren(g);
  let t = r() * 10;
  return {
    group: g,
    update(dt, near) {
      t += dt;
      body.scale.y = 1 + Math.sin(t * 2) * 0.012;
      head.rotation.y = Math.sin(t * 0.5) * 0.4 + (near ? 0 : 0);
      arms[1].rotation.x = near ? -0.6 + Math.sin(t * 6) * 0.5 : Math.sin(t * 0.8) * 0.05;
      arms[1].rotation.z = near ? 0.3 : 0;
    },
  };
}
function shadowAll(g) { g.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } }); }

// ----------------------------------------------------------------- ÇEŞME
export function fountain() {
  const g = new THREE.Group();
  const st = tmat(T.stone('#b3ada2'));
  cyl(g, 3.0, 3.1, 0.7, 16, st, 0, 0.35, 0, 0, 0, 0, 2);
  const wat = new THREE.Mesh(new THREE.CircleGeometry(2.7, 24), mat('#3f8fa8', { roughness: 0.05, metalness: 0.2 }));
  wat.rotation.x = -Math.PI / 2; wat.position.y = 0.62; g.add(wat);
  cyl(g, 0.35, 0.5, 1.8, 10, st, 0, 1.2, 0);
  cyl(g, 1.0, 0.6, 0.3, 12, st, 0, 2.1, 0);
  cyl(g, 0.15, 0.2, 0.7, 8, st, 0, 2.6, 0);
  const jet = dynamic(new THREE.Mesh(new THREE.ConeGeometry(0.9, 1.2, 12, 1, true), mat('#cfe8f2', { transparent: true, opacity: 0.45, side: THREE.DoubleSide })));
  jet.position.y = 2.2; jet.rotation.x = Math.PI;
  g.add(jet);
  bake(g);
  return {
    group: g, colliders: [[-3, 3, -3, 3]], interacts: [{ id: 'fountain', x: 0, y: 1, z: 3.2, r: 2, label: 'Çeşme (su iç, enerji +5)' }],
    update(dt, ctx) { jet.scale.y = 1 + Math.sin(ctx.time * 8) * 0.05; jet.visible = ctx.season !== 3; },
  };
}

// ----------------------------------------------------------------- İLAN PANOSU
export function jobBoard() {
  const g = new THREE.Group();
  const w = mat('#5a3d28');
  for (const s of [-1, 1]) box(g, 0.15, 2.4, 0.15, w, s * 1.1, 1.2, 0);
  box(g, 2.4, 1.4, 0.1, tmat(T.planks('#8a5a34', 6, { nails: false })), 0, 1.5, 0);
  const roofM = tmat(T.roofTiles('#8a3a2c'));
  box(g, 2.8, 0.08, 0.7, roofM, 0, 2.45, 0.05, 0.3);
  const r = mulberry32(5);
  for (let i = 0; i < 6; i++) {
    const p = box(g, 0.45, 0.55, 0.01, mat(['#f6f1e1', '#f3e6c4', '#e9f1f6'][i % 3]), -0.8 + (i % 3) * 0.8, 1.8 - Math.floor(i / 3) * 0.65, 0.06);
    p.rotation.z = (r() - 0.5) * 0.15;
  }
  box(g, 2.0, 0.35, 0.05, tmat(T.boardText(['İŞ İLANLARI'], '#2f4a3a')), 0, 2.15, 0.08);
  bake(g);
  return { group: g, colliders: [[-1.2, 1.2, -0.2, 0.2]], interacts: [{ id: 'jobs', x: 0, y: 1.4, z: 0.9, r: 2.2, label: 'İş İlanları Panosu' }] };
}

// ----------------------------------------------------------------- İSKELE
export function dock(length = 14) {
  const g = new THREE.Group();
  const deck = tmat(T.planks('#8a6a48', 6, { weathered: true }));
  const post = tmat(T.bark('#5a4232'));
  box(g, 2.4, 0.15, length, deck, 0, 0.6, -length / 2, 0, Math.PI / 2 * 0, 0, 2);
  for (let z = 0; z <= length; z += 2.5) for (const s of [-1, 1]) cyl(g, 0.12, 0.12, 3.6, 6, post, s * 1.15, -1.1, -z);
  for (const s of [-1, 1]) box(g, 0.08, 0.08, length, mat('#6b4a2f'), s * 1.15, 1.25, -length / 2);
  // fener
  cyl(g, 0.05, 0.05, 1.6, 6, post, 1.1, 1.4, -length + 0.3);
  const lan = box(g, 0.2, 0.3, 0.2, mat('#ffd27a', { emissive: '#ffb347', emissiveIntensity: 0 }), 1.1, 2.25, -length + 0.3);
  dynamic(lan);
  bake(g);
  return {
    group: g, colliders: [],
    interacts: [{ id: 'fishspot', x: 0, y: 1, z: -length + 1, r: 2.5, label: 'İskele (olta ile balık tut)' }],
    deckHeight: 0.68, length,
    update(dt, ctx) { lan.material.emissiveIntensity = ctx.night ? 2 : 0; },
  };
}

// ----------------------------------------------------------------- KAMELYA (ada)
export function gazebo() {
  const g = new THREE.Group();
  const w = tmat(T.planks('#e9e2d3', 6));
  cyl(g, 2.8, 2.8, 0.25, 6, w, 0, 0.12, 0);
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2;
    box(g, 0.16, 2.6, 0.16, mat('#f1ece2'), Math.cos(a) * 2.5, 1.4, Math.sin(a) * 2.5);
  }
  cone(g, 3.4, 1.8, 6, tmat(T.roofTiles('#3e6a8a')), 0, 3.6, 0);
  for (let i = 0; i < 5; i++) {
    const a = (i / 6) * Math.PI * 2 + Math.PI / 6;
    const b = box(g, 2.2, 0.08, 0.5, w, Math.cos(a) * 2.1, 0.55, Math.sin(a) * 2.1);
    b.rotation.y = -a + Math.PI / 2;
  }
  bake(g);
  return { group: g, colliders: [], interacts: [{ id: 'rest', x: 0, y: 0.6, z: 0, r: 2.5, label: 'Kamelya (dinlen, enerji +)' }] };
}

export function bench() {
  const g = new THREE.Group();
  const w = tmat(T.planks('#8a5a34', 4, { nails: false }));
  box(g, 1.8, 0.08, 0.45, w, 0, 0.45, 0);
  box(g, 1.8, 0.4, 0.06, w, 0, 0.75, -0.22, -0.15);
  for (const s of [-1, 1]) box(g, 0.08, 0.45, 0.45, mat('#3a3a3a', { metalness: 0.5 }), s * 0.8, 0.22, 0);
  bake(g);
  return { group: g, colliders: [[-0.9, 0.9, -0.3, 0.25]], interacts: [{ id: 'rest', x: 0, y: 0.5, z: 0.5, r: 1.6, label: 'Bank (manzarayı izle, dinlen)' }] };
}

export function flag() {
  const g = new THREE.Group();
  cyl(g, 0.05, 0.06, 6, 8, mat('#d8d8d4', { metalness: 0.6 }), 0, 3, 0);
  const tex = (() => {
    const c = document.createElement('canvas'); c.width = 240; c.height = 160;
    const x = c.getContext('2d');
    x.fillStyle = '#e30a17'; x.fillRect(0, 0, 240, 160);
    x.fillStyle = '#fff'; x.beginPath(); x.arc(85, 80, 40, 0, 7); x.fill();
    x.fillStyle = '#e30a17'; x.beginPath(); x.arc(95, 80, 32, 0, 7); x.fill();
    x.fillStyle = '#fff'; x.beginPath();
    for (let i = 0; i < 10; i++) {
      const a = -Math.PI / 2 + (i * Math.PI) / 5, rr = i % 2 ? 7 : 17;
      x.lineTo(135 + Math.cos(a + Math.PI / 2) * rr, 80 + Math.sin(a + Math.PI / 2) * rr);
    }
    x.fill();
    const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
  })();
  const geo = new THREE.PlaneGeometry(1.8, 1.2, 12, 4);
  geo.translate(0.9, 0, 0);
  const cloth = dynamic(new THREE.Mesh(geo, new THREE.MeshStandardMaterial({ map: tex, side: THREE.DoubleSide, roughness: 0.9 })));
  cloth.position.set(0.05, 5.3, 0);
  g.add(cloth);
  const base = geo.attributes.position.array.slice();
  bake(g);
  return {
    group: g, colliders: [[-0.2, 0.2, -0.2, 0.2]], interacts: [],
    update(dt, ctx) {
      const p = geo.attributes.position.array;
      for (let i = 0; i < p.length; i += 3) {
        const x = base[i];
        p[i + 2] = Math.sin(x * 3 - ctx.time * 6) * 0.12 * x;
      }
      geo.attributes.position.needsUpdate = true;
    },
  };
}

export function cairn() {
  const g = new THREE.Group();
  const r = mulberry32(3);
  let y = 0;
  for (let i = 0; i < 7; i++) {
    const s = 0.55 - i * 0.06;
    sphere(g, s, mat('#8d8a83', { flat: true }), (r() - 0.5) * 0.1, y + s * 0.4, (r() - 0.5) * 0.1, 1.2, 0.55, 1.1, 0);
    y += s * 0.75;
  }
  bake(g);
  return { group: g, colliders: [[-0.6, 0.6, -0.6, 0.6]], interacts: [] };
}

export function signpost(entries) {
  const g = new THREE.Group();
  cyl(g, 0.08, 0.09, 2.6, 8, tmat(T.bark('#6b4a2f')), 0, 1.3, 0);
  entries.forEach((e, i) => {
    const arm = new THREE.Group();
    arm.position.y = 2.25 - i * 0.42;
    arm.rotation.y = e.angle;
    const b = box(arm, 1.5, 0.32, 0.05, tmat(T.boardText([e.text], '#7a5232', '#fff4d6')), 0.8, 0, 0);
    g.add(arm);
  });
  bake(g);
  return { group: g, colliders: [[-0.15, 0.15, -0.15, 0.15]], interacts: [] };
}

export function lampPost() {
  const g = new THREE.Group();
  const iron = mat('#2f3236', { metalness: 0.6, roughness: 0.4 });
  cyl(g, 0.07, 0.1, 3.4, 8, iron, 0, 1.7, 0);
  box(g, 0.35, 0.05, 0.35, iron, 0, 3.4, 0);
  const lamp = dynamic(box(g, 0.28, 0.4, 0.28, mat('#ffe2a0', { emissive: '#ffb347', emissiveIntensity: 0 }), 0, 3.65, 0));
  cone(g, 0.3, 0.25, 4, iron, 0, 3.98, 0, 0, Math.PI / 4);
  bake(g);
  return {
    group: g, colliders: [[-0.15, 0.15, -0.15, 0.15]], interacts: [],
    update(dt, ctx) { lamp.material.emissiveIntensity = ctx.night ? 2.5 : 0; },
  };
}

// ----------------------------------------------------------------- ORMAN KULÜBESİ
export function cabin() {
  const g = new THREE.Group();
  const logM = tmat(T.bark('#7a5434'));
  const W = 6, D = 5;
  for (let i = 0; i < 9; i++) {
    const y = 0.15 + i * 0.28;
    cyl(g, 0.15, 0.15, W + 0.5, 7, logM, 0, y, -D / 2, 0, 0, Math.PI / 2);
    cyl(g, 0.15, 0.15, D + 0.5, 7, logM, -W / 2, y, 0, Math.PI / 2);
    cyl(g, 0.15, 0.15, D + 0.5, 7, logM, W / 2, y, 0, Math.PI / 2);
    if (y > 2.1 || true) {
      cyl(g, 0.15, 0.15, W / 2 - 0.6, 7, logM, -W / 4 - 0.3, y, D / 2, 0, 0, Math.PI / 2);
      cyl(g, 0.15, 0.15, W / 2 - 0.6, 7, logM, W / 4 + 0.3, y, D / 2, 0, 0, Math.PI / 2);
    }
  }
  box(g, 1.2, 0.5, 0.3, logM, 0, 2.4, D / 2);
  const roofM = tmat(T.planks('#5a4a3a', 8, { weathered: true }));
  const half = D / 2 + 0.6, RH = 1.8, ang = Math.atan2(RH, half), len = Math.hypot(half, RH);
  for (const s of [-1, 1]) box(g, W + 1.2, 0.14, len, roofM, 0, 2.6 + RH / 2, s * half / 2, s * ang, 0, 0, 2);
  for (const s of [-1, 1]) prism(g, [[-D / 2, 0], [D / 2, 0], [0, RH]], 0.2, logM, s * W / 2, 2.6, 0, Math.PI / 2, 1.5);
  box(g, 1.0, 2.0, 0.08, tmat(T.planks('#4e3322', 4)), 0, 1.0, D / 2 + 0.05);
  windowFrame(g, -1.8, 1.5, D / 2 + 0.17, 0.7, 0.6);
  box(g, W - 0.5, 0.05, D - 0.5, tmat(T.planks('#8a6a48', 6)), 0, 0.02, 0);
  bake(g);
  return { group: g, colliders: [[-W / 2 - 0.2, W / 2 + 0.2, -D / 2 - 0.2, D / 2 + 0.2]], interacts: [{ id: 'rest', x: 0, y: 1, z: D / 2 + 0.9, r: 1.8, label: 'Oduncu Kulübesi (dinlen)' }] };
}

// ----------------------------------------------------------------- SERA
export function greenhouse() {
  const g = new THREE.Group();
  const W = 8, D = 12, H = 2.4;
  const fr = mat('#f2f2ee', { metalness: 0.3 });
  const glass = mat('#cfe6ee', { transparent: true, opacity: 0.28, roughness: 0.1, side: THREE.DoubleSide });
  for (let z = -D / 2; z <= D / 2 + 0.01; z += 2) {
    for (const s of [-1, 1]) box(g, 0.08, H, 0.08, fr, s * W / 2, H / 2, z);
    for (const s of [-1, 1]) beam(g, [s * W / 2, H, z], [0, H + 1.6, z], 0.08, 0.08, fr);
  }
  box(g, 0.1, 0.1, D, fr, 0, H + 1.6, 0);
  for (const s of [-1, 1]) {
    const side = box(g, 0.02, H, D, glass, s * W / 2, H / 2, 0);
    side.castShadow = false;
    beam(g, [s * W / 2, H, -D / 2], [0, H + 1.6, -D / 2], 0.02, 0.02, glass);
    const rp = box(g, Math.hypot(W / 2, 1.6), 0.02, D, glass, s * W / 4, H + 0.8, 0, 0, 0, -s * Math.atan2(1.6, W / 2));
  }
  box(g, W, H, 0.02, glass, 0, H / 2, -D / 2);
  prism(g, [[-W / 2, 0], [W / 2, 0], [0, 1.6]], 0.02, glass, 0, H, -D / 2, 0, 1);
  prism(g, [[-W / 2, 0], [W / 2, 0], [0, 1.6]], 0.02, glass, 0, H, D / 2, 0, 1);
  for (const s of [-1, 1]) box(g, (W - 1.6) / 2, H, 0.02, glass, s * (W / 4 + 0.4), H / 2, D / 2);
  bake(g, { cast: false });
  g.traverse((o) => { if (o.isMesh && o.material === glass) o.castShadow = false; });
  return {
    group: g,
    colliders: [[-W / 2 - 0.1, -W / 2 + 0.1, -D / 2, D / 2], [W / 2 - 0.1, W / 2 + 0.1, -D / 2, D / 2], [-W / 2, W / 2, -D / 2 - 0.1, -D / 2 + 0.1],
      [-W / 2, -0.8, D / 2 - 0.1, D / 2 + 0.1], [0.8, W / 2, D / 2 - 0.1, D / 2 + 0.1]],
    interacts: [],
  };
}

export function stoneDisc(r, color = '#b8b0a2') {
  const tex = T.stone(color).clone();
  tex.needsUpdate = true;
  tex.repeat.set(r / 2, r / 2);
  const m = new THREE.Mesh(new THREE.CircleGeometry(r, 32), new THREE.MeshStandardMaterial({ map: tex, roughness: 0.9 }));
  m.rotation.x = -Math.PI / 2;
  m.receiveShadow = true;
  return m;
}
