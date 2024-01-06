package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ForkMiddle extends ForkComponent {
    public ForkMiddle(ForkBlock.ForkInfo info, Color c) {
        super(ScratchType.none, c, info);
    }

    @Override
    public ScratchBlock copy() {
        return null;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        c.row();
    }

    @Override
    public void draw() {

    }
}
