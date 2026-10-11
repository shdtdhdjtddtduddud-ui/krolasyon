// Ana dosya: her şeyi birbirine bağlar ve oyun döngüsünü çalıştırır
import * as THREE from 'three';
import { buildHeights, buildTerrainMesh, colorizeTerrain, buildWater, waterUniformsList, fallUniforms, heightAt, distToRiver } from './terrain.js';
import { Sky, SEASONS, WEATHER } from './sky.js';
import { Colliders } from './physics.js';
import { World, AREAS } from './world.js';
import { Player } from './player.js';
import { Input } from './input.js';
import { UI } from './ui.js';
import { Audio } from './audio.js';
import { Game } from './game.js';
import { windUniforms } from './nature.js';
import { windowGlowMats } from './buildings.js';
import { ITEMS, CROPS, SPECIES, TOOLS, MINUTES_PER_REAL_SECOND, SEASON_INFO } from './data.js';
import { FARM_Y, PLACES, WATERFALL_X, POOL, LAKE } from './layout.js';
import { clamp, smoothstep } from './util.js';

const isMobile = (window.matchMedia && matchMedia('(pointer: coarse)').matches) || ('ontouchstart' in window && navigator.maxTouchPoints > 0);
if (isMobile) document.body.classList.add('mobile');
const $ = (id) => document.getElementById(id);

const canvas = $('c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: !isMobile, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, isMobile ? 1.15 : 1.75));
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.0;

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.05, 1500);
scene.add(camera);
window.addEventListener('resize', () => {
  renderer.setSize(window.innerWidth, window.innerHeight);
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
});

const game = new Game();
const audio = new Audio();
let world, player, input, ui, sky, water, colliders;
let running = false, paused = false, busy = false;
let clockTime = 0;
let seasonApplied = -1, seasonDayApplied = -1;
let fishing = { state: 'idle' };
const falling = [];
let hudTimer = 0, saveTimer = 0;
let lastTarget = null;
let warnedLate = false;

// ---------------------------------------------------------------- kurulum
function setup() {
  buildHeights();
  const terrain = buildTerrainMesh();
  scene.add(terrain);
  water = buildWater(scene);
  sky = new Sky(scene, renderer, isMobile);
  sky.onThunder = () => audio.thunder();
  colliders = new Colliders(8);
  world = new World(scene, colliders, game, isMobile);
  player = new Player(camera, scene);
  input = new Input(canvas, isMobile);
  input.onLockChange = (locked) => {
    if (!running || isMobile) return;
    if (!locked && !ui.panelOpen && !busy) { paused = true; $('pauseNote').classList.remove('hidden'); }
    if (locked) { paused = false; $('pauseNote').classList.add('hidden'); }
  };
  ui = new UI(game, audio, {
    onPanel: (open) => {
      if (open) input.unlock();
      else if (!isMobile && running) { paused = true; $('pauseNote').classList.remove('hidden'); }
    },
    onChange: () => ui.updateHotbar(),
    onAnimals: () => world.syncAnimals(),
    onPots: () => world.refreshPots(),
    onUpgrade: (k) => { world.applySeason(game.season, game.seasonDay); if (k === 'canta') game.gainEnergy(30); },
    onShift: (id, hours, pay, energy, extra) => doShift(id, hours, pay, energy, extra),
    onEat: (id) => eat(id),
    onTool: (i) => selectTool(i),
    onSave: () => saveAll(),
  });
  ui.buildHotbar((i) => selectTool(i));
  applySeasonNow(true);
  // ilk kare için ısınma çizimi
  sky.update(0, 0, 8, 0, 'clear', new THREE.Vector3(0, 2, 0));
  camera.position.set(0, 12, 40);
  camera.lookAt(0, 4, -20);
}

function selectTool(i) {
  game.s.tool = i;
  player.setTool(i);
  ui.updateHotbar();
  if (fishing.state !== 'idle' && i !== 5) cancelFishing();
}

function saveAll() {
  const s = game.s;
  s.player = { x: player.pos.x, y: player.pos.y, z: player.pos.z, yaw: player.yaw };
  s.veh = {};
  for (const v of world.vehicles) s.veh[v.key] = { x: v.pos.x, z: v.pos.z, h: v.heading };
  return game.save();
}

function startGame(isNew) {
  if (isNew) {
    game.reset();
    game.genJobs(Math.random);
  } else if (!game.load()) {
    game.genJobs(Math.random);
  }
  if (!game.s.jobs.length) game.genJobs(Math.random);
  $('start').classList.add('hidden');
  $('hud').classList.remove('hidden');
  if (isMobile) $('touch').classList.remove('hidden');
  // araç konumları
  const vs = game.s.veh || {};
  for (const v of world.vehicles) {
    if (vs[v.key]) { v.pos.set(vs[v.key].x, 0, vs[v.key].z); v.heading = vs[v.key].h; v.place(0, { time: 0 }); }
  }
  world.syncAnimals();
  world.refreshPots();
  applySeasonNow(true);
  const p = game.s.player;
  if (p && !isNew) { player.pos.set(p.x, p.y, p.z); player.yaw = p.yaw; }
  else { player.pos.copy(world.homePos); player.yaw = Math.PI; }
  player.setTool(game.s.tool);
  ui.updateHotbar();
  audio.start();
  running = true;
  paused = false;
  if (!isMobile) {
    try { const r = canvas.requestPointerLock && canvas.requestPointerLock(); if (r && r.catch) r.catch(() => {}); } catch (e) { /* yok say */ }
  }
  if (isNew) {
    setTimeout(() => {
      ui.openSeasonIntro(0, 'Hoş geldin! Dedenden kalan bu çiftlik artık senin. Elinde biraz para, birkaç tohum ve iki tavuk var. Tarlayı sür, ek, hayvan al, kasabada çalış, dağları ve şelaleyi gez. Her mevsimin kendine göre avantajı ve zorluğu var:');
    }, 400);
    setTimeout(() => ui.toast('İpucu: 2 = Çapa ile tarlayı sür, 4 = tohum ek, 3 = sula.'), 1500);
    setTimeout(() => ui.toast('İpucu: Kuyudan (evin solunda) sulama kabını doldur.'), 6000);
  }
}

function applySeasonNow(force = false) {
  const se = game.season, sd = game.seasonDay;
  if (!force && se === seasonApplied && sd === seasonDayApplied) return;
  const snow = world.applySeason(se, sd);
  colorizeTerrain(se, snow);
  seasonApplied = se; seasonDayApplied = sd;
}

// ---------------------------------------------------------------- yardımcılar
function forwardFlat() {
  const f = player.forward(new THREE.Vector3());
  f.y = 0; return f.normalize();
}
function facing(x, z, minDot = 0.35) {
  const dx = x - player.pos.x, dz = z - player.pos.z;
  const d = Math.hypot(dx, dz);
  if (d < 1.0) return true;
  const f = forwardFlat();
  return (dx * f.x + dz * f.z) / d > minDot;
}
function eat(id) {
  const it = ITEMS[id];
  if (!it || !it.food || !game.take(id, 1)) return;
  game.gainEnergy(it.food);
  audio.pop();
  ui.toast(`${it.icon} ${it.name} yedin: ⚡+${it.food}`);
}
function eatBest() {
  const foods = Object.keys(game.s.inv).filter((k) => ITEMS[k]?.food && game.count(k) > 0);
  if (!foods.length) { ui.toast('Yiyecek bir şeyin yok.', 'bad'); return; }
  foods.sort((a, b) => ITEMS[a].price / ITEMS[a].food - ITEMS[b].price / ITEMS[b].food);
  eat(foods[0]);
}
function useEnergy(n) {
  if (game.s.energy <= 0) return false;
  game.useEnergy(n);
  if (game.s.energy <= 0) setTimeout(() => passOut('Yorgunluktan bayıldın!'), 200);
  return true;
}

async function fadeTo(text, ms = 700) {
  ui.fade(true, text);
  await new Promise((r) => setTimeout(r, ms));
}
async function sleep(late = false) {
  if (busy) return;
  busy = true;
  cancelFishing();
  if (player.vehicle) player.exitVehicle(colliders);
  await fadeTo('💤 İyi geceler…', 900);
  game.s.lastSleepLate = game.s.minutes >= 24 * 60 || late ? 1 : 0;
  const prevSeason = game.season;
  const res = game.newDay(true);
  player.pos.copy(world.bedPos); player.yaw = Math.PI * 0.5;
  world.syncAnimals();
  applySeasonNow(true);
  saveAll();
  ui.updateHotbar();
  await new Promise((r) => setTimeout(r, 600));
  ui.fade(false);
  busy = false;
  warnedLate = false;
  if (res.seasonChanged) ui.openSeasonIntro(game.season, res.report.join(' '));
  else ui.openMorning(res.report, game.s.weather, game.s.tomorrow);
}
async function passOut(msg) {
  if (busy) return;
  ui.toast(msg, 'bad');
  const fee = Math.min(200, Math.round(game.s.money * 0.1));
  game.s.money -= fee;
  await sleep(true);
  ui.toast(`Köy doktoru seni evine getirdi. Masraf: ${fee} ₺`, 'bad');
}
async function doShift(id, hours, pay, energy, extra) {
  if (busy) return;
  busy = true;
  await fadeTo(`⏳ ${hours} saat çalışıyorsun…`, 1300);
  game.s.minutes += hours * 60;
  game.useEnergy(energy);
  game.earn(pay);
  game.s.shiftsDone[id] = 1;
  if (extra) extra();
  audio.coin();
  ui.fade(false);
  busy = false;
  ui.toast(`Vardiya bitti: +${pay} ₺`, 'gold');
}
async function rest(minutes, energyGain, text) {
  if (busy) return;
  busy = true;
  await fadeTo(text, 900);
  game.s.minutes += minutes;
  game.gainEnergy(energyGain);
  ui.fade(false);
  busy = false;
}

// ---------------------------------------------------------------- hedefleme
const _ray = new THREE.Raycaster();
function targetTile() {
  if (player.vehicle) return -1;
  const origin = camera.position.clone();
  const dir = player.forward(new THREE.Vector3());
  const ghOwned = !!game.s.upgrades.sera;
  if (dir.y < -0.05) {
    const t = (FARM_Y + 0.05 - origin.y) / dir.y;
    if (t > 0 && t < 5.5) {
      const x = origin.x + dir.x * t, z = origin.z + dir.z * t;
      const i = world.fields.tileAt(x, z, ghOwned);
      if (i >= 0) return i;
    }
  }
  const f = forwardFlat();
  const i = world.fields.tileAt(player.pos.x + f.x * 1.3, player.pos.z + f.z * 1.3, ghOwned);
  return i;
}

function findInteract() {
  const px = player.pos.x, pz = player.pos.z;
  if (player.vehicle) {
    return { label: `${player.vehicle.def.name}: in`, fn: () => exitVehicle() };
  }
  let best = null, bd = Infinity;
  const consider = (d, t) => { if (d < bd) { bd = d; best = t; } };
  for (const v of world.vehicles) {
    if (!v.def.group.visible) continue;
    const d = Math.hypot(v.pos.x - px, v.pos.z - pz);
    if (d < v.def.radius + 1.8 && facing(v.pos.x, v.pos.z, 0.2)) consider(d - 1, { label: `${v.def.name}'a bin`, fn: () => enterVehicle(v) });
  }
  for (const it of world.interacts) {
    const d = Math.hypot(it.x - px, it.z - pz);
    if (d < it.r && Math.abs(it.y - (player.pos.y + 1)) < 3 && facing(it.x, it.z, 0.25)) consider(d, { label: it.label, fn: () => doInteract(it) });
  }
  for (const a of world.animals) {
    const d = Math.hypot(a.pos.x - px, a.pos.z - pz);
    if (d < a.radius + 1.6 && facing(a.pos.x, a.pos.z, 0.5)) consider(d - 0.3, animalAction(a));
  }
  for (const ft of world.fruitTrees) {
    const d = Math.hypot(ft.x - px, ft.z - pz);
    if (d < 2.8 && facing(ft.x, ft.z, 0.3)) {
      const has = ft.fruitSeason.includes(game.season) && !game.s.fruit[ft.id];
      if (has) consider(d, { label: `${ft.kind === 'elma' ? 'Elma' : 'Şeftali'} topla`, fn: () => pickFruit(ft) });
    }
  }
  const fo = world.forage.nearest(px, pz, game.season, 2.4);
  if (fo && facing(fo.x, fo.z, 0.3)) {
    const lbl = { bush: game.season === 0 ? 'Çalı' : 'Böğürtlen topla', mushroom: 'Mantar topla', herb: 'Kekik topla' }[fo.kind];
    consider(Math.hypot(fo.x - px, fo.z - pz), { label: lbl, fn: () => takeForage(fo) });
  }
  return best;
}

function animalAction(a) {
  const d = a.data, sp = SPECIES[d.kind];
  if (d.kind === 'at') return { label: `${d.name}: ata bin`, fn: () => rideHorse(a) };
  if (game.animalProductAvailable(d)) {
    if (d.kind === 'koyun' && d.sheared) return { label: `${d.name}: sev`, fn: () => pet(a) };
    return { label: `${d.name}: ${sp.verb}`, fn: () => collectProduct(a) };
  }
  return { label: `${d.name} (${sp.name}): sev`, fn: () => pet(a) };
}
function pet(a) {
  const d = a.data;
  if (!d.petted) { d.happy = Math.min(100, d.happy + 5); d.petted = 1; ui.toast(`${SPECIES[d.kind].icon} ${d.name} mutlu oldu ❤`); }
  else ui.toast(`${d.name} seni seviyor.`);
  a.happyFx = 1;
  audio.animal(d.kind);
}
function collectProduct(a) {
  const d = a.data, sp = SPECIES[d.kind];
  let n = 1;
  if (d.happy > 85 && Math.random() < 0.3) n = 2;
  game.add(sp.product, n);
  d.ready = 0; d.last = game.s.day;
  if (d.kind === 'koyun') { d.sheared = 1; a.applyScale(); }
  if (!d.petted) { d.happy = Math.min(100, d.happy + 3); d.petted = 1; }
  a.happyFx = 1;
  audio.animal(d.kind); audio.pop();
  useEnergy(0.5);
  ui.toast(`${ITEMS[sp.product].icon} +${n} ${ITEMS[sp.product].name}`);
}
function pickFruit(ft) {
  const n = 3 + Math.floor(Math.random() * 3) + (game.season === 2 ? 1 : 0);
  const id = ft.kind;
  game.add(id, n);
  game.s.fruit[ft.id] = game.s.day + 3;
  ft.applySeason(game.season, false);
  audio.pop(); useEnergy(1);
  ui.toast(`${ITEMS[id].icon} +${n} ${ITEMS[id].name}`);
}
function takeForage(fo) {
  const se = game.season;
  let id, n;
  if (fo.kind === 'bush') {
    if (se === 0) { ui.toast('Çalı çiçek açmış, böğürtlen yazın olur.'); return; }
    id = 'bogurtlen'; n = se === 1 ? 3 + Math.floor(Math.random() * 3) : 2;
  } else if (fo.kind === 'mushroom') { id = 'mantar'; n = 1 + (Math.random() < 0.4 ? 1 : 0); }
  else { id = 'kekik'; n = 1 + (Math.random() < 0.5 ? 1 : 0); }
  game.add(id, n);
  game.s.forage[fo.kind + Math.round(fo.x) + '_' + Math.round(fo.z)] = game.s.day + 3 + Math.floor(Math.random() * 3);
  fo.taken = true;
  world.forage.refresh(se);
  audio.pop(); useEnergy(0.5);
  ui.toast(`${ITEMS[id].icon} +${n} ${ITEMS[id].name}`);
}

function doInteract(it) {
  switch (it.id) {
    case 'bed':
      if (game.s.minutes < 17 * 60) {
        if (game.s.energy < game.maxEnergy * 0.6) rest(120, 25, '😴 Biraz kestiriyorsun…');
        else ui.toast('Uyumak için daha çok erken (17:00 sonrası). Yorgunken kestirebilirsin.');
      } else sleep();
      break;
    case 'kitchen':
      ui.openKitchen();
      if (game.season === 3) { game.gainEnergy(5); }
      break;
    case 'shop': {
      const m = { tohumcu: () => ui.openSeeds(), hayvan: () => ui.openAnimals(), nalbur: () => ui.openUpgrades(), cicekci: () => ui.openFlowers(), firin: () => ui.openBakery(), pazar: () => ui.openSell('pazar') };
      m[it.shop]();
      break;
    }
    case 'farmstand': ui.openSell('farm'); break;
    case 'jobs': ui.openJobs(); break;
    case 'barn': ui.openBarn(); break;
    case 'mill': ui.openMill(); break;
    case 'coop': {
      let got = 0;
      for (const a of world.animals) {
        if (['tavuk', 'kaz'].includes(a.data.kind) && game.animalProductAvailable(a.data)) { collectProduct(a); got++; }
      }
      if (!got) ui.toast('Kümeste toplanacak yumurta yok.');
      break;
    }
    case 'silo': ui.toast(`Silo: ${ITEMS.saman.icon} ${game.count('saman')} saman · ${ITEMS.yem.icon} ${game.count('yem')} yem`); break;
    case 'well': refillWater('Kuyudan'); break;
    case 'workbench': ui.openBag(); break;
    case 'fishspot': ui.toast('Oltayı seç (6) ve suya doğru kullan.'); break;
    case 'fountain': game.gainEnergy(5); audio.splash(); ui.toast('Buz gibi çeşme suyu: ⚡+5'); break;
    case 'rest': rest(30, 12 + game.s.pots.length, '🌄 Biraz oturup manzarayı izliyorsun…'); break;
    case 'peak':
      if (!game.s.peakToday || game.s.peakToday !== game.s.day) { game.s.peakToday = game.s.day; game.gainEnergy(25); ui.toast('Kartal Tepesi! Bütün vadi ayaklarının altında. ⚡+25', 'gold'); }
      else ui.toast('Manzara hâlâ muhteşem.');
      break;
    default: break;
  }
}

function refillWater(src) {
  const cap = game.s.upgrades.sulama2 ? 40 : 20;
  game.s.water = cap;
  audio.pour();
  ui.updateHotbar();
  ui.toast(`🚿 ${src} su doldurdun (${cap}).`);
}

// ---------------------------------------------------------------- araçlar
function enterVehicle(v) {
  cancelFishing();
  player.enterVehicle(v);
  ui.vehHint(v.def.water ? 'W/S: kürek · A/D: dümen · E: in' : 'W/S: gaz/fren · A/D: direksiyon · E: in' + (v.key === 'tractor' ? ' · tarlada sürersen çapalar' : ''));
}
function exitVehicle() {
  const v = player.vehicle;
  if (v && v.key === 'horse') return dismountHorse();
  if (player.exitVehicle(colliders) === false) { ui.toast('Burada inemezsin, kıyıya yanaş.', 'bad'); return; }
  ui.vehHint(null);
}
let horseVehicle = null;
function rideHorse(a) {
  const def = {
    group: a.group, name: 'At', seat: new THREE.Vector3(0, 2.25, 0.1), radius: 0.9, maxSpeed: 11, accel: 5, turn: 1.8, reverse: 1.5,
    exitOffset: new THREE.Vector3(1.3, 0, 0),
    animate(dist, steer, ctx, speed) {
      const P = a.parts;
      a.phase += dist * 1.8;
      const sw = Math.abs(speed) > 0.2 ? Math.sin(a.phase) : 0;
      const amp = Math.abs(speed) > 6 ? 0.8 : 0.5;
      P.legs[0].rotation.x = sw * amp; P.legs[3].rotation.x = sw * amp;
      P.legs[1].rotation.x = -sw * amp; P.legs[2].rotation.x = -sw * amp;
      P.neck.rotation.x = -0.85 + Math.sin(a.phase * 2) * 0.08;
      P.body.position.y = 1.42 + Math.abs(Math.sin(a.phase)) * (Math.abs(speed) > 6 ? 0.08 : 0.03);
    },
  };
  horseVehicle = new (world.vehicles[0].constructor)('horse', def, a.pos.x, a.pos.z, a.heading);
  horseVehicle.animal = a;
  a.ridden = true;
  a.data.happy = Math.min(100, a.data.happy + 2);
  audio.animal('at');
  player.enterVehicle(horseVehicle);
  ui.vehHint('W/S: dört nala / yavaşla · A/D: dön · E: in');
}
function dismountHorse() {
  const hv = player.vehicle;
  const a = hv.animal;
  player.exitVehicle(colliders);
  a.ridden = false;
  a.pos.set(hv.pos.x, 0, hv.pos.z);
  a.heading = hv.heading;
  const ar = a.area;
  if (a.pos.x < ar.x0 || a.pos.x > ar.x1 || a.pos.z < ar.z0 || a.pos.z > ar.z1) {
    a.pos.x = clamp(a.pos.x, ar.x0 + 1, ar.x1 - 1); a.pos.z = clamp(a.pos.z, ar.z0 + 1, ar.z1 - 1);
    ui.toast('Atın kendi başına meraya döndü.');
  }
  horseVehicle = null;
  ui.vehHint(null);
}

// ---------------------------------------------------------------- alet kullanımı
function useTool() {
  if (busy) return;
  if (player.vehicle) return;
  const tool = game.s.tool;
  const s = game.s;
  const se = game.season;
  if (tool === 5) return fishAction();
  const ti = targetTile();
  const gh = ti >= 240;
  const tile = ti >= 0 ? s.tiles[ti] : null;
  if (tool === 0) {
    if (tile && tile.c && !tile.dead && tile.g >= CROPS[tile.c].days) return harvest(ti);
    const t = findInteract();
    if (t) return t.fn();
    player.doSwing();
    return;
  }
  if (tool === 1) {
    player.doSwing();
    if (!tile) return ui.toast('Çapayı tarlada kullan.');
    if (se === 3 && !gh) return ui.toast('Toprak donmuş! Kışın sadece serada ekim yapılır.', 'bad');
    if (tile.dead) { tile.c = null; tile.dead = false; tile.g = 0; audio.hoe(); useEnergy(1); }
    else if (!tile.t) { tile.t = 1; audio.hoe(); useEnergy(2); }
    else if (tile.c) return ui.toast('Burada ekin var.');
    else return;
    refreshTile(ti);
    return;
  }
  if (tool === 2) {
    // su kaynağı?
    const f = forwardFlat();
    const ax = player.pos.x + f.x * 2, az = player.pos.z + f.z * 2;
    const nearWell = world.interacts.some((it) => it.id === 'well' && Math.hypot(it.x - player.pos.x, it.z - player.pos.z) < 2.6);
    const nearWater = (heightAt(ax, az) < -0.3 || heightAt(player.pos.x, player.pos.z) < -0.2) && !(se === 3);
    if (nearWell || nearWater) return refillWater(nearWell ? 'Kuyudan' : 'Sudan');
    player.doSwing(1);
    if (!tile) return ui.toast('Sulama kabını tarlada kullan.');
    if (!tile.t) return ui.toast('Önce toprağı çapala.');
    if (se === 3 && !gh) return ui.toast('Toprak donmuş.', 'bad');
    if (s.water <= 0) return ui.toast('Sulama kabı boş! Kuyudan veya gölden doldur.', 'bad');
    const targets = [ti];
    if (s.upgrades.sulama2) {
      const r = Math.abs(f.x) > Math.abs(f.z);
      const cols = gh ? 3 : 20;
      const base = gh ? 240 : 0;
      const local = ti - base;
      const c = local % cols, row = Math.floor(local / cols);
      if (r) { if (row > 0) targets.push(ti - cols); if (row < (gh ? 4 : 11)) targets.push(ti + cols); }
      else { if (c > 0) targets.push(ti - 1); if (c < cols - 1) targets.push(ti + 1); }
    }
    for (const k of targets) if (s.tiles[k].t) s.tiles[k].w = 1;
    refreshTile();
    s.water--; audio.pour(); useEnergy(1);
    ui.updateHotbar();
    return;
  }
  if (tool === 3) {
    player.doSwing();
    if (!tile) return ui.toast('Tohumu çapalanmış tarlaya ek.');
    if (!tile.t) return ui.toast('Önce toprağı çapala (2).');
    if (tile.c) return ui.toast(tile.dead ? 'Ölü ekini çapayla temizle.' : 'Burada zaten ekin var.');
    const seed = s.seed;
    const crop = ITEMS[seed]?.crop;
    if (!crop || game.count(seed) <= 0) return ui.toast('Bu tohumdan kalmadı. Q ile değiştir ya da tohumcudan al.', 'bad');
    if (se === 3 && !gh) return ui.toast('Kışın dışarıda ekim yapılmaz.', 'bad');
    if (!gh && !CROPS[crop].seasons.includes(se)) return ui.toast(`${CROPS[crop].name} bu mevsim dışarıda büyümez.`, 'bad');
    game.take(seed, 1);
    tile.c = crop; tile.g = 0; tile.d = 0; tile.dead = false;
    if (game.count('gubre') > 0 && !tile.f) { game.take('gubre', 1); tile.f = 1; }
    audio.pop(); useEnergy(0.5);
    refreshTile(ti);
    ui.updateHotbar();
    return;
  }
  if (tool === 4) {
    player.doSwing();
    const f = forwardFlat();
    const tr = world.trees.nearest(player.pos.x + f.x * 1.2, player.pos.z + f.z * 1.2, 2.2);
    if (!tr) return;
    audio.chop();
    useEnergy(s.upgrades.balta2 ? 2 : 3);
    tr.hp -= s.upgrades.balta2 ? 2 : 1;
    if (tr.hp <= 0) fellTree(tr, f);
    else ui.toast('Tak! 🪓');
    return;
  }
}
function harvest(ti) {
  const tile = game.s.tiles[ti];
  const def = CROPS[tile.c];
  let n = def.yield[0] + Math.floor(Math.random() * (def.yield[1] - def.yield[0] + 1));
  if (game.season === 2 && Math.random() < 0.35) n++;
  game.add(tile.c, n);
  game.s.stats.harvested += n;
  ui.toast(`${def.icon} +${n} ${def.name}`);
  if (def.regrow) { tile.g = def.days - def.regrow; }
  else { tile.c = null; tile.g = 0; tile.f = 0; }
  audio.pop();
  useEnergy(0.5);
  player.doSwing();
  refreshTile(ti);
}
function refreshTile() {
  world.fields.ghOwned = !!game.s.upgrades.sera;
  world.fields.refreshAll(game.s.tiles, game.season);
}
function fellTree(tr, f) {
  const key = tr.type + tr.idx;
  game.s.trees[key] = game.s.day + 8;
  tr.hp = 4;
  const fallObj = world.trees.makeFalling(tr);
  scene.add(fallObj);
  world.trees.setAlive(tr, false);
  world.trees.applySeason(game.season, game.seasonDay);
  // devrilme ekseni: oyuncudan uzağa
  const axis = new THREE.Vector3(f.z, 0, -f.x).normalize();
  falling.push({ obj: fallObj, axis, t: 0, base: fallObj.quaternion.clone() });
  const n = 3 + Math.floor(Math.random() * 3) + (tr.type === 'oak' ? 1 : 0);
  game.add('odun', n);
  game.s.stats.wood++;
  audio.fall();
  ui.toast(`🪵 Ağaç devrildi! +${n} odun`);
}
function updateFalling(dt) {
  for (let i = falling.length - 1; i >= 0; i--) {
    const f = falling[i];
    f.t += dt;
    const ang = Math.min(Math.PI / 2, f.t * f.t * 2.2);
    const q = new THREE.Quaternion().setFromAxisAngle(f.axis, ang);
    f.obj.quaternion.copy(q).multiply(f.base);
    if (f.t > 1.3) {
      f.obj.traverse((o) => { if (o.material && o.material.transparent !== undefined) { o.material = o.material.clone(); o.material.transparent = true; o.material.opacity = Math.max(0, 1 - (f.t - 1.3) * 1.5); } });
    }
    if (f.t > 2.1) { scene.remove(f.obj); falling.splice(i, 1); }
  }
}

// ---------------------------------------------------------------- balıkçılık
function findWaterPoint() {
  const f = forwardFlat();
  const frozen = game.season === 3;
  for (let d = 2; d <= 11; d += 0.5) {
    const x = player.pos.x + f.x * d, z = player.pos.z + f.z * d;
    const h = heightAt(x, z);
    const upper = Math.abs(x - WATERFALL_X) < 6 && z < -162 && z > -215 && h < 25.6;
    if (h < -0.9 || upper) return { x, z, y: upper ? 26 : 0, upper };
  }
  if (frozen && heightAt(player.pos.x, player.pos.z) < -0.9) return { x: player.pos.x + f.x * 1.2, z: player.pos.z + f.z * 1.2, y: 0, ice: true };
  return null;
}
function pickFish(pt) {
  const r = Math.random();
  const pro = game.s.upgrades.olta2 ? 0.1 : 0;
  const fromBoat = player.vehicle && player.vehicle.key === 'boat';
  const riverish = pt.upper || distToRiver(pt.x, pt.z) < 8 || Math.hypot(pt.x - POOL.x, pt.z - POOL.z) < 14;
  if (game.season === 3) return r < 0.3 + pro ? 'buz_baligi' : r < 0.7 ? 'levrek' : 'sazan';
  if (riverish) return r < 0.6 + pro ? 'alabalik' : 'sazan';
  const deep = Math.hypot(pt.x - LAKE.x, pt.z - LAKE.z) < 35 || fromBoat;
  if (r < (deep ? 0.18 : 0.08) + pro) return 'turna';
  if (r < 0.5) return 'levrek';
  return 'sazan';
}
function fishAction() {
  if (busy) return;
  const st = fishing.state;
  if (st === 'idle') {
    if (player.vehicle && player.vehicle.key !== 'boat') return;
    const pt = findWaterPoint();
    if (!pt) return ui.toast('Suya doğru dönüp oltayı at.');
    if (pt.ice) ui.toast('Buzda küçük bir delik açtın ❄');
    player.doSwing(2);
    useEnergy(1);
    fishing = { state: 'wait', pt, t: (2.5 + Math.random() * 6) * (game.s.upgrades.olta2 ? 0.6 : 1) };
    player.bobber.position.set(pt.x, pt.y + 0.02, pt.z);
    player.bobber.visible = true; player.line.visible = true;
    setTimeout(() => audio.splash(), 300);
    return;
  }
  if (st === 'wait') { cancelFishing(); ui.toast('Oltayı topladın.'); return; }
  if (st === 'bite') {
    const fish = pickFish(fishing.pt);
    const rare = ITEMS[fish].price > 100;
    fishing = { ...fishing, state: 'reel', fish, zone: rare ? 0.14 : ITEMS[fish].price > 60 ? 0.22 : 0.3, zx: 0.15 + Math.random() * 0.55, cur: 0, dir: 1, speed: rare ? 1.6 : 1.1 };
    $('fish').classList.remove('hidden');
    $('fishText').textContent = 'Sarı çizgi yeşildeyken tıkla!';
    $('fishZone').style.left = `${fishing.zx * 100}%`;
    $('fishZone').style.width = `${fishing.zone * 100}%`;
    return;
  }
  if (st === 'reel') {
    const ok = fishing.cur >= fishing.zx && fishing.cur <= fishing.zx + fishing.zone;
    const fish = fishing.fish;
    cancelFishing();
    if (ok) {
      game.add(fish, 1); game.s.stats.fish++;
      audio.splash(); audio.coin();
      ui.toast(`${ITEMS[fish].icon} ${ITEMS[fish].name} yakaladın!`, ITEMS[fish].price > 100 ? 'gold' : '');
    } else { audio.bad(); ui.toast('Balık kaçtı…', 'bad'); }
  }
}
function cancelFishing() {
  fishing = { state: 'idle' };
  if (player) { player.bobber.visible = false; player.line.visible = false; }
  $('fish').classList.add('hidden');
}
function updateFishing(dt) {
  if (fishing.state === 'idle') return;
  const b = player.bobber;
  if (game.s.tool !== 5) { cancelFishing(); return; }
  if (Math.hypot(fishing.pt.x - player.pos.x, fishing.pt.z - player.pos.z) > 14) { cancelFishing(); return; }
  const t = clockTime;
  if (fishing.state === 'wait') {
    b.position.y = fishing.pt.y + 0.02 + Math.sin(t * 2) * 0.03;
    fishing.t -= dt;
    if (fishing.t <= 0) { fishing.state = 'bite'; fishing.bt = 1.1; audio.bite(); ui.toast('❗ Balık vurdu! Hemen kullan!'); }
  } else if (fishing.state === 'bite') {
    b.position.y = fishing.pt.y - 0.08 + Math.sin(t * 30) * 0.05;
    fishing.bt -= dt;
    if (fishing.bt <= 0) { fishing.state = 'wait'; fishing.t = 2 + Math.random() * 5; ui.toast('Kaçırdın, bekle…'); }
  } else if (fishing.state === 'reel') {
    fishing.cur += fishing.dir * fishing.speed * dt;
    if (fishing.cur > 1) { fishing.cur = 1; fishing.dir = -1; }
    if (fishing.cur < 0) { fishing.cur = 0; fishing.dir = 1; }
    $('fishCur').style.left = `calc(${fishing.cur * 100}% - 2px)`;
    b.position.y = fishing.pt.y - 0.05 + Math.sin(t * 20) * 0.04;
  }
  // misina
  const tip = new THREE.Vector3(0.32, 1.1, -1.05).applyMatrix4(camera.matrixWorld);
  const arr = player.line.geometry.attributes.position.array;
  arr[0] = tip.x; arr[1] = tip.y; arr[2] = tip.z;
  arr[3] = b.position.x; arr[4] = b.position.y + 0.08; arr[5] = b.position.z;
  player.line.geometry.attributes.position.needsUpdate = true;
}

// ---------------------------------------------------------------- aksiyonlar
function handleActions() {
  for (const a of input.consumeActions()) {
    if (ui.panelOpen) {
      if (a === 'escape' || a === 'bag' || a === 'map' || a === 'help') ui.close();
      continue;
    }
    if (busy) continue;
    switch (a) {
      case 'use': useTool(); break;
      case 'interact': {
        if (fishing.state === 'bite' || fishing.state === 'reel') { fishAction(); break; }
        const t = findInteract();
        if (t) t.fn();
        else {
          const ti = targetTile();
          const tile = ti >= 0 ? game.s.tiles[ti] : null;
          if (tile && tile.c && !tile.dead && tile.g >= CROPS[tile.c].days) harvest(ti);
        }
        break;
      }
      case 'jump': player.jump(); break;
      case 'bag': ui.openBag(); break;
      case 'map': ui.openMap(player); break;
      case 'help': ui.openHelp(isMobile); break;
      case 'eat': eatBest(); break;
      case 'nextSeed': {
        const seeds = Object.keys(game.s.inv).filter((k) => ITEMS[k]?.cat === 'tohum' && game.count(k) > 0);
        if (!seeds.length) { ui.toast('Tohumun yok. Kasabadaki tohumcudan al.', 'bad'); break; }
        const i = seeds.indexOf(game.s.seed);
        game.s.seed = seeds[(i + 1) % seeds.length];
        selectTool(3);
        ui.toast(`Tohum: ${ITEMS[game.s.seed].sub} ${ITEMS[game.s.seed].name} (${game.count(game.s.seed)})`);
        break;
      }
      case 'nextTool': selectTool((game.s.tool + 1) % TOOLS.length); break;
      case 'prevTool': selectTool((game.s.tool + TOOLS.length - 1) % TOOLS.length); break;
      default:
        if (a.startsWith('tool')) selectTool(parseInt(a.slice(4), 10));
    }
  }
}

// ---------------------------------------------------------------- ipuçları
function toolHintFor() {
  if (player.vehicle) return null;
  const tool = game.s.tool;
  const ti = targetTile();
  world.fields.setHighlight(-1);
  if (tool === 5) {
    if (fishing.state === 'wait') return 'Oltayı topla';
    if (fishing.state === 'bite') return 'ÇEK!';
    if (fishing.state === 'reel') return 'Yeşilde bırak!';
    return findWaterPoint() ? 'Oltayı at' : null;
  }
  if (tool === 4) {
    const f = forwardFlat();
    const tr = world.trees.nearest(player.pos.x + f.x * 1.2, player.pos.z + f.z * 1.2, 2.2);
    return tr ? 'Ağacı kes' : null;
  }
  if (tool === 2) {
    const nearWell = world.interacts.some((it) => it.id === 'well' && Math.hypot(it.x - player.pos.x, it.z - player.pos.z) < 2.6);
    const f = forwardFlat();
    if (nearWell || (game.season !== 3 && heightAt(player.pos.x + f.x * 2, player.pos.z + f.z * 2) < -0.3)) return 'Su doldur';
  }
  if (ti < 0) return null;
  const tile = game.s.tiles[ti];
  const se = game.season, gh = ti >= 240;
  const frozen = se === 3 && !gh;
  let txt = null, ok = true;
  if (tool === 0 && tile.c && !tile.dead && tile.g >= CROPS[tile.c].days) txt = 'Hasat et';
  else if (tool === 1) { if (frozen) { txt = 'Toprak donmuş'; ok = false; } else if (tile.dead) txt = 'Ölü ekini temizle'; else if (!tile.t) txt = 'Çapala'; }
  else if (tool === 2) { if (tile.t && !frozen) txt = tile.w ? 'Sulandı ✔' : `Sula (${game.s.water})`; ok = !!tile.t && game.s.water > 0; }
  else if (tool === 3) {
    const crop = ITEMS[game.s.seed]?.crop;
    if (tile.t && !tile.c && crop) {
      ok = (gh || (CROPS[crop].seasons.includes(se) && !frozen)) && game.count(game.s.seed) > 0;
      txt = ok ? `${CROPS[crop].name} ek` : `${CROPS[crop].name} burada ekilemez`;
    }
  }
  if (tile.c && tool !== 0) {
    const def = CROPS[tile.c];
    const info = tile.dead ? 'kurumuş' : tile.g >= def.days ? 'olgun! (E)' : `${Math.floor(tile.g)}/${def.days} gün${tile.w ? ', sulandı' : ', susuz'}`;
    txt = txt ? `${txt} · ${def.name} ${info}` : `${def.name}: ${info}`;
  }
  world.fields.setHighlight(ti, ok ? '#ffffff' : '#ff6a5a');
  return txt;
}

// ---------------------------------------------------------------- ana döngü
let last = performance.now();
const _c = new THREE.Vector3();
function frame(now) {
  requestAnimationFrame(frame);
  const dt = Math.min(0.05, (now - last) / 1000);
  last = now;
  clockTime += dt;
  if (!world) return;
  const g = game;
  const playing = running && !paused && !ui.panelOpen;
  if (running) handleActions();
  const season = g.season;
  const weather = g.s.weather;
  const W = WEATHER[weather] || WEATHER.clear;
  const ctxBase = {
    time: clockTime, season, frozen: season === 3, night: false, dark: W.dark > 0.4, wind: (W.cloud || 0) + (weather === 'storm' || weather === 'blizzard' ? 1.5 : 0),
    platforms: world.platforms, colliders, player: player.pos, energy: g.s.energy,
    dynamicCircles: [], dynamicBlock: null,
  };
  if (playing && !busy) {
    g.s.minutes += dt * MINUTES_PER_REAL_SECOND;
    if (g.s.minutes >= 24 * 60 && !warnedLate) { warnedLate = true; ui.toast('Gece yarısı oldu. 02:00\'den önce yatağına git!', 'bad'); }
    if (g.s.minutes >= 26 * 60) passOut('Uykusuzluktan bayıldın!');
  }
  const hour = (g.s.minutes / 60) % 24;
  const skyRes = sky.update(dt, clockTime, hour, season, weather, camera.position);
  ctxBase.night = sky.isNight;
  // dinamik engeller
  for (const a of world.animals) if (!a.ridden) ctxBase.dynamicCircles.push({ x: a.pos.x, z: a.pos.z, r: a.radius * 0.8 });
  for (const v of world.vehicles) if (!v.driven && v.def.group.visible) ctxBase.dynamicCircles.push({ x: v.pos.x, z: v.pos.z, r: v.def.radius * 0.85 });
  ctxBase.dynamicBlock = (x, z, r, self) => {
    for (const a of world.animals) if (!a.ridden && a !== self.animal && Math.hypot(a.pos.x - x, a.pos.z - z) < r + a.radius * 0.6) return true;
    for (const v of world.vehicles) if (v !== self && !v.driven && v.def.group.visible && Math.hypot(v.pos.x - x, v.pos.z - z) < r + v.def.radius * 0.8) return true;
    return false;
  };
  ctxBase.onStep = () => {
    const surf = player.swimming ? 'water' : player.onPlatform ? 'wood' : season === 3 ? 'snow' : 'grass';
    audio.step(surf);
  };

  let mv = { moving: false };
  if (playing && !busy) {
    mv = player.update(dt, input, ctxBase);
    // traktör çapalaması
    const v = player.vehicle;
    if (v && v.key === 'tractor' && v.speed > 0.6 && season !== 3) {
      const s = Math.sin(v.heading), c = Math.cos(v.heading);
      for (const off of [-0.7, 0, 0.7]) {
        const hx = v.pos.x - s * 2.1 + c * off, hz = v.pos.z - c * 2.1 - s * off;
        const ti = world.fields.tileAt(hx, hz, false);
        if (ti >= 0 && !g.s.tiles[ti].t) { g.s.tiles[ti].t = 1; refreshTile(ti); }
      }
    }
    // enerji tüketimi
    let drain = 0;
    if (mv.running) drain += 0.3;
    if (player.swimming) drain += season === 3 ? 2 : 0.5;
    player.indoors = Math.abs(player.pos.x) < 6 && player.pos.z > -26.5 && player.pos.z < -17.5;
    if (season === 3 && !player.indoors) drain += sky.isNight ? 0.12 : 0.06;
    if (weather === 'hot' && hour > 11 && hour < 16 && !player.indoors) drain += 0.1;
    if (drain > 0) useEnergy(drain * dt);
    if (player.indoors && season === 3) game.gainEnergy(0.08 * dt);
    if (player.indoors && game.s.pots.length) game.gainEnergy(0.01 * game.s.pots.length * dt);
  } else if (player.vehicle || !running) {
    // menüde/kamerada sabit
  }
  if (horseVehicle && player.vehicle === horseVehicle) {
    horseVehicle.animal.pos.copy(horseVehicle.pos);
  }

  // dünya güncelle
  windUniforms.uTime.value = clockTime;
  windUniforms.uWind.value = 0.6 + ctxBase.wind * 0.6;
  const wctx = { ...ctxBase, player: player.pos };
  if (running) world.update(playing ? dt : 0, wctx);
  water.update(clockTime);
  for (const u of waterUniformsList) {
    u.uTime.value = clockTime;
    u.uIce.value = season === 3 ? 1 : 0;
    u.uLight.value = sky.light;
    u.uSunDir.value.copy(sky.sunDir);
    u.uSunColor.value.copy(sky.sunColor).multiplyScalar(skyRes.sunY > 0 ? 1 : 0.2);
    u.uSkyTop.value.copy(sky.skyTop);
    u.uSkyHorizon.value.copy(sky.skyHor);
  }
  fallUniforms.uTime.value = clockTime;
  fallUniforms.uIce.value = season === 3 ? 0.85 : 0;
  fallUniforms.uLight.value = Math.min(1, sky.light + 0.1);
  for (const m of windowGlowMats) m.emissiveIntensity = sky.isNight ? 1.4 : 0;
  updateFalling(dt);
  if (running) updateFishing(dt);

  // HUD
  if (running) {
    hudTimer -= dt;
    if (hudTimer <= 0) {
      hudTimer = 0.15;
      let place = 'Doğada';
      let bestD = Infinity;
      for (const p of PLACES) {
        const d = Math.hypot(p.x - player.pos.x, p.z - player.pos.z);
        if (d < p.r && d < bestD) { bestD = d; place = p.name; }
        if (d < p.r * 0.8 && !g.s.discovered[p.id]) {
          g.s.discovered[p.id] = 1;
          ui.discover(p.name);
          g.gainEnergy(10);
          audio.coin();
        }
      }
      ui.updateHud(place);
      if (playing) {
        const t = findInteract();
        lastTarget = t;
        ui.prompt(t ? t.label : null, isMobile ? 'Etkileş' : 'E');
        const th = toolHintFor();
        ui.toolHint(th, isMobile ? 'Kullan' : 'Tık');
      } else { ui.prompt(null); ui.toolHint(null); }
    }
    const bearing = ((-player.yaw * 180) / Math.PI) % 360;
    ui.updateCompass((bearing + 360) % 360);
    // ses
    const dw = Math.hypot(player.pos.x - WATERFALL_X, player.pos.z + 156);
    const waterAmt = clamp(1 - dw / 70, 0, 1) * (season === 3 ? 0.2 : 1) + clamp(1 - distToRiver(player.pos.x, player.pos.z) / 25, 0, 1) * 0.3 * (season === 3 ? 0 : 1);
    const v = player.vehicle;
    audio.ambient(dt, {
      wind: ctxBase.wind * 0.5, rain: (W.rain || 0) * (player.indoors ? 0.4 : 1), water: waterAmt, day: skyRes.day, season,
      altitude: player.pos.y, engine: v && (v.key === 'tractor' || v.key === 'truck') ? { speed: v.speed, truck: v.key === 'truck' } : null,
    });
    saveTimer += dt;
    if (saveTimer > 60 && playing) { saveTimer = 0; saveAll(); }
  }
  // gece günü değiştir (mevsim görsel)
  if (running) applySeasonNow(false);
  renderer.render(scene, camera);
}

// ---------------------------------------------------------------- başlat
function boot() {
  try {
    setup();
  } catch (e) {
    console.error(e);
    $('loading').textContent = 'Hata: ' + e.message;
    return;
  }
  $('loading').textContent = '';
  const has = game.hasSave();
  $('bContinue').classList.toggle('hidden', !has);
  if (!has) $('bNew').textContent = 'Oyuna Başla';
  let armed = false;
  $('bNew').addEventListener('click', () => {
    if (has && !armed) {
      armed = true;
      $('bNew').textContent = 'Kayıt silinecek. Emin misin? Tekrar bas';
      $('bNew').style.background = 'var(--danger)';
      $('bNew').style.color = '#fff';
      return;
    }
    startGame(true);
  });
  $('bContinue').addEventListener('click', () => startGame(false));
  if (isMobile) {
    $('keysHelp').innerHTML = '<div>Sol daire ile yürü, sağ tarafı kaydırarak etrafa bak.</div><div><b>Kullan</b>: elindeki alet · <b>Etkileş</b>: topla, konuş, araca bin.</div><div>Alttaki çubuktan alet seç.</div>';
  }
  $('pauseNote').addEventListener('click', () => {});
  canvas.addEventListener('click', () => {
    if (running && paused && !isMobile) { paused = false; $('pauseNote').classList.add('hidden'); }
  });
  document.addEventListener('visibilitychange', () => { if (document.hidden && running) saveAll(); });
  window.__game = { dbg: () => ({ tile: targetTile(), inter: findInteract()?.label, pos: player.pos.toArray(), cam: camera.position.toArray(), tool: game.s.tool, busy, paused, panel: ui.panelOpen }), act: (a) => input.actions.push(a), sleep: () => sleep(), applySeason: () => applySeasonNow(true), ui: () => ui, forcePlay: () => { paused = false; $('pauseNote').classList.add('hidden'); }, game, world: () => world, player: () => player, renderer, scene, camera, startGame, sky: () => sky };
}

requestAnimationFrame(frame);
setTimeout(boot, 50);
