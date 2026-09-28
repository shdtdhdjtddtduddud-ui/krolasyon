// Basit 2D (XZ) çarpışma: daireler ve döndürülmüş kutular, uzamsal hash ile
const CS = 16;
const cells = new Map();
const key = (i, j) => i * 100003 + j;
export const colliders = [];
export function addCircle(x, z, r, h = 99) { add({ t: 0, x, z, r, h }); }
export function addBox(x, z, hx, hz, rot = 0, h = 99) { const c = { t: 1, x, z, hx, hz, c: Math.cos(rot), s: Math.sin(rot), r: Math.hypot(hx, hz), h }; add(c); return c; }
function add(c) {
  colliders.push(c);
  const i0 = Math.floor((c.x - c.r) / CS), i1 = Math.floor((c.x + c.r) / CS), j0 = Math.floor((c.z - c.r) / CS), j1 = Math.floor((c.z + c.r) / CS);
  for (let i = i0; i <= i1; i++) for (let j = j0; j <= j1; j++) { const k = key(i, j); let a = cells.get(k); if (!a) cells.set(k, (a = [])); a.push(c); }
}
const seen = new Set();
// pos: {x,z}; y: karakterin ayak yüksekliği (üstünden atlanabilir objeler için)
export function resolve(pos, rad, y = -1e9) {
  const i0 = Math.floor((pos.x - rad) / CS), i1 = Math.floor((pos.x + rad) / CS), j0 = Math.floor((pos.z - rad) / CS), j1 = Math.floor((pos.z + rad) / CS);
  seen.clear(); let hit = false;
  for (let i = i0; i <= i1; i++) for (let j = j0; j <= j1; j++) {
    const a = cells.get(key(i, j)); if (!a) continue;
    for (const c of a) {
      if (seen.has(c)) continue; seen.add(c);
      if (y > c.h || c.off) continue;
      if (c.t === 0) {
        const dx = pos.x - c.x, dz = pos.z - c.z, d = Math.hypot(dx, dz), m = c.r + rad;
        if (d < m && d > 1e-5) { pos.x = c.x + (dx / d) * m; pos.z = c.z + (dz / d) * m; hit = true; }
      } else {
        const dx = pos.x - c.x, dz = pos.z - c.z;
        const lx = dx * c.c - dz * c.s, lz = dx * c.s + dz * c.c; // three.js rotation.y ile aynı yön
        const cx = Math.max(-c.hx, Math.min(c.hx, lx)), cz = Math.max(-c.hz, Math.min(c.hz, lz));
        let ox = lx - cx, oz = lz - cz, d = Math.hypot(ox, oz);
        if (d < rad) {
          let nx, nz, pen;
          if (d > 1e-5) { nx = ox / d; nz = oz / d; pen = rad - d; }
          else { // merkez kutunun içinde: en yakın kenara it
            const px = c.hx - Math.abs(lx), pz = c.hz - Math.abs(lz);
            if (px < pz) { nx = Math.sign(lx) || 1; nz = 0; pen = px + rad; } else { nx = 0; nz = Math.sign(lz) || 1; pen = pz + rad; }
          }
          const wx = nx * c.c + nz * c.s, wz = -nx * c.s + nz * c.c;
          pos.x += wx * pen; pos.z += wz * pen; hit = true;
        }
      }
    }
  }
  return hit;
}
