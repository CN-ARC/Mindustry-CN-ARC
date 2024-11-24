package mindustry.arcModule.ui.scratch.element;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ClickListener;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class InputElement extends ScratchElement implements ScratchBlock.HoldInput {
    protected static TextField.TextFieldStyle style;
    protected static GlyphLayout prefSizeLayout = new GlyphLayout();
    private static final float minWidth = 40;
    public TextField field;
    private final boolean num;
    private final Cell<TextField> cell;
    private final ClickListener listener;

    public InputElement(boolean num, String def) {
        field = new HoldTextField(def, ScratchStyles.clearField);
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
        calcWidth();
        addListener(listener = new ClickListener());
    }

    private void calcWidth() {
        if (child != null) return;
        prefSizeLayout.setText(field.getStyle().font, field.getText() + "  ");
        cell.width(Math.max(minWidth, prefSizeLayout.width));
        invalidateHierarchy();
    }

    @Override
    public void setChild(ScratchBlock child) {
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
        } catch (NumberFormatException e) {
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

    @Override
    public ScratchElement copy() {
        InputElement e = new InputElement(num, field.getText());
        if (child != null) child.copy().asChild(e);
        return e;
    }

    @Override
    public void read(Reads r) {
        super.read(r);
        field.setText(r.str());
    }

    @Override
    public void write(Writes w) {
        super.write(w);
        w.str(field.getText());
    }

    @Override
    public boolean holding() {
        return Core.scene.getKeyboardFocus() == field && listener.isOver();
    }

    private class HoldTextField extends TextField implements ScratchBlock.HoldInput {
        public HoldTextField(String text, TextFieldStyle style) {
            super(text, style);
        }

        @Override
        public Element hit(float x, float y, boolean touchable) {
            return ScratchController.dragging == null ? super.hit(x, y, touchable) : null;
        }

        @Override
        public boolean holding() {
            return InputElement.this.holding();
        }
    }
}
