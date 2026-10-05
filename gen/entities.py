"""All Solo Leveling creature models: monsters, bosses, shadows. Each entry: (Model, Painter, Anims)."""
import math
import numpy as np
from core import Model, rot_zyx
from paint import Painter
from entlib import *

MODELS = {}


def reg(name, M, P, A, glow_default=None):
    M.pack()
    MODELS[name] = (M, P, A)


def new(name, w=128, h=128):
    M = Model(name, w, h)
    return M, Painter(M), Anims()


def limb(M, bone, parent, pivot, frm, size, mat, rot=(0, 0, 0)):
    M.bone(bone, parent, pivot, rot)
    M.cube(bone, frm, size, mat)


# ============================================================ GOBLIN
def goblin():
    M, P, A = new('goblin', 128, 64)
    P.mats.update(skin=mat_skin((50, 100, 40), (120, 170, 70), spots=(40, 70, 30), dark_edge=0.4),
                  skin2=mat_skin((70, 120, 50), (150, 190, 90)), cloth=mat_cloth((80, 50, 30), (140, 100, 60), True),
                  armor=mat_plate((70, 56, 44), (150, 126, 100), seams=False), eye=mat_eye((255, 220, 40)),
                  blade=mat_plate((130, 130, 140), (220, 224, 232), seams=False, rivets=False), wrap=mat_cloth((40, 28, 20), (80, 56, 36)),
                  tooth=mat_flat((240, 235, 200)))
    r = humanoid(M, 'skin', 'armor', 'skin', 'skin', head=(8, 7, 7), torso=(7, 10, 4), arm=(3, 11, 3), leg=(3.5, 11, 3.5))
    y0 = r['y0']
    # ears, nose, eyes, teeth
    for sd in (-1, 1):
        M.cube('head', (sd * 4 - (0 if sd > 0 else 4.5), y0 - 5.4, -0.5), (4.5, 1.6, 1.0), 'skin2')
        M.cube('head', (sd * 8.2 - (0 if sd > 0 else 2), y0 - 6.8, -0.4), (2, 1.4, 0.8), 'skin2')
        M.cube('head', (sd * 1.3 - 0.4, y0 - 1.6, -3.9), (0.8, 1.4, 0.5), 'tooth')
    M.cube('head', (-1.0, y0 - 4.4, -5.0), (2.0, 2.6, 1.6), 'skin2')
    add_eyes(M, 'head', y0 - 5.6, -3.5, 2.0, (1.8, 1.2), 'eye')
    M.cube('body', (-3.0, y0 + 7.5, -2.5), (6.0, 5.0, 0.6), 'cloth')
    M.cube('body', (-3.0, y0 + 7.5, 1.9), (6.0, 5.0, 0.6), 'cloth')
    M.cube('body', (-3.6, y0 + 8.6, -2.2), (7.2, 1.0, 4.4), 'wrap')
    # dagger
    M.cube('right_arm', (-6.0, y0 + 10.3, -9.5), (1.0, 1.2, 8.5), 'blade')
    M.cube('right_arm', (-6.3, y0 + 9.8, -2.0), (1.6, 2.2, 1.0), 'wrap')
    A.humanoid(attack_arm='right_arm')
    reg('goblin', M, P, A)


# ============================================================ ORC
def orc():
    M, P, A = new('orc', 128, 128)
    P.mats.update(skin=mat_skin((60, 95, 55), (120, 150, 90), spots=(40, 60, 40), dark_edge=0.4),
                  armor=mat_plate((50, 48, 54), (140, 134, 144), trim=(150, 110, 50)), fur=mat_fur((60, 40, 28), (120, 88, 60)),
                  eye=mat_eye((255, 60, 40)), tusk=mat_flat((235, 225, 190)), metal=mat_plate((90, 90, 100), (200, 204, 214), seams=False),
                  wood=mat_cloth((60, 40, 24), (110, 76, 44)), pants=mat_cloth((50, 34, 28), (100, 72, 56)))
    r = humanoid(M, 'skin', 'armor', 'skin', 'pants', head=(9, 8, 9), torso=(11, 12, 6), arm=(5, 12, 5), leg=(5, 12, 5), gap=0.3)
    y0 = r['y0']
    add_eyes(M, 'head', y0 - 5.6, -4.5, 2.4, (2.0, 1.2), 'eye')
    for sd in (-1, 1):
        M.cube('head', (sd * 2.8 - 0.6, y0 - 3.6, -4.8), (1.2, 3.2, 1.0), 'tusk')
        M.cube('head', (sd * 4.6 - (0 if sd > 0 else 1.5), y0 - 6, -0.5), (1.5, 3, 1), 'skin')
        # pauldrons
        M.cube(f"{'right' if sd < 0 else 'left'}_arm", (sd * 8.4 - 3.5, y0 - 1.2, -3.5), (7, 3.2, 7), 'armor')
        M.cube(f"{'right' if sd < 0 else 'left'}_arm", (sd * 8.4 - 0.5, y0 - 3.4, -0.5), (1, 2.4, 1), 'tusk')
    M.cube('body', (-5.8, y0 + 8, -3.4), (11.6, 2.2, 6.8), 'fur')
    M.cube('head', (-4.7, y0 - 8.6, -4.7), (9.4, 1.6, 9.4), 'fur')
    # battle axe in right hand
    M.cube('right_arm', (-9.6, y0 + 10, -3.5), (1.6, 1.6, 12), 'wood')
    M.cube('right_arm', (-10.0, y0 + 8.4, -10.5), (2.4, 5.4, 4.5), 'metal')
    M.cube('right_arm', (-10.0, y0 + 9.4, -14), (1.6, 3.4, 3.5), 'metal')
    A.humanoid(attack_arm='right_arm', arm_amp=0.8, leg_amp=1.1)
    reg('orc', M, P, A)


# ============================================================ STONE SOLDIER (Statue of God minion)
def stone_soldier(name='stone_soldier', big=False):
    M, P, A = new(name, 128, 128)
    P.mats.update(stone=mat_stone((90, 90, 100), (170, 170, 180), rune=(90, 190, 255)),
                  stone2=mat_stone((70, 70, 80), (140, 140, 150)), eye=mat_eye((120, 220, 255)),
                  metal=mat_plate((70, 74, 90), (180, 190, 215), seams=False), glow=mat_glow((110, 210, 255)))
    r = humanoid(M, 'stone', 'stone', 'stone2', 'stone2', head=(8, 9, 8), torso=(10, 12, 5), arm=(4.5, 13, 4.5), leg=(5, 12, 5), gap=0.3)
    y0 = r['y0']
    add_eyes(M, 'head', y0 - 5.4, -4.0, 2.0, (2, 1.4), 'eye')
    M.cube('head', (-4.5, y0 - 9.6, -4.5), (9, 1.4, 9), 'stone2')
    for sd in (-1, 1):
        M.cube(f"{'right' if sd < 0 else 'left'}_arm", (sd * 7.9 - 3, y0 - 1.0, -3), (6, 2.6, 6), 'stone2')
    M.cube('body', (-1.5, y0 + 3, -3.2), (3, 3, 0.8), 'glow')
    # sword + shield
    M.cube('right_arm', (-7.9 - 0.6, y0 + 10.5, -3.0), (1.2, 1.4, 6), 'metal')
    M.cube('right_arm', (-7.9 - 0.8, y0 + 9.6, -3.6), (1.6, 3.2, 1), 'stone2')
    M.cube('right_arm', (-7.9 - 0.5, y0 + 10.6, -23), (1.0, 1.2, 20), 'metal')
    M.cube('left_arm', (7.9 - 1, y0 + 5, -6.5), (2, 9, 7), 'metal')
    A.humanoid(attack_arm='right_arm', arm_amp=0.7, leg_amp=1.0)
    reg(name, M, P, A)


def statue_of_god():
    M, P, A = new('statue_of_god', 256, 256)
    P.mats.update(stone=mat_stone((150, 146, 138), (230, 226, 214), rune=(255, 210, 110)),
                  stone2=mat_stone((120, 116, 110), (200, 196, 186)), gold=mat_plate((150, 100, 20), (255, 225, 110), seams=False),
                  eye=mat_eye((255, 220, 100)), glow=mat_glow((255, 210, 100)), robe=mat_cloth((190, 186, 176), (240, 236, 226)),
                  sword=mat_plate((190, 196, 210), (250, 252, 255), seams=False, rivets=False))
    r = humanoid(M, 'stone', 'stone', 'stone2', 'robe', head=(8, 9, 8), torso=(11, 13, 6), arm=(5, 14, 5), leg=(6, 13, 6), gap=0.4)
    y0 = r['y0']
    add_eyes(M, 'head', y0 - 5.4, -4.0, 2.0, (2.2, 1.4), 'eye')
    # halo ring
    for i in range(12):
        a = i / 12 * math.tau
        x, y = math.cos(a) * 9.5, math.sin(a) * 9.5
        M.bone_w(f'halo{i}', 'head', (x, y0 - 14 + y, 3.5), rot_zyx(0, 0, a))
        M.cube_l(f'halo{i}', (-2.0, -0.6, -0.6), (4.0, 1.2, 1.2), 'glow')
    # crown rays
    for k, ang in enumerate((-30, 0, 30)):
        M.bone(f'ray{k}', 'head', (0, y0 - 9, 0), (0, 0, ang))
        M.cube(f'ray{k}', (-0.8, y0 - 15, -0.8), (1.6, 6, 1.6), 'gold')
    M.cube('body', (-2.0, y0 + 3, -3.5), (4, 4, 1.0), 'glow')
    M.cube('body', (-6, y0 + 11, -3.5), (12, 2.2, 7), 'gold')
    M.cube('body', (-5.5, y0 + 11, 3.2), (11, 14, 0.8), 'robe')
    for sd in (-1, 1):
        M.cube(f"{'right' if sd < 0 else 'left'}_arm", (sd * 8.9 - 3.5, y0 - 1.2, -3.5), (7, 3, 7), 'gold')
    # great sword held in front
    M.cube('right_arm', (-8.9 - 0.7, y0 + 12.4, -3.4), (1.4, 1.6, 7), 'gold')
    M.cube('right_arm', (-8.9 - 2.4, y0 + 12.0, -9.0), (4.8, 2.0, 1.6), 'gold')
    M.cube('right_arm', (-8.9 - 1.2, y0 + 12.3, -40), (2.4, 1.4, 31), 'sword')
    A.humanoid(attack_arm='right_arm', arm_amp=0.5, leg_amp=0.8)
    reg('statue_of_god', M, P, A)


# ============================================================ ICE ELF
def ice_elf(name='ice_elf', king=False):
    M, P, A = new(name, 128, 128)
    ice = ((120, 170, 225), (225, 245, 255))
    P.mats.update(skin=mat_skin((170, 200, 230), (235, 245, 255), dark_edge=0.25), hair=mat_fur((190, 210, 235), (255, 255, 255)),
                  armor=mat_plate((50, 80, 140), (170, 210, 245), trim=(230, 245, 255), glow=(120, 220, 255)),
                  cloth=mat_cloth((40, 70, 130), (120, 170, 230), True), eye=mat_eye((120, 240, 255)),
                  crystal=mat_glow((110, 210, 255)), blade=mat_plate((150, 200, 240), (245, 252, 255), seams=False, rivets=False),
                  leather=mat_cloth((30, 40, 70), (70, 90, 130)))
    r = humanoid(M, 'skin', 'armor', 'skin', 'leather', head=(8, 8, 8), torso=(8, 12, 4), arm=(3.5, 12, 3.5), leg=(4, 12, 4), gap=0.2)
    y0 = r['y0']
    add_eyes(M, 'head', y0 - 4.4, -4.0, 2.0, (2, 1.2), 'eye')
    M.cube('head', (-4.4, y0 - 8.4, -4.4), (8.8, 2.4, 8.8), 'hair')
    M.cube('head', (-4.4, y0 - 8.4, 3.0), (8.8, 11, 1.6), 'hair')
    for sd in (-1, 1):
        M.cube('head', (sd * 4.4 - (0 if sd > 0 else 0.8), y0 - 8, -4.4), (0.8, 8, 2.0), 'hair')
        M.cube('head', (sd * 4.0 - (0 if sd > 0 else 4.5), y0 - 5.2, -0.5), (4.5, 1.2, 1.0), 'skin')
        M.cube(f"{'right' if sd < 0 else 'left'}_arm", (sd * 6.3 - 2.5, y0 - 0.6, -2.5), (5, 2.6, 5), 'armor')
    # crown of ice shards
    for k, (x, hh, ang) in enumerate(((0, 8, 0), (-2.8, 6, -14), (2.8, 6, 14), (-4.2, 4, -28), (4.2, 4, 28))):
        M.bone(f'shard{k}', 'head', (x, y0 - 8.4, 0), (0, 0, ang))
        M.cube(f'shard{k}', (x - 0.9, y0 - 8.4 - hh * (1.4 if king else 1), -0.9), (1.8, hh * (1.4 if king else 1), 1.8), 'crystal')
    M.cube('body', (-3.6, y0 + 3.2, -2.5), (7.2, 5, 0.7), 'crystal')
    # cape
    M.bone('cape', None, (0, y0 + 1, 2.2))
    M.cube('cape', (-4.6, y0 + 1, 2.2), (9.2, 26, 0.8), 'cloth')
    # ice dagger
    M.cube('right_arm', (-6.2 - 0.5, y0 + 10.5, -8), (1.0, 1.4, 7.5), 'blade')
    M.cube('right_arm', (-6.2 - 1.4, y0 + 10.2, -1.6), (2.8, 1.8, 0.8), 'armor')
    A.humanoid(attack_arm='right_arm', leg_amp=1.2)
    A.add('cape', IDLE, 0.06, 0.0, 0)
    A.add('cape', WALK, -0.35, 0.0)
    reg(name, M, P, A)


# ============================================================ KNIGHTS (Igris / Demon Knight / Shadow Igris)
def knight(name, pal, horns=False, plume=True, eye=(255, 60, 50), greatsword=True, cape=True, shadow=False):
    M, P, A = new(name, 128, 128)
    plate_lo, plate_hi, trim, cl_lo, cl_hi, glowc = pal
    P.mats.update(plate=mat_plate(plate_lo, plate_hi, trim=trim, glow=glowc if shadow else None),
                  plate2=mat_plate(tuple(int(v * 0.7) for v in plate_lo), tuple(int(v * 0.75) for v in plate_hi), seams=False),
                  cloth=mat_cloth(cl_lo, cl_hi, True), eye=mat_eye(eye), glow=mat_glow(glowc),
                  blade=mat_plate((90, 90, 100), (230, 232, 240), seams=False, rivets=False) if not shadow else mat_plate((40, 20, 80), (200, 150, 255), seams=False, rivets=False),
                  dark=mat_flat((14, 12, 18)), horn=mat_flat((230, 220, 200)))
    r = humanoid(M, 'plate', 'plate', 'plate2', 'plate2', head=(8, 9, 8), torso=(9, 12, 5), arm=(4.5, 12, 4.5), leg=(4.5, 12, 4.5), gap=0.3)
    y0 = r['y0']
    M.cube('head', (-3.6, y0 - 5.4, -4.4), (7.2, 1.8, 0.6), 'dark')
    add_eyes(M, 'head', y0 - 5.0, -4.6, 1.8, (1.8, 1.0), 'eye', 0.3)
    M.cube('head', (-0.5, y0 - 9.6, -4.4), (1, 9, 8.8), 'plate2')
    if plume:
        for k in range(6):
            M.cube('head', (-0.9, y0 - 11.6 - k * 0.3, -4 + k * 1.6), (1.8, 2.2, 1.9), 'cloth')
    if horns:
        for sd in (-1, 1):
            for k in range(4):
                M.cube('head', (sd * (4.2 + k * 1.0) - 0.7, y0 - 8 - k * 1.8, -0.7), (1.4, 2.0, 1.4), 'horn')
    for sd in (-1, 1):
        sdn = 'right' if sd < 0 else 'left'
        M.cube(f'{sdn}_arm', (sd * 7.2 - 3.5, y0 - 1.6, -3.5), (7, 3.2, 7), 'plate')
        M.cube(f'{sdn}_arm', (sd * 7.2 - 0.6, y0 - 3.6, -0.6), (1.2, 2.6, 1.2), 'plate2')
    M.cube('body', (-1.6, y0 + 3.2, -3.0), (3.2, 3.2, 0.8), 'glow')
    M.cube('body', (-5, y0 + 10.5, -3), (10, 2, 6), 'plate2')
    if cape:
        M.bone('cape', None, (0, y0 + 0.5, 2.8))
        M.cube('cape', (-5.4, y0 + 0.5, 2.8), (10.8, 28, 0.8), 'cloth')
        A.add('cape', IDLE, 0.05, 0.0, 0)
        A.add('cape', WALK, -0.4, 0.0)
    if greatsword:
        M.cube('right_arm', (-7.5 - 0.7, y0 + 11.2, -3), (1.4, 1.6, 7), 'dark')
        M.cube('right_arm', (-7.5 - 3.2, y0 + 10.8, -9), (6.4, 2.2, 1.8), 'plate2')
        M.cube('right_arm', (-7.5 - 1.2, y0 + 11.0, -37), (2.4, 1.6, 28), 'blade')
        M.cube('right_arm', (-7.5 - 0.5, y0 + 11.0, -42), (1.0, 1.4, 5), 'blade')
    A.humanoid(attack_arm='right_arm', arm_amp=0.8, leg_amp=1.1)
    reg(name, M, P, A)


# ============================================================ GIANT ANT / BERU
def giant_ant(name='giant_ant', pal=((50, 20, 14), (160, 70, 40)), eye=(255, 80, 40), glowc=(255, 120, 60)):
    M, P, A = new(name, 128, 128)
    P.mats.update(shell=mat_chitin(pal[0], pal[1], glow=None), shell2=mat_chitin(tuple(int(v * 0.7) for v in pal[0]), tuple(int(v * 0.8) for v in pal[1])),
                  eye=mat_eye(eye), horn=mat_flat((220, 205, 175)), glow=mat_glow(glowc))
    y = 14
    # thorax, abdomen, head
    M.bone('body', None, (0, y, 0))
    M.cube('body', (-4, y - 4, -6), (8, 8, 12), 'shell')
    M.cube('body', (-3, y - 6, -5), (6, 2, 9), 'shell2')
    M.bone('abdomen', 'body', (0, y - 1, 6))
    M.cube('abdomen', (-5, y - 5, 5), (10, 10, 16), 'shell')
    M.cube('abdomen', (-3, y + 3, 8), (6, 2, 10), 'shell2')
    M.bone('head', None, (0, y - 2, -6))
    M.cube('head', (-4, y - 6, -13), (8, 8, 8), 'shell')
    add_eyes(M, 'head', y - 5, -13, 3.2, (2.0, 2.0), 'eye', 0.4)
    for sd in (-1, 1):
        M.bone(f'mand_{"r" if sd < 0 else "l"}', 'head', (sd * 2, y + 0, -13), (0, sd * -8, 0))
        M.cube(f'mand_{"r" if sd < 0 else "l"}', (sd * 2 - 0.9, y - 1.5, -19), (1.8, 1.8, 6.5), 'horn')
        M.cube(f'mand_{"r" if sd < 0 else "l"}', (sd * 2 - 0.9 - sd * 1.4, y - 1.5, -19.5), (1.8, 1.8, 2.5), 'horn')
        M.bone(f'ant_{"r" if sd < 0 else "l"}', 'head', (sd * 2, y - 6, -11), (-30, sd * -20, 0))
        M.cube(f'ant_{"r" if sd < 0 else "l"}', (sd * 2 - 0.4, y - 6.5, -18), (0.8, 0.8, 8), 'shell2')
    # six legs
    idx = 0
    for k, zz in enumerate((-4, 0, 4)):
        for sd in (-1, 1):
            nm = f'leg_{"r" if sd < 0 else "l"}{k}'
            M.bone(nm, None, (sd * 4, y + 1, zz), (0, 0, sd * 22))
            M.cube(nm, (sd * 4 - (0 if sd > 0 else 8.5), y + 0.2, zz - 0.6), (8.5, 1.4, 1.2), 'shell2')
            M.bone(nm + 'b', nm, (sd * 12, y + 1, zz), (0, 0, sd * 55))
            M.cube(nm + 'b', (sd * 12 - 0.7, y + 0.5, zz - 0.5), (1.4, 10, 1.0), 'shell2')
            A.add(nm, WALKY, 0.5, (k % 2) * math.pi + (0 if sd > 0 else math.pi))
            A.add(nm, LIFTZ, 0.35, (k % 2) * math.pi + (0 if sd > 0 else math.pi), 1 if sd > 0 else -1)
    A.add('head', LOOK)
    A.add('abdomen', IDLE, 0.05, 0.0, 0)
    reg(name, M, P, A)


def beru(name='beru', shadow=False):
    M, P, A = new(name, 256, 256)
    lo, hi = ((20, 6, 40), (110, 60, 190)) if shadow else ((60, 14, 12), (190, 70, 50))
    glowc = (190, 90, 255) if shadow else (255, 90, 50)
    P.mats.update(shell=mat_chitin(lo, hi), shell2=mat_chitin(tuple(int(v * 0.65) for v in lo), tuple(int(v * 0.75) for v in hi)),
                  eye=mat_eye(glowc), horn=mat_chitin((200, 190, 170), (255, 250, 235)) if not shadow else mat_chitin((60, 30, 110), (190, 150, 255)),
                  glow=mat_glow(glowc), wing=mat_chitin(lo, hi))
    r = humanoid(M, 'shell', 'shell', 'shell2', 'shell2', head=(8, 10, 9), torso=(11, 13, 6), arm=(4.5, 13, 4.5), leg=(5, 13, 5), gap=0.3)
    y0 = r['y0']
    # ant head details
    add_eyes(M, 'head', y0 - 6.5, -4.5, 3.0, (2.2, 2.4), 'eye')
    M.cube('head', (-1.8, y0 - 10.4, -4.8), (3.6, 2.2, 1), 'shell2')
    for sd in (-1, 1):
        nm = f'mand_{"r" if sd < 0 else "l"}'
        M.bone(nm, 'head', (sd * 2.2, y0 - 1.5, -4.5), (0, sd * -6, 0))
        M.cube(nm, (sd * 2.2 - 0.9, y0 - 2.4, -12), (1.8, 1.8, 7.5), 'horn')
        M.cube(nm, (sd * 2.2 - 0.9 - sd * 1.5, y0 - 2.4, -12.5), (1.8, 1.8, 3), 'horn')
        an = f'ant_{"r" if sd < 0 else "l"}'
        M.bone(an, 'head', (sd * 2, y0 - 10, 0), (-25, sd * -18, 0))
        M.cube(an, (sd * 2 - 0.4, y0 - 10.5, -9), (0.8, 0.8, 9), 'horn')
    # shoulder spikes + chest plates
    for sd in (-1, 1):
        sdn = 'right' if sd < 0 else 'left'
        for k in range(3):
            M.cube(f'{sdn}_arm', (sd * 8.2 - 0.9 + sd * k * 0.4, y0 - 1 - k * 2.2, -0.9), (1.8, 3.2, 1.8), 'horn')
        M.cube(f'{sdn}_arm', (sd * 8.0 - 3.2, y0 - 1.2, -3.2), (6.4, 3, 6.4), 'shell')
        # arm blade
        M.cube(f'{sdn}_arm', (sd * 8.0 - 0.5, y0 + 11, -9), (1.0, 1.4, 9), 'horn')
    M.cube('body', (-4.6, y0 + 1, -3.6), (9.2, 4, 0.8), 'shell2')
    M.cube('body', (-1.4, y0 + 5, -3.6), (2.8, 2.8, 0.8), 'glow')
    # tail
    M.bone('tail', None, (0, y0 + 11, 3))
    M.cube('tail', (-1.4, y0 + 9, 3), (2.8, 2.8, 14), 'shell2')
    M.cube('tail', (-0.8, y0 + 9.4, 16), (1.6, 1.6, 6), 'horn')
    # wings
    for sd in (-1, 1):
        nm = f'wing_{"r" if sd < 0 else "l"}'
        M.bone(nm, None, (sd * 2, y0 + 2, 3.3), (0, 0, sd * 40))
        M.cube(nm, (sd * 2 - (0 if sd > 0 else 14), y0 + 1, 3.0), (14, 0.8, 5), 'wing')
        M.cube(nm, (sd * 2 + sd * 2 - (0 if sd > 0 else 12), y0 + 1, 8), (12, 0.8, 4), 'wing')
        A.add(nm, FLAP, 0.12 * sd, 0.0)
    A.humanoid(attack_arm='right_arm', two_hand=False, arm_amp=0.9)
    A.add('tail', WAVE, 0.4, 0.6, 0)
    reg(name, M, P, A)


# ============================================================ KASAKA (serpent boss)
def kasaka():
    M, P, A = new('kasaka', 256, 256)
    P.mats.update(scale=mat_scale((20, 70, 40), (110, 200, 110)), scale2=mat_scale((60, 100, 50), (200, 230, 140)),
                  eye=mat_eye((255, 240, 60)), fang=mat_flat((240, 245, 220)), glow=mat_glow((130, 255, 60)),
                  hood=mat_scale((16, 56, 36), (80, 170, 100)), spine=mat_chitin((20, 60, 40), (150, 220, 130)))
    n = 12
    zs = []
    for i in range(n):
        w = 10 - i * 0.55
        w = max(2.4, w)
        z = 22 - i * 6.2
        M.bone(f'seg{i}', None, (0, 24 - w / 2, z + 3))
        M.cube(f'seg{i}', (-w / 2, 24 - w, z), (w, w, 6.4), 'scale' if i % 2 == 0 else 'scale2')
        M.cube(f'seg{i}', (-w / 2 + 0.2, 24 - w - 1.4, z + 1), (w - 0.4, 1.4, 4), 'spine')
        A.add(f'seg{i}', WAVE, 0.22, i * 0.55, 0)
    # raised front: head rises on neck segments
    for i in range(4):
        M.bone(f'neck{i}', None, (0, 22 - i * 5, -4 - i * 0.5))
        M.cube(f'neck{i}', (-4 + i * 0.4, 18 - i * 5, -8 - i * 0.5), (8 - i * 0.8, 6, 6), 'scale')
        A.add(f'neck{i}', IDLE, 0.05, i * 0.4, 0)
    M.bone('head', None, (0, 4, -9))
    M.cube('head', (-4.5, 0, -20), (9, 6, 11), 'scale')
    M.cube('head', (-4, 5.5, -19), (8, 1.4, 10), 'scale2')
    add_eyes(M, 'head', 1.2, -19.5, 3.2, (2, 2), 'eye')
    for sd in (-1, 1):
        M.cube('head', (sd * 1.8 - 0.4, 5.6, -19.5), (0.8, 5, 0.8), 'fang')
        # hood
        M.bone(f'hood_{"r" if sd < 0 else "l"}', 'head', (sd * 4, 3, -9), (0, sd * 25, 0))
        M.cube(f'hood_{"r" if sd < 0 else "l"}', (sd * 4 - (0 if sd > 0 else 8), -4, -10), (8), 'hood') if False else None
        M.cube(f'hood_{"r" if sd < 0 else "l"}', (sd * 4 - (0 if sd > 0 else 8), -4, -9.6), (8, 12, 0.8), 'hood')
    M.cube('head', (-0.8, -5, -12), (1.6, 5, 6), 'spine')
    A.add('head', LOOK)
    reg('kasaka', M, P, A)


# ============================================================ QUADRUPEDS
def quad_base(M, body_mat, leg_mat, bl=18, bh=10, bw=10, leg=10, y_body=None):
    yb = 24 - leg - bh
    M.bone('body', None, (0, yb + bh / 2, 0))
    M.cube('body', (-bw / 2, yb, -bl / 2), (bw, bh, bl), body_mat)
    for fb, zz in (('f', -bl / 2 + 2.5), ('b', bl / 2 - 2.5)):
        for sd, side in ((-1, 'r'), (1, 'l')):
            nm = f'leg_{fb}{side}'
            px = sd * (bw / 2 - 2)
            M.bone(nm, None, (px, yb + bh - 1, zz))
            M.cube(nm, (px - 2, yb + bh - 1, zz - 2), (4, leg + 1, 4), leg_mat)
    return yb


def wolf_like(name, pal, eye, glowc, scale_cfg, heads=1, shadow=False, mane=False, fur_mat=None):
    M, P, A = new(name, 128, 128)
    lo, hi = pal
    P.mats.update(fur=mat_fur(lo, hi), fur2=mat_fur(tuple(int(v * 0.6) for v in lo), tuple(int(v * 0.75) for v in hi)), eye=mat_eye(eye),
                  fang=mat_flat((240, 235, 220)), glow=mat_glow(glowc), claw=mat_flat((200, 190, 180)), nose=mat_flat((14, 12, 16)))
    bl, bh, bw, lg = scale_cfg
    yb = quad_base(M, 'fur', 'fur2', bl, bh, bw, lg)
    hz = -bl / 2
    heads_x = [0] if heads == 1 else [-5, 0, 5]
    for i, hx in enumerate(heads_x):
        hn = 'head' if i == (0 if heads == 1 else 1) else f'head{i}'
        hy = yb + 1
        M.bone(hn, None, (hx, hy + 2, hz))
        M.cube(hn, (hx - 3.6, hy - 2, hz - 7), (7.2, 7.2, 7.5), 'fur')
        M.cube(hn, (hx - 2.4, hy + 1.2, hz - 12), (4.8, 3.4, 5.5), 'fur2')
        M.cube(hn, (hx - 1.0, hy + 0.8, hz - 12.4), (2.0, 1.4, 0.6), 'nose')
        M.cube(hn, (hx - 2.0, hy + 4.2, hz - 11.5), (0.8, 1.6, 0.8), 'fang')
        M.cube(hn, (hx + 1.2, hy + 4.2, hz - 11.5), (0.8, 1.6, 0.8), 'fang')
        add_eyes(M, hn, hy + 0.2, hz - 7.0, 2.2, (1.6, 1.0), 'eye', 0.3)
        for sd in (-1, 1):
            M.cube(hn, (hx + sd * 2.6 - 1.0, hy - 4.2, hz - 3), (2, 2.8, 1.2), 'fur2')
        A.add(hn, LOOK)
    if mane:
        M.cube('body', (-bw / 2 - 1, 24 - lg - bh - 2, -bl / 2 - 1), (bw + 2, bh * 0.7, 8), 'fur2')
    M.bone('tail', None, (0, yb + 2, bl / 2))
    M.cube('tail', (-1.4, yb + 1, bl / 2), (2.8, 2.8, 11), 'fur2')
    for k in range(5):
        M.cube('body', (-0.8, yb - 1.2, -bl / 2 + 2 + k * (bl - 4) / 5), (1.6, 1.6, 2.4), 'glow') if shadow else None
    A.quad(amp=1.0, head=None)
    A.add('tail', WAVE, 0.35, 0.0, 0)
    reg(name, M, P, A)


def tank_bear():
    M, P, A = new('shadow_tank', 256, 256)
    P.mats.update(fur=mat_fur((16, 10, 30), (84, 52, 140)), fur2=mat_fur((10, 6, 20), (56, 34, 100)), eye=mat_eye((200, 110, 255)),
                  claw=mat_flat((200, 170, 255)), glow=mat_glow((190, 100, 255)), nose=mat_flat((8, 6, 14)))
    yb = quad_base(M, 'fur', 'fur2', 24, 16, 18, 14)
    # hump / shoulders
    M.cube('body', (-8, 24 - 14 - 16 - 4, -12), (16, 6, 12), 'fur2')
    M.bone('head', None, (0, yb + 3, -12))
    M.cube('head', (-5.5, yb - 3, -22), (11, 10, 11), 'fur')
    M.cube('head', (-3.5, yb + 2, -27), (7, 5, 6), 'fur2')
    M.cube('head', (-1.2, yb + 2, -27.5), (2.4, 1.8, 0.6), 'nose')
    add_eyes(M, 'head', yb - 0.6, -21.8, 3.2, (2, 1.4), 'eye', 0.3)
    for sd in (-1, 1):
        M.cube('head', (sd * 4.4 - 1.5, yb - 5.4, -17), (3, 3, 2), 'fur2')
        for k in range(3):
            M.cube('head', (sd * 2 - 0.4, yb + 6.8, -26.5 + k * 1.2), (0.8, 1.2, 0.8), 'claw')
    for fb in ('f', 'b'):
        for sd, s in ((-1, 'r'), (1, 'l')):
            pass
    A.quad(amp=0.8, head='head')
    reg('shadow_tank', M, P, A)


# ============================================================ DRAGON (Kamish)
def kamish():
    M, P, A = new('kamish', 256, 256)
    P.mats.update(scale=mat_scale((90, 14, 10), (230, 90, 40), edge=(40, 4, 4)), scale2=mat_scale((50, 8, 8), (150, 50, 30), edge=(30, 4, 4)),
                  belly=mat_scale((160, 90, 30), (255, 200, 110)), eye=mat_eye((255, 230, 60)), horn=mat_flat((236, 220, 190)),
                  membrane=mat_cloth((100, 16, 14), (220, 70, 40), True), glow=mat_glow((255, 150, 40)), claw=mat_flat((235, 225, 200)))
    yb = 24 - 14 - 12
    M.bone('body', None, (0, yb + 6, 0))
    M.cube('body', (-8, yb, -12), (16, 12, 28), 'scale')
    M.cube('body', (-6, yb + 12, -10), (12, 2, 24), 'belly')
    for k in range(7):
        M.cube('body', (-1.2, yb - 2.5, -11 + k * 4), (2.4, 3, 2.4), 'horn')
    # legs
    for fb, zz in (('f', -8), ('b', 11)):
        for sd, s in ((-1, 'r'), (1, 'l')):
            nm = f'leg_{fb}{s}'
            px = sd * 8
            M.bone(nm, None, (px, yb + 9, zz))
            M.cube(nm, (px - 3.2, yb + 9, zz - 3), (6.4, 15, 6), 'scale2')
            M.cube(nm, (px - 3, 24 - 2, zz - 6.5), (6, 2, 4), 'claw')
    # neck + head
    for i in range(4):
        M.bone(f'neck{i}', None, (0, yb + 4 - i * 4, -12 - i * 3))
        M.cube(f'neck{i}', (-4.2 + i * 0.3, yb - i * 4, -15 - i * 3), (8.4 - i * 0.6, 8, 6), 'scale')
        A.add(f'neck{i}', IDLE, 0.04, i * 0.5, 0)
    hy = yb - 14
    M.bone('head', None, (0, hy + 4, -24))
    M.cube('head', (-4.8, hy, -33), (9.6, 8, 10), 'scale')
    M.cube('head', (-3.4, hy + 3, -42), (6.8, 5, 9.5), 'scale2')
    M.cube('head', (-3.0, hy + 8, -41), (6, 1.4, 9), 'belly')
    add_eyes(M, 'head', hy + 1.4, -32.6, 3.8, (2, 2), 'eye', 0.3)
    for sd in (-1, 1):
        for k in range(4):
            M.cube('head', (sd * (2.6 + k * 0.6) - 0.7, hy - 1 - k * 1.8, -28 + k * 2.4), (1.4, 2.4, 1.4 + k * 0.4), 'horn')
        M.cube('head', (sd * 2.4 - 0.4, hy + 7.6, -40.5), (0.8, 2.2, 0.8), 'claw')
    # wings
    for sd in (-1, 1):
        nm = f'wing_{"r" if sd < 0 else "l"}'
        M.bone(nm, None, (sd * 7, yb + 1, -4), (0, 0, sd * -10))
        M.cube(nm, (sd * 7 - (0 if sd > 0 else 4), yb - 1, -5), (4, 3, 4), 'scale2')
        M.cube(nm, (sd * 7 + sd * 3 - (0 if sd > 0 else 24), yb - 1, -4.5), (24, 1.8, 3), 'scale2')
        M.cube(nm, (sd * 7 + sd * 3 - (0 if sd > 0 else 24), yb, -3.5), (24, 0.8, 22), 'membrane')
        M.cube(nm, (sd * 7 + sd * 26 - 0.8, yb - 2, 12), (1.6, 3, 9), 'horn') if False else None
        A.add(nm, FLAP, 0.1 * sd, 0.0)
    # tail
    for i in range(7):
        w = max(2.4, 9 - i * 1.0)
        M.bone(f'tail{i}', None, (0, yb + 6, 16 + i * 5))
        M.cube(f'tail{i}', (-w / 2, yb + 6 - w / 2, 15 + i * 5), (w, w, 5.4), 'scale')
        A.add(f'tail{i}', WAVE, 0.18, i * 0.6, 0)
    A.quad(front=('leg_fr', 'leg_fl'), back=('leg_br', 'leg_bl'), amp=0.8, head='head')
    reg('kamish', M, P, A)


# ============================================================ SHADOW SOLDIER (humanoid)
def shadow_soldier(name='shadow_soldier', accent=(170, 90, 255), helm=False):
    M, P, A = new(name, 128, 128)
    P.mats.update(body=mat_skin((6, 4, 14), (44, 28, 84), dark_edge=0.5), arm=mat_skin((6, 4, 14), (38, 24, 74), dark_edge=0.5),
                  eye=mat_eye(accent), wisp=mat_glow(accent), plate=mat_plate((10, 8, 22), (70, 48, 130), trim=accent, glow=accent),
                  cloth=mat_cloth((6, 4, 14), (30, 20, 60), True))
    r = humanoid(M, 'body', 'body', 'arm', 'body', head=(8, 8, 8), torso=(8, 12, 4), arm=(4, 12, 4), leg=(4, 12, 4), gap=0.1)
    y0 = r['y0']
    add_eyes(M, 'head', y0 - 4.6, -4.0, 2.0, (2.0, 1.2), 'eye', 0.4)
    # wisps rising from shoulders/head
    for k, (x, hh) in enumerate(((-2.5, 5), (0, 7), (2.5, 5))):
        M.cube('head', (x - 0.6, y0 - 8 - hh, -0.6), (1.2, hh, 1.2), 'wisp')
    for sd in (-1, 1):
        sdn = 'right' if sd < 0 else 'left'
        M.cube(f'{sdn}_arm', (sd * 6.1 - 2.4, y0 - 0.8, -2.4), (4.8, 2.8, 4.8), 'plate')
        for k in range(3):
            M.cube(f'{sdn}_arm', (sd * 6.1 - 0.5, y0 - 3.2 - k * 0.0, -2 + k * 1.6), (1.0, 2.4, 1.0), 'wisp')
    M.cube('body', (-4.3, y0 + 0.5, -2.3), (8.6, 6, 4.6), 'plate')
    M.bone('cape', None, (0, y0 + 1, 2.2))
    M.cube('cape', (-4.4, y0 + 1, 2.2), (8.8, 20, 0.6), 'cloth')
    A.add('cape', WALK, -0.3, 0.0)
    A.humanoid(attack_arm='right_arm')
    reg(name, M, P, A)


def build_all():
    goblin(); orc(); stone_soldier(); stone_soldier('stone_guard_big'); statue_of_god()
    ice_elf('ice_elf'); ice_elf('baruka', king=True)
    knight('igris', ((60, 8, 16), (190, 36, 52), (210, 150, 60), (110, 14, 22), (200, 40, 50), (255, 90, 70)), plume=True, horns=False)
    knight('demon_knight', ((22, 14, 26), (88, 56, 100), (200, 60, 70), (50, 10, 24), (140, 30, 60), (255, 50, 90)), horns=True, plume=False, eye=(255, 40, 80))
    knight('shadow_igris', ((8, 6, 20), (70, 44, 140), (190, 130, 255), (16, 10, 40), (80, 50, 160), (190, 120, 255)), plume=True, eye=(210, 140, 255), shadow=True)
    giant_ant()
    beru('ant_king'); beru('shadow_beru', shadow=True)
    kasaka()
    wolf_like('cerberus', ((30, 14, 14), (110, 44, 36)), (255, 90, 30), (255, 120, 40), (30, 18, 14, 16), heads=3, mane=True)
    wolf_like('shadow_wolf', ((8, 6, 20), (66, 42, 128)), (200, 120, 255), (190, 110, 255), (20, 10, 8, 10), shadow=True)
    tank_bear()
    kamish()
    shadow_soldier('shadow_soldier')
    shadow_soldier('shadow_mage', accent=(90, 180, 255))
    return MODELS


if __name__ == '__main__':
    import os
    from PIL import Image
    from core import render
    os.makedirs('/tmp/prev', exist_ok=True)
    ms = build_all()
    names = list(ms)
    S = 300
    cols = 6
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGB', (S * cols, S * rows))
    for i, n in enumerate(names):
        M, P, A = ms[n]
        tex, glow = P.run().images()
        im = render(M, tex, yaw=-35, pitch=10, size=S, extra_glow=glow)
        sheet.paste(im, ((i % cols) * S, (i // cols) * S))
    sheet.save('/tmp/prev/entities.png')
    print(len(names), 'models:', ' '.join(names))
