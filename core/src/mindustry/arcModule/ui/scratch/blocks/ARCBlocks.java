package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
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
        ScratchController.registerBlock("info", new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo(block -> {
            block.label("显示弹窗");
            block.input();
        }, elements -> {
            Vars.ui.showInfo(String.valueOf(((InputElement) elements.get(1)).getValue()));
            return null;
        })));
        ScratchController.registerBlock("addChatMessage", new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo(block -> {
            block.label("聊天框信息");
            block.input();
        }, elements -> {
            Vars.ui.chatfrag.addMessage(String.valueOf(((InputElement) elements.get(1)).getValue()));
            return null;
        })));
        ScratchController.registerBlock("sendChatMessage", new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo(block -> {
            block.label("发送聊天");
            block.input();
        }, elements -> {
            Call.sendChatMessage(String.valueOf(((InputElement) elements.get(1)).getValue()));
            return null;
        })));
        ScratchController.registerBlock("js", new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo(block -> {
            block.label("运行js");
            block.input();
        }, elements -> Vars.mods.getScripts().runConsole(String.valueOf(((InputElement) elements.get(1)).getValue())))));
        ScratchController.registerBlock("js2", new ScratchBlock(ScratchType.input, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo(block -> {
            block.label("运行js");
            block.input();
        }, elements -> {
            Scripts s = Vars.mods.getScripts();
            Object o = s.context.evaluateString(s.scope, String.valueOf(((InputElement) elements.get(1)).getValue()), "scratch.js", 1);
            if (o instanceof NativeJavaObject n) o = n.unwrap();
            return o instanceof Undefined ? "undefined" : o;
        })));
    }
}
