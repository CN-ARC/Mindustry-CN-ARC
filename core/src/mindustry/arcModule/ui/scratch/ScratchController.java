package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;

public class ScratchController {
    public static ScratchUI ui;
    public static ScratchTable dragging, selected;
    protected static ObjectMap<String, Integer> map = new ObjectMap<>();
    protected static Seq<ScratchBlock> list = new Seq<>();
    public static void init() {
        ui = new ScratchUI();
    }

    public static void registerBlock(String name, ScratchBlock e) {
        if (!list.contains(e)) {
            map.put(name, list.add(e).size - 1);
            ui.addBlocks(e);
        }
    }

    public static ScratchTable get(String name) {
        return list.get(map.get(name));
    }

    public static ScratchTable get(int i) {
        return list.get(i);
    }

    public static DoubleResult checkDouble(Object ...objects) {
        double[] doubles = new double[objects.length];
        boolean success = true;
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            if (isNumber(obj)) {
                doubles[i] = (double) obj;
                continue;
            }
            if (obj instanceof String s) {
                try {
                    doubles[i] = Double.parseDouble(s);
                    continue;
                } catch (Exception ignored) {
                }
            }
            success = false;
            doubles[i] = Double.NaN;
        }
        return new DoubleResult(objects, doubles, success);
    }

    public static boolean isNumber(Object o) {
        return o instanceof Double || o instanceof Integer || o instanceof Boolean || o instanceof Float || o instanceof Long || o instanceof Short;
    }

    public static class DoubleResult {
        boolean success;
        double[] doubles;
        Object[] objects;

        DoubleResult(Object[] objects, double[] doubles, boolean success) {
            this.objects = objects;
            this.doubles = doubles;
            this.success = success;
        }
    }
}
