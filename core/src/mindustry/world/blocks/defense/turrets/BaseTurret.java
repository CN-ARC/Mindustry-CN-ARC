package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.audio.Sound;
import arc.graphics.*;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.*;
import arc.math.geom.Vec2;
import arc.struct.*;
import arc.util.*;
import mindustry.arcModule.ARCVars;
import mindustry.arcreeper.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.Item;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BaseTurret extends Block{
    public float range = 80f;
    public float placeOverlapMargin = 8 * 7f;
    public float rotateSpeed = 5;
    public float fogRadiusMultiplier = 1f;
    public boolean disableOverlapCheck = false;
    /** How much time to start shooting after placement. */
    public float activationTime = 0f;

    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    /** If not null, this consumer will be used for coolant. */
    public @Nullable ConsumeLiquidBase coolant;

    /** Whether this turret targets ARCreeper Spore entities. */
    public boolean targetSpore = false;

    /** Default damage dealt to one Spore by one Spore shot. */
    public float sporeDamage = 45f;

    /** Default color used by Spore attack effects. */
    public Color sporeColor = Pal.accent;

    /** Default beam effect used by shootSpore(). */
    public Effect sporeBeamEffect = Fx.pointBeam;

    /** Default hit effect used by shootSpore(). */
    public Effect sporeHitEffect = Fx.pointHit;

    public BaseTurret(String name){
        super(name);

        update = true;
        solid = true;
        outlineIcon = true;
        attacks = true;
        priority = TargetPriority.turret;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
    }

    @Override
    public void init(){
        if(coolant == null){
            coolant = findConsumer(c -> c instanceof ConsumeCoolant);
        }

        checkInitCoolant();

        if(!disableOverlapCheck){
            placeOverlapRange = Math.max(placeOverlapRange, range + placeOverlapMargin);
        }
        fogRadius = Math.max(Mathf.round(range / tilesize * fogRadiusMultiplier), fogRadius);
        super.init();
    }

    @Override
    public void reinitializeConsumers(){
        checkInitCoolant();

        super.reinitializeConsumers();
    }

    void checkInitCoolant(){
        if(coolant != null){
            coolant.update = false;
            coolant.booster = true;
            coolant.optional = true;

            //json parsing does not add to consumes
            if(!hasConsumer(coolant)) consume(coolant);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
        if(state.rules.placeRangeCheck && Core.settings.getBool("arcTurretPlaceCheck")){
            Draw.alpha(0.5f);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, placeOverlapRange, Pal.remove);
        }
        if(fogRadiusMultiplier < 0.99f && state.rules.fog){
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * fogRadiusMultiplier, Pal.lightishGray);
        }

    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(Stat.targetsSpore, targetSpore);
        if(activationTime > 0) stats.add(Stat.activationTime, activationTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(activationTime > 0){
            addBar("activationtimer", (BaseTurretBuild entity) ->
            new Bar(() ->
            (entity.activationTimer > 0)? Core.bundle.format("bar.activationtimer", Mathf.ceil(entity.activationTimer / 60f)) : Core.bundle.get("bar.activated"),
            () -> (entity.activationTimer > 0)?  Pal.lightOrange : Pal.techBlue,
            () -> 1 - entity.activationTimer / activationTime));
        }
    }

    public class BaseTurretBuild extends Building implements Ranged, RotBlock{
        public float rotation = 90;
        public float activationTimer = 0;

        @Override
        public void placed(){
            super.placed();
            activationTimer = activationTime;
        }

        @Override
        public float range(){
            return range;
        }

        @Override
        public float buildRotation(){
            return rotation;
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range(), team.color);
        }

        public float estimateDps(){
            return 0f;
        }

        @Override
        public BlockStatus status() {
            return (activationTimer <= 0)? super.status() : BlockStatus.inactive;
        }

        protected @Nullable Spore findSporeTarget(float range){
            return SporeCombat.bestTarget(team, x, y, range);
        }

        protected boolean invalidSporeTarget(@Nullable Posc target, float range){
            return !(target instanceof Spore spore) || SporeCombat.invalid(team, spore, x, y, range);
        }

        protected boolean setSporeTargetPosition(@Nullable Posc target, Vec2 out){
            if(!(target instanceof Spore spore)) return false;

            out.set(spore.x, spore.y);
            return true;
        }

        protected boolean shootSpore(Spore spore, float damage, @Nullable Effect shootEffect){
            if(spore == null) return false;

            float targetX = spore.x;
            float targetY = spore.y;
            float rot = Angles.angle(x, y, targetX, targetY);

            boolean hit = SporeCombat.attackSpore(
                    team,
                    spore,
                    x,
                    y,
                    range(),
                    damage * state.rules.blockDamage(team),
                    efficiency
            );

            if(!hit) return false;

            if(sporeBeamEffect != null && sporeBeamEffect != Fx.none){
                sporeBeamEffect.at(x, y, rot, sporeColor, new Vec2(targetX, targetY));
            }

            if(shootEffect != null && shootEffect != Fx.none){
                shootEffect.at(x, y, rot, sporeColor);
            }

            if(sporeHitEffect != null && sporeHitEffect != Fx.none){
                sporeHitEffect.at(targetX, targetY, sporeColor);
            }

            return true;
        }
    }
}
