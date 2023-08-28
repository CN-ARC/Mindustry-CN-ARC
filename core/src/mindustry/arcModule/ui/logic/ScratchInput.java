package mindustry.arcModule.ui.logic;

import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.util.Tmp;

public class ScratchInput {
    public static final float dragStartDistance = 20f;
    public static void addDraggingInput(ScratchElement e) {
        e.addListener(new InputListener() {
            float lastX, lastY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                e.toFront();
                e.selected = true;
                Vec2 v = e.localToParentCoordinates(Tmp.v1.set(x, y));
                lastX = v.x;
                lastY = v.y;
                ScratchController.ui.pane.setFlickScroll(false);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (ScratchController.dragging == e) {
                    ScratchElement sel = ScratchController.selected;
                    if (sel != null && sel.accept(e)) {
                        if (sel.child != null) {
                            ScratchController.ui.addElement(sel.child);
                            sel.child.setPosition(e.x + 10, e.y - 10);
                        }
                        e.asChild(sel);
                    }
                    ScratchController.dragging = ScratchController.selected = null;
                }
                e.selected = false;
                ScratchController.ui.pane.setFlickScroll(true);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (e.child != null && e.child.selected) return;
                e.toFront();
                Vec2 v = e.localToParentCoordinates(Tmp.v1.set(x, y));
                if (e.parent instanceof ScratchElement && ScratchController.dragging == null && Tmp.v2.set(v.x - lastX, v.y - lastY).len() < dragStartDistance) return;
                if (e.parent instanceof ScratchElement sel) {
                    sel.setChild(null);
                    ScratchController.ui.addElement(e);
                }
                ScratchController.dragging = e;
                e.touchable = Touchable.disabled;
                e.x += v.x - lastX;
                e.y += v.y - lastY;
                lastX = v.x;
                lastY = v.y;
                Element selected = ScratchController.ui.group.hit(e.x + e.getHeight() / 2, e.y + e.getHeight() / 2, true);
                if (selected instanceof ScratchElement sel) {
                    ScratchController.selected = sel;
                } else if (selected != null && selected != ScratchController.ui.group && selected.parent instanceof ScratchElement parent) {
                    ScratchController.selected = parent;
                } else {
                    ScratchController.selected = null;
                }
                e.touchable = Touchable.enabled;
            }
        });
    }
}
