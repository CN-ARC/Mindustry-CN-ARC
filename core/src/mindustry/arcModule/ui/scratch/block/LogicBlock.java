package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.event.VisibilityEvent;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.SnapshotSeq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Time;
import mindustry.Vars;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.ScratchUI;
import mindustry.arcModule.ui.scratch.builder.LogicBuildable;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.arcModule.ui.scratch.element.ListElement;
import mindustry.arcModule.ui.utils.DelayedInitListener;
import mindustry.logic.LStatement;
import mindustry.logic.LStatements;

import java.lang.reflect.Field;

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
    private static LogicBlock tmp;

    public LogicBlock(LStatement statement) {
        this(statement, false);
    }

    public LogicBlock(LStatement statement, boolean dragEnabled) {
        super(ScratchType.block, statement.category().color.cpy().value(1).saturation(0.4f), emptyInfo, dragEnabled);
        Table logicTable = new Table();
        this.statement = statement;
        flat(() -> statement.build(logicTable));
        logicTable.setFillParent(true);
        logicTable.visible = false;
        addChild(logicTable);
        marginLeft(9);
        marginRight(9);
        LogicConvertor.replaceLogicToScratch(logicTable, this, statement.name());
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
        return new LogicBlock(statement.copy(), drag);
    }

    @Override
    public void copyChildrenValue(ScratchBlock target, boolean drag) {
        super.copyChildrenValue(target, drag);
    }

    @Override
    public void buildLogic(StringBuilder builder) {
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
                ((ScratchBlock) dst).label(blockName);
                dst.row();
            } else {
                dst.clearChildren();
            }
            final Table[] t = {new Table()};
            dst.add(t[0]).left().row();
            Seq<Cell> es = src.getCells().copy();
            es.each(c -> {
                Element e = c.get();
                if (e != null) {
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
                                        String label = possible.getText().toString();
                                        s.setList(label);
                                        s.set(label);
                                    }
                                    s.update(() -> b.setPosition(s.x, s.y));
                                } else {
                                    Seq<String> str = new Seq<>(String.class);
                                    Seq<Tooltip> tooltips = new Seq<>(Tooltip.class);
                                    s = new ListElement();
                                    //buttons
                                    children.each(bb -> {
                                        str.add(((TextButton) bb).getText().toString());
                                        tooltips.add((Tooltip) bb.getListeners().find(l -> l instanceof Tooltip));
                                    });
                                    s.setList(str.toArray());
                                    s.setTooltip(tooltips.toArray());
                                    Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                    if (possible != null) s.set(possible.getText().toString());
                                }
                                ee.peek().remove();
                                ee.peek().remove();
                                s.cell(t[0].add(s));
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
                                s.cell(t[0].add(s));
                            }
                        } else {
                            //match failed
                            Log.warn("convert failed: @", ee.size);
                        }
                    } else if (e instanceof Table tt) {
                        replaceLogicToScratch(tt, t[0].add(new Table()).get(), null);
                    } else if (e instanceof Label l) {
                        LabelElement s = new LabelElement(l.getText().toString());
                        Tooltip tt = (Tooltip) l.getListeners().find(ls -> ls instanceof Tooltip);
                        if (tt != null) s.addListener(tt);
                        s.cell(t[0].add(s));
                    } else if (e instanceof TextField f) {
                        InputElement s = new InputElement(false, f.getText());
                        f.setProgrammaticChangeEvents(true);
                        s.changed(() -> f.setText(s.getText()));
                        s.cell(t[0].add(s));
                    } else {
                        t[0].add(e);
                        Log.warn("match failed @", e);
                    }
                }
                if (c.isEndRow()) {
                    dst.row();
                    t[0] = new Table();
                    dst.add(t[0]).left().row();
                }
            });
        }

        private static void wrap(Runnable r) {
            scene.clear();
            Scene old = Core.scene;
            Core.scene = scene;
            r.run();
            Core.scene = old;
        }

        public static void convert(ScratchUI ui, Seq<LStatement> ls) {
            ls.each(l -> {
                LogicBlock b;
                try {
                    b = new LogicBlock(l, true);
                } catch (Exception e) {
                    LStatements.InvalidStatement s = new LStatements.InvalidStatement();
                    b = new LogicBlock(s, true);
                    Log.err(e);
                }
                ui.addElement(b);
                if (tmp != null) {
                    b.linkTo(tmp);
                } else {
                    b.setPosition(0, 9000);
                }
                tmp = b;
            });
            tmp = null;
        }
    }
}
