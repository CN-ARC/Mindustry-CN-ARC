package mindustry.arcModule.ui.scratch.elements;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class InputElement extends ScratchElement {
    protected static TextField.TextFieldStyle style;
    protected static GlyphLayout prefSizeLayout = new GlyphLayout();
    private static final float minWidth = 40f;
    public TextField field;
    private final boolean num;
    Cell<TextField> cell;
    public InputElement() {
        this(false, "");
    }
    public InputElement(boolean num, String def) {
        super();
        field = new TextField(def, getStyle()) {
            @Override
            public Element hit(float x, float y, boolean touchable) {
                return ScratchController.dragging == null ? super.hit(x, y, touchable) : null;
            }
        };
        this.num = num;
        if (num) field.setFilter(TextField.TextFieldFilter.digitsOnly);
        field.setAlignment(Align.center);
        elemColor = Color.white;
        cell = add(field).left().pad(0, 10f, 0, 10f).width(20f);
        field.changed(() -> {
            prefSizeLayout.setText(style.font, field.getText() + " ");
            cell.width(Math.max(minWidth, prefSizeLayout.width));
            invalidateHierarchy();
        });
    }

    @Override
    public void setChild(ScratchTable child) {
        this.child = child;
        if (child == null) {
            cell.setElement(field).left().pad(0, 10f, 0, 10f).width(20f);
        } else {
            cell.pad(0).width(0);
            cell.setElement(child);
        }
    }

    @Override
    public ScratchType getType() {
        return ScratchType.input;
    }

    @Override
    public void draw() {
        if (child == null) {
            ScratchStyles.drawInput(x, y, width, height, elemColor);
        }
        super.draw();
        Draw.reset();
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

    @Override
    public void getValue(Cons<Object> callback) {
        if (child != null) {
            child.getValue(callback);
            return;
        }
        if (!num) {
            callback.get(field.getText());
            return;
        }
        try {
            callback.get(Double.parseDouble(field.getText()));
        } catch (Exception e) {
            callback.get(Double.NaN);
        }
    }

    @Override
    public void setValue(Object value) {
        field.setText((String) value);
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
}
