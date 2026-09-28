// KROLASYON: Kayıp Kristaller — ana oyun
import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { ShaderPass } from 'three/addons/postprocessing/ShaderPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { World } from './world.js';
import { buildStructures } from './structures.js';
import { heightAt, biomeWeights, dominantBiome, BIOME_NAMES, LOC, WAYSTONES } from './terrain.js';
import { FX } from './fx.js';
import { audio } from './audio.js';
import { Input } from './input.js';
import { UI } from './ui.js';
import { Player } from './player.js';
import { Enemy } from './enemy.js';
import { spawnNPCs } from './npc.js';
import { Loot } from './loot.js';
import { CLASSES, SKILLS, ENEMIES, CAMPS, BOSS_SPAWNS, QUESTS, makeItem, rollRarity, MATERIALS, RARITY } from './data.js';
import { buildHumanoid, makeWeapon } from './models.js';
import { animate } from './anim.js';
import { setAniso } from './textures.js';
import { clamp, damp, dampAngle, rand, randInt, pick, wrapAngle, lerp } from './util.js';
import { noise } from './noise.js';

const $ = (id) => document.getElementById(id);
const SAVE_KEY = 'krolasyon_rpg_save_v1', SET_KEY = 'krolasyon_rpg_settings_v1';
const QUALITY = {
  low: { shadows: false, shadowSize: 1024, viewDist: 230, grass: true, grassDist: 38, grassDensity: 260, lights: 1, bloom: false, pr: 0.8, particleMul: 0.5, aa: false },
  medium: { shadows: true, shadowSize: 1024, viewDist: 320, grass: true, grassDist: 55, grassDensity: 520, lights: 2, bloom: true, pr: 1, particleMul: 0.8, aa: false },
  high: { shadows: true, shadowSize: 2048, viewDist: 430, grass: true, grassDist: 72, grassDensity: 900, lights: 3, bloom: true, pr: 1.5, particleMul: 1, aa: true },
};
const GradeShader = {
  uniforms: { tDiffuse: { value: null }, uVig: { value: 0.35 }, uSat: { value: 1.12 }, uTint: { value: new THREE.Color(1, 1, 1) }, uLift: { value: 0.0 } },
  vertexShader: 'varying vec2 vUv; void main(){ vUv = uv; gl_Position = projectionMatrix*modelViewMatrix*vec4(position,1.0); }',
  fragmentShader: `uniform sampler2D tDiffuse; uniform float uVig; uniform float uSat; uniform vec3 uTint; uniform float uLift; varying vec2 vUv;
    void main(){ vec4 c = texture2D(tDiffuse, vUv); vec3 col = c.rgb * uTint; float l = dot(col, vec3(0.2126,0.7152,0.0722));
      col = max(mix(vec3(l), col, uSat), 0.0); col += uLift;
      vec2 d = vUv - 0.5; float v = 1.0 - uVig * pow(length(d) * 1.35, 2.4); col *= clamp(v, 0.0, 1.0);
      gl_FragColor = vec4(col, c.a); }`,
};
const TIPS = ['İpucu: Düşmanların büyük saldırılarından önce yerde kırmızı alanlar belirir, takla atarak kaç!', 'İpucu: Işınlanma taşlarını keşfet; bölgeler arasında anında yolculuk edebilirsin.', 'İpucu: Demirci Bora silahını güçlendirebilir.', 'İpucu: Savaşçı kombosunun üçüncü vuruşu düşmanları havaya savurur.', 'İpucu: Gece ormanda ateş böcekleri, çölde kum fırtınası seni bekler.', 'İpucu: Tab ile bir düşmana kilitlenebilirsin.'];
const nextFrame = () => new Promise((r) => requestAnimationFrame(() => setTimeout(r, 0)));

class Game {
  constructor() {
    this.canvas = $('game');
    this.settings = { quality: matchMedia('(pointer: coarse)').matches ? 'low' : 'high', master: 0.8, music: 0.45, sfx: 0.9, sens: 1, shake: 1 };
    try { Object.assign(this.settings, JSON.parse(localStorage.getItem(SET_KEY) || '{}')); } catch (e) {}
    this.q = { ...QUALITY[this.settings.quality] };
    this.state = 'loading'; this.time = 0; this.paused = false;
    this.enemies = []; this.projectiles = []; this.loots = []; this.effects = []; this.timers = [];
    this.hitstopT = 0; this.camYaw = Math.PI; this.camPitch = 0.32; this.camDist = 7.5; this.camTarget = new THREE.Vector3();
    this.initRenderer();
    this.input = new Input(this.canvas); this.input.sens = this.settings.sens;
    this.input.onLockChange = (l) => { $('cross').classList.toggle('hidden', !l); $('hint').classList.toggle('hidden', l || this.input.touch || this.state !== 'play' || this.input.lockFailed); };
    $('hint').onclick = () => this.input.requestLock();
    this.audio = audio; this.applyVolume();
    this.ui = new UI(this);
    this.last = performance.now(); this.frameAvg = 1 / 60; this.drsT = 0;
    requestAnimationFrame((t) => this.loop(t));
    this.load();
  }
  initRenderer() {
    const q = this.q;
    const r = new THREE.WebGLRenderer({ canvas: this.canvas, antialias: q.aa && !q.bloom, powerPreference: 'high-performance', preserveDrawingBuffer: !!window.__TEST__ });
    r.outputColorSpace = THREE.SRGBColorSpace; r.toneMapping = THREE.ACESFilmicToneMapping; r.toneMappingExposure = 1.0;
    r.shadowMap.enabled = q.shadows; r.shadowMap.type = THREE.PCFSoftShadowMap;
    this.pixelRatio = Math.min(devicePixelRatio || 1, q.pr); r.setPixelRatio(this.pixelRatio);
    r.setSize(innerWidth, innerHeight, false); this.renderer = r; setAniso(Math.min(8, r.capabilities.getMaxAnisotropy()));
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(62, innerWidth / innerHeight, 0.1, 6000);
    this.buildComposer();
    addEventListener('resize', () => this.resize());
  }
  buildComposer() {
    const r = this.renderer, q = this.q;
    if (!q.bloom) { this.composer = null; return; }
    const rt = new THREE.WebGLRenderTarget(innerWidth * this.pixelRatio, innerHeight * this.pixelRatio, { type: THREE.HalfFloatType, samples: q.aa ? 4 : 0 });
    const c = new EffectComposer(r, rt); c.setPixelRatio(this.pixelRatio); c.setSize(innerWidth, innerHeight);
    c.addPass(new RenderPass(this.scene, this.camera));
    this.bloom = new UnrealBloomPass(new THREE.Vector2(innerWidth / 2, innerHeight / 2), 0.55, 0.45, 0.92); c.addPass(this.bloom);
    this.grade = new ShaderPass(GradeShader); c.addPass(this.grade);
    c.addPass(new OutputPass()); this.composer = c;
  }
  resize() { this.renderer.setSize(innerWidth, innerHeight, false); this.camera.aspect = innerWidth / innerHeight; this.camera.updateProjectionMatrix(); this.composer?.setSize(innerWidth, innerHeight); }
  applyVolume() { const s = this.settings; audio.vol.master = s.master; audio.vol.music = s.music; audio.vol.sfx = s.sfx; audio.setVolumes(); }
  saveSettings() { try { localStorage.setItem(SET_KEY, JSON.stringify(this.settings)); } catch (e) {} }
  applyQuality() {
    const nq = QUALITY[this.settings.quality]; const q = this.q;
    Object.assign(q, { viewDist: nq.viewDist, grassDist: nq.grassDist, particleMul: nq.particleMul, pr: nq.pr, bloom: nq.bloom });
    this.pixelRatio = Math.min(devicePixelRatio || 1, q.pr); this.renderer.setPixelRatio(this.pixelRatio); this.buildComposer(); this.resize();
    for (const [k, c] of this.world.grassCells) { if (c.g) { this.scene.remove(c.g); c.g.dispose(); } if (c.f) { this.scene.remove(c.f); c.f.dispose(); } } this.world.grassCells.clear();
  }
  // ---------- yükleme ----------
  async load() {
    const bar = $('lbar'), tip = $('ltip'); let ti = 0;
    const prog = async (p, t) => { bar.style.width = p * 100 + '%'; if (t) tip.textContent = t; await nextFrame(); };
    await prog(0.05, TIPS[0]);
    this.world = new World(this.scene, this.q);
    const steps = ['Arazi şekilleniyor…', 'Nehirler akıyor…', 'Gökyüzü boyanıyor…', 'Ormanlar büyüyor…', 'Köy kuruluyor…'];
    const { buildHeights } = await import('./terrain.js');
    buildHeights(); await prog(0.2, steps[0]);
    this.world.buildTerrain(); await prog(0.35, steps[1]);
    this.world.buildLiquid(); this.world.buildSky(); await prog(0.45, steps[2]);
    this.world.planChunks(); await prog(0.6, steps[3]);
    this.world.buildMinimap();
    this.structs = buildStructures(this.scene, this.world, this.q); await prog(0.72, steps[4]);
    this.fx = new FX(this.scene, this.camera, this.q);
    this.barrierCol = null;
    const { addBox } = await import('./collision.js'); const B = this.structs.barrier; this.barrierCol = addBox(B.x, B.z, B.w, 1, 0);
    await prog(0.8, TIPS[1 + randInt(0, TIPS.length - 2)]);
    // shader ön-derleme: köy çevresi
    this.camera.position.set(60, heightAt(60, 60) + 30, 60); this.camera.lookAt(0, 5, 0);
    this.world.update(0.01, { pos: new THREE.Vector3() }, this.camera); this.world.updateChunks(0, 0);
    try { this.renderer.compile(this.scene, this.camera); } catch (e) {}
    await prog(1, 'Hazır!');
    await new Promise((r) => setTimeout(r, 250));
    $('loading').classList.add('hidden');
    if (window.__TEST__) { this.start(null, window.__TEST__.cls || 'warrior'); window.READY = true; return; }
    this.showTitle();
  }
  // ---------- başlık ----------
  loadSave() { try { const s = JSON.parse(localStorage.getItem(SAVE_KEY) || 'null'); return s && s.cls ? s : null; } catch (e) { return null; } }
  showTitle() {
    this.state = 'title'; $('title').classList.remove('hidden'); this.world.time = 0.3; window.TITLE_READY = true;
    const s = this.loadSave(); $('bContinue').classList.toggle('hidden', !s);
    if (s) $('bContinue').textContent = `Devam Et — ${CLASSES[s.cls].name} Sv ${s.level}`;
    $('bContinue').onclick = () => { audio.init(); $('title').classList.add('hidden'); this.start(s); };
    $('bNew').onclick = () => { audio.init(); audio.play('click'); $('title').classList.add('hidden'); this.showClassSelect(); };
    $('bSettings').onclick = () => { audio.init(); this.ui.open('Ayarlar', (b) => this.ui.renderSettings(b)); };
  }
  showClassSelect() {
    this.state = 'classSel'; $('classSel').classList.remove('hidden');
    const cx = 0, cz = 14, y = heightAt(cx, cz); this.previews = {};
    ['warrior', 'mage', 'ranger'].forEach((k, i) => {
      const C = CLASSES[k]; const m = buildHumanoid(C.look);
      const w = makeWeapon(C.weapon, k === 'mage' ? 0x80a0ff : 0xc8d0d8); w.rotation.set(Math.PI / 2, 0, 0);
      if (C.weapon === 'bow') m.p.lSocket.add(w); else { if (C.weapon === 'staff') w.position.set(0, 0, -0.5); m.p.rSocket.add(w); }
      if (k === 'warrior') { const s = makeWeapon('shield'); s.rotation.set(0, Math.PI / 2, 0); s.position.set(0.1, -0.18, 0); m.p.lElbow.add(s); }
      const x = cx + (i - 1) * 2.2; m.root.position.set(x, heightAt(x, cz), cz); m.root.rotation.y = 0; this.scene.add(m.root);
      this.previews[k] = { m, st: { time: i, holdPose: C.weapon === 'sword' ? 'sword' : C.weapon === 'staff' ? 'staff' : 'bow' }, x };
    });
    const cards = $('cards'); cards.innerHTML = '';
    const mx = { hp: 170, atk: 16, armor: 12, speed: 7 };
    for (const k of ['warrior', 'mage', 'ranger']) {
      const C = CLASSES[k]; const d = document.createElement('div'); d.className = 'card'; d.tabIndex = 0;
      d.innerHTML = `<h3>${C.name}</h3><p>${C.desc}</p><div class="st"><b>Can</b><div class="meter"><i style="width:${C.hp / mx.hp * 100}%"></i></div><b>Saldırı</b><div class="meter"><i style="width:${C.atk / mx.atk * 100}%"></i></div><b>Zırh</b><div class="meter"><i style="width:${C.armor / mx.armor * 100}%"></i></div><b>Hız</b><div class="meter"><i style="width:${C.speed / mx.speed * 100}%"></i></div><b>Kritik</b><div class="meter"><i style="width:${C.crit / 0.2 * 100}%"></i></div></div><div class="sk">${SKILLS[k].map((s) => `<span title="${s.name}: ${s.desc}">${s.icon}</span>`).join('')}</div>`;
      d.onclick = () => this.pickClass(k); d.onkeydown = (e) => { if (e.key === 'Enter') this.pickClass(k); }; d.dataset.k = k; cards.appendChild(d);
    }
    this.pickClass('warrior');
    $('bBack').onclick = () => { this.clearPreviews(); $('classSel').classList.add('hidden'); this.showTitle(); };
    $('bStart').onclick = () => { audio.play('quest'); this.clearPreviews(); $('classSel').classList.add('hidden'); this.start(null, this.selClass); };
  }
  pickClass(k) {
    this.selClass = k; audio.play('click');
    document.querySelectorAll('.card').forEach((c) => c.classList.toggle('sel', c.dataset.k === k));
    const p = this.previews[k]; p.st.act = { name: k === 'warrior' ? 'overhead' : k === 'mage' ? 'castUp' : 'bow', t: 0 }; p.actDur = 1.2;
    if (k === 'mage') this.later(0.5, () => this.fx.burst(new THREE.Vector3(p.x, heightAt(p.x, 14) + 2.4, 14), 30, { speed: 3, life: 0.8, size: 0.25, color: [0.5, 0.6, 1] }), true);
    if (k === 'warrior') this.later(0.5, () => { this.fx.sparks(new THREE.Vector3(p.x, heightAt(p.x, 14) + 0.3, 14.8), [1, 0.8, 0.4], 20); audio.play('hit'); }, true);
    if (k === 'ranger') this.later(0.3, () => audio.play('bow'), true);
  }
  clearPreviews() { for (const k in this.previews || {}) this.scene.remove(this.previews[k].m.root); this.previews = null; }
  // ---------- oyun başlat ----------
  start(save, cls) {
    if (!save) save = { cls, level: 1, xp: 0, gold: 30, inv: [], eq: {}, mats: {}, potions: { hp: 3, mp: 2 }, quests: {}, mainQ: 'm1', crystals: [], bosses: [], ways: ['village'], chests: [], time: 0.32, playTime: 0, x: 3, z: 9, yaw: Math.PI };
    save.ways ||= ['village']; save.chests ||= []; save.bosses ||= []; save.crystals ||= []; save.quests ||= {};
    this.save = save; this.world.time = save.time ?? 0.32;
    this.player = new Player(this, save); this.camYaw = this.player.yaw + Math.PI;
    this.npcs = spawnNPCs(this);
    this.camps = CAMPS.map((c, i) => ({ ...c, id: i, list: [], respawn: 0, spawned: false, lvl: (i * 7) % 3 === 0 ? 1 : 0 }));
    this.bossState = BOSS_SPAWNS.map((b) => ({ ...b, e: null }));
    this.structs.interact.forEach((it) => { if (it.kind === 'chest' && save.chests.includes(it.id)) { it.opened = true; it.lid.rotation.x = -1.9; } });
    this.updateBarrier(true);
    this.ui.buildSkillbar();
    $('hud').classList.remove('hidden'); this.state = 'play'; this.input.enabled = true;
    this.lastRegion = null; this.saveT = 20;
    this.ui.banner('Krolasyon Köyü', 'YEŞİL VADİ', 3.5);
    if (!save.quests.m1) this.later(3.5, () => this.ui.toast('Köy meydanındaki Bilge Arda ile konuş (!)', 'gold'));
    if (!this.input.touch) $('hint').classList.remove('hidden');
    this.camTarget.copy(this.player.pos);
  }
  save_() {
    if (!this.player) return; const P = this.player, s = this.save;
    Object.assign(s, { level: P.level, xp: P.xp, gold: P.gold, inv: P.inv, eq: P.eq, mats: P.mats, potions: P.potions, upgrade: P.upgrade, x: P.pos.x, z: P.pos.z, yaw: P.yaw, hp: P.alive ? P.hp : P.stats.maxHp, mp: P.mp, time: this.world.time });
    try { localStorage.setItem(SAVE_KEY, JSON.stringify(s)); } catch (e) {}
  }
  // ---------- yardımcılar ----------
  later(t, fn, real = false) { this.timers.push({ t, fn, real }); }
  hitstop(t) { this.hitstopT = Math.max(this.hitstopT, t); }
  spawnEnemy(type, x, z, lvl = 0, camp = null) { if (this.enemies.length > 60) return null; const e = new Enemy(this, type, x, z, lvl, camp); this.enemies.push(e); return e; }
  npcName(id) { return this.npcs.find((n) => n.id === id)?.name || id; }
  // ---------- görevler ----------
  questComplete(id) {
    const Q = QUESTS[id], q = this.save.quests[id]; if (!q || q.state !== 'active') return false;
    if (Q.kill) for (const t in Q.kill) if ((q.prog[t] || 0) < Q.kill[t]) return false;
    if (Q.collect) for (const m in Q.collect) if ((this.player.mats[m] || 0) < Q.collect[m]) return false;
    return true;
  }
  availableQuests(npc) {
    const out = [];
    for (const id in QUESTS) { const Q = QUESTS[id]; if (Q.giver !== npc || this.save.quests[id]) continue; if (Q.main && this.save.mainQ !== id) continue; if (Q.minLvl && this.player.level < Q.minLvl) continue; out.push(id); }
    return out;
  }
  questMarker(npc) { for (const id in this.save.quests) if (QUESTS[id].giver === npc && this.questComplete(id)) return '?'; return this.availableQuests(npc).length ? '!' : null; }
  mainQuestMarker() { const id = this.save.mainQ; if (!id) return null; const q = this.save.quests[id]; if (!q) return this.npcs.find((n) => n.id === 'elder')?.pos; if (this.questComplete(id)) return this.npcs.find((n) => n.id === 'elder')?.pos; const m = QUESTS[id].marker; return m ? LOC[m] : null; }
  matNeeded(m) { for (const id in this.save.quests) { const q = this.save.quests[id]; if (q.state === 'active' && QUESTS[id].collect?.[m]) return true; } return false; }
  acceptQuest(id) { this.save.quests[id] = { state: 'active', prog: {} }; audio.play('quest'); this.ui.toast(`Yeni görev: ${QUESTS[id].name}`, 'gold'); this.ui.updateTracker(); this.save_(); }
  turnIn(id) {
    const Q = QUESTS[id], P = this.player; const q = this.save.quests[id]; q.state = 'done';
    if (Q.collect) for (const m in Q.collect) P.mats[m] -= Q.collect[m];
    const R = Q.reward; P.gold += R.gold || 0; if (R.potions) { P.potions.hp += R.potions; P.potions.mp += Math.ceil(R.potions / 2); }
    if (R.item) { const it = makeItem(R.item, P.level + 1, R.rarity ?? Math.max(2, rollRarity(0.2)), P.cls); if (P.inv.length < 30) P.inv.push(it); else this.dropItem(P.pos, it); this.ui.toast(`Ödül: ${it.name}`, ['', 'blue', 'purple', 'orange'][it.rarity]); }
    this.ui.banner('Görev Tamamlandı', Q.name.toUpperCase(), 3); audio.play('quest');
    P.gainXp(R.xp || 0);
    if (Q.main) { this.save.mainQ = Q.next; if (!Q.next) {} }
    this.ui.updateTracker(); this.save_();
  }
  // ---------- NPC konuşmaları ----------
  talk(npc) {
    const P = this.player, ui = this.ui; const id = npc.id;
    const LINES = {
      elder: ['Krolasyon\'un üç kristali çalındığından beri topraklar karanlığa gömülüyor. Kadim bekçiler kristalleri koruyor, ejderha ise uyanmak üzere.', 'Kuzeyde Karanlık Orman, doğuda Kızıl Çöl, batıda Buzul Zirveleri… Güneyde ise Kül Diyarı. Her birinde bir kristal ya da bir felaket bekliyor.'],
      merchant: ['En iyi mallar, en uygun fiyatlar! Bir şeye mi bakmıştın?'], smith: ['Çelik yalan söylemez. Silahını buraya getir, onu efsaneye dönüştürelim.'],
      hunter: ['Vahşi doğa acımasızdır. Kurtlar sürü halinde avlanır; birini kızdırırsan hepsi gelir.'], guard: ['Ben buradayken köye kimse zarar veremez. Ama dışarısı… orası başka.'],
    };
    const gen = ['Hava bugün güzel, değil mi?', 'Değirmen yine gıcırdıyor…', 'Geceleri ormandan tuhaf sesler geliyor.', 'Kahraman sen misin? Çok daha uzun sanıyordum!', 'Ejderhanın kükremesini duydun mu? Tüylerim diken diken oldu.'];
    const opts = [];
    for (const qid in this.save.quests) if (QUESTS[qid].giver === id && this.questComplete(qid)) opts.push({ t: `✔ Görevi teslim et: ${QUESTS[qid].name}`, q: true, fn: () => { this.turnIn(qid); ui.dialog(npc, qid === 'm6' ? 'Başardın… Ejderha öldü ve kristaller yeniden parlıyor. Krolasyon sonsuza dek sana minnettar.' : 'Harika iş çıkardın! İşte ödülün. Krolasyon sana güveniyor.', [{ t: 'Teşekkürler' }]); } });
    for (const qid of this.availableQuests(id)) opts.push({ t: `! Görev: ${QUESTS[qid].name}`, q: true, fn: () => ui.dialog(npc, QUESTS[qid].desc + `\n\nÖdül: ${QUESTS[qid].reward.xp} TP, ${QUESTS[qid].reward.gold} altın.`, [{ t: 'Kabul ediyorum', q: true, fn: () => { this.acceptQuest(qid); ui.closeDialog(); } }, { t: 'Belki sonra' }]) });
    if (npc.def.shop) opts.push({ t: '🛒 Alışveriş yap', fn: () => { ui.closeDialog(); ui.openShop(); } });
    if (npc.def.smith) opts.push({ t: '⚒️ Silahımı güçlendir', fn: () => { ui.closeDialog(); ui.openSmith(); } });
    opts.push({ t: 'Hoşça kal' });
    let text = LINES[id] ? pick(LINES[id]) : pick(gen);
    const active = Object.keys(this.save.quests).find((q) => QUESTS[q].giver === id && this.save.quests[q].state === 'active' && !this.questComplete(q));
    if (active) text = `${QUESTS[active].name} görevi nasıl gidiyor? ` + text;
    if (id === 'elder' && this.save.mainQ === 'm6' && this.save.crystals.length >= 3 && !this.save.quests.m6) text = 'Üç kristal elinde! Işıkları ejderha ininin mührünü kırabilir. Güneye git, kahraman.';
    npc.st.act = { name: 'talk', t: 0 }; audio.play('click');
    ui.dialog(npc, text, opts);
  }
  // ---------- etkileşim ----------
  nearShop() { const n = this.npcs.find((x) => x.id === 'merchant'); return n && n.pos.distanceTo(this.player.pos) < 6; }
  findInteract() {
    const P = this.player; let best = null, bd = 1e9;
    for (const n of this.npcs) { const d = n.pos.distanceTo(P.pos); if (d < 3.4 && d < bd) { bd = d; best = { kind: 'npc', n, text: `${n.name} ile konuş` }; } }
    for (const it of this.structs.interact) { const d = Math.hypot(it.x - P.pos.x, it.z - P.pos.z); if (d < it.r && d < bd && !(it.kind === 'chest' && it.opened)) { bd = d; best = { kind: it.kind, it, text: it.kind === 'chest' ? 'Sandığı aç' : `${it.name} — Işınlan` }; } }
    return best;
  }
  interact(x) {
    if (!x) return;
    if (x.kind === 'npc') this.talk(x.n);
    else if (x.kind === 'waystone') this.ui.openTravel();
    else if (x.kind === 'chest') {
      const it = x.it; it.opened = true; this.save.chests.push(it.id); audio.play('chest', it.mesh.position);
      let t = 0; this.effects.push({ update: (dt) => { t += dt; it.lid.rotation.x = -1.9 * Math.min(1, t / 0.4); return t < 0.5; } });
      const p = it.mesh.position.clone().setY(it.mesh.position.y + 0.8); this.fx.burst(p, 40, { speed: 4, up: 2, life: 1, size: 0.2, color: [1, 0.85, 0.4] }); this.fx.light(p, [1, 0.8, 0.4], 20, 0.8);
      const lvl = Math.max(this.player.level, it.level);
      this.loots.push(new Loot(this, p, 'gold', randInt(20, 40) * lvl));
      for (let i = 0; i < 2; i++) this.dropItem(p, makeItem(pick(['weapon', 'armor', 'helm', 'amulet', 'ring']), lvl, Math.max(1, rollRarity(0.15)), this.player.cls));
      if (Math.random() < 0.7) this.loots.push(new Loot(this, p, 'potion', 'hp'));
      this.save_();
    }
  }
  teleport(x, z) {
    const f = $('fade'); f.style.opacity = 1; audio.play('teleport');
    this.fx.burst(this.player.center(), 60, { speed: 4, up: 2, life: 1, size: 0.25, color: [0.4, 0.85, 1] });
    this.later(0.55, () => { this.player.pos.set(x, heightAt(x, z), z); this.player.vel.set(0, 0, 0); this.camTarget.copy(this.player.pos); this.world.updateChunks(x, z); this.fx.burst(this.player.center(), 60, { speed: 4, up: 2, life: 1, size: 0.25, color: [0.4, 0.85, 1] }); f.style.opacity = 0; }, true);
  }
  updateBarrier(init) {
    const unlocked = this.save.crystals.length >= 3; const B = this.structs.barrier;
    this.barrierCol.off = unlocked; B.mesh.visible = !unlocked;
    if (!init && unlocked) { this.ui.banner('Mühür Kırıldı', 'EJDERHA İNİ AÇIK', 4); }
  }
  // ---------- ganimet ----------
  dropItem(pos, it) { this.loots.push(new Loot(this, pos, 'item', it)); }
  pickup(l) {
    const P = this.player;
    if (l.kind === 'gold') { P.gold += l.data; audio.play('coin', null); this.fx.number(P.center(), `+${l.data} 🪙`, 'info'); }
    else if (l.kind === 'item') { if (P.inv.length >= 30) { this.ui.toast('Çanta dolu!', 'red'); return false; } P.inv.push(l.data); audio.play(l.data.rarity >= 2 ? 'rare' : 'pickup', null); this.ui.toast(`${l.data.name}`, ['', 'blue', 'purple', 'orange'][l.data.rarity]); }
    else if (l.kind === 'mat') { P.mats[l.data] = (P.mats[l.data] || 0) + 1; audio.play('pickup', null); this.ui.toast(`${MATERIALS[l.data].icon} ${MATERIALS[l.data].name} (${P.mats[l.data]})`, 'green'); }
    else if (l.kind === 'potion') { P.potions[l.data]++; audio.play('potion', null); this.ui.toast(l.data === 'hp' ? '❤️ Can İksiri' : '💧 Mana İksiri', 'red'); }
    return true;
  }
  onKill(e) {
    const P = this.player, def = e.def, G = this;
    let xp = def.xp * (1 + (e.level - def.lvl) * 0.25); const diff = P.level - e.level; if (diff > 4) xp *= Math.max(0.2, 1 - (diff - 4) * 0.2);
    P.gainXp(Math.round(xp)); this.fx.number(e.center().add(new THREE.Vector3(0, 0.6, 0)), `+${Math.round(xp)} TP`, 'info');
    const c = e.center();
    const gold = randInt(def.gold[0], def.gold[1]) * (1 + (e.level - 1) * 0.1); if (gold > 0) this.loots.push(new Loot(this, c, 'gold', Math.round(gold)));
    for (const [m, ch] of def.drops || []) if (Math.random() < ch) this.loots.push(new Loot(this, c, 'mat', m));
    if (Math.random() < 0.07) this.loots.push(new Loot(this, c, 'potion', Math.random() < 0.6 ? 'hp' : 'mp'));
    if (def.boss) {
      const min = { rare: 1, epic: 2, legendary: 3 }[def.loot] || 1; const n = def.final ? 4 : 3;
      for (let i = 0; i < n; i++) this.dropItem(c, makeItem(i === 0 ? 'weapon' : pick(['armor', 'helm', 'amulet', 'ring']), e.level, Math.max(i === 0 ? min : min - 1, rollRarity(0.2)), P.cls));
      this.save.bosses.push(e.type); this.activeBoss = null;
      this.ui.banner(`${def.name} yenildi!`, 'ZAFER', 4); audio.play('victory');
      this.fx.burst(c, 120, { speed: 12, life: 1.6, size: 0.4, color: [1, 0.8, 0.3], drag: 1.5 }); this.fx.light(c, [1, 0.8, 0.4], 60, 1.5);
      if (def.crystal && !this.save.crystals.includes(def.crystal)) { this.save.crystals.push(def.crystal); this.later(2.5, () => { this.ui.banner({ forest: 'Orman Kristali', desert: 'Çöl Kristali', snow: 'Buz Kristali' }[def.crystal], `KRİSTAL ${this.save.crystals.length}/3`, 4); audio.play('levelup'); this.updateBarrier(false); }); }
      if (def.final) { this.save.won = true; this.later(5, () => this.ui.over(true)); }
    } else if (Math.random() < 0.12) this.dropItem(c, makeItem(pick(['weapon', 'weapon', 'armor', 'helm', 'amulet', 'ring']), e.level, rollRarity(), Math.random() < 0.8 ? P.cls : pick(['warrior', 'mage', 'ranger'])));
    for (const id in this.save.quests) { const q = this.save.quests[id]; if (q.state !== 'active') continue; const Q = QUESTS[id]; const key = e.type === 'slimeBlue' ? 'slime' : e.type; if (Q.kill?.[key] != null) { q.prog[key] = (q.prog[key] || 0) + 1; if (q.prog[key] <= Q.kill[key]) this.ui.toast(`${Q.name}: ${ENEMIES[key].name} ${q.prog[key]}/${Q.kill[key]}`); if (this.questComplete(id)) { this.ui.toast(`Görev hazır: ${Q.name} → ${this.npcName(Q.giver)}`, 'green'); audio.play('quest'); } } }
    if (this.lockTarget === e) this.lockTarget = null;
    this.save_();
  }
  onPlayerDeath() { this.lockTarget = null; this.later(2.2, () => this.ui.over(false)); }
  respawn() {
    const P = this.player; P.gold = Math.floor(P.gold * 0.9);
    const w = WAYSTONES[0]; P.respawn(w.x + 3, w.z + 3); this.camTarget.copy(P.pos); this.activeBoss = null;
    for (const b of this.bossState) if (b.e && b.e.alive) { b.e.hp = b.e.maxHp; b.e.state = 'idle'; b.e.phase2 = false; b.e.pos.set(b.x, heightAt(b.x, b.z), b.z); }
    this.world.updateChunks(P.pos.x, P.pos.z); this.save_();
  }
  // ---------- düşman yönetimi ----------
  updateSpawns() {
    const P = this.player;
    for (const c of this.camps) {
      const d = Math.hypot(c.x - P.pos.x, c.z - P.pos.z);
      if (c.spawned) {
        c.list = c.list.filter((e) => e.alive);
        if (!c.list.length) { c.spawned = false; c.respawn = 100; }
        else if (d > 210 && !c.list.some((e) => e.state === 'chase')) { for (const e of c.list) { e.dispose(); this.enemies.splice(this.enemies.indexOf(e), 1); } c.list = []; c.spawned = false; }
      } else if (c.respawn <= 0 && d < 130 && d > 25) {
        c.spawned = true;
        for (let i = 0; i < c.n; i++) { const a = (i / c.n) * Math.PI * 2 + rand(0, 1), r = rand(2, c.r); let x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r; if (heightAt(x, z) < -0.8) { x = c.x; z = c.z; } const e = this.spawnEnemy(c.types[i % c.types.length], x, z, c.lvl + (Math.random() < 0.25 ? 1 : 0), c); if (e) c.list.push(e); }
      }
    }
    for (const b of this.bossState) {
      if (this.save.bosses.includes(b.type)) continue; const d = Math.hypot(b.x - P.pos.x, b.z - P.pos.z);
      if (!b.e && d < 110 && (b.type !== 'dragon' || this.save.crystals.length >= 3)) { b.e = this.spawnEnemy(b.type, b.x, b.z, 0, null); b.e.home.set(b.x, 0, b.z); }
      else if (b.e && !b.e.alive) b.e = null;
      else if (b.e && d > 200) { b.e.dispose(); this.enemies.splice(this.enemies.indexOf(b.e), 1); b.e = null; if (this.activeBoss && !this.activeBoss.alive) this.activeBoss = null; }
    }
    if (this.activeBoss && (this.activeBoss.state !== 'chase' || !this.activeBoss.alive)) { if (!this.activeBoss.alive || this.activeBoss.pos.distanceTo(P.pos) > 80) this.activeBoss = null; }
  }
  // ---------- kamera ----------
  updateCamera(rdt) {
    const P = this.player, I = this.input;
    this.camYaw += I.dYaw; this.camPitch = clamp(this.camPitch - I.dPitch, -0.35, 1.25); I.dYaw = 0; I.dPitch = 0;
    this.camDist = clamp(this.camDist + I.dZoom * 0.8, 3.2, 16); I.dZoom = 0;
    const lt = this.lockTarget;
    if (lt && lt.alive && lt.pos.distanceTo(P.pos) < 45) { const want = Math.atan2(P.pos.x - lt.pos.x, P.pos.z - lt.pos.z); this.camYaw = dampAngle(this.camYaw, want, 5, rdt); }
    else if (lt) this.lockTarget = null;
    else if (I.touch && P.st.speed > 1 && Math.abs(I.mz) > 0.3 && !this.touchCamT) { this.camYaw = dampAngle(this.camYaw, P.yaw + Math.PI, 0.8, rdt); }
    const tgt = P.pos.clone(); tgt.y += 1.65 * (P.swimming ? 0.6 : 1);
    this.camTarget.x = damp(this.camTarget.x, tgt.x, 14, rdt); this.camTarget.z = damp(this.camTarget.z, tgt.z, 14, rdt); this.camTarget.y = damp(this.camTarget.y, tgt.y, 8, rdt);
    const boss = this.activeBoss?.alive ? 1 : 0; const dist = this.camDist + boss * 3;
    const cp = Math.cos(this.camPitch), sp = Math.sin(this.camPitch);
    const cam = this.camera; cam.position.set(this.camTarget.x + Math.sin(this.camYaw) * cp * dist, this.camTarget.y + sp * dist, this.camTarget.z + Math.cos(this.camYaw) * cp * dist);
    const gh = heightAt(cam.position.x, cam.position.z) + 0.5; if (cam.position.y < gh) cam.position.y = gh;
    const look = this.camTarget.clone();
    if (lt && lt.alive) look.lerp(lt.center(), 0.3);
    cam.lookAt(look);
    const sh = this.fx.shake * this.fx.shake * this.settings.shake; if (sh > 0.001) { const t = this.time * 40; cam.position.x += noise.n2(t, 1) * sh * 0.5; cam.position.y += noise.n2(t, 7) * sh * 0.5; cam.rotation.z += noise.n2(t, 13) * sh * 0.04; }
    const fovT = 62 + (P.st.speed > 8 ? 5 : 0) + boss * 4; cam.fov = damp(cam.fov, fovT, 4, rdt); cam.updateProjectionMatrix();
    audio.listener.pos.copy(P.pos); audio.listener.right.set(Math.cos(this.camYaw), 0, -Math.sin(this.camYaw));
  }
  toggleLock() {
    if (this.lockTarget) { this.lockTarget = null; return; }
    const P = this.player; let best = null, bs = 1e9; const v = new THREE.Vector3();
    for (const e of this.enemies) { if (!e.alive) continue; const d = e.pos.distanceTo(P.pos); if (d > 35) continue; v.copy(e.center()).project(this.camera); if (v.z > 1) continue; const s = Math.hypot(v.x, v.y) * 20 + d; if (s < bs) { bs = s; best = e; } }
    this.lockTarget = best; if (best) audio.play('click');
  }
  // ---------- ana döngü ----------
  loop(now) {
    requestAnimationFrame((t) => this.loop(t));
    const rdt = Math.min(0.05, (now - this.last) / 1000 || 0.016); this.last = now;
    this.frameAvg = lerp(this.frameAvg, rdt, 0.05);
    for (let i = this.timers.length - 1; i >= 0; i--) { const t = this.timers[i]; if (t.real || (this.state === 'play' && !this.paused)) t.t -= rdt; if (t.t <= 0) { this.timers.splice(i, 1); t.fn(); } }
    if (this.state === 'loading') return;
    if (this.state === 'title' || this.state === 'classSel') this.updateMenu(rdt);
    else if (this.state === 'play') this.updatePlay(rdt);
    this.render();
    this.dynRes(rdt);
  }
  updateMenu(rdt) {
    this.time += rdt; this.world.update(rdt * 0.3, { pos: new THREE.Vector3(0, 0, 0) }, this.camera);
    if (this.state === 'title') { const a = this.time * 0.05; this.camera.position.set(Math.cos(a) * 75, heightAt(Math.cos(a) * 75, Math.sin(a) * 75) + 26, Math.sin(a) * 75); this.camera.lookAt(0, 6, 0); }
    else { const k = this.selClass; const p = this.previews?.[k]; const tx = p ? p.x : 0; this.camX = damp(this.camX ?? 0, tx * 0.7, 3, rdt); const y = heightAt(0, 14); this.camera.position.set(this.camX, y + 2.1, 20.5); this.camera.lookAt(this.camX, y + 0.9, 13); }
    for (const k in this.previews || {}) { const p = this.previews[k]; p.st.time += rdt; if (p.st.act) { p.st.act.t += rdt / 1.2; if (p.st.act.t >= 1) p.st.act = null; } animate(p.m, p.st, rdt); }
    this.world.updateChunks(0, 0);
    for (const f of this.structs.animated) f(rdt, this.time);
    for (const e of this.world.emitters) if (e.pos.distanceTo(this.camera.position) < 120) this.fx.emit(rdt, e);
    this.fx.update(rdt, rdt); this.fx.ambient(rdt, this.camera, biomeWeights(this.camera.position.x, this.camera.position.z), this.world.isNight);
  }
  updatePlay(rdt) {
    const I = this.input, P = this.player, ui = this.ui;
    const events = I.poll();
    for (const ev of events) {
      if (ev === 'menu') { if (ui.isOpen) ui.close(); else ui.openTab('Ayarlar'); continue; }
      if (ev === 'inv') { if (ui.modal?.title === 'Çanta') ui.close(); else ui.openTab('Çanta'); continue; }
      if (ev === 'quests') { if (ui.modal?.title === 'Görevler') ui.close(); else ui.openTab('Görevler'); continue; }
      if (ev === 'map') { if (ui.modal?.title === 'Harita') ui.close(); else ui.openTab('Harita'); continue; }
      if (this.paused || ui.isOpen) continue;
      if (ev === 'attack') P.attack();
      else if (ev === 'dodge') P.dodge(this.moveWorld().x, this.moveWorld().z);
      else if (ev === 'jump') P.jump();
      else if (ev.startsWith('skill')) P.useSkill(+ev[5]);
      else if (ev === 'potHp') P.drink('hp');
      else if (ev === 'potMp') P.drink('mp');
      else if (ev === 'interact') this.interact(this.findInteract());
      else if (ev === 'lock') this.toggleLock();
    }
    if (this.paused || ui.isOpen) { this.world.update(0, P, this.camera); this.updateCamera(rdt); return; }
    if (I.attackHeld && !P.action && P.alive) P.attack();
    this.hitstopT -= rdt; const ts = this.hitstopT > 0 ? 0.07 : 1; const dt = rdt * ts;
    this.time += dt; this.save.playTime = (this.save.playTime || 0) + rdt;
    P.update(dt);
    for (let i = this.enemies.length - 1; i >= 0; i--) { const e = this.enemies[i]; const d = e.pos.distanceTo(P.pos); if (d > 150 && e.alive && !e.boss) { e.root.visible = false; continue; } e.root.visible = !e.burrowed; if (!e.update(dt)) { e.dispose(); this.enemies.splice(i, 1); } }
    for (let i = this.projectiles.length - 1; i >= 0; i--) { const p = this.projectiles[i]; p.update(dt); if (!p.alive) this.projectiles.splice(i, 1); }
    for (let i = this.loots.length - 1; i >= 0; i--) if (!this.loots[i].update(dt)) this.loots.splice(i, 1);
    for (let i = this.effects.length - 1; i >= 0; i--) if (!this.effects[i].update(dt)) this.effects.splice(i, 1);
    for (const n of this.npcs) n.update(dt);
    this.spawnT = (this.spawnT || 0) - rdt; if (this.spawnT <= 0) { this.spawnT = 0.5; this.updateSpawns(); this.checkWorld(); for (const c of this.camps) c.respawn -= 0.5; }
    this.world.update(dt, P, this.camera);
    this.chunkT = (this.chunkT || 0) - rdt; if (this.chunkT <= 0) { this.chunkT = 0.3; this.world.updateChunks(P.pos.x, P.pos.z); }
    for (const f of this.structs.animated) f(dt, this.time);
    for (const e of this.world.emitters) if (e.pos.distanceTo(P.pos) < 80) this.fx.emit(dt, e);
    const w = biomeWeights(P.pos.x, P.pos.z);
    this.fx.ambient(dt, this.camera, w, this.world.isNight);
    this.updateCamera(rdt);
    this.fx.update(dt, rdt);
    ui.update(rdt);
    const x = this.findInteract(); ui.prompt(x && P.alive ? x.text : null);
    // müzik
    let combat = false; for (const e of this.enemies) if (e.alive && e.state === 'chase' && e.pos.distanceTo(P.pos) < 35) { combat = true; break; }
    this.inCombat = combat;
    audio.mode = this.activeBoss?.alive ? 'boss' : combat ? 'combat' : 'explore'; audio.biome = dominantBiome(P.pos.x, P.pos.z);
    // renk düzeni
    if (this.grade) { const g = this.grade.uniforms; g.uTint.value.setRGB(1 + w.desert * 0.06 + w.volcano * 0.1, 1 - w.volcano * 0.04, 1 + w.snow * 0.06 - w.desert * 0.05); g.uSat.value = 1.15 - w.snow * 0.1 + w.forest * 0.05; g.uVig.value = 0.35 + (P.hp / P.stats.maxHp < 0.3 ? 0.25 : 0); }
    this.saveT -= rdt; if (this.saveT <= 0) { this.saveT = 20; this.save_(); }
  }
  moveWorld() { const I = this.input, cy = this.camYaw; const fx = -Math.sin(cy), fz = -Math.cos(cy), rx = Math.cos(cy), rz = -Math.sin(cy); return { x: fx * I.mz + rx * I.mx, z: fz * I.mz + rz * I.mx }; }
  checkWorld() {
    const P = this.player;
    for (const w of WAYSTONES) if (!this.save.ways.includes(w.id) && Math.hypot(w.x - P.pos.x, w.z - P.pos.z) < 9) { this.save.ways.push(w.id); this.ui.toast(`Işınlanma taşı keşfedildi: ${w.name}`, 'blue'); audio.play('teleport'); this.save_(); }
    const b = Math.hypot(P.pos.x, P.pos.z) < 62 ? 'village' : dominantBiome(P.pos.x, P.pos.z);
    if (b !== this.lastRegion) { if (this.lastRegion) this.ui.banner(b === 'village' ? 'Krolasyon Köyü' : BIOME_NAMES[b], { village: 'GÜVENLİ BÖLGE', grass: 'SEVİYE 1-5', forest: 'SEVİYE 3-8', desert: 'SEVİYE 7-11', snow: 'SEVİYE 9-14', volcano: 'SEVİYE 12-17' }[b], 2.6); this.lastRegion = b; }
    const B = this.structs.barrier; if (this.save.crystals.length < 3 && Math.hypot(P.pos.x - B.x, P.pos.z - B.z) < 10) { this.barrierMsgT = (this.barrierMsgT || 0) - 0.5; if (this.barrierMsgT <= 0) { this.barrierMsgT = 6; this.ui.toast(`Kadim bir mühür yolu kapatıyor. Gereken kristaller: ${this.save.crystals.length}/3`, 'red'); } }
  }
  shopStock() {
    if (!this.stock || this.time - this.stockT > 300 || this.stockLvl !== this.player.level) { this.stockT = this.time; this.stockLvl = this.player.level; this.stock = []; for (let i = 0; i < 5; i++) this.stock.push(makeItem(pick(['weapon', 'armor', 'helm', 'amulet', 'ring']), this.player.level, Math.min(2, rollRarity(0.1)), this.player.cls)); }
    return this.stock;
  }
  render() {
    if (this.composer) this.composer.render(); else this.renderer.render(this.scene, this.camera);
  }
  dynRes(rdt) {
    this.drsT += rdt; if (this.drsT < 3) return; this.drsT = 0;
    const fps = 1 / this.frameAvg; const max = Math.min(devicePixelRatio || 1, this.q.pr);
    let pr = this.pixelRatio;
    if (fps < 38 && pr > 0.55) pr = Math.max(0.55, pr - 0.15); else if (fps > 57 && pr < max) pr = Math.min(max, pr + 0.1);
    if (pr !== this.pixelRatio) { this.pixelRatio = pr; this.renderer.setPixelRatio(pr); this.composer?.setPixelRatio(pr); this.resize(); }
  }
}
window.GAME = new Game();
