package mindustry.arcreeper;

import arc.math.Mathf;
import mindustry.game.Team;

public final class SporeCombat{
    public static boolean canAttack(Team attacker, Spore spore){
        if(spore == null || spore.removed) return false;

        // 正数 payload 视为 C 方孢子，负数 payload 视为 AC 方孢子。
        if(spore.creeperAmount > 0f){
            return attacker != CreeperCore.creeperTeam;
        }

        if(spore.creeperAmount < 0f){
            return attacker != CreeperCore.antiCreeperTeam;
        }

        return true;
    }

    public static Spore bestTarget(Team team, float x, float y, float range){
        return SporeCore.nearest(team, x, y, range);
    }

    public static boolean invalid(Team team, Spore spore, float x, float y, float range){
        if(spore == null || spore.removed) return true;
        if(!canAttack(team, spore)) return true;
        return Mathf.dst2(x, y, spore.x, spore.y) > range * range;
    }

    public static void damage(Spore spore, float damage){
        if(spore != null && !spore.removed){
            spore.damage(damage);
        }
    }
}