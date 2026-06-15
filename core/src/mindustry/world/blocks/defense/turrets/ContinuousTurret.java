package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.arcreeper.Spore;
import mindustry.arcreeper.SporeCombat;
import mindustry.content.*;
import mindustry.entities.Damage;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

/** A turret that fires a continuous beam bullet with no reload or coolant necessary. The bullet only disappears when the turret stops shooting. */
public class ContinuousTurret extends Turret{
    public BulletType shootType = Bullets.placeholder;
    /** Speed at which the turret can change its bullet "aim" distance. This is only used for point laser bullets. */
    public float aimChangeSpeed = Float.POSITIVE_INFINITY;
    public boolean scaleDamageEfficiency = false;
    /** Visual hit size used when testing whether a continuous beam touches an ARCreeper Spore. */
    public float sporeHitSize = 20f;

    public ContinuousTurret(String name){
        super(name);

        coolantMultiplier = 1f;
        envEnabled |= Env.space;
        displayAmmoMultiplier = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
        stats.remove(Stat.reload);
        stats.remove(Stat.inaccuracy);
    }

    //TODO LaserTurret shared code
    public class ContinuousTurretBuild extends TurretBuild{
        public Seq<BulletEntry> bullets = new Seq<>();
        public float lastLength = size * 4f;
        public float sporeDamageTimer;
        public boolean sporeBeamHitting;

        @Override
        public float estimateDps(){
            if(!hasAmmo()) return 0f;
            return shootType.damage * 60f / (shootType instanceof ContinuousBulletType c ? c.damageInterval : 5f);
        }

        @Override
        protected void updateCooling(){
            //TODO how does coolant work here, if at all?
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            return canConsume();
        }

        @Override
        public boolean shouldConsume(){
            return isShooting() && (!(targetSpore && target instanceof Spore) || sporeBeamHitting);
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }

        @Override
        public float getAmmoFraction(){
            //TODO unclean way of calculating ammo fraction to display
            float ammoFract = efficiency;
            if(findConsumer(f -> f instanceof ConsumeLiquidBase) instanceof ConsumeLiquid cons){
                ammoFract = Math.min(ammoFract, liquids.get(cons.liquid) / liquidCapacity);
            }

            return ammoFract;
        }

        @Override
        public void updateTile(){
            sporeBeamHitting = false;

            super.updateTile();

            bullets.removeAll(b -> !b.bullet.isAdded() || b.bullet.type == null || b.bullet.owner != this);

            if(bullets.any()){
                for(var entry : bullets){
                    updateBullet(entry);
                }

                if(targetSpore && target instanceof Spore spore && isShooting() && canConsume() && !charging() && shootWarmup >= minWarmup){
                    sporeBeamHitting = updateSporeBeamDamage(spore);
                }else{
                    sporeDamageTimer = 0f;
                }

                wasShooting = true;
                heat = 1f;
                curRecoil = recoil;
            }else{
                sporeDamageTimer = 0f;
            }
        }

        protected void updateBullet(BulletEntry entry){
            float
                bulletX = x + Angles.trnsx(rotation - 90, shootX + entry.x, shootY + entry.y),
                bulletY = y + Angles.trnsy(rotation - 90, shootX + entry.x, shootY + entry.y),
                angle = rotation + entry.rotation;

            entry.bullet.rotation(angle);
            entry.bullet.set(bulletX, bulletY);

            //target length of laser
            float shootLength = Math.min(dst(targetPos), range);
            //current length of laser
            float curLength = dst(entry.bullet.aimX, entry.bullet.aimY);
            //resulting length of the bullet (smoothed)
            float resultLength = Mathf.approachDelta(curLength, shootLength, aimChangeSpeed);
            //actual aim end point based on length
            Tmp.v1.trns(rotation, lastLength = resultLength).add(x, y);

            entry.bullet.aimX = Tmp.v1.x;
            entry.bullet.aimY = Tmp.v1.y;
            if(scaleDamageEfficiency){
                entry.bullet.damage = entry.bullet.type.damage * Math.min(efficiency, 1f) * timeScale * entry.bullet.damageMultiplier();
            }

            if(isShooting() && hasAmmo()){
                entry.bullet.time = entry.bullet.lifetime * entry.bullet.type.optimalLifeFract * Math.min(shootWarmup, efficiency);
                entry.bullet.keepAlive = true;
            }
        }

        @Override
        protected void updateReload(){
            //continuous turrets don't have a concept of reload, they are always firing when possible
        }

        protected float sporeDamageInterval(){
            BulletType type = peekAmmo();

            if(type instanceof ContinuousBulletType c){
                return Math.max(c.damageInterval, 1f);
            }

            if(type instanceof PointLaserBulletType p){
                return Math.max(p.damageInterval, 1f);
            }

            return 5f;
        }

        protected boolean updateSporeBeamDamage(Spore spore){
            int hitCount = 0;

            for(var entry : bullets){
                if(sporeBeamHits(entry.bullet, spore)){
                    hitCount++;
                }
            }

            if(hitCount <= 0){
                sporeDamageTimer = 0f;
                return false;
            }

            sporeDamageTimer += delta() * efficiency;

            float interval = sporeDamageInterval();

            while(sporeDamageTimer >= interval){
                sporeDamageTimer -= interval;

                if(!SporeCombat.attackSpore(
                        team,
                        spore,
                        x,
                        y,
                        range(),
                        sporeDamage * hitCount * Vars.state.rules.blockDamage(team),
                        efficiency
                )){
                    sporeDamageTimer = 0f;
                    break;
                }

                if(sporeHitEffect != null && sporeHitEffect != Fx.none){
                    sporeHitEffect.at(spore.x, spore.y, sporeColor);
                }

                if(spore.removed){
                    break;
                }
            }

            return true;
        }

        protected boolean sporeBeamHits(Bullet bullet, Spore spore){
            if(bullet == null || bullet.type == null || !bullet.isAdded() || spore == null || spore.removed){
                return false;
            }

            float radius = sporeHitSize / 2f;

            if(bullet.type instanceof PointLaserBulletType){
                return Mathf.dst2(bullet.aimX, bullet.aimY, spore.x, spore.y) <= radius * radius;
            }

            float length;

            if(bullet.type instanceof ContinuousBulletType c){
                length = Damage.findLength(bullet, c.currentLength(bullet), c.laserAbsorb, c.pierceCap);
            }else{
                length = Mathf.dst(bullet.x, bullet.y, bullet.aimX, bullet.aimY);
            }

            if(length <= 0f){
                return false;
            }

            float x2 = bullet.x + Angles.trnsx(bullet.rotation(), length);
            float y2 = bullet.y + Angles.trnsy(bullet.rotation(), length);

            return dst2PointSegment(spore.x, spore.y, bullet.x, bullet.y, x2, y2) <= radius * radius;
        }

        protected float dst2PointSegment(float px, float py, float x1, float y1, float x2, float y2){
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len2 = dx * dx + dy * dy;

            if(len2 <= 0.0001f){
                return Mathf.dst2(px, py, x1, y1);
            }

            float t = Mathf.clamp(((px - x1) * dx + (py - y1) * dy) / len2);
            float cx = x1 + dx * t;
            float cy = y1 + dy * t;

            return Mathf.dst2(px, py, cx, cy);
        }

        @Override
        protected void updateShooting(){
            if(targetSpore && target instanceof Spore){
                if(!bullets.any() && canConsume() && !charging() && shootWarmup >= minWarmup){
                    shoot(peekAmmo());
                }

                return;
            }

            if(bullets.any()){
                return;
            }

            if(canConsume() && !charging() && shootWarmup >= minWarmup){
                shoot(peekAmmo());
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, efficiency * rotateSpeed * delta());
        }

        @Override
        protected void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){
            if(bullet != null){
                bullets.add(new BulletEntry(bullet, offsetX, offsetY, angleOffset, 0f));

                //make sure the length updates to the last set value
                Tmp.v1.trns(rotation, shootY + lastLength).add(x, y);
                bullet.aimX = Tmp.v1.x;
                bullet.aimY = Tmp.v1.y;
            }
        }

        @Override
        public boolean shouldActiveSound(){
            return bullets.any();
        }

        @Override
        public float activeSoundVolume(){
            return 1f;
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(lastLength);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 3){
                lastLength = read.f();
            }
        }
    }
}
