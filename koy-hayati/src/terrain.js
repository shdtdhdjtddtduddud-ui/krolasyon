// Arazi: yükseklik fonksiyonu, mesh, mevsimsel renkler, su, şelale
import * as THREE from 'three';
import { fbm, ridged, noise2, smoothstep, lerp, clamp, distToPolyline, distToSegment } from './util.js';
import {
  WORLD, HALF, SEG, CELL, FARM, TOWN, LAKE, ISLAND, POOL, PEAK, RIVER, UPPER_STREAM, ROADS,
  FARM_Y, TOWN_Y, UPPER_WATER_Y, WATERFALL_X, MEADOW,
} from './layout.js';
import { groundDetail } from './textures.js';

function carve(h, d, inner, outer, depth) {
  if (d >= outer) return h;
  const t = smoothstep(inner, outer, d);
  return Math.min(h, lerp(depth, h, t));
}
function flatten(h, x, z, c, target) {
  const d = Math.hypot(x - c.x, z - c.z);
  if (d >= c.r1) return h;
  return lerp(target, h, smoothstep(c.r0, c.r1, d));
}

// Analitik yükseklik (mesh üretimi için)
export function rawHeight(x, z) {
  let h = 3 + fbm(x * 0.0065 + 3.1, z * 0.0065 - 1.7, 4) * 7 + fbm(x * 0.03, z * 0.03, 2) * 1.2;
  // kuzey dağ silsilesi
  const m = smoothstep(-115, -235, z);
  if (m > 0) h += m * (34 + ridged(x * 0.011 + 5, z * 0.011, 5) * 52);
  // sınır dağları
  const edge = Math.max(Math.abs(x), Math.abs(z));
  const e = smoothstep(238, 305, edge);
  if (e > 0) h += e * (45 + ridged(x * 0.013, z * 0.013, 4) * 40);
  // Kartal Tepesi
  const dp = Math.hypot(x - PEAK.x, z - PEAK.z);
  h += 48 * Math.exp(-(dp * dp) / (2 * 38 * 38));
  // yayla: hafif tümsek
  const dm = Math.hypot(x - MEADOW.x, z - MEADOW.z);
  h += 4 * Math.exp(-(dm * dm) / (2 * 30 * 30));
  // orman tepeleri
  if (x < -90) h += smoothstep(-90, -170, x) * fbm(x * 0.02, z * 0.02, 3) * 6;

  h = Math.max(h, 0.9);

  // yollar: biraz yumuşat
  let dr = Infinity;
  for (const r of ROADS) dr = Math.min(dr, distToPolyline(x, z, r));
  if (dr < 8) {
    const smooth = 3 + fbm(x * 0.0065 + 3.1, z * 0.0065 - 1.7, 2) * 5;
    if (h < 25) h = lerp(lerp(h, Math.max(smooth, 1.2), 0.35), h, smoothstep(3, 8, dr));
  }


  // düzlükler
  h = flatten(h, x, z, FARM, FARM_Y);
  h = flatten(h, x, z, TOWN, TOWN_Y);
  // zirvede küçük düzlük
  if (dp < 14) {
    const peakH = rawPeakTop();
    h = lerp(peakH, h, smoothstep(5, 14, dp));
  }

  // şelale yarı: kuzeyde dik kaya
  const ax = Math.abs(x - WATERFALL_X);
  const cliffT = smoothstep(-156, -163.5, z) * (1 - smoothstep(30, 55, ax));
  if (cliffT > 0) h = lerp(h, Math.max(h, UPPER_WATER_Y + 1.2), cliffT);

  // üst dere yatağı
  const du = distToPolyline(x, z, UPPER_STREAM);
  if (z < -159) h = carve(h, du, 2.2, 5.5, UPPER_WATER_Y - 1.4);

  // havuz
  const dpool = Math.hypot(x - POOL.x, z - POOL.z);
  h = carve(h, dpool, 7, 13, -3.2);

  // nehir
  const driver = distToPolyline(x, z, RIVER);
  h = carve(h, driver, 3.2, 9, -1.8);

  // göl (dalgalı kıyı)
  const ang = Math.atan2(z - LAKE.z, x - LAKE.x);
  const lr = LAKE.r * (1 + 0.16 * Math.sin(ang * 3 + 1) + 0.08 * Math.sin(ang * 7));
  const dl = Math.hypot(x - LAKE.x, z - LAKE.z);
  h = carve(h, dl, lr - 22, lr + 6, -5.5);
  // göl yatağı biraz pürüzlü
  if (dl < lr) h += noise2(x * 0.08, z * 0.08) * 0.4;
  // ada
  const di = Math.hypot(x - ISLAND.x, z - ISLAND.z);
  if (di < ISLAND.r + 6) h = Math.max(h, lerp(2.4, -5, smoothstep(ISLAND.r - 3, ISLAND.r + 6, di)));

  return h;
}

let _peakTop = null;
function rawPeakTop() {
  if (_peakTop !== null) return _peakTop;
  _peakTop = 0; // geçici: özyinelemeyi engelle
  const x = PEAK.x, z = PEAK.z;
  let h = 3 + fbm(x * 0.0065 + 3.1, z * 0.0065 - 1.7, 4) * 7;
  h += smoothstep(-115, -235, z) * (34 + ridged(x * 0.011 + 5, z * 0.011, 5) * 52);
  h += 48;
  _peakTop = h;
  return h;
}

// --- Izgara ---
const N = SEG + 1;
export const heights = new Float32Array(N * N);
const slopes = new Float32Array(N * N);
const roadMask = new Float32Array(N * N);
const shoreMask = new Float32Array(N * N);
const noiseA = new Float32Array(N * N);

export function buildHeights() {
  for (let iz = 0; iz < N; iz++) {
    for (let ix = 0; ix < N; ix++) {
      const x = -HALF + ix * CELL, z = -HALF + iz * CELL;
      const i = iz * N + ix;
      heights[i] = rawHeight(x, z);
      let dr = Infinity;
      for (const r of ROADS) dr = Math.min(dr, distToPolyline(x, z, r));
      roadMask[i] = 1 - smoothstep(1.4, 3.4, dr);
      noiseA[i] = fbm(x * 0.05, z * 0.05, 3);
    }
  }
  for (let iz = 0; iz < N; iz++) {
    for (let ix = 0; ix < N; ix++) {
      const i = iz * N + ix;
      const hx = heights[iz * N + Math.min(ix + 1, N - 1)] - heights[iz * N + Math.max(ix - 1, 0)];
      const hz = heights[Math.min(iz + 1, N - 1) * N + ix] - heights[Math.max(iz - 1, 0) * N + ix];
      slopes[i] = Math.hypot(hx, hz) / (2 * CELL);
      const h = heights[i];
      shoreMask[i] = h < 1.6 ? 1 - smoothstep(0.6, 1.6, h) : 0;
    }
  }
}

// Mesh üçgenlemesiyle birebir uyumlu yükseklik sorgusu
export function heightAt(x, z) {
  const fx = (x + HALF) / CELL, fz = (z + HALF) / CELL;
  let ix = Math.floor(fx), iz = Math.floor(fz);
  if (ix < 0) ix = 0; if (iz < 0) iz = 0;
  if (ix > SEG - 1) ix = SEG - 1; if (iz > SEG - 1) iz = SEG - 1;
  const u = clamp(fx - ix, 0, 1), v = clamp(fz - iz, 0, 1);
  const ha = heights[iz * N + ix];
  const hb = heights[(iz + 1) * N + ix];
  const hc = heights[(iz + 1) * N + ix + 1];
  const hd = heights[iz * N + ix + 1];
  if (u + v <= 1) return ha + (hd - ha) * u + (hb - ha) * v;
  return hc + (hb - hc) * (1 - u) + (hd - hc) * (1 - v);
}

export function slopeAt(x, z) {
  const e = 1.0;
  const hx = heightAt(x + e, z) - heightAt(x - e, z);
  const hz = heightAt(x, z + e) - heightAt(x, z - e);
  return Math.hypot(hx, hz) / (2 * e);
}

export function normalAt(x, z, out = new THREE.Vector3()) {
  const e = 0.8;
  out.set(heightAt(x - e, z) - heightAt(x + e, z), 2 * e, heightAt(x, z - e) - heightAt(x, z + e));
  return out.normalize();
}

let terrainMesh = null;
export function buildTerrainMesh() {
  const geo = new THREE.BufferGeometry();
  const pos = new Float32Array(N * N * 3);
  const uv = new Float32Array(N * N * 2);
  const col = new Float32Array(N * N * 3);
  for (let iz = 0; iz < N; iz++) {
    for (let ix = 0; ix < N; ix++) {
      const i = iz * N + ix;
      const x = -HALF + ix * CELL, z = -HALF + iz * CELL;
      pos[i * 3] = x; pos[i * 3 + 1] = heights[i]; pos[i * 3 + 2] = z;
      uv[i * 2] = x / 6; uv[i * 2 + 1] = z / 6;
    }
  }
  const idx = new Uint32Array(SEG * SEG * 6);
  let k = 0;
  for (let iz = 0; iz < SEG; iz++) {
    for (let ix = 0; ix < SEG; ix++) {
      const a = iz * N + ix, b = (iz + 1) * N + ix, c = (iz + 1) * N + ix + 1, d = iz * N + ix + 1;
      idx[k++] = a; idx[k++] = b; idx[k++] = d;
      idx[k++] = b; idx[k++] = c; idx[k++] = d;
    }
  }
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  geo.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
  geo.setAttribute('color', new THREE.BufferAttribute(col, 3));
  geo.setIndex(new THREE.BufferAttribute(idx, 1));
  geo.computeVertexNormals();
  const mat = new THREE.MeshStandardMaterial({
    vertexColors: true, map: groundDetail(), roughness: 0.96, metalness: 0,
  });
  terrainMesh = new THREE.Mesh(geo, mat);
  terrainMesh.receiveShadow = true;
  terrainMesh.name = 'terrain';
  return terrainMesh;
}

const C = (hex) => new THREE.Color(hex);
const PAL = {
  grass: [C('#6fae47'), C('#83a83a'), C('#a99a42'), C('#e9eef3')],
  grass2: [C('#4f9440'), C('#6c9431'), C('#b07a35'), C('#dfe7ef')],
  dirt: C('#8a6a45'), dirtWinter: C('#b9b4ad'),
  rock: C('#7b7670'), rock2: C('#5f5a55'),
  sand: C('#cdb886'), sandWinter: C('#d9dde2'),
  snow: C('#f4f7fb'),
  lakebed: C('#5a6b52'),
};

// Mevsime göre arazi renklerini yeniden hesapla
export function colorizeTerrain(season, snowAmount) {
  if (!terrainMesh) return;
  const col = terrainMesh.geometry.attributes.color.array;
  const tmp = new THREE.Color();
  const g1 = PAL.grass[season], g2 = PAL.grass2[season];
  const snowLine = season === 1 ? 82 : season === 3 ? 30 : 62;
  for (let i = 0; i < N * N; i++) {
    const h = heights[i], s = slopes[i], n = noiseA[i];
    tmp.copy(g1).lerp(g2, clamp(n * 1.8 + 0.5, 0, 1));
    if (season !== 3) {
      // çimenin altında toprak lekeleri
      if (n > 0.35) tmp.lerp(PAL.dirt, (n - 0.35) * 1.2);
    }
    // kaya
    const rockT = smoothstep(0.55, 0.95, s);
    if (rockT > 0) tmp.lerp(n > 0 ? PAL.rock : PAL.rock2, rockT);
    // yol
    if (roadMask[i] > 0) tmp.lerp(season === 3 ? PAL.dirtWinter : PAL.dirt, roadMask[i] * 0.92);
    // kıyı kumu
    if (shoreMask[i] > 0) tmp.lerp(season === 3 ? PAL.sandWinter : PAL.sand, shoreMask[i]);
    if (h < -0.5) tmp.lerp(PAL.lakebed, smoothstep(-0.5, -2.5, h));
    // kar
    let snow = smoothstep(snowLine, snowLine + 10, h + n * 6) * (1 - smoothstep(1.0, 1.6, s) * 0.6);
    if (snowAmount > 0 && h > 0.2) {
      const patch = smoothstep(1 - snowAmount - 0.15, 1 - snowAmount + 0.15, n * 0.5 + 0.5 + (snowAmount >= 1 ? 1 : 0));
      snow = Math.max(snow, patch * (1 - rockT * 0.55));
    }
    if (snow > 0) tmp.lerp(PAL.snow, snow);
    col[i * 3] = tmp.r; col[i * 3 + 1] = tmp.g; col[i * 3 + 2] = tmp.b;
  }
  terrainMesh.geometry.attributes.color.needsUpdate = true;
}

// Su derinliği dokusu (R: göl seviyesi, G: üst dere seviyesi)
function buildDepthTexture() {
  const data = new Uint8Array(N * N * 4);
  for (let i = 0; i < N * N; i++) {
    const h = heights[i];
    data[i * 4] = Math.round(clamp(-h / 6, 0, 1) * 255);
    data[i * 4 + 1] = Math.round(clamp((UPPER_WATER_Y - h) / 4, 0, 1) * 255);
    data[i * 4 + 2] = 0; data[i * 4 + 3] = 255;
  }
  const t = new THREE.DataTexture(data, N, N, THREE.RGBAFormat);
  t.magFilter = THREE.LinearFilter; t.minFilter = THREE.LinearFilter;
  t.needsUpdate = true;
  return t;
}

const waterVert = /* glsl */`
  #include <fog_pars_vertex>
  uniform float uTime; uniform float uIce;
  varying vec3 vWorld;
  void main(){
    vec4 wp = modelMatrix * vec4(position, 1.0);
    wp.y += (sin(wp.x*0.35+uTime*1.3)+sin(wp.z*0.28+uTime*1.1))*0.035*(1.0-uIce);
    vWorld = wp.xyz;
    vec4 mvPosition = viewMatrix * wp;
    gl_Position = projectionMatrix * mvPosition;
    #include <fog_vertex>
  }`;
const waterFrag = /* glsl */`
  #include <common>
  #include <fog_pars_fragment>
  uniform float uTime, uIce, uLight, uChannel, uFlow;
  uniform vec3 uSunDir, uSunColor, uSkyTop, uSkyHorizon, uDeep, uShallow;
  uniform sampler2D uDepth;
  uniform float uHalf, uWorld;
  varying vec3 vWorld;
  float wave(vec2 p){
    vec2 q = p + vec2(0.0, uFlow*uTime);
    return sin(q.x*0.8+uTime*1.5)*0.05 + sin(q.y*1.1-uTime*1.2)*0.04
         + sin((q.x+q.y)*2.3+uTime*2.7)*0.02 + sin((q.x-q.y)*3.7-uTime*3.1)*0.012
         + sin((q.x*0.6-q.y*1.7)*5.1+uTime*4.0)*0.006;
  }
  float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453); }
  void main(){
    vec2 p = vWorld.xz;
    float e = 0.06;
    vec3 n = normalize(vec3(-(wave(p+vec2(e,0.0))-wave(p-vec2(e,0.0)))/(2.0*e)*1.4, 1.0,
                            -(wave(p+vec2(0.0,e))-wave(p-vec2(0.0,e)))/(2.0*e)*1.4));
    n = normalize(mix(n, vec3(0.0,1.0,0.0), uIce*0.85));
    vec3 V = normalize(cameraPosition - vWorld);
    float fres = pow(1.0 - max(dot(n, V), 0.0), 4.0) * 0.85 + 0.06;
    vec3 R = reflect(-V, n);
    vec3 sky = mix(uSkyHorizon, uSkyTop, clamp(R.y*1.6, 0.0, 1.0));
    vec2 duv = (p + uHalf) / uWorld;
    vec4 dt = texture2D(uDepth, duv);
    float depth = mix(dt.r*6.0, dt.g*4.0, uChannel);
    vec3 base = mix(uShallow, uDeep, smoothstep(0.0, 3.5, depth)) * uLight;
    vec3 col = mix(base, sky, fres);
    float spec = pow(max(dot(R, uSunDir), 0.0), 220.0) * 2.5;
    col += uSunColor * spec;
    // kıyı köpüğü
    float foam = (1.0 - smoothstep(0.0, 0.35, depth)) * (0.55 + 0.45*sin(uTime*2.0 + p.x*0.7 + p.y*0.5));
    col = mix(col, vec3(0.92,0.96,0.97)*uLight, foam*0.55*(1.0-uIce));
    float alpha = clamp(0.55 + smoothstep(0.0, 1.6, depth)*0.35 + fres*0.2, 0.0, 0.96);
    // buz
    float crack = step(0.985, hash(floor(p*1.5))) * 0.15;
    vec3 ice = mix(vec3(0.70,0.82,0.90), vec3(0.88,0.94,1.0), fres + crack) * uLight + uSunColor*spec*0.25;
    col = mix(col, ice, uIce);
    alpha = mix(alpha, 0.97, uIce);
    gl_FragColor = vec4(col, alpha);
    #include <tonemapping_fragment>
    #include <colorspace_fragment>
    #include <fog_fragment>
  }`;

export const waterUniformsList = [];
function makeWaterMaterial(depthTex, channel) {
  const u = THREE.UniformsUtils.merge([THREE.UniformsLib.fog, {
    uTime: { value: 0 }, uIce: { value: 0 }, uLight: { value: 1 }, uChannel: { value: channel }, uFlow: { value: channel ? -1.2 : 0 },
    uSunDir: { value: new THREE.Vector3(0, 1, 0) }, uSunColor: { value: new THREE.Color(1, 1, 1) },
    uSkyTop: { value: new THREE.Color('#5a8fd0') }, uSkyHorizon: { value: new THREE.Color('#bcd6ec') },
    uDeep: { value: new THREE.Color('#14485e') }, uShallow: { value: new THREE.Color('#3f9a96') },
    uDepth: { value: null }, uHalf: { value: HALF }, uWorld: { value: WORLD },
  }]);
  u.uDepth.value = depthTex;
  const m = new THREE.ShaderMaterial({
    uniforms: u, vertexShader: waterVert, fragmentShader: waterFrag,
    transparent: true, fog: true, depthWrite: false,
  });
  waterUniformsList.push(u);
  return m;
}

// Şelale shader'ı
const fallVert = /* glsl */`
  #include <fog_pars_vertex>
  varying vec2 vUv;
  void main(){
    vUv = uv;
    vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * mvPosition;
    #include <fog_vertex>
  }`;
const fallFrag = /* glsl */`
  #include <common>
  #include <fog_pars_fragment>
  uniform float uTime, uIce, uLight;
  varying vec2 vUv;
  float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453); }
  float vnoise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    vec2 u = f*f*(3.0-2.0*f);
    return mix(mix(hash(i), hash(i+vec2(1,0)), u.x), mix(hash(i+vec2(0,1)), hash(i+vec2(1,1)), u.x), u.y);
  }
  void main(){
    float t = uTime * (1.0 - uIce);
    vec2 q = vec2(vUv.x*14.0, vUv.y*4.0 + t*3.2);
    float s = vnoise(q) * 0.6 + vnoise(q*2.3 + 7.0) * 0.4;
    float streak = smoothstep(0.35, 0.85, s);
    vec3 col = mix(vec3(0.55,0.75,0.85), vec3(0.97,0.99,1.0), streak);
    float edge = smoothstep(0.0, 0.12, vUv.x) * smoothstep(1.0, 0.88, vUv.x);
    float bottomFoam = smoothstep(0.75, 1.0, vUv.y);
    col = mix(col, vec3(1.0), bottomFoam*0.6);
    float alpha = edge * (0.55 + streak*0.4 + bottomFoam*0.3);
    vec3 ice = mix(vec3(0.72,0.86,0.95), vec3(0.95,0.98,1.0), s);
    col = mix(col, ice, uIce);
    alpha = mix(alpha, edge*0.95, uIce);
    gl_FragColor = vec4(col * uLight, alpha);
    #include <tonemapping_fragment>
    #include <colorspace_fragment>
    #include <fog_fragment>
  }`;

export const fallUniforms = THREE.UniformsUtils.merge([THREE.UniformsLib.fog, {
  uTime: { value: 0 }, uIce: { value: 0 }, uLight: { value: 1 },
}]);

export function terrainZAtHeight(x, y, zSouth, zNorth) {
  // güneyden kuzeye ilerle, yüksekliğin y'yi geçtiği ilk noktayı bul
  for (let z = zSouth; z >= zNorth; z -= 0.1) {
    if (heightAt(x, z) >= y) return z;
  }
  return zNorth;
}

export function buildWater(scene) {
  const depthTex = buildDepthTexture();
  const lakeGeo = new THREE.PlaneGeometry(WORLD, WORLD, 128, 128);
  lakeGeo.rotateX(-Math.PI / 2);
  const lake = new THREE.Mesh(lakeGeo, makeWaterMaterial(depthTex, 0));
  lake.position.y = 0;
  lake.renderOrder = 2;
  scene.add(lake);

  // üst dere suyu
  const upGeo = new THREE.PlaneGeometry(14, 56, 8, 32);
  upGeo.rotateX(-Math.PI / 2);
  const up = new THREE.Mesh(upGeo, makeWaterMaterial(depthTex, 1));
  up.position.set(WATERFALL_X - 1, UPPER_WATER_Y, -162.6 - 28);
  up.renderOrder = 2;
  scene.add(up);

  // şelale: kayaya yaslanan şerit
  const rows = 40, cols = 6, width = 6.5;
  const top = UPPER_WATER_Y + 0.05, bottom = -0.4;
  const fpos = [], fuv = [], fidx = [];
  for (let r = 0; r <= rows; r++) {
    const t = r / rows;
    const y = lerp(top, bottom, t);
    for (let c = 0; c <= cols; c++) {
      const s = c / cols;
      const x = WATERFALL_X + (s - 0.5) * width * (1 + t * 0.35);
      let z = terrainZAtHeight(x, y, -150, -166) + 0.7 + t * 0.6;
      if (r === 0) z = -162.4;
      fpos.push(x, y, z);
      fuv.push(s, t);
    }
  }
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const a = r * (cols + 1) + c, b = a + cols + 1;
      fidx.push(a, b, a + 1, b, b + 1, a + 1);
    }
  }
  const fgeo = new THREE.BufferGeometry();
  fgeo.setAttribute('position', new THREE.Float32BufferAttribute(fpos, 3));
  fgeo.setAttribute('uv', new THREE.Float32BufferAttribute(fuv, 2));
  fgeo.setIndex(fidx);
  fgeo.computeVertexNormals();
  const fmat = new THREE.ShaderMaterial({
    uniforms: fallUniforms, vertexShader: fallVert, fragmentShader: fallFrag,
    transparent: true, fog: true, depthWrite: false, side: THREE.DoubleSide,
  });
  const fall = new THREE.Mesh(fgeo, fmat);
  fall.renderOrder = 3;
  scene.add(fall);

  // sis/serpinti parçacıkları
  const mistCount = 160;
  const mgeo = new THREE.BufferGeometry();
  const mp = new Float32Array(mistCount * 3);
  const seeds = new Float32Array(mistCount);
  for (let i = 0; i < mistCount; i++) { seeds[i] = Math.random(); }
  mgeo.setAttribute('position', new THREE.BufferAttribute(mp, 3));
  const mistTex = softDot();
  const mmat = new THREE.PointsMaterial({ size: 2.2, map: mistTex, transparent: true, opacity: 0.45, depthWrite: false, color: 0xffffff });
  const mist = new THREE.Points(mgeo, mmat);
  mist.frustumCulled = false;
  scene.add(mist);
  const baseZ = terrainZAtHeight(WATERFALL_X, 0.5, -150, -166) + 1.5;

  return {
    lake, up, fall, mist,
    update(time) {
      const arr = mgeo.attributes.position.array;
      for (let i = 0; i < mistCount; i++) {
        const ph = (seeds[i] + time * 0.25) % 1;
        const a = seeds[i] * 40;
        arr[i * 3] = WATERFALL_X + Math.sin(a) * (1 + ph * 4);
        arr[i * 3 + 1] = 0.2 + ph * 4.5;
        arr[i * 3 + 2] = baseZ + Math.cos(a) * (0.5 + ph * 3) + ph * 2;
      }
      mgeo.attributes.position.needsUpdate = true;
    },
  };
}

let _dot = null;
export function softDot() {
  if (_dot) return _dot;
  const c = document.createElement('canvas'); c.width = c.height = 64;
  const g = c.getContext('2d');
  const gr = g.createRadialGradient(32, 32, 0, 32, 32, 32);
  gr.addColorStop(0, 'rgba(255,255,255,1)'); gr.addColorStop(1, 'rgba(255,255,255,0)');
  g.fillStyle = gr; g.fillRect(0, 0, 64, 64);
  _dot = new THREE.CanvasTexture(c);
  return _dot;
}

// Bir noktanın suda olup olmadığı (göl seviyesine göre)
export function isWater(x, z, minDepth = 0.4) {
  return heightAt(x, z) < -minDepth;
}

export function distToRiver(x, z) { return distToPolyline(x, z, RIVER); }
export function distToRoad(x, z) {
  let d = Infinity;
  for (const r of ROADS) d = Math.min(d, distToPolyline(x, z, r));
  return d;
}
export { distToSegment };
