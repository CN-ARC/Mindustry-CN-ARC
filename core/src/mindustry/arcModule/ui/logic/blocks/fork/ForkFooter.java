package mindustry.arcModule.ui.logic.blocks.fork;

import arc.graphics.Color;
import mindustry.arcModule.ui.logic.BlockInfo;
import mindustry.arcModule.ui.logic.ScratchType;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

public class ForkFooter extends ForkComponent {

    public ForkFooter(String name, ScratchType type, Color color, BlockInfo info) {
        super(name, type, color, info);
    }

    @Override
    public ScratchBlock copy() {
        return null;
    }
}
