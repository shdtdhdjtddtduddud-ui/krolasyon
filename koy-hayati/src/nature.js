// Doğa: ağaçlar (instanced), çimen, çiçekler, kayalar, çalılar, mantarlar, meyve ağaçları
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import * as T from './textures.js';
import { heightAt, slopeAt, distToRiver, distToRoad } from './terrain.js';
import { mulberry32, smoothstep, clamp, fbm } from './util.js';
import { FARM, TOWN, LAKE, ISLAND, POOL, PEAK, MEADOW, FOREST, HALF, WATERFALL_X } from './layout.js';
import { mat, tmat, box, cyl, sphere, cone, rod, bake, mergeChildren } from './kit.js';

export const windUniforms = { uTime: { value: 0 }, uWind: { value: 1 } };

export function windMaterial(params, amp = 0.1, base = 0) {
  const m = new THREE.MeshStandardMaterial(params);
  m.onBeforeCompile = (shader) => {
    shader.uniforms.uTime = windUniforms.uTime;
    shader.uniforms.uWind = windUniforms.uWind;
    shader.vertexShader = shader.vertexShader
      .replace('#include <common>', '#include <common>\nuniform float uTime;\nuniform float uWind;')
      .replace('#include <begin_vertex>', `#include <begin_vertex>
        #ifdef USE_INSTANCING
          vec3 ip = vec3(instanceMatrix[3][0], instanceMatrix[3][1], instanceMatrix[3][2]);
        #else
          vec3 ip = vec3(modelMatrix[3][0], modelMatrix[3][1], modelMatrix[3][2]);
        #endif
        float hgt = max(position.y - ${base.toFixed(2)}, 0.0);
        float sw = sin(uTime*1.7 + ip.x*0.37 + ip.z*0.21)*0.6 + sin(uTime*3.3 + ip.x*0.9 + ip.z*0.4)*0.25;
        transformed.x += sw * uWind * ${amp.toFixed(3)} * hgt;
        transformed.z += cos(uTime*1.3 + ip.z*0.31) * 0.4 * uWind * ${amp.toFixed(3)} * hgt;`);
  };
  m.customProgramCacheKey = () => `wind${amp}_${base}`;
  return m;
}

function merged(list) {
  const geos = list.map((m) => {
    m.updateMatrix();
    let g = m.geometry.index ? m.geometry.toNonIndexed() : m.geometry.clone();
    for (const k of Object.keys(g.attributes)) if (!['position', 'normal', 'uv'].includes(k)) g.deleteAttribute(k);
    g.applyMatrix4(m.matrix);
    return g;
  });
  const g = mergeGeometries(geos, false);
  g.computeVertexNormals();
  return g;
}
const M = (geo, x = 0, y = 0, z = 0, sx = 1, sy = 1, sz = 1, rx = 0, ry = 0, rz = 0) => {
  const m = new THREE.Mesh(geo);
  m.position.set(x, y, z); m.scale.set(sx, sy, sz); m.rotation.set(rx, ry, rz);
  return m;
};

// --- ağaç geometrileri ---
function oakGeo() {
  const trunk = merged([
    M(new THREE.CylinderGeometry(0.22, 0.38, 3.2, 7), 0, 1.6, 0),
    M(new THREE.CylinderGeometry(0.08, 0.14, 1.6, 5), 0.5, 3.0, 0, 1, 1, 1, 0, 0, -0.7),
    M(new THREE.CylinderGeometry(0.08, 0.14, 1.4, 5), -0.45, 3.1, 0.2, 1, 1, 1, 0.3, 0, 0.7),
  ]);
  const ico = new THREE.IcosahedronGeometry(1, 0);
  const canopy = merged([
    M(ico, 0, 4.4, 0, 2.0, 1.6, 2.0),
    M(ico, 1.2, 3.9, 0.3, 1.4, 1.2, 1.4),
    M(ico, -1.1, 4.0, -0.4, 1.5, 1.2, 1.5),
    M(ico, 0.2, 5.3, -0.3, 1.3, 1.1, 1.3),
    M(ico, -0.3, 3.8, 1.1, 1.2, 1.0, 1.2),
  ]);
  const bare = merged([
    M(new THREE.CylinderGeometry(0.05, 0.1, 2.0, 4), 0.6, 3.8, 0, 1, 1, 1, 0, 0, -0.6),
    M(new THREE.CylinderGeometry(0.05, 0.1, 2.0, 4), -0.6, 3.9, 0.2, 1, 1, 1, 0.3, 0, 0.6),
    M(new THREE.CylinderGeometry(0.04, 0.08, 1.6, 4), 0.1, 4.2, -0.6, 1, 1, 1, -0.6, 0, 0),
    M(new THREE.CylinderGeometry(0.04, 0.08, 1.6, 4), 0.0, 4.3, 0.6, 1, 1, 1, 0.6, 0, 0.1),
    M(new THREE.CylinderGeometry(0.03, 0.06, 1.2, 4), 0.0, 4.8, 0.0, 1, 1, 1, 0, 0, 0.1),
  ]);
  return { trunk, canopy, bare, snow: null };
}
function pineGeo() {
  const trunk = merged([M(new THREE.CylinderGeometry(0.15, 0.32, 2.0, 6), 0, 1.0, 0)]);
  const tiers = [];
  const snow = [];
  const levels = [[2.0, 2.2, 2.6], [1.65, 2.0, 3.8], [1.25, 1.8, 4.9], [0.8, 1.5, 5.9]];
  for (const [r, h, y] of levels) {
    tiers.push(M(new THREE.ConeGeometry(r, h, 8), 0, y, 0));
    snow.push(M(new THREE.ConeGeometry(r * 0.75, h * 0.42, 8), 0, y + h * 0.3, 0));
  }
  return { trunk, canopy: merged(tiers), bare: null, snow: merged(snow) };
}
function birchGeo() {
  const trunk = merged([M(new THREE.CylinderGeometry(0.12, 0.2, 4.4, 6), 0, 2.2, 0)]);
  const ico = new THREE.IcosahedronGeometry(1, 0);
  const canopy = merged([
    M(ico, 0, 4.6, 0, 1.2, 1.9, 1.2),
    M(ico, 0.6, 3.9, 0.2, 0.9, 1.3, 0.9),
    M(ico, -0.5, 4.1, -0.3, 0.9, 1.4, 0.9),
  ]);
  const bare = merged([
    M(new THREE.CylinderGeometry(0.03, 0.06, 1.6, 4), 0.4, 4.0, 0, 1, 1, 1, 0, 0, -0.5),
    M(new THREE.CylinderGeometry(0.03, 0.06, 1.6, 4), -0.4, 4.2, 0.2, 1, 1, 1, 0.3, 0, 0.5),
    M(new THREE.CylinderGeometry(0.03, 0.05, 1.3, 4), 0, 4.8, 0, 1, 1, 1, 0, 0, 0.1),
  ]);
  return { trunk, canopy, bare, snow: null };
}

const SEASON_CANOPY = {
  oak: [['#7cc04a', '#93cf5a', '#6fb544'], ['#3f8a2f', '#4a9535', '#357a28'], ['#d9822b', '#c2452d', '#e0b53a', '#b8612a', '#a8b23a'], null],
  birch: [['#a8d860', '#9ccf5a'], ['#5f9e38', '#6aa83f'], ['#f0c040', '#e8b030', '#f4d35a'], null],
  pine: [['#2f6b3a', '#356f3d'], ['#2a5e33', '#2f6538'], ['#2d5a35', '#33603a'], ['#2a4f38', '#2e5640']],
};

export class Trees {
  constructor(scene, colliders, isMobile = false) {
    this.isMobile = isMobile;
    this.scene = scene;
    this.colliders = colliders;
    this.types = { oak: oakGeo(), pine: pineGeo(), birch: birchGeo() };
    this.list = [];
    this.generate();
    this.build();
  }
  allowed(x, z) {
    const h = heightAt(x, z);
    if (h < 1.2 || h > 78) return false;
    if (Math.hypot(x - FARM.x, z - FARM.z) < 92) {
      return false;
    }
    if (Math.hypot(x - TOWN.x, z - TOWN.z) < 68) return false;
    if (distToRoad(x, z) < 5) return false;
    if (distToRiver(x, z) < 9) return false;
    if (Math.hypot(x - LAKE.x, z - LAKE.z) < LAKE.r + 10) return false;
    if (Math.hypot(x - POOL.x, z - POOL.z) < 18) return false;
    if (Math.abs(x - WATERFALL_X) < 9 && z < -150 && z > -220) return false;
    if (Math.hypot(x - PEAK.x, z - PEAK.z) < 16) return false;
    if (Math.hypot(x - FOREST.x, z - FOREST.z + 10) < 9) return false; // kulübe açıklığı
    if (Math.hypot(x - 46, z + 140) < 8) return false; // şelale seyir terası
    if (slopeAt(x, z) > 1.1) return false;
    if (Math.abs(x) > HALF - 8 || Math.abs(z) > HALF - 8) return false;
    return true;
  }
  density(x, z) {
    let d = 0.05;
    // batı ormanı
    d += smoothstep(-90, -140, x) * 0.9;
    // kuzey dağ yamaçları (çam)
    d += smoothstep(-110, -150, z) * 0.55;
    // sınır yamaçları
    d += smoothstep(200, 250, Math.max(Math.abs(x), Math.abs(z))) * 0.35;
    // yayla açık
    d *= 1 - 0.8 * (1 - smoothstep(18, 40, Math.hypot(x - MEADOW.x, z - MEADOW.z)));
    d *= 0.7 + 0.6 * (fbm(x * 0.02, z * 0.02, 2) * 0.5 + 0.5);
    return clamp(d, 0, 1);
  }
  generate() {
    const r = mulberry32(2024);
    let tries = 0;
    while (this.list.length < (this.isMobile ? 1500 : 2100) && tries < 60000) {
      tries++;
      const x = (r() * 2 - 1) * (HALF - 10), z = (r() * 2 - 1) * (HALF - 10);
      if (r() > this.density(x, z)) continue;
      if (!this.allowed(x, z)) continue;
      const h = heightAt(x, z);
      let type;
      if (h > 40 || z < -140) type = r() < 0.85 ? 'pine' : 'birch';
      else if (x < -100) type = r() < 0.5 ? 'pine' : r() < 0.6 ? 'oak' : 'birch';
      else type = r() < 0.6 ? 'oak' : r() < 0.5 ? 'birch' : 'pine';
      const s = 0.8 + r() * 0.6;
      this.list.push({ type, x, z, y: h - 0.1, s, rot: r() * Math.PI * 2, cv: r(), alive: true, regrow: 0, hp: 4 });
    }
  }
  build() {
    this.meshes = {};
    const barkM = tmat(T.bark('#6b4a2f'));
    const birchM = tmat(T.birchBark());
    const dummy = new THREE.Object3D();
    for (const type of ['oak', 'pine', 'birch']) {
      const items = this.list.filter((t) => t.type === type);
      items.forEach((t, i) => { t.idx = i; });
      const G = this.types[type];
      const n = items.length;
      const parts = {};
      parts.trunk = new THREE.InstancedMesh(G.trunk, type === 'birch' ? birchM : barkM, n);
      parts.canopy = new THREE.InstancedMesh(G.canopy, windMaterial({ color: '#ffffff', roughness: 0.9, flatShading: true }, type === 'pine' ? 0.025 : 0.04, 2.5), n);
      if (G.bare) parts.bare = new THREE.InstancedMesh(G.bare, type === 'birch' ? birchM : barkM, n);
      if (G.snow) parts.snow = new THREE.InstancedMesh(G.snow, mat('#f4f7fb', { flat: true, roughness: 0.8 }), n);
      for (const k in parts) {
        parts[k].castShadow = true; parts[k].receiveShadow = true;
        this.scene.add(parts[k]);
      }
      this.meshes[type] = { items, parts };
      items.forEach((t) => {
        dummy.position.set(t.x, t.y, t.z); dummy.rotation.set(0, t.rot, 0); dummy.scale.setScalar(t.s);
        dummy.updateMatrix();
        t.matrix = dummy.matrix.clone();
        for (const k in parts) parts[k].setMatrixAt(t.idx, t.matrix);
        parts.canopy.setColorAt(t.idx, new THREE.Color('#3f8a2f'));
        t.collider = this.colliders.addCircle(t.x, t.z, 0.4 * t.s, { tree: t });
      });
    }
    // kütükler
    const stumpGeo = new THREE.CylinderGeometry(0.3, 0.4, 0.45, 7);
    stumpGeo.translate(0, 0.2, 0);
    this.stumps = new THREE.InstancedMesh(stumpGeo, barkM, this.list.length);
    this.stumps.castShadow = true;
    this.stumps.frustumCulled = false;
    this.stumpCount = 0;
    this.scene.add(this.stumps);
    this.zero = new THREE.Matrix4().makeScale(0, 0, 0);
  }
  applySeason(season, seasonDay, chopped) {
    const zero = this.zero;
    const late = season === 2 ? smoothstep(3, 7, seasonDay) : 0;
    for (const type in this.meshes) {
      const { items, parts } = this.meshes[type];
      const pal = SEASON_CANOPY[type][season];
      const c = new THREE.Color();
      for (const t of items) {
        const alive = t.alive;
        const showCanopy = alive && pal !== null;
        if (showCanopy) {
          c.set(pal[Math.floor(t.cv * pal.length) % pal.length]);
          c.multiplyScalar(0.85 + t.cv * 0.3);
          parts.canopy.setColorAt(t.idx, c);
          if (late > 0 && type !== 'pine') {
            const m = t.matrix.clone();
            const sc = 1 - late * 0.45 * (0.5 + t.cv);
            const s = new THREE.Matrix4().makeScale(1, Math.max(0.35, sc), 1);
            parts.canopy.setMatrixAt(t.idx, m.multiply(s));
          } else parts.canopy.setMatrixAt(t.idx, t.matrix);
        } else parts.canopy.setMatrixAt(t.idx, zero);
        parts.trunk.setMatrixAt(t.idx, alive ? t.matrix : zero);
        if (parts.bare) parts.bare.setMatrixAt(t.idx, alive && (season === 3 || late > 0.6) ? t.matrix : zero);
        if (parts.snow) parts.snow.setMatrixAt(t.idx, alive && season === 3 ? t.matrix : zero);
      }
      for (const k in parts) {
        parts[k].instanceMatrix.needsUpdate = true;
        if (parts[k].instanceColor) parts[k].instanceColor.needsUpdate = true;
      }
    }
    // kütükler
    let n = 0;
    const d = new THREE.Object3D();
    for (const t of this.list) {
      if (!t.alive) {
        d.position.set(t.x, t.y, t.z); d.rotation.set(0, t.rot, 0); d.scale.setScalar(t.s); d.updateMatrix();
        this.stumps.setMatrixAt(n++, d.matrix);
      }
    }
    this.stumps.count = n;
    this.stumps.instanceMatrix.needsUpdate = true;
  }
  nearest(x, z, maxD = 3) {
    let best = null, bd = maxD;
    for (const t of this.colliders.queryCircles(x, z, maxD)) {
      const tr = t.data && t.data.tree;
      if (!tr || !tr.alive) continue;
      const d = Math.hypot(tr.x - x, tr.z - z);
      if (d < bd) { bd = d; best = tr; }
    }
    return best;
  }
  // devrilme animasyonu için geçici kopya
  makeFalling(t) {
    const { parts } = this.meshes[t.type];
    const g = new THREE.Group();
    const trunk = new THREE.Mesh(parts.trunk.geometry, parts.trunk.material);
    const c = new THREE.Color(); parts.canopy.getColorAt(t.idx, c);
    const canopyVisible = parts.canopy.geometry && SEASON_CANOPY[t.type];
    const can = new THREE.Mesh(parts.canopy.geometry, new THREE.MeshStandardMaterial({ color: c, flatShading: true, roughness: 0.9 }));
    g.add(trunk, can);
    g.position.set(t.x, t.y, t.z); g.rotation.y = t.rot; g.scale.setScalar(t.s);
    g.traverse((o) => { if (o.isMesh) o.castShadow = true; });
    return g;
  }
  setAlive(t, alive) {
    t.alive = alive;
    t.collider.disabled = !alive;
  }
}

// --- çimen ---
export class Grass {
  constructor(scene, isMobile, excluded) {
    const blade = new THREE.BufferGeometry();
    const pos = [], nrm = [];
    // ince, hafif eğik yapraklardan oluşan tutam
    for (let i = 0; i < 7; i++) {
      const a = i * 2.39;
      const ca = Math.cos(a), sa = Math.sin(a);
      const w = 0.035 + (i % 3) * 0.01, h = 0.32 + ((i * 37) % 10) / 22;
      const ox = Math.cos(a * 1.7) * 0.09, oz = Math.sin(a * 1.3) * 0.09;
      const lean = 0.12 + (i % 2) * 0.08;
      pos.push(ox - w * ca, 0, oz - w * sa, ox + w * ca, 0, oz + w * sa, ox + sa * lean, h, oz - ca * lean);
      for (let k = 0; k < 3; k++) nrm.push(0, 1, 0);
    }
    // arka yüzler: ters sarım, yine yukarı bakan normaller (koyu görünmesin)
    const n0 = pos.length;
    for (let i = 0; i < n0; i += 9) {
      pos.push(pos[i], pos[i + 1], pos[i + 2], pos[i + 6], pos[i + 7], pos[i + 8], pos[i + 3], pos[i + 4], pos[i + 5]);
      for (let k = 0; k < 3; k++) nrm.push(0, 1, 0);
    }
    blade.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    blade.setAttribute('normal', new THREE.Float32BufferAttribute(nrm, 3));
    const N = isMobile ? 9000 : 24000;
    const m = windMaterial({ color: '#ffffff', roughness: 1 }, 0.35, 0);
    this.mesh = new THREE.InstancedMesh(blade, m, N);
    this.mesh.receiveShadow = true;
    this.items = [];
    const r = mulberry32(77);
    const d = new THREE.Object3D();
    let tries = 0;
    while (this.items.length < N && tries < N * 8) {
      tries++;
      // odak alanlar etrafında yoğunlaş
      let x, z;
      const pickR = r();
      if (pickR < 0.3) { const a = r() * 6.283, rr = 30 + r() * 80; x = FARM.x + Math.cos(a) * rr; z = FARM.z + Math.sin(a) * rr; }
      else if (pickR < 0.45) { const a = r() * 6.283, rr = r() * 40; x = MEADOW.x + Math.cos(a) * rr; z = MEADOW.z + Math.sin(a) * rr; }
      else if (pickR < 0.55) { const a = r() * 6.283, rr = LAKE.r + r() * 30; x = LAKE.x + Math.cos(a) * rr; z = LAKE.z + Math.sin(a) * rr; }
      else if (pickR < 0.62) { x = 30 + r() * 70; z = -150 + r() * 60; }
      else { x = (r() * 2 - 1) * (HALF - 20); z = (r() * 2 - 1) * (HALF - 20); }
      const h = heightAt(x, z);
      if (h < 1.0 || h > 55) continue;
      if (slopeAt(x, z) > 0.7) continue;
      if (distToRoad(x, z) < 2.5) continue;
      if (excluded(x, z)) continue;
      d.position.set(x, h - 0.05, z);
      d.rotation.set(0, r() * 6.28, 0);
      const s = 0.7 + r() * 0.8;
      d.scale.set(s, s * (0.8 + r() * 0.6), s);
      d.updateMatrix();
      const idx = this.items.length;
      this.mesh.setMatrixAt(idx, d.matrix);
      this.items.push({ x, z, v: r() });
    }
    this.mesh.count = this.items.length;
    this.mesh.frustumCulled = false;
    scene.add(this.mesh);
  }
  applySeason(season, snowAmount) {
    const pal = [['#6cb84a', '#86c95a', '#5aa83f'], ['#8db04a', '#a5b84f', '#7aa53c'], ['#b49a4a', '#a8863f', '#9a8a48'], ['#c8cfc0', '#b9c2b4']][season];
    const c = new THREE.Color();
    this.items.forEach((it, i) => {
      c.set(pal[Math.floor(it.v * pal.length) % pal.length]);
      this.mesh.setColorAt(i, c);
    });
    this.mesh.instanceColor.needsUpdate = true;
    this.mesh.visible = snowAmount < 0.7;
  }
}

// --- çiçekler ---
export class Flowers {
  constructor(scene, excluded) {
    const stem = new THREE.CylinderGeometry(0.012, 0.015, 0.35, 3); stem.translate(0, 0.17, 0);
    const head = new THREE.IcosahedronGeometry(0.07, 0); head.scale(1, 0.6, 1); head.translate(0, 0.37, 0);
    const N = 3500;
    this.stems = new THREE.InstancedMesh(stem, mat('#4f8a35'), N);
    this.heads = new THREE.InstancedMesh(head, mat('#ffffff', { roughness: 0.7, flat: true }), N);
    const r = mulberry32(91);
    const d = new THREE.Object3D();
    this.items = [];
    const colors = ['#e84a5f', '#f7d046', '#ffffff', '#9b6bd6', '#ff8c42', '#4aa3df', '#ff6fb5'];
    let tries = 0;
    while (this.items.length < N && tries < N * 10) {
      tries++;
      let x, z;
      if (r() < 0.55) { const a = r() * 6.283, rr = Math.sqrt(r()) * 45; x = MEADOW.x + Math.cos(a) * rr; z = MEADOW.z + Math.sin(a) * rr; }
      else { x = (r() * 2 - 1) * (HALF - 30); z = (r() * 2 - 1) * (HALF - 30); }
      const h = heightAt(x, z);
      if (h < 1.2 || h > 50 || slopeAt(x, z) > 0.6 || distToRoad(x, z) < 3 || excluded(x, z)) continue;
      d.position.set(x, h - 0.02, z); d.rotation.set(0, r() * 6, 0);
      const s = 0.8 + r() * 0.7; d.scale.setScalar(s); d.updateMatrix();
      const i = this.items.length;
      this.stems.setMatrixAt(i, d.matrix); this.heads.setMatrixAt(i, d.matrix);
      this.heads.setColorAt(i, new THREE.Color(colors[Math.floor(r() * colors.length)]));
      this.items.push({ v: r() });
    }
    this.stems.count = this.heads.count = this.items.length;
    this.stems.frustumCulled = this.heads.frustumCulled = false;
    scene.add(this.stems, this.heads);
  }
  applySeason(season) {
    const frac = [1, 0.6, 0.15, 0][season];
    const n = Math.floor(this.items.length * frac);
    this.stems.count = this.heads.count = n;
  }
}

// --- kayalar ---
export function rocks(scene, colliders) {
  const geo = new THREE.DodecahedronGeometry(1, 1);
  const p = geo.attributes.position;
  const r0 = mulberry32(5);
  for (let i = 0; i < p.count; i++) {
    const v = new THREE.Vector3().fromBufferAttribute(p, i);
    v.multiplyScalar(0.8 + fbm(v.x * 2 + 3, v.z * 2 + v.y, 2) * 0.5);
    p.setXYZ(i, v.x, v.y * 0.75, v.z);
  }
  geo.computeVertexNormals();
  const N = 700;
  const m = new THREE.InstancedMesh(geo, mat('#ffffff', { flat: true, roughness: 0.95, map: T.stone('#9a968e') }), N);
  m.castShadow = true; m.receiveShadow = true;
  const r = mulberry32(55);
  const d = new THREE.Object3D();
  let n = 0, tries = 0;
  while (n < N && tries < 30000) {
    tries++;
    const x = (r() * 2 - 1) * (HALF - 6), z = (r() * 2 - 1) * (HALF - 6);
    const h = heightAt(x, z);
    const sl = slopeAt(x, z);
    let w = 0.03 + smoothstep(0.4, 1.0, sl) * 0.5 + smoothstep(30, 60, h) * 0.3;
    if (distToRiver(x, z) < 14 && distToRiver(x, z) > 4) w += 0.25;
    if (Math.hypot(x - POOL.x, z - POOL.z) < 22 && Math.hypot(x - POOL.x, z - POOL.z) > 11) w += 0.6;
    if (r() > w) continue;
    if (Math.hypot(x - FARM.x, z - FARM.z) < 95 || Math.hypot(x - TOWN.x, z - TOWN.z) < 70 || distToRoad(x, z) < 4) continue;
    if (h < -0.5) continue;
    const s = 0.3 + Math.pow(r(), 2.5) * 2.4;
    d.position.set(x, h - s * 0.2, z);
    d.rotation.set(r() * 0.5, r() * 6, r() * 0.5);
    d.scale.set(s * (0.8 + r() * 0.5), s, s * (0.8 + r() * 0.5));
    d.updateMatrix();
    m.setMatrixAt(n, d.matrix);
    const g = 0.75 + r() * 0.3;
    m.setColorAt(n, new THREE.Color(g, g * 0.98, g * 0.94));
    if (s > 0.8) colliders.addCircle(x, z, s * 0.8);
    n++;
  }
  m.count = n;
  scene.add(m);
  return m;
}

// --- Toplanabilir doğa ürünleri: böğürtlen çalısı, mantar, kır çiçeği demeti ---
export class Forage {
  constructor(scene, excluded) {
    this.items = [];
    const r = mulberry32(303);
    const add = (kind, n, sampler) => {
      let k = 0, tries = 0;
      while (k < n && tries < n * 200) {
        tries++;
        const [x, z] = sampler();
        const h = heightAt(x, z);
        if (h < 1.2 || slopeAt(x, z) > 0.8 || distToRoad(x, z) < 2 || excluded(x, z)) continue;
        if (Math.hypot(x - FARM.x, z - FARM.z) < 90 || Math.hypot(x - TOWN.x, z - TOWN.z) < 65) continue;
        this.items.push({ kind, x, z, y: h, taken: false, rot: r() * 6 });
        k++;
      }
    };
    const around = (cx, cz, R) => () => { const a = r() * 6.283, rr = Math.sqrt(r()) * R; return [cx + Math.cos(a) * rr, cz + Math.sin(a) * rr]; };
    add('bush', 45, around(FOREST.x + 30, FOREST.z, 70));
    add('bush', 20, around(MEADOW.x, MEADOW.z, 50));
    add('mushroom', 60, around(FOREST.x, FOREST.z, 75));
    add('mushroom', 15, around(-120, -110, 40));
    add('herb', 35, around(MEADOW.x, MEADOW.z, 42));
    add('herb', 15, around(60, -120, 30));

    // instanced parçalar
    const bushGeo = merged([
      M(new THREE.IcosahedronGeometry(0.6, 1), 0, 0.5, 0, 1, 0.8, 1),
      M(new THREE.IcosahedronGeometry(0.45, 1), 0.45, 0.4, 0.1, 1, 0.8, 1),
      M(new THREE.IcosahedronGeometry(0.45, 1), -0.4, 0.38, -0.15, 1, 0.8, 1),
    ]);
    const berryGeo = merged(Array.from({ length: 14 }, (_, i) => {
      const a = i * 2.4, y = 0.3 + (i % 4) * 0.15;
      return M(new THREE.IcosahedronGeometry(0.06, 0), Math.cos(a) * 0.62, y, Math.sin(a) * 0.55);
    }));
    const stemGeo = new THREE.CylinderGeometry(0.05, 0.07, 0.18, 6); stemGeo.translate(0, 0.09, 0);
    const capGeo = new THREE.SphereGeometry(0.14, 8, 5, 0, Math.PI * 2, 0, Math.PI / 2); capGeo.scale(1, 0.7, 1); capGeo.translate(0, 0.17, 0);
    const herbGeo = merged(Array.from({ length: 7 }, (_, i) => M(new THREE.ConeGeometry(0.05, 0.4, 3), Math.cos(i) * 0.12, 0.2, Math.sin(i * 1.7) * 0.12, 1, 1, 1, Math.cos(i) * 0.3, 0, Math.sin(i) * 0.3)));
    const herbFlower = merged(Array.from({ length: 7 }, (_, i) => M(new THREE.IcosahedronGeometry(0.045, 0), Math.cos(i) * 0.12 * 1.3, 0.42, Math.sin(i * 1.7) * 0.12 * 1.3)));
    const count = (k) => this.items.filter((i) => i.kind === k).length;
    this.meshes = {
      bush: new THREE.InstancedMesh(bushGeo, windMaterial({ color: '#ffffff', flatShading: true, roughness: 0.9 }, 0.04, 0.2), count('bush')),
      berry: new THREE.InstancedMesh(berryGeo, mat('#4a1a5a', { roughness: 0.3 }), count('bush')),
      stem: new THREE.InstancedMesh(stemGeo, mat('#efe6d2'), count('mushroom')),
      cap: new THREE.InstancedMesh(capGeo, mat('#ffffff', { roughness: 0.6 }), count('mushroom')),
      herb: new THREE.InstancedMesh(herbGeo, mat('#5c9a3c', { flat: true }), count('herb')),
      herbF: new THREE.InstancedMesh(herbFlower, mat('#a678d8'), count('herb')),
    };
    for (const k in this.meshes) { this.meshes[k].castShadow = true; this.meshes[k].frustumCulled = false; scene.add(this.meshes[k]); }
    let bi = 0, mi = 0, hi = 0;
    for (const it of this.items) {
      if (it.kind === 'bush') it.idx = bi++;
      if (it.kind === 'mushroom') { it.idx = mi++; it.red = r() < 0.35; }
      if (it.kind === 'herb') it.idx = hi++;
    }
    this.zero = new THREE.Matrix4().makeScale(0, 0, 0);
  }
  // ürün mevsimde mi?
  available(it, season) {
    if (it.taken) return false;
    if (it.kind === 'bush') return season === 1 || season === 2;
    if (it.kind === 'mushroom') return season === 2 || season === 0;
    if (it.kind === 'herb') return season === 0 || season === 1;
    return false;
  }
  refresh(season) {
    const d = new THREE.Object3D();
    const c = new THREE.Color();
    for (const it of this.items) {
      d.position.set(it.x, it.y - 0.05, it.z); d.rotation.set(0, it.rot, 0); d.scale.setScalar(1); d.updateMatrix();
      const av = this.available(it, season);
      if (it.kind === 'bush') {
        this.meshes.bush.setMatrixAt(it.idx, season === 3 ? this.zero : d.matrix);
        c.set(['#5aa040', '#3f7f30', '#9a6a2a', '#000'][season]);
        this.meshes.bush.setColorAt(it.idx, c);
        this.meshes.berry.setMatrixAt(it.idx, av ? d.matrix : this.zero);
      } else if (it.kind === 'mushroom') {
        this.meshes.stem.setMatrixAt(it.idx, av ? d.matrix : this.zero);
        this.meshes.cap.setMatrixAt(it.idx, av ? d.matrix : this.zero);
        this.meshes.cap.setColorAt(it.idx, c.set(it.red ? '#c0392b' : '#a0703f'));
      } else {
        this.meshes.herb.setMatrixAt(it.idx, av ? d.matrix : this.zero);
        this.meshes.herbF.setMatrixAt(it.idx, av ? d.matrix : this.zero);
      }
    }
    for (const k in this.meshes) {
      this.meshes[k].instanceMatrix.needsUpdate = true;
      if (this.meshes[k].instanceColor) this.meshes[k].instanceColor.needsUpdate = true;
    }
  }
  nearest(x, z, season, maxD = 2.2) {
    let best = null, bd = maxD;
    for (const it of this.items) {
      if (!this.available(it, season)) continue;
      const d = Math.hypot(it.x - x, it.z - z);
      if (d < bd) { bd = d; best = it; }
    }
    return best;
  }
}

// --- Meyve ağacı (referans 2: yuvarlak taç, merdiven) ---
export function fruitTree(kind = 'elma', ladder = false, seed = 1) {
  const g = new THREE.Group();
  const r = mulberry32(seed);
  const barkM = tmat(T.bark('#7a5434'));
  cyl(g, 0.18, 0.28, 2.2, 7, barkM, 0, 1.1, 0);
  const canopy = new THREE.Group();
  const leafM = new THREE.MeshStandardMaterial({ color: '#4a9a35', flatShading: true, roughness: 0.85 });
  const puffs = [[0, 2.9, 0, 1.3], [0.9, 2.6, 0.2, 0.95], [-0.85, 2.65, -0.2, 1.0], [0.1, 3.5, -0.2, 0.9], [-0.2, 2.5, 0.85, 0.9], [0.3, 2.6, -0.85, 0.9]];
  for (const [x, y, z, s] of puffs) {
    const m = new THREE.Mesh(new THREE.IcosahedronGeometry(s, 1), leafM);
    m.position.set(x, y, z); m.scale.y = 0.85; m.castShadow = true; m.receiveShadow = true;
    canopy.add(m);
  }
  g.add(canopy);
  const bare = new THREE.Group();
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2;
    rod(bare, [0, 2.0, 0], [Math.cos(a) * 1.1, 3.0 + r() * 0.6, Math.sin(a) * 1.1], 0.05, barkM, 4);
  }
  bare.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  g.add(bare);
  const fruitM = new THREE.MeshStandardMaterial({ color: kind === 'elma' ? '#d2322a' : '#f29a3a', roughness: 0.45 });
  const fruits = new THREE.Group();
  for (let i = 0; i < 16; i++) {
    const a = r() * Math.PI * 2, el = r() * 1.0 - 0.2;
    const R = 1.25;
    const m = new THREE.Mesh(new THREE.IcosahedronGeometry(0.11, 1), fruitM);
    m.position.set(Math.cos(a) * R * Math.cos(el), 2.9 + Math.sin(el) * R * 0.85, Math.sin(a) * R * Math.cos(el));
    fruits.add(m);
  }
  g.add(fruits);
  const blossomM = new THREE.MeshStandardMaterial({ color: kind === 'elma' ? '#ffffff' : '#ffb7d0', roughness: 0.8 });
  const blossoms = new THREE.Group();
  for (let i = 0; i < 40; i++) {
    const a = r() * Math.PI * 2, el = r() * 1.2 - 0.3;
    const m = new THREE.Mesh(new THREE.IcosahedronGeometry(0.08, 0), blossomM);
    m.position.set(Math.cos(a) * 1.3 * Math.cos(el), 2.9 + Math.sin(el) * 1.15, Math.sin(a) * 1.3 * Math.cos(el));
    blossoms.add(m);
  }
  g.add(blossoms);
  const snow = new THREE.Group();
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2;
    const m = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.06, 0.12), mat('#f4f7fb'));
    m.position.set(Math.cos(a) * 0.8, 2.75 + r() * 0.4, Math.sin(a) * 0.8); m.rotation.y = -a;
    snow.add(m);
  }
  g.add(snow);
  if (ladder) {
    const lad = new THREE.Group();
    const lm = mat('#2aa79b');
    for (const s of [-1, 1]) box(lad, 0.07, 2.6, 0.07, lm, s * 0.25, 1.3, 0);
    for (let i = 0; i < 7; i++) box(lad, 0.5, 0.05, 0.06, lm, 0, 0.3 + i * 0.35, 0);
    lad.position.set(0.55, 0, 0.75); lad.rotation.set(-0.3, 0.4, 0);
    lad.traverse((o) => { if (o.isMesh) o.castShadow = true; });
    g.add(lad);
  }
  g.traverse((o) => { if (o.isMesh) o.receiveShadow = true; });
  g.children[0].castShadow = true;
  mergeChildren(g);
  const fruitSeason = kind === 'elma' ? [2] : [1];
  return {
    group: g, kind, fruitSeason,
    applySeason(season, hasFruit) {
      canopy.visible = season !== 3;
      bare.visible = season === 3;
      snow.visible = season === 3;
      blossoms.visible = season === 0;
      leafM.color.set(['#5aaa3c', '#3f8a2f', kind === 'elma' ? '#e08a2a' : '#d9a03a', '#3f8a2f'][season]);
      fruits.visible = hasFruit;
    },
  };
}
