-- Tüm dövüşçüleri (oyuncular + dummy'ler) yerel olarak prosedürel olarak canlandırır.
-- Sunucu karakterlere attribute yazar (Action, ActionStart, Blocking, StunEnd...),
-- her istemci bunları okuyup Motor6D.C0 üzerinden pozları yumuşak geçişle uygular.
-- Böylece hiçbir Roblox animasyon ID'si yüklemeye gerek kalmaz.

local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")
local CollectionService = game:GetService("CollectionService")

local Poses = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Poses"))

local arm, leg, neck, root = Poses.arm, Poses.leg, Poses.neck, Poses.root
local IDENTITY = CFrame.new()

local rigs = {}

local function setup(model)
	if rigs[model] then
		return
	end
	local hum = model:WaitForChild("Humanoid", 10)
	local hrp = model:WaitForChild("HumanoidRootPart", 10)
	local torso = model:WaitForChild("Torso", 10)
	if not (hum and hrp and torso) or hum.RigType ~= Enum.HumanoidRigType.R6 then
		return
	end
	local joints, base, current = {}, {}, {}
	for key, jointName in Poses.JointNames do
		local parent = key == "Root" and hrp or torso
		local m = parent:WaitForChild(jointName, 10)
		if not m then
			return
		end
		joints[key] = m
		base[key] = m.C0
		current[key] = IDENTITY
	end
	rigs[model] = { hum = hum, hrp = hrp, joints = joints, base = base, current = current, phase = 0, t = math.random() * 10 }
end

for _, m in CollectionService:GetTagged("Fighter") do
	task.spawn(setup, m)
end
CollectionService:GetInstanceAddedSignal("Fighter"):Connect(function(m)
	task.spawn(setup, m)
end)
CollectionService:GetInstanceRemovedSignal("Fighter"):Connect(function(m)
	rigs[m] = nil
end)

local function locomotion(rig, dt)
	local hum, hrp = rig.hum, rig.hrp
	local vel = hrp.AssemblyLinearVelocity
	local flat = Vector3.new(vel.X, 0, vel.Z)
	local speed = flat.Magnitude
	rig.t += dt

	if hum.FloorMaterial == Enum.Material.Air then
		return vel.Y > 2 and Poses.Jump or Poses.Fall
	end

	if speed > 1.5 then
		rig.phase += dt * math.clamp(speed, 6, 28) * 0.55
		local s = math.sin(rig.phase)
		local c = math.abs(math.cos(rig.phase))
		-- hareket yönüne göre bacak salınımı (geri yürürken ters)
		local fwdDot = flat.Unit:Dot(hrp.CFrame.LookVector)
		local dirSign = fwdDot < -0.3 and -1 or 1
		if speed > 14 and fwdDot > 0.3 then
			-- ninja koşusu: kollar geride, gövde öne eğik
			return {
				Root = root(20, 0, 0, -c * 0.18 + 0.1),
				Neck = neck(-14),
				RS = arm(1, -62 + s * 6, 16, 10),
				LS = arm(-1, -62 - s * 6, 16, 10),
				RH = leg(1, s * 55, 3),
				LH = leg(-1, -s * 55, 3),
			}
		end
		return {
			Root = root(6 * dirSign, 0, 0, -c * 0.1 + 0.05),
			Neck = neck(-2),
			RS = arm(1, -s * 30 * dirSign, 6),
			LS = arm(-1, s * 30 * dirSign, 6),
			RH = leg(1, s * 38 * dirSign, 2),
			LH = leg(-1, -s * 38 * dirSign, 2),
		}
	end

	-- nefes alma ile imza duruş
	local b = math.sin(rig.t * 2.2)
	local idle = Poses.Idle
	return {
		Root = idle.Root * CFrame.new(0, 0, b * 0.03),
		Neck = idle.Neck * CFrame.Angles(math.rad(b * 1.5), 0, 0),
		RS = idle.RS * CFrame.Angles(0, 0, math.rad(b * 2)),
		LS = idle.LS * CFrame.Angles(0, 0, math.rad(-b * 2)),
		RH = idle.RH,
		LH = idle.LH,
	}
end

RunService.RenderStepped:Connect(function(dt)
	local now = workspace:GetServerTimeNow()
	for model, rig in rigs do
		if not model.Parent then
			rigs[model] = nil
			continue
		end
		if rig.hum.Health <= 0 then
			continue
		end

		local target
		local speed = 16

		local action = model:GetAttribute("Action")
		local start = model:GetAttribute("ActionStart") or 0
		local actionDef = action and Poses.Actions[action]
		local actionT = now - start

		if (model:GetAttribute("TrappedEnd") or 0) > now then
			target = Poses.Trapped
			speed = 10
		elseif (model:GetAttribute("GuardBreakEnd") or 0) > now then
			target = Poses.GuardBroken
			speed = 20
		elseif actionDef and actionT >= 0 and actionT < actionDef.Duration then
			target = Poses.Sample(action, actionT)
			speed = 30
		elseif (model:GetAttribute("StunEnd") or 0) > now then
			target = Poses.Stunned
		elseif model:GetAttribute("Blocking") then
			target = Poses.Block
			speed = 26
		else
			target = locomotion(rig, dt)
			-- blok yaparken yürüme: bacaklar yürür, üst gövde blokta kalır
		end

		local alpha = 1 - math.exp(-dt * speed)
		for key, motor in rig.joints do
			local cur = rig.current[key]:Lerp(target[key] or IDENTITY, alpha)
			rig.current[key] = cur
			motor.C0 = rig.base[key] * cur
		end
	end
end)
