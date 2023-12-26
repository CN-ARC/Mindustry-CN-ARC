package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.style.TiledDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Tmp;
import mindustry.arcModule.ui.RStyles;
import mindustry.arcModule.ui.scratch.blocks.ScratchBlock;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static arc.Core.input;

public class ScratchUI extends Table {
    public Table table = new Table();
    public WidgetGroup group = new ScratchGroup(), overlay = new WidgetGroup();
    public ScrollPane pane = new ScrollPane(table, Styles.horizontalPane);
    public Stack stack = new Stack();
    private static final TiledDrawable bg;
    private static final TextureRegionDrawable bg2;
    private static final Vec2 vec = new Vec2();
    private static Label.LabelStyle ls;

    static {
        Pixmap pix = new Pixmap(27, 27);
        pix.fill(Color.packRgba(249, 249, 249, 255));
        pix.fillRect(0, 0, 2, 2, Color.packRgba(221, 221, 221, 255));
        bg = new TiledDrawable(new TextureRegion(new Texture(pix)));
        pix.dispose();
        Pixmap p = new Pixmap(150, 50);
        int w = Color.white.rgba8888();
        int b = Tmp.c1.set(Color.white).mul(0.3f).rgba8888();
        p.fillRect(0, 10, 150, 40, w);
        p.drawRect(0, 10, 150, 40, b);
        for (int i = 0; i < 11; i++) {
            p.drawLine(74 - i, i, 76 + i, i, b);
            p.drawLine(75 - i, i, 75 + i, i, w);
        }
        bg2 = new TextureRegionDrawable(new TextureRegion(new Texture(p)));
    }

    public ScratchUI() {
        setFillParent(true);
        stack.add(new Table(t -> {
            t.setFillParent(true);
            t.table().growY().width(64f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.red));
            t.table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.green));
            t.add(pane);
            t.table().growY().width(128f).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.sky));
        }));
        overlay.touchable = Touchable.childrenOnly;
        stack.add(overlay);
        table.setBackground(bg);
        table.add(group);
        add(stack).grow();
        ls = new Label.LabelStyle(Styles.defaultLabel) {{
            font = Fonts.outline;
            fontColor = Color.gray;
        }};
    }

    public void showResult(ScratchBlock e, String str) {
        Table t;
        overlay.addChild(t = new Table(bg2));
        t.add(new Label(str, ls));
        t.touchable = Touchable.disabled;
        stack.localToDescendantCoordinates(overlay, table.localToAscendantCoordinates(stack, vec.set(e.x + e.getWidth() / 2 - t.getPrefWidth() / 2, e.y)));
        t.setPosition(vec.x, vec.y);
        t.addAction(Actions.moveBy(0, -15, 0.2f));
        t.update(() -> {
            if (Core.input.keyTap(KeyCode.mouseLeft) || Core.input.keyTap(KeyCode.mouseRight)) t.remove();
        });
    }

    public void showMenu(ScratchBlock e) {
        overlay.addChild(new Table(t -> {
            t.setBackground(RStyles.black1);
            t.defaults().size(100, 30);
            t.button("copy", Styles.nonet, () -> {
                ScratchBlock b = e.copy();
                group.addChild(b);
                b.setPosition(e.x + 15, e.y - 15);
            });
            t.row();
            t.button("delete", Styles.nonet, e::remove);
            stack.localToDescendantCoordinates(overlay, table.localToAscendantCoordinates(stack, vec.set(e.x + e.getWidth() / 2 - t.getPrefWidth() / 2, e.y)));
            t.setPosition(vec.x, vec.y);
            t.getChildren().forEach(c -> c.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    t.remove();
                }
            }));
            t.update(() -> {
                boolean[] b = {true};
                t.getChildren().forEach(element -> {
                    element.stageToLocalCoordinates(vec.set(input.mouseX(), input.mouseY()));
                    if (((ClickListener) element.getListeners().find(ee -> ee instanceof ClickListener)).isOver(element, vec.x, vec.y)) {
                        b[0] = false;
                    }
                });
                if ((Core.input.keyTap(KeyCode.mouseLeft) || Core.input.keyTap(KeyCode.mouseRight)) && b[0]) t.remove();
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
