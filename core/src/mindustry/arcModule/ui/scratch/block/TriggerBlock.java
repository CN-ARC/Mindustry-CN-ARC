package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchType;

public class TriggerBlock extends ScratchBlock {
    private static TextureRegion circle = null;

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
        if (!(ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block) || ScratchController.dragging == linkTo || ScratchController.dragging == linkFrom) return super.hit(x, y, touchable);
        if (x >= 0 && x < width && y >= -padValue * 2 && y < 0) {
            dir = Align.bottom;
            return this;
        }
        dir = Align.bottom;
        return super.hitDefault(x, y, touchable);
    }

    @Override
    public TriggerBlock copy(boolean drag) {
        TriggerBlock sb = new TriggerBlock(elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void drawBackground() {
        if (circle == null) circle = Core.atlas.find("circle");
        int dark = Tmp.c1.set(elemColor).lerp(Color.black, 0.3f).rgba();
        Draw.color(dark);
        Draw.rect(circle, x + 31, y + 31, 64, 36);
        Draw.color(elemColor);
        Draw.rect(circle, x + 31, y + 31, 62, 34);
        ScratchStyles.drawBlockInner(x, y, width, 40);
        Draw.color(dark);
        Lines.stroke(1);
        ScratchStyles.drawBorderBottom(x, y, width);
        Lines.line(x, y, x, y + 33);
        Lines.line(x + 64, y + 33, x + width, y + 33);
        Lines.line(x + width, y + 33, x + width, y);
    }
}
