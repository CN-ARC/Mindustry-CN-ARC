package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.block.ForkBlock;

public class ForkFooter extends ForkComponent {

    public ForkFooter() {
        super(emptyInfo);
        touchable = Touchable.enabled;
    }

    @Override
    public void drawChildren() {
        Color col = ((ForkBlock) parent).elemColor;
        ScratchDraw.drawBlockHeader(x, y, width, height, col);
        ScratchDraw.drawBlockInner(x - 15, y, width + 15, height);
        Draw.color(Tmp.c1.set(col).lerp(Color.black, 0.3f));
        Lines.stroke(1);
        Lines.beginLine();
        ScratchDraw.drawBlockBorderTop(x, y, width, height);
        ScratchDraw.drawBlockBorderBottom(x - 15, y);
        Lines.endLine();
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.minHeight(15);
        margin(0);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        return hitDefault(x, y, touchable);
    }
}
