package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.scene.event.ClickListener;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.element.LabelElement;

public class VariableBlock extends ScratchBlock {
    public ClickListener click;
    private String var;

    public VariableBlock(ScratchType type, Color color, BlockInfo info) {
        super(type, color, info);
    }

    public VariableBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
        super(type, color, info, dragEnabled);
    }

    public VariableBlock var(String name) {
        if (elements.size == 0) {
            add(new LabelElement(name));
        } else {
            ((LabelElement) elements.get(0)).label.setText(var = name);
        }
        return this;
    }

    @Override
    public void init() {
        super.init();
        margin(0, addPadding * 3, 0, addPadding * 3);
        addListener(click = new ClickListener());
    }

    @Override
    public void cell(Cell<? extends ScratchTable> c) {
        c.minHeight(defHeight).pad(0, 5, 0, 5);
    }

    @Override
    public VariableBlock copy(boolean drag) {
        VariableBlock sb = new VariableBlock(type, elemColor, info, drag).var(var);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void buildMenu(Table t) {
    }

    @Override
    public void drawBackground() {
        if (parent instanceof FunctionBlock) {
            switch (type) {
                case input -> ScratchDraw.drawInputBorderless(x, y, width, height, elemColor);
                case condition -> ScratchDraw.drawCond(x, y, width, height, elemColor, true, false);
            }
        } else {
            switch (type) {
                case input -> ScratchDraw.drawInput(x, y, width, height, elemColor, false);
                case condition -> ScratchDraw.drawCond(x, y, width, height, elemColor, false, false);
            }
        }
    }
}
