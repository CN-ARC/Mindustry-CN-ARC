package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

public class ScratchController {
    public static ScratchUI ui;
    public static ScratchRunner runner;
    public static ScratchTable dragging, selected;
    public static ObjectMap<String, Integer> map = new ObjectMap<>();
    public static Seq<ScratchBlock> list = new Seq<>();

    public static void init() {
        ui = new ScratchUI();
        runner = new ScratchRunner();
    }

    public static void registerBlock(String name, ScratchBlock e) {
        if (!list.contains(e)) {
            map.put(name, list.add(e).size - 1);
            ui.addBlock(e);
            ScratchInput.addNewInput(e);
        }
    }

    public static void run(ScratchBlock.Run r) {
        runner.add(r);
    }

    public static void insert(ScratchBlock b) {
        runner.insert(b);
    }

    public static void reset() {
        dragging = selected = null;
        map.clear();
        list.clear();
        ui.clearBlocks();
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
                doubles[i] = toDouble(obj);
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
        return o instanceof Number || o instanceof Boolean;
    }

    public static double toDouble(Object o) {
        if (o instanceof Boolean b) return b ? 1 : 0;
        return (double) o;
    }

    public static class DoubleResult {
        public boolean success;
        public double[] doubles;
        public Object[] objects;

        DoubleResult(Object[] objects, double[] doubles, boolean success) {
            this.objects = objects;
            this.doubles = doubles;
            this.success = success;
        }
    }
}
