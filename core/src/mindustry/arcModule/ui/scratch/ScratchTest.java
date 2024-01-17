package mindustry.arcModule.ui.scratch;

import mindustry.arcModule.ui.scratch.blocks.ARCBlocks;
import mindustry.arcModule.ui.scratch.blocks.LogicBlocks;
import mindustry.arcModule.ui.scratch.blocks.TestBlocks;
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
        if ((id & 0b100) == 0b100) TestBlocks.init();
    }
}
