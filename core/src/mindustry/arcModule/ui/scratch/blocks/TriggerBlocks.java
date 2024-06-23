package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.TriggerBlock;

public class TriggerBlocks {
    public static void init() {
        Color c = new Color(Color.packRgba(255, 191, 0, 255));
        ScratchController.category("trigger", c);
        ScratchController.registerBlock("whenStart", new TriggerBlock(c, new BlockInfo(b -> {
            b.label("start");
        })));
    }
}
