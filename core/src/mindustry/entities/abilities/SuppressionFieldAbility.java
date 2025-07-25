package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.UnitType;

import static mindustry.Vars.tilesize;
import static mindustry.arcModule.RFuncs.abilitysFormat;
import mindustry.type.*;

import static mindustry.Vars.*;

public class SuppressionFieldAbility extends Ability{
    protected static Rand rand = new Rand();

    public float reload = 60f * 1.5f;
    public float maxDelay = 60f * 1.5f;
    public float range = 200f;

    public float orbRadius = 4.1f, orbMidScl = 0.33f, orbSinScl = 8f, orbSinMag = 1f;
    public Color color = Pal.suppress;
    public float layer = Layer.effect;

    public float x = 0f, y = 0f;
    public int particles = 15;
    public float particleSize = 4f;
    public float particleLen = 7f;
    public float rotateScl = 3f;
    public float particleLife = 110f;
    public boolean active = true;
    public Interp particleInterp = f -> Interp.circleOut.apply(Interp.slope.apply(f));
    public Color particleColor = Pal.sap.cpy();
    public Color effectColor = Pal.sapBullet;

    public float applyParticleChance = 13f;

    protected float timer;

    @Override
    public void init(UnitType type){
        if(!active) display = false;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        t.row();
        t.add(abilityStat("duration", Strings.autoFixed(reload / 60f, 2)));
    }

    @Override
    public void update(Unit unit){
        if(!active) return;

        if((timer += Time.delta) >= maxDelay){
            Tmp.v1.set(x, y).rotate(unit.rotation - 90f).add(unit);
            Damage.applySuppression(unit.team, Tmp.v1.x, Tmp.v1.y, range, reload, maxDelay, applyParticleChance, unit, effectColor);
            timer = 0f;
        }
    }

    @Override
    public String description(UnitType unit){
        return abilitysFormat("@s~@格",
                reload / 60f,
                range / tilesize
        );
    }

    @Override
    public void draw(Unit unit){
        Draw.z(layer);

        float rad = orbRadius + Mathf.absin(orbSinScl, orbSinMag);
        Tmp.v1.set(x, y).rotate(unit.rotation - 90f).add(unit);
        float rx = Tmp.v1.x, ry = Tmp.v1.y;

        float base = (Time.time / particleLife);
        rand.setSeed(unit.id + hashCode());
        Draw.color(particleColor);
        for(int i = 0; i < particles; i++){
            float fin = (rand.random(1f) + base) % 1f, fout = 1f - fin;
            float angle = rand.random(360f) + (Time.time / rotateScl + unit.rotation) % 360f;
            float len = particleLen * particleInterp.apply(fout);
            Fill.circle(
            rx + Angles.trnsx(angle, len),
            ry + Angles.trnsy(angle, len),
            particleSize * Mathf.slope(fin)
            );
        }

        Lines.stroke(2f);

        Draw.color(color);
        Lines.circle(rx, ry, rad);

        Draw.color(color);
        Fill.circle(rx, ry, rad * orbMidScl);

        if(active){
            //TODO draw range when selected?
        }

        Draw.reset();
    }
}
