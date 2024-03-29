package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.ClickListener;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.struct.SnapshotSeq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.arcModule.ui.scratch.ScratchType;
import mindustry.arcModule.ui.scratch.ScratchUI;
import mindustry.arcModule.ui.scratch.builder.LogicBuildable;
import mindustry.arcModule.ui.scratch.element.InputElement;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.arcModule.ui.scratch.element.ListElement;
import mindustry.logic.LStatement;
import mindustry.logic.LStatements;

public class LogicBlock extends ScratchBlock implements LogicBuildable {
    public LStatement statement;
    private final Table logicTable;
    private static LogicBlock tmp;
    public LogicBlock(LStatement statement) {
        this(statement, false);
    }

    public LogicBlock(LStatement statement, boolean dragEnabled) {
        super(ScratchType.block, statement.category().color, emptyInfo, dragEnabled);
        logicTable = new Table();
        this.statement = statement;
        statement.build(logicTable);
        logicTable.setFillParent(true);
        logicTable.visible = false;
        replaceLogicToScratch(logicTable, this, statement.name());
    }

    @Override
    public void act(float delta) {
        super.act(delta);
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

    @SuppressWarnings("rawtypes")
    public static void replaceLogicToScratch(Table src, Table dst, String name) {
        src.act(Time.delta);
        dst.clearChildren();
        if (name != null) {
            ((ScratchBlock) dst).label(name).get().marginRight(9);
            dst.row();
        }
        final Table[] t = {new Table()};
        dst.add(t[0]).left().row();
        Seq<Cell> es = src.getCells().copy();
        es.each(c -> {
            Element e = c.get();
            if (e != null) {
                if (e instanceof Button b) {
                    int size = Core.scene.getElements().size;
                    b.fireClick();
                    Seq<Element> ee = Core.scene.getElements();
                    if (ee.size == size + 2) {
                        //Buttons?
                        try {
                            Seq<String> str = new Seq<>(String.class);
                            SnapshotSeq<Element> children = ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren();
                            ListElement s;
                            if (children.contains(e2 -> !(e2 instanceof Button))) {
                                //not buttons
                                s = new ListElement() {
                                    @Override
                                    public void showList() {
                                        b.fireClick();
                                        ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget().clicked(() -> replaceLogicToScratch(src, dst, name));
                                    }
                                };
                                Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                if (possible != null) {
                                    String label = possible.getText().toString();
                                    s.setList(new String[]{label});
                                    s.set(label);
                                }
                                s.update(() -> b.setPosition(s.x, s.y));
                            } else {
                                s = new ListElement();
                                //buttons
                                children.each(bb -> str.add(((TextButton) bb).getText().toString()));
                                s.setList(str.toArray());
                                Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                                if (possible != null) s.set(possible.getText().toString());
                            }
                            ee.peek().remove();
                            ee.peek().remove();
                            s.cell(t[0].add(s));
                            s.changed(() -> {
                                b.fireClick();
                                ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren().get(s.index()).fireClick();
                                replaceLogicToScratch(src, dst, name);
                            });
                        } catch (Exception err) {
                            Vars.ui.showException(err);
                        }
                    } else if (ee.size == size + 1 || ee.size == size) {
                        //Dialog?
                        Element d = ee.peek();
                        if (d instanceof Dialog dialog && dialog.getActions().size != 0) {
                            dialog.clearActions();
                            dialog.hide(null);
                            ListElement s = new ListElement() {
                                @Override
                                public void showList() {
                                    b.fireClick();//TODO rebuild
                                }
                            };
                            Label possible = ((Label) b.getChildren().find(el -> el instanceof Label));
                            if (possible != null) {
                                String label = possible.getText().toString();
                                s.setList(new String[]{label});
                                s.set(label);
                            }
                            s.cell(t[0].add(s));
                        }
                    } else {
                        //match failed
                        Log.warn("convert failed: size @ @, last @", size, ee.size, ee.peek());
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
    }
}
