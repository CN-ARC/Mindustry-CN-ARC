package mindustry.arcModule.ui.scratch;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.block.FunctionBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ScratchContext {
    public ScratchUI ui = new ScratchUI();
    public ScratchRunner runner = new ScratchRunner();
    public ScratchTable selected;
    public ScratchBlock dragging;
    public ObjectMap<String, Integer> map = new ObjectMap<>();
    public Seq<ScratchBlock> list = new Seq<>();
    public Seq<FunctionBlock> functions = new Seq<>();

    public ScratchContext() {
        functions.add(new FunctionBlock(Color.white));
    }

    public void write(Writes w) {

    }

    public void read(Reads r) {

    }

    public void registerFunction(FunctionBlock f) {
        functions.add(f.id(functions.size));
    }

    public static ScratchContext createContext() {
        return new ScratchContext();
    }
}
