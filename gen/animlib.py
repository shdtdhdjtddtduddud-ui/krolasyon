"""shared animation helpers"""


def add_rot(a, frames, interp='C'):
    out = {}
    for t, d in frames:
        for b, v in d.items():
            out.setdefault(b, []).append((t, v))
    for b, keys in out.items():
        a.rot(b, *[(t, v, interp) for t, v in keys])


def sym(d):
    """mirror right_* rotation entries to left_*"""
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
    return o


def zero(names):
    return {n: (0, 0, 0) for n in names}


def pose_seq(a, frames, names, interp='C'):
    """frames: list of (t, dict); every frame is completed with zeros for names not given, so poses blend back cleanly"""
    full = []
    for t, d in frames:
        f = zero(names)
        f.update(d)
        full.append((t, f))
    add_rot(a, full, interp)
