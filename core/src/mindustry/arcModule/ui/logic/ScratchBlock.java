package mindustry.arcModule.ui.logic;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.logic.elements.CondElement;
import mindustry.arcModule.ui.logic.elements.InputElement;
import mindustry.arcModule.ui.logic.elements.LabelElement;
import mindustry.arcModule.ui.logic.elements.ScratchElement;

public class ScratchBlock extends ScratchTable {
    public ScratchType type;
    private final BlockInfo info;

    public ScratchBlock(String name, ScratchType type, Color color, BlockInfo info) {
        ScratchController.registerBlock(name, this);
        this.type = type;
        elemColor = color;
        this.info = info;
        info.build(this);
        if (children.size != 0) {
            if (children.get(0) instanceof InputElement e) {
                getCell(e).padLeft(addPadding + 10);
            }
            if (children.get(children.size - 1) instanceof InputElement e) {
                getCell(e).padRight(addPadding + 10);
            }
        }
        ScratchInput.addDraggingInput(this);
    }

    public LabelElement label(String str) {
        LabelElement l = new LabelElement(str);
        add(l);
        return l;
    }

    public CondElement cond() {
        CondElement e = new CondElement();
        e.cell(add(e));
        return e;
    }

    public InputElement input() {
        return input(false);
    }

    public InputElement input(boolean num) {
        return input(num, "");
    }

    public InputElement input(boolean num, String def) {
        InputElement e = new InputElement(def);
        Cell<ScratchTable> c;
        e.cell(c = add(e));
        if (num) {
            e.field.setFilter(TextField.TextFieldFilter.digitsOnly);
        }
        if (children.size == 1) {
            c.padLeft(addPadding + 10);
        }
        return e;
    }

    public ScratchBlock copy() {
        ScratchBlock sb = new ScratchBlock(name, type, elemColor, new BlockInfo());
        children.each(e -> {
            if (e instanceof ScratchElement se) {
                se.cell(sb.add(se.copy()));
            }
        });
        return sb;
    }

    @Override
    public Object getValue() {
        return info.getValue(children);
    }

    @Override
    public ScratchType getType() {
        return type;
    }

    @Override
    public void draw() {
        Draw.reset();
        switch (type) {
            case input -> ScratchStyles.drawInput(x, y, width, height, elemColor);
            case condition, conditionInner -> ScratchStyles.drawCond(x, y, width, height, elemColor);
            case block -> ScratchStyles.drawBlock(x, y, width, height, elemColor);
        }
        super.draw();
    }
}
