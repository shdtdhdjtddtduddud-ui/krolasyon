// Hayvan modelleri (iskeletli, animasyonlu) ve basit yapay zekâ
import * as THREE from 'three';
import * as T from './textures.js';
import { mat, tmat, box, cyl, sphere, cone, mergeChildren } from './kit.js';
import { heightAt } from './terrain.js';
import { angleDiff, clamp } from './util.js';

function shadows(g) { g.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } }); }

function legGroup(parent, x, y, z) {
  const g = new THREE.Group();
  g.position.set(x, y, z);
  parent.add(g);
  return g;
}

// Dört ayaklı genel kurucu
function quadruped(o) {
  const root = new THREE.Group();
  const body = new THREE.Group();
  body.position.y = o.hip;
  root.add(body);
  const bm = o.bodyMat;
  if (o.round) {
    sphere(body, 0.5, bm, 0, 0, 0, o.bw, o.bh, o.bl, 2);
  } else {
    box(body, o.bw, o.bh, o.bl, bm, 0, 0, 0);
    box(body, o.bw * 0.92, o.bh * 0.9, 0.12, bm, 0, -0.02, o.bl / 2 + 0.03);
    box(body, o.bw * 0.92, o.bh * 0.9, 0.12, bm, 0, -0.02, -o.bl / 2 - 0.03);
  }
  const legs = [];
  const lx = o.bw / 2 - o.legT / 2 - 0.02, lz = o.bl / 2 - o.legT / 2 - 0.06;
  for (const [sx, sz] of [[-1, 1], [1, 1], [-1, -1], [1, -1]]) {
    const L = legGroup(body, sx * lx, -o.bh * 0.35, sz * lz);
    box(L, o.legT, o.legL, o.legT, o.legMat, 0, -o.legL / 2, 0);
    box(L, o.legT * 1.05, o.legL * 0.14, o.legT * 1.1, o.hoofMat, 0, -o.legL + o.legL * 0.07 - 0.0, 0.01);
    legs.push(L);
  }
  // boyun + baş
  const neck = new THREE.Group();
  neck.position.set(0, o.neckY, o.bl / 2 - 0.05);
  body.add(neck);
  neck.rotation.x = o.neckTilt || 0;
  if (o.neckL > 0) box(neck, o.neckW, o.neckW * 1.1, o.neckL, o.neckMat || bm, 0, 0, o.neckL / 2 - 0.05);
  const head = new THREE.Group();
  head.position.set(0, o.headUp || 0, o.neckL);
  neck.add(head);
  // kuyruk
  const tail = new THREE.Group();
  tail.position.set(0, o.bh * 0.3, -o.bl / 2 - 0.05);
  body.add(tail);
  return { root, body, legs, neck, head, tail };
}

const SPECIES_BUILD = {
  inek(babyTint) {
    const p = quadruped({
      hip: 1.12, bw: 0.85, bh: 0.8, bl: 1.75, legT: 0.18, legL: 0.75,
      bodyMat: tmat(T.cowSpots()), legMat: mat('#efe7d8'), hoofMat: mat('#2c2420'),
      neckY: 0.15, neckL: 0.3, neckW: 0.45, neckTilt: -0.2, headUp: 0.05,
    });
    const h = p.head;
    box(h, 0.42, 0.42, 0.55, mat('#f3efe6'), 0, 0, 0.2);
    box(h, 0.38, 0.22, 0.2, mat('#e7a9a0'), 0, -0.1, 0.52);
    for (const s of [-1, 1]) {
      box(h, 0.05, 0.05, 0.02, mat('#151515'), s * 0.17, 0.08, 0.45);
      box(h, 0.22, 0.08, 0.12, mat('#7a4a2a'), s * 0.3, 0.12, 0.05, 0, 0, s * -0.4);
      cone(h, 0.04, 0.16, 5, mat('#e8dcc0'), s * 0.15, 0.27, 0.1, 0, 0, s * -0.5);
    }
    sphere(p.body, 0.18, mat('#e7a9a0'), 0, -0.42, -0.4, 1.2, 0.7, 1);
    box(p.tail, 0.05, 0.7, 0.05, mat('#efe7d8'), 0, -0.35, 0);
    box(p.tail, 0.1, 0.16, 0.1, mat('#7a4a2a'), 0, -0.72, 0);
    p.tail.rotation.x = 0.15;
    return p;
  },
  boga() {
    const p = quadruped({
      hip: 1.18, bw: 0.95, bh: 0.9, bl: 1.85, legT: 0.2, legL: 0.78,
      bodyMat: mat('#6e3a24'), legMat: mat('#5e3020'), hoofMat: mat('#1e1714'),
      neckY: 0.18, neckL: 0.3, neckW: 0.55, neckTilt: -0.15,
    });
    const h = p.head;
    box(h, 0.48, 0.46, 0.55, mat('#6e3a24'), 0, 0, 0.2);
    box(h, 0.42, 0.24, 0.2, mat('#3d2218'), 0, -0.1, 0.52);
    const ring = new THREE.Mesh(new THREE.TorusGeometry(0.06, 0.012, 4, 10), mat('#c9a34a', { metalness: 0.9, roughness: 0.3 }));
    ring.position.set(0, -0.22, 0.62); h.add(ring);
    for (const s of [-1, 1]) {
      box(h, 0.05, 0.05, 0.02, mat('#151515'), s * 0.19, 0.08, 0.45);
      const horn = cone(h, 0.06, 0.38, 6, mat('#efe3c8'), s * 0.36, 0.24, 0.1, 0, 0, s * -1.1);
    }
    box(p.tail, 0.05, 0.7, 0.05, mat('#5e3020'), 0, -0.35, 0);
    box(p.tail, 0.1, 0.16, 0.1, mat('#2e1810'), 0, -0.72, 0);
    return p;
  },
  at() {
    const coat = mat('#8a5332');
    const p = quadruped({
      hip: 1.42, bw: 0.62, bh: 0.72, bl: 1.6, legT: 0.13, legL: 1.05,
      bodyMat: coat, legMat: mat('#7a4628'), hoofMat: mat('#2a211c'),
      neckY: 0.25, neckL: 0.75, neckW: 0.3, neckTilt: -0.85, headUp: 0.02,
    });
    // beyaz çoraplar
    for (const L of p.legs.slice(2)) box(L, 0.14, 0.22, 0.14, mat('#eee6d6'), 0, -0.86, 0);
    const h = p.head;
    h.rotation.x = 1.1;
    box(h, 0.24, 0.28, 0.62, coat, 0, -0.04, 0.22);
    box(h, 0.22, 0.2, 0.18, mat('#6a3a20'), 0, -0.1, 0.55);
    box(h, 0.06, 0.2, 0.3, mat('#f2ead8'), 0, 0.06, 0.4);
    for (const s of [-1, 1]) {
      cone(h, 0.05, 0.16, 4, coat, s * 0.08, 0.2, -0.02, -0.3);
      box(h, 0.04, 0.05, 0.02, mat('#111'), s * 0.12, 0.05, 0.15);
    }
    // yele
    const mane = mat('#e8cf9a');
    for (let i = 0; i < 6; i++) box(p.neck, 0.08, 0.18, 0.14, mane, 0, 0.18, 0.05 + i * 0.12);
    box(h, 0.1, 0.1, 0.2, mane, 0, 0.17, 0.05);
    box(p.tail, 0.12, 0.85, 0.12, mane, 0, -0.42, -0.05);
    p.tail.rotation.x = 0.35;
    return p;
  },
  koyun() {
    const wool = tmat(T.wool());
    const p = quadruped({
      hip: 0.62, bw: 0.75, bh: 0.62, bl: 1.0, legT: 0.08, legL: 0.42, round: false,
      bodyMat: wool, legMat: mat('#3a3633'), hoofMat: mat('#1e1b19'),
      neckY: 0.12, neckL: 0.12, neckW: 0.25, neckTilt: -0.1, headUp: 0.05,
    });
    // yün topakları
    p.woolParts = [];
    for (let i = 0; i < 9; i++) {
      const a = i * 2.3;
      const s = sphere(p.body, 0.25, wool, Math.sin(a) * 0.3, 0.12 + Math.cos(a * 1.3) * 0.12, -0.4 + (i / 8) * 0.8, 1, 0.9, 1, 1);
      s.userData.dynamic = true;
      p.woolParts.push(s);
    }
    const h = p.head;
    box(h, 0.26, 0.3, 0.38, mat('#4a4542'), 0, 0, 0.12);
    sphere(h, 0.17, wool, 0, 0.14, 0.02, 1, 0.8, 1);
    for (const s of [-1, 1]) {
      box(h, 0.18, 0.06, 0.08, mat('#4a4542'), s * 0.18, 0.06, 0.02, 0, 0, s * 0.3);
      box(h, 0.04, 0.04, 0.02, mat('#d8c890'), s * 0.1, 0.05, 0.3);
    }
    sphere(p.tail, 0.1, wool, 0, -0.05, 0);
    return p;
  },
  keci() {
    const coat = mat('#efebe2');
    const p = quadruped({
      hip: 0.72, bw: 0.42, bh: 0.48, bl: 0.95, legT: 0.08, legL: 0.55,
      bodyMat: coat, legMat: coat, hoofMat: mat('#3a3330'),
      neckY: 0.12, neckL: 0.3, neckW: 0.18, neckTilt: -0.7,
    });
    const h = p.head;
    h.rotation.x = 0.9;
    box(h, 0.2, 0.22, 0.4, coat, 0, 0, 0.15);
    box(h, 0.08, 0.16, 0.06, mat('#d8d2c4'), 0, -0.16, 0.3);
    for (const s of [-1, 1]) {
      box(h, 0.16, 0.05, 0.08, coat, s * 0.15, 0.05, 0.0, 0, 0, s * -0.3);
      const horn = cone(h, 0.03, 0.22, 5, mat('#8a7a64'), s * 0.06, 0.18, -0.02, -0.7);
      box(h, 0.03, 0.03, 0.02, mat('#111'), s * 0.09, 0.04, 0.24);
    }
    box(p.tail, 0.05, 0.18, 0.05, coat, 0, 0.06, -0.02, -0.6);
    return p;
  },
  domuz() {
    const pink = mat('#f2b0b4', { roughness: 0.7 });
    const p = quadruped({
      hip: 0.48, bw: 0.62, bh: 0.55, bl: 1.05, legT: 0.12, legL: 0.3, round: true,
      bodyMat: pink, legMat: pink, hoofMat: mat('#8a5a5a'),
      neckY: 0.02, neckL: 0.05, neckW: 0.3,
    });
    const h = p.head;
    sphere(h, 0.24, pink, 0, 0, 0.12, 1, 0.95, 1);
    cyl(h, 0.11, 0.12, 0.1, 10, mat('#e8949a'), 0, -0.03, 0.36, Math.PI / 2);
    for (const s of [-1, 1]) {
      box(h, 0.03, 0.03, 0.02, mat('#6a3a3a'), s * 0.04, -0.03, 0.415);
      cone(h, 0.09, 0.16, 4, pink, s * 0.14, 0.2, 0.08, 0.6, 0, s * -0.3);
      box(h, 0.04, 0.04, 0.02, mat('#111'), s * 0.1, 0.07, 0.31);
    }
    const tl = new THREE.Mesh(new THREE.TorusGeometry(0.06, 0.018, 4, 10, Math.PI * 1.6), pink);
    p.tail.add(tl);
    return p;
  },
  tavsan() {
    const fur = mat('#b9b4ad');
    const root = new THREE.Group();
    const body = new THREE.Group(); body.position.y = 0.22; root.add(body);
    sphere(body, 0.2, fur, 0, 0, -0.02, 1, 0.9, 1.3);
    const head = new THREE.Group(); head.position.set(0, 0.12, 0.22); body.add(head);
    sphere(head, 0.13, fur, 0, 0, 0, 1, 1, 1.1);
    for (const s of [-1, 1]) {
      box(head, 0.06, 0.26, 0.03, fur, s * 0.05, 0.2, -0.02, -0.15, 0, s * 0.12);
      box(head, 0.025, 0.025, 0.02, mat('#111'), s * 0.07, 0.03, 0.11);
    }
    box(head, 0.03, 0.02, 0.02, mat('#e8a0a0'), 0, -0.02, 0.14);
    const tail = new THREE.Group(); tail.position.set(0, 0.04, -0.27); body.add(tail);
    sphere(tail, 0.07, mat('#f2efe8'), 0, 0, 0);
    const legs = [];
    for (const [sx, sz, l] of [[-1, 1, 0.12], [1, 1, 0.12], [-1, -1, 0.16], [1, -1, 0.16]]) {
      const L = legGroup(body, sx * 0.09, -0.08, sz * 0.12);
      box(L, 0.06, l, sz > 0 ? 0.06 : 0.14, fur, 0, -l / 2, 0);
      legs.push(L);
    }
    return { root, body, legs, neck: head, head, tail, hopper: true };
  },
};

function bird(kind) {
  const root = new THREE.Group();
  const body = new THREE.Group();
  const isGoose = kind === 'kaz', isRooster = kind === 'horoz';
  const bodyC = isGoose ? '#f2f0ea' : isRooster ? '#c8551e' : '#d9823a';
  const bm = mat(bodyC, { flat: true });
  const legH = isGoose ? 0.28 : isRooster ? 0.26 : 0.2;
  body.position.y = legH + 0.12;
  root.add(body);
  const sz = isGoose ? 1.35 : isRooster ? 1.15 : 1;
  sphere(body, 0.17 * sz, bm, 0, 0, 0, 1, 0.95, 1.3, 1);
  // kanatlar
  for (const s of [-1, 1]) sphere(body, 0.11 * sz, mat(isGoose ? '#e3e0d8' : isRooster ? '#8a3a14' : '#b8692a', { flat: true }), s * 0.13 * sz, 0.02, -0.02, 0.4, 0.8, 1.3, 0);
  // kuyruk tüyleri
  const tail = new THREE.Group(); tail.position.set(0, 0.08 * sz, -0.2 * sz); body.add(tail);
  if (isRooster) {
    for (let i = 0; i < 4; i++) {
      const f = box(tail, 0.04, 0.38, 0.12, mat(i % 2 ? '#1f3b2c' : '#222831', { metalness: 0.3, roughness: 0.4 }), (i - 1.5) * 0.04, 0.15, -0.05);
      f.rotation.x = -0.5 - i * 0.12;
    }
  } else {
    cone(tail, 0.08 * sz, 0.18 * sz, 5, bm, 0, 0.04, 0, -1.1);
  }
  const neck = new THREE.Group(); neck.position.set(0, 0.1 * sz, 0.16 * sz); body.add(neck);
  if (isGoose) box(neck, 0.08, 0.38, 0.08, bm, 0, 0.18, 0);
  const head = new THREE.Group(); head.position.set(0, isGoose ? 0.4 : 0.1 * sz, isGoose ? 0.02 : 0.02); neck.add(head);
  sphere(head, 0.08 * sz, bm, 0, 0, 0, 1, 1.05, 1.1, 1);
  cone(head, 0.03 * sz, isGoose ? 0.16 : 0.08, 4, mat(isGoose ? '#e8892c' : '#e8b33a'), 0, -0.01, 0.1 * sz + (isGoose ? 0.05 : 0.02), Math.PI / 2);
  for (const s of [-1, 1]) box(head, 0.02, 0.025, 0.02, mat('#111'), s * 0.06 * sz, 0.02, 0.04 * sz);
  if (!isGoose) {
    const comb = mat('#d42a2a');
    const cs = isRooster ? 1.6 : 1;
    for (let i = 0; i < 3; i++) box(head, 0.025, 0.06 * cs, 0.04, comb, 0, 0.08 * sz + 0.02 * cs, -0.03 + i * 0.035);
    box(head, 0.03, 0.06 * cs, 0.03, comb, 0, -0.07, 0.07);
  }
  const legs = [];
  const lm = mat(isGoose ? '#e8892c' : '#e8b33a');
  for (const s of [-1, 1]) {
    const L = legGroup(body, s * 0.06 * sz, -0.1, 0);
    box(L, 0.025, legH, 0.025, lm, 0, -legH / 2, 0);
    box(L, 0.07, 0.015, 0.1, lm, 0, -legH, 0.03);
    legs.push(L);
  }
  return { root, body, legs, neck, head, tail, biped: true };
}

export function buildAnimalModel(kind) {
  let p;
  if (kind === 'tavuk' || kind === 'horoz' || kind === 'kaz') p = bird(kind);
  else p = SPECIES_BUILD[kind]();
  shadows(p.root);
  mergeChildren(p.root);
  return p;
}

// Hayvan nesnesi: model + davranış
export class Animal {
  constructor(data, area, rnd) {
    this.data = data; // kalıcı durum (game state içindeki kayıt)
    this.kind = data.kind;
    this.area = area; // {x0,x1,z0,z1}
    this.rnd = rnd;
    this.parts = buildAnimalModel(this.kind);
    this.group = this.parts.root;
    this.pos = new THREE.Vector3(data.x ?? (area.x0 + area.x1) / 2, 0, data.z ?? (area.z0 + area.z1) / 2);
    this.heading = rnd() * Math.PI * 2;
    this.target = null;
    this.state = 'idle';
    this.timer = 1 + rnd() * 3;
    this.phase = rnd() * 10;
    this.speed = 0;
    this.walkSpeed = { inek: 0.9, boga: 0.9, at: 1.3, koyun: 0.8, keci: 1.0, domuz: 0.8, tavsan: 1.6, tavuk: 0.7, horoz: 0.7, kaz: 0.8 }[this.kind] || 1;
    this.radius = { inek: 0.9, boga: 1.0, at: 0.9, koyun: 0.55, keci: 0.45, domuz: 0.55, tavsan: 0.2, tavuk: 0.2, horoz: 0.22, kaz: 0.28 }[this.kind] || 0.5;
    this.ridden = false;
    this.followPlayer = false;
    this.happyFx = 0;
    this.applyScale();
  }
  applyScale() {
    const age = this.data.age ?? 10;
    const s = age >= 5 ? 1 : 0.55 + (age / 5) * 0.45;
    this.group.scale.setScalar(s);
    if (this.parts.woolParts) for (const w of this.parts.woolParts) w.visible = !this.data.sheared;
  }
  update(dt, ctx) {
    const P = this.parts;
    if (this.ridden) return;
    this.timer -= dt;
    const a = this.area;
    if (this.state === 'idle' || this.state === 'graze') {
      this.speed = Math.max(0, this.speed - dt * 2);
      if (this.timer <= 0) {
        const r = this.rnd();
        if (r < 0.55) {
          this.state = 'walk';
          this.target = new THREE.Vector3(a.x0 + 1 + this.rnd() * (a.x1 - a.x0 - 2), 0, a.z0 + 1 + this.rnd() * (a.z1 - a.z0 - 2));
          this.timer = 12;
        } else if (r < 0.85 && !P.biped && !P.hopper) {
          this.state = 'graze'; this.timer = 3 + this.rnd() * 6;
        } else { this.state = 'idle'; this.timer = 2 + this.rnd() * 4; }
      }
    }
    if (this.state === 'walk') {
      const dx = this.target.x - this.pos.x, dz = this.target.z - this.pos.z;
      const d = Math.hypot(dx, dz);
      if (d < 0.4 || this.timer <= 0) { this.state = 'idle'; this.timer = 1 + this.rnd() * 3; }
      else {
        const want = Math.atan2(dx, dz);
        this.heading += clamp(angleDiff(this.heading, want), -dt * 2.5, dt * 2.5);
        this.speed = Math.min(this.walkSpeed, this.speed + dt * 2);
      }
    }
    // ilerle
    if (this.speed > 0.01) {
      this.pos.x += Math.sin(this.heading) * this.speed * dt;
      this.pos.z += Math.cos(this.heading) * this.speed * dt;
      this.pos.x = clamp(this.pos.x, a.x0 + 0.5, a.x1 - 0.5);
      this.pos.z = clamp(this.pos.z, a.z0 + 0.5, a.z1 - 0.5);
    }
    // oyuncuya çarpmasın: oyuncuyu it değil kendini kenara çek
    if (ctx.player) {
      const dx = this.pos.x - ctx.player.x, dz = this.pos.z - ctx.player.z;
      const d = Math.hypot(dx, dz), min = this.radius + 0.4;
      if (d < min && d > 0.001) { this.pos.x += (dx / d) * (min - d) * 0.5; this.pos.z += (dz / d) * (min - d) * 0.5; }
    }
    this.pos.y = heightAt(this.pos.x, this.pos.z);
    this.group.position.copy(this.pos);
    this.group.rotation.y = this.heading;
    this.data.x = this.pos.x; this.data.z = this.pos.z;
    this.animate(dt, ctx);
  }
  animate(dt, ctx) {
    const P = this.parts;
    const moving = this.speed > 0.05;
    this.phase += dt * (moving ? this.speed * (P.biped ? 9 : P.hopper ? 7 : 4.5) : 0);
    const sw = moving ? Math.sin(this.phase) : 0;
    if (P.hopper) {
      const hop = moving ? Math.abs(Math.sin(this.phase)) : 0;
      P.body.position.y = 0.22 + hop * 0.18;
      P.legs.forEach((L, i) => { L.rotation.x = moving ? (i < 2 ? -1 : 1) * Math.sin(this.phase) * 0.6 : 0; });
      P.head.rotation.x = Math.sin(ctx.time * 6 + this.phase) * 0.05;
      return;
    }
    if (P.biped) {
      P.legs[0].rotation.x = sw * 0.7; P.legs[1].rotation.x = -sw * 0.7;
      // gagalama / kafa sallama
      const peck = !moving && this.state === 'idle' ? Math.max(0, Math.sin(ctx.time * 3 + this.phase * 3)) : 0;
      P.neck.rotation.x = peck * 0.9 + (moving ? Math.sin(this.phase * 2) * 0.15 : 0);
      P.body.rotation.z = moving ? sw * 0.06 : 0;
      P.tail.rotation.x = Math.sin(ctx.time * 2 + this.phase) * 0.08;
      return;
    }
    P.legs[0].rotation.x = sw * 0.45; P.legs[3].rotation.x = sw * 0.45;
    P.legs[1].rotation.x = -sw * 0.45; P.legs[2].rotation.x = -sw * 0.45;
    P.body.position.y += 0; // gövde sabit
    const graze = this.state === 'graze' ? 1 : 0;
    this.grazeT = (this.grazeT || 0) + (graze - (this.grazeT || 0)) * Math.min(1, dt * 2);
    const baseTilt = this.kind === 'at' ? -0.85 : this.kind === 'keci' ? -0.7 : this.kind === 'inek' ? -0.2 : this.kind === 'boga' ? -0.15 : -0.1;
    P.neck.rotation.x = baseTilt + this.grazeT * 0.9 + (moving ? Math.sin(this.phase * 2) * 0.05 : Math.sin(ctx.time * 0.7 + this.phase) * 0.04);
    P.neck.rotation.y = this.grazeT > 0.5 ? Math.sin(ctx.time * 1.5) * 0.15 : Math.sin(ctx.time * 0.3 + this.phase) * 0.2;
    P.tail.rotation.z = Math.sin(ctx.time * 2.5 + this.phase) * 0.3;
    if (this.happyFx > 0) {
      this.happyFx -= dt;
      P.body.position.y = (P.baseY ??= P.body.position.y) + Math.abs(Math.sin(this.happyFx * 12)) * 0.08;
    }
  }
}
