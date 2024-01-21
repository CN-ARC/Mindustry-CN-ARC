package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
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
        if (linkFrom != null) fork.setChild(linkFrom.copy());
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
        if (hit == this) {
            this.touchable = Touchable.disabled;
            localToAscendantCoordinates(ScratchController.ui.group, Tmp.v1.set(x, y));
            Element hit2 = ScratchController.ui.group.hit(Tmp.v1.x, Tmp.v1.y, touchable);
            this.touchable = Touchable.enabled;
            return hit2;
        }
        return hit;
    }

    @Override
    public void linkUpdate(ScratchBlock target) {
        target.setPosition(parent.x + x, parent.y + y - target.getHeight());
    }

    @Override
    public void ensureParent() {
        ScratchBlock b = linkFrom;
        if (b != null && b.parent == parent.parent) return;
        while (b != null) {
            b.setParent((ForkBlock) parent);
            b = b.linkFrom;
        }
    }
}
