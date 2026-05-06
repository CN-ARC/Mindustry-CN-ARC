package mindustry.arcreeper;

import arc.func.Boolf;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.tilesize;

public final class CreeperCombat {
    private CreeperCombat() {}

    public static boolean targetCreeper = true;
    public static boolean damageCreeper = true;

    public static int maxScanTiles = 4096;

    /** creeper 伪目标半径，只在本类内部用于 target validation。 */
    public static float targetHitSize = tilesize;

    public static Posc bestTarget(
            Team team,
            float x,
            float y,
            float range,
            Boolf<Unit> unitPred,
            Boolf<Building> buildingPred,
            Units.Sortf unitSort,
            boolean allowCreeper,
            boolean allowBuilding
    ) {
        if (!CreeperCore.enabled()) {
            return allowBuilding
                    ? Units.bestTarget(team, x, y, range, unitPred, buildingPred, unitSort)
                    : Units.bestEnemy(team, x, y, range, unitPred, unitSort);
        }

        // 1. 单位优先。
        Unit unit = Units.bestEnemy(team, x, y, range, unitPred, unitSort);
        if (unit != null) return unit;

        // 2. creeper 次之。
        if (allowCreeper && targetCreeper) {
            CreeperTarget creeper = findCreeperTarget(team, x, y, range);
            if (creeper != null) return creeper;
        }

        // 3. 建筑最后。
        return allowBuilding ? Units.findEnemyTile(team, x, y, range, buildingPred) : null;
    }

    public static boolean invalidateTarget(Posc target, Team team, float x, float y, float range) {
        if (target instanceof CreeperTarget ct) {
            float realRange = range + targetHitSize / 2f;

            return !canAttackCreeper(team)
                    || !validCreeperTile(ct.tile)
                    || !within(x, y, ct.x(), ct.y(), realRange);
        }

        return Units.invalidateTarget(target, team, x, y, range);
    }

    public static CreeperTarget findCreeperTarget(Team attacker, float wx, float wy, float range) {
        if (!canAttackCreeper(attacker)) return null;

        int minX = toTile(wx - range);
        int maxX = toTile(wx + range);
        int minY = toTile(wy - range);
        int maxY = toTile(wy + range);

        float range2 = range * range;
        Tile best = null;
        float bestDst2 = Float.MAX_VALUE;
        int scanned = 0;

        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                if (++scanned > maxScanTiles) {
                    return best == null ? null : new CreeperTarget(best);
                }

                Tile tile = Vars.world.tile(tx, ty);
                if (!validCreeperTile(tile)) continue;

                float dx = tile.worldx() - wx;
                float dy = tile.worldy() - wy;
                float dst2 = dx * dx + dy * dy;

                if (dst2 > range2) continue;

                if (best == null
                        || dst2 < bestDst2
                        || (Mathf.equal(dst2, bestDst2) && tile.creeper > best.creeper)) {
                    best = tile;
                    bestDst2 = dst2;
                }
            }
        }

        return best == null ? null : new CreeperTarget(best);
    }

    public static float damageAt(Team attacker, float wx, float wy, float damage) {
        if (!damageCreeper || damage <= 0f || !canAttackCreeper(attacker)) return 0f;

        Tile tile = Vars.world.tileWorld(wx, wy);
        return damageTile(attacker, tile, damage);
    }

    public static float damageTile(Team attacker, Tile tile, float damage) {
        if (!damageCreeper || damage <= 0f || !canAttackCreeper(attacker)) return 0f;
        if (!validCreeperTile(tile)) return 0f;

        float damagePerCreeper = Math.max(CreeperCore.creeperTile.creeperDamage, 0.0001f);
        float consume = damage / damagePerCreeper;

        float used = Math.min(tile.creeper, consume);
        tile.creeper -= used;

        if (tile.creeper < CreeperCore.creeperTile.minCreeper) {
            tile.creeper = 0f;
        }

        return used * damagePerCreeper;
    }

    public static void splashDamage(Team attacker, float wx, float wy, float radius, float damage) {
        if (!damageCreeper || damage <= 0f || radius <= 0f || !canAttackCreeper(attacker)) return;

        int minX = toTile(wx - radius);
        int maxX = toTile(wx + radius);
        int minY = toTile(wy - radius);
        int maxY = toTile(wy + radius);

        float radius2 = radius * radius;
        int scanned = 0;

        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                if (++scanned > maxScanTiles) return;

                Tile tile = Vars.world.tile(tx, ty);
                if (!validCreeperTile(tile)) continue;

                float dx = tile.worldx() - wx;
                float dy = tile.worldy() - wy;
                float dst2 = dx * dx + dy * dy;

                if (dst2 > radius2) continue;

                float falloff = 1f - Mathf.sqrt(dst2) / radius;
                damageTile(attacker, tile, damage * falloff);
            }
        }
    }

    public static boolean canAttackCreeper(Team attacker) {
        return CreeperCore.enabled()
                && attacker != null
                && attacker != Team.derelict
                && attacker != CreeperCore.creeperTeam;
    }

    public static boolean validCreeperTile(Tile tile) {
        return tile != null
                && tile.creeper > CreeperCore.creeperTile.minCreeper;
    }

    private static int toTile(float world) {
        return Mathf.floor(world / tilesize);
    }

    private static boolean within(float x, float y, float tx, float ty, float range) {
        float dx = tx - x;
        float dy = ty - y;
        return dx * dx + dy * dy <= range * range;
    }

    /**
     * 不再 implements Sized。
     *
     * 它只是一个能被 Turret.target 持有的坐标目标。
     * hitSize 相关判断由 CreeperCombat.invalidateTarget() 自己处理。
     */
    public static final class CreeperTarget implements Posc {
        public final Tile tile;

        private float x;
        private float y;

        public CreeperTarget(Tile tile) {
            this.tile = tile;
            this.x = tile.worldx();
            this.y = tile.worldy();
        }

        @Override
        public float x() {
            return x;
        }

        @Override
        public void x(float x) {
            this.x = x;
        }

        @Override
        public float y() {
            return y;
        }

        @Override
        public int tileX() {
            return 0;
        }

        @Override
        public int tileY() {
            return 0;
        }

        @Override
        public Block blockOn() {
            return null;
        }

        @Override
        public Tile tileOn() {
            return null;
        }

        @Override
        public void y(float y) {
            this.y = y;
        }

        @Override
        public Floor floorOn() {
            return null;
        }

        @Override
        public Building buildOn() {
            return null;
        }

        @Override
        public boolean onSolid() {
            return false;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public void set(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void set(Position pos) {
            set(pos.getX(), pos.getY());
        }

        @Override
        public void trns(float x, float y) {
            this.x += x;
            this.y += y;
        }

        @Override
        public void trns(Position pos) {
            trns(pos.getX(), pos.getY());
        }

        @Override
        public String toString() {
            return "CreeperTarget{" + tile.x + "," + tile.y + ", creeper=" + tile.creeper + "}";
        }

        @Override
        public <T extends Entityc> T self() {
            return null;
        }

        @Override
        public <T> T as() {
            return null;
        }

        @Override
        public boolean isAdded() {
            return false;
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public boolean serialize() {
            return false;
        }

        @Override
        public int classId() {
            return 0;
        }

        @Override
        public int id() {
            return 0;
        }

        @Override
        public void add() {

        }

        @Override
        public void afterRead() {

        }

        @Override
        public void afterReadAll() {

        }

        @Override
        public void beforeWrite() {

        }

        @Override
        public void id(int id) {

        }

        @Override
        public void read(Reads read) {

        }

        @Override
        public void remove() {

        }

        @Override
        public void update() {

        }

        @Override
        public void write(Writes write) {

        }
    }
}