package mindustry.arcModule.ui.scratch.element;

import arc.graphics.Color;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ClickListener;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import arc.util.pooling.Pools;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class InputElement extends ScratchElement implements ScratchBlock.HoldInput {
    protected static TextField.TextFieldStyle style;
    protected static GlyphLayout prefSizeLayout = new GlyphLayout();
    private static final float minWidth = 40;
    public TextField field;
    private final boolean num;
    private final Cell<TextField> cell;
    private final ClickListener listener;

    public InputElement(boolean num, String def) {
        field = new TextField(def, getStyle()) {
            @Override
            public Element hit(float x, float y, boolean touchable) {
                return ScratchController.dragging == null ? super.hit(x, y, touchable) : null;
            }
        };
        field.changed(() -> {
            ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class, ChangeListener.ChangeEvent::new);
            fire(changeEvent);
            Pools.free(changeEvent);
        });
        this.num = num;
        if (num) field.setFilter(TextField.TextFieldFilter.digitsOnly);
        field.setAlignment(Align.center);
        elemColor = Color.white;
        cell = add(field).left().width(minWidth).minHeight(23);
        field.changed(this::calcWidth);
        field.setProgrammaticChangeEvents(true);
        addListener(listener = new ClickListener());
        calcWidth();
    }

    private void calcWidth() {
        prefSizeLayout.setText(style.font, field.getText() + "  ");
        cell.width(Math.max(minWidth, prefSizeLayout.width));
        invalidateHierarchy();
    }

    @Override
    public void setChild(ScratchTable child) {
        this.child = child;
        if (child == null) {
            cell.setElement(field);
            field.change();
        } else {
            cell.pad(0).width(0);
            cell.setElement(child);
        }
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        c.pad(addPadding, 5, addPadding, 5);
    }

    @Override
    public ScratchType getType() {
        return ScratchType.input;
    }

    @Override
    public void drawChildren() {
        if (child == null) {
            ScratchDraw.drawInput(x, y, width, height, elemColor, ScratchController.selected == this);
        } else if (ScratchController.selected == this && accept(ScratchController.dragging)) {
            ScratchDraw.drawInputSelected(x, y, width, height);
        }
        super.drawChildren();
    }

    @Override
    public boolean accept(ScratchTable e) {
        return e.getType() == ScratchType.input || e.getType() == ScratchType.condition;
    }

    @Override
    public Object getValue() {
        if (child != null) return child.getValue();
        if (!num) return field.getText();
        try {
            return Double.parseDouble(field.getText());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public String getText() {
        return field == null ? "" : field.getText();
    }

    @Override
    public Object getElementValue() {
        return field == null ? null : field.getText();
    }

    @Override
    public void setElementValue(Object value) {
        if (field != null) field.setText((String) value);
    }

    private TextField.TextFieldStyle getStyle() {
        if (style == null) {
            style = new TextField.TextFieldStyle(Styles.defaultField);
            style.focusedBackground = null;
            style.disabledBackground = null;
            style.background = null;
            style.invalidBackground = null;
            style.font = Fonts.outline;
            style.fontColor = Color.gray;
        }
        return style;
    }

    @Override
    public ScratchElement copy() {
        InputElement e = new InputElement(num, field.getText());
        if (child instanceof ScratchBlock sb) sb.copy().asChild(e);
        return e;
    }

    @Override
    public boolean holding() {
        return !listener.isOver();
    }
}
