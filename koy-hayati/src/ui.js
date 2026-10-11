// Arayüz: HUD, alet çubuğu, paneller (dükkânlar, çanta, harita...)
import { ITEMS, CROPS, SPECIES, TOOLS, UPGRADES, POTS, RECIPES, SEASON_INFO } from './data.js';
import { WEATHER, SEASONS } from './sky.js';
import { PLACES, ROADS, RIVER, HALF, WORLD } from './layout.js';
import { heightAt } from './terrain.js';

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

const CAPACITY = { coop: 12, pasture: 14, pigpen: 4 };
const AREA_OF = { tavuk: 'coop', horoz: 'coop', kaz: 'coop', tavsan: 'coop', domuz: 'pigpen' };
const SEASON_NAMES = ['İlkbahar', 'Yaz', 'Sonbahar', 'Kış'];

export class UI {
  constructor(game, audio, hooks) {
    this.g = game; this.audio = audio; this.hooks = hooks;
    this.panelOpen = false;
    $('pClose').addEventListener('click', () => this.close());
    $('panel').addEventListener('click', (e) => { if (e.target.id === 'panel') this.close(); });
    this.buildCompass();
    this.discTimer = 0;
  }
  // ------------------------------------------------ HUD
  buildHotbar(onSelect) {
    const hb = $('hotbar');
    hb.innerHTML = '';
    TOOLS.forEach((t, i) => {
      const d = document.createElement('div');
      d.className = 'slot';
      d.innerHTML = `<span class="k">${i + 1}</span><span>${t.icon}</span><span class="sub"></span><small></small>`;
      d.title = t.name;
      d.addEventListener('click', () => onSelect(i));
      d.addEventListener('touchstart', (e) => { onSelect(i); e.preventDefault(); }, { passive: false });
      hb.appendChild(d);
    });
    this.slots = [...hb.children];
  }
  updateHotbar() {
    const s = this.g.s;
    this.slots.forEach((el, i) => {
      el.classList.toggle('sel', s.tool === i);
      const small = el.querySelector('small'), sub = el.querySelector('.sub');
      small.textContent = ''; sub.textContent = '';
      if (i === 2) small.textContent = s.water;
      if (i === 3) {
        const it = ITEMS[s.seed];
        sub.textContent = it ? it.sub : '';
        small.textContent = this.g.count(s.seed);
      }
    });
  }
  updateHud(place) {
    const g = this.g;
    $('hDate').textContent = g.dateString();
    $('hTime').textContent = g.timeString();
    const w = WEATHER[g.s.weather] || WEATHER.clear;
    $('hWeather').textContent = w.icon;
    $('hWeather').title = w.name;
    $('hMoney').textContent = `${g.s.money.toLocaleString('tr-TR')} ₺`;
    $('hEnergy').style.width = `${(g.s.energy / g.maxEnergy) * 100}%`;
    $('hEnergy').style.background = g.s.energy < 20 ? 'linear-gradient(90deg,#d9532f,#f2a04a)' : '';
    $('place').textContent = `📍 ${place} · ⚡ ${Math.round(g.s.energy)}/${g.maxEnergy}`;
  }
  buildCompass() {
    const dirs = ['K', '·', 'KD', '·', 'D', '·', 'GD', '·', 'G', '·', 'GB', '·', 'B', '·', 'KB', '·'];
    let html = '';
    for (let k = 0; k < 3; k++) for (const d of dirs) html += `<span>${d}</span>`;
    $('cStrip').innerHTML = html;
  }
  updateCompass(bearingDeg) {
    const per = 40 / 22.5; // px/derece
    const w = $('compass').clientWidth;
    const off = -(bearingDeg + 360) * per + w / 2 - 20;
    $('cStrip').style.transform = `translateX(${off}px)`;
  }
  prompt(text, key = 'E') {
    const el = $('prompt');
    if (!text) { el.classList.add('hidden'); return; }
    el.classList.remove('hidden');
    el.innerHTML = `<kbd>${key}</kbd>${esc(text)}`;
  }
  toolHint(text, key) {
    const el = $('toolHint');
    if (!text) { el.classList.add('hidden'); return; }
    el.classList.remove('hidden');
    el.innerHTML = `<kbd>${key}</kbd>${esc(text)}`;
  }
  vehHint(text) {
    const el = $('vehHint');
    if (!text) { el.classList.add('hidden'); return; }
    el.classList.remove('hidden'); el.textContent = text;
  }
  toast(msg, kind = '') {
    const el = document.createElement('div');
    el.className = 'toast ' + kind;
    el.textContent = msg;
    $('toasts').appendChild(el);
    while ($('toasts').children.length > 5) $('toasts').firstChild.remove();
    setTimeout(() => { el.style.transition = 'opacity .5s'; el.style.opacity = '0'; }, 3800);
    setTimeout(() => el.remove(), 4400);
  }
  discover(name) {
    $('discName').textContent = name;
    const d = $('discover');
    d.style.opacity = '1';
    clearTimeout(this.discT);
    this.discT = setTimeout(() => { d.style.opacity = '0'; }, 3200);
  }
  fade(on, text = '') {
    const f = $('fade');
    f.textContent = text;
    f.style.opacity = on ? '1' : '0';
  }

  // ------------------------------------------------ panel altyapısı
  open(title, render) {
    this.panelOpen = true;
    this.render = render;
    $('pTitle').textContent = title;
    $('panel').classList.remove('hidden');
    this.refresh();
    if (this.hooks.onPanel) this.hooks.onPanel(true);
  }
  refresh() {
    if (!this.panelOpen) return;
    $('pMoney').textContent = `${this.g.s.money.toLocaleString('tr-TR')} ₺`;
    const body = $('pBody');
    const scroll = body.scrollTop;
    body.innerHTML = '';
    this.render(body);
    body.scrollTop = scroll;
  }
  close() {
    if (!this.panelOpen) return;
    this.panelOpen = false;
    $('panel').classList.add('hidden');
    if (this.hooks.onPanel) this.hooks.onPanel(false);
  }
  el(html) { const d = document.createElement('div'); d.innerHTML = html.trim(); return d.firstChild; }
  itemRow(icon, name, desc, buttons, sub = '') {
    const row = this.el(`<div class="item"><div class="ic">${icon}${sub ? `<span class="sub">${sub}</span>` : ''}</div><div class="nm"><b>${esc(name)}</b><span>${desc}</span></div><div class="act"></div></div>`);
    const act = row.querySelector('.act');
    for (const b of buttons) {
      const btn = document.createElement('button');
      btn.className = 'btn ' + (b.cls || '');
      btn.textContent = b.label;
      btn.disabled = !!b.disabled;
      btn.addEventListener('click', () => { b.fn(); this.refresh(); this.hooks.onChange && this.hooks.onChange(); });
      act.appendChild(btn);
    }
    return row;
  }
  tabs(body, names, cur, onPick) {
    const t = this.el('<div class="tabs"></div>');
    names.forEach((n, i) => {
      const b = document.createElement('button');
      b.textContent = n; if (i === cur) b.className = 'on';
      b.addEventListener('click', () => onPick(i));
      t.appendChild(b);
    });
    body.appendChild(t);
  }

  // ------------------------------------------------ satış (pazar / çiftlik tezgâhı)
  openSell(where) {
    const g = this.g;
    const title = where === 'farm' ? 'Çiftlik Tezgâhı' : 'Kasaba Pazarı';
    this.open(title, (body) => {
      body.appendChild(this.el(`<p class="note">${where === 'farm' ? 'Burada satış fiyatın %75. Daha iyi fiyat için kasaba pazarına götür.' : 'Fiyatlar mevsime ve güne göre değişir. ⬆ pahalı, ⬇ ucuz.'}</p>`));
      const list = this.el('<div class="list"></div>');
      const ids = Object.keys(g.s.inv).filter((k) => ITEMS[k] && ITEMS[k].cat !== 'tohum' && g.count(k) > 0);
      if (!ids.length) list.appendChild(this.el('<p class="note">Satacak bir şeyin yok. Hasat et, balık tut, odun kes!</p>'));
      let total = 0;
      for (const id of ids) {
        const it = ITEMS[id];
        const p = g.sellPrice(id, where);
        const m = g.priceMult(id);
        const tag = m > 1.15 ? ' ⬆' : m < 0.95 ? ' ⬇' : '';
        total += p * g.count(id);
        list.appendChild(this.itemRow(it.icon, `${it.name} ×${g.count(id)}`, `${p} ₺ / adet${tag}`, [
          { label: 'Sat 1', fn: () => { g.sell(id, 1, where); this.audio.coin(); } },
          { label: 'Hepsi', cls: 'gold', fn: () => { const e = g.sell(id, g.count(id), where); this.audio.coin(); this.toast(`+${e} ₺`, 'gold'); } },
        ]));
      }
      if (ids.length > 1) {
        const all = this.itemRow('💰', 'Hepsini sat', `Toplam yaklaşık ${total} ₺`, [{ label: 'Hepsini Sat', cls: 'gold', fn: () => {
          let e = 0; for (const id of ids) e += g.sell(id, g.count(id), where);
          this.audio.coin(); this.toast(`+${e} ₺ kazandın`, 'gold');
        } }]);
        body.appendChild(all);
      }
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ tohumcu
  openSeeds() {
    const g = this.g;
    let tab = 0;
    const render = (body) => {
      this.tabs(body, ['Tohumlar', 'Yem & Malzeme'], tab, (i) => { tab = i; this.refresh(); });
      const list = this.el('<div class="list"></div>');
      if (tab === 0) {
        body.appendChild(this.el(`<p class="note">Şu an: <b>${SEASON_NAMES[g.season]}</b>. Mevsimi dışında ekilen tohum sadece serada büyür.</p>`));
        for (const [k, c] of Object.entries(CROPS)) {
          const id = k + '_t';
          const ok = c.seasons.includes(g.season);
          const desc = `${c.seasons.map((x) => SEASON_NAMES[x]).join(', ')} · ${c.days} gün · satış ~${c.price} ₺${c.regrow ? ' · yeniden verir' : ''}${ok ? '' : ' · <b>mevsimi değil</b>'}`;
          list.appendChild(this.itemRow('🌱', `${c.name} Tohumu (${g.count(id)})`, desc, [
            { label: `1 · ${c.seedPrice}₺`, disabled: g.s.money < c.seedPrice, fn: () => { if (g.spend(c.seedPrice)) g.add(id, 1); } },
            { label: `10 · ${c.seedPrice * 10}₺`, cls: 'alt', disabled: g.s.money < c.seedPrice * 10, fn: () => { if (g.spend(c.seedPrice * 10)) { g.add(id, 10); this.audio.coin(); } } },
          ], c.icon));
        }
      } else {
        body.appendChild(this.el('<p class="note">Kışın hayvanlar otlayamaz: büyükbaşlar <b>saman</b>, kümes hayvanları ve domuz <b>yem</b> yer. Gübre ekini %50 hızlandırır.</p>'));
        for (const id of ['yem', 'saman', 'gubre']) {
          const it = ITEMS[id], p = g.buyPrice(id);
          list.appendChild(this.itemRow(it.icon, `${it.name} (${g.count(id)})`, `${p} ₺`, [
            { label: `1`, disabled: g.s.money < p, fn: () => { if (g.spend(p)) g.add(id, 1); } },
            { label: `10 · ${p * 10}₺`, cls: 'alt', disabled: g.s.money < p * 10, fn: () => { if (g.spend(p * 10)) { g.add(id, 10); this.audio.coin(); } } },
          ]));
        }
      }
      body.appendChild(list);
    };
    this.open('Tohumcu & Yemci', render);
  }

  // ------------------------------------------------ hayvan pazarı
  openAnimals() {
    const g = this.g;
    let tab = 0;
    const render = (body) => {
      this.tabs(body, ['Satın Al', 'Hayvanlarım'], tab, (i) => { tab = i; this.refresh(); });
      const list = this.el('<div class="list"></div>');
      const count = (area) => g.s.animals.filter((a) => (AREA_OF[a.kind] || 'pasture') === area).length;
      if (tab === 0) {
        body.appendChild(this.el(`<p class="note">Kümes ${count('coop')}/${CAPACITY.coop} · Mera ${count('pasture')}/${CAPACITY.pasture} · Domuz ağılı ${count('pigpen')}/${CAPACITY.pigpen}. Aldığın hayvan çiftliğine teslim edilir.</p>`));
        for (const [k, sp] of Object.entries(SPECIES)) {
          const area = AREA_OF[k] || 'pasture';
          const full = count(area) >= CAPACITY[area];
          const prod = sp.product ? `${ITEMS[sp.product].icon} ${ITEMS[sp.product].name}${sp.every > 1 ? ` (${sp.every} günde bir)` : ''}` : sp.note;
          list.appendChild(this.itemRow(sp.icon, sp.name, `${sp.price} ₺ · ${prod}${sp.note && sp.product ? '' : ''}${full ? ' · <b>yer yok</b>' : ''}`, [
            { label: 'Satın Al', disabled: full || g.s.money < sp.price, fn: () => {
              if (!g.spend(sp.price)) return;
              const names = ['Sarıkız', 'Karabaş', 'Benek', 'Paşa', 'Pamuk', 'Nazlı', 'Duman', 'Tarçın', 'Fındık', 'Boncuk', 'Yıldız', 'Çakıl', 'Limon', 'Zeytin'];
              g.s.animals.push({ id: g.s.nextAnimalId++, kind: k, name: names[Math.floor(Math.random() * names.length)], age: 6, happy: 55, fed: 1, ready: 0, last: g.s.day });
              this.audio.animal(k); this.toast(`${sp.icon} ${sp.name} çiftliğine gönderildi!`);
              this.hooks.onAnimals();
            } },
          ]));
        }
      } else {
        if (!g.s.animals.length) list.appendChild(this.el('<p class="note">Henüz hayvanın yok.</p>'));
        g.s.animals.forEach((a) => {
          const sp = SPECIES[a.kind];
          const val = Math.round(sp.price * (a.age < 5 ? 0.5 : 0.55 + a.happy / 250));
          list.appendChild(this.itemRow(sp.icon, `${a.name} (${sp.name})`, `Mutluluk ${a.happy}% · ${a.age < 5 ? 'yavru' : 'yetişkin'} · değeri ${val} ₺`, [
            { label: `Sat ${val}₺`, cls: 'alt', fn: () => {
              g.s.animals = g.s.animals.filter((x) => x.id !== a.id);
              g.earn(val); this.audio.coin(); this.hooks.onAnimals();
            } },
          ]));
        });
      }
      body.appendChild(list);
    };
    this.open('Hayvan Pazarı', render);
  }

  // ------------------------------------------------ nalbur & galeri
  openUpgrades() {
    const g = this.g;
    this.open('Nalbur & Galeri', (body) => {
      body.appendChild(this.el('<p class="note">Aletlerini geliştir, araç al. Araçlar çiftliğe (kayık iskeleye) teslim edilir.</p>'));
      const list = this.el('<div class="list"></div>');
      for (const [k, u] of Object.entries(UPGRADES)) {
        const own = !!g.s.upgrades[k];
        let note = u.desc;
        if (k === 'atarabasi' && !g.s.animals.some((a) => a.kind === 'at')) note += ' <b>(Önce bir at al.)</b>';
        list.appendChild(this.itemRow(u.icon, u.name, `${note}`, [
          { label: own ? 'Sende var' : `${u.price.toLocaleString('tr-TR')} ₺`, cls: own ? 'alt' : 'gold', disabled: own || g.s.money < u.price, fn: () => {
            if (!g.spend(u.price)) return;
            g.s.upgrades[k] = 1; this.audio.coin(); this.toast(`${u.icon} ${u.name} senin!`, 'gold');
            this.hooks.onUpgrade(k);
          } },
        ]));
      }
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ çiçekçi
  openFlowers() {
    const g = this.g;
    this.open('Çiçekçi', (body) => {
      body.appendChild(this.el(`<p class="note">Saksı bitkileri evini süsler (en fazla 10). Evinde ${g.s.pots.length}/10 saksı var. Her saksı, evdeyken dinlenmeni biraz artırır.</p>`));
      const list = this.el('<div class="list"></div>');
      for (const p of POTS) {
        const n = g.s.pots.filter((x) => x === p.id).length;
        list.appendChild(this.itemRow('🪴', `${p.name}${n ? ` (evde ${n})` : ''}`, `${p.price} ₺`, [
          { label: 'Satın Al', disabled: g.s.pots.length >= 10 || g.s.money < p.price, fn: () => {
            if (!g.spend(p.price)) return;
            g.s.pots.push(p.id); this.audio.coin(); this.toast(`🪴 ${p.name} evine kondu.`);
            this.hooks.onPots();
          } },
        ]));
      }
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ fırın
  openBakery() {
    const g = this.g;
    this.open('Fırın', (body) => {
      body.appendChild(this.el('<p class="note">Fırıncı Hasan Usta yardımcı arıyor. Vardiya 4 saat sürer.</p>'));
      const list = this.el('<div class="list"></div>');
      const done = g.s.shiftsDone.firin;
      list.appendChild(this.itemRow('👨‍🍳', 'Fırında vardiya', '4 saat · 220 ₺ + 1 ekmek · −30 enerji', [
        { label: done ? 'Bugün çalıştın' : 'Çalış', disabled: done || g.s.energy < 30, fn: () => { this.close(); this.hooks.onShift('firin', 4, 220, 30, () => g.add('ekmek', 1)); } },
      ]));
      for (const id of ['ekmek', 'un']) {
        const it = ITEMS[id], p = Math.round(it.price * 0.9);
        list.appendChild(this.itemRow(it.icon, it.name, `${p} ₺${it.food ? ` · ⚡+${it.food}` : ''}`, [
          { label: 'Satın Al', disabled: g.s.money < p, fn: () => { if (g.spend(p)) g.add(id, 1); } },
        ]));
      }
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ iş ilanları
  openJobs() {
    const g = this.g;
    this.open('İş İlanları', (body) => {
      body.appendChild(this.el('<p class="note">Siparişler her sabah yenilenir. İstenen ürün çantandaysa teslim et, ödülü al.</p>'));
      const list = this.el('<div class="list"></div>');
      for (const j of g.s.jobs) {
        const it = ITEMS[j.item];
        const have = g.count(j.item);
        list.appendChild(this.itemRow(it.icon, `${j.who}: ${j.qty} × ${it.name}`, j.done ? 'Teslim edildi ✔' : `Ödül ${j.reward} ₺ · sende ${have}`, [
          { label: j.done ? 'Tamam' : 'Teslim Et', disabled: j.done || have < j.qty, fn: () => {
            if (g.completeJob(j.id)) { this.audio.coin(); this.toast(`Sipariş teslim edildi: +${j.reward} ₺`, 'gold'); }
          } },
        ]));
      }
      const done = g.s.shiftsDone.yevmiye;
      list.appendChild(this.itemRow('🧑‍🌾', 'Yevmiye: muhtarlık tarlasında çalış', '5 saat · 280 ₺ · −40 enerji', [
        { label: done ? 'Bugün çalıştın' : 'Çalış', cls: 'alt', disabled: done || g.s.energy < 40, fn: () => { this.close(); this.hooks.onShift('yevmiye', 5, 280, 40); } },
      ]));
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ değirmen
  openMill() {
    const g = this.g;
    this.open('Değirmen', (body) => {
      const list = this.el('<div class="list"></div>');
      list.appendChild(this.itemRow('🥣', 'Un öğüt', `3 buğday → 1 un (sende ${g.count('bugday')} buğday)`, [
        { label: 'Öğüt', disabled: g.count('bugday') < 3, fn: () => { g.take('bugday', 3); g.add('un', 1); this.audio.pop(); } },
        { label: 'Hepsini', cls: 'alt', disabled: g.count('bugday') < 3, fn: () => { const n = Math.floor(g.count('bugday') / 3); g.take('bugday', n * 3); g.add('un', n); this.audio.pop(); } },
      ]));
      const done = g.s.shiftsDone.mill;
      list.appendChild(this.itemRow('⚙️', 'Değirmende çalış', '3 saat · 160 ₺ · −25 enerji', [
        { label: done ? 'Bugün çalıştın' : 'Çalış', cls: 'alt', disabled: done || g.s.energy < 25, fn: () => { this.close(); this.hooks.onShift('mill', 3, 160, 25); } },
      ]));
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ ahır / hayvan durumu
  openBarn() {
    const g = this.g;
    this.open('Ahır · Hayvanların', (body) => {
      const winter = g.season === 3;
      body.appendChild(this.el(`<p class="note">Stok: ${ITEMS.saman.icon} Saman ${g.count('saman')} · ${ITEMS.yem.icon} Yem ${g.count('yem')}. ${winter ? '<b>Kış:</b> her sabah her hayvan için saman/yem harcanır.' : 'Hayvanlar meradan beslenir. Fırtınada ve kışta yem gerekir.'} Hayvanlarını sev, mutlu hayvan daha çok ürün verir.</p>`));
      const list = this.el('<div class="list"></div>');
      g.s.animals.forEach((a) => {
        const sp = SPECIES[a.kind];
        const ready = g.animalProductAvailable(a);
        const st = [`😊 ${a.happy}%`, a.fed ? 'tok' : '<b>aç</b>', a.age < 5 ? `yavru (${5 - a.age} gün)` : ''];
        if (sp.product) st.push(ready ? `<b>${ITEMS[sp.product].icon} hazır</b>` : 'ürün yok');
        list.appendChild(this.itemRow(sp.icon, `${a.name}`, st.filter(Boolean).join(' · '), []));
      });
      if (!g.s.animals.length) list.appendChild(this.el('<p class="note">Kasabadaki Hayvan Pazarından hayvan alabilirsin.</p>'));
      body.appendChild(list);
    });
  }

  // ------------------------------------------------ mutfak
  openKitchen() {
    const g = this.g;
    this.open('Mutfak · Soba', (body) => {
      body.appendChild(this.el('<p class="note">Malzemelerini işle: işlenmiş ürünler çok daha değerli. Yemek yemek enerji verir.</p>'));
      const list = this.el('<div class="list"></div>');
      for (const r of RECIPES) {
        const out = ITEMS[r.out];
        const need = Object.entries(r.need).map(([k, n]) => `${ITEMS[k].icon}${n} (${g.count(k)})`).join(' + ');
        const ok = Object.entries(r.need).every(([k, n]) => g.count(k) >= n);
        list.appendChild(this.itemRow(out.icon, `${r.name} → ${r.n} ${out.name}`, need, [
          { label: 'Yap', disabled: !ok, fn: () => { if (g.craft(r)) { this.audio.pop(); this.toast(`${out.icon} ${out.name} hazır!`); } } },
        ]));
      }
      body.appendChild(list);
      body.appendChild(this.el('<h3 style="font-family:Fredoka;margin:14px 0 6px">Ye</h3>'));
      body.appendChild(this.foodList());
    });
  }
  foodList() {
    const g = this.g;
    const list = this.el('<div class="list"></div>');
    const foods = Object.keys(g.s.inv).filter((k) => ITEMS[k]?.food && g.count(k) > 0).sort((a, b) => ITEMS[b].food - ITEMS[a].food);
    if (!foods.length) list.appendChild(this.el('<p class="note">Yenecek bir şey yok.</p>'));
    for (const id of foods) {
      const it = ITEMS[id];
      list.appendChild(this.itemRow(it.icon, `${it.name} ×${g.count(id)}`, `⚡ +${it.food}`, [
        { label: 'Ye', disabled: g.s.energy >= g.maxEnergy, fn: () => { this.hooks.onEat(id); } },
      ]));
    }
    return list;
  }

  // ------------------------------------------------ çanta
  openBag() {
    const g = this.g;
    let tab = 0;
    this.open('Çanta', (body) => {
      this.tabs(body, ['Eşyalar', 'Ye', 'Aletler', 'İstatistik'], tab, (i) => { tab = i; this.refresh(); });
      if (tab === 0) {
        const list = this.el('<div class="list"></div>');
        const cats = {};
        for (const id of Object.keys(g.s.inv)) {
          const it = ITEMS[id]; if (!it || g.count(id) <= 0) continue;
          (cats[it.cat] ||= []).push(id);
        }
        const order = ['tohum', 'ürün', 'meyve', 'hayvansal', 'balık', 'doğa', 'mutfak', 'malzeme'];
        for (const c of order) {
          if (!cats[c]) continue;
          list.appendChild(this.el(`<div class="note" style="margin-top:6px"><b>${c[0].toUpperCase() + c.slice(1)}</b></div>`));
          for (const id of cats[c]) {
            const it = ITEMS[id];
            const btns = [];
            if (it.cat === 'tohum') btns.push({ label: g.s.seed === id ? 'Seçili' : 'Seç', cls: 'alt', disabled: g.s.seed === id, fn: () => { g.s.seed = id; g.s.tool = 3; this.hooks.onTool(3); } });
            if (it.food) btns.push({ label: 'Ye', fn: () => this.hooks.onEat(id), disabled: g.s.energy >= g.maxEnergy });
            list.appendChild(this.itemRow(it.icon, `${it.name} ×${g.count(id)}`, `değeri ~${g.sellPrice(id)} ₺`, btns, it.sub || ''));
          }
        }
        if (!Object.keys(cats).length) list.appendChild(this.el('<p class="note">Çantan boş.</p>'));
        body.appendChild(list);
      } else if (tab === 1) {
        body.appendChild(this.foodList());
      } else if (tab === 2) {
        const list = this.el('<div class="list"></div>');
        const up = g.s.upgrades;
        list.appendChild(this.itemRow('⛏️', 'Çapa', 'Tarlayı sürer. Ölü ekinleri temizler.', []));
        list.appendChild(this.itemRow('🚿', up.sulama2 ? 'Bakır Sulama Kabı' : 'Sulama Kabı', `Su: ${g.s.water}/${up.sulama2 ? 40 : 20}. Kuyudan, gölden, nehirden doldur.`, []));
        list.appendChild(this.itemRow('🌱', 'Tohum Kesesi', 'Q ile tohum değiştir.', []));
        list.appendChild(this.itemRow('🪓', up.balta2 ? 'Çelik Balta' : 'Balta', 'Ağaç kes, odun topla. Ağaçlar 8 günde yeniden büyür.', []));
        list.appendChild(this.itemRow('🎣', up.olta2 ? 'Usta Oltası' : 'Olta', 'Göl, nehir, şelale havuzu. Kışın buzda balık tut.', []));
        for (const [k, u] of Object.entries(UPGRADES)) if (up[k] && !['sulama2', 'balta2', 'olta2'].includes(k)) list.appendChild(this.itemRow(u.icon, u.name, u.desc, []));
        body.appendChild(list);
      } else {
        const st = g.s.stats;
        body.appendChild(this.el(`<div class="card"><h3>Hayatın</h3><ul>
          <li>Toplam kazanç: <b>${st.earned.toLocaleString('tr-TR')} ₺</b></li>
          <li>Hasat edilen ürün: <b>${st.harvested}</b></li>
          <li>Tutulan balık: <b>${st.fish}</b></li>
          <li>Kesilen ağaç: <b>${st.wood}</b></li>
          <li>Hayvan sayısı: <b>${g.s.animals.length}</b></li>
          <li>Keşfedilen yerler: <b>${Object.keys(g.s.discovered).length}/${PLACES.length}</b></li>
        </ul></div>`));
        const b = document.createElement('button');
        b.className = 'btn alt'; b.style.marginTop = '12px'; b.textContent = 'Oyunu kaydet';
        b.addEventListener('click', () => { this.hooks.onSave(); this.toast('Kaydedildi ✔'); });
        body.appendChild(b);
      }
    });
  }

  // ------------------------------------------------ harita
  drawMapBase() {
    if (this.mapBase) return this.mapBase;
    const S = 320;
    const c = document.createElement('canvas'); c.width = c.height = S;
    const x = c.getContext('2d');
    const img = x.createImageData(S, S);
    for (let j = 0; j < S; j++) for (let i = 0; i < S; i++) {
      const wx = -HALF + (i + 0.5) * (WORLD / S), wz = -HALF + (j + 0.5) * (WORLD / S);
      const h = heightAt(wx, wz);
      const hx = heightAt(wx + 2, wz) - h;
      let r, gg, b;
      if (h < -0.2) { const d = Math.min(1, -h / 5); r = 70 - d * 30; gg = 140 - d * 40; b = 170 - d * 20; }
      else if (h > 60) { r = 230; gg = 235; b = 240; }
      else if (h > 30) { r = 130; gg = 125; b = 110; }
      else { r = 110 + h * 1.2; gg = 150 - h * 0.6; b = 80; }
      const sh = Math.max(-30, Math.min(30, -hx * 25));
      const k = (j * S + i) * 4;
      img.data[k] = r + sh; img.data[k + 1] = gg + sh; img.data[k + 2] = b + sh; img.data[k + 3] = 255;
    }
    x.putImageData(img, 0, 0);
    const toP = (p) => [(p[0] + HALF) / WORLD * S, (p[1] + HALF) / WORLD * S];
    x.strokeStyle = 'rgba(140, 100, 60, .9)'; x.lineWidth = 2; x.lineJoin = 'round';
    for (const r of ROADS) { x.beginPath(); r.forEach((p, i) => { const [a, b] = toP(p); i ? x.lineTo(a, b) : x.moveTo(a, b); }); x.stroke(); }
    this.mapBase = c;
    return c;
  }
  openMap(player) {
    const g = this.g;
    this.open('Harita', (body) => {
      const cv = document.createElement('canvas');
      cv.id = 'mapCanvas'; cv.width = cv.height = 640;
      body.appendChild(cv);
      const x = cv.getContext('2d');
      x.imageSmoothingEnabled = true;
      x.drawImage(this.drawMapBase(), 0, 0, 640, 640);
      const toP = (wx, wz) => [(wx + HALF) / WORLD * 640, (wz + HALF) / WORLD * 640];
      x.font = 'bold 15px Nunito, sans-serif'; x.textAlign = 'center';
      for (const p of PLACES) {
        const [a, b] = toP(p.x, p.z);
        const known = g.s.discovered[p.id];
        x.fillStyle = known ? '#fff6e2' : 'rgba(255,255,255,.45)';
        x.strokeStyle = 'rgba(0,0,0,.6)'; x.lineWidth = 3;
        const label = known ? p.name : '???';
        x.strokeText(label, a, b - 8); x.fillText(label, a, b - 8);
        x.beginPath(); x.arc(a, b, 5, 0, 7); x.fillStyle = known ? '#e2a93b' : '#999'; x.fill();
      }
      const [px, pz] = toP(player.pos.x, player.pos.z);
      const yaw = player.yaw;
      x.save(); x.translate(px, pz); x.rotate(-yaw);
      x.fillStyle = '#e03a2a'; x.strokeStyle = '#fff'; x.lineWidth = 2;
      x.beginPath(); x.moveTo(0, -12); x.lineTo(8, 9); x.lineTo(0, 4); x.lineTo(-8, 9); x.closePath(); x.fill(); x.stroke();
      x.restore();
      body.appendChild(this.el('<p class="note">Kuzey yukarıda. Kırmızı ok sensin. Yollar kahverengi.</p>'));
    });
  }

  // ------------------------------------------------ yardım + mevsim bilgisi
  seasonCards(season) {
    const S = SEASON_INFO[season];
    return `<div class="grid2"><div class="card"><h3>${S.icon} ${S.name}: avantajlar</h3><ul class="plus">${S.plus.map((p) => `<li>${p}</li>`).join('')}</ul></div>
      <div class="card"><h3>Zorluklar</h3><ul class="minus">${S.minus.map((p) => `<li>${p}</li>`).join('')}</ul></div></div>`;
  }
  openHelp(isMobile) {
    const g = this.g;
    let tab = 0;
    this.open('Rehber', (body) => {
      this.tabs(body, ['Nasıl oynanır', 'Mevsimler', 'Kontroller'], tab, (i) => { tab = i; this.refresh(); });
      if (tab === 0) {
        body.appendChild(this.el(`<div class="list">
          <div class="card"><h3>🌱 Çiftçilik</h3><ul><li><b>Çapa</b> ile tarlayı sür, <b>tohum</b> ek, <b>sulama kabı</b> ile her gün sula.</li><li>Sulama kabını kuyudan ya da gölden doldur. Yağmur tarlayı sular.</li><li>Olgunlaşan ekini <b>el</b> ya da <b>E</b> ile hasat et. Traktörle tarlada sürersen toprağı hızlıca çapalar.</li></ul></div>
          <div class="card"><h3>🐄 Hayvanlar</h3><ul><li>Kasabadaki Hayvan Pazarından al, sat.</li><li>Yanlarına gidip <b>E</b>: sev, süt sağ, yumurta topla, yün kırk.</li><li>Kışın saman/yem stoğu tut. Atına binebilirsin.</li></ul></div>
          <div class="card"><h3>💼 Çalışma hayatı</h3><ul><li>Kasabadaki İş İlanlarından sipariş teslim et ya da yevmiyeye çık.</li><li>Fırında ve değirmende vardiyada çalışabilirsin.</li><li>Ürünlerini pazarda tam fiyata, çiftlik tezgâhında %75'e sat.</li></ul></div>
          <div class="card"><h3>🧭 Gezinti</h3><ul><li>Gümüş Şelale (kuzey), Ayna Göl ve ada (doğu), Çam Ormanı (batı), Kartal Tepesi (kuzeybatı zirve), Çiçekli Yayla.</li><li>Balık tut, mantar/böğürtlen topla, odun kes, manzaranın tadını çıkar.</li><li>Gece 02:00'den önce yatağında uyu!</li></ul></div>
        </div>`));
      } else if (tab === 1) {
        for (let i = 0; i < 4; i++) body.appendChild(this.el(`<div style="margin-bottom:10px">${this.seasonCards(i)}</div>`));
      } else {
        body.appendChild(this.el(isMobile ? `<div class="card"><ul>
          <li>Sol daire: yürü · Sağ taraf: kaydırarak bak, kısa dokun: kullan</li>
          <li><b>Kullan</b>: elindeki aleti kullan · <b>Etkileş</b>: konuş, topla, bin/in</li>
          <li>Alttaki çubuktan alet seç · 🎒 çanta · 🗺️ harita</li></ul></div>` : `<div class="card"><ul>
          <li><b>WASD</b> yürü, <b>Shift</b> koş, <b>Boşluk</b> zıpla</li>
          <li><b>Sol tık</b> alet kullan, <b>Sağ tık / E</b> etkileşim, araca bin/in</li>
          <li><b>1–6</b> ya da fare tekerleği: alet seç · <b>Q</b> tohum değiştir</li>
          <li><b>I</b> çanta · <b>M</b> harita · <b>R</b> en iyi yiyeceği ye · <b>H</b> rehber · <b>Esc</b> fareyi bırak</li></ul></div>`));
      }
    });
  }
  openSeasonIntro(season, extra = '') {
    this.open(`${SEASON_INFO[season].icon} ${SEASON_INFO[season].name} geldi`, (body) => {
      if (extra) body.appendChild(this.el(`<p class="note">${extra}</p>`));
      body.appendChild(this.el(this.seasonCards(season)));
    });
  }
  openMorning(report, weatherKey, tomorrow) {
    const g = this.g;
    const w = WEATHER[weatherKey], t = WEATHER[tomorrow];
    this.open('Günaydın!', (body) => {
      body.appendChild(this.el(`<p class="note" style="font-size:15px">${g.dateString()}<br>Bugün hava: <b>${w.icon} ${w.name}</b> · Yarın tahmini: ${t.icon} ${t.name}</p>`));
      const ul = this.el('<div class="card"><ul></ul></div>');
      const lines = report.length ? report : ['Sakin bir gece geçti.'];
      for (const l of lines) { const li = document.createElement('li'); li.textContent = l; ul.querySelector('ul').appendChild(li); }
      body.appendChild(ul);
      const j = g.s.jobs.map((x) => `${ITEMS[x.item].icon} ${x.qty} ${ITEMS[x.item].name} → ${x.reward}₺`).join(' · ');
      body.appendChild(this.el(`<p class="note">Kasabada bugünkü siparişler: ${esc(j)}</p>`));
    });
  }
}
