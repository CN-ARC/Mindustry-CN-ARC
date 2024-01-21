package mindustry.arcModule.ui;

import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;

public class UIUtils {
    public static void clicked(Element group, Runnable c) {
        group.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                c.run();
            }
        });
    }
}
