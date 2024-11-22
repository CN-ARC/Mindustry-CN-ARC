package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchType;

public class VariableBlock extends ScratchBlock {
    private static final BlockInfo info = new BlockInfo(b -> b.label("test"));

    public VariableBlock(Color color) {
        super(ScratchType.input, color, info);
    }

    public VariableBlock(Color color, boolean dragEnabled) {
        super(ScratchType.input, color, info, dragEnabled);
    }

    @Override
    public void init() {
        super.init();
        marginRight(addPadding * 3);
    }

    @Override
    public VariableBlock copy(boolean drag) {
        VariableBlock sb = new VariableBlock(elemColor, drag);
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
