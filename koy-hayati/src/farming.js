// Tarla kareleri: toprak, ekinler (instanced), hedef vurgusu
import * as THREE from 'three';
import * as T from './textures.js';
import { mat, tmat } from './kit.js';
import { cropModels } from './plants.js';
import { windMaterial } from './nature.js';
import { CROPS } from './data.js';
import { FARM_Y } from './layout.js';

export const FIELD = { x0: 6, z0: 22, cols: 20, rows: 12 };
export const GH = { x0: 51, z0: 35, cols: 3, rows: 5 };

export class Fields {
  constructor(scene) {
    this.scene = scene;
    this.tiles = [];
    for (let r = 0; r < FIELD.rows; r++) for (let c = 0; c < FIELD.cols; c++) {
      this.tiles.push({ x: FIELD.x0 + 1 + c * 2, z: FIELD.z0 + 1 + r * 2, gh: false });
    }
    for (let r = 0; r < GH.rows; r++) for (let c = 0; c < GH.cols; c++) {
      this.tiles.push({ x: GH.x0 + 1 + c * 2, z: GH.z0 + 1 + r * 2, gh: true });
    }
    const N = this.tiles.length;
    const soilGeo = new THREE.BoxGeometry(1.94, 0.12, 1.94);
    this.soil = new THREE.InstancedMesh(soilGeo, tmat(T.soil(false), '#ffffff', { roughness: 1 }), N);
    this.soil.receiveShadow = true;
    this.soil.frustumCulled = false;
    scene.add(this.soil);
    this.models = cropModels();
    this.crops = {};
    for (const k of Object.keys(CROPS)) {
      const m = this.models[k];
      const leaf = new THREE.InstancedMesh(m.leaf, windMaterial({ color: '#ffffff', side: THREE.DoubleSide, roughness: 0.8, flatShading: true }, 0.12, 0.1), N);
      const fruitMat = m.fruitTex ? tmat(T.melonStripes(), '#ffffff', { roughness: 0.5 }) : mat('#ffffff', { roughness: 0.45, flat: true });
      const fruit = new THREE.InstancedMesh(m.fruit, fruitMat, N);
      leaf.castShadow = true; fruit.castShadow = true;
      leaf.frustumCulled = false; fruit.frustumCulled = false;
      leaf.receiveShadow = true;
      scene.add(leaf, fruit);
      this.crops[k] = { leaf, fruit };
    }
    this.zero = new THREE.Matrix4().makeScale(0, 0, 0);
    for (let i = 0; i < N; i++) {
      this.soil.setMatrixAt(i, this.zero);
      this.soil.setColorAt(i, new THREE.Color('#ffffff'));
      for (const k in this.crops) {
        this.crops[k].leaf.setMatrixAt(i, this.zero);
        this.crops[k].leaf.setColorAt(i, new THREE.Color('#4f9a38'));
        this.crops[k].fruit.setMatrixAt(i, this.zero);
        this.crops[k].fruit.setColorAt(i, new THREE.Color('#ffffff'));
      }
    }
    // hedef vurgusu
    const eg = new THREE.EdgesGeometry(new THREE.BoxGeometry(2, 0.25, 2));
    this.highlight = new THREE.LineSegments(eg, new THREE.LineBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.9 }));
    this.highlight.visible = false;
    scene.add(this.highlight);
  }
  tileAt(x, z, ghOwned) {
    let c = Math.floor((x - FIELD.x0) / 2), r = Math.floor((z - FIELD.z0) / 2);
    if (c >= 0 && c < FIELD.cols && r >= 0 && r < FIELD.rows) return r * FIELD.cols + c;
    if (!ghOwned) return -1;
    c = Math.floor((x - GH.x0) / 2); r = Math.floor((z - GH.z0) / 2);
    if (c >= 0 && c < GH.cols && r >= 0 && r < GH.rows) return FIELD.rows * FIELD.cols + r * GH.cols + c;
    return -1;
  }
  setHighlight(i, color) {
    if (i < 0) { this.highlight.visible = false; return; }
    const t = this.tiles[i];
    this.highlight.visible = true;
    this.highlight.position.set(t.x, FARM_Y + 0.1, t.z);
    this.highlight.material.color.set(color);
  }
  // Tüm kareleri yeniden kur: yalnızca dolu kareler çizilir (boş örnek yok)
  refreshAll(tilesState, season) {
    const m = new THREE.Matrix4(), q = new THREE.Quaternion(), up = new THREE.Vector3(0, 1, 0);
    const P = new THREE.Vector3(), S = new THREE.Vector3();
    const c = new THREE.Color();
    let ns = 0;
    const counts = {};
    for (const k in this.crops) counts[k] = { l: 0, f: 0 };
    tilesState.forEach((st, i) => {
      const t = this.tiles[i];
      if (t.gh && !this.ghOwned) return;
      const frozen = season === 3 && !t.gh;
      if (st.t) {
        m.makeTranslation(t.x, FARM_Y, t.z);
        this.soil.setMatrixAt(ns, m);
        this.soil.setColorAt(ns, c.set(frozen ? '#c9d2da' : st.w ? '#6e5a50' : '#ffffff'));
        ns++;
      }
      if (!st.c || !this.crops[st.c]) return;
      const k = st.c, def = CROPS[k], md = this.models[k];
      const { leaf, fruit } = this.crops[k];
      const s = Math.min(1, st.g / def.days);
      const ripe = st.g >= def.days && !st.dead;
      const sc = 0.22 + 0.78 * s;
      q.setFromAxisAngle(up, (i * 1.7) % 6.28);
      P.set(t.x, FARM_Y + 0.06, t.z);
      m.compose(P, q, S.set(sc, st.dead ? sc * 0.45 : sc, sc));
      const li = counts[k].l++;
      leaf.setMatrixAt(li, m);
      c.set(st.dead ? '#7a5a2a' : ripe ? md.ripeLeaf : '#5aa83f');
      if (!st.dead && !st.w && !t.gh) c.lerp(new THREE.Color('#a0a040'), 0.25);
      leaf.setColorAt(li, c);
      if (!st.dead && s >= 0.6) {
        const fs = ripe ? sc : sc * 0.6;
        m.compose(P, q, S.set(fs, fs, fs));
        const fi = counts[k].f++;
        fruit.setMatrixAt(fi, m);
        fruit.setColorAt(fi, c.set(ripe ? (md.fruitTex ? '#ffffff' : md.fruitColor) : '#7ab85a'));
      }
    });
    this.soil.count = ns;
    for (const k in this.crops) {
      this.crops[k].leaf.count = counts[k].l;
      this.crops[k].fruit.count = counts[k].f;
    }
    this.flush();
  }
  flush() {
    this.soil.instanceMatrix.needsUpdate = true;
    this.soil.instanceColor.needsUpdate = true;
    for (const k in this.crops) {
      const { leaf, fruit } = this.crops[k];
      leaf.instanceMatrix.needsUpdate = true; fruit.instanceMatrix.needsUpdate = true;
      leaf.instanceColor.needsUpdate = true; fruit.instanceColor.needsUpdate = true;
    }
  }
}
