package mindustry.arcModule.ui.scratch.element;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.ScratchUI;

public class LabelElement extends ScratchElement {
    public final Label label;

    public LabelElement(String str) {
        add(label = new Label(str));
        label.setFontScale(ScratchUI.textScale);
        hittable = false;
    }

    public LabelElement(String str, boolean bundle) {
        this(bundle ? ScratchController.getLocalized("elem." + str + ".name") : str);
    }

    @Override
    public void cell(Cell<? extends ScratchTable> c) {
        if (parent.getChildren().size == 1) {
            c.pad(0, addPadding * 3, 0, 1);
        } else {
            c.pad(0, 1, 0 ,1);
        }
    }

    @Override
    public ScratchType getType() {
        return ScratchType.none;
    }

    @Override
    public ScratchElement copy() {
        return new LabelElement(label.getText().toString());
    }

    @Override
    public void read(Reads r) {
    }

    @Override
    public void write(Writes w) {
    }
}
