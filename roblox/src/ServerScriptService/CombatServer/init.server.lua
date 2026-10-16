-- Ana sunucu betiği: karakteri kurar, RemoteEvent'leri oluşturur, girdileri işler,
-- antrenman dummy'lerini ve sparring botunu çalıştırır.

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local StarterPlayer = game:GetService("StarterPlayer")

local Shared = ReplicatedStorage:WaitForChild("Shared")
local Config = require(Shared.Config)
local ItachiBuilder = require(Shared.ItachiBuilder)
local Core = require(script.CombatCore)
local Kit = require(script.ItachiKit)

-- ---------------------------------------------------------------- remotes

local remotesFolder = Instance.new("Folder")
remotesFolder.Name = "Remotes"
local function remote(name)
	local r = Instance.new("RemoteEvent")
	r.Name = name
	r.Parent = remotesFolder
	return r
end
Core.Remotes = {
	Action = remote("Action"),
	VFX = remote("VFX"),
	Cooldown = remote("Cooldown"),
}
remotesFolder.Parent = ReplicatedStorage

-- ---------------------------------------------------------------- oyuncu karakteri

local old = StarterPlayer:FindFirstChild("StarterCharacter")
if old then
	old:Destroy()
end
local starter = ItachiBuilder.Build("StarterCharacter")
starter.Parent = StarterPlayer

local function onCharacter(char)
	char:WaitForChild("HumanoidRootPart")
	Core.Register(char)
end

local function onPlayer(player)
	player.CharacterAdded:Connect(onCharacter)
	if player.Character then
		if player.Character:GetAttribute("CharacterKit") ~= "Itachi" then
			player:LoadCharacter()
		else
			onCharacter(player.Character)
		end
	end
end
Players.PlayerAdded:Connect(onPlayer)
for _, p in Players:GetPlayers() do
	task.spawn(onPlayer, p)
end

-- ---------------------------------------------------------------- girdi

local DASH_ACTIONS = { F = "DashF", B = "DashB", L = "DashL", R = "DashR" }

local function useAbility(char, id, aim, aimDir)
	local fn = Kit[id]
	local info = Kit.Info[id]
	if not fn or not info then
		return
	end
	if not Core.CanAct(char) or char:GetAttribute("Blocking") or Core.OnCooldown(char, id) then
		return
	end
	fn(char, aim, aimDir)
end

Core.Remotes.Action.OnServerEvent:Connect(function(player, action, payload)
	local char = player.Character
	if not char or not Core.State(char) or typeof(action) ~= "string" then
		return
	end
	if payload ~= nil and typeof(payload) ~= "table" then
		return
	end
	payload = payload or {}

	if action == "M1" then
		Core.M1(char)
	elseif action == "BlockStart" then
		if Core.CanAct(char) then
			Core.SetBlocking(char, true)
		end
	elseif action == "BlockEnd" then
		Core.SetBlocking(char, false)
	elseif action == "Dash" then
		local dirName = DASH_ACTIONS[payload.dir] and payload.dir or "F"
		if Core.Alive(char) and not Core.IsStunned(char) and not Core.OnCooldown(char, "Dash") then
			Core.SetCooldown(char, "Dash", Config.Dash.Cooldown - 0.1)
			Core.SetBlocking(char, false)
			Core.SetIFrames(char, Config.Dash.IFrames)
			Core.SetAction(char, DASH_ACTIONS[dirName], nil)
			local vec = typeof(payload.vec) == "Vector3" and payload.vec or nil
			Core.FXExcept(player, "Dash", { char = char, dir = vec })
		end
	elseif action == "Ability" then
		local aim = payload.aim
		local aimDir = payload.dir
		if typeof(aim) ~= "Vector3" or typeof(payload.id) ~= "string" then
			return
		end
		if typeof(aimDir) ~= "Vector3" or aimDir.Magnitude < 0.01 then
			aimDir = nil
		else
			aimDir = aimDir.Unit
		end
		useAbility(char, payload.id, aim, aimDir)
	end
end)

-- ---------------------------------------------------------------- dummy'ler

local npcFolder = Instance.new("Folder")
npcFolder.Name = "NPCs"
npcFolder.Parent = workspace

local function spawnNPC(name, cf, behavior)
	local model = ItachiBuilder.Build(name)
	model:PivotTo(cf)
	model.Parent = npcFolder
	local hum = model:FindFirstChildOfClass("Humanoid")
	hum.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.Viewer
	hum.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOn
	hum.NameDisplayDistance = 60
	hum.HealthDisplayDistance = 60
	model.HumanoidRootPart:SetNetworkOwner(nil)
	local state = Core.Register(model)

	hum.Died:Connect(function()
		task.wait(Config.Respawn.DummyRespawn)
		model:Destroy()
		spawnNPC(name, cf, behavior)
	end)

	-- iyileşme
	task.spawn(function()
		while model.Parent and hum.Health > 0 do
			if Core.Now() - state.lastDamaged > Config.Respawn.DummyRegenDelay and hum.Health < hum.MaxHealth then
				hum.Health = math.min(hum.MaxHealth, hum.Health + 8)
			end
			task.wait(0.25)
		end
	end)

	if behavior then
		task.spawn(behavior, model, state)
	end
	return model
end

-- Sürekli blok tutan dummy (parry yapmaz)
local function blockerBehavior(model, state)
	local hum = model:FindFirstChildOfClass("Humanoid")
	while model.Parent and hum.Health > 0 do
		if not Core.IsStunned(model) and not model:GetAttribute("Blocking") then
			Core.SetBlocking(model, true)
			state.blockStart = -math.huge
		end
		task.wait(0.2)
	end
end

-- Basit yapay zeka: yaklaşır, kombo atar, bloklar, yetenek kullanır
local function botBehavior(model, state)
	local hum = model:FindFirstChildOfClass("Humanoid")
	local root = model:FindFirstChild("HumanoidRootPart")
	local rng = Random.new()
	local blockUntil = 0
	local home = root.Position

	local function nearestPlayer()
		local best, bestDist = nil, 90
		for _, p in Players:GetPlayers() do
			local c = p.Character
			if c and Core.Alive(c) then
				local d = (Core.Root(c).Position - root.Position).Magnitude
				if d < bestDist then
					best, bestDist = c, d
				end
			end
		end
		return best, bestDist
	end

	while model.Parent and hum.Health > 0 do
		task.wait(0.12)
		if Core.IsStunned(model) then
			continue
		end
		local target, dist = nearestPlayer()
		if not target then
			if (root.Position - home).Magnitude > 4 then
				hum:MoveTo(home)
			end
			continue
		end
		local tRoot = Core.Root(target)
		local tPos = tRoot.Position
		local toTarget = (tPos - root.Position)
		local dir = Vector3.new(toTarget.X, 0, toTarget.Z).Unit
		local now = Core.Now()

		-- blok kararı: rakip yakında saldırıyorsa
		local tAction = target:GetAttribute("Action")
		local tActionAge = now - (target:GetAttribute("ActionStart") or 0)
		if dist < 9 and tAction and string.sub(tAction, 1, 2) == "M1" and tActionAge < 0.15 and rng:NextNumber() < 0.3 then
			blockUntil = now + rng:NextNumber(0.5, 1.0)
		end
		if now < blockUntil then
			Core.FaceTowards(model, tPos)
			if not model:GetAttribute("Blocking") and Core.CanAct(model) then
				Core.SetBlocking(model, true)
			end
			continue
		elseif model:GetAttribute("Blocking") then
			Core.SetBlocking(model, false)
		end

		if not Core.CanAct(model) then
			continue
		end

		local roll = rng:NextNumber()
		if dist > 22 and dist < 70 and not Core.OnCooldown(model, "Katon") and roll < 0.08 then
			hum:MoveTo(root.Position)
			Kit.Katon(model, tPos, dir)
		elseif dist > 10 and dist < 40 and not Core.OnCooldown(model, "Crow") and roll < 0.06 then
			Kit.Crow(model, tPos, dir)
		elseif dist < 35 and not Core.OnCooldown(model, "Amaterasu") and roll < 0.025 then
			Kit.Amaterasu(model, tPos, dir)
		elseif dist < 18 and not Core.OnCooldown(model, "Tsukuyomi") and roll < 0.012 then
			Core.FaceTowards(model, tPos)
			Kit.Tsukuyomi(model, tPos, dir)
		elseif dist > 5 then
			hum:MoveTo(tPos - dir * 3.5)
		else
			hum:MoveTo(root.Position)
			Core.FaceTowards(model, tPos)
			Core.M1(model)
		end
	end
end

task.defer(function()
	-- arena zemininin oluşmasını bekle
	workspace:WaitForChild("Arena", 10)
	spawnNPC("Antrenman Kuklası", CFrame.new(18, 3.2, -32) * CFrame.Angles(0, math.rad(200), 0))
	spawnNPC("Antrenman Kuklası", CFrame.new(-18, 3.2, -32) * CFrame.Angles(0, math.rad(160), 0))
	spawnNPC("Blok Kuklası", CFrame.new(0, 3.2, -40) * CFrame.Angles(0, math.rad(180), 0), blockerBehavior)
	if Config.SpawnSparringBot then
		spawnNPC("Sparring Itachi", CFrame.new(0, 3.2, 45), botBehavior)
	end
end)
