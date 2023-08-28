package mindustry.arcModule.ui.logic.blockbase;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import mindustry.arcModule.ui.logic.ElementType;
import mindustry.arcModule.ui.logic.ScratchInput;
import mindustry.arcModule.ui.logic.ScratchStyles;

public class CondBlock extends ScratchBlock {
    protected static Color defaultColor = new Color(Color.packRgba(89, 192, 89, 255));

    public CondBlock(Element[] elements) {
        this(elements, defaultColor);
    }

    public CondBlock(Element[] elements, Color color) {
        super(elements, true);
        type = ElementType.condition;
        this.color = color;
        ScratchInput.addDraggingInput(this);
    }

    @Override
    public void draw() {
        Draw.reset();
        if (type == ElementType.condition) {
            ScratchStyles.drawCond(x, y, width, height, color);
        }
        super.draw();
    }
}
