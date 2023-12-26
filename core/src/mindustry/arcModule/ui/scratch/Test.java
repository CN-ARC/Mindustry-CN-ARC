package mindustry.arcModule.ui.scratch;

import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.arcModule.ui.scratch.blocks.ForkBlock;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;
import mindustry.arcModule.ui.scratch.elements.InputElement;
import mindustry.logic.LogicOp;

import java.util.Objects;

public class Test {
    public static void test() {
        ScratchController.init();
        testBlocks();
        testUI();
    }

    public static void testBlocks() {
        ScratchController.ui.addElement(new ScratchBlock(ScratchType.condition, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.cond();
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock(ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock(ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.cond();
                block.input();
            }
        }));
        for (LogicOp logicOp : LogicOp.all) {
            final LogicOp op = logicOp;
            ScratchController.registerBlock("op" + op.name(), new ScratchBlock(op == LogicOp.equal ||
                            op == LogicOp.notEqual ||
                            op == LogicOp.land ||
                            op == LogicOp.lessThan ||
                            op == LogicOp.lessThanEq ||
                            op == LogicOp.greaterThan ||
                            op == LogicOp.greaterThanEq ||
                            op == LogicOp.strictEqual ? ScratchType.condition : ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
                @Override
                public void build(ScratchBlock block) {
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
                }

                @Override
                public Object getValue(Seq<Element> elements) {
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
                }
            }));
        }
        ScratchController.ui.addElement(new ForkBlock(ScratchType.block, new Color(Color.packRgba(89, 192, 89, 255)), new ForkBlock.ForkInfo() {
            @Override
            public void build(ForkBlock block) {
                block.header(new ForkBlock.ForkInfo() {
                    @Override
                    public void build(ScratchBlock block) {
                        block.label("test");
                    }
                });
            }
        }));
    }

    public static void testUI() {
        ScratchController.ui.createWindow();
    }
}
