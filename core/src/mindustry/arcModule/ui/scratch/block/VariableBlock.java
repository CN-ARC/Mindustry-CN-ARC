package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.scene.event.ClickListener;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.element.LabelElement;

public class VariableBlock extends ScratchBlock {
    public ClickListener click;
    private String var;

    public VariableBlock(Color color, BlockInfo info) {
        super(ScratchType.input, color, info);
    }

    public VariableBlock(Color color, BlockInfo info, boolean dragEnabled) {
        super(ScratchType.input, color, info, dragEnabled);
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
    public VariableBlock copy(boolean drag) {
        VariableBlock sb = new VariableBlock(elemColor, info, drag).var(var);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void drawBackground() {
        if (parent instanceof FunctionBlock) {
            ScratchDraw.drawInputBorderless(x, y, width, height, elemColor);
        } else {
            ScratchDraw.drawInput(x, y, width, height, elemColor, false);
        }
    }
}
