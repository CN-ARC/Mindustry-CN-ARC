package mindustry.arcreeper;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.gen.Entityc;
import mindustry.gen.Posc;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.logic.Senseable;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.arcreeper.CreeperCore.drawSporeHealth;

@SuppressWarnings("unchecked")
public class Spore implements Posc, Senseable{
    public int id;
    public int generation;

    public float startX, startY;
    public float targetX, targetY;
    public float x, y;

    // world units / second
    public float speed;

    public float health;
    public float maxHealth;

    // 正数 = C，负数 = AC
    public float creeperAmount;

    // tile 半径
    public int releaseRadius = 1;

    public boolean removed;

    float sporeSize = 20f;
    float rotate = 0f;
    float fxInterval = 60f;
    float fxTimer = 0f;

    float drawycorr = 2f;
    /**
     * 只更新飞行位置。
     * 返回 true 表示已到达目标。
     * 注意：这里不结算爆炸，不修改 C/AC，不 remove。
     * 爆炸和释放必须由 SporeCore.arriveAuthoritative() 处理。
     */
    @Override
    public void update(){
        // Entityc 要求的接口。
        // Spore 的真实 gameplay 更新由 SporeCore.update() 统一驱动，
        // 这里不要结算移动、爆炸、C/AC 释放，避免客户端或实体系统重复更新。
    }

    /**
     * 只更新飞行位置。
     * 返回 true 表示已经到达目标。
     *
     * 注意：
     * - 不在这里释放 C/AC。
     * - 不在这里 remove。
     * - 不在这里发包。
     * - 到达后的结算必须由 SporeCore.arriveAuthoritative() 处理。
     */
    public boolean updateMotion(){
        if(removed) return false;

        float dx = targetX - x;
        float dy = targetY - y;
        float dst = Mathf.dst(dx, dy);

        float step = speed * Time.delta / 60f;

        if(dst <= step || dst <= 0.001f){
            x = targetX;
            y = targetY;
            return true;
        }

        float scl = step / dst;
        x += dx * scl;
        y += dy * scl;

        return false;
    }

    public void draw(){
        if(removed) return;
        Draw.reset();

        Draw.rect(
                Items.sporePod.uiIcon,
                x,
                y,
                sporeSize,
                sporeSize,
                360f * rotate + Time.time * 0.5f
        );

        fxTimer += Time.delta;
        if(fxTimer > fxInterval){
            fxTimer = 0f;
            Fx.unitDust.at(
                    x,
                    y,
                    creeperAmount > 0f ? Vars.state.rules.creeperColor : Vars.state.rules.antiCreeperColor
            );
        }

        if (drawSporeHealth && health < maxHealth){
            Draw.reset();
            Lines.stroke(4f);
            Draw.color(Vars.state.rules.creeperColor, 0.5f);
            Lines.line(x - sporeSize * 0.6f, y + (sporeSize / 2f) + drawycorr, x + sporeSize * 0.6f, y + (sporeSize / 2f) + drawycorr);
            Lines.stroke(2f);
            Draw.color(Pal.health, 0.8f);
            Lines.line(
                    x - sporeSize * 0.6f, y + (sporeSize / 2f) + drawycorr,
                    x + sporeSize * (Math.min(Mathf.maxZero(health), maxHealth) * 1.2f / maxHealth - 0.6f), y + (sporeSize / 2f) + drawycorr);
            Lines.stroke(2f);
        }
    }

    public void damage(float amount){
        SporeCore.damage(this, amount);
    }

    public void kill(){
        SporeCore.killAuthoritative(this);
    }

    public void arrive(){
        SporeCore.arriveAuthoritative(this);
    }

    @Override
    public void remove(){
        SporeCore.removeAuthoritative(this, SporeCore.removeDespawned);
    }

    @Override
    public void write(Writes write){
        write.i(id);
        write.i(generation);

        write.f(startX);
        write.f(startY);
        write.f(targetX);
        write.f(targetY);
        write.f(x);
        write.f(y);

        write.f(speed);
        write.f(health);
        write.f(maxHealth);
        write.f(creeperAmount);
        write.i(releaseRadius);

        write.f(rotate);
        write.f(fxTimer);
        write.bool(removed);
    }

    @Override
    public void read(Reads read){
        id = read.i();
        generation = read.i();

        startX = read.f();
        startY = read.f();
        targetX = read.f();
        targetY = read.f();
        x = read.f();
        y = read.f();

        speed = read.f();
        health = read.f();
        maxHealth = read.f();
        creeperAmount = read.f();
        releaseRadius = read.i();

        rotate = read.f();
        fxTimer = read.f();
        removed = read.bool();
    }

    @Override
    public Floor floorOn(){
        Tile tile = tileOn();
        return tile == null ? null : tile.floor();
    }

    @Override
    public Building buildOn(){
        Tile tile = tileOn();
        return tile == null ? null : tile.build;
    }

    @Override
    public boolean onSolid(){
        Tile tile = tileOn();
        return tile != null && tile.solid();
    }

    @Override
    public float getX(){
        return x;
    }

    @Override
    public float getY(){
        return y;
    }

    @Override
    public float x(){
        return x;
    }

    @Override
    public float y(){
        return y;
    }

    @Override
    public int tileX(){
        return (int)(x / Vars.tilesize);
    }

    @Override
    public int tileY(){
        return (int)(y / Vars.tilesize);
    }

    @Override
    public Block blockOn(){
        Tile tile = tileOn();
        return tile == null ? null : tile.block();
    }

    @Override
    public Tile tileOn(){
        return Vars.world.tileWorld(x, y);
    }

    @Override
    public void set(Position pos){
        set(pos.getX(), pos.getY());
    }

    @Override
    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    @Override
    public void trns(Position pos){
        trns(pos.getX(), pos.getY());
    }

    @Override
    public void trns(float x, float y){
        this.x += x;
        this.y += y;
    }

    @Override
    public void x(float x){
        this.x = x;
    }

    @Override
    public void y(float y){
        this.y = y;
    }

    @Override
    public <T extends Entityc> T self(){
        return (T)this;
    }

    @Override
    public <T> T as(){
        return (T)this;
    }

    @Override
    public boolean isAdded(){
        return !removed && SporeCore.get(id) == this;
    }

    @Override
    public boolean isLocal(){
        return !Vars.net.client();
    }

    @Override
    public boolean isNull(){
        return false;
    }

    @Override
    public boolean isRemote(){
        return Vars.net.client();
    }

    /**
     * 不走 Mindustry 原生 Entity 保存。
     * Spore 使用 ARCreeper 自己的 custom chunk 保存。
     */
    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public int classId(){
        return 0;
    }

    @Override
    public int id(){
        return id;
    }

    @Override
    public void id(int id){
        this.id = id;
    }

    @Override
    public void add(){
        SporeCore.addLocal(this);
    }

    @Override
    public void afterRead(){
    }

    @Override
    public void afterReadAll(){
    }

    @Override
    public void beforeWrite(){
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case x -> x;
            case y -> y;
            case ammo -> creeperAmount;
            case health -> health;
            case maxHealth -> maxHealth;
            case speed -> speed;
            case dead -> removed ? 1 : 0;
            case id -> id;
            case shootX -> targetX;
            case shootY -> targetY;
            case buildX -> startX;
            case buildY -> startY;
            default -> Double.NaN;
        };
    }
}