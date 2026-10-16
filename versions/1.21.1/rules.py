"""Source changes from Minecraft Forge 1.20.1 to 1.21.1 (see versions/port.py)."""
import json, os

SYNC = 'net.minecraft.network.syncher.SynchedEntityData.Builder'
TOOLTIP = 'net.minecraft.world.item.Item.TooltipContext'

GLOBAL = [
    # entity data is declared through a builder
    (r'protected void defineSynchedData\(\) \{', 'protected void defineSynchedData(%s builder) {' % SYNC, 4),
    (r'super\.defineSynchedData\(\);', 'super.defineSynchedData(builder);', 2),
    (r'this\.entityData\.define\(', 'builder.define(', 20),
    # tooltips get a context instead of the level
    (r'appendHoverText\(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag\)',
     'appendHoverText(ItemStack stack, %s tipContext, List<Component> tip, TooltipFlag flag)' % TOOLTIP, 4),
    # client interpolation lost the teleport flag
    (r'public void lerpTo\(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport\)',
     'public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps)', 1),
    (r'\.setSecondsOnFire\(', '.igniteForSeconds(', 4),
    (r'Player\.PERSISTED_NBT_TAG', '"PlayerPersisted"', 2),
]

FILES = {
    'entity/BeybladeEntity.java': [
        ('        this.setMaxUpStep(0.6F);\n', ''),
        ('    /** test hook: force an ability regardless of cooldown */',
         '    @Override\n    public float maxUpStep() { return 0.6F; }\n\n    /** test hook: force an ability regardless of cooldown */'),
    ],
    'client/AutoTest.java': [
        ('reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());',
         'reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new net.minecraft.client.gui.screens.TitleScreen());'),
    ],
    'item/LauncherItem.java': [
        ('public int getUseDuration(ItemStack stack) {', 'public int getUseDuration(ItemStack stack, LivingEntity holder) {'),
        ('int used = getUseDuration(stack) - remaining;', 'int used = getUseDuration(stack, entity) - remaining;'),
    ],
    'block/StadiumBarrierBlock.java': [
        ('isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type)',
         'isPathfindable(BlockState state, PathComputationType type)'),
    ],
    # Screen.render already draws the (blurred) background, a second call throws
    'client/CommandScreen.java': [('        renderBackground(g);\n', '')],
    'world/StadiumWorldGen.java': [
        ('computeIfAbsent(Sites::load, Sites::new, ', 'computeIfAbsent(new SavedData.Factory<>(Sites::new, Sites::load, null), '),
        ('static Sites load(CompoundTag tag) {', 'static Sites load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {'),
        ('public CompoundTag save(CompoundTag tag) {', 'public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {'),
        ('cbe.setLootTable(CHEST_LOOT, ', 'cbe.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, CHEST_LOOT), '),
    ],
}

# data folders were renamed to singular names
MOVES = [
    ('data/librabeyblade/recipes', 'data/librabeyblade/recipe'),
    ('data/librabeyblade/loot_tables', 'data/librabeyblade/loot_table'),
]


def fix_resources(res):
    rdir = os.path.join(res, 'data/librabeyblade/recipe')
    for f in os.listdir(rdir):
        p = os.path.join(rdir, f)
        j = json.load(open(p))
        r = j.get('result')
        if isinstance(r, dict) and 'item' in r:
            r['id'] = r.pop('item')
        json.dump(j, open(p, 'w'), indent=1)
    toml = os.path.join(res, 'META-INF/mods.toml')
    t = open(toml).read()
    for old, new in (('loaderVersion="[47,)"', 'loaderVersion="[52,)"'), ('versionRange="[47,)"', 'versionRange="[52,)"'),
                     ('versionRange="[1.20.1,1.21)"', 'versionRange="[1.21.1,1.21.2)"')):
        assert old in t, old
        t = t.replace(old, new)
    open(toml, 'w').write(t)
    with open(os.path.join(res, 'pack.mcmeta'), 'w') as f:
        json.dump({'pack': {'description': 'Beyblade Metal Fight resources', 'pack_format': 34}}, f)
