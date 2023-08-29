package mindustry.arcModule.ui.logic.blockbase;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Gl;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
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
                se.cell(add(se));
            } else {
                add(e);
            }
        }
        if (extraPadding) {
            if (elem[0] instanceof ScratchElement se && se.getType() == ElementType.input) getCell(se).padLeft(addPadding + 10);
            if (elem[elem.length - 1] instanceof ScratchElement se && se.getType() == ElementType.input) getCell(se).padRight(addPadding + 10);
        }
        elements = elem;
    }

    @Override
    public ElementType getType() {
        return type;
    }
}
