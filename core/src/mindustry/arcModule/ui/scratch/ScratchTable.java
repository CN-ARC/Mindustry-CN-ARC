package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public abstract class ScratchTable extends Table {
    public static final float defWidth = 50, defHeight = 30, addPadding = 3;
    protected static final float padValue = 25;
    protected final ObjectMap<Enum<ScratchEvents>, Seq<Cons<ScratchTable>>> events = new ObjectMap<>();
    public boolean selected = false, hittable = true;
    public ScratchBlock child = null;
    public Color elemColor = new Color(1, 1, 1, 1);

    {
        touchable = Touchable.enabled;
        align(Align.left);
    }

    public void addListener(Enum<ScratchEvents> type, Cons<ScratchTable> listener) {
        events.get(type, () -> new Seq<>(Cons.class)).add(listener);
    }

    public void fire(Enum<ScratchEvents> type) {
        Seq<Cons<ScratchTable>> listeners = events.get(type);

        if (listeners != null) {
            int len = listeners.size;
            Cons<ScratchTable>[] items = listeners.items;
            for (int i = 0; i < len; i++) {
                items[i].get(this);
            }
        }
    }

    public void asChild(ScratchTable parent) {
        parent.setChild((ScratchBlock) this);
    }

    public void setChild(ScratchBlock child) {
        if (this.child != null) removeChild(this.child);
        this.child = child;
        if (child != null) add(child);
    }

    public boolean accept(ScratchTable e) {
        return false;
    }

    public void cell(Cell<ScratchTable> c) {
        c.minWidth(defWidth).minHeight(defHeight).pad(addPadding);
    }

    public Cell<?> getCell() {
        if (parent instanceof ScratchTable se) {
            return se.getCell(this);
        }
        return null;
    }

    public Object getValue() {
        return null;
    }

    public void drawDebug() {
        Draw.color(Color.red.cpy().mulA(0.5f));
        Lines.stroke(1f);
        Lines.rect(x, y, width, height);
        Draw.color(ScratchController.selected == this ? Color.blue.cpy().mulA(0.5f) : Color.green.cpy().mulA(0.5f));
        if (ScratchController.dragging != null && hittable) {
            Lines.rect(x - padValue, y - padValue, width + padValue * 2, height + padValue * 2);
        } else {
            Lines.rect(x, y, width, height);
        }
    }

    public void read(Reads r) {
        boolean hasChild = r.bool();
        if (hasChild) {
            ScratchBlock b = ScratchController.newBlock(r.s());
            b.read(r);
            setChild(b);
        }
    }

    public void write(Writes w) {
        w.bool(child != null);
        if (child != null) {
            w.s(child.info.id);
            child.write(w);
        }
    }

    public void readPos(Reads r) {
        x = r.f();
        y = r.f();
    }

    public void writePos(Writes w) {
        w.f(x);
        w.f(y);
    }

    @Override
    public void toFront() {
        super.toFront();
        if (parent != null) parent.toFront();
    }

    abstract public boolean acceptLink(ScratchBlock block);

    abstract public ScratchType getType();
}
