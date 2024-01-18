package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.element.CondElement;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.arcModule.ui.scratch.element.ScratchElement;

public class ScratchBlock extends ScratchTable {
    public static boolean removing = false;
    public ScratchType type;
    public final BlockInfo info;
    public ScratchBlock linkTo, linkFrom;
    public byte dir = 0;
    public Seq<Element> elements = new Seq<>();

    public ScratchBlock(ScratchType type, Color color, BlockInfo info) {
        this(type, color, info, false);
    }

    public ScratchBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
        this(type, color, info, dragEnabled, true);
    }

    public ScratchBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled, boolean fill) {
        this.type = type;
        elemColor = color;
        this.info = info;
        info.build(this);
        if (children.size != 0) {
            if (children.get(0) instanceof InputElement e) {
                getCell(e).padLeft(addPadding + 10);
            }
            if (children.get(children.size - 1) instanceof InputElement e) {
                getCell(e).padRight(addPadding + 10);
            }
        }
        if (dragEnabled) ScratchInput.addDraggingInput(this);
        if (fill) add().minHeight(40);
    }

    public void label(String str) {
        LabelElement l = new LabelElement(str);
        l.cell(add(l));
    }

    public void cond() {
        CondElement e = new CondElement();
        e.cell(add(e));
    }

    public void input() {
        input(InputElement.InputType.string);
    }

    public void input(InputElement.InputType type) {
        input(type, "");
    }

    public void input(InputElement.InputType type, String def) {
        InputElement e = new InputElement(type, def);
        Cell<ScratchTable> c;
        e.cell(c = add(e));
        if (children.size == 1) {
            c.padLeft(addPadding + 10);
        }
    }

    public ScratchBlock copy() {
        return copy(true);
    }

    public ScratchBlock copy(boolean drag) {//TODO Move copy tree function from menu#copy
        ScratchBlock sb = new ScratchBlock(type, elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    public void copyChildrenValue(ScratchBlock target, boolean drag) {
        for (int i = 0; i < elements.size; i++) {
            Element child = elements.get(i);
            Element element = target.elements.get(i);
            if (child instanceof ScratchElement st1 && element instanceof ScratchElement st2) {
                st2.setElementValue(st1.getElementValue());
                if (st1.child instanceof ScratchBlock sb2) st2.setChild(sb2.copy(drag));
            }
        }
    }

    public void linkFrom(@Nullable ScratchBlock source) {
        linkFrom = source;
    }

    public void linkTo(ScratchBlock target) {
        if (linkTo != null) unlink();
        target.linkFrom(this);
        linkTo = target;
    }

    public void insertLinkTop(ScratchBlock before) {
        if (before.linkTo != null) {
            linkTo(before.linkTo);
        } else {
            x = before.x;
            y = before.y;
        }
        ScratchBlock l = this;
        while (l.linkFrom != null) l = l.linkFrom;
        before.linkTo(l);
    }

    public void insertLinkBottom(ScratchBlock after) {
        ScratchBlock target = after.linkFrom;
        linkTo(after);
        ScratchBlock l = this;
        while (l.linkFrom != null) l = l.linkFrom;
        if (target != null) target.linkTo(l);
    }

    public void removeAndKeepLink() {
        boolean linked = linkFrom != null && linkTo != null;
        if (linked) {
            linkFrom.linkTo(linkTo);
        }
        if (linkFrom != null && linkTo == null) {
            linkFrom.x = x;
            linkFrom.y = y;
        }
        unlinkAll();
        remove();
    }

    public void unlink() {
        if (linkTo == null) return;
        if (linkTo.linkFrom == this) linkTo.linkFrom(null);
        linkTo = null;
    }

    public void unlinkFrom() {
        if (linkFrom == null) return;
        if (linkFrom.linkTo == this) linkFrom.unlink();
    }

    public void unlinkAll() {
        unlink();
        unlinkFrom();
    }

    public ScratchBlock getTopBlock() {
        ScratchBlock b = this;
        if (getType() == ScratchType.block) {
            while (b.linkTo != null) b = b.linkTo;
        } else {
            while (b.parent instanceof ScratchElement se) b = (ScratchBlock) se.parent;
        }
        return b;
    }

    public void removeLinked() {
        ScratchBlock b = linkFrom;
        while (b != null) {
            b.remove();
            b = b.linkFrom;
        }
    }

    public boolean linked() {
        return linkTo != null || linkFrom != null;
    }

    public void drawBackground() {
        switch (type) {
            case input -> ScratchStyles.drawInput(x, y, width, height, elemColor, false);
            case condition -> ScratchStyles.drawCond(x, y, width, height, elemColor, false);
            case block -> ScratchStyles.drawBlock(x, y, width, height, elemColor, false);
        }
    }

    public void drawDebug() {
        super.drawDebug();
        if (ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block && type == ScratchType.block) {
            Lines.stroke(1f);
            Draw.color(Color.gold.cpy().mulA(0.5f));
            Lines.rect(x, y - padValue * 2, width, padValue);
            if (linkTo == null || linkTo instanceof FakeBlock) {
                Draw.color(Color.blue.cpy().mulA(0.5f));
                Lines.rect(x, y + padValue, width, height + padValue * 2);
            }
        }
    }

    public void linkUpdate(ScratchBlock target) {
        target.setPosition(x, y - target.getHeight() + 7);
    }

    public Element hitDefault(float x, float y, boolean touchable) {
        return super.hit(x, y, touchable);
    }

    public void ensureParent() {
        if (getType() == ScratchType.block) {
            ScratchBlock b = linkFrom;
            if (b != null && b.parent == parent) return;
            while (b != null) {
                b.setParent(this);
                b = b.linkFrom;
            }
        }
    }

    public void setParent(ScratchBlock target) {
        parent.removeChild(this);
        target.parent.addChild(this);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return type == ScratchType.block;
    }

    @Override
    public Object getValue() {
        if (type == ScratchType.block) {
            ScratchBlock b = this;
            while (b != null) {
                b.info.getValue(b.elements);
                b = b.linkFrom;
            }
            return null;
        }
        return info.getValue(elements);
    }

    @Override
    public ScratchType getType() {
        return type;
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (touchable && this.touchable != Touchable.enabled) return null;
        if (!(ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block) || ScratchController.dragging == linkTo || ScratchController.dragging == linkFrom) return super.hit(x, y, touchable);
        if (x >= 0 && x < width) {
            if (y >= -padValue * 2 && y < 0) {
                dir = Align.bottom;
                return this;
            }
            if ((linkTo == null || linkTo instanceof FakeBlock) && y >= height + padValue && y < height + padValue * 2 + (linkTo != null ? linkTo.getHeight() : 0)) {
                dir = Align.top;
                return this;
            }
        }
        return super.hit(x, y, touchable);
    }

    @Override
    public void drawChildren() {
        drawBackground();
        super.drawChildren();
        //drawDebug();
    }

    @Override
    public void act(float delta) {
        if (linkTo == null) {
            ScratchBlock b = this;
            do {
                b.toFront();
                b = b.linkFrom;
            } while (b != null);
        } else {
            linkTo.linkUpdate(this);
        }
        super.act(delta);
    }

    @Override
    public boolean remove() {
        if (!removing) {
            removing = true;
            removeLinked();
            removing = false;
        }
        if (parent != null) return parent.removeChild(this);
        return false;
    }

    @Override
    public void addChild(Element actor) {
        super.addChild(actor);
        elements.add(actor);
    }
}
