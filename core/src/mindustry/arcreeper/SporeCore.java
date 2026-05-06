package mindustry.arcreeper;

import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;

public final class SporeCore{
    private static final Seq<Spore> spores = new Seq<>();
    private static int nextId = 1;

    public static void init(){
        // 预留事件注册、存档读取、网络同步等。
    }

    public static void reset(){
        spores.clear();
        nextId = 1;
    }

    public static Spore spawn(
            float startX, float startY,
            float targetX, float targetY,
            float speed,
            float health,
            float creeperAmount,
            int releaseRadius
    ){
        Spore spore = new Spore();
        spore.id = nextId++;

        spore.startX = startX;
        spore.startY = startY;
        spore.targetX = targetX;
        spore.targetY = targetY;
        spore.x = startX;
        spore.y = startY;

        spore.speed = speed;
        spore.health = health;
        spore.maxHealth = health;
        spore.creeperAmount = creeperAmount;
        spore.releaseRadius = releaseRadius;

        spores.add(spore);
        return spore;
    }

    public static void update(){
        for(int i = spores.size - 1; i >= 0; i--){
            Spore spore = spores.get(i);
            spore.update();

            if(spore.removed){
                spores.remove(i);
            }
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

    public static Seq<Spore> all(){
        return spores;
    }

    public static class SporeI implements LExecutor.LInstruction {
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

            Spore spore = SporeCore.spawn(
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
