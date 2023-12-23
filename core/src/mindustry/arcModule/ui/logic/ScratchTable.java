package mindustry.arcModule.ui.logic;

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

public abstract class ScratchTable extends Table {
    public static float defHeight = 30f, addPadding = 10f, defWidth = 50f;
    protected static final float padValue = 30f;
    protected final ObjectMap<Enum<ScratchEvents>, Seq<Cons<ScratchTable>>> events = new ObjectMap<>();
    public boolean selected = false, hittable = true;
    public ScratchTable child = null;
    protected Color elemColor = new Color(1, 1, 1, 1);
    public Color tmpColor;

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

    public Object getValue() {
        return null;
    }

    public void asChild(ScratchTable parent) {
        parent.setChild(this);
    }

    public void setChild(ScratchTable child) {
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

    public void setValue(Object value) {
    }

    @Override
    public void draw() {
        super.draw();
        Draw.color(Color.red.cpy().mulA(0.5f));
        Lines.stroke(1f);
        Lines.rect(x, y, width, height);
        Draw.color(ScratchController.selected == this ? Color.blue.cpy().mulA(0.5f) : Color.green.cpy().mulA(0.5f));
        if (ScratchController.dragging != null && hittable) {
            Lines.rect(x - padValue, y - padValue, width + padValue * 2, height + padValue * 2);
        } else {
            Lines.rect(x, y, width, height);
        }
        Draw.reset();
    }

    @Override
    public void toFront() {
        super.toFront();
        parent.toFront();
    }

    abstract public ScratchType getType();
}
