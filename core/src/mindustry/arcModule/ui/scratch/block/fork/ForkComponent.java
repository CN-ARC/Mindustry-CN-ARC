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

    ForkComponent(BlockInfo info) {
        this(Color.white, info);
    }

    ForkComponent(Color c, BlockInfo info) {
        super(ScratchType.none, c, info, false);
        touchable = Touchable.disabled;
        margin(10, 0, 10, 10);
    }

    @Override
    public void init() {
    }

    public void copyTo(ForkComponent fork, boolean drag) {
    }

    public boolean touched(float x, float y) {
        return x > 0 && x < width && y > 0 && y < height;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0).align(Align.left).growX().row();
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }
}
