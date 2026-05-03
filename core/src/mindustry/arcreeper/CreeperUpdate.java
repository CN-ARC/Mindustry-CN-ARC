package mindustry.arcreeper;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.Vars;
import mindustry.world.Tile;

public class CreeperUpdate {
    private float[][] creeperData; // for later multiplayer sync

    public float maxAmount = 1000f;
    public float flowRate = 0.18f;

    public void init() {
        reset();
    }

    public void reset() {
        Vars.world.tiles.eachTile(tile -> tile.creeper = 0f);
        clearTmp();
    }

    private void clearTmp() {
        Vars.world.tiles.eachTile(tile -> tile.creeperTmp = 0f);
    }

    public void set(int x, int y, float value) {
        Vars.world.tile(x, y).creeper = value;
    }

    public void add(int x, int y, float value) {
        add(Vars.world.tile(x, y), value);
    }

    public void add(Tile tile, float value) {
        tile.creeper += value;
    }

    public void addArea(Tile tile, int size, float value) {
        int offset = -(size - 1) / 2;
        float each = value / Math.max(1, size * size);

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                add(tile.x + offset + dx, tile.y + offset + dy, each);
            }
        }
    }

    public void update() {
        clearTmp();
        updateFlow();

        Vars.world.tiles.eachTile(tile -> tile.creeper = tile.creeperTmp);
    }

    void updateFlow() {
        Vars.world.tiles.eachTile(tile -> {
            tile.creeperTmp = tile.creeper;
            tile.creeperTmp -= flowFrom(Vars.world.tile(tile.x - 1, tile.y), tile);
            tile.creeperTmp -= flowFrom(Vars.world.tile(tile.x, tile.y - 1), tile);
        });
    }

    float flowFrom(Tile tile, Tile oriTile) {
        if (tile == null) return 0f;
        float flowAmount = (oriTile.creeper - tile.creeper) * flowRate;
        tile.creeper += flowAmount;
        return flowAmount;
    }

    public void draw() {
        Draw.z(30f);

        Vars.world.tiles.eachTile(tile -> {
            float value = tile.creeper;
            if(value <= 0.01f) return;

            float alpha = Math.min(value / maxAmount, 1f) * 0.75f;

            Draw.color(0.1f, 0.35f, 1f, alpha);

            Fill.square(
                    tile.worldx(),
                    tile.worldy(),
                    Vars.tilesize / 2f
            );
        });

        Draw.color();
    }
}