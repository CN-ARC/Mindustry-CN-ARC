package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.Element;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ForkInner extends ForkComponent {

    public ForkInner(String name, ScratchType type, Color color, BlockInfo info) {
        super(type, color, info);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        return super.hit(x, y, touchable);
    }

    @Override
    public ScratchBlock copy() {
        return null;
    }
}
