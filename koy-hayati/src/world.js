// Dünyayı kurar: binaları, eşyaları, doğayı ve araçları yerleştirir
import * as THREE from 'three';
import * as B from './buildings.js';
import * as V from './vehicles.js';
import { heightAt, distToRoad } from './terrain.js';
import { Trees, Grass, Flowers, rocks, Forage, fruitTree } from './nature.js';
import { Fields, FIELD, GH } from './farming.js';
import { pottedPlant } from './plants.js';
import { Animal } from './animals.js';
import { FARM_Y, TOWN_Y, LAKE, ISLAND, PEAK, FOREST, POOL, WATERFALL_X, PLACES } from './layout.js';
import { mulberry32, clamp, angleDiff } from './util.js';
import { mat } from './kit.js';

export const AREAS = {
  pasture: { x0: -61, x1: -23, z0: -7, z1: 27 },
  coop: { x0: 23, x1: 43, z0: -39, z1: -19 },
  pigpen: { x0: 49, x1: 59, z0: -39, z1: -27 },
  market: { x0: 31, x1: 45, z0: 205, z1: 219 },
};

export class World {
  constructor(scene, colliders, game, isMobile = false) {
    this.isMobile = isMobile;
    this.scene = scene;
    this.colliders = colliders;
    this.game = game;
    this.updatables = [];
    this.interacts = [];
    this.platforms = [];
    this.npcs = [];
    this.build();
  }

  place(b, x, z, rot = 0, y = null) {
    const gy = y ?? heightAt(x, z);
    b.group.position.set(x, gy, z);
    b.group.rotation.y = rot;
    this.scene.add(b.group);
    const c = Math.cos(rot), s = Math.sin(rot);
    const toW = (lx, lz) => [x + lx * c + lz * s, z - lx * s + lz * c];
    b.toWorld = toW;
    b.colliderRefs = [];
    for (const col of b.colliders || []) {
      let ref;
      if (b.worldSpace) ref = this.colliders.addBox(col[0], col[1], col[2], col[3]);
      else {
        const pts = [toW(col[0], col[2]), toW(col[1], col[2]), toW(col[0], col[3]), toW(col[1], col[3])];
        const xs = pts.map((p) => p[0]), zs = pts.map((p) => p[1]);
        ref = this.colliders.addBox(Math.min(...xs), Math.max(...xs), Math.min(...zs), Math.max(...zs));
      }
      b.colliderRefs.push(ref);
    }
    for (const it of b.interacts || []) {
      const [wx, wz] = b.worldSpace ? [it.x, it.z] : toW(it.x, it.z);
      const w = { ...it, x: wx, z: wz, y: gy + (it.y || 1), owner: b };
      this.interacts.push(w);
    }
    if (b.update) {
      const self = this;
      this.updatables.push((dt, ctx) => {
        ctx.localPlayerDist = (lx, lz) => { const [wx, wz] = toW(lx, lz); return Math.hypot(wx - ctx.player.x, wz - ctx.player.z); };
        b.update(dt, ctx);
      });
    }
    return b;
  }
  platformLocal(b, x0, x1, z0, z1, y) {
    const pts = [b.toWorld(x0, z0), b.toWorld(x1, z1)];
    this.platforms.push({
      minX: Math.min(pts[0][0], pts[1][0]), maxX: Math.max(pts[0][0], pts[1][0]),
      minZ: Math.min(pts[0][1], pts[1][1]), maxZ: Math.max(pts[0][1], pts[1][1]), y,
    });
  }
  addInteract(id, x, z, y, r, label, extra = {}) {
    const w = { id, x, z, y, r, label, ...extra };
    this.interacts.push(w);
    return w;
  }

  build() {
    const g = this.game;
    // ------------- ÇİFTLİK
    const house = this.place(B.farmhouse(), 0, -22, 0, FARM_Y);
    this.house = house;
    this.platformLocal(house, -5.9, 5.9, -4.4, 4.4, FARM_Y + 0.09);
    this.platformLocal(house, -6.2, 6.2, 4.5, 7.6, FARM_Y + 0.1);
    this.homePos = new THREE.Vector3(0, FARM_Y, -19);
    this.bedPos = new THREE.Vector3(-3.0, FARM_Y, -24.8);
    this.place(B.barn(), -32, -26, 0, FARM_Y);
    this.place(B.silo(), -44, -31, 0, FARM_Y);
    const hs = { group: B.hayStack(), colliders: [[-1.4, 1.6, -0.7, 0.7]], interacts: [] };
    this.place(hs, -23, -13, 0.0, FARM_Y);
    this.place(B.handCart(), -19, -17, 0, FARM_Y);
    this.place(B.fenceRect(-62, -22, -8, 28, [{ side: 'e', a: 0, b: 4 }]), 0, 0, 0, FARM_Y);
    this.place(B.shelter(), -50, -4, 0, FARM_Y);
    this.place(B.windPump(), -14, -40, 0, FARM_Y);
    this.place(B.waterTower(), 16, -40, 0, FARM_Y);
    this.place(B.coop(), 30, -33, 0, FARM_Y);
    this.place(B.fenceRect(22, 44, -40, -18, [{ side: 's', a: 31, b: 34 }], '#d8c8a8'), 0, 0, 0, FARM_Y);
    this.place(B.fenceRect(48, 60, -40, -26, [{ side: 's', a: 52, b: 55 }], '#a8835a'), 0, 0, 0, FARM_Y);
    const mud = new THREE.Mesh(new THREE.CircleGeometry(3, 16), mat('#4a3424', { roughness: 0.3 }));
    mud.rotation.x = -Math.PI / 2; mud.position.set(55, FARM_Y + 0.03, -33); mud.receiveShadow = true;
    this.scene.add(mud);
    this.place(B.shed(), 36, -4, 0, FARM_Y);
    this.platformLocal({ toWorld: (a, b) => [36 + a, -4 + b] }, -6, 6, -5, 5, FARM_Y + 0.08);
    this.place(B.well(), -12, -10, 0, FARM_Y);
    this.place(B.woodpile(), 10, -21, 0, FARM_Y);
    this.place(B.signA(), 7.5, 13, -Math.PI / 2, FARM_Y);
    this.place(B.fieldStall(), 51, 27, -Math.PI / 2, FARM_Y);
    this.place(B.scarecrow(), 47.6, 34, -Math.PI / 2, FARM_Y);
    this.place(B.wheelbarrow(), 3.2, 25, 0.6, FARM_Y);
    this.place(B.windmill(), -14, 66, Math.PI / 2);
    this.place(B.signpost([{ text: 'Kasaba ↓', angle: -Math.PI / 2 }, { text: 'Ayna Göl →', angle: 0 }, { text: 'Şelale ↑', angle: Math.PI / 2 }, { text: '← Orman', angle: Math.PI }]), 4.5, 16, 0);
    for (const [x, z] of [[-8, -12], [8, -12], [3, 18]]) this.place(B.lampPost(), x, z, 0, FARM_Y);
    // sera (satın alınınca görünür)
    this.greenhouse = this.place(B.greenhouse(), 54, 40, 0, FARM_Y);
    // meyve bahçesi
    this.fruitTrees = [];
    let fi = 0;
    for (const [z, kind] of [[42, 'elma'], [52, 'seftali']]) {
      for (const x of [-40, -32, -24, -16]) {
        const ft = fruitTree(kind, fi === 1 || fi === 6, fi + 3);
        ft.group.position.set(x, heightAt(x, z), z);
        ft.group.rotation.y = fi * 1.3;
        this.scene.add(ft.group);
        ft.id = 'f' + fi; ft.x = x; ft.z = z;
        this.colliders.addCircle(x, z, 0.4);
        this.fruitTrees.push(ft);
        fi++;
      }
    }
    // tarla
    this.fields = new Fields(this.scene);
    // ev saksıları
    this.potGroup = new THREE.Group();
    this.scene.add(this.potGroup);

    // ------------- KASABA
    const TY = TOWN_Y;
    const sq = B.stoneDisc(19);
    sq.position.set(0, TY + 0.04, 215);
    this.scene.add(sq);
    this.place(B.fountain(), 0, 215, 0, TY);
    const shops = [
      { id: 'tohumcu', sign: ['Tohumcu', '& Yemci'], x: -24, z: 205, rot: Math.PI / 2, seed: 11, aw: ['#f1e6cf', '#3a7a3a'] },
      { id: 'hayvan', sign: ['Hayvan', 'Pazarı'], x: 24, z: 200, rot: -Math.PI / 2, seed: 12, aw: ['#f1e6cf', '#8a5a2a'] },
      { id: 'nalbur', sign: ['Nalbur', '& Galeri'], x: 24, z: 228, rot: -Math.PI / 2, seed: 13, aw: ['#f1e6cf', '#2b3566'] },
      { id: 'cicekci', sign: ['Çiçekçi'], x: -24, z: 226, rot: Math.PI / 2, seed: 14, aw: ['#fde3ec', '#c2457a'] },
      { id: 'firin', sign: ['Fırın'], x: 0, z: 242, rot: Math.PI, seed: 15, aw: ['#f6e3b0', '#a0522d'] },
    ];
    const labels = {
      tohumcu: 'Tohumcu & Yemci (tohum, yem, saman, gübre)', hayvan: 'Hayvan Pazarı (al / sat)', nalbur: 'Nalbur & Galeri (aletler, araçlar, sera)',
      cicekci: 'Çiçekçi (saksı bitkileri)', firin: 'Fırın (vardiyada çalış, ekmek al)',
    };
    for (const sh of shops) {
      const h = this.place(B.townHouse(sh.seed, { sign: sh.sign, shop: true, w: 8, d: 6 }), sh.x, sh.z, sh.rot, TY);
      const [cx, cz] = h.toWorld(0, 4.6);
      const counter = B.shopCounter(sh.aw[0], sh.aw[1]);
      counter.position.set(cx, TY, cz); counter.rotation.y = sh.rot;
      this.scene.add(counter);
      const [c0x, c0z] = h.toWorld(-1.6, 4.2), [c1x, c1z] = h.toWorld(1.6, 5.0);
      this.colliders.addBox(Math.min(c0x, c1x), Math.max(c0x, c1x), Math.min(c0z, c1z), Math.max(c0z, c1z));
      const n = B.npc(sh.seed);
      const [nx, nz] = h.toWorld(0, 3.6);
      n.group.position.set(nx, TY, nz); n.group.rotation.y = sh.rot;
      this.scene.add(n.group);
      this.npcs.push({ ...n, x: nx, z: nz });
      const [ix, iz] = h.toWorld(0, 5.9);
      this.addInteract('shop', ix, iz, TY + 1.2, 2.4, labels[sh.id], { shop: sh.id });
    }
    // pazar arabaları
    const carts = [['melon', 'tomato', 'cabbage'], ['apple', 'carrot', 'pumpkin'], ['tomato', 'cabbage', 'carrot']];
    [-8, 0, 8].forEach((x, i) => this.place(B.marketCart(carts[i]), x, 230, Math.PI, TY));
    const pn = B.npc(21);
    pn.group.position.set(0, TY, 232.2); pn.group.rotation.y = Math.PI;
    this.scene.add(pn.group); this.npcs.push({ ...pn, x: 0, z: 232.2 });
    this.addInteract('shop', 0, 227.6, TY + 1.2, 3, 'Pazar (ürünlerini tam fiyata sat)', { shop: 'pazar' });
    this.place(B.jobBoard(), 9, 200, 0, TY);
    // hayvan pazarı ağılı
    this.place(B.fenceRect(30, 46, 204, 220, [{ side: 'w', a: 210, b: 213 }], '#c9b48a'), 0, 0, 0, TY);
    // süs evler
    const deco = [[-30, 183, Math.PI / 2], [30, 183, -Math.PI / 2], [-40, 242, Math.PI / 2], [40, 246, -Math.PI / 2], [-16, 256, Math.PI], [16, 258, Math.PI], [-42, 214, Math.PI / 2]];
    deco.forEach(([x, z, r], i) => this.place(B.townHouse(100 + i), x, z, r, heightAt(x, z)));
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * Math.PI * 2 + 0.3;
      this.place(B.lampPost(), Math.cos(a) * 16, 215 + Math.sin(a) * 16, 0, TY);
    }
    for (const [x, z, r] of [[-6, 210, Math.PI / 2], [6, 220, -Math.PI / 2]]) this.place(B.bench(), x, z, r, TY);
    this.place(B.signpost([{ text: 'Çiftlik ↑', angle: Math.PI / 2 }]), 3, 192, 0);
    // pazar çiçekçi saksıları
    ['palm', 'cactus', 'monstera', 'fern', 'caladium'].forEach((t, i) => {
      const p = pottedPlant(t, i);
      p.position.set(-19.5, TY, 222 + i * 0.9 - 0.5);
      this.scene.add(p);
    });

    // ------------- GÖL
    let shoreX = 90;
    for (let x = 90; x < 140; x += 0.25) { if (heightAt(x, 32) < 0.2) { shoreX = x; break; } }
    const dk = B.dock(16);
    const dockRoot = shoreX - 2.5;
    this.place(dk, dockRoot, 32, -Math.PI / 2, 0);
    this.platforms.push({ minX: dockRoot - 0.5, maxX: dockRoot + 16, minZ: 30.8, maxZ: 33.2, y: 0.68 });
    this.dockEnd = new THREE.Vector3(dockRoot + 16, 0, 32);
    this.place(B.bench(), dockRoot - 4, 36, -Math.PI / 2);
    this.place(B.signpost([{ text: 'Ayna Göl', angle: 0 }]), dockRoot - 3, 29, 0);
    const gz = B.gazebo();
    this.place(gz, ISLAND.x, ISLAND.z, 0);
    this.platforms.push({ minX: ISLAND.x - 2.6, maxX: ISLAND.x + 2.6, minZ: ISLAND.z - 2.6, maxZ: ISLAND.z + 2.6, y: heightAt(ISLAND.x, ISLAND.z) + 0.25 });

    // ------------- ŞELALE
    const wb = B.bench();
    this.place(wb, 47, -139, Math.atan2(60 - 47, -152 - -139) + 0);
    this.place(B.signpost([{ text: 'Gümüş Şelale', angle: Math.PI / 2 }]), 44, -136, 0);
    this.addInteract('fishspot', 56, -144, heightAt(56, -144) + 1, 3.5, 'Şelale havuzu (olta ile alabalık tut)');

    // ------------- ZİRVE
    const py = heightAt(PEAK.x, PEAK.z);
    this.place(B.flag(), PEAK.x + 1.5, PEAK.z - 1.5, 0);
    this.place(B.cairn(), PEAK.x - 2.5, PEAK.z - 1, 0);
    this.place(B.bench(), PEAK.x, PEAK.z + 3, 0);
    this.addInteract('peak', PEAK.x, PEAK.z, py + 1, 4, 'Kartal Tepesi (manzarayı seyret)');

    // ------------- ORMAN
    const cab = this.place(B.cabin(), FOREST.x, FOREST.z - 10, 0);
    this.place(B.woodpile(), FOREST.x + 5, FOREST.z - 7, 0.5);
    this.place(B.signpost([{ text: 'Çam Ormanı', angle: 0 }, { text: 'Zirve ↑', angle: Math.PI / 2 }]), -150, 30, 0);
    this.place(B.signpost([{ text: '→ Çiftlik', angle: 0 }]), -120, 22, 0);

    // ------------- DOĞA
    const excluded = (x, z) => {
      if (x > FIELD.x0 - 1 && x < FIELD.x0 + FIELD.cols * 2 + 1 && z > FIELD.z0 - 1 && z < FIELD.z0 + FIELD.rows * 2 + 1) return true;
      if (x > -8 && x < 8 && z > -28 && z < -12) return true;
      if (this.colliders.hits(x, z, 0.6)) return true;
      if (Math.hypot(x, z - 215) < 21) return true;
      return false;
    };
    this.trees = new Trees(this.scene, this.colliders, this.isMobile);
    this.grass = new Grass(this.scene, this.isMobile, excluded);
    this.flowers = new Flowers(this.scene, excluded);
    this.rocks = rocks(this.scene, this.colliders);
    this.forage = new Forage(this.scene, excluded);

    // ------------- ARAÇLAR
    const vs = g.s.veh || {};
    this.vehicles = [];
    this.addVehicle('tractor', V.tractor(), vs.tractor || { x: 28, z: 8, h: 0 }, () => true);
    this.addVehicle('truck', V.truck(), vs.truck || { x: 40, z: 8.5, h: 0 }, () => !!g.s.upgrades.kamyonet);
    this.addVehicle('boat', V.boat(), vs.boat || { x: dockRoot + 13, z: 35.5, h: Math.PI / 2 }, () => !!g.s.upgrades.kayik);
    this.addVehicle('horsecart', V.horseCart(), vs.horsecart || { x: -16, z: 10, h: Math.PI }, () => !!g.s.upgrades.atarabasi && g.s.animals.some((a) => a.kind === 'at'));

    // ------------- HAYVANLAR
    this.animals = [];
    this.rnd = mulberry32(99);
    this.syncAnimals();
    // hayvan pazarı vitrin hayvanları
    let did = -1;
    for (const k of ['inek', 'koyun', 'keci', 'at', 'domuz']) {
      const a = new Animal({ id: did--, kind: k, age: 10 }, AREAS.market, this.rnd);
      this.scene.add(a.group);
      this.displayAnimals = this.displayAnimals || [];
      this.displayAnimals.push(a);
    }
  }

  addVehicle(key, def, p, ownedFn) {
    const v = new Drivable(key, def, p.x, p.z, p.h);
    v.ownedFn = ownedFn;
    this.scene.add(def.group);
    this.vehicles.push(v);
    return v;
  }

  syncAnimals() {
    const have = new Map(this.animals.map((a) => [a.data.id, a]));
    const want = new Set(this.game.s.animals.map((a) => a.id));
    for (const a of this.animals) if (!want.has(a.data.id)) this.scene.remove(a.group);
    this.animals = this.animals.filter((a) => want.has(a.data.id));
    for (const d of this.game.s.animals) {
      if (have.has(d.id)) { have.get(d.id).data = d; have.get(d.id).applyScale(); continue; }
      const sp = d.kind;
      const areaKey = { tavuk: 'coop', horoz: 'coop', kaz: 'coop', tavsan: 'coop', domuz: 'pigpen' }[sp] || 'pasture';
      const area = AREAS[areaKey];
      if (d.x === undefined || d.x < area.x0 || d.x > area.x1 || d.z < area.z0 || d.z > area.z1) {
        d.x = area.x0 + 2 + this.rnd() * (area.x1 - area.x0 - 4);
        d.z = area.z0 + 2 + this.rnd() * (area.z1 - area.z0 - 4);
      }
      const a = new Animal(d, area, this.rnd);
      this.scene.add(a.group);
      this.animals.push(a);
    }
  }

  refreshPots() {
    while (this.potGroup.children.length) this.potGroup.remove(this.potGroup.children[0]);
    const slots = this.house.plantSlots;
    this.game.s.pots.forEach((id, i) => {
      if (i >= slots.length) return;
      const [lx, ly, lz] = slots[i];
      const [x, z] = this.house.toWorld(lx, lz);
      const p = pottedPlant(id, i + 1);
      p.position.set(x, FARM_Y + ly, z);
      this.potGroup.add(p);
    });
  }

  applySeason(season, seasonDay) {
    const s = this.game.s;
    const snow = season === 3 ? 1 : 0;
    // ağaçlar
    for (const t of this.trees.list) {
      const chopped = s.trees[t.type + t.idx];
      this.trees.setAlive(t, !chopped);
    }
    this.trees.applySeason(season, seasonDay);
    this.grass.applySeason(season, snow);
    this.flowers.applySeason(season);
    for (const it of this.forage.items) it.taken = !!s.forage[it.kind + Math.round(it.x) + '_' + Math.round(it.z)];
    this.forage.refresh(season);
    for (const ft of this.fruitTrees) {
      const has = ft.fruitSeason.includes(season) && !s.fruit[ft.id];
      ft.applySeason(season, has);
    }
    const gh = !!s.upgrades.sera;
    this.greenhouse.group.visible = gh;
    for (const c of this.greenhouse.colliderRefs) c.disabled = !gh;
    this.fields.ghOwned = gh;
    this.fields.refreshAll(s.tiles, season);
    for (const v of this.vehicles) v.def.group.visible = v.ownedFn();
    return snow;
  }

  update(dt, ctx) {
    for (const u of this.updatables) u(dt, ctx);
    for (const a of this.animals) a.update(dt, ctx);
    for (const a of this.displayAnimals) a.update(dt, ctx);
    for (const n of this.npcs) {
      const near = Math.hypot(n.x - ctx.player.x, n.z - ctx.player.z) < 5;
      n.update(dt, near);
    }
    for (const v of this.vehicles) if (!v.driven) v.idle(dt, ctx);
  }
}

// ----------------------------------------------------------------- sürülebilir araç
const _v = new THREE.Vector3();
export class Drivable {
  constructor(key, def, x, z, h) {
    this.key = key;
    this.def = def;
    this.group = def.group;
    this.group.rotation.order = 'YXZ';
    this.pos = new THREE.Vector3(x, 0, z);
    this.heading = h;
    this.speed = 0;
    this.steer = 0;
    this.driven = false;
    this.pitch = 0; this.roll = 0;
    this.place(0, {});
  }
  groundAt(x, z) {
    return heightAt(x, z);
  }
  place(dt, ctx) {
    const d = this.def;
    const s = Math.sin(this.heading), c = Math.cos(this.heading);
    if (d.water) {
      const t = ctx.time || 0;
      this.pos.y = 0.05 + Math.sin(t * 1.4) * 0.06;
      this.group.position.copy(this.pos);
      this.group.rotation.set(Math.sin(t * 1.1) * 0.03, this.heading, Math.sin(t * 0.9) * 0.04 - this.steer * this.speed * 0.01);
      return;
    }
    const L = 1.6, W = 0.9;
    const hf = this.groundAt(this.pos.x + s * L, this.pos.z + c * L);
    const hb = this.groundAt(this.pos.x - s * L, this.pos.z - c * L);
    const hr = this.groundAt(this.pos.x - c * W, this.pos.z + s * W);
    const hl = this.groundAt(this.pos.x + c * W, this.pos.z - s * W);
    const y = Math.max((hf + hb + hr + hl) / 4, this.groundAt(this.pos.x, this.pos.z));
    this.pos.y = y;
    const tp = -Math.atan2(hf - hb, 2 * L), tr = Math.atan2(hr - hl, 2 * W);
    const k = dt ? Math.min(1, dt * 8) : 1;
    this.pitch += (tp - this.pitch) * k; this.roll += (tr - this.roll) * k;
    this.group.position.copy(this.pos);
    this.group.rotation.set(this.pitch, this.heading, -this.roll);
  }
  idle(dt, ctx) {
    if (this.def.water) this.place(dt, ctx);
    this.def.animate(0, 0, ctx, 0);
  }
  drive(dt, input, colliders, ctx) {
    const d = this.def;
    const thr = input.y;
    if (thr > 0.05) this.speed += d.accel * thr * dt * (this.speed < 0 ? 2.5 : 1);
    else if (thr < -0.05) this.speed += d.accel * thr * dt * (this.speed > 0 ? 2.5 : 1);
    else this.speed -= Math.sign(this.speed) * Math.min(Math.abs(this.speed), dt * (d.water ? 1 : 3));
    const snowSlow = ctx.season === 3 && !d.water ? 0.8 : 1;
    this.speed = clamp(this.speed, -d.reverse, d.maxSpeed * snowSlow);
    this.steer += (input.x - this.steer) * Math.min(1, dt * 5);
    const sf = clamp(Math.abs(this.speed) / 3, 0, 1) * Math.sign(this.speed || 1);
    this.heading -= this.steer * d.turn * dt * sf * (d.water ? 1 : 1.4);
    const dist = this.speed * dt;
    const nx = this.pos.x + Math.sin(this.heading) * dist;
    const nz = this.pos.z + Math.cos(this.heading) * dist;
    let blocked = false;
    const hgt = heightAt(nx, nz);
    if (d.water) { if (hgt > -0.7) blocked = true; }
    else {
      if (hgt < -0.5 && !(ctx.season === 3)) blocked = true;
      if (heightAt(nx, nz) - heightAt(this.pos.x, this.pos.z) > Math.abs(dist) * 1.6 + 0.05) blocked = true;
    }
    if (!blocked && colliders.hits(nx, nz, d.radius)) blocked = true;
    if (!blocked && ctx.dynamicBlock && ctx.dynamicBlock(nx, nz, d.radius, this)) blocked = true;
    if (blocked) { this.speed *= -0.25; }
    else { this.pos.x = nx; this.pos.z = nz; }
    this.place(dt, ctx);
    d.animate(blocked ? 0 : dist, this.steer, ctx, this.speed);
    return { dist: blocked ? 0 : dist, blocked };
  }
  seatWorld(out) {
    this.group.updateMatrixWorld();
    return out.copy(this.def.seat).applyMatrix4(this.group.matrixWorld);
  }
}
