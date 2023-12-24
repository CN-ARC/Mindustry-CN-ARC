package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.style.TiledDrawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

public class ScratchUI extends Table {
    public Table table = new Table();
    public WidgetGroup group = new ScratchGroup();
    public ScrollPane pane = new ScrollPane(table, Styles.horizontalPane);
    static TiledDrawable bg;

    static {
        Pixmap pix = new Pixmap(27, 27);
        pix.fill(Color.packRgba(249, 249, 249, 255));
        pix.fillRect(0, 0, 2, 2, Color.packRgba(221, 221, 221, 255));
        bg = new TiledDrawable(new TextureRegion(new Texture(pix)));
        pix.dispose();
    }

    public ScratchUI() {
        setFillParent(true);
        table().growY().width(64f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.red));
        table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.green));
        table.add(group).size(10000);
        table.setBackground(bg);
        add(pane).grow();
        table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.sky));
    }

    public void show() {
        Core.scene.add(this);
    }

    public void hide() {
        parent.removeChild(this);
    }

    public void addElement(ScratchTable e) {
        group.addChild(e);
    }

    private static class ScratchGroup extends WidgetGroup {
        @Override
        public float getPrefWidth() {
            return 10000;
        }

        @Override
        public float getPrefHeight() {
            return 10000;
        }

        @Override
        public void layout() {
            children.each(e -> e.setBounds(e.x, e.y, e.getPrefWidth(), e.getPrefHeight()));
        }
    }
}
