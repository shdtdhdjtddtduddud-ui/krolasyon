// HULK MOD - davranış scripti (@minecraft/server 1.16.0, Minecraft Bedrock 1.21.60+)
//
//  Radyoaktif Elma  -> dönüşüm sekansı -> HULK
//  Yetenekler:
//    * Süper zıplama (normal zıplama çok yükseğe çıkar)
//    * Sprint + zıpla        : Hulk Leap (uzun atlayış)
//    * Yüksekten iniş        : Yer Sarsıntısı (otomatik)
//    * Havada eğil (sneak)   : Meteor Dalışı -> inişte dev şok dalgası
//    * Eğilerek vur          : HULK SMASH (alan hasarı + blok kırma)
//    * Yerde eğil + zıpla    : Thunderclap (çift el alkışı, şok dalgası)
//    * Yerde 1.5 sn eğil     : Kükreme (Roar) - düşmanları geri iter, korkutur
//    * Öfke barı             : vurdukça/vuruldukça dolar, dolunca RAGE modu
//    * Düşme hasarı yok, 40 can, direnç, hız, kazma hızı, yenilenme
//  Sakinleştirici Hap veya süre bitince eski haline dönersin.
import { world, system, EntityDamageCause, ItemStack } from "@minecraft/server";

const CFG = {
  durationTicks: 20 * 60 * 5,   // dönüşüm süresi (5 dk)
  rageMax: 100,
  rageModeTicks: 20 * 18,
  breakBlocks: true,            // ezme yetenekleri yumuşak blokları kırsın mı
  softBlocks: new Set([
    "minecraft:dirt", "minecraft:grass_block", "minecraft:sand", "minecraft:gravel", "minecraft:stone",
    "minecraft:cobblestone", "minecraft:netherrack", "minecraft:sandstone", "minecraft:glass", "minecraft:glass_pane",
    "minecraft:oak_leaves", "minecraft:birch_leaves", "minecraft:spruce_leaves", "minecraft:jungle_leaves",
    "minecraft:acacia_leaves", "minecraft:dark_oak_leaves", "minecraft:mud", "minecraft:clay", "minecraft:snow",
    "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:mycelium", "minecraft:andesite", "minecraft:diorite",
    "minecraft:granite", "minecraft:deepslate", "minecraft:terracotta", "minecraft:planks", "minecraft:oak_planks",
    "minecraft:red_sand", "minecraft:soul_sand", "minecraft:tuff", "minecraft:short_grass", "minecraft:tallgrass",
  ]),
};

// hulk:anim değerleri (RP animasyon controller'ı ile aynı)
const ANIM = { NONE: 0, TRANSFORM: 1, REVERT: 2, ROAR: 3, SMASH: 4, CLAP: 5, LAND: 6, DIVE: 7 };
const ANIM_TICKS = { 1: 72, 2: 40, 3: 48, 4: 26, 5: 28, 6: 20, 7: 0 };

/** oyuncu başına çalışma zamanı durumu */
const S = new Map();
const key = (p) => p.id;

function st(p) {
  let s = S.get(key(p));
  if (!s) {
    s = {
      left: p.getDynamicProperty("hulk:left") ?? 0,
      rage: p.getDynamicProperty("hulk:rage") ?? 0,
      rageMode: 0, busy: 0, busyKind: 0,
      cd: { smash: 0, clap: 0, roar: 0, leap: 0 },
      airTicks: 0, peakY: p.location.y, wasGround: true,
      charge: 0, diving: false, step: 0, lastHud: 0, wasJump: false, tf: 0, reverting: false,
    };
    S.set(key(p), s);
  }
  return s;
}

const isHulk = (p) => {
  try { return p.getProperty("hulk:active") === true; } catch { return false; }
};
const setP = (p, k, v) => { try { p.setProperty(k, v); } catch { /* property yoksa yut */ } };
const cmd = (e, c) => { try { return e.runCommand(c); } catch { return undefined; } };
const valid = (e) => {
  try { return !!e && (typeof e.isValid === "function" ? e.isValid() : e.isValid); } catch { return false; }
};

function sound(dim, loc, id, vol = 1, pitch = 1) {
  cmd(dim, `playsound ${id} @a ${loc.x.toFixed(1)} ${loc.y.toFixed(1)} ${loc.z.toFixed(1)} ${vol} ${pitch}`);
}
function particle(dim, id, loc) {
  try { dim.spawnParticle(id, loc); } catch { /* özel particle yüklenmediyse yut */ }
}
function shake(dim, loc, r, intensity, secs) {
  cmd(dim, `camerashake add @a[x=${loc.x.toFixed(0)},y=${loc.y.toFixed(0)},z=${loc.z.toFixed(0)},r=${r}] ${intensity} ${secs} positional`);
}
function say(p, text) { try { p.onScreenDisplay.setActionBar(text); } catch { /* */ } }

function setAnim(p, kind) {
  setP(p, "hulk:anim", kind);
  const s = st(p);
  s.busyKind = kind;
  s.busy = ANIM_TICKS[kind] ?? 0;
}

// ---------------------------------------------------------------------- efektler
function applyEffects(p, s) {
  const rage = s.rageMode > 0;
  const o = (amp) => ({ amplifier: amp, showParticles: false });
  try {
    p.addEffect("strength", 100, o(rage ? 5 : 3));
    p.addEffect("resistance", 100, o(rage ? 2 : 1));
    p.addEffect("speed", 100, o(rage ? 2 : 1));
    p.addEffect("jump_boost", 100, o(rage ? 5 : 3));
    p.addEffect("haste", 100, o(2));
    p.addEffect("regeneration", 100, o(rage ? 2 : 0));
    p.addEffect("health_boost", 100, o(4));
    p.addEffect("saturation", 100, o(0));
    p.addEffect("fire_resistance", 100, o(0));
  } catch { /* */ }
}

function clearEffects(p) {
  for (const e of ["strength", "resistance", "speed", "jump_boost", "haste", "regeneration", "health_boost", "saturation", "fire_resistance", "slowness"]) {
    try { p.removeEffect(e); } catch { /* */ }
  }
}

// ---------------------------------------------------------------------- şok dalgası
function shockwave(p, center, radius, damage, kb, opts = {}) {
  const dim = p.dimension;
  let list = [];
  try { list = dim.getEntities({ location: center, maxDistance: radius }); } catch { /* */ }
  for (const e of list) {
    if (e.id === p.id) continue;
    if (e.typeId === "minecraft:item" || e.typeId === "minecraft:xp_orb") continue;
    const dx = e.location.x - center.x, dz = e.location.z - center.z;
    const len = Math.max(0.3, Math.hypot(dx, dz));
    const fall = 1 - Math.min(1, len / radius) * 0.6;
    try { e.applyKnockback(dx / len, dz / len, kb * fall, opts.up ?? 0.9); } catch { /* */ }
    if (e.typeId !== "minecraft:player" && damage > 0) {
      try { e.applyDamage(damage * fall, { cause: EntityDamageCause.entityAttack, damagingEntity: p }); } catch { /* */ }
    }
    if (opts.fear && e.typeId !== "minecraft:player") {
      try { e.addEffect("slowness", 100, { amplifier: 1, showParticles: true }); e.addEffect("weakness", 200, { amplifier: 1 }); } catch { /* */ }
    }
  }
  if (opts.blocks && CFG.breakBlocks) breakBlocks(dim, center, Math.min(3, Math.floor(radius / 2)));
}

function breakBlocks(dim, c, r) {
  let n = 0;
  const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
  for (let dx = -r; dx <= r; dx++) for (let dz = -r; dz <= r; dz++) for (let dy = -1; dy <= 0; dy++) {
    if (dx * dx + dz * dz > r * r + 1) continue;
    if (n > 60) return;
    let b;
    try { b = dim.getBlock({ x: cx + dx, y: cy + dy, z: cz + dz }); } catch { continue; }
    if (b && CFG.softBlocks.has(b.typeId)) {
      cmd(dim, `setblock ${cx + dx} ${cy + dy} ${cz + dz} air destroy`);
      n++;
    }
  }
}

function groundSmash(p, power) {
  const dim = p.dimension, l = p.location;
  const r = Math.min(14, 5 + power * 0.8);
  particle(dim, "hulk:shockwave_big", { x: l.x, y: l.y, z: l.z });
  particle(dim, "hulk:debris", { x: l.x, y: l.y, z: l.z });
  particle(dim, "minecraft:huge_explosion_emitter", l);
  sound(dim, l, "random.explode", 2, 0.6);
  sound(dim, l, "mob.ravager.stunned", 2, 0.5);
  shake(dim, l, 40, Math.min(1.2, 0.5 + power * 0.06), 0.8);
  shockwave(p, l, r, 6 + power * 1.5, 2.2 + power * 0.12, { blocks: true });
}

// ---------------------------------------------------------------------- yetenekler
function startRoar(p, s) {
  s.cd.roar = 20 * 8;
  setAnim(p, ANIM.ROAR);
  const l = p.location, dim = p.dimension;
  system.runTimeout(() => {
    if (!isHulk(p)) return;
    const l2 = p.location;
    sound(dim, l2, "mob.ravager.roar", 4, 0.5);
    sound(dim, l2, "mob.warden.roar", 3, 0.7);
    particle(dim, "hulk:shockwave_big", { x: l2.x, y: l2.y + 0.1, z: l2.z });
    particle(dim, "hulk:gamma_burst", l2);
    shake(dim, l2, 40, 0.5, 1.6);
    shockwave(p, l2, 12, 0, 1.6, { fear: true, up: 0.5 });
    p.addEffect("strength", 20 * 12, { amplifier: 5, showParticles: false });
    addRage(p, s, 15);
  }, 16);
}

function startClap(p, s) {
  s.cd.clap = 20 * 4;
  setAnim(p, ANIM.CLAP);
  system.runTimeout(() => {
    if (!isHulk(p)) return;
    const l = p.location, dim = p.dimension;
    sound(dim, l, "random.explode", 3, 0.8);
    sound(dim, l, "mob.warden.sonic_boom", 2, 1.4);
    particle(dim, "hulk:shockwave", { x: l.x, y: l.y + 0.1, z: l.z });
    particle(dim, "hulk:shockwave_big", { x: l.x, y: l.y + 0.2, z: l.z });
    shake(dim, l, 30, 0.9, 0.6);
    shockwave(p, l, 10, 10, 3.2, { up: 1.1 });
  }, 11);
}

function startSmash(p, s, target) {
  s.cd.smash = 20 * 3;
  setAnim(p, ANIM.SMASH);
  system.runTimeout(() => {
    if (!isHulk(p)) return;
    const l = valid(target) ? target.location : p.location;
    const dim = p.dimension;
    particle(dim, "hulk:shockwave", { x: l.x, y: l.y + 0.1, z: l.z });
    particle(dim, "hulk:debris", { x: l.x, y: l.y, z: l.z });
    sound(dim, l, "random.explode", 3, 0.7);
    sound(dim, l, "mob.irongolem.death", 2, 0.5);
    shake(dim, l, 30, 1.0, 0.7);
    shockwave(p, l, 7, 14, 2.6, { blocks: true });
    addRage(p, s, 6);
  }, 12);
}

function addRage(p, s, n) {
  if (s.rageMode > 0) return;
  s.rage = Math.min(CFG.rageMax, s.rage + n);
  if (s.rage >= CFG.rageMax) {
    s.rage = 0;
    s.rageMode = CFG.rageModeTicks;
    setP(p, "hulk:stage", 4);
    setAnim(p, ANIM.ROAR);
    const l = p.location;
    sound(p.dimension, l, "mob.ravager.roar", 4, 0.45);
    particle(p.dimension, "hulk:gamma_burst", l);
    particle(p.dimension, "hulk:shockwave_big", { x: l.x, y: l.y + 0.1, z: l.z });
    shake(p.dimension, l, 30, 0.7, 1.2);
    try { p.onScreenDisplay.setTitle("§2§lÖFKE MODU!", { fadeInDuration: 2, stayDuration: 30, fadeOutDuration: 10 }); } catch { /* */ }
    applyEffects(p, s);
  }
}

// ---------------------------------------------------------------------- dönüşüm
function hasRoom(p) {
  const l = p.location, dim = p.dimension;
  for (const dy of [2, 3]) {
    let b;
    try { b = dim.getBlock({ x: Math.floor(l.x), y: Math.floor(l.y) + dy, z: Math.floor(l.z) }); } catch { return true; }
    if (b && !(b.isAir || b.isLiquid)) return false;
  }
  return true;
}

function startTransform(p) {
  const s = st(p);
  if (isHulk(p)) {
    s.left = Math.min(CFG.durationTicks, s.left + 20 * 60);
    addRage(p, s, 35);
    say(p, "§a☢ Gama enerjisi arttı! +1 dk");
    return;
  }
  if (!hasRoom(p)) {
    say(p, "§cDönüşmek için üstünde en az 3 blok boşluk olmalı!");
    try { p.getComponent("minecraft:inventory").container.addItem(new ItemStack("hulk:radioactive_apple", 1)); } catch { /* */ }
    return;
  }
  s.left = CFG.durationTicks; s.rage = 0; s.rageMode = 0; s.tf = 0; s.reverting = false;
  setP(p, "hulk:stage", 0);
  setP(p, "hulk:active", true);
  p.triggerEvent("hulk:transform");
  setAnim(p, ANIM.TRANSFORM);
  const dim = p.dimension, l = p.location;
  sound(dim, l, "mob.warden.heartbeat", 3, 0.6);
  cmd(p, "inputpermission set @s movement disabled");
  cmd(p, "inputpermission set @s camera enabled");
  try { p.onScreenDisplay.setTitle("§2§l...", { fadeInDuration: 5, stayDuration: 50, fadeOutDuration: 10 }); } catch { /* */ }
}

function tickTransform(p, s) {
  const dim = p.dimension, l = p.location;
  const t = s.tf++;
  // dönüşüm sırasında dokular: 0 -> 1 -> 2 -> 3
  if (t === 24) setP(p, "hulk:stage", 1);
  if (t === 46) setP(p, "hulk:stage", 2);
  if (t === 62) setP(p, "hulk:stage", 3);
  if (t % 6 === 0) {
    particle(dim, "hulk:gamma_aura", l);
    if (t > 20) sound(dim, l, "mob.warden.heartbeat", 2, 0.5 + t / 60);
  }
  if (t === 8 || t === 30 || t === 46) {
    sound(dim, l, "mob.zombie.unfect", 1.5, 0.5);
    cmd(p, "camerashake add @s 0.25 1.2 positional");
  }
  if (t >= 24 && t % 8 === 0) { particle(dim, "hulk:cloth", l); sound(dim, l, "random.bowhit", 1, 0.6); }
  if (t === 50) {
    sound(dim, l, "mob.ravager.roar", 4, 0.5);
    sound(dim, l, "mob.warden.roar", 3, 0.6);
    cmd(p, "camerashake add @s 0.9 2.0 positional");
    shake(dim, l, 30, 0.6, 2.0);
  }
  if (t === 54) {
    particle(dim, "hulk:gamma_burst", l);
    particle(dim, "hulk:shockwave_big", { x: l.x, y: l.y + 0.1, z: l.z });
    particle(dim, "minecraft:huge_explosion_emitter", l);
    sound(dim, l, "random.explode", 3, 0.6);
    shockwave(p, l, 10, 0, 2.4, { up: 0.8, fear: true });
  }
  if (t >= 72) {
    cmd(p, "inputpermission set @s movement enabled");
    s.tf = -1;
    setAnim(p, ANIM.NONE);
    applyEffects(p, s);
    try {
      const h = p.getComponent("minecraft:health");
      h.resetToMaxValue();
    } catch { /* */ }
    try { p.onScreenDisplay.setTitle("§a§lHULK SMASH!", { fadeInDuration: 2, stayDuration: 40, fadeOutDuration: 15, subtitle: "§7Öfkeni serbest bırak" }); } catch { /* */ }
  }
}

function startRevert(p, reason) {
  const s = st(p);
  if (!isHulk(p) || s.reverting) return;
  s.reverting = true; s.tf = 0; s.rageMode = 0;
  cmd(p, "inputpermission set @s movement disabled");
  setP(p, "hulk:stage", 3);
  setAnim(p, ANIM.REVERT);
  sound(p.dimension, p.location, "mob.ravager.hurt", 2, 0.6);
  if (reason) say(p, reason);
}

function tickRevert(p, s) {
  const t = s.tf++;
  if (t === 8) setP(p, "hulk:stage", 2);
  if (t === 18) setP(p, "hulk:stage", 1);
  if (t === 28) setP(p, "hulk:stage", 0);
  if (t % 5 === 0) particle(p.dimension, "hulk:gamma_aura", p.location);
  if (t >= 40) finishRevert(p, s, true);
}

function finishRevert(p, s, comedown) {
  s.reverting = false; s.tf = -1; s.left = 0; s.rage = 0; s.rageMode = 0;
  setP(p, "hulk:active", false);
  setP(p, "hulk:stage", 0);
  setP(p, "hulk:anim", 0);
  try { p.triggerEvent("hulk:revert"); } catch { /* */ }
  cmd(p, "inputpermission set @s movement enabled");
  clearEffects(p);
  p.setDynamicProperty("hulk:left", 0);
  if (comedown) {
    try {
      p.addEffect("weakness", 20 * 12, { amplifier: 1 });
      p.addEffect("hunger", 20 * 10, { amplifier: 2 });
      p.addEffect("slowness", 20 * 6, { amplifier: 0 });
    } catch { /* */ }
    say(p, "§7Bruce Banner'a geri döndün...");
  }
}

// ---------------------------------------------------------------------- ana döngü
function forward(p) {
  const v = p.getViewDirection();
  const h = Math.hypot(v.x, v.z) || 1;
  return { x: v.x / h, z: v.z / h };
}

function tickHulk(p, s) {
  if (s.tf >= 0 && !s.reverting && s.busyKind === ANIM.TRANSFORM) { tickTransform(p, s); return; }
  if (s.reverting) { tickRevert(p, s); return; }

  const dim = p.dimension, loc = p.location;
  const vel = p.getVelocity();
  const ground = p.isOnGround;
  const sneak = p.isSneaking;
  const hSpeed = Math.hypot(vel.x, vel.z);

  // zaman / öfke
  s.left--;
  if (s.left % 40 === 0) p.setDynamicProperty("hulk:left", s.left);
  if (s.left <= 0) { startRevert(p, "§7Gama enerjin tükendi..."); return; }
  if (s.rageMode > 0) {
    s.rageMode--;
    if (s.rageMode === 0) { setP(p, "hulk:stage", 3); applyEffects(p, s); say(p, "§7Öfken yatıştı."); }
    if (s.rageMode % 6 === 0) particle(dim, "hulk:gamma_aura", loc);
  } else if (s.rage > 0 && s.left % 10 === 0) {
    s.rage = Math.max(0, s.rage - 0.6);
  }
  if (s.left % 40 === 0) applyEffects(p, s);
  for (const k in s.cd) if (s.cd[k] > 0) s.cd[k]--;

  // animasyon süresi bitince sıfırla
  if (s.busy > 0) {
    s.busy--;
    if (s.busy === 0 && s.busyKind !== ANIM.NONE && s.busyKind !== ANIM.DIVE) { setP(p, "hulk:anim", 0); s.busyKind = 0; }
  }

  // ---- havada
  if (!ground) {
    s.airTicks++;
    if (loc.y > s.peakY) s.peakY = loc.y;
  }
  const jumpNow = p.isJumping;

  // Sprint + zıpla = Hulk Leap
  if (jumpNow && ground && p.isSprinting && s.cd.leap === 0 && !sneak) {
    s.cd.leap = 12;
    const f = forward(p);
    try { p.applyKnockback(f.x, f.z, 3.0, 1.35); } catch { /* */ }
    sound(dim, loc, "mob.ravager.step", 2, 0.5);
    particle(dim, "hulk:debris", loc);
    shake(dim, loc, 12, 0.3, 0.3);
  }

  // Thunderclap: yerde eğil + zıpla
  if (jumpNow && !s.wasJump && ground && sneak && s.cd.clap === 0 && s.busy === 0) startClap(p, s);
  s.wasJump = jumpNow;

  // Meteor dalışı: havada eğil
  if (!ground && sneak && s.airTicks > 8 && !s.diving && loc.y - s.peakY > -50 && s.busy === 0) {
    s.diving = true;
    setAnim(p, ANIM.DIVE);
    sound(dim, loc, "mob.ravager.roar", 2, 0.8);
  }
  if (s.diving) {
    if (!ground && (sneak || s.airTicks < 400)) {
      try { p.applyKnockback(0, 0, 0, -1.4); } catch { /* */ }
      if (s.airTicks % 2 === 0) particle(dim, "hulk:gamma_aura", loc);
    }
  }

  // iniş
  if (ground && !s.wasGround) {
    const drop = s.peakY - loc.y;
    if (s.diving) {
      s.diving = false;
      setAnim(p, ANIM.LAND);
      groundSmash(p, Math.max(8, drop));
    } else if (drop >= 4.5) {
      setAnim(p, ANIM.LAND);
      groundSmash(p, drop);
    } else if (drop >= 1.6) {
      shake(dim, loc, 8, 0.12, 0.2);
      sound(dim, loc, "mob.ravager.step", 1.4, 0.5);
    }
    s.airTicks = 0;
  }
  if (ground) { s.peakY = loc.y; s.airTicks = 0; }
  s.wasGround = ground;

  // Kükreme: yerde 1.5 sn eğil
  if (sneak && ground && hSpeed < 0.03 && s.busy === 0 && s.cd.roar === 0) {
    s.charge++;
    if (s.charge % 6 === 0) { particle(dim, "hulk:gamma_aura", loc); sound(dim, loc, "mob.warden.heartbeat", 1.2, 0.6); }
    if (s.charge >= 30) { s.charge = 0; startRoar(p, s); }
  } else if (!sneak || !ground) {
    s.charge = 0;
  }

  // adım sesleri + titreşim
  if (ground && hSpeed > 0.08 && !sneak && s.busy === 0) {
    s.step++;
    const every = p.isSprinting ? 5 : 8;
    if (s.step >= every) {
      s.step = 0;
      sound(dim, loc, "mob.ravager.step", 1.2, 0.55);
      if (p.isSprinting) { particle(dim, "hulk:debris", loc); shake(dim, loc, 10, 0.06, 0.15); }
    }
  }

  // HUD
  if (s.left % 5 === 0) {
    const bars = 20, full = Math.round((s.rageMode > 0 ? s.rageMode / CFG.rageModeTicks : s.rage / CFG.rageMax) * bars);
    const bar = (s.rageMode > 0 ? "§a" : "§2") + "█".repeat(full) + "§8" + "█".repeat(bars - full);
    const secs = Math.ceil(s.left / 20);
    const time = `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, "0")}`;
    say(p, `§a☢ HULK §r§7| ${s.rageMode > 0 ? "§c§lÖFKE MODU" : "§fÖfke"} ${bar} §7| §f${time}`);
  }
}

system.runInterval(() => {
  for (const p of world.getAllPlayers()) {
    let hulk = false;
    try { hulk = isHulk(p); } catch { /* */ }
    if (!hulk) { if (S.has(key(p))) S.delete(key(p)); continue; }
    try { tickHulk(p, st(p)); } catch (e) { /* tek oyuncu hatası diğerlerini bozmasın */ }
  }
}, 1);

// ---------------------------------------------------------------------- olaylar
world.afterEvents.itemCompleteUse.subscribe((ev) => {
  const p = ev.source;
  const id = ev.itemStack?.typeId;
  if (id === "hulk:radioactive_apple") startTransform(p);
  else if (id === "hulk:calm_pill" && isHulk(p)) startRevert(p, "§bSakinleştirici hap etki ediyor...");
});

world.afterEvents.entityHitEntity.subscribe((ev) => {
  const p = ev.damagingEntity;
  if (p?.typeId !== "minecraft:player" || !isHulk(p)) return;
  const s = st(p);
  addRage(p, s, 3);
  const tgt = ev.hitEntity;
  // ekstra geri tepme
  try {
    const f = forward(p);
    tgt.applyKnockback(f.x, f.z, s.rageMode > 0 ? 2.6 : 1.6, 0.55);
  } catch { /* */ }
  if (p.isSneaking && s.cd.smash === 0 && s.busy === 0) startSmash(p, s, tgt);
});

world.afterEvents.entityHurt.subscribe((ev) => {
  const e = ev.hurtEntity;
  if (e?.typeId !== "minecraft:player" || !isHulk(e)) return;
  const s = st(e);
  if (ev.damageSource.cause === EntityDamageCause.fall) {
    try {
      const h = e.getComponent("minecraft:health");
      h.setCurrentValue(Math.min(h.effectiveMax, h.currentValue + ev.damage));
    } catch { /* */ }
    return;
  }
  addRage(e, s, Math.min(14, ev.damage * 1.5));
});

world.afterEvents.entityDie.subscribe((ev) => {
  const e = ev.deadEntity;
  if (e?.typeId !== "minecraft:player") return;
  try {
    if (isHulk(e)) finishRevert(e, st(e), false);
  } catch { /* */ }
});

world.afterEvents.playerSpawn.subscribe((ev) => {
  const p = ev.player;
  try {
    if (!isHulk(p)) return;
    const s = st(p);
    if (!ev.initialSpawn || s.left <= 0) { finishRevert(p, s, false); return; }
    // yeniden bağlanınca devam et
    s.tf = -1; s.reverting = false; s.busy = 0;
    cmd(p, "inputpermission set @s movement enabled");
    setP(p, "hulk:anim", 0);
    try { p.triggerEvent("hulk:transform"); } catch { /* */ }
    applyEffects(p, s);
  } catch { /* */ }
});

// yönetici komutu:  /scriptevent hulk:revert   |   /scriptevent hulk:transform
system.afterEvents.scriptEventReceive.subscribe((ev) => {
  const p = ev.sourceEntity;
  if (p?.typeId !== "minecraft:player") return;
  if (ev.id === "hulk:transform") startTransform(p);
  if (ev.id === "hulk:revert") startRevert(p, "§7Geri dönüyorsun...");
});

// yedek: /scriptevent hulk:give  -> Radyoaktif Elma + Sakinleştirici Hap verir
system.afterEvents.scriptEventReceive.subscribe((ev) => {
  const p = ev.sourceEntity;
  if (p?.typeId !== "minecraft:player" || ev.id !== "hulk:give") return;
  try {
    const c = p.getComponent("minecraft:inventory").container;
    c.addItem(new ItemStack("hulk:radioactive_apple", 4));
    c.addItem(new ItemStack("hulk:calm_pill", 2));
  } catch (e) { p.sendMessage("§c[Hulk Mod] Eşya verilemedi: " + e); }
});

// script yüklendi mi? (dünyaya girince sohbete yazar)
system.runTimeout(() => {
  try { world.sendMessage("§a[Hulk Mod] Script yüklendi. §7/scriptevent hulk:give ile elma alabilirsin."); } catch { /* */ }
}, 100);
