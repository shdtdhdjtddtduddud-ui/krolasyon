// Klavye/fare ve dokunmatik kontroller
export class Input {
  constructor(canvas, isMobile) {
    this.canvas = canvas;
    this.isMobile = isMobile;
    this.keys = new Set();
    this.look = { x: 0, y: 0 };
    this.move = { x: 0, y: 0 }; // dokunmatik joystick
    this.actions = [];
    this.runToggle = false;
    this.enabled = true;
    this.locked = false;

    window.addEventListener('keydown', (e) => {
      if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) return;
      if (!this.keys.has(e.code)) this.onKey(e.code);
      this.keys.add(e.code);
      if (['Space', 'Tab', 'ArrowUp', 'ArrowDown'].includes(e.code)) e.preventDefault();
    });
    window.addEventListener('keyup', (e) => this.keys.delete(e.code));
    window.addEventListener('blur', () => this.keys.clear());

    if (!isMobile) {
      canvas.addEventListener('click', () => {
        if (this.enabled && !this.locked && canvas.requestPointerLock) {
          try { const p = canvas.requestPointerLock(); if (p && p.catch) p.catch(() => {}); } catch (e) { /* yok say */ }
        }
      });
      document.addEventListener('pointerlockchange', () => {
        this.locked = document.pointerLockElement === canvas;
        if (this.onLockChange) this.onLockChange(this.locked);
      });
      document.addEventListener('mousemove', (e) => {
        if (!this.locked) return;
        this.look.x += e.movementX; this.look.y += e.movementY;
      });
      canvas.addEventListener('mousedown', (e) => {
        if (!this.locked) return;
        if (e.button === 0) this.actions.push('use');
        if (e.button === 2) this.actions.push('interact');
      });
      canvas.addEventListener('contextmenu', (e) => e.preventDefault());
      window.addEventListener('wheel', (e) => {
        if (!this.locked) return;
        this.actions.push(e.deltaY > 0 ? 'nextTool' : 'prevTool');
      }, { passive: true });
    }
    this.setupTouch();
  }
  onKey(code) {
    const map = {
      KeyE: 'interact', KeyF: 'interact', Space: 'jump', KeyI: 'bag', Tab: 'bag', KeyM: 'map', KeyQ: 'nextSeed',
      KeyH: 'help', Escape: 'escape', KeyR: 'eat',
    };
    if (map[code]) this.actions.push(map[code]);
    if (code.startsWith('Digit')) {
      const n = parseInt(code.slice(5), 10);
      if (n >= 1 && n <= 6) this.actions.push('tool' + (n - 1));
    }
  }
  setupTouch() {
    const joy = document.getElementById('joy');
    const knob = document.getElementById('joyKnob');
    const lookArea = document.getElementById('lookArea');
    if (!joy || !lookArea) return;
    let joyId = null, cx = 0, cy = 0;
    const R = 55;
    joy.addEventListener('touchstart', (e) => {
      const t = e.changedTouches[0];
      joyId = t.identifier;
      const r = joy.getBoundingClientRect();
      cx = r.left + r.width / 2; cy = r.top + r.height / 2;
      this.updJoy(t.clientX - cx, t.clientY - cy, knob, R);
      e.preventDefault();
    }, { passive: false });
    joy.addEventListener('touchmove', (e) => {
      for (const t of e.changedTouches) if (t.identifier === joyId) this.updJoy(t.clientX - cx, t.clientY - cy, knob, R);
      e.preventDefault();
    }, { passive: false });
    const endJoy = (e) => {
      for (const t of e.changedTouches) if (t.identifier === joyId) {
        joyId = null; this.move.x = 0; this.move.y = 0; knob.style.transform = 'translate(-50%,-50%)';
      }
    };
    joy.addEventListener('touchend', endJoy);
    joy.addEventListener('touchcancel', endJoy);

    const lookTouches = new Map();
    lookArea.addEventListener('touchstart', (e) => {
      for (const t of e.changedTouches) lookTouches.set(t.identifier, { x: t.clientX, y: t.clientY, sx: t.clientX, sy: t.clientY, t: performance.now() });
      e.preventDefault();
    }, { passive: false });
    lookArea.addEventListener('touchmove', (e) => {
      for (const t of e.changedTouches) {
        const p = lookTouches.get(t.identifier);
        if (!p) continue;
        this.look.x += (t.clientX - p.x) * 2.2;
        this.look.y += (t.clientY - p.y) * 2.2;
        p.x = t.clientX; p.y = t.clientY;
      }
      e.preventDefault();
    }, { passive: false });
    const endLook = (e) => {
      for (const t of e.changedTouches) {
        const p = lookTouches.get(t.identifier);
        if (p && performance.now() - p.t < 220 && Math.hypot(t.clientX - p.sx, t.clientY - p.sy) < 12) this.actions.push('use');
        lookTouches.delete(t.identifier);
      }
    };
    lookArea.addEventListener('touchend', endLook);
    lookArea.addEventListener('touchcancel', endLook);

    document.querySelectorAll('[data-act]').forEach((el) => {
      el.addEventListener('touchstart', (e) => {
        const a = el.dataset.act;
        if (a === 'run') { this.runToggle = !this.runToggle; el.classList.toggle('on', this.runToggle); }
        else this.actions.push(a);
        e.preventDefault(); e.stopPropagation();
      }, { passive: false });
      el.addEventListener('click', (e) => {
        if (this.isMobile) return;
        const a = el.dataset.act;
        if (a === 'run') { this.runToggle = !this.runToggle; el.classList.toggle('on', this.runToggle); }
        else this.actions.push(a);
      });
    });
  }
  updJoy(dx, dy, knob, R) {
    const d = Math.hypot(dx, dy);
    if (d > R) { dx *= R / d; dy *= R / d; }
    this.move.x = dx / R; this.move.y = -dy / R;
    knob.style.transform = `translate(calc(-50% + ${dx}px), calc(-50% + ${dy}px))`;
  }
  axis() {
    let x = this.move.x, y = this.move.y;
    if (this.keys.has('KeyW') || this.keys.has('ArrowUp')) y += 1;
    if (this.keys.has('KeyS') || this.keys.has('ArrowDown')) y -= 1;
    if (this.keys.has('KeyD') || this.keys.has('ArrowRight')) x += 1;
    if (this.keys.has('KeyA') || this.keys.has('ArrowLeft')) x -= 1;
    const l = Math.hypot(x, y);
    if (l > 1) { x /= l; y /= l; }
    return { x, y };
  }
  running() { return this.keys.has('ShiftLeft') || this.keys.has('ShiftRight') || this.runToggle; }
  consumeLook() { const l = { ...this.look }; this.look.x = 0; this.look.y = 0; return l; }
  consumeActions() { const a = this.actions; this.actions = []; return a; }
  unlock() { if (document.exitPointerLock && this.locked) document.exitPointerLock(); }
}
