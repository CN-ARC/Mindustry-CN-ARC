package mindustry.arcModule.ui.logic.blockbase;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import mindustry.arcModule.ui.logic.ElementType;
import mindustry.arcModule.ui.logic.ScratchInput;
import mindustry.arcModule.ui.logic.ScratchStyles;

public class InputBlock extends ScratchBlock {
    protected static Color defaultColor = new Color(Color.packRgba(89, 192, 89, 255));

    public InputBlock(Element[] elements) {
        this(elements, defaultColor);
    }

    public InputBlock(Element[] elements, Color color) {
        super(elements, false);
        type = ElementType.input;
        this.color = color;
        ScratchInput.addDraggingInput(this);
    }

    @Override
    public void draw() {
        Draw.reset();
        ScratchStyles.drawInput(x, y, width, height, color);
        super.draw();
    }
}
