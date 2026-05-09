package mindustry.arcreeper;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.world.Tile;

import java.io.*;
import java.util.Arrays;

import static mindustry.Vars.tilesize;
import static mindustry.arcreeper.CreeperTile.snapshotVersion;

public final class CreeperNet{

    /*
     * Tile.netStat 是唯一同步状态。
     *
     * 0: 无网
     * 1: 有网，未激活
     * 2: 有网，已激活
     * 3: C 喷口
     * 4: AC 喷口
     */
    public static final int none = 0;
    public static final int inactive = 1;
    public static final int active = 2;
    public static final int outlet = 3;
    public static final int antiOutlet = 4;

    /*
     * Tile.netHealth 不同步。
     * 它只作为本地运行时状态，用于损坏、恢复、绘制透明度等。
     */
    public static final float defaultHealth = 100f;

    /*
     * 固定在 ARCreeper 内部，不进 Rules。
     * 目的是保持原 ARCreeper 的 flow tick 节奏，而不是新增一堆地图规则参数。
     */
    private static final float healRate = 8f;
    private static final float disconnectedDecayRate = 18f;
    private static final float activeHeightBonus = -0.35f;
    private static final float activeFlowMultiplier = 1.35f;
    private static final float attackedSpreadRatio = 0.25f;

    public static boolean showNet = true;

    private static final float drawLayer = 55.05f;

    private static final Color inactiveColor = new Color(0.45f, 0.55f, 0.65f, 1f);
    private static final Color activeColor = new Color(0.20f, 0.55f, 1f, 1f);
    private static final Color outletColor = new Color(0.25f, 0.95f, 1f, 1f);
    private static final Color antiOutletColor = new Color(1f, 0.45f, 0.90f, 1f);

    private static boolean[] powered = new boolean[0];
    private static final IntSeq queue = new IntSeq();

    public static boolean brushEnabled = false;
    public static int brushStat = inactive;
    public static int brushRadius = 0;

    private CreeperNet(){
    }

    public static void init(){
        /*
         * 预留给后续事件注册。
         * 注意：这里不注册 packet，不处理 snapshot。
         * 网络层属于 CreeperNetwork。
         */
    }

    public static void initWorld(){
        if(Vars.world == null || Vars.world.tiles == null) return;

        ensureRuntime();

        Vars.world.tiles.eachTile(tile -> {
            tile.netStat = sanitize(tile.netStat);

            if(tile.netStat == none){
                tile.netHealth = 0f;
            }else if(tile.netHealth <= 0f){
                tile.netHealth = defaultHealth;
            }else{
                tile.netHealth = Mathf.clamp(tile.netHealth, 0f, defaultHealth);
            }

            if(tile.netStat == outlet || tile.netStat == antiOutlet){
                tile.netHealth = defaultHealth;
            }
        });

        clearRuntime();
    }

    public static void resetWorld(){
        if(Vars.world == null || Vars.world.tiles == null) return;

        Vars.world.tiles.eachTile(tile -> {
            tile.netStat = none;
            tile.netHealth = 0f;
        });

        clearRuntime();
    }

    public static int sanitize(int stat){
        return switch(stat){
            case inactive, active, outlet, antiOutlet -> stat;
            default -> none;
        };
    }

    public static int state(Tile tile){
        return tile == null ? none : sanitize(tile.netStat);
    }

    public static boolean has(Tile tile){
        return state(tile) != none;
    }

    public static boolean active(Tile tile){
        int stat = state(tile);
        return stat == active || stat == outlet || stat == antiOutlet;
    }

    public static boolean inactive(Tile tile){
        return state(tile) == inactive;
    }

    public static boolean outlet(Tile tile){
        int stat = state(tile);
        return stat == outlet || stat == antiOutlet;
    }

    public static int outletSign(Tile tile){
        int stat = state(tile);

        if(stat == outlet) return 1;
        if(stat == antiOutlet) return -1;

        return 0;
    }

    public static float health(Tile tile){
        if(tile == null || !has(tile)) return 0f;
        return tile.netHealth;
    }

    public static float healthf(Tile tile){
        if(tile == null || !has(tile)) return 0f;
        return Mathf.clamp(tile.netHealth / defaultHealth);
    }

    public static boolean powered(Tile tile){
        int index = index(tile);
        return index >= 0 && index < powered.length && powered[index];
    }

    /*
     * 权威设置入口。
     *
     * 单机 / 服务端逻辑调用这个。
     * 客户端不要直接调用这个改状态，客户端应该通过 CreeperNetwork 请求服务端。
     */
    public static void setAuthoritative(Tile tile, int stat){
        if(tile == null) return;

        applyLocal(tile, stat);

        /*
         * 这里只通知网络层。
         * 真正的 packet 发送由 CreeperNetwork 实现。
         */
        if(Vars.net.server()){
            CreeperNetwork.sendNetStat(tile);
        }
    }

    /*
     * 本地应用入口。
     *
     * 用于：
     * - 读 snapshot
     * - 客户端收到服务端同步
     * - 服务端内部落地前后的统一归一化
     *
     * 不发包。
     */
    public static void applyLocal(Tile tile, int stat){
        if(tile == null) return;

        int next = sanitize(stat);
        int old = sanitize(tile.netStat);

        tile.netStat = next;

        if(old != next){
            normalizeHealth(tile);
        }else{
            normalizeHealth(tile);
        }
    }

    /*
     * 客户端/通用设置入口。
     *
     * UI、逻辑、输入层如果不确定当前是客户端还是服务端，可以调用这个。
     */
    public static void set(Tile tile, int stat){
        if(tile == null) return;

        stat = sanitize(stat);

        if(Vars.net.client()){
            CreeperNetwork.requestSetNetStat(tile, stat);
        }else{
            setAuthoritative(tile, stat);
        }
    }

    public static void clear(Tile tile){
        set(tile, none);
    }

    private static void normalizeHealth(Tile tile){
        if(tile == null) return;

        tile.netStat = sanitize(tile.netStat);

        if(tile.netStat == none){
            tile.netHealth = 0f;
            return;
        }

        if(tile.netStat == outlet || tile.netStat == antiOutlet){
            tile.netHealth = defaultHealth;
            return;
        }

        if(tile.netHealth <= 0f){
            tile.netHealth = defaultHealth;
        }else{
            tile.netHealth = Mathf.clamp(tile.netHealth, 0f, defaultHealth);
        }
    }

    /*
     * 跟随 ARCreeper flow tick 调用。
     * 不单独开节奏。
     */
    public static void update(){
        if(!CreeperCore.enabled()) return;
        if(Vars.world == null || Vars.world.tiles == null) return;

        ensureRuntime();

        Arrays.fill(powered, false);
        queue.clear();

        seedPower();
        spreadPower();

        /*
         * 客户端只计算 powered 缓存用于显示。
         * 会改变 netStat 的逻辑只在服务端/单机跑。
         */
        if(Vars.net.client()) return;

        float delta = Math.max(0.001f, Vars.state.rules.creeperFlowInterval);

        Vars.world.tiles.eachTile(tile -> {
            int stat = state(tile);

            if(stat == none){
                tile.netHealth = 0f;
                return;
            }

            if(stat == inactive){
                if(tile.netHealth < 0f) tile.netHealth = 0f;
                return;
            }

            if(stat == outlet || stat == antiOutlet){
                tile.netHealth = defaultHealth;
                activateAdjacentInactive(tile);
                return;
            }

            if(stat == active){
                if(powered(tile)){
                    tile.netHealth = Math.min(defaultHealth, tile.netHealth + healRate * delta);
                    activateAdjacentInactive(tile);
                }else if(isDisconnectedBoundary(tile)){
                    damage(tile, disconnectedDecayRate * delta);
                }
            }
        });
    }

    /*
     * 攻击网。
     *
     * netHealth 不同步。
     * 只有 netHealth 掉到 0 导致 netStat 变化时，才通过 CreeperNetwork 同步 netStat。
     */
    public static float damage(Tile tile, float damage){
        if(tile == null || damage <= 0f) return 0f;
        if(!has(tile)) return 0f;
        if(outlet(tile)) return 0f;

        if(tile.netHealth <= 0f){
            tile.netHealth = defaultHealth;
        }

        float before = tile.netHealth;
        tile.netHealth = Math.max(0f, tile.netHealth - damage);

        if(tile.netHealth <= 0f && !Vars.net.client()){
            setAuthoritative(tile, inactive);
        }

        return before - tile.netHealth;
    }

    public static void spreadAttacked(Tile tile, int sign, float amount, float used){
        if(tile == null || !has(tile)) return;
        if(sign == 0 || amount <= 0f || used <= 0f) return;

        float spread = used * attackedSpreadRatio;
        if(spread <= 0f) return;

        int count = 0;

        for(Point2 p : Geometry.d4){
            Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);

            if(other != null && !other.solid()){
                count++;
            }
        }

        if(count <= 0) return;

        float each = spread / count * sign;

        for(Point2 p : Geometry.d4){
            Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);

            if(other != null && !other.solid()){
                other.creeper += each;
            }
        }
    }

    /*
     * Flow helpers.
     */

    public static float heightBonus(Tile tile){
        return active(tile) ? activeHeightBonus : 0f;
    }

    public static float flowMultiplier(Tile from, Tile to){
        return active(from) && active(to) ? activeFlowMultiplier : 1f;
    }

    /*
     * Snapshot helpers.
     *
     * CreeperTile.writeSnapshot/readSnapshot 调用这里。
     * 网络传输仍由 CreeperNetwork 负责。
     */
    public static void writeTile(Writes write, Tile tile){
        write.i(state(tile));
    }

    public static void readTile(Reads read, Tile tile){
        int stat = read.i();

        if(tile != null){
            applyLocal(tile, stat);
        }
    }

    public static void readTileDefault(Tile tile){
        if(tile != null){
            applyLocal(tile, none);
        }
    }

    /*
     * Draw.
     */

    public static void draw(){
        if(!showNet) return;
        if(Vars.headless) return;
        if(Vars.world == null || Vars.world.tiles == null) return;

        Draw.draw(drawLayer, CreeperNet::drawRaw);
    }

    private static void drawRaw(){
        float radius = tilesize * 0.42f;

        Vars.world.tiles.eachTile(tile -> {
            int stat = state(tile);
            if(stat == none) return;

            switch(stat){
                case inactive -> Draw.color(inactiveColor);
                case active -> Draw.color(activeColor);
                case outlet -> Draw.color(outletColor);
                case antiOutlet -> Draw.color(antiOutletColor);
                default -> Draw.color(inactiveColor);
            }

            float alpha = switch(stat){
                case inactive -> 0.25f + 0.25f * healthf(tile);
                case active -> 0.35f + 0.35f * healthf(tile);
                case outlet, antiOutlet -> 0.85f;
                default -> 0.25f;
            };

            Draw.alpha(alpha);
            Fill.square(tile.worldx(), tile.worldy(), radius);
        });

        Draw.color();
        Draw.alpha(1f);
    }

    /*
     * Power propagation.
     */

    private static void seedPower(){
        Vars.world.tiles.eachTile(tile -> {
            int stat = state(tile);

            if(stat != outlet && stat != antiOutlet) return;

            tile.netHealth = defaultHealth;

            int index = index(tile);
            if(index < 0 || index >= powered.length) return;

            powered[index] = true;
            queue.add(index);
        });
    }

    private static void spreadPower(){
        while(queue.size > 0){
            int index = queue.pop();
            Tile tile = tileByIndex(index);

            if(tile == null) continue;

            for(Point2 p : Geometry.d4){
                Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);

                if(other == null || !conducts(other)) continue;

                int otherIndex = index(other);
                if(otherIndex < 0 || otherIndex >= powered.length) continue;
                if(powered[otherIndex]) continue;

                powered[otherIndex] = true;
                queue.add(otherIndex);
            }
        }
    }

    private static boolean conducts(Tile tile){
        int stat = state(tile);
        return stat == active || stat == outlet || stat == antiOutlet;
    }

    private static void activateAdjacentInactive(Tile tile){
        for(Point2 p : Geometry.d4){
            Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);

            if(other == null) continue;
            if(state(other) != inactive) continue;
            if(other.netHealth <= 0f) continue;

            setAuthoritative(other, active);
        }
    }

    private static boolean isDisconnectedBoundary(Tile tile){
        for(Point2 p : Geometry.d4){
            Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);

            if(other == null) return true;
            if(!conducts(other)) return true;
        }

        return false;
    }

    private static void ensureRuntime(){
        if(Vars.world == null || Vars.world.tiles == null) return;

        int size = Vars.world.width() * Vars.world.height();

        if(powered.length != size){
            powered = new boolean[size];
        }
    }

    private static void clearRuntime(){
        if(powered.length > 0){
            Arrays.fill(powered, false);
        }

        queue.clear();
    }

    private static int index(Tile tile){
        if(tile == null) return -1;
        return tile.array();
    }

    private static Tile tileByIndex(int index){
        if(Vars.world == null) return null;

        int width = Vars.world.width();
        if(width <= 0) return null;

        int x = index % width;
        int y = index / width;

        return Vars.world.tile(x, y);
    }

    public static void setBrush(int stat){
        brushEnabled = true;
        brushStat = sanitize(stat);
    }

    public static void disableBrush(){
        brushEnabled = false;
    }

    public static void paint(Tile tile){
        paint(tile, brushStat);
    }

    public static void paint(Tile tile, int stat){
        if(tile == null) return;

        stat = sanitize(stat);

        if(brushRadius <= 0){
            set(tile, stat);
            return;
        }

        for(int dx = -brushRadius; dx <= brushRadius; dx++){
            for(int dy = -brushRadius; dy <= brushRadius; dy++){
                if(dx * dx + dy * dy > brushRadius * brushRadius) continue;

                Tile other = Vars.world.tile(tile.x + dx, tile.y + dy);
                if(other != null){
                    set(other, stat);
                }
            }
        }
    }

    public static void erase(Tile tile){
        paint(tile, none);
    }

    public static boolean hasAnyNet(){
        if(Vars.world == null || Vars.world.tiles == null) return false;

        final boolean[] found = {false};

        Vars.world.tiles.eachTile(tile -> {
            if(tile.netStat != none){
                found[0] = true;
            }
        });

        return found[0];
    }

    public static void writeSnapshot(Writes write){
        write.s(snapshotVersion);
        write.i(Vars.world.width());
        write.i(Vars.world.height());

        Vars.world.tiles.eachTile(tile -> {
            write.i(state(tile));
        });
    }

    public static void readSnapshot(Reads read){
        short version = read.s();

        int width = read.i();
        int height = read.i();

        int worldWidth = Vars.world.width();
        int worldHeight = Vars.world.height();

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int stat = none;

                if(version >= 1){
                    stat = read.i();
                }

                if(x < worldWidth && y < worldHeight){
                    Tile tile = Vars.world.tile(x, y);

                    if(tile != null){
                        applyLocal(tile, stat);
                    }
                }
            }
        }

        initWorld();
    }

    public static void readEmptySnapshot(){
        if(Vars.world == null || Vars.world.tiles == null) return;

        Vars.world.tiles.eachTile(tile -> {
            applyLocal(tile, none);
        });

        initWorld();
    }

    public static byte[] writeSnapshotBytes(){
        try{
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            Writes write = new Writes(data);

            writeSnapshot(write);

            data.flush();
            return bytes.toByteArray();
        }catch(IOException e){
            throw new RuntimeException("Failed to write ARCreeper net snapshot.", e);
        }
    }

    public static void readSnapshotBytes(byte[] bytes){
        try{
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            DataInputStream data = new DataInputStream(input);
            Reads read = new Reads(data);

            readSnapshot(read);
        }catch(Exception e){
            throw new RuntimeException("Failed to read ARCreeper net snapshot.", e);
        }
    }
}
