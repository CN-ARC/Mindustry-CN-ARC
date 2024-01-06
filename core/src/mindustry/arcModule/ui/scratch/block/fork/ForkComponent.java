package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public abstract class ForkComponent extends ScratchBlock {
    public static final float padLeft = 10f;
    public ForkBlock.ForkInfo info;
    {
        hittable = false;
    }

    public ForkComponent(ScratchType type, Color color, BlockInfo info) {
        super(type, color, info, false);
        this.info = (ForkBlock.ForkInfo) info;
        margin(10);
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0);
        c.row();
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }
}
