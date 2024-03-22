package mindustry.arcModule.ui.scratch;

import mindustry.arcModule.ui.scratch.blocks.ARCBlocks;
import mindustry.arcModule.ui.scratch.blocks.ControlBlocks;
import mindustry.arcModule.ui.scratch.blocks.LogicBlocks;
//Packages.mindustry.arcModule.ui.scratch.ScratchTest
public class ScratchTest {
    public static void test() {
        ScratchController.init();
        ScratchController.ui.createWindow();
        init(0b111);
    }

    public static void init(int id) {
        ScratchController.reset();
        if ((id & 0b1) == 0b1) ARCBlocks.init();
        if ((id & 0b10) == 0b10) LogicBlocks.init();
        if ((id & 0b100) == 0b100) ControlBlocks.init();

        /*for (int x = 0; x < 10000; x += 100) {
            for (int y = 0; y < 10000; y += 100) {
                ScratchBlock b = ScratchController.list.random().copy();
                ScratchController.ui.addElement(b);
                b.setPosition(x, y);
            }
        }*/
    }
}
