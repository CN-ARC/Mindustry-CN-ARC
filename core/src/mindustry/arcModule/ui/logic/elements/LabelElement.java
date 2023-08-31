package mindustry.arcModule.ui.logic.elements;

import arc.scene.ui.Label;
import mindustry.arcModule.ui.logic.ScratchType;

public class LabelElement extends ScratchElement {
    private final Label l;

    {
        hitable = false;
    }

    public LabelElement(String str) {
        add(l = new Label(str));
    }

    @Override
    public Object getValue() {
        return l.getText();
    }

    @Override
    public void setValue(Object value) {
        l.setText((String) value);
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }

    @Override
    public ScratchElement copy() {
        return new LabelElement(l.getText().toString());
    }
}
