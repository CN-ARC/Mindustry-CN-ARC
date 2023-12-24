package mindustry.arcModule.ui.scratch.elements;

import arc.func.Cons;
import arc.scene.ui.Label;
import mindustry.arcModule.ui.scratch.ScratchType;

public class LabelElement extends ScratchElement {
    private final Label l;

    {
        hittable = false;
    }

    public LabelElement(String str) {
        add(l = new Label(str));
    }

    @Override
    public void getValue(Cons<Object> callback) {
        callback.get(l.getText());
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
