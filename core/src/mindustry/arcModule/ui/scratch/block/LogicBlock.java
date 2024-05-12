package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.VisibilityEvent;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.SnapshotSeq;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.arcModule.ui.scratch.BlockInfo;
import mindustry.arcModule.ui.scratch.ScratchController;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.ScratchUI;
import mindustry.arcModule.ui.scratch.builder.LogicBuildable;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.arcModule.ui.scratch.element.ListElement;
import mindustry.arcModule.ui.utils.DelayedInitListener;
import mindustry.gen.LogicIO;
import mindustry.gen.Tex;
import mindustry.logic.*;

import java.lang.reflect.Field;
import java.util.StringTokenizer;

import static mindustry.arcModule.ui.scratch.ScratchController.getLocalized;

public class LogicBlock extends ScratchBlock implements LogicBuildable {
    private static final Field sclField;
    static {
        try {
            sclField = Scl.class.getDeclaredField("product");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        sclField.setAccessible(true);
    }
    public LStatement statement;
    public ScratchBlock jump = null;
    private static LogicBlock tmp;


    public LogicBlock(LStatement statement) {
        this(statement, new BlockInfo());
    }

    public LogicBlock(LStatement statement, boolean dragEnabled) {
        this(statement, new BlockInfo(), dragEnabled);
    }

    public LogicBlock(LStatement statement, BlockInfo info) {
        this(statement, info, false);
    }

    public LogicBlock(LStatement statement, BlockInfo info, boolean dragEnabled) {
        super(ScratchType.block, Color.white, info, dragEnabled);
        marginLeft(9);
        marginRight(9);
        if (statement != null) build(statement);
    }

    public void build(LStatement statement) {
        clearChildren();
        elemColor = statement.category().color;
        Table logicTable = new Table();
        this.statement = statement;
        flat(() -> statement.build(logicTable));
        logicTable.setFillParent(true);
        logicTable.visible = false;
        addChild(logicTable);
        LogicConvertor.replaceLogicToScratch(logicTable, this, statement.getClass().getSimpleName().replace("Statement", ""));
    }

    private static void flat(Runnable r) {
        try {
            float n = (float) sclField.get(null);
            Scl.setProduct(-1);
            r.run();
            Scl.setProduct(n);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LogicBlock copy(boolean drag) {
        return new LogicBlock(statement.copy(), info, drag);
    }

    @Override
    public void copyChildrenValue(ScratchBlock target, boolean drag) {
        super.copyChildrenValue(target, drag);
    }

    @Override
    public void buildLogic(StringBuilder builder) {
    }

    @Override
    public void readElements(Reads r) {
        build(LAssembler.read(r.str(), true).first());
    }

    @Override
    public void writeElements(Writes w) {
        StringBuilder sb = new StringBuilder();
        LogicIO.write(statement, sb);
        w.str(sb.toString());
    }

    protected static void drawCurve(float x1, float y1, float x2, float y2, boolean left) {
        Lines.stroke(4f);
        if (left) {
            float dist = Math.abs(x1 - x2) / 2f;
            Lines.curve(x1, y1, x1 + dist, y1, x2 - dist, y2, x2, y2, Math.max(4, (int) (Mathf.dst(x1, y1, x2, y2) / 4f)));
        } else {
            Lines.curve(x1, y1, x1 + 100, y1, x2 + 100, y2, x2, y2, Math.max(18, (int)(Mathf.dst(x1, y1, x2, y2) / 6)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class LogicConvertor {
        private static final Scene scene = new Scene(Core.scene.getViewport());
        static {
            try {
                Field f = Scene.class.getDeclaredField("styleDefaults");
                f.setAccessible(true);
                ((ObjectMap<Class, Object>) f.get(scene)).putAll((ObjectMap<Class, Object>) f.get(Core.scene));
            } catch (Exception ignored) {
            }
        }

        public static void replaceLogicToScratch(Table src, Table dst, String blockName) {
            src.act(Time.delta);
            if (blockName != null) {
                Seq<Element> keep = new Seq<>();
                dst.getChildren().each(c -> {
                    if (dst.getCell(c) == null) keep.add(c);
                });
                dst.clearChildren();
                keep.each(dst::addChild);
                ((ScratchBlock) dst).label(getLocalized("block." + blockName + ".name"));
                dst.row();
            } else {
                dst.clearChildren();
            }
            Table t = new Table();
            dst.add(t).left().row();
            Seq<Cell> es = src.getCells().copy();
            for (Cell c : es) {
                Element e = c.get();
                if (e instanceof LCanvas.JumpButton) {
                    //special: jump
                    t.add(new ImageButton(Tex.logicNode, new ImageButton.ImageButtonStyle() {{
                        imageUpColor = Color.white;
                    }}) {
                        private final ClickListener l = new ClickListener();
                        private boolean touched;
                        private float lastX, lastY;
                        {
                            addListener(l);
                            addListener(new InputListener() {
                                @Override
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                                    ScratchController.ui.pane.setFlickScroll(false);
                                    Vec2 v = localToAscendantCoordinates(ScratchController.ui.group, Tmp.v1.set(x, y));
                                    lastX = v.x;
                                    lastY = v.y;
                                    event.cancel();
                                    return touched = true;
                                }

                                @Override
                                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                                    touched = false;
                                    ScratchController.ui.pane.setFlickScroll(true);
                                }

                                @Override
                                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                                    Vec2 v = localToAscendantCoordinates(ScratchController.ui.group, Tmp.v1.set(x, y));
                                    lastX = v.x;
                                    lastY = v.y;
                                }
                            });
                            update(() -> getStyle().imageUpColor = touched ? Color.gold : Color.white);
                        }
                        @Override
                        public void draw() {
                            boolean left = lastX < parent.getWidth() / 2;
                            Element e = ScratchController.ui.group.hit(lastX, lastY, true);
                            ScratchBlock sb = null;
                            if (e != null) {
                                Element p = e;
                                while (!(p instanceof ScratchBlock)) {
                                    p = p.parent;
                                    if (p == null) break;
                                }
                                if (p != null && p.parent != ScratchController.ui.group) sb = (ScratchBlock) p;
                            }

                            super.draw();
                            Draw.color(touched || l.isOver() ? Color.gold : Color.white);
                            if (sb != null) {
                                drawCurve(x + width - 8, y + height / 2, sb.x, sb.y, left);
                            } else if (touched) drawCurve(x + width - 8, y + height / 2, lastX, lastY, left);
                        }
                    }).size(30).padLeft(-8);
                } else if (e != null) {
                    if (e instanceof Button b) {
                        wrap(b::fireClick);
                        Seq<Element> ee = scene.getElements();
                        if (ee.size == 2) {
                            //Buttons?
                            try {
                                SnapshotSeq<Element> children = ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren();
                                ListElement s;
                                if (children.contains(e2 -> !(e2 instanceof Button))) {
                                    //not buttons
                                    s = new ListElement() {
                                        @Override
                                        public void showList() {
                                            b.fireClick();
                                            ((ScrollPane) ((Table) Core.scene.getElements().peek()).getChildren().peek()).getWidget().clicked(() -> replaceLogicToScratch(src, dst, blockName));
                                        }
                                    };
                                    Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                    if (possible != null) {
                                        String label = getLocalized("list", possible.getText().toString(), "name", blockName);
                                        s.setList(label);
                                        s.set(label);
                                    }
                                    s.update(() -> b.setPosition(s.x, s.y));
                                } else {
                                    //buttons
                                    Seq<String> str = new Seq<>(String.class);
                                    Seq<Tooltip> tooltips = new Seq<>(Tooltip.class);
                                    s = new ListElement();
                                    children.each(bb -> {
                                        str.add(getLocalized("list", ((TextButton) bb).getText().toString(), "name", blockName));
                                        tooltips.add((Tooltip) bb.getListeners().find(l -> l instanceof Tooltip));
                                    });
                                    s.setList(str.toArray());
                                    s.setTooltip(tooltips.toArray());
                                    Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                    if (possible != null) s.set(getLocalized("list." + possible.getText().toString().replace(" ", "") + ".name"));
                                    s.changed(() -> {
                                        Scene old = Core.scene;
                                        Core.scene = scene;
                                        b.fireClick();
                                        Core.scene = old;
                                        ((Table) ((ScrollPane) ((Table) scene.getElements().peek()).getChildren().peek()).getWidget()).getChildren().get(s.index()).fireClick();
                                        replaceLogicToScratch(src, dst, blockName);
                                    });
                                }
                                ee.peek().remove();
                                ee.peek().remove();
                                s.cell(t.add(s));
                                s.changed(() -> {
                                    wrap(b::fireClick);
                                    ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren().get(s.index()).fireClick();
                                    replaceLogicToScratch(src, dst, blockName);
                                });
                            } catch (Exception err) {
                                Vars.ui.showException(err);
                            }
                        } else if (ee.size == 1) {
                            //Dialog?
                            Element d = ee.peek();
                            if (d instanceof Dialog dialog && dialog.getActions().size != 0) {
                                dialog.clearActions();
                                dialog.hide(null);
                                d.remove();
                                ListElement s = new ListElement() {
                                    @Override
                                    public void showList() {
                                        b.fireClick();
                                        DelayedInitListener l = new DelayedInitListener();
                                        d.addListener(l.setListener(e -> {
                                            if (e instanceof VisibilityEvent v) {
                                                l.remove();
                                                d.removeListener(l);
                                                if (v.isHide()) replaceLogicToScratch(src, dst, blockName);
                                            }
                                            return false;
                                        }));
                                    }
                                };
                                Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                if (possible != null) {
                                    String label = possible.getText().toString();
                                    s.setList(label);
                                    s.set(label);
                                }
                                s.cell(t.add(s));
                            }
                        } else {
                            //match failed
                            Log.warn("convert failed: @ @", ee.size, b);
                        }
                    } else if (e instanceof Table tt) {
                        replaceLogicToScratch(tt, t.add(new Table()).get(), null);
                    } else if (e instanceof Label l) {
                        LabelElement s = new LabelElement(replace(l.getText().toString(), blockName));
                        Tooltip tt = (Tooltip) l.getListeners().find(ls -> ls instanceof Tooltip);
                        if (tt != null) s.addListener(tt);
                        s.cell(t.add(s));
                    } else if (e instanceof TextField f) {
                        InputElement s = new InputElement(false, f.getText());
                        f.setProgrammaticChangeEvents(true);
                        s.changed(() -> f.setText(s.getText()));
                        s.cell(t.add(s));
                    } else {
                        t.add(e);
                        Log.warn("match failed @", e);
                    }
                }
                if (c.isEndRow()) {
                    dst.row();
                    t = new Table();
                    dst.add(t).left().row();
                }
            }
            scene.clear();
        }

        private static void wrap(Runnable r) {
            scene.clear();
            Scene old = Core.scene;
            Core.scene = scene;
            r.run();
            Core.scene = old;
        }

        public static LogicBlock from(LStatement l) {
            LogicBlock block = (LogicBlock) ScratchController.newBlock(l.name());
            block.build(l);
            return block;
        }

        public static void convert(ScratchUI ui, Seq<LStatement> ls) {
            ls.each(l -> {
                LogicBlock b;
                try {
                    b = from(l);
                } catch (Exception e) {
                    b = from(new LStatements.InvalidStatement());
                    Log.err(e);
                }
                ui.addBlock(b);
                if (tmp != null) {
                    b.linkTo(tmp);
                } else {
                    b.setPosition(0, 9900);
                }
                tmp = b;
            });
            tmp = null;
        }
    }

    private static String replace(String src, String block) {
        StringTokenizer tokenizer = new StringTokenizer(src.trim(), " ");
        StringBuilder builder = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            builder.append(getLocalized("elem", token, "name", block));
        }
        return builder.toString();
    }
}
