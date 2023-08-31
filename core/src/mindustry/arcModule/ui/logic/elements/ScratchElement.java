package mindustry.arcModule.ui.logic.elements;

import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Tmp;
import mindustry.arcModule.ui.logic.ScratchController;
import mindustry.arcModule.ui.logic.ScratchTable;

abstract public class ScratchElement extends ScratchTable {
    abstract public ScratchElement copy();

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (!hitable) return null;
        if (ScratchController.dragging != null) {
            if ((!touchable || this.touchable == Touchable.enabled) && x >= -padValue && x <= width + padValue && y >= -padValue && y <= height + padValue) {
                if (child != null) {
                    Vec2 v = child.parentToLocalCoordinates(Tmp.v1.set(x, y));
                    Element e = child.hit(v.x, v.y, touchable);
                    if (e != null) return e;
                }
                return this;
            }
            return null;
        }
        return super.hit(x, y, touchable);
    }
}
