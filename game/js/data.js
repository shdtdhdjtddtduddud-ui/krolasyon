// Oyun verileri: sınıflar, yetenekler, düşmanlar, eşyalar, görevler
import { LOC } from './terrain.js';

export const CLASSES = {
  warrior: {
    name: 'Savaşçı', desc: 'Ağır zırhlı yakın dövüş ustası. Kombolar, dönen kılıç ve yer sarsıntısıyla kalabalıkları ezer.',
    hp: 170, hpL: 19, mp: 70, mpL: 4, atk: 15, atkL: 2.4, armor: 12, crit: 0.06, speed: 6.4, weapon: 'sword', color: 0xd04040,
    look: { armor: true, helmet: true, plume: 0xc02020, cape: 0x7a1414, cloth: 0x5a2626, pants: 0x3a3030, armorCol: 0xb8c0c8 },
    dodge: 'roll',
  },
  mage: {
    name: 'Büyücü', desc: 'Kadim elementlerin efendisi. Ateş, buz ve şimşekle uzaktan yıkım yağdırır. Kaçınma yerine ışınlanır.',
    hp: 110, hpL: 12, mp: 160, mpL: 11, atk: 13, atkL: 2.6, armor: 4, crit: 0.08, speed: 6.2, weapon: 'staff', color: 0x6080ff,
    look: { robe: true, hat: 'wizard', cloth: 0x2e3c8a, cloth2: 0x1e2a66, beard: 0xd8d8d8, hair: 0xd0d0d0, cape: 0x1a2250, bracers: true },
    dodge: 'blink',
  },
  ranger: {
    name: 'Okçu', desc: 'Çevik bir avcı. Ok yağmuru, zehirli oklar ve tuzaklarla düşmanlarını uzaktan avlar.',
    hp: 130, hpL: 15, mp: 100, mpL: 7, atk: 12, atkL: 2.3, armor: 7, crit: 0.16, speed: 6.9, weapon: 'bow', color: 0x60c060,
    look: { hood: true, cloth: 0x3e5a2c, cloth2: 0x2c4420, pants: 0x4a3a28, quiver: true, bracers: true, cape: 0x2c4420 },
    dodge: 'roll',
  },
};

// Yetenekler (her sınıf 4) — lvl: açılma seviyesi
export const SKILLS = {
  warrior: [
    { id: 'whirl', name: 'Kasırga', icon: '🌀', mp: 20, cd: 6, lvl: 1, desc: 'Kılıcınla dönerek etrafındaki tüm düşmanlara 3 kez vurursun.' },
    { id: 'slam', name: 'Yer Sarsıntısı', icon: '💥', mp: 30, cd: 10, lvl: 2, desc: 'Havaya sıçrayıp yere çarparsın: geniş alanda ağır hasar ve sersemletme.' },
    { id: 'charge', name: 'Hücum', icon: '⚡', mp: 15, cd: 7, lvl: 4, desc: 'İleri atılırsın, yolundaki herkesi savurursun.' },
    { id: 'warcry', name: 'Savaş Narası', icon: '📯', mp: 40, cd: 30, lvl: 6, desc: '10 sn boyunca +%40 hasar, +%20 hız ve can yenilenmesi.' },
  ],
  mage: [
    { id: 'fireball', name: 'Ateş Topu', icon: '🔥', mp: 16, cd: 2.2, lvl: 1, desc: 'Çarpınca patlayan ve yakan bir ateş topu fırlatır.' },
    { id: 'nova', name: 'Buz Novası', icon: '❄️', mp: 30, cd: 9, lvl: 2, desc: 'Etrafına buz dalgası yayar; düşmanları dondurur.' },
    { id: 'chain', name: 'Zincir Şimşek', icon: '🌩️', mp: 25, cd: 5, lvl: 4, desc: 'Bir düşmandan diğerine seken şimşek.' },
    { id: 'meteor', name: 'Meteor Yağmuru', icon: '☄️', mp: 60, cd: 24, lvl: 6, desc: 'Hedef bölgeye gökten meteorlar yağdırır.' },
  ],
  ranger: [
    { id: 'multishot', name: 'Çoklu Atış', icon: '🏹', mp: 15, cd: 4, lvl: 1, desc: 'Yelpaze şeklinde 7 ok fırlatır.' },
    { id: 'poison', name: 'Zehirli Ok', icon: '☠️', mp: 20, cd: 7, lvl: 2, desc: 'Delip geçen ve zehir bulutu bırakan ok.' },
    { id: 'leap', name: 'Geri Sıçrama', icon: '💨', mp: 20, cd: 8, lvl: 4, desc: 'Geriye takla atarsın ve patlayan bir tuzak bırakırsın.' },
    { id: 'rain', name: 'Ok Yağmuru', icon: '🌧️', mp: 50, cd: 22, lvl: 6, desc: 'Hedef bölgeye 4 sn boyunca ok yağdırır ve yavaşlatır.' },
  ],
};

// Düşman tanımları
export const ENEMIES = {
  slime: { name: 'Balçık', model: 'slime', look: { color: 0x70d050, core: 0x2a7a20 }, hp: 34, dmg: 6, speed: 3.2, range: 1.6, aggro: 14, xp: 14, gold: [1, 4], radius: 0.6, attacks: ['slimeSlam'], drops: [['gel', 0.5]], lvl: 1, sound: 'splat' },
  slimeBlue: { name: 'Mavi Balçık', model: 'slime', look: { color: 0x40a0ff, core: 0x1040a0, s: 1.3 }, hp: 60, dmg: 9, speed: 3.4, range: 1.9, aggro: 15, xp: 26, gold: [2, 6], radius: 0.8, attacks: ['slimeSlam'], lvl: 3, sound: 'splat' },
  wolf: { name: 'Kurt', model: 'quad', look: { color: 0x7a746c, color2: 0x9a948c }, hp: 46, dmg: 8, speed: 7.6, range: 2, aggro: 20, xp: 22, gold: [1, 5], radius: 0.7, attacks: ['bite', 'pounce'], drops: [['pelt', 0.6]], lvl: 2, sound: 'growl', pitch: 1.2 },
  goblin: { name: 'Goblin', model: 'humanoid', look: { head: 'goblin', s: 0.72, skin: 0x6a9a3a, cloth: 0x5a4030, pants: 0x4a3020, bareFeet: true, hood: false }, weapon: 'dagger', hp: 62, dmg: 11, speed: 5.8, range: 1.8, aggro: 18, xp: 34, gold: [3, 9], radius: 0.5, attacks: ['stab', 'stab2'], lvl: 4, sound: 'growl', pitch: 1.9 },
  goblinShaman: { name: 'Goblin Şaman', model: 'humanoid', look: { head: 'goblin', s: 0.75, skin: 0x5a8a50, cloth: 0x503068, robe: true, cloth2: 0x3a2050, bareFeet: true }, weapon: 'staff', weaponColor: 0x80ff40, hp: 50, dmg: 13, speed: 4.5, range: 16, aggro: 22, xp: 40, gold: [4, 10], radius: 0.5, attacks: ['boltGreen'], ranged: true, lvl: 4, sound: 'growl', pitch: 2.1 },
  spider: { name: 'Dev Örümcek', model: 'spider', look: { color: 0x2a2018, color2: 0x6a2a18 }, hp: 80, dmg: 13, speed: 6.6, range: 2.2, aggro: 18, xp: 44, gold: [2, 8], radius: 1.1, attacks: ['spiderBite', 'web'], lvl: 5, sound: 'web', drops: [['silk', 0.6]] },
  skeleton: { name: 'İskelet Savaşçı', model: 'humanoid', look: { head: 'skull', skin: 0xe8e0c8, skinTex: 'bone', eyeGlow: 0x40c0ff, noBelt: true }, weapon: 'sword', weaponColor: 0x8a8a7a, shield: true, hp: 95, dmg: 15, speed: 4.8, range: 2.2, aggro: 18, xp: 50, gold: [4, 12], radius: 0.5, attacks: ['slash1', 'overhead'], lvl: 6, sound: 'metal' },
  orc: { name: 'Ork Savaşçısı', model: 'humanoid', look: { head: 'orc', s: 1.25, bulk: 1.35, skin: 0x5a8a40, shirtless: true, pants: 0x4a3020, bareArms: true, leather: 0x3a2a1a }, weapon: 'axe', hp: 130, dmg: 16, speed: 5.0, range: 2.6, aggro: 18, xp: 48, gold: [5, 14], radius: 0.8, attacks: ['overhead', 'sweep'], lvl: 5, sound: 'growl', pitch: 0.8 },
  scorpion: { name: 'Çöl Akrebi', model: 'scorpion', look: { color: 0xb07030, color2: 0x8a4a20 }, hp: 130, dmg: 18, speed: 5.6, range: 2.6, aggro: 18, xp: 60, gold: [5, 14], radius: 1.1, attacks: ['pinch', 'sting'], lvl: 7, sound: 'web' },
  sandGolem: { name: 'Kum Golemi', model: 'humanoid', look: { head: 'golem', s: 1.7, bulk: 1.4, skin: 0xc8a070, skinTex: 'sandstone', eyeGlow: 0xffc040 }, hp: 240, dmg: 26, speed: 3.6, range: 3, aggro: 16, xp: 90, gold: [8, 20], radius: 1.3, attacks: ['punch', 'slam'], drops: [['ore', 0.55]], lvl: 8, sound: 'growl', pitch: 0.5, heavy: 1 },
  mummy: { name: 'Mumya', model: 'humanoid', look: { skin: 0xd8c8a0, skinTex: 'cloth', cloth: 0xd8c8a0, pants: 0xc8b890, eyeGlow: 0x40ffa0, hair: null, bareFeet: true }, hp: 150, dmg: 20, speed: 3.8, range: 2, aggro: 16, xp: 70, gold: [6, 16], radius: 0.5, attacks: ['punch', 'punch2'], lvl: 8, sound: 'growl', pitch: 0.7 },
  frostWolf: { name: 'Buz Kurdu', model: 'quad', look: { color: 0xd8e4f0, color2: 0xa8c0d8, eyeGlow: 0x40c0ff, s: 1.15 }, hp: 150, dmg: 22, speed: 8, range: 2.2, aggro: 22, xp: 72, gold: [5, 14], radius: 0.8, attacks: ['bite', 'pounce'], drops: [['pelt', 0.4]], lvl: 9, sound: 'growl', pitch: 1.1 },
  yeti: { name: 'Yeti', model: 'humanoid', look: { head: 'yeti', s: 1.6, bulk: 1.5, skin: 0xe8eef8, skinTex: 'fur', hair: 0xe8eef8, shirtless: true, fur: true, furLegs: true, bareFeet: true, claws: true, noBelt: true, armLen: 1.25 }, hp: 260, dmg: 28, speed: 5.2, range: 2.8, aggro: 18, xp: 110, gold: [8, 22], radius: 1.1, attacks: ['punch', 'punch2', 'slam'], lvl: 10, sound: 'growl', pitch: 0.6, heavy: 1 },
  iceGolem: { name: 'Buz Golemi', model: 'humanoid', look: { head: 'golem', s: 1.8, bulk: 1.5, skin: 0x9ad0ff, skinTex: 'ice', eyeGlow: 0x40d0ff, skinEmissive: 0x2070c0, skinEI: 0.5 }, hp: 320, dmg: 30, speed: 3.4, range: 3, aggro: 16, xp: 130, gold: [10, 25], radius: 1.3, attacks: ['punch', 'slam'], drops: [['ore', 0.5]], lvl: 11, sound: 'ice', heavy: 1 },
  imp: { name: 'Ateş İblisi', model: 'humanoid', look: { head: 'imp', s: 0.8, skin: 0xc03018, skinEmissive: 0x601000, skinEI: 0.6, shirtless: true, bareFeet: true, pants: 0x301008, wings: 0x401010, wingGlow: 0x401000, tail: true, eyeGlow: 0xffe040, noBelt: true }, hp: 140, dmg: 24, speed: 6, range: 18, aggro: 24, xp: 110, gold: [8, 20], radius: 0.6, attacks: ['fireBolt'], ranged: true, flying: true, lvl: 12, sound: 'fireball' },
  hellhound: { name: 'Cehennem Tazısı', model: 'quad', look: { color: 0x2a1a18, color2: 0x4a2018, eyeGlow: 0xff4010, spikes: true, tailTip: 0xff5010, emissive: 0x300800, ei: 0.5, s: 1.2 }, hp: 200, dmg: 30, speed: 8.4, range: 2.3, aggro: 22, xp: 125, gold: [8, 22], radius: 0.9, attacks: ['bite', 'fireBreathSmall'], lvl: 12, sound: 'growl', pitch: 0.9 },
  magmaGolem: { name: 'Magma Golemi', model: 'humanoid', look: { head: 'golem', s: 1.9, bulk: 1.6, skin: 0x3a2a24, skinTex: 'lavaRock', eyeGlow: 0xff6010, skinEmissive: 0xff4010, skinEI: 0.9, skinExtra: {} }, hp: 400, dmg: 36, speed: 3.4, range: 3.2, aggro: 16, xp: 170, gold: [12, 30], radius: 1.4, attacks: ['punch', 'slam'], drops: [['ore', 0.5]], lvl: 13, sound: 'growl', pitch: 0.4, heavy: 1 },
  // ---- bosslar ----
  orcChief: { boss: true, name: 'Ork Şefi Grol', model: 'humanoid', look: { head: 'orc', s: 1.8, bulk: 1.6, skin: 0x4a7a30, armor: true, armorCol: 0x6a5a4a, helmet: true, horns: true, pants: 0x3a2010, cape: 0x3a1a0a }, weapon: 'axe', weaponScale: 1.6, hp: 900, dmg: 22, speed: 5.2, range: 3.4, aggro: 24, xp: 400, gold: [80, 120], radius: 1.1, attacks: ['overhead', 'sweep', 'bossStomp', 'bossCharge'], lvl: 6, sound: 'growl', pitch: 0.6, heavy: 1, loot: 'rare' },
  treant: { boss: true, name: 'Çürük Kök, Orman Lordu', model: 'humanoid', look: { head: 'treant', s: 3.0, bulk: 1.5, skin: 0x5a4030, skinTex: 'bark', cloth: 0x3a5a20, shirtless: true, furLegs: false, bareFeet: true, noBelt: true, armLen: 1.3, pants: 0x4a3020 }, hp: 2400, dmg: 30, speed: 3.2, range: 5, aggro: 30, xp: 1200, gold: [200, 300], radius: 2.2, attacks: ['bossSlam', 'rootLine', 'sweep', 'summonSaplings'], lvl: 8, sound: 'growl', pitch: 0.35, heavy: 1, loot: 'epic', crystal: 'forest' },
  scorpionKing: { boss: true, name: 'Akrep Kral Zehirkuyruk', model: 'scorpion', look: { s: 3.2, color: 0x503018, color2: 0xd0a040, crown: true, stingGlow: 0x80ff30, eyeGlow: 0xffe020 }, hp: 4200, dmg: 40, speed: 4.8, range: 5.5, aggro: 32, xp: 2400, gold: [350, 500], radius: 3, attacks: ['pinch', 'bossSting', 'sandSpray', 'burrow'], lvl: 11, sound: 'growl', pitch: 0.5, loot: 'epic', crystal: 'desert' },
  frostGiant: { boss: true, name: 'Buz Devi Hrimgar', model: 'humanoid', look: { head: 'giant', s: 3.6, bulk: 1.5, skin: 0x7aa8d8, cloth: 0x405878, pants: 0x303a50, beard: 0xcfefff, armor: true, armorCol: 0x9ac8e8, bareFeet: false, boots: 0x505a70 }, weapon: 'club', weaponScale: 2.2, hp: 6000, dmg: 50, speed: 3.6, range: 6, aggro: 34, xp: 3800, gold: [500, 700], radius: 2.6, attacks: ['bossSlam', 'iceSpikes', 'frostBreath', 'bossStomp'], lvl: 14, sound: 'growl', pitch: 0.3, heavy: 1, loot: 'legendary', crystal: 'snow' },
  dragon: { boss: true, name: 'Kızıl Ejderha Ignarok', model: 'dragon', look: { s: 2.4 }, hp: 11000, dmg: 60, speed: 5.5, range: 7, aggro: 60, xp: 9000, gold: [1500, 2000], radius: 4, attacks: ['dBite', 'dClaw', 'dTail', 'dBreath', 'dFly'], lvl: 17, sound: 'roar', loot: 'legendary', final: true },
};

// Düşman kampları: [tip listesi, x, z, adet]
export const CAMPS = [];
(function genCamps() {
  const add = (types, x, z, n, r = 10) => CAMPS.push({ types, x, z, n, r });
  // Yeşil vadi
  [[40, 90], [-70, -80], [90, -60], [-100, 20], [60, 130], [130, 60], [-60, 150], [140, -120], [-150, -60], [20, -140]].forEach(([x, z], i) => add(i % 3 === 2 ? ['wolf', 'wolf'] : i % 3 === 1 ? ['slime', 'slime', 'slimeBlue'] : ['slime', 'slime'], x, z, 3 + (i % 2)));
  add(['orc', 'orc', 'goblin'], LOC.orcCamp.x + 8, LOC.orcCamp.z - 8, 5, 14);
  // Karanlık orman
  [[-60, -220], [60, -240], [-120, -300], [110, -310], [0, -300], [-40, -350], [70, -380], [-160, -380], [160, -220], [-180, -250]].forEach(([x, z], i) => add([['goblin', 'goblin', 'goblinShaman'], ['spider', 'spider'], ['wolf', 'wolf', 'wolf'], ['skeleton', 'skeleton', 'goblin']][i % 4], x, z, 3 + (i % 2)));
  // Çöl
  [[230, -80], [240, 90], [300, 40], [330, -40], [360, 120], [300, -170], [400, -110], [200, 170], [380, 180], [220, -180]].forEach(([x, z], i) => add([['scorpion', 'scorpion'], ['sandGolem'], ['mummy', 'mummy', 'scorpion']][i % 3], x, z, 2 + (i % 2)));
  // Buzul
  [[-230, -80], [-240, 90], [-300, 40], [-330, -60], [-360, 120], [-300, -170], [-410, -120], [-210, 170], [-380, 190]].forEach(([x, z], i) => add([['frostWolf', 'frostWolf'], ['yeti'], ['iceGolem'], ['frostWolf', 'yeti']][i % 4], x, z, 2 + (i % 2)));
  // Kül diyarı
  [[-80, 230], [80, 240], [-130, 300], [120, 300], [-60, 320], [60, 330], [160, 220], [-170, 250]].forEach(([x, z], i) => add([['imp', 'imp'], ['hellhound', 'hellhound'], ['magmaGolem'], ['imp', 'hellhound']][i % 4], x, z, 2 + (i % 2)));
})();
export const BOSS_SPAWNS = [
  { type: 'orcChief', x: LOC.orcCamp.x - 4, z: LOC.orcCamp.z + 4 },
  { type: 'treant', x: LOC.forestBoss.x, z: LOC.forestBoss.z },
  { type: 'scorpionKing', x: LOC.desertBoss.x, z: LOC.desertBoss.z },
  { type: 'frostGiant', x: LOC.snowBoss.x, z: LOC.snowBoss.z },
  { type: 'dragon', x: LOC.dragonLair.x, z: LOC.dragonLair.z + 10 },
];

// ---------------- EŞYALAR ----------------
export const RARITY = [
  { id: 0, name: 'Sıradan', color: '#c8c8c8', mult: 1, hex: 0xc8c8c8 },
  { id: 1, name: 'Nadir', color: '#4aa8ff', mult: 1.3, hex: 0x4aa8ff },
  { id: 2, name: 'Epik', color: '#b060ff', mult: 1.65, hex: 0xb060ff },
  { id: 3, name: 'Efsanevi', color: '#ffa030', mult: 2.1, hex: 0xffa030 },
];
export const SLOTS = { weapon: 'Silah', armor: 'Zırh', helm: 'Başlık', amulet: 'Kolye', ring: 'Yüzük' };
const BASES = {
  weapon: { warrior: ['Kılıç', 'Uzun Kılıç', 'Pala', 'Savaş Kılıcı'], mage: ['Asa', 'Kadim Asa', 'Rün Asası', 'Kristal Asa'], ranger: ['Yay', 'Uzun Yay', 'Av Yayı', 'Kompozit Yay'] },
  armor: ['Zırh', 'Zincir Zırh', 'Deri Zırh', 'Plaka Zırh', 'Cübbe'],
  helm: ['Miğfer', 'Başlık', 'Taç', 'Kukuleta'],
  amulet: ['Kolye', 'Muska', 'Tılsım'],
  ring: ['Yüzük', 'Mühür', 'Halka'],
};
const PREFIX = [['Paslı', 'Eski', 'Basit', 'Köylü'], ['Keskin', 'Sağlam', 'Parlak', 'Usta İşi'], ['Kadim', 'Ruhani', 'Yıldız', 'Gölge'], ['Ejderha', 'Titan', 'Anka', 'Göksel']];
const SUFFIX = ['', '', ' (Güç)', ' (Kudret)', ' (Çeviklik)', ' (Bilgelik)', ' (Kan)'];
let uid = 1;
export function makeItem(slot, lvl, rarity, cls) {
  const R = RARITY[rarity]; const r = () => 0.85 + Math.random() * 0.3;
  const it = { uid: Date.now() + '_' + uid++, slot, lvl, rarity, stats: {} };
  const base = slot === 'weapon' ? BASES.weapon[cls][Math.min(3, Math.floor(lvl / 5))] : BASES[slot][Math.floor(Math.random() * BASES[slot].length)];
  it.name = PREFIX[rarity][Math.floor(Math.random() * 4)] + ' ' + base + (rarity > 0 ? SUFFIX[Math.floor(Math.random() * SUFFIX.length)] : '');
  const S = it.stats;
  if (slot === 'weapon') { S.atk = Math.round((5 + lvl * 2.6) * R.mult * r()); if (rarity >= 1) S.crit = +(0.02 + Math.random() * 0.04 * rarity).toFixed(3); if (rarity >= 2) S.critDmg = +(0.15 + Math.random() * 0.2).toFixed(2); it.cls = cls; }
  if (slot === 'armor') { S.armor = Math.round((4 + lvl * 1.6) * R.mult * r()); S.hp = Math.round((10 + lvl * 6) * R.mult * r()); }
  if (slot === 'helm') { S.armor = Math.round((2 + lvl * 0.9) * R.mult * r()); if (rarity >= 1) S.hp = Math.round((6 + lvl * 3) * R.mult * r()); }
  if (slot === 'amulet') { S.mp = Math.round((10 + lvl * 4) * R.mult * r()); if (rarity >= 1) S.crit = +(0.02 + Math.random() * 0.03 * rarity).toFixed(3); if (rarity >= 2) S.atk = Math.round(lvl * 0.8 * R.mult); }
  if (slot === 'ring') { S.critDmg = +(0.1 + Math.random() * 0.15 * (rarity + 1)).toFixed(2); if (rarity >= 1) S.speed = +(0.02 + Math.random() * 0.04).toFixed(3); if (rarity >= 2) S.hp = Math.round((8 + lvl * 3) * R.mult); }
  it.value = Math.round((8 + lvl * 5) * (1 + rarity * 1.5));
  return it;
}
export function rollRarity(bonus = 0) { const r = Math.random() - bonus; return r < 0.02 ? 3 : r < 0.1 ? 2 : r < 0.32 ? 1 : 0; }
export const STAT_NAMES = { atk: 'Saldırı', armor: 'Zırh', hp: 'Can', mp: 'Mana', crit: 'Kritik Şans', critDmg: 'Kritik Hasar', speed: 'Hız' };
export function statText(k, v) { if (k === 'crit' || k === 'speed' || k === 'critDmg') return `+${(v * 100).toFixed(0)}% ${STAT_NAMES[k]}`; return `+${v} ${STAT_NAMES[k]}`; }
export const MATERIALS = { gel: { name: 'Balçık Özü', value: 3, icon: '🟢' }, pelt: { name: 'Kurt Postu', value: 8, icon: '🐺' }, silk: { name: 'Örümcek İpeği', value: 10, icon: '🕸️' }, ore: { name: 'Yıldız Demiri', value: 25, icon: '💎' } };

// ---------------- GÖREVLER ----------------
// kind: kill (target:{type:count}), collect (mat:count), boss
export const QUESTS = {
  m1: { main: true, giver: 'elder', name: 'Köyün Güvenliği', desc: 'Köyün çevresindeki balçıklar ve kurtlar çiftlikleri tehdit ediyor. 6 Balçık ve 4 Kurt avla.', kill: { slime: 6, wolf: 4 }, reward: { xp: 180, gold: 60, potions: 3 }, next: 'm2' },
  m2: { main: true, giver: 'elder', name: 'Ork Tehdidi', desc: 'Güneybatıdaki ork kampı köye saldırı hazırlığında. Kampa gir ve Ork Şefi Grol\'ü yen.', kill: { orcChief: 1 }, reward: { xp: 450, gold: 150, potions: 3 }, next: 'm3', marker: 'orcCamp' },
  m3: { main: true, giver: 'elder', name: 'Karanlık Orman', desc: 'Kuzeydeki orman lanetlendi. Goblinleri temizle ve harabelerdeki Orman Lordu Çürük Kök\'ü yok et. Orman Kristali\'ni geri getir.', kill: { goblin: 6, treant: 1 }, reward: { xp: 1200, gold: 300, potions: 4 }, next: 'm4', marker: 'forestBoss' },
  m4: { main: true, giver: 'elder', name: 'Kızıl Çölün Sırrı', desc: 'Doğudaki çölde piramidin önünde Akrep Kral uyandı. Onu yen ve Çöl Kristali\'ni al.', kill: { scorpionKing: 1 }, reward: { xp: 2200, gold: 500, potions: 5 }, next: 'm5', marker: 'desertBoss' },
  m5: { main: true, giver: 'elder', name: 'Buzul Kalbi', desc: 'Batının buzul zirvelerinde Buz Devi Hrimgar hüküm sürüyor. Onu devir ve Buz Kristali\'ni al.', kill: { frostGiant: 1 }, reward: { xp: 3500, gold: 700, potions: 5 }, next: 'm6', marker: 'snowBoss' },
  m6: { main: true, giver: 'elder', name: 'Ejderhanın Uyanışı', desc: 'Üç kristal toplandı. Güneydeki Kül Diyarı\'nda ejderha inine gir ve Kızıl Ejderha Ignarok\'u öldür!', kill: { dragon: 1 }, reward: { xp: 8000, gold: 2000 }, next: null, marker: 'dragonLair' },
  s1: { giver: 'hunter', name: 'Post Avı', desc: 'Kış yaklaşıyor. Avcı Kaan için 8 Kurt Postu topla.', collect: { pelt: 8 }, reward: { xp: 300, gold: 120, item: 'weapon' }, minLvl: 2 },
  s2: { giver: 'merchant', name: 'İpek Yolu', desc: 'Tüccar Selin nadir ipek kumaşlar dokuyor. 6 Örümcek İpeği getir.', collect: { silk: 6 }, reward: { xp: 600, gold: 250, item: 'amulet' }, minLvl: 4 },
  s3: { giver: 'smith', name: 'Yıldız Demiri', desc: 'Demirci Bora efsanevi bir zırh dövmek istiyor. Golemlerden 5 Yıldız Demiri topla.', collect: { ore: 5 }, reward: { xp: 1500, gold: 300, item: 'armor', rarity: 3 }, minLvl: 7 },
  s4: { giver: 'guard', name: 'Kemik Yığını', desc: 'Ormandaki harabelerde iskeletler dolaşıyor. 8 İskelet Savaşçı yok et.', kill: { skeleton: 8 }, reward: { xp: 900, gold: 200, item: 'helm' }, minLvl: 5 },
  s5: { giver: 'hunter', name: 'Ateş Tazıları', desc: 'Kül Diyarı\'ndaki cehennem tazıları avcıları avlıyor. 6 tanesini öldür.', kill: { hellhound: 6 }, reward: { xp: 3000, gold: 600, item: 'ring', rarity: 2 }, minLvl: 11 },
};

export const NPCS = [
  { id: 'elder', name: 'Bilge Arda', title: 'Köy Yaşlısı', x: -3, z: -10, look: { robe: true, cloth: 0x6a5a8a, cloth2: 0x4a3a6a, beard: 0xe8e8e8, hair: 0xd8d8d8, hat: null }, weapon: 'staff', weaponColor: 0xa0e0ff, idle: 'talk' },
  { id: 'merchant', name: 'Tüccar Selin', title: 'Tüccar', x: 17, z: -5, look: { cloth: 0xa04a2a, cloth2: 0xd0a040, skirt: true, hair: 0x8a3a1a, hat: 'cap' }, idle: 'talk', shop: true },
  { id: 'smith', name: 'Demirci Bora', title: 'Demirci', x: -19, z: -3, look: { cloth: 0x3a3a3a, leather: 0x4a2a1a, beard: 0x2a1a0a, hair: 0x1a1008, bareArms: true, bulk: 1.25 }, weapon: 'axe', idle: 'hammer', smith: true },
  { id: 'hunter', name: 'Avcı Kaan', title: 'Avcı', x: 10, z: 20, look: { hood: true, cloth: 0x5a4a2a, cloth2: 0x3a3020, quiver: true, bracers: true }, idle: 'talk' },
  { id: 'guard', name: 'Muhafız Deniz', title: 'Köy Muhafızı', x: -2, z: 30, look: { armor: true, helmet: true, cloth: 0x2a3a6a, armorCol: 0xa0a8b0 }, weapon: 'spear', idle: null },
];
export const VILLAGERS = [
  { look: { cloth: 0x8a6a4a, hat: 'straw', beard: 0x6a4a2a } }, { look: { cloth: 0x4a7a8a, skirt: true, hair: 0xe0c070 } }, { look: { cloth: 0x7a3a4a, skirt: true, hair: 0x2a1a0a } }, { look: { cloth: 0x5a6a3a, hair: 0x5a3a1a, s: 0.7 } },
];
