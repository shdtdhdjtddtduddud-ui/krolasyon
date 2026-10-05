"""Armor sets: 64x64 armor textures (HD x4) with extra 3D parts, Java model code and inventory icons."""
import os
import numpy as np
import tex as T
from model import Model, Cube

H = T.hexc
R = 4  # texture resolution multiplier

# standard humanoid parts: name -> (u, v, w, h, d)
STD = {'head': (0, 0, 8, 8, 8), 'hat': (32, 0, 8, 8, 8), 'body': (16, 16, 8, 12, 4), 'right_arm': (40, 16, 4, 12, 4), 'right_leg': (0, 16, 4, 12, 4)}

SETS = {
    'shadow_monarch': dict(base='#1A1622', dark='#0A080E', accent='#9A6AFF', trim='#3A2A5A', glow=True),
    'crimson_knight': dict(base='#8C1622', dark='#2A0A10', accent='#D4A841', trim='#D4A841', glow=False, eye='#FF2A2A'),
    'high_orc': dict(base='#5A5E64', dark='#3A2A1E', accent='#E8E0C8', trim='#7A5A3A', glow=False),
    'hunter': dict(base='#3A4A5E', dark='#1E2632', accent='#4AD8FF', trim='#9AA8B8', glow=False),
}


class Part:
    """An extra cube attached to a standard part. Coordinates are model space (y down) relative to the part pivot."""

    def __init__(self, parent, name, x, y, z, w, h, d, mat, rot=(0, 0, 0), glow=False, inflate=0.0):
        self.parent, self.name = parent, name
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, int(w), int(h), int(d)
        self.mat, self.rot, self.glow, self.inflate = mat, rot, glow, inflate
        self.u = self.v = 0


def armor_mats(s):
    if s == 'high_orc':
        return T.m_leather(SETS[s]['dark'], '#C8A070'), T.m_metal(SETS[s]['base'], trim=SETS[s]['trim'])
    if s == 'hunter':
        return T.m_cloth(SETS[s]['base'], trim=SETS[s]['trim']), T.m_metal('#2A3442', rivets=False, trim='#4AD8FF')
    return T.m_metal(SETS[s]['base'], trim=SETS[s]['trim']), T.m_metal(SETS[s]['dark'], trim=SETS[s]['accent'])


def glow_lines(color):
    def d(img, g):
        h, w = img.shape[:2]
        c = H(color)
        T.line(img, w * 0.5, 0, w * 0.5, h * 0.55, c, max(1, w / 16))
        T.line(img, w * 0.5, h * 0.55, w * 0.15, h, c, max(1, w / 18))
        T.line(img, w * 0.5, h * 0.55, w * 0.85, h, c, max(1, w / 18))
        T.line(g, w * 0.5, 0, w * 0.5, h * 0.55, c, max(1, w / 16))
    return d


def eye_slit(color):
    def d(img, g):
        h, w = img.shape[:2]
        T.rect(img, w * 0.12, h * 0.42, w * 0.88, h * 0.55, H('#050505'))
        for cx in (0.3, 0.7):
            T.draw_mask(img, T.ellipse_mask(w, h, w * cx, h * 0.485, w * 0.09, h * 0.05, 0.4), H(color))
    return d


def goggles(color):
    def d(img, g):
        h, w = img.shape[:2]
        T.rect(img, w * 0.08, h * 0.35, w * 0.92, h * 0.55, H('#14181E'))
        for cx in (0.3, 0.7):
            T.draw_mask(img, T.ellipse_mask(w, h, w * cx, h * 0.45, w * 0.15, h * 0.08, 0.4), H(color))
    return d


def paint_set(s):
    """Returns (layer1, layer2, extras(list of Part))."""
    cfg = SETS[s]
    main, second = armor_mats(s)
    extras = []
    acc = cfg['accent']
    # ------------- standard faces painted with box-UV painter
    def box_paint(img, u, v, w, h, d, mat, decals=None, mask=None):
        c = Cube(None, 0, 0, 0, w, h, d, mat, decals=decals)
        c.u, c.v = u, v
        m = Model('tmp', 64, R)
        m.cubes = [c]
        m.tex_h = 64
        full, _ = m.paint()
        if mask is not None:
            full[:, :, 3] *= mask
        sub = full[v * R:(v + d + h) * R, u * R:(u + 2 * (w + d)) * R]
        a = sub[:, :, 3:4]
        dst = img[v * R:(v + d + h) * R, u * R:(u + 2 * (w + d)) * R]
        dst[:] = dst * (1 - a) + sub * a
    L1 = T.new(64 * R, 64 * R)
    L2 = T.new(64 * R, 64 * R)
    face = {'front': []}
    if s == 'shadow_monarch':
        face['front'].append(eye_slit('#B08AFF'))
    elif s == 'crimson_knight':
        face['front'].append(eye_slit('#FF2A2A'))
    elif s == 'hunter':
        face['front'].append(goggles('#4AD8FF'))
    else:
        face['front'].append(lambda img, g: T.rect(img, 0, img.shape[0] * 0.5, img.shape[1], img.shape[0], np.array([0, 0, 0, 0], np.float32)))
    box_paint(L1, 0, 0, 8, 8, 8, main, face)
    chest_dec = {'front': [glow_lines(acc)] if cfg['glow'] else [lambda img, g: T.decal_band(img, 0.4, 0.48, cfg['trim'])]}
    box_paint(L1, 16, 16, 8, 12, 4, main, chest_dec)
    box_paint(L1, 40, 16, 4, 12, 4, second)
    # boots: only the lower 4 pixels of the leg region
    boots = T.new(64 * R, 64 * R)
    box_paint(boots, 0, 16, 4, 12, 4, second)
    ys = np.arange(64 * R)[:, None]
    xs = np.arange(64 * R)[None, :]
    sides = (ys >= 27 * R) & (ys < 32 * R) & (xs < 16 * R)
    sole = (ys >= 16 * R) & (ys < 20 * R) & (xs >= 8 * R) & (xs < 12 * R)
    boots[:, :, 3] *= (sides | sole)
    a = boots[:, :, 3:4]
    L1[:] = L1 * (1 - a) + boots * a
    # leggings (layer 2): body (belt) and legs
    box_paint(L2, 16, 16, 8, 12, 4, second, mask=None)
    L2[16 * R + 4 * R:(16 + 4 + 7) * R, 16 * R:40 * R, 3] = 0  # keep only the belt part of the body
    box_paint(L2, 0, 16, 4, 12, 4, main)
    # ------------- extras
    if s == 'shadow_monarch':
        horn = T.m_bone('#14101C')
        extras += [Part('head', 'horn_l', 3, -11, -1, 1, 5, 1, horn, rot=(0, 0, 20)), Part('head', 'horn_r', -4, -11, -1, 1, 5, 1, horn, rot=(0, 0, -20)),
                   Part('head', 'crest', -0.5, -10.5, -4.5, 1, 3, 1, T.m_glow(acc), glow=True),
                   Part('body', 'cape', -4.5, 0, 2.6, 9, 18, 1, T.m_cloth('#0E0A16', trim=acc)),
                   Part('right_arm', 'pauldron_r', -4.5, -3.5, -3, 5, 3, 6, main), Part('left_arm', 'pauldron_l', -0.5, -3.5, -3, 5, 3, 6, main),
                   Part('right_arm', 'spike_r', -3.5, -5.5, -0.5, 1, 2, 1, T.m_glow(acc), glow=True),
                   Part('left_arm', 'spike_l', 2.5, -5.5, -0.5, 1, 2, 1, T.m_glow(acc), glow=True)]
    elif s == 'crimson_knight':
        hair = T.m_hair('#B0101C', '#FF5A50')
        extras += [Part('head', 'crest', -1, -10.5, -4.5, 2, 2, 9, T.m_metal('#D4A841', rivets=False)),
                   Part('head', 'plume', -1.5, -9.5, 4.0, 3, 12, 2, hair, rot=(20, 0, 0)),
                   Part('body', 'cape', -4.5, 0, 2.6, 9, 16, 1, T.m_cloth('#7A0E18', trim='#D4A841')),
                   Part('right_arm', 'pauldron_r', -4.5, -3.5, -3, 5, 4, 6, main), Part('left_arm', 'pauldron_l', -0.5, -3.5, -3, 5, 4, 6, main)]
    elif s == 'high_orc':
        bone = T.m_bone('#E8E0C8')
        fur = T.m_fur('#5A4632', '#9A7C5A', '#2E2216')
        extras += [Part('head', 'horn_l', 4.5, -9, -1, 1, 4, 1, bone, rot=(0, 0, 35)), Part('head', 'horn_r', -5.5, -9, -1, 1, 4, 1, bone, rot=(0, 0, -35)),
                   Part('right_arm', 'fur_r', -4.5, -3.5, -3, 5, 3, 6, fur), Part('left_arm', 'fur_l', -0.5, -3.5, -3, 5, 3, 6, fur),
                   Part('body', 'skull', -1.5, 2, -3.2, 3, 3, 1, bone)]
    else:
        extras += [Part('head', 'visor', -4.5, -6, -5.2, 9, 2, 1, T.m_metal('#2A3442', rivets=False, trim='#4AD8FF')),
                   Part('body', 'pouch_l', 1, 7, -3.4, 3, 3, 1, T.m_cloth('#2A3442')), Part('body', 'pouch_r', -4, 7, -3.4, 3, 3, 1, T.m_cloth('#2A3442')),
                   Part('right_arm', 'pad_r', -3.5, -2.5, -2.5, 4, 3, 5, T.m_metal('#2A3442', rivets=False))]
    # pack extras into the lower half (y >= 32)
    x, y, shelf = 0, 32, 0
    for p in sorted(extras, key=lambda p: -(p.d + p.h)):
        uw, uh = 2 * (p.w + p.d), p.d + p.h
        if x + uw > 64:
            x, y, shelf = 0, y + shelf, 0
        p.u, p.v = x, y
        x += uw
        shelf = max(shelf, uh)
        box_paint(L1, p.u, p.v, p.w, p.h, p.d, p.mat)
    assert y + shelf <= 64, s
    return L1, L2, extras


def java_parts(extras):
    out = []
    for p in extras:
        rx, ry, rz = [a * np.pi / 180 for a in p.rot]
        out.append(f'        root.getChild("{p.parent}").addOrReplaceChild("{p.name}", CubeListBuilder.create().texOffs({p.u}, {p.v})'
                   f'.addBox({p.x}F, {p.y}F, {p.z}F, {p.w}F, {p.h}F, {p.d}F, new CubeDeformation({p.inflate}F)), '
                   f'PartPose.offsetAndRotation(0F, 0F, 0F, {rx:.4f}F, {ry:.4f}F, {rz:.4f}F));')
    return '\n'.join(out)


ICON_SHAPES = {
    'helmet': ["..XXXXXX..", ".XXXXXXXX.", "XXXXXXXXXX", "XXX....XXX", "XX......XX", "XX......XX"],
    'chestplate': ["XXX....XXX", "XXXXXXXXXX", "XXXXXXXXXX", ".XXXXXXXX.", ".XXXXXXXX.", ".XXXXXXXX.", ".XXXXXXXX.", ".XXXXXXXX."],
    'leggings': ["XXXXXXXXXX", "XXXXXXXXXX", "XXXX..XXXX", "XXX....XXX", "XXX....XXX", "XXX....XXX", "XXX....XXX"],
    'boots': ["XXX....XXX", "XXX....XXX", "XXX....XXX", "XXXX..XXXX", "XXXXX.XXXXX"],
}


def icon(s, piece):
    cfg = SETS[s]
    shape = ICON_SHAPES[piece]
    gh, gw = len(shape), max(len(r) for r in shape)
    px = 3
    img = T.new(32, 32)
    oy = (32 - gh * px) // 2
    ox = (32 - gw * px) // 2
    base = H(cfg['base'])
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch != 'X':
                continue
            t = 1.15 - 0.35 * (j / gh) + (0.1 if i < gw / 2 else -0.05)
            c = np.clip(base * np.array([t, t, t, 1], np.float32), 0, 1)
            T.rect(img, ox + i * px, oy + j * px, ox + (i + 1) * px, oy + (j + 1) * px, c)
    # trim and accents
    acc = H(cfg['accent'])
    for i, ch in enumerate(shape[0]):
        if ch == 'X':
            T.rect(img, ox + i * px, oy, ox + (i + 1) * px, oy + 1, np.clip(acc * 1.1, 0, 1))
    if piece == 'chestplate':
        T.rect(img, 15, oy + px, 17, oy + gh * px - 2, acc)
    if piece == 'helmet' and s in ('shadow_monarch', 'crimson_knight'):
        T.rect(img, ox + 3 * px, oy + 3 * px - 1, ox + 7 * px, oy + 3 * px + 1, H('#FF2A2A' if s == 'crimson_knight' else '#B08AFF'))
    n = T.value_noise(32, 32, 3, hash(s + piece) % 1000, 2)
    img = T.shade(img, 0.9 + n * 0.2)
    from itemmodel import outline
    return outline(img)


def render_icon(L1, L2, extras, piece):
    """Renders the real armor texture on humanoid boxes (with the extra parts) as an inventory icon."""
    import preview
    from model import Model as M
    m = M('icon', 64, R)
    m.tex_h = 64
    parts = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0), 'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}
    boxes = {'head': (-4, -8, -4, 8, 8, 8, 0, 0, False), 'body': (-4, 0, -2, 8, 12, 4, 16, 16, False),
             'right_arm': (-3, -2, -2, 4, 12, 4, 40, 16, False), 'left_arm': (-1, -2, -2, 4, 12, 4, 40, 16, True),
             'right_leg': (-2, 0, -2, 4, 12, 4, 0, 16, False), 'left_leg': (-2, 0, -2, 4, 12, 4, 0, 16, True)}
    use = {'helmet': ['head'], 'chestplate': ['body', 'right_arm', 'left_arm'], 'leggings': ['body', 'right_leg', 'left_leg'], 'boots': ['right_leg', 'left_leg']}[piece]
    texture = L2 if piece == 'leggings' else L1
    for name in use:
        px, py, pz = parts[name]
        m.bone(name, None, (px, 24 - py, pz))
        x, oy, z, w, h, d, u, v, mir = boxes[name]
        c = m.box(name, px + x, 24 - py - oy - h, pz + z, w, h, d, lambda *a: None, inflate=1.0 if piece != 'leggings' else 0.5, mirror=mir)
        c.u, c.v = u, v
        if piece == 'leggings':
            continue
        for e in extras:
            if e.parent != name:
                continue
            bn = m.bone(name + '_' + e.name, name, (px, 24 - py, pz), rot=e.rot)
            c = m.box(bn, px + e.x, 24 - py - e.y - e.h, pz + e.z, e.w, e.h, e.d, lambda *a: None, inflate=e.inflate)
            c.u, c.v = e.u, e.v
    img = preview.render(m, texture, -30, 12, 256, bg=(0, 0, 0))
    a = (img.sum(2) > 0.001).astype(np.float32)
    rgba = np.concatenate([img, a[:, :, None]], 2)
    # crop to content
    ys, xs = np.where(a > 0)
    if len(xs):
        x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
        side = max(x1 - x0, y1 - y0)
        cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
        canvas = np.zeros((side + 8, side + 8, 4))
        sub = rgba[y0:y1, x0:x1]
        oy, ox = (side + 8 - (y1 - y0)) // 2, (side + 8 - (x1 - x0)) // 2
        canvas[oy:oy + y1 - y0, ox:ox + x1 - x0] = sub
        rgba = canvas
    from itemmodel import outline
    return outline(T.resize(rgba.astype(np.float32), 32, 32))


def run(res_root, java_path):
    tex_dir = os.path.join(res_root, 'textures', 'models', 'armor')
    os.makedirs(tex_dir, exist_ok=True)
    item_dir = os.path.join(res_root, 'textures', 'item')
    model_dir = os.path.join(res_root, 'models', 'item')
    layers = []
    names = {'shadow_monarch': 'MONARCH', 'crimson_knight': 'KNIGHT', 'high_orc': 'ORC', 'hunter': 'HUNTER'}
    for s in SETS:
        L1, L2, extras = paint_set(s)
        T.save(L1, os.path.join(tex_dir, s + '_layer_1.png'))
        T.save(L2, os.path.join(tex_dir, s + '_layer_2.png'))
        layers.append((names[s], s, extras))
        for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
            item = f'{s}_{piece}'
            T.save(render_icon(L1, L2, extras, piece), os.path.join(item_dir, item + '.png'))
            import json
            with open(os.path.join(model_dir, item + '.json'), 'w') as f:
                json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'sololeveling:item/' + item}}, f)
    methods = []
    for const, s, extras in layers:
        methods.append(f'''    public static LayerDefinition {const.lower()}Layer() {{
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0F);
        PartDefinition root = mesh.getRoot();
{java_parts(extras)}
        return LayerDefinition.create(mesh, 64, 64);
    }}
''')
    src = f'''package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.item.SLArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** GENERATED by gen/armor.py — custom 3D armor models (horns, plumes, capes, pauldrons). */
public final class ArmorModels {{
    public static final ModelLayerLocation MONARCH = new ModelLayerLocation(SoloLeveling.id("armor_monarch"), "main");
    public static final ModelLayerLocation KNIGHT = new ModelLayerLocation(SoloLeveling.id("armor_knight"), "main");
    public static final ModelLayerLocation ORC = new ModelLayerLocation(SoloLeveling.id("armor_orc"), "main");
    public static final ModelLayerLocation HUNTER = new ModelLayerLocation(SoloLeveling.id("armor_hunter"), "main");
    public static final ModelLayerLocation INNER = new ModelLayerLocation(SoloLeveling.id("armor_inner"), "main");

    private ArmorModels() {{}}

{chr(10).join(methods)}
    public static LayerDefinition innerLayer() {{
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0F), 64, 64);
    }}

    public static IClientItemExtensions extensions() {{
        return new IClientItemExtensions() {{
            private HumanoidModel<LivingEntity> outer, inner;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {{
                var models = Minecraft.getInstance().getEntityModels();
                SLArmorItem.Mat m = stack.getItem() instanceof SLArmorItem a ? a.mat : SLArmorItem.Mat.HUNTER;
                if (outer == null) {{
                    ModelLayerLocation loc = switch (m) {{
                        case SHADOW_MONARCH -> MONARCH;
                        case CRIMSON_KNIGHT -> KNIGHT;
                        case HIGH_ORC -> ORC;
                        case HUNTER -> HUNTER;
                    }};
                    outer = new HumanoidModel<>(models.bakeLayer(loc));
                    inner = new HumanoidModel<>(models.bakeLayer(INNER));
                }}
                return slot == EquipmentSlot.LEGS ? inner : outer;
            }}
        }};
    }}
}}
'''
    with open(java_path, 'w') as f:
        f.write(src)
