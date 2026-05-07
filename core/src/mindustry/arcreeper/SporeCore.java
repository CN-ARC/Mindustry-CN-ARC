package mindustry.arcreeper;

import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.Team;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.Tile;

public final class SporeCore{
    public static final byte removeKilled = 1;
    public static final byte removeArrived = 2;
    public static final byte removeDespawned = 3;

    private static final Seq<Spore> spores = new Seq<>();
    private static final IntMap<Spore> byId = new IntMap<>();

    private static int nextId = 1;
    static int generation = 1;

    private static float syncTimer;

    private SporeCore(){
    }

    public static void init(){
        SporeSave.init();
        SporeNet.init();
    }

    public static int generation(){
        return generation;
    }

    public static int nextId(){
        return nextId;
    }

    public static Spore get(int id){
        return byId.get(id);
    }

    public static Seq<Spore> all(){
        return spores;
    }

    public static boolean hasAny(){
        return spores.size > 0;
    }

    public static void resetLocal(){
        spores.clear();
        byId.clear();
        nextId = 1;
        syncTimer = 0f;
    }

    public static void resetLocalKeepingGeneration(){
        spores.clear();
        byId.clear();
        syncTimer = 0f;
    }

    public static void setSaveState(int newGeneration, int newNextId){
        generation = Math.max(1, newGeneration);
        nextId = Math.max(1, newNextId);
    }

    public static void clearAuthoritative(){
        if(Vars.net.client()){
            resetLocal();
            return;
        }

        generation++;
        resetLocal();

        if(Vars.net.server()){
            SporeNet.sendClear(generation);
        }
    }

    /**
     * 兼容旧调用。
     */
    public static void reset(){
        clearAuthoritative();
    }

    public static void addLocal(Spore spore){
        if(spore == null || spore.id <= 0) return;

        Spore old = byId.get(spore.id);
        if(old != null && old != spore){
            spores.remove(old, true);
        }

        if(!spores.contains(spore, true)){
            spores.add(spore);
        }

        byId.put(spore.id, spore);
        spore.removed = false;
    }

    public static void removeLocal(Spore spore){
        if(spore == null) return;

        spore.removed = true;
        spores.remove(spore, true);

        if(byId.get(spore.id) == spore){
            byId.remove(spore.id);
        }
    }

    public static Spore spawn(
            float startX,
            float startY,
            float targetX,
            float targetY,
            float speed,
            float health,
            float creeperAmount,
            int releaseRadius
    ){
        return createAuthoritative(
                startX,
                startY,
                targetX,
                targetY,
                speed,
                health,
                creeperAmount,
                releaseRadius
        );
    }

    public static Spore createAuthoritative(
            float startX,
            float startY,
            float targetX,
            float targetY,
            float speed,
            float health,
            float creeperAmount,
            int releaseRadius
    ){
        if(Vars.net.client()){
            return null;
        }

        Spore spore = new Spore();
        spore.id = nextId++;
        spore.generation = generation;

        spore.startX = startX;
        spore.startY = startY;
        spore.targetX = targetX;
        spore.targetY = targetY;
        spore.x = startX;
        spore.y = startY;

        spore.speed = Math.max(0f, speed);
        spore.health = Math.max(0f, health);
        spore.maxHealth = Math.max(0f, health);
        spore.creeperAmount = creeperAmount;
        spore.releaseRadius = Math.max(0, releaseRadius);
        spore.rotate = Mathf.random();

        addLocal(spore);

        if(Vars.net.server()){
            SporeNet.sendSpawn(spore);
        }

        return spore;
    }


    public static void update(){
        if(!CreeperCore.enabled()) return;

        for(int i = spores.size - 1; i >= 0; i--){
            Spore spore = spores.get(i);

            if(spore.removed){
                removeLocal(spore);
                continue;
            }

            boolean reached = spore.updateMotion();

            // 客户端只预测位置，不结算爆炸。
            if(reached && !Vars.net.client()){
                arriveAuthoritative(spore);
            }
        }

        if(Vars.net.server()){
            syncTimer += Time.delta;
            if(syncTimer >= 15f){
                syncTimer = 0f;
                for(Spore spore : spores){
                    if(!spore.removed){
                        SporeNet.sendState(spore);
                    }
                }
            }
        }
    }

    public static void draw(){
        for(int i = spores.size - 1; i >= 0; i--){
            Spore spore = spores.get(i);
            spore.draw();
        }
    }

    public static void damage(Spore spore, float amount){
        if(spore == null || spore.removed) return;
        if(amount <= 0f) return;

        // 客户端不能扣血。
        if(Vars.net.client()){
            return;
        }

        spore.health -= amount;

        if(spore.health <= 0f){
            killAuthoritative(spore);
        }else if(Vars.net.server()){
            SporeNet.sendState(spore);
        }
    }

    public static void killAuthoritative(Spore spore){
        if(spore == null || spore.removed) return;
        if(Vars.net.client()) return;

        Fx.blastExplosion.at(spore.x, spore.y);
        removeAuthoritative(spore, removeKilled);
    }

    public static void arriveAuthoritative(Spore spore){
        if(spore == null || spore.removed) return;
        if(Vars.net.client()) return;

        CreeperCore.creeperTile.applySporeExplosion(
                spore.targetX,
                spore.targetY,
                spore.releaseRadius,
                spore.creeperAmount
        );

        Fx.blastExplosion.at(spore.targetX, spore.targetY);

        removeAuthoritative(spore, removeArrived);
    }

    public static void removeAuthoritative(Spore spore, byte reason){
        if(spore == null || spore.removed) return;

        if(Vars.net.client()){
            return;
        }

        if(Vars.net.server()){
            SporeNet.sendRemove(spore, reason);
        }

        removeLocal(spore);
    }

    public static void applyRemoteSpawn(SporeNet.SporeState state){
        if(state == null) return;
        if(state.generation != generation) return;

        Spore spore = byId.get(state.id);
        if(spore == null){
            spore = new Spore();
            state.apply(spore);
            addLocal(spore);
        }else{
            state.apply(spore);
        }
    }

    public static void applyRemoteState(SporeNet.SporeState state){
        if(state == null) return;
        if(state.generation != generation) return;

        Spore spore = byId.get(state.id);
        if(spore == null) return;

        state.applyMutable(spore);
    }

    public static void applyRemoteRemove(SporeNet.RemoveState state){
        if(state == null) return;
        if(state.generation != generation) return;

        if(state.reason == removeArrived){
            CreeperCore.creeperTile.applySporeExplosion(
                    state.targetX,
                    state.targetY,
                    state.releaseRadius,
                    state.creeperAmount
            );

            Fx.blastExplosion.at(state.targetX, state.targetY);
        }else if(state.reason == removeKilled){
            Fx.blastExplosion.at(state.x, state.y);
        }

        Spore spore = byId.get(state.id);
        if(spore != null){
            spore.x = state.x;
            spore.y = state.y;
            removeLocal(spore);
        }
    }

    public static void applyRemoteClear(int remoteGeneration){
        if(remoteGeneration < generation) return;

        generation = remoteGeneration;
        resetLocal();
    }

    public static void applySnapshot(int snapshotGeneration, int snapshotNextId, Seq<SporeNet.SporeState> states){
        generation = Math.max(1, snapshotGeneration);
        nextId = Math.max(1, snapshotNextId);

        resetLocalKeepingGeneration();

        for(SporeNet.SporeState state : states){
            Spore spore = new Spore();
            state.apply(spore);
            addLocal(spore);
        }
    }

    public static Spore nearest(Team attacker, float x, float y, float range){
        Spore best = null;
        float bestDst2 = range * range;

        for(Spore spore : spores){
            if(spore.removed) continue;
            if(!SporeCombat.canAttack(attacker, spore)) continue;

            float dst2 = Mathf.dst2(x, y, spore.x, spore.y);
            if(dst2 < bestDst2){
                bestDst2 = dst2;
                best = spore;
            }
        }

        return best;
    }

    public static class SporeI implements LExecutor.LInstruction{
        public LVar output;

        public LVar startX, startY;
        public LVar targetX, targetY;
        public LVar speed;
        public LVar health;
        public LVar creeperAmount;
        public LVar releaseRadius;

        public SporeI(
                LVar output,
                LVar startX,
                LVar startY,
                LVar targetX,
                LVar targetY,
                LVar speed,
                LVar health,
                LVar creeperAmount,
                LVar releaseRadius
        ){
            this.output = output;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.speed = speed;
            this.health = health;
            this.creeperAmount = creeperAmount;
            this.releaseRadius = releaseRadius;
        }

        public SporeI(){
        }

        @Override
        public void run(LExecutor exec){
            if(!exec.privileged) return;

            // 客户端不允许本地生成 gameplay Spore。
            if(Vars.net.client()){
                if(output != null && !output.constant){
                    output.setobj(null);
                }
                return;
            }

            Spore spore = SporeCore.createAuthoritative(
                    (float)startX.num() * Vars.tilesize,
                    (float)startY.num() * Vars.tilesize,
                    (float)targetX.num() * Vars.tilesize,
                    (float)targetY.num() * Vars.tilesize,
                    Math.max(0f, (float)speed.num()),
                    Math.max(0f, (float)health.num()),
                    (float)creeperAmount.num(),
                    Math.max(0, releaseRadius.numi())
            );

            if(output != null && !output.constant){
                output.setobj(spore);
            }
        }
    }
}