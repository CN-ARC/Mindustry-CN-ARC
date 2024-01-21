package mindustry.arcModule.ui.scratch.element;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class CondElement extends ScratchElement {
    public CondElement() {
    }

    @Override
    public boolean accept(ScratchTable e) {
        return e.getType() == ScratchType.condition;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        c.pad(addPadding, 5, addPadding, 5).minSize(40, 23);
        elemColor = ((ScratchTable) parent).elemColor.cpy().lerp(Color.black, 0.3f);
    }

    @Override
    public ScratchType getType() {
        return ScratchType.condition;
    }

    @Override
    public Object getValue() {
        return child != null ? child.getValue() : null;
    }

    @Override
    public void drawChildren() {
        if (child == null) {
            ScratchStyles.drawCond(x, y, width, height, elemColor, ScratchController.selected == this);
        } else if (ScratchController.selected == this && accept(ScratchController.dragging)) {
            ScratchStyles.drawCondSelected(x, y, width, height);
        }
        super.drawChildren();
    }

    @Override
    public ScratchElement copy() {
        CondElement e = new CondElement();
        if (child instanceof ScratchBlock sb) sb.copy().asChild(e);
        return e;
    }
}
