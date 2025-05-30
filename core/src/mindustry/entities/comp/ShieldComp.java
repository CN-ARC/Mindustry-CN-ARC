package mindustry.entities.comp;

import arc.math.WindowedMean;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

@Component
abstract class ShieldComp implements Healthc, Posc{
    @Import float health, hitTime, x, y, healthMultiplier, armorOverride;
    @Import boolean dead;
    @Import Team team;
    @Import UnitType type;

    /** Absorbs health damage. */
    float shield;
    /** Subtracts an amount from damage. No need to save. */
    transient float armor;
    /** Shield opacity. */
    transient float shieldAlpha = 0f;
    transient float lastHealth = 0f, lastShield = 0f;
    transient WindowedMean healthBalance = new WindowedMean(120);

    @Replace
    @Override
    public void damage(float amount){
        //apply armor and scaling effects
        rawDamage(Damage.applyArmor(amount, armorOverride >= 0f ? armorOverride : armor) / healthMultiplier / Vars.state.rules.unitHealth(team));
    }

    @Replace
    @Override
    public void damagePierce(float amount, boolean withEffect){
        float pre = hitTime;

        rawDamage(amount / healthMultiplier / Vars.state.rules.unitHealth(team));

        if(!withEffect){
            hitTime = pre;
        }
    }

    protected void rawDamage(float amount){
        boolean hadShields = shield > 0.0001f;

        if(Float.isNaN(health)) health = 0f;

        if(hadShields){
            shieldAlpha = 1f;
        }

        float shieldDamage = Math.min(Math.max(shield, 0), amount);
        shield -= shieldDamage;
        hitTime = 1f;
        amount -= shieldDamage;

        if(amount > 0 && type.killable){
            health -= amount;
            if(health <= 0 && !dead){
                kill();
            }

            if(hadShields && shield <= 0.0001f){
                Fx.unitShieldBreak.at(x, y, 0, type.shieldColor(self()), this);
            }
        }
    }

    @Override
    public void update(){
        shieldAlpha -= Time.delta / 15f;
        if(shieldAlpha < 0) shieldAlpha = 0f;
        healthBalance.add(((health - lastHealth) + (shield - lastShield)) / Time.delta);
        lastHealth = health;
        lastShield = shield;
    }
}
