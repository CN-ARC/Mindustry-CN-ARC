package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.TextField;
import arc.util.Align;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.block.FakeBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.arcModule.ui.scratch.element.ScratchElement;

import static arc.Core.input;
import static arc.Core.scene;
import static mindustry.arcModule.ui.scratch.ScratchController.*;

public class ScratchInput {
    public static final float dragStartDistance = 20f, dragMinDistance = 3f;
    public static final Vec2 v1 = new Vec2();
    public static final Vec2 v2 = new Vec2();
    public static ScratchBlock cur = null;
    public static FakeBlock fake = new FakeBlock();
    public static boolean dragged = false, menu = false, removing = false, checking = false;

    public static void addDraggingInput(ScratchBlock e) {
        e.addListener(new ScratchDragListener(e));
        e.addListener(new HandCursorListener());
    }

    public static void checkHit(float x, float y) {
        checking = true;
        Element hit = ui.group.hit(x, y, true);
        checking = false;
        if (dragging instanceof ScratchBlock block && block.type == ScratchType.block && hit instanceof ScratchTable t && t.acceptLink(block)) {
            ScratchBlock b = (ScratchBlock) (hit instanceof ScratchBlock ? hit : hit.parent);
            if (b.linkTo != dragging && b.linkFrom != dragging) {
                fake.setReal(block);
                if (b.dir == Align.top) {
                    fake.unlinkAll();
                    fake.insertLinkTop(b);
                } else {
                    fake.unlinkAll();
                    fake.insertLinkBottom(b);
                }
                ui.group.addChild(fake);
                fake.toBack();
                return;
            }
        }
        if (fake.linked()) fake.removeAndKeepLink();
        if (hit instanceof ScratchTable sel) {
            selected = sel;
        } else if (hit != null && hit != ui.group && hit.parent instanceof ScratchTable parent) {
            selected = parent;
        } else {
            selected = null;
        }
        if (selected != null && !selected.accept(dragging)) {
            selected = null;
        }
    }

    public static void addNewInput(ScratchBlock e) {
        e.addListener(new ScratchNewInputListener(e));
    }

    enum SLayer {
        group, overlay
    }

    static class ScratchDragListener extends InputListener {
        float lastX, lastY;
        SLayer layer = SLayer.group;
        ScratchBlock target;

        public ScratchDragListener(ScratchBlock target) {
            this.target = target;
        }

        public Vec2 checkMoved(Vec2 v, float x, float y, int pointer) {
            if (target.parent instanceof ScratchTable sel) {
                target.localToAscendantCoordinates(ui.group, Tmp.v1.set(target.x, target.y));
                float ox = Tmp.v1.x, oy = Tmp.v1.y;
                sel.setChild(null);
                ui.addElement(target);
                target.setPosition(ox, oy);
                target.x += v.x - lastX;
                target.y += v.y - lastY;
                v = target.localToParentCoordinates(v1.set(x - v.x + lastX, y - v.y + lastY));
                Core.scene.addTouchFocus(this, target, target, pointer, KeyCode.mouseLeft);
            } else {
                if (target.type == ScratchType.block) target.unlink();
                target.x += v.x - lastX;
                target.y += v.y - lastY;
            }
            return v;
        }

        public void checkPlace() {
            if (dragging == target) {
                ScratchTable sel = selected;
                if (sel != null && sel.accept(target)) {
                    ScratchTable oldChild = sel.child;
                    target.asChild(sel);
                    if (oldChild != null) {
                        ui.addElement(oldChild);
                        oldChild.setPosition(target.x + 10, target.y - 10);
                    }
                }
                if (fake.linked()) {
                    target.insertLinkTop(fake);
                    fake.removeAndKeepLink();
                }
                dragging = selected = null;
            }
        }

        public void checkClick() {
            if (!dragged && layer == SLayer.group && (scene.getKeyboardFocus() == null || !(scene.getKeyboardFocus().parent instanceof InputElement e && ((ClickListener) e.getListeners().find(l -> l instanceof ClickListener)).isOver()))) {
                ScratchBlock b = target.getTopBlock();
                if (menu) {
                    ui.showMenu(b, true);
                } else {
                    try {
                        Object o = b.getValue();
                        if (b.getType() != ScratchType.block) {
                            Log.info(o);
                            ui.showResult(b, String.valueOf(o));
                        }
                    } catch (Exception ex) {
                        Log.err(ex);
                        ui.showResult(b, ex.getMessage());
                    }
                }
            }
        }

        public void toOverlay(float x, float y, int pointer) {
            Vec2 pos = ScratchUI.oldPosToNewPos(ui.stack, target, ui.overlay);
            removing = true;
            ui.group.removeChild(target);
            ui.overlay.addChild(target);
            target.setPosition(pos.x, pos.y);
            Core.scene.addTouchFocus(this, target, target, pointer, KeyCode.mouseLeft);
            Vec2 v2 = target.localToParentCoordinates(v1.set(x, y));
            lastX = v2.x;
            lastY = v2.y;
            layer = SLayer.overlay;
        }

        public void toGroup() {
            Vec2 pos = ScratchUI.oldPosToNewPos(ui.stack, target, ui.group);
            removing = true;
            ui.overlay.removeChild(target);
            ui.group.addChild(target);
            target.setPosition(pos.x, pos.y);
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (cur != target && cur != null || Core.scene.getKeyboardFocus() instanceof TextField f && f.parent instanceof InputElement el && el.parent == target)
                return false;
            dragged = false;
            target.selected = true;
            Vec2 v = target.localToParentCoordinates(v1.set(x, y));
            lastX = v.x;
            lastY = v.y;
            ui.pane.setFlickScroll(false);
            cur = target;
            menu = button == KeyCode.mouseRight;
            removing = false;
            return true;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer) {
            if (target.child != null && target.child.selected) return;
            target.toFront();
            Vec2 v = target.localToParentCoordinates(v1.set(x, y));
            if (target.parent instanceof ScratchTable && dragging == null && Tmp.v1.set(v.x - lastX, v.y - lastY).len() < dragStartDistance || !dragged && Tmp.v1.set(v.x - lastX, v.y - lastY).len() < dragMinDistance)
                return;
            ScratchElement old = target.parent instanceof ScratchElement el ? el : null;
            if (layer == SLayer.group) toOverlay(x, y, pointer);
            dragged = true;
            if (old != null) old.setChild(null);
            v = checkMoved(v, x, y, pointer);
            dragging = target;
            lastX = v.x;
            lastY = v.y;
            Vec2 pos = ScratchUI.oldPosToNewPos(ui.stack, target, ui.group);
            target.touchable = Touchable.disabled;
            checkHit(pos.x + 10, pos.y + target.getHeight() / 2);
            target.touchable = Touchable.enabled;
            target.ensureParent();
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (removing) {
                removing = false;
                return;
            }
            if (layer == SLayer.overlay) toGroup();
            cur = null;
            checkPlace();
            target.selected = false;
            ui.pane.setFlickScroll(true);
            checkClick();
            ui.overlay.stageToLocalCoordinates(v1.set(input.mouseX(), input.mouseY()));
            if (((ClickListener) ui.blocksPane.getListeners().find(e -> e instanceof ClickListener)).isOver(ui.blocksPane, v1.x, v1.y))
                target.remove();
            layer = SLayer.group;
            target.ensureParent();
        }
    }

    static class ScratchNewInputListener extends InputListener {
        boolean dragged;
        ScratchBlock target;

        public ScratchNewInputListener(ScratchBlock target) {
            this.target = target;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            ui.blocksPane.setFlickScroll(false);
            ui.pane.setFlickScroll(false);
            return true;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer) {
            if (!dragged) {
                ScratchBlock b = target.copy();
                ui.overlay.addChild(b);
                Vec2 v = ScratchUI.oldPosToNewPos(ui.stack, target, ui.overlay);
                b.setPosition(v.x, v.y);
                ScratchDragListener sl = (ScratchDragListener) b.getListeners().find(l -> l instanceof ScratchDragListener);
                sl.touchDown(event, x, y, pointer, KeyCode.mouseLeft);
                sl.touchDragged(event, x, y, pointer);
                Core.scene.addTouchFocus(sl, b, b, pointer, KeyCode.mouseLeft);
            }
            dragged = true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (!dragged) {
                try {
                    Object o = target.getValue();
                    if (target.getType() != ScratchType.block) {
                        Log.info(o);
                        ui.showResult(target, String.valueOf(o));
                    }
                } catch (Exception ex) {
                    Log.err(ex);
                    ui.showResult(target, ex.getMessage());
                }
            }
            ui.blocksPane.setFlickScroll(true);
            ui.pane.setFlickScroll(true);
            dragged = false;
        }
    }
}
