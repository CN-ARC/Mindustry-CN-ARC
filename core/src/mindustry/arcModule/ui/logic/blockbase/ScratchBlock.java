package mindustry.arcModule.ui.logic.blockbase;

import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Align;
import mindustry.arcModule.ui.logic.ElementType;
import mindustry.arcModule.ui.logic.ScratchElement;
import mindustry.arcModule.ui.logic.elements.InputElement;

public class ScratchBlock extends ScratchElement {
    protected Element[] elements;
    public ElementType type = ElementType.none;
    protected Color color;

    public ScratchBlock(Element[] elem, boolean extraPadding) {
        super();
        align(Align.bottomLeft);
        for (Element e : elem) {
            if (e instanceof ScratchElement se) {
                if (extraPadding && se.getType() == ElementType.input) {
                    se.cell(add(se).padLeft(10).padRight(10));
                } else {
                    se.cell(add(se));
                }
            } else {
                add(e);
            }
        }
        elements = elem;
    }

    @Override
    public ElementType getType() {
        return type;
    }
}
