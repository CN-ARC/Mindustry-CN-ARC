package mindustry.arcreeper;

import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.TargetPriority;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

import static mindustry.Vars.tilesize;

public class SporeTurret extends Turret{
    public float damage = 45f;
    public Color color = Pal.accent;

    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;

    public SporeTurret(String name){
        super(name);

        update = true;
        solid = true;
        rotate = true;
        priority = TargetPriority.turret;

        // SporeTurret 不走标准 Unit/Building/BulletType 炮塔流程。
        playerControllable = false;
        targetAir = false;
        targetGround = false;
        targetBlocks = false;
        displayAmmoMultiplier = false;

        shootCone = 5f;
        shootEffect = Fx.sparkShoot;
        shootSound = Sounds.shootSegment;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.damage, damage);
    }

    public class SporeTurretBuild extends TurretBuild{
        public Spore target;

        @Override
        public void updateTile(){
            if(target == null || SporeCombat.invalid(team, target, x, y, range())){
                target = null;
            }

            if(timer(timerTarget, target == null ? targetInterval : newTargetInterval)){
                target = SporeCombat.bestTarget(team, x, y, range());
            }

            if(soundLoop != null){
                soundLoop.update(x, y, shouldActiveSound(), activeSoundVolume());
            }

            boolean active = target != null && enabled && activationTimer <= 0f;
            float warmupTarget = active && canConsume() ? 1f : 0f;

            if(linearWarmup){
                shootWarmup = Mathf.approachDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0f ? efficiency : 1f));
            }else{
                shootWarmup = Mathf.lerpDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0f ? efficiency : 1f));
            }

            wasShooting = false;
            curRecoil = Mathf.approachDelta(curRecoil, 0f, 1f / recoilTime);

            if(recoils > 0){
                if(curRecoils == null) curRecoils = new float[recoils];
                for(int i = 0; i < recoils; i++){
                    curRecoils[i] = Mathf.approachDelta(curRecoils[i], 0f, 1f / recoilTime);
                }
            }

            heat = Mathf.approachDelta(heat, 0f, 1f / cooldownTime);
            charge = 0f;

            unit.tile(this);
            unit.rotation(rotation);
            unit.team(team);
            recoilOffset.trns(rotation, -Mathf.pow(curRecoil, recoilPow) * recoil);

            if(activationTimer > 0f){
                activationTimer -= Time.delta;
                return;
            }

            if(target == null){
                targetPos.setZero();
                return;
            }

            targetPos.set(target.x, target.y);

            float targetRot = angleTo(targetPos);
            if(shouldTurn()){
                turnToTarget(targetRot);
            }

            if(efficiency > 0f){
                reloadCounter += delta() * baseReloadSpeed();
                reloadCounter = Math.min(reloadCounter, reload);
                updateCooling();
            }

            if(Angles.within(rotation, targetRot, shootCone) && reloadCounter >= reload && canConsume()){
                wasShooting = true;
                shootSpore();
                reloadCounter = 0f;
            }
        }

        @Override
        public boolean hasAmmo(){
            // 孢子炮塔没有标准 BulletType 弹药；物品/电力消耗仍由 consume 与 efficiency 管线处理。
            return true;
        }

        @Override
        public boolean shouldConsume(){
            return target != null && enabled;
        }

        @Override
        public float fogRadius(){
            return range / tilesize * fogRadiusMultiplier;
        }

        @Override
        public boolean isShooting(){
            return target != null;
        }

        @Override
        public boolean isActive(){
            return (target != null || wasShooting) && enabled && activationTimer <= 0f;
        }

        @Override
        protected float ammoReloadMultiplier(){
            return 1f;
        }

        protected void shootSpore(){
            if(target == null) return;

            float targetX = target.x;
            float targetY = target.y;
            float shootWorldX = x + Angles.trnsx(rotation - 90f, shootX, shootY);
            float shootWorldY = y + Angles.trnsy(rotation - 90f, shootX, shootY);

            boolean hit = SporeCombat.attackSpore(
                    team,
                    target,
                    x,
                    y,
                    range(),
                    damage * Vars.state.rules.blockDamage(team),
                    efficiency
            );

            if(!hit) return;

            beamEffect.at(
                    shootWorldX,
                    shootWorldY,
                    rotation,
                    color,
                    new Vec2(targetX, targetY)
            );

            if(shootEffect != null){
                shootEffect.at(
                        shootWorldX,
                        shootWorldY,
                        rotation,
                        color
                );
            }

            hitEffect.at(
                    targetX,
                    targetY,
                    color
            );

            shootSound.at(
                    shootWorldX,
                    shootWorldY,
                    Mathf.random(soundPitchMin, soundPitchMax),
                    shootSoundVolume
            );

            curRecoil = 1f;
            heat = 1f;
            totalShots++;

            if(target.removed){
                target = null;
            }
        }
    }
}
