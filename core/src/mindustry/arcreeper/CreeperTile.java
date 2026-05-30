package mindustry.arcreeper;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatements;
import mindustry.logic.LVar;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.ForceProjector;

import java.io.*;

import static mindustry.Vars.*;

public class CreeperTile {
    private static final short snapshotVersion = 2;

    // SaveVersion 读到 arc-creeper chunk 后置 true。
    // CreeperCore.enable() 看到这个标记时，不再 reset 掉刚读出的 creeper/height。
    private boolean snapshotLoaded = false;

    private boolean eventsRegistered = false;

    private float updateTimer = 0f;
    
    // 后续会更新
    int log2Min = (int) Mathf.log2(0.01f);
    int log2Max = (int) Mathf.log2(1000f);

    // 最小流动阈值。
    public float minFlow = 0.001f;

    // 最小水面差阈值。小于该差值时不产生流动，用于减少微小来回抖动。
    public float minSurfaceDiff = 0.01f;

    // 单个 tile 每轮最多流出自身深度的比例，用于限制多方向同时出流造成的过抽。
    // 0.75 在默认 flowRate = 0.18 时通常不会明显降低扩散效率。
    public float maxDrainFraction = 0.75f;

    /**
     * FX 播放间隔。
     * Mindustry 中 Time.delta 以 tick 为单位，60 tick 约等于 1 秒。
     */
    public float fxInterval = 60f;

    private float fxTimer = 0f;

    /**
     * 只记录需要播放 FX 的 tile。
     * 不每秒扫描全图，降低性能消耗。
     */
    private final Seq<Tile> fxTiles = new Seq<>(false);

    /**
     * 敌方建筑是否吸收流动量。
     * <p>
     * true：流动量会从来源格扣除，并转换为建筑伤害，不进入目标格。
     * false：只对建筑造成流动伤害，不改变两侧 creeper 数值。
     */
    public boolean buildingAbsorb = false;

    /**
     * 是否播放 creeper 相关 FX。
     * false 时不会添加新的 FX，并会清理已经等待播放的 FX 队列。
     */
    public static boolean playCreeperFx = true;
    /**
     * 绘制creeper的模式
     */
    public static int creeperDrawType = 2;
    public static boolean creeperDrawTrans = true;

    private static final float DRAW_LAYER = 55f;
    private static final float TILE_HEIGHT_EDGE_LAYER = DRAW_LAYER + 0.02f;
    /**
     * 2D 绘制中是否叠加显示地形高度边界。
     * 边界判断直接使用 Tile.height，不使用 heightScale。
     */
    public static boolean drawTileHeight = false;
    // 可调参数
    private static final float EDGE_RATIO = 0.12f;        // 边界厚度占 tileSize 的比例
    private static final float EDGE_ALPHA_BOOST = 0.15f;  // 边界额外透明度增强
    private static final float TILE_HEIGHT_EDGE_ALPHA = 0.85f; // 2D 地形高度边界透明度

    private static final float TOP_LIGHT_MIX = 0.55f;     // 上边界向白色混合
    private static final float SIDE_GRAY_MIX = 0.45f;     // 左右边界向灰色混合
    private static final float BOTTOM_DARK_MIX = 0.55f;   // 下边界向黑色混合

    private final Color tmpDrawColor = new Color();
    private static final Color sideGray = new Color(0.55f, 0.55f, 0.55f, 1f);

    public static int cTile = 0, acTile = 0;

    public void init(){
        if(snapshotLoaded){
            clearTmp();
            clearFxQueue();
            snapshotLoaded = false;
        }else{
            reset();
        }

        clearCreeperTile();
        initTileHeight();//好像有点问题，这样会导致没法读取地形高度，等稍后看看啥情况吧

        if(!eventsRegistered){
            eventsRegistered = true;

            Events.on(EventType.TileChangeEvent.class, t -> {
                if(!CreeperCore.enabled()) return;
                updateTileHeight(t.tile);
            });
        }
    }
    public void writeSnapshot(Writes write){
        write.s(snapshotVersion);
        write.i(Vars.world.width());
        write.i(Vars.world.height());

        Vars.world.tiles.eachTile(tile -> {
            write.f(tile.creeper);
        });
    }

    public void readSnapshot(Reads read){
        short version = read.s();
        int width = read.i();
        int height = read.i();

        int worldWidth = Vars.world.width();
        int worldHeight = Vars.world.height();

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                float creeper = read.f();

                Tile tile = x < worldWidth && y < worldHeight ? Vars.world.tile(x, y) : null;
                if(tile != null){
                    tile.creeper = creeper;
                }

                // 兼容旧 snapshotVersion：如果旧版本还写了 height，就读掉但不要使用
                if(version <= 1){
                    read.f();
                }
            }
        }

        clearTmp();
        clearFxQueue();
        snapshotLoaded = true;

        // custom chunk 读完后，重新按当前 patch 计算高度
        initTileHeight();
    }

    public byte[] writeSnapshotBytes(){
        try{
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            Writes write = new Writes(data);

            writeSnapshot(write);

            data.flush();
            return bytes.toByteArray();
        }catch(IOException e){
            throw new RuntimeException("Failed to write ARCreeper snapshot.", e);
        }
    }

    public void readSnapshotBytes(byte[] bytes){
        try{
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            DataInputStream data = new DataInputStream(input);
            Reads read = new Reads(data);

            readSnapshot(read);
        }catch(Exception e){
            throw new RuntimeException("Failed to read ARCreeper snapshot.", e);
        }
    }

    public void applySporeExplosion(float worldX, float worldY, int releaseRadius, float creeperAmount){
        Tile tile = Vars.world.tileWorld(worldX, worldY);
        if(tile == null) return;

        addArea(tile, releaseRadius, creeperAmount);
    }


    public void initTileHeight() {
        Vars.world.tiles.eachTile(this::updateTileHeight);
    }

    void updateTileHeight(Tile tile) {
        tile.height = tile.block().creeperHeight + tile.floor().creeperHeight;
    }

    public void reset() {
        Vars.world.tiles.eachTile(tile -> {
            tile.creeper = 0f;
            tile.creeperFx = null;
        });

        clearTmp();
        clearFxQueue();
    }

    private void clearTmp() {
        Vars.world.tiles.eachTile(tile -> {
            tile.creeperTmp = 0f;
            tile.creeperOutPos = 0f;
            tile.creeperOutNeg = 0f;
        });
    }

    private void clearFxQueue() {
        fxTimer = 0f;

        for (int i = 0; i < fxTiles.size; i++) {
            Tile tile = fxTiles.get(i);
            if (tile != null) {
                tile.creeperFx = null;
            }
        }

        fxTiles.clear();
    }

    private void setCreeperFx(Tile tile, Effect fx) {
        if (!playCreeperFx || Vars.headless || tile == null || fx == null) return;

        // 已经在队列里：只更新效果，不重复添加。
        if (tile.creeperFx != null) {
            tile.creeperFx = fx;
            return;
        }

        tile.creeperFx = fx;
        fxTiles.add(tile);
    }

    private Effect strongerFx(Effect oldFx, Effect newFx) {
        // 建筑受伤 bubbles 优先级高于正负水抵消 smoke。
        if (oldFx == Fx.bubble || newFx == Fx.smoke) return Fx.smoke;
        return newFx;
    }

    private void updateFx() {
        if (Vars.headless || fxTiles.isEmpty()) return;

        if (!playCreeperFx) {
            clearFxQueue();
            return;
        }

        fxTimer += Time.delta;
        if (fxTimer < fxInterval) return;
        fxTimer %= fxInterval;

        for (int i = fxTiles.size - 1; i >= 0; i--) {
            Tile tile = fxTiles.get(i);

            if (tile == null || tile.creeperFx == null) {
                fxTiles.remove(i);
                continue;
            }

            tile.creeperFx.at(tile.worldx(), tile.worldy());

            // 一次性播放。播放后清理，允许下次事件再次加入队列。
            tile.creeperFx = null;
            fxTiles.remove(i);
        }
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
    public void update(){
        updateClamp();

        updateFx();

        updateTimer += Time.delta / 60f;
        if(updateTimer < Vars.state.rules.creeperFlowInterval) return;
        updateTimer -= Vars.state.rules.creeperFlowInterval;

        updateHeightTemp();
        clearTmp();
        updateFlow();

        clearCreeperTile();
        Vars.world.tiles.eachTile(tile -> {
            tile.creeper += tile.creeperTmp;
            countCreeperTile(tile);
        });

        damageUnits();
    }

    void countCreeperTile(Tile tile){
        if (tile.creeper > state.rules.minCreeperCount) cTile +=1;
        else if (tile.creeper < -state.rules.minCreeperCount) acTile += 1;
    }

    void clearCreeperTile(){
        cTile = 0;
        acTile = 0;
    }

    /** ARCreeper: 每帧重算所有单位/建筑立场提供的临时高度。 */
    void updateHeightTemp(){
        clearHeightTemp();
        applyUnitProjectorHeight();
        applyBuildingProjectorHeight();
    }

    /** ARCreeper: 清除全地图临时高度。 */
    void clearHeightTemp(){
        Vars.world.tiles.eachTile(Tile::clearHeightTemp);
    }

    /** ARCreeper: 单位 ForceFieldAbility 提供临时高度。 */
    void applyUnitProjectorHeight(){
        Groups.unit.each(unit -> {
            if(unit.type == null || unit.shield <= 0f) return;

            for(Ability ability : unit.type.abilities){
                if(!(ability instanceof ForceFieldAbility field)) continue;
                if(field.heightEnhance == 0f) continue;

                float radius = field.getRealRad();
                if(radius <= 0.001f) continue;

                applyHeightTempCircle(unit.x, unit.y, radius, field.heightEnhance);
            }
        });
    }

    /** ARCreeper: 建筑 ForceProjector 提供临时高度。 */
    void applyBuildingProjectorHeight(){
        Groups.build.each(build -> {
            if(!(build.block instanceof ForceProjector projector)) return;
            if(!(build instanceof ForceProjector.ForceBuild force)) return;
            if(projector.heightEnhance == 0f || force.broken) return;

            float radius = force.realRadius();
            if(radius <= 0.001f) return;

            applyHeightTempCircle(force.x, force.y, radius, projector.heightEnhance + force.phaseHeat * projector.heightEnhanceBoost);
        });
    }

    /** ARCreeper: 对圆形覆盖范围内的 tile 累加 heightTemp。 */
    void applyHeightTempCircle(float wx, float wy, float radius, float heightEnhance){
        if(radius <= 0f || heightEnhance == 0f) return;

        int minX = Math.max(0, Mathf.floor((wx - radius) / tilesize));
        int maxX = Math.min(Vars.world.width() - 1, Mathf.floor((wx + radius) / tilesize));
        int minY = Math.max(0, Mathf.floor((wy - radius) / tilesize));
        int maxY = Math.min(Vars.world.height() - 1, Mathf.floor((wy + radius) / tilesize));

        float radius2 = radius * radius;

        for(int x = minX; x <= maxX; x++){
            for(int y = minY; y <= maxY; y++){
                Tile tile = Vars.world.tile(x, y);
                if(tile == null) continue;

                float dx = tile.worldx() - wx;
                float dy = tile.worldy() - wy;
                float dr = dx * dx + dy * dy;

                if(dr <= radius2){
                    tile.addHeightTemp(heightEnhance * (1-dr/radius2));
                }
            }
        }
    }
    
    void updateClamp() {
        log2Min = (int) Mathf.log2(Vars.state.rules.minCreeper);
        log2Max = (int) Mathf.log2(Vars.state.rules.maxCreeper);
    }

    void updateFlow() {
        float rate = Vars.state.rules.flowRate;

        // 第一遍：统计每个 tile 本轮同号/空格传播想流出的总量。
        // 只统计来源格出流，不写 creeperTmp。
        Vars.world.tiles.eachTile(tile -> {
            collectOutflow(tile, Vars.world.tile(tile.x + 1, tile.y), rate);
            collectOutflow(tile, Vars.world.tile(tile.x, tile.y + 1), rate);
        });

        // 第二遍：按来源格总出流上限缩放后，实际写入 creeperTmp。
        Vars.world.tiles.eachTile(tile -> {
            applyLimitedFlow(tile, Vars.world.tile(tile.x + 1, tile.y), rate);
            applyLimitedFlow(tile, Vars.world.tile(tile.x, tile.y + 1), rate);
        });

        // 第三遍：C / AC 相邻时只抵消，不做穿透式 transfer。
        // 使用 creeper + creeperTmp 作为本轮预测值，避免同一轮多条边过量抵消。
        Vars.world.tiles.eachTile(tile -> {
            flowOppositeCancel(tile, Vars.world.tile(tile.x + 1, tile.y), rate);
            flowOppositeCancel(tile, Vars.world.tile(tile.x, tile.y + 1), rate);
        });
    }

    /**
     * 获取 tile 的地形高度，并转换为 creeper 深度单位。
     */
    float heightOf(Tile tile) {
        return tile.getSumHeight() * Vars.state.rules.heightScale;
    }

    /**
     * 获取 creeper 的极性。
     */
    int signOf(float value) {
        if (value > Vars.state.rules.minCreeper) return 1;
        if (value < -Vars.state.rules.minCreeper) return -1;
        return 0;
    }

    /**
     * 将指定极性的流体从 from 转移到 to。
     * <p>
     * 如果目标格存在敌方建筑，则先结算建筑交互：
     * - buildingAbsorb = true：本次流动量从来源格扣除，并转换为建筑伤害，不进入目标格；
     * - buildingAbsorb = false：只对建筑造成流动伤害，不改变 from/to 的 creeper 数值。
     */
    void transfer(Tile from, Tile to, int sign, float amount) {
        if (amount <= 0f) return;

        if (damageBuildingOnFlow(to, sign, amount)) {
            if (buildingAbsorb) {
                from.creeperTmp -= sign * amount;
            }
            return;
        }

        from.creeperTmp -= sign * amount;
        to.creeperTmp += sign * amount;
    }

    /**
     * 流动进入目标格时，对敌方建筑造成伤害。
     * <p>
     * creeper 使用 CreeperCore.creeperTeam；antiCreeper 使用 CreeperCore.antiCreeperTeam。
     * 只有 building.team 与对应队伍不同时，才视为敌方建筑。
     *
     * @return 是否命中了敌方建筑。
     */
    boolean damageBuildingOnFlow(Tile tile, int sign, float amount) {
        if (tile == null || tile.build == null) return false;

        Team team = teamOf(sign);
        if (tile.build.team == team || tile.build.block.privileged) return false;

        tile.build.damage(teamOf(sign), amount * Vars.state.rules.creeperBlockDamage * tile.build.block.creeperDmgReceived);
        if (sign > 0) setCreeperFx(tile, Fx.creeperDamage);
        else setCreeperFx(tile, Fx.antiCreeperDamage);

        return true;
    }

    /**
     * 获取指定极性对应的控制队伍。
     */
    Team teamOf(int sign) {
        return sign > 0 ? Vars.state.rules.creeperTeam : Vars.state.rules.antiCreeperTeam;
    }

    /**
     * 第一遍：统计 a-b 这条边上可能发生的同号/空格传播候选出流。
     * 每条边仍然只由 updateFlow() 传入一次，但这里内部检查两个方向。
     */
    private void collectOutflow(Tile a, Tile b, float rate) {
        if (a == null || b == null) return;

        collectOutflowOneWay(a, b, rate);
        collectOutflowOneWay(b, a, rate);
    }

    private void collectOutflowOneWay(Tile from, Tile to, float rate) {
        int signFrom = signOf(from.creeper);
        if (signFrom == 0) return;

        int signTo = signOf(to.creeper);

        // 反号相邻不在这里传播，交给 flowOppositeCancel() 只做抵消，避免一帧内穿透翻色。
        if (signTo != 0 && signTo != signFrom) return;

        float amount = rawSameSignAmount(from, to, signFrom, rate);
        if (amount < minFlow) return;

        // buildingAbsorb=false 时，敌方建筑只受伤、不吃水，因此不占用来源出流预算。
        if (isEnemyBuilding(to, signFrom) && !buildingAbsorb) return;

        addOut(from, signFrom, amount);
    }

    /**
     * 第二遍：按来源 tile 的总出流预算缩放后应用同号/空格传播，随后处理 C/AC 抵消。
     */
    private void applyLimitedFlow(Tile a, Tile b, float rate) {
        if (a == null || b == null) return;

        applyLimitedFlowOneWay(a, b, rate);
        applyLimitedFlowOneWay(b, a, rate);
    }

    private void applyLimitedFlowOneWay(Tile from, Tile to, float rate) {
        int signFrom = signOf(from.creeper);
        if (signFrom == 0) return;

        int signTo = signOf(to.creeper);

        // 反号相邻不做流入穿透，统一交给 flowOppositeCancel() 抵消。
        if (signTo != 0 && signTo != signFrom) return;

        float raw = rawSameSignAmount(from, to, signFrom, rate);
        if (raw < minFlow) return;

        float amount = raw;

        if (!isEnemyBuilding(to, signFrom) || buildingAbsorb) {
            float depth = Math.max(0f, from.creeper * signFrom);
            float maxOut = depth * maxDrainFraction;
            float totalOut = outOf(from, signFrom);

            if (totalOut > maxOut && totalOut > 0f) {
                amount *= maxOut / totalOut;
            }
        }

        if (amount < minFlow) return;

        transfer(from, to, signFrom, amount);
    }

    /**
     * 原始同号/空格传播量。
     * <p>
     * 这里仍然保留原来的 surface = height + depth 逻辑，
     * 只是把“水面差是否足够大”和“实际流量是否足够大”分成两个阈值。
     */
    private float rawSameSignAmount(Tile from, Tile to, int sign, float rate) {
        float depthFrom = Math.max(0f, from.creeper * sign);
        if (depthFrom <= Vars.state.rules.minCreeper) return 0f;

        float depthTo = Math.max(0f, to.creeper * sign);

        float surfaceFrom = heightOf(from) + depthFrom;
        float surfaceTo = heightOf(to) + depthTo;

        float diff = surfaceFrom - surfaceTo;
        if (diff <= minSurfaceDiff) return 0f;

        return Math.min(diff, depthFrom - Vars.state.rules.minCreeper) * rate;
    }

    private float outOf(Tile tile, int sign) {
        return sign > 0 ? tile.creeperOutPos : tile.creeperOutNeg;
    }

    private void addOut(Tile tile, int sign, float amount) {
        if (sign > 0) tile.creeperOutPos += amount;
        else tile.creeperOutNeg += amount;
    }

    private boolean isEnemyBuilding(Tile tile, int sign) {
        return tile != null
                && tile.build != null
                && !tile.build.block.privileged
                && tile.build.team != teamOf(sign);
    }

    /**
     * 处理 creeper 与 antiCreeper 相邻时的抵消。
     * <p>
     * 与旧版 flowOppositeSign() 的区别：
     * - 不再把 C 直接 transfer 到 AC 格，也不把 AC 直接 transfer 到 C 格；
     * - 只在边界上按推进能力抵消双方；
     * - 目标格归零后，下一轮再由同号/空格传播逻辑占领，减少前线来回穿透和翻色抖动。
     */
    private void flowOppositeCancel(Tile a, Tile b, float rate) {
        if (a == null || b == null) return;

        float valueA = a.creeper + a.creeperTmp;
        float valueB = b.creeper + b.creeperTmp;

        int signA = signOf(valueA);
        int signB = signOf(valueB);

        if (signA == 0 || signB == 0 || signA == signB) return;

        float depthA = Math.abs(valueA);
        float depthB = Math.abs(valueB);

        float heightA = heightOf(a);
        float heightB = heightOf(b);

        float reachA = Math.max(0f, heightA + depthA - heightB);
        float reachB = Math.max(0f, heightB + depthB - heightA);

        float capA = Math.min(depthA, reachA);
        float capB = Math.min(depthB, reachB);

        if (capA <= minFlow && capB <= minFlow) return;

        float amount = Math.min(Math.min(depthA, depthB), Math.max(capA, capB) * rate);
        if (amount < minFlow) return;

        a.creeperTmp -= signA * amount;
        b.creeperTmp -= signB * amount;

        setCreeperFx(a, Fx.creeperCancel);
        setCreeperFx(b, Fx.creeperCancel);
    }

    public void draw() {
        switch (creeperDrawType){
            case 0:
                return;
            case 1:
                draw2d();
                break;
            case 2:
                draw3d();
                break;
            default:
                return;
        }

        // 高度边界是叠加层：主体先提交，边界再以更高 z 提交。
        if (drawTileHeight) draw2dTileHeightEdges();
    }

    void draw2d(){
        Draw.draw(DRAW_LAYER, this::draw2dRaw);
    }

    void draw2dRaw(){
        Vars.world.tiles.eachTile(tile -> {
            float raw = tile.creeper;

            // creeper = 0 时不绘制
            if (raw == 0f) return;

            // 负数表示 Anti-Creeper
            boolean anti = raw < 0f;

            // 强度按绝对值计算
            float value = Math.abs(raw);

            // 绝对值低于阈值则不显示
            if (value < Vars.state.rules.minCreeper) return;

            float v = Mathf.clamp(value, Vars.state.rules.minCreeper, Vars.state.rules.maxCreeper);

            int bits = Float.floatToIntBits(v);
            int exp = ((bits >>> 23) & 0xFF) - 127;

            float normalized = Mathf.clamp((float) (exp - log2Min) / (log2Max - log2Min), 0f, 1f);
            if (creeperDrawTrans) normalized *=0.3f;
            float alpha = 0.2f + normalized * 0.7f;

            Color color = anti ? Vars.state.rules.antiCreeperColor : Vars.state.rules.creeperColor;
            Draw.color(color);
            Draw.alpha(alpha);

            Fill.square(
                    tile.worldx(),
                    tile.worldy(),
                    tilesize / 2f
            );
        });

        Draw.color();
        Draw.alpha(1f);
    }

    void draw2dTileHeightEdges() {
        Draw.draw(TILE_HEIGHT_EDGE_LAYER, this::draw2dTileHeightEdgesRaw);
    }

    void draw2dTileHeightEdgesRaw() {
        final float tileSize = tilesize;
        final float half = tileSize / 2f;
        final float edge = Mathf.clamp(tileSize * EDGE_RATIO, 0.75f, half * 0.5f);

        Draw.color(Pal.stat);
        Draw.alpha(TILE_HEIGHT_EDGE_ALPHA);

        Vars.world.tiles.eachTile(tile -> {
            float x = tile.worldx();
            float y = tile.worldy();

            if (shouldDrawTileHeightEdge(tile, tile.x, tile.y + 1)) {
                Fill.rect(x, y + half - edge / 2f, tileSize, edge);
            }

            if (shouldDrawTileHeightEdge(tile, tile.x, tile.y - 1)) {
                Fill.rect(x, y - half + edge / 2f, tileSize, edge);
            }

            if (shouldDrawTileHeightEdge(tile, tile.x - 1, tile.y)) {
                Fill.rect(x - half + edge / 2f, y, edge, tileSize);
            }

            if (shouldDrawTileHeightEdge(tile, tile.x + 1, tile.y)) {
                Fill.rect(x + half - edge / 2f, y, edge, tileSize);
            }
        });

        Draw.color();
        Draw.alpha(1f);
    }

    /**
     * 判断当前 tile 是否需要在指向指定邻居的一侧绘制地形高度边界。
     */
    private boolean shouldDrawTileHeightEdge(Tile current, int neighborX, int neighborY) {
        if (current == null) return false;

        Tile other = Vars.world.tile(neighborX, neighborY);
        if (other == null) return false;

        return current.getSumHeight() > other.getSumHeight();
    }

    public void draw3d() {
        Draw.draw(DRAW_LAYER, this::draw3dRaw);
    }

    void draw3dRaw() {
        final float tileSize = tilesize;
        final float half = tileSize / 2f;
        final float edge = Mathf.clamp(tileSize * EDGE_RATIO, 0.75f, half * 0.5f);

        Vars.world.tiles.eachTile(tile -> {
            float raw = tile.creeper;

            int layer = creeperLayer(raw);
            if (layer < 0) return;

            boolean anti = raw < 0f;

            float value = Math.abs(raw);
            float v = Mathf.clamp(value, Vars.state.rules.minCreeper, Vars.state.rules.maxCreeper);

            int bits = Float.floatToIntBits(v);
            int exp = ((bits >>> 23) & 0xFF) - 127;

            float normalized = Mathf.clamp((float) (exp - log2Min) / (log2Max - log2Min), 0f, 1f);
            if (creeperDrawTrans) normalized *=0.3f;
            float alpha = 0.2f + normalized * 0.7f;

            Color base = anti ? Vars.state.rules.antiCreeperColor : Vars.state.rules.creeperColor;

            float x = tile.worldx();
            float y = tile.worldy();

            // 主体填充
            Draw.color(base);
            Draw.alpha(alpha);

            Fill.square(
                    x,
                    y,
                    half
            );

            /*
             * 单向分界：
             * 只由当前 tile 高于相邻 tile 时绘制。
             *
             * 这样相邻两层之间不会出现：
             * - 低层画一次
             * - 高层再画一次
             *
             * 最终只保留高层 tile 的那一侧边界。
             */
            boolean top = shouldDrawCreeperEdge(tile.x, tile.y + 1, anti, layer);
            boolean bottom = shouldDrawCreeperEdge(tile.x, tile.y - 1, anti, layer);
            boolean left = shouldDrawCreeperEdge(tile.x - 1, tile.y, anti, layer);
            boolean right = shouldDrawCreeperEdge(tile.x + 1, tile.y, anti, layer);

            if (!top && !bottom && !left && !right) return;

            float edgeAlpha = Mathf.clamp(alpha + EDGE_ALPHA_BOOST, 0f, 1f);

            // 左右边界：偏灰
            if (left || right) {
                setMixedColor(base, sideGray, SIDE_GRAY_MIX, edgeAlpha);

                if (left) {
                    Fill.rect(
                            x - half + edge / 2f,
                            y,
                            edge,
                            tileSize
                    );
                }

                if (right) {
                    Fill.rect(
                            x + half - edge / 2f,
                            y,
                            edge,
                            tileSize
                    );
                }
            }

            // 下边界：偏深
            if (bottom) {
                setMixedColor(base, Color.black, BOTTOM_DARK_MIX, edgeAlpha);

                Fill.rect(
                        x,
                        y - half + edge / 2f,
                        tileSize,
                        edge
                );
            }

            // 上边界：偏白
            if (top) {
                setMixedColor(base, Color.white, TOP_LIGHT_MIX, edgeAlpha);

                Fill.rect(
                        x,
                        y + half - edge / 2f,
                        tileSize,
                        edge
                );
            }
        });

        Draw.color();
        Draw.alpha(1f);
    }

    /**
     * 返回 creeper 所在的强度层级。
     *
     * -1 表示不显示。
     *
     * 当前实现按 log2 指数层级划分，
     * 也就是原 draw() 里用于 alpha 计算的 exp 层。
     * 因此 2、4、8、16、32 这类数量级变化都会形成分界。
     */
    private int creeperLayer(float raw) {
        if (raw == 0f) return -1;

        float value = Math.abs(raw);
        if (value < Vars.state.rules.minCreeper) return -1;

        float v = Mathf.clamp(value, Vars.state.rules.minCreeper, Vars.state.rules.maxCreeper);

        int bits = Float.floatToIntBits(v);
        int exp = ((bits >>> 23) & 0xFF) - 127;

        int layer = (exp - log2Min);
        int maxLayer = (log2Max - log2Min);

        if (layer < 0) return 0;
        if (layer > maxLayer) return maxLayer;
        return layer;
    }

    /**
     * 判断当前 tile 是否应该在指向 neighbor 的方向绘制边界。
     *
     * 单向规则：
     *
     * 1. neighbor 不存在：绘制
     * 2. neighbor 不显示：绘制
     * 3. currentLayer > neighborLayer：绘制
     * 4. currentLayer < neighborLayer：不绘制
     * 5. currentLayer == neighborLayer 且正负不同：
     *      为避免双绘制，只让正 Creeper 一侧绘制
     * 6. 其他情况：不绘制
     */
    private boolean shouldDrawCreeperEdge(int neighborX, int neighborY, boolean currentAnti, int currentLayer) {
        Tile other = Vars.world.tile(neighborX, neighborY);

        // 地图边缘：当前 tile 必须明确绘制外边界
        if (other == null) return true;

        float otherRaw = other.creeper;
        int otherLayer = creeperLayer(otherRaw);

        // 相邻 tile 不可见：当前 tile 绘制外边界
        if (otherLayer < 0) return true;

        // 核心：只允许高层向低层绘制
        if (currentLayer > otherLayer) return true;
        if (currentLayer < otherLayer) return false;

        /*
         * 层级相同但正负不同。
         *
         * 这里不是“高低层级”问题，而是类型分界问题。
         * 为避免两侧都画，使用稳定优先级：
         *
         * positive Creeper 优先于 Anti-Creeper。
         */
        boolean otherAnti = otherRaw < 0f;
        if (currentAnti != otherAnti) {
            return !currentAnti;
        }

        // 同层级、同类型，不是分界
        return false;
    }

    /**
     * 判断指定邻居位置是否与当前 tile 形成层级分界。
     */
    private boolean isCreeperLayerBoundary(int x, int y, boolean anti, int currentLayer) {
        Tile other = Vars.world.tile(x, y);

        // 地图外边界也要明确绘制
        if (other == null) return true;

        float raw = other.creeper;

        // 相邻 tile 不显示，则当前 tile 这一侧是边界
        int otherLayer = creeperLayer(raw);
        if (otherLayer < 0) return true;

        // Creeper 与 Anti-Creeper 之间也视为边界
        if ((raw < 0f) != anti) return true;

        // 核心改动：层级不同就画分界线
        return otherLayer != currentLayer;
    }

    private void setMixedColor(Color base, Color target, float mix, float alpha) {
        tmpDrawColor.set(base).lerp(target, mix);
        Draw.color(tmpDrawColor);
        Draw.alpha(alpha);
    }

    /** 对站在 creeper / anti-creeper 上的敌对单位造成伤害。 */
    private void damageUnits(){
        float baseDamage = Vars.state.rules.creeperUnitDamage;
        if(baseDamage <= 0f) return;

        Groups.unit.each(unit -> {
            damageUnitOnTile(unit, baseDamage);
        });
    }

    private void damageUnitOnTile(Unit unit, float baseDamage){
        if(unit == null || unit.dead() || unit.type == null) return;

        Tile tile = unit.tileOn();
        if(tile == null || Math.abs(tile.creeper) < Vars.state.rules.minCreeper) return;

        int sign = signOf(tile.creeper);
        if(sign == 0) return;

        Team team = teamOf(sign);

        // 与 damageBuildingOnFlow() 保持一致：属于对应 C/AC 队伍的不受该液体伤害。
        if(unit.team == team) return;

        float damage = baseDamage * (1f -  unit.type.creeperEvade) * Math.abs(tile.creeper);
        if(damage <= 0f) return;

        unit.damagePierce(damage);
        unit.apply(StatusEffects.sporeSlowed, 300f);

        if(sign > 0){
            setCreeperFx(tile, Fx.creeperDamage);
        }else{
            setCreeperFx(tile, Fx.antiCreeperDamage);
        }
    }

    public static class GetARCreeperI implements LExecutor.LInstruction {
        public final LVar x, y, result;
        public final LStatements.ARCreeperData type;

        public GetARCreeperI(LVar x, LVar y, LVar result, LStatements.ARCreeperData type){
            this.x = x;
            this.y = y;
            this.result = result;
            this.type = type;
        }

        @Override
        public void run(LExecutor exec){
            Tile tile = world.tile((int)x.num(), (int)y.num());

            if(tile == null){
                result.setnum(0);
                return;
            }

            result.setnum(switch(type){
                case creeper -> tile.creeper;
            });
        }
    }

    public static class SetARCreeperI implements LExecutor.LInstruction {
        public final LVar x, y, value;
        public final LStatements.ARCreeperData type;

        public SetARCreeperI(LVar x, LVar y, LVar value, LStatements.ARCreeperData type){
            this.x = x;
            this.y = y;
            this.value = value;
            this.type = type;
        }

        @Override
        public void run(LExecutor exec){
            Tile tile = world.tile((int)x.num(), (int)y.num());

            if(tile == null) return;

            float v = (float)value.num();

            switch(type){
                case creeper -> tile.creeper = v;
            }

        }
    }
}