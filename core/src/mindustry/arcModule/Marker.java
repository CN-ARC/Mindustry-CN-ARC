package mindustry.arcModule;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.DesktopInput;

import static mindustry.Vars.*;

public class Marker{
    /** 冷却时间*/
    public static final float heatTime = 60f;
    /** 滞留时间*/
    public static final float retainTime = 1800f;

    public static final String preFixed = "<ARC";
    public static final String versionFixed = preFixed + Vars.arcVersion + ">";

    public static MarkType mark, gatherMark, attackMark, defenseMark, quesMark;

    public static Seq<MarkType> markTypes = Seq.with(
            mark = new MarkType("Mark", Fx.arcMarker, Color.valueOf("eab678")),
            gatherMark = new MarkType("Gather", Fx.arcGatherMarker, Color.cyan),
            attackMark = new MarkType("Attack", Fx.arcAttackMarker, Color.red),
            defenseMark = new MarkType("Defend", Fx.arcDefenseMarker, Color.acid),
            quesMark = new MarkType("What", Fx.arcQuesMarker, Color.pink)
    );

    public static boolean isLocal;
    public static boolean teamMark = false;

    public static final Seq<MarkElement> markList = new Seq<>();

    static{
        Events.run(WorldLoadEvent.class, () -> {
            markList.clear();
            //teamMark = state.rules.pvp;
        });
    }

    public static void mark(MarkType type, float x, float y){
        mark(type, Tmp.v1.set(x, y), true);
    }

    public static void mark(MarkType type, float x, float y, boolean sendMessage){
        mark(type, Tmp.v1.set(x, y), sendMessage);
    }

    public static void mark(MarkType type, Vec2 pos){
        mark(type, pos, true);
    }

    public static void mark(MarkType type, Vec2 pos, boolean sendMessage){
        if(markList.size>0 && (Time.time - markList.peek().time)<heatTime){
            Vars.ui.announce("请不要频繁标记!");
            return;
        }

        markList.add(new MarkElement(type,pos));

        type.showEffect(pos);

        if(sendMessage){
            isLocal = true;
            type.sendMessage(pos);
        }
    }

    public static boolean resolveMessage(String text){
        if(isLocal){
            isLocal = false;
            return true;
        }

            MarkType markType = null;
            int Indexer = -1;

            for(MarkType markType1 : markTypes){
                if (text.contains("<" + markType1.name + ">") || text.contains("<" + markType1.localizedName + ">")) {
                    markType = markType1;
                    Indexer = text.indexOf("<" + markType1.name + ">");
                }
            }

            if(Indexer>10 && markType!=null){
                /* Parse position */
                String posStr = text.substring(text.indexOf('(', Indexer + 1));

                Vec2 pos = Tmp.v1;

                try{
                    pos.fromString(posStr);
                }catch(Throwable e){
                    Log.err("Cannot resolve position from " + posStr);
                    return false;
                }

                mark(markType, pos.scl(tilesize), false);
                ui.MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc,text,pos));
                return true;
            }

        if(text.contains("[YELLOW][集合]")&& text.contains("[WHITE]\"[WHITE]\",输入\"[gold]go[WHITE]\"前往")){

            int typeStart = text.indexOf("[WHITE]发起集合([RED]");
            int typeEnd = text.indexOf("[WHITE])[WHITE]");
            if(typeStart == -1 || typeEnd == -1){
                return false;
            }

            /* Parse position */
            String posStr = text.substring(typeStart + 17 , typeEnd);

            Vec2 pos = Tmp.v1;

            try{
                pos.fromString("(" + posStr + ")");
            }catch(Throwable e){
                Log.err("Cannot resolve position from " + posStr);
                return false;
            }

            mark(findLocalizedName("集合"), pos.scl(tilesize), false);
            ui.MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc,text,pos));
            return true;
        }
        return false;

    }

    public static MarkType findType(String name){
        return markTypes.find(maskType -> maskType.name.equals(name));
    }

    public static MarkType findLocalizedName(String localizedName){
        return markTypes.find(maskType -> maskType.localizedName.equals(localizedName));
    }

    public static void lockonLastMark(){
        if(markList.size == 0) return;

        if(control.input instanceof DesktopInput input){
            input.panning = true;
        }

        Core.camera.position.set(Marker.markList.peek().markPos);

        markList.peek().showEffect();
    }

    public static class MarkType{
        private final String name;

        public String localizedName;
        public String describe;

        private final Effect effect;
        public final Color color;

        public MarkType(String name, Effect effect){
            this(name, effect, Color.white);
        }

        public MarkType(String name, Effect effect, Color color){
            this.name = name;
            this.effect = effect;
            this.color = color;

            localizedName = Core.bundle.get("marker." + name + ".name", "unknown");
            describe = Core.bundle.get("marker." + name + ".name", "unknown");
        }

        public String shortName(){
            return "[#" + color + "]" + localizedName;
        }

        public String tinyName(){
            return "[#" + color + "]" + localizedName.substring(0,1);
        }

        public void showEffect(Vec2 pos){
            effect.arcCreate(pos.x, pos.y, 0, color, null);
        }

        public void sendMessage(Vec2 pos){
            String text = (teamMark ? "/t ":"") + versionFixed +
                    "[#" + color + "]" + "<" + name + ">" +
                    "[white]" + ": " +
                    "(" + World.toTile(pos.x) + "," + World.toTile(pos.y)+")";
            Call.sendChatMessage(text);
            ui.MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc,text,pos));
        }

    }

    public static class MarkElement{
        public MarkType markType;
        public float time;
        public String player;
        public Vec2 markPos;

        public MarkElement(MarkType markType,Vec2 markPos){
            this(markType,"",markPos);
        }

        public MarkElement(MarkType markType,String player,Vec2 markPos){
            this(markType,Time.time,player,markPos);
        }

        public MarkElement(MarkType markType,float time,String player,Vec2 markPos){
            this.markType = markType;
            this.time = time;
            this.player = player;
            this.markPos = new Vec2().set(markPos);
        }

        public void showEffect(){
            markType.showEffect(markPos);
        }

    }
}
