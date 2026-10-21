"""Film ses miksajı: oyun içinde günlüğe yazılan ses olaylarını (konum, ses, perde) ve müzik işaretlerini
Minecraft'ın kendi ses dosyalarından tek bir stereo WAV'a karıştırır.

kullanım: python3 mix.py events.jsonl out.wav libs_dir
"""
import glob
import json
import math
import os
import subprocess
import sys
import wave
import zipfile

import numpy as np

SR = 48000


def find_assets():
    roots = [os.path.expanduser(p) for p in ("~/.gradle/caches/forge_gradle/assets", "~/.gradle/caches/minecraft/assets",
                                             "~/.minecraft/assets")]
    for base in (os.path.expanduser("~/.gradle/caches/forge_gradle"), os.path.expanduser("~/.gradle/caches/minecraft")):
        for dp, dn, fn in os.walk(base):
            if os.path.basename(dp) == "indexes" and any(f.endswith(".json") for f in fn):
                roots.append(os.path.dirname(dp))
    for r in roots:
        idx = sorted(glob.glob(os.path.join(r, "indexes", "*.json")))
        if not idx:
            continue
        pick = [i for i in idx if os.path.basename(i) == "5.json"] or idx[-1:]
        with open(pick[0]) as f:
            objs = json.load(f)["objects"]
        if any(k.startswith("minecraft/sounds/") for k in objs):
            print("assets:", r, pick[0], len(objs))
            return r, objs
    print("WARNING: no asset index found")
    return None, {}


ROOT, OBJS = find_assets()
LIBS = sys.argv[3] if len(sys.argv) > 3 else "libs"
JARS = sorted(glob.glob(os.path.join(LIBS, "*.jar")))
CACHE = {}
MISSING = set()
TMP = os.path.join(os.path.dirname(os.path.abspath(sys.argv[2])), "snd_cache")
os.makedirs(TMP, exist_ok=True)


def locate(rl):
    ns, p = rl.split(":", 1)
    if ns == "minecraft":
        h = OBJS.get("minecraft/" + p, {}).get("hash")
        if h and ROOT:
            f = os.path.join(ROOT, "objects", h[:2], h)
            if os.path.exists(f):
                return f
        return None
    name = "assets/%s/%s" % (ns, p)
    for j in JARS:
        try:
            with zipfile.ZipFile(j) as z:
                if name in z.namelist():
                    out = os.path.join(TMP, ns + "_" + p.replace("/", "_"))
                    with open(out, "wb") as o:
                        o.write(z.read(name))
                    return out
        except zipfile.BadZipFile:
            pass
    return None


def load(rl):
    if rl in CACHE:
        return CACHE[rl]
    f = locate(rl)
    a = None
    if f:
        try:
            raw = subprocess.run(["ffmpeg", "-v", "error", "-i", f, "-f", "f32le", "-ac", "2", "-ar", str(SR), "-"],
                                 capture_output=True, check=True).stdout
            a = np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()
        except Exception as e:  # noqa
            print("decode failed", rl, e)
    if a is None:
        MISSING.add(rl)
    CACHE[rl] = a
    return a


def repitch(a, pitch):
    if abs(pitch - 1.0) < 0.01:
        return a
    n = int(len(a) / pitch)
    if n < 2:
        return a
    src = np.arange(len(a))
    idx = np.arange(n) * pitch
    return np.stack([np.interp(idx, src, a[:, c]) for c in (0, 1)], 1).astype(np.float32)


def add(buf, a, t, gl, gr):
    i0 = int(t * SR)
    if i0 >= len(buf) or i0 < 0:
        return
    i1 = min(len(buf), i0 + len(a))
    buf[i0:i1, 0] += a[: i1 - i0, 0] * gl
    buf[i0:i1, 1] += a[: i1 - i0, 1] * gr


def main():
    ev_path, out_wav = sys.argv[1], sys.argv[2]
    events = [json.loads(l) for l in open(ev_path, encoding="utf-8") if l.strip()]
    end = max([e.get("end", 0) for e in events] + [e.get("t", 0) for e in events]) + 1.0
    n = int(end * SR) + SR
    sfx = np.zeros((n, 2), np.float32)
    count = 0
    for e in events:
        if "p" not in e:
            continue
        a = load(e["p"])
        if a is None:
            continue
        pitch = min(2.0, max(0.5, float(e.get("pi", 1.0))))
        vol = float(e.get("v", 1.0))
        pan = 0.0
        g = vol
        if not e.get("rel", False) and e.get("lin", True) and "lx" in e:
            dx, dy, dz = e["x"] - e["lx"], e["y"] - e["ly"], e["z"] - e["lz"]
            dist = math.sqrt(dx * dx + dy * dy + dz * dz)
            maxd = max(vol, 1.0) * float(e.get("d", 16))
            g = min(vol, 1.0) * max(0.0, 1.0 - dist / maxd)
            hx, hz = dx, dz
            hl = math.hypot(hx, hz)
            if hl > 0.3:
                yaw = math.radians(e.get("yaw", 0.0))
                rx, rz = -math.cos(yaw), -math.sin(yaw)
                pan = (hx * rx + hz * rz) / hl * min(1.0, hl / 3.0)
        if g < 0.004:
            continue
        a = repitch(a, pitch)
        add(sfx, a, e["t"], g * (1 - max(0.0, pan) * 0.6), g * (1 - max(0.0, -pan) * 0.6))
        count += 1
    print("sfx events mixed:", count)

    # müzik: her işaret bir sonrakine kadar çalar (çapraz geçişle)
    cues = sorted([e for e in events if "music" in e], key=lambda e: e["t"])
    music = np.zeros_like(sfx)
    for i, c in enumerate(cues):
        track = None
        for cand in c["music"]:
            track = load(cand)
            if track is not None:
                print("music @%.1f: %s" % (c["t"], cand))
                break
        if track is None:
            print("music @%.1f: none of %s" % (c["t"], c["music"]))
            continue
        stop = cues[i + 1]["t"] if i + 1 < len(cues) else end
        need = int((stop - c["t"] + 2.5) * SR)
        reps = int(math.ceil(need / max(1, len(track))))
        a = np.concatenate([track] * reps)[:need].copy()
        fin, fout = int(1.2 * SR), int(2.5 * SR)
        a[:fin] *= np.linspace(0, 1, fin)[:, None]
        a[-fout:] *= np.linspace(1, 0, fout)[:, None]
        add(music, a, c["t"], c.get("v", 0.5), c.get("v", 0.5))

    # müziği yoğun efektlerde biraz kıs
    env = np.abs(sfx).mean(axis=1)
    k = int(0.3 * SR)
    env = np.convolve(env, np.ones(k) / k, mode="same")
    duck = 1.0 - 0.45 * np.clip(env / 0.25, 0, 1)
    mix = sfx * 0.95 + music * 1.25 * duck[:, None]
    peak = np.percentile(np.abs(mix), 99.95) + 1e-6
    mix = np.tanh(mix * (0.85 / peak)) * 0.97
    pcm = (mix * 32767).astype(np.int16)
    with wave.open(out_wav, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    print("wrote", out_wav, "%.1f s" % (len(pcm) / SR))
    if MISSING:
        print("missing sounds (%d):" % len(MISSING))
        for m in sorted(MISSING)[:60]:
            print("  ", m)


if __name__ == "__main__":
    main()
