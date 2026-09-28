// Arayüz: HUD, mini harita, paneller (çanta, görevler, harita, ayarlar), diyaloglar, dükkan
import * as THREE from 'three';
import { CLASSES, SKILLS, RARITY, SLOTS, STAT_NAMES, statText, MATERIALS, QUESTS, ENEMIES } from './data.js';
import { HALF, SIZE, BIOME_NAMES, dominantBiome, WAYSTONES, LOC } from './terrain.js';
import { fmt, clamp } from './util.js';

const $ = (id) => document.getElementById(id);
const ICON = { weapon: { warrior: '🗡️', mage: '🪄', ranger: '🏹' }, armor: '🛡️', helm: '⛑️', amulet: '📿', ring: '💍' };
export function itemIcon(it) { return it.slot === 'weapon' ? ICON.weapon[it.cls || 'warrior'] : ICON[it.slot]; }

export class UI {
  constructor(game) {
    this.g = game; this.toastEl = $('toasts'); this.ebars = new Map(); this.modal = null; this.lastHp = 1;
    $('mClose').onclick = () => this.close();
    $('modal').addEventListener('pointerdown', (e) => { if (e.target.id === 'modal') this.close(); });
    this.mini = $('miniCanvas').getContext('2d');
  }
  // ---------- HUD ----------
  buildSkillbar() {
    const P = this.g.player, sb = $('skillbar'); sb.innerHTML = '';
    const mk = (k, icon, id) => { const d = document.createElement('div'); d.className = 'slot'; d.id = id; d.innerHTML = `<span class="k">${k}</span>${icon}<span class="c"></span><div class="cd"></div>`; sb.appendChild(d); return d; };
    this.potHp = mk('Q', '❤️', 'sPotHp'); this.potHp.classList.add('small');
    this.skillEls = P.skills.map((s, i) => { const d = mk(i + 1, s.icon, 'sk' + i); d.title = `${s.name} — ${s.desc} (${s.mp} mana, ${s.cd} sn)`; return d; });
    this.potMp = mk('R', '💧', 'sPotMp'); this.potMp.classList.add('small');
    this.tSkills = [...document.querySelectorAll('#tbtns .sk')];
    this.tSkills.forEach((b, i) => { b.innerHTML = `${P.skills[i].icon}<div class="cd"></div>`; });
    $('pName').textContent = P.C.name;
  }
  update(dt) {
    const G = this.g, P = G.player, S = P.stats;
    const hpP = clamp(P.hp / S.maxHp, 0, 1) * 100;
    $('hpBar').style.width = hpP + '%'; $('hpLag').style.width = hpP + '%'; $('hpTxt').textContent = `${Math.ceil(P.hp)} / ${S.maxHp}`;
    $('mpBar').style.width = clamp(P.mp / S.maxMp, 0, 1) * 100 + '%'; $('mpTxt').textContent = `${Math.floor(P.mp)} / ${S.maxMp}`;
    $('stBar').style.width = P.stamina + '%';
    $('lvlNum').textContent = P.level; $('xpBar').style.width = (P.level >= 20 ? 100 : (P.xp / P.xpNext()) * 100) + '%';
    $('gold').textContent = '🪙 ' + fmt(P.gold);
    $('lowhp').style.display = P.alive && hpP < 28 ? 'block' : 'none';
    let bf = ''; if (P.buff > 0) bf += `<span>📯 ${Math.ceil(P.buff)}</span>`; for (const k of ['burn', 'poison', 'slow', 'freeze', 'stun']) if (P.status[k] > 0) bf += `<span>${{ burn: '🔥', poison: '☠️', slow: '🐌', freeze: '❄️', stun: '💫' }[k]}</span>`;
    if ($('buffs').innerHTML !== bf) $('buffs').innerHTML = bf;
    // yetenekler
    const upd = (el, i) => {
      const s = P.skills[i]; const cd = P.cds[i]; const locked = P.level < s.lvl; const cdEl = el.querySelector('.cd');
      el.classList.toggle('locked', locked); el.classList.toggle('nomp', !locked && P.mp < s.mp);
      if (locked) { cdEl.style.setProperty('--p', '100%'); cdEl.textContent = 'Sv' + s.lvl; }
      else if (cd > 0) { cdEl.style.setProperty('--p', (cd / s.cd) * 100 + '%'); cdEl.textContent = cd > 1 ? Math.ceil(cd) : cd.toFixed(1); el.dataset.cd = 1; }
      else { cdEl.style.setProperty('--p', '0%'); cdEl.textContent = ''; if (el.dataset.cd === '1') { el.dataset.cd = 0; el.classList.remove('ready'); void el.offsetWidth; el.classList.add('ready'); } }
    };
    this.skillEls.forEach(upd); this.tSkills.forEach(upd);
    this.potHp.querySelector('.c').textContent = P.potions.hp || 0; this.potMp.querySelector('.c').textContent = P.potions.mp || 0;
    this.potHp.querySelector('.cd').style.setProperty('--p', (P.potionCd / 1.2) * 100 + '%'); this.potMp.querySelector('.cd').style.setProperty('--p', (P.potionCd / 1.2) * 100 + '%');
    // hedef
    const t = G.lockTarget?.alive ? G.lockTarget : G.lastHit?.alive && G.time - G.lastHitT < 5 ? G.lastHit : null;
    if (t && !t.boss) { $('target').classList.remove('hidden'); $('tName').innerHTML = `${t.def.name} <span>Sv ${t.level}${G.lockTarget === t ? ' · 🎯' : ''}</span>`; $('tBar').style.width = (t.hp / t.maxHp) * 100 + '%'; }
    else $('target').classList.add('hidden');
    const b = G.activeBoss;
    if (b && b.alive) { $('boss').classList.remove('hidden'); $('bName').textContent = b.def.name + (b.phase2 ? ' — ÖFKELİ' : ''); const p = (b.hp / b.maxHp) * 100; $('bBar').style.width = p + '%'; $('bLag').style.width = p + '%'; $('bTxt').textContent = `${fmt(b.hp)} / ${fmt(b.maxHp)}`; }
    else $('boss').classList.add('hidden');
    // saat & bölge
    const tod = G.world.time; const hh = Math.floor(((tod * 24) + 0) % 24), mm = Math.floor((tod * 24 * 60) % 60);
    $('clock').textContent = `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')} ${G.world.isNight ? '🌙' : '☀️'}`;
    this.drawMini();
    this.updateEnemyBars();
    this.trackT = (this.trackT || 0) - dt; if (this.trackT <= 0) { this.trackT = 0.5; this.updateTracker(); }
  }
  updateEnemyBars() {
    const G = this.g, cam = G.camera, v = new THREE.Vector3(); const seen = new Set();
    for (const e of G.enemies) {
      if (!e.alive || e.boss || e.hp >= e.maxHp || e.pos.distanceTo(G.player.pos) > 40) continue;
      v.copy(e.pos); v.y += e.model.height + 0.5; v.project(cam); if (v.z > 1 || Math.abs(v.x) > 1.1 || Math.abs(v.y) > 1.1) continue;
      let el = this.ebars.get(e); if (!el) { el = document.createElement('div'); el.className = 'ebar'; el.innerHTML = `<b></b><i></i>`; $('dmgLayer').appendChild(el); this.ebars.set(e, el); el.querySelector('b').textContent = `${e.def.name} · ${e.level}`; }
      seen.add(e); el.style.transform = `translate(${(v.x * 0.5 + 0.5) * innerWidth - 30}px, ${(-v.y * 0.5 + 0.5) * innerHeight}px)`; el.querySelector('i').style.width = (e.hp / e.maxHp) * 100 + '%';
    }
    for (const [e, el] of this.ebars) if (!seen.has(e)) { el.remove(); this.ebars.delete(e); }
  }
  drawMini() {
    const G = this.g, P = G.player, c = this.mini, W = 170, R = 85; const scale = 1.7; // piksel/metre
    c.save(); c.clearRect(0, 0, W, W); c.fillStyle = '#000'; c.fillRect(0, 0, W, W);
    c.translate(R, R);
    const img = G.world.mapCanvas; const px = ((P.pos.x + HALF) / SIZE) * 256, pz = ((P.pos.z + HALF) / SIZE) * 256; const k = scale * (SIZE / 256);
    c.imageSmoothingEnabled = true; c.drawImage(img, -px * k, -pz * k, 256 * k, 256 * k);
    const dot = (x, z, col, r = 3) => { const dx = (x - P.pos.x) * scale, dz = (z - P.pos.z) * scale; const d = Math.hypot(dx, dz); if (d > R - 5) return; c.fillStyle = col; c.beginPath(); c.arc(dx, dz, r, 0, 7); c.fill(); };
    const edge = (x, z, col, sym) => { let dx = (x - P.pos.x) * scale, dz = (z - P.pos.z) * scale; const d = Math.hypot(dx, dz); if (d > R - 10) { dx *= (R - 10) / d; dz *= (R - 10) / d; } c.font = 'bold 14px sans-serif'; c.textAlign = 'center'; c.textBaseline = 'middle'; c.fillStyle = '#000'; c.fillText(sym, dx + 1, dz + 1); c.fillStyle = col; c.fillText(sym, dx, dz); };
    for (const e of G.enemies) if (e.alive) dot(e.pos.x, e.pos.z, e.boss ? '#ff4020' : '#ff6a5a', e.boss ? 5 : 2.5);
    for (const n of G.npcs) if (G.questMarker(n.id)) edge(n.pos.x, n.pos.z, G.questMarker(n.id) === '?' ? '#80ff80' : '#ffd040', G.questMarker(n.id));
    for (const w of WAYSTONES) dot(w.x, w.z, G.save.ways.includes(w.id) ? '#60d0ff' : '#406070', 3.5);
    const mq = G.mainQuestMarker(); if (mq) edge(mq.x, mq.z, '#ffd040', '◆');
    // oyuncu oku
    c.rotate(-P.yaw + Math.PI); c.fillStyle = '#fff'; c.strokeStyle = '#000'; c.lineWidth = 1.5; c.beginPath(); c.moveTo(0, -8); c.lineTo(5.5, 6); c.lineTo(0, 3); c.lineTo(-5.5, 6); c.closePath(); c.fill(); c.stroke();
    c.restore();
  }
  updateTracker() {
    const G = this.g; let h = '';
    for (const id in G.save.quests) {
      const q = G.save.quests[id]; if (q.state !== 'active') continue; const Q = QUESTS[id];
      h += `<div class="q ${Q.main ? 'main' : ''}"><b>${Q.main ? '◆ ' : ''}${Q.name}</b>${this.progressLines(id).map((l) => `<div class="${l.ok ? 'ok' : ''}">${l.t}</div>`).join('')}</div>`;
    }
    if ($('tracker').innerHTML !== h) $('tracker').innerHTML = h;
    const b = dominantBiome(G.player.pos.x, G.player.pos.z); const name = Math.hypot(G.player.pos.x, G.player.pos.z) < 62 ? 'Krolasyon Köyü' : BIOME_NAMES[b];
    $('region').textContent = name;
  }
  progressLines(id) {
    const G = this.g, Q = QUESTS[id], q = G.save.quests[id]; const out = [];
    if (Q.kill) for (const t in Q.kill) { const n = Math.min(q.prog[t] || 0, Q.kill[t]); out.push({ t: `${ENEMIES[t].name}: ${n}/${Q.kill[t]}`, ok: n >= Q.kill[t] }); }
    if (Q.collect) for (const m in Q.collect) { const n = Math.min(G.player.mats[m] || 0, Q.collect[m]); out.push({ t: `${MATERIALS[m].name}: ${n}/${Q.collect[m]}`, ok: n >= Q.collect[m] }); }
    if (G.questComplete(id)) out.push({ t: `→ ${G.npcName(Q.giver)} ile konuş`, ok: true });
    return out;
  }
  // ---------- bildirimler ----------
  toast(t, cls = '') { const d = document.createElement('div'); d.className = 'toast ' + cls; d.textContent = t; this.toastEl.appendChild(d); while (this.toastEl.children.length > 5) this.toastEl.firstChild.remove(); setTimeout(() => { d.style.transition = 'opacity .5s'; d.style.opacity = 0; setTimeout(() => d.remove(), 500); }, 2600); }
  banner(t, s = '', dur = 3) { $('banT').textContent = t; $('banS').textContent = s; $('banner').classList.add('on'); clearTimeout(this.banT); this.banT = setTimeout(() => $('banner').classList.remove('on'), dur * 1000); }
  hurtFlash() { const h = $('hurt'); h.style.transition = 'none'; h.style.opacity = 1; requestAnimationFrame(() => { h.style.transition = 'opacity .45s'; h.style.opacity = 0; }); }
  bossIntro(e) { this.g.activeBoss = e; this.banner(e.def.name, 'BOSS', 3.2); }
  prompt(text) { const p = $('prompt'); if (!text) { p.classList.add('hidden'); $('tInteract').classList.add('hidden'); return; } p.classList.remove('hidden'); const html = this.g.input.touch ? text : `<kbd>E</kbd>${text}`; if (p.innerHTML !== html) p.innerHTML = html; $('tInteract').classList.remove('hidden'); }
  // ---------- modal ----------
  open(title, render, tabs = null) {
    const G = this.g; this.modal = { title, render, tabs }; $('modal').classList.remove('hidden'); $('mTitle').textContent = title;
    G.input.releaseLock(); G.paused = true; G.audio.play('open');
    const tb = $('mTabs'); if (tabs) { tb.classList.remove('hidden'); tb.innerHTML = ''; tabs.forEach(([k, name, fn]) => { const b = document.createElement('button'); b.textContent = name; b.onclick = () => { this.open(k, fn, tabs); }; if (k === title) b.classList.add('on'); tb.appendChild(b); }); } else tb.classList.add('hidden');
    this.refresh();
  }
  refresh() { if (!this.modal) return; const b = $('mBody'); b.innerHTML = ''; this.modal.render(b); }
  close() { if (!this.modal && $('dialog').classList.contains('hidden')) return false; this.modal = null; $('modal').classList.add('hidden'); this.closeDialog(); this.g.paused = false; return true; }
  get isOpen() { return !!this.modal || !$('dialog').classList.contains('hidden'); }
  mainTabs() { return [['Çanta', (b) => this.renderInv(b)], ['Görevler', (b) => this.renderQuests(b)], ['Harita', (b) => this.renderMap(b)], ['Ayarlar', (b) => this.renderSettings(b)]]; }
  openTab(name) { const t = this.mainTabs(); const f = t.find((x) => x[0] === name); this.open(name, f[1], t); }
  // ---------- çanta ----------
  renderInv(b) {
    const G = this.g, P = G.player, S = P.stats; let sel = this.sel;
    b.innerHTML = `<div class="inv"><div><h3 style="font-size:15px;color:var(--bronze);margin-bottom:8px">Kuşanılanlar</h3><div class="eq" id="eqg"></div>
      <div class="stats">
        <span>Sınıf</span><span>${P.C.name} · Seviye ${P.level}</span><span>Can</span><span>${Math.ceil(P.hp)} / ${S.maxHp}</span><span>Mana</span><span>${Math.floor(P.mp)} / ${S.maxMp}</span>
        <span>Saldırı</span><span>${S.atk}${P.upgrade ? ` (+${P.upgrade})` : ''}</span><span>Zırh</span><span>${S.armor} (%${Math.round(100 - 10000 / (100 + S.armor * 2.5))} azaltma)</span>
        <span>Kritik Şans</span><span>%${(S.crit * 100).toFixed(0)}</span><span>Kritik Hasar</span><span>%${(S.critDmg * 100).toFixed(0)}</span><span>Hız</span><span>${S.speed.toFixed(1)} m/s</span>
        <span>Deneyim</span><span>${fmt(P.xp)} / ${fmt(P.xpNext())}</span><span>Altın</span><span>🪙 ${fmt(P.gold)}</span><span>İksirler</span><span>❤️ ${P.potions.hp} · 💧 ${P.potions.mp}</span>
        <span>Kristaller</span><span>${['forest', 'desert', 'snow'].map((c) => G.save.crystals.includes(c) ? { forest: '💚', desert: '💛', snow: '💙' }[c] : '▫️').join(' ')}</span>
      </div>
      <h3 style="font-size:15px;color:var(--bronze);margin:14px 0 8px">Malzemeler</h3><div class="grid" id="matg"></div></div>
      <div><h3 style="font-size:15px;color:var(--bronze);margin-bottom:8px">Çanta (${P.inv.length}/30)</h3><div class="grid" id="invg"></div><div class="tip" id="tip"><span class="m">Bir eşyaya dokun ya da tıkla. Kuşanmak için "Kuşan"a bas.</span></div></div></div>`;
    const cell = (it, slotName) => { const d = document.createElement('div'); d.className = 'it ' + (it ? 'r' + it.rarity : 'empty'); d.innerHTML = it ? `${itemIcon(it)}<span class="lv">${it.lvl}</span>` : `<span class="sl">${slotName}</span>`; return d; };
    for (const s in SLOTS) { const it = P.eq[s]; const d = cell(it, SLOTS[s]); if (it) d.onclick = () => this.showTip(it, true); $('eqg').appendChild(d); }
    P.inv.forEach((it) => { const d = cell(it); d.onclick = () => this.showTip(it, false); $('invg').appendChild(d); });
    for (let i = P.inv.length; i < 30; i++) { const d = document.createElement('div'); d.className = 'it empty'; $('invg').appendChild(d); }
    for (const m in MATERIALS) { if (!P.mats[m]) continue; const d = document.createElement('div'); d.className = 'it'; d.title = MATERIALS[m].name; d.innerHTML = `${MATERIALS[m].icon}<span class="lv">${P.mats[m]}</span>`; $('matg').appendChild(d); }
  }
  itemHTML(it, cmp) {
    const R = RARITY[it.rarity]; let h = `<h4 style="color:${R.color}">${it.name}</h4><div class="m">${R.name} ${SLOTS[it.slot]} · Seviye ${it.lvl}${it.slot === 'weapon' ? ' · ' + CLASSES[it.cls].name : ''}</div>`;
    for (const k in it.stats) { const d = cmp ? it.stats[k] - (cmp.stats[k] || 0) : 0; h += `<div class="s">${statText(k, it.stats[k])}${cmp && d !== 0 ? ` <span class="${d > 0 ? 's' : 'worse'}">(${d > 0 ? '▲' : '▼'}${k === 'crit' || k === 'critDmg' || k === 'speed' ? Math.abs(d * 100).toFixed(0) + '%' : Math.abs(d)})</span>` : ''}</div>`; }
    if (cmp) for (const k in cmp.stats) if (!(k in it.stats)) h += `<div class="worse">−${statText(k, cmp.stats[k]).slice(1)}</div>`;
    h += `<div class="m">Değer: 🪙 ${it.value}</div>`; return h;
  }
  showTip(it, equipped) {
    const G = this.g, P = G.player; const tip = $('tip'); const cur = P.eq[it.slot];
    tip.innerHTML = this.itemHTML(it, equipped ? null : cur) + `<div class="acts"></div>`;
    const acts = tip.querySelector('.acts');
    const btn = (t, fn, cls = '') => { const b = document.createElement('button'); b.className = 'btn ' + cls; b.textContent = t; b.onclick = fn; acts.appendChild(b); };
    if (equipped) btn('Çıkar', () => { if (P.inv.length >= 30) return this.toast('Çanta dolu'); delete P.eq[it.slot]; P.inv.push(it); P.recalc(); if (it.slot === 'weapon') P.attachWeapon(); G.audio.play('click'); this.refresh(); });
    else {
      const wrong = it.slot === 'weapon' && it.cls !== P.cls;
      if (!wrong) btn('Kuşan', () => { const i = P.inv.indexOf(it); P.inv.splice(i, 1); if (cur) P.inv.push(cur); P.eq[it.slot] = it; P.recalc(); if (it.slot === 'weapon') P.attachWeapon(); G.audio.play('metal', null); G.save_(); this.refresh(); }, 'gold');
      else acts.insertAdjacentHTML('beforeend', `<span class="worse" style="align-self:center">${CLASSES[it.cls].name} sınıfına ait</span>`);
      if (G.nearShop()) btn(`Sat (🪙 ${it.value})`, () => { P.inv.splice(P.inv.indexOf(it), 1); P.gold += it.value; G.audio.play('coin'); this.refresh(); });
      btn('At', () => { P.inv.splice(P.inv.indexOf(it), 1); this.refresh(); });
    }
  }
  // ---------- görevler ----------
  renderQuests(b) {
    const G = this.g; let h = '<div class="qlist">'; let any = false;
    for (const id in G.save.quests) { const q = G.save.quests[id]; const Q = QUESTS[id]; any = true; h += `<div class="qitem ${q.state === 'done' ? 'done' : ''}"><h4>${Q.main ? '◆ ' : ''}${Q.name}${q.state === 'done' ? ' ✔' : ''}</h4><p>${Q.desc}</p>${q.state === 'active' ? `<div class="pr">${this.progressLines(id).map((l) => l.t).join(' · ')}</div><div class="pr">Ödül: ${Q.reward.xp} TP · 🪙 ${Q.reward.gold}${Q.reward.item ? ' · eşya' : ''}${Q.reward.potions ? ` · ${Q.reward.potions} iksir` : ''}</div>` : ''}</div>`; }
    if (!any) h += '<p style="color:var(--muted)">Henüz görevin yok. Köydeki Bilge Arda ile konuş (sarı ! işareti).</p>';
    b.innerHTML = h + '</div>';
  }
  // ---------- harita ----------
  renderMap(b) {
    const G = this.g, P = G.player; b.innerHTML = `<canvas id="mapCanvas" width="680" height="680"></canvas><div class="maplegend"><span>⬆ Sen</span><span style="color:#60d0ff">● Işınlanma taşı</span><span style="color:#ffd040">◆ Ana görev</span><span style="color:#ff5030">☠ Boss</span></div>`;
    const c = $('mapCanvas').getContext('2d'), S = 680, k = S / SIZE; c.drawImage(G.world.mapCanvas, 0, 0, S, S);
    const toM = (x, z) => [(x + HALF) * k, (z + HALF) * k];
    c.font = '700 15px Cinzel, serif'; c.textAlign = 'center'; c.fillStyle = 'rgba(255,240,210,.9)'; c.strokeStyle = 'rgba(0,0,0,.8)'; c.lineWidth = 3;
    const label = (t, x, z) => { const [a, bb] = toM(x, z); c.strokeText(t, a, bb); c.fillText(t, a, bb); };
    label('Karanlık Orman', 0, -300); label('Kızıl Çöl', 330, -40); label('Buzul Zirveleri', -330, -40); label('Kül Diyarı', 0, 250); label('Krolasyon', 0, 30);
    for (const w of WAYSTONES) { const [a, bb] = toM(w.x, w.z); c.fillStyle = G.save.ways.includes(w.id) ? '#60d0ff' : '#304050'; c.beginPath(); c.arc(a, bb, 6, 0, 7); c.fill(); c.strokeStyle = '#000'; c.lineWidth = 1.5; c.stroke(); }
    const bosses = [['orcChief', LOC.orcCamp], ['treant', LOC.forestBoss], ['scorpionKing', LOC.desertBoss], ['frostGiant', LOC.snowBoss], ['dragon', LOC.dragonLair]];
    c.font = '18px sans-serif'; for (const [t, l] of bosses) { if (G.save.bosses.includes(t)) continue; const [a, bb] = toM(l.x, l.z); c.fillText('☠', a, bb + 6); }
    const mq = G.mainQuestMarker(); if (mq) { const [a, bb] = toM(mq.x, mq.z); c.fillStyle = '#ffd040'; c.font = '22px sans-serif'; c.fillText('◆', a, bb - 10); }
    const [px, pz] = toM(P.pos.x, P.pos.z); c.save(); c.translate(px, pz); c.rotate(-P.yaw + Math.PI); c.fillStyle = '#fff'; c.strokeStyle = '#000'; c.lineWidth = 2; c.beginPath(); c.moveTo(0, -11); c.lineTo(8, 8); c.lineTo(0, 4); c.lineTo(-8, 8); c.closePath(); c.fill(); c.stroke(); c.restore();
  }
  // ---------- ayarlar ----------
  renderSettings(b) {
    const G = this.g, s = G.settings;
    b.innerHTML = `<div class="set">
      <label for="sQ">Grafik kalitesi</label><select id="sQ"><option value="low">Düşük (telefon)</option><option value="medium">Orta</option><option value="high">Yüksek</option></select>
      <label for="sM">Ana ses</label><input id="sM" type="range" min="0" max="1" step="0.05">
      <label for="sMu">Müzik</label><input id="sMu" type="range" min="0" max="1" step="0.05">
      <label for="sS">Efektler</label><input id="sS" type="range" min="0" max="1" step="0.05">
      <label for="sSe">Kamera hassasiyeti</label><input id="sSe" type="range" min="0.3" max="2.5" step="0.1">
      <label for="sSh">Ekran sarsıntısı</label><input id="sSh" type="range" min="0" max="1.5" step="0.1">
    </div><p class="m" style="color:var(--muted);font-size:13px">Kalite değişikliği gölge, çimen ve çizim mesafesini etkiler; bazı değişiklikler oyunu yeniden açınca tam uygulanır.</p>
    <div class="keys"><kbd>WASD</kbd><span>Hareket (Shift ile koş)</span><kbd>Fare</kbd><span>Kamera (oyuna tıklayınca kilitlenir)</span><kbd>Sol tık</kbd><span>Saldırı / kombo (basılı tut)</span><kbd>Sağ tık · F</kbd><span>Takla (büyücü: ışınlanma)</span><kbd>Boşluk</kbd><span>Zıpla</span><kbd>1 2 3 4</kbd><span>Yetenekler</span><kbd>Q · R</kbd><span>Can / mana iksiri</span><kbd>E</kbd><span>Konuş, sandık aç, ışınlan</span><kbd>Tab</kbd><span>Hedef kilitle</span><kbd>I · J · M</kbd><span>Çanta, görevler, harita</span></div>
    <div class="row" style="margin-top:16px"><button class="btn" id="sSave">Oyunu Kaydet</button><button class="btn" id="sTitle">Ana Menüye Dön</button></div>`;
    const bind = (id, key, fn) => { const el = $(id); el.value = s[key]; el.oninput = () => { s[key] = el.type === 'range' ? +el.value : el.value; fn?.(); G.saveSettings(); }; };
    bind('sQ', 'quality', () => G.applyQuality()); bind('sM', 'master', () => G.applyVolume()); bind('sMu', 'music', () => G.applyVolume()); bind('sS', 'sfx', () => G.applyVolume()); bind('sSe', 'sens', () => (G.input.sens = s.sens)); bind('sSh', 'shake');
    $('sSave').onclick = () => { G.save_(); this.toast('Oyun kaydedildi', 'green'); };
    $('sTitle').onclick = () => { G.save_(); location.reload(); };
  }
  // ---------- diyalog ----------
  dialog(npc, text, opts) {
    const G = this.g; G.input.releaseLock(); G.paused = true; this.talkNpc = npc; if (npc) npc.talking = true;
    $('dialog').classList.remove('hidden'); $('dWho').innerHTML = npc ? `${npc.def.name}<span>${npc.def.title || ''}</span>` : '';
    $('dTxt').textContent = text; const o = $('dOpts'); o.innerHTML = '';
    for (const op of opts) { const b = document.createElement('button'); b.textContent = op.t; if (op.q) b.className = 'q'; b.onclick = () => { G.audio.play('click'); op.fn ? op.fn() : this.closeDialog(); }; o.appendChild(b); }
  }
  closeDialog() { $('dialog').classList.add('hidden'); if (this.talkNpc) this.talkNpc.talking = false; this.talkNpc = null; if (!this.modal) this.g.paused = false; }
  // ---------- dükkan ----------
  openShop() {
    const G = this.g, P = G.player;
    this.open('Tüccar Selin', (b) => {
      const stock = G.shopStock();
      b.innerHTML = `<div class="shop"><div><h3 style="font-size:15px;color:var(--bronze);margin-bottom:8px">Satılık (🪙 ${fmt(P.gold)})</h3><div id="buy"></div></div><div><h3 style="font-size:15px;color:var(--bronze);margin-bottom:8px">Sat</h3><div id="sell"></div><div class="tip" id="tip"><span class="m">Çantadaki eşyaları buradan satabilirsin.</span></div></div></div>`;
      const row = (html, price, fn, dis) => { const d = document.createElement('div'); d.className = 'shopRow'; d.innerHTML = `<span class="nm">${html}</span><span class="pr">🪙 ${price}</span>`; const bt = document.createElement('button'); bt.className = 'btn'; bt.textContent = 'Al'; bt.disabled = dis || P.gold < price; bt.onclick = fn; d.appendChild(bt); return d; };
      const buy = $('buy');
      buy.appendChild(row('❤️ Can İksiri', 25, () => { P.gold -= 25; P.potions.hp++; G.audio.play('coin'); this.refresh(); }));
      buy.appendChild(row('💧 Mana İksiri', 20, () => { P.gold -= 20; P.potions.mp++; G.audio.play('coin'); this.refresh(); }));
      stock.forEach((it) => { const price = it.value * 3; buy.appendChild(row(`<span style="color:${RARITY[it.rarity].color}">${itemIcon(it)} ${it.name}</span> <span style="color:var(--muted);font-size:12px">Sv${it.lvl}</span>`, price, () => { if (P.inv.length >= 30) return this.toast('Çanta dolu'); P.gold -= price; P.inv.push(it); stock.splice(stock.indexOf(it), 1); G.audio.play('coin'); this.refresh(); })); });
      const sell = $('sell');
      P.inv.forEach((it) => { const d = document.createElement('div'); d.className = 'shopRow'; d.innerHTML = `<span class="nm" style="color:${RARITY[it.rarity].color}">${itemIcon(it)} ${it.name}</span><span class="pr">🪙 ${it.value}</span>`; const bt = document.createElement('button'); bt.className = 'btn'; bt.textContent = 'Sat'; bt.onclick = () => { P.inv.splice(P.inv.indexOf(it), 1); P.gold += it.value; G.audio.play('coin'); this.refresh(); }; d.appendChild(bt); d.querySelector('.nm').onmouseenter = () => ($('tip').innerHTML = this.itemHTML(it, P.eq[it.slot])); sell.appendChild(d); });
      for (const m in MATERIALS) if (P.mats[m] && !G.matNeeded(m)) { const v = MATERIALS[m].value; const d = document.createElement('div'); d.className = 'shopRow'; d.innerHTML = `<span class="nm">${MATERIALS[m].icon} ${MATERIALS[m].name} ×${P.mats[m]}</span><span class="pr">🪙 ${v}</span>`; const bt = document.createElement('button'); bt.className = 'btn'; bt.textContent = 'Hepsini Sat'; bt.onclick = () => { P.gold += v * P.mats[m]; P.mats[m] = 0; G.audio.play('coin'); this.refresh(); }; d.appendChild(bt); sell.appendChild(d); }
      if (!P.inv.length) sell.insertAdjacentHTML('afterbegin', '<p style="color:var(--muted);font-size:14px">Satacak eşya yok.</p>');
    });
  }
  openSmith() {
    const G = this.g, P = G.player;
    this.open('Demirci Bora', (b) => {
      const w = P.eq.weapon; const cost = Math.round(60 * Math.pow(1.6, P.upgrade)); const max = 10;
      b.innerHTML = `<p style="max-width:60ch;line-height:1.5">“Silahını örse koy, gerisini bana bırak. Her dövüşte daha keskin olur.”</p>
        <div class="shopRow"><span class="nm">⚒️ Silahı güçlendir: <b>+${P.upgrade} → +${P.upgrade + 1}</b> <span style="color:var(--muted)">(+%8 saldırı)</span></span><span class="pr">🪙 ${cost}</span></div>
        <div class="row"><button class="btn gold" id="up" ${P.gold < cost || P.upgrade >= max ? 'disabled' : ''}>Güçlendir</button><span style="color:var(--muted)">${w ? w.name : 'Başlangıç silahı'} · Saldırı ${P.stats.atk}</span></div>`;
      $('up').onclick = () => { P.gold -= cost; P.upgrade++; P.recalc(); P.attachWeapon(); G.audio.play('metal'); G.audio.play('levelup'); G.fx.sparks(P.center(), [1, 0.7, 0.3], 30); this.toast(`Silah +${P.upgrade} oldu!`, 'gold'); G.save_(); this.refresh(); };
    });
  }
  openTravel() {
    const G = this.g;
    this.open('Işınlanma Taşı', (b) => {
      b.innerHTML = '<p style="color:var(--muted)">Keşfettiğin taşlar arasında anında yolculuk edebilirsin.</p>';
      for (const w of WAYSTONES) { const found = G.save.ways.includes(w.id); const d = document.createElement('div'); d.className = 'shopRow'; d.innerHTML = `<span class="nm">${found ? '🔷' : '▫️'} ${w.name}</span>`; const bt = document.createElement('button'); bt.className = 'btn'; bt.textContent = found ? 'Işınlan' : 'Keşfedilmedi'; bt.disabled = !found; bt.onclick = () => { this.close(); G.teleport(w.x + 3, w.z + 3); }; d.appendChild(bt); b.appendChild(d); }
    });
  }
  // ---------- ölüm / zafer ----------
  over(win) {
    const o = $('over'); o.classList.remove('hidden'); o.classList.toggle('win', win); const G = this.g;
    o.innerHTML = win ? `<h1>ZAFER</h1><p>Kızıl Ejderha Ignarok düştü. Üç kristalin ışığı diyarı yeniden sarıyor. Krolasyon köyü senin adını şarkılarla anacak.</p><p style="color:var(--muted);font-size:15px">Seviye ${G.player.level} · 🪙 ${fmt(G.player.gold)} · Oyun süresi ${Math.round(G.save.playTime / 60)} dk</p><button class="btn gold" id="oBtn">Maceraya Devam Et</button>`
      : `<h1>ÖLDÜN</h1><p>Karanlık seni yuttu… ama kahramanlar geri döner. Köydeki ışınlanma taşında yeniden doğacaksın (altınının %10'unu kaybedersin).</p><button class="btn gold" id="oBtn">Yeniden Doğ</button>`;
    G.input.releaseLock();
    $('oBtn').onclick = () => { o.classList.add('hidden'); if (!win) G.respawn(); };
  }
}
