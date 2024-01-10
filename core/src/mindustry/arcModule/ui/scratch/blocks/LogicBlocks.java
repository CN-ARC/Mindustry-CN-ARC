package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import arc.util.Structs;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.logic.LogicOp;

import java.util.Objects;

public class LogicBlocks {
    public static void init() {

        for (LogicOp logicOp : LogicOp.all) {
            final LogicOp op = logicOp;
            ScratchController.registerBlock("op" + op.name(), new ScratchBlock(op == LogicOp.equal ||
                    op == LogicOp.notEqual ||
                    op == LogicOp.land ||
                    op == LogicOp.lessThan ||
                    op == LogicOp.lessThanEq ||
                    op == LogicOp.greaterThan ||
                    op == LogicOp.greaterThanEq ||
                    op == LogicOp.strictEqual ? ScratchType.condition : ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo(block -> {
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
        }
    }
}
