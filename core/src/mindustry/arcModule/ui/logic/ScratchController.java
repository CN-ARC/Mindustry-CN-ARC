package mindustry.arcModule.ui.logic;

import arc.struct.ObjectMap;
import arc.struct.Seq;

public class ScratchController {
    public static LogicUI ui;
    public static ScratchTable dragging, selected;
    protected static ObjectMap<String, Integer> map = new ObjectMap<>();
    protected static Seq<ScratchTable> list = new Seq<>();
    public static void init() {
        ui = new LogicUI();
    }

    public static void registerBlock(String name, ScratchTable e) {
        map.put(name, list.add(e).size - 1);
    }

    public static ScratchTable get(String name) {
        return list.get(map.get(name));
    }

    public static ScratchTable get(int i) {
        return list.get(i);
    }
}
