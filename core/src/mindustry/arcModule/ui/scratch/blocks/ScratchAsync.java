package mindustry.arcModule.ui.scratch.blocks;

import arc.func.Cons;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.elements.ScratchElement;

public class ScratchAsync {
    public static void asyncGet(ScratchElement e1, Cons<ScratchController.DoubleResult> callback) {
        e1.getValue(e -> callback.get(ScratchController.checkDouble(e, null)));
    }

    public static void asyncGet(ScratchElement e1, ScratchElement e2, Cons<ScratchController.DoubleResult> callback) {
        boolean[] stat = new boolean[]{false, false};
        Object[] objects = new Object[]{null, null};
        e1.getValue(o -> {
            stat[0] = true;
            objects[0] = o;
            if (stat[1]) callback.get(ScratchController.checkDouble(objects[0], objects[1]));
        });
        e2.getValue(o -> {
            stat[1] = true;
            objects[1] = o;
            if (stat[0]) callback.get(ScratchController.checkDouble(objects[0], objects[1]));
        });
    }
}
