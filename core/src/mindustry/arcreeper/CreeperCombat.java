package mindustry.arcreeper;

import arc.func.Boolf;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.World;
import mindustry.entities.Sized;
import mindustry.entities.Units;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.tilesize;

public final class CreeperCombat {
    private CreeperCombat() {}

    public static boolean targetCreeper = true;
    public static boolean damageCreeper = true;

    public static int maxScanTiles = 10000;

    /** creeper 伪目标半径，只在本类内部用于 target validation。 */
    public static float targetHitSize = tilesize;

    /**
     * 炮塔用目标选择。
     *
     * 原版 Turret.target 是 Posc，因此这里保留 Posc 返回值。
     */
    public static Posc bestTarget(
            Team team,
            float x,
            float y,
            float range,
            Boolf<Unit> unitPred,
            Boolf<Building> buildingPred,
            Units.Sortf unitSort,
            boolean allowCreeper,
            boolean allowBuilding,
            boolean targetHighestCreeper
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
            CreeperTarget creeper = findCreeperTarget(team, x, y, range, targetHighestCreeper);
            if (creeper != null) return creeper;
        }

        // 3. 建筑最后。
        return allowBuilding ? Units.findEnemyTile(team, x, y, range, buildingPred) : null;
    }

    /**
     * 单位/武器用目标选择。
     *
     * AIController 与 Weapon 的目标字段是 Teamc；CreeperTarget 实现 Teamc 后可以直接进入单位索敌链路。
     */
    public static Teamc closestTarget(
            Team team,
            float x,
            float y,
            float range,
            Boolf<Unit> unitPred,
            Boolf<Building> buildingPred,
            boolean allowCreeper,
            boolean allowBuilding,
            boolean targetHighestCreeper
    ) {
        if (!CreeperCore.enabled()) {
            return allowBuilding
                    ? Units.closestTarget(team, x, y, range, unitPred, buildingPred)
                    : Units.closestEnemy(team, x, y, range, unitPred);
        }

        // 1. 单位优先。
        Unit unit = Units.closestEnemy(team, x, y, range, unitPred);
        if (unit != null) return unit;

        // 2. creeper 次之。
        if (allowCreeper && targetCreeper) {
            CreeperTarget creeper = findCreeperTarget(team, x, y, range, targetHighestCreeper);
            if (creeper != null) return creeper;
        }

        // 3. 建筑最后。
        return allowBuilding ? Units.findEnemyTile(team, x, y, range, buildingPred) : null;
    }

    public static boolean invalidateTarget(Posc target, Team team, float x, float y, float range) {
        if (target instanceof CreeperTarget ct) {
            float realRange = range + targetHitSize / 2f;

            return !canAttackCreeper(team, ct.tile)
                    || !within(x, y, ct.x(), ct.y(), realRange);
        }

        return Units.invalidateTarget(target, team, x, y, range);
    }

    public static boolean invalidateTarget(Posc target, Team team, float x, float y) {
        return invalidateTarget(target, team, x, y, Float.MAX_VALUE);
    }

    public static boolean invalidateTarget(Teamc target, Unit targeter, float range) {
        return targeter == null || invalidateTarget(target, targeter.team(), targeter.x(), targeter.y(), range);
    }

    /**
     * 单位开火距离判断用。
     *
     * 原版会通过 Sized.hitSize() 扩展射程；CreeperTarget 不实现 Sized，因此这里统一处理。
     */
    public static float hitSize(Posc target) {
        if (target instanceof CreeperTarget) return targetHitSize;
        return target instanceof Sized sized ? sized.hitSize() : 0f;
    }

    public static CreeperTarget findCreeperTarget(Team attacker, float wx, float wy, float range, boolean targetHighestCreeper) {
        if (!canAttackCreeper(attacker)) return null;
        return targetHighestCreeper
                ? CreeperCore.creeperGrid.findHighestTarget(attacker, wx / tilesize, wy / tilesize, range / tilesize)
                : CreeperCore.creeperGrid.findNearestTarget(attacker, wx / tilesize, wy / tilesize, range / tilesize);
    }

    public static float damageAt(Team attacker, float wx, float wy, float damage) {
        if (!damageCreeper || damage <= 0f || !canAttackCreeper(attacker)) return 0f;

        Tile tile = Vars.world.tileWorld(wx, wy);
        return damageTile(attacker, tile, damage);
    }

    public static float damageTile(Team attacker, Tile tile, float damage) {
        if (!damageCreeper || damage <= 0f || !canAttackCreeper(attacker, tile)) return 0f;

        float damagePerCreeper = Math.max(Vars.state.rules.creeperDamage, 0.0001f);
        float consume = damage / damagePerCreeper;

        float amount = creeperAmount(tile);
        float used = Math.min(amount, consume);

        // 正 creeper 归 creeperTeam，负 creeper 归 antiCreeperTeam；伤害总是把绝对值推向 0。
        tile.creeper -= (tile.creeper > 0f ? 1f : -1f) * used;

        if (creeperAmount(tile) < Vars.state.rules.minCreeper) {
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
                if (!canAttackCreeper(attacker, tile)) continue;

                float dx = tile.worldx() - wx;
                float dy = tile.worldy() - wy;
                float dst2 = dx * dx + dy * dy;

                if (dst2 > radius2) continue;

                float falloff = 1f - Mathf.sqrt(dst2) / radius;
                damageTile(attacker, tile, damage * falloff);
            }
        }
    }

    /**
     * 是否允许该队伍参与 creeper 攻击逻辑。
     *
     * 这里只检查全局状态与攻击者合法性；具体 tile 是否敌对由 canAttackCreeper(attacker, tile) 判断。
     */
    public static boolean canAttackCreeper(Team attacker) {
        return CreeperCore.enabled()
                && attacker != null
                && attacker != Team.derelict;
    }

    /**
     * 是否可以攻击某个 creeper tile。
     *
     * tile.creeper > 0 归 creeperTeam；tile.creeper <= 0 归 antiCreeperTeam。
     */
    public static boolean canAttackCreeper(Team attacker, Tile tile) {
        return canAttackCreeper(attacker)
                && validCreeperTile(tile)
                && creeperTeam(tile) != attacker;
    }

    public static boolean validCreeperTile(Tile tile) {
        return tile != null
                && creeperAmount(tile) > Vars.state.rules.minCreeper;
    }

    /**
     * 根据 creeper 数值符号判断 tile 所属队伍。
     *
     * 正数属于 creeperTeam；0 或负数属于 antiCreeperTeam。
     * 注意：0 通常不会被 validCreeperTile() 视为有效目标。
     */
    public static Team creeperTeam(Tile tile) {
        return tile != null && tile.creeper > 0f
                ? Vars.state.rules.creeperTeam
                : Vars.state.rules.antiCreeperTeam;
    }

    public static float creeperAmount(Tile tile) {
        return tile == null ? 0f : Math.abs(tile.creeper);
    }

    private static int toTile(float world) {
        return Mathf.floor(world / tilesize);
    }

    private static boolean within(float x, float y, float tx, float ty, float range) {
        if (range == Float.MAX_VALUE) return true;

        float dx = tx - x;
        float dy = ty - y;
        return dx * dx + dy * dy <= range * range;
    }

    public static void lineDamage(
            Team attacker,
            float x,
            float y,
            float angle,
            float length,
            float damage,
            int pierceCap
    ) {
        if (!damageCreeper || damage <= 0f || length <= 0f || !canAttackCreeper(attacker)) return;

        Tmp.v1.trnsExact(angle, length);

        final int[] hits = {0};

        World.raycastEachWorld(x, y, x + Tmp.v1.x, y + Tmp.v1.y, (tx, ty) -> {
            Tile tile = Vars.world.tile(tx, ty);

            if (!canAttackCreeper(attacker, tile)) return false;

            float absorbed = damageTile(attacker, tile, damage);
            if (absorbed <= 0f) return false;

            hits[0]++;

            return pierceCap > 0 && hits[0] >= pierceCap;
        });

    }

    /**
     * creeper 伪目标。
     *
     * 炮塔只需要 Posc；单位 AI/Weapon 需要 Teamc。
     * 队伍按 tile.creeper 的符号动态判断，以兼容 creeperTeam 与 antiCreeperTeam。
     */
    public static final class CreeperTarget implements Teamc {
        public final Tile tile;

        private float x;
        private float y;

        public CreeperTarget(Tile tile) {
            this.tile = tile;
            this.x = tile.worldx();
            this.y = tile.worldy();
        }

        @Override
        public boolean inFogTo(Team viewer) {
            return false;
        }

        @Override
        public boolean cheating() {
            return false;
        }

        @Override
        public Team team() {
            return CreeperCombat.creeperTeam(tile);
        }

        @Override
        public CoreBlock.CoreBuild closestCore() {
            return null;
        }

        @Override
        public CoreBlock.CoreBuild closestEnemyCore() {
            return null;
        }

        @Override
        public CoreBlock.CoreBuild core() {
            return null;
        }

        @Override
        public void team(Team team) {
            // pseudo target，不允许外部改队伍。
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
        public void y(float y) {
            this.y = y;
        }

        @Override
        public int tileX() {
            return tile.x;
        }

        @Override
        public int tileY() {
            return tile.y;
        }

        @Override
        public Block blockOn() {
            return tile.block();
        }

        @Override
        public Tile tileOn() {
            return tile;
        }

        @Override
        public Floor floorOn() {
            return tile.floor();
        }

        @Override
        public Building buildOn() {
            return tile.build;
        }

        @Override
        public boolean onSolid() {
            return tile.solid();
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
            return "CreeperTarget{" + tile.x + "," + tile.y + ", creeper=" + tile.creeper + ", team=" + team() + "}";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Entityc> T self() {
            return (T)this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T as() {
            return (T)this;
        }

        @Override
        public boolean isAdded() {
            return CreeperCombat.validCreeperTile(tile);
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
            int height = Math.max(Vars.world.height(), 1);
            return -1 - (tile.x * height + tile.y);
        }

        @Override
        public void id(int id) {
            // pseudo target，无实体 ID 可写。
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
