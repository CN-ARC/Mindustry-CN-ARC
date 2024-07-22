package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.LogicBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.gen.LogicIO;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatement;
import mindustry.logic.LogicOp;

import java.util.Objects;

public class LogicBlocks {
    public LExecutor executor = new LExecutor();
    public static void init() {
        generateLogic();
        initExtra();
    }

    public static void generateLogic() {
        ObjectMap<LCategory, Seq<LStatement>> map = new ObjectMap<>();
        LogicIO.allStatements.each(p -> {
            LStatement l = p.get();
            map.get(l.category(), Seq::new).add(l);
        });
        map.each((c, s) -> {
            ScratchController.category(c.name, c.color);
            s.each(l -> ScratchController.registerBlock(l.name(), new LogicBlock(l)));
        });
    }
    
    public static void initExtra() {
        Color c = new Color(Color.packRgba(89, 192, 89, 255));
        ScratchController.ui.addCategory("运算", c);
        for (LogicOp logicOp : LogicOp.all) {
            final LogicOp op = logicOp;
            ScratchController.registerBlock("op" + op.name(), new ScratchBlock(op == LogicOp.equal ||
                    op == LogicOp.notEqual ||
                    op == LogicOp.land ||
                    op == LogicOp.lessThan ||
                    op == LogicOp.lessThanEq ||
                    op == LogicOp.greaterThan ||
                    op == LogicOp.greaterThanEq ||
                    op == LogicOp.strictEqual ? ScratchType.condition : ScratchType.input, c, new BlockInfo(block -> {
                if (op.function2 == null) {
                    block.label(op.symbol);
                    block.input();
                } else {
                    if (op.func) {
                        block.label(op.symbol);
                        block.input();
                        block.input();
                    } else {
                        block.input();
                        block.label(op.symbol);
                        block.input();
                    }
                }
            }, elements -> {
                if (op == LogicOp.strictEqual) {
                    DoubleResult s = checkDouble(((InputElement) elements.get(0)).getValue(), ((InputElement) elements.get(2)).getValue());
                    return s.success && s.doubles[0] == s.doubles[1] || !s.success && Structs.eq(s.objects[0], s.objects[1]) ? 1 : 0;
                }
                if (op.function2 == null) {
                    return Objects.requireNonNull(op.function1).get(checkDouble(((InputElement) elements.get(1)).getValue()).doubles[0]);
                } else {
                    DoubleResult s = checkDouble(((InputElement) elements.get(op.func ? 1 : 0)).getValue(), ((InputElement) elements.get(2)).getValue());
                    if (s.success || op.objFunction2 == null) {
                        return Objects.requireNonNull(op.function2).get(s.doubles[0], s.doubles[1]);
                    } else {
                        return Objects.requireNonNull(op.objFunction2).get(s.objects[0], s.objects[1]);
                    }
                }
            })));
        }
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
