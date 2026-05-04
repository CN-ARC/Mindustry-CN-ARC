package mindustry.arcreeper;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
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

    public void init() {
        reset();
        randomTest();
    }

    public void randomTest() {
        Vars.world.tile(100, 100).creeper = maxCreeper;
        Vars.world.tile(80, 100).creeper = -maxCreeper;

        clearTmp();
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
        if(tile != null){
            tile.creeper = value;
        }
    }

    public void add(Tile tile, float value) {
        if(tile == null) return;
        tile.creeper += value;
    }

    public void add(int x, int y, float value){
        add(Vars.world.tile(x, y),value);
    }

    public void addArea(Tile tile, int size, float value) {
        if(tile == null || size <= 0) return;

        int offset = -(size - 1) / 2;
        float each = value / Math.max(1, size * size);

        for(int dx = 0; dx < size; dx++){
            for(int dy = 0; dy < size; dy++){
                add(tile.x + offset + dx, tile.y + offset + dy, each);
            }
        }
    }

    public void update() {
        updateTimer += Time.delta / 60f;
        if(updateTimer < timeInterval) return;
        updateTimer -= timeInterval;

        float rate = Math.min(flowRate, 0.25f);

        Vars.world.tiles.eachTile(tile -> {
            tile.creeperTmp = 0f; // 只作为 delta 使用
        });

        updateFlow(rate);

        Vars.world.tiles.eachTile(tile -> {
            tile.creeper += tile.creeperTmp;

            // 可选：清除极小残留，避免浮点噪声
            if(Math.abs(tile.creeper) < 0.0001f){
                tile.creeper = 0f;
            }
        });
    }

    void updateFlow(float rate) {
        Vars.world.tiles.eachTile(tile -> {
            flowBetween(tile, Vars.world.tile(tile.x + 1, tile.y), rate);
            flowBetween(tile, Vars.world.tile(tile.x, tile.y + 1), rate);
        });
    }

    void flowBetween(Tile a, Tile b, float rate) {
        if(a == null || b == null) return;

        float diff = a.creeper - b.creeper;
        if(diff > -0.001f && diff < 0.001f) return;

        float amount = diff * rate;

        a.creeperTmp -= amount;
        b.creeperTmp += amount;
    }

    public void draw() {
        Draw.z(120f);

        final float min = minCreeper;
        final float max = maxCreeper;

        final float log2Max = ((Float.floatToIntBits(max) >>> 23) & 0xFF) - 127;

        Vars.world.tiles.eachTile(tile -> {
            float raw = tile.creeper;

            // creeper = 0 时不绘制
            if(raw == 0f) return;

            // 负数表示 Anti-Creeper
            boolean anti = raw < 0f;

            // 强度按绝对值计算
            float value = Math.abs(raw);

            // 绝对值低于阈值则不显示
            if(value < min) return;

            float v = Mathf.clamp(value, min, max);

            int bits = Float.floatToIntBits(v);
            int exp = ((bits >>> 23) & 0xFF) - 127;

            float normalized = Mathf.clamp((exp - log2Min) / (log2Max - log2Min), 0f, 1f);
            float alpha = 0.3f + normalized * 0.7f;

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