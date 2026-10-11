// Prosedürel (kodla çizilen) dokular. Hepsi canvas üzerinde üretilir.
import * as THREE from 'three';
import { mulberry32 } from './util.js';

const cache = new Map();

function makeTex(key, w, h, draw, opts = {}) {
  if (cache.has(key)) return cache.get(key);
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  const g = c.getContext('2d');
  draw(g, w, h);
  const t = new THREE.CanvasTexture(c);
  if (opts.color !== false) t.colorSpace = THREE.SRGBColorSpace;
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  t.anisotropy = 4;
  t.needsUpdate = true;
  cache.set(key, t);
  return t;
}

function hexToRgb(hex) {
  const n = parseInt(hex.replace('#', ''), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}
function shade(hex, f, r = 0) {
  const [R, G, B] = hexToRgb(hex);
  const k = (v) => Math.max(0, Math.min(255, Math.round(v * f + r)));
  return `rgb(${k(R)},${k(G)},${k(B)})`;
}

function speckle(g, w, h, rnd, n, colors, size = 1.5, alpha = 0.25) {
  for (let i = 0; i < n; i++) {
    g.globalAlpha = alpha * rnd();
    g.fillStyle = colors[Math.floor(rnd() * colors.length)];
    const s = size * (0.5 + rnd());
    g.fillRect(rnd() * w, rnd() * h, s, s);
  }
  g.globalAlpha = 1;
}

// Tahta kalaslar: dikey ya da yatay, damarlı, çivili
export function planks(color = '#8a5a34', count = 6, opts = {}) {
  const key = `planks${color}${count}${JSON.stringify(opts)}`;
  return makeTex(key, 256, 256, (g, w, h) => {
    const rnd = mulberry32(color.length * 31 + count);
    const pw = w / count;
    for (let i = 0; i < count; i++) {
      const f = 0.82 + rnd() * 0.3;
      g.fillStyle = shade(color, f);
      g.fillRect(i * pw, 0, pw, h);
      // damar çizgileri
      for (let k = 0; k < 9; k++) {
        g.strokeStyle = shade(color, f * (0.7 + rnd() * 0.2));
        g.globalAlpha = 0.25 + rnd() * 0.3;
        g.lineWidth = 0.6 + rnd() * 1.2;
        g.beginPath();
        const x0 = i * pw + rnd() * pw;
        g.moveTo(x0, 0);
        for (let y = 0; y <= h; y += 16) {
          g.lineTo(x0 + Math.sin(y * 0.05 + k + i) * (2 + rnd() * 2), y);
        }
        g.stroke();
      }
      g.globalAlpha = 1;
      // budak
      if (rnd() < 0.6) {
        const kx = i * pw + pw * (0.3 + rnd() * 0.4), ky = rnd() * h;
        g.fillStyle = shade(color, 0.5);
        g.beginPath(); g.ellipse(kx, ky, 3 + rnd() * 3, 5 + rnd() * 5, 0, 0, Math.PI * 2); g.fill();
        g.strokeStyle = shade(color, 0.65); g.lineWidth = 1;
        g.beginPath(); g.ellipse(kx, ky, 6 + rnd() * 3, 10 + rnd() * 5, 0, 0, Math.PI * 2); g.stroke();
      }
      // derz
      g.fillStyle = shade(color, 0.35);
      g.fillRect(i * pw, 0, 2, h);
      // kalas uç kesikleri
      const cut = rnd() * h;
      g.fillRect(i * pw, cut, pw, 2);
      if (opts.nails !== false) {
        g.fillStyle = '#3a3a3a';
        for (const y of [10, h - 12, cut + 8, cut - 8]) {
          g.fillRect(i * pw + 5, y, 3, 3);
          g.fillRect(i * pw + pw - 8, y, 3, 3);
        }
      }
    }
    speckle(g, w, h, rnd, 900, ['#000', '#fff'], 1.5, 0.12);
    if (opts.weathered) {
      for (let i = 0; i < 40; i++) {
        g.globalAlpha = 0.08 + rnd() * 0.1;
        g.fillStyle = rnd() < 0.5 ? '#ddd' : '#222';
        g.beginPath(); g.ellipse(rnd() * w, rnd() * h, 10 + rnd() * 30, 4 + rnd() * 10, rnd() * 3, 0, 7); g.fill();
      }
      g.globalAlpha = 1;
    }
  });
}

// Kiremit çatı
export function roofTiles(color = '#a8462e') {
  return makeTex('roof' + color, 256, 256, (g, w, h) => {
    const rnd = mulberry32(77);
    g.fillStyle = shade(color, 0.6); g.fillRect(0, 0, w, h);
    const rows = 8, cols = 8, rh = h / rows, cw = w / cols;
    for (let r = 0; r < rows; r++) {
      for (let c = -1; c < cols; c++) {
        const x = c * cw + (r % 2 ? cw / 2 : 0), y = r * rh;
        const f = 0.85 + rnd() * 0.3;
        const gr = g.createLinearGradient(x, y, x + cw, y);
        gr.addColorStop(0, shade(color, f * 0.75));
        gr.addColorStop(0.5, shade(color, f * 1.1));
        gr.addColorStop(1, shade(color, f * 0.7));
        g.fillStyle = gr;
        g.beginPath();
        g.moveTo(x + 1, y);
        g.lineTo(x + cw - 1, y);
        g.lineTo(x + cw - 1, y + rh - 4);
        g.quadraticCurveTo(x + cw / 2, y + rh + 3, x + 1, y + rh - 4);
        g.fill();
        g.fillStyle = 'rgba(0,0,0,0.35)';
        g.fillRect(x + 1, y + rh - 3, cw - 2, 2);
      }
    }
    speckle(g, w, h, rnd, 1500, ['#000', '#fff', '#6b7a3a'], 1.6, 0.18);
  });
}

// Oluklu metal sac
export function corrugated(color = '#b9bec2') {
  return makeTex('corr' + color, 128, 128, (g, w, h) => {
    const rnd = mulberry32(5);
    for (let x = 0; x < w; x++) {
      const v = 0.75 + 0.3 * Math.sin((x / w) * Math.PI * 2 * 8);
      g.fillStyle = shade(color, v);
      g.fillRect(x, 0, 1, h);
    }
    for (let i = 0; i < 25; i++) {
      g.globalAlpha = 0.1 + rnd() * 0.15;
      g.fillStyle = '#7a4b2a';
      g.fillRect(rnd() * w, rnd() * h, 2 + rnd() * 4, 6 + rnd() * 30);
    }
    g.globalAlpha = 1;
    speckle(g, w, h, rnd, 400, ['#000', '#fff'], 1, 0.15);
  });
}

export function bricks(color = '#9a4a32') {
  return makeTex('brick' + color, 256, 256, (g, w, h) => {
    const rnd = mulberry32(9);
    g.fillStyle = '#cfc6b5'; g.fillRect(0, 0, w, h);
    const rh = 16, bw = 40;
    for (let r = 0; r < h / rh; r++) {
      for (let c = -1; c < w / bw + 1; c++) {
        const x = c * bw + (r % 2 ? bw / 2 : 0);
        g.fillStyle = shade(color, 0.8 + rnd() * 0.35);
        g.fillRect(x + 2, r * rh + 2, bw - 3, rh - 3);
      }
    }
    speckle(g, w, h, rnd, 1500, ['#000', '#fff'], 1.5, 0.15);
  });
}

export function stone(color = '#8d8a83') {
  return makeTex('stone' + color, 256, 256, (g, w, h) => {
    const rnd = mulberry32(21);
    g.fillStyle = shade(color, 0.55); g.fillRect(0, 0, w, h);
    for (let i = 0; i < 70; i++) {
      const x = rnd() * w, y = rnd() * h, rx = 12 + rnd() * 18, ry = 9 + rnd() * 12;
      g.fillStyle = shade(color, 0.8 + rnd() * 0.4);
      g.beginPath(); g.ellipse(x, y, rx, ry, rnd() * 3, 0, 7); g.fill();
      g.fillStyle = 'rgba(255,255,255,0.08)';
      g.beginPath(); g.ellipse(x - 2, y - 3, rx * 0.6, ry * 0.5, 0, 0, 7); g.fill();
    }
    speckle(g, w, h, rnd, 2000, ['#000', '#fff'], 1.4, 0.2);
  });
}

export function bark(color = '#6b4a2f') {
  return makeTex('bark' + color, 128, 256, (g, w, h) => {
    const rnd = mulberry32(3);
    g.fillStyle = color; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 70; i++) {
      g.strokeStyle = shade(color, 0.5 + rnd() * 0.3);
      g.lineWidth = 1 + rnd() * 3;
      g.beginPath();
      let x = rnd() * w;
      g.moveTo(x, 0);
      for (let y = 0; y < h; y += 20) { x += (rnd() - 0.5) * 6; g.lineTo(x, y); }
      g.stroke();
    }
    speckle(g, w, h, rnd, 800, ['#000', '#c8b89a'], 2, 0.2);
  });
}

export function birchBark() {
  return makeTex('birch', 128, 256, (g, w, h) => {
    const rnd = mulberry32(8);
    g.fillStyle = '#e9e5dc'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 40; i++) {
      g.fillStyle = '#2b2a28';
      g.globalAlpha = 0.6 + rnd() * 0.4;
      g.fillRect(rnd() * w, rnd() * h, 6 + rnd() * 30, 2 + rnd() * 3);
    }
    g.globalAlpha = 1;
    speckle(g, w, h, rnd, 600, ['#888', '#000'], 2, 0.2);
  });
}

export function hay() {
  return makeTex('hay', 256, 256, (g, w, h) => {
    const rnd = mulberry32(12);
    g.fillStyle = '#d9a845'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 2600; i++) {
      g.strokeStyle = ['#f2cd6b', '#b98a2c', '#e8bb52', '#a07424', '#fbe08d'][Math.floor(rnd() * 5)];
      g.globalAlpha = 0.5 + rnd() * 0.5;
      g.lineWidth = 0.8 + rnd();
      const x = rnd() * w, y = rnd() * h, a = (rnd() - 0.5) * 0.9, l = 6 + rnd() * 14;
      g.beginPath(); g.moveTo(x, y); g.lineTo(x + Math.cos(a) * l, y + Math.sin(a) * l); g.stroke();
    }
    g.globalAlpha = 1;
  });
}

// Saman balyası yüzü (sarmal)
export function haySpiral() {
  return makeTex('hayspiral', 256, 256, (g, w, h) => {
    const rnd = mulberry32(13);
    g.fillStyle = '#d6a443'; g.fillRect(0, 0, w, h);
    g.lineWidth = 3;
    for (let a = 0; a < 40; a += 0.05) {
      const r = a * 3.2;
      g.strokeStyle = ['#f0cb69', '#a97c28', '#e2b450'][Math.floor(rnd() * 3)];
      g.beginPath();
      g.arc(w / 2, h / 2, r, a, a + 0.08);
      g.stroke();
    }
  });
}

export function soil(wet = false) {
  return makeTex('soil' + wet, 256, 256, (g, w, h) => {
    const rnd = mulberry32(wet ? 40 : 41);
    const base = wet ? '#3e2a1c' : '#6e4a2e';
    g.fillStyle = base; g.fillRect(0, 0, w, h);
    for (let y = 0; y < h; y += 32) {
      const gr = g.createLinearGradient(0, y, 0, y + 32);
      gr.addColorStop(0, 'rgba(0,0,0,0.35)');
      gr.addColorStop(0.5, 'rgba(255,255,255,0.08)');
      gr.addColorStop(1, 'rgba(0,0,0,0.35)');
      g.fillStyle = gr; g.fillRect(0, y, w, 32);
    }
    speckle(g, w, h, rnd, 3000, ['#000', '#a07850', '#2a1a10', '#8a8070'], 2, 0.4);
  });
}

// Arazi detay dokusu (gri tonlu, vertex rengiyle çarpılır)
export function groundDetail() {
  return makeTex('ground', 256, 256, (g, w, h) => {
    const rnd = mulberry32(99);
    g.fillStyle = '#d8d8d8'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 5000; i++) {
      const v = Math.floor(170 + rnd() * 85);
      g.fillStyle = `rgb(${v},${v},${v})`;
      g.globalAlpha = 0.5;
      const s = 1 + rnd() * 3;
      g.fillRect(rnd() * w, rnd() * h, s, s * (1 + rnd() * 2));
    }
    g.globalAlpha = 1;
    for (let i = 0; i < 400; i++) {
      g.strokeStyle = rnd() < 0.5 ? '#ffffff' : '#9a9a9a';
      g.globalAlpha = 0.35;
      const x = rnd() * w, y = rnd() * h;
      g.beginPath(); g.moveTo(x, y); g.lineTo(x + (rnd() - 0.5) * 3, y - 4 - rnd() * 6); g.stroke();
    }
    g.globalAlpha = 1;
  });
}

export function cowSpots() {
  return makeTex('cow', 256, 256, (g, w, h) => {
    const rnd = mulberry32(17);
    g.fillStyle = '#f3efe6'; g.fillRect(0, 0, w, h);
    g.fillStyle = '#7a4a2a';
    for (let i = 0; i < 9; i++) {
      const x = rnd() * w, y = rnd() * h;
      g.beginPath();
      for (let a = 0; a < Math.PI * 2; a += 0.4) {
        const r = 20 + rnd() * 22;
        g.lineTo(x + Math.cos(a) * r, y + Math.sin(a) * r * 0.8);
      }
      g.fill();
    }
    speckle(g, w, h, rnd, 600, ['#000', '#fff'], 2, 0.1);
  });
}

export function wool() {
  return makeTex('wool', 128, 128, (g, w, h) => {
    const rnd = mulberry32(19);
    g.fillStyle = '#ece6da'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 400; i++) {
      g.strokeStyle = rnd() < 0.5 ? '#fffaf0' : '#cfc6b4';
      g.lineWidth = 1.5;
      g.beginPath(); g.arc(rnd() * w, rnd() * h, 2 + rnd() * 4, 0, Math.PI * 1.5); g.stroke();
    }
  });
}

export function stripes(a = '#f1e6cf', b = '#2b3566', n = 8) {
  return makeTex('stripes' + a + b + n, 256, 64, (g, w, h) => {
    const sw = w / n;
    for (let i = 0; i < n; i++) {
      g.fillStyle = i % 2 ? b : a;
      g.fillRect(i * sw, 0, sw, h);
    }
    const rnd = mulberry32(2);
    speckle(g, w, h, rnd, 500, ['#000', '#fff'], 1.5, 0.12);
  });
}

// Mavi Yunan desenli yelken
export function sail() {
  return makeTex('sail', 256, 256, (g, w, h) => {
    const rnd = mulberry32(23);
    g.fillStyle = '#efe7d4'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 1500; i++) {
      g.fillStyle = rnd() < 0.5 ? 'rgba(0,0,0,0.05)' : 'rgba(255,255,255,0.12)';
      g.fillRect(rnd() * w, rnd() * h, 2, 2);
    }
    const band = (y) => {
      g.fillStyle = '#2f7fc4'; g.fillRect(0, y, w, 34);
      g.strokeStyle = '#efe7d4'; g.lineWidth = 4;
      for (let x = 0; x < w; x += 32) {
        g.beginPath();
        g.moveTo(x + 4, y + 28); g.lineTo(x + 4, y + 6); g.lineTo(x + 26, y + 6);
        g.lineTo(x + 26, y + 22); g.lineTo(x + 14, y + 22); g.lineTo(x + 14, y + 14);
        g.stroke();
      }
    };
    band(14); band(h - 48);
  });
}

// Paslı, solgun açık mavi kamyon boyası
export function rustyPaint(color = '#8fc1b5') {
  return makeTex('rusty' + color, 256, 256, (g, w, h) => {
    const rnd = mulberry32(31);
    g.fillStyle = color; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 60; i++) {
      g.globalAlpha = 0.2 + rnd() * 0.5;
      g.fillStyle = ['#a0522d', '#7b3f1d', '#c87a4a', '#d9e8e2'][Math.floor(rnd() * 4)];
      g.beginPath(); g.ellipse(rnd() * w, rnd() * h, 3 + rnd() * 16, 1 + rnd() * 6, rnd() * 3, 0, 7); g.fill();
    }
    g.globalAlpha = 1;
    speckle(g, w, h, rnd, 1200, ['#000', '#fff', '#8b4513'], 1.5, 0.2);
  });
}

export function paintedMetal(color = '#2f8a3a') {
  return makeTex('pm' + color, 128, 128, (g, w, h) => {
    const rnd = mulberry32(37);
    const gr = g.createLinearGradient(0, 0, 0, h);
    gr.addColorStop(0, shade(color, 1.12)); gr.addColorStop(1, shade(color, 0.9));
    g.fillStyle = gr; g.fillRect(0, 0, w, h);
    speckle(g, w, h, rnd, 500, ['#000', '#fff'], 1.2, 0.08);
  });
}

export function tire() {
  return makeTex('tire', 256, 64, (g, w, h) => {
    g.fillStyle = '#262422'; g.fillRect(0, 0, w, h);
    g.fillStyle = '#121110';
    for (let x = 0; x < w; x += 16) {
      g.beginPath(); g.moveTo(x, 0); g.lineTo(x + 10, h / 2); g.lineTo(x, h); g.lineTo(x + 6, h); g.lineTo(x + 16, h / 2); g.lineTo(x + 6, 0); g.fill();
    }
  });
}

export function halfTimber(wall = '#f1e8d6', beam = '#5b3a24') {
  return makeTex('ht' + wall + beam, 256, 256, (g, w, h) => {
    const rnd = mulberry32(43);
    g.fillStyle = wall; g.fillRect(0, 0, w, h);
    speckle(g, w, h, rnd, 2000, ['#000', '#fff', '#c9b896'], 2, 0.1);
    g.fillStyle = beam;
    g.fillRect(0, 0, w, 14); g.fillRect(0, h - 14, w, 14);
    g.fillRect(0, 0, 14, h); g.fillRect(w - 14, 0, 14, h);
    g.fillRect(w / 2 - 7, 0, 14, h);
    g.lineWidth = 12; g.strokeStyle = beam;
    g.beginPath(); g.moveTo(14, h - 14); g.lineTo(w / 2 - 7, 14); g.stroke();
    g.beginPath(); g.moveTo(w - 14, h - 14); g.lineTo(w / 2 + 7, 14); g.stroke();
  });
}

export function plaster(color = '#efe6d2') {
  return makeTex('plaster' + color, 256, 256, (g, w, h) => {
    const rnd = mulberry32(47);
    g.fillStyle = color; g.fillRect(0, 0, w, h);
    speckle(g, w, h, rnd, 3000, ['#000', '#fff', '#c9b896'], 2.5, 0.08);
  });
}

export function rug() {
  return makeTex('rug', 256, 256, (g, w, h) => {
    g.fillStyle = '#8c2a22'; g.fillRect(0, 0, w, h);
    g.strokeStyle = '#e2b65c'; g.lineWidth = 8; g.strokeRect(14, 14, w - 28, h - 28);
    g.strokeStyle = '#1f3d6b'; g.lineWidth = 6; g.strokeRect(34, 34, w - 68, h - 68);
    g.fillStyle = '#e2b65c';
    g.save(); g.translate(w / 2, h / 2); g.rotate(Math.PI / 4);
    g.fillRect(-40, -40, 80, 80); g.fillStyle = '#1f3d6b'; g.fillRect(-22, -22, 44, 44);
    g.restore();
    const rnd = mulberry32(51);
    speckle(g, w, h, rnd, 1500, ['#000', '#fff'], 2, 0.1);
  });
}

// Dantel örtü (masa için)
export function doily() {
  return makeTex('doily', 256, 256, (g, w, h) => {
    g.fillStyle = '#7a4024'; g.fillRect(0, 0, w, h);
    g.translate(w / 2, h / 2);
    g.fillStyle = '#f4ecf2';
    for (let i = 0; i < 16; i++) {
      g.save(); g.rotate((i / 16) * Math.PI * 2);
      g.beginPath(); g.arc(0, 88, 22, 0, Math.PI * 2); g.fill();
      g.restore();
    }
    g.beginPath(); g.arc(0, 0, 92, 0, Math.PI * 2); g.fill();
    g.strokeStyle = '#d9c3d2'; g.lineWidth = 3;
    for (const r of [20, 40, 60, 78]) { g.beginPath(); g.arc(0, 0, r, 0, Math.PI * 2); g.stroke(); }
    g.fillStyle = '#7a4024';
    for (let i = 0; i < 24; i++) {
      g.save(); g.rotate((i / 24) * Math.PI * 2);
      g.beginPath(); g.arc(0, 68, 4, 0, Math.PI * 2); g.fill();
      g.restore();
    }
  });
}

export function signCarrot() {
  return makeTex('signcarrot', 128, 160, (g, w, h) => {
    g.fillStyle = '#2d2622'; g.fillRect(0, 0, w, h);
    const rnd = mulberry32(53);
    speckle(g, w, h, rnd, 600, ['#fff'], 1.5, 0.08);
    g.fillStyle = '#e86a2a';
    g.beginPath(); g.moveTo(44, 60); g.quadraticCurveTo(64, 50, 84, 60); g.lineTo(66, 140); g.closePath(); g.fill();
    g.strokeStyle = '#b84a1a'; g.lineWidth = 2;
    for (let y = 75; y < 130; y += 12) { g.beginPath(); g.moveTo(52 + (y - 60) * 0.15, y); g.lineTo(62, y + 2); g.stroke(); }
    g.fillStyle = '#5cae3a';
    for (const a of [-0.5, 0, 0.5]) {
      g.save(); g.translate(64, 58); g.rotate(a);
      g.beginPath(); g.ellipse(0, -18, 6, 18, 0, 0, 7); g.fill(); g.restore();
    }
  });
}

export function boardText(lines, bg = '#6b4a2f', fg = '#f4e7c9') {
  return makeTex('board' + lines.join('|') + bg, 256, 128, (g, w, h) => {
    g.fillStyle = bg; g.fillRect(0, 0, w, h);
    const rnd = mulberry32(57);
    speckle(g, w, h, rnd, 800, ['#000', '#fff'], 2, 0.1);
    g.fillStyle = fg; g.textAlign = 'center'; g.textBaseline = 'middle';
    const fs = lines.length > 1 ? 34 : 44;
    g.font = `bold ${fs}px Georgia, serif`;
    lines.forEach((l, i) => g.fillText(l, w / 2, h / 2 + (i - (lines.length - 1) / 2) * (fs + 6)));
  }, {});
}

export function melonStripes() {
  return makeTex('melon', 128, 128, (g, w, h) => {
    g.fillStyle = '#3f8a35'; g.fillRect(0, 0, w, h);
    g.fillStyle = '#1f4f1c';
    for (let x = 0; x < w; x += 16) {
      g.beginPath();
      for (let y = 0; y <= h; y += 8) g.lineTo(x + Math.sin(y * 0.3) * 2, y);
      for (let y = h; y >= 0; y -= 8) g.lineTo(x + 7 + Math.sin(y * 0.3) * 2, y);
      g.fill();
    }
  });
}

export function cheese() {
  return makeTex('cheese', 128, 128, (g, w, h) => {
    g.fillStyle = '#f2c14e'; g.fillRect(0, 0, w, h);
    const rnd = mulberry32(61);
    g.fillStyle = '#d99c2b';
    for (let i = 0; i < 14; i++) { g.beginPath(); g.arc(rnd() * w, rnd() * h, 3 + rnd() * 6, 0, 7); g.fill(); }
  });
}
