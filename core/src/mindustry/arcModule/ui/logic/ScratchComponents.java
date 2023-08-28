package mindustry.arcModule.ui.logic;

import mindustry.arcModule.ui.logic.elements.CondElement;
import mindustry.arcModule.ui.logic.elements.InputElement;

public class ScratchComponents {
    public static ScratchElement input(String def) {
        return new InputElement(def);
    }

    public static ScratchElement cond() {
        return new CondElement();
    }
}
