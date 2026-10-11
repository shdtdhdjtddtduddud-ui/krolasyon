"""Skeletons for the Higgsfield meshes (block units, character faces +z, character's left = +x).

bone: name -> (parent, pivot, segment end, constraint dict)
constraints limit which vertices a bone may claim: xmin/xmax/ymin/ymax/zmin/zmax.
"""


def humanoid(hip_y, chest_y, neck_y, top, shoulder, elbow, hand, hip, knee, foot, arm_min_x=None, leg_max_y=None, z=0.0, extra=None):
    sx, sy = shoulder
    ex, ey = elbow
    hx, hy = hand
    px, py = hip
    kx, ky = knee
    fx, fy = foot
    amx = arm_min_x if arm_min_x is not None else sx * 0.75
    lmy = leg_max_y if leg_max_y is not None else py + 0.06
    b = {
        'body': (None, (0, hip_y, z), (0, chest_y, z), {}),
        'head': ('body', (0, neck_y, z), (0, top, z), {'ymin': neck_y - 0.12}),
        'arm_l': ('body', (sx, sy, z), (ex, ey, z), {'xmin': amx}),
        'forearm_l': ('arm_l', (ex, ey, z), (hx, hy, z), {'xmin': amx}),
        'arm_r': ('body', (-sx, sy, z), (-ex, ey, z), {'xmax': -amx}),
        'forearm_r': ('arm_r', (-ex, ey, z), (-hx, hy, z), {'xmax': -amx}),
        'leg_l': ('body', (px, py, z), (kx, ky, z), {'xmin': -0.02, 'ymax': lmy}),
        'shin_l': ('leg_l', (kx, ky, z), (fx, fy, z), {'xmin': -0.02, 'ymax': lmy}),
        'leg_r': ('body', (-px, py, z), (-kx, ky, z), {'xmax': 0.02, 'ymax': lmy}),
        'shin_r': ('leg_r', (-kx, ky, z), (-fx, fy, z), {'xmax': 0.02, 'ymax': lmy}),
    }
    if extra:
        b.update(extra)
    return b


RIGS = {
    'heatblast': humanoid(0.92, 1.55, 1.62, 2.05, (0.27, 1.60), (0.40, 1.25), (0.38, 0.75), (0.12, 0.92), (0.14, 0.48), (0.16, 0.02),
                          arm_min_x=0.25),
    'fourarms': humanoid(1.28, 2.05, 2.18, 2.6, (0.46, 2.08), (0.60, 1.55), (0.60, 0.92), (0.18, 1.28), (0.20, 0.66), (0.22, 0.04),
                         arm_min_x=0.47, extra={
            'arm2_l': ('body', (0.30, 1.95, 0), (0.36, 1.48, 0), {'xmin': 0.27, 'xmax': 0.47, 'ymax': 1.85}),
            'forearm2_l': ('arm2_l', (0.36, 1.48, 0), (0.33, 0.9, 0), {'xmin': 0.24, 'xmax': 0.47}),
            'arm2_r': ('body', (-0.30, 1.95, 0), (-0.36, 1.48, 0), {'xmax': -0.27, 'xmin': -0.47, 'ymax': 1.85}),
            'forearm2_r': ('arm2_r', (-0.36, 1.48, 0), (-0.33, 0.9, 0), {'xmax': -0.24, 'xmin': -0.47}),
        }),
    'diamondhead': humanoid(1.0, 1.70, 1.80, 2.3, (0.38, 1.75), (0.50, 1.30), (0.48, 0.60), (0.13, 1.0), (0.15, 0.52), (0.17, 0.03),
                            arm_min_x=0.30),
    'goop': humanoid(0.95, 1.6, 1.68, 2.25, (0.22, 1.58), (0.42, 1.15), (0.45, 0.6), (0.08, 0.95), (0.30, 0.42), (0.45, 0.0),
                     arm_min_x=0.20, leg_max_y=1.0),
    'xlr8': {
        'body': (None, (0, 0.85, -0.1), (0, 1.3, 0.25), {}),
        'head': ('body', (0, 1.35, 0.3), (0, 1.85, 0.45), {'ymin': 1.25, 'zmin': 0.1}),
        'arm_l': ('body', (0.22, 1.32, 0.2), (0.48, 1.25, 0.25), {'xmin': 0.2, 'ymin': 0.9}),
        'forearm_l': ('arm_l', (0.48, 1.25, 0.25), (0.5, 1.0, 0.6), {'xmin': 0.2, 'ymin': 0.9}),
        'arm_r': ('body', (-0.22, 1.32, 0.2), (-0.48, 1.25, 0.25), {'xmax': -0.2, 'ymin': 0.9}),
        'forearm_r': ('arm_r', (-0.48, 1.25, 0.25), (-0.5, 1.0, 0.6), {'xmax': -0.2, 'ymin': 0.9}),
        'leg_l': ('body', (0.18, 0.82, -0.05), (0.35, 0.5, 0.1), {'xmin': 0.02, 'ymax': 0.86, 'zmin': -0.35}),
        'shin_l': ('leg_l', (0.35, 0.5, 0.1), (0.5, 0.05, 0.05), {'xmin': 0.02, 'ymax': 0.86, 'zmin': -0.35}),
        'leg_r': ('body', (-0.18, 0.82, -0.05), (-0.35, 0.5, 0.1), {'xmax': -0.02, 'ymax': 0.86, 'zmin': -0.35}),
        'shin_r': ('leg_r', (-0.35, 0.5, 0.1), (-0.5, 0.05, 0.05), {'xmax': -0.02, 'ymax': 0.86, 'zmin': -0.35}),
        'tail1': ('body', (0, 0.85, -0.3), (-0.15, 0.8, -0.7), {'zmax': -0.25}),
        'tail2': ('tail1', (-0.15, 0.8, -0.7), (-0.45, 0.7, -0.95), {'zmax': -0.25}),
    },
    'terraspin': {
        'body': (None, (0, 0.85, 0), (0, 1.8, 0), {}),
        'head': ('body', (0, 1.62, 0.08), (0, 1.9, 0.15), {'ymin': 1.55, 'xmin': -0.2, 'xmax': 0.2, 'zmin': 0.0}),
        'arm_l': ('body', (0.42, 1.68, 0), (0.62, 1.2, 0), {'xmin': 0.42}),
        'forearm_l': ('arm_l', (0.62, 1.2, 0), (0.70, 0.55, 0), {'xmin': 0.42}),
        'arm_r': ('body', (-0.42, 1.68, 0), (-0.62, 1.2, 0), {'xmax': -0.42}),
        'forearm_r': ('arm_r', (-0.62, 1.2, 0), (-0.70, 0.55, 0), {'xmax': -0.42}),
        'leg_l': ('body', (0.17, 0.85, 0.05), (0.19, 0.45, 0.05), {'xmin': 0.0, 'xmax': 0.42, 'ymax': 0.85}),
        'shin_l': ('leg_l', (0.19, 0.45, 0.05), (0.2, 0.0, 0.05), {'xmin': 0.0, 'xmax': 0.42, 'ymax': 0.85}),
        'leg_r': ('body', (-0.17, 0.85, 0.05), (-0.19, 0.45, 0.05), {'xmax': 0.0, 'xmin': -0.42, 'ymax': 0.85}),
        'shin_r': ('leg_r', (-0.19, 0.45, 0.05), (-0.2, 0.0, 0.05), {'xmax': 0.0, 'xmin': -0.42, 'ymax': 0.85}),
    },
    'bigchill': humanoid(0.75, 1.75, 1.92, 2.3, (0.24, 1.80), (0.33, 1.30), (0.42, 0.85), (0.13, 0.58), (0.13, 0.30), (0.2, 0.0),
                         arm_min_x=0.26, leg_max_y=0.6),
    'ultimate_echo_echo': humanoid(1.12, 1.90, 2.02, 2.3, (0.33, 1.95), (0.40, 1.45), (0.38, 0.90), (0.12, 1.1), (0.17, 0.55), (0.2, 0.0),
                                   arm_min_x=0.30),
    'ghostfreak': {
        'tail': (None, (0, 0.62, 0), (0, 0.02, -0.05), {'ymax': 0.7}),
        'body': ('tail', (0, 0.62, 0), (0, 1.6, 0.0), {}),
        'head': ('body', (0, 1.7, 0.05), (0, 2.2, 0.15), {'ymin': 1.62}),
        'arm_l': ('body', (0.18, 1.6, 0.1), (0.35, 1.4, 0.35), {'xmin': 0.12, 'zmin': 0.05}),
        'forearm_l': ('arm_l', (0.35, 1.4, 0.35), (0.3, 1.0, 0.55), {'xmin': 0.12, 'zmin': 0.05}),
        'arm_r': ('body', (-0.18, 1.55, 0.1), (-0.3, 1.3, 0.35), {'xmax': -0.08, 'zmin': 0.05}),
        'forearm_r': ('arm_r', (-0.3, 1.3, 0.35), (-0.25, 0.95, 0.55), {'xmax': -0.08, 'zmin': 0.05}),
    },
}
