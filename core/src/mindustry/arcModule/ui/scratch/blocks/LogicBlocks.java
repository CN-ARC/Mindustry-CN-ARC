package mindustry.arcModule.ui.scratch.blocks;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.LogicBlock;
import mindustry.gen.LogicIO;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatement;

public class LogicBlocks {
    public LExecutor e = new LExecutor();
    public static void init() {
        generateLogic();
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
}
