package mindustry.world.meta;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.ARCVars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.BurstDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.Separator;

import static mindustry.Vars.*;

/** Utilities for displaying certain stats in a table. */
public class StatValues{

    public static StatValue string(String value, Object... args){
        String result = Strings.format(value, args);
        return table -> table.add(result);
    }

    public static StatValue bool(boolean value){
        return table ->  table.add(!value ? "@no" : "@yes");
    }

    public static String fixValue(float value){
        return Strings.autoFixed(value, 2);
    }

    public static StatValue squared(float value, StatUnit unit){
        return table -> {
            String fixed = fixValue(value);
            table.add(fixed + "x" + fixed);
            table.add((unit.space ? " " : "") + unit.localized());
        };
    }

    public static StatValue number(float value, StatUnit unit, boolean merge){
        return table -> {
            String l1 = (unit.icon == null ? "" : unit.icon + " ") + fixValue(value), l2 = (unit.space ? " " : "") + unit.localized();

            if(merge){
                table.add(l1 + l2).left();
            }else{
                table.add(l1).left();
                table.add(l2).left();
            }
        };
    }

    public static StatValue number(float value, StatUnit unit){
        return number(value, unit, false);
    }

    public static StatValue multiplierModifier(float value, StatUnit unit, boolean merge){
        return table -> {
            String l1 = (unit.icon == null ? "" : unit.icon + " ") + multStat(value), l2 = (unit.space ? " " : "") + unit.localized();

            if(merge){
                table.add(l1 + l2).left();
            }else{
                table.add(l1).left();
                table.add(l2).left();
            }
        };
    }

    public static StatValue multiplierModifier(float value, StatUnit unit){
        return multiplierModifier(value, unit, true);
    }

    public static StatValue multiplierModifier(float value){
        return multiplierModifier(value, StatUnit.multiplier);
    }

    public static StatValue percentModifier(float value, StatUnit unit, boolean merge){
        return table -> {
            String l1 = (unit.icon == null ? "" : unit.icon + " ") + ammoStat((value - 1) * 100), l2 = (unit.space ? " " : "") + unit.localized();

            if(merge){
                table.add(l1 + l2).left();
            }else{
                table.add(l1).left();
                table.add(l2).left();
            }
        };
    }

    public static StatValue percentModifier(float value, StatUnit unit){
        return percentModifier(value, unit, true);
    }

    public static StatValue percentModifier(float value){
        return percentModifier(value, StatUnit.percent);
    }

    public static StatValue liquid(Liquid liquid, float amount, boolean perSecond){
        return table -> table.add(displayLiquid(liquid, amount, perSecond)).left();
    }

    public static StatValue liquids(Boolf<Liquid> filter, float amount, boolean perSecond){
        return table -> {
            Seq<Liquid> list = content.liquids().select(i -> filter.get(i) && i.unlockedNow() && !i.isHidden());

            for(int i = 0; i < list.size; i++){
                table.add(displayLiquid(list.get(i), amount, perSecond)).padRight(5);

                if(i != list.size - 1){
                    table.add("/");
                }
            }
        };
    }

    public static StatValue liquids(float timePeriod, LiquidStack... stacks){
        return liquids(timePeriod, true, stacks);
    }

    public static StatValue liquids(float timePeriod, boolean perSecond, LiquidStack... stacks){
        return table -> {
            for(var stack : stacks){
                table.add(displayLiquid(stack.liquid, stack.amount * (60f / timePeriod), perSecond)).padRight(5);
            }
        };
    }

    public static StatValue items(ItemStack... stacks){
        return items(true, stacks);
    }

    public static StatValue items(boolean displayName, ItemStack... stacks){
        return table -> {
            for(ItemStack stack : stacks){
                table.add(displayItem(stack.item, stack.amount, displayName)).padRight(5);
            }
        };
    }

    public static StatValue items(float timePeriod, ItemStack... stacks){
        return table -> {
            for(ItemStack stack : stacks){
                table.add(displayItem(stack.item, stack.amount, timePeriod, true)).padRight(5);
            }
        };
    }

    public static StatValue items(Boolf<Item> filter){
        return items(-1, filter);
    }

    public static StatValue items(float timePeriod, Boolf<Item> filter){
        return table -> {
            Seq<Item> list = content.items().select(i -> filter.get(i) && i.unlockedNow() && !i.isHidden());

            for(int i = 0; i < list.size; i++){
                Item item = list.get(i);

                table.add(timePeriod <= 0 ? displayItem(item) : displayItem(item, 1, timePeriod, true)).padRight(5);

                if(i != list.size - 1){
                    table.add("/");
                }
            }
        };
    }

    public static Table displayLiquid(Liquid liquid, float amount, boolean perSecond){
        Table t = new Table();

        t.add(new Stack(){{
            add(new Image(liquid.uiIcon).setScaling(Scaling.fit));

            if(amount != 0){
                Table t = new Table().left().bottom();
                t.add(Strings.autoFixed(amount, 2)).style(Styles.outlineLabel);
                add(t);
            }
        }}).size(iconMed).padRight(3  + (amount != 0 ? (Strings.autoFixed(amount, 2).length() - 1) * 10 : 0)).with(s -> withTooltip(s, liquid, false));

        if(perSecond && amount != 0){
            t.add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);
        }

        t.add(liquid.localizedName);

        return t;
    }

    public static <T extends Element> T withTooltip(T element, UnlockableContent content, boolean tooltip){
        if(content != null){
            if(!mobile){
                if(tooltip){
                    element.addListener(Tooltips.getInstance().create(content.localizedName, mobile));
                }
                element.addListener(new HandCursorListener(() -> !content.isHidden(), true));
            }
            element.clicked(() -> {
                if(!content.isHidden()){
                    Vars.ui.content.show(content);
                }
            });
        }
        return element;
    }

    public static <T extends Element> T withTooltip(T element, UnlockableContent content){
        return withTooltip(element, content, false);
    }

    /** Displays an item with a specified amount. */
    private static Stack stack(TextureRegion region, int amount, @Nullable UnlockableContent content, boolean tooltip){
        Stack stack = new Stack();

        stack.add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f).scaling(Scaling.fit);
        }));

        if(amount != 0){
            stack.add(new Table(t -> {
                t.left().bottom();
                t.add(amount >= 1000 ? UI.formatAmount(amount) : amount + "").style(Styles.outlineLabel);
                t.pack();
            }));
        }

        withTooltip(stack, content, tooltip);

        return stack;
    }

    /** Displays an item with a specified amount. */
    private static Stack stack(TextureRegion region, int amount, @Nullable UnlockableContent content){
        return stack(region, amount, content, true);
    }

    public static Stack stack(ItemStack stack){
        return stack(stack.item.uiIcon, stack.amount, stack.item);
    }

    public static Stack stack(UnlockableContent item, int amount){
        return stack(item.uiIcon, amount, item);
    }

    public static Stack stack(UnlockableContent item, int amount, boolean tooltip){
        return stack(item.uiIcon, amount, item, tooltip);
    }

    public static Stack stack(Item item){
        return stack(item.uiIcon, 0, item);
    }

    public static Stack stack(PayloadStack stack){
        return stack(stack.item.uiIcon, stack.amount, stack.item);
    }

    public static Table displayItem(Item item){
        return displayItem(item, 0);
    }

    public static Table displayItem(Item item, int amount, boolean showName){
        Table t = new Table();
        t.add(stack(item, amount, !showName));
        if(showName) t.add(item.localizedName).padLeft(4 + amount > 99 ? 4 : 0);
        return t;
    }

    public static Table displayItem(Item item, int amount){
        return displayItem(item, amount, true);
    }

    /** Displays the item with a "/sec" qualifier based on the time period, in ticks. */
    public static Table displayItem(Item item, int amount, float timePeriod, boolean showName){
        Table t = new Table();
        t.add(stack(item, amount, !showName));
        t.add((showName ? item.localizedName + "\n" : "") + "[lightgray]" + Strings.autoFixed(amount / (timePeriod / 60f), 2) + StatUnit.perSecond.localized()).padLeft(2).padRight(5).style(Styles.outlineLabel);
        return t;
    }

    /** Displays the item with a "/sec" qualifier based on the time period, in ticks. */
    public static Table displayItemPercent(Item item, int percent, boolean showName){
        Table t = new Table();
        t.add(stack(item, 0, !showName));
        t.add((showName ? item.localizedName + "\n" : "") + "[lightgray]" +  percent + "%").padLeft(2).padRight(5).style(Styles.outlineLabel);
        return t;
    }

    public static StatValue content(UnlockableContent content){
        return table -> {
            table.add(new Image(content.uiIcon)).size(iconSmall).padRight(3);
            table.add(content.localizedName).padRight(3);
        };
    }

    public static StatValue blockEfficiency(Block floor, float multiplier, boolean startZero){
        return table -> table.stack(
            new Image(floor.uiIcon).setScaling(Scaling.fit),
            new Table(t -> t.top().right().add((multiplier < 0 ? "[scarlet]" : startZero ? "[accent]" : "[accent]+") + (int)((multiplier) * 100) + "%").style(Styles.outlineLabel))
        ).maxSize(64f);
    }

    public static StatValue blocks(Attribute attr, boolean floating, float scale, boolean startZero){
        return blocks(attr, floating, scale, startZero, true);
    }

    public static StatValue blocks(Attribute attr, boolean floating, float scale, boolean startZero, boolean checkFloors){
        return table -> table.table(c -> {
            Runnable[] rebuild = {null};
            Map[] lastMap = {null};

            rebuild[0] = () -> {
                c.clearChildren();
                c.left();

                if(state.isGame()){
                    var blocks = Vars.content.blocks()
                    .select(block -> (!checkFloors || block instanceof Floor) && block.attributes.get(attr) != 0 && !((block instanceof Floor f && f.isDeep()) && !floating))
                    .with(s -> s.sort(f -> f.attributes.get(attr)));

                    if(blocks.any()){
                        int i = 0;
                        for(var block : blocks){

                            blockEfficiency(block, block.attributes.get(attr) * scale, startZero).display(c);
                            if(++i % 5 == 0){
                                c.row();
                            }
                        }
                    }else{
                        c.add("@none.inmap");
                    }
                }else{
                    c.add("@stat.showinmap");
                }
            };

            rebuild[0].run();

            //rebuild when map changes.
            c.update(() -> {
                Map current = state.isGame() ? state.map : null;

                if(current != lastMap[0]){
                    rebuild[0].run();
                    lastMap[0] = current;
                }
            });
        });
    }
    public static StatValue content(Seq<UnlockableContent> list){
        return content(list, i -> true);
    }

    public static <T extends UnlockableContent> StatValue content(Seq<T> list, Boolf<T> check){
        return table -> table.table(l -> {
            l.left();

            boolean any = false;
            for(int i = 0; i < list.size; i++){
                var item = list.get(i);

                if(!check.get(item)) continue;
                any = true;

                if(item.uiIcon.found()) l.image(item.uiIcon).size(iconSmall).scaling(Scaling.fit).padRight(2).padLeft(2).padTop(3).padBottom(3).with(img -> withTooltip(img, item, false));
                l.add(item.localizedName).left().padLeft(1).padRight(4).colspan(item.uiIcon.found() ? 1 : 2);
                if(i % 5 == 4){
                    l.row();
                }
            }

            if(!any){
                l.add("@none.inmap");
            }
        });
    }

    public static StatValue drillUnit(UnitType unit){
        Seq<Block> list = content.blocks().select(b ->
                b.itemDrop != null &&
                        (b instanceof Floor f && (((f.wallOre && unit.mineWalls) || (!f.wallOre && unit.mineFloor))) ||
                                (!(b instanceof Floor) && unit.mineWalls)) &&
                        b.itemDrop.hardness <= unit.mineTier && (!b.playerUnmineable || Core.settings.getBool("doubletapmine")));
        list.sort(t->t.itemDrop.hardness);
        return table -> {
            table.row();
            table.table(t -> {
                t.background(Styles.grayPanel);
                t.table(tt->{
                    StringBuilder oreList = new StringBuilder();
                    if(unit.mineHardnessScaling) {
                        for (int i = 0; i < list.size; i++) {
                            Block block = list.get(i);
                            oreList.append(block.emoji()).append(" ").append(block.localizedName);
                            if (i == list.size - 1 || list.get(i + 1).itemDrop.hardness != block.itemDrop.hardness) {
                                tt.labelWrap(oreList.toString()).width(250f).padLeft(20f).padTop(5f);
                                float eff = 60f * unit.mineSpeed / (50f + list.get(i).itemDrop.hardness * 15f);
                                tt.add("[stat]" + Strings.fixed(eff, 2)).padLeft(20f);
                                tt.row();
                                oreList = new StringBuilder();
                            } else oreList.append("  ");
                        }
                    } else {
                        for (int i = 0; i < list.size; i++) {
                            Block block = list.get(i);
                            oreList.append(block.emoji()).append(" ").append(block.localizedName);
                        }
                        tt.labelWrap(oreList.toString()).width(250f).padLeft(20f).padTop(5f);
                        float eff = 60f * unit.mineSpeed / (50f + 15f);
                        tt.add("[stat]" + Strings.fixed(eff, 2)).padLeft(20f);
                    }
                });
            });
        };
    }
    public static StatValue drillBlock(Drill drill) {
        Seq<Block> list = content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null && f.itemDrop.hardness <= drill.tier && f.itemDrop != drill.blockedItem);

        if (drill instanceof BurstDrill burstDrill) {
            list.sort(t -> burstDrill.drillMultipliers.get(t.itemDrop, 1f));
            return table -> {
                table.row();
                table.table(at->{
                    at.background(Styles.grayPanel);
                    at.add("[stat]" + drill.tier + "[lightgray]级[#" + ARCVars.getThemeColor() + "] ~ [stat]" +
                            Strings.autoFixed(60f / drill.drillTime * drill.size * drill.size,2) + "[lightgray]物品/s");
                    at.row();
                    at.table(t -> {
                        StringBuilder oreList = new StringBuilder();
                        for (int i = 0; i < list.size; i++) {
                            Block block = list.get(i);
                            oreList.append(block.emoji()).append(" ").append(block.localizedName);
                            if (i == list.size - 1 || burstDrill.drillMultipliers.containsKey(list.get(i + 1).itemDrop)) {
                                t.labelWrap(oreList.toString()).width(250f).padLeft(20f).padTop(5f);
                                float eff = 60f / (drill.drillTime + drill.hardnessDrillMultiplier * block.itemDrop.hardness) * drill.size * drill.size * burstDrill.drillMultipliers.get(block.itemDrop, 1f);
                                t.add("[stat]" + Strings.fixed(eff, 2)).padLeft(20f);
                                t.add("[cyan]" + Strings.fixed(eff * drill.liquidBoostIntensity * drill.liquidBoostIntensity, 2)).padLeft(20f).padRight(20f);
                                t.row();
                                oreList = new StringBuilder();
                            } else oreList.append("  ");
                        }
                    });
                });
            };
        } else {
            list.sort(t -> t.itemDrop.hardness);
            return table -> {
            table.row();
            table.table(at->{
                at.background(Styles.grayPanel);
                at.add("[stat]" + drill.tier + "[lightgray]级[#" + ARCVars.getThemeColor() + "] ~ [stat]" +
                        Strings.autoFixed(60f / drill.drillTime * drill.size * drill.size,2) + "[lightgray]物品/s");
                at.row();
                at.table(t -> {
                    StringBuilder oreList = new StringBuilder();
                    for (int i = 0; i < list.size; i++) {
                        Block block = list.get(i);
                        oreList.append(block.emoji()).append(" ").append(block.localizedName);
                        if (i == list.size - 1 || list.get(i + 1).itemDrop.hardness != block.itemDrop.hardness) {
                            t.labelWrap(oreList.toString()).width(250f).padLeft(20f).padTop(5f);
                            float eff = 60f / (drill.drillTime + drill.hardnessDrillMultiplier * block.itemDrop.hardness) * drill.size * drill.size;
                            t.add("[stat]" + Strings.fixed(eff, 2)).padLeft(20f);
                            t.add("[cyan]" + Strings.fixed(eff * drill.liquidBoostIntensity * drill.liquidBoostIntensity, 2)).padLeft(20f).padRight(20f);
                            t.row();
                            oreList = new StringBuilder();
                        } else oreList.append("  ");
                    }
                });
            });
        };
        }
    }

    public static StatValue arcSeparator(Separator separator){
        return table -> {
            for(ItemStack stack : separator.results){
                table.add(StatValues.stack(stack)).padRight(5);
                table.add("  ");
            }
        };
    }


    public static StatValue blocks(Boolf<Block> pred){
        return content(content.blocks(), pred);
    }

    public static StatValue blocks(Seq<Block> list){
        return content(list.as());
    }

    public static StatValue statusEffects(Seq<StatusEffect> list){
        return content(list.as());
    }

    public static StatValue drillables(float drillTime, float drillMultiplier, float size, ObjectFloatMap<Item> multipliers, Boolf<Block> filter){
        return table -> {
            table.row();
            table.table(c -> {
                int i = 0;
                for(Block block : content.blocks()){
                    if(!filter.get(block)) continue;

                    c.table(Styles.grayPanel, b -> {
                        b.image(block.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit);
                        b.table(info -> {
                            info.left();
                            info.add(block.localizedName).left().row();
                            info.add(block.itemDrop.emoji()).with(l -> withTooltip(l, block.itemDrop)).left();
                        }).grow();
                        b.add(Strings.autoFixed(60f / ((drillTime + drillMultiplier * block.itemDrop.hardness) / (multipliers == null ? 1 : multipliers.get(block.itemDrop, 1f))) * size, 2) + StatUnit.perSecond.localized())
                        .right().pad(10f).padRight(15f).color(Color.lightGray);
                    }).growX().pad(5);
                    if(++i % 2 == 0) c.row();
                }
            }).growX().colspan(table.getColumns());
        };
    }

    public static StatValue boosters(float reload, float maxUsed, float multiplier, boolean baseReload, Boolf<Liquid> filter){
        return table -> {
            table.row();
            table.table(c -> {
                for(Liquid liquid : content.liquids()){
                    if(!filter.get(liquid)) continue;

                    c.table(Styles.grayPanel, b -> {
                        b.image(liquid.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit).with(i -> withTooltip(i, liquid, false));;
                        b.table(info -> {
                            info.add(liquid.localizedName).left().row();
                            info.add(Strings.autoFixed(maxUsed * 60f, 2) + StatUnit.perSecond.localized()).left().color(Color.lightGray);
                        });

                        b.table(bt -> {
                            bt.right().defaults().padRight(3).left();

                            float reloadRate = (baseReload ? 1f : 0f) + maxUsed * multiplier * liquid.heatCapacity;
                            float standardReload = baseReload ? reload : reload / (maxUsed * multiplier * 0.4f);
                            float result = standardReload / (reload / reloadRate);
                            bt.add(Core.bundle.format("bullet.reload", Strings.autoFixed(result * 100, 2))).pad(5);
                        }).right().grow().pad(10f).padRight(15f);
                    }).growX().pad(5).row();
                }
            }).growX().colspan(table.getColumns());
            table.row();
        };
    }

    public static StatValue itemEffMultiplier(Floatf<Item> efficiency, float timePeriod, Boolf<Item> filter){
        return itemEffMultiplier(efficiency, timePeriod, filter, null);
    }

    public static StatValue itemEffMultiplier(Floatf<Item> efficiency, float timePeriod, Boolf<Item> filter, @Nullable ObjectFloatMap<Item> itemDurationMultipliers){
        return table -> {
            if(table.getCells().size > 0) table.getCells().peek().growX(); //Expand the spacer on the row above to push everything to the left
            table.row();
            table.table(c -> {
                for(Item item : content.items().select(i -> filter.get(i) && i.unlockedNow() && !i.isHidden())){
                    float timeMultiplier = itemDurationMultipliers == null ? 1f : itemDurationMultipliers.get(item, 1f);
                    float time = 1f / (timePeriod * timeMultiplier / 60f);

                    c.table(Styles.grayPanel, b -> {
                        b.image(item.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit);
                        b.add(item.localizedName + (timePeriod > 0 ? "\n[lightgray]" + (time < 0.01f ? Strings.fixed(time, 4) : Strings.autoFixed(time, 2)) + StatUnit.perSecond.localized() : "")).left().grow();
                        b.add(Core.bundle.format("stat.efficiency", fixValue(efficiency.get(item) * 100f))).right().pad(10f).padRight(15f);
                    }).growX().pad(5).row();
                }
            }).growX().colspan(table.getColumns()).row();
        };
    }

    public static StatValue liquidEffMultiplier(Floatf<Liquid> efficiency, float amount, Boolf<Liquid> filter){
        return table -> {
            if(table.getCells().size > 0) table.getCells().peek().growX(); //Expand the spacer on the row above to push everything to the left
            table.row();
            table.table(c -> {
                for(Liquid liquid : content.liquids().select(l -> filter.get(l) && l.unlockedNow() && !l.isHidden())){
                    c.table(Styles.grayPanel, b -> {
                        b.add(displayLiquid(liquid, amount, true)).pad(10f).left().grow();
                        b.add(Core.bundle.format("stat.efficiency", fixValue(efficiency.get(liquid) * 100f))).right().pad(10f).padRight(15f);
                    }).growX().pad(5).row();
                }
            }).growX().colspan(table.getColumns()).row();
        };
    }

    public static StatValue speedBoosters(String unit, float amount, float speed, boolean strength, Boolf<Liquid> filter) {
        return speedBoosters(unit, amount, speed, strength, filter, false);
    }
    public static StatValue speedBoosters(String unit, float amount, float speed, boolean strength, Boolf<Liquid> filter, Boolean isForce){
        return table -> {
            table.row();
            table.table(c -> {
                for(Liquid liquid : content.liquids()){
                    if(!filter.get(liquid)) continue;

                    c.table(Styles.grayPanel, b -> {
                        b.image(liquid.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit).with(i -> withTooltip(i, liquid, false));;
                        b.table(info -> {
                            info.add(liquid.localizedName).left().row();
                            info.add(Strings.autoFixed(amount * 60f, 2) + StatUnit.perSecond.localized()).left().color(Color.lightGray);
                        });
                        b.table(bt -> {
                            bt.right().defaults().padRight(3).left();
                           if(isForce) bt.add(unit.replace("{0}", "[stat]" + Strings.autoFixed(speed * ((liquid.heatCapacity - 0.4f) * 0.9f + 1), 2) + "[lightgray]")).pad(5);
                           else if(speed != Float.MAX_VALUE) bt.add(unit.replace("{0}", "[stat]" + Strings.autoFixed(speed * (strength ? liquid.heatCapacity : 1f) + (strength ? 1f : 0f), 2) + "[lightgray]")).pad(5);
                        }).right().grow().pad(10f).padRight(15f);
                    }).growX().pad(5).row();
                }
            }).growX().colspan(table.getColumns());
            table.row();
        };
    }

    public static StatValue itemBoosters(String unit, float timePeriod, float speedBoost, float rangeBoost, ItemStack[] items){
        return table -> {
            table.row();
            table.table(c -> {
                c.table(Styles.grayPanel, b -> {
                    b.table(it -> {
                        for(ItemStack stack : items){
                            if(timePeriod < 0){
                                it.add(displayItem(stack.item, stack.amount, true)).pad(10f).padLeft(15f).left();
                            }else{
                                it.add(displayItem(stack.item, stack.amount, timePeriod, true)).pad(10f).padLeft(15f).left();
                            }
                            it.row();
                        }
    }).left();

                    b.table(bt -> {
                        bt.right().defaults().padRight(3).left();
                        if(rangeBoost != 0) bt.add("[lightgray]+[stat]" + Strings.autoFixed(rangeBoost / tilesize, 2) + "[lightgray] " + StatUnit.blocks.localized()).row();
                        if(speedBoost != 0) bt.add("[lightgray]" + unit.replace("{0}", "[stat]" + Strings.autoFixed(speedBoost, 2) + "[lightgray]"));
                    }).right().top().grow().pad(10f).padRight(15f);
                }).growX().pad(5).padBottom(-5).row();
            }).growX().colspan(table.getColumns());
            table.row();
        };
    }

    public static StatValue weapons(UnitType unit, Seq<Weapon> weapons){
        return table -> {
            table.row();
            for(int i = 0; i < weapons.size; i++){
                Weapon weapon = weapons.get(i);

                if(weapon.flipSprite || !weapon.hasStats(unit)){
                    //flipped weapons are not given stats
                    continue;
                }

                TextureRegion region = !weapon.name.isEmpty() ? Core.atlas.find(weapon.name + "-preview", weapon.region) : null;

                table.table(Styles.grayPanel, w -> {
                    w.left().top().defaults().padRight(3).left();
                    if(region != null && region.found() && weapon.showStatSprite) w.image(region).size(60).scaling(Scaling.bounded).left().top();
                    w.row();

                    weapon.addStats(unit, w);
                }).growX().pad(5).margin(10);
                table.row();
            }
        };
    }

    public static StatValue abilities(Seq<Ability> abilities){
        return table -> {
            table.row();
            table.table(t -> {
                int count = 0;
                for(Ability ability : abilities){
                    if(ability.display){
                        ability.display(t);

                        if((++count) == 2){
                            count = 0;
                            t.row();
                        }
                    }
                }
            });
        };
    }

    public static StatValue targets(UnitType unit, BlockFlag[] targetFlags){
        return table -> {
            table.row();
            table.table(t -> {
                t.background(Styles.grayPanel);
                for(BlockFlag flag : targetFlags){
                    if (flag == null) continue;
                    t.add(flag.name()).width(150f).padBottom(5f);
                    int count = 0;
                    for (Block block: content.blocks()){
                        if (block.flags.contains(flag)) {
                            if (count >= 3) {
                                t.add("\uE813").width(30f);
                                break;
                            }else t.add(block.emoji()).width(30f);
                            count += 1;
                        }
                    }
                    t.row();
                }
            }).padLeft(12f);
        };
    }

    public static StatValue abilities(UnitType unit, Seq<Ability> abilities){
        return table -> {
            table.row();
            table.table(t -> {
                t.background(Styles.grayPanel);
                for(Ability a : abilities){
                    if (!a.display) continue;
                    if (a.description(unit).length() > 0){
                        t.table(tt->{
                            tt.add(a.localized()).width(100f);
                            tt.add(a.description(unit)).minWidth(350f).padRight(12f).padBottom(5f);
                        });
                    }else{
                        t.add(a.localized()).minWidth(350f).padRight(12f).padBottom(5f);
                    }
                    t.row();
                }
            }).padLeft(12f);
        };
    }

    public static <T extends UnlockableContent> StatValue ammo(ObjectMap<T, BulletType> map){
        return ammo(map, false, false);
    }

    public static <T extends UnlockableContent> StatValue ammo(ObjectMap<T, BulletType> map, boolean showUnit){
        return ammo(map, false, showUnit);
    }

    public static <T extends UnlockableContent> StatValue ammo(ObjectMap<T, BulletType> map, boolean nested, boolean showUnit){
        return table -> {

            table.row();

            var orderedKeys = map.keys().toSeq();
            orderedKeys.sort();

            for(T t : orderedKeys){
                boolean compact = t instanceof UnitType && !showUnit || nested;

                BulletType type = map.get(t);

                if(type.spawnUnit != null && type.spawnUnit.weapons.size > 0){
                    ammo(ObjectMap.of(t, type.spawnUnit.weapons.first().bullet), nested, false).display(table);
                    continue;
                }

                table.table(Styles.grayPanel, bt -> {
                    bt.left().top().defaults().padRight(3).left();
                    //no point in displaying unit icon twice
                    if(!compact && !(t instanceof Turret)){
                        bt.table(title -> {
                            title.image(icon(t)).size(3 * 8).padRight(4).right().scaling(Scaling.fit).top().with(i -> withTooltip(i, t, false));

                            title.add(t.localizedName).padRight(10).left().top();

                            if(type.displayAmmoMultiplier && type.statLiquidConsumed > 0f){
                                title.add("[stat]" + fixValue(type.statLiquidConsumed / type.ammoMultiplier * 60f) + " [lightgray]" + StatUnit.perSecond.localized());
                            }
                        });
                        bt.row();
                    }

                    if (type instanceof LightningBulletType lb) {
                        lightning(0, lb.damage, lb.lightningLength, lb.lightningLengthRand).display(bt);
                    }
                    else if(type.damage > 0 && (type.collides || type.splashDamage <= 0)){
                        if(type.continuousDamage() > 0){
                            bt.add(Core.bundle.format("bullet.damage", type.continuousDamage()) + StatUnit.perSecond.localized());
                        }else{
                            bt.add(Core.bundle.format("bullet.damage", type.damage));
                        }
                    }

                    if(type.buildingDamageMultiplier != 1){
                        sep(bt, colorize(type.buildingDamageMultiplier) + "[lightgray]x建筑伤害");
                    }

                    if(type.rangeChange != 0 && !compact){
                        sep(bt, "[lightgray]射程 + " + colorize(type.rangeChange / tilesize > 0) + Strings.autoFixed(type.rangeChange / tilesize,1) + " [lightgray]格");
                    }

                    if(type.shieldDamageMultiplier != 1){
                        sep(bt, Core.bundle.format("bullet.shielddamage", ammoStat((int)(type.shieldDamageMultiplier * 100 - 100))));
                    }

                    if(type.splashDamage > 0){
                        sep(bt, Core.bundle.format("bullet.splashdamage", (int)type.splashDamage, Strings.fixed(type.splashDamageRadius / tilesize, 1)));
                    }

                    if(type.statLiquidConsumed <= 0f && !compact && !Mathf.equal(type.ammoMultiplier, 1f) && type.displayAmmoMultiplier && (!(t instanceof Turret turret) || turret.displayAmmoMultiplier)){
                        sep(bt, Core.bundle.format("bullet.multiplier", (int)type.ammoMultiplier));
                    }

                    if(!compact && !Mathf.equal(type.reloadMultiplier, 1f)){
                        sep(bt,  colorize(type.reloadMultiplier) + "[lightgray]x射速");
                    }

                    if(type.knockback > 0){
                        sep(bt, Core.bundle.format("bullet.knockback", Strings.autoFixed(type.knockback, 2)));
                    }

                    if(type.healPercent > 0f){
                        sep(bt, Core.bundle.format("bullet.healpercent", Strings.autoFixed(type.healPercent, 2)));
                    }

                    if(type.healAmount > 0f){
                        sep(bt, Core.bundle.format("bullet.healamount", Strings.autoFixed(type.healAmount, 2)));
                    }

                    if((type.pierce || type.pierceCap != -1) && !(type instanceof PointLaserBulletType)){
                        boolean laserPierce = type instanceof LaserBulletType || type instanceof ContinuousLaserBulletType || type instanceof ShrapnelBulletType;
                        boolean pierceBuilding = laserPierce || type instanceof ContinuousFlameBulletType || type instanceof RailBulletType || type.pierceBuilding;
                        boolean pierceUnit = type.pierce;
                        StringBuilder str = new StringBuilder("[stat]");
                        if(type instanceof RailBulletType rail){
                            str.append(Strings.autoFixed(rail.pierceDamageFactor * 100f, 1) + "%衰减");
                        }else{
                            str.append(type.pierceCap == -1? "无限" : type.pierceCap + "x");
                        }
                        str.append("穿透[lightgray]");
                        if (pierceBuilding && pierceUnit) {
                            str.append("建筑与单位");
                        }
                        else {
                            str.append(pierceBuilding ? "建筑" : "单位");
                        }
                        if(laserPierce) str.append("[stat](电性)");
                        sep(bt, str.toString());
                    }

                    if(type.incendAmount > 0){
                        sep(bt, "@bullet.incendiary");
                    }

                    if(type.homingPower > 0.01f){
                        sep(bt, "[stat]追踪[lightgray]~[]"+Strings.autoFixed(type.homingPower * 50 * Time.toSeconds, 1)+"°/s[lightgray]~[]"+Strings.fixed(type.homingRange / tilesize,1)+"[lightgray]格");
                    }

                    if(!(type instanceof LightningBulletType) && type.lightning > 0){
                        lightning(type.lightning, type.lightningDamage < 0 ? type.damage : type.lightningDamage, type.lightningLength, type.lightningLengthRand).display(bt);
                    }

                    if(type.pierceArmor){
                        sep(bt, "@bullet.armorpierce");
                    }

                    if(type.maxDamageFraction > 0){
                        sep(bt, Core.bundle.format("bullet.maxdamagefraction", (int)(type.maxDamageFraction * 100)));
                    }

                    if(type.suppressionRange > 0){
                        sep(bt, Core.bundle.format("bullet.suppression", Strings.autoFixed(type.suppressionDuration / 60f, 2), Strings.fixed(type.suppressionRange / tilesize, 1)));
                    }

                    if(type instanceof EmpBulletType eb) {
                        collapser(bt, Strings.format("[stat]EMP~@[lightgray]格[]~[white]\uE810[]@%/[white]\uE86D[]@%~[white]\uF899[][green]@%[]/[negstat]@%[]",
                                Strings.autoFixed(eb.radius / tilesize, 0),
                                Strings.autoFixed(eb.powerDamageScl * 100, 0),
                                Strings.autoFixed(eb.unitDamageScl * 100, 0),
                                Strings.autoFixed(eb.timeIncrease * 100, 0),
                                Strings.autoFixed(eb.powerSclDecrease * 100, 0)
                        ), ec -> {
                            ec.defaults().padLeft(5f);
                            sep(ec,Strings.format("[stat]对敌方电网建筑造成@%子弹伤害", Strings.autoFixed(eb.powerDamageScl * 100, 0)));
                            sep(ec,Strings.format("[stat]对敌方单位造成@%子弹伤害", Strings.autoFixed(eb.unitDamageScl * 100, 0)));
                            sep(ec,Strings.format("[stat]对我方耗电建筑超速至@%", Strings.autoFixed(eb.timeIncrease * 100, 0)));
                            sep(ec,Strings.format("[stat]对敌方电网建筑减速至@%", Strings.autoFixed(eb.powerSclDecrease * 100, 0)));
                        });
                    }

                    if(type.status != StatusEffects.none){
                        sep(bt, (type.status.hasEmoji() ? type.status.emoji() : "") + "[stat]" + type.status.localizedName + (type.status.reactive ? "" : "[lightgray] ~ [stat]" +
                            Strings.autoFixed(type.statusDuration / 60f, 1) + "[lightgray] " + Core.bundle.get("unit.seconds"))).with(c -> withTooltip(c, type.status));
                    }

                    if(!type.targetMissiles){
                        sep(bt, "@bullet.notargetsmissiles");
                    }

                    if(!type.targetBlocks){
                        sep(bt, "@bullet.notargetsbuildings");
                    }

                    if(type.fragBullet != null){
                        bt.row();

                        Table fc = new Table();
                        ammo(ObjectMap.of(t, type.fragBullet), true, false).display(fc);
                        Collapser coll = new Collapser(fc, true);
                        coll.setDuration(0.1f);
                    }

                    if(type.intervalBullet != null){
                        collapser(bt, Core.bundle.format("bullet.interval", Strings.autoFixed(type.intervalBullets / type.bulletInterval * 60, 2)), ic -> {
                            ammo(ObjectMap.of(t, type.intervalBullet), nested, false).display(ic);
                        });
                    }

                    Seq<BulletType> spawn = type.spawnBullets.copy();
                    if (spawn.any()) {
                        collapser(bt, Strings.format("[stat]@x[lightgray]生成子弹：", spawn.size), sc -> {
                            while (spawn.any()) {
                                BulletType bullet = spawn.first();
                                Boolf<BulletType> pred = b -> bullet.damage == b.damage && bullet.splashDamage == b.splashDamage;
                                //通过pred的的子弹被认为和当前子弹是一样的，合并显示
                                int count = spawn.count(pred);
                                if (count == type.spawnBullets.size) {
                                    ammo(ObjectMap.of(t, bullet), nested, false).display(sc);
                                } else {
                                    sep(sc, Strings.format(" [stat]@x[lightgray]子弹：", count)).padLeft(0f);//不知道为什么padLeft0刚刚好，就这样了
                                    ammo(ObjectMap.of(t, bullet), nested, false).display(sc);
                                }
                                bt.row();
                                spawn.removeAll(pred);
                            }
                        });
                    }
                }).padLeft(5).padTop(5).padBottom(compact ? 0 : 5).growX().margin(compact ? 0 : 10);
                table.row();
            }
        };
    }

    public static StatValue lightning(int shots, float damage, int length, int lengthRand) {
        return table -> {
            String str = "[lightgray]";
            if (shots > 0) {
                str += String.format("[stat]%d[]x", shots);
            }
            str += String.format("闪电~[stat]%s[]伤害~", Strings.autoFixed(damage, 1));
            if (lengthRand > 0) {
                str += String.format("[stat]%d~%d[]长度", length, length + lengthRand);
            }
            else {
                str += String.format("[stat]%d[]长度", length);
            }
            sep(table, str);
        };
    }

    public static StatValue turretReload(Turret turret) {
        return table -> table.add((turret.shoot.totalShots() == 1f ? "" : turret.shoot.totalShots() + " x ") + Strings.autoFixed(60f / turret.reload, 2) + "/s");
    }

    //for AmmoListValue
    private static Cell<Label> sep(Table table, String text){
        table.row();
        return table.add(text);
    }
    private static void collapser(Table table, String text, Cons<Table> cons){
        table.row();

        Table collt = new Table();
        collt.left().defaults().left();
        cons.get(collt);

        Collapser coll = new Collapser(collt, true);
        coll.setDuration(0.1f);

        table.table(tt -> {
            tt.add(text);
            tt.button(Icon.downOpen, Styles.emptyi, () -> coll.toggle(false))
                    .update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen))
                    .size(8).padLeft(16f).expandX();
        });
        table.row();
        table.add(coll);
    }

    //for AmmoListValue
    private static String ammoStat(float val){
        return (val > 0 ? "[stat]+" : "[negstat]") + Strings.autoFixed(val, 1);
    }

    private static String colorize(float val){
        return (val > 1 ? "[stat]" : "[negstat]") + Strings.autoFixed(val, 2);
    }

    private static String colorize(boolean val){
        return val ? "[stat]" : "[negstat]";
    }

    private static String multStat(float val){
        return (val >= 1 ? "[stat]" : "[negstat]") + Strings.autoFixed(val, 2);
    }

    private static TextureRegion icon(UnlockableContent t){
        return t.uiIcon;
    }
}
