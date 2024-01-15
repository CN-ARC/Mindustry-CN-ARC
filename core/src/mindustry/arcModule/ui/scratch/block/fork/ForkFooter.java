package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchType;

public class ForkFooter extends ForkComponent {

    public ForkFooter(ScratchType type, Color color, BlockInfo info) {
        super(type, color, info);
    }

    @Override
    public ForkComponent copy(boolean drag) {
        return null;
    }
}
