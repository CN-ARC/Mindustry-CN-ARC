package mindustry.arcModule.ui.scratch.block;

import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchTable;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.block.fork.*;

public class ForkBlock extends ScratchBlock {

    public ForkBlock(ScratchType type, Color color, ForkInfo info) {
        super(type, color, new BlockInfo() {
            @Override
            public void build(ScratchBlock block) {
                info.build((ForkBlock) block);
            }
        });
    }

    public void header(ForkInfo info) {
        ForkHeader e = new ForkHeader(info, elemColor);
        e.cell(add(e));
    }

    public void inner() {
        ForkInner e = new ForkInner(null, ScratchType.none, elemColor, new BlockInfo());
        e.cell(add(e));
    }

    public void middle(ForkInfo info) {
        ForkMiddle e = new ForkMiddle(info, elemColor);
        e.cell(add(e));
    }

    public void footer() {
        ForkFooter e = new ForkFooter(null, ScratchType.none, elemColor, new BlockInfo());
        e.cell(add(e));
    }

    @Override
    public void cell(Cell<ScratchTable> c) {
        super.cell(c);
        c.pad(0);
    }

    @Override
    public void draw() {
        super.drawSuper();
    }

    public static class ForkInfo extends BlockInfo {
        public void build(ForkBlock block) {
        }
    }
}
