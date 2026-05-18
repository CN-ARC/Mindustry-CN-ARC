package mindustry.arcreeper;

import arc.Events;
import arc.graphics.Color;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;

public final class CreeperCore {
    public static final String tag = "@Arcreeper";

    private static boolean enabled = false;
    private static boolean eventsLoaded = false;

    public static CreeperTile creeperTile = new CreeperTile();
    public static CreeperGrid creeperGrid = new CreeperGrid();
    public CreeperCore(){}

    public static boolean drawSporeHealth = true;

    public static boolean performanceAnalyze = false;

    public static void init(){
        if(eventsLoaded) return;
        eventsLoaded = true;

        Log.info("init ArcCreeper");

        CreeperSave.init();
        CreeperNetwork.init();
        SporeCore.init();

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(isFloodMap()){
                enable();
            }else{
                disable();
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> disable());

        Events.run(EventType.Trigger.update, CreeperCore::update);
        Events.run(EventType.Trigger.draw, CreeperCore::draw);
    }

    /** 判断当前地图/规则是否启用 creeper 模式 调试：直接为true */
    public static boolean isFloodMap(){
        return !Vars.state.isEditor();
        /* return Vars.state != null
                && Vars.state.rules != null
                && Vars.state.rules.tags.containsKey(tag);*/
    }

    /** 进入模式 */
    public static void enable(){
        //if(enabled) return;
        enabled = true;
        creeperTile.init();
    }

    /** 退出模式 */
    public static void disable(){
        if(!enabled) return;
        enabled = false;

        creeperTile.reset();
        SporeCore.reset();

        if(Vars.net.client()){
            SporeCore.resetLocal();
        }else{
            SporeCore.clearAuthoritative();
        }

    }

    /** 每帧只调用一次 */
    public static void update(){
        if(!enabled) return;
        if(Vars.state.isPaused()) return;

        // 约每秒采样一次；非采样帧完全走原逻辑，尽量减少性能统计本身的影响
        boolean profile = ((long)(arc.util.Time.time / 60f)) != ((long)((arc.util.Time.time - arc.util.Time.delta) / 60f));

        if(!profile){
            creeperTile.update();
            creeperGrid.update();
            SporeCore.update();
            return;
        }

        long t0 = System.nanoTime();
        creeperTile.update();

        long t1 = System.nanoTime();
        creeperGrid.update();

        long t2 = System.nanoTime();
        SporeCore.update();

        long t3 = System.nanoTime();

        double tileMs = (t1 - t0) / 1_000_000.0;
        double gridMs = (t2 - t1) / 1_000_000.0;
        double sporeMs = (t3 - t2) / 1_000_000.0;
        double totalMs = (t3 - t0) / 1_000_000.0;

        String maxName = "creeperTile";
        double maxMs = tileMs;
        if(gridMs > maxMs){
            maxName = "creeperGrid";
            maxMs = gridMs;
        }
        if(sporeMs > maxMs){
            maxName = "SporeCore";
            maxMs = sporeMs;
        }
        if (performanceAnalyze) Log.info(java.lang.String.format(java.util.Locale.ROOT,
                        "[ARCreeper性能] creeperTile=%.3fms, creeperGrid=%.3fms, SporeCore=%.3fms, total=%.3fms, 60FPS帧预算占比=%.1f%%, 最慢=%s %.3fms",
                        tileMs, gridMs, sporeMs, totalMs, totalMs / 16.6666667 * 100.0, maxName, maxMs
                                ));
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void draw(){
        if(!enabled) return;
        creeperTile.draw();
        SporeCore.draw();
    }

    public static Color getCreeperColor(float creeper){
        return creeper>0? Vars.state.rules.creeperColor: Vars.state.rules.antiCreeperColor;
    }

}