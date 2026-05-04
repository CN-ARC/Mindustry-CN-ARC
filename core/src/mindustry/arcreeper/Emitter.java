package mindustry.arcreeper;

import arc.util.Interval;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

/**
 * 单个 Creeper Emitter。
 *
 * 这个类只负责一个建筑的出水/吸水逻辑。
 *
 * amt:
 *   每次触发时对整个建筑占地范围添加的 creeper 总量。
 *   正数 = 出 Creeper。
 *   负数 = 出 Anti-Creeper / 吸 Creeper。
 *
 * intervals:
 *   每隔多少秒触发一次。
 *
 * maxLayer:
 *   单格最大层数限制。
 *   > 0 时启用限制。
 *   <= 0 时不限制。
 */
public class Emitter {
    /** 绑定的建筑。Emitter 的位置、大小、队伍都从这个建筑读取。 */
    private final Building build;

    /** Mindustry 自带计时器，用于控制 intervals 间隔触发。 */
    private final Interval timer = new Interval();

    /** 每次触发时的总出水量，不是每秒出水量。 */
    private float amt;

    /** 每隔多少秒触发一次。 */
    private float intervals;

    /** 单格最大 creeper 层数；<= 0 表示无限制。 */
    private float maxLayer;

    public Emitter(Building build, float amt, float intervals, float maxLayer) {
        this.build = build;
        this.amt = amt;

        // 避免 intervals <= 0 导致计时器异常或每帧无限触发。
        this.intervals = Math.max(intervals, 1f / 60f);

        this.maxLayer = maxLayer;
    }

    public Building build() {
        return build;
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
     * 重新设置 Emitter 参数。
     *
     * 用于同一个 Building 已经存在 Emitter 时，直接更新参数，
     * 避免重复创建多个 Emitter。
     */
    public void set(float amt, float intervals, float maxLayer) {
        this.amt = amt;
        this.intervals = Math.max(intervals, 1f / 60f);
        this.maxLayer = maxLayer;
    }

    /**
     * 判断这个 Emitter 绑定的建筑是否仍然有效。
     *
     * 需要检查：
     * 1. build 不为空；
     * 2. build.tile 不为空；
     * 3. tile 上当前建筑仍然是这个 build；
     * 4. build 自身仍然有效。
     *
     * 如果建筑被拆除、替换、死亡，就应该从 Emitters 中移除。
     */
    public boolean valid() {
        return build != null
                && build.tile != null
                && build.tile.build == build
                && build.isValid();
    }

    /**
     * 每帧调用。
     *
     * 但不会每帧都真正出水。
     * 只有 timer 达到 intervals 秒后，才会执行一次 emit(amt)。
     */
    public void update() {
        if (!valid()) return;
        if (!build.enabled) return;
        if (amt == 0f) return;

        // Interval 使用的是帧数，所以 seconds * 60。
        if (!timer.get(intervals * 60f)) return;

        emit(amt);
    }

    /**
     * 对建筑占地范围出水。
     *
     * totalAmount 是整个建筑总共添加的量。
     * 例如 2x2 建筑，amt = 40，则每格 +10。
     */
    private void emit(float totalAmount) {
        if (totalAmount == 0f || build.tile == null) return;

        int size = Math.max(1, build.block.size);

        /*
         * Mindustry 的多格建筑以中心 tile 作为 build.tile。
         * offset 用来从中心 tile 推出建筑占地范围左下角。
         *
         * size = 1, offset = 0
         * size = 2, offset = 0
         * size = 3, offset = -1
         * size = 4, offset = -1
         */
        int offset = -(size - 1) / 2;

        // 将总出水量平均分配到建筑占据的每个 tile。
        float each = totalAmount / (size * size);

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Tile tile = Vars.world.tile(
                        build.tile.x + offset + dx,
                        build.tile.y + offset + dy
                );

                if (tile == null) continue;

                tile.creeper += each;
                clamp(tile);
            }
        }
    }

    /**
     * 限制单格最大层数。
     *
     * 正数 Emitter 只限制正 Creeper 的上限。
     * 负数 Emitter 只限制负 Creeper 的下限。
     *
     * 例如：
     * amt > 0, maxLayer = 10，则 tile.creeper 最大为 10。
     * amt < 0, maxLayer = 10，则 tile.creeper 最小为 -10。
     */
    private void clamp(Tile tile) {
        if (maxLayer <= 0f) return;

        // 不允许超过 CreeperTile 的全局最大层数。
        float limit = Math.min(maxLayer, CreeperCore.creeperTile.maxCreeper);

        if (amt > 0f && tile.creeper > limit) {
            tile.creeper = limit;
        } else if (amt < 0f && tile.creeper < -limit) {
            tile.creeper = -limit;
        }
    }
}