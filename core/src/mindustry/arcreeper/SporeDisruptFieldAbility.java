package mindustry.arcreeper;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

import static mindustry.Vars.tilesize;

    public class SporeDisruptFieldAbility extends Ability {
    public float range = 60, reload = 60, damage = 1;

    protected float timer;

    public SporeDisruptFieldAbility(){};

    public SporeDisruptFieldAbility(float range, float reload, float damage){
        this.range = range;
        this.reload = reload;
        this.damage = damage;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        t.row();
        t.add(abilityStat("firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        t.add(Core.bundle.format("bullet.damage", Strings.autoFixed(damage, 2)));
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;
        Fx.overdriveWave.at(unit.x, unit.y, range, Color.purple);

        if(timer >= reload){
            for(Spore spore : SporeCore.all()){
                if(spore == null || spore.removed) continue;
                if(!SporeCombat.canAttack(unit.team, spore)) continue;
                if(Mathf.dst2(unit.x, unit.y, spore.x, spore.y) > range * range) continue;

                spore.damage(damage);

            }

            timer = 0f;
        }
    }

}
