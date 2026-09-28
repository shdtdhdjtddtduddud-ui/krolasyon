// Arazi yüksekliği, biyomlar, yollar – tüm dünya bu fonksiyonlardan türetilir
import { noise, noiseB } from './noise.js';
import { smoothstep, clamp, lerp } from './util.js';

export const HALF = 600, SIZE = 1200, N = 257, CELL = SIZE / (N - 1);
export const BORDER = 520;
export const BIOME_NAMES = { grass: 'Yeşil Vadi', forest: 'Karanlık Orman', desert: 'Kızıl Çöl', snow: 'Buzul Zirveleri', volcano: 'Kül Diyarı' };
const ANG = { desert: 0, volcano: Math.PI / 2, snow: Math.PI, forest: -Math.PI / 2 };

export const LOC = {
  village: { x: 0, z: 0 },
  orcCamp: { x: -120, z: 95 },
  forestBoss: { x: 0, z: -395 },
  desertBoss: { x: 385, z: 10 },
  pyramid: { x: 455, z: 10 },
  oasis: { x: 300, z: -120 },
  snowBoss: { x: -390, z: 0 },
  dragonLair: { x: 0, z: 375 },
  volcanoPeak: { x: 0, z: 475 },
};
export const WAYSTONES = [
  { id: 'village', name: 'Köy Meydanı', x: 14, z: -18 },
  { id: 'forest', name: 'Orman Girişi', x: 8, z: -255 },
  { id: 'desert', name: 'Çöl Kapısı', x: 255, z: 8 },
  { id: 'snow', name: 'Buzul Geçidi', x: -255, z: 8 },
  { id: 'volcano', name: 'Kül Sınırı', x: 8, z: 255 },
];
// Yollar (köyden her bölgeye)
const ROADS = [
  [[0, 0], [10, -120], [-6, -255], [0, -350]],
  [[0, 0], [130, 12], [255, -4], [350, 10]],
  [[0, 0], [-130, -10], [-255, 4], [-350, 0]],
  [[0, 0], [-10, 130], [6, 255], [0, 335]],
  [[-10, 60], [-70, 80], [-110, 90]],
  [[250, -4], [300, -110]],
];
const FLAT = [
  { x: 0, z: 0, r: 62, f: 30, h: 4 },
  { x: LOC.orcCamp.x, z: LOC.orcCamp.z, r: 26, f: 16, h: null },
  { x: LOC.forestBoss.x, z: LOC.forestBoss.z, r: 38, f: 20, h: null },
  { x: LOC.desertBoss.x, z: LOC.desertBoss.z, r: 38, f: 22, h: 6 },
  { x: LOC.pyramid.x, z: LOC.pyramid.z, r: 40, f: 10, h: 6 },
  { x: LOC.snowBoss.x, z: LOC.snowBoss.z, r: 40, f: 30, h: 16 },
  { x: LOC.dragonLair.x, z: LOC.dragonLair.z, r: 44, f: 26, h: 12 },
  ...WAYSTONES.map((w) => ({ x: w.x, z: w.z, r: 6, f: 8, h: null })),
];

export function biomeWeights(x, z, out = {}) {
  const r = Math.hypot(x, z);
  const warp = noise.fbm(x * 0.004 + 10, z * 0.004 - 7, 3);
  const th = Math.atan2(z, x) + warp * 0.45;
  const outer = smoothstep(135, 225, r + warp * 55);
  let s = 0; const w = {};
  for (const k in ANG) { const c = Math.cos(th - ANG[k]); const v = c > 0 ? c ** 6 : 0; w[k] = v; s += v; }
  out.grass = 1 - outer;
  for (const k in ANG) out[k] = (outer * w[k]) / (s || 1);
  return out;
}
export function dominantBiome(x, z) {
  const w = biomeWeights(x, z); let best = 'grass', bv = -1;
  for (const k in w) if (w[k] > bv) { bv = w[k]; best = k; }
  return best;
}
function segDist(px, pz, ax, az, bx, bz) {
  const dx = bx - ax, dz = bz - az; const t = clamp(((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz), 0, 1);
  return Math.hypot(px - ax - dx * t, pz - az - dz * t);
}
export function roadDist(x, z) {
  const wx = x + noise.n2(x * 0.02, z * 0.02) * 6, wz = z + noise.n2(x * 0.02 + 9, z * 0.02) * 6;
  let d = 1e9;
  for (const r of ROADS) for (let i = 0; i < r.length - 1; i++) d = Math.min(d, segDist(wx, wz, r[i][0], r[i][1], r[i + 1][0], r[i + 1][1]));
  return d;
}

const _w = {};
function rawHeight(x, z) {
  const w = biomeWeights(x, z, _w);
  const rd = roadDist(x, z); const roadWide = smoothstep(34, 8, rd);
  const n1 = noise.fbm(x * 0.0035, z * 0.0035, 5);
  const n2 = noise.fbm(x * 0.012 + 50, z * 0.012, 3);
  let h = 0;
  h += w.grass * (4 + n1 * 7 + n2 * 1.5);
  h += w.forest * (6 + n1 * 12 + n2 * 4);
  const dn = noise.n2(x * 0.011, z * 0.02 + x * 0.004); const dune = (1 - Math.abs(dn)) ** 3;
  h += w.desert * (4 + dune * 10 + n1 * 5);
  if (w.snow > 0.001) {
    const rg = noise.ridged(x * 0.0055 + 3, z * 0.0055 - 3, 5);
    h += w.snow * (9 + rg * 80 * smoothstep(0.15, 0.6, rg) * (1 - roadWide * 0.92) + n2 * 3);
  }
  if (w.volcano > 0.001) {
    const dv = Math.hypot(x - LOC.volcanoPeak.x, z - LOC.volcanoPeak.z); const cone = Math.max(0, 1 - dv / 230);
    let vh = 4 + n1 * 5 + cone ** 1.6 * 115 * (1 - roadWide * 0.6);
    if (dv < 38) vh -= (1 - dv / 38) ** 2 * 45;
    const pn = noiseB.fbm(x * 0.02, z * 0.02, 3); if (pn > 0.22) vh -= (pn - 0.22) * 30 * (1 - roadWide);
    h += w.volcano * vh;
  }
  const ln = noiseB.fbm(x * 0.006 + 20, z * 0.006 - 40, 3);
  let lake = smoothstep(0.3, 0.5, ln) * clamp((w.grass + w.forest) * 1.5 - 0.3, 0, 1) * (1 - smoothstep(14, 4, rd));
  const oas = Math.hypot(x - LOC.oasis.x, z - LOC.oasis.z); lake = Math.max(lake, smoothstep(34, 16, oas));
  h = lerp(h, -4.5, lake);
  const e = Math.max(Math.abs(x), Math.abs(z)); if (e > 470) h += ((e - 470) / 130) ** 2 * 90 * (1 + n2 * 0.3);
  // yol kenarlarını hafif düzleştir
  h = lerp(h, h * 0.85 + 1, smoothstep(6, 1, rd) * 0.5);
  return h;
}
for (const f of FLAT) if (f.h === null) f.h = Math.max(1.5, rawHeight(f.x, f.z));
function worldHeight(x, z) {
  let h = rawHeight(x, z);
  for (const f of FLAT) {
    const d = Math.hypot(x - f.x, z - f.z); if (d < f.r + f.f) h = lerp(h, f.h, smoothstep(f.r + f.f, f.r, d));
  }
  return h;
}

export const H = new Float32Array(N * N);
export function buildHeights() { for (let j = 0; j < N; j++) for (let i = 0; i < N; i++) H[j * N + i] = worldHeight(-HALF + i * CELL, -HALF + j * CELL); }

// Mesh üçgenleriyle birebir aynı yükseklik interpolasyonu
export function heightAt(x, z) {
  let gx = (x + HALF) / CELL, gz = (z + HALF) / CELL;
  gx = clamp(gx, 0, N - 1.001); gz = clamp(gz, 0, N - 1.001);
  const i = Math.floor(gx), j = Math.floor(gz), fx = gx - i, fz = gz - j;
  const h00 = H[j * N + i], h10 = H[j * N + i + 1], h01 = H[(j + 1) * N + i], h11 = H[(j + 1) * N + i + 1];
  if (fx > fz) return h00 + fx * (h10 - h00) + fz * (h11 - h10);
  return h00 + fz * (h01 - h00) + fx * (h11 - h01);
}
export function slopeAt(x, z) {
  const e = 1.5; const dx = heightAt(x + e, z) - heightAt(x - e, z), dz = heightAt(x, z + e) - heightAt(x, z - e);
  return Math.hypot(dx, dz) / (2 * e);
}
export const WATER_Y = 0;
export function groundY(x, z) { return heightAt(x, z); }
export function inWater(x, z) { return heightAt(x, z) < WATER_Y - 1.1; }
