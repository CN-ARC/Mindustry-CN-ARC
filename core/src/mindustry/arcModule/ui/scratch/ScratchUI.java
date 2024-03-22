package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.actions.Actions;
import arc.scene.event.ClickListener;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.style.TiledDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.UIUtils;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.widgets.BoundedGroup;
import mindustry.game.EventType;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import java.util.Objects;

import static arc.Core.input;

public class ScratchUI extends Table {
    public Table blocks = new Table(), types = new Table();
    public BoundedGroup group = new ScratchGroup(), overlay = new BoundedGroup();
    public ScrollPane pane = new ScrollPane(group, Styles.horizontalPane), blocksPane = new ScrollPane(blocks, Styles.smallPane), typesPane = new ScrollPane(types) {
        @Override
        public void draw() {
            super.draw();
            Draw.color(Tmp.c1.set(Color.black).a(0.2f));
            Lines.stroke(1);
            Lines.line(x + width, y, x + width, y + height);
        }
    };
    public Stack stack = new Stack();
    public Seq<Label> categories = new Seq<>();
    public String nowCategory = null;
    private static final TiledDrawable bg;
    private static final TextureRegionDrawable bg2;
    private static final Vec2 v1 = new Vec2(), v2 = new Vec2();
    private static Label.LabelStyle ls;
    private static final Color hoverColor = new Color(Color.packRgba(76, 151, 255, 255));

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
        p.dispose();
    }

    public ScratchUI() {
        setFillParent(true);
        stack.add(new Table(t -> {
            t.setFillParent(true);
            t.add(typesPane).growY().width(64);
            types.top().defaults().size(64, 48);
            types.setBackground(Tex.whiteui);
            t.add(blocksPane).growY().width(256);
            blocksPane.addListener(new ClickListener());
            blocks.setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Tmp.c1.set(Color.white).a(0.97f)));
            t.add(pane);
            t.table().growY().width(128).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.sky));
        }));
        overlay.touchable = Touchable.childrenOnly;
        stack.add(overlay);
        group.background = bg;
        add(stack).grow();
        ls = new Label.LabelStyle(Styles.defaultLabel) {{
            font = Fonts.outline;
            fontColor = Color.gray;
        }};
        blocks.defaults().pad(10);
        Events.run(EventType.Trigger.update, this::findCategory);
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v2.set(e.x, e.y)));
        return v2;
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Vec2 v, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v));
        return v2;
    }

    public void showResult(ScratchBlock e, String str) {
        Table t;
        overlay.addChild(t = new Table(bg2));
        t.add(new Label(str, ls));
        t.touchable = Touchable.disabled;
        Vec2 v = oldPosToNewPos(stack, e, v2.set(e.x + e.getWidth() / 2 - t.getPrefWidth() / 2, e.y - e.getHeight()), overlay);
        t.setPosition(v.x, v.y);
        t.addAction(Actions.moveBy(0, -15, 0.2f));
        t.update(() -> {
            if (input.keyTap(KeyCode.mouseLeft) || input.keyTap(KeyCode.mouseRight)) t.remove();
        });
    }

    public void showMenu(Object e, boolean inStage) {
        overlay.addChild(new Table(t -> {
            t.setBackground(Styles.black3);
            t.defaults().size(100, 30);
            if (e instanceof ScratchBlock sb && inStage) {
                t.button("copy", Styles.nonet, () -> sb.getTopBlock().copyTree(true).setPosition(sb.x + 15, sb.y - 15));
                t.row();
                t.button("delete", Styles.nonet, sb::remove);
            }
            overlay.stageToLocalCoordinates(v1.set(input.mouseX(), input.mouseY()));
            t.setPosition(v1.x, v1.y - t.getPrefHeight());
            t.getChildren().forEach(c -> UIUtils.clicked(c, t::remove));
            t.addListener(new ClickListener());
            t.update(() -> {
                if (!((ClickListener) t.getListeners().find(ee -> ee instanceof ClickListener)).isOver()) if ((input.keyTap(KeyCode.mouseLeft) || input.keyTap(KeyCode.mouseRight))) t.remove();
            });
        }));
    }

    public void createWindow() {
        Core.scene.add(this);
        /*Window w = new Window();
        w.setBody(this);
        w.add();*/
    }

    public void addBlock(ScratchBlock b) {
        blocks.add(b).align(Align.left).row();
    }

    public void clearBlocks() {
        blocks.clear();
        group.clear();
        overlay.clear();
        types.clear();
        categories.clear();
    }

    public void addElement(ScratchTable e) {
        group.addChild(e);
    }

    public void addCategory(String cname, Color c) {
        types.table(t -> {
            t.add(new Element() {
                @Override
                public void draw() {
                    Draw.color(c);
                    float cx = x + width / 2, cy = y + height / 2;
                    Fill.circle(cx, cy, 11);
                    Draw.color(Tmp.c1.set(Color.black).a(0.3f));
                    Lines.stroke(1.5f);
                    Lines.circle(cx, cy, 11);
                }
            }).grow().row();
            Label title = blocks.add(cname).ellipsis(true).growX().get();
            title.setStyle(ls);
            categories.add(title);
            Label l = t.add(cname).growX().get();
            ClickListener cl = new ClickListener();
            t.addListener(cl);
            l.update(() -> l.getStyle().fontColor = cl.isOver() ? hoverColor : Color.gray);
            l.setStyle(new Label.LabelStyle(ls));
            l.setFontScale(0.8f);
            l.setAlignment(Align.center);
            t.clicked(() -> blocksPane.setScrollY(blocks.getHeight() - title.getY(Align.top) - 10));
            t.touchable = Touchable.enabled;
        }).update(t -> t.setBackground(Objects.equals(nowCategory, cname) ? Styles.black3 : null)).row();
        blocks.row();
    }

    public void findCategory() {
        Label prev = null;
        for (Label l : categories) {
            if (l.y + l.parent.y < types.getHeight() - typesPane.getScrollY() - l.getHeight() - 20) {
                nowCategory = String.valueOf(prev == null ? categories.get(0).getText() : prev.getText());
                return;
            }
            prev = l;
        }
    }

    private static class ScratchGroup extends BoundedGroup {
        @Override
        public float getPrefWidth() {
            return 10000;
        }

        @Override
        public float getPrefHeight() {
            return 10000;
        }
    }
}
