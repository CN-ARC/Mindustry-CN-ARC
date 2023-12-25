package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.style.TiledDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class ScratchUI extends Table {
    public Table table = new Table();
    public WidgetGroup group = new ScratchGroup(), overlay = new WidgetGroup();
    public ScrollPane pane = new ScrollPane(table, Styles.horizontalPane);
    private static final TiledDrawable bg;
    private static final TextureRegionDrawable bg2;
    private static Label.LabelStyle ls;

    static {
        Pixmap pix = new Pixmap(27, 27);
        pix.fill(Color.packRgba(249, 249, 249, 255));
        pix.fillRect(0, 0, 2, 2, Color.packRgba(221, 221, 221, 255));
        bg = new TiledDrawable(new TextureRegion(new Texture(pix)));
        pix.dispose();
        Pixmap pix2 = new Pixmap(150, 50);
        int w = Color.white.rgba8888();
        int b = Tmp.c1.set(Color.white).mul(0.3f).rgba8888();
        pix2.fillRect(0, 15, 150, 35, w);
        pix2.drawRect(0, 15, 150, 35, b);
        for (int i = 0; i < 16; i++) {
            pix2.drawLine(74 - i, i, 76 + i, i, b);
            pix2.drawLine(75 - i, i, 75 + i, i, w);
        }
        bg2 = new TextureRegionDrawable(new TextureRegion(new Texture(pix2)));
        pix2.dispose();
    }

    public ScratchUI() {
        setFillParent(true);
        table().growY().width(64f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.red));
        table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.green));
        Stack stack = new Stack();
        stack.add(pane);
        overlay.touchable = Touchable.childrenOnly;
        stack.add(overlay);
        table.setBackground(bg);
        table.add(group);
        add(stack).grow();
        table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.sky));
        ls = new Label.LabelStyle(Styles.defaultLabel) {{
            font = Fonts.outline;
            fontColor = Color.gray;
        }};
    }

    public void showResult(ScratchBlock target, String str) {
        overlay.addChild(new Table(t -> {
            t.setBackground(bg2);
            t.add(new Label(str, ls));
            t.touchable = Touchable.disabled;
            t.setPosition(target.x + target.getWidth() - 75, target.y);
            t.addAction(Actions.moveBy(0, -15, 0.2f));
            t.update(() -> {
                if (Core.input.keyTap(KeyCode.mouseLeft)) t.remove();
            });
        }));
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
