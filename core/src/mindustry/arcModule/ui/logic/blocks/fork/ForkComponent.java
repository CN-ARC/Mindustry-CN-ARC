package mindustry.arcModule.ui.logic.blocks.fork;

import arc.graphics.Color;
import mindustry.arcModule.ui.logic.BlockInfo;
import mindustry.arcModule.ui.logic.ScratchType;
import mindustry.arcModule.ui.logic.blocks.ForkBlock;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;
import mindustry.arcModule.ui.logic.elements.ScratchElement;

public abstract class ForkComponent extends ScratchBlock {
    public static final float padLeft = 10f;
    public ForkBlock.ForkInfo info;
    {
        hittable = false;
    }

    public ForkComponent(String name, ScratchType type, Color color, BlockInfo info) {
        super(name, type, color, info, false);
        this.info = (ForkBlock.ForkInfo) info;
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }
}
