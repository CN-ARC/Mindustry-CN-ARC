package mindustry.arcreeper;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.world.Tile;

public class CreeperTile {
    private float[][] creeperData; // for later multiplayer sync

    public float minCreeper = 0.01f;
    public float maxCreeper = 1000f;
    public float flowRate = 0.18f;

    private float updateTimer = 0f;
    public float timeInterval = 0.02f;

    float log2Min = Mathf.log2(minCreeper);
    float log2Max = Mathf.log2(maxCreeper);

    public Color creeperColor = new Color(0.1f, 0.35f, 1f, 1f);
    public Color anticreeperColor = new Color(0.45f, 0.85f, 1f, 1f);

    // 地形高度到 creeper 深度的换算倍率。
    public float heightScale = 1f;

    // 最小流动阈值。
    public float minFlow = 0.001f;

    public void init() {
        reset();
        initTileHeight();

        Events.on(EventType.TileChangeEvent.class, t -> {
            if (!CreeperCore.enabled()) return;
            updateTileHeight(t.tile);
        });
    }

    public void initTileHeight() {
        Vars.world.tiles.eachTile(this::updateTileHeight);
    }

    void updateTileHeight(Tile tile) {
        tile.height = tile.block().creeperHeight + tile.floor().creeperHeight;
    }

    public void reset() {
        Vars.world.tiles.eachTile(tile -> tile.creeper = 0f);
        clearTmp();
    }

    private void clearTmp() {
        Vars.world.tiles.eachTile(tile -> tile.creeperTmp = 0f);
    }

    public void set(int x, int y, float value) {
        Tile tile = Vars.world.tile(x, y);
        if (tile != null) {
            tile.creeper = value;
        }
    }

    public void add(Tile tile, float value) {
        if (tile == null) return;
        tile.creeper += value;
    }

    public void add(int x, int y, float value) {
        add(Vars.world.tile(x, y), value);
    }

    public void addArea(Tile tile, int size, float value) {
        if (tile == null || size <= 0) return;

        int offset = -(size - 1) / 2;
        float each = value / Math.max(1, size * size);

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                add(tile.x + offset + dx, tile.y + offset + dy, each);
            }
        }
    }

    /**
     * 推进 creeper 模拟。
     */
    public void update() {
        updateTimer += Time.delta / 60f;
        if (updateTimer < timeInterval) return;
        updateTimer -= timeInterval;

        clearTmp();

        updateFlow();

        Vars.world.tiles.eachTile(tile -> {
            tile.creeper += tile.creeperTmp;

            if (Math.abs(tile.creeper) < minCreeper) {
                tile.creeper = 0f;
            }
        });
    }

    void updateFlow() {
        Vars.world.tiles.eachTile(tile -> {
            flowBetween(tile, Vars.world.tile(tile.x + 1, tile.y), flowRate);
            flowBetween(tile, Vars.world.tile(tile.x, tile.y + 1), flowRate);
        });
    }

    /**
     * 获取 tile 的地形高度，并转换为 creeper 深度单位。
     */
    float heightOf(Tile tile) {
        return tile.height * heightScale;
    }

    /**
     * 获取 creeper 的极性。
     */
    int signOf(float value) {
        if (value > minCreeper) return 1;
        if (value < -minCreeper) return -1;
        return 0;
    }

    /**
     * 将指定极性的流体从 from 转移到 to。
     */
    void transfer(Tile from, Tile to, int sign, float amount) {
        from.creeperTmp -= sign * amount;
        to.creeperTmp += sign * amount;
    }

    void flowBetween(Tile a, Tile b, float rate) {
        if (a == null || b == null) return;

        int signA = signOf(a.creeper);
        int signB = signOf(b.creeper);

        if (signA == 0 && signB == 0) return;

        if (signA == signB || signA == 0 || signB == 0) {
            flowSameSign(a, b, signA, signB, rate);
        } else {
            flowOppositeSign(a, b, signA, signB, rate);
        }
    }

    /**
     * 处理同号传播，或者一边为空的传播。
     * <p>
     * 适用情况：
     * + creeper      对 creeper
     * - antiCreeper  对 antiCreeper
     * + creeper      对空 tile
     * - antiCreeper  对空 tile
     * <p>
     * 传播规则：
     * <p>
     * depth   = abs(creeper)
     * surface = height + depth
     * <p>
     * 只有 surface 较高的一侧会向 surface 较低的一侧传播。
     * <p>
     * 对于 antiCreeper，sign = -1，
     * 但 depth 仍然是正数，因此它和 creeper 的流动规则完全一致。
     */
    void flowSameSign(Tile a, Tile b, int signA, int signB, float rate) {
        int sign = signA != 0 ? signA : signB;

        float depthA = Math.max(0f, a.creeper * sign);
        float depthB = Math.max(0f, b.creeper * sign);

        float surfaceA = heightOf(a) + depthA;
        float surfaceB = heightOf(b) + depthB;

        float diff = surfaceA - surfaceB;

        if (diff > minFlow) {
            float amount = Math.min(diff, depthA) * rate;
            if (amount < minFlow) return;

            transfer(a, b, sign, amount);
        } else if (diff < -minFlow) {
            float amount = Math.min(-diff, depthB) * rate;
            if (amount < minFlow) return;

            transfer(b, a, sign, amount);
        }
    }

    /**
     * 处理 creeper 与 antiCreeper 相邻时的传播与抵消。
     * <p>
     * 这里不能使用：
     * <p>
     * a.creeper - b.creeper
     * <p>
     * 因为当 a = +10, b = -10 时，
     * signed diff 会得到 20，导致传播速度变成正常值的两倍。
     * <p>
     * 正确做法：
     * <p>
     * 1. 对双方都使用 abs(creeper) 作为水深；
     * 2. 使用 height + depth 判断是否能越过对方地形；
     * 3. 分别计算双方的推进能力；
     * 4. 只选择推进能力更强的一侧进行传播，而不是把两侧能力相加。
     * <p>
     * 这样可以保证：
     * - creeper 与 antiCreeper 完全对称；
     * - antiCreeper 也必须拥有足够深度才能越过地形；
     * - 同高度 +x 与 -x 相遇时，传播量是 x * rate，而不是 2x * rate。
     */
    void flowOppositeSign(Tile a, Tile b, int signA, int signB, float rate) {
        float depthA = Math.abs(a.creeper);
        float depthB = Math.abs(b.creeper);

        float heightA = heightOf(a);
        float heightB = heightOf(b);

        float reachA = Math.max(0f, heightA + depthA - heightB);
        float reachB = Math.max(0f, heightB + depthB - heightA);

        float capA = Math.min(depthA, reachA);
        float capB = Math.min(depthB, reachB);

        if (capA <= minFlow && capB <= minFlow) return;

        if (capA >= capB) {
            float amount = capA * rate;
            if (amount < minFlow) return;

            transfer(a, b, signA, amount);
        } else {
            float amount = capB * rate;
            if (amount < minFlow) return;

            transfer(b, a, signB, amount);
        }
    }

    public void draw() {
        Draw.z(120f);

        Vars.world.tiles.eachTile(tile -> {
            float raw = tile.creeper;

            // creeper = 0 时不绘制
            if (raw == 0f) return;

            // 负数表示 Anti-Creeper
            boolean anti = raw < 0f;

            // 强度按绝对值计算
            float value = Math.abs(raw);

            // 绝对值低于阈值则不显示
            if (value < minCreeper) return;

            float v = Mathf.clamp(value, minCreeper, maxCreeper);

            int bits = Float.floatToIntBits(v);
            int exp = ((bits >>> 23) & 0xFF) - 127;

            float normalized = Mathf.clamp((exp - log2Min) / (log2Max - log2Min), 0f, 1f);
            float alpha = 0.2f + normalized * 0.7f;

            Color color = anti ? anticreeperColor : creeperColor;
            Draw.color(color);
            Draw.alpha(alpha);
            //Draw.color(color.r, color.g, color.b, alpha);

            Fill.square(
                    tile.worldx(),
                    tile.worldy(),
                    Vars.tilesize / 2f
            );
        });

        Draw.color();
    }
}