package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ForkHeader extends ForkComponent {
    public ForkHeader(BlockInfo info, Color c) {
        super(ScratchType.none, c, info);
        touchable = Touchable.enabled;
    }

    public static void drawBorderDirect(float x, float y, float w, float h) {
        Lines.line(x + 10, y + h, x + 15, y + h - 7);
        Lines.line(x + 15, y + h - 7, x + 30, y + h - 7);
        Lines.line(x + 30, y + h - 7, x + 35, y + h);
        Lines.line(x + 35, y + h, x + w, y + h);
        Lines.line(x + w, y + h, x + w, y + 7);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return true;
    }

    @Override
    public void drawBackground() {
        Color col = ((ForkBlock) parent).elemColor;
        ScratchStyles.drawBlockHeader(x - 15, y, width + 15, height, col);
        Draw.color(col);
        ScratchStyles.drawBlockInner(x, y, width, height);
        Lines.beginLine();
        ScratchStyles.drawBlockBorderBottom(x, y);
        Lines.endLine();
        Draw.color(Tmp.c1.set(col).lerp(Color.black, 0.3f));
        drawBorderDirect(x - 15, y, width + 15, height);
        drawBorderBottom(x, y, width);
    }

    @Override
    public void drawChildren() {
        super.drawChildren();
        drawDebug();
    }

    @Override
    public void copyTo(ForkComponent fork, boolean drag) {
        copyChildrenValue(fork, drag);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (ScratchController.dragging == null || touchable && this.touchable != Touchable.enabled) return null;
        if (!(ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block) || ScratchController.dragging == linkTo) return hitDefault(x, y, touchable);
        if (x >= 0 && x < width && y >= -padValue * 2 && y < 0) {
            dir = Align.bottom;
            return this;
        }
        return hitDefault(x, y, touchable);
    }

    @Override
    public void linkUpdate(ScratchBlock target) {
        target.setPosition(parent.x + x, parent.y + y - target.getHeight() + 7);
    }
}
