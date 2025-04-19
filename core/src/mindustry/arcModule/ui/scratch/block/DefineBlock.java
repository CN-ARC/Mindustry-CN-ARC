package mindustry.arcModule.ui.scratch.block;

import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.style.BaseDrawable;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.arcModule.RFuncs;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.arcModule.ui.scratch.element.ScratchElement;
import mindustry.arcModule.ui.scratch.element.UserElement;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static arc.Core.input;
import static mindustry.arcModule.ui.scratch.ScratchController.getLocalized;
import static mindustry.arcModule.ui.scratch.ScratchController.getState;

public class DefineBlock extends ScratchBlock {
    public static final String defineName = "define", functionName = "function";
    private static final Color funcColor = new Color(Color.packRgba(255, 102, 128, 255));
    private static final Color innerColor = new Color(Color.packRgba(255, 77, 106, 255));
    private static final Vec2 v1 = new Vec2();
    private static final String inputName = "funcvar", condName = inputName + 'c';
    private static final Seq<FuncVarBlock> tmpVars = new Seq<>();
    private LabelElement define;
    private int id = -1, varID = 0;
    private Seq<FuncVarBlock>[] vars;

    public DefineBlock(Color color) {
        this(color, new FunctionInfo(b -> b.define = (LabelElement) b.labelBundle("define").padRight(addPadding * 6).get()));
    }

    public DefineBlock(Color color, BlockInfo info) {
        super(ScratchType.block, color, info, false);
    }

    public DefineBlock(Color color, BlockInfo info, boolean drag) {
        super(ScratchType.block, color, info, false);
        if (drag) ScratchInput.addDraggingInput(this).enabled = () -> {
            for (Element e : getChildren()) {
                e.stageToLocalCoordinates(v1.set(input.mouseX(), input.mouseY()));
                if (e instanceof VariableBlock s && s.click.isOver(e, v1.x, v1.y)) {
                    return false;
                }
            }

            return true;
        };
    }

    private FuncVarBlock addVar(String name) {
        FuncVarBlock b = ScratchController.newBlock(name, false);
        tmpVars.add(b);
        b.varID = varID++;
        b.cell(add(b));
        ScratchInput.addNewInput(b).enabled = () -> getListeners().contains((Boolf<EventListener>) e -> e instanceof ScratchInput.ScratchDragListener);
        return b;
    }

    public void inputVar(String name) {
        addVar(inputName).var(name);
    }

    public void condVar(String name) {
        addVar(condName).var(name);
    }

    public DefineBlock id(int id) {
        if (this.id != -1) throw new IllegalStateException("ID has already been set: old: " + this.id + ", new: " + id);
        this.id = id;
        return this;
    }

    public void registerVar(FuncVarBlock var) {
        vars[var.varID].add(var);
    }

    public void unregisterVar(FuncVarBlock var) {
        vars[var.varID].remove(var);
    }

    @SuppressWarnings("unchecked")
    public void make() {
        vars = new Seq[varID];
        for (int i = 0; i < varID; i++) {
            vars[i] = new Seq<>();
        }
        tmpVars.each(v -> v.func(this));
        tmpVars.clear();
    }

    @Override
    public void input(boolean num, String def) {
        throw new IllegalStateException("Not allowed");
    }

    @Override
    public void list(String[] list) {
        throw new IllegalStateException("Not allowed");
    }

    @Override
    public void cond() {
        throw new IllegalStateException("Not allowed");
    }

    @Override
    public void init() {
        minHeight = 56;
        marginRight(addPadding * 6);
    }

    @Override
    public void drawBackground() {
        float dw = define.getWidth();
        ScratchDraw.drawFunctionBlock(x, y, dw + addPadding * 6, width, width - dw - addPadding * 9, elemColor, innerColor);
    }

    @Override
    public DefineBlock copy(boolean drag) {
        DefineBlock sb = new DefineBlock(elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void buildMenu(Table t) {
    }

    @Override
    public void read(Reads r) {
        super.read(r);
        id = r.i();
        if (getState() == ScratchController.State.loading) ScratchController.nowContext.functions.add(this);
    }

    @Override
    public void write(Writes w) {
        super.write(w);
        w.i(id);
    }

    @Override
    public void readElements(Reads r) {
        int size = r.b();
        for (int i = 0; i < size; i++) {
            switch (r.b()) {
                case 0 -> label(r.str());
                case 1 -> {
                    switch (ScratchType.all[r.b()]) {
                        case input -> addVar(inputName).read(r);
                        case condition -> addVar(condName).read(r);
                    }
                }
            }
        }
    }

    @Override
    public void writeElements(Writes w) {
        w.b(elements.size - 1);
        for (int i = 1; i < elements.size; i++) {
            Element e = elements.get(i);
            if (e.getClass() == LabelElement.class) {
                LabelElement el = (LabelElement) e;
                w.b(0);
                w.str(el.value);
            } else if (e.getClass() == FuncVarBlock.class) {
                FuncVarBlock b = (FuncVarBlock) e;
                w.b(1);
                w.b(b.type.ordinal());
                b.write(w);
            } else {
                throw new IllegalArgumentException("Unknown element type: " + e.getClass());
            }
        }
    }

    public void invoke(FunctionBlock f) {
        Seq<Element> c = f.elements.select(e -> e instanceof UserElement);
        for (int i = 0; i < vars.length; i++) {
            Object val = ((ScratchElement) c.get(i)).getValue();
            vars[i].each(b -> b.obj = val);
        }
        insertRun();
    }

    public static void register() {
        ScratchController.registerBlock(inputName, new FuncVarBlock(ScratchType.input, funcColor, new BlockInfo()), false);
        ScratchController.registerBlock(condName, new FuncVarBlock(ScratchType.condition, funcColor, new BlockInfo()), false);
        ScratchController.category("function", funcColor);
        ScratchController.button("function.create", () -> ScratchController.ui.showWindow("button.function.create.name", new Table(MakeFunctionUI::build))).setStyle(ScratchStyles.flatText);
        ScratchController.registerBlock(defineName, new DefineBlock(funcColor));
        ScratchController.registerBlock("return", new ScratchBlock(ScratchType.block, funcColor, new BlockInfo(b -> {
            b.labelBundle("return");
            b.input();
        })));
        ScratchController.registerBlock(functionName, new FunctionBlock(), false);
    }

    public static class FunctionInfo extends BlockInfo {
        protected Cons<DefineBlock> builder;

        public FunctionInfo(Cons<DefineBlock> builder) {
            this.builder = builder;
            run = s -> {
            };
        }

        @Override
        public void build(ScratchBlock block) {
            builder.get((DefineBlock) block);
        }
    }

    public static class FuncVarBlock extends VariableBlock {
        private static final Seq<FuncVarBlock> neededLink = new Seq<>();
        public DefineBlock function;
        private int funcID = -1;
        private int varID = -1;
        private Object obj;

        static {
            ScratchEvents.on(ScratchEvents.Event.loadEnd, FuncVarBlock::map);
        }

        public FuncVarBlock(ScratchType type, Color color, BlockInfo info) {
            super(type, color, info);
        }

        public FuncVarBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
            super(type, color, info, dragEnabled);
        }

        public FuncVarBlock varID(int id) {
            varID = id;
            return this;
        }

        public FuncVarBlock func(DefineBlock f) {
            if (function != null) function.unregisterVar(this);
            function = f;
            if (function != null) function.registerVar(this);
            return this;
        }

        @Override
        public boolean remove() {
            if (function != null) function.unregisterVar(this);
            return super.remove();
        }

        @Override
        public void read(Reads r) {
            super.read(r);
            funcID = r.i();
            varID = r.i();
            var(r.str());
            link();
        }

        @Override
        public void write(Writes w) {
            if (function == null) throw new IllegalStateException("function is null");
            super.write(w);
            w.i(function.id);
            w.i(varID);
            w.str(var);
        }

        @Override
        public FuncVarBlock var(String name) {
            super.var(name);
            return this;
        }

        @Override
        public Object getValue() {
            return obj;
        }

        @Override
        public FuncVarBlock copy(boolean drag) {
            FuncVarBlock sb = new FuncVarBlock(type, elemColor, info, drag).varID(varID).var(var).func(function);
            copyChildrenValue(sb, drag);
            return sb;
        }

        @Override
        public void copyChildrenValue(ScratchBlock target, boolean drag) {
            super.copyChildrenValue(target, drag);
            FuncVarBlock b = ((FuncVarBlock) target);
            b.function = function;
            b.varID = varID;
        }

        @Override
        public void drawBackground() {
            if (parent instanceof DefineBlock) {
                switch (type) {
                    case input -> ScratchDraw.drawInputBorderless(x, y, width, height, elemColor);
                    case condition -> ScratchDraw.drawCond(x, y, width, height, elemColor, true, false);
                }
                return;
            }
            super.drawBackground();
        }

        public void link() {
            if (ScratchController.getState() == ScratchController.State.loading) {
                neededLink.add(this);
            } else {
                func(ScratchController.getFunction(funcID));
            }
        }

        public static void map() {
            neededLink.each(b -> b.func(ScratchController.getFunction(b.funcID)));
            neededLink.clear();
        }
    }

    public static class MakeFunctionUI {
        public static PreviewFieldStyle label = new PreviewFieldStyle(Styles.defaultField) {
            {
                focusedBackground = Tex.whiteui;
                background = RFuncs.tint(innerColor);
                fontColor = Color.white;
                focusedFontColor = Color.gray;
                focusedFont = Fonts.outline;
            }

            @Override
            public void build(PreviewField f, DefineBlock b) {
                b.label(f.getText());
            }

            @Override
            public void build(PreviewField f, FunctionBlock b) {
                b.label(f.getText());
            }
        };
        public static PreviewFieldStyle cond = new PreviewFieldStyle(Styles.defaultField) {
            {
                focusedBackground = Tex.whiteui;
                background = new BaseDrawable() {
                    @Override
                    public void draw(float x, float y, float width, float height) {
                        ScratchDraw.drawCond(x, y, width, height, Color.white, true, false);
                    }
                };
                font = Fonts.outline;
                fontColor = Color.gray;
            }

            @Override
            public void build(PreviewField f, DefineBlock b) {
                b.condVar(f.getText());
            }

            @Override
            public void build(PreviewField f, FunctionBlock b) {
                b.cond();
            }
        };
        public static PreviewFieldStyle input = new PreviewFieldStyle(Styles.defaultField) {
            {
                background = new BaseDrawable() {
                    @Override
                    public void draw(float x, float y, float width, float height) {
                        ScratchDraw.drawInputBorderless(x, y, width, height, Color.white);
                    }
                };
                font = Fonts.outline;
                fontColor = Color.gray;
            }

            @Override
            public void build(PreviewField f, DefineBlock b) {
                b.inputVar(f.getText());
            }

            @Override
            public void build(PreviewField f, FunctionBlock b) {
                b.input();
            }
        };
        public static Drawable bg = RFuncs.tint(new Color(Color.packRgba(230, 240, 255, 255)));

        public static void build(Table t) {
            t.table(t2 -> {
                PreviewTable preview = new PreviewTable();
                t2.setBackground(Tex.whiteui);
                t2.margin(10);
                t2.table(t3 -> {
                    t3.setBackground(bg);
                    t3.pane(Styles.noBarPane, new Table(t4 -> {
                        t4.margin(100);
                        t4.add(preview);
                    })).name("pane").grow();
                }).fillX().height(200).row();
                t2.table(t3 -> {
                    t3.defaults().pad(10);
                    int all = 0x7fffffff;
                    t3.add(new ScratchUI.OutlineTable(t4 -> {
                        t4.addListener(new HandCursorListener());
                        t4.table(t5 -> t5.add(new ScratchUI.DrawableElement((x, y, w, h) -> {
                            ScratchDraw.drawRect(x, y, w, h, funcColor);
                            ScratchDraw.drawInputBorderless(x + 10, y + 10, 40, 30, innerColor);
                        })).size(60, 50)).grow().row();
                        t4.table(t5 -> {
                            t5.add(getLocalized("button.function.input.name"), ScratchStyles.grayOutline).row();
                            t5.add(getLocalized("button.function.input.desc"), ScratchStyles.grayFont);
                        }).grow();
                        t4.clicked(() -> preview.add(new PreviewField(getLocalized("button.function.input.desc"), input)).height(30));
                    }, all)).touchable(Touchable.enabled).size(180, 130);
                    t3.add(new ScratchUI.OutlineTable(t4 -> {
                        t4.addListener(new HandCursorListener());
                        t4.table(t5 -> t5.add(new ScratchUI.DrawableElement((x, y, w, h) -> {
                            ScratchDraw.drawRect(x, y, w, h, funcColor);
                            ScratchDraw.drawCond(x + 10, y + 10, 40, 30, innerColor, true, false);
                        })).size(60, 50)).grow().row();
                        t4.table(t5 -> {
                            t5.add(getLocalized("button.function.input.name"), ScratchStyles.grayOutline).row();
                            t5.add(getLocalized("button.function.bool.desc"), ScratchStyles.grayFont);
                        }).grow();
                        t4.clicked(() -> preview.add(new PreviewField(getLocalized("button.function.bool.desc"), cond)).height(30));
                    }, all)).touchable(Touchable.enabled).size(180, 130);
                    t3.add(new ScratchUI.OutlineTable(t4 -> {
                        t4.addListener(new HandCursorListener());
                        t4.add(new Table(t5 -> t5.add("text").fontScale(ScratchUI.textScale)) {
                            @Override
                            protected void drawBackground(float x, float y) {
                                ScratchDraw.drawRect(x, y, width, height, funcColor);
                            }
                        }).size(60, 50).grow().row();
                        t4.table(t5 -> {
                            t5.add(getLocalized("button.function.label.desc"), ScratchStyles.grayOutline).row();
                            t5.add("");
                        }).grow();
                        t4.clicked(() -> preview.add(new PreviewField(getLocalized("elem.label.name"), label)));
                    }, all)).touchable(Touchable.enabled).size(180, 130);
                }).grow().row();
                t2.table(t3 -> {
                    t3.right();
                    t3.defaults().pad(3);
                    t3.button(getLocalized("button.cancel.name"), ScratchStyles.flatText, () -> t.parent.find("close").fireClick()).size(64, 48);
                    t3.button(getLocalized("button.confirm.name"), ScratchStyles.flatText, () -> {
                        t.parent.find("close").fireClick();
                        createFunction(preview);
                    }).size(64, 48);
                }).margin(10f).fillX();
            }).width(620);
        }

        public static void createFunction(PreviewTable p) {
            DefineBlock b = ScratchController.newBlock(defineName);
            FunctionBlock fb = ScratchController.newBlock(functionName, false);
            p.getChildren().each(e -> {
                PreviewField f = (PreviewField) e;
                f.style.build(f, b);
                f.style.build(f, fb);
            });
            if (!(b.getChildren().get(1) instanceof LabelElement)) b.getCell(b.getChildren().get(0)).padRight(addPadding * 3 + 35);
            ScratchController.registerFunction(b);
            b.make();
            fb.func(b);
            ScratchController.ui.addBlock(b);
            ScratchInput.addNewInput(fb);
            ScratchController.ui.registerBlock(fb);
            b.setPosition(100, 9900);
        }

        public static class PreviewTable extends Table {
            public PreviewTable() {
                margin(7, 35, 7, 7);
                defaults().pad(3);
                add(new PreviewField(getLocalized("elem.funcname.name"), label));
            }

            @Override
            protected void drawBackground(float x, float y) {
                ScratchDraw.drawBlock(x, y, width, height, funcColor, false);
            }
        }

        public static class PreviewField extends TextField {
            private static final GlyphLayout prefSizeLayout = new GlyphLayout();
            private float prefWidth;
            private final PreviewFieldStyle style;

            public PreviewField(String text, PreviewFieldStyle style) {
                super(text, style);
                changed(this::calcWidth);
                setAlignment(Align.center);
                this.style = style;
                calcWidth();
                addListener(new InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        parent.parent.addChild(new TrashImage(PreviewField.this));
                        return false;
                    }
                });
            }

            private void calcWidth() {
                prefSizeLayout.setText(style.font, getText() + "    ");
                prefWidth = prefSizeLayout.width;
                invalidateHierarchy();
            }

            @Override
            public float getPrefWidth() {
                return Math.max(36, prefWidth);
            }

            @Override
            public float getPrefHeight() {
                return 30;
            }

            @Override
            public void draw() {
                if (style.focusedFont != null)
                    style.font = getScene().getKeyboardFocus() == this ? style.focusedFont : Fonts.def;
                super.draw();
            }
        }

        public static abstract class PreviewFieldStyle extends TextField.TextFieldStyle {
            public Font focusedFont;

            public PreviewFieldStyle(TextField.TextFieldStyle style) {
                super(style);
            }

            abstract public void build(PreviewField f, DefineBlock b);
            abstract public void build(PreviewField f, FunctionBlock b);
        }

        public static class TrashImage extends Image {
            public static final Color c = new Color(Color.packRgba(255, 102, 26, 255));

            public TrashImage(PreviewField target) {
                super(Icon.trashSmall);
                color.set(c);
                addListener(new HandCursorListener());
                addListener(new InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        Table t = (Table) target.parent;
                        if (t.getChildren().size > 1) {
                            target.remove();
                            remove();
                            Seq<Element> es = t.getChildren().copy();
                            t.clearChildren();
                            es.each(t::add);
                        }
                        return false;
                    }
                });
                update(() -> {
                    if (target.getScene().getKeyboardFocus() != target) {
                        remove();
                        return;
                    }
                    x = target.parent.x + target.x + target.getWidth() / 2 - width / 2;
                    y = target.parent.y + target.y + 50;
                });
            }
        }
    }

    public static class FunctionBlock extends ScratchBlock {
        private static final Seq<FunctionBlock> neededLink = new Seq<>();
        private int funcID = -1;
        public DefineBlock function;

        static {
            ScratchEvents.on(ScratchEvents.Event.loadEnd, FunctionBlock::map);
        }

        public FunctionBlock() {
            super(ScratchType.block, funcColor, new BlockInfo());
        }

        public FunctionBlock(boolean dragEnabled) {
            super(ScratchType.block, funcColor, new BlockInfo(), dragEnabled);
        }

        public FunctionBlock func(DefineBlock f) {
            function = f;
            funcID = f.id;
            return this;
        }

        @Override
        public void write(Writes w) {
            super.write(w);
            w.i(funcID);
        }

        @Override
        public void read(Reads r) {
            super.read(r);
            funcID = r.i();
        }

        @Override
        public void writeElements(Writes w) {
            w.i(elements.size);
            elements.each(e -> {
                ScratchElement r = (ScratchElement) e;
                w.i(r.getType().ordinal());
                if (r instanceof LabelElement l) w.str(l.value);
            });
            super.writeElements(w);
        }

        @Override
        public void readElements(Reads r) {
            int s = r.i();
            for (int i = 0; i < s; i++) {
                ScratchType t = ScratchType.all[r.i()];
                switch (t) {
                    case label -> label(r.str());
                    case input -> input();
                    case condition -> cond();
                    default -> throw new IllegalStateException("Unexpected type: " + t);
                }
            }
            super.readElements(r);
        }

        @Override
        public FunctionBlock copy(boolean drag) {
            FunctionBlock b = new FunctionBlock(drag);
            copyChildrenValue(b, drag);
            return b;
        }

        @Override
        public void copyChildrenValue(ScratchBlock target, boolean drag) {
            cloneCopy(target);
            ((FunctionBlock) target).function = function;
        }

        @Override
        public void run() {
            function.invoke(this);
        }

        public void link() {
            if (ScratchController.getState() == ScratchController.State.loading) {
                neededLink.add(this);
            } else {
                function = ScratchController.getFunction(funcID);
            }
        }

        public static void map() {
            neededLink.each(b -> b.function = ScratchController.getFunction(b.funcID));
            neededLink.clear();
        }
    }
}
