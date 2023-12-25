package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.SnapshotSeq;
import mindustry.arcModule.ui.scratch.blocks.ForkBlock;
import mindustry.arcModule.ui.scratch.blocks.ScratchAsync;
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
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.condition, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.cond();
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.block, new Color(Color.packRgba(76, 151, 255, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.input();
            }
        }));
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
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
            ScratchController.ui.addElement(new ScratchBlock(logicOp.name(), ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
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
                public void getValue(SnapshotSeq<Element> elements, Cons<Object> callback) {
                    if (op.function2 == null) {
                        ScratchAsync.asyncGet((InputElement) elements.get(1), s -> callback.get(Objects.requireNonNull(op.function1).get(s.d1)));
                    } else {
                        ScratchAsync.asyncGet((InputElement) elements.get(op.func ? 1 : 0), (InputElement) elements.get(2), s -> {
                            if (s.success) {
                                callback.get(Objects.requireNonNull(op.function2).get(s.d1, s.d2));
                            } else if (op.objFunction2 != null) {
                                callback.get(Objects.requireNonNull(op.objFunction2).get(s.o1, s.o2));
                            }
                        });
                    }
                }
            }));
        }
        ScratchController.ui.addElement(new ForkBlock("test", ScratchType.block, new Color(Color.packRgba(89, 192, 89, 255)), new ForkBlock.ForkInfo() {
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
        ScratchController.ui.show();
    }
}
