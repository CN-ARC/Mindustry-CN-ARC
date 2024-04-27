package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ScratchContext {
    public ScratchUI ui = new ScratchUI();
    public ScratchRunner runner = new ScratchRunner();
    public ScratchTable dragging, selected;
    public ObjectMap<String, Integer> map = new ObjectMap<>();
    public Seq<ScratchBlock> list = new Seq<>();

    public static ScratchContext createContext() {
        return new ScratchContext();
    }
}
