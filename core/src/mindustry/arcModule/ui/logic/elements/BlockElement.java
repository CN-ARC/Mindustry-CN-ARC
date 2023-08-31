package mindustry.arcModule.ui.logic.elements;

import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;
import mindustry.arcModule.ui.logic.ScratchTable;
import mindustry.arcModule.ui.logic.ScratchType;

public class BlockElement extends ScratchElement {
    @Override
    public ScratchType getType() {
        return ScratchType.blockInner;
    }

    @Override
    public ScratchElement copy() {
        BlockElement e = new BlockElement();
        if (child instanceof ScratchBlock sb) sb.copy().asChild(e);
        return e;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0).padLeft(addPadding);
    }
}
