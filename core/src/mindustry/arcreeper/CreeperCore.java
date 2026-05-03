package mindustry.arcreeper;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;

public final class CreeperCore {
    public static final String tag = "@Arcreeper";

    private static boolean enabled = false;
    private static boolean eventsLoaded = false;

    public static CreeperUpdate creeperUpdate = new CreeperUpdate();
    public CreeperCore(){}

    /** 在 Mod.init() 或游戏包初始化阶段调用一次 */
    public static void init(){
        if(eventsLoaded) return;
        eventsLoaded = true;
        Log.info("init ArcCreeper");

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(isFloodMap()){
                enable();
                creeperUpdate.init();
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
        creeperUpdate.init();

    }

    /** 退出模式 */
    public static void disable(){
        if(!enabled) return;
        enabled = false;

        creeperUpdate.reset();

    }

    /** 每帧只调用一次 */
    public static void update(){
        if(!enabled) return;
        creeperUpdate.update();
    }

    /** 注册某种建筑在 creeper 模式下的附加行为 */
    /*
    public static void register(Block block, Func<Building, BuildingLogic> factory){
        buildingFactories.put(block, factory);
    }

    private static void registerDefaultFactories(){
        register(Blocks.coreShard, CoreEmitterLogic::new);
        register(Blocks.coreFoundation, CoreEmitterLogic::new);
        register(Blocks.coreNucleus, CoreEmitterLogic::new);

        register(Blocks.container, ChargedEmitterLogic::new);
        register(Blocks.vault, ChargedEmitterLogic::new);
        register(Blocks.launchPad, ChargedEmitterLogic::new);
        register(Blocks.interplanetaryAccelerator, ChargedEmitterLogic::new);

        register(Blocks.forceProjector, ForceProjectorLogic::new);
        register(Blocks.impactReactor, ImpactReactorLogic::new);
        register(Blocks.thoriumReactor, ThoriumReactorLogic::new);
    }

    private static void scanWorldBuildings(){
        activeBuildings.clear();

        Vars.world.tiles.eachTile(tile -> {
            Building build = tile.build;
            if(build != null && build.tile == tile){
                addBuilding(build);
            }
        });
    }

    private static void addBuilding(Building build){
        if(build == null) return;
        if(activeBuildings.containsKey(build)) return;

        Func<Building, BuildingLogic> factory = buildingFactories.get(build.block);
        if(factory == null) return;

        BuildingLogic logic = factory.get(build);
        if(logic == null) return;

        activeBuildings.put(build, logic);
        logic.added();
    }

    private static void removeBuilding(Building build){
        if(build == null) return;

        BuildingLogic logic = activeBuildings.remove(build);
        if(logic != null){
            logic.removed(false);
        }
    }

    private static void clearBuildings(){
        for(BuildingLogic logic : activeBuildings.values()){
            logic.removed(true);
        }
        activeBuildings.clear();
    }

    public static boolean enabled(){
        return enabled;
    }

    public static Seq<BuildingLogic> activeLogics(){
        return activeBuildings.values().toSeq();
    }

    public interface ArcreeperSystem{
        default void enable(){}
        default void update(){}
        default void disable(){}
    }

    public interface BuildingLogic{
        Building build();

        default void added(){}
        default void update(){}
        default void removed(boolean reset){}
    }*/
}