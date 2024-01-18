package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

abstract public class ForkHasChild extends ForkComponent {

    ForkHasChild(ScratchType type, Color color, BlockInfo info, int id) {
        super(type, color, info, id);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return true;
    }

    @Override
    public void copyTo(ForkComponent fork, boolean drag) {
        copyChildrenValue(fork, drag);
        fork.setChild(linkFrom.copy());
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.marginTop(0).marginBottom(0);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (touchable && this.touchable != Touchable.enabled) return null;
        if (x >= 0 && x < width && y >= -padValue * 2 && y < 0) {
            dir = Align.bottom;
            return this;
        }
        Element hit = hitDefault(x, y, touchable);
        return hit == this ? null : hit;
    }

    @Override
    public void linkUpdate(ScratchBlock target) {
        target.setPosition(parent.x + x, parent.y + y - target.getHeight() + 7);
    }

    @Override
    public void setParent(ScratchBlock target) {
        super.setParent(target);
        if (linkFrom != null) linkFrom.setParent(this);
    }
}
