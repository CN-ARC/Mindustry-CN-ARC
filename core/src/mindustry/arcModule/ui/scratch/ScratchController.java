package mindustry.arcModule.ui.scratch;

import arc.files.Fi;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.I18NBundle;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

import java.util.Locale;

public class ScratchController {
    public static ScratchUI ui;
    public static ScratchRunner runner;
    public static ScratchTable dragging, selected;
    public static ObjectMap<String, Integer> map;
    public static Seq<ScratchBlock> list;
    public static ScratchContext nowContext;

    private static I18NBundle bundle;

    public static void init() {
        loadBundle();
        setContext(ScratchContext.createContext());
    }

    public static void reload() {
        ui.remove();
        ScratchTest.test();
    }

    public static void loadBundle() {
        Fi handle = new Fi("E:\\Mindustry\\lstbundles");
        bundle = I18NBundle.createBundle(handle, Locale.ENGLISH);
    }

    public static String getText(String r) {
        return bundle.get(r);
    }

    public static String getText(String r, String def) {
        return bundle.get(r, def);
    }

    public static void setContext(ScratchContext context) {
        if (nowContext == context) return;
        if (nowContext != null) saveContext(nowContext);
        nowContext = context;
        if (context != null) loadContext(context);
    }

    public static void loadContext(ScratchContext context) {
        ui = context.ui;
        runner = context.runner;
        dragging = context.dragging;
        selected = context.selected;
        map = context.map;
        list = context.list;
    }

    public static void saveContext(ScratchContext context) {
        context.ui = ui;
        context.runner = runner;
        context.dragging = dragging;
        context.selected = selected;
        context.map = map;
        context.list = list;
    }

    public static void registerBlock(String name, ScratchBlock e) {
        if (!list.contains(e)) {
            int id = list.add(e).size - 1;
            map.put(name, id);
            ui.registerBlock(e);
            ScratchInput.addNewInput(e);
            e.info.setID((short) id);
        }
    }

    public static void run(ScratchBlock.Run r) {
        runner.add(r);
    }

    public static void insert(ScratchBlock b) {
        runner.insert(b);
    }

    public static void reset() {
        dragging = selected = null;
        map.clear();
        list.clear();
        ui.clearData();
    }

    public static ScratchBlock get(String name) {
        return list.get(map.get(name));
    }

    public static ScratchBlock get(int i) {
        return list.get(i);
    }

    public static void category(String name, Color color) {
        ui.addCategory(getText("category." + name + ".name"), color);
    }
}
