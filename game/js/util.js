// Genel yardımcı fonksiyonlar
export const clamp = (v, a, b) => (v < a ? a : v > b ? b : v);
export const lerp = (a, b, t) => a + (b - a) * t;
export const damp = (a, b, k, dt) => a + (b - a) * (1 - Math.exp(-k * dt));
export const smoothstep = (a, b, x) => { const t = clamp((x - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); };
export const TAU = Math.PI * 2;
export function wrapAngle(a) { while (a > Math.PI) a -= TAU; while (a < -Math.PI) a += TAU; return a; }
export function dampAngle(a, b, k, dt) { return a + wrapAngle(b - a) * (1 - Math.exp(-k * dt)); }
export function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
export const rand = (a = 0, b = 1) => a + Math.random() * (b - a);
export const randInt = (a, b) => Math.floor(rand(a, b + 1));
export const pick = (arr, r = Math.random) => arr[Math.floor(r() * arr.length)];
export function hash2(x, z) {
  let h = (x * 374761393 + z * 668265263) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}
export function dist2(ax, az, bx, bz) { const dx = ax - bx, dz = az - bz; return Math.sqrt(dx * dx + dz * dz); }
export function fmt(n) { return Math.round(n).toLocaleString('tr-TR'); }
