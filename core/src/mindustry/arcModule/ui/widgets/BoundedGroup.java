package mindustry.arcModule.ui.widgets;

import arc.scene.ui.layout.WidgetGroup;

public class BoundedGroup extends WidgetGroup {
    @Override
    public void layout() {
        children.each(e -> e.setBounds(e.x, e.y, e.getPrefWidth(), e.getPrefHeight()));
    }
}
