package mindustry.arcreeper;

import arc.util.Interval;
import mindustry.gen.Building;
import mindustry.world.Tile;

/**
 * Single Creeper emitter building behavior.
 *
 * <p>It periodically adds Creeper or Anti-Creeper to the full occupied area of
 * the bound building.</p>
 *
 * <ul>
 *     <li>{@code amt}: total amount added each trigger. Positive values emit
 *     Creeper; negative values emit Anti-Creeper / drain Creeper.</li>
 *     <li>{@code intervals}: trigger interval in seconds.</li>
 *     <li>{@code maxLayer}: per-tile maximum layer. Values {@code <= 0} disable
 *     the limit.</li>
 * </ul>
 */
public class Emitter extends CreeperBuilding {
    /** Mindustry frame timer used to trigger emission at {@link #intervals}. */
    private final Interval timer = new Interval();

    /** Total amount emitted per trigger, not per second. */
    private float amt;

    /** Trigger interval in seconds. */
    private float intervals;

    /** Per-tile Creeper layer cap; {@code <= 0} means unlimited. */
    private float maxLayer;

    public Emitter(Building build, float amt, float intervals, float maxLayer) {
        super(build);
        set(amt, intervals, maxLayer);
    }

    public float amt() {
        return amt;
    }

    public float intervals() {
        return intervals;
    }

    public float maxLayer() {
        return maxLayer;
    }

    /**
     * Replaces this emitter's parameters without creating a duplicate behavior
     * for the same building.
     */
    public void set(float amt, float intervals, float maxLayer) {
        this.amt = amt;

        // Prevent intervals <= 0 from causing timer edge cases or per-frame spam.
        this.intervals = Math.max(intervals, 1f / 60f);
        this.maxLayer = maxLayer;
    }

    /**
     * Updates timer state and emits only when the configured interval is reached.
     */
    @Override
    public void update() {
        if (!valid()) return;
        if (!build.enabled) return;
        if (amt == 0f) return;

        // Interval works in frames, so seconds are converted to 60 FPS frames.
        if (!timer.get(intervals * 60f)) return;

        emit(amt);
    }

    /**
     * Emits the total amount evenly over all tiles occupied by the building.
     */
    private void emit(float totalAmount) {
        if (totalAmount == 0f || build.tile == null) return;

        build.tile.creeper += totalAmount;  //修订，直接加在核心格子上
        /*
        int size = blockSize();
        float each = totalAmount / (size * size);

        eachLinkedTile(tile -> {
            tile.creeper += each;
            clamp(tile);
        });*/
    }

    /**
     * Applies directional layer caps while preserving positive/negative polarity.
     */
    private void clamp(Tile tile) {
        if (tile == null || maxLayer <= 0f) return;

        // Never allow an emitter-specific cap above the global Creeper cap.
        float limit = Math.min(maxLayer, CreeperCore.creeperTile.maxCreeper);

        if (amt > 0f && tile.creeper > limit) {
            tile.creeper = limit;
        } else if (amt < 0f && tile.creeper < -limit) {
            tile.creeper = -limit;
        }
    }
}
