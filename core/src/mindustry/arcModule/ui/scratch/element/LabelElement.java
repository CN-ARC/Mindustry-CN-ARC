package mindustry.arcModule.ui.scratch.element;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;

public class LabelElement extends ScratchElement {
    private final Label l;

    public LabelElement(String str) {
        add(l = new Label(str));
        hittable = false;
    }

    public LabelElement(String str, boolean bundle) {
        this(bundle ? ScratchController.getLocalized("elem." + str + ".name") : str);
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        if (parent.getChildren().size == 1) c.padLeft(addPadding * 3);
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }

    @Override
    public ScratchElement copy() {
        return new LabelElement(l.getText().toString());
    }

    @Override
    public void read(Reads r) {
    }

    @Override
    public void write(Writes w) {
    }
}
