-- Itachi'nin 4 yeteneği (sunucu mantığı).
-- Her fonksiyon: (char, aimPos: Vector3, aimDir: Vector3)

local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")
local CollectionService = game:GetService("CollectionService")

local Config = require(ReplicatedStorage.Shared.Config)
local ItachiBuilder = require(ReplicatedStorage.Shared.ItachiBuilder)
local Core = require(script.Parent.CombatCore)

local Kit = {}

local ABILITY = {}
for _, a in Config.Abilities do
	ABILITY[a.Id] = a
end
Kit.Info = ABILITY

local function flat(v)
	local f = Vector3.new(v.X, 0, v.Z)
	return f.Magnitude > 0.001 and f.Unit or Vector3.new(0, 0, -1)
end

local function worldParams(char)
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	local list = { char }
	for _, m in CollectionService:GetTagged("Fighter") do
		table.insert(list, m)
	end
	params.FilterDescendantsInstances = list
	return params
end

local function usable(char)
	return Core.Alive(char) and not Core.IsStunned(char)
end

local function mangekyo(char, seconds, bleed)
	ItachiBuilder.SetEyes(char, "Mangekyo")
	if bleed then
		ItachiBuilder.SetBleeding(char, true)
	end
	task.delay(seconds, function()
		if char.Parent then
			ItachiBuilder.SetEyes(char, "Sharingan")
			ItachiBuilder.SetBleeding(char, false)
		end
	end)
end

local projectileId = 0

-- ---------------------------------------------------------------- 1) Katon: Gōkakyū no Jutsu
-- El mühürleri -> dev ateş topu. Çarpınca patlar, alan hasarı + yanma.

function Kit.Katon(char, aimPos)
	local cfg = ABILITY.Katon
	Core.SetCooldown(char, "Katon", cfg.Cooldown)
	Core.FaceTowards(char, aimPos)
	Core.SetAction(char, "Katon", 1.05)
	Core.FX("JutsuName", { char = char, text = "火遁・豪火球の術" })

	task.delay(0.58, function()
		if not usable(char) then
			return
		end
		local root = Core.Root(char)
		local origin = root.Position + root.CFrame.LookVector * 3.5 + Vector3.new(0, 1.2, 0)
		local dir = aimPos - origin
		if dir.Magnitude < 1 then
			dir = root.CFrame.LookVector
		end
		dir = dir.Unit
		-- dikey açıyı sınırla
		local y = math.clamp(dir.Y, -0.35, 0.35)
		dir = (flat(dir) * math.sqrt(1 - y * y) + Vector3.new(0, y, 0)).Unit

		projectileId += 1
		local id = projectileId
		local radius = 3
		Core.FX("KatonStart", { id = id, char = char, origin = origin, dir = dir, speed = cfg.Speed, range = cfg.Range, radius = radius })

		local params = worldParams(char)
		local pos, traveled = origin, 0
		local exploded = false

		local function explode(at)
			if exploded then
				return
			end
			exploded = true
			Core.FX("KatonExplode", { id = id, pos = at, radius = cfg.Radius })
			for _, victim in Core.HitboxSphere(char, at, cfg.Radius) do
				local vr = Core.Root(victim)
				local away = flat(vr.Position - at)
				local res = Core.Damage(char, victim, cfg.Damage, {
					stun = 1.0,
					heavy = true,
					knockback = away * 55 + Vector3.new(0, 30, 0),
					noParry = true,
					color = Color3.fromRGB(255, 150, 50),
				})
				if res == "hit" then
					Core.Burn(char, victim, cfg.BurnDamage, cfg.BurnTicks, 0.5, false)
				end
			end
		end

		while not exploded do
			local dt = RunService.Heartbeat:Wait()
			local step = cfg.Speed * dt
			local hit = workspace:Raycast(pos, dir * (step + radius * 0.6), params)
			if hit then
				explode(hit.Position - dir * 1)
				break
			end
			pos += dir * step
			traveled += step
			if #Core.HitboxSphere(char, pos, radius + 0.8) > 0 then
				explode(pos)
				break
			end
			if traveled >= cfg.Range then
				explode(pos)
				break
			end
		end
	end)
end

-- ---------------------------------------------------------------- 2) Karasu Bunshin
-- Kargalara dönüşüp kaybolur, en yakın düşmanın arkasında belirip tekme atar.

function Kit.Crow(char, aimPos, aimDir)
	local cfg = ABILITY.Crow
	local root = Core.Root(char)
	Core.SetCooldown(char, "Crow", cfg.Cooldown)
	Core.SetAction(char, "CrowVanish", 0.75)
	Core.SetIFrames(char, 0.45)
	Core.FX("JutsuName", { char = char, text = "烏分身の術" })
	Core.FX("CrowBurst", { pos = root.Position, count = 16 })

	local target = Core.FindTarget(char, cfg.Range, 0.1, aimDir)

	task.delay(0.22, function()
		if not usable(char) then
			return
		end
		root = Core.Root(char)
		local dest
		if target and Core.Alive(target) then
			local tr = Core.Root(target)
			local behind = tr.Position - flat(tr.CFrame.LookVector) * 3.6
			dest = CFrame.lookAt(behind, Vector3.new(tr.Position.X, behind.Y, tr.Position.Z))
		else
			local look = flat(aimDir or root.CFrame.LookVector)
			local dist = 26
			local hit = workspace:Raycast(root.Position, look * dist, worldParams(char))
			if hit then
				dist = math.max(0, (hit.Position - root.Position).Magnitude - 3)
			end
			local p = root.Position + look * dist
			dest = CFrame.lookAt(p, p + look)
		end
		char:PivotTo(dest)
		Core.FX("CrowBurst", { pos = dest.Position, count = 9 })
		Core.SetAction(char, "CrowStrike", 0.55)

		if target then
			task.delay(0.12, function()
				if not usable(char) then
					return
				end
				local r = Core.Root(char)
				local cf = r.CFrame * CFrame.new(0, 0, -3)
				for _, victim in Core.HitboxBox(char, cf, Vector3.new(6, 6, 6)) do
					Core.Damage(char, victim, cfg.Damage, {
						stun = cfg.Stun,
						heavy = true,
						knockback = r.CFrame.LookVector * 30 + Vector3.new(0, 14, 0),
					})
				end
			end)
		end
	end)
end

-- ---------------------------------------------------------------- 3) Amaterasu
-- Mangekyō ile hedef noktada sönmeyen siyah alevler. Bloklanamaz, alan + yanma.

function Kit.Amaterasu(char, aimPos, aimDir)
	local cfg = ABILITY.Amaterasu
	local root = Core.Root(char)
	-- menzil sınırı
	local off = aimPos - root.Position
	if off.Magnitude > cfg.Range then
		aimPos = root.Position + off.Unit * cfg.Range
	end
	-- nişanın yakınında bir düşman varsa ona kilitlen
	local target = Core.FindTarget(char, cfg.Range, 0.55, aimDir)
	if target then
		local tr = Core.Root(target)
		if (tr.Position - aimPos).Magnitude < 14 then
			aimPos = tr.Position
		end
	end

	Core.SetCooldown(char, "Amaterasu", cfg.Cooldown)
	Core.FaceTowards(char, aimPos)
	Core.SetAction(char, "Amaterasu", 0.9)
	Core.FX("JutsuName", { char = char, text = "天照" })
	mangekyo(char, 3.5, true)

	task.delay(0.55, function()
		if not usable(char) then
			return
		end
		-- hedef hareket ettiyse takip et
		if target and Core.Alive(target) then
			local tr = Core.Root(target)
			if (tr.Position - aimPos).Magnitude < 8 then
				aimPos = tr.Position
			end
		end
		local ground = workspace:Raycast(aimPos + Vector3.new(0, 4, 0), Vector3.new(0, -40, 0), worldParams(char))
		local center = ground and ground.Position or aimPos
		Core.FX("AmaterasuCast", { char = char, target = center })
		Core.FX("AmaterasuZone", { pos = center, radius = cfg.Radius, duration = cfg.ZoneDuration })

		local burning = {}
		local elapsed = 0
		while elapsed < cfg.ZoneDuration do
			for _, victim in Core.HitboxSphere(char, center + Vector3.new(0, 2, 0), cfg.Radius) do
				Core.Damage(char, victim, cfg.ZoneTickDamage, {
					unblockable = true,
					stun = 0.25,
					hurtAnim = false,
					silent = true,
					color = Color3.fromRGB(170, 60, 255),
				})
				if not burning[victim] then
					burning[victim] = true
					local ticks = math.floor(cfg.BurnDuration / 0.5)
					Core.Burn(char, victim, cfg.BurnTickDamage, ticks, 0.5, true)
				end
			end
			task.wait(0.4)
			elapsed += 0.4
		end
	end)
end

-- ---------------------------------------------------------------- 4) Tsukuyomi
-- Göz teması: öndeki hedefi genjutsuya hapseder (donar), bloklanamaz hasar.

function Kit.Tsukuyomi(char, aimPos, aimDir)
	local cfg = ABILITY.Tsukuyomi
	local target = Core.FindTarget(char, cfg.Range, cfg.ConeDot, aimDir)
	mangekyo(char, 1.2, false)
	Core.SetAction(char, "Tsukuyomi", 0.6)

	if not target then
		Core.SetCooldown(char, "Tsukuyomi", cfg.FailCooldown)
		local head = char:FindFirstChild("Head")
		if head then
			Core.FX("Text", { pos = head.Position + Vector3.new(0, 3, 0), text = "Hedef yok", color = Color3.fromRGB(200, 200, 200), size = 1.2, life = 0.8 })
		end
		return
	end

	Core.SetCooldown(char, "Tsukuyomi", cfg.Cooldown)
	Core.FaceTowards(char, Core.Root(target).Position)
	Core.FX("JutsuName", { char = char, text = "月読" })

	task.delay(0.4, function()
		if not usable(char) or not Core.Alive(target) then
			return
		end
		local r, tr = Core.Root(char), Core.Root(target)
		if (tr.Position - r.Position).Magnitude > cfg.Range + 6 then
			return
		end
		if Core.HasIFrames(target) then
			Core.FX("Text", { pos = tr.Position + Vector3.new(0, 4, 0), text = "KAÇTI", color = Color3.fromRGB(200, 200, 200), size = 1.2, life = 0.8 })
			return
		end
		Core.SetAction(char, "Tsukuyomi", 1.0)
		mangekyo(char, cfg.Duration, false)
		Core.SetBlocking(target, false)
		Core.Stun(target, cfg.Duration)
		target:SetAttribute("TrappedEnd", Core.Now() + cfg.Duration)
		Core.FX("TsukuyomiCast", { caster = char, victim = target, duration = cfg.Duration })

		local ticks = 4
		for i = 1, ticks do
			task.wait(cfg.Duration / ticks)
			if not Core.Alive(target) then
				break
			end
			Core.Damage(char, target, math.floor(cfg.Damage / ticks + 0.5), {
				unblockable = true,
				silent = i < ticks,
				heavy = i == ticks,
				color = Color3.fromRGB(255, 40, 40),
			})
		end
	end)
end

return Kit
