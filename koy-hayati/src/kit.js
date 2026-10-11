// Model kurma yardımcıları: malzeme önbelleği, kutu/silindir oluşturucular, statik birleştirme
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';

const matCache = new Map();
export function mat(color, opts = {}) {
  const key = color + JSON.stringify(opts, (k, v) => (v && v.isTexture ? v.uuid : v));
  if (matCache.has(key)) return matCache.get(key);
  const { flat = false, ...rest } = opts;
  const m = new THREE.MeshStandardMaterial({ color, roughness: 0.85, metalness: 0, flatShading: flat, ...rest });
  matCache.set(key, m);
  return m;
}
export function tmat(tex, color = '#ffffff', opts = {}) {
  return mat(color, { map: tex, ...opts });
}

// Kutunun UV'lerini dünya ölçüsüne göre ölçekle (s metre = 1 doku tekrarı)
export function boxUV(geo, w, h, d, s) {
  const uv = geo.attributes.uv;
  const dims = [[d, h], [d, h], [w, d], [w, d], [w, h], [w, h]];
  for (let f = 0; f < 6; f++) {
    for (let k = 0; k < 4; k++) {
      const i = f * 4 + k;
      uv.setXY(i, uv.getX(i) * dims[f][0] / s, uv.getY(i) * dims[f][1] / s);
    }
  }
  uv.needsUpdate = true;
  return geo;
}

export function box(parent, w, h, d, material, x = 0, y = 0, z = 0, rx = 0, ry = 0, rz = 0, uvs = 0) {
  const g = new THREE.BoxGeometry(w, h, d);
  if (uvs) boxUV(g, w, h, d, uvs);
  const m = new THREE.Mesh(g, material);
  m.position.set(x, y, z);
  m.rotation.set(rx, ry, rz);
  if (parent) parent.add(m);
  return m;
}

export function cyl(parent, rt, rb, h, seg, material, x = 0, y = 0, z = 0, rx = 0, ry = 0, rz = 0, uvs = 0, open = false) {
  const g = new THREE.CylinderGeometry(rt, rb, h, seg, 1, open);
  if (uvs) {
    const uv = g.attributes.uv;
    const circ = 2 * Math.PI * Math.max(rt, rb);
    for (let i = 0; i < uv.count; i++) uv.setXY(i, uv.getX(i) * circ / uvs, uv.getY(i) * h / uvs);
  }
  const m = new THREE.Mesh(g, material);
  m.position.set(x, y, z);
  m.rotation.set(rx, ry, rz);
  if (parent) parent.add(m);
  return m;
}

export function sphere(parent, r, material, x = 0, y = 0, z = 0, sx = 1, sy = 1, sz = 1, detail = 1) {
  const m = new THREE.Mesh(new THREE.IcosahedronGeometry(r, detail), material);
  m.position.set(x, y, z);
  m.scale.set(sx, sy, sz);
  if (parent) parent.add(m);
  return m;
}

export function cone(parent, r, h, seg, material, x = 0, y = 0, z = 0, rx = 0, ry = 0, rz = 0) {
  const m = new THREE.Mesh(new THREE.ConeGeometry(r, h, seg), material);
  m.position.set(x, y, z);
  m.rotation.set(rx, ry, rz);
  if (parent) parent.add(m);
  return m;
}

// İki nokta arasında çubuk (silindir)
export function rod(parent, a, b, r, material, seg = 6) {
  const A = new THREE.Vector3(...a), B = new THREE.Vector3(...b);
  const len = A.distanceTo(B);
  const m = new THREE.Mesh(new THREE.CylinderGeometry(r, r, len, seg), material);
  m.position.copy(A).add(B).multiplyScalar(0.5);
  m.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), B.clone().sub(A).normalize());
  if (parent) parent.add(m);
  return m;
}
// İki nokta arasında kalas (kutu)
export function beam(parent, a, b, w, t, material, uvs = 0) {
  const A = new THREE.Vector3(...a), B = new THREE.Vector3(...b);
  const len = A.distanceTo(B);
  const g = new THREE.BoxGeometry(w, len, t);
  if (uvs) boxUV(g, w, len, t, uvs);
  const m = new THREE.Mesh(g, material);
  m.position.copy(A).add(B).multiplyScalar(0.5);
  m.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), B.clone().sub(A).normalize());
  if (parent) parent.add(m);
  return m;
}

// Çokgen profili ekstrüde et (ör. üçgen alınlık). pts: [[x,y],...] ; derinlik z boyunca
export function prism(parent, pts, depth, material, x = 0, y = 0, z = 0, ry = 0, uvs = 1) {
  const shape = new THREE.Shape(pts.map((p) => new THREE.Vector2(p[0], p[1])));
  const g = new THREE.ExtrudeGeometry(shape, { depth, bevelEnabled: false });
  g.translate(0, 0, -depth / 2);
  const uv = g.attributes.uv;
  for (let i = 0; i < uv.count; i++) uv.setXY(i, uv.getX(i) / uvs, uv.getY(i) / uvs);
  const m = new THREE.Mesh(g, material);
  m.position.set(x, y, z);
  m.rotation.y = ry;
  if (parent) parent.add(m);
  return m;
}

export function dynamic(obj) { obj.userData.dynamic = true; return obj; }

function isUnderDynamic(o, root) {
  let p = o;
  while (p && p !== root) { if (p.userData.dynamic) return true; p = p.parent; }
  return false;
}

// Statik parçaları malzemeye göre tek mesh'te birleştir (çizim çağrısı azaltır)
export function bake(root, { cast = true, receive = true } = {}) {
  root.updateMatrixWorld(true);
  const inv = new THREE.Matrix4().copy(root.matrixWorld).invert();
  const byMat = new Map();
  const remove = [];
  root.traverse((o) => {
    if (!o.isMesh || o === root || isUnderDynamic(o, root) || o.userData.noBake) return;
    if (Array.isArray(o.material)) return;
    let g = o.geometry.index ? o.geometry.toNonIndexed() : o.geometry.clone();
    for (const k of Object.keys(g.attributes)) if (!['position', 'normal', 'uv'].includes(k)) g.deleteAttribute(k);
    if (!g.attributes.uv) g.setAttribute('uv', new THREE.Float32BufferAttribute(new Float32Array(g.attributes.position.count * 2), 2));
    if (!g.attributes.normal) g.computeVertexNormals();
    g.clearGroups();
    const m = new THREE.Matrix4().multiplyMatrices(inv, o.matrixWorld);
    g.applyMatrix4(m);
    if (!byMat.has(o.material)) byMat.set(o.material, []);
    byMat.get(o.material).push(g);
    remove.push(o);
  });
  for (const o of remove) o.parent.remove(o);
  for (const [material, geos] of byMat) {
    const merged = mergeGeometries(geos, false);
    if (!merged) continue;
    const mesh = new THREE.Mesh(merged, material);
    mesh.castShadow = cast && !material.transparent;
    mesh.receiveShadow = receive;
    root.add(mesh);
  }
  root.traverse((o) => {
    if (o.isMesh && isUnderDynamic(o, root)) {
      o.castShadow = cast && !(o.material && o.material.transparent);
      o.receiveShadow = receive;
    }
  });
  return root;
}

export function shadowAll(root, cast = true, receive = true) {
  root.traverse((o) => { if (o.isMesh) { o.castShadow = cast; o.receiveShadow = receive; } });
  return root;
}

// Her grubun doğrudan mesh çocuklarını malzemeye göre birleştir (iskelet yapısı korunur)
export function mergeChildren(root) {
  const visit = (g) => {
    const meshes = g.children.filter((c) => c.isMesh && !c.userData.dynamic && !Array.isArray(c.material) && c.children.length === 0);
    const by = new Map();
    for (const m of meshes) { if (!by.has(m.material)) by.set(m.material, []); by.get(m.material).push(m); }
    for (const [material, list] of by) {
      if (list.length < 2) continue;
      const geos = list.map((m) => {
        m.updateMatrix();
        const geo = m.geometry.index ? m.geometry.toNonIndexed() : m.geometry.clone();
        for (const k of Object.keys(geo.attributes)) if (!['position', 'normal', 'uv'].includes(k)) geo.deleteAttribute(k);
        if (!geo.attributes.uv) geo.setAttribute('uv', new THREE.Float32BufferAttribute(new Float32Array(geo.attributes.position.count * 2), 2));
        geo.clearGroups();
        geo.applyMatrix4(m.matrix);
        return geo;
      });
      const merged = mergeGeometries(geos, false);
      if (!merged) continue;
      const mesh = new THREE.Mesh(merged, material);
      mesh.castShadow = list[0].castShadow; mesh.receiveShadow = list[0].receiveShadow;
      for (const m of list) g.remove(m);
      g.add(mesh);
    }
    for (const c of [...g.children]) if (!c.isMesh || c.children.length) visit(c);
  };
  visit(root);
  return root;
}
