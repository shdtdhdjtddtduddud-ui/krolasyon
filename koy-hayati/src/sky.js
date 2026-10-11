// Gökyüzü, güneş/ay, ışık, bulutlar, hava durumu parçacıkları
import * as THREE from 'three';
import { smoothstep, clamp, lerp, mulberry32 } from './util.js';
import { softDot } from './terrain.js';

export const SEASONS = [
  { name: 'İlkbahar', icon: '🌸', sunrise: 6, sunset: 20, elev: 0.85,
    weather: [['clear', 40], ['cloudy', 25], ['rain', 28], ['storm', 7]] },
  { name: 'Yaz', icon: '☀️', sunrise: 5, sunset: 21.5, elev: 1.0,
    weather: [['clear', 50], ['hot', 30], ['cloudy', 12], ['storm', 8]] },
  { name: 'Sonbahar', icon: '🍂', sunrise: 6.5, sunset: 19, elev: 0.7,
    weather: [['clear', 30], ['cloudy', 30], ['rain', 28], ['storm', 12]] },
  { name: 'Kış', icon: '❄️', sunrise: 7.5, sunset: 17.5, elev: 0.5,
    weather: [['clear', 30], ['cloudy', 25], ['snow', 33], ['blizzard', 12]] },
];

export const WEATHER = {
  clear: { name: 'Açık', icon: '☀️', cloud: 0.15, fog: 1.0, dark: 0 },
  hot: { name: 'Sıcak', icon: '🔥', cloud: 0.0, fog: 0.9, dark: 0 },
  cloudy: { name: 'Bulutlu', icon: '☁️', cloud: 0.7, fog: 0.8, dark: 0.25 },
  rain: { name: 'Yağmurlu', icon: '🌧️', cloud: 1, fog: 0.5, dark: 0.45, rain: 1 },
  storm: { name: 'Fırtına', icon: '⛈️', cloud: 1, fog: 0.38, dark: 0.62, rain: 1.6, lightning: true },
  snow: { name: 'Karlı', icon: '🌨️', cloud: 0.9, fog: 0.42, dark: 0.35, snow: 1 },
  blizzard: { name: 'Tipi', icon: '🌬️', cloud: 1, fog: 0.18, dark: 0.55, snow: 2.4 },
};

const skyVert = `
  varying vec3 vDir;
  void main(){
    vDir = normalize(position);
    vec4 p = modelViewMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * p;
    gl_Position.z = gl_Position.w;
  }`;
const skyFrag = `
  #include <common>
  uniform vec3 uTop, uHorizon, uBottom, uSunDir, uSunColor, uMoonDir;
  uniform float uSunVis, uMoonVis, uCloud;
  varying vec3 vDir;
  void main(){
    vec3 d = normalize(vDir);
    float h = d.y;
    vec3 col = mix(uHorizon, uTop, pow(clamp(h, 0.0, 1.0), 0.55));
    col = mix(col, uBottom, smoothstep(0.0, -0.25, h));
    float sd = max(dot(d, uSunDir), 0.0);
    col += uSunColor * (pow(sd, 900.0) * 18.0 + pow(sd, 12.0) * 0.35) * uSunVis * (1.0 - uCloud*0.8);
    float md = max(dot(d, uMoonDir), 0.0);
    col += vec3(0.85,0.9,1.0) * (smoothstep(0.9993, 0.9996, md) * 1.6 + pow(md, 60.0)*0.12) * uMoonVis;
    gl_FragColor = vec4(col, 1.0);
    #include <tonemapping_fragment>
    #include <colorspace_fragment>
  }`;

const COL = {
  dayTop: new THREE.Color('#3f7fd1'), dayHor: new THREE.Color('#b9d7f0'),
  setTop: new THREE.Color('#33558f'), setHor: new THREE.Color('#f39a5e'),
  nightTop: new THREE.Color('#040916'), nightHor: new THREE.Color('#142036'),
  overTop: new THREE.Color('#7d8792'), overHor: new THREE.Color('#aab2ba'),
  stormTop: new THREE.Color('#3b424a'), stormHor: new THREE.Color('#5f666d'),
  sunDay: new THREE.Color('#fff4e0'), sunSet: new THREE.Color('#ffb070'),
  moon: new THREE.Color('#8fa8d8'),
};

export class Sky {
  constructor(scene, renderer, isMobile) {
    this.scene = scene;
    this.uniforms = {
      uTop: { value: new THREE.Color() }, uHorizon: { value: new THREE.Color() }, uBottom: { value: new THREE.Color('#5b6b5e') },
      uSunDir: { value: new THREE.Vector3(0, 1, 0) }, uSunColor: { value: new THREE.Color() },
      uMoonDir: { value: new THREE.Vector3(0, -1, 0) }, uSunVis: { value: 1 }, uMoonVis: { value: 0 }, uCloud: { value: 0 },
    };
    const dome = new THREE.Mesh(
      new THREE.SphereGeometry(900, 32, 16),
      new THREE.ShaderMaterial({ uniforms: this.uniforms, vertexShader: skyVert, fragmentShader: skyFrag, side: THREE.BackSide, depthWrite: false, fog: false }),
    );
    dome.renderOrder = -10;
    dome.frustumCulled = false;
    this.dome = dome;
    scene.add(dome);

    // yıldızlar
    const sg = new THREE.BufferGeometry();
    const sp = [];
    const r = mulberry32(4);
    for (let i = 0; i < 1400; i++) {
      const u = r() * 2 - 1, a = r() * Math.PI * 2;
      const y = Math.abs(u);
      const s = Math.sqrt(1 - y * y);
      sp.push(Math.cos(a) * s * 850, y * 850, Math.sin(a) * s * 850);
    }
    sg.setAttribute('position', new THREE.Float32BufferAttribute(sp, 3));
    this.stars = new THREE.Points(sg, new THREE.PointsMaterial({ size: 2.2, sizeAttenuation: false, color: 0xffffff, transparent: true, opacity: 0, fog: false, depthWrite: false }));
    this.stars.frustumCulled = false;
    scene.add(this.stars);

    // ışıklar
    this.sun = new THREE.DirectionalLight(0xffffff, 3);
    this.sun.castShadow = true;
    const sz = isMobile ? 1024 : 2048;
    this.sun.shadow.mapSize.set(sz, sz);
    const sc = this.sun.shadow.camera;
    sc.left = -60; sc.right = 60; sc.top = 60; sc.bottom = -60; sc.near = 1; sc.far = 400;
    this.sun.shadow.bias = -0.0006;
    this.sun.shadow.normalBias = 0.04;
    scene.add(this.sun, this.sun.target);
    this.hemi = new THREE.HemisphereLight(0xcfe3ff, 0x5a5236, 1.1);
    scene.add(this.hemi);
    // gölge tarafların simsiyah kalmaması için yumuşak dolgu ışığı
    this.fill = new THREE.DirectionalLight(0xbfd4ff, 0.5);
    scene.add(this.fill, this.fill.target);

    scene.fog = new THREE.Fog(0xbcd6ec, 80, 520);

    // bulutlar
    this.clouds = new THREE.Group();
    const cmat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 1, flatShading: true, transparent: true, opacity: 0.92, fog: false });
    this.cloudMat = cmat;
    const cr = mulberry32(11);
    for (let i = 0; i < 26; i++) {
      const c = new THREE.Group();
      const n = 4 + Math.floor(cr() * 5);
      for (let k = 0; k < n; k++) {
        const m = new THREE.Mesh(new THREE.IcosahedronGeometry(8 + cr() * 10, 1), cmat);
        m.position.set((k - n / 2) * 10 + cr() * 6, cr() * 6, cr() * 12 - 6);
        m.scale.y = 0.55;
        c.add(m);
      }
      c.position.set((cr() - 0.5) * 900, 140 + cr() * 60, (cr() - 0.5) * 900);
      c.userData.speed = 2 + cr() * 2;
      this.clouds.add(c);
    }
    scene.add(this.clouds);

    // yağmur
    const rainN = isMobile ? 1500 : 3000;
    const rg = new THREE.BufferGeometry();
    this.rainPos = new Float32Array(rainN * 6);
    this.rainSeed = new Float32Array(rainN * 3);
    for (let i = 0; i < rainN; i++) { this.rainSeed[i * 3] = r(); this.rainSeed[i * 3 + 1] = r(); this.rainSeed[i * 3 + 2] = r(); }
    rg.setAttribute('position', new THREE.BufferAttribute(this.rainPos, 3));
    this.rain = new THREE.LineSegments(rg, new THREE.LineBasicMaterial({ color: 0xaabbcc, transparent: true, opacity: 0.45 }));
    this.rain.frustumCulled = false;
    this.rainN = rainN;
    scene.add(this.rain);

    // kar
    const snowN = isMobile ? 1500 : 3000;
    const sgeo = new THREE.BufferGeometry();
    this.snowPos = new Float32Array(snowN * 3);
    this.snowSeed = new Float32Array(snowN * 3);
    for (let i = 0; i < snowN * 3; i++) this.snowSeed[i] = r();
    sgeo.setAttribute('position', new THREE.BufferAttribute(this.snowPos, 3));
    this.snow = new THREE.Points(sgeo, new THREE.PointsMaterial({ size: 0.16, map: softDot(), color: 0xffffff, transparent: true, opacity: 0.95, depthWrite: false }));
    this.snow.frustumCulled = false;
    this.snowN = snowN;
    scene.add(this.snow);

    this.flash = 0;
    this.nextFlash = 5;
    this.light = 1;
    this.skyTop = new THREE.Color(); this.skyHor = new THREE.Color();
    this.sunColor = new THREE.Color();
    this.sunDir = new THREE.Vector3();
    this.onThunder = null;
  }

  // hour: 0-24 ondalıklı
  update(dt, time, hour, season, weatherKey, center) {
    const S = SEASONS[season];
    const W = WEATHER[weatherKey] || WEATHER.clear;
    const dayLen = S.sunset - S.sunrise;
    const t = (hour - S.sunrise) / dayLen; // 0..1 gündüz
    const a = Math.PI * t;
    const sunDir = this.sunDir.set(Math.cos(a), Math.sin(a) * S.elev, 0.42).normalize();
    // gece ayı: güneşin tersi
    const nt = ((hour - S.sunset + 24) % 24) / (24 - dayLen);
    const ma = Math.PI * nt;
    const moonDir = new THREE.Vector3(Math.cos(ma), Math.sin(ma) * 0.7, -0.35).normalize();

    const sunY = sunDir.y;
    const day = smoothstep(-0.12, 0.22, sunY);
    const golden = smoothstep(0.45, 0.02, sunY) * smoothstep(-0.15, 0.03, sunY);
    const dark = W.dark;

    // gök renkleri
    const top = this.skyTop, hor = this.skyHor;
    top.copy(COL.nightTop).lerp(COL.dayTop, day);
    hor.copy(COL.nightHor).lerp(COL.dayHor, day);
    top.lerp(COL.setTop, golden * 0.6);
    hor.lerp(COL.setHor, golden * 0.85);
    if (season === 3) { top.lerp(new THREE.Color('#7aa2cf'), 0.25 * day); hor.lerp(new THREE.Color('#dfe8f0'), 0.3 * day); }
    const over = clamp(W.cloud - 0.2, 0, 1) * 0.85;
    const oTop = dark > 0.5 ? COL.stormTop : COL.overTop, oHor = dark > 0.5 ? COL.stormHor : COL.overHor;
    top.lerp(oTop.clone().multiplyScalar(0.15 + 0.85 * day), over);
    hor.lerp(oHor.clone().multiplyScalar(0.15 + 0.85 * day), over);

    // şimşek
    if (W.lightning) {
      this.nextFlash -= dt;
      if (this.nextFlash < 0) {
        this.flash = 1; this.nextFlash = 6 + Math.random() * 14;
        if (this.onThunder) setTimeout(() => this.onThunder(), 400 + Math.random() * 1500);
      }
    }
    this.flash = Math.max(0, this.flash - dt * 3.5);
    const fl = this.flash > 0.5 || (this.flash > 0.2 && this.flash < 0.3) ? this.flash : 0;
    if (fl) { top.lerp(new THREE.Color('#dfe6ff'), fl * 0.7); hor.lerp(new THREE.Color('#ffffff'), fl * 0.7); }

    this.uniforms.uTop.value.copy(top);
    this.uniforms.uHorizon.value.copy(hor);
    this.uniforms.uBottom.value.copy(hor).multiplyScalar(0.6);
    this.uniforms.uSunDir.value.copy(sunDir);
    this.sunColor.copy(COL.sunDay).lerp(COL.sunSet, golden);
    this.uniforms.uSunColor.value.copy(this.sunColor);
    this.uniforms.uMoonDir.value.copy(moonDir);
    this.uniforms.uSunVis.value = smoothstep(-0.05, 0.05, sunY);
    this.uniforms.uMoonVis.value = (1 - day) * (1 - over * 0.8);
    this.uniforms.uCloud.value = over;
    this.stars.material.opacity = (1 - smoothstep(0.0, 0.5, day)) * (1 - over);

    // ışıklar
    const sunUp = sunY > -0.02;
    const lightDir = sunUp ? sunDir : moonDir;
    this.sun.position.copy(center).addScaledVector(lightDir, 200);
    this.sun.target.position.copy(center);
    const shadowStep = 4;
    this.sun.target.position.x = Math.round(center.x / shadowStep) * shadowStep;
    this.sun.target.position.z = Math.round(center.z / shadowStep) * shadowStep;
    this.sun.position.x = this.sun.target.position.x + lightDir.x * 200;
    this.sun.position.z = this.sun.target.position.z + lightDir.z * 200;
    if (sunUp) {
      this.sun.color.copy(this.sunColor);
      this.sun.intensity = lerp(0.0, 3.2, smoothstep(-0.02, 0.25, sunY)) * (1 - dark * 0.75);
    } else {
      this.sun.color.copy(COL.moon);
      this.sun.intensity = 0.7 * smoothstep(-0.02, 0.2, moonDir.y) * (1 - over * 0.6);
    }
    this.sun.intensity += fl * 3;
    this.hemi.color.copy(top).lerp(new THREE.Color('#ffffff'), 0.4);
    this.hemi.groundColor.set(season === 3 ? '#8d929a' : '#5c5233');
    this.hemi.intensity = (0.4 + 0.8 * day) * (1 - dark * 0.35) + fl * 1.5;

    this.fill.position.copy(center).addScaledVector(new THREE.Vector3(-lightDir.x, 0.6, -lightDir.z).normalize(), 100);
    this.fill.target.position.copy(center);
    this.fill.intensity = (0.15 + 0.7 * day) * (1 - dark * 0.4);
    this.light = clamp(0.12 + day * 0.95 * (1 - dark * 0.45) + fl * 0.5, 0.08, 1.1);

    // sis
    const fogMul = W.fog;
    this.scene.fog.color.copy(hor);
    this.scene.fog.near = 60 * fogMul + 8;
    this.scene.fog.far = 560 * fogMul + 40;

    // bulutlar
    const cl = this.clouds;
    this.cloudMat.color.copy(new THREE.Color('#ffffff')).multiplyScalar(0.25 + 0.75 * day).lerp(hor, 0.35);
    if (dark > 0.4) this.cloudMat.color.multiplyScalar(0.55);
    this.cloudMat.opacity = 0.35 + W.cloud * 0.6;
    for (let i = 0; i < cl.children.length; i++) {
      const c = cl.children[i];
      c.position.x += c.userData.speed * dt * (1 + W.cloud);
      if (c.position.x > 500) c.position.x = -500;
      c.visible = i / cl.children.length < 0.25 + W.cloud * 0.75;
    }
    cl.position.set(center.x * 0.6, 0, center.z * 0.6);

    // yağmur
    const rainAmt = W.rain || 0;
    this.rain.visible = rainAmt > 0;
    if (rainAmt > 0) {
      const n = Math.floor(this.rainN * Math.min(1, rainAmt));
      const P = this.rainPos, sd = this.rainSeed;
      const fall = 26;
      for (let i = 0; i < this.rainN; i++) {
        if (i >= n) { P.fill(0, i * 6, i * 6 + 6); continue; }
        const x = center.x + (sd[i * 3] - 0.5) * 50;
        const z = center.z + (sd[i * 3 + 1] - 0.5) * 50;
        const y = center.y + 25 - ((time * fall + sd[i * 3 + 2] * 40) % 40);
        P[i * 6] = x; P[i * 6 + 1] = y; P[i * 6 + 2] = z;
        P[i * 6 + 3] = x + 0.08 * rainAmt; P[i * 6 + 4] = y - 0.7; P[i * 6 + 5] = z;
      }
      this.rain.geometry.attributes.position.needsUpdate = true;
      this.rain.material.opacity = 0.3 + 0.15 * day;
    }
    // kar
    const snowAmt = W.snow || 0;
    this.snow.visible = snowAmt > 0;
    if (snowAmt > 0) {
      const P = this.snowPos, sd = this.snowSeed;
      const n = Math.floor(this.snowN * Math.min(1, snowAmt));
      for (let i = 0; i < this.snowN; i++) {
        if (i >= n) { P[i * 3 + 1] = -999; continue; }
        const sp = 1.6 * (snowAmt > 1 ? 2.2 : 1);
        const y = center.y + 18 - ((time * sp + sd[i * 3 + 2] * 30) % 30);
        const wind = snowAmt > 1 ? time * 6 : 0;
        P[i * 3] = center.x + (((sd[i * 3] * 40 + Math.sin(time * 0.7 + i) * 0.8 + wind) % 40) - 20);
        P[i * 3 + 1] = y;
        P[i * 3 + 2] = center.z + (sd[i * 3 + 1] - 0.5) * 40 + Math.cos(time * 0.5 + i * 1.3) * 0.8;
      }
      this.snow.geometry.attributes.position.needsUpdate = true;
    }
    this.dome.position.copy(center);
    this.stars.position.copy(center);
    this.stars.rotation.y = time * 0.002;
    this.isNight = day < 0.3;
    return { day, sunY };
  }
}
