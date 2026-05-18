package mindustry.arcreeper;

import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Rules;
import mindustry.game.Team;

public final class SporeCombat{
    private SporeCombat(){
    }

    public static boolean canAttack(Team attacker, Spore spore){
        if(spore == null || spore.removed) return false;

        // 正数 payload 视为 C 方孢子，负数 payload 视为 AC 方孢子。
        if(spore.creeperAmount > 0f){
            return attacker != Vars.state.rules.creeperTeam;
        }

        if(spore.creeperAmount < 0f){
            return attacker != Vars.state.rules.antiCreeperTeam;
        }

        return true;
    }

    public static Seq<Spore> targets(Team team, float x, float y, float range){
        Seq<Spore> result = new Seq<>();
        float range2 = range * range;

        for(Spore spore : SporeCore.all()){
            if(spore == null || spore.removed) continue;
            if(!canAttack(team, spore)) continue;
            if(Mathf.dst2(x, y, spore.x, spore.y) > range2) continue;

            result.add(spore);
        }

        return result;
    }

    public static Spore bestTarget(Team team, float x, float y, float range){
        Spore best = null;
        float bestDst2 = range * range;

        for(Spore spore : SporeCore.all()){
            if(spore == null || spore.removed) continue;
            if(!canAttack(team, spore)) continue;

            float dst2 = Mathf.dst2(x, y, spore.x, spore.y);
            if(dst2 < bestDst2){
                bestDst2 = dst2;
                best = spore;
            }
        }

        return best;
    }

    public static boolean invalid(Team team, Spore spore, float x, float y, float range){
        if(spore == null || spore.removed) return true;
        if(!canAttack(team, spore)) return true;
        return Mathf.dst2(x, y, spore.x, spore.y) > range * range;
    }

    public static boolean attackSpore(Team team, Spore spore, float x, float y, float range, float damage, float efficiency){
        if(efficiency <= 0f) return false;
        if(damage <= 0f) return false;
        if(invalid(team, spore, x, y, range)) return false;

        spore.damage(damage);
        return true;
    }

    public static void damage(Spore spore, float damage){
        if(spore != null && !spore.removed){
            spore.damage(damage);
        }
    }
}
