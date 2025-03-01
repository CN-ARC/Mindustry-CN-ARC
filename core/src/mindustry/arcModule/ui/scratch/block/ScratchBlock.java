package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.block.fork.ForkComponent;
import mindustry.arcModule.ui.scratch.element.*;
import mindustry.ui.Styles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static mindustry.arcModule.ui.scratch.ScratchController.dragging;

public class ScratchBlock extends ScratchTable {
    public static final BlockInfo emptyInfo = new BlockInfo();
    public static boolean removing = false;
    public ScratchType type;
    public final BlockInfo info;
    public ScratchBlock linkTo, linkFrom;
    public byte dir = 0;
    public Seq<Element> elements = new Seq<>();
    public ScratchRunner.Task running;
    public boolean header;
    protected float minWidth = 0, minHeight = 0;

    public ScratchBlock(ScratchType type, Color color, BlockInfo info) {
        this(type, color, info, false);
    }

    public ScratchBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
        this.type = type;
        elemColor = color;
        this.info = info;
        construct();
        info.build(this);
        if (dragEnabled) ScratchInput.addDraggingInput(this);
    }

    public void construct() {
        init();
    }

    public void init() {
        minHeight = type == ScratchType.block ? 32 : 28;
        if (type == ScratchType.condition) margin(0, addPadding, 0, addPadding);
    }

    public Cell<ScratchTable> label(String str) {
        LabelElement l = new LabelElement(str);
        Cell<ScratchTable> c;
        l.cell(c = add(l));
        return c;
    }

    public Cell<ScratchTable> labelBundle(String str) {
        LabelElement l = new LabelElement(str, true);
        Cell<ScratchTable> c;
        l.cell(c = add(l));
        return c;
    }

    public void cond() {
        CondElement e = new CondElement();
        e.cell(add(e));
    }

    public void input() {
        input(false);
    }

    public void input(boolean num) {
        input(num, "");
    }

    public void input(boolean num, String def) {
        InputElement e = new InputElement(num, def);
        e.cell(add(e));
    }

    public void list(String[] list) {
        ListElement e = new ListElement(list);
        e.cell(add(e));
    }

    public ScratchBlock copy() {
        return copy(true);
    }

    public ScratchBlock copy(boolean drag) {
        ScratchBlock sb = new ScratchBlock(type, elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    public ScratchBlock copyTree(boolean add) {
        ScratchBlock top = copy();
        if (add) ScratchController.ui.group.addChild(top);
        if (getType() == ScratchType.block) {
            ScratchBlock from = linkFrom;
            ScratchBlock to = top;
            while (from != null) {
                ScratchBlock copy = from.copy();
                copy.linkTo(to);
                if (add) ScratchController.ui.group.addChild(copy);
                to = copy;
                from = from.linkFrom;
            }
        }
        return top;
    }

    public void copyChildrenValue(ScratchBlock target, boolean drag) {
        for (int i = 0; i < elements.size; i++) {
            Element child = elements.get(i);
            Element element = target.elements.get(i);
            if (child instanceof ScratchElement st1 && element instanceof ScratchElement st2) {
                st2.setElementValue(st1.getElementValue());
                if (st1.child != null) st2.setChild(st1.child.copy(drag));
            }
        }
    }

    public void cloneCopy(ScratchBlock target) {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        writeElements(new Writes(new DataOutputStream(tmp)));
        target.readElements(new Reads(new DataInputStream(new ByteArrayInputStream(tmp.toByteArray()))));
    }

    public void linkFrom(ScratchBlock source) {
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

    public void unlinkKeep() {
        boolean linked = linkFrom != null && linkTo != null;
        if (linked) {
            linkFrom.linkTo(linkTo);
        }
        if (linkFrom != null && linkTo == null) {
            linkFrom.x = x;
            linkFrom.y = y;
        }
        unlinkAll();
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
            while (b.linkTo != null && !(b.linkTo instanceof ForkComponent)) b = b.linkTo;
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
            case input -> ScratchDraw.drawInput(x, y, width, height, elemColor, false);
            case condition -> ScratchDraw.drawCond(x, y, width, height, elemColor, false, false);
            case block -> ScratchDraw.drawBlock(x, y, width, height, elemColor, false);
        }
    }

    public void drawDebug() {
        super.drawDebug();
        if (dragging.type == ScratchType.block && type == ScratchType.block) {
            Lines.stroke(1f);
            Draw.color(Color.gold.cpy().mulA(0.5f));
            Lines.rect(x, y - padValue * 2, width, padValue);
            if (linkTo == null || linkTo instanceof FakeBlock) {
                Draw.color(Color.blue.cpy().mulA(0.5f));
                Lines.rect(x, y + padValue, width, height + padValue * 2);
            }
        }
        if (dragging == this) {
            Draw.color(Color.gold);
            Fill.rect(x + 10, y + height / 2, 3, 3);
        }
    }

    public void linkUpdate(ScratchBlock target) {
        target.setPosition(x, y - target.getHeight());
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

    public float getTotalHeight() {
        ScratchBlock e = this;
        float height = 0;
        while (e != null) {
            height += e.getPrefHeight();
            e = e.linkFrom;
        }
        return height;
    }

    public void scheduleRun() {
        if (running != null) return;
        running = new ScratchRunner.Task(this);
        ScratchController.run(running);
    }

    public void insertRun() {
        ScratchController.insert(this);
    }

    public void prepareRun() {
    }

    public void run() {
        info.run(this);
    }

    public void finishRun() {
        running = null;
    }

    public boolean runFinished() {
        return true;
    }

    public void readElements(Reads r) {
        elements.each(e -> {
            if (e instanceof ScratchTable t) t.read(r);
        });
    }

    public void writeElements(Writes w) {
        elements.each(e -> {
            if (e instanceof ScratchTable t) t.write(w);
        });
    }

    public void actChain(float delta) {
        act(delta);
        if (linkFrom != null) linkFrom.actChain(delta);
    }

    public void buildMenu(Table t) {
        t.button("copy", Styles.nonet, () -> getTopBlock().copyTree(true).setPosition(x + 15, y - 15));
        t.row();
        t.button("delete", Styles.nonet, () -> {
            destroy();
            remove();
        });
    }

    @Override
    public void read(Reads r) {
        super.read(r);
        readPos(r);
        readElements(r);
    }

    @Override
    public void write(Writes w) {
        super.write(w);
        writePos(w);
        writeElements(w);
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return type == ScratchType.block && (dir == Align.top || !(block instanceof TriggerBlock));
    }

    @Override
    public Object getValue() {
        return info.getValue(elements);
    }

    @Override
    public ScratchType getType() {
        return type;
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (touchable && this.touchable != Touchable.enabled) return null;
        if (!(dragging != null && dragging.type == ScratchType.block) || dragging == linkTo || dragging == linkFrom) return super.hit(x, y, touchable);
        if (x >= 0 && x < width) {
            if (y >= -padValue * 2 && y < 0) {
                dir = Align.bottom;
                return this;
            }
            if ((linkTo == null || linkTo instanceof FakeBlock) && y >= height && y < height + padValue * 2 + (linkTo != null ? linkTo.getHeight() : 0)) {
                dir = Align.top;
                return this;
            }
        }
        dir = Align.bottom;
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
        if (linkTo != null) {
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

    @Override
    public boolean removeChild(Element element, boolean unfocus) {
        boolean success = super.removeChild(element, unfocus);
        if (success) elements.remove(element);
        return success;
    }

    @Override
    public float getPrefWidth() {
        return Math.max(super.getPrefWidth(), minWidth);
    }

    @Override
    public float getPrefHeight() {
        return Math.max(super.getPrefHeight(), minHeight);
    }

    public interface HoldInput {
        default boolean holding() {
            return false;
        }
    }
}
