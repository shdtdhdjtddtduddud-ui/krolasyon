// Basit 2B (x-z) çarpışma: kutular ve daireler, uzaysal ızgara ile
export class Colliders {
  constructor(cell = 8) {
    this.cell = cell;
    this.map = new Map();
    this.all = [];
  }
  _cells(minX, maxX, minZ, maxZ, fn) {
    const c = this.cell;
    const x0 = Math.floor(minX / c), x1 = Math.floor(maxX / c);
    const z0 = Math.floor(minZ / c), z1 = Math.floor(maxZ / c);
    for (let ix = x0; ix <= x1; ix++) for (let iz = z0; iz <= z1; iz++) fn(ix + ',' + iz);
  }
  _insert(col) {
    this.all.push(col);
    this._cells(col.minX, col.maxX, col.minZ, col.maxZ, (k) => {
      let a = this.map.get(k);
      if (!a) { a = []; this.map.set(k, a); }
      a.push(col);
    });
    return col;
  }
  addBox(minX, maxX, minZ, maxZ, data = null) {
    return this._insert({ type: 'box', minX, maxX, minZ, maxZ, data, disabled: false });
  }
  addCircle(x, z, r, data = null) {
    return this._insert({ type: 'circle', x, z, r, minX: x - r, maxX: x + r, minZ: z - r, maxZ: z + r, data, disabled: false });
  }
  remove(col) {
    col.disabled = true;
  }
  query(minX, maxX, minZ, maxZ) {
    const out = new Set();
    this._cells(minX, maxX, minZ, maxZ, (k) => {
      const a = this.map.get(k);
      if (a) for (const c of a) if (!c.disabled) out.add(c);
    });
    return out;
  }
  queryCircles(x, z, r) {
    const out = [];
    for (const c of this.query(x - r, x + r, z - r, z + r)) if (c.type === 'circle') out.push(c);
    return out;
  }
  // p: {x, z}; yarıçap r. Çakışmayı çözer, çarpıştıysa true döner
  resolve(p, r) {
    let hit = false;
    for (let iter = 0; iter < 2; iter++) {
      for (const c of this.query(p.x - r, p.x + r, p.z - r, p.z + r)) {
        if (c.type === 'circle') {
          const dx = p.x - c.x, dz = p.z - c.z;
          const d = Math.hypot(dx, dz), m = r + c.r;
          if (d < m && d > 1e-5) { p.x = c.x + (dx / d) * m; p.z = c.z + (dz / d) * m; hit = true; }
        } else {
          const cx = Math.max(c.minX, Math.min(p.x, c.maxX));
          const cz = Math.max(c.minZ, Math.min(p.z, c.maxZ));
          const dx = p.x - cx, dz = p.z - cz;
          const d2 = dx * dx + dz * dz;
          if (d2 < r * r) {
            hit = true;
            if (d2 > 1e-8) {
              const d = Math.sqrt(d2);
              p.x = cx + (dx / d) * r; p.z = cz + (dz / d) * r;
            } else {
              // merkez kutunun içinde: en yakın kenardan çıkar
              const l = p.x - c.minX, rr = c.maxX - p.x, t = p.z - c.minZ, b = c.maxZ - p.z;
              const m = Math.min(l, rr, t, b);
              if (m === l) p.x = c.minX - r; else if (m === rr) p.x = c.maxX + r;
              else if (m === t) p.z = c.minZ - r; else p.z = c.maxZ + r;
            }
          }
        }
      }
    }
    return hit;
  }
  hits(x, z, r) {
    for (const c of this.query(x - r, x + r, z - r, z + r)) {
      if (c.type === 'circle') { if (Math.hypot(x - c.x, z - c.z) < r + c.r) return true; }
      else {
        const cx = Math.max(c.minX, Math.min(x, c.maxX)), cz = Math.max(c.minZ, Math.min(z, c.maxZ));
        if ((x - cx) ** 2 + (z - cz) ** 2 < r * r) return true;
      }
    }
    return false;
  }
}
