// Karakter ve yaratık modelleri: hiyerarşik "kemik" pivotlarıyla prosedürel olarak kurulur
import * as THREE from 'three';
import { tex } from './textures.js';
import { jitter } from './props.js';

const gc = {};
const G = (key, fn) => gc[key] || (gc[key] = fn());
export function newMats() {
  const list = [];
  const mk = (o) => { const m = new THREE.MeshStandardMaterial({ roughness: 0.8, ...o }); m.userData.baseEmissive = m.emissive.clone(); m.userData.baseEI = m.emissiveIntensity; list.push(m); return m; };
  return { list, mk };
}
export function mesh(geo, mat, parent, x = 0, y = 0, z = 0, rx = 0, ry = 0, rz = 0, sx = 1, sy = sx, sz = sx) {
  const m = new THREE.Mesh(geo, mat); m.position.set(x, y, z); m.rotation.set(rx, ry, rz); m.scale.set(sx, sy, sz); m.castShadow = true; m.receiveShadow = true; parent.add(m); return m;
}
export function piv(parent, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); parent.add(g); return g; }
const cap = (r, l, rs = 8) => G(`cap${r}_${l}_${rs}`, () => new THREE.CapsuleGeometry(r, l, 3, rs));
const sph = (r, w = 12, h = 10) => G(`sph${r}_${w}`, () => new THREE.SphereGeometry(r, w, h));
const box = (x, y, z) => G(`box${x}_${y}_${z}`, () => new THREE.BoxGeometry(x, y, z));
const cyl = (a, b, h, s = 10, open = false) => G(`cyl${a}_${b}_${h}_${s}_${open}`, () => new THREE.CylinderGeometry(a, b, h, s, 1, open));
const cone = (r, h, s = 8) => G(`cone${r}_${h}_${s}`, () => new THREE.ConeGeometry(r, h, s));
const rockG = (r, seed) => G(`rock${r}_${seed}`, () => jitter(new THREE.DodecahedronGeometry(r, 1), 0.25, 1.2 / r, seed));

// ================= SİLAHLAR =================
export function makeWeapon(kind, colorHex = 0xc8d0d8, glow = 0) {
  const g = new THREE.Group(); const { mk, list } = newMats();
  const metal = mk({ color: colorHex, map: tex('metal'), metalness: 0.9, roughness: 0.3, emissive: glow ? colorHex : 0, emissiveIntensity: glow });
  const grip = mk({ color: 0x4a2e1c, map: tex('leather'), roughness: 0.9 });
  const goldM = mk({ color: 0xe0b050, metalness: 1, roughness: 0.35 });
  const tip = new THREE.Object3D(), base = new THREE.Object3D(); g.add(tip, base);
  g.userData = { tip, base, mats: list };
  if (kind === 'sword') {
    const blade = new THREE.Shape(); blade.moveTo(-0.045, 0); blade.lineTo(0.045, 0); blade.lineTo(0.04, 0.82); blade.lineTo(0, 0.95); blade.lineTo(-0.04, 0.82); blade.closePath();
    const bg = G('swordBlade', () => { const e = new THREE.ExtrudeGeometry(blade, { depth: 0.012, bevelEnabled: true, bevelThickness: 0.008, bevelSize: 0.008, bevelSegments: 1 }); e.translate(0, 0, -0.006); return e; });
    mesh(bg, metal, g, 0, 0.1, 0);
    mesh(box(0.3, 0.045, 0.06), goldM, g, 0, 0.09, 0);
    mesh(cyl(0.022, 0.025, 0.2, 6), grip, g, 0, -0.02, 0);
    mesh(sph(0.04, 8, 6), goldM, g, 0, -0.13, 0);
    base.position.set(0, 0.2, 0); tip.position.set(0, 1.05, 0);
  } else if (kind === 'greatsword') {
    const blade = new THREE.Shape(); blade.moveTo(-0.07, 0); blade.lineTo(0.07, 0); blade.lineTo(0.06, 1.25); blade.lineTo(0, 1.42); blade.lineTo(-0.06, 1.25); blade.closePath();
    const bg = G('gswordBlade', () => { const e = new THREE.ExtrudeGeometry(blade, { depth: 0.02, bevelEnabled: true, bevelThickness: 0.01, bevelSize: 0.01, bevelSegments: 1 }); e.translate(0, 0, -0.01); return e; });
    mesh(bg, metal, g, 0, 0.14, 0); mesh(box(0.42, 0.06, 0.08), goldM, g, 0, 0.12, 0); mesh(cyl(0.028, 0.03, 0.3, 6), grip, g, 0, -0.04, 0);
    base.position.set(0, 0.3, 0); tip.position.set(0, 1.5, 0);
  } else if (kind === 'axe') {
    mesh(cyl(0.035, 0.04, 1.3, 6), grip, g, 0, 0.35, 0);
    const hs = new THREE.Shape(); hs.moveTo(0, 0.12); hs.quadraticCurveTo(0.3, 0.3, 0.42, 0.28); hs.quadraticCurveTo(0.34, 0, 0.42, -0.28); hs.quadraticCurveTo(0.3, -0.3, 0, -0.12); hs.closePath();
    const hg = G('axeHead', () => { const e = new THREE.ExtrudeGeometry(hs, { depth: 0.03, bevelEnabled: true, bevelThickness: 0.01, bevelSize: 0.01, bevelSegments: 1 }); e.translate(0, 0, -0.015); return e; });
    mesh(hg, metal, g, 0.02, 0.85, 0); mesh(hg, metal, g, -0.02, 0.85, 0, 0, Math.PI, 0);
    base.position.set(0, 0.6, 0); tip.position.set(0.42, 0.9, 0);
  } else if (kind === 'dagger') {
    mesh(cone(0.04, 0.4, 4), metal, g, 0, 0.3, 0, 0, Math.PI / 4, 0, 1, 1, 0.3); mesh(box(0.14, 0.03, 0.04), goldM, g, 0, 0.1, 0); mesh(cyl(0.02, 0.02, 0.14, 5), grip, g, 0, 0.02, 0);
    base.position.set(0, 0.12, 0); tip.position.set(0, 0.5, 0);
  } else if (kind === 'club') {
    mesh(cyl(0.06, 0.16, 1.2, 7), mk({ color: 0x6a4a2a, map: tex('bark'), roughness: 1 }), g, 0, 0.5, 0);
    for (let i = 0; i < 5; i++) mesh(cone(0.05, 0.16, 5), metal, g, Math.cos(i * 1.3) * 0.15, 0.8 + i * 0.07, Math.sin(i * 1.3) * 0.15, Math.sin(i * 1.3) * 1.5, 0, -Math.cos(i * 1.3) * 1.5);
    base.position.set(0, 0.4, 0); tip.position.set(0, 1.1, 0);
  } else if (kind === 'staff') {
    mesh(cyl(0.028, 0.035, 1.8, 6), mk({ color: 0x5a3a22, map: tex('bark'), roughness: 0.9 }), g, 0, 0.5, 0);
    for (let i = 0; i < 3; i++) mesh(cone(0.03, 0.35, 4), goldM, g, Math.cos(i * 2.1) * 0.07, 1.45, Math.sin(i * 2.1) * 0.07, Math.sin(i * 2.1) * -0.5, 0, Math.cos(i * 2.1) * 0.5);
    const orb = mesh(sph(0.1, 14, 10), mk({ color: colorHex, emissive: colorHex, emissiveIntensity: 3 + glow, roughness: 0.2 }), g, 0, 1.55, 0);
    g.userData.orb = orb; base.position.set(0, 1.2, 0); tip.position.set(0, 1.55, 0);
  } else if (kind === 'bow') {
    const bowM = mk({ color: 0x6a4424, map: tex('planks'), roughness: 0.7 });
    const curve = new THREE.QuadraticBezierCurve3(new THREE.Vector3(0, -0.7, 0), new THREE.Vector3(0, 0, 0.35), new THREE.Vector3(0, 0.7, 0));
    mesh(G('bowLimb', () => new THREE.TubeGeometry(curve, 16, 0.025, 5)), bowM, g);
    mesh(cyl(0.035, 0.035, 0.2, 6), grip, g, 0, 0, 0.17);
    const sg = new THREE.BufferGeometry(); sg.setAttribute('position', new THREE.Float32BufferAttribute([0, -0.7, 0, 0, 0, 0, 0, 0.7, 0], 3));
    const str = new THREE.Line(sg, new THREE.LineBasicMaterial({ color: 0xeeeeee })); g.add(str);
    const arrow = new THREE.Group(); mesh(cyl(0.008, 0.008, 0.75, 4), bowM, arrow, 0, 0, 0.3, Math.PI / 2); mesh(cone(0.025, 0.08, 4), metal, arrow, 0, 0, 0.7, Math.PI / 2);
    arrow.visible = false; g.add(arrow);
    g.userData.setDraw = (d) => { const p = sg.attributes.position; p.setZ(1, -d * 0.45); p.needsUpdate = true; arrow.position.z = -d * 0.45; arrow.visible = d > 0.05; };
    base.position.set(0, 0, 0.2); tip.position.set(0, 0.7, 0);
  } else if (kind === 'shield') {
    const wood = mk({ color: 0x7a2020, map: tex('planks'), roughness: 0.8 });
    mesh(cyl(0.34, 0.34, 0.06, 16), wood, g, 0, 0, 0, Math.PI / 2);
    mesh(G('shieldRim', () => new THREE.TorusGeometry(0.34, 0.025, 5, 20)), metal, g, 0, 0, 0.0);
    mesh(sph(0.08, 10, 6), goldM, g, 0, 0, 0.04, 0, 0, 0, 1, 1, 0.6);
  } else if (kind === 'spear') {
    mesh(cyl(0.025, 0.03, 2.2, 6), grip, g, 0, 0.6, 0); mesh(cone(0.06, 0.35, 4), metal, g, 0, 1.85, 0);
    base.position.set(0, 1.4, 0); tip.position.set(0, 2.0, 0);
  } else if (kind === 'scythe') {
    mesh(cyl(0.03, 0.035, 2.0, 6), grip, g, 0, 0.6, 0);
    const bs = new THREE.Shape(); bs.moveTo(0, 0); bs.quadraticCurveTo(0.5, 0.25, 0.95, -0.2); bs.quadraticCurveTo(0.45, 0.05, 0, -0.12); bs.closePath();
    mesh(G('scytheB', () => new THREE.ExtrudeGeometry(bs, { depth: 0.02, bevelEnabled: false })), metal, g, 0, 1.55, -0.01);
    base.position.set(0, 1.5, 0); tip.position.set(0.9, 1.35, 0);
  }
  g.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return g;
}

// ================= İNSANSI =================
// o: { s, skin, cloth, cloth2, pants, boots, armor, armorCol, head, hair, beard, helmet, hood, hat, robe, cape, bulk, eyes, eyeGlow, tail, horns, ears }
export function buildHumanoid(o) {
  const s = o.s || 1, b = o.bulk || 1;
  const { mk, list } = newMats();
  const skinT = o.skinTex ? tex(o.skinTex) : tex('skin');
  const skin = mk({ color: o.skin ?? 0xe0ac86, map: skinT, roughness: o.skinRough ?? 0.75, emissive: o.skinEmissive || 0, emissiveIntensity: o.skinEI || 0, ...(o.skinExtra || {}) });
  const cloth = mk({ color: o.cloth ?? 0x6a4a3a, map: tex('cloth'), roughness: 1 });
  const cloth2 = mk({ color: o.cloth2 ?? o.cloth ?? 0x6a4a3a, map: tex('cloth'), roughness: 1 });
  const pants = mk({ color: o.pants ?? 0x4a3a2e, map: tex('cloth'), roughness: 1 });
  const boots = mk({ color: o.boots ?? 0x3a2618, map: tex('leather'), roughness: 0.8 });
  const armor = mk({ color: o.armorCol ?? 0xb8c0c8, map: tex('metal'), metalness: 0.85, roughness: 0.35 });
  const leather = mk({ color: o.leather ?? 0x6a4428, map: tex('leather'), roughness: 0.85 });
  const eyeM = mk({ color: o.eyeGlow ?? 0x111111, emissive: o.eyeGlow ?? 0x000000, emissiveIntensity: o.eyeGlow ? 4 : 0, roughness: 0.3 });
  const hairM = mk({ color: o.hair ?? 0x3a2414, map: tex('fur'), roughness: 0.9 });
  const root = new THREE.Group(); const p = {}; p.root = root;
  const inner = piv(root); inner.scale.setScalar(s); p.inner = inner;
  const body = piv(inner, 0, 0.9, 0); p.body = body;
  const hips = piv(body, 0, 0.05, 0); p.hips = hips;
  const skel = o.head === 'skull', golem = o.head === 'golem', treant = o.head === 'treant';
  // gövde
  const spine = piv(hips, 0, 0.06, 0); p.spine = spine;
  const chest = piv(spine, 0, 0.26, 0); p.chest = chest;
  const torsoMat = skel ? skin : o.robe ? cloth : o.shirtless ? skin : cloth;
  if (skel) {
    mesh(cyl(0.035, 0.035, 0.34, 6), skin, spine, 0, 0.12, -0.03);
    for (let i = 0; i < 4; i++) mesh(G('rib' + i, () => new THREE.TorusGeometry(0.15 - i * 0.012, 0.018, 4, 12, Math.PI * 1.5)), skin, chest, 0, 0.1 - i * 0.07, 0.02, Math.PI / 2, 0, Math.PI * 0.25);
    mesh(cyl(0.16, 0.12, 0.08, 8), skin, hips, 0, 0, 0);
  } else if (golem) {
    mesh(rockG(0.34, 1), skin, chest, 0, 0.05, 0, 0, 0, 0, 1.3 * b, 1, 0.9);
    mesh(rockG(0.24, 2), skin, spine, 0, 0.05, 0, 0, 0, 0, 1.1 * b, 1, 0.9);
    mesh(rockG(0.2, 3), skin, hips, 0, 0, 0, 0, 0, 0, 1.2 * b, 0.8, 1);
  } else {
    mesh(cap(0.19, 0.12), torsoMat, spine, 0, 0.1, 0, 0, 0, 0, 1.05 * b, 1, 0.78);
    mesh(cap(0.21, 0.14), torsoMat, chest, 0, 0.06, 0, 0, 0, 0, 1.15 * b, 1, 0.8);
    mesh(cap(0.17, 0.1), pants, hips, 0, 0, 0, 0, 0, 0, 1.1 * b, 0.8, 0.8);
    if (!o.noBelt) mesh(G('belt', () => new THREE.TorusGeometry(0.19, 0.035, 5, 16)), leather, hips, 0, 0.07, 0, Math.PI / 2, 0, 0, 1.08 * b, 0.85, 1);
    if (o.armor) {
      mesh(cap(0.225, 0.12), armor, chest, 0, 0.08, 0.01, 0, 0, 0, 1.18 * b, 1, 0.84);
      for (const sx of [-1, 1]) mesh(sph(0.13, 10, 8), armor, chest, sx * 0.25 * b, 0.19, 0, 0, 0, sx * -0.4, 1.1, 0.8, 1.1);
    }
    if (o.robe) mesh(cyl(0.2 * b, 0.36 * b, 0.85, 12, true), cloth2, hips, 0, -0.4, 0);
    else if (o.skirt) mesh(cyl(0.2 * b, 0.28 * b, 0.36, 10, true), cloth2, hips, 0, -0.14, 0);
    if (o.shirtless && o.fur) for (let i = 0; i < 6; i++) mesh(cone(0.09, 0.26, 5), hairM, chest, Math.cos(i) * 0.18 * b, 0.25, -0.1 + Math.sin(i) * 0.06, -0.6, 0, Math.cos(i) * 0.5);
  }
  if (treant) {
    for (let i = 0; i < 5; i++) { const a = -1 + i * 0.5; const br = piv(chest, Math.sin(a) * 0.2, 0.3, -0.1); br.rotation.set(-0.4, 0, a * 0.8); mesh(cyl(0.02, 0.06, 0.8, 5), skin, br, 0, 0.4, 0); mesh(G('tleaf' + i, () => jitter(new THREE.IcosahedronGeometry(0.28, 1), 0.3, 3, i)), cloth, br, 0, 0.85, 0); }
  }
  if (o.cape) {
    const capeM = mk({ color: o.cape, map: tex('cloth'), roughness: 1, side: THREE.DoubleSide });
    let par = piv(chest, 0, 0.22, -0.17); p.cape = [];
    for (let i = 0; i < 3; i++) { const seg = piv(par, 0, i === 0 ? 0 : -0.27, 0); mesh(box(0.42 - i * 0.02 + (i ? 0.06 : 0), 0.28, 0.02), capeM, seg, 0, -0.14, 0); p.cape.push(seg); par = seg; }
  }
  if (o.quiver) { const qv = mesh(cyl(0.07, 0.06, 0.5, 8), leather, chest, 0.1, 0.05, -0.2, 0.2, 0, -0.5); for (let i = 0; i < 4; i++) mesh(box(0.015, 0.2, 0.06), mk({ color: 0xe8e0d0 }), qv, (i - 1.5) * 0.025, 0.3, 0); }
  if (o.wings) {
    const wm = mk({ color: o.wings, roughness: 0.8, side: THREE.DoubleSide, emissive: o.wingGlow || 0, emissiveIntensity: o.wingGlow ? 1 : 0 });
    p.wings = [];
    for (const sx of [-1, 1]) {
      const w = piv(chest, sx * 0.1, 0.15, -0.14); const sh = new THREE.Shape(); sh.moveTo(0, 0); sh.lineTo(0.7, 0.35); sh.lineTo(0.9, -0.1); sh.lineTo(0.6, -0.2); sh.lineTo(0.45, -0.45); sh.lineTo(0.25, -0.2); sh.closePath();
      mesh(G('wingS', () => new THREE.ShapeGeometry(sh)), wm, w, 0, 0, 0, 0, sx > 0 ? 0 : Math.PI, 0, 1.1); p.wings.push(w); w.userData.side = sx;
    }
  }
  // boyun ve kafa
  const neck = piv(chest, 0, 0.26, 0); p.neck = neck;
  if (!golem) mesh(cyl(0.06, 0.07, 0.12, 8), skel ? skin : skin, neck, 0, 0.03, 0);
  const head = piv(neck, 0, 0.1, 0); p.head = head;
  buildHead(o, head, { skin, hairM, eyeM, cloth, cloth2, armor, leather, mk }, b);
  // kollar
  const armR = o.armR ?? 0.06 * (skel ? 0.5 : 1) * Math.sqrt(b);
  for (const side of ['l', 'r']) {
    const sx = side === 'l' ? 1 : -1;
    const sh = piv(chest, sx * 0.25 * b, 0.17, 0); p[side + 'Shoulder'] = sh;
    const ua = o.armLen ?? 1;
    if (golem) mesh(rockG(0.16, 4 + (sx > 0 ? 1 : 0)), skin, sh, 0, -0.14 * ua, 0, 0, 0, 0, 1, 1.3 * ua, 1);
    else mesh(cap(armR, 0.18 * ua), o.sleeves === false || o.shirtless || skel ? skin : cloth, sh, 0, -0.13 * ua, 0);
    const el = piv(sh, 0, -0.29 * ua, 0); p[side + 'Elbow'] = el;
    if (golem) mesh(rockG(0.15, 6 + (sx > 0 ? 1 : 0)), skin, el, 0, -0.14 * ua, 0, 0, 0, 0, 1, 1.3 * ua, 1);
    else mesh(cap(armR * 0.85, 0.17 * ua), skel || o.shirtless || o.bareArms ? skin : cloth, el, 0, -0.13 * ua, 0);
    if (o.armor || o.bracers) mesh(cyl(armR * 1.05, armR * 1.2, 0.14 * ua, 8), o.armor ? armor : leather, el, 0, -0.17 * ua, 0);
    const hand = piv(el, 0, -0.29 * ua, 0); p[side + 'Hand'] = hand;
    if (golem) mesh(rockG(0.13, 8), skin, hand, 0, -0.06, 0);
    else if (o.claws) { mesh(sph(0.055 * Math.sqrt(b), 8, 6), skin, hand, 0, -0.03, 0); for (let i = 0; i < 3; i++) mesh(cone(0.015, 0.12, 4), mk({ color: 0x222222 }), hand, (i - 1) * 0.03, -0.12, 0.02, 0.3); }
    else mesh(box(0.07, 0.1, 0.06), o.gloves ? leather : skin, hand, 0, -0.05, 0, 0, 0, 0, Math.sqrt(b), 1, Math.sqrt(b));
    const sock = piv(hand, 0, -0.07, 0.01); p[side + 'Socket'] = sock;
  }
  // bacaklar
  const legR = (skel ? 0.035 : 0.08) * Math.sqrt(b);
  for (const side of ['l', 'r']) {
    const sx = side === 'l' ? 1 : -1;
    const hp = piv(hips, sx * 0.11 * b, -0.04, 0); p[side + 'Hip'] = hp;
    if (golem) mesh(rockG(0.17, 10 + (sx > 0 ? 1 : 0)), skin, hp, 0, -0.2, 0, 0, 0, 0, 1, 1.3, 1);
    else mesh(cap(legR, 0.28), skel ? skin : o.furLegs ? hairM : pants, hp, 0, -0.2, 0);
    const kn = piv(hp, 0, -0.43, 0); p[side + 'Knee'] = kn;
    if (golem) mesh(rockG(0.15, 12 + (sx > 0 ? 1 : 0)), skin, kn, 0, -0.2, 0, 0, 0, 0, 1, 1.3, 1);
    else mesh(cap(legR * 0.88, 0.26), skel ? skin : o.furLegs ? hairM : o.bareLegs ? skin : pants, kn, 0, -0.19, 0);
    if (!skel && !golem && !o.bareFeet) mesh(cyl(legR * 1.08, legR * 1.15, 0.2, 8), boots, kn, 0, -0.31, 0);
    const ft = piv(kn, 0, -0.42, 0); p[side + 'Foot'] = ft;
    if (golem) mesh(rockG(0.13, 14), skin, ft, 0, -0.02, 0.05, 0, 0, 0, 1, 0.6, 1.3);
    else mesh(box(0.1 * Math.sqrt(b), 0.08, 0.22), skel || o.bareFeet ? skin : boots, ft, 0, -0.03, 0.05);
  }
  if (o.tail) { let par = piv(hips, 0, 0, -0.14); p.tail = []; for (let i = 0; i < 4; i++) { const t = piv(par, 0, 0, i ? -0.18 : 0); mesh(cap(0.05 - i * 0.008, 0.12), skin, t, 0, 0, -0.09, Math.PI / 2); p.tail.push(t); par = t; } }
  root.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return { root, p, mats: list, kind: 'humanoid', height: 1.8 * s, scale: s };
}

function buildHead(o, head, M, b) {
  const { skin, hairM, eyeM, cloth, cloth2, armor, leather, mk } = M;
  const t = o.head || 'human';
  if (t === 'human' || t === 'orc' || t === 'goblin' || t === 'imp') {
    const hs = t === 'goblin' ? 1.15 : t === 'orc' ? 1.08 : 1;
    mesh(sph(0.13, 14, 12), skin, head, 0, 0.12, 0, 0, 0, 0, 0.95 * hs, 1.08 * hs, 1.0 * hs);
    mesh(box(0.05, 0.05, 0.05), skin, head, 0, 0.1, 0.13, 0.4, 0, 0, t === 'goblin' ? 1.2 : 0.8, t === 'goblin' ? 1.6 : 1, t === 'goblin' ? 1.8 : 1);
    for (const sx of [-1, 1]) mesh(sph(0.02, 6, 5), eyeM, head, sx * 0.045 * hs, 0.14, 0.115 * hs);
    if (t === 'goblin' || t === 'imp') for (const sx of [-1, 1]) mesh(cone(0.05, 0.26, 5), skin, head, sx * 0.15, 0.16, -0.01, 0, 0, sx * -1.2);
    if (t === 'orc') { for (const sx of [-1, 1]) mesh(cone(0.018, 0.08, 5), mk({ color: 0xf0e8d0 }), head, sx * 0.05, 0.03, 0.12, 0.2); mesh(box(0.2, 0.05, 0.08), skin, head, 0, 0.19, 0.1); }
    if (t === 'imp') for (const sx of [-1, 1]) mesh(cone(0.03, 0.18, 5), mk({ color: 0x220a05 }), head, sx * 0.07, 0.25, 0, -0.3, 0, sx * -0.4);
  } else if (t === 'skull') {
    mesh(sph(0.12, 12, 10), skin, head, 0, 0.13, 0, 0, 0, 0, 0.9, 1, 1.05);
    const jaw = piv(head, 0, 0.06, 0.02); mesh(box(0.13, 0.04, 0.1), skin, jaw, 0, -0.02, 0.04);
    for (const sx of [-1, 1]) mesh(sph(0.03, 6, 5), mk({ color: 0x000000, emissive: o.eyeGlow || 0x40a0ff, emissiveIntensity: 5 }), head, sx * 0.045, 0.14, 0.09);
  } else if (t === 'yeti' || t === 'giant') {
    mesh(sph(0.16, 14, 12), skin, head, 0, 0.12, 0.02, 0, 0, 0, 1, 1, 1);
    mesh(box(0.2, 0.06, 0.1), skin, head, 0, 0.2, 0.1);
    for (const sx of [-1, 1]) mesh(sph(0.025, 6, 5), eyeM, head, sx * 0.06, 0.15, 0.15);
    if (t === 'yeti') { for (let i = 0; i < 8; i++) mesh(cone(0.06, 0.22, 5), hairM, head, Math.cos(i * 0.8) * 0.14, 0.22, Math.sin(i * 0.8) * 0.1 - 0.03, -0.5 + Math.sin(i * 0.8) * 0.3, 0, Math.cos(i * 0.8) * 0.6); for (const sx of [-1, 1]) mesh(cone(0.04, 0.25, 5), mk({ color: 0x404040 }), head, sx * 0.14, 0.26, 0, 0, 0, sx * -0.8); }
    if (t === 'giant') { for (let i = 0; i < 7; i++) mesh(cone(0.03, 0.3 + (i % 3) * 0.08, 4), mk({ color: 0xcfefff, emissive: 0x70c0ff, emissiveIntensity: 1.2, roughness: 0.2 }), head, (i - 3) * 0.03, 0.0, 0.14, Math.PI - 0.2, 0, (i - 3) * 0.08); mesh(G('crownG', () => new THREE.CylinderGeometry(0.15, 0.16, 0.08, 8, 1, true)), mk({ color: 0x9ad8ff, emissive: 0x3aa0ff, emissiveIntensity: 1.5, metalness: 0.5, roughness: 0.2 }), head, 0, 0.24, 0); for (let i = 0; i < 6; i++) mesh(cone(0.03, 0.14, 4), mk({ color: 0xcfefff, emissive: 0x70c0ff, emissiveIntensity: 2 }), head, Math.cos(i) * 0.15, 0.32, Math.sin(i) * 0.15); }
  } else if (t === 'golem') {
    mesh(rockG(0.16, 20), skin, head, 0, 0.1, 0.03);
    for (const sx of [-1, 1]) mesh(box(0.06, 0.03, 0.03), mk({ color: 0x000000, emissive: o.eyeGlow || 0xff7020, emissiveIntensity: 5 }), head, sx * 0.06, 0.12, 0.17);
  } else if (t === 'treant') {
    mesh(cyl(0.12, 0.16, 0.34, 8), skin, head, 0, 0.14, 0);
    for (const sx of [-1, 1]) mesh(sph(0.03, 6, 5), mk({ color: 0x000000, emissive: 0x80ff40, emissiveIntensity: 5 }), head, sx * 0.06, 0.16, 0.13);
    mesh(box(0.14, 0.03, 0.02), mk({ color: 0x000000, emissive: 0x80ff40, emissiveIntensity: 3 }), head, 0, 0.06, 0.15);
    for (let i = 0; i < 4; i++) mesh(cyl(0.01, 0.03, 0.4, 4), skin, head, (i - 1.5) * 0.07, 0.4, 0, 0, 0, (i - 1.5) * 0.4);
  }
  if (t === 'human') {
    if (o.hair !== null && !o.helmet && !o.hood && !o.hat) mesh(sph(0.14, 12, 8, 0), hairM, head, 0, 0.16, -0.02, 0, 0, 0, 1, 0.8, 1.05);
    if (o.beard) mesh(cone(0.09, 0.22, 7), mk({ color: o.beard, map: tex('fur'), roughness: 1 }), head, 0, 0.0, 0.08, Math.PI + 0.35, 0, 0);
  }
  if (o.helmet) {
    mesh(sph(0.15, 14, 10), armor, head, 0, 0.15, 0, 0, 0, 0, 1, 0.95, 1.05);
    mesh(box(0.03, 0.12, 0.03), armor, head, 0, 0.1, 0.15);
    if (o.plume) { const pm = mk({ color: o.plume, map: tex('fur'), roughness: 1 }); for (let i = 0; i < 4; i++) mesh(box(0.04, 0.12, 0.08), pm, head, 0, 0.3 - i * 0.02, -0.02 - i * 0.07, -0.3 - i * 0.3); }
    if (o.horns) for (const sx of [-1, 1]) mesh(cone(0.035, 0.24, 6), mk({ color: 0xe8dcc0 }), head, sx * 0.15, 0.25, 0, 0, 0, sx * -0.9);
  }
  if (o.hood) { mesh(sph(0.165, 12, 10), cloth2, head, 0, 0.15, -0.02, 0, 0, 0, 1, 1.05, 1.1); mesh(cone(0.1, 0.2, 8), cloth2, head, 0, 0.18, -0.16, -1.6); }
  if (o.hat === 'wizard') { mesh(cyl(0.26, 0.26, 0.02, 16), cloth2, head, 0, 0.23, 0); const c1 = piv(head, 0, 0.23, 0); mesh(cone(0.14, 0.36, 12), cloth2, c1, 0, 0.18, 0); const c2 = piv(c1, 0, 0.34, -0.02); c2.rotation.x = -0.5; mesh(cone(0.06, 0.2, 8), cloth2, c2, 0, 0.1, 0); }
  if (o.hat === 'straw') { mesh(cyl(0.3, 0.3, 0.02, 14), mk({ color: 0xd8b860, map: tex('fur') }), head, 0, 0.22, 0); mesh(cyl(0.13, 0.15, 0.12, 12), mk({ color: 0xd8b860, map: tex('fur') }), head, 0, 0.28, 0); }
  if (o.hat === 'cap') mesh(sph(0.145, 12, 8), cloth2, head, 0, 0.17, 0, 0, 0, 0, 1, 0.7, 1.05);
}

// ================= DÖRT AYAKLI (kurt, cehennem köpeği) =================
export function buildQuadruped(o) {
  const s = o.s || 1; const { mk, list } = newMats();
  const fur = mk({ color: o.color ?? 0x7a7068, map: tex('fur'), normalMap: tex('furNormal'), roughness: 1, emissive: o.emissive || 0, emissiveIntensity: o.ei || 0 });
  const fur2 = mk({ color: o.color2 ?? o.color ?? 0x7a7068, map: tex('fur'), roughness: 1 });
  const eye = mk({ color: 0x000000, emissive: o.eyeGlow ?? 0xffc040, emissiveIntensity: 4 });
  const teeth = mk({ color: 0xf0e8d8 });
  const root = new THREE.Group(); const p = { root };
  const inner = piv(root); inner.scale.setScalar(s); p.inner = inner;
  const body = piv(inner, 0, 0.72, 0); p.body = body;
  mesh(cap(0.25, 0.55), fur, body, 0, 0, -0.05, Math.PI / 2, 0, 0, 1, 1, 1.05);
  mesh(sph(0.3, 12, 10), fur2, body, 0, 0.04, 0.32, 0, 0, 0, 1, 1.05, 1);
  for (let i = 0; i < 6; i++) mesh(cone(0.07, 0.26, 5), fur2, body, 0, 0.22, 0.3 - i * 0.12, -1.2, 0, 0);
  if (o.spikes) for (let i = 0; i < 5; i++) mesh(cone(0.05, 0.3, 5), mk({ color: 0x220804, emissive: 0xff3000, emissiveIntensity: 2 }), body, 0, 0.3, 0.25 - i * 0.14, -0.6);
  const neck = piv(body, 0, 0.12, 0.45); p.neck = neck;
  mesh(cap(0.15, 0.18), fur2, neck, 0, 0.06, 0.08, 1.0);
  const head = piv(neck, 0, 0.14, 0.2); p.head = head;
  mesh(sph(0.16, 12, 10), fur, head, 0, 0.02, 0, 0, 0, 0, 1, 0.95, 1.1);
  mesh(box(0.13, 0.1, 0.24), fur, head, 0, -0.01, 0.2);
  mesh(sph(0.035, 6, 5), mk({ color: 0x111111 }), head, 0, 0.03, 0.33);
  const jaw = piv(head, 0, -0.06, 0.08); p.jaw = jaw;
  mesh(box(0.11, 0.04, 0.22), fur2, jaw, 0, -0.01, 0.1);
  for (const sx of [-1, 1]) mesh(cone(0.012, 0.05, 4), teeth, jaw, sx * 0.04, 0.02, 0.18, 0, 0, 0);
  for (const sx of [-1, 1]) { mesh(cone(0.055, 0.16, 4), fur, head, sx * 0.08, 0.16, -0.04, -0.2, 0, sx * -0.25); mesh(sph(0.022, 6, 5), eye, head, sx * 0.07, 0.06, 0.12); }
  const legs = [];
  for (const [nm, x, z, back] of [['fl', 0.14, 0.34, 0], ['fr', -0.14, 0.34, 0], ['bl', 0.14, -0.38, 1], ['br', -0.14, -0.38, 1]]) {
    const up = piv(body, x, -0.02, z); mesh(cap(back ? 0.09 : 0.075, 0.22), fur, up, 0, -0.16, back ? -0.03 : 0);
    const lo = piv(up, 0, -0.33, back ? -0.04 : 0); mesh(cap(0.05, 0.22), fur2, lo, 0, -0.14, 0);
    const paw = piv(lo, 0, -0.32, 0); mesh(box(0.09, 0.05, 0.13), fur2, paw, 0, 0, 0.03);
    p[nm] = up; p[nm + 'Lo'] = lo; p[nm + 'Paw'] = paw; legs.push(up);
  }
  let par = piv(body, 0, 0.1, -0.5); p.tail = [];
  for (let i = 0; i < 4; i++) { const t = piv(par, 0, 0, i ? -0.17 : 0); mesh(cap(0.07 - i * 0.008, 0.12), i === 3 && o.tailTip ? mk({ color: o.tailTip, emissive: o.tailTip, emissiveIntensity: 2 }) : fur2, t, 0, 0, -0.08, Math.PI / 2); p.tail.push(t); par = t; }
  root.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return { root, p, mats: list, kind: 'quad', height: 1.1 * s, scale: s };
}

// ================= ÖRÜMCEK / AKREP =================
export function buildArachnid(o) {
  const s = o.s || 1; const scorp = o.scorpion; const { mk, list } = newMats();
  const shell = mk({ color: o.color ?? 0x2a2018, map: tex(scorp ? 'scales' : 'fur'), normalMap: tex(scorp ? 'scalesNormal' : 'furNormal'), roughness: scorp ? 0.45 : 0.9, metalness: scorp ? 0.2 : 0 });
  const shell2 = mk({ color: o.color2 ?? 0x5a2a18, map: tex(scorp ? 'scales' : 'fur'), roughness: 0.6 });
  const eye = mk({ color: 0x000000, emissive: o.eyeGlow ?? 0xff2020, emissiveIntensity: 5 });
  const root = new THREE.Group(); const p = { root };
  const inner = piv(root); inner.scale.setScalar(s); p.inner = inner;
  const body = piv(inner, 0, scorp ? 0.42 : 0.55, 0); p.body = body;
  if (!scorp) {
    const abd = piv(body, 0, 0.12, -0.35); p.abdomen = abd;
    mesh(sph(0.42, 14, 12), shell, abd, 0, 0.05, -0.2, 0, 0, 0, 1, 0.85, 1.2);
    for (let i = 0; i < 3; i++) mesh(box(0.25 - i * 0.05, 0.03, 0.1), shell2, abd, 0, 0.4 - i * 0.06, -0.1 - i * 0.18, -0.3);
    mesh(sph(0.24, 12, 10), shell, body, 0, 0, 0.12, 0, 0, 0, 1, 0.75, 1.1);
    const head = piv(body, 0, 0.02, 0.35); p.head = head;
    for (let i = 0; i < 6; i++) mesh(sph(0.035, 6, 5), eye, head, (i % 3 - 1) * 0.06, 0.06 + Math.floor(i / 3) * 0.05, 0.02);
    p.fangs = [];
    for (const sx of [-1, 1]) { const f = piv(head, sx * 0.06, -0.04, 0.02); mesh(cone(0.03, 0.16, 5), shell2, f, 0, -0.07, 0.02, Math.PI - 0.3); p.fangs.push(f); }
  } else {
    for (let i = 0; i < 4; i++) mesh(sph(0.3 - i * 0.03, 12, 8), i % 2 ? shell2 : shell, body, 0, 0, 0.3 - i * 0.24, 0, 0, 0, 1.15, 0.55, 0.9);
    const head = piv(body, 0, 0.02, 0.45); p.head = head;
    mesh(sph(0.2, 10, 8), shell, head, 0, 0, 0, 0, 0, 0, 1.2, 0.6, 1);
    for (const sx of [-1, 1]) mesh(sph(0.03, 6, 5), eye, head, sx * 0.07, 0.1, 0.14);
    // kıskaçlar
    for (const side of ['l', 'r']) {
      const sx = side === 'l' ? 1 : -1;
      const a = piv(body, sx * 0.22, 0, 0.5); a.rotation.y = sx * 0.5; mesh(cap(0.06, 0.3), shell, a, 0, 0, 0.2, Math.PI / 2);
      const f = piv(a, 0, 0, 0.42); f.rotation.y = -sx * 1.0; mesh(cap(0.055, 0.25), shell, f, 0, 0, 0.17, Math.PI / 2);
      const claw = piv(f, 0, 0, 0.36); mesh(sph(0.12, 10, 8), shell2, claw, 0, 0, 0.05, 0, 0, 0, 0.8, 0.6, 1.3);
      mesh(cone(0.05, 0.28, 5), shell2, claw, sx * 0.03, 0, 0.26, Math.PI / 2);
      const fin = piv(claw, -sx * 0.05, 0, 0.1); mesh(cone(0.04, 0.26, 5), shell2, fin, 0, 0, 0.13, Math.PI / 2); p[side + 'Claw'] = fin;
      p[side + 'Arm'] = a; p[side + 'Fore'] = f;
    }
    // kuyruk
    let par = piv(body, 0, 0.05, -0.5); p.tail = [];
    for (let i = 0; i < 6; i++) { const t = piv(par, 0, i ? 0.24 : 0, 0); t.rotation.x = i ? -0.45 : -0.9; mesh(sph(0.13 - i * 0.012, 8, 6), i % 2 ? shell2 : shell, t, 0, 0.12, 0, 0, 0, 0, 1, 1.3, 1); p.tail.push(t); par = t; }
    const st = piv(par, 0, 0.26, 0); mesh(sph(0.1, 8, 6), shell2, st, 0, 0.02, 0); mesh(cone(0.04, 0.3, 5), mk({ color: 0x100808, emissive: o.stingGlow ?? 0x80ff30, emissiveIntensity: 2.5 }), st, 0, 0.1, 0.15, 1.9);
    p.sting = st;
    if (o.crown) { const cm = mk({ color: 0xffc040, metalness: 1, roughness: 0.3, emissive: 0x805000, emissiveIntensity: 0.5 }); for (let i = 0; i < 5; i++) mesh(cone(0.04, 0.16, 4), cm, head, (i - 2) * 0.06, 0.16, 0.02); mesh(cyl(0.16, 0.16, 0.05, 10, true), cm, head, 0, 0.12, 0); }
  }
  // bacaklar
  p.legs = [];
  const n = scorp ? 3 : 4;
  for (let i = 0; i < n; i++) for (const sx of [-1, 1]) {
    const z = scorp ? 0.2 - i * 0.25 : 0.25 - i * 0.12;
    const hip = piv(body, sx * 0.18, 0, z); hip.rotation.y = sx * (Math.PI / 2) - sx * (i - (n - 1) / 2) * (scorp ? 0.4 : 0.45);
    const fem = piv(hip); fem.rotation.z = 0; const femL = scorp ? 0.34 : 0.55;
    mesh(cap(0.035, femL), shell, fem, 0, 0, femL / 2 + 0.03, Math.PI / 2);
    const tib = piv(fem, 0, 0, femL + 0.06); const tibL = scorp ? 0.4 : 0.75;
    mesh(cap(0.028, tibL), shell2, tib, 0, 0, tibL / 2 + 0.02, Math.PI / 2);
    hip.userData = { sx, i, femL, tibL }; p.legs.push({ hip, fem, tib, sx, i });
    // x ekseni etrafında: fem yukarı, tib aşağı → yerel z ekseni dışarı doğru
    fem.rotation.x = -0.7; tib.rotation.x = scorp ? 1.5 : 1.7;
  }
  root.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return { root, p, mats: list, kind: scorp ? 'scorpion' : 'spider', height: 1 * s, scale: s };
}

// ================= BALÇIK =================
export function buildSlime(o) {
  const s = o.s || 1; const { mk, list } = newMats();
  const root = new THREE.Group(); const p = { root };
  const inner = piv(root); inner.scale.setScalar(s); p.inner = inner;
  const body = piv(inner, 0, 0, 0); p.body = body;
  const gel = mk({ color: o.color ?? 0x60d060, map: tex('slime'), transparent: true, opacity: 0.78, roughness: 0.15, metalness: 0.1, emissive: o.color ?? 0x60d060, emissiveIntensity: 0.35 });
  mesh(G('slimeB', () => { const g = new THREE.SphereGeometry(0.5, 20, 14); g.translate(0, 0.45, 0); const pp = g.attributes.position; for (let i = 0; i < pp.count; i++) if (pp.getY(i) < 0.2) pp.setY(i, 0.2 - (0.2 - pp.getY(i)) * 0.3); g.computeVertexNormals(); return g; }), gel, body);
  mesh(sph(0.16, 10, 8), mk({ color: o.core ?? 0x207020, emissive: o.core ?? 0x207020, emissiveIntensity: 0.6 }), body, 0, 0.4, 0);
  for (const sx of [-1, 1]) { mesh(sph(0.075, 10, 8), mk({ color: 0xffffff, roughness: 0.3 }), body, sx * 0.15, 0.55, 0.38); mesh(sph(0.04, 8, 6), mk({ color: 0x101010 }), body, sx * 0.15, 0.55, 0.44); }
  if (o.crown) { const cm = mk({ color: 0xffd040, metalness: 1, roughness: 0.3 }); for (let i = 0; i < 5; i++) mesh(cone(0.05, 0.18, 4), cm, body, Math.cos(i * 1.256) * 0.18, 0.98, Math.sin(i * 1.256) * 0.18); }
  root.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return { root, p, mats: list, kind: 'slime', height: 0.9 * s, scale: s };
}

// ================= EJDERHA =================
export function buildDragon(o) {
  const s = o.s || 1; const { mk, list } = newMats();
  const sc = mk({ color: o.color ?? 0x8a1a12, map: tex('scales'), normalMap: tex('scalesNormal'), roughness: 0.45, metalness: 0.25 });
  const belly = mk({ color: 0xd8a060, map: tex('scales'), roughness: 0.6 });
  const horn = mk({ color: 0x2a2220, roughness: 0.5 });
  const memb = mk({ color: 0x6a1410, roughness: 0.8, side: THREE.DoubleSide, emissive: 0x401000, emissiveIntensity: 0.4 });
  const eye = mk({ color: 0x000000, emissive: 0xffc020, emissiveIntensity: 6 });
  const glowM = mk({ color: 0x200500, emissive: 0xff4a10, emissiveIntensity: 2.5 });
  const root = new THREE.Group(); const p = { root };
  const inner = piv(root); inner.scale.setScalar(s); p.inner = inner;
  const body = piv(inner, 0, 1.3, 0); p.body = body;
  mesh(cap(0.6, 1.4), sc, body, 0, 0, 0, Math.PI / 2, 0, 0, 1, 1, 0.9);
  mesh(cap(0.52, 1.2), belly, body, 0, -0.18, 0.05, Math.PI / 2, 0, 0, 0.85, 1, 0.8);
  for (let i = 0; i < 7; i++) mesh(cone(0.1, 0.4, 4), horn, body, 0, 0.62, 0.8 - i * 0.28, -0.3);
  mesh(sph(0.35, 10, 8), glowM, body, 0, -0.35, 0.6, 0, 0, 0, 1, 0.5, 1.2);
  // boyun zinciri
  let par = piv(body, 0, 0.3, 1.0); p.neck = [];
  for (let i = 0; i < 5; i++) { const n = piv(par, 0, 0, i ? 0.4 : 0); n.rotation.x = i === 0 ? -0.5 : 0.1; mesh(cap(0.3 - i * 0.03, 0.3), sc, n, 0, 0, 0.2, Math.PI / 2); mesh(cone(0.07, 0.25, 4), horn, n, 0, 0.3, 0.2, -0.3); p.neck.push(n); par = n; }
  const head = piv(par, 0, 0, 0.45); p.head = head;
  mesh(box(0.5, 0.36, 0.6), sc, head, 0, 0.05, 0.15); mesh(box(0.38, 0.22, 0.55), sc, head, 0, 0, 0.6);
  for (const sx of [-1, 1]) { mesh(cone(0.07, 0.8, 6), horn, head, sx * 0.2, 0.3, -0.25, -2.2, 0, sx * 0.3); mesh(sph(0.05, 6, 5), eye, head, sx * 0.2, 0.15, 0.4); mesh(cone(0.04, 0.2, 4), horn, head, sx * 0.15, 0.14, 0.85, 1.2); }
  const jaw = piv(head, 0, -0.1, 0.2); p.jaw = jaw; mesh(box(0.34, 0.1, 0.65), belly, jaw, 0, -0.06, 0.3);
  for (let i = 0; i < 5; i++) for (const sx of [-1, 1]) mesh(cone(0.025, 0.1, 4), mk({ color: 0xf0e8d0 }), jaw, sx * 0.14, 0.03, 0.1 + i * 0.12);
  p.mouth = piv(head, 0, -0.08, 0.9);
  // bacaklar
  for (const [nm, x, z, back] of [['fl', 0.45, 0.8, 0], ['fr', -0.45, 0.8, 0], ['bl', 0.5, -0.7, 1], ['br', -0.5, -0.7, 1]]) {
    const up = piv(body, x, -0.2, z); mesh(cap(back ? 0.26 : 0.2, 0.5), sc, up, 0, -0.35, 0);
    const lo = piv(up, 0, -0.7, 0); mesh(cap(0.15, 0.45), sc, lo, 0, -0.3, 0);
    const paw = piv(lo, 0, -0.62, 0); mesh(box(0.3, 0.12, 0.4), sc, paw, 0, 0, 0.1);
    for (let i = 0; i < 3; i++) mesh(cone(0.04, 0.18, 4), horn, paw, (i - 1) * 0.1, -0.02, 0.32, Math.PI / 2);
    p[nm] = up; p[nm + 'Lo'] = lo;
  }
  // kanatlar
  p.wings = [];
  for (const sx of [-1, 1]) {
    const sh = piv(body, sx * 0.4, 0.45, 0.5); sh.userData.side = sx;
    mesh(cap(0.1, 1.4), sc, sh, sx * 0.8, 0, 0, 0, 0, Math.PI / 2);
    const fa = piv(sh, sx * 1.6, 0, 0); mesh(cap(0.07, 1.6), sc, fa, sx * 0.9, 0, 0, 0, 0, Math.PI / 2);
    const s1 = new THREE.Shape(); s1.moveTo(0, 0); s1.lineTo(1.6, 0); s1.lineTo(1.4, -1.8); s1.lineTo(0.2, -2.2); s1.closePath();
    const s2 = new THREE.Shape(); s2.moveTo(0, 0); s2.lineTo(1.9, 0.1); s2.lineTo(1.2, -1.2); s2.lineTo(0.6, -1.5); s2.lineTo(0, -1.8); s2.closePath();
    const g1 = G('dw1', () => new THREE.ShapeGeometry(s1)), g2 = G('dw2', () => new THREE.ShapeGeometry(s2));
    mesh(g1, memb, sh, 0, 0, 0, Math.PI / 2, 0, 0, sx, 1, 1);
    mesh(g2, memb, fa, 0, 0, 0, Math.PI / 2, 0, 0, sx, 1, 1);
    for (let i = 0; i < 3; i++) mesh(cyl(0.025, 0.04, 1.8, 4), horn, fa, sx * (0.6 + i * 0.5), 0, -0.7 + i * 0.2, Math.PI / 2 - 0.2 + i * 0.3, 0, 0);
    p.wings.push({ sh, fa, sx });
  }
  // kuyruk
  par = piv(body, 0, 0.1, -1.2); p.tail = [];
  for (let i = 0; i < 8; i++) { const t = piv(par, 0, 0, i ? -0.45 : 0); t.rotation.x = i ? 0.06 : 0.25; mesh(cap(0.32 - i * 0.035, 0.3), sc, t, 0, 0, -0.22, Math.PI / 2); mesh(cone(0.06, 0.25, 4), horn, t, 0, 0.3 - i * 0.03, -0.2, -0.4); p.tail.push(t); par = t; }
  mesh(cone(0.25, 0.6, 4), horn, par, 0, 0, -0.7, -Math.PI / 2);
  root.traverse((m) => { if (m.isMesh) m.castShadow = true; });
  return { root, p, mats: list, kind: 'dragon', height: 3.5 * s, scale: s };
}
