-- Prosedürel animasyon pozları (asset ID gerektirmez).
-- Her poz: eklem adı -> CFrame ofseti (Motor6D.C0 üzerine uygulanır).
-- Aksiyonlar: zaman çizelgesinde anahtar karelerle (keyframe) tanımlanır.

local Poses = {}
local R = math.rad

-- açı yardımcıları (derece). side: 1 = sağ, -1 = sol
local function arm(side, fwd, out, twist)
	twist = twist or 0
	if side == 1 then
		return CFrame.Angles(R(-out), R(twist), R(fwd))
	end
	return CFrame.Angles(R(-out), R(-twist), R(-fwd))
end
local leg = arm

local function neck(down, turn, tilt)
	return CFrame.Angles(R(down or 0), R(tilt or 0), R(turn or 0))
end

local function root(lean, yaw, roll, down, fwd)
	return CFrame.new(0, -(fwd or 0), -(down or 0)) * CFrame.Angles(R(lean or 0), R(roll or 0), R(yaw or 0))
end

Poses.arm, Poses.leg, Poses.neck, Poses.root = arm, leg, neck, root

Poses.Joints = { "Root", "Neck", "RS", "LS", "RH", "LH" }
Poses.JointNames = {
	Root = "RootJoint",
	Neck = "Neck",
	RS = "Right Shoulder",
	LS = "Left Shoulder",
	RH = "Right Hip",
	LH = "Left Hip",
}

-- ---------------------------------------------------------------- duruş pozları

-- Referans görseldeki imza duruş: sağ kol öne/aşağı uzanmış, açık el
Poses.Idle = {
	Root = root(0, 14, 0),
	Neck = neck(2, -12, 0),
	RS = arm(1, 42, 18, -20),
	LS = arm(-1, -6, 7),
	RH = leg(1, -6, 7),
	LH = leg(-1, 9, 3),
}

Poses.Block = {
	Root = root(10, 0, 0, 0.35),
	Neck = neck(12, 0, 0),
	RS = arm(1, 95, -42, 10),
	LS = arm(-1, 80, -38, 10),
	RH = leg(1, 18, 8),
	LH = leg(-1, -12, 8),
}

Poses.Jump = {
	Root = root(-4, 0, 0),
	RS = arm(1, 20, 35),
	LS = arm(-1, 20, 35),
	RH = leg(1, 35, 4),
	LH = leg(-1, -8, 4),
}

Poses.Fall = {
	Root = root(6, 0, 0),
	RS = arm(1, 10, 60),
	LS = arm(-1, 10, 60),
	RH = leg(1, 20, 8),
	LH = leg(-1, 5, 8),
}

Poses.Stunned = {
	Root = root(-16, 0, 0, 0.2),
	Neck = neck(-18, 0, 6),
	RS = arm(1, -20, 30),
	LS = arm(-1, -25, 28),
	RH = leg(1, 8, 6),
	LH = leg(-1, -14, 6),
}

Poses.GuardBroken = {
	Root = root(-22, 0, 0, 0.25),
	Neck = neck(-25, 0, 0),
	RS = arm(1, 140, 50),
	LS = arm(-1, 130, 55),
	RH = leg(1, 12, 10),
	LH = leg(-1, -18, 10),
}

-- Tsukuyomi'ye yakalanan kurban: donmuş, başı öne düşmüş
Poses.Trapped = {
	Root = root(14, 0, 0, 0.4),
	Neck = neck(35, 0, 8),
	RS = arm(1, 5, 4),
	LS = arm(-1, 5, 4),
	RH = leg(1, 10, 4),
	LH = leg(-1, 10, 4),
}

-- ---------------------------------------------------------------- aksiyonlar (keyframe)

local function key(t, pose)
	return { t = t, pose = pose }
end

Poses.Actions = {}

Poses.Actions.M1_1 = {
	Duration = 0.34,
	Keys = {
		key(0, { Root = root(4, 25, 0), RS = arm(1, 25, 12), LS = arm(-1, 65, -20) }),
		key(0.08, { Root = root(12, -22, 0, 0, 0.3), Neck = neck(0, 18), RS = arm(1, 96, -4, 0), LS = arm(-1, 70, -28), RH = leg(1, -18, 4), LH = leg(-1, 22, 4) }),
		key(0.34, { Root = root(4, -8, 0), RS = arm(1, 60, 10), LS = arm(-1, 55, -15) }),
	},
}

Poses.Actions.M1_2 = {
	Duration = 0.34,
	Keys = {
		key(0, { Root = root(4, -25, 0), LS = arm(-1, 25, 12), RS = arm(1, 65, -20) }),
		key(0.08, { Root = root(12, 22, 0, 0, 0.3), Neck = neck(0, -18), LS = arm(-1, 96, -4), RS = arm(1, 70, -28), LH = leg(-1, -18, 4), RH = leg(1, 22, 4) }),
		key(0.34, { Root = root(4, 8, 0), LS = arm(-1, 60, 10), RS = arm(1, 55, -15) }),
	},
}

Poses.Actions.M1_3 = {
	Duration = 0.38,
	Keys = {
		key(0, { Root = root(14, 20, 0, 0.3), RS = arm(1, -10, 20), LS = arm(-1, 60, -20), RH = leg(1, 20, 5), LH = leg(-1, -20, 5) }),
		key(0.1, { Root = root(-12, -15, 0, 0, 0.4), Neck = neck(-10, 10), RS = arm(1, 165, -10), LS = arm(-1, 30, 20), RH = leg(1, -10, 5), LH = leg(-1, 25, 5) }),
		key(0.38, { Root = root(0, 0, 0), RS = arm(1, 70, 10), LS = arm(-1, 40, 0) }),
	},
}

-- 4. vuruş: dönerek tekme (fırlatır)
Poses.Actions.M1_4 = {
	Duration = 0.55,
	Keys = {
		key(0, { Root = root(5, 40, 0, 0.2), RS = arm(1, 30, 40), LS = arm(-1, 30, 40), RH = leg(1, -10, 5) }),
		key(0.12, { Root = root(-25, -70, -15, 0, 0.3), Neck = neck(15, 50), RS = arm(1, -20, 70), LS = arm(-1, 40, 70), RH = leg(1, 95, 45), LH = leg(-1, -10, 5) }),
		key(0.3, { Root = root(-18, -110, -10, 0, 0.3), Neck = neck(10, 70), RS = arm(1, -20, 70), LS = arm(-1, 40, 70), RH = leg(1, 80, 50), LH = leg(-1, -10, 5) }),
		key(0.55, { Root = root(0, 0, 0), RS = arm(1, 40, 15), LS = arm(-1, 0, 8) }),
	},
}

Poses.Actions.DashF = {
	Duration = 0.3,
	Keys = {
		key(0, { Root = root(32, 0, 0, 0.3), Neck = neck(-20), RS = arm(1, -65, 18), LS = arm(-1, -65, 18), RH = leg(1, 50, 5), LH = leg(-1, -40, 5) }),
		key(0.3, { Root = root(10, 0, 0), RS = arm(1, -20, 10), LS = arm(-1, -20, 10) }),
	},
}
Poses.Actions.DashB = {
	Duration = 0.3,
	Keys = {
		key(0, { Root = root(-20, 0, 0, 0.3), Neck = neck(20), RS = arm(1, 40, 25), LS = arm(-1, 40, 25), RH = leg(1, -30, 5), LH = leg(-1, 40, 5) }),
		key(0.3, { Root = root(0, 0, 0), RS = arm(1, 20, 10), LS = arm(-1, 20, 10) }),
	},
}
Poses.Actions.DashL = {
	Duration = 0.3,
	Keys = {
		key(0, { Root = root(8, 10, 22, 0.3), RS = arm(1, 0, 50), LS = arm(-1, 0, 10), RH = leg(1, 0, 35), LH = leg(-1, 0, 5) }),
		key(0.3, { Root = root(0, 0, 0) }),
	},
}
Poses.Actions.DashR = {
	Duration = 0.3,
	Keys = {
		key(0, { Root = root(8, -10, -22, 0.3), RS = arm(1, 0, 10), LS = arm(-1, 0, 50), RH = leg(1, 0, 5), LH = leg(-1, 0, 35) }),
		key(0.3, { Root = root(0, 0, 0) }),
	},
}

-- Katon: el mühürleri -> eli ağza götürüp ateş üfleme
local seal1 = { Root = root(6, 0, 0, 0.2), Neck = neck(14), RS = arm(1, 70, -42, 30), LS = arm(-1, 70, -42, 30), RH = leg(1, 10, 10), LH = leg(-1, -10, 10) }
local seal2 = { Root = root(6, 0, 0, 0.2), Neck = neck(14), RS = arm(1, 85, -38, -40), LS = arm(-1, 62, -45, 50), RH = leg(1, 10, 10), LH = leg(-1, -10, 10) }
Poses.Actions.Katon = {
	Duration = 1.15,
	Keys = {
		key(0, seal1),
		key(0.15, seal2),
		key(0.3, seal1),
		key(0.45, seal2),
		key(0.55, { Root = root(-14, 0, 0, 0.1), Neck = neck(-14), RS = arm(1, 128, -48, 0), LS = arm(-1, 10, 20), RH = leg(1, -20, 8), LH = leg(-1, 20, 8) }),
		key(0.72, { Root = root(16, 0, 0, 0.3, 0.2), Neck = neck(6), RS = arm(1, 120, -40, 0), LS = arm(-1, -20, 25), RH = leg(1, 25, 8), LH = leg(-1, -25, 8) }),
		key(1.15, Poses.Idle),
	},
}

Poses.Actions.CrowVanish = {
	Duration = 0.3,
	Keys = {
		key(0, { Root = root(4, 0, 0), RS = arm(1, 30, 60), LS = arm(-1, 30, 60), Neck = neck(-10) }),
		key(0.3, { Root = root(10, 0, 0, 0.3), RS = arm(1, 60, 90), LS = arm(-1, 60, 90) }),
	},
}

Poses.Actions.CrowStrike = {
	Duration = 0.6,
	Keys = {
		key(0, { Root = root(10, 50, 0, 0.3), RS = arm(1, 30, 45), LS = arm(-1, 60, 20), RH = leg(1, -20, 5) }),
		key(0.12, { Root = root(-28, -40, -15, 0, 0.3), Neck = neck(18, 35), RS = arm(1, -30, 75), LS = arm(-1, 50, 65), RH = leg(1, 105, 30), LH = leg(-1, -12, 5) }),
		key(0.35, { Root = root(-20, -60, -10, 0, 0.3), Neck = neck(12, 45), RS = arm(1, -30, 75), LS = arm(-1, 50, 65), RH = leg(1, 90, 30), LH = leg(-1, -12, 5) }),
		key(0.6, Poses.Idle),
	},
}

-- Amaterasu: el kanayan göze, sonra hedefe bakış
Poses.Actions.Amaterasu = {
	Duration = 1.0,
	Keys = {
		key(0, { Root = root(10, 0, 0, 0.2), Neck = neck(18, 0, 10), RS = arm(1, 150, -55, 20), LS = arm(-1, 10, 10) }),
		key(0.45, { Root = root(14, 0, 0, 0.3), Neck = neck(22, 0, 14), RS = arm(1, 155, -58, 25), LS = arm(-1, 15, 12), RH = leg(1, 12, 8), LH = leg(-1, -10, 8) }),
		key(0.6, { Root = root(-6, 5, 0, 0.1), Neck = neck(-6, -5), RS = arm(1, 45, 20, -20), LS = arm(-1, 95, 5), RH = leg(1, -8, 8), LH = leg(-1, 12, 8) }),
		key(1.0, Poses.Idle),
	},
}

Poses.Actions.Tsukuyomi = {
	Duration = 1.3,
	Keys = {
		key(0, { Root = root(-4, 0, 0), Neck = neck(-6, 0), RS = arm(1, 15, 12), LS = arm(-1, 15, 12) }),
		key(0.4, { Root = root(6, 0, 0, 0.1), Neck = neck(8, 0), RS = arm(1, 70, 30, -30), LS = arm(-1, 8, 10) }),
		key(1.0, { Root = root(6, 0, 0, 0.1), Neck = neck(8, 0), RS = arm(1, 70, 30, -30), LS = arm(-1, 8, 10) }),
		key(1.3, Poses.Idle),
	},
}

Poses.Actions.Parried = {
	Duration = 0.5,
	Keys = {
		key(0, Poses.GuardBroken),
		key(0.5, Poses.Stunned),
	},
}

Poses.Actions.Hurt = {
	Duration = 0.35,
	Keys = {
		key(0, { Root = root(-18, 8, 6, 0.15), Neck = neck(-22, 10), RS = arm(1, -15, 30), LS = arm(-1, -10, 35), RH = leg(1, 15, 5), LH = leg(-1, -15, 5) }),
		key(0.35, Poses.Stunned),
	},
}

-- ---------------------------------------------------------------- örnekleme

local function ease(a)
	return 0.5 - math.cos(a * math.pi) * 0.5
end

local IDENTITY = CFrame.new()

-- Bir aksiyonun t anındaki pozunu döndürür (joint -> CFrame)
function Poses.Sample(actionName, t)
	local action = Poses.Actions[actionName]
	if not action then
		return nil
	end
	local keys = action.Keys
	local out = {}
	if t <= keys[1].t then
		for _, j in Poses.Joints do
			out[j] = keys[1].pose[j] or IDENTITY
		end
		return out
	end
	for i = 1, #keys - 1 do
		local a, b = keys[i], keys[i + 1]
		if t <= b.t then
			local alpha = ease((t - a.t) / math.max(b.t - a.t, 1e-3))
			for _, j in Poses.Joints do
				out[j] = (a.pose[j] or IDENTITY):Lerp(b.pose[j] or IDENTITY, alpha)
			end
			return out
		end
	end
	local last = keys[#keys].pose
	for _, j in Poses.Joints do
		out[j] = last[j] or IDENTITY
	end
	return out
end

return Poses
