package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.func.Boolp;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.HandCursorListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.util.Align;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.block.FakeBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.ScratchElement;

import static arc.Core.input;
import static arc.Core.scene;
import static mindustry.arcModule.ui.scratch.ScratchController.*;

public class ScratchInput {
    public static final float dragStartDistance = 20, dragMinDistance = 3;
    public static final Vec2 v1 = new Vec2();
    public static final Vec2 v2 = new Vec2();
    public static ScratchBlock cur = null;
    public static FakeBlock fake = new FakeBlock();
    public static boolean dragged = false, menu = false, removing = false, checking = false;

    public static ScratchDragListener addDraggingInput(ScratchBlock e) {
        ScratchDragListener l;
        e.addListener(l = new ScratchDragListener(e));
        e.addListener(new HandCursorListener());
        return l;
    }

    public static ScratchNewInputListener addNewInput(ScratchBlock e) {
        ScratchNewInputListener l;
        e.addListener(l = new ScratchNewInputListener(e));
        e.addListener(new HandCursorListener());
        return l;
    }

    public static void checkHit(float x, float y) {
        checking = true;
        Element hit = ui.group.hit(x, y, true);
        checking = false;
        if (dragging.type == ScratchType.block && hit instanceof ScratchTable t && t.acceptLink(dragging)) {
            ScratchBlock b = (ScratchBlock) (hit instanceof ScratchBlock ? hit : hit.parent);
            ui.group.addChild(fake);
            if (b.linkTo != dragging && b.linkFrom != dragging) {
                fake.setReal(dragging);
                if (b.dir == Align.top) {
                    fake.unlinkKeep();
                    fake.insertLinkTop(b);
                } else {
                    fake.unlinkKeep();
                    fake.insertLinkBottom(b);
                }
                fake.toBack();
                return;
            }
        }
        if (fake.linked()) {
            fake.unlinkKeep();
            fake.remove();
        }
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

    public static boolean valid() {
        Element e = scene.hit(input.mouseX(), input.mouseY(), true);
        return (!(e instanceof ScratchBlock.HoldInput h) || h.holding()) && (e == null || !(e.parent instanceof ScratchBlock.HoldInput h2) || h2.holding());
    }

    enum SLayer {
        group, overlay
    }

    public static class ScratchDragListener extends InputListener {
        float lastX, lastY;
        SLayer layer = SLayer.group;
        ScratchBlock target;
        public Boolp enabled = null;

        public ScratchDragListener(ScratchBlock target) {
            this.target = target;
        }

        public Vec2 checkMoved(Vec2 v, float x, float y, int pointer) {
            if (target.parent instanceof ScratchTable sel) {
                target.localToAscendantCoordinates(ui.group, Tmp.v1.set(target.x, target.y));
                float ox = Tmp.v1.x, oy = Tmp.v1.y;
                sel.setChild(null);
                ui.addBlock(target);
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
                    ScratchBlock oldChild = sel.child;
                    target.asChild(sel);
                    if (oldChild != null) {
                        ui.addBlock(oldChild);
                        oldChild.setPosition(target.x + 10, target.y - 10);
                    }
                }
                if (fake.linked()) {
                    target.insertLinkTop(fake);
                    fake.unlinkKeep();
                    fake.remove();
                }
                selected = null;
                dragging = null;
            }
        }

        public void checkClick() {
            if (!dragged && layer == SLayer.group && valid()) {
                ScratchBlock b = target.getTopBlock();
                if (menu) {
                    ui.showMenu(b);
                } else {
                    run(b);
                }
            }
        }

        public static void run(ScratchBlock b) {
            try {
                if (b.getType() == ScratchType.block) {
                    b.scheduleRun();
                } else {
                    Object o = b.getValue();
                    Log.info(o);
                    ui.showResult(b, String.valueOf(o));
                }
            } catch (Exception ex) {
                Log.err(ex);
                ui.showResult(b, ex.getMessage());
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
            if (enabled != null && !enabled.get() || cur != target && cur != null || Core.scene.getKeyboardFocus() instanceof ScratchBlock.HoldInput f && f.holding())
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
            ui.overlay.stageToLocalCoordinates(Tmp.v1.set(input.mouseX(), input.mouseY()));
            float tx = Tmp.v1.x, ty = Tmp.v1.y;
            if (0 < tx && ui.overlay.y < ty && ui.blocksPane.getWidth() + ui.typesPane.getWidth() > tx && ui.overlay.getHeight() > ty) {
                target.destroy();
                target.remove();
                return;
            }
            layer = SLayer.group;
            target.ensureParent();
        }
    }

    public static class ScratchNewInputListener extends InputListener {
        boolean dragged;
        ScratchBlock target;
        public Boolp enabled = null;

        public ScratchNewInputListener(ScratchBlock target) {
            this.target = target;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (enabled != null && !enabled.get() || Core.scene.getKeyboardFocus() instanceof ScratchBlock.HoldInput f && f.holding()) return false;
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
                b.setPosition(Mathf.round(v.x), Mathf.round(v.y));
                ScratchDragListener sl = (ScratchDragListener) b.getListeners().find(l -> l instanceof ScratchDragListener);
                sl.touchDown(event, x, y, pointer, KeyCode.mouseLeft);
                sl.touchDragged(event, x, y, pointer);
                Core.scene.addTouchFocus(sl, b, b, pointer, KeyCode.mouseLeft);
            }
            dragged = true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (!dragged && valid()) ScratchDragListener.run(target);
            ui.blocksPane.setFlickScroll(true);
            ui.pane.setFlickScroll(true);
            dragged = false;
        }
    }
}
