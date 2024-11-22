package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.element.LabelElement;

public class FunctionBlock extends ScratchBlock {
    private static final BlockInfo info = new BlockInfo(b -> {
        ((FunctionBlock) b).define = (LabelElement) b.labelBundle("define").padRight(addPadding * 6).get();
        b.label("test").padRight(addPadding * 3);
        ScratchInput.addNewInput(b.add(ScratchController.newBlock("variable", false)).get());
    });
    private static final Color innerColor = new Color(Color.packRgba(255, 77, 106, 255));
    private LabelElement define;

    public FunctionBlock(Color color) {
        super(ScratchType.block, color, info, false);
    }

    public FunctionBlock(Color color, boolean drag) {
        super(ScratchType.block, color, info, drag);
    }

    @Override
    public void init() {
        minHeight = 56;
        marginRight(addPadding * 6);
    }

    @Override
    public void drawBackground() {
        float dw = define.getWidth();
        ScratchDraw.drawFunctionBlock(x, y, dw + addPadding * 6, width, width - dw - addPadding * 9, elemColor, innerColor);
    }

    @Override
    public FunctionBlock copy(boolean drag) {
        FunctionBlock sb = new FunctionBlock(elemColor, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }
}
