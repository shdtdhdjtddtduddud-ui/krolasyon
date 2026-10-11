// Birinci şahıs oyuncu: hareket, kamera, elde tutulan aletler
import * as THREE from 'three';
import * as T from './textures.js';
import { mat, tmat, box, cyl, sphere, cone, rod } from './kit.js';
import { heightAt, slopeAt, distToRoad } from './terrain.js';
import { clamp, smoothstep } from './util.js';
import { UPPER_WATER_Y, WATERFALL_X } from './layout.js';

const EYE = 1.65;

function buildTools() {
  const tools = [];
  const skin = mat('#e0b090');
  const sleeve = mat('#5a7aa8');
  const handGrp = () => {
    const g = new THREE.Group();
    box(g, 0.075, 0.075, 0.12, sleeve, 0, -0.02, 0.07);
    box(g, 0.07, 0.065, 0.1, skin, 0, -0.01, -0.03);
    return g;
  };
  // 0 el
  const el = new THREE.Group();
  const h0 = handGrp(); h0.rotation.set(0.15, 0.1, 0); el.add(h0);
  tools.push(el);
  // 1 çapa
  const capa = new THREE.Group();
  capa.add(handGrp());
  rod(capa, [0, -0.3, -0.05], [0, 0.65, -0.12], 0.018, mat('#8a6a45'));
  const blade = box(capa, 0.2, 0.03, 0.14, mat('#8d9399', { metalness: 0.7, roughness: 0.35 }), 0, 0.65, -0.2);
  tools.push(capa);
  // 2 sulama kabı
  const can = new THREE.Group();
  can.add(handGrp());
  const cm = mat('#3f8a5a', { metalness: 0.4, roughness: 0.4 });
  cyl(can, 0.1, 0.11, 0.22, 12, cm, 0, -0.14, -0.1);
  rod(can, [0, -0.16, -0.2], [0, -0.04, -0.42], 0.02, cm);
  cyl(can, 0.03, 0.015, 0.05, 8, mat('#2f6a4a'), 0, -0.03, -0.44, -1.0);
  const handle = new THREE.Mesh(new THREE.TorusGeometry(0.08, 0.012, 5, 10, Math.PI), cm);
  handle.position.set(0, -0.02, -0.05); handle.rotation.y = Math.PI / 2; can.add(handle);
  tools.push(can);
  // 3 tohum kesesi
  const bag = new THREE.Group();
  bag.add(handGrp());
  sphere(bag, 0.1, tmat(T.hay(), '#c9a87a'), 0, -0.1, -0.12, 1, 1.1, 1, 1);
  cyl(bag, 0.03, 0.05, 0.05, 8, mat('#8a6a45'), 0, 0.0, -0.12);
  tools.push(bag);
  // 4 balta
  const axe = new THREE.Group();
  axe.add(handGrp());
  rod(axe, [0, -0.25, -0.05], [0, 0.42, -0.12], 0.02, mat('#8a6a45'));
  box(axe, 0.03, 0.12, 0.2, mat('#8d9399', { metalness: 0.75, roughness: 0.3 }), 0, 0.38, -0.2);
  tools.push(axe);
  // 5 olta
  const rodG = new THREE.Group();
  rodG.add(handGrp());
  rod(rodG, [0, -0.15, 0], [0, 1.4, -0.55], 0.012, mat('#3a3a3a'));
  cyl(rodG, 0.04, 0.04, 0.04, 10, mat('#b8bec4', { metalness: 0.8 }), 0.04, -0.02, -0.03, 0, 0, Math.PI / 2);
  tools.push(rodG);
  tools.forEach((t) => { t.visible = false; t.traverse((o) => { if (o.isMesh) { o.castShadow = false; o.frustumCulled = false; } }); });
  return tools;
}

export class Player {
  constructor(camera, scene) {
    this.camera = camera;
    this.scene = scene;
    this.pos = new THREE.Vector3(0, 2, -19);
    this.vy = 0;
    this.yaw = Math.PI; // +z yönüne (güneye) bak
    this.pitch = -0.05;
    this.onGround = true;
    this.swimming = false;
    this.bobPhase = 0;
    this.stepAcc = 0;
    this.vehicle = null;
    this.lookYaw = 0;
    this.speedNow = 0;
    this.radius = 0.35;
    this.indoors = false;

    this.viewmodel = new THREE.Group();
    camera.add(this.viewmodel);
    this.viewmodel.position.set(0.3, -0.36, -0.5);
    this.viewmodel.scale.setScalar(0.62);
    this.tools = buildTools();
    for (const t of this.tools) this.viewmodel.add(t);
    this.toolIdx = -1;
    this.swing = 0;
    this.swingKind = 0;
    // fener (gece)
    this.lantern = new THREE.PointLight(0xffd090, 0, 16, 1.6);
    this.lantern.position.set(0.2, -0.1, -0.2);
    camera.add(this.lantern);
    // balık şamandırası + misina
    this.bobber = new THREE.Group();
    sphere(this.bobber, 0.07, mat('#e03a2a'), 0, 0.04, 0, 1, 1, 1, 1);
    sphere(this.bobber, 0.06, mat('#ffffff'), 0, -0.02, 0, 1, 0.8, 1, 1);
    this.bobber.visible = false;
    scene.add(this.bobber);
    const lg = new THREE.BufferGeometry();
    lg.setAttribute('position', new THREE.Float32BufferAttribute([0, 0, 0, 0, 0, 0], 3));
    this.line = new THREE.Line(lg, new THREE.LineBasicMaterial({ color: 0xeeeeee, transparent: true, opacity: 0.7 }));
    this.line.frustumCulled = false;
    this.line.visible = false;
    scene.add(this.line);
  }
  setTool(i) {
    if (i === this.toolIdx) return;
    this.toolIdx = i;
    this.tools.forEach((t, k) => { t.visible = k === i; });
    this.swing = 0;
  }
  doSwing(kind = 0) { this.swing = 1; this.swingKind = kind; }

  forward(out = new THREE.Vector3()) {
    return out.set(0, 0, -1).applyEuler(this.camera.rotation);
  }
  waterLevelAt(x, z) {
    if (Math.abs(x - WATERFALL_X) < 8 && z < -162 && z > -220) return UPPER_WATER_Y;
    return 0;
  }
  groundAt(x, z, ctx, curY) {
    let g = heightAt(x, z);
    const wl = this.waterLevelAt(x, z);
    if (ctx.frozen && g < wl) g = wl;
    for (const p of ctx.platforms) {
      if (x >= p.minX && x <= p.maxX && z >= p.minZ && z <= p.maxZ && p.y > g && curY >= p.y - 0.7) g = p.y;
    }
    return g;
  }

  update(dt, input, ctx) {
    const look = input.consumeLook();
    const sens = 0.0024;
    if (this.vehicle) {
      this.lookYaw = clamp(this.lookYaw - look.x * sens, -2.4, 2.4);
      this.pitch = clamp(this.pitch - look.y * sens, -1.3, 1.2);
      return this.updateVehicle(dt, input, ctx);
    }
    this.yaw -= look.x * sens;
    this.pitch = clamp(this.pitch - look.y * sens, -1.45, 1.45);

    const ax = input.axis();
    const running = input.running() && ctx.energy > 5;
    let speed = running ? 7.2 : 4.2;
    if (ctx.season === 3 && !this.indoors && distToRoad(this.pos.x, this.pos.z) > 3 && !this.onPlatform) speed *= 0.82;
    if (this.swimming) speed = 2.2;
    const sy = Math.sin(this.yaw), cy = Math.cos(this.yaw);
    const fx = -sy, fz = -cy, rx = cy, rz = -sy;
    let mx = (fx * ax.y + rx * ax.x) * speed, mz = (fz * ax.y + rz * ax.x) * speed;
    const moving = Math.hypot(ax.x, ax.y) > 0.1;

    const old = { x: this.pos.x, z: this.pos.z };
    const nx = this.pos.x + mx * dt, nz = this.pos.z + mz * dt;
    // dik yamaç kontrolü
    const g0 = this.groundAt(this.pos.x, this.pos.z, ctx, this.pos.y);
    const g1 = this.groundAt(nx, nz, ctx, this.pos.y);
    const step = Math.hypot(nx - this.pos.x, nz - this.pos.z);
    if (!this.swimming && step > 0 && (g1 - g0) / step > 1.5 && g1 - this.pos.y > 0.45) {
      // tırmanılamaz
    } else { this.pos.x = nx; this.pos.z = nz; }
    const p = { x: this.pos.x, z: this.pos.z };
    ctx.colliders.resolve(p, this.radius);
    for (const c of ctx.dynamicCircles) {
      const dx = p.x - c.x, dz = p.z - c.z, d = Math.hypot(dx, dz), m = this.radius + c.r;
      if (d < m && d > 1e-4) { p.x = c.x + (dx / d) * m; p.z = c.z + (dz / d) * m; }
    }
    this.pos.x = clamp(p.x, -310, 310); this.pos.z = clamp(p.z, -310, 310);

    // dikey
    const ground = this.groundAt(this.pos.x, this.pos.z, ctx, this.pos.y);
    const wl = this.waterLevelAt(this.pos.x, this.pos.z);
    const depth = wl - ground;
    this.swimming = !ctx.frozen && depth > 1.35;
    this.onPlatform = ground > heightAt(this.pos.x, this.pos.z) + 0.05;
    if (this.swimming) {
      const target = wl - 1.25 + Math.sin(ctx.time * 2) * 0.05;
      this.pos.y += (target - this.pos.y) * Math.min(1, dt * 4);
      this.vy = 0; this.onGround = false;
    } else {
      this.vy -= 18 * dt;
      this.pos.y += this.vy * dt;
      if (this.pos.y <= ground) {
        if (this.vy < -12) ctx.onLand && ctx.onLand();
        this.pos.y = ground; this.vy = 0; this.onGround = true;
      } else if (this.pos.y - ground > 0.3) this.onGround = false;
      if (this.onGround && this.pos.y - ground < 0.5 && this.vy <= 0) this.pos.y = ground;
    }

    const actualSpeed = Math.hypot(this.pos.x - old.x, this.pos.z - old.z) / Math.max(dt, 1e-4);
    this.speedNow = actualSpeed;
    if (moving && this.onGround) {
      this.bobPhase += dt * actualSpeed * 1.9;
      this.stepAcc += actualSpeed * dt;
      if (this.stepAcc > (running ? 1.6 : 1.3)) { this.stepAcc = 0; ctx.onStep && ctx.onStep(); }
    }
    const bob = this.onGround && moving ? Math.sin(this.bobPhase) * 0.045 : 0;
    this.camera.position.set(this.pos.x, this.pos.y + EYE + bob, this.pos.z);
    this.camera.rotation.order = 'YXZ';
    this.camera.rotation.set(this.pitch, this.yaw, 0);
    this.animateTool(dt, moving, actualSpeed, ctx);
    return { moving, running: running && moving, speed: actualSpeed };
  }

  jump() {
    if (this.vehicle) return;
    if (this.onGround && !this.swimming) { this.vy = 5.6; this.onGround = false; }
  }

  updateVehicle(dt, input, ctx) {
    const v = this.vehicle;
    const ax = input.axis();
    const res = v.drive(dt, { x: ax.x, y: ax.y }, ctx.colliders, ctx);
    const seat = v.seatWorld(new THREE.Vector3());
    this.camera.position.copy(seat);
    this.camera.rotation.order = 'YXZ';
    this.camera.rotation.set(this.pitch + v.pitch * 0.5, v.heading + Math.PI + this.lookYaw, -v.roll * 0.5);
    this.pos.set(v.pos.x, v.pos.y, v.pos.z);
    this.yaw = v.heading + Math.PI + this.lookYaw;
    this.animateTool(dt, false, 0, ctx);
    return { moving: Math.abs(v.speed) > 0.2, vehicle: true, speed: v.speed, dist: res.dist };
  }
  enterVehicle(v) {
    this.vehicle = v;
    v.driven = true;
    this.lookYaw = 0;
    this.pitch = -0.08;
    this.viewmodel.visible = false;
  }
  exitVehicle(colliders) {
    const v = this.vehicle;
    if (!v) return;
    v.driven = false;
    v.speed = 0;
    const tryOffsets = [v.def.exitOffset, v.def.exitOffset.clone().setX(-v.def.exitOffset.x), new THREE.Vector3(0, 0, -3.5), new THREE.Vector3(0, 0, 3.5)];
    const s = Math.sin(v.heading), c = Math.cos(v.heading);
    let placed = false;
    for (const o of tryOffsets) {
      const x = v.pos.x + o.x * c + o.z * s, z = v.pos.z - o.x * s + o.z * c;
      if (!colliders.hits(x, z, 0.4) && (!v.def.water || heightAt(x, z) > -1.2)) {
        this.pos.set(x, heightAt(x, z), z); placed = true; break;
      }
    }
    if (!placed) {
      if (v.def.water) return false;
      this.pos.set(v.pos.x + 2.5 * c, v.pos.y, v.pos.z - 2.5 * s);
    }
    this.yaw = v.heading + Math.PI;
    this.vehicle = null;
    this.viewmodel.visible = true;
    return true;
  }

  animateTool(dt, moving, speed, ctx) {
    const vm = this.viewmodel;
    const t = ctx.time;
    const bobX = moving ? Math.sin(this.bobPhase * 0.5) * 0.018 : Math.sin(t * 1.2) * 0.004;
    const bobY = moving ? Math.abs(Math.sin(this.bobPhase * 0.5)) * 0.02 : Math.sin(t * 1.6) * 0.005;
    vm.position.set(0.3 + bobX, -0.36 + bobY, -0.5);
    this.swing = Math.max(0, this.swing - dt * 2.8);
    const s = this.swing;
    const k = s > 0 ? Math.sin((1 - s) * Math.PI) : 0;
    if (this.swingKind === 1) { // sulama: eğ
      vm.rotation.set(-0.2, 0.25, -k * 0.7);
      vm.position.x -= k * 0.08;
    } else if (this.swingKind === 2) { // olta at
      vm.rotation.set(-0.3 - Math.sin((1 - s) * Math.PI * 2) * 0.6, 0.2, 0);
    } else {
      vm.rotation.set(-0.55 - k * 1.0, 0.3 + k * 0.15, 0.2);
      vm.position.y += k * 0.12;
    }
    this.lantern.intensity = ctx.night && !this.indoors ? 5.5 : 0;
  }
}
