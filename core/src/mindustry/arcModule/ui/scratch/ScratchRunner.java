package mindustry.arcModule.ui.scratch;

import arc.Events;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.fork.ForkHasChild;
import mindustry.game.EventType;

public class ScratchRunner {
    public int runCount = 0, runLimit = 1;
    private final Seq<ScratchBlock.Run> runs = new Seq<>(), will = new Seq<>();
    private ScratchBlock.Run run;
    private boolean inserted;

    public ScratchRunner() {
        Events.run(EventType.Trigger.update, this::update);
    }

    public void add(ScratchBlock.Run r) {
        will.add(r);
    }

    public void remove(ScratchBlock.Run r) {
        will.remove(r);
    }

    public void update() {
        runs.clear();
        runs.addAll(will);
        for (int i = 0, l = runs.size; i < l; i++) {
            ScratchBlock.Run value = runs.get(i);
            runCount = 0;
            run = value;
            while (runCount < runLimit && run.pointer != null) {
                do {
                    inserted = false;
                    run.pointer.prepareRun();
                    run.pointer.run();
                    runCount++;
                } while (inserted && runCount < runLimit);
                if (inserted) break;
                if (run.pointer.linkFrom == null) {
                    if (run.pointer.getTopBlock().linkTo instanceof ForkHasChild fork) {
                        run.pointer = fork.pop();
                        if (run.pointer != null) continue;
                    }
                    run.block.runFinished();
                    will.remove(run);
                    break;
                }
                run.pointer = run.pointer.linkFrom;
            }
        }
    }

    public void insert(ScratchBlock target) {
        run.pointer = target;
        inserted = true;
    }

    public void skip() {
        runCount = runLimit;
    }

    public void reset() {
        will.clear();
        run = null;
    }
}
