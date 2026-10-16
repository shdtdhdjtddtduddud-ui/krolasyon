-- Itachi tarzı R6 karakter modelini tamamen kodla (asset ID gerektirmeden) inşa eder.
-- Akatsuki pelerini (kırmızı bulutlar + yüksek kırmızı yaka), alın koruyucusu,
-- Sharingan gözler, at kuyruğu saç, mor tırnaklar, kırmızı yüzük, beyaz bacak sargıları.
--
-- ItachiBuilder.Build(name) -> Model  (Humanoid'li, oynanabilir R6 rig)

local ItachiBuilder = {}

local COLORS = {
	Cloak = Color3.fromRGB(20, 20, 26),
	CloakShade = Color3.fromRGB(12, 12, 16),
	Red = Color3.fromRGB(178, 24, 36),
	CloudOutline = Color3.fromRGB(242, 238, 236),
	Skin = Color3.fromRGB(252, 222, 196),
	Hair = Color3.fromRGB(16, 16, 22),
	Pants = Color3.fromRGB(38, 42, 68),
	Sandal = Color3.fromRGB(78, 82, 128),
	Nail = Color3.fromRGB(96, 84, 168),
	BandCloth = Color3.fromRGB(32, 36, 64),
	Metal = Color3.fromRGB(196, 200, 210),
	Fishnet = Color3.fromRGB(58, 60, 72),
	Wrap = Color3.fromRGB(236, 236, 240),
	Eye = Color3.fromRGB(205, 16, 22),
}
ItachiBuilder.Colors = COLORS

local R = math.rad

-- ---------------------------------------------------------------- helpers

local function newPart(props)
	local p = Instance.new("Part")
	p.Anchored = false
	p.CanCollide = false
	p.CanQuery = false
	p.CanTouch = false
	p.Massless = true
	p.CastShadow = true
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Material = Enum.Material.SmoothPlastic
	for k, v in props do
		(p :: any)[k] = v
	end
	return p
end

-- Bir parçayı vücut parçasına göre konumlayıp kaynakla
local function attach(container, body, name, size, color, offset, extra)
	local p = newPart({ Name = name, Size = size, Color = color })
	if extra then
		for k, v in extra do
			(p :: any)[k] = v
		end
	end
	p.CFrame = body.CFrame * offset
	local w = Instance.new("WeldConstraint")
	w.Part0 = body
	w.Part1 = p
	w.Parent = p
	p.Parent = container
	return p
end

local function frame(parent, props)
	local f = Instance.new("Frame")
	f.BorderSizePixel = 0
	f.BackgroundTransparency = 0
	for k, v in props do
		(f :: any)[k] = v
	end
	f.Parent = parent
	return f
end

local function round(f, scale)
	local c = Instance.new("UICorner")
	c.CornerRadius = UDim.new(scale or 0.5, 0)
	c.Parent = f
	return c
end

local function surfaceGui(part, face, pps)
	local g = Instance.new("SurfaceGui")
	g.Face = face
	g.SizingMode = Enum.SurfaceGuiSizingMode.PixelsPerStud
	g.PixelsPerStud = pps or 50
	g.LightInfluence = 1
	g.ClipsDescendants = true
	g.ResetOnSpawn = false
	g.Parent = part
	return g
end

-- Akatsuki bulutu: beyaz dış hat + kırmızı dolgu (overlapping daireler)
-- x,y: merkez (0..1 ölçek), w: genişlik (piksel)
local CLOUD_SHAPES = {
	-- {x, y, w, h, corner}  (40x26 birimlik bulut içinde)
	{ 0, 8, 17, 17, 0.5 },
	{ 10, 0, 21, 21, 0.5 },
	{ 23, 6, 17, 17, 0.5 },
	{ 5, 14, 31, 12, 0.5 },
}

local function cloud(gui, cx, cy, width, rotation)
	local s = width / 40
	local holder = frame(gui, {
		Name = "Cloud",
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.new(cx, 0, cy, 0),
		Size = UDim2.fromOffset(40 * s, 26 * s),
		Rotation = rotation or 0,
	})
	local o = 1.6 * s
	for _, sh in CLOUD_SHAPES do
		local f = frame(holder, {
			BackgroundColor3 = COLORS.CloudOutline,
			Position = UDim2.fromOffset(sh[1] * s - o, sh[2] * s - o),
			Size = UDim2.fromOffset(sh[3] * s + o * 2, sh[4] * s + o * 2),
			ZIndex = 1,
		})
		round(f, sh[5])
	end
	for _, sh in CLOUD_SHAPES do
		local f = frame(holder, {
			BackgroundColor3 = COLORS.Red,
			Position = UDim2.fromOffset(sh[1] * s, sh[2] * s),
			Size = UDim2.fromOffset(sh[3] * s, sh[4] * s),
			ZIndex = 2,
		})
		round(f, sh[5])
	end
	-- iç kıvrım çizgisi
	local swirl = frame(holder, {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(13 * s, 4 * s),
		Size = UDim2.fromOffset(12 * s, 12 * s),
		ZIndex = 3,
	})
	round(swirl)
	local st = Instance.new("UIStroke")
	st.Color = COLORS.CloudOutline
	st.Thickness = math.max(1, 1.2 * s)
	st.Parent = swirl
	return holder
end

-- ---------------------------------------------------------------- R6 rig

local function buildRig(name)
	local model = Instance.new("Model")
	model.Name = name

	local origin = CFrame.new(0, 3, 0)

	local hrp = newPart({
		Name = "HumanoidRootPart",
		Size = Vector3.new(2, 2, 1),
		Transparency = 1,
		CanCollide = false,
		CanQuery = true,
		CanTouch = true,
		Massless = false,
	})
	hrp.CFrame = origin

	local function body(n, size, offset, color)
		local p = newPart({
			Name = n,
			Size = size,
			Color = color,
			CanQuery = true,
			CanTouch = true,
			Massless = false,
		})
		p.CFrame = origin * offset
		p.Parent = model
		return p
	end

	local torso = body("Torso", Vector3.new(2, 2, 1), CFrame.new(), COLORS.Cloak)
	torso.CanCollide = true
	local head = body("Head", Vector3.new(2, 1, 1), CFrame.new(0, 1.5, 0), COLORS.Skin)
	head.CanCollide = true
	local mesh = Instance.new("SpecialMesh")
	mesh.MeshType = Enum.MeshType.Head
	mesh.Scale = Vector3.new(1.25, 1.25, 1.25)
	mesh.Parent = head
	local rArm = body("Right Arm", Vector3.new(1, 2, 1), CFrame.new(1.5, 0, 0), COLORS.Skin)
	local lArm = body("Left Arm", Vector3.new(1, 2, 1), CFrame.new(-1.5, 0, 0), COLORS.Skin)
	local rLeg = body("Right Leg", Vector3.new(1, 2, 1), CFrame.new(0.5, -2, 0), COLORS.Pants)
	local lLeg = body("Left Leg", Vector3.new(1, 2, 1), CFrame.new(-0.5, -2, 0), COLORS.Pants)
	hrp.Parent = model

	local function motor(n, p0, p1, c0, c1)
		local m = Instance.new("Motor6D")
		m.Name = n
		m.Part0 = p0
		m.Part1 = p1
		m.C0 = c0
		m.C1 = c1
		m.Parent = p0
		return m
	end

	motor("RootJoint", hrp, torso, CFrame.new(0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 1, 0), CFrame.new(0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 1, 0))
	motor("Neck", torso, head, CFrame.new(0, 1, 0, -1, 0, 0, 0, 0, 1, 0, 1, 0), CFrame.new(0, -0.5, 0, -1, 0, 0, 0, 0, 1, 0, 1, 0))
	motor("Right Shoulder", torso, rArm, CFrame.new(1, 0.5, 0, 0, 0, 1, 0, 1, 0, -1, 0, 0), CFrame.new(-0.5, 0.5, 0, 0, 0, 1, 0, 1, 0, -1, 0, 0))
	motor("Left Shoulder", torso, lArm, CFrame.new(-1, 0.5, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0), CFrame.new(0.5, 0.5, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0))
	motor("Right Hip", torso, rLeg, CFrame.new(1, -1, 0, 0, 0, 1, 0, 1, 0, -1, 0, 0), CFrame.new(0.5, 1, 0, 0, 0, 1, 0, 1, 0, -1, 0, 0))
	motor("Left Hip", torso, lLeg, CFrame.new(-1, -1, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0), CFrame.new(-0.5, 1, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0))

	local hum = Instance.new("Humanoid")
	hum.RigType = Enum.HumanoidRigType.R6
	hum.HipHeight = 0
	hum.WalkSpeed = 20
	hum.UseJumpPower = true
	hum.JumpPower = 50
	hum.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.Viewer
	hum.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff
	hum.Parent = model

	local animator = Instance.new("Animator")
	animator.Parent = hum

	model.PrimaryPart = hrp
	return model, {
		HRP = hrp,
		Torso = torso,
		Head = head,
		RArm = rArm,
		LArm = lArm,
		RLeg = rLeg,
		LLeg = lLeg,
	}
end

-- ---------------------------------------------------------------- yüz

local function buildFace(outfit, head)
	-- Kafanın önüne ince, görünmez bir yüzey; üstüne yüz çizilir
	local plane = attach(outfit, head, "FacePlane", Vector3.new(1, 0.9, 0.05), COLORS.Skin, CFrame.new(0, -0.03, -0.6), {
		Transparency = 1,
		CastShadow = false,
	})
	local gui = surfaceGui(plane, Enum.NormalId.Front, 100)
	gui.Name = "Face"

	local function eye(cx, side)
		-- side: -1 = karakterin sağ gözü (ekranda solda), 1 = sol göz
		local e = frame(gui, {
			Name = side < 0 and "RightEye" or "LeftEye",
			BackgroundColor3 = Color3.fromRGB(250, 250, 250),
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromOffset(cx, 44),
			Size = UDim2.fromOffset(25, 11),
			Rotation = side * -6,
			ClipsDescendants = true,
		})
		round(e, 0.45)
		local iris = frame(e, {
			Name = "Iris",
			BackgroundColor3 = COLORS.Eye,
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.new(0.5, side * 1, 0.5, 0),
			Size = UDim2.fromOffset(12, 12),
		})
		round(iris)
		local ring = Instance.new("UIStroke")
		ring.Color = Color3.fromRGB(40, 0, 0)
		ring.Thickness = 0.8
		ring.Parent = iris

		-- 3 tomoe Sharingan
		local tomoe = frame(iris, { Name = "Tomoe", BackgroundTransparency = 1, Size = UDim2.fromScale(1, 1) })
		local pupil = frame(tomoe, {
			BackgroundColor3 = Color3.new(0, 0, 0),
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(0.5, 0.5),
			Size = UDim2.fromOffset(3.5, 3.5),
		})
		round(pupil)
		for i = 0, 2 do
			local a = R(90 + i * 120)
			local t = frame(tomoe, {
				BackgroundColor3 = Color3.new(0, 0, 0),
				AnchorPoint = Vector2.new(0.5, 0.5),
				Position = UDim2.new(0.5, math.cos(a) * 3.6, 0.5, math.sin(a) * 3.6),
				Size = UDim2.fromOffset(2.6, 2.6),
			})
			round(t)
		end

		-- Mangekyō (Amaterasu / Tsukuyomi sırasında görünür)
		local ms = frame(iris, { Name = "Mangekyo", BackgroundTransparency = 1, Size = UDim2.fromScale(1, 1), Visible = false })
		local core = frame(ms, {
			BackgroundColor3 = Color3.new(0, 0, 0),
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(0.5, 0.5),
			Size = UDim2.fromOffset(4, 4),
		})
		round(core)
		for i = 0, 2 do
			local blade = frame(ms, {
				BackgroundColor3 = Color3.new(0, 0, 0),
				AnchorPoint = Vector2.new(0, 0.5),
				Position = UDim2.fromScale(0.5, 0.5),
				Size = UDim2.fromOffset(6.5, 2.2),
				Rotation = i * 120 + 20,
			})
			round(blade, 0.5)
		end

		-- üst göz kapağı çizgisi
		frame(gui, {
			Name = "Lid",
			BackgroundColor3 = Color3.fromRGB(20, 12, 12),
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromOffset(cx, 38.5),
			Size = UDim2.fromOffset(28, 2.4),
			Rotation = side * -8,
		})
		-- kaş
		frame(gui, {
			Name = "Brow",
			BackgroundColor3 = Color3.fromRGB(20, 16, 18),
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromOffset(cx + side * 1, 29),
			Size = UDim2.fromOffset(20, 1.8),
			Rotation = side * -10,
		})
		-- Itachi'nin göz altı çizgileri
		frame(gui, {
			Name = "TearTrough",
			BackgroundColor3 = Color3.fromRGB(150, 110, 100),
			AnchorPoint = Vector2.new(0.5, 0),
			Position = UDim2.fromOffset(cx - side * 7, 49),
			Size = UDim2.fromOffset(1.3, 12),
			Rotation = side * 22,
		})
		return e
	end

	eye(30, -1)
	eye(70, 1)

	-- Amaterasu kanaması (karakterin sağ gözü)
	frame(gui, {
		Name = "Blood",
		BackgroundColor3 = Color3.fromRGB(150, 0, 8),
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.fromOffset(31, 49),
		Size = UDim2.fromOffset(2.6, 30),
		Visible = false,
	})

	-- burun ve ağız
	frame(gui, {
		BackgroundColor3 = Color3.fromRGB(205, 160, 140),
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromOffset(52, 60),
		Size = UDim2.fromOffset(1.4, 6),
		Rotation = 12,
	})
	frame(gui, {
		BackgroundColor3 = Color3.fromRGB(120, 60, 60),
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromOffset(50, 73),
		Size = UDim2.fromOffset(11, 1.6),
	})
end

-- ---------------------------------------------------------------- saç ve alın koruyucu

local function buildHair(outfit, head)
	local H = COLORS.Hair
	local cap = attach(outfit, head, "HairCap", Vector3.new(1.36, 1.18, 1.36), H, CFrame.new(0, 0.24, 0.1))
	local capMesh = Instance.new("SpecialMesh")
	capMesh.MeshType = Enum.MeshType.Sphere
	capMesh.Parent = cap
	attach(outfit, head, "HairBack", Vector3.new(1.26, 0.95, 0.42), H, CFrame.new(0, -0.08, 0.46))
	-- tepedeki sivri tutamlar
	attach(outfit, head, "Spike1", Vector3.new(0.35, 0.42, 0.62), H, CFrame.new(0, 0.78, 0.28) * CFrame.Angles(R(-35), 0, 0), { Shape = Enum.PartType.Wedge })
	attach(outfit, head, "Spike2", Vector3.new(0.3, 0.38, 0.55), H, CFrame.new(0.32, 0.7, 0.3) * CFrame.Angles(R(-30), R(20), R(-20)), { Shape = Enum.PartType.Wedge })
	attach(outfit, head, "Spike3", Vector3.new(0.3, 0.38, 0.55), H, CFrame.new(-0.32, 0.7, 0.3) * CFrame.Angles(R(-30), R(-20), R(20)), { Shape = Enum.PartType.Wedge })
	-- yüzü çerçeveleyen uzun yan perçemler
	for _, s in { -1, 1 } do
		attach(outfit, head, "Bang", Vector3.new(0.16, 1.12, 0.3), H, CFrame.new(s * 0.57, -0.2, -0.4) * CFrame.Angles(0, 0, R(s * 4)))
		attach(outfit, head, "BangTip", Vector3.new(0.14, 0.3, 0.24), H, CFrame.new(s * 0.6, -0.83, -0.42) * CFrame.Angles(R(180), 0, R(s * 10)), { Shape = Enum.PartType.Wedge })
		attach(outfit, head, "Fringe", Vector3.new(0.18, 0.55, 0.07), H, CFrame.new(s * 0.36, 0.08, -0.62) * CFrame.Angles(0, 0, R(s * 14)))
	end
	-- düşük at kuyruğu
	attach(outfit, head, "PonytailTie", Vector3.new(0.26, 0.12, 0.26), COLORS.BandCloth, CFrame.new(0, -0.55, 0.66))
	attach(outfit, head, "Ponytail", Vector3.new(0.24, 0.85, 0.22), H, CFrame.new(0, -1.0, 0.74) * CFrame.Angles(R(12), 0, 0))
	attach(outfit, head, "PonytailTip", Vector3.new(0.22, 0.3, 0.2), H, CFrame.new(0, -1.52, 0.82) * CFrame.Angles(R(192), 0, 0), { Shape = Enum.PartType.Wedge })

	-- alın koruyucu (Konoha sembolü + çizik)
	attach(outfit, head, "BandCloth", Vector3.new(0.24, 1.44, 1.44), COLORS.BandCloth, CFrame.new(0, 0.3, 0.08) * CFrame.Angles(0, 0, R(90)), { Shape = Enum.PartType.Cylinder })
	local plate = attach(outfit, head, "Protector", Vector3.new(0.82, 0.27, 0.06), COLORS.Metal, CFrame.new(0, 0.3, -0.66), { Material = Enum.Material.Metal, Reflectance = 0.15 })
	local g = surfaceGui(plate, Enum.NormalId.Front, 120)
	g.Name = "Symbol"
	local leaf = frame(g, {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.52),
		Size = UDim2.fromOffset(22, 22),
	})
	round(leaf)
	local st = Instance.new("UIStroke")
	st.Thickness = 2
	st.Color = Color3.fromRGB(60, 60, 70)
	st.Parent = leaf
	local inner = frame(leaf, {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.55, 0.55),
		Size = UDim2.fromOffset(9, 9),
	})
	round(inner)
	local st2 = st:Clone()
	st2.Parent = inner
	frame(leaf, {
		BackgroundColor3 = Color3.fromRGB(60, 60, 70),
		Position = UDim2.new(0.85, 0, 0.05, 0),
		Size = UDim2.fromOffset(8, 2),
		Rotation = -40,
	})
	-- Akatsuki'ye geçişi simgeleyen yatay çizik
	frame(g, {
		BackgroundColor3 = Color3.fromRGB(40, 40, 46),
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.new(0.86, 0, 0, 2),
		Rotation = -3,
	})
	-- arkadaki bağ uçları
	attach(outfit, head, "BandTailL", Vector3.new(0.14, 0.62, 0.04), COLORS.BandCloth, CFrame.new(-0.1, -0.02, 0.76) * CFrame.Angles(0, 0, R(8)))
	attach(outfit, head, "BandTailR", Vector3.new(0.14, 0.55, 0.04), COLORS.BandCloth, CFrame.new(0.12, 0.0, 0.77) * CFrame.Angles(0, 0, R(-10)))
end

-- ---------------------------------------------------------------- Akatsuki pelerini

local function buildCloak(outfit, parts)
	local B, RED = COLORS.Cloak, COLORS.Red
	local torso = parts.Torso

	-- gövde
	local body = attach(outfit, torso, "CloakTorso", Vector3.new(2.14, 2.06, 1.16), B, CFrame.new())
	do
		local front = surfaceGui(body, Enum.NormalId.Front, 50)
		cloud(front, 0.74, 0.55, 40, -8)
		local back = surfaceGui(body, Enum.NormalId.Back, 50)
		cloud(back, 0.5, 0.42, 58, 0)
	end
	-- önden açık kısım: koyu iç tişört + kırmızı kenar şeritleri
	attach(outfit, torso, "Undershirt", Vector3.new(0.42, 1.25, 0.03), COLORS.Pants, CFrame.new(-0.05, 0.4, -0.585))
	attach(outfit, torso, "Fishnet", Vector3.new(0.5, 0.35, 0.03), COLORS.Fishnet, CFrame.new(-0.05, 0.85, -0.6), { Material = Enum.Material.DiamondPlate })
	attach(outfit, torso, "TrimL", Vector3.new(0.06, 2.06, 0.03), RED, CFrame.new(-0.29, 0, -0.595))
	attach(outfit, torso, "TrimR", Vector3.new(0.06, 2.06, 0.03), RED, CFrame.new(0.2, 0, -0.595))
	attach(outfit, torso, "ChestFlapL", Vector3.new(0.6, 1.3, 0.04), B, CFrame.new(-0.62, 0.3, -0.6) * CFrame.Angles(0, R(-8), 0))

	-- yüksek yaka: dışı siyah, içi kırmızı
	local collarY = 1.42
	attach(outfit, torso, "CollarBack", Vector3.new(1.7, 0.95, 0.1), B, CFrame.new(0, collarY, 0.5) * CFrame.Angles(R(12), 0, 0))
	attach(outfit, torso, "CollarBackIn", Vector3.new(1.6, 0.9, 0.04), RED, CFrame.new(0, collarY, 0.43) * CFrame.Angles(R(12), 0, 0))
	for _, s in { -1, 1 } do
		attach(outfit, torso, "CollarSide", Vector3.new(0.1, 0.95, 1.05), B, CFrame.new(s * 0.82, collarY, 0.0) * CFrame.Angles(0, 0, R(-s * 14)))
		attach(outfit, torso, "CollarSideIn", Vector3.new(0.04, 0.9, 1.0), RED, CFrame.new(s * 0.75, collarY, 0.0) * CFrame.Angles(0, 0, R(-s * 14)))
		-- öne katlanmış yaka kanatları (kırmızı astar öne bakar)
		local flapCF = CFrame.new(s * 0.62, collarY - 0.05, -0.5) * CFrame.Angles(R(-6), R(s * 32), R(-s * 8))
		attach(outfit, torso, "CollarFlap", Vector3.new(0.55, 0.9, 0.08), B, flapCF)
		attach(outfit, torso, "CollarFlapIn", Vector3.new(0.5, 0.86, 0.03), RED, flapCF * CFrame.new(0, 0, -0.055))
	end

	-- kollar: geniş Akatsuki yenleri
	for _, info in { { parts.RArm, 1, Enum.NormalId.Right }, { parts.LArm, -1, Enum.NormalId.Left } } do
		local arm, s, face = info[1], info[2], info[3]
		local upper = attach(outfit, arm, "SleeveUpper", Vector3.new(1.2, 1.3, 1.2), B, CFrame.new(0, 0.38, 0))
		local g = surfaceGui(upper, face, 50)
		cloud(g, 0.5, 0.45, 38, s * 12)
		attach(outfit, arm, "SleeveFlare", Vector3.new(1.42, 0.78, 1.42), B, CFrame.new(0, -0.26, 0))
		attach(outfit, arm, "SleeveLining", Vector3.new(1.34, 0.06, 1.34), RED, CFrame.new(0, -0.66, 0))
		-- mor tırnaklar
		attach(outfit, arm, "Nails", Vector3.new(0.82, 0.07, 0.06), COLORS.Nail, CFrame.new(0, -0.97, -0.48))
	end
	-- sağ yüzük parmağında kırmızı Akatsuki yüzüğü
	attach(outfit, parts.RArm, "Ring", Vector3.new(0.22, 0.14, 0.22), RED, CFrame.new(-0.28, -0.84, -0.42), { Material = Enum.Material.Neon, Color = Color3.fromRGB(150, 10, 20) })

	-- bacaklar: pelerin eteği, beyaz sargılar, sandaletler
	for _, info in { { parts.RLeg, 1 }, { parts.LLeg, -1 } } do
		local leg, s = info[1], info[2]
		local skirt = attach(outfit, leg, "Skirt", Vector3.new(1.18, 1.5, 1.22), B, CFrame.new(s * 0.05, 0.28, 0))
		attach(outfit, leg, "Hem", Vector3.new(1.3, 0.32, 1.34), B, CFrame.new(s * 0.08, -0.33, 0))
		attach(outfit, leg, "SkirtTrim", Vector3.new(0.06, 1.8, 0.03), RED, CFrame.new(-s * 0.55, 0.12, -0.665))
		attach(outfit, leg, "SkirtLining", Vector3.new(0.4, 0.3, 0.03), RED, CFrame.new(-s * 0.38, -0.3, -0.672))
		if s > 0 then
			local g = surfaceGui(skirt, Enum.NormalId.Front, 50)
			cloud(g, 0.45, 0.6, 40, 6)
		else
			local g = surfaceGui(skirt, Enum.NormalId.Back, 50)
			cloud(g, 0.5, 0.55, 42, -6)
			local g2 = surfaceGui(skirt, Enum.NormalId.Left, 50)
			cloud(g2, 0.5, 0.35, 36, 4)
		end
		attach(outfit, leg, "Wrap", Vector3.new(1.08, 0.5, 1.08), COLORS.Wrap, CFrame.new(0, -0.6, 0), { Material = Enum.Material.Fabric })
		attach(outfit, leg, "Sandal", Vector3.new(1.1, 0.18, 1.32), COLORS.Sandal, CFrame.new(0, -0.93, -0.13))
		attach(outfit, leg, "Strap", Vector3.new(1.12, 0.14, 0.3), COLORS.Sandal, CFrame.new(0, -0.8, -0.4))
		attach(outfit, leg, "Toes", Vector3.new(0.92, 0.13, 0.26), COLORS.Skin, CFrame.new(0, -0.79, -0.66))
		attach(outfit, leg, "ToeNails", Vector3.new(0.8, 0.07, 0.03), COLORS.Nail, CFrame.new(0, -0.8, -0.8))
	end
end

-- ---------------------------------------------------------------- public

function ItachiBuilder.Build(name)
	local model, parts = buildRig(name or "Itachi")
	local outfit = Instance.new("Model")
	outfit.Name = "Outfit"
	outfit.Parent = model

	buildFace(outfit, parts.Head)
	buildHair(outfit, parts.Head)
	buildCloak(outfit, parts)

	model:SetAttribute("CharacterKit", "Itachi")
	return model
end

-- Yüzdeki göz modunu değiştir ("Sharingan" | "Mangekyo")
function ItachiBuilder.SetEyes(character, mode)
	local face = character:FindFirstChild("FacePlane", true)
	local gui = face and face:FindFirstChild("Face")
	if not gui then
		return
	end
	for _, eyeName in { "RightEye", "LeftEye" } do
		local e = gui:FindFirstChild(eyeName)
		local iris = e and e:FindFirstChild("Iris")
		if iris then
			iris.Tomoe.Visible = mode ~= "Mangekyo"
			iris.Mangekyo.Visible = mode == "Mangekyo"
		end
	end
end

function ItachiBuilder.SetBleeding(character, on)
	local face = character:FindFirstChild("FacePlane", true)
	local gui = face and face:FindFirstChild("Face")
	local blood = gui and gui:FindFirstChild("Blood")
	if blood then
		blood.Visible = on
	end
end

return ItachiBuilder
