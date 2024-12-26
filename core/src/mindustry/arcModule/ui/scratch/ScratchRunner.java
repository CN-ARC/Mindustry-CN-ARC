package mindustry.arcModule.ui.scratch;

import arc.struct.Seq;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.scratch.block.fork.ForkHasChild;
import mindustry.logic.LAssembler;
import mindustry.logic.LExecutor;

public class ScratchRunner {
    public LExecutor executor = new LExecutor();
    public LAssembler asm = new LAssembler();
    public int runCount = 0, runLimit = 1;
    public boolean paused = false;
    private final Seq<Task> tasks = new Seq<>(), will = new Seq<>();
    private Task task;
    private boolean inserted;

    public ScratchRunner() {
        executor.load(asm);
    }

    public void destroy() {
        tasks.clear();
        will.clear();
        task = null;
        executor = null;
        asm = null;
    }

    public void add(Task r) {
        will.add(r);
    }

    public void remove(Task r) {
        will.remove(r);
    }

    public void update() {
        if (paused || will.size == 0) return;
        tasks.addAll(will);
        for (int i = 0, l = tasks.size; i < l; i++) {
            Task r = tasks.get(i);
            if (r.pointer.parent == null) continue;
            if (r.paused) {
                will.add(r);
                continue;
            }
            runCount = 0;
            task = r;
            label:
            while (runCount < runLimit && task.pointer != null) {
                do {
                    inserted = false;
                    if (task.running) task.pointer.prepareRun();
                    task.pointer.run();
                    if (!task.pointer.runFinished()) {
                        task.running = false;
                        break label;
                    }
                    task.running = true;
                    runCount++;
                } while (inserted && runCount < runLimit);
                if (inserted) break;
                if (task.pointer.linkFrom == null) {
                    if (task.pointer.getTopBlock().linkTo instanceof ForkHasChild fork) {
                        task.pointer = fork.pop();
                        if (task.pointer != null) continue;
                    }
                    task.block.finishRun();
                    will.remove(task);
                    break;
                }
                task.pointer = task.pointer.linkFrom;
            }
        }
        tasks.clear();
    }

    public void insert(ScratchBlock target) {
        task.pointer = target;
        inserted = true;
    }

    public void skip() {
        runCount = runLimit;
    }

    public void reset() {
        will.clear();
        task = null;
    }

    public Seq<Task> getTasks() {
        return will;
    }

    public static class Task {
        public final ScratchBlock block;
        public ScratchBlock pointer;
        public boolean running = true, paused = false;

        public Task(ScratchBlock block) {
            this.block = pointer = block;
        }
    }
}
