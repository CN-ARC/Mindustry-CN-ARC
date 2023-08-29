package mindustry.arcModule.ui.logic.elements;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.logic.ElementType;
import mindustry.arcModule.ui.logic.ScratchElement;
import mindustry.arcModule.ui.logic.ScratchInput;
import mindustry.arcModule.ui.logic.ScratchStyles;
import mindustry.arcModule.ui.logic.blockbase.InputBlock;

public class CondElement extends ScratchElement {
    public CondElement() {
        elemColor = new Color(Color.packRgba(56, 148, 56, 255));
    }

    @Override
    public boolean accept(ScratchElement e) {
        return e.getType() == ElementType.condition;
    }

    @Override
    public ElementType getType() {
        return ElementType.condition;
    }

    @Override
    public void draw() {
        if (child == null) ScratchStyles.drawCond(x, y, width, height, elemColor);
        super.draw();
        Draw.reset();
    }
}
