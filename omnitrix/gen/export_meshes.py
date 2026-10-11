"""Stage 2a: export each Higgsfield GLB as a compact npz (positions, uvs, faces) + its base colour texture (512px)."""
import json, os
import numpy as np
import trimesh
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, 'models_raw')
OUT = os.path.join(HERE, 'mesh')
os.makedirs(OUT, exist_ok=True)
for name in [k for k in json.load(open(os.path.join(HERE, 'higgsfield_models.json'))) if not k.startswith('_')]:
    m = trimesh.load(os.path.join(RAW, name + '.glb'), force='mesh', process=False)
    uv = np.asarray(m.visual.uv, dtype=np.float32)
    mat = m.visual.material
    img = getattr(mat, 'baseColorTexture', None) or getattr(mat, 'image', None)
    img.convert('RGBA').resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, name + '.png'))
    np.savez_compressed(os.path.join(OUT, name + '.npz'), v=np.asarray(m.vertices, np.float32), f=np.asarray(m.faces, np.int32), uv=uv)
    print(name, len(m.vertices), len(m.faces), uv.min(0), uv.max(0))
