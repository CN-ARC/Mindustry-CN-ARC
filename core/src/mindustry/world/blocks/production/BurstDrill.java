package mindustry.world.blocks.production;

import arc.Core;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.iconSmall;
import static mindustry.Vars.tilesize;

public class BurstDrill extends Drill{
    public float shake = 2f;
    public Interp speedCurve = Interp.pow2In;

    public @Load("@-top-invert") TextureRegion topInvertRegion;
    public @Load("@-glow") TextureRegion glowRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public @Load("@-arrow-blur") TextureRegion arrowBlurRegion;

    public float invertedTime = 200f;
    public float arrowSpacing = 4f, arrowOffset = 0f;
    public int arrows = 3;
    public Color arrowColor = Color.valueOf("feb380"), baseArrowColor = Color.valueOf("6e7080");
    public Color glowColor = arrowColor.cpy();

    public Sound drillSound = Sounds.drillImpact;
    public float drillSoundVolume = 0.6f, drillSoundPitchRand = 0.1f;

    public BurstDrill(String name){
        super(name);

        //does not drill in the traditional sense, so this is not even used
        hardnessDrillMultiplier = 0f;
        //generally at center
        drillEffectRnd = 0f;
        drillEffect = Fx.shockwave;
        ambientSoundVolume = 0.18f;
        ambientSound = Sounds.drillCharge;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public float getDrillTime(Item item){
        return drillTime / drillMultipliers.get(item, 1f);
    }

    @Override
    public void setStats(){
        super.setStats();

        if(liquidBoostIntensity != 1 && findConsumer(f -> f instanceof ConsumeLiquidBase && f.booster) instanceof ConsumeLiquidBase consBase){
            stats.remove(Stat.booster);
            stats.add(Stat.booster,
                StatValues.speedBoosters("{0}" + StatUnit.timesSpeed.localized(),
                consBase.amount, liquidBoostIntensity, false, consBase::consumes)
            );
        }
    }

    public class BurstDrillBuild extends DrillBuild{
        //used so the lights don't fade out immediately
        public float smoothProgress = 0f;
        public float invertTime = 0f;

        @Override
        public void updateTile(){
            if(dominantItem == null){
                return;
            }

            if(invertTime > 0f) invertTime -= delta() / invertedTime;

            if(timer(timerDump, dumpTime / timeScale)){
                dump(items.has(dominantItem) ? dominantItem : null);
            }

            float drillTime = getDrillTime(dominantItem);

            smoothProgress = Mathf.lerpDelta(smoothProgress, progress / (drillTime - 20f), 0.1f);

            if(items.total() <= itemCapacity - dominantItems && dominantItems > 0 && efficiency > 0){
                warmup = Mathf.approachDelta(warmup, progress / drillTime, 0.01f);

                float speed = Mathf.lerp(1f, liquidBoostIntensity, optionalEfficiency) * efficiency;

                timeDrilled += speedCurve.apply(progress / drillTime) * speed;

                lastDrillSpeed = 1f / drillTime * speed * dominantItems;
                progress += delta() * speed;
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, 0.01f);
                lastDrillSpeed = 0f;
                return;
            }

            if(dominantItems > 0 && progress >= drillTime && items.total() < itemCapacity){
                for(int i = 0; i < dominantItems; i++){
                    offload(dominantItem);
                }

                invertTime = 1f;
                progress %= drillTime;

                if(wasVisible){
                    Effect.shake(shake, shake, this);
                    drillSound.at(x, y, 1f + Mathf.range(drillSoundPitchRand), drillSoundVolume);
                    drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), dominantItem.color);
                }
            }
        }

        @Override
        public float ambientVolume(){
            return super.ambientVolume() * Mathf.pow(progress(), 4f);
        }

        @Override
        public boolean shouldConsume(){
            return items.total() <= itemCapacity - dominantItems && enabled;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            drawDefaultCracks();

            Draw.rect(topRegion, x, y);
            if(invertTime > 0 && topInvertRegion.found()){
                Draw.alpha(Interp.pow3Out.apply(invertTime));
                Draw.rect(topInvertRegion, x, y);
                Draw.color();
            }

            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }

            float fract = smoothProgress;
            Draw.color(arrowColor);
            for(int i = 0; i < 4; i++){
                for(int j = 0; j < arrows; j++){
                    float arrowFract = (arrows - 1 - j);
                    float a = Mathf.clamp(fract * arrows - arrowFract);
                    Tmp.v1.trns(i * 90 + 45, j * arrowSpacing + arrowOffset);

                    //TODO maybe just use arrow alpha and draw gray on the base?
                    Draw.z(Layer.block);
                    Draw.color(baseArrowColor, arrowColor, a);
                    Draw.rect(arrowRegion, x + Tmp.v1.x, y + Tmp.v1.y, i * 90);

                    Draw.color(arrowColor);

                    if(arrowBlurRegion.found()){
                        Draw.z(Layer.blockAdditive);
                        Draw.blend(Blending.additive);
                        Draw.alpha(Mathf.pow(a, 10f));
                        Draw.rect(arrowBlurRegion, x + Tmp.v1.x, y + Tmp.v1.y, i * 90);
                        Draw.blend();
                    }
                }
            }
            Draw.color();

            if(glowRegion.found()){
                Drawf.additive(glowRegion, Tmp.c2.set(glowColor).a(Mathf.pow(fract, 3f) * glowColor.a), x, y);
            }

            if(Core.settings.getBool("arcdrillmode") && dominantItem != null){
                float dx = x - size * tilesize/2f + 5, dy = y - size * tilesize/2f + 5;
                float iconSize = 5f;
                Draw.rect(dominantItem.fullIcon, dx, dy, iconSize, iconSize);
                Draw.reset();

                float eff = Mathf.lerp(0,1,Math.min(1f, (float)dominantItems/(size * size)));
                if (eff<0.9f){
                    Draw.alpha(0.5f);
                    Draw.color(dominantItem.color);
                    Lines.stroke(1f);
                    Lines.arc(dx, dy, iconSize * 0.75f, eff);
                }
            }

        }
    }
}
