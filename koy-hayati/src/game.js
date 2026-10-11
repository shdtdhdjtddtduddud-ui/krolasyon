// Oyun durumu ve kuralları (ekonomi, mevsim, gün döngüsü, işler, kayıt)
import { ITEMS, CROPS, SPECIES, DAYS_PER_SEASON, UPGRADES, RECIPES } from './data.js';
import { SEASONS } from './sky.js';
import { mulberry32 } from './util.js';

const SAVE_KEY = 'koyHayati.save.v1';
const TILE_COUNT = 20 * 12 + 15;

function rollWeather(season, r) {
  const list = SEASONS[season].weather;
  const tot = list.reduce((a, b) => a + b[1], 0);
  let x = r() * tot;
  for (const [k, w] of list) { x -= w; if (x <= 0) return k; }
  return list[0][0];
}

export class Game {
  constructor() {
    this.listeners = {};
    this.s = this.fresh();
  }
  fresh() {
    return {
      v: 1,
      money: 600,
      day: 0,
      minutes: 6 * 60 + 30,
      energy: 100,
      inv: { havuc_t: 12, patates_t: 6, yem: 10, saman: 4 },
      tool: 1,
      seed: 'havuc_t',
      water: 20,
      tiles: Array.from({ length: TILE_COUNT }, () => ({ t: 0, w: 0, c: null, g: 0, d: 0, dead: false, f: 0 })),
      animals: [
        { id: 1, kind: 'tavuk', name: 'Pamuk', age: 10, happy: 60, fed: 0, ready: 1, last: -9 },
        { id: 2, kind: 'tavuk', name: 'Benekli', age: 10, happy: 60, fed: 0, ready: 1, last: -9 },
      ],
      nextAnimalId: 3,
      upgrades: {},
      pots: [],
      trees: {},
      forage: {},
      fruit: {},
      weather: 'clear',
      tomorrow: 'clear',
      jobs: [],
      shiftsDone: {},
      discovered: { farm: 1 },
      stats: { harvested: 0, fish: 0, wood: 0, earned: 0 },
      priceSeed: 1,
      seenSeason: -1,
      tutorial: 0,
      lastSleepDay: -1,
      boatPos: null,
    };
  }
  on(ev, fn) { (this.listeners[ev] ||= []).push(fn); }
  emit(ev, ...a) { for (const f of this.listeners[ev] || []) f(...a); }

  // --- zaman ---
  get season() { return Math.floor(this.s.day / DAYS_PER_SEASON) % 4; }
  get seasonDay() { return this.s.day % DAYS_PER_SEASON; }
  get year() { return Math.floor(this.s.day / (DAYS_PER_SEASON * 4)) + 1; }
  get hour() { return this.s.minutes / 60; }
  get maxEnergy() { return this.s.upgrades.canta ? 130 : 100; }
  timeString() {
    const m = Math.floor(this.s.minutes) % (24 * 60);
    return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;
  }
  dateString() {
    return `${SEASONS[this.season].icon} ${SEASONS[this.season].name}, ${this.seasonDay + 1}. gün · Yıl ${this.year}`;
  }

  // --- envanter ---
  count(id) { return this.s.inv[id] || 0; }
  add(id, n = 1) {
    this.s.inv[id] = (this.s.inv[id] || 0) + n;
    if (this.s.inv[id] <= 0) delete this.s.inv[id];
    this.emit('inv');
  }
  take(id, n = 1) {
    if (this.count(id) < n) return false;
    this.add(id, -n);
    return true;
  }
  spend(n) {
    if (this.s.money < n) return false;
    this.s.money -= n; this.emit('money'); return true;
  }
  earn(n) { this.s.money += n; this.s.stats.earned += n; this.emit('money'); }
  useEnergy(n) {
    this.s.energy = Math.max(0, this.s.energy - n);
    this.emit('energy');
    return this.s.energy > 0;
  }
  gainEnergy(n) { this.s.energy = Math.min(this.maxEnergy, this.s.energy + n); this.emit('energy'); }

  // --- fiyatlar ---
  priceMult(id) {
    const it = ITEMS[id];
    if (!it) return 1;
    const se = this.season;
    let m = 1;
    if (it.cat === 'ürün') {
      const c = CROPS[id];
      m = c.seasons.includes(se) ? 0.92 : 1.4;
    }
    if (id === 'odun') m = [1, 0.9, 1.3, 1.8][se];
    if (it.cat === 'balık') m = se === 3 ? 1.4 : 1;
    if (id === 'sut' || id === 'keci_sutu' || id === 'yumurta') m = se === 3 ? 1.3 : 1;
    if (it.cat === 'meyve') m = (id === 'elma' && se === 2) || (id === 'seftali' && se === 1) ? 0.95 : 1.5;
    if (id === 'mantar') m = se === 2 ? 1 : 1.5;
    if (id === 'saman') m = se === 3 ? 1.5 : 1;
    // günlük dalgalanma
    const r = mulberry32(this.s.day * 131 + id.length * 7 + id.charCodeAt(0));
    m *= 0.9 + r() * 0.2;
    return m;
  }
  sellPrice(id, where = 'pazar') {
    const base = ITEMS[id]?.price || 0;
    let p = base * this.priceMult(id);
    if (where === 'farm') p *= 0.75;
    return Math.max(1, Math.round(p));
  }
  buyPrice(id) {
    const it = ITEMS[id];
    if (!it) return 0;
    let p = it.price;
    if (id === 'saman' || id === 'yem') p *= this.season === 3 ? 1.4 : 1;
    return Math.round(p);
  }
  sell(id, n, where) {
    n = Math.min(n, this.count(id));
    if (n <= 0) return 0;
    const p = this.sellPrice(id, where) * n;
    this.take(id, n);
    this.earn(p);
    return p;
  }

  // --- tarla ---
  canPlantHere(tileIdx, isGh) {
    const se = this.season;
    if (isGh) return true;
    return se !== 3;
  }
  // --- hayvanlar ---
  animalProductAvailable(a) {
    const sp = SPECIES[a.kind];
    if (!sp.product) return false;
    if (a.age < 5) return false;
    if (sp.seasons && !sp.seasons.includes(this.season)) return false;
    return a.ready;
  }

  // --- yeni gün ---
  newDay(slept) {
    const s = this.s;
    const prevSeason = this.season;
    s.day++;
    s.minutes = 6 * 60;
    const r = mulberry32(s.day * 977 + 13);
    const se = this.season;
    const report = [];
    const seasonChanged = se !== prevSeason;
    s.weather = s.tomorrow || rollWeather(se, r);
    if (seasonChanged) s.weather = rollWeather(se, r);
    s.tomorrow = rollWeather(se, mulberry32(s.day * 31 + 7));
    const rain = s.weather === 'rain' || s.weather === 'storm';

    // ekinler
    let died = 0, grew = 0;
    s.tiles.forEach((t, i) => {
      const gh = i >= 240;
      if (gh && !s.upgrades.sera) return;
      if (t.c && !t.dead) {
        const def = CROPS[t.c];
        if (seasonChanged && !gh && !def.seasons.includes(se)) {
          t.dead = true; died++;
        } else if (t.w || gh && t.w) {
          let rate = 1;
          if (prevSeason === 0 && !gh) rate = 1.25;
          if (t.f) rate += 0.5;
          if (t.g < def.days) { t.g = Math.min(def.days, t.g + rate); grew++; }
          t.d = 0;
        } else if (t.g < def.days) {
          t.d++;
          const limit = prevSeason === 1 && !gh ? 2 : 3;
          if (t.d >= limit) { t.dead = true; died++; }
        }
      }
      t.w = 0;
      if (rain && !gh && se !== 3) t.w = 1;
      if (se === 3 && !gh && t.c && !t.dead) { t.dead = true; died++; }
    });
    if (died) report.push(`🥀 ${died} ekin kurudu/dondu.`);
    if (rain && se !== 3) report.push('🌧️ Yağmur tarlanı suladı.');

    // hayvanlar
    const hasRooster = s.animals.some((a) => a.kind === 'horoz');
    const hasBull = s.animals.some((a) => a.kind === 'boga');
    let hungry = 0;
    const babies = [];
    for (const a of s.animals) {
      const sp = SPECIES[a.kind];
      a.age++;
      let fed = false;
      if (se === 3 || s.weather === 'storm' || s.weather === 'blizzard') {
        if (this.take(sp.winterFeed, 1)) fed = true;
        else if (sp.winterFeed === 'saman' && this.take('yem', 2)) fed = true;
        else if (sp.winterFeed === 'yem' && this.take('saman', 1)) fed = true;
      } else fed = true;
      if (!fed) { hungry++; a.happy = Math.max(0, a.happy - 20); }
      else a.happy = Math.min(100, a.happy + (a.petted ? 6 : -2));
      a.petted = 0;
      a.fed = fed ? 1 : 0;
      if (sp.product && fed && a.age >= 5) {
        const every = sp.every || 1;
        if (s.day - (a.last ?? -9) >= every) {
          let chance = 0.6 + a.happy / 250;
          if (a.kind === 'tavuk' && hasRooster) chance += 0.15;
          if (sp.seasons && !sp.seasons.includes(se)) chance = 0;
          if (a.kind === 'domuz') chance *= 0.55;
          a.ready = r() < chance ? 1 : 0;
        }
      }
      if (a.kind === 'koyun' && a.sheared && s.day - a.last >= 3) a.sheared = 0;
    }
    // ilkbahar yavruları
    if (se === 0) {
      const bySp = {};
      for (const a of s.animals) if (a.age >= 5 && a.happy >= 55) (bySp[a.kind] ||= []).push(a);
      for (const k in bySp) {
        if (k === 'horoz' || k === 'boga' || k === 'at') continue;
        const n = bySp[k].length + (k === 'inek' && hasBull ? 1 : 0) + (k === 'tavuk' && hasRooster ? 1 : 0);
        const total = s.animals.filter((a) => a.kind === k).length;
        if (n >= 2 && total < 8 && r() < 0.18 + (hasBull && k === 'inek' ? 0.15 : 0)) babies.push(k);
      }
      for (const k of babies) {
        s.animals.push({ id: s.nextAnimalId++, kind: k, name: 'Yavru ' + SPECIES[k].name, age: 0, happy: 70, fed: 1, ready: 0, last: s.day });
        report.push(`🍼 Bir ${SPECIES[k].name.toLowerCase()} yavrusu doğdu!`);
      }
    }
    if (hungry) report.push(`⚠️ ${hungry} hayvan aç kaldı (kışın saman/yem gerekir).`);

    // meyve ağaçları
    for (const k in s.fruit) if (s.day >= s.fruit[k]) delete s.fruit[k];
    // doğa ürünleri yenilenir (her gün bir kısmı)
    for (const k in s.forage) if (s.day >= s.forage[k]) delete s.forage[k];
    // ağaçlar yeniden büyür
    for (const k in s.trees) if (s.day >= s.trees[k]) delete s.trees[k];

    // enerji
    if (slept) {
      const late = s.lastSleepLate;
      s.energy = late ? Math.round(this.maxEnergy * 0.7) : this.maxEnergy;
    }
    s.lastSleepLate = 0;
    s.water = s.upgrades.sulama2 ? 40 : 20;
    s.shiftsDone = {};
    this.genJobs(r);
    this.emit('day', { seasonChanged, report });
    this.save();
    return { seasonChanged, report };
  }

  // --- işler ---
  genJobs(r) {
    const se = this.season;
    const pool = [];
    for (const [k, c] of Object.entries(CROPS)) if (c.seasons.includes(se) || this.s.upgrades.sera) pool.push(k);
    pool.push('odun', 'odun', 'sazan', 'levrek');
    if (this.s.animals.some((a) => a.kind === 'tavuk')) pool.push('yumurta');
    if (this.s.animals.some((a) => a.kind === 'inek')) pool.push('sut', 'peynir');
    if (this.s.animals.some((a) => a.kind === 'koyun')) pool.push('yun');
    if (se === 2) pool.push('mantar', 'elma');
    if (se === 1) pool.push('bogurtlen', 'seftali');
    if (se === 0) pool.push('kekik');
    if (se === 3) pool.push('buz_baligi', 'ekmek');
    const who = ['Lokanta', 'Fırıncı Hasan', 'Muhtarlık', 'Bakkal Ayşe', 'Okul kantini', 'Han sahibi', 'Marangoz Rıza', 'Pazarcı Emine'];
    const jobs = [];
    for (let i = 0; i < 3; i++) {
      const item = pool[Math.floor(r() * pool.length)];
      const it = ITEMS[item];
      const qty = Math.max(1, Math.round((it.price > 100 ? 1 + r() * 2 : it.price > 40 ? 2 + r() * 4 : 4 + r() * 8)));
      const reward = Math.round(it.price * qty * (1.6 + r() * 0.6) / 5) * 5;
      jobs.push({ id: this.s.day * 10 + i, who: who[Math.floor(r() * who.length)], item, qty, reward, done: false });
    }
    this.s.jobs = jobs;
  }
  completeJob(id) {
    const j = this.s.jobs.find((x) => x.id === id);
    if (!j || j.done) return false;
    if (!this.take(j.item, j.qty)) return false;
    j.done = true;
    this.earn(j.reward);
    return true;
  }
  craft(rec) {
    for (const [k, n] of Object.entries(rec.need)) if (this.count(k) < n) return false;
    for (const [k, n] of Object.entries(rec.need)) this.take(k, n);
    this.add(rec.out, rec.n);
    return true;
  }

  // --- kayıt ---
  save() {
    try { localStorage.setItem(SAVE_KEY, JSON.stringify(this.s)); return true; } catch (e) { return false; }
  }
  hasSave() {
    try { return !!localStorage.getItem(SAVE_KEY); } catch (e) { return false; }
  }
  load() {
    try {
      const raw = localStorage.getItem(SAVE_KEY);
      if (!raw) return false;
      const d = JSON.parse(raw);
      const f = this.fresh();
      this.s = { ...f, ...d };
      if (!Array.isArray(this.s.tiles) || this.s.tiles.length !== TILE_COUNT) this.s.tiles = f.tiles;
      return true;
    } catch (e) { return false; }
  }
  reset() {
    try { localStorage.removeItem(SAVE_KEY); } catch (e) { /* yok say */ }
    this.s = this.fresh();
  }
}
