package mindustry.arcModule.ui.scratch.element;

import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public abstract class ScratchElement extends ScratchTable {
    abstract public ScratchElement copy();

    public Object getElementValue() {
        return null;
    }

    public void setElementValue(Object value) {
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (ScratchController.dragging != null) {
            if ((!touchable || this.touchable == Touchable.enabled) && x >= -padValue && x <= width + padValue && y >= -padValue && y <= height + padValue) {
                if (child != null) {
                    Vec2 v = child.parentToLocalCoordinates(Tmp.v1.set(x, y));
                    Element e = child.hit(v.x, v.y, touchable);
                    if (e != null) return e;
                }
                return hittable ? this : null;
            }
            return null;
        }
        return super.hit(x, y, touchable);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return parent instanceof ScratchTable t && t.acceptLink(block);
    }
}
