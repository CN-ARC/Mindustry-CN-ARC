package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class BlockInfo {
    protected Cons<ScratchBlock> builder = s -> {};
    protected ValSupp supp = s -> null;
    protected Cons<Seq<Element>> run = e -> {};
    public short id = -1;
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

    public Object getValue(Seq<Element> elements) {
        return supp.get(elements);
    }

    public void run(ScratchBlock block) {
        run.get(block.elements);
    }

    public void setID(short id) {
        if (this.id != -1) throw new IllegalStateException("ID is already set! old ID: " + this.id + "(" + ScratchController.getBlock(this.id).getClass().getSimpleName() + "), new ID: " + id + "(" + ScratchController.getBlock(id).getClass().getSimpleName() + ")");
        this.id = id;
    }

    public interface ValSupp {
        Object get(Seq<Element> elements);
    }
}
