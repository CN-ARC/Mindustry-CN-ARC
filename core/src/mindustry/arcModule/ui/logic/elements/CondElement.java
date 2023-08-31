package mindustry.arcModule.ui.logic.elements;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import mindustry.arcModule.ui.logic.ScratchType;
import mindustry.arcModule.ui.logic.ScratchTable;
import mindustry.arcModule.ui.logic.ScratchStyles;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

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
