package mindustry.world.blocks.defense.turrets;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.Time;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class ContinuousLiquidTurret extends ContinuousTurret{
    public ObjectMap<Liquid, BulletType> ammoTypes = new ObjectMap<>();
    public float liquidConsumed = 1f / 60f;

    public ContinuousLiquidTurret(String name){
        super(name);
        hasLiquids = true;
        //TODO
        loopSound = Sounds.loopMineBeam;
        shootSound = Sounds.none;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
    }

    /** Initializes accepted ammo map. Format: [liquid1, bullet1, liquid2, bullet2...] */
    public void ammo(Object... objects){
        ammoTypes = ObjectMap.of(objects);
    }

    @Override
    public void setStats(){
        super.setStats();
        //mirror stats onto each bullet (purely visual)
        ammoTypes.each((l, b) -> b.statLiquidConsumed = liquidConsumed);

        stats.replace(Stat.ammo, StatValues.ammo(ammoTypes));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        ammoTypes.each((Item, BulletType)->{
            if(BulletType.rangeChange>0 && Item.unlockedNow()) Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + BulletType.rangeChange, Pal.placing);
        });

        if(Core.settings.getBool("arcTurretPlacementItem") && ammoTypes.size>=2) {
            int sectors = ammoTypes.size;
            drawIndex = 0;
            float iconSize = 6f + 2f * size;
            ammoTypes.each((Item, BulletType) -> {
                drawIndex += 1;
                if (!Item.unlockedNow()) return;
                for (int i = 0; i < 4; i++) {
                    float rot = (i + ((float)drawIndex) / sectors) / 4 * 360f + Time.time * 0.5f;
                    Draw.rect(Item.uiIcon,
                            x * tilesize + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                            y * tilesize + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                            iconSize, iconSize, -rot);
                }
            });
        }
    }

    @Override
    public void init(){
        consume(new ConsumeLiquidFilter(i -> ammoTypes.containsKey(i), liquidConsumed){

            {
                multiplier = b -> {
                    var ammo = ammoTypes.get(b.liquids.current());
                    return ammo == null ? 1f : 1f / ammo.ammoMultiplier;
                };
            }

            @Override
            public void display(Stats stats){

            }
        });

        if(targetGround){
            ammoTypes.each((item, type) -> placeOverlapRange = Math.max(placeOverlapRange, range + type.rangeChange + placeOverlapMargin));
        }

        super.init();
    }

    public class ContinuousLiquidTurretBuild extends ContinuousTurretBuild{
        boolean activated;

        @Override
        public void drawSelect(){
            super.drawSelect();
            if(Core.settings.getBool("arcTurretPlacementItem")){
                if(liquids.currentAmount()==0){
                    int sectors = ammoTypes.size;
                    drawIndex = 0;
                    float iconSize = 6f + 2f * size;
                    ammoTypes.each((Item, BulletType) -> {
                        drawIndex += 1;
                        if (!Item.unlockedNow()) return;
                        for (int i = 0; i < 4; i++) {
                            float rot = (i + ((float)drawIndex) / sectors) / 4 * 360f + Time.time * 0.5f;
                            Draw.rect(Item.uiIcon,
                                    x + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                                    y + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                                    iconSize, iconSize, -rot);
                        }
                    });
                }
                else{
                    float iconSize = 6f + 2f * size;
                    for (int i = 0; i < 4; i++) {
                        float rot = i / 4f * 360f + Time.time * 0.5f;
                        Draw.rect(liquids.current().uiIcon,
                                x + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + ammoTypes.get(liquids.current()).rangeChange + iconSize + 1f)),
                                y + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + ammoTypes.get(liquids.current()).rangeChange + iconSize + 1f)),
                                iconSize, iconSize, -rot);
                    }
                }
            }
        }

        @Override
        public boolean shouldActiveSound(){
            return wasShooting && enabled;
        }

        @Override
        public float getAmmoFraction(){
            return liquids.currentAmount() / liquidCapacity;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            //only allow the turret to begin firing when it can fire for 4 continuous updates
            if(liquids.currentAmount() >= liquidConsumed * 4f){
                activated = true;
            }else if(liquids.currentAmount() < liquidConsumed){
                activated = false;
            }
        }

        @Override
        public Object senseObject(LAccess sensor){
            return switch(sensor){
                case currentAmmoType -> liquids.current();
                default -> super.senseObject(sensor);
            };
        }

        @Override
        public boolean canConsume(){
            return hasCorrectAmmo() && super.canConsume();
        }

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() && activated;
        }

        @Override
        public BulletType useAmmo(){
            //does not consume ammo upon firing
            return peekAmmo();
        }

        @Override
        public BulletType peekAmmo(){
            return ammoTypes.get(liquids.current());
        }

        @Override
        public boolean hasAmmo(){
            return hasCorrectAmmo() && ammoTypes.get(liquids.current()) != null && liquids.currentAmount() > 0f && activated;
        }

        public boolean hasCorrectAmmo(){
            return !bullets.any() || bullets.first().bullet.type == peekAmmo();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return ammoTypes.get(liquid) != null &&
                (liquids.current() == liquid ||
                ((!ammoTypes.containsKey(liquids.current()) || liquids.get(liquids.current()) <= 1f / ammoTypes.get(liquids.current()).ammoMultiplier + 0.001f)));
        }
    }
}
