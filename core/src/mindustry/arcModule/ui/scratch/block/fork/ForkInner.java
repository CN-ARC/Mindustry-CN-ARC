package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.block.ForkBlock;

public class ForkInner extends ForkHasChild {

    public ForkInner(Color c, BlockInfo info) {
        super(c, info);
    }

    public ForkInner(Color c, BlockInfo info, ForkPop pop) {
        super(c, info);
        this.pop = pop;
    }

    @Override
    public void drawBackground() {
        Color col = ((ForkBlock) parent).elemColor;
        ScratchStyles.drawBlockHeader(x, y, width, height, col);
        ScratchStyles.drawBlockInner(x, y, width, height);
        Draw.color(Tmp.c1.set(col).lerp(Color.black, 0.3f));
        Lines.stroke(1);
        drawBorderDirect(x, y, width, height);
        ScratchStyles.drawBorderBottom(x, y, width);
        drawLeftBorder();
        Lines.line(x, y + height, x + 10, y + height);
    }

}
