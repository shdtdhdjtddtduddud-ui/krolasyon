-- İstemci: girdi (klavye/fare/mobil), dash fiziği, HUD (can, posture, yetenek
-- cooldown'ları) ve sunucudan gelen efektlerin oynatılması.
--
-- Kontroller:
--   Sol tık  : M1 kombo (4 vuruş)
--   F (basılı): Blok   (doğru zamanlama = PARRY)
--   Q        : Dash (W/A/S/D yönüne)
--   1 2 3 4  : Katon / Karasu Bunshin / Amaterasu / Tsukuyomi

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local UserInputService = game:GetService("UserInputService")
local ContextActionService = game:GetService("ContextActionService")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")

local Shared = ReplicatedStorage:WaitForChild("Shared")
local Config = require(Shared:WaitForChild("Config"))
local VFX = require(Shared:WaitForChild("VFX"))

local Remotes = ReplicatedStorage:WaitForChild("Remotes")
local ActionRemote = Remotes:WaitForChild("Action")
local VFXRemote = Remotes:WaitForChild("VFX")
local CooldownRemote = Remotes:WaitForChild("Cooldown")

local player = Players.LocalPlayer
local camera = workspace.CurrentCamera

-- ---------------------------------------------------------------- efektler

VFXRemote.OnClientEvent:Connect(function(name, data)
	local fn = VFX[name]
	if fn then
		local ok, err = pcall(fn, data)
		if not ok then
			warn("[VFX] " .. tostring(name) .. ": " .. tostring(err))
		end
	end
end)

-- ---------------------------------------------------------------- yardımcılar

local function character()
	return player.Character
end

local function rootPart()
	local c = character()
	return c and c:FindFirstChild("HumanoidRootPart")
end

local function alive()
	local c = character()
	local h = c and c:FindFirstChildOfClass("Humanoid")
	return h ~= nil and h.Health > 0
end

local function stunned()
	local c = character()
	return c ~= nil and (c:GetAttribute("StunEnd") or 0) > workspace:GetServerTimeNow()
end

local function aim()
	local mousePos = UserInputService:GetMouseLocation()
	local ray = camera:ViewportPointToRay(mousePos.X, mousePos.Y)
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	local exclude = {}
	if character() then
		table.insert(exclude, character())
	end
	local fx = workspace:FindFirstChild("ClientVFX")
	if fx then
		table.insert(exclude, fx)
	end
	params.FilterDescendantsInstances = exclude
	local hit = workspace:Raycast(ray.Origin, ray.Direction * 500, params)
	local pos = hit and hit.Position or (ray.Origin + ray.Direction * 120)
	local root = rootPart()
	local dir = root and (pos - root.Position) or camera.CFrame.LookVector
	-- dokunmatik cihazlarda fare yok: kameranın baktığı yön
	if UserInputService.TouchEnabled and not UserInputService.MouseEnabled then
		dir = camera.CFrame.LookVector
		pos = (root and root.Position or camera.CFrame.Position) + dir * 40
	end
	return pos, dir
end

local function faceAim(pos)
	local root = rootPart()
	if not root then
		return
	end
	local flat = Vector3.new(pos.X, root.Position.Y, pos.Z)
	if (flat - root.Position).Magnitude > 0.5 then
		root.CFrame = CFrame.lookAt(root.Position, flat)
	end
end

-- yerel olarak hemen poz başlat (sunucu onayı gelmeden önce tepki hissi için)
local function localPose(action)
	local c = character()
	if c then
		c:SetAttribute("Action", action)
		c:SetAttribute("ActionStart", workspace:GetServerTimeNow())
	end
end

-- ---------------------------------------------------------------- cooldown takibi

local cooldownEnds = {}
local cooldownLengths = {}

CooldownRemote.OnClientEvent:Connect(function(name, duration)
	cooldownEnds[name] = os.clock() + duration
	cooldownLengths[name] = duration
end)

local function onCooldown(name)
	return (cooldownEnds[name] or 0) > os.clock()
end

-- ---------------------------------------------------------------- dash

local function dash()
	local c = character()
	local root = rootPart()
	local hum = c and c:FindFirstChildOfClass("Humanoid")
	if not (root and hum and alive()) or stunned() or onCooldown("Dash") then
		return
	end
	cooldownEnds.Dash = os.clock() + Config.Dash.Cooldown
	cooldownLengths.Dash = Config.Dash.Cooldown

	local move = hum.MoveDirection
	local look = root.CFrame.LookVector
	local right = root.CFrame.RightVector
	local dirName, vec = "F", Vector3.new(look.X, 0, look.Z).Unit
	if move.Magnitude > 0.1 then
		local f, r = move:Dot(look), move:Dot(right)
		if math.abs(f) >= math.abs(r) then
			dirName = f >= 0 and "F" or "B"
		else
			dirName = r >= 0 and "R" or "L"
		end
		vec = Vector3.new(move.X, 0, move.Z).Unit
	end

	local att = Instance.new("Attachment")
	att.Parent = root
	local lv = Instance.new("LinearVelocity")
	lv.Attachment0 = att
	lv.MaxForce = 1e6
	lv.VelocityConstraintMode = Enum.VelocityConstraintMode.Plane
	lv.PrimaryTangentAxis = Vector3.xAxis
	lv.SecondaryTangentAxis = Vector3.zAxis
	lv.PlaneVelocity = Vector2.new(vec.X, vec.Z) * Config.Dash.Speed
	lv.RelativeTo = Enum.ActuatorRelativeTo.World
	lv.Parent = root
	task.delay(Config.Dash.Duration, function()
		lv:Destroy()
		att:Destroy()
	end)

	localPose("Dash" .. dirName)
	VFX.Dash({ char = c, dir = vec })
	ActionRemote:FireServer("Dash", { dir = dirName, vec = vec })
end

-- ---------------------------------------------------------------- aksiyonlar

local blocking = false

local function m1()
	if not alive() or stunned() or blocking then
		return
	end
	ActionRemote:FireServer("M1")
end

local function setBlock(on)
	if on == blocking then
		return
	end
	blocking = on
	if on and (not alive() or stunned()) then
		blocking = false
		return
	end
	ActionRemote:FireServer(on and "BlockStart" or "BlockEnd")
end

local function ability(index)
	local info = Config.Abilities[index]
	if not info or not alive() or stunned() or blocking or onCooldown(info.Id) then
		return
	end
	local pos, dir = aim()
	faceAim(pos)
	ActionRemote:FireServer("Ability", { id = info.Id, aim = pos, dir = dir })
end

-- ---------------------------------------------------------------- girdi bağlama

local KEY_TO_ABILITY = {
	[Enum.KeyCode.One] = 1,
	[Enum.KeyCode.Two] = 2,
	[Enum.KeyCode.Three] = 3,
	[Enum.KeyCode.Four] = 4,
}

ContextActionService:BindAction("CombatM1", function(_, state)
	if state == Enum.UserInputState.Begin then
		m1()
	end
	return Enum.ContextActionResult.Pass
end, true, Enum.UserInputType.MouseButton1, Enum.KeyCode.ButtonR2)

ContextActionService:BindAction("CombatBlock", function(_, state)
	if state == Enum.UserInputState.Begin then
		setBlock(true)
	elseif state == Enum.UserInputState.End or state == Enum.UserInputState.Cancel then
		setBlock(false)
	end
	return Enum.ContextActionResult.Sink
end, true, Enum.KeyCode.F, Enum.KeyCode.ButtonL2)

ContextActionService:BindAction("CombatDash", function(_, state)
	if state == Enum.UserInputState.Begin then
		dash()
	end
	return Enum.ContextActionResult.Sink
end, true, Enum.KeyCode.Q, Enum.KeyCode.ButtonB)

for key, index in KEY_TO_ABILITY do
	ContextActionService:BindAction("CombatAbility" .. index, function(_, state)
		if state == Enum.UserInputState.Begin then
			ability(index)
		end
		return Enum.ContextActionResult.Sink
	end, true, key)
end

-- mobil buton etiketleri
task.defer(function()
	ContextActionService:SetTitle("CombatM1", "M1")
	ContextActionService:SetTitle("CombatBlock", "BLOK")
	ContextActionService:SetTitle("CombatDash", "DASH")
	for i in Config.Abilities do
		ContextActionService:SetTitle("CombatAbility" .. i, tostring(i))
		ContextActionService:SetPosition("CombatAbility" .. i, UDim2.new(1, -70 - (i - 1) * 60, 0, 20))
	end
end)

-- sersemleyince bloğu bırak
RunService.Heartbeat:Connect(function()
	if blocking and (stunned() or not alive()) then
		blocking = false
	end
end)

-- ---------------------------------------------------------------- HUD

local gui = Instance.new("ScreenGui")
gui.Name = "CombatHUD"
gui.ResetOnSpawn = false
gui.IgnoreGuiInset = false
gui.Parent = player:WaitForChild("PlayerGui")

local function make(class, props, parent)
	local i = Instance.new(class)
	for k, v in props do
		(i :: any)[k] = v
	end
	i.Parent = parent
	return i
end

local function corner(p, r)
	make("UICorner", { CornerRadius = UDim.new(0, r or 6) }, p)
end

local root = make("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 1),
	Position = UDim2.new(0.5, 0, 1, -18),
	Size = UDim2.fromOffset(440, 140),
}, gui)

-- isim
make("TextLabel", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 0),
	Size = UDim2.new(1, 0, 0, 20),
	Text = "ITACHI UCHIHA",
	Font = Enum.Font.GothamBlack,
	TextSize = 16,
	TextXAlignment = Enum.TextXAlignment.Left,
	TextColor3 = Color3.fromRGB(255, 255, 255),
	TextStrokeTransparency = 0.5,
}, root)

-- can barı
local hpBack = make("Frame", { BackgroundColor3 = Color3.fromRGB(25, 20, 22), Position = UDim2.fromOffset(0, 22), Size = UDim2.new(1, 0, 0, 18), BorderSizePixel = 0 }, root)
corner(hpBack, 5)
make("UIStroke", { Color = Color3.fromRGB(0, 0, 0), Thickness = 1.5 }, hpBack)
local hpLag = make("Frame", { BackgroundColor3 = Color3.fromRGB(255, 255, 255), Size = UDim2.fromScale(1, 1), BorderSizePixel = 0 }, hpBack)
corner(hpLag, 5)
local hpFill = make("Frame", { BackgroundColor3 = Color3.fromRGB(200, 25, 40), Size = UDim2.fromScale(1, 1), BorderSizePixel = 0 }, hpBack)
corner(hpFill, 5)
make("UIGradient", { Color = ColorSequence.new(Color3.fromRGB(255, 80, 90), Color3.fromRGB(150, 10, 25)), Rotation = 90 }, hpFill)
local hpText = make("TextLabel", { BackgroundTransparency = 1, Size = UDim2.fromScale(1, 1), Font = Enum.Font.GothamBold, TextSize = 13, TextColor3 = Color3.new(1, 1, 1), TextStrokeTransparency = 0.4, Text = "100 / 100", ZIndex = 3 }, hpBack)

-- posture barı
local postBack = make("Frame", { BackgroundColor3 = Color3.fromRGB(25, 25, 30), Position = UDim2.fromOffset(0, 43), Size = UDim2.new(0.6, 0, 0, 7), BorderSizePixel = 0 }, root)
corner(postBack, 3)
local postFill = make("Frame", { BackgroundColor3 = Color3.fromRGB(120, 190, 255), Size = UDim2.fromScale(1, 1), BorderSizePixel = 0 }, postBack)
corner(postFill, 3)
make("TextLabel", { BackgroundTransparency = 1, Position = UDim2.new(0.6, 6, 0, 39), Size = UDim2.fromOffset(120, 14), Text = "POSTURE", Font = Enum.Font.GothamBold, TextSize = 11, TextXAlignment = Enum.TextXAlignment.Left, TextColor3 = Color3.fromRGB(170, 200, 230) }, root)

-- yetenek slotları
local slots = {}
local slotRow = make("Frame", { BackgroundTransparency = 1, Position = UDim2.fromOffset(0, 58), Size = UDim2.new(1, 0, 0, 80) }, root)
make("UIListLayout", { FillDirection = Enum.FillDirection.Horizontal, Padding = UDim.new(0, 8), HorizontalAlignment = Enum.HorizontalAlignment.Center }, slotRow)

local function slot(key, title, id, color)
	local f = make("Frame", { BackgroundColor3 = Color3.fromRGB(18, 16, 20), BackgroundTransparency = 0.1, Size = UDim2.fromOffset(80, 80), BorderSizePixel = 0 }, slotRow)
	corner(f, 8)
	make("UIStroke", { Color = color, Thickness = 2, Transparency = 0.2 }, f)
	make("TextLabel", { BackgroundTransparency = 1, Position = UDim2.fromOffset(6, 4), Size = UDim2.fromOffset(20, 16), Text = key, Font = Enum.Font.GothamBlack, TextSize = 14, TextXAlignment = Enum.TextXAlignment.Left, TextColor3 = color }, f)
	make("TextLabel", {
		BackgroundTransparency = 1,
		Position = UDim2.new(0, 4, 0, 22),
		Size = UDim2.new(1, -8, 1, -26),
		Text = title,
		TextWrapped = true,
		Font = Enum.Font.GothamBold,
		TextSize = 12,
		TextColor3 = Color3.fromRGB(240, 235, 235),
	}, f)
	local shade = make("Frame", { BackgroundColor3 = Color3.new(0, 0, 0), BackgroundTransparency = 0.35, AnchorPoint = Vector2.new(0, 1), Position = UDim2.fromScale(0, 1), Size = UDim2.fromScale(1, 0), BorderSizePixel = 0, ZIndex = 3 }, f)
	corner(shade, 8)
	local cdText = make("TextLabel", { BackgroundTransparency = 1, Size = UDim2.fromScale(1, 1), Text = "", Font = Enum.Font.GothamBlack, TextSize = 22, TextColor3 = Color3.new(1, 1, 1), TextStrokeTransparency = 0.3, ZIndex = 4 }, f)
	slots[id] = { frame = f, shade = shade, text = cdText, ready = true }
end

local SLOT_COLORS = {
	Color3.fromRGB(255, 140, 40),
	Color3.fromRGB(150, 150, 170),
	Color3.fromRGB(170, 60, 255),
	Color3.fromRGB(230, 30, 40),
}
for i, info in Config.Abilities do
	slot(tostring(i), info.Name, info.Id, SLOT_COLORS[i] or Color3.new(1, 1, 1))
end
slot("Q", "Dash", "Dash", Color3.fromRGB(120, 200, 255))

-- kontrol ipucu
make("TextLabel", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, -12, 1, -12),
	Size = UDim2.fromOffset(260, 70),
	Text = "LMB: Kombo   F: Blok/Parry\nQ: Dash   1-4: Jutsu\nShift: Shift Lock",
	TextXAlignment = Enum.TextXAlignment.Right,
	TextYAlignment = Enum.TextYAlignment.Bottom,
	Font = Enum.Font.GothamMedium,
	TextSize = 13,
	TextColor3 = Color3.fromRGB(220, 210, 210),
	TextStrokeTransparency = 0.5,
}, gui)

local lastHealth = 100
RunService.RenderStepped:Connect(function()
	local c = character()
	local hum = c and c:FindFirstChildOfClass("Humanoid")
	if hum then
		local ratio = math.clamp(hum.Health / hum.MaxHealth, 0, 1)
		hpFill.Size = UDim2.fromScale(ratio, 1)
		hpText.Text = string.format("%d / %d", math.ceil(hum.Health), hum.MaxHealth)
		if hum.Health < lastHealth then
			TweenService:Create(hpLag, TweenInfo.new(0.6, Enum.EasingStyle.Quad, Enum.EasingDirection.Out, 0, false, 0.25), { Size = UDim2.fromScale(ratio, 1) }):Play()
		elseif hum.Health > lastHealth then
			hpLag.Size = UDim2.fromScale(ratio, 1)
		end
		lastHealth = hum.Health
		local posture = c:GetAttribute("Posture") or Config.Block.MaxPosture
		postFill.Size = UDim2.fromScale(math.clamp(posture / Config.Block.MaxPosture, 0, 1), 1)
		postFill.BackgroundColor3 = posture < Config.Block.MaxPosture * 0.3 and Color3.fromRGB(255, 90, 90) or Color3.fromRGB(120, 190, 255)
	end

	local t = os.clock()
	for id, s in slots do
		local remaining = (cooldownEnds[id] or 0) - t
		if remaining > 0 then
			local len = cooldownLengths[id] or 1
			s.shade.Size = UDim2.fromScale(1, math.clamp(remaining / len, 0, 1))
			s.text.Text = remaining >= 1 and string.format("%d", math.ceil(remaining)) or string.format("%.1f", remaining)
			s.ready = false
		elseif not s.ready then
			s.ready = true
			s.shade.Size = UDim2.fromScale(1, 0)
			s.text.Text = ""
			-- hazır olunca parlama
			local glow = make("Frame", { BackgroundColor3 = Color3.new(1, 1, 1), BackgroundTransparency = 0.3, Size = UDim2.fromScale(1, 1), ZIndex = 5 }, s.frame)
			corner(glow, 8)
			TweenService:Create(glow, TweenInfo.new(0.35), { BackgroundTransparency = 1 }):Play()
			task.delay(0.4, function()
				glow:Destroy()
			end)
		end
	end
end)

player.CharacterAdded:Connect(function()
	blocking = false
	lastHealth = 100
	table.clear(cooldownEnds)
end)
