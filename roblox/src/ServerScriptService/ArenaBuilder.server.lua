-- Arena + ışıklandırma + vitrin heykeli. Oyun başlarken kodla inşa edilir.
-- (Studio'da Play'e basıp Workspace.Arena'yı kopyalayıp durdurduktan sonra
--  yapıştırarak kalıcı hale getirebilir ve elle düzenleyebilirsin.)

local Lighting = game:GetService("Lighting")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local ItachiBuilder = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("ItachiBuilder"))
local Poses = require(ReplicatedStorage.Shared:WaitForChild("Poses"))

local R = math.rad
local ARENA_RADIUS = 95

local existing = workspace:FindFirstChild("Arena")
if existing then
	existing:Destroy()
end

local arena = Instance.new("Model")
arena.Name = "Arena"

local function part(props)
	local p = Instance.new("Part")
	p.Anchored = true
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	for k, v in props do
		(p :: any)[k] = v
	end
	p.Parent = props.Parent or arena
	return p
end

local function disc(y, diameter, height, color, material, props)
	local p = part({
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(height, diameter, diameter),
		CFrame = CFrame.new(0, y, 0) * CFrame.Angles(0, 0, R(90)),
		Color = color,
		Material = material,
	})
	if props then
		for k, v in props do
			(p :: any)[k] = v
		end
	end
	return p
end

-- ---------------------------------------------------------------- zemin

part({
	Name = "Ground",
	Size = Vector3.new(1200, 4, 1200),
	CFrame = CFrame.new(0, -9, 0),
	Color = Color3.fromRGB(46, 42, 40),
	Material = Enum.Material.Ground,
})

disc(-4, ARENA_RADIUS * 2 + 14, 8, Color3.fromRGB(70, 66, 64), Enum.Material.Slate, { Name = "Base" })
disc(-1, ARENA_RADIUS * 2, 2, Color3.fromRGB(118, 112, 106), Enum.Material.Slate, { Name = "Floor" })
-- kırmızı halkalar (büyükten küçüğe, her biri biraz daha yüksekte -> z-fighting yok)
for idx, d in { ARENA_RADIUS * 2 - 4, 130, 64 } do
	local y = 0.02 + idx * 0.03
	disc(y, d, 0.06, Color3.fromRGB(150, 15, 25), Enum.Material.Neon, { Name = "Ring", CanCollide = false, Transparency = 0.15 })
	disc(y + 0.01, d - 1.2, 0.06, Color3.fromRGB(96 - idx * 6, 90 - idx * 6, 86 - idx * 6), Enum.Material.Cobblestone, { Name = "RingInner", CanCollide = false })
end

-- merkezde dev Akatsuki bulutu (zemine gömülü parçalar)
do
	local cloudParts = {
		{ -6, 0, 9 }, { 1, -3, 11 }, { 8, 0.5, 9 }, { 1, 3, 13 },
	}
	for _, c in cloudParts do
		part({
			Name = "CloudOutline",
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.1, c[3] + 1.2, c[3] + 1.2),
			CFrame = CFrame.new(c[1], 0.16, c[2]) * CFrame.Angles(0, 0, R(90)),
			Color = Color3.fromRGB(235, 230, 228),
			Material = Enum.Material.SmoothPlastic,
			CanCollide = false,
		})
	end
	for _, c in cloudParts do
		part({
			Name = "Cloud",
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.12, c[3], c[3]),
			CFrame = CFrame.new(c[1], 0.17, c[2]) * CFrame.Angles(0, 0, R(90)),
			Color = Color3.fromRGB(170, 20, 30),
			Material = Enum.Material.SmoothPlastic,
			CanCollide = false,
		})
	end
end

-- ---------------------------------------------------------------- duvar, sütunlar, meşaleler

local PILLARS = 20
for i = 1, PILLARS do
	local a = (i / PILLARS) * math.pi * 2
	local pos = Vector3.new(math.cos(a) * (ARENA_RADIUS + 3), 0, math.sin(a) * (ARENA_RADIUS + 3))
	local facing = CFrame.lookAt(pos, Vector3.new(0, 0, 0))

	part({ Name = "PillarBase", Size = Vector3.new(7, 2, 7), CFrame = facing * CFrame.new(0, 1, 0), Color = Color3.fromRGB(60, 56, 54), Material = Enum.Material.Slate })
	local h = 22
	part({ Name = "Pillar", Size = Vector3.new(5, h, 5), CFrame = facing * CFrame.new(0, h / 2 + 2, 0), Color = Color3.fromRGB(84, 78, 74), Material = Enum.Material.Slate })
	part({ Name = "PillarCap", Size = Vector3.new(6.5, 1.5, 6.5), CFrame = facing * CFrame.new(0, h + 2.75, 0), Color = Color3.fromRGB(60, 56, 54), Material = Enum.Material.Slate })
	-- kırmızı sancak
	part({ Name = "Banner", Size = Vector3.new(3.6, 9, 0.15), CFrame = facing * CFrame.new(0, h - 4, -2.6), Color = Color3.fromRGB(120, 10, 20), Material = Enum.Material.Fabric, CanCollide = false })

	-- meşale
	local bowl = part({ Name = "Torch", Shape = Enum.PartType.Cylinder, Size = Vector3.new(1.2, 3, 3), CFrame = facing * CFrame.new(0, h + 4.1, 0) * CFrame.Angles(0, 0, R(90)), Color = Color3.fromRGB(40, 36, 34), Material = Enum.Material.Metal })
	local fire = Instance.new("Fire")
	fire.Size = 6
	fire.Heat = 12
	fire.Color = Color3.fromRGB(255, 120, 40)
	fire.SecondaryColor = Color3.fromRGB(200, 30, 10)
	fire.Parent = bowl
	local light = Instance.new("PointLight")
	light.Color = Color3.fromRGB(255, 140, 70)
	light.Range = 32
	light.Brightness = 2.2
	light.Shadows = true
	light.Parent = bowl

	-- sütunlar arası alçak duvar
	local a2 = ((i + 0.5) / PILLARS) * math.pi * 2
	local wpos = Vector3.new(math.cos(a2) * (ARENA_RADIUS + 3), 0, math.sin(a2) * (ARENA_RADIUS + 3))
	local segLen = 2 * math.pi * (ARENA_RADIUS + 3) / PILLARS - 5
	part({ Name = "Wall", Size = Vector3.new(segLen, 6, 2.5), CFrame = CFrame.lookAt(wpos, Vector3.zero) * CFrame.new(0, 3, 0), Color = Color3.fromRGB(74, 68, 64), Material = Enum.Material.Slate })
	-- görünmez yüksek bariyer (arenadan düşmeyi engeller)
	part({ Name = "Barrier", Size = Vector3.new(segLen + 5, 60, 2), CFrame = CFrame.lookAt(wpos, Vector3.zero) * CFrame.new(0, 30, 0), Transparency = 1, CanCollide = true })
end

-- ---------------------------------------------------------------- siper kayaları

local rng = Random.new(7)
for i = 1, 6 do
	local a = (i / 6) * math.pi * 2 + 0.3
	local center = Vector3.new(math.cos(a) * 52, 0, math.sin(a) * 52)
	for j = 1, 4 do
		local s = Vector3.new(rng:NextNumber(4, 8), rng:NextNumber(4, 10), rng:NextNumber(4, 8))
		local off = Vector3.new(rng:NextNumber(-4, 4), s.Y / 2 - 0.5, rng:NextNumber(-4, 4))
		part({
			Name = "Rock",
			Size = s,
			CFrame = CFrame.new(center + off) * CFrame.Angles(R(rng:NextNumber(-12, 12)), R(rng:NextNumber(0, 360)), R(rng:NextNumber(-12, 12))),
			Color = Color3.fromRGB(88 + j * 4, 82 + j * 4, 78 + j * 4),
			Material = Enum.Material.Rock,
			Shape = j == 4 and Enum.PartType.Wedge or Enum.PartType.Block,
		})
	end
end

-- ---------------------------------------------------------------- doğma noktaları

for i = 1, 6 do
	local a = (i / 6) * math.pi * 2
	local pos = Vector3.new(math.cos(a) * 70, 0.6, math.sin(a) * 70)
	local sp = Instance.new("SpawnLocation")
	sp.Name = "Spawn"
	sp.Anchored = true
	sp.Size = Vector3.new(8, 1, 8)
	sp.CFrame = CFrame.lookAt(pos, Vector3.new(0, 0.6, 0))
	sp.Color = Color3.fromRGB(60, 56, 54)
	sp.Material = Enum.Material.Slate
	sp.Neutral = true
	sp.Duration = 0
	sp.TopSurface = Enum.SurfaceType.Smooth
	sp.Parent = arena
end

-- ---------------------------------------------------------------- gökyüzündeki kızıl ay

part({
	Name = "RedMoon",
	Shape = Enum.PartType.Ball,
	Size = Vector3.one * 220,
	CFrame = CFrame.new(-350, 520, -1100),
	Color = Color3.fromRGB(200, 20, 25),
	Material = Enum.Material.Neon,
	CanCollide = false,
	CastShadow = false,
})

-- ---------------------------------------------------------------- vitrin heykeli (modeli incelemek için)

do
	local pedestalPos = Vector3.new(0, 0, -ARENA_RADIUS + 12)
	part({ Name = "Pedestal", Size = Vector3.new(9, 3, 9), CFrame = CFrame.new(pedestalPos + Vector3.new(0, 1.5, 0)), Color = Color3.fromRGB(50, 46, 44), Material = Enum.Material.Marble })
	part({ Name = "PedestalTrim", Size = Vector3.new(9.4, 0.3, 9.4), CFrame = CFrame.new(pedestalPos + Vector3.new(0, 3.05, 0)), Color = Color3.fromRGB(150, 15, 25), Material = Enum.Material.Neon })
	local statue = ItachiBuilder.Build("Itachi Uchiha")
	for _, d in statue:GetDescendants() do
		if d:IsA("BasePart") then
			d.CanCollide = false
		end
	end
	statue.HumanoidRootPart.Anchored = true
	statue:PivotTo(CFrame.lookAt(pedestalPos + Vector3.new(0, 6.2, 0), Vector3.new(0, 6.2, 0)))
	-- imza duruşu uygula
	local torso = statue.Torso
	local joints = {
		Root = statue.HumanoidRootPart.RootJoint,
		Neck = torso.Neck,
		RS = torso["Right Shoulder"],
		LS = torso["Left Shoulder"],
		RH = torso["Right Hip"],
		LH = torso["Left Hip"],
	}
	for key, m in joints do
		local pose = Poses.Idle[key]
		if pose then
			m.C0 = m.C0 * pose
		end
	end
	local hum = statue:FindFirstChildOfClass("Humanoid")
	hum.DisplayName = "Itachi Uchiha"
	hum.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff
	hum.NameDisplayDistance = 40
	statue.Parent = arena
end

arena.Parent = workspace

-- ---------------------------------------------------------------- ışık ve atmosfer

Lighting.ClockTime = 18.4
Lighting.GeographicLatitude = 30
Lighting.Brightness = 2
Lighting.Ambient = Color3.fromRGB(60, 40, 45)
Lighting.OutdoorAmbient = Color3.fromRGB(110, 80, 85)
Lighting.EnvironmentDiffuseScale = 0.5
Lighting.EnvironmentSpecularScale = 0.5
Lighting.GlobalShadows = true
pcall(function()
	Lighting.Technology = Enum.Technology.Future
end)

for _, c in Lighting:GetChildren() do
	if c:IsA("PostEffect") or c:IsA("Atmosphere") or c:IsA("Sky") then
		c:Destroy()
	end
end

local atmosphere = Instance.new("Atmosphere")
atmosphere.Density = 0.32
atmosphere.Offset = 0.15
atmosphere.Color = Color3.fromRGB(200, 120, 110)
atmosphere.Decay = Color3.fromRGB(110, 40, 50)
atmosphere.Glare = 0.4
atmosphere.Haze = 1.6
atmosphere.Parent = Lighting

local bloom = Instance.new("BloomEffect")
bloom.Intensity = 0.8
bloom.Size = 30
bloom.Threshold = 1.6
bloom.Parent = Lighting

local cc = Instance.new("ColorCorrectionEffect")
cc.Contrast = 0.12
cc.Saturation = 0.05
cc.TintColor = Color3.fromRGB(255, 238, 235)
cc.Parent = Lighting

local rays = Instance.new("SunRaysEffect")
rays.Intensity = 0.06
rays.Spread = 0.6
rays.Parent = Lighting
