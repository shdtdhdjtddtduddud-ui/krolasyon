// Tamamen sentezlenmiş ses efektleri ve dinamik müzik (WebAudio)
import * as THREE from 'three';
export class Audio {
  constructor() { this.ctx = null; this.vol = { master: 0.8, sfx: 0.9, music: 0.45 }; this.mode = 'explore'; this.biome = 'grass'; this.listener = { pos: new THREE.Vector3(), right: new THREE.Vector3(1, 0, 0) }; this.last = {}; }
  init() {
    if (this.ctx) { if (this.ctx.state === 'suspended') this.ctx.resume(); return; }
    const C = window.AudioContext || window.webkitAudioContext; if (!C) return;
    const ctx = (this.ctx = new C());
    this.master = ctx.createGain(); this.master.gain.value = this.vol.master; this.master.connect(ctx.destination);
    const comp = ctx.createDynamicsCompressor(); comp.threshold.value = -14; comp.ratio.value = 4; comp.connect(this.master); this.bus = comp;
    this.sfx = ctx.createGain(); this.sfx.gain.value = this.vol.sfx; this.sfx.connect(comp);
    this.music = ctx.createGain(); this.music.gain.value = this.vol.music; this.music.connect(comp);
    // yankı
    const len = ctx.sampleRate * 2.6, ir = ctx.createBuffer(2, len, ctx.sampleRate);
    for (let c = 0; c < 2; c++) { const d = ir.getChannelData(c); for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3); }
    this.rev = ctx.createConvolver(); this.rev.buffer = ir; const rg = ctx.createGain(); rg.gain.value = 0.35; this.rev.connect(rg); rg.connect(comp);
    const nb = ctx.createBuffer(1, ctx.sampleRate * 2, ctx.sampleRate); const nd = nb.getChannelData(0); for (let i = 0; i < nd.length; i++) nd[i] = Math.random() * 2 - 1; this.noiseBuf = nb;
    this.startMusic();
  }
  setVolumes() { if (!this.ctx) return; this.master.gain.value = this.vol.master; this.sfx.gain.value = this.vol.sfx; this.music.gain.value = this.vol.music; }
  // ----- temel yapı taşları -----
  out(pos, vol = 1, rev = 0.2) {
    const ctx = this.ctx; const g = ctx.createGain(); let v = vol;
    let node = g;
    if (pos) {
      const d = pos.distanceTo(this.listener.pos); v *= 1 / (1 + d * 0.07); if (d > 70) v = 0;
      const pn = ctx.createStereoPanner ? ctx.createStereoPanner() : null;
      if (pn) { const dx = pos.clone().sub(this.listener.pos).normalize().dot(this.listener.right); pn.pan.value = Math.max(-0.8, Math.min(0.8, dx * 0.8)); g.connect(pn); node = pn; }
    }
    g.gain.value = v; node.connect(this.sfx);
    if (rev > 0) { const s = ctx.createGain(); s.gain.value = rev; node.connect(s); s.connect(this.rev); }
    return v > 0.001 ? g : null;
  }
  noise(dest, t0, dur, { type = 'lowpass', f0 = 1000, f1 = f0, q = 1, vol = 1, attack = 0.005 } = {}) {
    const ctx = this.ctx, src = ctx.createBufferSource(); src.buffer = this.noiseBuf; src.playbackRate.value = 0.8 + Math.random() * 0.4;
    const f = ctx.createBiquadFilter(); f.type = type; f.Q.value = q; f.frequency.setValueAtTime(f0, t0); f.frequency.exponentialRampToValueAtTime(Math.max(20, f1), t0 + dur);
    const g = ctx.createGain(); g.gain.setValueAtTime(0.0001, t0); g.gain.exponentialRampToValueAtTime(vol, t0 + attack); g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    src.connect(f); f.connect(g); g.connect(dest); src.start(t0, Math.random() * 1.5); src.stop(t0 + dur + 0.05);
  }
  tone(dest, t0, dur, { type = 'sine', f0 = 440, f1 = f0, vol = 0.5, attack = 0.005, curve = 'exp' } = {}) {
    const ctx = this.ctx, o = ctx.createOscillator(); o.type = type; o.frequency.setValueAtTime(f0, t0);
    if (f1 !== f0) o.frequency.exponentialRampToValueAtTime(Math.max(10, f1), t0 + dur);
    const g = ctx.createGain(); g.gain.setValueAtTime(0.0001, t0); g.gain.exponentialRampToValueAtTime(vol, t0 + attack);
    if (curve === 'exp') g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur); else { g.gain.setValueAtTime(vol, t0 + dur * 0.7); g.gain.linearRampToValueAtTime(0, t0 + dur); }
    o.connect(g); g.connect(dest); o.start(t0); o.stop(t0 + dur + 0.05); return o;
  }
  // ----- efektler -----
  play(name, pos = null, o = {}) {
    if (!this.ctx || this.ctx.state !== 'running') return;
    const now = this.ctx.currentTime;
    const key = name + (pos ? 'p' : ''); if (this.last[key] && now - this.last[key] < (o.min ?? 0.03)) return; this.last[key] = now;
    const p = o.pitch ?? 1, t = now + 0.005;
    const S = (vol, rev) => this.out(pos, vol, rev);
    let d;
    switch (name) {
      case 'swing': if (!(d = S(0.5, 0.1))) return; this.noise(d, t, 0.22, { type: 'bandpass', f0: 600 * p, f1: 2600 * p, q: 1.5, vol: 0.8, attack: 0.05 }); break;
      case 'heavySwing': if (!(d = S(0.6, 0.15))) return; this.noise(d, t, 0.38, { type: 'bandpass', f0: 250 * p, f1: 1400 * p, q: 1.2, vol: 0.9, attack: 0.12 }); break;
      case 'hit': if (!(d = S(0.8, 0.12))) return; this.noise(d, t, 0.14, { f0: 3000, f1: 400, vol: 0.9 }); this.tone(d, t, 0.18, { f0: 160 * p, f1: 50, vol: 0.9 }); break;
      case 'hitHeavy': if (!(d = S(1, 0.2))) return; this.noise(d, t, 0.3, { f0: 2000, f1: 100, vol: 1 }); this.tone(d, t, 0.35, { f0: 110 * p, f1: 35, vol: 1 }); break;
      case 'crit': if (!(d = S(0.7, 0.3))) return; this.tone(d, t, 0.4, { type: 'triangle', f0: 1400, f1: 1300, vol: 0.35 }); this.tone(d, t, 0.3, { type: 'square', f0: 2100, f1: 2000, vol: 0.06 }); break;
      case 'metal': if (!(d = S(0.6, 0.3))) return; this.tone(d, t, 0.5, { type: 'triangle', f0: 900 * p, vol: 0.3 }); this.tone(d, t, 0.4, { type: 'sine', f0: 2300 * p, vol: 0.15 }); this.noise(d, t, 0.08, { type: 'highpass', f0: 4000, vol: 0.5 }); break;
      case 'step': if (!(d = S(0.12 * (o.vol ?? 1), 0))) return; this.noise(d, t, 0.07, { f0: 900 * p, f1: 200, vol: 0.8 }); break;
      case 'snowstep': if (!(d = S(0.14, 0))) return; this.noise(d, t, 0.1, { type: 'bandpass', f0: 3000, f1: 1500, q: 0.8, vol: 0.7 }); break;
      case 'jump': if (!(d = S(0.3, 0))) return; this.noise(d, t, 0.15, { type: 'bandpass', f0: 400, f1: 1200, vol: 0.6 }); break;
      case 'land': if (!(d = S(0.4, 0))) return; this.noise(d, t, 0.15, { f0: 600, f1: 100, vol: 0.9 }); this.tone(d, t, 0.12, { f0: 90, f1: 40, vol: 0.5 }); break;
      case 'roll': if (!(d = S(0.4, 0.05))) return; this.noise(d, t, 0.4, { type: 'bandpass', f0: 300, f1: 900, q: 0.8, vol: 0.7, attack: 0.1 }); break;
      case 'blink': if (!(d = S(0.5, 0.4))) return; this.tone(d, t, 0.3, { type: 'sine', f0: 300, f1: 2400, vol: 0.3 }); this.noise(d, t, 0.25, { type: 'highpass', f0: 2000, f1: 8000, vol: 0.4 }); break;
      case 'hurt': if (!(d = S(0.7, 0.1))) return; this.tone(d, t, 0.25, { type: 'sawtooth', f0: 200 * p, f1: 110 * p, vol: 0.25 }); this.noise(d, t, 0.12, { f0: 1500, f1: 300, vol: 0.6 }); break;
      case 'growl': { if (!(d = S(0.5 * (o.vol ?? 1), 0.2))) return; const f = this.ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 700 * p; f.connect(d); const os = this.tone(f, t, 0.7, { type: 'sawtooth', f0: 90 * p, f1: 70 * p, vol: 0.5, attack: 0.08, curve: 'lin' }); const l = this.ctx.createOscillator(); l.frequency.value = 18; const lg = this.ctx.createGain(); lg.gain.value = 18 * p; l.connect(lg); lg.connect(os.frequency); l.start(t); l.stop(t + 0.8); this.noise(d, t, 0.6, { type: 'bandpass', f0: 500 * p, f1: 300 * p, q: 2, vol: 0.3, attack: 0.1 }); break; }
      case 'roar': { if (!(d = S(1.2, 0.5))) return; const f = this.ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.setValueAtTime(300, t); f.frequency.linearRampToValueAtTime(1400, t + 0.5); f.frequency.linearRampToValueAtTime(300, t + 2); f.connect(d); const os = this.tone(f, t, 2, { type: 'sawtooth', f0: 70 * p, f1: 45 * p, vol: 0.7, attack: 0.2, curve: 'lin' }); const l = this.ctx.createOscillator(); l.frequency.value = 23; const lg = this.ctx.createGain(); lg.gain.value = 25; l.connect(lg); lg.connect(os.frequency); l.start(t); l.stop(t + 2); this.noise(d, t, 2, { type: 'bandpass', f0: 600, f1: 250, q: 1, vol: 0.6, attack: 0.3 }); break; }
      case 'die': if (!(d = S(0.5, 0.25))) return; this.tone(d, t, 0.6, { type: 'sawtooth', f0: 180 * p, f1: 40 * p, vol: 0.2 }); this.noise(d, t, 0.4, { f0: 1200, f1: 100, vol: 0.5 }); break;
      case 'splat': if (!(d = S(0.5, 0.1))) return; this.noise(d, t, 0.2, { f0: 800, f1: 150, vol: 0.9 }); this.tone(d, t, 0.2, { f0: 300 * p, f1: 80, vol: 0.4 }); break;
      case 'fireball': if (!(d = S(0.6, 0.2))) return; this.noise(d, t, 0.5, { type: 'bandpass', f0: 300, f1: 1800, q: 0.7, vol: 0.9, attack: 0.03 }); this.tone(d, t, 0.3, { type: 'sawtooth', f0: 120, f1: 300, vol: 0.12 }); break;
      case 'explode': if (!(d = S(1.2, 0.5))) return; this.noise(d, t, 1.2, { f0: 2500, f1: 60, vol: 1 }); this.tone(d, t, 0.6, { f0: 90, f1: 30, vol: 1 }); break;
      case 'ice': if (!(d = S(0.6, 0.5))) return; for (let i = 0; i < 6; i++) this.tone(d, t + i * 0.03, 0.5, { type: 'sine', f0: 1800 + Math.random() * 2200, vol: 0.12 }); this.noise(d, t, 0.4, { type: 'highpass', f0: 3000, f1: 1500, vol: 0.5 }); break;
      case 'zap': { if (!(d = S(0.6, 0.3))) return; for (let i = 0; i < 5; i++) this.tone(d, t + i * 0.035, 0.07, { type: 'sawtooth', f0: 200 + Math.random() * 1600, f1: 100 + Math.random() * 600, vol: 0.18 }); this.noise(d, t, 0.25, { type: 'highpass', f0: 2500, vol: 0.7 }); break; }
      case 'bow': if (!(d = S(0.5, 0.1))) return; this.tone(d, t, 0.18, { type: 'triangle', f0: 190 * p, f1: 150, vol: 0.4 }); this.noise(d, t, 0.2, { type: 'bandpass', f0: 2000, f1: 5000, q: 2, vol: 0.4 }); break;
      case 'arrowHit': if (!(d = S(0.5, 0.05))) return; this.noise(d, t, 0.06, { f0: 3000, f1: 800, vol: 0.9 }); this.tone(d, t, 0.08, { type: 'triangle', f0: 400, f1: 200, vol: 0.3 }); break;
      case 'magic': if (!(d = S(0.5, 0.4))) return; this.tone(d, t, 0.35, { type: 'triangle', f0: 500 * p, f1: 900 * p, vol: 0.2 }); this.tone(d, t + 0.02, 0.35, { type: 'sine', f0: 750 * p, f1: 1350 * p, vol: 0.15 }); break;
      case 'shock': if (!(d = S(1, 0.4))) return; this.noise(d, t, 0.7, { f0: 900, f1: 50, vol: 1 }); this.tone(d, t, 0.5, { f0: 70, f1: 28, vol: 1 }); break;
      case 'buff': if (!(d = S(0.7, 0.5))) return; [0, 4, 7, 12].forEach((n, i) => this.tone(d, t + i * 0.06, 0.6, { type: 'sawtooth', f0: 220 * 2 ** (n / 12), vol: 0.06 })); this.noise(d, t, 0.8, { type: 'bandpass', f0: 400, f1: 2000, vol: 0.3, attack: 0.2 }); break;
      case 'levelup': if (!(d = S(0.8, 0.6))) return; [0, 4, 7, 12, 16, 19, 24].forEach((n, i) => this.tone(d, t + i * 0.07, 0.8, { type: 'triangle', f0: 392 * 2 ** (n / 12), vol: 0.15 })); break;
      case 'quest': if (!(d = S(0.7, 0.6))) return; [[0, 0], [4, 0.12], [7, 0.24], [12, 0.4]].forEach(([n, dt]) => { this.tone(d, t + dt, 0.9, { type: 'triangle', f0: 330 * 2 ** (n / 12), vol: 0.16 }); this.tone(d, t + dt, 0.9, { type: 'sine', f0: 660 * 2 ** (n / 12), vol: 0.06 }); }); break;
      case 'coin': if (!(d = S(0.4, 0.2))) return; this.tone(d, t, 0.1, { type: 'square', f0: 1300, vol: 0.06 }); this.tone(d, t + 0.07, 0.3, { type: 'square', f0: 1950, vol: 0.06 }); break;
      case 'pickup': if (!(d = S(0.5, 0.3))) return; [0, 5, 9].forEach((n, i) => this.tone(d, t + i * 0.05, 0.25, { type: 'triangle', f0: 660 * 2 ** (n / 12), vol: 0.14 })); break;
      case 'rare': if (!(d = S(0.7, 0.6))) return; [0, 7, 12, 19].forEach((n, i) => this.tone(d, t + i * 0.08, 0.7, { type: 'sine', f0: 523 * 2 ** (n / 12), vol: 0.14 })); break;
      case 'potion': if (!(d = S(0.5, 0.2))) return; for (let i = 0; i < 5; i++) this.tone(d, t + i * 0.07, 0.1, { type: 'sine', f0: 400 + Math.random() * 500, f1: 900 + Math.random() * 400, vol: 0.12 }); break;
      case 'heal': if (!(d = S(0.6, 0.5))) return; [0, 4, 7].forEach((n) => this.tone(d, t, 0.8, { type: 'sine', f0: 523 * 2 ** (n / 12), vol: 0.1, attack: 0.1 })); break;
      case 'click': if (!(d = S(0.3, 0))) return; this.tone(d, t, 0.05, { type: 'triangle', f0: 900, vol: 0.2 }); break;
      case 'open': if (!(d = S(0.3, 0.1))) return; this.noise(d, t, 0.15, { type: 'bandpass', f0: 800, f1: 2000, vol: 0.4 }); break;
      case 'teleport': if (!(d = S(0.7, 0.6))) return; this.tone(d, t, 1, { type: 'sine', f0: 200, f1: 1600, vol: 0.25 }); this.tone(d, t, 1, { type: 'triangle', f0: 300, f1: 2400, vol: 0.1 }); this.noise(d, t, 1, { type: 'bandpass', f0: 500, f1: 6000, vol: 0.3, attack: 0.3 }); break;
      case 'chest': if (!(d = S(0.6, 0.3))) return; this.noise(d, t, 0.3, { f0: 400, f1: 200, vol: 0.6 }); [0, 4, 7, 11, 14].forEach((n, i) => this.tone(d, t + 0.15 + i * 0.06, 0.5, { type: 'triangle', f0: 523 * 2 ** (n / 12), vol: 0.12 })); break;
      case 'web': if (!(d = S(0.4, 0.1))) return; this.noise(d, t, 0.25, { type: 'bandpass', f0: 1200, f1: 400, q: 3, vol: 0.6 }); break;
      case 'deny': if (!(d = S(0.4, 0))) return; this.tone(d, t, 0.15, { type: 'square', f0: 180, vol: 0.08 }); this.tone(d, t + 0.1, 0.2, { type: 'square', f0: 140, vol: 0.08 }); break;
      case 'breath': if (!(d = S(1, 0.3))) return; this.noise(d, t, 1.6, { type: 'bandpass', f0: 500, f1: 1400, q: 0.5, vol: 1, attack: 0.1 }); this.noise(d, t, 1.6, { f0: 300, f1: 200, vol: 0.7, attack: 0.1 }); break;
      case 'flap': if (!(d = S(0.8, 0.1))) return; this.noise(d, t, 0.3, { f0: 500, f1: 100, vol: 0.9, attack: 0.05 }); break;
      case 'victory': if (!(d = S(0.9, 0.7))) return; [[0, 0], [4, 0.2], [7, 0.4], [12, 0.6], [7, 0.9], [12, 1.1], [16, 1.3]].forEach(([n, dt]) => { this.tone(d, t + dt, 1, { type: 'triangle', f0: 262 * 2 ** (n / 12), vol: 0.18 }); this.tone(d, t + dt, 1, { type: 'sawtooth', f0: 131 * 2 ** (n / 12), vol: 0.04 }); }); break;
    }
  }
  // ----- müzik -----
  startMusic() {
    const ctx = this.ctx; this.mNext = ctx.currentTime + 0.5; this.mStep = 0; this.mChord = 0;
    this.mFilter = ctx.createBiquadFilter(); this.mFilter.type = 'lowpass'; this.mFilter.frequency.value = 1400; this.mFilter.connect(this.music);
    const ms = ctx.createGain(); ms.gain.value = 0.5; this.mFilter.connect(ms); ms.connect(this.rev);
    setInterval(() => this.schedule(), 90);
  }
  schedule() {
    const ctx = this.ctx; if (!ctx || ctx.state !== 'running') return;
    const SC = { grass: [0, 2, 4, 7, 9], forest: [0, 2, 3, 7, 8], desert: [0, 1, 4, 5, 7, 8], snow: [0, 2, 3, 5, 7, 10], volcano: [0, 1, 3, 6, 7, 10] };
    const ROOTS = { grass: 57, forest: 50, desert: 52, snow: 55, volcano: 45 };
    const PROG = [[0, 3, 7], [-4, 0, 3], [-7, -4, 0], [-2, 2, 5]];
    const combat = this.mode !== 'explore'; const bpm = this.mode === 'boss' ? 132 : combat ? 118 : 72; const st = 60 / bpm / 2;
    while (this.mNext < ctx.currentTime + 0.3) {
      const t = this.mNext, step = this.mStep, root = ROOTS[this.biome] ?? 57, sc = SC[this.biome] ?? SC.grass;
      const f = (n) => 440 * 2 ** ((n - 69) / 12);
      if (step % 16 === 0) {
        this.mChord = (this.mChord + 1) % PROG.length; const ch = PROG[this.mChord];
        for (const n of ch) for (const det of [-6, 6]) { const o = this.tone(this.mFilter, t, st * 16, { type: combat ? 'sawtooth' : 'triangle', f0: f(root - 12 + n), vol: combat ? 0.035 : 0.05, attack: st * 4, curve: 'lin' }); o.detune.value = det; }
        this.tone(this.mFilter, t, st * 16, { type: 'sine', f0: f(root - 24 + ch[0]), vol: 0.12, attack: 0.3, curve: 'lin' });
      }
      const density = combat ? 0.75 : 0.32;
      if (Math.random() < density && step % (combat ? 1 : 2) === 0) { const n = sc[Math.floor(Math.random() * sc.length)] + (Math.random() < 0.3 ? 12 : 0); this.tone(this.mFilter, t, st * 3, { type: this.biome === 'snow' ? 'sine' : 'triangle', f0: f(root + n), vol: combat ? 0.05 : 0.07 }); }
      if (combat) {
        if (step % 4 === 0) { this.tone(this.music, t, 0.3, { f0: 110, f1: 40, vol: 0.5 }); }
        if (step % 8 === 4) this.noise(this.music, t, 0.18, { type: 'bandpass', f0: 1800, f1: 900, vol: 0.25 });
        if (step % 2 === 1) this.noise(this.music, t, 0.04, { type: 'highpass', f0: 7000, vol: 0.08 });
        if (this.mode === 'boss' && step % 16 === 14) this.tone(this.music, t, 0.4, { f0: 70, f1: 35, vol: 0.5 });
      }
      this.mNext += st; this.mStep++;
    }
  }
}
export const audio = new Audio();
