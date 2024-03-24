package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.block.ForkBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.TriggerBlock;
import mindustry.arcModule.ui.scratch.block.fork.ForkComponent;
import mindustry.arcModule.ui.scratch.element.CondElement;

import static mindustry.arcModule.ui.scratch.ScratchController.runner;

public class ControlBlocks {
    public static void init() {
        Color c = new Color(Color.packRgba(255, 171, 25, 255));
        ScratchController.ui.addCategory("控制", c);
        ScratchController.registerBlock("when", new TriggerBlock(c, new BlockInfo(b -> b.label("当开始运行时"))));
        ScratchController.registerBlock("if", new ForkBlock(c, new ForkBlock.ForkInfo(block -> block.header(b -> {
            b.label("如果");
            b.cond();
        }), e -> {
            if (((CondElement) ((ForkComponent) e.get(0)).elements.get(1)).getValue()) {
                ScratchBlock run = ((ForkComponent) e.get(0)).linkFrom;
                if (run != null) run.insertRun();
            }
        })));
        ScratchController.registerBlock("ifelse", new ForkBlock(c, new ForkBlock.ForkInfo(block -> {
            block.header(b -> {
                b.label("如果");
                b.cond();
            });
            block.inner(b -> b.label("否则"));
        }, e -> {
            if (((CondElement) ((ForkComponent) e.get(0)).elements.get(1)).getValue()) {
                ScratchBlock run = ((ForkComponent) e.get(0)).linkFrom;
                if (run != null) run.insertRun();
            } else {
                ScratchBlock run = ((ForkComponent) e.get(1)).linkFrom;
                if (run != null) run.insertRun();
            }
        })));
        ScratchController.registerBlock("while", new ForkBlock(c, new ForkBlock.ForkInfo(block -> block.header(b -> b.label("重复执行"), e -> {
            ScratchBlock run = ((ForkComponent) e.get(0)).linkFrom;
            return run == null ? ((ForkBlock) e.get(0).parent).linkFrom : run;
        }), e -> {
            ScratchBlock run = ((ForkComponent) e.get(0)).linkFrom;
            if (run != null) {
                run.insertRun();
            } else {
                ((ForkBlock) e.get(0).parent).insertRun();
                runner.skip();
            }
        })));
    }
}
