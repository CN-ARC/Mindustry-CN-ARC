package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class BlockInfo {
    Cons<ScratchBlock> builder = s -> {};
    ValSupp supp = s -> null;
    public BlockInfo() {

    }

    public BlockInfo(Cons<ScratchBlock> builder, ValSupp supp) {
        this.builder = builder;
        this.supp = supp;
    }

    public void build(ScratchBlock block) {
        builder.get(block);
    }

    public Object getValue(Seq<Element> elements) {
        return supp.get(elements);
    }

    public interface ValSupp {
        Object get(Seq<Element> elements);
    }
}
