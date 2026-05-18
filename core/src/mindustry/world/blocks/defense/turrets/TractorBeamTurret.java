package mindustry.world.blocks.defense.turrets;

import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.arcreeper.Spore;
import mindustry.arcreeper.SporeCombat;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class TractorBeamTurret extends BaseTurret{
    public final int timerTarget = timers++;
    public float retargetTime = 5f;

    public float shootCone = 6f;
    public float shootLength = 5f;
    public float laserWidth = 0.6f;
    public float force = 0.3f;
    public float scaledForce = 0f;
    public float damage = 0f;
    public boolean targetAir = true, targetGround = false;
    public Color laserColor = Color.white;
    public StatusEffect status = StatusEffects.none;
    public float statusDuration = 300;

    public Sound shootSound = Sounds.beamParallax;
    public float shootSoundVolume = 0.9f;

    public @Load(value = "@-base", fallback = "block-@size") TextureRegion baseRegion;
    public @Load("@-laser") TextureRegion laser;
    public @Load(value = "@-laser-start", fallback = "@-laser-end") TextureRegion laserStart;
    public @Load("@-laser-end") TextureRegion laserEnd;

    public TractorBeamTurret(String name){
        super(name);

        rotateSpeed = 10f;
        coolantMultiplier = 1f;
        envEnabled |= Env.space;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.targetsAir, targetAir);
        stats.add(Stat.targetsGround, targetGround);
        if(damage > 0) stats.add(Stat.damage, damage * 60f, StatUnit.perSecond);
    }

    @Override
    public void init(){
        super.init();

        updateClipRadius(range + tilesize);
    }

    public class TractorBeamBuild extends BaseTurretBuild{
        public @Nullable Unit target;
        public @Nullable Spore sporeTarget;
        public float lastX, lastY, strength;
        public boolean any;
        public float coolantMultiplier = 1f;

        @Override
        public void updateTile(){
            if(activationTimer > 0){
                activationTimer -= Time.delta;
                return;
            }

            float eff = efficiency * coolantMultiplier, edelta = eff * delta();

            //retarget
            if(timer(timerTarget, retargetTime)){
                if(targetSpore){
                    sporeTarget = findSporeTarget(range); //傻逼ai我真服了，真不如自己写，至少这个确实能用
                    target = null;
                }else{
                    target = Units.closestEnemy(team, x, y, range, u -> u.checkTarget(targetAir, targetGround));
                    sporeTarget = null;
                }
            }
            if(targetSpore && SporeCombat.invalid(team, sporeTarget, x, y, range)){
                sporeTarget = null;
            }
            boolean hasTarget = targetSpore ? sporeTarget != null : target != null;

            //consume coolant
            if(hasTarget && coolant != null){
                float maxUsed = coolant.amount;
                Liquid liquid = liquids.current();
                float used = Math.min(
                        Math.min(liquids.get(liquid), maxUsed * Time.delta),
                        Math.max(0, (1f / coolantMultiplier) / liquid.heatCapacity)
                );

                liquids.remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(
                            x + Mathf.range(size * tilesize / 2f),
                            y + Mathf.range(size * tilesize / 2f)
                    );
                }

                coolantMultiplier = 1f + (used * liquid.heatCapacity * coolantMultiplier);
            }

            any = false;

            if(targetSpore){
                if(sporeTarget != null && !SporeCombat.invalid(team, sporeTarget, x, y, range) && efficiency > 0.02f){
                    if(!headless){
                        control.sound.loop(shootSound, this, shootSoundVolume);
                    }

                    float dest = Angles.angle(x, y, sporeTarget.x, sporeTarget.y);
                    rotation = Angles.moveToward(rotation, dest, rotateSpeed * edelta);

                    lastX = sporeTarget.x;
                    lastY = sporeTarget.y;
                    strength = Mathf.lerpDelta(strength, 1f, 0.1f);

                    if(Angles.within(rotation, dest, shootCone)){
                        if(damage > 0f){
                            shootSpore(sporeTarget, damage * eff * timeScale, Fx.none);
                        }

                        any = true;

                        if(sporeTarget.removed){
                            sporeTarget = null;
                        }
                    }
                }else{
                    strength = Mathf.lerpDelta(strength, 0, 0.1f);
                }
            }else if(target != null && target.within(this, range + target.hitSize/2f)
                    && target.team() != team
                    && target.checkTarget(targetAir, targetGround)
                    && efficiency > 0.02f){
                // 原 Unit 逻辑完整保留
            }else{
                strength = Mathf.lerpDelta(strength, 0, 0.1f);
            }

            //look at target
            if(target != null && target.within(this, range + target.hitSize/2f) && target.team() != team && target.checkTarget(targetAir, targetGround) && efficiency > 0.02f){
                if(!headless){
                    control.sound.loop(shootSound, this, shootSoundVolume);
                }

                float dest = angleTo(target);
                rotation = Angles.moveToward(rotation, dest, rotateSpeed * edelta);
                lastX = target.x;
                lastY = target.y;
                strength = Mathf.lerpDelta(strength, 1f, 0.1f);

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone)){
                    if(damage > 0){
                        target.damageContinuous(damage * eff * timeScale * state.rules.blockDamage(team));
                    }

                    if(status != StatusEffects.none){
                        target.apply(status, statusDuration);
                    }

                    any = true;
                    target.impulseNet(Tmp.v1.set(this).sub(target).limit((force + (1f - target.dst(this) / range) * scaledForce) * edelta));
                }
            }else{
                strength = Mathf.lerpDelta(strength, 0, 0.1f);
            }
        }

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() && (targetSpore ? sporeTarget != null : target != null);
        }

        @Override
        public float estimateDps(){
            if(!any || damage <= 0) return 0f;
            return damage * 60f * efficiency * coolantMultiplier;
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Drawf.shadow(region, x - (size / 2f), y - (size / 2f), rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            //draw laser if applicable
            if(any && !isPayload()){
                Draw.z(Layer.bullet);
                float ang = angleTo(lastX, lastY);

                Draw.mixcol(laserColor, Mathf.absin(4f, 0.6f));

                Drawf.laser(laser, laserStart, laserEnd,
                x + Angles.trnsx(ang, shootLength), y + Angles.trnsy(ang, shootLength),
                lastX, lastY, strength * efficiency * laserWidth);

                Draw.mixcol();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            rotation = read.f();
        }
    }
}
