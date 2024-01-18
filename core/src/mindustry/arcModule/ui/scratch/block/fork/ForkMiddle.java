package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;

public class ForkMiddle extends ForkComponent {//TODO Replace to Cell
    public ForkMiddle(Color c, int id) {
        super(ScratchType.none, c, emptyInfo, id);
    }

    @Override
    public ForkComponent copy(boolean drag) {
        return null;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.minHeight(20).padLeft(0);
    }

    @Override
    public void draw() {
        super.draw();
        drawDebug();
    }
}
