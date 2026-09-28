// Prosedürel canvas texture'ları (hepsi kodla üretilir)
import * as THREE from 'three';
import { makeNoise } from './noise.js';
import { mulberry32 } from './util.js';

const nz = makeNoise(99);
const cache = {};
let maxAniso = 4;
export function setAniso(a) { maxAniso = a; }

function canvas(size) { const c = document.createElement('canvas'); c.width = c.height = size; return c; }
function toTex(c, repeat = 1, srgb = true) {
  const t = new THREE.CanvasTexture(c);
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  t.repeat.set(repeat, repeat);
  t.anisotropy = maxAniso;
  t.colorSpace = srgb ? THREE.SRGBColorSpace : THREE.NoColorSpace;
  return t;
}
// Döşenebilir fbm: torus eşlemesi ile dikişsiz
function tileNoise(u, v, scale, oct = 4) {
  const a = u * Math.PI * 2, b = v * Math.PI * 2;
  const x = Math.cos(a) * scale, y = Math.sin(a) * scale, z = Math.cos(b) * scale, w = Math.sin(b) * scale;
  return (nz.fbm(x + z * 0.7, y + w * 0.7, oct) + nz.fbm(z - y * 0.5 + 31, w + x * 0.5 + 17, oct)) * 0.5;
}
function pixels(size, fn) {
  const c = canvas(size), ctx = c.getContext('2d');
  const img = ctx.createImageData(size, size), d = img.data;
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    const col = fn(x / size, y / size, x, y); const i = (y * size + x) * 4;
    d[i] = col[0]; d[i + 1] = col[1]; d[i + 2] = col[2]; d[i + 3] = col[3] ?? 255;
  }
  ctx.putImageData(img, 0, 0); return c;
}
const mix = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
const cl = (v) => Math.max(0, Math.min(255, v));

// Normal haritası: yükseklik fonksiyonundan
function normalFromHeight(size, hfn, strength = 2) {
  const h = new Float32Array(size * size);
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) h[y * size + x] = hfn(x / size, y / size);
  const c = canvas(size), ctx = c.getContext('2d'); const img = ctx.createImageData(size, size), d = img.data;
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    const l = h[y * size + ((x - 1 + size) % size)], r = h[y * size + ((x + 1) % size)];
    const u = h[((y - 1 + size) % size) * size + x], dn = h[((y + 1) % size) * size + x];
    let nx = (l - r) * strength, ny = (u - dn) * strength, nzz = 1; const len = Math.hypot(nx, ny, nzz);
    const i = (y * size + x) * 4; d[i] = (nx / len * 0.5 + 0.5) * 255; d[i + 1] = (ny / len * 0.5 + 0.5) * 255; d[i + 2] = (nzz / len * 0.5 + 0.5) * 255; d[i + 3] = 255;
  }
  ctx.putImageData(img, 0, 0); return c;
}

export function tex(name) {
  if (cache[name]) return cache[name];
  const gen = GEN[name]; if (!gen) throw new Error('texture yok: ' + name);
  return (cache[name] = gen());
}

const GEN = {
  terrainDetail: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 6, 5) * 0.5 + 0.5, n2 = tileNoise(u, v, 22, 2) * 0.5 + 0.5;
    const g = 212 + n * 34 + n2 * 9; return [g, g, g];
  }), 160, false),
  terrainNormal: () => toTex(normalFromHeight(256, (u, v) => tileNoise(u, v, 9, 5) + tileNoise(u, v, 30, 2) * 0.35, 6), 160, false),
  bark: () => toTex(pixels(256, (u, v) => {
    const n = nz.fbm(Math.cos(u * 6.283) * 2 + v * 1.5, Math.sin(u * 6.283) * 2 + 50, 4);
    const s = Math.abs(Math.sin(u * 40 + n * 6)); const k = 0.55 + s * 0.3 + n * 0.25;
    return [95 * k + 20, 70 * k + 12, 48 * k + 8];
  })),
  barkNormal: () => toTex(normalFromHeight(256, (u, v) => Math.abs(Math.sin(u * 40 + nz.fbm(Math.cos(u * 6.283) * 2 + v * 1.5, Math.sin(u * 6.283) * 2 + 50, 4) * 6)), 3), 1, false),
  leaves: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 8, 4), c = tileNoise(u, v, 30, 2);
    const k = 0.7 + n * 0.35 + (c > 0.3 ? 0.2 : 0); return [cl(70 * k), cl(120 * k), cl(45 * k)];
  })),
  stone: () => {
    const r = mulberry32(7); const pts = []; for (let i = 0; i < 40; i++) pts.push([r(), r()]);
    return toTex(pixels(256, (u, v) => {
      let d1 = 9, d2 = 9;
      for (const p of pts) for (let ox = -1; ox <= 1; ox++) for (let oy = -1; oy <= 1; oy++) {
        const dx = u - p[0] - ox, dy = v - p[1] - oy, d = dx * dx + dy * dy;
        if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) d2 = d;
      }
      const edge = Math.sqrt(d2) - Math.sqrt(d1); const n = tileNoise(u, v, 12, 4) * 0.5 + 0.5;
      const k = (edge < 0.012 ? 0.45 : 0.8 + n * 0.3); return [cl(135 * k), cl(130 * k), cl(122 * k)];
    }));
  },
  rock: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 5, 5) * 0.5 + 0.5, s = tileNoise(u, v, 25, 2) * 0.5 + 0.5; const k = 0.55 + n * 0.5 + s * 0.12;
    return [cl(120 * k), cl(116 * k), cl(110 * k)];
  })),
  rockNormal: () => toTex(normalFromHeight(256, (u, v) => tileNoise(u, v, 5, 5) + tileNoise(u, v, 25, 2) * 0.3, 5), 1, false),
  plaster: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 8, 5) * 0.5 + 0.5; const k = 0.86 + n * 0.14; const dirt = Math.max(0, 1 - v * 3) * 0.15;
    return [cl(232 * k - dirt * 60), cl(220 * k - dirt * 60), cl(196 * k - dirt * 50)];
  })),
  planks: () => toTex(pixels(256, (u, v) => {
    const row = Math.floor(v * 8); const off = (row * 0.37) % 1;
    const seam = (v * 8) % 1 < 0.06 || ((u + off) * 2) % 1 < 0.01;
    const n = nz.fbm(u * 3 + row * 13, v * 40, 3); const grain = Math.sin((u + off) * 60 + n * 8) * 0.5 + 0.5;
    const k = seam ? 0.35 : 0.7 + grain * 0.2 + n * 0.15; return [cl(150 * k), cl(104 * k), cl(62 * k)];
  })),
  roof: () => toTex(pixels(256, (u, v) => {
    const row = Math.floor(v * 10); const off = row % 2 ? 0.5 : 0; const tx = ((u * 8 + off) % 1), ty = (v * 10) % 1;
    const shade = 0.55 + ty * 0.45 - (tx < 0.05 || tx > 0.95 ? 0.3 : 0); const n = tileNoise(u, v, 12, 3) * 0.12;
    const k = shade + n; return [cl(165 * k), cl(72 * k), cl(50 * k)];
  })),
  roofNormal: () => toTex(normalFromHeight(256, (u, v) => { const row = Math.floor(v * 10); const off = row % 2 ? 0.5 : 0; const tx = ((u * 8 + off) % 1); return (v * 10) % 1 - (tx < 0.05 || tx > 0.95 ? 0.5 : 0); }, 3), 1, false),
  cloth: () => toTex(pixels(128, (u, v, x, y) => {
    const w = ((x + y) % 4 < 2 ? 1 : 0.9) * (0.85 + (tileNoise(u, v, 10, 2) * 0.5 + 0.5) * 0.15) * 255; return [w, w, w];
  })),
  leather: () => toTex(pixels(128, (u, v) => {
    const n = tileNoise(u, v, 14, 4) * 0.5 + 0.5; const k = 0.75 + n * 0.25; return [cl(255 * k), cl(255 * k), cl(255 * k)];
  })),
  metal: () => toTex(pixels(128, (u, v) => {
    const s = nz.fbm(u * 2, v * 90, 2) * 0.5 + 0.5, n = tileNoise(u, v, 6, 3) * 0.5 + 0.5; const k = 0.75 + s * 0.15 + n * 0.1;
    return [cl(255 * k), cl(255 * k), cl(255 * k)];
  })),
  skin: () => toTex(pixels(128, (u, v) => { const n = tileNoise(u, v, 10, 3) * 0.5 + 0.5; const k = 0.9 + n * 0.1; return [cl(255 * k), cl(255 * k), cl(255 * k)]; })),
  fur: () => toTex(pixels(256, (u, v) => {
    const s = nz.fbm(u * 60, v * 8, 3) * 0.5 + 0.5, n = tileNoise(u, v, 6, 3) * 0.5 + 0.5; const k = 0.55 + s * 0.35 + n * 0.15;
    return [cl(255 * k), cl(255 * k), cl(255 * k)];
  })),
  furNormal: () => toTex(normalFromHeight(256, (u, v) => nz.fbm(u * 60, v * 8, 3), 4), 1, false),
  scales: () => toTex(pixels(256, (u, v) => {
    const sx = u * 12, sy = v * 16; const row = Math.floor(sy); const ox = row % 2 ? 0.5 : 0;
    const fx = (sx + ox) % 1 - 0.5, fy = sy % 1; const d = Math.sqrt(fx * fx + (fy - 0.2) * (fy - 0.2));
    const k = d > 0.52 ? 0.45 : 0.6 + (1 - d) * 0.45; return [cl(255 * k), cl(255 * k), cl(255 * k)];
  })),
  scalesNormal: () => toTex(normalFromHeight(256, (u, v) => { const sx = u * 12, sy = v * 16; const row = Math.floor(sy); const ox = row % 2 ? 0.5 : 0; const fx = (sx + ox) % 1 - 0.5, fy = sy % 1; return 1 - Math.min(1, Math.sqrt(fx * fx + (fy - 0.2) * (fy - 0.2)) * 1.6); }, 4), 1, false),
  lavaRock: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 5, 5); const k = 0.25 + (n * 0.5 + 0.5) * 0.3; return [cl(90 * k), cl(80 * k), cl(78 * k)];
  })),
  lavaEmissive: () => toTex(pixels(256, (u, v) => {
    const n = Math.abs(tileNoise(u, v, 5, 5)); const c = n < 0.06 ? (1 - n / 0.06) : 0;
    return [cl(255 * c), cl(110 * c * c), cl(20 * c * c * c)];
  })),
  ice: () => toTex(pixels(256, (u, v) => {
    const n = tileNoise(u, v, 4, 4) * 0.5 + 0.5; const crack = Math.abs(tileNoise(u, v, 9, 3)) < 0.03 ? 1 : 0;
    const k = 0.75 + n * 0.25 + crack * 0.2; return [cl(190 * k), cl(225 * k), cl(255 * k)];
  })),
  sandstone: () => toTex(pixels(256, (u, v) => {
    const band = Math.sin(v * 50 + tileNoise(u, v, 4, 3) * 4) * 0.5 + 0.5; const n = tileNoise(u, v, 16, 3) * 0.5 + 0.5; const k = 0.75 + band * 0.15 + n * 0.12;
    return [cl(225 * k), cl(180 * k), cl(120 * k)];
  })),
  bone: () => toTex(pixels(128, (u, v) => { const n = tileNoise(u, v, 8, 4) * 0.5 + 0.5; const k = 0.78 + n * 0.22; return [cl(235 * k), cl(225 * k), cl(195 * k)]; })),
  slime: () => toTex(pixels(128, (u, v) => { const n = tileNoise(u, v, 5, 3) * 0.5 + 0.5; const k = 0.8 + n * 0.2; return [cl(255 * k), cl(255 * k), cl(255 * k)]; })),
  runes: () => {
    const c = canvas(256), ctx = c.getContext('2d'); ctx.fillStyle = '#000'; ctx.fillRect(0, 0, 256, 256);
    const r = mulberry32(3); ctx.strokeStyle = '#fff'; ctx.lineWidth = 5; ctx.lineCap = 'round';
    for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) {
      const cx = 32 + i * 64, cy = 32 + j * 64; ctx.beginPath();
      for (let k = 0; k < 4; k++) { const a = r() * 6.28, b = r() * 6.28; ctx.moveTo(cx + Math.cos(a) * 20, cy + Math.sin(a) * 20); ctx.lineTo(cx + Math.cos(b) * 20, cy + Math.sin(b) * 20); }
      ctx.stroke();
    }
    return toTex(c, 1, true);
  },
  glow: () => {
    const c = canvas(128), ctx = c.getContext('2d'); const g = ctx.createRadialGradient(64, 64, 0, 64, 64, 64);
    g.addColorStop(0, 'rgba(255,255,255,1)'); g.addColorStop(0.25, 'rgba(255,255,255,0.6)'); g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, 128, 128); const t = toTex(c, 1, true); t.wrapS = t.wrapT = THREE.ClampToEdgeWrapping; return t;
  },
  ring: () => {
    const c = canvas(256), ctx = c.getContext('2d'); const g = ctx.createRadialGradient(128, 128, 60, 128, 128, 128);
    g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.75, 'rgba(255,255,255,0.35)'); g.addColorStop(0.92, 'rgba(255,255,255,1)'); g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, 256, 256); const t = toTex(c, 1, true); t.wrapS = t.wrapT = THREE.ClampToEdgeWrapping; return t;
  },
  telegraph: () => {
    const c = canvas(256), ctx = c.getContext('2d'); const g = ctx.createRadialGradient(128, 128, 0, 128, 128, 128);
    g.addColorStop(0, 'rgba(255,255,255,0.25)'); g.addColorStop(0.9, 'rgba(255,255,255,0.45)'); g.addColorStop(0.96, 'rgba(255,255,255,1)'); g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, 256, 256); const t = toTex(c, 1, true); t.wrapS = t.wrapT = THREE.ClampToEdgeWrapping; return t;
  },
  stripes: () => toTex(pixels(128, (u) => (Math.floor(u * 8) % 2 ? [230, 220, 200] : [170, 45, 40]))),
};
