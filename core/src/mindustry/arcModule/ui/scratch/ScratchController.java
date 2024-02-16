package mindustry.arcModule.ui.scratch;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.game.EventType;

import java.util.Iterator;

public class ScratchController {
    public static int runCount = 0, runLimit = 1;
    public static ScratchUI ui;
    public static ScratchTable dragging, selected;
    protected static Seq<ScratchBlock.Run> runs = new Seq<>(), runs2 = new Seq<>();
    protected static ObjectMap<String, Integer> map = new ObjectMap<>();
    protected static Seq<ScratchBlock> list = new Seq<>();
    private static ScratchBlock.Run run;

    public static void init() {
        ui = new ScratchUI();
        Events.run(EventType.Trigger.update, ScratchController::run);
    }

    public static void run(ScratchBlock.Run r) {
        runs2.add(r);
    }

    public static void run() {
        Iterator<ScratchBlock.Run> iterator = runs.iterator();
        while (iterator.hasNext()) {
            ScratchBlock.Run next = iterator.next();
            run = next;
            runCount = 0;
            while (run.child != null) run = run.child;
            while (runCount++ < runLimit) run.cycle.get(run.block.elements);
            if (run.valid != null && !run.valid.get(run.block.elements)) {
                ScratchBlock b = run.block.linkFrom;
                if (b != null) {
                    b.scheduleRun(true);
                } else if (run.parent != null) {
                    run.parent.child = null;
                    run.block.running = null;
                    continue;
                }
                run.block.running = null;
                iterator.remove();
            }
        }
        runs.addAll(runs2);
        runs2.clear();
    }

    public static void skipRunning() {
        runCount = runLimit;
    }

    public static void setRunning(ScratchBlock.Run r) {
        run.child = r;
        r.parent = run;
        run = r;
    }

    public static void registerBlock(String name, ScratchBlock e) {
        if (!list.contains(e)) {
            map.put(name, list.add(e).size - 1);
            ui.addBlocks(e);
            ScratchInput.addNewInput(e);
        }
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
