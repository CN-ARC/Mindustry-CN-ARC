package mindustry.arcModule.ui.scratch.utils;

import arc.scene.ui.layout.Table;
import mindustry.arcModule.ui.window.Window;
import mindustry.ui.Styles;

public class UIBuilder extends Table {

    public UIBuilder() {
        setFillParent(true);
        add(new UISidebar()).growY().width(200f);
        add(new UIGroup()).grow();
        add(new UIAttributes()).growY().width(200f);
    }

    public static void buildWindow() {
        Window w = new Window();
        w.setBody(new UIBuilder());
        w.add();
    }

    public class UISidebar extends Table {
        public UISidebar() {
            setBackground(Styles.black3);
        }
    }

    public class UIGroup extends Table {
        public UIGroup() {
            setBackground(Styles.grayPanel);
        }
    }

    public class UIAttributes extends Table {
        public UIAttributes() {
            setBackground(Styles.black5);
        }
    }
}
