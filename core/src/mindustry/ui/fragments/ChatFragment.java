package mindustry.ui.fragments;

import arc.*;
import arc.Input.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class ChatFragment extends Table{
    private static final int messagesShown = 10;
    private Seq<String> messages = new Seq<>();
    private float fadetime;
    private boolean shown = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private ChatMode mode = ChatMode.normal;
    private Font font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Scl.scl(4), offsety = Scl.scl(4), fontoffsetx = Scl.scl(2), chatspace = Scl.scl(50);
    private Color shadowColor = new Color(0, 0, 0, 0.5f);
    private float textspacing = Scl.scl(10);
    private Seq<String> history = new Seq<>();
    private int historyPos = 0;
    private int scrollPos = 0;

    public ChatFragment(){
        super();

        setFillParent(true);
        font = Fonts.def;

        visible(() -> {
            if(!net.active() && messages.size > 0){
                clearMessages();

                if(shown){
                    hide();
                }
            }

            return net.active() && ui.hudfrag.shown;
        });

        update(() -> {

            if(net.active() && input.keyTap(Binding.chat) && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null || ui.minimapfrag.shown()) && !ui.consolefrag.shown()){
                toggle();
            }

            if(shown){
                if(input.keyTap(Binding.chat_history_prev) && historyPos < history.size - 1){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                     while(!chatValidType(messages.get(historyPos)) && historyPos < history.size - 2) historyPos++;
                    historyPos++;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_history_next) && historyPos > 0){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                    while(!chatValidType(messages.get(historyPos)) && historyPos < history.size - 1 && historyPos > 0) historyPos--;
                    historyPos--;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_mode)){
                    nextMode();
                }
                scrollPos = (int)Mathf.clamp(scrollPos + input.axis(Binding.chat_scroll), 0, Math.max(0, messages.size - messagesShown));
            }
        });

        history.insert(0, "");
        setup();
    }

    public void build(Group parent){
        scene.add(this);
    }

    public void clearMessages(){
        messages.clear();
        history.clear();
        history.insert(0, "");
    }

    private void setup(){
        fieldlabel.setStyle(new LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextFieldStyle(scene.getStyle(TextFieldStyle.class)));
        chatfield.setMaxLength(Vars.maxTextLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().fontColor = Color.white;
        chatfield.setStyle(chatfield.getStyle());

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if(Vars.mobile){
            marginBottom(105f);
            marginRight(240f);
        }
    }

    protected void rect(float x, float y, float w, float h){
        //prevents texture bindings; the string lookup is irrelevant as it is only called <10 times per frame, and maps are very fast anyway
        Draw.rect("whiteui", x + w/2f, y + h/2f, w, h);
    }

    @Override
    public void draw(){
        float opacity = Core.settings.getInt("chatopacity") / 100f;
        float textWidth = Math.min(Core.graphics.getWidth()/1.5f, Scl.scl(700f));

        Draw.color(shadowColor);

        if(shown){
            rect(offsetx, chatfield.y + scene.marginBottom, chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw();

        float spacing = chatspace;

        chatfield.visible = shown;
        fieldlabel.visible = shown;

        Draw.color(shadowColor);
        Draw.alpha(shadowColor.a * opacity);

        float theight = offsety + spacing + getMarginBottom() + scene.marginBottom;
        int messageCount = 0;
        for (int i = scrollPos; i < messages.size && messageCount < messagesShown && (i < fadetime || shown); i++) {
            if(!chatValidType(messages.get(i))) continue;
            messageCount += 1;

            layout.setText(font, messages.get(i), Color.white, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if(i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().setColor(Color.white);
            font.getCache().addText(messages.get(i), fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(!shown && fadetime - i < 1f && fadetime - i >= 0f){
                font.getCache().setAlphas((fadetime - i) * opacity);
                Draw.color(0, 0, 0, shadowColor.a * (fadetime - i) * opacity);
            }else{
                font.getCache().setAlphas(opacity);
            }

            rect(offsetx, theight - layout.height - 2, textWidth + Scl.scl(4f), layout.height + textspacing);
            Draw.color(shadowColor);
            Draw.alpha(opacity * shadowColor.a);

            font.getCache().draw();
        }

        Draw.color();

        if(fadetime > 0 && !shown){
            fadetime -= Time.delta / 180f;
        }
    }

    private void sendMessage(){
        String message = chatfield.getText().trim();
        clearChatInput();

        //avoid sending prefix-empty messages
        if(message.isEmpty() || (message.startsWith(mode.prefix) && message.substring(mode.prefix.length()).isEmpty())) return;

        if (arcMessage(message)) return;

        history.insert(1, message);

        Call.sendChatMessage(message);
    }

    public void toggle(){

        if(!shown){
            scene.setKeyboardFocus(chatfield);
            shown = true;
            if(mobile){
                TextInput input = new TextInput();
                input.maxLength = maxTextLength;
                input.accepted = text -> {
                    chatfield.setText(text);
                    sendMessage();
                    hide();
                    Core.input.setOnscreenKeyboardVisible(false);
                };
                input.canceled = this::hide;
                Core.input.getTextInput(input);
            }else{
                chatfield.fireClick();
            }
        }else{
            //sending chat has a delay; workaround for issue #1943
            Time.runTask(2f, () ->{
                scene.setKeyboardFocus(null);
                shown = false;
                scrollPos = 0;
                sendMessage();
            });
        }
    }

    public void hide(){
        scene.setKeyboardFocus(null);
        shown = false;
        clearChatInput();
    }

    public void updateChat() {
        if  (history.get(historyPos).contains(mode.normalizedPrefix())) {
            chatfield.setText(history.get(historyPos));
        } else {
            chatfield.setText(mode.normalizedPrefix() + history.get(historyPos));
        }
        updateCursor();
    }

    public void nextMode(){
        ChatMode prev = mode;

        do{
            mode = mode.next();
        }while(!mode.isValid());

        if(chatfield.getText().startsWith(prev.normalizedPrefix())){
            chatfield.setText(mode.normalizedPrefix() + chatfield.getText().substring(prev.normalizedPrefix().length()));
        }else{
            chatfield.setText(mode.normalizedPrefix());
        }

        updateCursor();
    }

    public void clearChatInput(){
        historyPos = 0;
        history.set(0, "");
        chatfield.setText(mode.normalizedPrefix());
        updateCursor();
    }

    public void updateCursor(){
        chatfield.setCursorPosition(chatfield.getText().length());
    }

    public boolean shown(){
        return shown;
    }

    public void addMessage(String message){
        if(message == null) return;

        messages.insert(0, message);

        fadetime += 1f;
        fadetime = Math.min(fadetime, messagesShown) + 1f;
        
        if(scrollPos > 0) scrollPos++;
    }

    public void addMessage(String message,Boolean resolve){
        if(message != null && resolve) ui.MessageDialog.resolveMsg(message);
        addMessage(message);
    }

    public void addMessage(String message,@Nullable Player playersender){
        if(message != null) ui.MessageDialog.resolveMsg(message,playersender);

        addMessage(message);
    }

    private boolean arcMessage(String message){
        if (message.startsWith("!help")) {
            helpMessage();
            return true;
        }
        if (message.startsWith("!clear")) {
            clearMessages();
            messages.insert(0, "[cyan][ARC" + arcVersion + "][pink]聊天记录已清空");
            return true;
        }
        if (message.startsWith("!log")) {
            copyMessage(message);
            messages.insert(0, "[cyan][ARC" + arcVersion + "][pink]聊天记录已复制到粘贴板");
            return true;
        }
        return false;
    }

    private void copyMessage(String message) {
        int logLength = Integer.min(20,messages.size - 1);
        try {
            logLength = Integer.min(messages.size - 1, Integer.parseInt(message.substring(4, message.length()).trim()));
        } catch (Exception e) {
        }

        StringBuilder messageHis = new StringBuilder();
        messageHis.append("下面是[ARC").append(arcVersion).append("] 导出的游戏内聊天记录").append("\n");
        messageHis.append("*** 当前地图名称: ").append(state.map.name()).append("（模式：").append(state.rules.modeName).append("）\n");
        messageHis.append("*** 当前波次: ").append(state.wave).append("\n");
        messageHis.append("*** 导出模式: ").append(getValidType()).append("\n");

        StringBuilder messageLs = new StringBuilder();
        int messageCount = 0;
        for (int i = 0; i <messages.size && messageCount <= logLength; i++) {
            String msg = messages.get(i);
            if (!chatValidType(msg)) continue;
            messageLs.insert(0,messages.get(i) + "\n");
            messageCount +=1;
        }

        messageHis.append("成功选取共 ").append(messageCount).append(" 条记录，如下：\n");
        messageHis.append(messageLs);
        Core.app.setClipboardText(Strings.stripGlyphs(Strings.stripColors(messageHis.toString())));
    }

    private void helpMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("[cyan][ARC").append(arcVersion).append("][violet]聊天辅助器").append("\n\n");
        msg.append("[acid]!help  [white]调出本帮助菜单").append("\n");
        msg.append("[acid]!clear  [white]清除聊天记录").append("\n");
        msg.append("[acid]!log  [white]复制聊天记录到粘贴板，默认导出20条").append("\n");
        msg.append("[acid]!logx  [white]复制最近的x条聊天记录，数字可修改").append("\n\n");
        msg.append("[orange]如果与服务器插件有指令冲突，请及时反馈。\n可以在设置处设置导出情况");
        ui.showInfo(msg.toString());
    }

    private boolean chatValidType(String msg) {
        int chatType = settings.getInt("chatValidType");
        if (chatType==0 && (msg.contains("[acid][公屏][white]") || msg.contains("[violet][逻辑-"))) return false;
        else if(chatType==1 &&
                (msg.contains("加入了服务器") ||msg.contains("离开了服务器")||msg.contains("小贴士")||msg.contains("自动存档完成")||
                msg.contains("登录成功")||msg.contains("经验+")||msg.contains("[ARC")
                ||(msg.contains("[acid][公屏][white]"))|| msg.contains("[violet][逻辑-"))) return false;
        else if(chatType==2 && !(msg.contains("[acid][公屏][white]") || msg.contains("[violet][逻辑-") )) return false;
        return true;
    }

    private String getValidType() {
        int chatType = settings.getInt("chatValidType");
        if (chatType==0) return "原版模式";   //默认下无视hub
        else if (chatType==1) return "纯净聊天";
        else if (chatType==2) return "服务器记录";
        return "全部记录";
    }


    private enum ChatMode{
        normal(""),
        team("/t"),
        admin("/a", player::admin)
        ;

        public String prefix;
        public Boolp valid;
        public static final ChatMode[] all = values();

        ChatMode(String prefix){
            this.prefix = prefix;
            this.valid = () -> true;
        }

        ChatMode(String prefix, Boolp valid){
            this.prefix = prefix;
            this.valid = valid;
        }

        public ChatMode next(){
            return all[(ordinal() + 1) % all.length];
        }

        public String normalizedPrefix(){
            return prefix.isEmpty() ? "" : prefix + " ";
        }

        public boolean isValid(){
            return valid.get();
        }
    }
}
