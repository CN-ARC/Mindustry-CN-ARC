package mindustry.arcreeper;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.game.EventType;
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

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class CreeperTile {
    private static final short snapshotVersion = 4;

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
    public static boolean showCreeperNet = true;
    public static boolean creeperDrawTrans = false;

    private static final float DRAW_LAYER = 55f;
    private static final float TILE_HEIGHT_EDGE_LAYER = DRAW_LAYER + 0.02f;
    private static final float NET_LAYER = DRAW_LAYER + 0.01f;
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

    private static final float netFlowBoost = 5f;
    private static final float defaultNetHeight = -1f;
    private static final float netRetainedDepth = 1f;
    private static final float netActivationTime = 1.2f;
    private static final float netDisconnectWearRate = 4f;
    private static final float netWearThresholdActive = 8f;
    private static final float netWearThresholdDamaged1 = 6f;
    private static final float netWearThresholdDamaged2 = 4f;
    public static final int netStateNone = Tile.creeperNetNone;
    public static final int netStateInactive = Tile.creeperNetInactive;
    public static final int netStateActive = Tile.creeperNetActive;
    public static final int netStateDamaged1 = Tile.creeperNetDamaged1;
    public static final int netStateDamaged2 = Tile.creeperNetDamaged2;
    public static final int netStateAntiOutlet = Tile.creeperNetAntiOutlet;
    public static final int netStateOutlet = Tile.creeperNetOutlet;
    private static final float netNodeInactiveRadius = 0.16f;
    private static final float netNodeActiveRadius = 0.16f;
    private static final float netNodeDamaged1Radius = 0.16f;
    private static final float netNodeDamaged2Radius = 0.16f;
    private static final float netNodeOutletRadius = 0.18f;
    private static final float netLineInactiveThickness = 0.12f;
    private static final float netLineActiveThickness = 0.12f;
    private static final float netLineDamaged1Thickness = 0.12f;
    private static final float netLineDamaged2Thickness = 0.12f;
    private static final float netLineOutletThickness = 0.14f;
    private static final float netOutlineScale = 1.45f;
    private final Color tmpDrawColor = new Color();
    private static final Color sideGray = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color netOutlineColor = new Color(0.05f, 0.07f, 0.1f, 1f);
    private static final Color netInactiveColor = new Color(0.28f, 0.33f, 0.38f, 1f);
    private static final Color netDamage1Color = new Color(0.78f, 0.50f, 0.32f, 1f);
    private static final Color netDamage2Color = new Color(0.64f, 0.22f, 0.18f, 1f);
    private float[] netHeights = new float[0];
    private float[] netCharge = new float[0];
    private float[] netWear = new float[0];
    private boolean[] netPowered = new boolean[0];
    private boolean netHeightsDirty = true;
    private final IntSeq netQueue = new IntSeq();
    private final IntSeq netComponent = new IntSeq();
    private final IntSeq netPowerQueue = new IntSeq();

    public void init(){
        if(snapshotLoaded){
            clearTmp();
            clearFxQueue();
            snapshotLoaded = false;
        }else{
            reset();
        }

        markNetHeightsDirty();
        ensureNetHeights();
        initTileHeight();//好像有点问题，这样会导致没法读取地形高度，等稍后看看啥情况吧

        if(!eventsRegistered){
            eventsRegistered = true;

            Events.on(EventType.TileChangeEvent.class, t -> {
                if(!CreeperCore.enabled()) return;
                updateTileHeight(t.tile);
                markNetHeightsDirty();
            });
        }
    }
    public void writeSnapshot(Writes write){
        write.s(snapshotVersion);
        write.i(Vars.world.width());
        write.i(Vars.world.height());

        Vars.world.tiles.eachTile(tile -> {
            write.f(tile.creeper);
            write.f(tile.height);
            write.i(tile.getCreeperNetState());
            CreeperCore.creeperNetCombat.writeSnapshotData(write, tile);
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
                float tileHeight = read.f();

                if(x < worldWidth && y < worldHeight){
                    Tile tile = Vars.world.tile(x, y);
                    if(tile != null){
                        tile.creeper = creeper;
                        tile.height = tileHeight;
                        if(version >= 4){
                            tile.creeperNet = sanitizeNetState(read.i());
                            CreeperCore.creeperNetCombat.readSnapshotData(tile, tile.creeperNet, read);
                        }else if(version >= 3){
                            tile.creeperNet = sanitizeNetState(read.i());
                            CreeperCore.creeperNetCombat.readSnapshotData(tile, tile.creeperNet, 0f, 0f);
                        }else if(version >= 2){
                            tile.creeperNet = read.bool() ? netStateActive : netStateNone;
                            CreeperCore.creeperNetCombat.readSnapshotData(tile, tile.creeperNet, 0f, 0f);
                        }
                    }else if(version >= 4){
                        read.i();
                        read.f();
                        read.f();
                    }else if(version >= 3){
                        read.i();
                    }else if(version >= 2){
                        read.bool();
                    }
                }else if(version >= 4){
                    read.i();
                    read.f();
                    read.f();
                }else if(version >= 3){
                    read.i();
                }else if(version >= 2){
                    read.bool();
                }
            }
        }

        clearTmp();
        clearFxQueue();
        markNetHeightsDirty();
        snapshotLoaded = true;
    }

    public void onNetStateChanged(Tile tile, int oldState, int newState){
        CreeperCore.creeperNetCombat.onStateChanged(tile, oldState, newState);
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

    public void markNetHeightsDirty(){
        netHeightsDirty = true;
    }

    public static int sanitizeNetState(int state){
        return switch(state){
            case netStateNone,
                 netStateInactive,
                 netStateActive,
                 netStateDamaged1,
                 netStateDamaged2,
                 netStateAntiOutlet,
                 netStateOutlet -> state;
            default -> state < netStateInactive ? netStateNone : netStateInactive;
        };
    }

    public static boolean hasNetState(int state){
        return state != netStateNone;
    }

    public static boolean isOutletState(int state){
        return state == netStateOutlet || state == netStateAntiOutlet;
    }

    public static boolean isDamageState(int state){
        return state == netStateDamaged1 || state == netStateDamaged2;
    }

    public static boolean isBoostedNetState(int state){
        return state == netStateActive || state == netStateDamaged1 || state == netStateDamaged2 || isOutletState(state);
    }

    public static boolean isDegradableNetState(int state){
        return state == netStateActive || state == netStateDamaged1 || state == netStateDamaged2;
    }

    public static int degradeNetState(int state){
        return switch(state){
            case netStateActive -> netStateDamaged1;
            case netStateDamaged1 -> netStateDamaged2;
            case netStateDamaged2 -> netStateInactive;
            default -> state;
        };
    }

    public static int outletNetSign(int state){
        return switch(state){
            case netStateOutlet -> 1;
            case netStateAntiOutlet -> -1;
            default -> 0;
        };
    }

    private void ensureNetHeights(){
        if(netHeightsDirty){
            initNetHeights();
            netHeightsDirty = false;
        }
    }

    private void ensureNetRuntimeArrays(int size){
        if(netHeights.length != size){
            netHeights = new float[size];
        }
        if(netCharge.length != size){
            netCharge = new float[size];
        }
        if(netWear.length != size){
            netWear = new float[size];
        }
        if(netPowered.length != size){
            netPowered = new boolean[size];
        }
    }

    private void initNetHeights(){
        int size = Vars.world.width() * Vars.world.height();
        float defaultHeight = defaultNetHeight * Vars.state.rules.heightScale;

        ensureNetRuntimeArrays(size);

        for(int i = 0; i < size; i++){
            netHeights[i] = defaultHeight;
        }

        boolean[] visited = new boolean[size];

        Vars.world.tiles.eachTile(tile -> {
            if(tile == null || !isNetTile(tile)) return;

            int index = tile.array();
            if(index < 0 || index >= size || visited[index]) return;

            float componentHeight = defaultHeight;
            netQueue.clear();
            netComponent.clear();
            netQueue.add(index);
            visited[index] = true;

            while(!netQueue.isEmpty()){
                int current = netQueue.pop();
                netComponent.add(current);

                Tile currentTile = Vars.world.tiles.geti(current);
                if(currentTile == null) continue;

                for(Point2 point : Geometry.d4){
                    Tile other = currentTile.nearby(point);
                    if(other == null) continue;

                    if(isNetTile(other)){
                        int otherIndex = other.array();
                        if(otherIndex >= 0 && otherIndex < size && !visited[otherIndex]){
                            visited[otherIndex] = true;
                            netQueue.add(otherIndex);
                        }
                    }else{
                        componentHeight = Math.min(componentHeight, heightOf(other) - Vars.state.rules.heightScale);
                    }
                }
            }

            for(int i = 0; i < netComponent.size; i++){
                netHeights[netComponent.get(i)] = componentHeight;
            }
        });
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
        
        updateHeightTemp();

        updateFx();

        updateTimer += Time.delta / 60f;
        if(updateTimer < Vars.state.rules.creeperFlowInterval) return;
        updateTimer -= Vars.state.rules.creeperFlowInterval;

        clearTmp();
        updateNetStates();
        updateFlow();

        Vars.world.tiles.eachTile(tile -> {
            tile.creeper += tile.creeperTmp;
        });

        damageUnits();
    }

    private void updateNetStates(){
        CreeperCore.creeperNetCombat.update();
    }

    private void updatePoweredNetMap(){
        for(int i = 0; i < netPowered.length; i++){
            netPowered[i] = false;
        }

        netPowerQueue.clear();

        Vars.world.tiles.eachTile(tile -> {
            if(tile == null) return;

            int state = netStateOf(tile);
            if(!isOutletState(state)) return;

            int index = tile.array();
            if(index < 0 || index >= netPowered.length || netPowered[index]) return;

            netPowered[index] = true;
            netPowerQueue.add(index);
        });

        while(!netPowerQueue.isEmpty()){
            int current = netPowerQueue.pop();
            Tile tile = Vars.world.tiles.geti(current);
            if(tile == null) continue;

            for(Point2 point : Geometry.d4){
                Tile other = tile.nearby(point);
                if(other == null) continue;

                int otherIndex = other.array();
                if(otherIndex < 0 || otherIndex >= netPowered.length || netPowered[otherIndex]) continue;
                if(!isBoostedNetState(netStateOf(other))) continue;

                netPowered[otherIndex] = true;
                netPowerQueue.add(otherIndex);
            }
        }
    }

    private boolean isNetCharging(Tile tile){
        for(Point2 point : Geometry.d4){
            Tile other = tile.nearby(point);
            if(other == null) continue;

            int otherIndex = other.array();
            if(otherIndex >= 0 && otherIndex < netPowered.length && netPowered[otherIndex]){
                return true;
            }
        }

        return false;
    }

    private void applyNetWear(Tile tile, int index){
        while(index >= 0 && index < netWear.length){
            int state = netStateOf(tile);
            if(!isDegradableNetState(state)) break;

            float threshold = netWearThreshold(state);
            if(netWear[index] < threshold) break;

            netWear[index] -= threshold;
            tile.setCreeperNet(degradeNetState(state));

            if(netStateOf(tile) == netStateInactive){
                netCharge[index] = 0f;
                netWear[index] = 0f;
                break;
            }
        }
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
        ensureNetHeights();

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
        if (tile.build.team == team) return false;

        tile.build.damage(amount * Vars.state.rules.creeperDamage);
        if (sign > 0) setCreeperFx(tile, Fx.creeperDamage);
        else setCreeperFx(tile, Fx.antiCreeperDamage);

        return true;
    }

    /**
     * 获取指定极性对应的控制队伍。
     */
    Team teamOf(int sign) {
        return sign > 0 ? CreeperCore.creeperTeam : CreeperCore.antiCreeperTeam;
    }

    private int netStateOf(Tile tile){
        return tile == null ? netStateNone : sanitizeNetState(tile.getCreeperNetState());
    }

    private float netWearThreshold(int state){
        return switch(state){
            case netStateActive -> netWearThresholdActive;
            case netStateDamaged1 -> netWearThresholdDamaged1;
            case netStateDamaged2 -> netWearThresholdDamaged2;
            default -> Float.MAX_VALUE;
        };
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
        if(!canNetSpreadTo(from, to, signFrom)) return;

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
        if(!canNetSpreadTo(from, to, signFrom)) return;

        float raw = rawSameSignAmount(from, to, signFrom, rate);
        if (raw < minFlow) return;

        float amount = raw;

        if (!isEnemyBuilding(to, signFrom) || buildingAbsorb) {
            float depth = spreadableDepth(from, signFrom);
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

        float surfaceFrom = flowSurfaceHeight(from) + depthFrom;
        float surfaceTo = flowSurfaceHeight(to) + depthTo;

        float diff = surfaceFrom - surfaceTo;
        if (diff <= minSurfaceDiff) return 0f;

        return Math.min(diff, depthFrom) * rate * flowBoost(from, to);
    }

    private float flowBoost(Tile from, Tile to){
        return isBoostedNetTile(from) || isBoostedNetTile(to) ? netFlowBoost : 1f;
    }

    private float spreadableDepth(Tile tile, int sign){
        float depth = Math.max(0f, tile.creeper * sign);
        if(affectsCreeperFlow(tile)){
            depth = Math.max(0f, depth - netRetainedDepth);
        }
        return depth;
    }

    private float flowSurfaceHeight(Tile tile){
        return affectsCreeperFlow(tile) ? netHeightOf(tile) : heightOf(tile);
    }

    private float netHeightOf(Tile tile){
        float scaledDefault = defaultNetHeight * Vars.state.rules.heightScale;
        if(tile == null) return scaledDefault;

        int index = tile.array();
        if(index < 0 || index >= netHeights.length){
            return scaledDefault;
        }

        return netHeights[index];
    }

    private boolean canNetSpreadTo(Tile from, Tile to, int sign){
        if(!affectsCreeperFlow(from)) return true;
        if(to == null) return false;
        if(affectsCreeperFlow(to)) return spreadableDepth(from, sign) > minFlow;
        if(to.floor() == Blocks.space || to.floor() == Blocks.empty) return false;
        return spreadableDepth(from, sign) > minFlow;
    }

    public void applyNetAttackDamage(Tile tile, float damage){
        // ARCreeper: 普通伤害在这里直接交给 creeperNet 独立结算，不经过 creeper 的 used/consume 转换。
        CreeperCore.creeperNetCombat.damageTile(tile, damage);
    }

    void spreadAttackedNet(Tile tile, int sign, float amountBefore, float used){
        if(tile == null || !affectsCreeperFlow(tile) || used <= minFlow) return;

        float burstBudget = Math.max(0f, amountBefore - netRetainedDepth);
        float burstAmount = Math.min(used, burstBudget);
        if(burstAmount <= minFlow) return;

        int neighbors = 0;
        for(Point2 point : Geometry.d4){
            if(tile.nearby(point) != null){
                neighbors++;
            }
        }

        if(neighbors == 0) return;

        float share = burstAmount / neighbors;
        for(Point2 point : Geometry.d4){
            Tile other = tile.nearby(point);
            if(other != null){
                other.creeper += sign * share;
            }
        }
    }

    public boolean isNetTile(Tile tile){
        return hasNetState(netStateOf(tile));
    }

    public boolean isBoostedNetTile(Tile tile){
        return isBoostedNetState(netStateOf(tile));
    }

    private boolean affectsCreeperFlow(Tile tile){
        return isBoostedNetTile(tile);
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

        float heightA = flowSurfaceHeight(a);
        float heightB = flowSurfaceHeight(b);

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
                drawNet();
                draw2d();
                break;
            case 2:
                drawNet();
                draw3d();
                break;
            default:
                return;
        }

        // 高度边界是叠加层：主体先提交，边界再以更高 z 提交。
        if (drawTileHeight) draw2dTileHeightEdges();
    }

    void drawNet(){
        Draw.draw(NET_LAYER, this::drawNetRaw);
    }

    void drawNetRaw(){
        if(!showCreeperNet) return;

        Vars.world.tiles.eachTile(tile -> {
            if(!isNetTile(tile)) return;

            int state = netRenderState(tile);
            float nodeRadius = tilesize * netNodeRadius(state);
            float lineThickness = tilesize * netLineThickness(state);
            float alpha = netAlpha(tile, state);
            Color main = netColor(tile, state);
            float x = tile.worldx();
            float y = tile.worldy();

            drawNetSegments(tile, x, y, nodeRadius * netOutlineScale, lineThickness * netOutlineScale, netOutlineColor, alpha * 0.55f);
            drawNetSegments(tile, x, y, nodeRadius, lineThickness, main, alpha);
            drawNetOutletMark(x, y, state, alpha);

            boolean brightState = isBoostedNetState(state);
            Draw.color(Color.white);
            Draw.alpha(alpha * (brightState ? 0.20f : 0.10f));
            Fill.square(x, y, nodeRadius * (brightState ? 0.45f : 0.32f));
        });

        Draw.color();
        Draw.alpha(1f);
    }

    private void drawNetSegments(Tile tile, float x, float y, float nodeRadius, float lineThickness, Color color, float alpha){
        float half = tilesize / 2f;
        float lineLength = half + lineThickness;

        Draw.color(color);
        Draw.alpha(alpha);

        if(connectsNet(tile, -1, 0)){
            Fill.rect(x - half / 2f, y, lineLength, lineThickness);
        }
        if(connectsNet(tile, 1, 0)){
            Fill.rect(x + half / 2f, y, lineLength, lineThickness);
        }
        if(connectsNet(tile, 0, -1)){
            Fill.rect(x, y - half / 2f, lineThickness, lineLength);
        }
        if(connectsNet(tile, 0, 1)){
            Fill.rect(x, y + half / 2f, lineThickness, lineLength);
        }

        Fill.square(x, y, nodeRadius);
    }

    private void drawNetOutletMark(float x, float y, int state, float alpha){
        if(state != netStateOutlet && state != netStateAntiOutlet) return;

        float size = tilesize * 0.12f;
        float offset = tilesize * 0.10f;
        float tip = state == netStateOutlet ? offset : -offset;
        Color color = state == netStateOutlet ? Vars.state.rules.creeperColor : Vars.state.rules.antiCreeperColor;

        Draw.color(Color.black);
        Draw.alpha(alpha * 0.55f);
        Fill.rect(x + tip * 0.25f, y, size * 1.15f, size * 1.15f);

        Draw.color(color);
        Draw.alpha(alpha);
        Fill.rect(x, y, size, size * 1.5f);
        Fill.rect(x + tip, y, size * 1.4f, size * 0.55f);
        Fill.rect(x + tip * 0.55f, y + size * 0.55f, size * 0.55f, size * 0.55f);
        Fill.rect(x + tip * 0.55f, y - size * 0.55f, size * 0.55f, size * 0.55f);
    }

    private boolean connectsNet(Tile tile, int dx, int dy){
        return isNetTile(Vars.world.tile(tile.x + dx, tile.y + dy));
    }

    private int netRenderState(Tile tile){
        return netStateOf(tile);
    }

    private boolean isNetActive(Tile tile){
        return netStateOf(tile) == netStateActive;
    }

    private int netDamageLevel(Tile tile){
        return switch(netStateOf(tile)){
            case netStateDamaged1 -> 1;
            case netStateDamaged2 -> 2;
            default -> 0;
        };
    }

    private float netNodeRadius(int state){
        return switch(state){
            case netStateActive -> netNodeActiveRadius;
            case netStateDamaged1 -> netNodeDamaged1Radius;
            case netStateDamaged2 -> netNodeDamaged2Radius;
            case netStateOutlet, netStateAntiOutlet -> netNodeOutletRadius;
            default -> netNodeInactiveRadius;
        };
    }

    private float netLineThickness(int state){
        return switch(state){
            case netStateActive -> netLineActiveThickness;
            case netStateDamaged1 -> netLineDamaged1Thickness;
            case netStateDamaged2 -> netLineDamaged2Thickness;
            case netStateOutlet, netStateAntiOutlet -> netLineOutletThickness;
            default -> netLineInactiveThickness;
        };
    }

    private float netAlpha(Tile tile, int state){
        return switch(state){
            case netStateActive -> 0.95f;
            case netStateDamaged1 -> 0.90f;
            case netStateDamaged2 -> 0.86f;
            case netStateOutlet, netStateAntiOutlet -> 1f;
            default -> Math.abs(tile.creeper) > minCreeper ? 0.78f : 0.66f;
        };
    }

    private Color netColor(Tile tile, int state){
        return switch(state){
            case netStateActive -> tile.creeper < 0f ? Vars.state.rules.antiCreeperColor : Vars.state.rules.creeperColor;
            case netStateDamaged1 -> netDamage1Color;
            case netStateDamaged2 -> netDamage2Color;
            case netStateOutlet -> tmpDrawColor.set(Vars.state.rules.creeperColor).lerp(Color.white, 0.25f);
            case netStateAntiOutlet -> tmpDrawColor.set(Vars.state.rules.antiCreeperColor).lerp(Color.white, 0.25f);
            default -> {
                if(tile.creeper > minCreeper){
                    yield tmpDrawColor.set(netInactiveColor).lerp(Vars.state.rules.creeperColor, 0.25f);
                }
                if(tile.creeper < -minCreeper){
                    yield tmpDrawColor.set(netInactiveColor).lerp(Vars.state.rules.antiCreeperColor, 0.25f);
                }
                yield netInactiveColor;
            }
        };
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
                case height -> tile.height;
                // ARCreeper: 世界处理器读取 creeperNet 状态口。
                case creeperNet -> tile.getCreeperNetState();
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
                case height -> tile.height = v;
                // ARCreeper: 世界处理器写入 creeperNet 接口，喷口/网格状态都从这里进。
                case creeperNet -> tile.setCreeperNet(Mathf.round(v));
            }

        }
    }
}
