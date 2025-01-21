package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.block.DefineBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ScratchContext {
    public ScratchUI ui = new ScratchUI();
    public ScratchRunner runner = new ScratchRunner();
    public ScratchTable selected;
    public ScratchBlock dragging;
    public ObjectMap<String, Integer> map = new ObjectMap<>();
    public Seq<ScratchBlock> list = new Seq<>();
    public Seq<DefineBlock> functions = new Seq<>();
    public ScratchController.State state = ScratchController.State.idle;

    public void save(Writes w) {
        state = ScratchController.State.saving;
        ScratchEvents.fire(ScratchEvents.Event.saveBegin);
        write(w);
        state = ScratchController.State.idle;
        ScratchEvents.fire(ScratchEvents.Event.saveEnd);
    }

    public void load(Reads r) {
        state = ScratchController.State.loading;
        ScratchEvents.fire(ScratchEvents.Event.loadBegin);
        read(r);
        state = ScratchController.State.idle;
        ScratchEvents.fire(ScratchEvents.Event.loadEnd);
    }

    public void write(Writes w) {
        ui.write(w);
    }

    public void read(Reads r) {
        ui.read(r);
    }

    public void registerFunction(DefineBlock f) {
        functions.add(f.id(functions.size));
    }

    public void reset() {
        functions.clear();
    }

    public static ScratchContext createContext() {
        return new ScratchContext();
    }
}
