package mindustry.arcreeper;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Func;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.WorldLabel;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.ImpactReactor;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/**
 * Creeper mode building behaviour manager.
 *
 * <p>Java port of the KTS BuildingTracker + Emitter model.</p>
 * <ul>
 *     <li>Emitter blocks are tracked only when their team is {@link #creepTeam}.</li>
 *     <li>Impact reactors are tracked only when their team is not {@link #creepTeam}.</li>
 *     <li>Each tracked emitter owns a WorldLabel, and label.text is rebuilt with StringBuilder.</li>
 * </ul>
 */
public final class CreeperBuilding {
    private static final ObjectMap<Block, Entry> buildingFactories = new ObjectMap<>();
    private static final ObjectMap<Building, BuildingLogic> activeBuildings = new ObjectMap<>();
    private static boolean initialized = false;

    /** Same as Emitter.healthOffset in the KTS version. */
    public static final float healthOffset = 1_000_000f;

    /** Mirrors dmgPerFlood in module.kts. */
    public static float dmgPerCreeper = 0.2f;

    /** Team that owns creeper emitters. Must match your map/rules. */
    public static Team creepTeam = Team.blue;

    /** Impact reactor nullify distance. KTS: 16f * tilesize. */
    public static float impactRange = 16f * Vars.tilesize;

    private CreeperBuilding() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        registerDefaultFactories();

        Events.on(EventType.TilePreChangeEvent.class, e -> {
            if (!CreeperCore.enabled()) return;
            Building build = e.tile.build;
            if (build != null && build.tile == e.tile) removeTracker(build);
        });

        Events.on(EventType.TileChangeEvent.class, e -> {
            if (!CreeperCore.enabled()) return;
            Building build = e.tile.build;
            if (build != null && build.tile == e.tile) addTracker(build);
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (!CreeperCore.enabled()) return;
            BuildingLogic logic = activeBuildings.get(e.tile.build);
            if (logic instanceof NuclearReactorLogic) {
                ((NuclearReactorLogic) logic).onDestroy();
            }
        });
    }

    /** Re-scan the whole world and attach logic to all matching buildings. */
    public static void load() {
        reset(false);
        scanWorldBuildings();
        Log.info("CreeperBuilding loaded, active: @", activeBuildings.size);
    }

    public static void update() {
        Seq<Building> invalid = new Seq<>();

        for (ObjectMap.Entry<Building, BuildingLogic> entry : activeBuildings) {
            Building build = entry.key;
            if (build == null || build.tile == null || build.tile.build != build || !build.isValid()) {
                invalid.add(build);
                continue;
            }
            entry.value.update();
        }

        for (Building build : invalid) removeTracker(build);
    }

    public static void reset() {
        reset(false);
    }

    public static void reset(boolean resetEvent) {
        for (BuildingLogic logic : activeBuildings.values()) {
            logic.removed(resetEvent);
        }
        activeBuildings.clear();
    }

    public static int activeCount() {
        return activeBuildings.size;
    }

    public static Seq<BuildingLogic> activeLogics() {
        Seq<BuildingLogic> out = new Seq<>();
        for (BuildingLogic logic : activeBuildings.values()) out.add(logic);
        return out;
    }

    public static void register(Block block, Boolf<Building> filter, Func<Building, BuildingLogic> factory) {
        if (block == null || filter == null || factory == null) return;
        buildingFactories.put(block, new Entry(filter, factory));
    }

    private static void registerEmitter(Block block, Func<Building, BuildingLogic> factory) {
        register(block, b -> b.team == creepTeam, factory);
    }

    private static void registerDefaultFactories() {
        buildingFactories.clear();

        // KTS emitterCore.kts
        registerEmitter(Blocks.coreShard, b -> new CoreEmitterLogic(b, 4f, 0.5f, 5f, 20f * 30f, Blocks.coreFoundation, 3000f, 3f * 60f, true));
        registerEmitter(Blocks.coreFoundation, b -> new CoreEmitterLogic(b, 10f, 0.4f, 10f, 100f * 30f, Blocks.coreNucleus, 6000f, 5f * 60f, true));
        registerEmitter(Blocks.coreNucleus, b -> new CoreEmitterLogic(b, 20f, 0.2f, -1f, -1f, null, 10000f, 6f * 60f, true));

        registerEmitter(Blocks.reinforcedContainer, b -> new CoreEmitterLogic(b, 3f, 1f, -1f, -1f, null, 1000f, 3f * 60f, true));
        registerEmitter(Blocks.reinforcedVault, b -> new CoreEmitterLogic(b, 7f, 1f, -1f, -1f, null, 2000f, 3f * 60f, true));
        registerEmitter(Blocks.coreBastion, b -> new CoreEmitterLogic(b, 13f, 0.4f, -1f, -1f, null, 5000f, 3f * 60f, false));
        registerEmitter(Blocks.coreCitadel, b -> new CoreEmitterLogic(b, 25f, 0.2f, -1f, -1f, null, 10000f, 5f * 60f, false));
        registerEmitter(Blocks.coreAcropolis, b -> new CoreEmitterLogic(b, 50f, 0.2f, -1f, -1f, null, 15000f, 6f * 60f, false));

        // KTS emitterCharged.kts
        registerEmitter(Blocks.container, b -> new ChargedEmitterLogic(b, 4f * (1f + 1f / 0.3f), 2f, 500f, 0.3f, 900f, -1f, null));
        registerEmitter(Blocks.vault, b -> new ChargedEmitterLogic(b, 12f * (1f + 1f / 0.3f), 2f, 1500f, 0.3f, 900f, -1f, null));
        registerEmitter(Blocks.launchPad, b -> new ChargedEmitterLogic(b, 8f * (1f + 1f / 0.3f), 1f, 1000f, 0.3f, 900f, 60f * 30f, Blocks.interplanetaryAccelerator));
        registerEmitter(Blocks.interplanetaryAccelerator, b -> new ChargedEmitterLogic(b, 24f * (1f + 1f / 0.5f), 2f, 2500f, 0.5f, 2400f, -1f, null));

        // KTS emitterCore.kts: reactor is player-side/non-creepTeam and suppresses core emitters.
        register(Blocks.impactReactor, b -> b.team != creepTeam, ImpactReactorLogic::new);

        // KTS nuclearReactor.kts: creepTeam thorium reactor.
        register(Blocks.thoriumReactor, b -> b.team == creepTeam, NuclearReactorLogic::new);
    }

    private static void scanWorldBuildings() {
        if (Vars.world == null || Vars.world.tiles == null) return;
        Vars.world.tiles.eachTile(tile -> {
            Building build = tile.build;
            if (build != null && build.tile == tile) addTracker(build);
        });
    }

    private static void addTracker(Building build) {
        if (build == null || activeBuildings.containsKey(build)) return;
        Entry entry = buildingFactories.get(build.block);
        if (entry == null || !entry.filter.get(build)) return;

        BuildingLogic logic = entry.factory.get(build);
        if (logic == null) return;

        activeBuildings.put(build, logic);
        logic.added();
        Log.info("CreeperBuilding add: @ team=@", build.block.name, build.team);
    }

    private static void removeTracker(Building build) {
        if (build == null) return;
        BuildingLogic logic = activeBuildings.remove(build);
        if (logic != null) logic.removed(false);
    }

    private static void addCreeper(Tile tile, float amount) {
        if (tile == null || amount == 0f) return;
        tile.creeper = Math.max(0f, tile.creeper + amount);
    }

    private static void setCreeper(Tile tile, float amount) {
        if (tile == null) return;
        tile.creeper = Math.max(0f, amount);
    }

    private static void eachLinkedTile(Building build, arc.func.Cons<Tile> cons) {
        if (build == null || build.tile == null || cons == null) return;
        int size = Math.max(1, build.block.size);
        int offset = -(size - 1) / 2;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Tile tile = Vars.world.tile(build.tile.x + offset + dx, build.tile.y + offset + dy);
                if (tile != null) cons.get(tile);
            }
        }
    }

    private static void addCreeperArea(Building build, float totalAmount) {
        if (build == null) return;
        int size = Math.max(1, build.block.size);
        float each = totalAmount / (size * size);
        eachLinkedTile(build, tile -> addCreeper(tile, each));
    }

    private static void clearCreeperArea(Building build) {
        eachLinkedTile(build, tile -> setCreeper(tile, 0f));
    }

    private static float drainCreeperArea(Building build) {
        final float[] total = {0f};
        eachLinkedTile(build, tile -> {
            total[0] += tile.creeper;
            tile.creeper = 0f;
        });
        return total[0];
    }

    private static void clampAreaToMaxCreep(Building build) {
        eachLinkedTile(build, tile -> tile.creeper = Math.min(tile.creeper, CreeperCore.creeperTile.maxCreeper));
    }

    private static void upgrade(Building build, float value, float threshold, Block target) {
        if (build == null || build.tile == null || target == null || threshold <= 0f) return;
        if (value > threshold) Core.app.post(() -> build.tile.setNet(target, creepTeam, 0));
    }

    private static void appendUpgrade(StringBuilder sb, Building build, float value, float threshold, Block target) {
        if (target == null || threshold <= 0f) return;
        sb.append("[green]\uE804[] - [stat]")
                .append((int)(value / threshold * 100f))
                .append("%[]\n");
        upgrade(build, value, threshold, target);
    }

    private static final class Entry {
        final Boolf<Building> filter;
        final Func<Building, BuildingLogic> factory;

        Entry(Boolf<Building> filter, Func<Building, BuildingLogic> factory) {
            this.filter = filter;
            this.factory = factory;
        }
    }

    public interface BuildingLogic {
        Building build();

        default void added() {
        }

        default void update() {
        }

        default void removed(boolean resetEvent) {
        }
    }

    private abstract static class BaseBuildingLogic implements BuildingLogic {
        protected final Building build;
        protected WorldLabel label;
        private final Interval fxTimer = new Interval();

        BaseBuildingLogic(Building build) {
            this.build = build;
        }

        @Override
        public Building build() {
            return build;
        }

        @Override
        public void added() {
            makePseudoBoss();
            label = WorldLabel.create();
            if (label != null) {
                label.set(build);
                label.add();
                label.text = "[accent]Creeper[]";
            }
        }

        @Override
        public void removed(boolean resetEvent) {
            if (CreeperCore.enabled()) clampAreaToMaxCreep(build);
            if (label != null) {
                label.hide();
                label = null;
            }
        }

        protected void makePseudoBoss() {
            build.maxHealth = healthOffset;
            build.heal();
        }

        protected void setLabel(String text) {
            if (label == null) return;
            label.set(build);
            label.text = text;
        }

        protected void buildFx(Effect fx) {
            if (fxTimer.get(60f)) {
                Call.effect(fx, build.x, build.y, build.block.size, creepTeam.color);
            }
        }
    }

    public static class CoreEmitterLogic extends BaseBuildingLogic {
        private final float amountPerSecond;
        private final float intervalSeconds;
        private final float maxLayer;
        private final float upgradeThreshold;
        private final Block upgradeBlock;
        private final float nullifyDamageRequired;
        private final float nullifyTimeoutFrames;
        private final boolean canClear;

        private final Interval timer = new Interval();
        private float overflow = 0f;
        private float nullifyDamage = 0f;
        private float nullifyTimeout = 0f;
        private float floodDam = 0f;

        CoreEmitterLogic(Building build, float amountPerSecond, float intervalSeconds, float maxLayer,
                         float upgradeThreshold, Block upgradeBlock,
                         float nullifyDamageRequired, float nullifyTimeoutFrames, boolean canClear) {
            super(build);
            this.amountPerSecond = amountPerSecond;
            this.intervalSeconds = intervalSeconds;
            this.maxLayer = maxLayer;
            this.upgradeThreshold = upgradeThreshold;
            this.upgradeBlock = upgradeBlock;
            this.nullifyDamageRequired = nullifyDamageRequired;
            this.nullifyTimeoutFrames = nullifyTimeoutFrames;
            this.canClear = canClear;
        }

        @Override
        public void update() {
            nullifyTimeout -= Time.delta;

            if (build.damaged()) {
                nullifyDamage += build.maxHealth - build.health;
                build.heal();
                if (nullifyDamage > nullifyDamageRequired) {
                    nullifyDamage = 0f;
                    nullifyTimeout = nullifyTimeoutFrames;
                    overflow = Math.max(0f, overflow - nullifyDamageRequired / 300f);
                }
            }

            if (timer.get(intervalSeconds * 60f)) {
                if (nullified()) {
                    clearCreeperArea(build);
                } else if (build.enabled) {
                    applyItemEffect();

                    float amtEach = amountPerSecond * intervalSeconds / build.block.size / build.block.size;
                    eachLinkedTile(build, tile -> {
                        addCreeper(tile, amtEach);
                        if (maxLayer > 0f && tile.creeper > maxLayer) {
                            overflow += tile.creeper - maxLayer;
                            tile.creeper = maxLayer;
                        }
                    });
                }
            }

            StringBuilder sb = new StringBuilder();
            if (!build.enabled) sb.append("[red]\uE815 已禁用 \uE815[]\n");
            if (floodDam != 0f && build.enabled) {
                sb.append("额外")
                        .append(floodDam > 0f ? "[red]出水" : "[green]吸水")
                        .append(" ").append((int)floodDam).append("[]\n");
            }
            sb.append("[stat]伤害[white] ")
                    .append((int)nullifyDamage)
                    .append("/")
                    .append((int)nullifyDamageRequired)
                    .append("\n");
            if (nullified()) {
                sb.append("[red]**[yellow] 压制中 [red]**[]\n");
                buildFx(Fx.placeBlock);
            }
            appendUpgrade(sb, build, overflow, upgradeThreshold, upgradeBlock);
            if (canClear) {
                sb.append("[stat]目标[white] [white]在旁边完全启动").append(Blocks.impactReactor.emoji());
            } else {
                sb.append("[stat]目标[white] [white]持续压制");
            }
            setLabel(sb.toString());
        }

        private boolean nullified() {
            return nullifyTimeout > 0f;
        }

        private void applyItemEffect() {
            floodDam = 0f;
            if (build instanceof CoreBuild || build.items == null || build.items.empty()) return;

            if (build.items.has(Items.sporePod)) {
                build.items.remove(Items.sporePod, 1);
                floodDam = 100f;
            } else if (build.items.has(Items.blastCompound)) {
                build.items.remove(Items.blastCompound, 1);
                floodDam = 10f;
            } else if (build.items.has(Items.pyratite)) {
                build.items.remove(Items.pyratite, 1);
                floodDam = 1f;
            } else if (build.items.has(Items.sand)) {
                build.items.remove(Items.sand, 1);
                floodDam = -10f;
            } else if (build.items.has(Items.coal)) {
                build.items.remove(Items.coal, 1);
                floodDam = -1f;
            }

            if (floodDam != 0f) {
                eachLinkedTile(build, tile -> setCreeper(tile, Math.min(CreeperCore.creeperTile.maxCreeper, tile.creeper + floodDam)));
            }
        }

        public boolean targetFinished() {
            return nullified();
        }

        public boolean canClear() {
            return canClear;
        }
    }

    public static class ChargedEmitterLogic extends BaseBuildingLogic {
        private final float amountPerSecond;
        private final float intervalSeconds;
        private final float protectDps;
        private final float chargeSpeed;
        private final float chargeCap;
        private final float upgradeThreshold;
        private final Block upgradeBlock;

        private final Interval timer = new Interval(3);
        private boolean emitting = false;
        private boolean dpsOk = false;
        private float overflow = 0f;
        private float lastDps = 0f;

        ChargedEmitterLogic(Building build, float amountPerSecond, float intervalSeconds, float protectDps,
                            float chargeSpeed, float chargeCap, float upgradeThreshold, Block upgradeBlock) {
            super(build);
            this.amountPerSecond = amountPerSecond;
            this.intervalSeconds = intervalSeconds;
            this.protectDps = protectDps;
            this.chargeSpeed = chargeSpeed;
            this.chargeCap = chargeCap;
            this.upgradeThreshold = upgradeThreshold;
            this.upgradeBlock = upgradeBlock;
        }

        @Override
        public void added() {
            super.added();
            timer.reset(1, 0f);
        }

        @Override
        public void update() {
            if (emitting && build.enabled) {
                if (timer.get(0, intervalSeconds * 60f)) {
                    addCreeperArea(build, amountPerSecond * intervalSeconds);
                }

                if (timer.get(1, chargeCap)) {
                    emitting = false;
                    if (overflow < maxOverflow()) overflow += drainCreeperArea(build);
                    timer.reset(1, 0f);
                }
            } else if (timer.get(1, chargeCap / chargeSpeed)) {
                emitting = true;
                timer.reset(1, 0f);
            }

            lastDps = build.maxHealth - build.health;
            if (timer.get(2, 60f)) {
                build.heal();
                dpsOk = lastDps > protectDps;
                if (dpsOk) {
                    overflow -= protectCost() * Mathf.log2(lastDps / protectDps + 1f);
                    Call.effect(Fx.healBlock, build.x, build.y, build.block.size, creepTeam.color);
                    if (overflow + overflowOffset() < 0f) Core.app.post(build::kill);
                }
            }

            StringBuilder sb = new StringBuilder();
            if (!build.enabled) sb.append("[red]\uE815 已禁用 \uE815[]\n");
            appendUpgrade(sb, build, Math.max(0f, overflow), upgradeThreshold, upgradeBlock);
            if (emitting && build.enabled) {
                buildFx(Fx.launch);
            } else {
                float chargeNeed = chargeCap / chargeSpeed;
                sb.append("[red]⚠[] - [stat] ")
                        .append((int)(timer.getTime(1) / chargeNeed * 100f))
                        .append("%[]\n");
            }
            sb.append("[stat]DPS[] [")
                    .append(dpsOk ? "green" : "red")
                    .append("] ")
                    .append(String.format(java.util.Locale.ROOT, "%.1f", lastDps))
                    .append(dpsOk ? " > " : " < ")
                    .append((int)protectDps)
                    .append("[]\n");
            if (dpsOk) {
                sb.append("[yellow]\uE84D[] [stat] ")
                        .append(String.format(java.util.Locale.ROOT, "%.1f", (overflow + overflowOffset()) / protectCost()))
                        .append("s[] ([stat]-")
                        .append(String.format(java.util.Locale.ROOT, "%.1f", Mathf.log2(lastDps / protectDps + 1f)))
                        .append("s[])");
            } else {
                sb.append("[stat]目标[white] 达到所需DPS");
            }
            setLabel(sb.toString());
        }

        private float protectCost() {
            return protectDps * dmgPerCreeper;
        }

        private float overflowOffset() {
            return protectCost() * 30f;
        }

        private float maxOverflow() {
            return protectCost() * 180f;
        }
    }

    public static class ImpactReactorLogic extends BaseBuildingLogic {
        private final Interval timer = new Interval(2);

        ImpactReactorLogic(Building build) {
            super(build);
        }

        @Override
        public void added() {
            // No pseudo-boss and no label needed for player-side impact reactor.
        }

        @Override
        public void update() {
            if (!(build instanceof ImpactReactor.ImpactReactorBuild)) return;
            ImpactReactor.ImpactReactorBuild reactor = (ImpactReactor.ImpactReactorBuild) build;

            Seq<CoreEmitterLogic> targets = new Seq<>();
            for (BuildingLogic logic : activeBuildings.values()) {
                if (!(logic instanceof CoreEmitterLogic)) continue;
                CoreEmitterLogic core = (CoreEmitterLogic) logic;
                if (core.canClear() && build.dst(core.build) <= impactRange) targets.add(core);
            }
            if (targets.isEmpty()) return;

            if (timer.get(0, (2f - reactor.warmup) * 60f)) {
                for (CoreEmitterLogic target : targets) {
                    Geometry.iterateLine(0f, build.x, build.y, target.build.x, target.build.y,
                            Mathf.clamp((1f - reactor.warmup) * 16f, 1f, 4f), (x, y) -> {
                                Timer.schedule(() -> Call.effect(Fx.lancerLaserChargeBegin, x, y, 1f, Pal.accent), build.dst(x, y) / impactRange);
                            });
                }
            }

            if (reactor.canConsume() || reactor.power.status >= 0.99f) {
                if (timer.get(1, 60f)) {
                    for (CoreEmitterLogic target : targets) target.build.damage(build.team, 1f);
                }

                if (reactor.warmup >= 0.999f) {
                    Core.app.post(() -> {
                        for (CoreEmitterLogic target : targets) {
                            target.build.damage(build.team, 1f);
                            target.build.kill();
                        }
                        build.tile.setNet(Blocks.air);
                    });
                }
            }
        }
    }

    public static class NuclearReactorLogic extends BaseBuildingLogic {
        private final Interval timer = new Interval();
        public static float sporeOffset = 16f;
        public static float maxDistance = 120f;

        NuclearReactorLogic(Building build) {
            super(build);
        }

        @Override
        public void update() {
            if (timer.get(60f) && Mathf.chance(0.3f)) {
                Call.setItem(build, Items.thorium, Mathf.random(0, 3));
            }
            setLabel("[scarlet]孢子发射器[]\n[stat]热量[white] " + (build instanceof NuclearReactor.NuclearReactorBuild ? (int)(((NuclearReactor.NuclearReactorBuild) build).heat * 100f) : 0) + "%");
        }

        public void onDestroy() {
            if (!(build instanceof NuclearReactor.NuclearReactorBuild)) return;
            NuclearReactor.NuclearReactorBuild reactor = (NuclearReactor.NuclearReactorBuild) build;
            if (reactor.heat < 0.999f) return;

            Tile target = findTarget();
            if (target != null) {
                addCreeper(target, 500f);
                Call.effect(Fx.sapExplosion, target.worldx(), target.worldy(), 5f, creepTeam.color);
            }

            Core.app.post(() -> {
                if (CreeperCore.enabled() && build.tile != null) {
                    build.tile.setNet(Blocks.thoriumReactor, creepTeam, 0);
                }
            });
        }

        private Tile findTarget() {
            for (int i = 0; i < 10; i++) {
                Player player = Groups.player.first();
                if (player == null || player.unit() == null || !player.unit().isValid()) continue;
                for (int j = 0; j < 100; j++) {
                    float x = player.x + Mathf.range(sporeOffset * Vars.tilesize);
                    float y = player.y + Mathf.range(sporeOffset * Vars.tilesize);
                    Tile tile = Vars.world.tileWorld(x, y);
                    if (tile != null) return tile;
                }
            }
            return build.tile;
        }
    }
}
