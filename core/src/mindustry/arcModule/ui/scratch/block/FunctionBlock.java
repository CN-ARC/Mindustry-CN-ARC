package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchType;

public class FunctionBlock extends ScratchBlock {
    public FunctionBlock(ScratchType type, Color color, BlockInfo info) {
        super(type, color, info);
    }

    public FunctionBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
        super(type, color, info, dragEnabled);
    }
}
