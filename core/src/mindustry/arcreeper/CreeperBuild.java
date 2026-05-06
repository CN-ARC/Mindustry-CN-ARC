package mindustry.arcreeper;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;

/**
 * Entry point and registry for Creeper-related building behaviors.
 *
 * <p>The current active implementation only manages simple core emitters. Other
 * experimental building logic has been moved to {@code CreeperBuildLegacyBackup}
 * as commented backup code.</p>
 */
public final class CreeperBuild {
    private static boolean initialized = false;

    /** Team that owns positive Creeper emitters. Must match the map/rules setup. */
    public static Team creepTeam = Team.blue;

    private CreeperBuild() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        Events.on(EventType.TilePreChangeEvent.class, e -> {
            if (!CreeperCore.enabled()) return;

            Building build = e.tile.build;
            if (build != null && build.tile == e.tile) {
                removeCreeperBuilding(build);
            }
        });

        Events.on(EventType.TileChangeEvent.class, e -> {
            if (!CreeperCore.enabled()) return;

            Building build = e.tile.build;
            if (build != null && build.tile == e.tile) {
                addCreeperBuilding(build);
            }
        });
    }

    /** Re-scans the world and attaches all active Creeper building behaviors. */
    public static void load() {
        reset(false);
        scanWorldBuildings();
        Log.info("CreeperBuildings loaded, emitters: @", Emitters.size());
    }

    /** Updates every active Creeper building behavior once per frame. */
    public static void update() {
        Emitters.update();
    }

    public static void reset() {
        reset(false);
    }

    public static void reset(boolean resetEvent) {
        Emitters.reset(resetEvent);
    }

    public static int activeCount() {
        return Emitters.size();
    }

    /** Attach supported Creeper behavior for a newly discovered building. */
    private static void addCreeperBuilding(Building build) {
        addCoreEmitterIfNeeded(build);
    }

    /** Remove all Creeper behavior owned by the given building. */
    private static void removeCreeperBuilding(Building build) {
        Emitters.removeEmitter(build);
    }

    /**
     * Scans only the known core lists used by the current emitter implementation.
     *
     * <p>This preserves the existing behavior: Creeper-team cores produce
     * positive Creeper, while Sharded cores produce Anti-Creeper.</p>
     */
// CreeperBuild.java
    private static void scanWorldBuildings() {
        if (Vars.world == null || Vars.world.tiles == null) return;

        CreeperCore.creeperTeam.cores().forEach(CreeperBuild::addCoreEmitterIfNeeded);
        CreeperCore.antiCreeperTeam.cores().forEach(CreeperBuild::addCoreEmitterIfNeeded);
    }

    private static void addCoreEmitterIfNeeded(Building build) {
        if (build == null || build.block == null) return;
        if (!isCoreEmitterBlock(build.block)) return;

        float amt = coreEmitterAmount(build.block);
        float intervals = coreEmitterIntervals(build.block);
        float maxLayer = coreEmitterMaxLayer(build.block);

        if (build.team == CreeperCore.creeperTeam) {
            Emitters.addEmitter(build, amt, intervals, maxLayer);
        } else if (build.team == CreeperCore.antiCreeperTeam) {
            Emitters.addEmitter(build, -amt, intervals, maxLayer);
        }
    }

    /** Returns whether the block is one of the currently supported core emitters. */
    private static boolean isCoreEmitterBlock(Block block) {
        return block == Blocks.coreShard
                || block == Blocks.coreFoundation
                || block == Blocks.coreNucleus
                || block == Blocks.coreBastion
                || block == Blocks.coreCitadel
                || block == Blocks.coreAcropolis;
    }

    /** Total amount emitted per trigger. This is not a per-second value. */
    private static float coreEmitterAmount(Block block) {
        if (block == Blocks.coreShard) return 4f;
        if (block == Blocks.coreFoundation) return 10f;
        if (block == Blocks.coreNucleus) return 20f;
        if (block == Blocks.coreBastion) return 13f;
        if (block == Blocks.coreCitadel) return 25f;
        if (block == Blocks.coreAcropolis) return 50f;

        return 4f;
    }

    /** Core emitter trigger interval in seconds. */
    private static float coreEmitterIntervals(Block block) {
        if (block == Blocks.coreShard) return 0.5f;
        if (block == Blocks.coreFoundation) return 0.4f;
        if (block == Blocks.coreNucleus) return 0.2f;
        if (block == Blocks.coreBastion) return 0.4f;
        if (block == Blocks.coreCitadel) return 0.2f;
        if (block == Blocks.coreAcropolis) return 0.2f;

        return 1f;
    }

    /** Core emitter per-tile maximum layer; {@code <= 0} means unlimited. */
    private static float coreEmitterMaxLayer(Block block) {
        if (block == Blocks.coreShard) return 5f;
        if (block == Blocks.coreFoundation) return 10f;

        return -1f;
    }

    /**
     * Base collection manager for a family of CreeperBuilding behaviors.
     *
     * <p>Subclasses provide domain-specific add/update wrappers while this base
     * class owns the common map, validity pruning and reset flow.</p>
     */
    public abstract static class CreeperBuildings<T extends CreeperBuilding> {
        protected final ObjectMap<Building, T> buildings = new ObjectMap<>();

        protected T get(Building build) {
            return buildings.get(build);
        }

        protected void put(T building) {
            if (building == null || building.build() == null) return;
            buildings.put(building.build(), building);
            building.added();
        }

        protected void remove(Building build, boolean resetEvent) {
            if (build == null) return;

            T building = buildings.remove(build);
            if (building != null) building.removed(resetEvent);
        }

        /** Updates valid behaviors and removes invalid ones after iteration. */
        protected void updateAll() {
            Seq<Building> invalid = new Seq<>();

            for (ObjectMap.Entry<Building, T> entry : buildings) {
                Building build = entry.key;
                T building = entry.value;

                if (building == null || !building.valid()) {
                    invalid.add(build);
                    continue;
                }

                building.update();
            }

            for (Building build : invalid) {
                remove(build, false);
            }
        }

        /** Removes every behavior, forwarding the reset reason to each one. */
        protected void resetAll(boolean resetEvent) {
            for (T building : buildings.values()) {
                if (building != null) building.removed(resetEvent);
            }
            buildings.clear();
        }

        protected int sizeInternal() {
            return buildings.size;
        }
    }

    /** Manager for all active {@link Emitter} behaviors. */
    public static final class Emitters extends CreeperBuildings<Emitter> {
        private static final Emitters instance = new Emitters();

        private Emitters() {
        }

        /**
         * Adds or updates an emitter bound to the given building.
         *
         * @param build bound building
         * @param amt total amount emitted per trigger; positive emits Creeper,
         *            negative emits Anti-Creeper
         * @param intervals trigger interval in seconds
         * @param maxLayer per-tile maximum layer; {@code <= 0} means unlimited
         */
        public static void addEmitter(Building build, float amt, float intervals, float maxLayer) {
            instance.add(build, amt, intervals, maxLayer);
        }

        /** Removes the emitter owned by the given building, if present. */
        public static void removeEmitter(Building build) {
            instance.remove(build, false);
        }

        /** Updates all emitters and prunes invalid entries safely. */
        public static void update() {
            instance.updateAll();
        }

        /** Clears all emitters, usually during world reset or Creeper shutdown. */
        public static void reset() {
            reset(false);
        }

        public static void reset(boolean resetEvent) {
            instance.resetAll(resetEvent);
        }

        public static int size() {
            return instance.sizeInternal();
        }

        private void add(Building build, float amt, float intervals, float maxLayer) {
            if (build == null || amt == 0f) return;

            Emitter old = get(build);
            if (old != null) {
                old.set(amt, intervals, maxLayer);
                return;
            }

            put(new Emitter(build, amt, intervals, maxLayer));
        }
    }
}
