-- Sunucu tarafı dövüş çekirdeği: durumlar, hasar, blok/parry/guard break,
-- sersemletme, geri itme, hitbox'lar, cooldown'lar ve efekt yayını.

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage.Shared.Config)

local Core = {}
Core.Remotes = nil :: any

local states = {} -- [character] = state

local function now()
	return workspace:GetServerTimeNow()
end
Core.Now = now

-- ---------------------------------------------------------------- kayıt

function Core.Register(char)
	local state = {
		cooldowns = {},
		combo = 0,
		lastM1 = 0,
		blockStart = 0,
		busyUntil = 0,
		posture = Config.Block.MaxPosture,
		lastDamaged = 0,
		player = Players:GetPlayerFromCharacter(char),
	}
	states[char] = state
	char:SetAttribute("Posture", state.posture)
	char:SetAttribute("Blocking", false)
	CollectionService:AddTag(char, "Fighter")
	char.AncestryChanged:Connect(function(_, parent)
		if not parent then
			states[char] = nil
		end
	end)
	return state
end

function Core.State(char)
	return states[char]
end

function Core.Humanoid(char)
	return char and char:FindFirstChildOfClass("Humanoid")
end

function Core.Root(char)
	return char and char:FindFirstChild("HumanoidRootPart")
end

function Core.Alive(char)
	local h = Core.Humanoid(char)
	return h ~= nil and h.Health > 0 and states[char] ~= nil
end

-- ---------------------------------------------------------------- efekt yayını

function Core.FX(name, data)
	Core.Remotes.VFX:FireAllClients(name, data)
end

function Core.FXExcept(exceptPlayer, name, data)
	for _, p in Players:GetPlayers() do
		if p ~= exceptPlayer then
			Core.Remotes.VFX:FireClient(p, name, data)
		end
	end
end

-- ---------------------------------------------------------------- durumlar

function Core.IsStunned(char)
	return (char:GetAttribute("StunEnd") or 0) > now()
end

function Core.IsBusy(char)
	local s = states[char]
	return s == nil or s.busyUntil > now() or Core.IsStunned(char)
end

function Core.CanAct(char)
	return Core.Alive(char) and not Core.IsBusy(char)
end

-- Aksiyon animasyonunu başlat ve karakteri belirli süre meşgul et
function Core.SetAction(char, action, busyFor)
	local s = states[char]
	if not s then
		return
	end
	char:SetAttribute("Action", action)
	char:SetAttribute("ActionStart", now())
	if busyFor then
		s.busyUntil = math.max(s.busyUntil, now() + busyFor)
	end
end

function Core.Stun(char, duration)
	local t = now() + duration
	if (char:GetAttribute("StunEnd") or 0) < t then
		char:SetAttribute("StunEnd", t)
	end
	Core.SetBlocking(char, false)
end

function Core.SetIFrames(char, duration)
	char:SetAttribute("IFrameEnd", now() + duration)
end

function Core.HasIFrames(char)
	return (char:GetAttribute("IFrameEnd") or 0) > now()
end

function Core.SetBlocking(char, on)
	local s = states[char]
	if not s then
		return
	end
	if on then
		if char:GetAttribute("Blocking") then
			return
		end
		s.blockStart = now()
	end
	char:SetAttribute("Blocking", on)
end

-- ---------------------------------------------------------------- cooldown

function Core.OnCooldown(char, name)
	local s = states[char]
	return s == nil or (s.cooldowns[name] or 0) > now()
end

function Core.SetCooldown(char, name, duration)
	local s = states[char]
	if not s then
		return
	end
	s.cooldowns[name] = now() + duration
	if s.player then
		Core.Remotes.Cooldown:FireClient(s.player, name, duration)
	end
end

-- ---------------------------------------------------------------- hareket

function Core.Knockback(char, velocity, duration)
	local root = Core.Root(char)
	if not root then
		return
	end
	local att = Instance.new("Attachment")
	att.Name = "KnockbackAtt"
	att.Parent = root
	local lv = Instance.new("LinearVelocity")
	lv.Name = "Knockback"
	lv.Attachment0 = att
	lv.MaxForce = 1e6
	lv.VectorVelocity = velocity
	lv.RelativeTo = Enum.ActuatorRelativeTo.World
	lv.Parent = root
	task.delay(duration or 0.2, function()
		lv:Destroy()
		att:Destroy()
	end)
end

function Core.FaceTowards(char, pos)
	local root = Core.Root(char)
	if not root then
		return
	end
	local flat = Vector3.new(pos.X, root.Position.Y, pos.Z)
	if (flat - root.Position).Magnitude > 0.1 then
		root.CFrame = CFrame.lookAt(root.Position, flat)
	end
end

-- ---------------------------------------------------------------- hitbox

local overlap = OverlapParams.new()
overlap.FilterType = Enum.RaycastFilterType.Include

local function fightersFromParts(parts, exclude)
	local found, list = {}, {}
	for _, part in parts do
		local model = part:FindFirstAncestorOfClass("Model")
		while model and not states[model] do
			model = model:FindFirstAncestorOfClass("Model")
		end
		if model and model ~= exclude and not found[model] and Core.Alive(model) then
			found[model] = true
			table.insert(list, model)
		end
	end
	return list
end

local function fighterParts()
	local list = {}
	for char in states do
		local r = Core.Root(char)
		local t = char:FindFirstChild("Torso")
		if r then
			table.insert(list, r)
		end
		if t then
			table.insert(list, t)
		end
	end
	return list
end

function Core.HitboxBox(attacker, cf, size)
	overlap.FilterDescendantsInstances = fighterParts()
	return fightersFromParts(workspace:GetPartBoundsInBox(cf, size, overlap), attacker)
end

function Core.HitboxSphere(attacker, pos, radius)
	overlap.FilterDescendantsInstances = fighterParts()
	return fightersFromParts(workspace:GetPartBoundsInRadius(pos, radius, overlap), attacker)
end

-- Saldırganın önündeki koni içinde en yakın düşman
function Core.FindTarget(attacker, maxDist, minDot, aimDir)
	local root = Core.Root(attacker)
	if not root then
		return nil
	end
	local look = aimDir or root.CFrame.LookVector
	look = Vector3.new(look.X, 0, look.Z)
	look = look.Magnitude > 0 and look.Unit or root.CFrame.LookVector
	local best, bestScore = nil, math.huge
	for char in states do
		if char ~= attacker and Core.Alive(char) then
			local r = Core.Root(char)
			if r then
				local off = r.Position - root.Position
				local dist = off.Magnitude
				local flat = Vector3.new(off.X, 0, off.Z)
				local dot = flat.Magnitude > 0 and flat.Unit:Dot(look) or 1
				if dist <= maxDist and dot >= minDot then
					local score = dist * (2 - dot)
					if score < bestScore then
						best, bestScore = char, score
					end
				end
			end
		end
	end
	return best
end

-- ---------------------------------------------------------------- hasar

--[[
opts:
  stun        : number  (sersemletme süresi)
  knockback   : Vector3 (dünya hızı)
  heavy       : bool    (büyük vuruş efekti)
  unblockable : bool
  guardBreak  : bool    (bloğu direkt kırar)
  noParry     : bool
  hurtAnim    : bool    (default true)
  silent      : bool    (vuruş efekti yok, sadece hasar)
  color       : Color3  (hasar sayısı rengi)
döndürür: "hit" | "blocked" | "parried" | "guardbreak" | "iframe" | nil
]]
function Core.Damage(attacker, victim, amount, opts)
	opts = opts or {}
	if not Core.Alive(victim) then
		return nil
	end
	local vs = states[victim]
	local hum = Core.Humanoid(victim)
	local vRoot = Core.Root(victim)
	if not vs or not hum or not vRoot then
		return nil
	end
	if Core.HasIFrames(victim) then
		return "iframe"
	end

	local aRoot = attacker and Core.Root(attacker)
	local hitPos = vRoot.Position
	local dir = aRoot and (vRoot.Position - aRoot.Position) or vRoot.CFrame.LookVector
	dir = Vector3.new(dir.X, 0, dir.Z)
	dir = dir.Magnitude > 0 and dir.Unit or Vector3.zAxis

	-- blok kontrolü: saldırı önden gelmeli
	if victim:GetAttribute("Blocking") and not opts.unblockable then
		local facing = vRoot.CFrame.LookVector:Dot(-dir)
		if facing > Config.Block.FrontDot then
			local sinceBlock = now() - vs.blockStart
			if not opts.noParry and sinceBlock <= Config.Block.PerfectWindow and attacker then
				-- PARRY: saldırgan sersemler
				Core.Stun(attacker, Config.Block.ParryStun)
				Core.SetAction(attacker, "Parried")
				local as = states[attacker]
				if as then
					as.combo = 0
				end
				Core.FX("Parry", { pos = hitPos - dir * 1.5, dir = dir })
				return "parried"
			end
			if opts.guardBreak then
				vs.posture = 0
			else
				vs.posture -= amount
			end
			if vs.posture <= 0 then
				vs.posture = Config.Block.MaxPosture
				victim:SetAttribute("Posture", vs.posture)
				Core.SetBlocking(victim, false)
				Core.Stun(victim, Config.Block.GuardBreakStun)
				victim:SetAttribute("GuardBreakEnd", now() + Config.Block.GuardBreakStun * 0.6)
				Core.FX("GuardBreak", { pos = hitPos - dir * 1 })
				return "guardbreak"
			end
			victim:SetAttribute("Posture", vs.posture)
			local chip = math.floor(amount * Config.Block.DamageMultiplier + 0.5)
			if chip > 0 then
				hum:TakeDamage(chip)
			end
			vs.lastDamaged = now()
			Core.FX("Block", { pos = hitPos - dir * 1.2 })
			if opts.knockback then
				Core.Knockback(victim, opts.knockback * 0.35, 0.12)
			end
			return "blocked"
		end
	end

	hum:TakeDamage(amount)
	vs.lastDamaged = now()
	if opts.stun and opts.stun > 0 then
		Core.Stun(victim, opts.stun)
		vs.combo = 0
		vs.busyUntil = 0
		if opts.hurtAnim ~= false then
			Core.SetAction(victim, "Hurt")
		end
	end
	if opts.knockback then
		Core.Knockback(victim, opts.knockback, 0.2)
	end
	if not opts.silent then
		Core.FX("Hit", { pos = hitPos - dir * 0.8 + Vector3.new(0, 0.5, 0), heavy = opts.heavy, dir = dir })
	end
	Core.FX("DamageNumber", { pos = hitPos, amount = amount, color = opts.color, big = opts.heavy })
	return "hit"
end

-- Zaman içinde hasar (yanma)
function Core.Burn(attacker, victim, tickDamage, ticks, interval, black)
	Core.FX("Burn", { char = victim, duration = ticks * interval, black = black })
	task.spawn(function()
		for _ = 1, ticks do
			task.wait(interval)
			if not Core.Alive(victim) then
				return
			end
			Core.Damage(attacker, victim, tickDamage, {
				unblockable = true,
				silent = true,
				color = black and Color3.fromRGB(170, 60, 255) or Color3.fromRGB(255, 140, 40),
			})
		end
	end)
end

-- ---------------------------------------------------------------- M1 kombosu (tüm karakterler)

function Core.M1(char)
	local s = states[char]
	if not s or not Core.CanAct(char) or char:GetAttribute("Blocking") then
		return
	end
	local t = now()
	if t - s.lastM1 < Config.M1.Interval then
		return
	end
	if t - s.lastM1 > Config.M1.ComboReset then
		s.combo = 0
	end
	s.combo += 1
	s.lastM1 = t
	local hitIndex = s.combo
	local finisher = hitIndex >= Config.M1.Hits

	Core.SetAction(char, "M1_" .. hitIndex, finisher and 0.5 or Config.M1.Interval * 0.9)
	if finisher then
		s.combo = 0
		s.busyUntil = t + Config.M1.EndLag
	end
	Core.FX("Swing", { char = char })

	task.delay(finisher and 0.12 or 0.07, function()
		if not Core.Alive(char) or Core.IsStunned(char) then
			return
		end
		local root = Core.Root(char)
		local cf = root.CFrame * CFrame.new(0, 0, -Config.M1.Range.Z / 2 - 0.5)
		for _, victim in Core.HitboxBox(char, cf, Config.M1.Range) do
			local look = root.CFrame.LookVector
			local res = Core.Damage(char, victim, finisher and Config.M1.FinisherDamage or Config.M1.Damage, {
				stun = finisher and 0.9 or Config.M1.HitStun,
				heavy = finisher,
				guardBreak = false,
				knockback = finisher and (look * Config.M1.FinisherKnockback + Vector3.new(0, 18, 0)) or (look * 6),
			})
			if res == "hit" and not finisher then
				-- saldırganı hafifçe öne taşı ki kombo devam etsin
				Core.Knockback(char, look * 6, 0.1)
			end
		end
	end)
end

-- ---------------------------------------------------------------- güncelleme döngüsü

RunService.Heartbeat:Connect(function(dt)
	local t = now()
	for char, s in states do
		local hum = Core.Humanoid(char)
		if not hum or hum.Health <= 0 then
			continue
		end
		local stunned = Core.IsStunned(char)
		local speed, jump = Config.WalkSpeed, Config.JumpPower
		if stunned or (char:GetAttribute("TrappedEnd") or 0) > t then
			speed, jump = 0, 0
		elseif char:GetAttribute("Blocking") then
			speed, jump = Config.BlockWalkSpeed, 0
		elseif s.busyUntil > t then
			speed, jump = Config.ActionWalkSpeed, 0
		end
		if hum.WalkSpeed ~= speed then
			hum.WalkSpeed = speed
		end
		if hum.JumpPower ~= jump then
			hum.JumpPower = jump
		end
		-- posture yenilenmesi
		if not char:GetAttribute("Blocking") and s.posture < Config.Block.MaxPosture then
			s.posture = math.min(Config.Block.MaxPosture, s.posture + Config.Block.PostureRegen * dt)
			char:SetAttribute("Posture", math.floor(s.posture))
		end
	end
end)

return Core
