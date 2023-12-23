package mindustry.arcModule.ui.logic.blocks.fork;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.logic.BlockInfo;
import mindustry.arcModule.ui.logic.ScratchTable;
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
