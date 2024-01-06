package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ForkHeader extends ForkComponent {
    public ForkHeader(ForkBlock.ForkInfo info, Color c) {
        super(ScratchType.none, c, info);
    }

    @Override
    public ScratchBlock copy() {
        return new ForkHeader(info, elemColor);
    }

    @Override
    public void drawChildren() {
        Color col = ((ForkBlock) parent).elemColor;
        ScratchStyles.drawBlockHeader(x, y, width, height, col);
        Fill.crect(x, y, width, height - 7);
        Draw.color(Tmp.c1.set(col).lerp(Color.black, 0.3f));
        Lines.beginLine();
        ScratchStyles.drawBlockBorderTop(x, y, width, height);
        Lines.endLine(true);
        super.drawChildren();
    }
}
