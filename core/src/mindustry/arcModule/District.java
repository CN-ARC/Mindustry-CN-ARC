package mindustry.arcModule;

import arc.*;
import arc.graphics.Color;
import arc.math.geom.*;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Tmp;
import mindustry.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.core.World;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Date;

import static mindustry.Vars.*;
import static mindustry.arcModule.Marker.teamMark;

public class District{

    public static final String ShareType = "[violet]<District>";

    /** 冷却时间*/
    public static final float heatTime = 60f;
    /** 滞留时间*/
    public static final float retainTime = 1800f;

    public static final String preFixed = "<ARC";
    public static final String versionFixed = preFixed + Vars.arcVersion + ">";

    public static final Seq<District.advDistrict> districtList = new Seq<>();

    private static advDistrict voidDistrict = new advDistrict();   //仅用于赋值，不实际处理

    static{
        Events.run(EventType.WorldLoadEvent.class, () -> {
            districtList.clear();
            //teamMark = state.rules.pvp;
        });
    }

    public static void unitSpawnMenu(){
        BaseDialog disSet = new BaseDialog("ARC-区域规划中心");

        disSet.cont.table(t->{
            t.add("坐标设置").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.table(tt->{
                tt.add("A点： ");
                tt.table(tx -> {
                    tx.add("x= ");
                    TextField Ax = tx.field(voidDistrict.districtA.x + "", text -> {
                        voidDistrict.districtA.x = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.add("  ,y= ");
                    TextField Ay = tx.field(voidDistrict.districtA.y + "", text -> {
                        voidDistrict.districtA.y = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.button(StatusEffects.blasted.emoji(), () -> {
                        if(Marker.markList.size == 0) return;
                        voidDistrict.districtA.set(World.toTile(Marker.markList.peek().markPos.x),World.toTile(Marker.markList.peek().markPos.y));
                        Ax.setText(voidDistrict.districtA.x + "");
                        Ay.setText(voidDistrict.districtA.y + "");
                    }).tooltip(Marker.markList.size==0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," +  World.toTile(Marker.markList.peek().markPos.y))).height(50f);
                });
                tt.row();
                tt.add("B点： ");
                tt.table(tx -> {
                    tx.add("x= ");
                    TextField Bx = tx.field(voidDistrict.districtB.x + "", text -> {
                        voidDistrict.districtB.x = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.add("  ,y= ");
                    TextField By = tx.field(voidDistrict.districtB.y + "", text -> {
                        voidDistrict.districtB.y = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.button(StatusEffects.blasted.emoji(), () -> {
                        if(Marker.markList.size == 0) return;
                        voidDistrict.districtB.set(World.toTile(Marker.markList.peek().markPos.x),World.toTile(Marker.markList.peek().markPos.y));
                        Bx.setText(voidDistrict.districtB.x + "");
                        By.setText(voidDistrict.districtB.y + "");
                    }).tooltip(Marker.markList.size==0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," +  World.toTile(Marker.markList.peek().markPos.y))).height(50f);
                });

            });
            t.row();
            t.add("时间设置").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.table(tt->{
                TextField sField = tt.field("" + 30, text -> {
                    voidDistrict.duration = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).tooltip("规划区持续时间(单位：秒)").maxTextLength(10).get();

                tt.add("秒");
                Slider sSlider = tt.slider(30f, 300f, 30f, voidDistrict.duration, n -> {
                    if(voidDistrict.duration != n){//用一种神奇的方式阻止了反复更新
                        sField.setText(voidDistrict.duration + "");
                    }
                    voidDistrict.duration = n;
                }).get();
                sField.update(() -> sSlider.setValue(voidDistrict.duration));
            });

            t.row();
            t.add("规划区显示").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.table(tt->{
                tt.add("图标：");
                tt.button(UnitTypes.gamma.emoji(),()->showNewIconTag(voidDistrict));
                tt.add("标签：");
                tt.field(voidDistrict.districtType.districtName, text -> {
                    voidDistrict.districtType.districtName = text;
                }).tooltip("规划区的标签条").maxTextLength(10).width(300f);
            });
            t.row();
            t.add("规划区设置").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.button("发布规划区!",()->{
                Call.sendChatMessage(voidDistrict.toString());
                //ui.MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.district,voidDistrict.toString(),voidDistrict.center()));
            }).fillX();
        });

        disSet.addCloseButton();
        disSet.show();
    }

    public static boolean resolveMessage(String text){

        advDistrict resolveDistrict = new advDistrict();
        int Indexer = -1;

        if(!text.contains(ShareType)){
            return false;
        }
        Indexer = text.indexOf(ShareType) + ShareType.length();

        resolveDistrict.message = text;

        int districtTypeHand = text.indexOf('<', Indexer + 1);
        int districtTypeTail = text.indexOf('>', Indexer + 2);
        if(districtTypeHand == -1 || districtTypeTail<districtTypeHand) return false;
        resolveDistrict.districtType.districtName = text.substring(districtTypeHand,districtTypeTail);

        Indexer = districtTypeTail;
        int locAHand = text.indexOf('(', Indexer + 1);
        int locATail = text.indexOf(')', Indexer + 2);

        Vec2 pos = Tmp.v1;
        try{
            pos.fromString(text.substring(locAHand,locATail+1));
        }catch(Throwable e){
            Call.sendChatMessage(text.substring(locAHand,locATail+1));
            Log.err("Cannot resolve position");
            return false;
        }
        resolveDistrict.districtA = new Vec2(pos);

        Indexer = locATail;
        int locBHand = text.indexOf('(', Indexer + 1);
        int locBTail = text.indexOf(')', Indexer + 2);

        pos = Tmp.v1;
        try{
            pos.fromString(text.substring(locBHand,locBTail+1));
        }catch(Throwable e){
            Log.err("Cannot resolve position" + text.substring(locBHand,locBTail));
            return false;
        }
        resolveDistrict.districtB = new Vec2(pos);

        districtList.add(resolveDistrict);
        ui.MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.district,text,resolveDistrict.center()));
        return true;
    }

    static void showNewIconTag(advDistrict district){
        new Dialog("arc-区域规划中心"){{
            closeOnBack();
            setFillParent(true);

            cont.pane(t -> {
                resized(true, () -> {
                    t.clearChildren();
                    t.marginRight(19f);
                    t.defaults().size(48f);

                    int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                    for(ContentType ctype : defaultContentIcons){
                        t.row();
                        t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                        t.row();

                        int i = 0;
                        for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                            if(!u.isHidden() && u.unlockedNow() && u.hasEmoji()){
                                t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                    district.districtType.districtType = u;

                                    hide();
                                });

                                if(++i % cols == 0) t.row();
                            }
                        }
                    }
                });
            });
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }}.show();
    }

    public static class advDistrict{
        public District.DistictType districtType = new DistictType("");
        public String message;
        public Date time;
        public Float duration = 30f;
        public String creator;
        public boolean selected;
        public Vec2 districtA = new Vec2();
        public Vec2 districtB = new Vec2();

        public advDistrict(District.DistictType districtType, String message, Date time, String creator, Vec2 districtA, Vec2 districtB){
            this.districtType = districtType;
            this.message = message;
            this.time = time;
            this.creator = creator;
            this.districtA = new Vec2().set(districtA);
            this.districtB = new Vec2().set(districtB);
        }

        public advDistrict(){}

        public advDistrict(District.DistictType districtType, String message, String creator, Vec2 districtA, Vec2 districtB){
            this(districtType, message, new Date(), creator, districtA, districtB);
        }

        public advDistrict(District.DistictType districtType, String message, Vec2 districtA, Vec2 districtB){
            this(districtType, message, null, districtA, districtB);
        }

        public advDistrict(advDistrict voidDistrict){
            this.districtType = voidDistrict.districtType;
            this.message = voidDistrict.message;
            this.duration = voidDistrict.duration;
            this.time = new Date();
            this.creator = voidDistrict.creator;
            this.districtA = new Vec2().set(voidDistrict.districtA);
            this.districtB = new Vec2().set(voidDistrict.districtB);
        }

        public String toString(){
            return  (teamMark ? "/t ":"") + versionFixed +
                    ShareType + "[white]<" + districtType.getName() + ">" +
                    "[white]：" +
                    "(" + (int)districtA.x + "," + (int)districtA.y+")~" +
                    "(" + (int)districtB.x + "," + (int)districtB.y+")"
                    ;
        }

        public Vec2 center(){
            return new Vec2((districtA.x + districtB.x)/2f,(districtA.y + districtB.y)/2f);
        }

    }

    public static class DistictType {
        public UnlockableContent districtType = null;
        public String districtName = null;
        public Color BorderColor = Color.gold;

        DistictType(UnlockableContent districtType, String districtName) {
            this.districtType = districtType;
            this.districtName = districtName;
        }

        DistictType(UnlockableContent districtType) {
            this.districtType = districtType;
        }

        DistictType(String districtName) {
            this.districtName = districtName;
        }

        public String getName() {
            if (districtName == null) return districtType.emoji();
            else if (districtType == null) return districtName;
            else return districtType.emoji() + " " + districtName;
        }
    }

}
