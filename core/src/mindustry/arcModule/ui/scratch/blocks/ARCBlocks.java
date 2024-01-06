package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import mindustry.Vars;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.gen.Call;

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
    }
}
