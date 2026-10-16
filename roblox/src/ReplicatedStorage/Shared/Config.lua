-- Tüm denge (balance) ayarları tek yerde. Oyunu ayarlamak için burayı değiştir.

local Config = {}

Config.WalkSpeed = 20
Config.JumpPower = 50
Config.BlockWalkSpeed = 7
Config.ActionWalkSpeed = 6

-- M1 (sol tık) kombosu
Config.M1 = {
	Damage = 4,
	FinisherDamage = 7,
	Hits = 4,
	Interval = 0.32, -- iki vuruş arası minimum süre
	ComboReset = 1.1, -- bu kadar süre vurmazsan kombo sıfırlanır
	EndLag = 1.0, -- 4. vuruştan sonra bekleme
	HitStun = 0.45,
	Range = Vector3.new(5, 5, 5.5),
	FinisherKnockback = 55,
}

-- Blok / parry / posture
Config.Block = {
	DamageMultiplier = 0.15, -- blok varken alınan hasar oranı
	MaxPosture = 60, -- bu kadar hasar bloklanırsa guard break
	PostureRegen = 12, -- saniyede
	PerfectWindow = 0.22, -- bloğa basıştan sonra bu sürede gelen vuruş = PARRY
	ParryStun = 1.3,
	GuardBreakStun = 1.8,
	FrontDot = -0.15, -- arkadan gelen vuruşlar bloklanamaz
}

-- Dash
Config.Dash = {
	Cooldown = 1.8,
	Speed = 95,
	Duration = 0.2,
	IFrames = 0.18,
}

Config.Abilities = {
	{
		Key = "One",
		Id = "Katon",
		Name = "Katon: Gōkakyū",
		Cooldown = 8,
		Damage = 18,
		BurnDamage = 2,
		BurnTicks = 4,
		Speed = 75,
		Range = 130,
		Radius = 13,
	},
	{
		Key = "Two",
		Id = "Crow",
		Name = "Karasu Bunshin",
		Cooldown = 10,
		Damage = 10,
		Range = 45,
		Stun = 0.9,
	},
	{
		Key = "Three",
		Id = "Amaterasu",
		Name = "Amaterasu",
		Cooldown = 14,
		Range = 70,
		Radius = 8,
		ZoneDuration = 3.5,
		ZoneTickDamage = 4,
		BurnDuration = 3,
		BurnTickDamage = 2,
	},
	{
		Key = "Four",
		Id = "Tsukuyomi",
		Name = "Tsukuyomi",
		Cooldown = 20,
		FailCooldown = 3,
		Range = 32,
		ConeDot = 0.75,
		Duration = 2.6,
		Damage = 26,
	},
}

Config.Respawn = {
	DummyRespawn = 3,
	DummyRegenDelay = 4,
}

Config.SpawnSparringBot = true

-- Ses dosyaları. Kendi Roblox ses ID'lerinle değiştirebilirsin (örn. "rbxassetid://123456").
Config.Sounds = {
	Swing = "rbxasset://sounds/swordlunge.wav",
	Hit = "rbxasset://sounds/collide.wav",
	HeavyHit = "rbxasset://sounds/collide.wav",
	Block = "rbxasset://sounds/unsheath.wav",
	Parry = "rbxasset://sounds/electronicpingshort.wav",
	GuardBreak = "rbxasset://sounds/swordslash.wav",
	Dash = "rbxasset://sounds/swordlunge.wav",
	Fire = "rbxasset://sounds/Rocket shot.wav",
	Explosion = "rbxasset://sounds/Launching rocket.wav",
	Crow = "rbxasset://sounds/swordlunge.wav",
	Amaterasu = "rbxasset://sounds/Rocket shot.wav",
	Tsukuyomi = "rbxasset://sounds/electronicpingshort.wav",
}

Config.Textures = {
	Fire = "rbxasset://textures/particles/fire_main.dds",
	Smoke = "rbxasset://textures/particles/smoke_main.dds",
	Spark = "rbxasset://textures/particles/sparkles_main.dds",
}

return Config
