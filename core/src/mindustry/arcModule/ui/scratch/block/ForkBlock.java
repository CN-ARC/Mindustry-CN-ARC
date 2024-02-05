package mindustry.arcModule.ui.scratch.block;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.fork.*;

@SuppressWarnings("unused")
public class ForkBlock extends ScratchBlock {
    public ForkBlock(Color color, ForkInfo info) {
        this(color, info, false);
    }

    public ForkBlock(Color color, ForkInfo info, boolean drag) {
        super(ScratchType.block, color, info, drag, false);
        marginLeft(15);
        ForkFooter e = new ForkFooter();
        e.cell(add(e));
    }

    public void header(BlockInfo info) {
        ForkHeader e = new ForkHeader(elemColor, info);
        e.cell(add(e));
        e.cell = add().minHeight(ForkHasChild.defHeight);
        row();
    }

    public void inner() {
        ForkInner e = new ForkInner(elemColor, emptyInfo);
        e.cell(add(e));
    }

    public void updateFork(ScratchBlock target) {
        if (elements.get(0).parent != target.parent) elements.each(e -> {
            if (e instanceof ForkHasChild f) f.setLinkedParent(target);
        });
    }

    @Override
    public ForkBlock copy(boolean drag) {
        ForkBlock sb = new ForkBlock(elemColor, (ForkInfo) info, true);
        for (int i = 0; i < elements.size; i++) ((ForkComponent) elements.get(i)).copyTo((ForkComponent) sb.elements.get(i), drag);
        return sb;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0);
    }

    @Override
    public void drawBackground() {
        Draw.color(elemColor);
        Fill.crect(x, y, 15, height - 7);
        Draw.color(Tmp.c1.set(elemColor).lerp(Color.black, 0.3f));
        Lines.line(x, y, x, y + height);
    }

    @Override
    public void drawChildren() {
        super.drawChildren();
    }

    @Override
    public void ensureParent() {
        updateFork(this);
        super.ensureParent();
    }

    @Override
    public void setParent(ScratchBlock target) {
        updateFork(target);
        super.setParent(target);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        this.touchable = Touchable.childrenOnly;
        Element hit2 = hitDefault(x, y, touchable);
        this.touchable = Touchable.enabled;
        return hit2 == null ? super.hit(x, y, touchable) : hit2;
    }

    @Override
    public void removeLinked() {
        elements.each(e -> ((ForkComponent) e).removeLinked());
        super.removeLinked();
    }

    public static class ForkInfo extends BlockInfo {
        Cons<ForkBlock> builder;
        ValSupp supp;

        public ForkInfo(Cons<ForkBlock> builder, BlockInfo.ValSupp supp) {
            this.builder = builder;
            this.supp = supp;
        }

        public void build(ForkBlock block) {
            builder.get(block);
        }

        public Object getValue(Seq<Element> elements) {
            return supp.get(elements);
        }

        @Override
        public void build(ScratchBlock block) {
            builder.get((ForkBlock) block);
        }
    }
}
