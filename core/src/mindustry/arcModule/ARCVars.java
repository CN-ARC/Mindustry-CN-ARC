package mindustry.arcModule;

import arc.Core;
import arc.Events;
import arc.assets.Loadable;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.arcModule.toolpack.ARCTeam;
import mindustry.arcModule.ui.ARCUI;
import mindustry.arcreeper.CreeperCore;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;

import static arc.Core.settings;

public class ARCVars implements Loadable {
    public static ARCUI arcui = new ARCUI();
    public static final int minimapSize = 40;
    public static boolean unitHide = false;
    public static boolean limitUpdate = false;
    public static int limitDst = 0;

    /** ARC */
    public static String arcVersion = Version.arcBuild <= 0 ? "dev" : String.valueOf(Version.arcBuild);
    public static String arcVersionPrefix = "<ARCreeper~" + arcVersion + ">";

    public static Seq<District.advDistrict> districtList = new Seq<>();
    /** 服务器远程控制允许或移除作弊功能 */
    public static boolean arcCheatServer = false;
    public static boolean replaying = false;
    public static ReplayController replayController;

    public static boolean arcInfoControl = false;

    /** Control */
    public static boolean quickBelt;

    /** UI */
    public static boolean arcSelfName;
    public static boolean arcHideName;
    public static boolean payloadPreview;

    public static final int maxBuildPlans = 100;

    public static ARCTeam arcTeam = new ARCTeam();

    public static String arcFolderName = "arcCustom";
    public static String arcCustomBackgroundName = "background";
    /** Arcreeper */
    public static CreeperCore creeperCore;

    public static final String FAKEMODNAME = "ARCreeper-Client:1";

    public static ARCBackup arcBackup = new ARCBackup();

    public static void init(){
        if(!Vars.headless) {
            Events.run(EventType.Trigger.update, () -> {
                arcInfoControl = !arcCheatServer && (Core.settings.getBool("showOtherTeamState") ||
                        Vars.player.team().id == 255 || Vars.state.rules.mode() != Gamemode.pvp);
                arcSelfName = settings.getBool("arcSelfName");
                arcHideName = settings.getBool("arcHideName");
                payloadPreview = settings.getBool("payloadpreview");

                quickBelt = settings.getBool("quickBelt");
            });
            ARCVars.replayController = new ReplayController();

            arcBackup.init();
        }

        ARCVars.creeperCore = new CreeperCore();
        CreeperCore.init();
    }


    public static int getMaxSchematicSize(){
        int s = Core.settings.getInt("maxSchematicSize");
        return s == 501 ? Integer.MAX_VALUE : s;
    }

    public static int getMinimapSize(){
        return settings.getInt("minimapSize",minimapSize);
    }

    public static String getThemeColorCode(){
        return "[#" + getThemeColor() + "]";
    }

    public static Color getThemeColor(){
        try {
            return Color.valueOf(settings.getString("themeColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Color getPlayerEffectColor(){
        try {
            return Color.valueOf(settings.getString("playerEffectColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Boolean arcInfoControl(Team team){
        return team == Vars.player.team() || arcInfoControl;
    }
}
