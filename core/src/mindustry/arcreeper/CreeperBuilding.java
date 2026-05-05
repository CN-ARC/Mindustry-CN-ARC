package mindustry.arcreeper;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

/**
 * Base type for every building-bound Creeper behavior.
 *
 * <p>A CreeperBuilding is attached to a Mindustry {@link Building} and provides
 * shared lifecycle, validity and occupied-tile traversal helpers. Concrete
 * implementations, such as {@link Emitter}, only need to implement their own
 * update behavior.</p>
 */
public abstract class CreeperBuilding {
    /** Bound Mindustry building. Position, size and team are read from it. */
    protected final Building build;

    protected CreeperBuilding(Building build) {
        this.build = build;
    }

    public Building build() {
        return build;
    }

    /**
     * Returns whether the bound building is still the active building on its tile.
     *
     * <p>Managers use this before updating so removed, replaced or invalid
     * buildings can be dropped safely.</p>
     */
    public boolean valid() {
        return build != null
                && build.tile != null
                && build.tile.build == build
                && build.isValid();
    }

    /** Called once when this behavior is first attached. */
    public void added() {
    }

    /** Called every frame while this behavior is valid. */
    public void update() {
    }

    /** Called when this behavior is removed or when Creeper mode resets. */
    public void removed(boolean resetEvent) {
    }

    /** Safe size helper for multi-tile buildings. */
    protected int blockSize() {
        if (build == null || build.block == null) return 1;
        return Math.max(1, build.block.size);
    }

    /**
     * Iterates every tile occupied by the bound building.
     *
     * <p>Mindustry stores multi-tile buildings on their center tile. The offset
     * calculation mirrors the existing emitter logic and preserves the original
     * distribution area for 1x1, 2x2, 3x3 and larger blocks.</p>
     */
    protected void eachLinkedTile(Cons<Tile> cons) {
        if (build == null || build.tile == null || cons == null) return;

        int size = blockSize();
        int offset = -(size - 1) / 2;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Tile tile = Vars.world.tile(build.tile.x + offset + dx, build.tile.y + offset + dy);
                if (tile != null) cons.get(tile);
            }
        }
    }
}
