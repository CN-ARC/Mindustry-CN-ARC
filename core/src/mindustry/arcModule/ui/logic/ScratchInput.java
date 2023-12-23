package mindustry.arcModule.ui.logic;

import arc.Core;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.logic.blocks.FakeBlock;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

public class ScratchInput {
    public static final float dragStartDistance = 20f;
    public static final Vec2 tmp = new Vec2();
    public static final Vec2 tmp2 = new Vec2();
    public static ScratchTable cur = null;
    public static FakeBlock fake = new FakeBlock();
    public static void addDraggingInput(ScratchTable e) {
        e.addListener(new InputListener() {
            float lastX, lastY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (cur != null) return false;
                e.toFront();
                e.selected = true;
                Vec2 v = e.localToParentCoordinates(tmp.set(x, y));
                lastX = v.x;
                lastY = v.y;
                ScratchController.ui.pane.setFlickScroll(false);
                cur = e;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                cur = null;
                if (ScratchController.dragging == e) {
                    ScratchTable sel = ScratchController.selected;
                    if (sel != null && sel.accept(e)) {
                        ScratchTable oldChild = sel.child;
                        e.asChild(sel);
                        if (oldChild != null) {
                            ScratchController.ui.addElement(oldChild);
                            oldChild.setPosition(e.x + 10, e.y - 10);
                        }
                    }
                    if (e instanceof ScratchBlock block && (fake.linkTo != null || fake.linkFrom != null)) {
                        block.insertLinkBottom(fake);
                        fake.removeAndKeepLink();
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
                Vec2 v = e.localToParentCoordinates(tmp.set(x, y));
                if (((e.parent instanceof ScratchTable) || (e instanceof ScratchBlock b && b.type == ScratchType.block)) && ScratchController.dragging == null && Tmp.v1.set(v.x - lastX, v.y - lastY).len() < dragStartDistance) return;
                if (e.parent instanceof ScratchTable sel) {
                    e.localToAscendantCoordinates(ScratchController.ui.group, Tmp.v1.set(e.x, e.y));
                    float ox = Tmp.v1.x, oy = Tmp.v1.y;
                    sel.setChild(null);
                    ScratchController.ui.addElement(e);
                    e.setPosition(ox, oy);
                    e.x += v.x - lastX;
                    e.y += v.y - lastY;
                    v = e.localToParentCoordinates(tmp.set(x - v.x + lastX, y - v.y + lastY));
                    Core.scene.addTouchFocus(this, e, e, pointer, KeyCode.mouseLeft);
                } else {
                    if (e instanceof ScratchBlock b && b.type == ScratchType.block) b.unlink();
                    e.x += v.x - lastX;
                    e.y += v.y - lastY;
                }
                ScratchController.dragging = e;
                e.touchable = Touchable.disabled;
                lastX = v.x;
                lastY = v.y;
                Element selected = ScratchController.ui.group.hit(e.x + e.getHeight() / 2, e.y + e.getHeight() / 2, true);
                e.touchable = Touchable.enabled;
                if (selected != null && e instanceof ScratchBlock block && block.type == ScratchType.block &&
                        (selected instanceof ScratchBlock sb && sb.type == ScratchType.block ||
                                selected.parent instanceof ScratchBlock sb2 && sb2.type == ScratchType.block)) {
                    ScratchBlock b = (ScratchBlock) (selected instanceof ScratchBlock ? selected : selected.parent);
                    if (b.linkTo != e && b.linkFrom != e) {
                        fake.setReal(block);
                        if (b.dir == Align.top) {
                            fake.unlinkAll();
                            fake.insertLinkTop(b);
                        } else {
                            fake.unlinkAll();
                            fake.insertLinkBottom(b);
                        }
                        ScratchController.ui.group.addChild(fake);
                        fake.toBack();
                        return;
                    }
                }
                if (fake.linkTo != null || fake.linkFrom != null) fake.removeAndKeepLink();
                if (selected instanceof ScratchTable sel) {
                    ScratchController.selected = sel;
                } else if (selected != null && selected != ScratchController.ui.group && selected.parent instanceof ScratchTable parent) {
                    ScratchController.selected = parent;
                } else {
                    ScratchController.selected = null;
                }
            }
        });
    }
}
