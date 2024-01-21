package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

abstract public class ForkHasChild extends ForkComponent {
    public static final float defHeight = 20;
    public Cell<?> cell;
    private float drawHeight = defHeight;

    ForkHasChild(Color c, BlockInfo info) {
        super(c, info);
        touchable = Touchable.enabled;
    }

    protected void drawLeftBorder() {
        Lines.line(x, y, x, y - drawHeight);
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
    public void linkFrom(ScratchBlock source) {
        super.linkFrom(source);
        drawHeight = source == null ? defHeight : source.getTotalHeight();
        cell.minHeight(drawHeight);
        invalidateHierarchy();
    }

    @Override
    public void act(float delta) {
        if (linkFrom != null) {
            float f = linkFrom.getTotalHeight();
            if (f != drawHeight) {
                drawHeight = f;
                cell.minHeight(drawHeight);
                invalidateHierarchy();
            }
        }
        super.act(delta);
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
            this.parent.touchable = Touchable.disabled;
            localToAscendantCoordinates(ScratchController.ui.group, Tmp.v1.set(x, y));
            Element hit2 = ScratchController.ui.group.hit(Tmp.v1.x, Tmp.v1.y, touchable);
            this.parent.touchable = Touchable.enabled;
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
