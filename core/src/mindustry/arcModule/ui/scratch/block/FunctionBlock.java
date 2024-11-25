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
import mindustry.arcModule.RFuncs;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.element.LabelElement;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static arc.Core.input;
import static mindustry.arcModule.ui.scratch.ScratchController.getLocalized;

public class FunctionBlock extends ScratchBlock {
    public static final String name = "function";
    private static final Color funcColor = new Color(Color.packRgba(255, 102, 128, 255));
    private static final Color innerColor = new Color(Color.packRgba(255, 77, 106, 255));
    private static final Vec2 v1 = new Vec2();
    private static final String inputName = "funcvar", condName = inputName + 'c';
    private LabelElement define;

    public FunctionBlock(Color color) {
        this(color, new FunctionInfo(b -> b.define = (LabelElement) b.labelBundle("define").padRight(addPadding * 6).get()));
    }

    public FunctionBlock(Color color, BlockInfo info) {
        super(ScratchType.block, color, info, false);
    }

    public FunctionBlock(Color color, BlockInfo info, boolean drag) {
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

    public void inputVar(String name) {
        ScratchInput.addNewInput(add(((VariableBlock) ScratchController.newBlock(inputName, false)).var(name)).self(c -> c.get().cell(c)).get()).enabled = () -> getListeners().contains((Boolf<EventListener>) e -> e instanceof ScratchInput.ScratchDragListener);
    }

    public void condVar(String name) {
        ScratchInput.addNewInput(add(((VariableBlock) ScratchController.newBlock(condName, false)).var(name)).self(c -> c.get().cell(c)).get()).enabled = () -> getListeners().contains((Boolf<EventListener>) e -> e instanceof ScratchInput.ScratchDragListener);
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
    public FunctionBlock copy(boolean drag) {
        FunctionBlock sb = new FunctionBlock(elemColor, info, drag);
        copyChildrenValue(sb, drag);
        return sb;
    }

    @Override
    public void buildMenu(Table t) {
    }

    public static void register() {
        ScratchController.registerBlock(inputName, new VariableBlock(ScratchType.input, funcColor, new BlockInfo()), false);
        ScratchController.registerBlock(condName, new VariableBlock(ScratchType.condition, funcColor, new BlockInfo()), false);
        ScratchController.category("function", funcColor);
        ScratchController.button("function.create", () -> ScratchController.ui.showWindow("button.function.create.name", new Table(MakeFunctionUI::build))).setStyle(ScratchStyles.flatText);
        ScratchController.registerBlock(name, new FunctionBlock(funcColor));
        ScratchController.registerBlock("return", new ScratchBlock(ScratchType.block, funcColor, new BlockInfo(b -> {
            b.labelBundle("return");
            b.input();
        })));
    }

    public static class FunctionInfo extends BlockInfo {
        protected Cons<FunctionBlock> builder;

        public FunctionInfo(Cons<FunctionBlock> builder) {
            this.builder = builder;
            run = s -> {
            };
        }

        @Override
        public void build(ScratchBlock block) {
            builder.get((FunctionBlock) block);
        }
    }

    public static class MakeFunctionUI {
        public static PreviewFieldStyle label = new PreviewFieldStyle(Styles.defaultField) {{
            focusedBackground = Tex.whiteui;
            background = RFuncs.tint(innerColor);
            fontColor = Color.white;
            focusedFontColor = Color.gray;
            focusedFont = Fonts.outline;
        }
            @Override
            public void build(PreviewField f, FunctionBlock b) {
                b.label(f.getText());
            }
        };
        public static PreviewFieldStyle cond = new PreviewFieldStyle(Styles.defaultField) {{
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
            public void build(PreviewField f, FunctionBlock b) {
                b.condVar(f.getText());
            }
        };
        public static PreviewFieldStyle input = new PreviewFieldStyle(Styles.defaultField) {{
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
            public void build(PreviewField f, FunctionBlock b) {
                b.inputVar(f.getText());
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
                    t3.button("取消", ScratchStyles.flatText, () -> t.parent.find("close").fireClick()).size(64, 48);
                    t3.button("确定", ScratchStyles.flatText, () -> {
                        t.parent.find("close").fireClick();
                        createFunction(preview);
                    }).size(64, 48);
                }).margin(10f).fillX();
            }).width(620);
        }

        public static void createFunction(PreviewTable p) {
            FunctionBlock b = (FunctionBlock) ScratchController.newBlock(name);
            p.getChildren().each(e -> {
                PreviewField f = (PreviewField) e;
                f.style.build(f, b);
            });
            if (!(b.getChildren().get(1) instanceof LabelElement)) b.getCell(b.getChildren().get(0)).padRight(addPadding * 3 + 35);
            ScratchController.ui.addBlock(b);
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
                if (style.focusedFont != null) style.font = getScene().getKeyboardFocus() == this ? style.focusedFont : Fonts.def;
                super.draw();
            }
        }

        public static abstract class PreviewFieldStyle extends TextField.TextFieldStyle {
            public Font focusedFont;

            public PreviewFieldStyle(TextField.TextFieldStyle style) {
                super(style);
            }

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
}
