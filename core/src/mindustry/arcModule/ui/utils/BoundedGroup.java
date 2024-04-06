package mindustry.arcModule.ui.utils;

import arc.scene.style.Drawable;
import arc.scene.ui.layout.WidgetGroup;

public class BoundedGroup extends WidgetGroup {
    public Drawable background = null;
    @Override
    public void layout() {
        children.each(e -> e.setBounds(e.x, e.y, e.getPrefWidth(), e.getPrefHeight()));
    }

    @Override
    public void draw() {
        validate();
        if (background != null) background.draw(x, y, width, height);
        super.draw();
    }
}
