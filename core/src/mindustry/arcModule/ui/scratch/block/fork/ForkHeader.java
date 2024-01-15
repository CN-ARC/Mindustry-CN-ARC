package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;

public class ForkHeader extends ForkComponent {
    public ForkHeader(BlockInfo info, Color c) {
        super(ScratchType.none, c, info);
    }

    public void drawBorderDirect(float x, float y, float w, float h) {
        Lines.line(x + 10, y + h, x + 15, y + h - 7);
        Lines.line(x + 15, y + h - 7, x + 30, y + h - 7);
        Lines.line(x + 30, y + h - 7, x + 35, y + h);
        Lines.line(x + 35, y + h, x + w, y + h);
        Lines.line(x + w, y + h, x + w, y + 7);
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
    }

    @Override
    public void drawChildren() {
        super.drawChildren();
    }

    @Override
    public void copyTo(ForkComponent fork, boolean drag) {
        copyChildrenValue(fork, drag);
    }
}
