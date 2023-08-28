package mindustry.arcModule.ui.logic;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.arcModule.ui.logic.blockbase.CondBlock;
import mindustry.arcModule.ui.logic.blockbase.InputBlock;
import mindustry.arcModule.ui.window.Window;

public class Test {
    public static void test() {
        ScratchController.init();
        testBlocks();
        testUI();
    }

    public static void testSwitch() {
        Table t = new Table();
        t.table(tt -> tt.setBackground(ScratchStyles.createSwitchBackground(20, 30, 20, Color.white))).pad(1).grow();

        Window w = Vars.ui.WindowManager.createWindow();
        w.setBody(t);
    }

    public static void testBlocks() {
        ScratchController.ui.addElement(new CondBlock(new Element[]{ScratchComponents.cond(), new Label("&"), ScratchComponents.cond()}));
        ScratchController.ui.addElement(new CondBlock(new Element[]{ScratchComponents.input(""), new Label(">"), ScratchComponents.input("")}));
        ScratchController.ui.addElement(new InputBlock(new Element[]{ScratchComponents.input(""), new Label("+"), ScratchComponents.input("")}));
        ScratchController.ui.addElement(new InputBlock(new Element[]{ScratchComponents.input(""), new Label("+"), ScratchComponents.input("")}));
        ScratchController.ui.addElement(new InputBlock(new Element[]{ScratchComponents.input(""), new Label("+"), ScratchComponents.input("")}));
    }

    public static void testUI() {
        ScratchController.ui.show();
    }
}
