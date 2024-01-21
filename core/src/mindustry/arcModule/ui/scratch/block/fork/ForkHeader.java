package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.block.ForkBlock;

public class ForkHeader extends ForkHasChild {
    public ForkHeader(Color c, BlockInfo info) {
        super(c, info);
    }

    public static void drawBorderDirect(float x, float y, float w, float h) {
        Lines.line(x + 10, y + h, x + 15, y + h - 7);
        Lines.line(x + 15, y + h - 7, x + 30, y + h - 7);
        Lines.line(x + 30, y + h - 7, x + 35, y + h);
        Lines.line(x + 35, y + h, x + w, y + h);
        Lines.line(x + w, y + h, x + w, y);
    }

    @Override
    public void drawBackground() {
        Color col = ((ForkBlock) parent).elemColor;
        ScratchStyles.drawBlockHeader(x - 15, y, width + 15, height, col);
        Draw.color(col);
        ScratchStyles.drawBlockInner(x, y, width, height);
        Lines.beginLine();
        Lines.endLine();
        Draw.color(Tmp.c1.set(col).lerp(Color.black, 0.3f));
        drawBorderDirect(x - 15, y, width + 15, height);
        drawBorderBottom(x, y, width);
        drawLeftBorder();
        Lines.line(x - 15, y + height, x - 5, y + height);
    }

    @Override
    public void drawChildren() {
        super.drawChildren();
        //drawDebug();
    }
}
