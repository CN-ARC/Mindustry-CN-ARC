package mindustry.arcModule.ui.scratch;

import arc.func.Cons;
import arc.func.Cons4;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.actions.Actions;
import arc.scene.event.ClickListener;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.RFuncs;
import mindustry.arcModule.ui.scratch.block.ScratchBlock;
import mindustry.arcModule.ui.window.Window;
import mindustry.content.Blocks;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

import java.io.*;
import java.util.Objects;

import static arc.Core.input;
import static mindustry.arcModule.ui.scratch.ScratchController.getLocalized;

public class ScratchUI extends Table {
    public Table blocks = new Table(), types = new Table();
    public ScratchGroup group = new ScratchGroup();
    public LinkedGroup overlay = new LinkedGroup(), overlay2 = new LinkedGroup();
    public ScrollPane pane = new ScrollPane(group, Styles.horizontalPane), blocksPane = new ScrollPane(blocks, Styles.smallPane), typesPane = new OutlinePane(types, Styles.smallPane, Align.right);
    public ScratchActorPanel panel = new ScratchActorPanel();
    public Stack stack = new Stack();
    public Seq<Label> categories = new Seq<>();
    public String nowCategory = null;
    public static final float textScale = 0.8f;
    private float zoom = 1;
    private static final Vec2 v1 = new Vec2(), v2 = new Vec2();
    private static final Color hoverColor = new Color(Color.packRgba(76, 151, 255, 255));
    private static final Color themeColor = new Color(Color.packRgba(133, 92, 214, 255));
    private static byte[] tmpData;

    public ScratchUI() {
        setFillParent(true);
        blocksPane.setOverscroll(false, false);
        blocksPane.setClip(false);
        blocksPane.setTransform(true);
        blocksPane.addListener(new ClickListener());
        types.top().defaults().size(64, 48);
        types.setBackground(Tex.whiteui);
        overlay.name = "blockOverlay";
        overlay.touchable = Touchable.childrenOnly;
        overlay.setTransform(true);
        overlay2.name = "menuOverlay";
        overlay2.touchable = Touchable.childrenOnly;
        group.name = "main";
        group.setTransform(true);
        blocks.defaults().left().pad(10);
        stack.add(new Table(t -> {
            t.setFillParent(true);
            t.add(typesPane).name("types").growY().width(64);
            t.table(t2 -> {
                t2.setBackground(((TextureRegionDrawable) Tex.whiteui).tint(Tmp.c1.set(Color.white).mul(0.97f)));
                t2.add(blocksPane).name("blocks").grow();
            }).growY().width(384).get().setClip(true);
            t.add(new Stack(pane, new Table(t2 -> {
                t2.name = "zoom";
                t2.setFillParent(true);
                t2.margin(30);
                t2.bottom().right();
                t2.defaults().pad(5);
                t2.button(Icon.addSmall, ScratchStyles.flatImage, this::zoomIn).name("zoomIn").size(48).row();
                t2.button("一", ScratchStyles.flatText, this::zoomOut).name("zoomOut").size(48).row();
            }) {
                @Override
                public void drawChildren() {
                    super.drawChildren();
                    for (ScratchRunner.Task task : ScratchController.runner.getTasks()) {
                        if (task.pointer.parent == null) continue;
                        Vec2 v = oldPosToNewPos(ScratchUI.this, task.pointer, v1.set(task.pointer.x + task.pointer.getWidth(), task.pointer.y + task.pointer.getHeight() / 2), this);
                        ScratchDraw.drawArrow(v.x + x, v.y + y, Color.red);
                    }
                }
            })).name("work");
            t.table(t2 -> {
                t2.setBackground(RFuncs.tint(Color.gray));
                t2.add(new Table(t3 -> t3.setBackground(Styles.black3))).name("canvas").size(480, 360).pad(5).row();
                t2.add(panel).name("actors").grow().pad(5);
                panel.addActor(new ScratchActorPanel.ScratchActor(Blocks.logicProcessor.uiIcon, "logic1"));
            }).growY();
        }));
        stack.add(overlay);
        stack.add(overlay2);
        stack.add(new Table(t -> {
            t.defaults().size(48).pad(10);
            t.bottom();
            t.button(getLocalized("button.save.name"), ScratchStyles.flatText, () -> {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                write(new Writes(new DataOutputStream(o)));
                tmpData = o.toByteArray();
            });
            t.button(getLocalized("button.load.name"), ScratchStyles.flatText, () -> read(new Reads(new DataInputStream(new ByteArrayInputStream(tmpData))))).disabled(b -> tmpData == null);
            t.button(getLocalized("button.clear.name"), ScratchStyles.flatText, this::clearBlocks);
            t.button(getLocalized("button.reload.name"), ScratchStyles.flatText, ScratchController::reload);
        }));
        add(stack).name("container").grow();
    }

    public void read(Reads r) {
        int size = r.i();
        for (int i = 0; i < size; i++) {
            ScratchBlock sb = ScratchController.newBlock(r.s());
            sb.read(r);
            addBlock(sb);
        }
    }

    public void write(Writes w) {
        Seq<ScratchBlock> blocks = group.getChildren().select(e -> e instanceof ScratchBlock).map(e -> (ScratchBlock) e);
        w.i(blocks.size);
        for (ScratchBlock sb : blocks) {
            if (sb.info.id == -1) throw new IllegalStateException("ID is unset! block: " + sb.getClass().getSimpleName());
            w.s(sb.info.id);
            sb.write(w);
        }
    }

    @Override
    public void act(float delta) {
        ScratchController.runner.update();
        super.act(delta);
        findCategory();
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v2.set(e.x, e.y)));
        return v2;
    }

    public static Vec2 oldPosToNewPos(Group top, Element e, Vec2 v, Element target) {
        top.localToDescendantCoordinates(target, e.parent.localToAscendantCoordinates(top, v));
        return v;
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

    public void showWindow(String name, Group target) {
        overlay2.addChild(new BackgroundTable(t -> {
            t.color.a = 0;
            t.addAction(Actions.fadeIn(0.1f));
            t.touchable = Touchable.enabled;
            t.setFillParent(true);
            t.margin(50);
            t.top();
            t.add(new Table(t2 -> {
                t2.add(new BackgroundTable(t3 -> t3.stack(new Table(t4 -> t4.add(getLocalized(name)).name("title")), new Table(t4 -> {
                    t4.right();
                    t4.button(Icon.cancel, ScratchStyles.flatImage, () -> {
                        t.actions(Actions.fadeOut(0.5f), Actions.remove());
                        t2.addAction(Actions.translateBy(0, t2.getHeight() + 50, 0.5f, Interp.swingIn));
                    }).name("close").size(48).pad(4);
                })).grow(), themeColor)).height(56).growX().row();
                t2.add(target).name("body");
            })).with(t2 -> {
                float d = t2.getPrefHeight() + 50;
                t2.translation.y = d;
                t2.addAction(Actions.translateBy(0, -d, 0.5f, Interp.swingOut));
            }).fillX();
        }, themeColor.cpy().a(0.5f)));
    }

    public void showResult(ScratchTable b, String str) {
        showPopup(b, t -> {
            t.margin(10);
            t.add(new Label(str, ScratchStyles.grayOutline));
        }, Color.white);
    }

    public void showMenu(Object e) {
        overlay2.addChild(new Table(t -> {
            t.setBackground(Styles.black3);
            t.defaults().size(100, 30);
            if (e instanceof ScratchBlock sb) sb.buildMenu(t);
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
        //Core.scene.add(this);
        Window w = new Window();
        w.setBody(this);
        w.add();
    }

    public void registerBlock(ScratchBlock b) {
        blocks.add(b).row();
    }

    public void clearBlocks() {
        group.clear();
    }

    public void clearData() {
        blocks.clear();
        group.clear();
        overlay.clear();
        types.clear();
        categories.clear();
    }

    public void addBlock(ScratchTable e) {
        group.addChild(e);
    }

    public TextButton addButton(String name, Runnable callback) {
        TextButton b = new TextButton(name, Styles.squareTogglet);
        b.clicked(callback);
        blocks.add(b).size(200, 40).row();
        return b;
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
            title.setStyle(ScratchStyles.grayOutline);
            categories.add(title);
            Label l = t.add(cname).growX().get();
            ClickListener cl = new ClickListener();
            t.addListener(cl);
            l.update(() -> l.getStyle().fontColor = cl.isOver() ? hoverColor : Color.gray);
            l.setStyle(new Label.LabelStyle(ScratchStyles.grayOutline));
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

    public static class ScratchGroup extends LinkedGroup {

        @Override
        public float getPrefWidth() {
            return 10000;
        }

        @Override
        public float getPrefHeight() {
            return 10000;
        }
    }

    public static class LinkedGroup extends WidgetGroup {

        @Override
        public void act(float delta) {
            Element[] actors = children.begin();
            for (int i = 0, n = children.size; i < n; i++) {
                if (actors[i].visible) {
                    if (actors[i] instanceof ScratchBlock sb) {
                        if (sb.linkTo == null) sb.actChain(delta);
                    } else {
                        actors[i].act(delta);
                    }
                }
            }
            children.end();
        }

        @Override
        public void layout() {
            children.each(e -> e.setBounds(e.x, e.y, e.getPrefWidth(), e.getPrefHeight()));
        }
    }

    public static class ScratchActorPanel extends Table {
        public ScratchActor now;
        public ScratchActorPanel() {
            margin(6);
            setBackground(Tex.whiteui);
            top().left();
        }

        public void addActor(ScratchActor a) {
            add(a);
            now = a;
        }

        public static class ScratchActor extends Table {
            Image icon;
            String name;
            public Table label;
            public ScratchActor(TextureRegion tex, String name) {
                icon = new Image(tex);
                this.name = name;
                icon.setScaling(Scaling.fit);
                add(icon).size(58, 30).pad(5).grow().row();
                add(label = new Table(t -> t.add(name).fontScale(0.7f))).size(68, 24);
            }

            @Override
            protected void drawBackground(float x, float y) {
                Draw.color(Color.white);
                Fill.crect(x, y, width, height);
                if (ScratchController.ui.panel.now == ScratchActor.this) {
                    Draw.color(themeColor);
                    Fill.crect(x, y, width, 24);
                    Lines.rect(x, y, width, height);
                }
            }

            @Override
            public float getPrefWidth() {
                return 68;
            }

            @Override
            public float getPrefHeight() {
                return 64;
            }
        }
    }

    public static class OutlineTable extends Table {
        int dir;

        public OutlineTable(Cons<Table> cons, int dir) {
            super(cons);
            this.dir = dir;
        }

        @Override
        public void draw() {
            super.draw();
            if (dir < 2) return;
            ScratchDraw.drawOutline(x, y, width, height, dir);
        }
    }

    public static class OutlinePane extends ScrollPane {
        int dir;

        public OutlinePane(Element widget, ScrollPaneStyle style, int direction) {
            super(widget, style);
            dir = direction;
        }

        @Override
        public void draw() {
            super.draw();
            if (dir < 2) return;
            ScratchDraw.drawOutline(x, y, width, height, dir);
        }
    }

    public static class BackgroundTable extends Table {
        public Color backgroundColor;

        public BackgroundTable(Cons<Table> cons, Color backgroundColor) {
            super(cons);
            this.backgroundColor = backgroundColor;
        }

        @Override
        protected void drawBackground(float x, float y) {
            Draw.color(Tmp.c1.set(backgroundColor).mulA(color.a * parentAlpha));
            Fill.crect(x, y, width, height);
        }
    }

    public static class DrawableElement extends Element {
        Cons4<Float, Float, Float, Float> draw;

        public DrawableElement(Cons4<Float, Float, Float, Float> draw) {
            this.draw = draw;
        }

        @Override
        public void draw() {
            validate();
            draw.get(x, y, width, height);
        }
    }
}
