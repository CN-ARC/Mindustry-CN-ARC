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

        creeperTile.update();
        creeperGrid.update();
        SporeCore.update();
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