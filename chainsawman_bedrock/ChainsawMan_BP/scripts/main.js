// Chainsaw Man (Denji) add-on – Minecraft Bedrock 1.21.60+ / @minecraft/server 1.15.0
import { world, system, ItemStack, ItemLockMode, EntityDamageCause } from "@minecraft/server";

const NS = "chainsaw_man";
const HEART = `${NS}:pochita_heart`;
const TAG_FORM = "cm_form";
const TAG_GOT_HEART = "cm_got_heart";

// ------------------------------------------------------------------ config
const ANIM = { NONE: 0, TRANSFORM: 1, UNTRANSFORM: 2, SLASH: 3, SPIN: 4, DASH: 5, ROAR: 6, SPRAY: 7 };
const TRANSFORM_TICKS = 64; // 3.2 s, matches animation.cm.transform
const MODEL_SWAP_TICK = 32; // 1.6 s
const UNTRANSFORM_TICKS = 30; // 1.5 s

const ABILITIES = {
    [`${NS}:blade_slash`]: { label: "Kesik", cooldown: 50, run: abilitySlash, lore: ["§7Üç hamlelik testere kesik serisi", "§8Soğuma: 2.5 sn"] },
    [`${NS}:hurricane`]: { label: "Kasırga", cooldown: 160, run: abilityHurricane, lore: ["§7Kollardaki bıçaklarla 360° dönüş", "§8Soğuma: 8 sn"] },
    [`${NS}:chainsaw_dash`]: { label: "Atılış", cooldown: 100, run: abilityDash, lore: ["§7Bıçaklar önde ileri atılış", "§8Soğuma: 5 sn"] },
    [`${NS}:roar`]: { label: "Kükreme", cooldown: 600, run: abilityRoar, lore: ["§7Korku dalgası + kan çılgınlığı", "§8Soğuma: 30 sn"] },
    [`${NS}:blood_spray`]: { label: "Kan", cooldown: 200, run: abilitySpray, lore: ["§7Baş testeresinden kan püskürtür", "§8Soğuma: 10 sn"] },
};
const ABILITY_IDS = Object.keys(ABILITIES);

const IGNORED_TYPES = [
    "minecraft:item", "minecraft:xp_orb", "minecraft:arrow", "minecraft:thrown_trident", "minecraft:snowball",
    "minecraft:egg", "minecraft:ender_pearl", "minecraft:xp_bottle", "minecraft:fireball", "minecraft:small_fireball",
    "minecraft:armor_stand", "minecraft:painting", "minecraft:falling_block", "minecraft:tnt",
];

// -------------------------------------------------------------------- state
const cooldowns = new Map(); // playerId -> { itemId: readyTick }
const busyUntil = new Map(); // playerId -> tick
const frenzyUntil = new Map(); // playerId -> tick
const deathDefyReady = new Map(); // playerId -> tick
const lastUse = new Map(); // playerId -> tick (debounce)

// ------------------------------------------------------------------ helpers
function valid(e) {
    try {
        const v = e.isValid;
        return typeof v === "function" ? e.isValid() : !!v;
    } catch {
        return false;
    }
}

function later(player, ticks, fn) {
    return system.runTimeout(() => {
        if (!valid(player)) return;
        try {
            fn();
        } catch (err) {
            console.warn(`[chainsawman] ${err}`);
            try {
                player.sendMessage(`§c[Chainsaw Man] Hata: ${err}`);
            } catch { /* ignore */ }
        }
    }, Math.max(1, ticks));
}

const isForm = (p) => p.hasTag(TAG_FORM);
const isBusy = (p) => (busyUntil.get(p.id) ?? 0) > system.currentTick;

function setAnim(p, id, ticks) {
    p.setProperty("cm:anim", id);
    later(p, ticks, () => {
        if (p.getProperty("cm:anim") === id) p.setProperty("cm:anim", ANIM.NONE);
    });
}

function sound(dim, id, loc, volume = 1, pitch = 1) {
    try {
        dim.runCommand(`playsound ${id} @a ${loc.x.toFixed(2)} ${loc.y.toFixed(2)} ${loc.z.toFixed(2)} ${volume} ${pitch}`);
    } catch { /* unknown sound / no players */ }
}

function fx(dim, id, loc) {
    try {
        dim.spawnParticle(id, loc);
    } catch { /* particle not loaded */ }
}

function shake(p, intensity, seconds, mode = "positional") {
    try {
        p.runCommand(`camerashake add @s ${intensity} ${seconds} ${mode}`);
    } catch { /* ignore */ }
}

function shakeNearby(p, radius, intensity, seconds) {
    for (const other of p.dimension.getPlayers({ location: p.location, maxDistance: radius })) shake(other, intensity, seconds);
}

function flash(p, r, g, b, inS, holdS, outS) {
    try {
        p.runCommand(`camera @s fade time ${inS} ${holdS} ${outS} color ${r} ${g} ${b}`);
    } catch { /* ignore */ }
}

function horizontalDir(p) {
    const v = p.getViewDirection();
    const len = Math.hypot(v.x, v.z) || 1;
    return { x: v.x / len, z: v.z / len };
}

function knock(e, dx, dz, h, v) {
    try {
        e.applyKnockback(dx, dz, h, v);
    } catch {
        try {
            e.applyKnockback({ x: dx * h, z: dz * h }, v);
        } catch { /* entity can't be pushed */ }
    }
}

function targetsAround(p, center, radius) {
    let list = [];
    try {
        list = p.dimension.getEntities({ location: center, maxDistance: radius, excludeTypes: IGNORED_TYPES });
    } catch { /* ignore */ }
    return list.filter((e) => e.id !== p.id && valid(e) && e.getComponent("minecraft:health"));
}

function hurt(p, e, amount) {
    try {
        e.applyDamage(amount, { cause: EntityDamageCause.entityAttack, damagingEntity: p });
        return true;
    } catch {
        return false;
    }
}

function heal(p, amount) {
    const h = p.getComponent("minecraft:health");
    if (!h) return;
    h.setCurrentValue(Math.min(h.effectiveMax, h.currentValue + amount));
}

function ring(dim, center, radius, count, particle, y = 0.4) {
    for (let i = 0; i < count; i++) {
        const a = (i / count) * Math.PI * 2;
        fx(dim, particle, { x: center.x + Math.cos(a) * radius, y: center.y + y, z: center.z + Math.sin(a) * radius });
    }
}

function chest(p, up = 1.2) {
    const l = p.location;
    return { x: l.x, y: l.y + up, z: l.z };
}

// --------------------------------------------------------------- effects
function applyBaseEffects(p) {
    const add = (id, amp) => p.addEffect(id, 20000000, { amplifier: amp, showParticles: false });
    add("speed", 1);
    add("strength", 1);
    add("jump_boost", 2);
    add("resistance", 1);
    add("regeneration", 0);
    add("health_boost", 4);
}

function clearBaseEffects(p) {
    for (const id of ["speed", "strength", "jump_boost", "resistance", "regeneration", "health_boost", "hunger"]) {
        try {
            p.removeEffect(id);
        } catch { /* ignore */ }
    }
}

// --------------------------------------------------------------- inventory
function giveAbilityItems(p) {
    const inv = p.getComponent("minecraft:inventory")?.container;
    if (!inv) return;
    removeAbilityItems(p);
    ABILITY_IDS.forEach((id, i) => {
        const stack = new ItemStack(id, 1);
        stack.lockMode = ItemLockMode.inventory;
        stack.setLore(ABILITIES[id].lore);
        const slot = 4 + i; // hotbar slots 5-9
        if (!inv.getItem(slot)) inv.setItem(slot, stack);
        else inv.addItem(stack);
    });
}

function removeAbilityItems(p) {
    const inv = p.getComponent("minecraft:inventory")?.container;
    if (!inv) return;
    for (let i = 0; i < inv.size; i++) {
        const it = inv.getItem(i);
        if (it && ABILITY_IDS.includes(it.typeId)) inv.setItem(i, undefined);
    }
}

function giveHeart(p) {
    const inv = p.getComponent("minecraft:inventory")?.container;
    if (!inv) return;
    const heart = new ItemStack(HEART, 1);
    heart.setLore(["§7Kullan: Chainsaw Man'e dönüş", "§7Tekrar kullan: insana dön"]);
    const left = inv.addItem(heart);
    if (left) p.dimension.spawnItem(left, p.location);
}

// --------------------------------------------------------------- transform
function startTransform(p) {
    const start = system.currentTick;
    busyUntil.set(p.id, start + TRANSFORM_TICKS + 4);
    setAnim(p, ANIM.TRANSFORM, TRANSFORM_TICKS);
    p.addEffect("resistance", TRANSFORM_TICKS + 20, { amplifier: 4, showParticles: false });
    p.addEffect("slowness", TRANSFORM_TICKS - 10, { amplifier: 5, showParticles: false });
    p.onScreenDisplay.setActionBar("§4§lPochita'nın kalbi çarpıyor...");
    shake(p, 0.15, 1.6);

    for (let t = 0; t < MODEL_SWAP_TICK; t += 6) {
        later(p, t, () => {
            const c = chest(p);
            sound(p.dimension, "mob.warden.heartbeat", c, 1.2, 0.7 + t / 60);
            fx(p.dimension, "cm:pulse", c);
            if (t >= 12) fx(p.dimension, "cm:blood_mist", c);
        });
    }
    later(p, 16, () => {
        sound(p.dimension, "cm.engine_rev", p.location, 1, 1);
        shake(p, 0.35, 1.2);
    });
    later(p, MODEL_SWAP_TICK, () => {
        p.addTag(TAG_FORM);
        p.setProperty("cm:form", true);
        applyBaseEffects(p);
        const c = chest(p);
        flash(p, 150, 0, 8, 0.05, 0.12, 0.6);
        shake(p, 0.9, 1.2);
        sound(p.dimension, "random.explode", c, 1, 0.6);
        sound(p.dimension, "cm.engine_roar", c, 1.2, 1);
        fx(p.dimension, "cm:blood_burst", c);
        fx(p.dimension, "cm:blood_burst", { x: c.x, y: c.y + 0.6, z: c.z });
        fx(p.dimension, "cm:smoke", c);
    });
    for (let i = 0; i < 4; i++) {
        later(p, MODEL_SWAP_TICK + 2 + i * 4, () => {
            ring(p.dimension, p.location, 0.8 + i * 0.9, 18 + i * 6, "cm:spark", 0.3 + i * 0.15);
            ring(p.dimension, p.location, 0.5 + i * 0.8, 10, "cm:blood_burst", 0.2);
        });
    }
    later(p, TRANSFORM_TICKS - 2, () => {
        giveAbilityItems(p);
        p.onScreenDisplay.setTitle("§4§lCHAINSAW MAN", {
            subtitle: "§cPochita ile birleştin",
            fadeInDuration: 4,
            stayDuration: 40,
            fadeOutDuration: 16,
        });
        sound(p.dimension, "cm.engine_idle", p.location, 1, 1);
    });
}

function startUntransform(p) {
    busyUntil.set(p.id, system.currentTick + UNTRANSFORM_TICKS + 2);
    setAnim(p, ANIM.UNTRANSFORM, UNTRANSFORM_TICKS);
    sound(p.dimension, "cm.engine_idle", p.location, 1, 0.7);
    fx(p.dimension, "cm:smoke", chest(p));
    later(p, 12, () => {
        p.removeTag(TAG_FORM);
        p.setProperty("cm:form", false);
        clearBaseEffects(p);
        removeAbilityItems(p);
        fx(p.dimension, "cm:blood_burst", chest(p));
        fx(p.dimension, "cm:smoke", chest(p));
        sound(p.dimension, "random.fizz", p.location, 1, 0.8);
        shake(p, 0.3, 0.6);
    });
}

function forceReset(p) {
    p.removeTag(TAG_FORM);
    p.setProperty("cm:form", false);
    p.setProperty("cm:anim", ANIM.NONE);
    clearBaseEffects(p);
    removeAbilityItems(p);
    busyUntil.delete(p.id);
}

function toggleForm(p) {
    if (isBusy(p)) return;
    if (isForm(p)) startUntransform(p);
    else startTransform(p);
}

// --------------------------------------------------------------- abilities
function coneHit(p, reach, minDot, damage, kb, up) {
    const eye = p.getHeadLocation();
    const dir = p.getViewDirection();
    const center = { x: eye.x + dir.x * reach * 0.5, y: eye.y - 0.3, z: eye.z + dir.z * reach * 0.5 };
    let hits = 0;
    for (const e of targetsAround(p, center, reach * 0.75 + 1)) {
        const l = e.location;
        const dx = l.x - eye.x;
        const dz = l.z - eye.z;
        const dy = l.y + 0.8 - eye.y;
        const len = Math.hypot(dx, dy, dz) || 1;
        if (len > reach + 1 || (dx * dir.x + dy * dir.y + dz * dir.z) / len < minDot) continue;
        if (hurt(p, e, damage)) {
            const h = Math.hypot(dx, dz) || 1;
            knock(e, dx / h, dz / h, kb, up);
            fx(p.dimension, "cm:blood_burst", { x: l.x, y: l.y + 1, z: l.z });
            fx(p.dimension, "cm:spark", { x: l.x, y: l.y + 1, z: l.z });
            hits++;
        }
    }
    if (hits) {
        sound(p.dimension, "cm.blade_hit", p.location, 1, 0.9 + Math.random() * 0.2);
        heal(p, hits * 0.5);
    }
    return hits;
}

function abilitySlash(p) {
    setAnim(p, ANIM.SLASH, 18);
    const d = horizontalDir(p);
    knock(p, d.x, d.z, 0.8, 0.1);
    [[5, 7, 0.9], [11, 7, 0.9], [16, 11, 1.6]].forEach(([t, dmg, kb], i) => {
        later(p, t, () => {
            sound(p.dimension, "cm.blade_swing", p.location, 1, 1 + i * 0.1);
            const dir = p.getViewDirection();
            const eye = p.getHeadLocation();
            for (let k = 1; k <= 4; k++) {
                fx(p.dimension, "cm:spark", { x: eye.x + dir.x * k, y: eye.y - 0.4 + (i % 2 ? 0.3 : -0.2) * (k / 4), z: eye.z + dir.z * k });
            }
            coneHit(p, 4.2, 0.25, dmg, kb, 0.25);
            if (i === 2) shake(p, 0.35, 0.3);
        });
    });
}

function abilityHurricane(p) {
    setAnim(p, ANIM.SPIN, 24);
    p.addEffect("resistance", 30, { amplifier: 3, showParticles: false });
    sound(p.dimension, "cm.engine_rev", p.location, 1, 1);
    shake(p, 0.3, 1.1);
    for (let i = 0; i < 10; i++) {
        later(p, 4 + i * 2, () => {
            const l = p.location;
            ring(p.dimension, l, 2.6, 14, "cm:spark", 1.0);
            ring(p.dimension, l, 4.2, 20, "cm:spark", 1.3);
            if (i % 3 === 0) ring(p.dimension, l, 3.4, 12, "cm:blood_burst", 1.1);
            if (i % 2 === 0) sound(p.dimension, "cm.blade_swing", l, 0.8, 0.9 + i * 0.03);
            let any = false;
            for (const e of targetsAround(p, { x: l.x, y: l.y + 1, z: l.z }, 4.8)) {
                if (hurt(p, e, 4)) {
                    const dx = e.location.x - l.x;
                    const dz = e.location.z - l.z;
                    const h = Math.hypot(dx, dz) || 1;
                    knock(e, dx / h, dz / h, 0.6, i === 9 ? 0.9 : 0.15);
                    fx(p.dimension, "cm:blood_burst", { x: e.location.x, y: e.location.y + 1, z: e.location.z });
                    any = true;
                }
            }
            if (any) sound(p.dimension, "cm.blade_hit", l, 0.8, 1);
        });
    }
}

function abilityDash(p) {
    setAnim(p, ANIM.DASH, 16);
    const d = horizontalDir(p);
    sound(p.dimension, "cm.engine_rev", p.location, 1, 1.15);
    knock(p, d.x, d.z, 4.4, 0.12);
    const hit = new Set();
    for (let i = 1; i <= 8; i++) {
        later(p, i * 2, () => {
            const l = p.location;
            if (i <= 5) knock(p, d.x, d.z, 1.4, 0);
            fx(p.dimension, "cm:spark", { x: l.x, y: l.y + 0.2, z: l.z });
            fx(p.dimension, "cm:smoke", { x: l.x - d.x, y: l.y + 0.3, z: l.z - d.z });
            for (const e of targetsAround(p, { x: l.x + d.x * 1.5, y: l.y + 1, z: l.z + d.z * 1.5 }, 2.6)) {
                if (hit.has(e.id)) continue;
                hit.add(e.id);
                if (hurt(p, e, 10)) {
                    knock(e, d.x, d.z, 2.2, 0.5);
                    fx(p.dimension, "cm:blood_burst", { x: e.location.x, y: e.location.y + 1, z: e.location.z });
                    sound(p.dimension, "cm.blade_hit", l, 1, 0.8);
                    shake(p, 0.4, 0.3);
                    heal(p, 1);
                }
            }
        });
    }
}

function abilityRoar(p) {
    setAnim(p, ANIM.ROAR, 40);
    sound(p.dimension, "cm.engine_roar", p.location, 1.5, 1);
    sound(p.dimension, "mob.ravager.roar", p.location, 1.2, 0.6);
    later(p, 8, () => {
        const l = p.location;
        shakeNearby(p, 24, 0.7, 1.5);
        for (let i = 0; i < 5; i++) {
            later(p, i * 2, () => ring(p.dimension, p.location, 1.5 + i * 2.2, 16 + i * 6, "cm:blood_burst", 0.5));
        }
        for (const e of targetsAround(p, l, 13)) {
            const dx = e.location.x - l.x;
            const dz = e.location.z - l.z;
            const h = Math.hypot(dx, dz) || 1;
            knock(e, dx / h, dz / h, 1.6, 0.4);
            try {
                e.addEffect("slowness", 120, { amplifier: 2 });
                e.addEffect("weakness", 120, { amplifier: 1 });
            } catch { /* entity can't take effects */ }
        }
        // blood frenzy: stronger buffs and heavier lifesteal for 15 s
        frenzyUntil.set(p.id, system.currentTick + 300);
        p.addEffect("strength", 300, { amplifier: 3, showParticles: false });
        p.addEffect("speed", 300, { amplifier: 2, showParticles: false });
        p.addEffect("absorption", 300, { amplifier: 2, showParticles: false });
        p.addEffect("regeneration", 300, { amplifier: 2, showParticles: false });
        later(p, 305, () => {
            if (isForm(p)) applyBaseEffects(p);
        });
    });
}

function abilitySpray(p) {
    setAnim(p, ANIM.SPRAY, 24);
    sound(p.dimension, "cm.engine_rev", p.location, 0.8, 1.3);
    for (let i = 0; i < 10; i++) {
        later(p, 5 + i * 2, () => {
            const eye = p.getHeadLocation();
            const dir = p.getViewDirection();
            for (let k = 1; k <= 8; k++) {
                const spread = k * 0.06;
                fx(p.dimension, "cm:blood_burst", {
                    x: eye.x + dir.x * k + (Math.random() - 0.5) * spread,
                    y: eye.y - 0.2 + dir.y * k + (Math.random() - 0.5) * spread,
                    z: eye.z + dir.z * k + (Math.random() - 0.5) * spread,
                });
            }
            if (i % 2 === 0) sound(p.dimension, "mob.wither.shoot", eye, 0.5, 1.6);
            for (const e of targetsAround(p, { x: eye.x + dir.x * 4.5, y: eye.y, z: eye.z + dir.z * 4.5 }, 5.5)) {
                const dx = e.location.x - eye.x;
                const dy = e.location.y + 1 - eye.y;
                const dz = e.location.z - eye.z;
                const len = Math.hypot(dx, dy, dz) || 1;
                if ((dx * dir.x + dy * dir.y + dz * dir.z) / len < 0.85) continue;
                hurt(p, e, 2);
                try {
                    e.addEffect("blindness", 60, { amplifier: 0 });
                    e.addEffect("wither", 80, { amplifier: 1 });
                } catch { /* ignore */ }
            }
        });
    }
}

function useAbility(p, itemId) {
    const ab = ABILITIES[itemId];
    if (!ab || !isForm(p) || isBusy(p)) return;
    const now = system.currentTick;
    const cds = cooldowns.get(p.id) ?? {};
    if ((cds[itemId] ?? 0) > now) return;
    cds[itemId] = now + ab.cooldown;
    cooldowns.set(p.id, cds);
    ab.run(p);
}

// ------------------------------------------------------------------- events
world.afterEvents.itemUse.subscribe((ev) => {
    const p = ev.source;
    if (!p || p.typeId !== "minecraft:player") return;
    const now = system.currentTick;
    if ((lastUse.get(p.id) ?? 0) + 4 > now) return; // debounce double-fire
    lastUse.set(p.id, now);
    const id = ev.itemStack?.typeId;
    try {
        if (id === HEART) toggleForm(p);
        else if (ABILITIES[id]) useAbility(p, id);
    } catch (err) {
        p.sendMessage(`§c[Chainsaw Man] Hata: ${err}`);
    }
});

world.afterEvents.entityHitEntity.subscribe((ev) => {
    const p = ev.damagingEntity;
    if (!p || p.typeId !== "minecraft:player" || !isForm(p) || !valid(ev.hitEntity)) return;
    const frenzy = (frenzyUntil.get(p.id) ?? 0) > system.currentTick;
    const t = ev.hitEntity;
    hurt(p, t, frenzy ? 6 : 3);
    heal(p, frenzy ? 2 : 0.5);
    const l = t.location;
    fx(p.dimension, "cm:blood_burst", { x: l.x, y: l.y + 1, z: l.z });
    fx(p.dimension, "cm:spark", { x: l.x, y: l.y + 1, z: l.z });
    sound(p.dimension, "cm.blade_hit", l, 0.9, 0.9 + Math.random() * 0.3);
});

world.afterEvents.entityDie.subscribe((ev) => {
    const killer = ev.damageSource?.damagingEntity;
    if (killer && killer.typeId === "minecraft:player" && valid(killer) && isForm(killer) && ev.deadEntity.id !== killer.id) {
        heal(killer, 4);
        try {
            fx(killer.dimension, "cm:blood_mist", killer.location);
        } catch { /* ignore */ }
    }
});

// "Blood drinking" – Denji pulls himself back from the brink once in a while
world.afterEvents.entityHurt.subscribe((ev) => {
    const p = ev.hurtEntity;
    if (!p || p.typeId !== "minecraft:player" || !valid(p) || !isForm(p)) return;
    const h = p.getComponent("minecraft:health");
    if (!h || h.currentValue <= 0 || h.currentValue > 6) return;
    const now = system.currentTick;
    if ((deathDefyReady.get(p.id) ?? 0) > now) return;
    deathDefyReady.set(p.id, now + 20 * 120);
    h.setCurrentValue(Math.min(h.effectiveMax, 20));
    p.addEffect("resistance", 60, { amplifier: 3, showParticles: false });
    fx(p.dimension, "cm:blood_burst", chest(p));
    fx(p.dimension, "cm:blood_mist", chest(p));
    sound(p.dimension, "cm.engine_roar", p.location, 1, 1.2);
    shake(p, 0.6, 0.8);
    p.onScreenDisplay.setActionBar("§4§lKan içtin! §cÖlümden döndün");
});

world.afterEvents.playerSpawn.subscribe((ev) => {
    const p = ev.player;
    if (ev.initialSpawn) {
        if (!p.hasTag(TAG_GOT_HEART)) {
            p.addTag(TAG_GOT_HEART);
            giveHeart(p);
            p.sendMessage("§c[Chainsaw Man] §fPochita'nın Kalbi envanterine eklendi. Kullanarak dönüş!");
        }
        // sync client property with the saved tag after re-joining
        try {
            p.setProperty("cm:form", p.hasTag(TAG_FORM));
            p.setProperty("cm:anim", ANIM.NONE);
        } catch (err) {
            p.sendMessage(`§c[Chainsaw Man] Oyuncu özellikleri yüklenemedi (BP player.json aktif mi?): ${err}`);
        }
    } else if (p.hasTag(TAG_FORM)) {
        forceReset(p); // died in Chainsaw form → back to human
    }
});

system.afterEvents.scriptEventReceive.subscribe((ev) => {
    const p = ev.sourceEntity;
    if (!p || p.typeId !== "minecraft:player") return;
    if (ev.id === "cm:heart") giveHeart(p);
    else if (ev.id === "cm:toggle") toggleForm(p);
    else if (ev.id === "cm:reset") forceReset(p);
});

// ------------------------------------------------------------- tick loops
let tickCount = 0;
system.runInterval(() => {
    tickCount += 2;
    const now = system.currentTick;
    for (const p of world.getAllPlayers()) {
        if (!isForm(p)) {
            if (p.getProperty("cm:form") === true && !isBusy(p)) p.setProperty("cm:form", false);
            continue;
        }
        if (p.getProperty("cm:form") !== true && !isBusy(p)) p.setProperty("cm:form", true);

        // sprinting: sparks at the feet + engine noise
        if (p.isSprinting && !isBusy(p)) {
            const l = p.location;
            fx(p.dimension, "cm:spark", { x: l.x, y: l.y + 0.1, z: l.z });
            if (tickCount % 20 === 0) sound(p.dimension, "cm.engine_rev", l, 0.35, 1.4);
        } else if (tickCount % 40 === 0 && !isBusy(p)) {
            sound(p.dimension, "cm.engine_idle", p.location, 0.5, 1);
        }

        // jumping: sparks + rev
        if (p.isJumping && !isBusy(p) && tickCount % 10 === 0) fx(p.dimension, "cm:smoke", p.location);

        // HUD
        if (tickCount % 6 === 0 && !isBusy(p)) {
            const cds = cooldowns.get(p.id) ?? {};
            const parts = ABILITY_IDS.map((id) => {
                const left = Math.max(0, (cds[id] ?? 0) - now);
                return left > 0 ? `§8${ABILITIES[id].label} ${(left / 20).toFixed(1)}` : `§c${ABILITIES[id].label} ✔`;
            });
            const frenzy = (frenzyUntil.get(p.id) ?? 0) > now ? " §4§l[ÇILGINLIK]" : "";
            p.onScreenDisplay.setActionBar(parts.join("  §7|  ") + frenzy);
        }
    }
}, 2);
