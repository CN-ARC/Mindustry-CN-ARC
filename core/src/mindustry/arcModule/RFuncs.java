package mindustry.arcModule;

import arc.Core;
import arc.Graphics;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.g2d.PixmapRegion;
import arc.util.Log;
import arc.util.OS;
import arc.util.Strings;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.input.DesktopInput;
import mindustry.ui.*;
import mindustry.ui.fragments.ChatFragment;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;

import static arc.Core.camera;
import static arc.graphics.Color.RGBtoHSV;
import static mindustry.Vars.*;

public class RFuncs {

    static boolean colorized = false;

    static int msgSeperator = 145;
    static boolean cursorChecked = false;
    static Fi cachedCursor = null;

    public interface Stringf<T> {
        String get(T i);
    }

    public static void arcSetCamera(Tile tile) {
        arcSetCamera(tile.worldx(), tile.worldy());
    }

    public static void arcSetCamera(Building building) {
        arcSetCamera(building.x, building.y);
    }

    public static void arcSetCamera(float x, float y) {
        if (control.input instanceof DesktopInput input) {
            input.panning = true;
        }
        camera.position.set(x, y);
        Fx.arcIndexer.at(x, y);
    }

    public static void sendChatMsg(String msg) {
        for (int i = 0; i < msg.length() / (float) msgSeperator; i++) {
            Call.sendChatMessage(msg.substring(i * msgSeperator, Math.min(msg.length(), (i + 1) * msgSeperator)));
        }
    }

    public static void colorizeContent() {
        colorized = Core.settings.getBool("colorizedContent");
        content.items().each(c -> c.localizedName = colorized(c.color, c.localizedName));
        content.liquids().each(c -> c.localizedName = colorized(c.color, c.localizedName));
        content.statusEffects().each(c -> c.localizedName = colorized(c.color, c.localizedName));
        content.planets().each(c -> c.localizedName = colorized(c.atmosphereColor, c.localizedName));
        content.blocks().each(c -> {
            if (c.hasColor) c.localizedName = colorized(blockColor(c), c.localizedName);
            else if (c.itemDrop != null) c.localizedName = colorized(c.itemDrop.color, c.localizedName);
        });
    }

    public static String colorized(Color color, String name) {
        if (colorized) return "[#" + color + "]" + name + "[]";
        else return name;
    }

    private static Color blockColor(Block block) {
        Color bc = new Color(0, 0, 0, 1);
        Color bestColor = new Color(0, 0, 0, 1);
        int highestS = 0;
        if (!block.synthetic()) {
            PixmapRegion image = Core.atlas.getPixmap(block.fullIcon);
            for (int x = 0; x < image.width; x++)
                for (int y = 0; y < image.height; y++) {
                    bc.set(image.get(x, y));
                    int s = RGBtoHSV(bc)[1] * RGBtoHSV(bc)[1] + RGBtoHSV(bc)[2] + RGBtoHSV(bc)[2];
                    if (s > highestS) {
                        highestS = s;
                        bestColor = bc;
                    }
                }
        } else {
            return block.mapColor.cpy().mul(1.2f);
        }
        return bestColor;
    }

    public static String arcShareWaveInfo(int waves) {
        if (!state.rules.waves) return " ";
        StringBuilder builder = new StringBuilder(getPrefix("orange", "Wave"));
        builder.append("标记了第").append(waves).append("波");
        if (waves < state.wave) {
            builder.append("。");
        } else {
            if (waves > state.wave) {
                builder.append("，还有").append(waves - state.wave).append("波");
            }
            int timer = (int) (state.wavetime + (waves - state.wave) * state.rules.waveSpacing);
            builder.append("[").append(fixedTime(timer)).append("]。");
        }

        builder.append(arcWaveInfo(waves));
        return builder.toString();
    }

    public static String calWaveTimer() {
        StringBuilder waveTimer = new StringBuilder();
        waveTimer.append("[orange]");
        int m = ((int) state.wavetime / 60) / 60;
        int s = ((int) state.wavetime / 60) % 60;
        int ms = (int) state.wavetime % 60;
        if (m > 0) {
            waveTimer.append(m).append("[white]: [orange]");
            if (s < 10) {
                waveTimer.append("0");
            }
            waveTimer.append(s).append("[white]min");
        } else {
            waveTimer.append(s).append("[white].[orange]").append(ms).append("[white]s");
        }
        return waveTimer.toString();
    }

    public static String arcWaveInfo(int waves) {
        StringBuilder builder = new StringBuilder();
        if (state.rules.attackMode) {
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append("包含(×").append(sum).append(")");
        } else {
            builder.append("包含(×").append(Vars.spawner.countSpawns()).append("):");
        }
        for (SpawnGroup group : state.rules.spawns) {
            if (group.getSpawned(waves - 1) > 0) {
                builder.append((char) Fonts.getUnicode(group.type.name)).append("(");
                if (group.effect != StatusEffects.invincible && group.effect != StatusEffects.none && group.effect != null) {
                    builder.append((char) Fonts.getUnicode(group.effect.name)).append("|");
                }
                if (group.getShield(waves - 1) > 0) {
                    builder.append(NumberFormat.formatInteger((int) group.getShield(waves - 1), 2, "@@")).append("|");
                }
                builder.append(group.getSpawned(waves - 1)).append(")");
            }
        }
        return builder.toString();
    }

    public static String arcColorTime(int timer) {
        return arcColorTime(timer, true);
    }

    public static String arcColorTime(int timer, boolean units) {
        StringBuilder str = new StringBuilder();
        String color = timer > 0 ? "[orange]" : "[acid]";
        timer = Math.abs(timer);
        str.append(color);
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if (m > 0) {
            str.append(m).append("[white]: ").append(color);
            if (s < 10) {
                str.append("0");
            }

            str.append(s);
            if (units) str.append("[white]min");
        } else {
            str.append(s).append("[white].").append(color).append(ms);
            if (units) str.append("[white]s");
        }
        return str.toString();
    }

    public static String fixedTime(int timer, boolean units) {
        StringBuilder str = new StringBuilder();
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if (m > 0) {
            str.append(m).append(": ");
            if (s < 10) {
                str.append("0");
            }

            str.append(s);
            if (units) str.append("min");
        } else {
            str.append(s).append(".").append(ms);
            if (units) str.append('s');
        }
        return str.toString();
    }

    public static String fixedTime(int timer) {
        return fixedTime(timer, true);
    }

    public static StringBuilder getPrefix(Color color, String type) {
        return getPrefix("#" + color, type);
    }

    public static StringBuilder getPrefix(String color, String type) {
        StringBuilder prefix = new StringBuilder();
        if (ui.chatfrag.mode == ChatFragment.ChatMode.team) prefix.append("/t ");
        prefix.append(ARCVars.arcVersionPrefix);
        prefix.append("[").append(color).append("]");
        prefix.append("<").append(type).append(">");
        prefix.append("[white]");
        return prefix;
    }

    public static String abilitysFormat(String format, Object... values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] instanceof Number n ? "[stat]" + Strings.autoFixed(n.floatValue(), 1) + "[]" : "[white]" + values[i] + "[]";
        }
        return Strings.format("[lightgray]" + format.replace("~", "[#" + ARCVars.getThemeColor() + "]~[]"), values);
    }

    public static void worldProcessor() {
        Log.info("当前地图:@", state.map.name());
        int[] data = new int[3];
        Groups.build.each(b -> {
            if (b instanceof LogicBlock.LogicBuild lb && lb.block.privileged) {
                data[0] += 1;
                data[1] += lb.code.split("\n").length + 1;
                data[2] += lb.code.length();
            }
        });
        Log.info("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]);
        ui.announce(Strings.format("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]), 10);
    }

    public static boolean has(boolean[] arr, boolean val) {
        for (boolean cur : arr) {
            if (cur == val) return true;
        }
        return false;
    }

    public static Graphics.Cursor customCursor(String name, int scale) {
        Fi path = getCursorDir();
        if (path != null && path.isDirectory()) {
            Fi child = path.child(name + ".png");
            if (child.exists()) {
                Pixmap base = new Pixmap(child);
            if (scale == 1 || OS.isAndroid || OS.isIos) {
                return Core.graphics.newCursor(base, base.width / 2, base.height / 2);
            }
            Pixmap result = Pixmaps.scale(base, base.width * scale, base.height * scale);
            base.dispose();
            return Core.graphics.newCursor(result, result.width / 2, result.height / 2);
        } else {
            return Core.graphics.newCursor(name, scale);
        }
    }

    private static Fi getCursorDir() {
        if (cursorChecked) return cachedCursor;
        cursorChecked = true;
        String path;
        Fi tmp;
        if ((path = Core.settings.getString("arcCursorPath", null)) != null && !path.isEmpty() && (tmp = new Fi(path)).isDirectory()) cachedCursor = tmp;
        return cachedCursor;
    }
}
