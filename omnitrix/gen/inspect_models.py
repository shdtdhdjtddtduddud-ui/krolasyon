"""Stage 1: download the Higgsfield GLBs, report their structure and render orthographic previews (6 views)."""
import json, os, sys, urllib.request
import numpy as np
import trimesh
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, 'models_raw')
OUT = os.path.join(HERE, 'inspect')
os.makedirs(RAW, exist_ok=True)
os.makedirs(OUT, exist_ok=True)
urls = {k: v for k, v in json.load(open(os.path.join(HERE, 'higgsfield_models.json'))).items() if not k.startswith('_')}


def fetch(name):
    p = os.path.join(RAW, name + '.glb')
    if not os.path.exists(p):
        with urllib.request.urlopen(urls[name]) as r:
            open(p, 'wb').write(r.read())
    return p


def colors_of(mesh):
    v = mesh.visual
    if isinstance(v, trimesh.visual.TextureVisuals):
        try:
            return v.to_color().vertex_colors[:, :3]
        except Exception as e:
            print('  to_color failed', e)
    try:
        return np.asarray(v.vertex_colors)[:, :3]
    except Exception:
        return np.full((len(mesh.vertices), 3), 200)


def splat(pts, cols, axis_u, axis_v, depth_axis, size=360, flip_depth=False):
    img = np.full((size, size, 3), 255, np.uint8)
    zb = np.full((size, size), -1e9)
    u, v, d = pts[:, axis_u], pts[:, axis_v], pts[:, depth_axis] * (-1 if flip_depth else 1)
    lo = pts.min(0); hi = pts.max(0)
    span = max(hi[axis_u] - lo[axis_u], hi[axis_v] - lo[axis_v]) * 1.05
    cu, cv = (hi[axis_u] + lo[axis_u]) / 2, (hi[axis_v] + lo[axis_v]) / 2
    x = ((u - cu) / span + 0.5) * (size - 1)
    y = (0.5 - (v - cv) / span) * (size - 1)
    xi, yi = x.astype(int), y.astype(int)
    for dx in (0, 1):
        for dy in (0, 1):
            xx, yy = np.clip(xi + dx, 0, size - 1), np.clip(yi + dy, 0, size - 1)
            order = np.argsort(d)
            # vectorised z-buffer: write in depth order so nearer points overwrite
            img[yy[order], xx[order]] = cols[order]
    return img


report = {}
names = sys.argv[1:] or list(urls)
for name in names:
    path = fetch(name)
    scene = trimesh.load(path)
    info = {'bytes': os.path.getsize(path), 'type': type(scene).__name__}
    if isinstance(scene, trimesh.Scene):
        info['geometries'] = {k: [len(g.vertices), len(g.faces), type(g.visual).__name__] for k, g in scene.geometry.items()}
        info['graph_nodes'] = len(scene.graph.nodes)
    mesh = trimesh.load(path, force='mesh')
    info['vertices'] = len(mesh.vertices)
    info['faces'] = len(mesh.faces)
    info['bounds'] = mesh.bounds.tolist()
    info['visual'] = type(mesh.visual).__name__
    if isinstance(mesh.visual, trimesh.visual.TextureVisuals) and mesh.visual.material is not None:
        mat = mesh.visual.material
        img = getattr(mat, 'baseColorTexture', None) or getattr(mat, 'image', None)
        info['texture'] = None if img is None else list(img.size)
    report[name] = info
    print(name, json.dumps(info))
    # dense surface samples for previews
    pts, fid = trimesh.sample.sample_surface(mesh, 200000)
    cols = colors_of(mesh)[mesh.faces[fid][:, 0]]
    views = [(0, 1, 2, False, '+z front'), (0, 1, 2, True, '-z back'), (2, 1, 0, False, '+x'), (2, 1, 0, True, '-x'), (0, 2, 1, False, '+y top'), (0, 2, 1, True, '-y bottom')]
    tiles = [splat(pts, cols, a, b, c, flip_depth=f) for a, b, c, f, _ in views]
    sheet = np.concatenate([np.concatenate(tiles[:3], 1), np.concatenate(tiles[3:], 1)], 0)
    Image.fromarray(sheet).save(os.path.join(OUT, name + '.png'))
json.dump(report, open(os.path.join(OUT, 'report.json'), 'w'), indent=1)
