package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class BlockInfo {
    protected Cons<ScratchBlock> builder = s -> {};
    protected ValSupp supp = s -> null;
    protected Cons<Seq<Element>> run = e -> {};
    protected Cons<ScratchBlock.Run> runBuilder = r -> {};
    public BlockInfo() {
    }

    public BlockInfo(Cons<ScratchBlock> builder) {
        this.builder = builder;
    }

    public BlockInfo(Cons<ScratchBlock> builder, ValSupp supp) {
        this.builder = builder;
        this.supp = supp;
    }

    public BlockInfo(Cons<ScratchBlock> builder, Cons<Seq<Element>> run) {
        this.builder = builder;
        this.run = run;
    }

    public void build(ScratchBlock block) {
        builder.get(block);
    }

    public ScratchBlock.Run build(ScratchBlock.Run r) {
        r.valid = e -> false;
        r.cycle = run;
        runBuilder.get(r);
        return r;
    }

    public Object getValue(Seq<Element> elements) {
        return supp.get(elements);
    }

    public interface ValSupp {
        Object get(Seq<Element> elements);
    }
}
