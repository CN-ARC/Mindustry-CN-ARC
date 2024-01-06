package mindustry.arcModule.ui.scratch.element;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchStyles;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class InputElement extends ScratchElement {
    protected static TextField.TextFieldStyle style;
    protected static GlyphLayout prefSizeLayout = new GlyphLayout();
    private static final float minWidth = 40f;
    public TextField field;
    private final boolean num;
    Cell<TextField> cell;

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
        field.setProgrammaticChangeEvents(true);
        addListener(new ClickListener());
    }

    @Override
    public void setChild(ScratchTable child) {
        this.child = child;
        if (child == null) {
            cell.setElement(field).left().pad(0, 10f, 0, 10f).width(20f);
            field.change();
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
    public void drawChildren() {
        if (child == null) {
            ScratchStyles.drawInput(x, y, width, height, elemColor);
        }
        super.drawChildren();
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
    public Object getElementValue() {
        return field.getText();
    }

    @Override
    public void setElementValue(Object value) {
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
