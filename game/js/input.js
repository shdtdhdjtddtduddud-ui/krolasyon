// Girdi: klavye + fare (işaretçi kilidi) ve dokunmatik sanal joystick/butonlar
export class Input {
  constructor(canvas) {
    this.canvas = canvas; this.keys = {}; this.events = []; this.mx = 0; this.mz = 0; this.dYaw = 0; this.dPitch = 0; this.dZoom = 0;
    this.locked = false; this.sens = 1; this.attackHeld = false; this.enabled = false;
    this.touch = matchMedia('(pointer: coarse)').matches || ('ontouchstart' in window && navigator.maxTouchPoints > 0);
    const K = { KeyQ: 'potHp', KeyR: 'potMp', KeyE: 'interact', Tab: 'lock', KeyI: 'inv', KeyC: 'inv', KeyJ: 'quests', KeyM: 'map', Escape: 'menu', Space: 'jump', Digit1: 'skill0', Digit2: 'skill1', Digit3: 'skill2', Digit4: 'skill3', KeyF: 'dodge', AltLeft: 'dodge' };
    addEventListener('keydown', (e) => {
      if (e.target.tagName === 'INPUT') return;
      if (e.code === 'Tab' || e.code === 'Space' || e.code.startsWith('Arrow')) e.preventDefault();
      if (!this.keys[e.code] && K[e.code]) this.events.push(K[e.code]);
      this.keys[e.code] = true;
    });
    addEventListener('keyup', (e) => { this.keys[e.code] = false; });
    addEventListener('blur', () => { this.keys = {}; this.attackHeld = false; });
    canvas.addEventListener('contextmenu', (e) => e.preventDefault());
    canvas.addEventListener('mousedown', (e) => {
      if (this.touch || !this.enabled) return;
      if (!this.locked && !this.lockFailed) { this.requestLock(); if (e.button === 0) return; }
      this.dragging = !this.locked; this.lastX = e.clientX; this.lastY = e.clientY; this.downX = e.clientX; this.downY = e.clientY;
      if (e.button === 0) { if (this.locked) { this.events.push('attack'); this.attackHeld = true; } else this.pendingClick = true; }
      if (e.button === 2) this.events.push('dodge');
      if (e.button === 1) this.events.push('lock');
    });
    addEventListener('mouseup', (e) => { if (e.button === 0) { this.attackHeld = false; if (this.pendingClick && Math.hypot(e.clientX - this.downX, e.clientY - this.downY) < 6) this.events.push('attack'); this.pendingClick = false; } this.dragging = false; });
    addEventListener('mousemove', (e) => {
      if (this.locked) { this.dYaw -= e.movementX * 0.0024 * this.sens; this.dPitch -= e.movementY * 0.0024 * this.sens; }
      else if (this.dragging) { this.dYaw -= (e.clientX - this.lastX) * 0.005 * this.sens; this.dPitch -= (e.clientY - this.lastY) * 0.005 * this.sens; this.lastX = e.clientX; this.lastY = e.clientY; }
    });
    canvas.addEventListener('wheel', (e) => { this.dZoom += Math.sign(e.deltaY); e.preventDefault(); }, { passive: false });
    document.addEventListener('pointerlockchange', () => { this.locked = document.pointerLockElement === canvas; this.onLockChange?.(this.locked); });
    document.addEventListener('pointerlockerror', () => { this.lockFailed = true; });
    if (this.touch) this.setupTouch();
  }
  requestLock() { try { const r = this.canvas.requestPointerLock(); if (r && r.catch) r.catch(() => { this.lockFailed = true; }); } catch (e) { this.lockFailed = true; } }
  releaseLock() { if (document.pointerLockElement) document.exitPointerLock(); }
  setupTouch() {
    document.body.classList.add('touch');
    const joy = document.getElementById('joy'), knob = document.getElementById('joyKnob'); let jid = null, jx = 0, jy = 0; const R = 55;
    const camZone = document.getElementById('camZone'); let cid = null, cx = 0, cy = 0;
    const start = (e) => {
      for (const t of e.changedTouches) {
        if (t.clientX < innerWidth * 0.42 && jid === null && t.target.closest('#camZone, #joyZone')) { jid = t.identifier; jx = t.clientX; jy = t.clientY; joy.style.left = jx + 'px'; joy.style.top = jy + 'px'; joy.classList.add('on'); }
        else if (cid === null && t.target.closest('#camZone')) { cid = t.identifier; cx = t.clientX; cy = t.clientY; }
      }
    };
    const move = (e) => {
      for (const t of e.changedTouches) {
        if (t.identifier === jid) { let dx = t.clientX - jx, dy = t.clientY - jy; const l = Math.hypot(dx, dy); if (l > R) { dx *= R / l; dy *= R / l; } knob.style.transform = `translate(${dx}px, ${dy}px)`; this.tmx = dx / R; this.tmz = -dy / R; this.tsprint = l > R * 1.6; }
        if (t.identifier === cid) { this.dYaw -= (t.clientX - cx) * 0.007 * this.sens; this.dPitch -= (t.clientY - cy) * 0.006 * this.sens; cx = t.clientX; cy = t.clientY; }
      }
      e.preventDefault();
    };
    const end = (e) => { for (const t of e.changedTouches) { if (t.identifier === jid) { jid = null; this.tmx = this.tmz = 0; knob.style.transform = ''; joy.classList.remove('on'); } if (t.identifier === cid) cid = null; } };
    for (const el of [camZone, document.getElementById('joyZone')]) { el.addEventListener('touchstart', start, { passive: false }); }
    addEventListener('touchmove', move, { passive: false }); addEventListener('touchend', end); addEventListener('touchcancel', end);
    document.querySelectorAll('[data-act]').forEach((b) => {
      const act = b.dataset.act;
      b.addEventListener('touchstart', (e) => { e.preventDefault(); e.stopPropagation(); this.events.push(act); if (act === 'attack') this.attackHeld = true; if (act === 'sprint') this.tsprintBtn = !this.tsprintBtn; b.classList.add('down'); }, { passive: false });
      b.addEventListener('touchend', (e) => { e.preventDefault(); if (act === 'attack') this.attackHeld = false; b.classList.remove('down'); }, { passive: false });
    });
  }
  poll() {
    const k = this.keys;
    let x = (k.KeyD || k.ArrowRight ? 1 : 0) - (k.KeyA || k.ArrowLeft ? 1 : 0), z = (k.KeyW || k.ArrowUp ? 1 : 0) - (k.KeyS || k.ArrowDown ? 1 : 0);
    if (this.touch) { x += this.tmx || 0; z += this.tmz || 0; }
    this.mx = x; this.mz = z; this.sprint = !!(k.ShiftLeft || k.ShiftRight || this.tsprint || this.tsprintBtn);
    if (k.KeyQ && k.ShiftLeft) {}
    const ev = this.events; this.events = []; return ev;
  }
}
