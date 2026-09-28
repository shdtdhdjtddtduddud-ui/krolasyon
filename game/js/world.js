// Dünya: arazi mesh'i, su/lav, gökyüzü, ışık, gece-gündüz, bitki örtüsü parçaları
import * as THREE from 'three';
import { Sky } from 'three/addons/objects/Sky.js';
import { HALF, SIZE, N, CELL, H, buildHeights, heightAt, biomeWeights, roadDist, slopeAt, LOC, WAYSTONES, WATER_Y } from './terrain.js';
import { noise, noiseB } from './noise.js';
import { tex } from './textures.js';
import { globalUniforms, addWind } from './mats.js';
import { getProps, GRASS_GEO, FLOWER_GEO } from './props.js';
import { addCircle } from './collision.js';
import { mulberry32, smoothstep, clamp, lerp } from './util.js';

const C = (r, g, b) => new THREE.Color(r, g, b);
const BC = {
  grass: [C(0.36, 0.56, 0.2), C(0.52, 0.62, 0.26)],
  forest: [C(0.2, 0.3, 0.13), C(0.3, 0.33, 0.16)],
  desert: [C(0.88, 0.7, 0.45), C(0.82, 0.5, 0.3)],
  snow: [C(0.9, 0.93, 0.98), C(0.78, 0.84, 0.92)],
  volcano: [C(0.17, 0.14, 0.13), C(0.3, 0.16, 0.12)],
};
const ROCKC = { def: C(0.45, 0.42, 0.39), snow: C(0.52, 0.55, 0.6), desert: C(0.62, 0.42, 0.3), volcano: C(0.12, 0.1, 0.1) };
const ROAD = C(0.5, 0.39, 0.25), SAND = C(0.76, 0.68, 0.5), MUD = C(0.3, 0.27, 0.2);
const _c = new THREE.Color(), _t = new THREE.Color();

export function groundColor(x, z, h, ny, out) {
  const w = biomeWeights(x, z);
  const n = noise.fbm(x * 0.03, z * 0.03, 3) * 0.5 + 0.5;
  out.setRGB(0, 0, 0);
  for (const k in w) { if (w[k] < 0.001) continue; _t.copy(BC[k][0]).lerp(BC[k][1], n); out.r += _t.r * w[k]; out.g += _t.g * w[k]; out.b += _t.b * w[k]; }
  // yükseklikte kar (buzul bölgesinde ve volkan dışındaki zirvelerde)
  const rockK = smoothstep(0.82, 0.62, ny);
  const rc = w.snow > 0.4 ? ROCKC.snow : w.volcano > 0.4 ? ROCKC.volcano : w.desert > 0.4 ? ROCKC.desert : ROCKC.def;
  out.lerp(rc, rockK * 0.85);
  const snowK = smoothstep(38, 55, h + n * 8) * (1 - w.volcano) * (1 - w.desert);
  out.lerp(BC.snow[0], snowK * smoothstep(0.45, 0.7, ny));
  const rd = roadDist(x, z); const roadK = smoothstep(4.2, 1.8, rd + n * 1.2) * (1 - w.snow * 0.5);
  out.lerp(w.desert > 0.5 ? C(0.7, 0.55, 0.38) : w.snow > 0.5 ? C(0.7, 0.72, 0.78) : ROAD, roadK * 0.85);
  // köy meydanı taşlı zemin
  const vd = Math.hypot(x, z); if (vd < 26) out.lerp(C(0.55, 0.5, 0.44), smoothstep(26, 20, vd + n * 4) * 0.8);
  // kıyı kumu ve su altı çamuru
  if (h < 1.2) out.lerp(w.volcano > 0.4 ? C(0.15, 0.08, 0.06) : SAND, smoothstep(1.2, 0.2, h) * 0.8);
  if (h < -1.5) out.lerp(MUD, 0.6);
  return out;
}

export class World {
  constructor(scene, quality) {
    this.scene = scene; this.q = quality;
    this.time = 0.36; // gün saati (0-1)
    this.dayLength = 600;
    this.chunks = new Map(); this.grassCells = new Map();
    this.emitters = []; // ateş, duman vb. partikül kaynakları
    this.lights = [];
  }
  build(progress) {
    buildHeights(); progress?.(0.15);
    this.buildTerrain(); progress?.(0.3);
    this.buildLiquid();
    this.buildSky(); progress?.(0.4);
    this.planChunks(); progress?.(0.5);
    this.buildMinimap();
  }
  buildTerrain() {
    const pos = new Float32Array(N * N * 3), col = new Float32Array(N * N * 3), uv = new Float32Array(N * N * 2);
    for (let j = 0; j < N; j++) for (let i = 0; i < N; i++) {
      const k = j * N + i; const x = -HALF + i * CELL, z = -HALF + j * CELL;
      pos[k * 3] = x; pos[k * 3 + 1] = H[k]; pos[k * 3 + 2] = z; uv[k * 2] = i / (N - 1); uv[k * 2 + 1] = j / (N - 1);
    }
    const idx = new Uint32Array((N - 1) * (N - 1) * 6); let p = 0;
    for (let j = 0; j < N - 1; j++) for (let i = 0; i < N - 1; i++) {
      const a = j * N + i, b = (j + 1) * N + i, c = (j + 1) * N + i + 1, d = j * N + i + 1;
      idx[p++] = a; idx[p++] = b; idx[p++] = c; idx[p++] = a; idx[p++] = c; idx[p++] = d;
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3)); g.setAttribute('uv', new THREE.BufferAttribute(uv, 2)); g.setIndex(new THREE.BufferAttribute(idx, 1));
    g.computeVertexNormals();
    const nrm = g.attributes.normal;
    for (let k = 0; k < N * N; k++) {
      groundColor(pos[k * 3], pos[k * 3 + 2], pos[k * 3 + 1], nrm.getY(k), _c);
      col[k * 3] = _c.r * 1.25; col[k * 3 + 1] = _c.g * 1.25; col[k * 3 + 2] = _c.b * 1.25;
    }
    g.setAttribute('color', new THREE.BufferAttribute(col, 3));
    const detail = tex('terrainDetail'), nmap = tex('terrainNormal');
    const mat = new THREE.MeshStandardMaterial({ vertexColors: true, map: detail, normalMap: nmap, normalScale: new THREE.Vector2(0.45, 0.45), roughness: 0.95, metalness: 0 });
    // uv (0-1) → detay tekrarını shader'da yap: map.repeat ile
    detail.repeat.set(220, 220); nmap.repeat.set(220, 220);
    const m = new THREE.Mesh(g, mat); m.receiveShadow = true; m.name = 'terrain';
    this.scene.add(m); this.terrain = m;
  }
  buildLiquid() {
    const S = 200, pos = [], depth = [], lava = [], idx = [];
    const step = SIZE / S;
    for (let j = 0; j <= S; j++) for (let i = 0; i <= S; i++) {
      const x = -HALF + i * step, z = -HALF + j * step;
      pos.push(x, WATER_Y, z); depth.push(WATER_Y - heightAt(x, z)); lava.push(smoothstep(0.35, 0.55, biomeWeights(x, z).volcano));
    }
    for (let j = 0; j < S; j++) for (let i = 0; i < S; i++) {
      const a = j * (S + 1) + i, b = a + S + 1; // yalnızca suyun görünebileceği hücreler
      const dm = Math.max(depth[a], depth[a + 1], depth[b], depth[b + 1]); if (dm < -3) continue;
      idx.push(a, b, b + 1, a, b + 1, a + 1);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setAttribute('aDepth', new THREE.Float32BufferAttribute(depth, 1)); g.setAttribute('aLava', new THREE.Float32BufferAttribute(lava, 1)); g.setIndex(idx);
    this.liquidUniforms = THREE.UniformsUtils.merge([THREE.UniformsLib.fog, {
      uTime: { value: 0 }, uSunDir: { value: new THREE.Vector3(0, 1, 0) }, uSunColor: { value: new THREE.Color(1, 1, 1) }, uSkyColor: { value: new THREE.Color(0.5, 0.7, 0.9) },
      uDeep: { value: new THREE.Color(0.02, 0.12, 0.2) }, uShallow: { value: new THREE.Color(0.1, 0.45, 0.5) },
    }]);
    const mat = new THREE.ShaderMaterial({
      uniforms: this.liquidUniforms, transparent: true, fog: true,
      vertexShader: `attribute float aDepth; attribute float aLava; uniform float uTime; varying float vDepth; varying float vLava; varying vec3 vWorld;
        #include <fog_pars_vertex>
        void main(){ vec3 p = position; float w = 1.0 - aLava;
          p.y += (sin(p.x*0.15+uTime*1.3)+cos(p.z*0.13+uTime*1.1))*0.1*w + aLava*0.25;
          vec4 wp = modelMatrix*vec4(p,1.0); vWorld = wp.xyz; vDepth = aDepth; vLava = aLava;
          vec4 mvPosition = viewMatrix*wp; gl_Position = projectionMatrix*mvPosition;
          #include <fog_vertex>
        }`,
      fragmentShader: `uniform float uTime; uniform vec3 uSunDir; uniform vec3 uSunColor; uniform vec3 uSkyColor; uniform vec3 uDeep; uniform vec3 uShallow;
        varying float vDepth; varying float vLava; varying vec3 vWorld;
        #include <fog_pars_fragment>
        float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453); }
        float vn(vec2 p){ vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
          return mix(mix(hash(i),hash(i+vec2(1,0)),f.x), mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x), f.y); }
        void main(){
          vec2 p = vWorld.xz;
          float n1 = vn(p*0.35 + uTime*vec2(0.3,0.2)), n2 = vn(p*0.9 - uTime*vec2(0.25,0.4)), n3 = vn(p*2.1 + uTime*vec2(0.5,-0.3));
          vec3 N = normalize(vec3((n1-0.5)*0.5+(n2-0.5)*0.3+(n3-0.5)*0.15, 1.0, (n2-0.5)*0.5-(n1-0.5)*0.3+(n3-0.5)*0.15));
          vec3 V = normalize(cameraPosition - vWorld);
          float fres = pow(1.0 - max(dot(N,V),0.0), 4.0)*0.85 + 0.06;
          float d = clamp(vDepth/5.0, 0.0, 1.0);
          vec3 water = mix(uShallow, uDeep, d);
          water = mix(water, uSkyColor, fres);
          vec3 Hh = normalize(uSunDir + V); float spec = pow(max(dot(N,Hh),0.0), 180.0)*3.0;
          water += uSunColor*spec;
          float foam = smoothstep(0.9, 0.0, vDepth) * (0.55 + 0.45*sin(vDepth*10.0 - uTime*2.5 + n1*6.0));
          water = mix(water, vec3(0.95), clamp(foam, 0.0, 1.0)*0.55);
          float alpha = mix(0.7, 0.94, d) + foam*0.2;
          float ln = vn(p*0.07 + uTime*0.02)*0.6 + vn(p*0.25 - uTime*0.04)*0.4;
          float crust = smoothstep(0.42, 0.62, ln + (n2-0.5)*0.15);
          vec3 lava = mix(vec3(5.0,1.4,0.2), vec3(0.22,0.06,0.03), crust);
          lava += vec3(3.0,0.8,0.05)*pow(vn(p*0.8+uTime*0.3),4.0)*(1.0-crust);
          vec3 col = mix(water, lava, vLava); alpha = mix(alpha, 1.0, vLava);
          gl_FragColor = vec4(col, clamp(alpha,0.0,1.0));
          #include <tonemapping_fragment>
          #include <colorspace_fragment>
          #include <fog_fragment>
        }`,
    });
    const m = new THREE.Mesh(g, mat); m.renderOrder = 2; m.frustumCulled = false; this.scene.add(m); this.liquid = m;
  }
  buildSky() {
    const sky = new Sky(); sky.scale.setScalar(4500); this.sky = sky; this.scene.add(sky);
    const u = sky.material.uniforms; u.turbidity.value = 6; u.rayleigh.value = 1.6; u.mieCoefficient.value = 0.005; u.mieDirectionalG.value = 0.82;
    // yıldızlar
    const sp = [], r = mulberry32(9); for (let i = 0; i < 2500; i++) { const th = r() * Math.PI * 2, ph = Math.acos(r() * 1.9 - 0.9); sp.push(Math.sin(ph) * Math.cos(th) * 3800, Math.cos(ph) * 3800, Math.sin(ph) * Math.sin(th) * 3800); }
    const sg = new THREE.BufferGeometry(); sg.setAttribute('position', new THREE.Float32BufferAttribute(sp, 3));
    this.stars = new THREE.Points(sg, new THREE.PointsMaterial({ color: 0xffffff, size: 2.2, sizeAttenuation: false, transparent: true, opacity: 0, fog: false, depthWrite: false }));
    this.scene.add(this.stars);
    const moon = new THREE.Mesh(new THREE.SphereGeometry(90, 24, 16), new THREE.MeshBasicMaterial({ color: 0xdde6ff, fog: false }));
    this.moon = moon; this.scene.add(moon);
    this.hemi = new THREE.HemisphereLight(0xbfd8ff, 0x5a4a30, 0.7); this.scene.add(this.hemi);
    const sun = new THREE.DirectionalLight(0xffffff, 3); this.sun = sun;
    sun.castShadow = this.q.shadows;
    if (this.q.shadows) {
      sun.shadow.mapSize.set(this.q.shadowSize, this.q.shadowSize);
      const s = sun.shadow.camera; s.left = -55; s.right = 55; s.top = 55; s.bottom = -55; s.near = 10; s.far = 400;
      sun.shadow.bias = -0.0004; sun.shadow.normalBias = 0.04;
    }
    this.scene.add(sun); this.scene.add(sun.target);
    this.scene.fog = new THREE.Fog(0xaac4dd, 40, this.q.viewDist);
    this.sunDir = new THREE.Vector3();
  }
  // ---------- parçalar (chunk) ----------
  planChunks() {
    const CH = 100, NC = SIZE / CH; this.CH = CH; this.NC = NC;
    const props = getProps();
    for (let cj = 0; cj < NC; cj++) for (let ci = 0; ci < NC; ci++) {
      const r = mulberry32(ci * 928371 + cj * 1231 + 7);
      const list = {};
      const x0 = -HALF + ci * CH, z0 = -HALF + cj * CH;
      const tries = 520;
      for (let t = 0; t < tries; t++) {
        const x = x0 + r() * CH, z = z0 + r() * CH;
        if (Math.max(Math.abs(x), Math.abs(z)) > 560) continue;
        const h = heightAt(x, z); if (h < 0.6) continue;
        const vd = Math.hypot(x, z); if (vd < 68) continue;
        if (this.nearSpecial(x, z)) continue;
        const rd = roadDist(x, z); if (rd < 5) continue;
        const sl = slopeAt(x, z); if (sl > 0.9) continue;
        const w = biomeWeights(x, z);
        const u = r();
        let b = 'grass', bv = 0; for (const k in w) if (w[k] * (0.7 + r() * 0.6) > bv) { bv = w[k]; b = k; }
        const type = this.choose(b, u, r, h, x, z);
        if (!type) continue;
        const s = 0.75 + r() * 0.6, rot = r() * Math.PI * 2;
        (list[type] ||= []).push([x, h - 0.1, z, rot, s]);
        const pr = props[type]; if (pr.col > 0) addCircle(x, z, pr.col * s, h + (type.includes('ock') && type !== 'bigRock' && type !== 'darkRock' && type !== 'snowRock' ? 0.8 * s : 99));
      }
      this.chunks.set(ci + ',' + cj, { ci, cj, list, meshes: null, cx: x0 + CH / 2, cz: z0 + CH / 2 });
    }
  }
  nearSpecial(x, z) {
    for (const k of ['orcCamp', 'forestBoss', 'desertBoss', 'pyramid', 'snowBoss', 'dragonLair']) { const l = LOC[k]; if (Math.hypot(x - l.x, z - l.z) < 48) return true; }
    for (const w of WAYSTONES) if (Math.hypot(x - w.x, z - w.z) < 9) return true;
    return false;
  }
  choose(b, u, r, h, x, z) {
    const lake = h < 2.2;
    switch (b) {
      case 'grass': if (u < 0.028) return r() < 0.15 ? 'autumnOak' : 'oak'; if (u < 0.045) return 'bush'; if (u < 0.052) return 'rock'; if (u < 0.055) return 'bigRock'; if (u < 0.058) return 'pine'; if (u < 0.061) return 'log'; return null;
      case 'forest': if (u < 0.09) return r() < 0.55 ? 'darkPine' : 'oak'; if (u < 0.12) return 'darkBush'; if (u < 0.135) return r() < 0.5 ? 'mushBlue' : 'mushPink'; if (u < 0.145) return 'bigRock'; if (u < 0.155) return 'log'; if (u < 0.162) return 'deadTree'; return null;
      case 'desert': { const oas = Math.hypot(x - LOC.oasis.x, z - LOC.oasis.z); if (oas < 55 && u < 0.2) return 'palm'; if (u < 0.012) return 'cactus'; if (u < 0.02) return 'sandRock'; if (u < 0.024) return 'hoodoo'; if (u < 0.0265) return 'ribs'; return null; }
      case 'snow': if (u < 0.04) return 'snowPine'; if (u < 0.052) return 'snowRock'; if (u < 0.06) return 'iceCrystal'; return null;
      case 'volcano': if (u < 0.012) return 'deadTree'; if (u < 0.03) return 'obsidian'; if (u < 0.04) return 'darkRock'; if (u < 0.047) return 'fireCrystal'; return null;
    }
    return null;
  }
  buildChunk(ch) {
    const props = getProps(); ch.meshes = [];
    const m4 = new THREE.Matrix4(), q = new THREE.Quaternion(), e = new THREE.Euler(), v = new THREE.Vector3(), s = new THREE.Vector3();
    for (const type in ch.list) {
      const arr = ch.list[type], pr = props[type];
      const im = new THREE.InstancedMesh(pr.geo, pr.mats, arr.length);
      arr.forEach((a, i) => { e.set(0, a[3], 0); q.setFromEuler(e); v.set(a[0], a[1], a[2]); s.setScalar(a[4]); m4.compose(v, q, s); im.setMatrixAt(i, m4); });
      im.castShadow = pr.shadow !== false && this.q.shadows; im.receiveShadow = true;
      im.computeBoundingSphere();
      this.scene.add(im); ch.meshes.push(im);
    }
  }
  updateChunks(px, pz) {
    const vd = this.q.viewDist + 60;
    for (const ch of this.chunks.values()) {
      const d = Math.hypot(ch.cx - px, ch.cz - pz) - 71;
      const vis = d < vd;
      if (vis && !ch.meshes) this.buildChunk(ch);
      if (ch.meshes) for (const m of ch.meshes) m.visible = vis;
    }
    this.updateGrass(px, pz);
  }
  // ---------- çimen ----------
  updateGrass(px, pz) {
    if (!this.q.grass) return;
    const GC = 32, R = this.q.grassDist;
    if (!this.grassMat) {
      this.grassGeo = GRASS_GEO(); this.flowerGeo = FLOWER_GEO();
      const gm = new THREE.MeshLambertMaterial({ vertexColors: true, side: THREE.DoubleSide });
      const fade = (mat, str) => {
        mat.onBeforeCompile = (sh) => {
          sh.uniforms.uTime = globalUniforms.uTime; sh.uniforms.uFar = { value: R };
          sh.vertexShader = 'uniform float uTime; uniform float uFar;\n' + sh.vertexShader.replace('#include <begin_vertex>', `#include <begin_vertex>
            vec3 ip = vec3(instanceMatrix[3][0], instanceMatrix[3][1], instanceMatrix[3][2]);
            float fd = distance(ip.xz, cameraPosition.xz);
            transformed *= smoothstep(uFar, uFar - 14.0, fd);
            float wph = uTime*2.2 + ip.x*0.25 + ip.z*0.2;
            float amt = position.y*position.y*${str};
            transformed.x += (sin(wph) + sin(wph*2.3)*0.4)*amt; transformed.z += cos(wph*0.9)*amt*0.7;`);
          sh.fragmentShader = sh.fragmentShader.replace('#include <normal_fragment_begin>', 'vec3 normal = normalize(vNormal); vec3 nonPerturbedNormal = normal;');
        };
      };
      fade(gm, '0.28'); this.grassMat = gm;
      const fm = new THREE.MeshLambertMaterial({ vertexColors: true }); fade(fm, '0.2'); this.flowerMat = fm;
    }
    const ci0 = Math.floor((px - R) / GC), ci1 = Math.floor((px + R) / GC), cj0 = Math.floor((pz - R) / GC), cj1 = Math.floor((pz + R) / GC);
    for (let cj = cj0; cj <= cj1; cj++) for (let ci = ci0; ci <= ci1; ci++) {
      const k = ci + ',' + cj; if (this.grassCells.has(k)) continue;
      const cx = ci * GC + GC / 2, cz = cj * GC + GC / 2; if (Math.hypot(cx - px, cz - pz) > R + 24) continue;
      this.grassCells.set(k, this.makeGrassCell(ci, cj, GC));
    }
    for (const [k, c] of this.grassCells) {
      const d = Math.hypot(c.cx - px, c.cz - pz);
      if (d > R + 70) { if (c.g) { this.scene.remove(c.g); c.g.dispose(); } if (c.f) { this.scene.remove(c.f); c.f.dispose(); } this.grassCells.delete(k); }
      else { const v = d < R + 24; if (c.g) c.g.visible = v; if (c.f) c.f.visible = v; }
    }
  }
  makeGrassCell(ci, cj, GC) {
    const r = mulberry32(ci * 7919 + cj * 104729 + 3);
    const cnt = this.q.grassDensity; const gi = [], fi = [];
    const m4 = new THREE.Matrix4(), q = new THREE.Quaternion(), v = new THREE.Vector3(), s = new THREE.Vector3(), up = new THREE.Vector3(0, 1, 0);
    const colG = [], colF = [];
    const flowerCols = [[1, 0.35, 0.35], [1, 0.9, 0.3], [0.6, 0.5, 1], [1, 1, 1], [1, 0.55, 0.85]];
    for (let t = 0; t < cnt; t++) {
      const x = ci * GC + r() * GC, z = cj * GC + r() * GC;
      const h = heightAt(x, z); if (h < 0.4) continue;
      const w = biomeWeights(x, z); const g = w.grass + w.forest * 0.8; if (r() > g * 1.3 - 0.15) continue;
      const vd = Math.hypot(x, z); if (vd < 22 && r() < 0.9) continue;
      if (roadDist(x, z) < 2.4 && r() < 0.85) continue;
      if (slopeAt(x, z) > 0.8) continue;
      const sc = 0.7 + r() * 0.7; q.setFromAxisAngle(up, r() * 6.28); v.set(x, h - 0.05, z);
      const n = noise.fbm(x * 0.03, z * 0.03, 3) * 0.5 + 0.5;
      if (w.grass > 0.5 && r() < 0.06) {
        s.setScalar(0.8 + r() * 0.5); m4.compose(v, q, s); fi.push(m4.clone()); colF.push(flowerCols[Math.floor(r() * flowerCols.length)]);
      } else {
        s.set(sc, sc * (0.8 + r() * 0.6), sc); m4.compose(v, q, s); gi.push(m4.clone());
        const gc = [0.34 + n * 0.2, 0.56 + n * 0.12, 0.18]; const fc = [0.2, 0.33, 0.14];
        const wf = w.forest / (w.grass + w.forest + 1e-4);
        colG.push([lerp(gc[0], fc[0], wf), lerp(gc[1], fc[1], wf), lerp(gc[2], fc[2], wf)]);
      }
    }
    const res = { cx: ci * GC + GC / 2, cz: cj * GC + GC / 2, g: null, f: null };
    const mk = (geo, mat, list, cols) => {
      if (!list.length) return null;
      const im = new THREE.InstancedMesh(geo, mat, list.length); const c = new THREE.Color();
      list.forEach((m, i) => { im.setMatrixAt(i, m); c.setRGB(cols[i][0], cols[i][1], cols[i][2]); im.setColorAt(i, c); });
      im.receiveShadow = true; im.computeBoundingSphere(); this.scene.add(im); return im;
    };
    res.g = mk(this.grassGeo, this.grassMat, gi, colG); res.f = mk(this.flowerGeo, this.flowerMat, fi, colF);
    return res;
  }
  // ---------- mini harita görüntüsü ----------
  buildMinimap() {
    const S = 256, c = document.createElement('canvas'); c.width = c.height = S; const ctx = c.getContext('2d'); const img = ctx.createImageData(S, S);
    for (let j = 0; j < S; j++) for (let i = 0; i < S; i++) {
      const x = -HALF + (i + 0.5) * (SIZE / S), z = -HALF + (j + 0.5) * (SIZE / S); const h = heightAt(x, z);
      const k = (j * S + i) * 4;
      if (h < WATER_Y - 0.3) { const lv = biomeWeights(x, z).volcano > 0.45; img.data[k] = lv ? 230 : 40; img.data[k + 1] = lv ? 90 : 110; img.data[k + 2] = lv ? 20 : 160; img.data[k + 3] = 255; continue; }
      groundColor(x, z, h, 1 - Math.min(0.5, slopeAt(x, z) * 0.4), _c);
      const shade = 0.75 + clamp((heightAt(x + 4, z - 4) - h) * -0.08, -0.3, 0.3) + h * 0.004;
      img.data[k] = clamp(Math.pow(_c.r, 1 / 2.2) * 255 * shade, 0, 255); img.data[k + 1] = clamp(Math.pow(_c.g, 1 / 2.2) * 255 * shade, 0, 255); img.data[k + 2] = clamp(Math.pow(_c.b, 1 / 2.2) * 255 * shade, 0, 255); img.data[k + 3] = 255;
    }
    ctx.putImageData(img, 0, 0); this.mapCanvas = c;
  }
  // ---------- gece/gündüz ----------
  update(dt, player, camera) {
    this.time = (this.time + dt / this.dayLength) % 1;
    globalUniforms.uTime.value += dt;
    const t = this.time; const ang = (t - 0.25) * Math.PI * 2;
    const sy = Math.sin(ang), sx = Math.cos(ang);
    this.sunDir.set(sx * 0.8, sy, 0.45).normalize();
    const day = smoothstep(-0.12, 0.2, sy), dusk = smoothstep(0.35, 0.02, Math.abs(sy)) ;
    const su = this.sky.material.uniforms; su.sunPosition.value.copy(this.sunDir);
    su.rayleigh.value = lerp(0.5, 1.6, day); su.turbidity.value = lerp(2, 6, day);
    this.stars.material.opacity = smoothstep(0.05, -0.2, sy);
    this.stars.rotation.y += dt * 0.004;
    const moonDir = this.sunDir.clone().negate();
    this.moon.position.copy(camera.position).addScaledVector(moonDir, 3500); this.moon.visible = moonDir.y > -0.1;
    this.stars.position.copy(camera.position); this.sky.position.copy(camera.position);
    // ışık: gündüz güneş, gece ay
    const L = this.sun; const useSun = sy > -0.05; const ld = useSun ? this.sunDir : moonDir;
    const sunCol = new THREE.Color().setRGB(1, lerp(0.55, 0.96, smoothstep(0, 0.45, sy)), lerp(0.3, 0.9, smoothstep(0, 0.45, sy)));
    L.color.copy(useSun ? sunCol : new THREE.Color(0.55, 0.65, 1));
    L.intensity = useSun ? 2.6 * smoothstep(-0.05, 0.2, sy) : 0.55 * smoothstep(-0.05, 0.3, -sy);
    const p = player.pos; const texel = 110 / (this.q.shadowSize || 1024);
    const fx = Math.round(p.x / texel) * texel, fz = Math.round(p.z / texel) * texel;
    L.target.position.set(fx, p.y, fz); L.position.set(fx + ld.x * 180, p.y + ld.y * 180, fz + ld.z * 180);
    // biyom tonu
    const w = biomeWeights(p.x, p.z);
    const dayFog = new THREE.Color(0.66, 0.78, 0.9);
    dayFog.lerp(new THREE.Color(0.95, 0.78, 0.55), w.desert * 0.7).lerp(new THREE.Color(0.88, 0.92, 1), w.snow * 0.8).lerp(new THREE.Color(0.45, 0.52, 0.42), w.forest * 0.6).lerp(new THREE.Color(0.45, 0.25, 0.18), w.volcano * 0.85);
    const duskFog = new THREE.Color(0.95, 0.55, 0.35); const nightFog = new THREE.Color(0.04, 0.06, 0.11).lerp(new THREE.Color(0.18, 0.05, 0.03), w.volcano * 0.8);
    const fog = nightFog.clone().lerp(dayFog, day).lerp(duskFog, dusk * 0.45 * day);
    this.scene.fog.color.copy(fog);
    const fogNear = lerp(70, 22, w.forest * 0.7 + w.volcano * 0.5), fogFar = this.q.viewDist * lerp(1, 0.6, w.forest * 0.5 + w.volcano * 0.5);
    this.scene.fog.near = fogNear; this.scene.fog.far = fogFar;
    this.hemi.intensity = lerp(0.3, 0.75, day) + w.volcano * 0.25;
    this.hemi.color.copy(fog).lerp(new THREE.Color(0.7, 0.8, 1), 0.5);
    this.hemi.groundColor.setRGB(0.35, 0.28, 0.2).lerp(new THREE.Color(0.5, 0.15, 0.05), w.volcano);
    this.fogColor = fog; this.daylight = day;
    const lu = this.liquidUniforms; lu.uTime.value += dt; lu.uSunDir.value.copy(ld); lu.uSunColor.value.copy(L.color).multiplyScalar(L.intensity * 0.5); lu.uSkyColor.value.copy(fog).multiplyScalar(1.1);
    lu.uDeep.value.setRGB(0.02, 0.1, 0.16).multiplyScalar(lerp(0.25, 1, day)); lu.uShallow.value.setRGB(0.08, 0.4, 0.45).multiplyScalar(lerp(0.25, 1, day));
    for (const l of this.lights) if (l.flicker) l.light.intensity = l.base * (0.8 + Math.sin(globalUniforms.uTime.value * 13 + l.seed) * 0.1 + Math.sin(globalUniforms.uTime.value * 7.3 + l.seed * 2) * 0.1) * lerp(1.6, 0.8, day);
  }
  get isNight() { return Math.sin((this.time - 0.25) * Math.PI * 2) < -0.05; }
}
