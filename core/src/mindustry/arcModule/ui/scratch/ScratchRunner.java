package mindustry.arcModule.ui.scratch;

import arc.Events;
import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.fork.ForkHasChild;
import mindustry.game.EventType;
import mindustry.logic.LAssembler;
import mindustry.logic.LExecutor;

public class ScratchRunner {
    public LExecutor executor = new LExecutor();
    public LAssembler asm = new LAssembler();
    public int runCount = 0, runLimit = 1;
    private final Seq<Run> runs = new Seq<>(), will = new Seq<>();
    private Run run;
    private boolean inserted;

    public ScratchRunner() {
        Events.run(EventType.Trigger.update, this::update);
    }

    public void add(Run r) {
        will.add(r);
    }

    public void remove(Run r) {
        will.remove(r);
    }

    public void update() {
        if (will.size == 0) return;
        runs.addAll(will);
        for (int i = 0, l = runs.size; i < l; i++) {
            Run value = runs.get(i);
            runCount = 0;
            run = value;
            label:
            while (runCount < runLimit && run.pointer != null) {
                do {
                    inserted = false;
                    if (run.running) run.pointer.prepareRun();
                    run.pointer.run();
                    if (!run.pointer.runFinished()) {
                        run.running = false;
                        break label;
                    }
                    run.running = true;
                    runCount++;
                } while (inserted && runCount < runLimit);
                if (inserted) break;
                if (run.pointer.linkFrom == null) {
                    if (run.pointer.getTopBlock().linkTo instanceof ForkHasChild fork) {
                        run.pointer = fork.pop();
                        if (run.pointer != null) continue;
                    }
                    run.block.finishRun();
                    will.remove(run);
                    break;
                }
                run.pointer = run.pointer.linkFrom;
            }
        }
        runs.clear();
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

    public static class Run {
        public final ScratchBlock block;
        public ScratchBlock pointer;
        public boolean running = true;

        public Run(ScratchBlock block) {
            this.block = pointer = block;
        }
    }
}
