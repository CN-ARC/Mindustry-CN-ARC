package mindustry.arcModule.ui.scratch;

import arc.Core;
import arc.Events;
import arc.func.Cons;
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
import arc.scene.ui.*;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.widgets.BoundedGroup;
import mindustry.arcModule.ui.window.Window;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import java.util.Objects;

import static arc.Core.input;

public class ScratchUI extends Table {
    public Table blocks = new Table(), types = new Table();
    public BoundedGroup group = new ScratchGroup(), overlay = new BoundedGroup(), overlay2 = new BoundedGroup();
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
    private float zoom = 1;
    private static final TiledDrawable bg;
    private static final Vec2 v1 = new Vec2(), v2 = new Vec2();
    private static Label.LabelStyle ls;
    private static final Color hoverColor = new Color(Color.packRgba(76, 151, 255, 255));

    static {
        Pixmap pix = new Pixmap(27, 27);
        pix.fill(Color.packRgba(249, 249, 249, 255));
        pix.fillRect(0, 0, 2, 2, Color.packRgba(221, 221, 221, 255));
        bg = new TiledDrawable(new TextureRegion(new Texture(pix)));
        pix.dispose();
    }

    public ScratchUI() {
        setFillParent(true);
        stack.add(new Table(t -> {
            t.setFillParent(true);
            t.add(typesPane).growY().width(64);
            types.top().defaults().size(64, 48);
            types.setBackground(Tex.whiteui);
            t.add(blocksPane).growY().width(384);
            blocksPane.addListener(new ClickListener());
            blocks.setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Tmp.c1.set(Color.white).a(0.97f)));
            t.add(new Stack(pane, new Table(t2 -> {
                t2.setFillParent(true);
                t2.margin(30);
                t2.bottom().right();
                t2.defaults().pad(5);
                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle(Styles.cleari) {{
                    up = Styles.black3;
                    over = Styles.black6;
                    down = Styles.black8;
                }};
                t2.button(Icon.addSmall, style, this::zoomIn).size(48).row();
                t2.button("一", new TextButton.TextButtonStyle(Styles.defaultt) {{
                    up = Styles.black3;
                    over = Styles.black6;
                    down = Styles.black8;
                }}, this::zoomOut).size(48).row();
            })));
            t.table().growY().width(128).get().setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Color.sky));
        }));
        overlay.touchable = Touchable.childrenOnly;
        overlay.setTransform(true);
        overlay2.touchable = Touchable.childrenOnly;
        stack.add(overlay);
        stack.add(overlay2);
        group.background = bg;
        group.setTransform(true);
        add(stack).grow();
        ls = new Label.LabelStyle(Styles.defaultLabel) {{
            font = Fonts.outline;
            fontColor = Color.gray;
        }};
        blocks.defaults().pad(10);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        findCategory();
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v2.set(e.x, e.y)));
        return v2;
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Vec2 v, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v));
        return v2;
    }

    public void zoomIn() {
        zoom = Math.min(zoom + 0.1f, 5);
        setScale(zoom);
    }

    public void zoomOut() {
        zoom = Math.max(zoom - 0.1f, 0.1f);
        setScale(zoom);
    }

    @Override
    public void setScale(float scl) {
        group.setScale(scl);
        overlay.setScale(scl);
    }

    public void showPopup(ScratchTable b, Cons<Table> builder, Color col) {
        Table t;
        overlay2.addChild(t = new Table() {
            @Override
            protected void drawBackground(float x, float y) {
                ScratchDraw.drawPopup(x - 1, y - 1, width + 2, height + 2, col);
            }
        });
        t.touchable = Touchable.disabled;
        builder.get(t);
        Vec2 v = oldPosToNewPos(stack, b, v2.set(b.x + b.getWidth() / 2, b.y), overlay2);
        t.setPosition(v.x - t.getPrefWidth() / 2, v.y - t.getPrefHeight());
        t.actions(Actions.moveBy(0, -15, 0.2f), Actions.touchable(Touchable.enabled), Actions.forever(Actions.run(() -> {
            Vec2 v3 = oldPosToNewPos(stack, b, v2.set(b.x + b.getWidth() / 2, b.y), overlay2);
            t.setPosition(v3.x - t.getPrefWidth() / 2, v3.y - t.getPrefHeight() - 15);
        })));
        ClickListener c = new ClickListener();
        t.addListener(c);
        t.update(() -> {
            if (!c.isOver() && (input.keyTap(KeyCode.mouseLeft) || input.keyTap(KeyCode.mouseRight))) t.remove();
        });
    }

    public void showResult(ScratchTable b, String str) {
        showPopup(b, t -> {
            t.margin(10);
            t.add(new Label(str, ls));
        }, Color.white);
    }

    public void showMenu(Object e, boolean inStage) {
        overlay2.addChild(new Table(t -> {
            t.setBackground(Styles.black3);
            t.defaults().size(100, 30);
            if (e instanceof ScratchBlock sb && inStage) {
                t.button("copy", Styles.nonet, () -> sb.getTopBlock().copyTree(true).setPosition(sb.x + 15, sb.y - 15));
                t.row();
                t.button("delete", Styles.nonet, sb::remove);
            }
            overlay2.stageToLocalCoordinates(v1.set(input.mouseX(), input.mouseY()));
            t.setPosition(v1.x, v1.y - t.getPrefHeight());
            t.getChildren().forEach(c -> c.clicked(t::remove));
            ClickListener c = new ClickListener();
            t.addListener(c);
            t.update(() -> {
                if (!c.isOver() && (input.keyTap(KeyCode.mouseLeft) || input.keyTap(KeyCode.mouseRight))) t.remove();
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
