// Tohumlu 2D simplex gürültü + fbm
import { mulberry32 } from './util.js';
const F2 = 0.5 * (Math.sqrt(3) - 1), G2 = (3 - Math.sqrt(3)) / 6;
const grad = [[1,1],[-1,1],[1,-1],[-1,-1],[1,0],[-1,0],[0,1],[0,-1]];
export function makeNoise(seed = 1) {
  const r = mulberry32(seed);
  const p = new Uint8Array(256); for (let i = 0; i < 256; i++) p[i] = i;
  for (let i = 255; i > 0; i--) { const j = Math.floor(r() * (i + 1)); const t = p[i]; p[i] = p[j]; p[j] = t; }
  const perm = new Uint8Array(512); for (let i = 0; i < 512; i++) perm[i] = p[i & 255];
  function n2(x, y) {
    const s = (x + y) * F2; const i = Math.floor(x + s), j = Math.floor(y + s);
    const t = (i + j) * G2; const x0 = x - (i - t), y0 = y - (j - t);
    const i1 = x0 > y0 ? 1 : 0, j1 = x0 > y0 ? 0 : 1;
    const x1 = x0 - i1 + G2, y1 = y0 - j1 + G2, x2 = x0 - 1 + 2 * G2, y2 = y0 - 1 + 2 * G2;
    const ii = i & 255, jj = j & 255;
    let n = 0, tt;
    tt = 0.5 - x0 * x0 - y0 * y0; if (tt > 0) { const g = grad[perm[ii + perm[jj]] & 7]; tt *= tt; n += tt * tt * (g[0] * x0 + g[1] * y0); }
    tt = 0.5 - x1 * x1 - y1 * y1; if (tt > 0) { const g = grad[perm[ii + i1 + perm[jj + j1]] & 7]; tt *= tt; n += tt * tt * (g[0] * x1 + g[1] * y1); }
    tt = 0.5 - x2 * x2 - y2 * y2; if (tt > 0) { const g = grad[perm[ii + 1 + perm[jj + 1]] & 7]; tt *= tt; n += tt * tt * (g[0] * x2 + g[1] * y2); }
    return 70 * n;
  }
  function fbm(x, y, oct = 4, lac = 2, gain = 0.5) {
    let a = 1, f = 1, s = 0, norm = 0;
    for (let o = 0; o < oct; o++) { s += a * n2(x * f, y * f); norm += a; a *= gain; f *= lac; }
    return s / norm;
  }
  function ridged(x, y, oct = 5) {
    let a = 0.5, f = 1, s = 0, w = 1;
    for (let o = 0; o < oct; o++) { let v = 1 - Math.abs(n2(x * f, y * f)); v *= v; v *= w; w = Math.min(1, v * 2); s += v * a; a *= 0.5; f *= 2.1; }
    return s;
  }
  return { n2, fbm, ridged };
}
export const noise = makeNoise(1337);
export const noiseB = makeNoise(4242);
