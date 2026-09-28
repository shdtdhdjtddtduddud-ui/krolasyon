// Paylaşılan malzemeler + rüzgar shader enjeksiyonu
import * as THREE from 'three';
import { tex } from './textures.js';

export const globalUniforms = { uTime: { value: 0 } };
const cache = {};

export function addWind(mat, strength = 1, bendPow = 1.0) {
  mat.onBeforeCompile = (sh) => {
    sh.uniforms.uTime = globalUniforms.uTime;
    sh.uniforms.uWind = { value: strength };
    sh.vertexShader = 'uniform float uTime; uniform float uWind;\n' + sh.vertexShader.replace('#include <begin_vertex>', `#include <begin_vertex>
      #ifdef USE_INSTANCING
        vec3 ip = vec3(instanceMatrix[3][0], instanceMatrix[3][1], instanceMatrix[3][2]);
      #else
        vec3 ip = vec3(modelMatrix[3][0], modelMatrix[3][1], modelMatrix[3][2]);
      #endif
      float wph = uTime * 1.6 + ip.x * 0.13 + ip.z * 0.11;
      float amt = pow(max(0.0, position.y), ${bendPow.toFixed(2)}) * uWind;
      transformed.x += (sin(wph) * 0.05 + sin(wph * 2.7 + position.y) * 0.02) * amt;
      transformed.z += (cos(wph * 0.8) * 0.04) * amt;`);
  };
  mat.customProgramCacheKey = () => 'wind' + strength + '_' + bendPow;
  return mat;
}

function std(o) { return new THREE.MeshStandardMaterial(o); }
const DEFS = {
  bark: () => std({ map: tex('bark'), normalMap: tex('barkNormal'), roughness: 0.95, color: 0xffffff }),
  darkBark: () => std({ map: tex('bark'), normalMap: tex('barkNormal'), roughness: 0.95, color: 0x6a5a55 }),
  charBark: () => std({ map: tex('bark'), roughness: 1, color: 0x2a2320 }),
  leaves: () => addWind(std({ map: tex('leaves'), roughness: 0.85, color: 0xb8d890, flatShading: true }), 1.0, 1.3),
  darkLeaves: () => addWind(std({ map: tex('leaves'), roughness: 0.9, color: 0x5b7a55, flatShading: true }), 0.8, 1.3),
  pineLeaves: () => addWind(std({ map: tex('leaves'), roughness: 0.9, color: 0x6f9a70, flatShading: true }), 0.6, 1.2),
  autumnLeaves: () => addWind(std({ map: tex('leaves'), roughness: 0.85, color: 0xffb070, flatShading: true }), 1.0, 1.3),
  snowCap: () => std({ color: 0xf2f6ff, roughness: 0.7, flatShading: true }),
  palmLeaves: () => addWind(std({ map: tex('leaves'), roughness: 0.8, color: 0xa8d070, side: THREE.DoubleSide }), 1.4, 1.0),
  cactus: () => std({ map: tex('fur'), color: 0x5f9a4a, roughness: 0.8 }),
  rock: () => std({ map: tex('rock'), normalMap: tex('rockNormal'), roughness: 0.95, flatShading: true }),
  darkRock: () => std({ map: tex('rock'), normalMap: tex('rockNormal'), roughness: 0.8, color: 0x4a4040, flatShading: true }),
  sandRock: () => std({ map: tex('sandstone'), normalMap: tex('rockNormal'), roughness: 0.95, flatShading: true }),
  snowRock: () => std({ map: tex('rock'), normalMap: tex('rockNormal'), roughness: 0.9, color: 0xc8d4e8, flatShading: true }),
  obsidian: () => std({ map: tex('lavaRock'), emissiveMap: tex('lavaEmissive'), emissive: 0xff5a10, emissiveIntensity: 2.2, roughness: 0.5, metalness: 0.2, flatShading: true }),
  stone: () => std({ map: tex('stone'), normalMap: tex('rockNormal'), roughness: 0.9 }),
  mossStone: () => std({ map: tex('stone'), normalMap: tex('rockNormal'), roughness: 0.9, color: 0x9aae88 }),
  sandstone: () => std({ map: tex('sandstone'), roughness: 0.9 }),
  plaster: () => std({ map: tex('plaster'), roughness: 0.95 }),
  wood: () => std({ map: tex('planks'), roughness: 0.85 }),
  darkWood: () => std({ map: tex('planks'), roughness: 0.85, color: 0x6a4a35 }),
  roof: () => std({ map: tex('roof'), normalMap: tex('roofNormal'), roughness: 0.8 }),
  roofBlue: () => std({ map: tex('roof'), normalMap: tex('roofNormal'), roughness: 0.8, color: 0x7aa0d0 }),
  thatch: () => std({ map: tex('fur'), normalMap: tex('furNormal'), roughness: 1, color: 0xd8b060 }),
  iron: () => std({ map: tex('metal'), color: 0x9098a0, roughness: 0.35, metalness: 0.85 }),
  darkIron: () => std({ map: tex('metal'), color: 0x3a3c42, roughness: 0.4, metalness: 0.8 }),
  gold: () => std({ map: tex('metal'), color: 0xffc85a, roughness: 0.3, metalness: 1 }),
  windowGlow: () => std({ color: 0x000000, emissive: 0xffb050, emissiveIntensity: 2.2 }),
  lantern: () => std({ color: 0xffe0a0, emissive: 0xffa040, emissiveIntensity: 4 }),
  mushroom: () => std({ color: 0x40e0ff, emissive: 0x20a0ff, emissiveIntensity: 2.2, roughness: 0.5 }),
  mushroomPink: () => std({ color: 0xff70d0, emissive: 0xc02090, emissiveIntensity: 2.2, roughness: 0.5 }),
  stem: () => std({ color: 0xe0d8c0, roughness: 0.8 }),
  ice: () => std({ map: tex('ice'), color: 0xbfe6ff, roughness: 0.15, metalness: 0.1, emissive: 0x3aa0ff, emissiveIntensity: 0.9, transparent: true, opacity: 0.88, flatShading: true }),
  fireCrystal: () => std({ color: 0xff6030, emissive: 0xff3000, emissiveIntensity: 3, roughness: 0.2, flatShading: true }),
  rune: () => std({ color: 0x80e0ff, emissive: 0x40c0ff, emissiveIntensity: 4, roughness: 0.3, flatShading: true }),
  bone: () => std({ map: tex('bone'), roughness: 0.7 }),
  cloth: () => std({ map: tex('cloth'), color: 0xb03030, roughness: 1, side: THREE.DoubleSide }),
  stripes: () => std({ map: tex('stripes'), roughness: 1, side: THREE.DoubleSide }),
  hay: () => std({ map: tex('fur'), color: 0xe0c070, roughness: 1 }),
  coal: () => std({ color: 0x220a04, emissive: 0xff4010, emissiveIntensity: 3, roughness: 0.9 }),
  barrier: () => new THREE.MeshBasicMaterial({ color: 0xff5020, transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide }),
  water: () => std({ color: 0x3080c0, roughness: 0.1, metalness: 0.2, transparent: true, opacity: 0.8 }),
};
export function M(name) {
  if (cache[name]) return cache[name];
  const d = DEFS[name]; if (!d) throw new Error('malzeme yok: ' + name);
  return (cache[name] = d());
}
