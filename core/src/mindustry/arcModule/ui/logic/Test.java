package mindustry.arcModule.ui.logic;

import arc.graphics.Color;

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
    }

    public static void testUI() {
        ScratchController.ui.show();
    }
}
