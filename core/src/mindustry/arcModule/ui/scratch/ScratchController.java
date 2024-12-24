package mindustry.arcModule.ui.scratch;

import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.ui.TextButton;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.I18NBundle;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.ui.scratch.block.FunctionBlock;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;

import java.util.Locale;

public class ScratchController {
    public static ScratchUI ui;
    public static ScratchRunner runner;
    public static ScratchTable selected;
    public static ScratchBlock dragging;
    public static ObjectMap<String, Integer> map;
    public static Seq<ScratchBlock> list;
    public static ScratchContext nowContext;

    private static I18NBundle bundle;

    public static void init() {
        loadBundle();
        ScratchStyles.init();
        ScratchDraw.init();
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

    public static String getLocalized(String r) {
        return bundle.get(r);
    }

    public static String getLocalized(String r, String def) {
        return bundle.get(r, def);
    }

    public static String getLocalized(String prefix, String key, String suffix, String fallback) {
        String s = bundle.getOrNull(prefix + "." + fallback + "." + key + "." + suffix);
        return s == null || s.equals("@@") ? bundle.get(prefix + "." + key + "." + suffix) : s;
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
        registerBlock(name, e, true);
    }

    public static void registerBlock(String name, ScratchBlock e, boolean show) {
        if (!list.contains(e)) {
            int id = list.add(e).size - 1;
            map.put(name, id);
            if (show) ui.registerBlock(e);
            ScratchInput.addNewInput(e);
            e.info.setID((short) id);
        }
    }

    public static void run(ScratchRunner.Task r) {
        runner.add(r);
    }

    public static void insert(ScratchBlock b) {
        runner.insert(b);
    }

    public static void reset() {
        selected = null;
        dragging = null;
        map.clear();
        list.clear();
        ui.clearData();
        nowContext.reset();
    }

    public static ScratchBlock getBlock(String name) {
        return list.get(map.get(name));
    }

    public static ScratchBlock getBlock(int i) {
        return list.get(i);
    }

    public static ScratchBlock newBlock(String name) {
        return newBlock(name, true);
    }

    public static ScratchBlock newBlock(String name, boolean drag) {
        return getBlock(name).copy(drag);
    }

    public static ScratchBlock newBlock(int i) {
        return newBlock(i, true);
    }

    public static ScratchBlock newBlock(int i, boolean drag) {
        return getBlock(i).copy(drag);
    }

    public static TextButton button(String name, Runnable callback) {
        return ui.addButton(getLocalized("button." + name + ".name"), callback);
    }

    public static void category(String name, Color color) {
        ui.addCategory(getLocalized("category." + name + ".name"), color);
    }

    public static void write(Writes w) {
        nowContext.write(w);
    }

    public static void read(Reads r) {
        nowContext.read(r);
    }

    public static void registerFunction(FunctionBlock f) {
        nowContext.registerFunction(f);
    }

    public static FunctionBlock getFunction(int id) {
        return nowContext.functions.get(id);
    }

    public static State getState() {
        return nowContext.state;
    }

    public enum State {
        saving, loading, idle, running
    }
}
