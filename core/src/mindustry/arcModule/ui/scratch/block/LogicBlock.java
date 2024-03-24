package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
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
    private static LogicBlock tmp;
    public LogicBlock(Color color, LStatement statement) {
        this(color, statement, false);
    }

    public LogicBlock(Color color, LStatement statement, boolean dragEnabled) {
        super(ScratchType.block, color, emptyInfo, dragEnabled);
        Table t = new Table();
        this.statement = statement;
        statement.build(t);
        t.setFillParent(true);
        t.visible = false;
        replaceLogicToScratch(t, this, statement.name());
    }

    @Override
    public LogicBlock copy(boolean drag) {
        return new LogicBlock(elemColor, statement.copy(), drag);
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
            if (e instanceof Button b) {
                Seq<String> str = new Seq<>(String.class);
                int size = Core.scene.getElements().size;
                b.fireClick();
                Seq<Element> ee = Core.scene.getElements();
                if (ee.size == size + 2) {
                    try {
                        ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren().each(bb -> str.add(((TextButton) bb).getText().toString()));
                        ee.peek().remove();
                        ee.peek().remove();
                    } catch (Exception err) {
                        Vars.ui.showException(err);
                    }
                    ListElement s = new ListElement(str.toArray());
                    s.set(((Label) b.getChildren().find(el -> el instanceof Label)).getText().toString());
                    s.changed(() -> {
                        b.fireClick();
                        ((Table) ((ScrollPane) ((Table) ee.peek()).getChildren().peek()).getWidget()).getChildren().get(s.index()).fireClick();
                        replaceLogicToScratch(src, dst, name);
                    });
                    s.cell(t[0].add(s));
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
                b = new LogicBlock(l.category().color, l);
            } catch (Exception e) {
                LStatements.InvalidStatement s = new LStatements.InvalidStatement();
                b = new LogicBlock(s.category().color, s, true);
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
