package mindustry.arcModule.ui.logic.elements;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.util.Align;
import mindustry.arcModule.ui.logic.ElementType;
import mindustry.arcModule.ui.logic.ScratchController;
import mindustry.arcModule.ui.logic.ScratchElement;
import mindustry.arcModule.ui.logic.ScratchStyles;
import mindustry.ui.Styles;

public class InputElement extends ScratchElement {
    protected static TextField.TextFieldStyle style;
    protected static GlyphLayout prefSizeLayout = new GlyphLayout();
    private static final float minWidth = 40f;
    TextField field;
    Cell<TextField> cell;
    public InputElement() {
        this("");
    }
    public InputElement(String def) {
        field = new TextField(def, getStyle()) {
            @Override
            public Element hit(float x, float y, boolean touchable) {
                return ScratchController.dragging == null ? super.hit(x, y, touchable) : null;
            }
        };
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
    public void setChild(ScratchElement child) {
        this.child = child;
        if (child == null) {
            cell.setElement(field).left().pad(0, 10f, 0, 10f).width(20f);
        } else {
            cell.pad(0).width(0);
            cell.setElement(child);
        }
    }

    @Override
    public ElementType getType() {
        return ElementType.input;
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
    public Element hit(float x, float y, boolean touchable) {
        if (ScratchController.dragging != null) {
            return (!touchable || this.touchable == Touchable.enabled) && x >= -padValue && x <= width + padValue && y >= -padValue && y <= height + padValue ? this : null;
        }
        return super.hit(x, y, touchable);
    }

    @Override
    public boolean accept(ScratchElement e) {
        return e.getType() == ElementType.input || e.getType() == ElementType.condition;
    }

    private TextField.TextFieldStyle getStyle() {
        if (style == null) {
            style = new TextField.TextFieldStyle(Styles.defaultField);
            style.focusedBackground = null;
            style.disabledBackground = null;
            style.background = null;
            style.fontColor = Color.black;
        }
        return style;
    }
}
