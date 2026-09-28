// Prosedürel animasyon: yürüyüş döngüleri + anahtar kareli saldırı/yetenek pozları + yumuşak geçişler
import { clamp, damp } from './util.js';

const TGT = new Map();
function setT(o, x, y, z) { if (!o) return; let t = TGT.get(o); if (!t) { t = [0, 0, 0]; TGT.set(o, t); } t[0] = x; t[1] = y; t[2] = z; }
function addT(o, x, y, z) { if (!o) return; const t = TGT.get(o); if (!t) return setT(o, x, y, z); t[0] += x; t[1] += y; t[2] += z; }
function apply(list, k, dt) {
  for (const o of list) { if (!o) continue; const t = TGT.get(o); if (!t) continue; const r = o.rotation; r.x = damp(r.x, t[0], k, dt); r.y = damp(r.y, t[1], k, dt); r.z = damp(r.z, t[2], k, dt); }
}
const ease = (t) => t * t * (3 - 2 * t);
// anahtar kareleri örnekle: keys = [[t, {pivot:[x,y,z]}], ...]
function sample(keys, t, p, w = 1) {
  let a = keys[0], b = keys[keys.length - 1];
  for (let i = 0; i < keys.length - 1; i++) if (t >= keys[i][0] && t <= keys[i + 1][0]) { a = keys[i]; b = keys[i + 1]; break; }
  const u = b[0] > a[0] ? ease(clamp((t - a[0]) / (b[0] - a[0]), 0, 1)) : 0;
  const names = new Set([...Object.keys(a[1]), ...Object.keys(b[1])]);
  for (const n of names) {
    const o = p[n]; if (!o) continue; const va = a[1][n] || b[1][n], vb = b[1][n] || a[1][n];
    const x = va[0] + (vb[0] - va[0]) * u, y = va[1] + (vb[1] - va[1]) * u, z = va[2] + (vb[2] - va[2]) * u;
    const cur = TGT.get(o) || [0, 0, 0];
    setT(o, cur[0] + (x - cur[0]) * w, cur[1] + (y - cur[1]) * w, cur[2] + (z - cur[2]) * w);
  }
}

// ---------------- İNSANSI ----------------
export const HACTS = {
  slash1: { keys: [[0, { chest: [0.05, -0.7, 0], rShoulder: [0, -0.8, -1.35], rElbow: [-0.7, 0, 0], rHand: [0.3, 0, 0], spine: [0.1, 0, 0] }], [0.32, { chest: [0.05, -0.8, 0], rShoulder: [0, -1.0, -1.45], rElbow: [-0.5, 0, 0] }], [0.55, { chest: [0.15, 0.75, 0], rShoulder: [0, 1.7, -1.45], rElbow: [-0.1, 0, 0], rHand: [0.2, 0, 0], spine: [0.25, 0, 0] }], [1, { chest: [0.05, 0.3, 0], rShoulder: [-0.4, 0.8, -0.8], rElbow: [-0.5, 0, 0] }]] },
  slash2: { keys: [[0, { chest: [0.05, 0.7, 0], rShoulder: [0, 1.5, -1.4], rElbow: [-0.5, 0, 0], rHand: [0.3, 0, 0], spine: [0.1, 0, 0] }], [0.32, { chest: [0.05, 0.8, 0], rShoulder: [0, 1.7, -1.45] }], [0.55, { chest: [0.15, -0.8, 0], rShoulder: [0, -1.0, -1.45], rElbow: [-0.1, 0, 0], spine: [0.25, 0, 0] }], [1, { chest: [0.05, -0.3, 0], rShoulder: [-0.3, -0.4, -0.9], rElbow: [-0.5, 0, 0] }]] },
  overhead: { full: true, keys: [[0, { spine: [-0.2, 0, 0], rShoulder: [-2.9, 0, -0.2], rElbow: [-0.6, 0, 0], rHand: [-0.4, 0, 0], lShoulder: [-2.6, 0, 0.2], lElbow: [-0.6, 0, 0], lHip: [-0.3, 0, 0], rHip: [0.3, 0, 0], lKnee: [0.3, 0, 0], rKnee: [0.2, 0, 0] }], [0.4, { spine: [-0.35, 0, 0], rShoulder: [-3.1, 0, -0.2], rElbow: [-0.7, 0, 0] }], [0.58, { spine: [0.55, 0, 0], rShoulder: [-1.0, 0, -0.1], rElbow: [-0.1, 0, 0], rHand: [1.1, 0, 0], lShoulder: [-0.9, 0, 0.3], lElbow: [-0.2, 0, 0], lHip: [-0.8, 0, 0], lKnee: [0.9, 0, 0], rHip: [0.4, 0, 0], rKnee: [0.3, 0, 0] }], [1, { spine: [0.3, 0, 0], rShoulder: [-0.8, 0, -0.2], rHand: [0.8, 0, 0], lShoulder: [-0.4, 0, 0.2], lHip: [-0.5, 0, 0], lKnee: [0.6, 0, 0] }]] },
  thrust: { keys: [[0, { chest: [0, -0.5, 0], rShoulder: [0.5, 0, -0.2], rElbow: [-1.8, 0, 0], rHand: [1.2, 0, 0] }], [0.35, { chest: [0, -0.6, 0], rShoulder: [0.6, 0, -0.2], rElbow: [-2.0, 0, 0] }], [0.55, { chest: [0.2, 0.5, 0], spine: [0.25, 0, 0], rShoulder: [-1.55, 0, 0], rElbow: [0, 0, 0], rHand: [1.5, 0, 0] }], [1, { chest: [0, 0.2, 0], rShoulder: [-0.8, 0, -0.1], rElbow: [-0.6, 0, 0], rHand: [0.6, 0, 0] }]] },
  spin: { keys: [[0, { rShoulder: [0, 0.5, -1.5], rElbow: [0, 0, 0], rHand: [0.1, 0, 0], lShoulder: [0, 0, 1.3], spine: [0.2, 0, 0], chest: [0, 0.3, 0] }], [1, { rShoulder: [0, 0.5, -1.5], rElbow: [0, 0, 0], lShoulder: [0, 0, 1.3], spine: [0.2, 0, 0], chest: [0, 0.3, 0] }]] },
  slam: { full: true, keys: [[0, { spine: [-0.3, 0, 0], rShoulder: [-3.0, 0, -0.3], lShoulder: [-3.0, 0, 0.3], rElbow: [-0.4, 0, 0], lElbow: [-0.4, 0, 0], lHip: [-0.9, 0, 0], rHip: [-0.9, 0, 0], lKnee: [1.4, 0, 0], rKnee: [1.4, 0, 0] }], [0.45, { spine: [-0.4, 0, 0], rShoulder: [-3.2, 0, -0.3], lShoulder: [-3.2, 0, 0.3] }], [0.6, { spine: [0.8, 0, 0], rShoulder: [-0.9, 0, -0.1], lShoulder: [-0.9, 0, 0.1], rElbow: [0, 0, 0], lElbow: [0, 0, 0], rHand: [1, 0, 0], lHip: [-1.1, 0, 0], rHip: [-0.3, 0, 0], lKnee: [1.6, 0, 0], rKnee: [1.2, 0, 0] }], [1, { spine: [0.4, 0, 0], rShoulder: [-0.6, 0, 0], lShoulder: [-0.6, 0, 0], lHip: [-0.4, 0, 0], lKnee: [0.6, 0, 0], rKnee: [0.5, 0, 0] }]] },
  cast: { keys: [[0, { chest: [-0.1, 0, 0], rShoulder: [-0.4, 0, -0.4], rElbow: [-1.5, 0, 0], lShoulder: [-0.4, 0, 0.4], lElbow: [-1.5, 0, 0] }], [0.35, { chest: [-0.15, 0, 0], rShoulder: [-0.6, 0, -0.5], lShoulder: [-0.6, 0, 0.5] }], [0.5, { chest: [0.2, 0, 0], spine: [0.15, 0, 0], rShoulder: [-1.5, 0, 0.1], rElbow: [-0.1, 0, 0], lShoulder: [-1.5, 0, -0.1], lElbow: [-0.1, 0, 0] }], [1, { chest: [0.05, 0, 0], rShoulder: [-0.9, 0, 0], lShoulder: [-0.8, 0, 0], rElbow: [-0.4, 0, 0], lElbow: [-0.4, 0, 0] }]] },
  staffBolt: { keys: [[0, { chest: [0, -0.4, 0], rShoulder: [-0.6, 0, -0.3], rElbow: [-1.2, 0, 0], rHand: [0.6, 0, 0], lShoulder: [-0.4, 0, 0.3] }], [0.4, { chest: [0.1, 0.35, 0], rShoulder: [-1.6, 0, 0], rElbow: [-0.1, 0, 0], rHand: [1.2, 0, 0], lShoulder: [-1.2, 0, 0.3], lElbow: [-0.2, 0, 0] }], [1, { chest: [0, 0.1, 0], rShoulder: [-0.7, 0, -0.1], rElbow: [-0.6, 0, 0], rHand: [0.5, 0, 0], lShoulder: [-0.2, 0, 0.1] }]] },
  castUp: { full: true, keys: [[0, { spine: [-0.1, 0, 0], rShoulder: [-1.5, 0, -0.2], rElbow: [-0.5, 0, 0], lShoulder: [-0.4, 0, 0.8] }], [0.4, { spine: [-0.3, 0, 0], head: [-0.4, 0, 0], rShoulder: [-3.0, 0, -0.1], rElbow: [0, 0, 0], rHand: [0, 0, 0], lShoulder: [-0.6, 0, 1.4], lElbow: [-0.2, 0, 0] }], [0.7, { spine: [0.3, 0, 0], rShoulder: [-1.4, 0, 0], rHand: [0.8, 0, 0], lShoulder: [-1.3, 0, 0.2], head: [0.2, 0, 0], lHip: [-0.5, 0, 0], lKnee: [0.5, 0, 0] }], [1, { spine: [0.1, 0, 0], rShoulder: [-0.8, 0, 0], lShoulder: [-0.4, 0, 0.2] }]] },
  bow: { keys: [[0, { chest: [0, 0.9, 0], head: [0, -0.8, 0], lShoulder: [-1.5, -0.9, 0], lElbow: [0, 0, 0], lHand: [0, 0, 0], rShoulder: [-1.5, 0.0, 0], rElbow: [-0.6, 0, 0] }], [0.45, { rShoulder: [-1.4, -0.7, 0], rElbow: [-2.3, 0, 0], lShoulder: [-1.55, -0.9, 0] }], [0.6, { rShoulder: [-1.1, -0.9, -0.6], rElbow: [-1.2, 0, 0] }], [1, { chest: [0, 0.6, 0], head: [0, -0.5, 0], lShoulder: [-1.2, -0.7, 0], rShoulder: [-0.6, 0, -0.3], rElbow: [-0.8, 0, 0] }]] },
  roar: { full: true, keys: [[0, { chest: [0.3, 0, 0], head: [0.3, 0, 0], rShoulder: [-0.3, 0, -0.3], lShoulder: [-0.3, 0, 0.3] }], [0.3, { chest: [-0.35, 0, 0], head: [-0.5, 0, 0], rShoulder: [-0.6, 0, -1.3], lShoulder: [-0.6, 0, 1.3], rElbow: [-0.8, 0, 0], lElbow: [-0.8, 0, 0], lHip: [-0.2, 0, 0.2], rHip: [-0.2, 0, -0.2], lKnee: [0.4, 0, 0], rKnee: [0.4, 0, 0] }], [0.85, { chest: [-0.4, 0, 0], head: [-0.6, 0, 0], rShoulder: [-0.7, 0, -1.4], lShoulder: [-0.7, 0, 1.4] }], [1, {}]] },
  punch: { keys: [[0, { chest: [0, -0.6, 0], rShoulder: [0.4, 0, -0.3], rElbow: [-2.0, 0, 0], lShoulder: [-0.8, 0, 0.3], lElbow: [-1.4, 0, 0] }], [0.35, { chest: [0, -0.7, 0], rShoulder: [0.5, 0, -0.3] }], [0.55, { chest: [0.2, 0.6, 0], spine: [0.25, 0, 0], rShoulder: [-1.6, 0, 0.1], rElbow: [-0.05, 0, 0] }], [1, { chest: [0, 0.2, 0], rShoulder: [-0.6, 0, -0.1], rElbow: [-0.9, 0, 0] }]] },
  punch2: { keys: [[0, { chest: [0, 0.6, 0], lShoulder: [0.4, 0, 0.3], lElbow: [-2.0, 0, 0], rShoulder: [-0.8, 0, -0.3], rElbow: [-1.4, 0, 0] }], [0.35, { chest: [0, 0.7, 0], lShoulder: [0.5, 0, 0.3] }], [0.55, { chest: [0.2, -0.6, 0], spine: [0.25, 0, 0], lShoulder: [-1.6, 0, -0.1], lElbow: [-0.05, 0, 0] }], [1, { chest: [0, -0.2, 0], lShoulder: [-0.6, 0, 0.1], lElbow: [-0.9, 0, 0] }]] },
  stomp: { full: true, keys: [[0, {}], [0.4, { lHip: [-1.4, 0, 0.2], lKnee: [1.6, 0, 0], spine: [-0.25, 0, 0], rShoulder: [-0.4, 0, -1.0], lShoulder: [-0.4, 0, 1.0] }], [0.55, { lHip: [-0.3, 0, 0.1], lKnee: [0.2, 0, 0], spine: [0.35, 0, 0], rKnee: [0.6, 0, 0], rHip: [-0.4, 0, 0] }], [1, { spine: [0.1, 0, 0] }]] },
  drink: { keys: [[0, { rShoulder: [-1.2, 0, 0], rElbow: [-1.8, 0, 0] }], [0.3, { rShoulder: [-1.6, 0, 0.3], rElbow: [-2.5, 0, 0], head: [-0.5, 0, 0] }], [0.8, { rShoulder: [-1.6, 0, 0.3], rElbow: [-2.5, 0, 0], head: [-0.5, 0, 0] }], [1, {}]] },
  wave: { keys: [[0, { rShoulder: [0, 0, -2.6], rElbow: [0, 0, 0.6] }], [0.25, { rShoulder: [0, 0, -2.7], rElbow: [0, 0, 1.0] }], [0.5, { rShoulder: [0, 0, -2.6], rElbow: [0, 0, 0.3] }], [0.75, { rShoulder: [0, 0, -2.7], rElbow: [0, 0, 1.0] }], [1, { rShoulder: [0, 0, -2.6], rElbow: [0, 0, 0.5] }]] },
  talk: { keys: [[0, { rShoulder: [-0.6, 0, -0.3], rElbow: [-1.2, 0, 0], lShoulder: [-0.2, 0, 0.1], head: [0.05, 0.1, 0] }], [0.4, { rShoulder: [-0.8, 0, -0.5], rElbow: [-1.0, 0.4, 0], head: [-0.05, -0.1, 0] }], [0.7, { lShoulder: [-0.7, 0, 0.4], lElbow: [-1.1, 0, 0], rShoulder: [-0.3, 0, -0.2] }], [1, { rShoulder: [-0.6, 0, -0.3], rElbow: [-1.2, 0, 0] }]] },
  hammer: { keys: [[0, { rShoulder: [-0.6, 0, -0.2], rElbow: [-0.6, 0, 0], spine: [0.3, 0, 0] }], [0.5, { rShoulder: [-2.6, 0, -0.2], rElbow: [-1.2, 0, 0], spine: [0.1, 0, 0] }], [0.7, { rShoulder: [-0.7, 0, -0.2], rElbow: [-0.3, 0, 0], spine: [0.4, 0, 0] }], [1, { rShoulder: [-0.6, 0, -0.2], rElbow: [-0.6, 0, 0], spine: [0.3, 0, 0] }]] },
  block: { keys: [[0, { lShoulder: [-1.3, 0.4, 0.3], lElbow: [-1.4, 0, 0], chest: [0, 0.3, 0] }], [1, { lShoulder: [-1.3, 0.4, 0.3], lElbow: [-1.4, 0, 0], chest: [0, 0.3, 0] }]] },
  sweep: { keys: [[0, { chest: [0, -0.9, 0], rShoulder: [0, -1.0, -1.2], lShoulder: [0, -0.3, 1.0], spine: [0.2, 0, 0] }], [0.4, { chest: [0, -1.0, 0] }], [0.6, { chest: [0.1, 1.0, 0], rShoulder: [0, 1.6, -1.3], lShoulder: [0, 1.6, 1.3] }], [1, { chest: [0, 0.5, 0] }]] },
};

export function animHumanoid(m, st, dt) {
  const p = m.p, sc = m.scale || 1;
  const spd = st.speed || 0; const amp = clamp(spd / (4.2 * sc), 0, 1.35);
  st.phase = (st.phase || 0) + dt * (spd / (0.95 * sc)) * 1.55;
  const ph = st.phase, t = st.time || 0;
  const heavy = st.heavy || 0;
  // temel poz: bekleme
  const br = Math.sin(t * 2) * 0.03;
  setT(p.body, 0, 0, 0); setT(p.hips, 0, 0, 0); setT(p.spine, 0.02 + br, 0, 0); setT(p.chest, br, 0, 0); setT(p.neck, 0, 0, 0); setT(p.head, -br + (st.lookX || 0), st.lookY || 0, 0);
  setT(p.lShoulder, 0.05 + br, 0, 0.1 + heavy * 0.2); setT(p.rShoulder, 0.05 + br, 0, -0.1 - heavy * 0.2); setT(p.lElbow, -0.2, 0, 0); setT(p.rElbow, -0.25, 0, 0);
  setT(p.lHand, 0, 0, 0); setT(p.rHand, 0, 0, 0);
  setT(p.lHip, 0, 0, 0.03); setT(p.rHip, 0, 0, -0.03); setT(p.lKnee, 0.02, 0, 0); setT(p.rKnee, 0.02, 0, 0); setT(p.lFoot, 0, 0, 0); setT(p.rFoot, 0, 0, 0);
  if (st.holdPose === 'sword') { setT(p.rShoulder, -0.25 + br, 0, -0.15); setT(p.rElbow, -0.7, 0, 0); setT(p.rHand, 0.2, 0, 0); }
  if (st.holdPose === 'staff') { setT(p.rShoulder, -0.3 + br, 0, -0.1); setT(p.rElbow, -0.9, 0, 0); setT(p.rHand, 1.3, 0, 0); }
  if (st.holdPose === 'bow') { setT(p.lShoulder, -0.25, 0, 0.12); setT(p.lElbow, -0.6, 0, 0); setT(p.lHand, 0, 0, 0); }
  if (st.holdPose === 'shield') { setT(p.lShoulder, -0.5, 0.2, 0.15); setT(p.lElbow, -1.3, 0, 0); }
  let bodyY = 0.9;
  if (st.swim) {
    setT(p.body, 1.25, 0, 0); setT(p.head, -1.0, 0, 0);
    setT(p.lShoulder, -1.6 + Math.sin(ph) * 1.4, 0, 0.3); setT(p.rShoulder, -1.6 - Math.sin(ph) * 1.4, 0, -0.3); setT(p.lElbow, -0.3, 0, 0); setT(p.rElbow, -0.3, 0, 0);
    setT(p.lHip, Math.sin(ph * 2) * 0.35, 0, 0.05); setT(p.rHip, -Math.sin(ph * 2) * 0.35, 0, -0.05); setT(p.lKnee, 0.3, 0, 0); setT(p.rKnee, 0.3, 0, 0);
    bodyY = 0.3 + Math.sin(t * 2) * 0.05;
    st.phase += dt * 2;
  } else if (st.air) {
    const up = (st.vy || 0) > 0;
    setT(p.lHip, -0.9, 0, 0.1); setT(p.lKnee, 1.3, 0, 0); setT(p.rHip, up ? 0.1 : -0.3, 0, -0.1); setT(p.rKnee, up ? 0.5 : 0.8, 0, 0);
    setT(p.lShoulder, -0.5, 0, 0.6); setT(p.rShoulder, 0.2, 0, -0.6); setT(p.spine, 0.15, 0, 0);
  } else if (amp > 0.02) {
    const A = 0.62 * amp;
    const s1 = Math.sin(ph), c1 = Math.cos(ph);
    setT(p.lHip, s1 * A - 0.05 * amp, 0, 0.03); setT(p.rHip, -s1 * A - 0.05 * amp, 0, -0.03);
    setT(p.lKnee, (0.12 + 1.15 * Math.max(0, -c1)) * amp, 0, 0); setT(p.rKnee, (0.12 + 1.15 * Math.max(0, c1)) * amp, 0, 0);
    setT(p.lFoot, Math.max(0, s1) * 0.3 * amp, 0, 0); setT(p.rFoot, Math.max(0, -s1) * 0.3 * amp, 0, 0);
    addT(p.lShoulder, -s1 * A * 0.8, 0, 0.05 * amp); addT(p.rShoulder, s1 * A * 0.8, 0, -0.05 * amp);
    addT(p.lElbow, -0.35 * amp, 0, 0); addT(p.rElbow, -0.35 * amp, 0, 0);
    addT(p.spine, 0.08 * amp + (amp > 1 ? 0.12 : 0), 0, 0); setT(p.hips, 0, s1 * 0.12 * amp, 0); addT(p.chest, 0, -s1 * 0.16 * amp, 0);
    bodyY = 0.9 - 0.035 * amp + Math.abs(c1) * 0.06 * amp;
  }
  // aksiyon
  const a = st.act;
  if (a && HACTS[a.name]) {
    const def = HACTS[a.name]; const w = clamp(Math.min(a.t * 8, (1 - a.t) * 6 + 0.25), 0, 1);
    sample(def.keys, clamp(a.t, 0, 1), p, a.name === 'spin' || a.name === 'block' ? 1 : w);
    if (def.full && a.t > 0.5 && a.t < 0.8) bodyY -= 0.12 * sc;
  }
  if (st.roll != null) {
    const r = st.roll; setT(p.lHip, -1.6, 0, 0); setT(p.rHip, -1.6, 0, 0); setT(p.lKnee, 2.0, 0, 0); setT(p.rKnee, 2.0, 0, 0); setT(p.spine, 0.7, 0, 0); setT(p.head, 0.6, 0, 0);
    setT(p.lShoulder, -1.0, 0, 0.2); setT(p.rShoulder, -1.0, 0, -0.2); setT(p.lElbow, -1.4, 0, 0); setT(p.rElbow, -1.4, 0, 0);
    p.body.rotation.x = (st.rollDir || 1) * r * Math.PI * 2; bodyY = 0.55 + Math.sin(r * Math.PI) * 0.2;
  }
  // yaralanma sarsıntısı
  if (st.hurt > 0) { const h = st.hurt; addT(p.spine, -0.35 * h, 0, 0); addT(p.head, -0.3 * h, 0, 0.1 * h); addT(p.lShoulder, 0.3 * h, 0, 0.3 * h); addT(p.rShoulder, 0.3 * h, 0, -0.3 * h); }
  if (st.dead >= 0 && st.dead != null) {
    const d = clamp(st.dead / 0.6, 0, 1);
    setT(p.body, -1.45 * ease(d), 0, 0.1); setT(p.lShoulder, -0.3, 0, 1.2); setT(p.rShoulder, -0.3, 0, -1.2); setT(p.lKnee, 0.3, 0, 0); setT(p.rKnee, 0.6, 0, 0); setT(p.head, 0.2, 0.4, 0);
    bodyY = 0.9 - 0.62 * ease(d);
  }
  if (p.cape) { const sw = clamp(spd * 0.09, 0, 0.9) + (st.air ? 0.5 : 0); p.cape.forEach((c, i) => setT(c, (i === 0 ? 0.08 : 0.12) + sw * (i === 0 ? 1 : 0.3) + Math.sin(t * 5 + i) * 0.04 * (0.4 + amp), 0, 0)); }
  if (p.wings) p.wings.forEach((w) => setT(w, 0.2, w.userData.side * (0.4 + Math.sin(t * 16) * 0.7), 0));
  if (p.tail) p.tail.forEach((tt, i) => setT(tt, 0.3 + (i ? 0.1 : 0), Math.sin(t * 3 - i) * 0.3, 0));
  const k = st.roll != null ? 30 : a ? 24 : 12;
  p.body.position.y = damp(p.body.position.y, bodyY, st.roll != null ? 40 : 14, dt);
  apply([p.body, p.hips, p.spine, p.chest, p.neck, p.head, p.lShoulder, p.rShoulder, p.lElbow, p.rElbow, p.lHand, p.rHand, p.lHip, p.rHip, p.lKnee, p.rKnee, p.lFoot, p.rFoot], k, dt);
  if (st.roll != null) p.body.rotation.x = (st.rollDir || 1) * st.roll * Math.PI * 2;
  if (p.cape) apply(p.cape, 8, dt);
  if (p.wings) apply(p.wings, 30, dt);
  if (p.tail) apply(p.tail, 6, dt);
  if (st.spin != null) p.body.rotation.y = st.spin; 
}

// ---------------- DÖRT AYAKLI ----------------
const QACTS = {
  bite: [[0, { body: [-0.1, 0, 0], neck: [-0.3, 0, 0], head: [-0.2, 0, 0], jaw: [0, 0, 0] }], [0.35, { body: [0.1, 0, 0], neck: [-0.5, 0, 0], head: [-0.4, 0, 0], jaw: [0.7, 0, 0], fl: [0.5, 0, 0], fr: [0.5, 0, 0] }], [0.55, { body: [0.25, 0, 0], neck: [0.3, 0, 0], head: [0.3, 0, 0], jaw: [0, 0, 0], fl: [-0.6, 0, 0], fr: [-0.6, 0, 0], bl: [0.4, 0, 0], br: [0.4, 0, 0] }], [1, {}]],
  pounce: [[0, { body: [0.2, 0, 0], bl: [0.6, 0, 0], br: [0.6, 0, 0], blLo: [-1.0, 0, 0], brLo: [-1.0, 0, 0] }], [0.3, { body: [-0.4, 0, 0], fl: [-1.2, 0, 0], fr: [-1.2, 0, 0], bl: [0.9, 0, 0], br: [0.9, 0, 0], jaw: [0.8, 0, 0], neck: [-0.2, 0, 0] }], [0.7, { body: [0.3, 0, 0], fl: [-0.8, 0, 0], fr: [-0.8, 0, 0], jaw: [0, 0, 0] }], [1, {}]],
  howl: [[0, {}], [0.3, { neck: [-1.1, 0, 0], head: [-0.4, 0, 0], jaw: [0.5, 0, 0], body: [-0.2, 0, 0] }], [0.85, { neck: [-1.2, 0, 0], head: [-0.5, 0, 0], jaw: [0.6, 0, 0], body: [-0.2, 0, 0] }], [1, {}]],
};
export function animQuad(m, st, dt) {
  const p = m.p, sc = m.scale || 1, spd = st.speed || 0; const amp = clamp(spd / (6 * sc), 0, 1.4);
  st.phase = (st.phase || 0) + dt * (spd / (0.9 * sc)) * 2.4; const ph = st.phase, t = st.time || 0;
  const s = Math.sin(ph), c = Math.cos(ph);
  const gallop = amp > 1;
  setT(p.body, gallop ? Math.sin(ph) * 0.1 : 0, 0, 0); setT(p.neck, -0.2 + Math.sin(t * 1.5) * 0.04 + (st.lookX || 0), st.lookY || 0, 0); setT(p.head, 0.15, 0, 0); setT(p.jaw, 0.05 + (st.pant ? Math.abs(Math.sin(t * 8)) * 0.2 : 0), 0, 0);
  const legs = gallop ? [['fl', 0], ['fr', 0.4], ['bl', Math.PI], ['br', Math.PI + 0.4]] : [['fl', 0], ['fr', Math.PI], ['bl', Math.PI], ['br', 0]];
  for (const [n, off] of legs) {
    const sp = Math.sin(ph + off), cp = Math.cos(ph + off); const back = n[0] === 'b';
    setT(p[n], -sp * 0.55 * amp, 0, 0);
    setT(p[n + 'Lo'], back ? -(0.15 + Math.max(0, cp) * 0.9) * amp - 0.1 : (0.1 + Math.max(0, cp) * 0.9) * amp, 0, 0);
  }
  let by = 0.72;
  by += Math.abs(c) * 0.05 * amp;
  const a = st.act;
  if (a && QACTS[a.name]) { const w = clamp(Math.min(a.t * 8, (1 - a.t) * 6 + 0.2), 0, 1); sample(QACTS[a.name], a.t, p, w); }
  if (st.air) { setT(p.fl, -1.1, 0, 0); setT(p.fr, -1.1, 0, 0); setT(p.bl, 1.0, 0, 0); setT(p.br, 1.0, 0, 0); setT(p.body, (st.vy || 0) > 0 ? -0.3 : 0.2, 0, 0); }
  if (st.hurt > 0) { addT(p.body, 0, 0, 0.2 * st.hurt); addT(p.neck, 0.4 * st.hurt, 0, 0); }
  p.tail?.forEach((tt, i) => setT(tt, (i === 0 ? -0.5 : 0.15) + (st.aggro ? -0.3 : 0.2), Math.sin(t * (st.aggro ? 10 : 4) - i * 0.8) * 0.3, 0));
  if (st.dead >= 0 && st.dead != null) {
    const d = ease(clamp(st.dead / 0.5, 0, 1)); setT(p.body, 0, 0, 1.5 * d); by = 0.72 - 0.45 * d;
    for (const n of ['fl', 'fr', 'bl', 'br']) setT(p[n], 0.3, 0, 0);
  }
  p.body.position.y = damp(p.body.position.y, by, 14, dt);
  apply([p.body, p.neck, p.head, p.jaw, p.fl, p.fr, p.bl, p.br, p.flLo, p.frLo, p.blLo, p.brLo], a ? 20 : 12, dt);
  apply(p.tail || [], 8, dt);
}

// ---------------- ÖRÜMCEK / AKREP ----------------
const SACTS = {
  bite: [[0, {}], [0.35, { body: [-0.35, 0, 0], head: [-0.2, 0, 0] }], [0.55, { body: [0.2, 0, 0], head: [0.2, 0, 0] }], [1, {}]],
  spit: [[0, {}], [0.4, { body: [-0.5, 0, 0] }], [0.6, { body: [0.1, 0, 0] }], [1, {}]],
  pinch: [[0, {}], [0.35, { lArm: [0, 1.1, 0], rArm: [0, -1.1, 0], lClaw: [0, -0.8, 0], rClaw: [0, 0.8, 0], body: [-0.1, 0, 0] }], [0.55, { lArm: [0, 0.1, 0], rArm: [0, -0.1, 0], lFore: [0, -0.3, 0], rFore: [0, 0.3, 0], lClaw: [0, 0, 0], rClaw: [0, 0, 0], body: [0.15, 0, 0] }], [1, {}]],
  sting: [[0, {}], [0.4, { t0: [-1.3, 0, 0], t1: [-0.7, 0, 0], t2: [-0.7, 0, 0], body: [0.15, 0, 0] }], [0.58, { t0: [-0.2, 0, 0], t1: [-0.1, 0, 0], t2: [-0.2, 0, 0], t3: [-0.3, 0, 0], t4: [-0.2, 0, 0], t5: [-0.1, 0, 0], body: [-0.15, 0, 0] }], [1, {}]],
  slam: [[0, {}], [0.4, { body: [-0.6, 0, 0] }], [0.55, { body: [0.3, 0, 0] }], [1, {}]],
};
export function animArachnid(m, st, dt) {
  const p = m.p, sc = m.scale || 1, spd = st.speed || 0, amp = clamp(spd / (5 * sc), 0, 1.3), scorp = m.kind === 'scorpion';
  st.phase = (st.phase || 0) + dt * (spd / (0.6 * sc)) * 2.2; const ph = st.phase, t = st.time || 0;
  if (scorp) p.tail.forEach((tt, i) => { p['t' + i] = tt; });
  setT(p.body, Math.sin(t * 2) * 0.02, 0, Math.sin(ph) * 0.04 * amp); setT(p.head, 0, 0, 0);
  if (p.abdomen) setT(p.abdomen, Math.sin(t * 2.5) * 0.05, 0, 0);
  for (const L of p.legs) {
    const grp = (L.i + (L.sx > 0 ? 0 : 1)) % 2; const lp = ph + grp * Math.PI;
    const baseY = L.sx * (Math.PI / 2) - L.sx * (L.i - (p.legs.length / 2 - 1) / 2) * (scorp ? 0.4 : 0.45);
    const lift = Math.max(0, Math.sin(lp)) * 0.45 * amp;
    setT(L.hip, 0, baseY + Math.cos(lp) * 0.3 * amp * L.sx, 0);
    setT(L.fem, (scorp ? -0.7 : -0.6) - lift, 0, 0); setT(L.tib, (scorp ? 1.5 : 1.9) + lift * 0.4, 0, 0);
  }
  if (scorp) {
    const aggro = st.aggro ? 1 : 0;
    p.tail.forEach((tt, i) => setT(tt, (i ? -0.45 : -0.9) + Math.sin(t * 2 + i) * 0.05 - aggro * 0.08, Math.sin(t * 1.5 - i * 0.5) * 0.08, 0));
    setT(p.lArm, 0, 0.5, 0); setT(p.rArm, 0, -0.5, 0); setT(p.lFore, 0, -1.0, 0); setT(p.rFore, 0, 1.0, 0);
    setT(p.lClaw, 0, -0.3 - Math.abs(Math.sin(t * 3)) * 0.3 * aggro, 0); setT(p.rClaw, 0, 0.3 + Math.abs(Math.sin(t * 3)) * 0.3 * aggro, 0);
  } else if (p.fangs) p.fangs.forEach((f, i) => setT(f, Math.sin(t * 6) * 0.1, 0, (i ? 1 : -1) * (0.1 + Math.abs(Math.sin(t * 5)) * 0.2)));
  const a = st.act;
  if (a && SACTS[a.name]) { const w = clamp(Math.min(a.t * 8, (1 - a.t) * 6 + 0.2), 0, 1); sample(SACTS[a.name], a.t, p, w); if (!scorp && a.name === 'bite' && a.t > 0.2 && a.t < 0.6) { const fr = p.legs.filter((l) => l.i === 0); fr.forEach((l) => setT(l.fem, -1.4, 0, 0)); } }
  let by = scorp ? 0.42 : 0.55;
  if (st.hurt > 0) addT(p.body, 0.25 * st.hurt, 0, 0);
  if (st.dead >= 0 && st.dead != null) { const d = ease(clamp(st.dead / 0.5, 0, 1)); setT(p.body, 0, 0, Math.PI * d * 0.95); by = by + 0.2 * d; p.legs.forEach((L) => { setT(L.fem, -1.2, 0, 0); setT(L.tib, 2.4, 0, 0); }); }
  p.body.position.y = damp(p.body.position.y, by + Math.abs(Math.sin(ph)) * 0.03 * amp, 12, dt);
  const list = [p.body, p.head, p.abdomen, ...(p.fangs || [])]; for (const L of p.legs) list.push(L.hip, L.fem, L.tib);
  if (scorp) list.push(p.lArm, p.rArm, p.lFore, p.rFore, p.lClaw, p.rClaw, ...p.tail);
  apply(list, a ? 22 : 14, dt);
}

// ---------------- BALÇIK ----------------
export function animSlime(m, st, dt) {
  const p = m.p, t = st.time || 0, spd = st.speed || 0;
  st.hop = (st.hop || 0) + dt * (spd > 0.2 ? 2.4 : 0.8);
  const h = st.hop % 1; let y = 0, sy = 1, sxz = 1;
  if (spd > 0.2) { const up = Math.sin(h * Math.PI); y = up * 0.9; sy = h < 0.12 ? 0.7 + h * 2.5 : 1 + up * 0.25; sxz = 1 / Math.sqrt(sy); }
  else { sy = 1 + Math.sin(t * 3) * 0.05; sxz = 1 - Math.sin(t * 3) * 0.03; }
  const a = st.act;
  if (a) { if (a.t < 0.4) { sy = 1 - a.t * 1.2; sxz = 1 + a.t * 0.8; } else if (a.t < 0.7) { y = Math.sin(((a.t - 0.4) / 0.3) * Math.PI) * 1.6; sy = 1.3; sxz = 0.85; } else { sy = 0.7 + (a.t - 0.7); sxz = 1.25 - (a.t - 0.7) * 0.8; } }
  if (st.hurt > 0) { sy *= 1 - st.hurt * 0.3; sxz *= 1 + st.hurt * 0.2; }
  if (st.dead >= 0 && st.dead != null) { const d = clamp(st.dead / 0.4, 0, 1); sy = 1 - d * 0.9; sxz = 1 + d * 0.8; y = 0; }
  p.body.position.y = damp(p.body.position.y, y, 18, dt);
  p.body.scale.set(damp(p.body.scale.x, sxz, 18, dt), damp(p.body.scale.y, sy, 18, dt), damp(p.body.scale.z, sxz, 18, dt));
}

// ---------------- EJDERHA ----------------
const DACTS = {
  bite: [[0, {}], [0.35, { n0: [-0.9, 0, 0], n1: [0.3, 0, 0], n2: [0.3, 0, 0], head: [-0.3, 0, 0], jaw: [0.8, 0, 0] }], [0.55, { n0: [-0.1, 0, 0], n1: [0.3, 0, 0], n2: [0.3, 0, 0], n3: [0.2, 0, 0], head: [0.3, 0, 0], jaw: [0, 0, 0], body: [0.15, 0, 0] }], [1, {}]],
  claw: [[0, {}], [0.4, { fr: [-1.7, 0, -0.3], frLo: [0.9, 0, 0], body: [-0.25, 0, 0.1] }], [0.58, { fr: [-0.2, 0, 0.3], frLo: [0.1, 0, 0], body: [0.15, 0, -0.1] }], [1, {}]],
  breath: [[0, {}], [0.25, { n0: [-1.0, 0, 0], head: [-0.4, 0, 0], jaw: [0.2, 0, 0], body: [-0.1, 0, 0] }], [0.35, { n0: [-0.2, 0, 0], n1: [0.25, 0, 0], n2: [0.25, 0, 0], n3: [0.2, 0, 0], head: [0.2, 0, 0], jaw: [0.9, 0, 0], body: [0.1, 0, 0] }], [0.9, { n0: [-0.2, 0, 0], n1: [0.25, 0, 0], n2: [0.25, 0, 0], n3: [0.2, 0, 0], head: [0.2, 0, 0], jaw: [0.9, 0, 0] }], [1, {}]],
  tail: [[0, {}], [0.4, { body: [0, 0.4, 0] }], [0.7, { body: [0, -0.9, 0] }], [1, {}]],
  roar: [[0, {}], [0.3, { n0: [-1.2, 0, 0], n1: [-0.2, 0, 0], head: [-0.5, 0, 0], jaw: [1.0, 0, 0], body: [-0.3, 0, 0] }], [0.85, { n0: [-1.3, 0, 0], head: [-0.6, 0, 0], jaw: [1.0, 0, 0], body: [-0.35, 0, 0] }], [1, {}]],
  stomp: [[0, {}], [0.4, { body: [-0.5, 0, 0], fl: [-1.2, 0, 0], fr: [-1.2, 0, 0] }], [0.55, { body: [0.15, 0, 0], fl: [0, 0, 0], fr: [0, 0, 0] }], [1, {}]],
};
export function animDragon(m, st, dt) {
  const p = m.p, sc = m.scale || 1, spd = st.speed || 0, amp = clamp(spd / (6 * sc), 0, 1.2), t = st.time || 0;
  p.neck.forEach((n, i) => { p['n' + i] = n; });
  st.phase = (st.phase || 0) + dt * (spd / (1.8 * sc)) * 2.2; const ph = st.phase;
  const fly = st.fly || 0;
  setT(p.body, -0.05 * fly, 0, 0); setT(p.head, 0.25, 0, 0); setT(p.jaw, 0.08 + Math.sin(t * 1.3) * 0.04, 0, 0);
  p.neck.forEach((n, i) => setT(n, i === 0 ? -0.55 + (st.lookX || 0) : 0.12 + Math.sin(t * 1.5 - i) * 0.03, i === 0 ? (st.lookY || 0) * 0.5 : (st.lookY || 0) * 0.15, 0));
  for (const [n, off] of [['fl', 0], ['fr', Math.PI], ['bl', Math.PI], ['br', 0]]) {
    const sp = Math.sin(ph + off), cp = Math.cos(ph + off);
    if (fly > 0.5) { setT(p[n], n[0] === 'f' ? -0.9 : 1.0, 0, 0); setT(p[n + 'Lo'], n[0] === 'f' ? 1.4 : -0.3, 0, 0); }
    else { setT(p[n], -sp * 0.5 * amp, 0, 0); setT(p[n + 'Lo'], (0.1 + Math.max(0, cp) * 0.7) * amp * (n[0] === 'b' ? -1 : 1), 0, 0); }
  }
  // kanatlar
  const flap = fly > 0.3 ? Math.sin(t * 5.5) : 0;
  p.wings.forEach((w) => {
    if (fly > 0.3) { setT(w.sh, 0, w.sx * 0.1, w.sx * (flap * 0.9 + 0.1)); setT(w.fa, 0, 0, w.sx * (flap * 0.5 - 0.1)); }
    else if (st.wingsOpen) { setT(w.sh, 0, w.sx * 0.3, w.sx * 0.35 + Math.sin(t * 3) * 0.1 * w.sx); setT(w.fa, 0, 0, w.sx * -0.2); }
    else { setT(w.sh, 0, w.sx * -0.9, w.sx * 0.45); setT(w.fa, 0, w.sx * 2.5, w.sx * -0.1); }
  });
  p.tail.forEach((tt, i) => setT(tt, i ? 0.05 - fly * 0.08 : 0.2, Math.sin(t * 1.8 - i * 0.6) * 0.15 * (1 + amp), 0));
  const a = st.act;
  if (a && DACTS[a.name]) { const w = clamp(Math.min(a.t * 8, (1 - a.t) * 6 + 0.2), 0, 1); sample(DACTS[a.name], a.t, p, w); if (a.name === 'tail') p.tail.forEach((tt, i) => addT(tt, 0, (a.t < 0.5 ? -0.25 : 0.35) * Math.sin(a.t * Math.PI), 0)); }
  if (st.hurt > 0) addT(p.n0, 0.3 * st.hurt, 0, 0);
  let by = 1.3 + Math.abs(Math.cos(ph)) * 0.05 * amp;
  if (st.dead >= 0 && st.dead != null) { const d = ease(clamp(st.dead / 1.2, 0, 1)); setT(p.body, 0, 0, 1.3 * d); by = 1.3 - 0.6 * d; p.neck.forEach((n) => setT(n, 0.35 * d, 0, 0)); }
  p.body.position.y = damp(p.body.position.y, by, 10, dt);
  apply([p.body, p.head, p.jaw, ...p.neck, p.fl, p.fr, p.bl, p.br, p.flLo, p.frLo, p.blLo, p.brLo], a ? 14 : 8, dt);
  apply(p.wings.flatMap((w) => [w.sh, w.fa]), fly > 0.3 ? 20 : 6, dt);
  apply(p.tail, 5, dt);
}

export function animate(m, st, dt) {
  switch (m.kind) {
    case 'humanoid': return animHumanoid(m, st, dt);
    case 'quad': return animQuad(m, st, dt);
    case 'spider': case 'scorpion': return animArachnid(m, st, dt);
    case 'slime': return animSlime(m, st, dt);
    case 'dragon': return animDragon(m, st, dt);
  }
}
