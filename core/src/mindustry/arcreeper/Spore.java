package mindustry.arcreeper;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.gen.Entityc;
import mindustry.gen.Posc;
import mindustry.logic.LAccess;
import mindustry.logic.Senseable;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class Spore implements Posc, Senseable {
    public int id;

    public float startX, startY;
    public float targetX, targetY;
    public float x, y;

    // 建议语义：world units per second
    public float speed;

    public float health;
    public float maxHealth;

    // 正数 = C，负数 = AC
    public float creeperAmount;

    // 命中释放半径，单位：tile 半径
    public int releaseRadius = 1;

    // 0.1s = 6 ticks，Mindustry/ARC 里很多逻辑按 60 tick = 1s 处理
    public float fxTimer;
    public float fxInterval = 6f;

    public boolean removed;

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
    public float getX(){
        return x;
    }

    @Override
    public float getY(){
        return y;
    }

    @Override
    public float x() {
        return 0;
    }

    @Override
    public float y() {
        return 0;
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
    public void set(Position pos) {

    }

    @Override
    public void set(float x, float y) {

    }

    @Override
    public void trns(Position pos) {

    }

    @Override
    public void trns(float x, float y) {

    }

    @Override
    public void x(float x) {

    }

    @Override
    public void y(float y) {

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

    public void update(){
        if(removed) return;

        fxTimer += Time.delta;
        if(fxTimer >= fxInterval){
            fxTimer %= fxInterval;
            Fx.explosion.at(x, y);
        }

        float dx = targetX - x;
        float dy = targetY - y;
        float dst = Mathf.dst(dx, dy);

        // speed 使用 world units / second
        float step = speed * Time.delta / 60f;

        if(dst <= step || dst <= 0.001f){
            x = targetX;
            y = targetY;
            arrive();
            return;
        }

        float scl = step / dst;
        x += dx * scl;
        y += dy * scl;
    }

    @Override
    public void write(Writes write) {

    }

    public void damage(float amount){
        if(removed) return;

        health -= amount;
        if(health <= 0f){
            kill();
        }
    }

    public void kill(){
        // 默认：被拦截后不释放 payload，更接近 CW3 的防孢子逻辑。
        removed = true;
        Fx.blastExplosion.at(x, y);
    }

    public void arrive(){
        Tile tile = Vars.world.tileWorld(targetX, targetY);
        if(tile != null && CreeperCore.creeperTile != null){
            CreeperCore.creeperTile.addArea(tile, releaseRadius, creeperAmount);
        }

        removed = true;
        Fx.blastExplosion.at(targetX, targetY);
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
