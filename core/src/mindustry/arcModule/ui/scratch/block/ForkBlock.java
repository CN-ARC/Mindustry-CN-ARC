package mindustry.arcModule.ui.scratch.block;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.fork.*;

@SuppressWarnings("unused")
public class ForkBlock extends ScratchBlock {
    private static final Vec2 v1 = new Vec2();
    public ForkBlock(Color color, ForkInfo info) {
        this(color, info, false);
    }

    public ForkBlock(Color color, ForkInfo info, boolean drag) {
        super(ScratchType.block, color, info, drag);
        marginLeft(15);
        ForkFooter e = new ForkFooter();
        e.cell(add(e));
    }

    @Override
    public void init() {
    }

    public void header(BlockInfo info) {
        ForkHeader e = new ForkHeader(elemColor, info);
        e.cell(add(e));
        e.cell = add().minHeight(ForkHasChild.defHeight);
        row();
    }

    public void header(BlockInfo info, ForkHasChild.ForkPop pop) {
        ForkHeader e = new ForkHeader(elemColor, info, pop);
        e.cell(add(e));
        e.cell = add().minHeight(ForkHasChild.defHeight);
        row();
    }

    public void header(Cons<ScratchBlock> lambda) {
        header(new BlockInfo(lambda));
    }

    public void header(Cons<ScratchBlock> lambda, ForkHasChild.ForkPop pop) {
        header(new BlockInfo(lambda), pop);
    }

    public void inner(BlockInfo info) {
        ForkInner e = new ForkInner(elemColor, info);
        e.cell(add(e));
        e.cell = add().minHeight(ForkHasChild.defHeight);
        row();
    }

    public void inner(BlockInfo info, ForkHasChild.ForkPop pop) {
        ForkInner e = new ForkInner(elemColor, info, pop);
        e.cell(add(e));
        e.cell = add().minHeight(ForkHasChild.defHeight);
        row();
    }

    public void inner(Cons<ScratchBlock> lambda) {
        inner(new BlockInfo(lambda));
    }

    public void inner(Cons<ScratchBlock> lambda, ForkHasChild.ForkPop pop) {
        inner(new BlockInfo(lambda), pop);
    }

    public void updateFork(ScratchBlock target) {
        if (elements.get(0).parent != target.parent) elements.each(e -> {
            if (e instanceof ForkHasChild f) f.setLinkedParent(target);
        });
    }

    public Element touched(float x, float y) {
        for (var i : children) {
            i.parentToLocalCoordinates(v1.set(x, y));
            if (((ForkComponent) i).touched(v1.x, v1.y)) return i.hit(v1.x, v1.y, true);
        }
        return null;
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
        if (ScratchController.dragging != null) return super.hit(x, y, touchable);
        if (x > 0 && x < 15 && y > 0 && y < height) return this;
        return touched(x, y);
    }

    @Override
    public boolean remove() {
        elements.each(e -> ((ForkComponent) e).removeLinked());
        return super.remove();
    }

    @Override
    public void actChain(float delta) {
        act(delta);
        children.each(c -> {
            if (c instanceof ScratchBlock sb) sb.actChain(delta);
        });
        if (linkFrom != null) linkFrom.actChain(delta);
    }

    public static class ForkInfo extends BlockInfo {
        protected Cons<ForkBlock> builder;

        public ForkInfo(Cons<ForkBlock> builder, BlockInfo.ValSupp supp) {
            this.builder = builder;
            this.supp = supp;
        }

        public ForkInfo(Cons<ForkBlock> builder, Cons<Seq<Element>> run) {
            this.builder = builder;
            this.run = run;
        }

        @Override
        public void build(ScratchBlock block) {
            builder.get((ForkBlock) block);
        }
    }
}
