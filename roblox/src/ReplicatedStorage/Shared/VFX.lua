-- İstemci tarafı görsel efektler. Sunucu "VFX" RemoteEvent'i ile (isim, veri) gönderir,
-- CombatClient bu modüldeki aynı isimli fonksiyonu çağırır.
-- Hiçbir efekt dışarıdan asset gerektirmez: Neon parçalar, tween'ler, Fire ve
-- Roblox'un dahili parçacık dokuları kullanılır.

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")
local Debris = game:GetService("Debris")
local Lighting = game:GetService("Lighting")

local Config = require(script.Parent.Config)
local ItachiBuilder = require(script.Parent.ItachiBuilder)

local VFX = {}

local player = Players.LocalPlayer
local rng = Random.new()

local folder = workspace:FindFirstChild("ClientVFX") or Instance.new("Folder")
folder.Name = "ClientVFX"
folder.Parent = workspace

-- ---------------------------------------------------------------- yardımcılar

local function tween(inst, t, props, style, dir)
	local tw = TweenService:Create(inst, TweenInfo.new(t, style or Enum.EasingStyle.Quad, dir or Enum.EasingDirection.Out), props)
	tw:Play()
	return tw
end

local function fxPart(props)
	local p = Instance.new("Part")
	p.Anchored = true
	p.CanCollide = false
	p.CanQuery = false
	p.CanTouch = false
	p.CastShadow = false
	p.Material = Enum.Material.Neon
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	for k, v in props do
		(p :: any)[k] = v
	end
	p.Parent = folder
	return p
end

local function ball(pos, size, color, props)
	local p = fxPart({ Shape = Enum.PartType.Ball, Size = Vector3.one * size, Color = color, CFrame = CFrame.new(pos) })
	if props then
		for k, v in props do
			(p :: any)[k] = v
		end
	end
	return p
end

local function sound(id, where, volume, pitch)
	if not id or id == "" then
		return
	end
	local holder
	if typeof(where) == "Vector3" then
		holder = fxPart({ Size = Vector3.one * 0.1, Transparency = 1, CFrame = CFrame.new(where) })
		Debris:AddItem(holder, 6)
	else
		holder = where
	end
	if not holder then
		return
	end
	local s = Instance.new("Sound")
	s.SoundId = id
	s.Volume = volume or 0.7
	s.PlaybackSpeed = (pitch or 1) * rng:NextNumber(0.93, 1.07)
	s.RollOffMaxDistance = 220
	s.Parent = holder
	s:Play()
	Debris:AddItem(s, 6)
end
VFX.Sound = sound

local function emitter(parent, props)
	local e = Instance.new("ParticleEmitter")
	e.Texture = Config.Textures.Fire
	e.Rate = 0
	e.LightEmission = 1
	e.Speed = NumberRange.new(5)
	e.Lifetime = NumberRange.new(0.5)
	e.SpreadAngle = Vector2.new(180, 180)
	for k, v in props do
		(e :: any)[k] = v
	end
	e.Parent = parent
	return e
end

local function seq(...)
	local args = { ... }
	local kps = {}
	for i = 1, #args, 2 do
		table.insert(kps, NumberSequenceKeypoint.new(args[i], args[i + 1]))
	end
	return NumberSequence.new(kps)
end

local function colorSeq(a, b)
	return ColorSequence.new(a, b or a)
end

local function anchorAt(pos, life)
	local p = fxPart({ Size = Vector3.one * 0.2, Transparency = 1, CFrame = CFrame.new(pos) })
	Debris:AddItem(p, life or 4)
	return p
end

local function groundAt(pos)
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = { folder }
	for _, plr in Players:GetPlayers() do
		if plr.Character then
			params:AddToFilter(plr.Character)
		end
	end
	local hit = workspace:Raycast(pos + Vector3.new(0, 3, 0), Vector3.new(0, -60, 0), params)
	return hit and hit.Position or (pos - Vector3.new(0, 3, 0))
end

local function myRoot()
	local c = player.Character
	return c and c:FindFirstChild("HumanoidRootPart")
end

-- ---------------------------------------------------------------- kamera sarsıntısı

local shakeUntil, shakePower = 0, 0
RunService:BindToRenderStep("CombatShake", Enum.RenderPriority.Camera.Value + 1, function()
	local hum = player.Character and player.Character:FindFirstChildOfClass("Humanoid")
	if not hum then
		return
	end
	local now = os.clock()
	if now < shakeUntil then
		local p = shakePower * ((shakeUntil - now) / 0.4) ^ 0.5
		hum.CameraOffset = Vector3.new(rng:NextNumber(-p, p), rng:NextNumber(-p, p), rng:NextNumber(-p, p) * 0.5)
	elseif hum.CameraOffset ~= Vector3.zero then
		hum.CameraOffset = Vector3.zero
	end
end)

function VFX.Shake(power, duration)
	shakePower = math.max(power, os.clock() < shakeUntil and shakePower or 0)
	shakeUntil = math.max(shakeUntil, os.clock() + (duration or 0.3))
end

local function shakeByDistance(pos, power, radius)
	local r = myRoot()
	if not r then
		return
	end
	local d = (r.Position - pos).Magnitude
	if d < radius then
		VFX.Shake(power * (1 - d / radius), 0.4)
	end
end

-- ---------------------------------------------------------------- temel parçalar

local function flash(pos, size, color, t)
	local b = ball(pos, size * 0.3, color, { Transparency = 0.1 })
	tween(b, t, { Size = Vector3.one * size, Transparency = 1 })
	Debris:AddItem(b, t)
end

local function shockwave(pos, color, size, t, normal)
	local cf = CFrame.lookAt(pos, pos + (normal or Vector3.yAxis)) * CFrame.Angles(0, math.rad(90), 0)
	local d = fxPart({ Shape = Enum.PartType.Cylinder, Size = Vector3.new(0.15, 1, 1), Color = color, CFrame = cf, Transparency = 0.2 })
	tween(d, t, { Size = Vector3.new(0.05, size, size), Transparency = 1 })
	Debris:AddItem(d, t)
end

local function sparks(pos, n, color, speed, life, len)
	for _ = 1, n do
		local dir = rng:NextUnitVector()
		local p = fxPart({ Size = Vector3.new(0.12, 0.12, len or 1.4), Color = color })
		p.CFrame = CFrame.lookAt(pos, pos + dir)
		local target = pos + dir * speed * rng:NextNumber(0.6, 1.1)
		tween(p, life, { CFrame = CFrame.lookAt(target, target + dir), Size = Vector3.new(0.02, 0.02, 0.2), Transparency = 1 })
		Debris:AddItem(p, life)
	end
end

local function billboardText(pos, text, color, sizeY, life, rise)
	local a = anchorAt(pos, life + 0.2)
	local bb = Instance.new("BillboardGui")
	bb.Size = UDim2.fromScale(sizeY * 4, sizeY)
	bb.AlwaysOnTop = true
	bb.LightInfluence = 0
	bb.MaxDistance = 250
	bb.Parent = a
	local label = Instance.new("TextLabel")
	label.BackgroundTransparency = 1
	label.Size = UDim2.fromScale(1, 1)
	label.Text = text
	label.TextScaled = true
	label.Font = Enum.Font.GothamBlack
	label.TextColor3 = color
	label.TextStrokeTransparency = 0
	label.TextStrokeColor3 = Color3.new(0, 0, 0)
	label.Parent = bb
	tween(bb, life, { StudsOffsetWorldSpace = Vector3.new(0, rise or 2, 0) })
	task.delay(life * 0.55, function()
		tween(label, life * 0.45, { TextTransparency = 1, TextStrokeTransparency = 1 })
	end)
	return label
end

-- ---------------------------------------------------------------- vuruş efektleri

function VFX.Hit(d)
	local pos = d.pos
	local heavy = d.heavy
	flash(pos, heavy and 6 or 3.2, Color3.fromRGB(255, 255, 255), heavy and 0.25 or 0.16)
	flash(pos, heavy and 9 or 4.5, Color3.fromRGB(255, 200, 120), heavy and 0.3 or 0.2)
	sparks(pos, heavy and 14 or 7, Color3.fromRGB(255, 235, 200), heavy and 10 or 6, 0.25)
	if heavy then
		shockwave(pos, Color3.fromRGB(255, 255, 255), 14, 0.3, d.dir)
	end
	sound(heavy and Config.Sounds.HeavyHit or Config.Sounds.Hit, pos, heavy and 1 or 0.7, heavy and 0.7 or 1.1)
	shakeByDistance(pos, heavy and 0.6 or 0.25, 25)
end

function VFX.Swing(d)
	local root = d.char and d.char:FindFirstChild("HumanoidRootPart")
	if not root then
		return
	end
	sound(Config.Sounds.Swing, root, 0.35, 1.5)
	local cf = root.CFrame * CFrame.new(0, 0.4, -2.6) * CFrame.Angles(0, 0, rng:NextNumber(-0.7, 0.7))
	local arc = fxPart({ Size = Vector3.new(3.6, 0.05, 0.35), Color = Color3.fromRGB(255, 255, 255), Transparency = 0.45, CFrame = cf })
	tween(arc, 0.14, { Transparency = 1, Size = Vector3.new(5, 0.02, 0.08) })
	Debris:AddItem(arc, 0.15)
end

function VFX.Block(d)
	local pos = d.pos
	local shield = ball(pos, 3, Color3.fromRGB(120, 190, 255), { Transparency = 0.4, Material = Enum.Material.ForceField })
	tween(shield, 0.25, { Size = Vector3.one * 5.5, Transparency = 1 })
	Debris:AddItem(shield, 0.25)
	sparks(pos, 6, Color3.fromRGB(150, 210, 255), 5, 0.2, 0.8)
	sound(Config.Sounds.Block, pos, 0.6, 1.4)
end

function VFX.Parry(d)
	local pos = d.pos
	flash(pos, 10, Color3.fromRGB(255, 255, 255), 0.3)
	flash(pos, 14, Color3.fromRGB(255, 215, 90), 0.4)
	shockwave(pos, Color3.fromRGB(255, 230, 140), 20, 0.35, d.dir)
	shockwave(pos, Color3.fromRGB(255, 255, 255), 12, 0.25)
	sparks(pos, 18, Color3.fromRGB(255, 225, 120), 12, 0.35)
	billboardText(pos + Vector3.new(0, 2, 0), "PARRY!", Color3.fromRGB(255, 225, 90), 2.2, 0.8, 3)
	sound(Config.Sounds.Parry, pos, 1, 0.9)
	shakeByDistance(pos, 0.8, 30)
end

function VFX.GuardBreak(d)
	local pos = d.pos
	for _ = 1, 12 do
		local dir = rng:NextUnitVector()
		local s = fxPart({ Size = Vector3.new(0.5, 0.08, 0.7), Color = Color3.fromRGB(140, 200, 255), Material = Enum.Material.Glass, Transparency = 0.2 })
		s.CFrame = CFrame.new(pos) * CFrame.Angles(rng:NextNumber(0, 6), rng:NextNumber(0, 6), 0)
		tween(s, 0.5, { CFrame = s.CFrame + dir * 6 + Vector3.new(0, -2, 0), Transparency = 1 })
		Debris:AddItem(s, 0.5)
	end
	flash(pos, 8, Color3.fromRGB(255, 80, 80), 0.3)
	billboardText(pos + Vector3.new(0, 2, 0), "GUARD BREAK", Color3.fromRGB(255, 80, 80), 2.2, 0.9, 3)
	sound(Config.Sounds.GuardBreak, pos, 1, 0.8)
	shakeByDistance(pos, 0.7, 30)
end

function VFX.DamageNumber(d)
	local offset = Vector3.new(rng:NextNumber(-1.5, 1.5), 2.5, rng:NextNumber(-1.5, 1.5))
	local color = d.color or Color3.fromRGB(255, 255, 255)
	local label = billboardText(d.pos + offset, tostring(d.amount), color, d.big and 1.6 or 1.1, 0.9, 2.5)
	label.Font = Enum.Font.GothamBold
end

function VFX.Text(d)
	billboardText(d.pos, d.text, d.color or Color3.new(1, 1, 1), d.size or 1.6, d.life or 1.2, 2)
end

-- ---------------------------------------------------------------- dash

local AFTERIMAGE_PARTS = {
	Head = true, Torso = true, ["Left Arm"] = true, ["Right Arm"] = true, ["Left Leg"] = true, ["Right Leg"] = true,
	CloakTorso = true, Skirt = true, Hem = true, SleeveUpper = true, SleeveFlare = true, HairCap = true, HairBack = true,
	CollarBack = true, CollarSide = true,
}

local function afterimage(char, color, life)
	for _, p in char:GetDescendants() do
		if p:IsA("BasePart") and AFTERIMAGE_PARTS[p.Name] then
			local c = fxPart({ Size = p.Size, CFrame = p.CFrame, Color = color, Transparency = 0.45, Material = Enum.Material.Neon })
			local m = p:FindFirstChildOfClass("SpecialMesh")
			if m then
				m:Clone().Parent = c
			end
			tween(c, life, { Transparency = 1 })
			Debris:AddItem(c, life)
		end
	end
end

function VFX.Dash(d)
	local char = d.char
	local root = char and char:FindFirstChild("HumanoidRootPart")
	if not root then
		return
	end
	local dir = d.dir or root.CFrame.LookVector
	local feet = groundAt(root.Position)
	shockwave(feet + Vector3.new(0, 0.2, 0), Color3.fromRGB(200, 200, 210), 9, 0.35)
	local dust = anchorAt(feet, 2)
	local e = emitter(dust, {
		Texture = Config.Textures.Smoke,
		LightEmission = 0,
		Color = colorSeq(Color3.fromRGB(170, 165, 160)),
		Size = seq(0, 1.5, 1, 4),
		Transparency = seq(0, 0.5, 1, 1),
		Speed = NumberRange.new(6, 12),
		Lifetime = NumberRange.new(0.5, 0.9),
		SpreadAngle = Vector2.new(80, 10),
		EmissionDirection = Enum.NormalId.Top,
	})
	e:Emit(14)
	sound(Config.Sounds.Dash, root, 0.6, 1.3)
	task.spawn(function()
		for i = 1, 4 do
			if not char.Parent then
				break
			end
			afterimage(char, Color3.fromRGB(60, 20, 30):Lerp(Color3.fromRGB(180, 20, 40), i / 4), 0.35)
			-- rüzgar çizgileri
			for _ = 1, 2 do
				local o = root.Position + Vector3.new(rng:NextNumber(-1.5, 1.5), rng:NextNumber(-2, 2), rng:NextNumber(-1.5, 1.5))
				local w = fxPart({ Size = Vector3.new(0.06, 0.06, 4), Color = Color3.fromRGB(255, 255, 255), Transparency = 0.3, CFrame = CFrame.lookAt(o, o + dir) })
				tween(w, 0.25, { Transparency = 1, Size = Vector3.new(0.02, 0.02, 7) })
				Debris:AddItem(w, 0.25)
			end
			task.wait(0.05)
		end
	end)
end

-- ---------------------------------------------------------------- jutsu ismi

function VFX.JutsuName(d)
	local head = d.char and d.char:FindFirstChild("Head")
	if not head then
		return
	end
	billboardText(head.Position + Vector3.new(0, 3, 0), d.text, Color3.fromRGB(255, 70, 70), 1.4, 1.4, 1.5)
end

-- ---------------------------------------------------------------- 1) Katon: Gōkakyū no Jutsu

local fireballs = {}

local function makeFireball(radius)
	local m = Instance.new("Model")
	m.Name = "Fireball"
	local core = ball(Vector3.zero, radius * 0.9, Color3.fromRGB(255, 220, 120))
	core.Parent = m
	local shell = ball(Vector3.zero, radius * 1.4, Color3.fromRGB(255, 110, 20), { Transparency = 0.45 })
	shell.Parent = m
	local outer = ball(Vector3.zero, radius * 1.9, Color3.fromRGB(255, 60, 10), { Transparency = 0.75 })
	outer.Parent = m
	local fire = Instance.new("Fire")
	fire.Size = radius * 2.2
	fire.Heat = 20
	fire.Color = Color3.fromRGB(255, 140, 30)
	fire.SecondaryColor = Color3.fromRGB(255, 40, 0)
	fire.Parent = core
	emitter(core, {
		Rate = 160,
		Color = ColorSequence.new({ ColorSequenceKeypoint.new(0, Color3.fromRGB(255, 230, 140)), ColorSequenceKeypoint.new(0.5, Color3.fromRGB(255, 100, 20)), ColorSequenceKeypoint.new(1, Color3.fromRGB(120, 20, 0)) }),
		Size = seq(0, radius * 0.9, 1, radius * 0.2),
		Transparency = seq(0, 0.1, 1, 1),
		Lifetime = NumberRange.new(0.35, 0.6),
		Speed = NumberRange.new(2, 6),
		RotSpeed = NumberRange.new(-200, 200),
		Rotation = NumberRange.new(0, 360),
	})
	emitter(outer, {
		Texture = Config.Textures.Smoke,
		Rate = 40,
		LightEmission = 0,
		Color = colorSeq(Color3.fromRGB(60, 40, 35)),
		Size = seq(0, radius * 0.6, 1, radius * 1.6),
		Transparency = seq(0, 0.6, 1, 1),
		Lifetime = NumberRange.new(0.8, 1.2),
		Speed = NumberRange.new(1, 3),
	})
	local light = Instance.new("PointLight")
	light.Color = Color3.fromRGB(255, 140, 50)
	light.Range = 40
	light.Brightness = 4
	light.Shadows = true
	light.Parent = core
	m.PrimaryPart = core
	m.Parent = folder
	return m, { core, shell, outer }
end

function VFX.KatonStart(d)
	local origin, dir = d.origin, d.dir
	-- ağızdan çıkan alev püskürmesi
	local head = d.char and d.char:FindFirstChild("Head")
	if head then
		local nozzle = fxPart({ Size = Vector3.one * 0.2, Transparency = 1, CFrame = CFrame.lookAt(head.Position, head.Position + dir) })
		local e = emitter(nozzle, {
			Rate = 250,
			EmissionDirection = Enum.NormalId.Front,
			SpreadAngle = Vector2.new(14, 14),
			Speed = NumberRange.new(35, 55),
			Lifetime = NumberRange.new(0.25, 0.4),
			Size = seq(0, 0.8, 1, 4),
			Transparency = seq(0, 0.1, 1, 1),
			Color = ColorSequence.new(Color3.fromRGB(255, 220, 120), Color3.fromRGB(255, 60, 0)),
		})
		task.delay(0.3, function()
			e.Enabled = false
		end)
		Debris:AddItem(nozzle, 1)
	end
	sound(Config.Sounds.Fire, origin, 1, 0.8)

	local model, balls = makeFireball(d.radius or 3)
	local fb = { model = model, balls = balls, origin = origin, dir = dir, speed = d.speed, life = d.range / d.speed + 0.3, t0 = os.clock() }
	fireballs[d.id] = fb
	-- büyüyerek doğar
	for _, b in balls do
		local s = b.Size
		b.Size = s * 0.1
		tween(b, 0.25, { Size = s })
	end
end

RunService.RenderStepped:Connect(function()
	local now = os.clock()
	for id, fb in fireballs do
		local t = now - fb.t0
		if t > fb.life then
			fb.model:Destroy()
			fireballs[id] = nil
		else
			local pos = fb.origin + fb.dir * fb.speed * t
			local spin = CFrame.Angles(t * 7, t * 5, 0)
			for _, b in fb.balls do
				b.CFrame = CFrame.new(pos) * spin
			end
		end
	end
end)

function VFX.KatonExplode(d)
	local fb = fireballs[d.id]
	if fb then
		fb.model:Destroy()
		fireballs[d.id] = nil
	end
	local pos, r = d.pos, d.radius
	local core = ball(pos, 2, Color3.fromRGB(255, 240, 180))
	tween(core, 0.25, { Size = Vector3.one * r * 1.4, Transparency = 1 })
	Debris:AddItem(core, 0.3)
	local blast = ball(pos, 3, Color3.fromRGB(255, 120, 20), { Transparency = 0.15 })
	tween(blast, 0.45, { Size = Vector3.one * r * 2.2, Transparency = 1 })
	Debris:AddItem(blast, 0.5)
	local smoke = ball(pos, r, Color3.fromRGB(40, 30, 28), { Material = Enum.Material.SmoothPlastic, Transparency = 0.5 })
	tween(smoke, 1.2, { Size = Vector3.one * r * 2.6, Transparency = 1 })
	Debris:AddItem(smoke, 1.2)

	local ground = groundAt(pos)
	shockwave(ground + Vector3.new(0, 0.3, 0), Color3.fromRGB(255, 160, 60), r * 3, 0.5)
	shockwave(ground + Vector3.new(0, 0.4, 0), Color3.fromRGB(255, 255, 220), r * 1.8, 0.3)

	local a = anchorAt(pos, 3)
	emitter(a, {
		Color = ColorSequence.new(Color3.fromRGB(255, 220, 120), Color3.fromRGB(200, 40, 0)),
		Size = seq(0, 3, 1, 0.5),
		Transparency = seq(0, 0, 1, 1),
		Speed = NumberRange.new(20, 45),
		Lifetime = NumberRange.new(0.4, 0.9),
		Drag = 4,
		RotSpeed = NumberRange.new(-300, 300),
	}):Emit(70)
	emitter(a, {
		Texture = Config.Textures.Smoke,
		LightEmission = 0,
		Color = colorSeq(Color3.fromRGB(50, 42, 40)),
		Size = seq(0, 4, 1, 10),
		Transparency = seq(0, 0.3, 1, 1),
		Speed = NumberRange.new(8, 18),
		Lifetime = NumberRange.new(1.2, 2),
		Drag = 2,
	}):Emit(25)
	sparks(pos, 20, Color3.fromRGB(255, 190, 80), r * 1.6, 0.6, 1.8)

	local light = Instance.new("PointLight")
	light.Color = Color3.fromRGB(255, 150, 60)
	light.Range = 60
	light.Brightness = 8
	light.Parent = a
	tween(light, 0.8, { Brightness = 0 })

	-- yanık izi
	local scorch = fxPart({
		Shape = Enum.PartType.Cylinder,
		Material = Enum.Material.SmoothPlastic,
		Color = Color3.fromRGB(25, 18, 15),
		Size = Vector3.new(0.05, r * 1.6, r * 1.6),
		CFrame = CFrame.new(ground + Vector3.new(0, 0.06, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Transparency = 0.15,
	})
	task.delay(3, function()
		tween(scorch, 2, { Transparency = 1 })
	end)
	Debris:AddItem(scorch, 5.2)

	sound(Config.Sounds.Explosion, pos, 1.2, 0.7)
	shakeByDistance(pos, 1.3, 60)
end

-- ---------------------------------------------------------------- 2) Karasu Bunshin (kargalar)

local crows = {}

local function makeCrow()
	local black = Color3.fromRGB(10, 10, 14)
	local body = fxPart({ Size = Vector3.new(0.45, 0.4, 1.0), Color = black, Material = Enum.Material.SmoothPlastic })
	local head = fxPart({ Shape = Enum.PartType.Ball, Size = Vector3.one * 0.38, Color = black, Material = Enum.Material.SmoothPlastic })
	local beak = fxPart({ Size = Vector3.new(0.12, 0.1, 0.3), Color = Color3.fromRGB(40, 38, 30), Material = Enum.Material.SmoothPlastic })
	local eye = fxPart({ Shape = Enum.PartType.Ball, Size = Vector3.one * 0.1, Color = Color3.fromRGB(255, 20, 20) })
	local wl = fxPart({ Size = Vector3.new(1.3, 0.05, 0.55), Color = black, Material = Enum.Material.SmoothPlastic })
	local wr = fxPart({ Size = Vector3.new(1.3, 0.05, 0.55), Color = black, Material = Enum.Material.SmoothPlastic })
	local tail = fxPart({ Size = Vector3.new(0.4, 0.05, 0.5), Color = black, Material = Enum.Material.SmoothPlastic })
	return { body = body, head = head, beak = beak, eye = eye, wl = wl, wr = wr, tail = tail }
end

local function placeCrow(c, cf, flap)
	c.body.CFrame = cf
	c.head.CFrame = cf * CFrame.new(0, 0.15, -0.6)
	c.beak.CFrame = cf * CFrame.new(0, 0.12, -0.9)
	c.eye.CFrame = cf * CFrame.new(0.13, 0.22, -0.68)
	c.tail.CFrame = cf * CFrame.new(0, 0, 0.7)
	c.wr.CFrame = cf * CFrame.new(0.2, 0.1, -0.05) * CFrame.Angles(0, 0, flap) * CFrame.new(0.65, 0, 0)
	c.wl.CFrame = cf * CFrame.new(-0.2, 0.1, -0.05) * CFrame.Angles(0, 0, -flap) * CFrame.new(-0.65, 0, 0)
end

function VFX.CrowBurst(d)
	local pos = d.pos
	for _ = 1, d.count or 14 do
		local dir = (rng:NextUnitVector() * Vector3.new(1, 0.5, 1) + Vector3.new(0, 0.6, 0)).Unit
		local c = makeCrow()
		local item = {
			parts = c,
			pos = pos + Vector3.new(rng:NextNumber(-1, 1), rng:NextNumber(-1.5, 1.5), rng:NextNumber(-1, 1)),
			vel = dir * rng:NextNumber(14, 26),
			t0 = os.clock(),
			life = rng:NextNumber(0.9, 1.4),
			phase = rng:NextNumber(0, 6),
		}
		table.insert(crows, item)
	end
	-- siyah tüyler
	for _ = 1, 16 do
		local f = fxPart({ Size = Vector3.new(0.25, 0.03, 0.6), Color = Color3.fromRGB(12, 12, 16), Material = Enum.Material.SmoothPlastic })
		f.CFrame = CFrame.new(pos + rng:NextUnitVector() * 2) * CFrame.Angles(rng:NextNumber(0, 6), rng:NextNumber(0, 6), 0)
		tween(f, 1.6, { CFrame = f.CFrame * CFrame.Angles(4, 2, 0) + Vector3.new(rng:NextNumber(-3, 3), -4, rng:NextNumber(-3, 3)), Transparency = 1 }, Enum.EasingStyle.Sine)
		Debris:AddItem(f, 1.6)
	end
	local a = anchorAt(pos, 2)
	emitter(a, {
		Texture = Config.Textures.Smoke,
		LightEmission = 0,
		Color = colorSeq(Color3.fromRGB(15, 12, 18)),
		Size = seq(0, 2, 1, 6),
		Transparency = seq(0, 0.2, 1, 1),
		Speed = NumberRange.new(4, 10),
		Lifetime = NumberRange.new(0.5, 0.8),
	}):Emit(20)
	flash(pos, 5, Color3.fromRGB(120, 0, 10), 0.25)
	sound(Config.Sounds.Crow, pos, 0.8, 0.6)
end

RunService.RenderStepped:Connect(function(dt)
	local now = os.clock()
	for i = #crows, 1, -1 do
		local c = crows[i]
		local t = now - c.t0
		if t > c.life then
			for _, p in c.parts do
				p:Destroy()
			end
			table.remove(crows, i)
		else
			c.vel += Vector3.new(0, 4 * dt, 0)
			c.pos += c.vel * dt
			local flap = math.sin(t * 28 + c.phase) * 0.9
			placeCrow(c.parts, CFrame.lookAt(c.pos, c.pos + c.vel), flap)
			if t > c.life - 0.3 then
				local a = (t - (c.life - 0.3)) / 0.3
				for _, p in c.parts do
					p.Transparency = a
				end
			end
		end
	end
end)

-- ---------------------------------------------------------------- 3) Amaterasu (siyah alevler)

local function blackFlame(parent, size)
	local fire = Instance.new("Fire")
	fire.Color = Color3.fromRGB(0, 0, 0)
	fire.SecondaryColor = Color3.fromRGB(40, 0, 12)
	fire.Size = size
	fire.Heat = 18
	fire.Parent = parent
	local e = emitter(parent, {
		Rate = 45,
		LightEmission = 0,
		LightInfluence = 0,
		Color = ColorSequence.new({
			ColorSequenceKeypoint.new(0, Color3.fromRGB(0, 0, 0)),
			ColorSequenceKeypoint.new(0.7, Color3.fromRGB(25, 0, 12)),
			ColorSequenceKeypoint.new(1, Color3.fromRGB(70, 0, 25)),
		}),
		Size = seq(0, size * 0.25, 0.5, size * 0.35, 1, 0),
		Transparency = seq(0, 0.15, 0.8, 0.4, 1, 1),
		EmissionDirection = Enum.NormalId.Top,
		SpreadAngle = Vector2.new(18, 18),
		Speed = NumberRange.new(4, 9),
		Lifetime = NumberRange.new(0.5, 0.9),
		Rotation = NumberRange.new(0, 360),
		RotSpeed = NumberRange.new(-120, 120),
	})
	return fire, e
end

function VFX.AmaterasuCast(d)
	local head = d.char and d.char:FindFirstChild("Head")
	if not head then
		return
	end
	local eyePos = (head.CFrame * CFrame.new(-0.3, 0.05, -0.62)).Position
	flash(eyePos, 1.6, Color3.fromRGB(255, 0, 20), 0.35)
	sparks(eyePos, 6, Color3.fromRGB(255, 20, 30), 3, 0.3, 0.6)
	sound(Config.Sounds.Amaterasu, head, 0.8, 0.45)
	if d.target then
		-- gözden hedefe bir an siyah-kırmızı bakış çizgisi
		local p0, p1 = eyePos, d.target
		local len = (p1 - p0).Magnitude
		local beam = fxPart({ Size = Vector3.new(0.12, 0.12, len), Color = Color3.fromRGB(200, 0, 20), Transparency = 0.2, CFrame = CFrame.lookAt(p0, p1) * CFrame.new(0, 0, -len / 2) })
		tween(beam, 0.35, { Transparency = 1, Size = Vector3.new(0.02, 0.02, len) })
		Debris:AddItem(beam, 0.35)
	end
end

function VFX.AmaterasuZone(d)
	local center = groundAt(d.pos)
	local r = d.radius
	local dur = d.duration

	local disk = fxPart({
		Shape = Enum.PartType.Cylinder,
		Material = Enum.Material.SmoothPlastic,
		Color = Color3.fromRGB(8, 0, 4),
		Size = Vector3.new(0.06, 1, 1),
		CFrame = CFrame.new(center + Vector3.new(0, 0.07, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Transparency = 0.1,
	})
	tween(disk, 0.3, { Size = Vector3.new(0.06, r * 2, r * 2) })
	local rim = fxPart({
		Shape = Enum.PartType.Cylinder,
		Color = Color3.fromRGB(120, 0, 30),
		Size = Vector3.new(0.04, 1, 1),
		CFrame = CFrame.new(center + Vector3.new(0, 0.05, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Transparency = 0.3,
	})
	tween(rim, 0.3, { Size = Vector3.new(0.04, r * 2.15, r * 2.15) })

	local holders = {}
	local count = 9
	for i = 1, count do
		local a = (i / count) * math.pi * 2 + rng:NextNumber(-0.3, 0.3)
		local dist = i == 1 and 0 or rng:NextNumber(r * 0.3, r * 0.85)
		local p = center + Vector3.new(math.cos(a) * dist, 0.5, math.sin(a) * dist)
		local h = anchorAt(p, dur + 2)
		blackFlame(h, rng:NextNumber(5, 9))
		table.insert(holders, h)
	end
	local glow = anchorAt(center + Vector3.new(0, 2, 0), dur + 2)
	local light = Instance.new("PointLight")
	light.Color = Color3.fromRGB(150, 0, 60)
	light.Range = r * 3
	light.Brightness = 3
	light.Parent = glow

	-- başlangıç patlaması
	flash(center + Vector3.new(0, 1, 0), r * 1.5, Color3.fromRGB(20, 0, 8), 0.4)
	shockwave(center + Vector3.new(0, 0.2, 0), Color3.fromRGB(90, 0, 30), r * 3, 0.45)
	sound(Config.Sounds.Amaterasu, center, 1, 0.35)
	shakeByDistance(center, 0.5, 40)

	task.delay(dur, function()
		for _, h in holders do
			for _, c in h:GetChildren() do
				if c:IsA("Fire") then
					c.Enabled = false
				elseif c:IsA("ParticleEmitter") then
					c.Enabled = false
				end
			end
		end
		tween(light, 1, { Brightness = 0 })
		tween(disk, 1.5, { Transparency = 1 })
		tween(rim, 1, { Transparency = 1 })
		Debris:AddItem(disk, 1.6)
		Debris:AddItem(rim, 1.1)
	end)
end

-- Bir karakterin üzerinde yanan alev (normal ateş veya Amaterasu)
function VFX.Burn(d)
	local char = d.char
	local torso = char and char:FindFirstChild("Torso")
	if not torso then
		return
	end
	local fire, e
	if d.black then
		fire, e = blackFlame(torso, 6)
	else
		fire = Instance.new("Fire")
		fire.Size = 5
		fire.Heat = 12
		fire.Color = Color3.fromRGB(255, 140, 40)
		fire.SecondaryColor = Color3.fromRGB(255, 40, 0)
		fire.Parent = torso
	end
	task.delay(d.duration, function()
		if fire then
			fire.Enabled = false
			Debris:AddItem(fire, 1)
		end
		if e then
			e.Enabled = false
			Debris:AddItem(e, 1)
		end
	end)
end

-- ---------------------------------------------------------------- 4) Tsukuyomi

local function mangekyoSymbol(parent, size)
	local holder = Instance.new("Frame")
	holder.BackgroundColor3 = Color3.fromRGB(200, 0, 15)
	holder.BorderSizePixel = 0
	holder.AnchorPoint = Vector2.new(0.5, 0.5)
	holder.Position = UDim2.fromScale(0.5, 0.5)
	holder.Size = UDim2.fromOffset(size, size)
	holder.Parent = parent
	local c = Instance.new("UICorner")
	c.CornerRadius = UDim.new(0.5, 0)
	c.Parent = holder
	local st = Instance.new("UIStroke")
	st.Thickness = size * 0.03
	st.Color = Color3.new(0, 0, 0)
	st.Parent = holder
	for i = 0, 2 do
		local blade = Instance.new("Frame")
		blade.BackgroundColor3 = Color3.new(0, 0, 0)
		blade.BorderSizePixel = 0
		blade.AnchorPoint = Vector2.new(0, 0.5)
		blade.Position = UDim2.fromScale(0.5, 0.5)
		blade.Size = UDim2.fromOffset(size * 0.47, size * 0.16)
		blade.Rotation = i * 120 + 20
		blade.Parent = holder
		local bc = Instance.new("UICorner")
		bc.CornerRadius = UDim.new(0.5, 0)
		bc.Parent = blade
	end
	local core = Instance.new("Frame")
	core.BackgroundColor3 = Color3.new(0, 0, 0)
	core.AnchorPoint = Vector2.new(0.5, 0.5)
	core.Position = UDim2.fromScale(0.5, 0.5)
	core.Size = UDim2.fromOffset(size * 0.3, size * 0.3)
	core.Parent = holder
	local cc = Instance.new("UICorner")
	cc.CornerRadius = UDim.new(0.5, 0)
	cc.Parent = core
	return holder
end

local function tsukuyomiScreen(duration)
	local gui = Instance.new("ScreenGui")
	gui.Name = "Tsukuyomi"
	gui.IgnoreGuiInset = true
	gui.DisplayOrder = 50
	gui.ResetOnSpawn = false
	gui.Parent = player:WaitForChild("PlayerGui")

	-- giriş: dev Mangekyō
	local sym = mangekyoSymbol(gui, 40)
	tween(sym, 0.35, { Size = UDim2.fromOffset(1400, 1400), Rotation = 220 }, Enum.EasingStyle.Quad, Enum.EasingDirection.In)

	local cc = Instance.new("ColorCorrectionEffect")
	cc.Name = "TsukuyomiCC"
	cc.Parent = Lighting
	task.delay(0.35, function()
		sym:Destroy()
		cc.Saturation = -1
		cc.Contrast = 0.6
		cc.Brightness = -0.1
		cc.TintColor = Color3.fromRGB(255, 70, 70)

		local tint = Instance.new("Frame")
		tint.BackgroundColor3 = Color3.fromRGB(90, 0, 0)
		tint.BackgroundTransparency = 0.45
		tint.Size = UDim2.fromScale(1, 1)
		tint.BorderSizePixel = 0
		tint.Parent = gui
		-- kızıl ay
		local moon = Instance.new("Frame")
		moon.BackgroundColor3 = Color3.fromRGB(230, 20, 20)
		moon.AnchorPoint = Vector2.new(0.5, 0.5)
		moon.Position = UDim2.fromScale(0.5, 0.25)
		moon.Size = UDim2.fromScale(0.22, 0.22)
		moon.Parent = gui
		local ar = Instance.new("UIAspectRatioConstraint")
		ar.Parent = moon
		local mc = Instance.new("UICorner")
		mc.CornerRadius = UDim.new(0.5, 0)
		mc.Parent = moon
		local label = Instance.new("TextLabel")
		label.BackgroundTransparency = 1
		label.AnchorPoint = Vector2.new(0.5, 0.5)
		label.Position = UDim2.fromScale(0.5, 0.75)
		label.Size = UDim2.fromScale(0.6, 0.12)
		label.Text = "月読  ·  TSUKUYOMI"
		label.TextScaled = true
		label.Font = Enum.Font.GothamBlack
		label.TextColor3 = Color3.fromRGB(255, 255, 255)
		label.TextStrokeTransparency = 0
		label.Parent = gui
		tween(moon, duration, { Size = UDim2.fromScale(0.3, 0.3) }, Enum.EasingStyle.Linear)

		task.delay(math.max(duration - 0.5, 0.1), function()
			tween(tint, 0.4, { BackgroundTransparency = 1 })
			tween(moon, 0.4, { BackgroundTransparency = 1 })
			tween(label, 0.4, { TextTransparency = 1, TextStrokeTransparency = 1 })
			tween(cc, 0.5, { Saturation = 0, Contrast = 0, Brightness = 0, TintColor = Color3.new(1, 1, 1) })
			task.wait(0.55)
			cc:Destroy()
			gui:Destroy()
		end)
	end)
end

function VFX.TsukuyomiCast(d)
	local caster, victim = d.caster, d.victim
	local cHead = caster and caster:FindFirstChild("Head")
	local vHead = victim and victim:FindFirstChild("Head")
	if cHead then
		flash(cHead.Position, 3, Color3.fromRGB(255, 0, 20), 0.4)
		sound(Config.Sounds.Tsukuyomi, cHead, 1, 0.5)
	end
	if vHead then
		local dome = ball(vHead.Position, 2, Color3.fromRGB(120, 0, 10), { Transparency = 0.3, Material = Enum.Material.ForceField })
		tween(dome, 0.6, { Size = Vector3.one * 16, Transparency = 1 })
		Debris:AddItem(dome, 0.6)
		-- kurbanın üstünde dönen Mangekyō
		local bb = Instance.new("BillboardGui")
		bb.Size = UDim2.fromScale(5, 5)
		bb.StudsOffset = Vector3.new(0, 3.5, 0)
		bb.AlwaysOnTop = true
		bb.LightInfluence = 0
		bb.Adornee = vHead
		bb.Parent = vHead
		local sym = mangekyoSymbol(bb, 80)
		sym.Size = UDim2.fromScale(1, 1)
		tween(sym, d.duration, { Rotation = 720 }, Enum.EasingStyle.Linear)
		Debris:AddItem(bb, d.duration)
	end
	if victim and victim == player.Character then
		tsukuyomiScreen(d.duration)
	elseif caster == player.Character then
		VFX.Shake(0.3, 0.3)
	end
end

-- ---------------------------------------------------------------- yüz efektleri (istemci)

function VFX.Eyes(d)
	if d.char then
		ItachiBuilder.SetEyes(d.char, d.mode)
	end
end

return VFX
