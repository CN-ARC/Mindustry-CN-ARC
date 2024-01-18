package mindustry.arcModule.ui.scratch.block.fork;

import arc.graphics.Color;
import arc.scene.Element;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchType;

public class ForkInner extends ForkComponent {

    public ForkInner(ScratchType type, Color color, BlockInfo info, int id) {
        super(type, color, info, id);
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        return super.hit(x, y, touchable);
    }

    @Override
    public ForkComponent copy(boolean drag) {
        return null;
    }
}
