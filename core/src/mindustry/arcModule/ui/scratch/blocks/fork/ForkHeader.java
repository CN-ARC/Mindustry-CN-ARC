package mindustry.arcModule.ui.scratch.blocks.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.blocks.ForkBlock;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;

public class ForkHeader extends ForkComponent {
    public ForkHeader(ForkBlock.ForkInfo info, Color c) {
        super(null, ScratchType.none, c, info);
    }

    @Override
    public ScratchBlock copy() {
        return new ForkHeader(info, elemColor);
    }

    @Override
    public void draw() {
        ScratchStyles.drawBlockHeader(x, y, width, height, elemColor);
        Fill.crect(x, y, width, height - 7);
        Lines.beginLine();
        ScratchStyles.drawBlockBorderTop(x, y, width, height);
        Lines.endLine(true);
        drawSuper();
    }
}
