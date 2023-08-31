package mindustry.arcModule.ui.logic.blocks.fork;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.logic.BlockInfo;
import mindustry.arcModule.ui.logic.ScratchTable;
import mindustry.arcModule.ui.logic.ScratchType;
import mindustry.arcModule.ui.logic.blocks.ForkBlock;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

public class ForkMiddle extends ForkComponent {
    public ForkMiddle(ForkBlock.ForkInfo info, Color c) {
        super(null, ScratchType.none, c, info);
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
