package mindustry.arcModule.ui.scratch.element;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ClickListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.pooling.Pools;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.ui.Styles;

import java.util.Objects;

public class ListElement extends ScratchElement implements ScratchBlock.HoldInput {
    private final String[] lists;
    private int now;
    private Label l;
    private final ClickListener listener;
    public ListElement(String[] lists) {
        this(lists, 0);
    }
    public ListElement(String[] lists, int def) {
        this.lists = lists;
        now = def;
        add(new ListTable(t -> {
            t.add(l = new Label(lists[def]));
            t.add(new Element() {
                @Override
                public void draw() {
                    Draw.color(Color.white);
                    Fill.tri(x, y + height, x + width / 2, y, x + width, y + height);
                }
            }).size(16, 12);
        }));
        listener = clicked(ListElement.this::showList);
    }

    public int index() {
        return now;
    }

    public String get() {
        return lists[now];
    }

    public void set(int id) {
        now = id;
        l.setText(lists[id]);
        ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class, ChangeListener.ChangeEvent::new);
        fire(changeEvent);
        Pools.free(changeEvent);
    }

    public void set(String str) {
        for (int i = 0; i < lists.length; i++) {
            if (Objects.equals(lists[i], str)) {
                set(i);
                break;
            }
        }
    }

    @Override
    public Object getElementValue() {
        return now;
    }

    @Override
    public void setElementValue(Object value) {
        now = (int) value;
        l.setText(lists[now]);
    }

    @Override
    public ScratchType getType() {
        return ScratchType.list;
    }

    @Override
    public ScratchElement copy() {
        return null;
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.minSize(Float.NEGATIVE_INFINITY);
        elemColor = getBlock().elemColor.cpy().lerp(Color.black, 0.3f);
    }

    @Override
    public boolean holding() {
        return !listener.isOver();
    }

    public void showList() {
        ScratchController.ui.showPopup(this, t -> {
            Table inner = new Table();
            t.add(new ScrollPane(inner, Styles.smallPane)).with(s -> s.setScrollingDisabledX(true)).width(250).maxHeight(200);
            for (int i = 0; i < lists.length; i++) {
                String s = lists[i];
                int id = i;
                inner.add(new Label(s) {
                    final ClickListener c;
                    {
                        c = clicked(() -> {
                            t.remove();
                            set(id);
                        });
                    }
                    @Override
                    public void draw() {
                        if (c.isOver()) Styles.black5.draw(x - 10, y, width + 10, height);
                        super.draw();
                    }
                }).size(240, 40).padLeft(10).labelAlign(Align.left).row();
            }
        }, getBlock().elemColor);
    }

    private class ListTable extends Table {
        ListTable(Cons<Table> builder) {
            super(builder);
            touchable = Touchable.disabled;
            margin(3, 5, 3, 5);
        }

        @Override
        protected void drawBackground(float x, float y) {
            Draw.color(elemColor);
            float halfH = height / 2;
            Fill.circle(x + halfH, y + halfH, halfH);
            Fill.circle(x + width - halfH, y + height - halfH, halfH);
            Fill.crect(x + halfH, y, width - height, height);
        }
    }
}
