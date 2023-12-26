package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;

public class ScratchController {
    public static ScratchUI ui;
    public static ScratchTable dragging, selected;
    protected static ObjectMap<String, Integer> map = new ObjectMap<>();
    protected static Seq<ScratchTable> list = new Seq<>();
    public static void init() {
        ui = new ScratchUI();
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

    public static DoubleResult checkDouble(Object o1, Object o2) {
        if (o2 == null) {
            if (isNumber(o1)) return new DoubleResult((double) o1, Double.NaN);
            if (o1 instanceof String s) {
                try {
                    return new DoubleResult(Double.parseDouble(s), Double.NaN);
                } catch (Exception ignored) {
                }
            }
            return new DoubleResult(o1, null);
        }
        if (isNumber(o1) && isNumber(o2)) {
            return new DoubleResult((double) o1, (double) o2);
        }
        double tmp1, tmp2;
        try {
            if (o1 instanceof String s1 && o2 instanceof String s2) {
                tmp1 = Double.parseDouble(s1);
                tmp2 = Double.parseDouble(s2);
            } else {
                return new DoubleResult(o1, o2);
            }
        } catch (Exception e) {
            return new DoubleResult(o1, o2);
        }
        return new DoubleResult(tmp1, tmp2);
    }

    public static boolean isNumber(Object o) {
        return o instanceof Double || o instanceof Integer || o instanceof Boolean || o instanceof Float || o instanceof Long || o instanceof Short;
    }

    public static class DoubleResult {
        boolean success;
        double d1 = Double.NaN;
        Object o1;
        double d2 = Double.NaN;
        Object o2;
        DoubleResult(double d1, double d2) {
            this.d1 = d1;
            this.d2 = d2;
            this.success = true;
        }
        DoubleResult(Object o1, Object o2) {
            this.o1 = o1;
            this.o2 = o2;
            this.success = false;
        }
    }
}
