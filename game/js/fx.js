// Görsel efektler: GPU partikülleri, kılıç izleri, şok dalgaları, ışık patlamaları, hasar sayıları, uyarı alanları
import * as THREE from 'three';
import { tex } from './textures.js';
import { rand, clamp } from './util.js';
import { heightAt } from './terrain.js';

const MAXP = 6000;
export class Particles {
  constructor(scene, additive = true) {
    const g = new THREE.BufferGeometry();
    this.pos = new Float32Array(MAXP * 3); this.col = new Float32Array(MAXP * 4); this.size = new Float32Array(MAXP);
    g.setAttribute('position', new THREE.BufferAttribute(this.pos, 3).setUsage(THREE.DynamicDrawUsage));
    g.setAttribute('aColor', new THREE.BufferAttribute(this.col, 4).setUsage(THREE.DynamicDrawUsage));
    g.setAttribute('size', new THREE.BufferAttribute(this.size, 1).setUsage(THREE.DynamicDrawUsage));
    this.mat = new THREE.ShaderMaterial({
      uniforms: { map: { value: tex('glow') }, scale: { value: 600 } }, transparent: true, depthWrite: false,
      blending: additive ? THREE.AdditiveBlending : THREE.NormalBlending,
      vertexShader: `attribute float size; attribute vec4 aColor; varying vec4 vC; uniform float scale;
        void main(){ vC = aColor; vec4 mv = modelViewMatrix*vec4(position,1.0); gl_PointSize = size*scale/max(0.5,-mv.z); gl_Position = projectionMatrix*mv; }`,
      fragmentShader: `uniform sampler2D map; varying vec4 vC; void main(){ vec4 t = texture2D(map, gl_PointCoord); gl_FragColor = vec4(vC.rgb, vC.a*t.a); if (gl_FragColor.a < 0.004) discard; }`,
    });
    this.points = new THREE.Points(g, this.mat); this.points.frustumCulled = false; this.points.renderOrder = 5; scene.add(this.points);
    this.geo = g; this.list = []; this.additive = additive;
  }
  // o: {x,y,z, vx,vy,vz, life, size, size2, r,g,b, a, grav, drag, r2,g2,b2}
  add(o) { if (this.list.length >= MAXP) this.list.shift(); o.age = 0; this.list.push(o); return o; }
  update(dt) {
    const L = this.list; let n = 0;
    for (let i = 0; i < L.length; i++) {
      const p = L[i]; p.age += dt; if (p.age >= p.life) continue;
      const d = Math.exp(-(p.drag || 0) * dt); p.vx *= d; p.vy *= d; p.vz *= d; p.vy -= (p.grav || 0) * dt;
      p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
      if (p.floor) { const h = heightAt(p.x, p.z); if (p.y < h + 0.05) { p.y = h + 0.05; p.vy *= -0.3; p.vx *= 0.6; p.vz *= 0.6; } }
      if (p.orbit) { p.x = p.orbit.x + Math.cos(p.age * p.os + p.oa) * p.or; p.z = p.orbit.z + Math.sin(p.age * p.os + p.oa) * p.or; }
      L[n++] = p;
      const k = p.age / p.life, j = (n - 1);
      this.pos[j * 3] = p.x; this.pos[j * 3 + 1] = p.y; this.pos[j * 3 + 2] = p.z;
      const fadeIn = clamp(p.age / (p.fin || 0.05), 0, 1);
      const r = p.r2 != null ? p.r + (p.r2 - p.r) * k : p.r, g = p.g2 != null ? p.g + (p.g2 - p.g) * k : p.g, b = p.b2 != null ? p.b + (p.b2 - p.b) * k : p.b;
      this.col[j * 4] = r; this.col[j * 4 + 1] = g; this.col[j * 4 + 2] = b; this.col[j * 4 + 3] = (p.a ?? 1) * (1 - k) * fadeIn;
      this.size[j] = p.size + ((p.size2 ?? p.size) - p.size) * k;
    }
    L.length = n;
    this.geo.setDrawRange(0, n);
    this.geo.attributes.position.needsUpdate = true; this.geo.attributes.aColor.needsUpdate = true; this.geo.attributes.size.needsUpdate = true;
  }
}

// Kılıç izi şeridi
export class Trail {
  constructor(scene, color = 0xffffff, len = 14, width = 1) {
    this.len = len; this.pts = []; this.color = new THREE.Color(color);
    const g = new THREE.BufferGeometry(); this.pos = new Float32Array(len * 2 * 3); this.al = new Float32Array(len * 2);
    g.setAttribute('position', new THREE.BufferAttribute(this.pos, 3)); g.setAttribute('alpha', new THREE.BufferAttribute(this.al, 1));
    const idx = []; for (let i = 0; i < len - 1; i++) { const a = i * 2; idx.push(a, a + 1, a + 2, a + 1, a + 3, a + 2); } g.setIndex(idx);
    this.mat = new THREE.ShaderMaterial({ uniforms: { color: { value: this.color }, op: { value: 0 } }, transparent: true, depthWrite: false, side: THREE.DoubleSide, blending: THREE.AdditiveBlending,
      vertexShader: 'attribute float alpha; varying float vA; void main(){ vA = alpha; gl_Position = projectionMatrix*modelViewMatrix*vec4(position,1.0); }',
      fragmentShader: 'uniform vec3 color; uniform float op; varying float vA; void main(){ gl_FragColor = vec4(color*1.8, vA*vA*op); }' });
    this.mesh = new THREE.Mesh(g, this.mat); this.mesh.frustumCulled = false; this.mesh.renderOrder = 6; scene.add(this.mesh); this.geo = g; this.active = false;
  }
  push(a, b) { this.pts.unshift([a.clone(), b.clone()]); if (this.pts.length > this.len) this.pts.pop(); }
  update(dt, base, tip, on) {
    this.mat.uniforms.op.value = clamp(this.mat.uniforms.op.value + (on ? 12 : -5) * dt, 0, 0.9);
    if (on) this.push(base, tip); else if (this.pts.length) this.pts.pop();
    const n = this.pts.length;
    for (let i = 0; i < this.len; i++) {
      const p = this.pts[Math.min(i, n - 1)]; if (!p) { this.al[i * 2] = this.al[i * 2 + 1] = 0; continue; }
      this.pos.set([p[0].x, p[0].y, p[0].z], i * 6); this.pos.set([p[1].x, p[1].y, p[1].z], i * 6 + 3);
      const a = i < n ? 1 - i / this.len : 0; this.al[i * 2] = a * 0.2; this.al[i * 2 + 1] = a;
    }
    this.geo.attributes.position.needsUpdate = true; this.geo.attributes.alpha.needsUpdate = true;
  }
}

export class FX {
  constructor(scene, camera, q) {
    this.scene = scene; this.camera = camera; this.q = q;
    this.add = new Particles(scene, true); this.norm = new Particles(scene, false);
    this.rings = []; this.shake = 0; this.flashT = 0; this.decals = [];
    this.lightPool = [];
    for (let i = 0; i < (q.lights > 1 ? 3 : 1); i++) { const l = new THREE.PointLight(0xffffff, 0, 16, 1.5); scene.add(l); this.lightPool.push({ l, t: 0, life: 1, base: 0 }); }
    this.numLayer = document.getElementById('dmgLayer'); this.nums = [];
    this.ringGeo = new THREE.PlaneGeometry(1, 1); this.ringGeo.rotateX(-Math.PI / 2);
  }
  // ---- temel yayıcılar ----
  burst(pos, n, o = {}) {
    const q = this.q.particleMul || 1; n = Math.ceil(n * q);
    for (let i = 0; i < n; i++) {
      const sp = rand(o.speedMin ?? (o.speed ?? 5) * 0.3, o.speed ?? 5); const th = rand(0, Math.PI * 2), ph = Math.acos(rand(-1, 1));
      let vx = Math.sin(ph) * Math.cos(th) * sp, vy = Math.cos(ph) * sp, vz = Math.sin(ph) * Math.sin(th) * sp;
      if (o.up) vy = Math.abs(vy) * o.up;
      if (o.dir) { vx += o.dir.x * (o.dirSpeed ?? 0); vy += o.dir.y * (o.dirSpeed ?? 0); vz += o.dir.z * (o.dirSpeed ?? 0); }
      const c = o.color || [1, 0.8, 0.4];
      (o.normal ? this.norm : this.add).add({ x: pos.x + rand(-1, 1) * (o.spread || 0), y: pos.y + rand(-1, 1) * (o.spreadY ?? o.spread ?? 0), z: pos.z + rand(-1, 1) * (o.spread || 0), vx, vy, vz,
        life: rand((o.life ?? 0.6) * 0.6, o.life ?? 0.6), size: o.size ?? 0.3, size2: o.size2, r: c[0], g: c[1], b: c[2], r2: o.color2?.[0], g2: o.color2?.[1], b2: o.color2?.[2], a: o.alpha ?? 1, grav: o.grav ?? 0, drag: o.drag ?? 2, floor: o.floor, fin: o.fin });
    }
  }
  sparks(pos, color = [1, 0.8, 0.4], n = 14) { this.burst(pos, n, { speed: 9, life: 0.35, size: 0.12, size2: 0.02, color, grav: 12, drag: 3 }); this.burst(pos, 1, { speed: 0, life: 0.12, size: 1.6, size2: 2.4, color }); }
  blood(pos, color = [0.6, 0.05, 0.05]) { this.burst(pos, 10, { speed: 5, life: 0.6, size: 0.14, size2: 0.05, color, grav: 14, drag: 1, normal: true, floor: true }); }
  dust(pos, n = 8, color = [0.6, 0.55, 0.45]) { this.burst(pos, n, { speed: 2.5, up: 0.4, life: 0.9, size: 0.5, size2: 1.4, color, drag: 3, alpha: 0.45, normal: true, spread: 0.3 }); }
  explosion(pos, r = 3, color = [1, 0.5, 0.15], color2 = [0.3, 0.05, 0]) {
    this.burst(pos, 40, { speed: r * 4, life: 0.6, size: 0.6, size2: 1.2, color, color2, drag: 4 });
    this.burst(pos, 18, { speed: r * 6, life: 0.5, size: 0.12, color: [1, 0.9, 0.6], grav: 10, drag: 1.5 });
    this.burst(pos, 14, { speed: r * 1.5, up: 1, life: 1.4, size: 0.8, size2: 2.5, color: [0.2, 0.18, 0.16], drag: 2, alpha: 0.5, normal: true, spread: r * 0.3 });
    this.burst(pos, 1, { speed: 0, life: 0.18, size: r * 2.5, size2: r * 4, color });
    this.ring(pos, r * 1.3, color, 0.4); this.light(pos, color, 40, 0.35); this.addShake(0.35);
  }
  ring(pos, radius, color = [1, 1, 1], life = 0.5, width = 1) {
    const m = new THREE.Mesh(this.ringGeo, new THREE.MeshBasicMaterial({ map: tex('ring'), color: new THREE.Color(...color), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false }));
    m.position.set(pos.x, heightAt(pos.x, pos.z) + 0.15, pos.z); m.renderOrder = 4; this.scene.add(m); this.rings.push({ m, t: 0, life, radius });
  }
  light(pos, color, intensity = 20, life = 0.3) {
    let best = this.lightPool[0]; for (const L of this.lightPool) if (L.t >= L.life) { best = L; break; } else if (L.life - L.t < best.life - best.t) best = L;
    best.l.position.copy(pos); best.l.color.setRGB(...color); best.base = intensity; best.t = 0; best.life = life;
  }
  addShake(v) { this.shake = Math.min(1, this.shake + v); }
  // ---- uyarı alanı (düşman büyük saldırıları) ----
  telegraph(x, z, radius, time, color = 0xff3020, shape = 'circle', angle = 0, arc = Math.PI / 3) {
    let geo;
    if (shape === 'circle') geo = this.ringGeo;
    else if (shape === 'cone') { geo = new THREE.CircleGeometry(1, 24, -arc / 2 - Math.PI / 2, arc); geo.rotateX(-Math.PI / 2); }
    else if (shape === 'line') { geo = new THREE.PlaneGeometry(1, 1); geo.translate(0, 0.5, 0); geo.rotateX(-Math.PI / 2); geo.scale(1, 1, -1); }
    const mat = new THREE.MeshBasicMaterial({ map: shape === 'circle' ? tex('telegraph') : null, color, transparent: true, opacity: 0.5, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
    const m = new THREE.Mesh(geo, mat); m.position.set(x, heightAt(x, z) + 0.2, z); m.renderOrder = 3;
    if (shape === 'circle' || shape === 'cone') m.scale.set(radius * (shape === 'circle' ? 2 : 1), 1, radius * (shape === 'circle' ? 2 : 1));
    else m.scale.set(arc, 1, radius);
    m.rotation.y = angle; this.scene.add(m);
    const inner = new THREE.Mesh(geo, mat.clone()); inner.position.copy(m.position); inner.position.y += 0.02; inner.rotation.y = angle; inner.scale.set(0.01, 1, 0.01); inner.material.opacity = 0.35; inner.renderOrder = 3; this.scene.add(inner);
    const d = { m, inner, t: 0, time, shape, radius, arc, own: shape !== 'circle' }; this.decals.push(d); return d;
  }
  // ---- hasar sayıları ----
  number(pos, val, type = 'dmg') {
    const el = document.createElement('div'); el.className = 'dnum ' + type; el.textContent = typeof val === 'number' ? Math.round(val) : val;
    this.numLayer.appendChild(el); this.nums.push({ el, pos: pos.clone(), t: 0, vx: rand(-0.6, 0.6), life: type === 'crit' ? 1.1 : 0.85 });
  }
  update(dt, rdt) {
    this.add.update(dt); this.norm.update(dt);
    for (let i = this.rings.length - 1; i >= 0; i--) {
      const r = this.rings[i]; r.t += dt; const k = r.t / r.life; const s = r.radius * 2 * (0.2 + k * 0.8);
      r.m.scale.set(s, 1, s); r.m.material.opacity = 1 - k;
      if (k >= 1) { this.scene.remove(r.m); r.m.material.dispose(); this.rings.splice(i, 1); }
    }
    for (let i = this.decals.length - 1; i >= 0; i--) {
      const d = this.decals[i]; d.t += dt; const k = clamp(d.t / d.time, 0, 1);
      if (d.shape === 'circle' || d.shape === 'cone') { const s = d.radius * (d.shape === 'circle' ? 2 : 1) * k; d.inner.scale.set(s, 1, s); }
      else d.inner.scale.set(d.arc, 1, d.radius * k);
      d.m.material.opacity = 0.35 + Math.sin(d.t * 20) * 0.1;
      if (d.t >= d.time + 0.08) { this.scene.remove(d.m); this.scene.remove(d.inner); d.m.material.dispose(); d.inner.material.dispose(); if (d.own) d.m.geometry.dispose(); this.decals.splice(i, 1); }
    }
    for (const L of this.lightPool) { L.t += dt; const k = clamp(L.t / L.life, 0, 1); L.l.intensity = L.base * (1 - k) * (1 - k); }
    this.shake = Math.max(0, this.shake - rdt * 1.6);
    const v = new THREE.Vector3(); const W = innerWidth, Hh = innerHeight;
    for (let i = this.nums.length - 1; i >= 0; i--) {
      const n = this.nums[i]; n.t += rdt; const k = n.t / n.life;
      v.copy(n.pos); v.y += k * 1.4; v.x += n.vx * k; v.project(this.camera);
      if (v.z > 1 || k >= 1) { if (k >= 1) { n.el.remove(); this.nums.splice(i, 1); } else n.el.style.opacity = 0; continue; }
      const sc = k < 0.12 ? 0.5 + k * 6 : 1.2 - k * 0.3;
      n.el.style.transform = `translate(${(v.x * 0.5 + 0.5) * W}px, ${(-v.y * 0.5 + 0.5) * Hh}px) translate(-50%,-50%) scale(${sc})`; n.el.style.opacity = k > 0.7 ? (1 - k) / 0.3 : 1;
    }
  }
  // Ortam partikülleri (biyoma göre): kar, kül, kum, ateş böceği, yaprak
  ambient(dt, cam, weights, night) {
    const q = this.q.particleMul || 1; const c = cam.position;
    const spawn = (rate, fn) => { let n = rate * dt * q; while (n > 0) { if (Math.random() < n) fn(); n -= 1; } };
    const around = (r, yMin, yMax) => ({ x: c.x + rand(-r, r), y: c.y + rand(yMin, yMax), z: c.z + rand(-r, r) });
    if (weights.snow > 0.3) spawn(90 * weights.snow, () => { const p = around(30, 0, 18); this.norm.add({ ...p, vx: rand(-0.5, 0.5) + 0.8, vy: rand(-2.2, -1.2), vz: rand(-0.5, 0.5), life: 7, size: 0.08, r: 1, g: 1, b: 1, a: 0.9, fin: 0.5 }); });
    if (weights.volcano > 0.3) { spawn(40 * weights.volcano, () => { const p = around(30, -6, 12); this.add.add({ ...p, vx: rand(-0.5, 0.5), vy: rand(0.5, 1.8), vz: rand(-0.5, 0.5), life: 4, size: 0.07, r: 1, g: 0.45, b: 0.1, a: 1, fin: 0.3 }); }); spawn(30 * weights.volcano, () => { const p = around(30, 0, 16); this.norm.add({ ...p, vx: rand(-0.3, 0.3), vy: rand(-0.6, -0.2), vz: rand(-0.3, 0.3), life: 6, size: 0.07, r: 0.25, g: 0.23, b: 0.22, a: 0.8, fin: 0.5 }); }); }
    if (weights.desert > 0.4) spawn(25 * weights.desert, () => { const p = around(30, -2, 5); this.norm.add({ ...p, vx: rand(4, 7), vy: rand(-0.2, 0.3), vz: rand(-1, 1), life: 3, size: 0.9, size2: 1.8, r: 0.85, g: 0.7, b: 0.5, a: 0.12, fin: 0.6 }); });
    if (weights.forest > 0.3) { spawn(10 * weights.forest, () => { const p = around(25, 2, 12); this.norm.add({ ...p, vx: rand(0.2, 1), vy: rand(-1, -0.4), vz: rand(-0.4, 0.4), life: 6, size: 0.1, r: 0.55, g: 0.45, b: 0.15, a: 0.9, fin: 0.3 }); }); }
    if (night && (weights.forest + weights.grass) > 0.4) spawn(8, () => { const p = around(22, -3, 3); this.add.add({ ...p, vx: rand(-0.4, 0.4), vy: rand(-0.2, 0.3), vz: rand(-0.4, 0.4), life: 4, size: 0.12, r: 0.7, g: 1, b: 0.3, a: 1, fin: 1, drag: 0 }); });
    if (weights.grass > 0.5 && !night) spawn(2, () => { const p = around(20, -1, 3); this.norm.add({ ...p, vx: rand(-0.6, 0.6), vy: rand(-0.1, 0.3), vz: rand(-0.6, 0.6), life: 5, size: 0.06, r: 1, g: 1, b: 0.9, a: 0.9, fin: 1, drag: 0 }); });
  }
  emit(dt, e) {
    const q = this.q.particleMul || 1; let n = e.rate * dt * q;
    while (n > 0) {
      if (Math.random() < n) {
        const p = e.pos;
        if (e.type === 'fire') { const s = e.size || 1; this.add.add({ x: p.x + rand(-0.35, 0.35) * s, y: p.y, z: p.z + rand(-0.35, 0.35) * s, vx: rand(-0.3, 0.3), vy: rand(1.5, 3) * s, vz: rand(-0.3, 0.3), life: rand(0.5, 0.9), size: 0.7 * s, size2: 0.15, r: 1, g: 0.75, b: 0.3, r2: 0.9, g2: 0.2, b2: 0.02, drag: 1 }); if (Math.random() < 0.15) this.add.add({ x: p.x, y: p.y + 0.5, z: p.z, vx: rand(-1, 1), vy: rand(2, 5), vz: rand(-1, 1), life: 1.4, size: 0.06, r: 1, g: 0.6, b: 0.2, drag: 1 }); }
        else if (e.type === 'smoke') this.norm.add({ x: p.x + rand(-0.2, 0.2), y: p.y, z: p.z + rand(-0.2, 0.2), vx: rand(0.2, 0.6), vy: rand(0.8, 1.4), vz: rand(-0.2, 0.2), life: 4, size: 0.5, size2: 2.6, r: 0.55, g: 0.55, b: 0.55, a: 0.28, drag: 0.3, fin: 0.5 });
        else if (e.type === 'ember') this.add.add({ x: p.x + rand(-0.4, 0.4), y: p.y, z: p.z + rand(-0.4, 0.4), vx: rand(-0.5, 0.5), vy: rand(1, 3), vz: rand(-0.5, 0.5), life: 1.2, size: 0.07, r: 1, g: 0.5, b: 0.1, drag: 1 });
        else if (e.type === 'rune') this.add.add({ x: p.x + rand(-1, 1), y: p.y - 2, z: p.z + rand(-1, 1), vx: 0, vy: rand(0.8, 1.6), vz: 0, life: 2, size: 0.12, r: 0.4, g: 0.85, b: 1, fin: 0.3, drag: 0 });
      }
      n -= 1;
    }
  }
}
