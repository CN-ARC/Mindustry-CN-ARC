package mindustry.arcModule.ui.scratch.blocks;

import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.gen.Call;
import mindustry.mod.Scripts;
import rhino.NativeJavaObject;
import rhino.Undefined;

public class ARCBlocks {
    public static void init() {
        Color c = new Color(Color.packRgba(76, 151, 255, 255));
        ScratchController.ui.addCategory("高级", c);
        ScratchController.registerBlock("info", new ScratchBlock(ScratchType.block, c, new BlockInfo(b -> {
            b.label("显示弹窗");
            b.input();
        }, e -> {
            Vars.ui.showInfo(String.valueOf(((InputElement) e.get(1)).getValue()));
        })));
        ScratchController.registerBlock("addChatMessage", new ScratchBlock(ScratchType.block, c, new BlockInfo(b -> {
            b.label("聊天框信息");
            b.input();
        }, e -> {
            Vars.ui.chatfrag.addMessage(String.valueOf(((InputElement) e.get(1)).getValue()));
        })));
        ScratchController.registerBlock("sendChatMessage", new ScratchBlock(ScratchType.block, c, new BlockInfo(b -> {
            b.label("发送聊天");
            b.input();
        }, e -> {
            Call.sendChatMessage(String.valueOf(((InputElement) e.get(1)).getValue()));
        })));
        ScratchController.registerBlock("js", new ScratchBlock(ScratchType.block, c, new BlockInfo(b -> {
            b.label("运行js");
            b.input();
        }, (Cons<Seq<Element>>) e -> Vars.mods.getScripts().runConsole(String.valueOf(((InputElement) e.get(1)).getValue())))));
        ScratchController.registerBlock("js2", new ScratchBlock(ScratchType.input, c, new BlockInfo(b -> {
            b.label("运行js");
            b.input();
        }, e -> {
            Scripts s = Vars.mods.getScripts();
            Object o = s.context.evaluateString(s.scope, String.valueOf(((InputElement) e.get(1)).getValue()), "scratch.js", 1);
            if (o instanceof NativeJavaObject n) o = n.unwrap();
            return o instanceof Undefined ? "undefined" : o;
        })));
        ScratchController.registerBlock("strcat", new ScratchBlock(ScratchType.input, c, new BlockInfo(b -> {
            b.label("连接字符串");
            b.input();
            b.input();
        }, e -> ((InputElement) e.get(1)).getValue() + String.valueOf(((InputElement) e.get(2)).getValue()))));
    }
}
