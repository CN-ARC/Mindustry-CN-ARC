package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ForkFooter extends ForkComponent {

    public ForkFooter(String name, ScratchType type, Color color, BlockInfo info) {
        super(type, color, info);
    }

    @Override
    public ScratchBlock copy() {
        return null;
    }
}
