package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.LogicBlock;
import mindustry.gen.LogicIO;
import mindustry.logic.LStatement;

public class LogicBlocks {
    public static void init() {
        Color c = new Color(Color.packRgba(89, 192, 89, 255));
        ScratchController.ui.addCategory("运算", c);
        LogicIO.allStatements.each(p -> {
            LStatement l = p.get();
            try {
                ScratchController.registerBlock(l.name(), new LogicBlock(l));
            } catch (Exception e) {
                Log.err("Load " + l.name() + " failed", e);
            }
        });
        /*for (LogicOp logicOp : LogicOp.all) {
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
                    ScratchController.DoubleResult s = ScratchController.checkDouble(((InputElement) elements.get(0)).getValue(), ((InputElement) elements.get(2)).getValue());
                    return s.success && s.doubles[0] == s.doubles[1] || !s.success && Structs.eq(s.objects[0], s.objects[1]) ? 1 : 0;
                }
                if (op.function2 == null) {
                    return Objects.requireNonNull(op.function1).get(ScratchController.checkDouble(((InputElement) elements.get(1)).getValue()).doubles[0]);
                } else {
                    ScratchController.DoubleResult s = ScratchController.checkDouble(((InputElement) elements.get(op.func ? 1 : 0)).getValue(), ((InputElement) elements.get(2)).getValue());
                    if (s.success || op.objFunction2 == null) {
                        return Objects.requireNonNull(op.function2).get(s.doubles[0], s.doubles[1]);
                    } else {
                        return Objects.requireNonNull(op.objFunction2).get(s.objects[0], s.objects[1]);
                    }
                }
            })));
        }*/
    }
}
