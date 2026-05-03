package mindustry.arcreeper;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;

public final class CreeperCore {
    public static final String tag = "@Arcreeper";

    private static boolean enabled = false;
    private static boolean eventsLoaded = false;

    public static CreeperTile creeperTile = new CreeperTile();
    public CreeperCore(){}

    /** 在 Mod.init() 或游戏包初始化阶段调用一次 */
    public static void init(){
        if(eventsLoaded) return;
        eventsLoaded = true;
        Log.info("init ArcCreeper");
        CreeperBuilding.init();

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(isFloodMap()){
                enable();
                creeperTile.init();
            }else{
                disable();
            }
        });
        Events.on(EventType.GameOverEvent.class, e-> disable());

        Events.run(EventType.Trigger.update, CreeperCore::update);
    }

    /** 判断当前地图/规则是否启用 creeper 模式 调试：直接为true */
    public static boolean isFloodMap(){
        return true;
        /* return Vars.state != null
                && Vars.state.rules != null
                && Vars.state.rules.tags.containsKey(tag);*/
    }

    /** 进入模式 */
    public static void enable(){
        if(enabled) return;
        enabled = true;
        creeperTile.init();
        CreeperBuilding.load();

    }

    /** 退出模式 */
    public static void disable(){
        if(!enabled) return;
        enabled = false;

        creeperTile.reset();
        CreeperBuilding.reset(true);

    }

    /** 每帧只调用一次 */
    public static void update(){
        if(!enabled) return;
        creeperTile.update();
        CreeperBuilding.update();
    }

    public static boolean enabled() {
        return enabled;
    }

    public void draw(){
        if(!enabled) return;
        creeperTile.draw();
    }

}