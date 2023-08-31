package mindustry.arcModule.ui.logic.blocks.fork;

import arc.graphics.Color;
import arc.scene.Element;
import mindustry.arcModule.ui.logic.BlockInfo;
import mindustry.arcModule.ui.logic.ScratchType;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

public class ForkInner extends ForkComponent {

    public ForkInner(String name, ScratchType type, Color color, BlockInfo info) {
        super(name, type, color, info);
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
