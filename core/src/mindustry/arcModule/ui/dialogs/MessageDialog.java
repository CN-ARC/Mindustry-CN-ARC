package mindustry.arcModule.ui.dialogs;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.event.HandCursorListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.arcModule.*;
import mindustry.arcModule.ai.ArcBuilderAI;
import mindustry.content.Fx;
import mindustry.core.World;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.input.DesktopInput;

import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.storage.CoreBlock;

import java.text.SimpleDateFormat;
import java.util.Date;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.nodeArea;

public class MessageDialog extends BaseDialog{
    /** 选择的第一个|最后一个记录 */
    private int msgInit, msgFinal;
    private int maxMsgRecorded = Math.max(Core.settings.getInt("maxMsgRecorded"),20);
    /** 存储的所有事件记录 */
    public static Seq<advanceMsg> msgList = new Seq<>();

    private Table historyTable;
    private Boolean fieldMode = false;

    public MessageDialog(){
        super("ARC-中央监控室");

        cont.pane(t -> {
            historyTable = t;
        }).growX().scrollX(false);

        addCloseButton();
        buttons.button("设置", Icon.settings, this::arcMsgSettingTable);

        buttons.button("清空", Icon.trash, () -> {
            clearMsg();
            build();
        });
        buttons.button("导出", Icon.upload, this::exportMsg).name("导出聊天记录");

        buttons.button("刷新", Icon.refresh, this::build);

        init();
        shown(this::build);
        onResize(this::build);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            addMsg(new MessageDialog.advanceMsg(arcMsgType.eventWorldLoad, "载入地图： " + state.map.name()));
            addMsg(new MessageDialog.advanceMsg(arcMsgType.eventWorldLoad, "简介： " + state.map.description()));
            limitMsg(maxMsgRecorded);
        });

        Events.on(EventType.WaveEvent.class, e -> {
            if(state.wavetime < 60f) return;
            addMsg(new MessageDialog.advanceMsg(arcMsgType.eventWave, "波次： " + state.wave + " | " + RFuncs.arcWaveInfo(state.wave - 1)));
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBlock.CoreBuild)
                addMsg(new MessageDialog.advanceMsg(arcMsgType.eventCoreDestory, "核心摧毁： " + "(" + (int)e.tile.x + "," + (int)e.tile.y + ")",new Vec2(e.tile.x * 8,e.tile.y * 8)));
        });
    }

    void init(){

    }

    void build(){
        historyTable.clear();
        historyTable.setWidth(800f);
        if(msgList.size == 0) return;
        for(int i = msgList.size - 1; i >= 0; i--){
            int finalI = i;
            if(!msgList.get(finalI).msgType.show) continue;
            historyTable.table(Tex.whiteui, t -> {
                advanceMsg thisMsg = msgList.get(finalI);
                t.background(Tex.whitePane);
                t.setColor(thisMsg.msgType.color);
                t.marginTop(5);

                t.table(Tex.whiteui, tt -> {
                    tt.color.set(thisMsg.msgType.color);

                    if(msgList.get(finalI).msgType == arcMsgType.chat)
                        tt.add("" + getPlayerName(thisMsg)).style(Styles.outlineLabel).left().width(300f);
                    else
                        tt.add(thisMsg.msgType.name).style(Styles.outlineLabel).color(thisMsg.msgType.color).left().width(300f);

                    tt.add(formatTime(thisMsg.time)).style(Styles.outlineLabel).color(thisMsg.msgType.color).left().padLeft(20f).width(100f);

                    if(thisMsg.msgLoc.x != -1){
                        tt.button("♐： " + (int)(thisMsg.msgLoc.x/tilesize) + "," + (int)(thisMsg.msgLoc.y/tilesize), Styles.logicButton, () -> {
                            if (Vars.control.input instanceof DesktopInput input) {
                                input.panning = true;
                            }

                            Core.camera.position.set(thisMsg.msgLoc);

                            Fx.arcMarker.arcCreate(thisMsg.msgLoc.x, thisMsg.msgLoc.y, 0, color, null);
                            hide();
                        }).padLeft(50f).height(24f).width(150f);
                    }

                    tt.add().growX();
                    tt.add("    " + (msgList.size - finalI) + "").style(Styles.outlineLabel).color(thisMsg.msgType.color).padRight(10);

                    tt.button(Icon.copy, Styles.logici, () -> {
                        Core.app.setClipboardText(thisMsg.message);
                        ui.announce("已导出本条聊天记录");
                    }).size(24f).padRight(6);
                    tt.button(Icon.cancel, Styles.logici, () -> {
                        msgList.remove(finalI);
                        build();
                    }).size(24f);

                }).growX().height(30);

                t.row();

                t.table(tt -> {
                    tt.left();
                    tt.marginLeft(4);
                    tt.setColor(thisMsg.msgType.color);
                    if(fieldMode) tt.field(thisMsg.message, nodeArea, text -> {
                    }).growX();
                    else tt.labelWrap(getPlayerMsg(thisMsg)).growX();
                }).pad(4).padTop(2).growX().grow();

                t.marginBottom(7);
            }).growX().maxWidth(1000f).padBottom(15f).row();
        }
    }

    private String getPlayerName(advanceMsg msgElement){
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd<=typeStart){
            return msgElement.msgType.name;
        }

        return msgElement.message.substring(typeStart + 20,typeEnd);
    }

    private String getPlayerMsg(advanceMsg msgElement){
        if(msgElement.msgType != arcMsgType.normal) return msgElement.message;
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd<=typeStart){
            return msgElement.message;
        }
        return msgElement.message.substring(typeEnd+9);
    }

    private void arcMsgSettingTable(){
        BaseDialog setDialog = new BaseDialog("中央监控室-设置");
        if(Core.settings.getInt("maxMsgRecorded") == 0) Core.settings.put("maxMsgRecorded",500);

        setDialog.cont.table(t->{

            t.check("信息编辑模式",fieldMode,a->{fieldMode = a; build();}).left().width(200f);
            t.row();

            t.add("调整显示的信息").height(50f);
            t.row();
            for (arcMsgType type : arcMsgType.values()){

                CheckBox box = new CheckBox("[#" +type.color.toString() + "]" + type.name);

                box.update(() -> box.setChecked(type.show));
                box.changed(() -> {
                    type.show = !type.show;
                    build();
                });

                box.left();
                t.add(box).left().padTop(3f);

                t.row();
            }
        });

        setDialog.cont.row();

        setDialog.cont.table(t->{
            t.add("最大储存聊天记录(过高可能导致卡顿)：");
            t.field(maxMsgRecorded + "", text -> {
                int record = Math.min(Math.max(Integer.parseInt(text),1),9999);
                maxMsgRecorded = record;
                Core.settings.put("maxMsgRecorded",record);
            }).valid(Strings::canParsePositiveInt).width(200f).get();
            t.row();
            t.add("超出限制的聊天记录将在载入地图时清除");
        });

        setDialog.addCloseButton();

        setDialog.show();
    }

    public String formatTime(Date time){
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("HH:mm:ss");// a为am/pm的标记
        return sdf.format(time);
    }

    public boolean resolveMsg(String message){

        if(Marker.resolveMessage(message)) return true;
        if(District.resolveMessage(message)) return true;
        if(resolveMarkMsg(message)) return true;
        if(resolveServerMsg(message)) return true;

        addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.normal, message));

        return false;
    }

    public boolean resolveMsg(String message,@Nullable Player playersender){
        if(Marker.resolveMessage(message)) return true;
        if(District.resolveMessage(message)) return true;
        if(resolveMarkMsg(message)) return true;

        if(playersender != null){
            addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.chat,message,playersender.name(),new Vec2(playersender.x,playersender.y)));
            return true;
        }
        if(resolveServerMsg(message)) return true;
        addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.normal, message));

        return false;
    }

    public boolean resolveMarkMsg(String message){
        //除了markType以外的内容
        if(message.contains("<ARC") && message.contains("标记了")){
            addMsg(new MessageDialog.advanceMsg(arcMsgType.markInfo, message));
            return true;
        }
        return false;
    }

    public boolean resolveServerMsg(String message){
        Seq<String> serverMsg = Seq.with("加入了服务器", "离开了服务器", "自动存档完成", "登录成功", "经验+", "[YELLOW]本局游戏时长:", "[YELLOW]单人快速投票", "[GREEN]回档成功",
        "[YELLOW]PVP保护时间, 全力进攻吧", "[YELLOW]发起", "[YELLOW]你可以在投票结束前使用", "[GREEN]投票成功", "[GREEN]换图成功,当前地图",
        "[RED]本地图禁用单位","[RED]该地图限制空军,禁止进入敌方领空","[yellow]本地图限制空军","[YELLOW]火焰过多造成服务器卡顿,自动关闭火焰",
        " [GREEN]====","[RED]无效指令","[RED]该技能"
        );
        for(int i = 0; i < serverMsg.size; i++){
            if(message.contains(serverMsg.get(i))){
                addMsg(new MessageDialog.advanceMsg(arcMsgType.serverMsg, message));
                return true;
            }
        }

        if(message.contains("小贴士")){
            addMsg(new MessageDialog.advanceMsg(arcMsgType.serverTips, message));
            return true;
        }
        if(message.contains("[acid][公屏][white]")){
            addMsg(new MessageDialog.advanceMsg(arcMsgType.serverToast, message));
            return true;
        }
        if(message.contains("[YELLOW][技能]")){
            addMsg(new MessageDialog.advanceMsg(arcMsgType.serverSkill, message));
            return true;
        }

        return false;
    }

    public void addMsg(advanceMsg msg){
        msgList.add(msg);
    }

    private void clearMsg(){
        msgList.clear();
        msgInit = 0;
        msgFinal = 0;
    }

    private void limitMsg(int maxMsg){
        /** 限制信息数量 */
        while (true) {
            if (msgList.size<maxMsg) return;
            msgList.remove(0);
        }
    }

    void exportMsg(){
        StringBuilder messageHis = new StringBuilder();
        messageHis.append("下面是[ARC").append(arcVersion).append("] 导出的游戏内聊天记录").append("\n");
        messageHis.append("*** 当前地图名称: ").append(state.map.name()).append("（模式：").append(state.rules.modeName).append("）\n");
        messageHis.append("*** 当前波次: ").append(state.wave).append("\n");

        StringBuilder messageLs = new StringBuilder();
        int messageCount = 0;
        for(int i = 0; i < msgList.size; i++){
            String msg = msgList.get(i).message;
            messageLs.insert(0, msg + "\n");
            messageCount += 1;
        }

        messageHis.append("成功选取共 ").append(messageCount).append(" 条记录，如下：\n");
        messageHis.append(messageLs);
        Core.app.setClipboardText(Strings.stripGlyphs(Strings.stripColors(messageHis.toString())));
    }

    public static class advanceMsg{
        public arcMsgType msgType;
        public String message;
        public Date time;
        public String sender;
        public boolean selected;
        public Vec2 msgLoc;

        public advanceMsg(arcMsgType msgType, String message, Date time, String sender,Vec2 msgLoc){
            this.msgType = msgType;
            this.message = message;
            this.time = time;
            this.sender = sender;
            this.msgLoc = new Vec2().set(msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message,  String sender,Vec2 msgLoc){
            this(msgType, message, new Date(), sender, msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message,Vec2 msgLoc){
            this(msgType, message,"null",msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message){
            this(msgType, message, new Vec2(-1,-1));
        }


        public advanceMsg sendMessage(){
            ui.chatfrag.addMessage(msgType.arcMsgPreFix() + message, false);
            return this;
        }
    }

    public enum arcMsgType{
        normal("消息", Color.gray),

        chat("聊天",Color.valueOf("#778899")),

        markLoc("标记", "坐标", Color.valueOf("#7FFFD4")),
        markInfo("标记", "波次", Color.valueOf("#7FFFD4")),
        district("规划区","",Color.violet),

        serverTips("服务器", "小贴士", Color.valueOf("#98FB98"),false),
        serverMsg("服务器", "信息", Color.valueOf("#cefdce")),
        serverToast("服务器", "通报", Color.valueOf("#00FA9A")),
        serverSkill("服务器", "技能", Color.valueOf("#e6ffcc")),

        logicNotify("逻辑", "通报", Color.valueOf("#ffccff")),
        logicAnnounce("逻辑", "公告", Color.valueOf("#ffccff")),

        eventWorldLoad("事件", "载入地图", Color.valueOf("#ff9999")),
        eventCoreDestory("事件", "核心摧毁", Color.valueOf("#ffcccc")),
        eventWave("事件", "波次", Color.valueOf("#ffcc99"));

        public String name;
        public String type;
        public String subClass;
        public Color color = Color.gray;
        public Boolean show = true;

        arcMsgType(String type, String subClass, Color color,Boolean show){
            this.name = subClass == "" ? type : (type + "~" + subClass);
            this.type = type;
            this.subClass = subClass;
            this.color = color;
            this.show = show;
        }

        arcMsgType(String type, String subClass, Color color){
            this(type, subClass, color,true);
        }

        arcMsgType(String type, Color color){
            this(type, "", color);
        }

        arcMsgType(String type){
            this(type, Color.gray);
        }

        arcMsgType(String type, String subClass){
            this(type, subClass, Color.gray);
        }

        public String arcMsgPreFix(){
            return "[#" + color.toString() + "]" + "[" + name + "][]";
        }

    }
}