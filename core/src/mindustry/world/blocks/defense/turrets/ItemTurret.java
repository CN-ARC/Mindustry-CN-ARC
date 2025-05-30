package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.Time;
import arc.util.io.*;
import mindustry.*;
import mindustry.arcModule.NumberFormat;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ItemTurret extends Turret{
    public ObjectMap<Item, BulletType> ammoTypes = new OrderedMap<>();

    public ItemTurret(String name){
        super(name);
        hasItems = true;
    }

    /** Initializes accepted ammo map. Format: [item1, bullet1, item2, bullet2...] */
    public void ammo(Object... objects){
        ammoTypes = OrderedMap.of(objects);
    }

    /** Limits bullet range to this turret's range value. */
    public void limitRange(){
        limitRange(9f);
    }

    /** Limits bullet range to this turret's range value. */
    public void limitRange(float margin){
        for(var entry : ammoTypes.entries()){
            limitRange(entry.value, margin);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.itemCapacity);
        stats.add(Stat.ammo, StatValues.ammo(ammoTypes));
        stats.add(Stat.ammoCapacity, maxAmmo / ammoPerShot, StatUnit.shots);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        ammoTypes.each((Item, BulletType) -> {
            if (!Item.unlockedNow()) return;
            if (BulletType.rangeChange > 0)
                Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + BulletType.rangeChange, Pal.placing);

        });

        if (Core.settings.getBool("arcTurretPlacementItem")) {
            int sectors = ammoTypes.size;
            drawIndex = 0;
            float iconSize = 6f + 2f * size;
            ammoTypes.each((Item, BulletType) -> {
                drawIndex += 1;
                if (!Item.unlockedNow()) return;
                for (int i = 0; i < 4; i++) {
                    float rot = (i + ((float) drawIndex) / sectors) / 4 * 360f + Time.time * 0.5f;
                    Draw.rect(Item.uiIcon,
                            x * tilesize + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                            y * tilesize + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                            iconSize, iconSize, -rot);
                }
            });
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("ammo", (ItemTurretBuild entity) ->
                new Bar(() -> "子弹" + ((float) entity.totalAmmo > 0 ? NumberFormat.formatPercent(((ItemTurret.ItemEntry) entity.ammo.peek()).item.emoji(), entity.totalAmmo, maxAmmo) : ""),
                        () -> Pal.ammo,
                        () -> (float) entity.totalAmmo / maxAmmo
                )
        );
    }

    @Override
    public void init(){
        consume(new ConsumeItemFilter(i -> ammoTypes.containsKey(i)){
            @Override
            public void build(Building build, Table table){
                MultiReqImage image = new MultiReqImage();
                content.items().each(i -> filter.get(i) && i.unlockedNow(),
                item -> image.add(new ReqImage(new Image(item.uiIcon),
                () -> build instanceof ItemTurretBuild it && !it.ammo.isEmpty() && ((ItemEntry)it.ammo.peek()).item == item)));

                table.add(image).size(8 * 4);
            }

            @Override
            public float efficiency(Building build){
                //valid when it can shoot
                return build instanceof ItemTurretBuild it && it.ammo.size > 0 && (it.ammo.peek().amount >= ammoPerShot || it.cheating()) ? 1f : 0f;
            }

            @Override
            public void display(Stats stats){
                //don't display
            }
        });

        ammoTypes.each((item, type) -> placeOverlapRange = Math.max(placeOverlapRange, range + type.rangeChange + placeOverlapMargin));

        super.init();
    }

    public class ItemTurretBuild extends TurretBuild{

        @Override
        public void drawSelect(){

            if(Core.settings.getBool("arcTurretPlacementItem")) {
                if (ammo.isEmpty()) {
                    int sectors = ammoTypes.size;
                    drawIndex = 0;
                    float iconSize = 6f + 2f * size;
                    ammoTypes.each((Item, BulletType) -> {
                        drawIndex += 1;
                        if (!Item.unlockedNow()) return;
                        for (int i = 0; i < 4; i++) {
                            float rot = (i + ((float) drawIndex) / sectors) / 4 * 360f + Time.time * 0.5f;
                            Draw.rect(Item.uiIcon,
                                    x + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                                    y + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                                    iconSize, iconSize, -rot);
                        }
                    });
                }
                else{
                    float iconSize = 6f + 2f * size;
                    ItemTurret.ItemEntry entry = (ItemTurret.ItemEntry) ammo.peek();
                    Item lastAmmo = entry.item;
                    for (int i = 0; i < 4; i++) {
                        float rot = i / 4f * 360f + Time.time * 0.5f;
                        Draw.rect(lastAmmo.uiIcon,
                                x + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + entry.type().rangeChange + iconSize + 1f)),
                                y + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + entry.type().rangeChange + iconSize + 1f)),
                                iconSize, iconSize, -rot);
                    }
                }
            }

            super.drawSelect();
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            //add first ammo item to cheaty blocks so they can shoot properly
            if(!hasAmmo() && cheating() && ammoTypes.size > 0){
                handleItem(this, ammoTypes.keys().next());
            }
        }

        @Override
        public Object senseObject(LAccess sensor){
            return switch(sensor){
                case currentAmmoType -> ammo.size > 0 ? ((ItemEntry)ammo.peek()).item : null;
                default -> super.senseObject(sensor);
            };
        }

        @Override
        public void updateTile(){
            unit.ammo((float)unit.type().ammoCapacity * totalAmmo / maxAmmo);

            super.updateTile();
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            BulletType type = ammoTypes.get(item);

            if(type == null) return 0;

            return Math.min((int)((maxAmmo - totalAmmo) / ammoTypes.get(item).ammoMultiplier), amount);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            for(int i = 0; i < amount; i++){
                handleItem(null, item);
            }
        }

        //currently can't remove items from turrets.
        @Override
        public int removeStack(Item item, int amount){
            return 0;
        }

        @Override
        public void handleItem(Building source, Item item){
            //TODO instead of all this "entry" crap, turrets could just accept only one type of ammo at a time - simpler for both users and the code

            if(item == Items.pyratite){
                Events.fire(Trigger.flameAmmo);
            }

            if(totalAmmo == 0){
                Events.fire(Trigger.resupplyTurret);
            }

            BulletType type = ammoTypes.get(item);
            if(type == null) return;
            totalAmmo += type.ammoMultiplier;

            //find ammo entry by type
            for(int i = 0; i < ammo.size; i++){
                ItemEntry entry = (ItemEntry)ammo.get(i);

                //if found, put it to the right
                if(entry.item == item){
                    entry.amount += type.ammoMultiplier;
                    ammo.swap(i, ammo.size - 1);
                    return;
                }
            }

            //must not be found
            ammo.add(new ItemEntry(item, (int)type.ammoMultiplier));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return ammoTypes.get(item) != null && totalAmmo + ammoTypes.get(item).ammoMultiplier <= maxAmmo;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(ammo.size);
            for(AmmoEntry entry : ammo){
                ItemEntry i = (ItemEntry)entry;
                write.s(i.item.id);
                write.s(i.amount);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            ammo.clear();
            totalAmmo = 0;
            int amount = read.ub();
            for(int i = 0; i < amount; i++){
                Item item = Vars.content.item(revision < 2 ? read.ub() : read.s());
                short a = read.s();

                //only add ammo if this is a valid ammo type
                if(item != null && ammoTypes.containsKey(item)){
                    totalAmmo += a;
                    ammo.add(new ItemEntry(item, a));
                }
            }
        }
    }

    public class ItemEntry extends AmmoEntry{
        public Item item;

        ItemEntry(Item item, int amount){
            this.item = item;
            this.amount = amount;
        }

        @Override
        public BulletType type(){
            return ammoTypes.get(item);
        }

        @Override
        public String toString(){
            return "ItemEntry{" +
            "item=" + item +
            ", amount=" + amount +
            '}';
        }
    }
}
