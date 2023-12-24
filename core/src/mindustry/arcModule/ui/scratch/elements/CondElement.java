package mindustry.arcModule.ui.scratch.elements;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;

public class CondElement extends ScratchElement {
    public CondElement() {
        elemColor = new Color(Color.packRgba(56, 148, 56, 255));
    }

    @Override
    public boolean accept(ScratchTable e) {
        return e.getType() == ScratchType.condition;
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
    public void getValue(Cons<Object> callback) {
        callback.get(child != null ? child.getValue() : null);
    }

    @Override
    public void draw() {
        if (child == null) ScratchStyles.drawCond(x, y, width, height, elemColor);
        super.draw();
        Draw.reset();
    }

    @Override
    public ScratchElement copy() {
        CondElement e = new CondElement();
        if (child instanceof ScratchBlock sb) sb.copy().asChild(e);
        return e;
    }
}
