package mindustry.arcModule.ui.logic;

import arc.graphics.Color;
import mindustry.arcModule.ui.logic.blocks.ForkBlock;
import mindustry.arcModule.ui.logic.blocks.ScratchBlock;

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
        ScratchController.ui.addElement(new ScratchBlock("test", ScratchType.input, new Color(Color.packRgba(89, 192, 89, 255)), new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                block.input();
                block.label("aaaaaa");
                block.cond();
                block.input();
            }
        }));
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
