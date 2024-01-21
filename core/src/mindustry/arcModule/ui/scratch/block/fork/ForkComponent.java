package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public abstract class ForkComponent extends ScratchBlock {
    public static final BlockInfo emptyInfo = new BlockInfo();
    public final int id;

    ForkComponent(ScratchType type, Color color, BlockInfo info, int id) {
        super(type, color, info, false, false);
        touchable = Touchable.disabled;
        margin(10, 0, 10, 10);
        this.id = id;
    }

    public void copyTo(ForkComponent fork, boolean drag) {
    }

    public static void drawBorderBottom(float x, float y, float w) {
        Lines.line(x + w, y, x + 35, y);
        Lines.line(x + 35, y, x + 30, y - 7);
        Lines.line(x + 30, y - 7, x + 15, y - 7);
        Lines.line(x + 15, y - 7, x + 10, y);
        Lines.line(x + 10, y, x, y);
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0).align(Align.left);
        c.row();
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }
}
