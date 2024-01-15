package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public abstract class ForkComponent extends ScratchBlock {
    public static final BlockInfo emptyInfo = new BlockInfo();

    public ForkComponent(ScratchType type, Color color, BlockInfo info) {
        super(type, color, info, false);
        touchable = Touchable.disabled;
        hittable = false;
        margin(10, 0, 10, 10);
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0).align(Align.left);
        c.row();
    }

    public void copyTo(ForkComponent fork, boolean drag) {
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }
}
