package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.TriggerBlock;

abstract public class ForkHasChild extends ForkComponent {
    public static final float defHeight = 20;
    public Cell<?> cell;
    public ForkPop pop = e -> ((ForkBlock) parent).linkFrom;
    private float drawHeight = defHeight;

    ForkHasChild(Color c, BlockInfo info) {
        super(c, info);
        touchable = Touchable.enabled;
    }

    public static void drawBorderDirect(float x, float y, float w, float h) {
        Lines.line(x + 10, y + h, x + 15, y + h - 7);
        Lines.line(x + 15, y + h - 7, x + 30, y + h - 7);
        Lines.line(x + 30, y + h - 7, x + 35, y + h);
        Lines.line(x + 35, y + h, x + w, y + h);
        Lines.line(x + w, y + h, x + w, y);
    }

    protected void drawLeftBorder() {
        Lines.line(x, y, x, y - drawHeight);
    }

    public void setLinkedParent(ScratchBlock target) {
        ScratchBlock b = linkFrom;
        if (b != null && b.parent == target.parent) return;
        while (b != null) {
            b.setParent(target);
            b = b.linkFrom;
        }
    }

    public ScratchBlock pop() {
        return pop.pop(((ForkBlock) parent).elements);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return !(block instanceof TriggerBlock);
    }

    @Override
    public void copyTo(ForkComponent fork, boolean drag) {
        copyChildrenValue(fork, drag);
        if (linkFrom != null) {
            ScratchBlock copied = linkFrom.copyTree(true);
            ScratchController.ui.group.addChild(copied);
            copied.linkTo(fork);
        }
    }

    @Override
    public void cell(Cell<? extends ScratchTable> c) {
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
        return hitDefault(x, y, touchable);
    }

    @Override
    public void linkUpdate(ScratchBlock target) {
        target.setPosition(parent.x + x, parent.y + y - target.getHeight());
    }

    public interface ForkPop {
        ScratchBlock pop(Seq<Element> e);
    }
}
