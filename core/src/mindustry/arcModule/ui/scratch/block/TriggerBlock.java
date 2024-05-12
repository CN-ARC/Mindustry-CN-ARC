package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchType;

import static mindustry.arcModule.ui.scratch.ScratchController.dragging;

public class TriggerBlock extends ScratchBlock {

    public TriggerBlock(Color color, BlockInfo info) {
        this(color, info, false);
    }

    public TriggerBlock(Color color, BlockInfo info, boolean dragEnabled) {
        super(ScratchType.block, color, info, dragEnabled);
    }

    @Override
    public void init() {
        minWidth = 64;
        minHeight = 50;
        margin(10, 0, 0, 10);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (touchable && this.touchable != Touchable.enabled) return null;
        if (!(dragging != null && dragging.type == ScratchType.block) || dragging == linkFrom) return super.hit(x, y, touchable);
        if (x >= 0 && x < width && y >= -padValue * 2 && y < 0) {
            dir = Align.bottom;
            return this;
        }
        dir = Align.bottom;
        return hitDefault(x, y, touchable);
    }

    @Override
    public TriggerBlock copy(boolean drag) {
        TriggerBlock sb = new TriggerBlock(elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void drawBackground() {
        ScratchDraw.drawTriggerBlock(x, y, width, elemColor, false);
    }
}
