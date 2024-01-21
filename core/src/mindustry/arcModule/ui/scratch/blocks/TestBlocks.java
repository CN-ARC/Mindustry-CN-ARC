package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.fork.ForkComponent;
import mindustry.arcModule.ui.scratch.element.CondElement;

public class TestBlocks {
    public static void init() {
        ScratchController.registerBlock("test5", new ForkBlock(new Color(Color.packRgba(255, 171, 25, 255)), new ForkBlock.ForkInfo(block -> block.header(new BlockInfo(b -> {
            b.label("如果");
            b.cond();
        }, e -> {
            if (((CondElement) ((ForkComponent) e.get(0)).elements.get(1)).getValue()) {
                ScratchBlock run = ((ForkComponent) e.get(0)).linkFrom;
                if (run != null) run.getValue();
            }
            return null;
        })), e -> null)));
    }
}
