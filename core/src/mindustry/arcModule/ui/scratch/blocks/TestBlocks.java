package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.ForkBlock;

public class TestBlocks {
    public static void init() {
        ScratchController.registerBlock("test5", new ForkBlock(new Color(Color.packRgba(255, 171, 25, 255)), new ForkBlock.ForkInfo(block -> {
            block.header(new BlockInfo(b -> {
                b.label("如果");
                b.cond();
            }, e -> null));
        }, e -> null)));
    }
}
