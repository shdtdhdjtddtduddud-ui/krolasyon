// Prosedürel ses: ortam (kuş, cırcır, rüzgâr, yağmur, su), efektler, hayvan sesleri, motor
export class Audio {
  constructor() {
    this.ctx = null;
    this.enabled = true;
    this.birdTimer = 2;
    this.cricketTimer = 1;
  }
  start() {
    if (this.ctx) { if (this.ctx.state === 'suspended') this.ctx.resume(); return; }
    try {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return;
      this.ctx = new AC();
    } catch (e) { return; }
    const c = this.ctx;
    this.master = c.createGain(); this.master.gain.value = 0.7; this.master.connect(c.destination);
    // gürültü tamponu
    const len = c.sampleRate * 2;
    const buf = c.createBuffer(1, len, c.sampleRate);
    const d = buf.getChannelData(0);
    let last = 0;
    for (let i = 0; i < len; i++) { const w = Math.random() * 2 - 1; last = (last + 0.02 * w) / 1.02; d[i] = w * 0.5 + last * 3; }
    this.noiseBuf = buf;
    const loop = (freq, type, q = 0.7) => {
      const src = c.createBufferSource(); src.buffer = buf; src.loop = true;
      const f = c.createBiquadFilter(); f.type = type; f.frequency.value = freq; f.Q.value = q;
      const g = c.createGain(); g.gain.value = 0;
      src.connect(f); f.connect(g); g.connect(this.master);
      src.start();
      return { g, f };
    };
    this.wind = loop(400, 'lowpass');
    this.rain = loop(2500, 'highpass', 0.3);
    this.water = loop(900, 'bandpass', 0.5);
    // motor
    this.engOsc = c.createOscillator(); this.engOsc.type = 'sawtooth'; this.engOsc.frequency.value = 40;
    const ef = c.createBiquadFilter(); ef.type = 'lowpass'; ef.frequency.value = 300;
    this.engGain = c.createGain(); this.engGain.gain.value = 0;
    this.engOsc.connect(ef); ef.connect(this.engGain); this.engGain.connect(this.master);
    this.engOsc.start();
  }
  set(node, v, t = 0.3) {
    if (!this.ctx) return;
    node.g.gain.setTargetAtTime(v, this.ctx.currentTime, t);
  }
  ambient(dt, p) {
    if (!this.ctx || !this.enabled) return;
    const c = this.ctx;
    this.set(this.wind, 0.05 + p.wind * 0.18 + (p.altitude > 50 ? 0.08 : 0));
    this.set(this.rain, p.rain * 0.12);
    this.set(this.water, Math.min(0.35, p.water * 0.4));
    // motor
    if (p.engine) {
      this.engGain.gain.setTargetAtTime(p.engine.boat ? 0 : 0.08, c.currentTime, 0.2);
      this.engOsc.frequency.setTargetAtTime(32 + Math.abs(p.engine.speed) * 6 + (p.engine.truck ? 15 : 0), c.currentTime, 0.2);
    } else this.engGain.gain.setTargetAtTime(0, c.currentTime, 0.3);
    // kuşlar
    if (p.day > 0.5 && p.season !== 3 && p.rain === 0) {
      this.birdTimer -= dt;
      if (this.birdTimer < 0) { this.birdTimer = 1.5 + Math.random() * 5; this.bird(); }
    }
    // cırcır böcekleri
    if (p.day < 0.3 && (p.season === 1 || p.season === 0) && p.rain === 0) {
      this.cricketTimer -= dt;
      if (this.cricketTimer < 0) { this.cricketTimer = 0.4 + Math.random() * 1.2; this.cricket(); }
    }
  }
  env(g, t0, a, peak, rel) {
    g.gain.setValueAtTime(0.0001, t0);
    g.gain.exponentialRampToValueAtTime(peak, t0 + a);
    g.gain.exponentialRampToValueAtTime(0.0001, t0 + a + rel);
  }
  tone(freq, type, dur, vol, glideTo = null, delay = 0) {
    if (!this.ctx) return;
    const c = this.ctx, t = c.currentTime + delay;
    const o = c.createOscillator(); o.type = type; o.frequency.setValueAtTime(freq, t);
    if (glideTo) o.frequency.exponentialRampToValueAtTime(glideTo, t + dur);
    const g = c.createGain();
    o.connect(g); g.connect(this.master);
    this.env(g, t, 0.01, vol, dur);
    o.start(t); o.stop(t + dur + 0.05);
    return o;
  }
  noise(dur, freq, vol, type = 'lowpass', delay = 0) {
    if (!this.ctx) return;
    const c = this.ctx, t = c.currentTime + delay;
    const s = c.createBufferSource(); s.buffer = this.noiseBuf;
    const f = c.createBiquadFilter(); f.type = type; f.frequency.value = freq;
    const g = c.createGain();
    s.connect(f); f.connect(g); g.connect(this.master);
    this.env(g, t, 0.005, vol, dur);
    s.start(t, Math.random()); s.stop(t + dur + 0.05);
  }
  bird() {
    const base = 2200 + Math.random() * 1800;
    const n = 2 + Math.floor(Math.random() * 4);
    for (let i = 0; i < n; i++) this.tone(base * (1 + Math.random() * 0.2), 'sine', 0.08, 0.03, base * (1.3 + Math.random() * 0.4), i * 0.11);
  }
  cricket() {
    for (let i = 0; i < 3; i++) this.tone(4300, 'square', 0.025, 0.008, null, i * 0.05);
  }
  step(surface) {
    if (surface === 'wood') this.noise(0.06, 900, 0.12, 'bandpass');
    else if (surface === 'snow') this.noise(0.12, 1800, 0.07, 'highpass');
    else if (surface === 'water') this.noise(0.15, 700, 0.1, 'bandpass');
    else this.noise(0.08, 600, 0.06);
  }
  chop() { this.noise(0.12, 500, 0.4, 'bandpass'); this.tone(180, 'triangle', 0.08, 0.2, 90); }
  hoe() { this.noise(0.15, 350, 0.3); }
  splash() { this.noise(0.35, 1200, 0.25, 'bandpass'); }
  pour() { this.noise(0.6, 2000, 0.12, 'bandpass'); }
  coin() { this.tone(988, 'square', 0.08, 0.06); this.tone(1318, 'square', 0.18, 0.06, null, 0.08); }
  pop() { this.tone(600, 'sine', 0.08, 0.12, 1200); }
  bad() { this.tone(220, 'square', 0.15, 0.05, 160); }
  fall() { this.noise(0.9, 300, 0.4); this.tone(90, 'sine', 0.5, 0.3, 40, 0.6); }
  thunder() { this.noise(2.5, 160, 0.6); }
  bite() { this.tone(1200, 'sine', 0.06, 0.15); this.tone(1500, 'sine', 0.06, 0.15, null, 0.08); }
  animal(kind) {
    if (!this.ctx) return;
    switch (kind) {
      case 'inek': case 'boga': {
        const o = this.tone(kind === 'boga' ? 95 : 130, 'sawtooth', 1.1, 0.12, kind === 'boga' ? 70 : 95);
        break;
      }
      case 'koyun': case 'keci': {
        const c = this.ctx, t = c.currentTime;
        const o = c.createOscillator(); o.type = 'sawtooth'; o.frequency.value = kind === 'keci' ? 420 : 330;
        const lfo = c.createOscillator(); lfo.frequency.value = 22; const lg = c.createGain(); lg.gain.value = 40;
        lfo.connect(lg); lg.connect(o.frequency);
        const f = c.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 1100;
        const g = c.createGain(); o.connect(f); f.connect(g); g.connect(this.master);
        this.env(g, t, 0.03, 0.12, 0.6);
        o.start(t); lfo.start(t); o.stop(t + 0.7); lfo.stop(t + 0.7);
        break;
      }
      case 'tavuk': case 'horoz':
        if (kind === 'horoz') { this.tone(600, 'square', 0.15, 0.05, 800); this.tone(800, 'square', 0.5, 0.05, 500, 0.18); }
        else for (let i = 0; i < 3; i++) this.tone(700 + Math.random() * 200, 'square', 0.05, 0.04, 500, i * 0.09);
        break;
      case 'kaz': for (let i = 0; i < 2; i++) this.tone(380, 'square', 0.12, 0.06, 300, i * 0.18); break;
      case 'domuz': this.noise(0.3, 300, 0.25, 'bandpass'); this.tone(140, 'sawtooth', 0.25, 0.08, 110); break;
      case 'at': this.tone(700, 'sawtooth', 0.8, 0.06, 300); break;
      case 'tavsan': this.tone(1800, 'sine', 0.05, 0.03); break;
    }
  }
}
