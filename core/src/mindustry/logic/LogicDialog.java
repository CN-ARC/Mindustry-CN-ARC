package mindustry.logic;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.core.GameState.State;
import mindustry.ctype.Content;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.logic.LExecutor.PrintI;
import mindustry.logic.LExecutor.Var;
import mindustry.logic.LStatements.InvalidStatement;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;


import mindustry.world.blocks.logic.LogicBlock;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.game.Schematic;



import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.logic.LCanvas.tooltip;

public class LogicDialog extends BaseDialog{
    public LCanvas canvas;
    Cons<String> consumer = s -> {};
    boolean privileged;
    public static float period = 15f;
    float counter = 0f;
    Table varTable = new Table();
    Table mainTable = new Table();
    public static boolean refreshing = true;

    public static String transText = "";

    @Nullable LExecutor executor;

    private boolean dispose = false;

    public LogicDialog(){
        super("logic");

        clearChildren();
        canvas = new LCanvas();
        shouldPause = true;

        addCloseListener();

        shown(this::setup);
        hidden(() -> {
            if(!dispose){
                consumer.get(canvas.save());
            } else {
                dispose = false;
            }});
        onResize(() -> {
            setup();
            canvas.rebuild();
            varsTable();
        });


        add(mainTable).grow().name("canvas");
        rebuildMain();

        row();

        add(buttons).growX().name("canvas");
    }

    private void rebuildMain(){
        mainTable.clear();
        canvas.rebuild();
        if(!Core.settings.getBool("logicSupport"))  {
            mainTable.add(canvas).grow();
        }else{
            varsTable();
            mainTable.add(varTable);
            mainTable.add(canvas).grow();
            counter=0;
            varTable.update(()->{
                counter+=Time.delta;
                if(counter>period && refreshing){
                    counter=0;
                }
            });
        }
    }
    private void varsTable(){
        varTable.clear();
        varTable.table(t->{
            t.table(tt->{
                tt.add("刷新间隔").padRight(5f).left();
                TextField field = tt.field((int)period + "", text -> {
                    period = Integer.parseInt(text);
                }).width(100f).valid(Strings::canParsePositiveInt).maxTextLength(5).get();
                tt.slider(1, 60,1, period, res -> {
                    period = res;
                    field.setText((int)res + "");
                });
            });
            t.row();
            t.table(tt -> {
                tt.button(Icon.cancelSmall, Styles.cleari, () -> {
                    Core.settings.put("logicSupport", !Core.settings.getBool("logicSupport"));
                    arcui.arcInfo("[orange]已关闭逻辑辅助器！");
                    rebuildMain();
                }).size(50f);
                tt.button(Icon.refreshSmall, Styles.cleari, () -> {
                    executor.build.updateCode(executor.build.code);
                    varsTable();
                    arcui.arcInfo("[orange]已更新逻辑显示！");
                }).size(50f);
                tt.button(Icon.pauseSmall, Styles.cleari, () -> {
                    refreshing = !refreshing;
                    arcui.arcInfo("[orange]已" + (refreshing ? "开启" : "关闭") + "逻辑刷新");
                }).checked(refreshing).size(50f);
                tt.button(Icon.rightOpenOutSmall, Styles.cleari, () -> {
                    Core.settings.put("rectJumpLine", !Core.settings.getBool("rectJumpLine"));
                    arcui.arcInfo("[orange]已" + (refreshing ? "开启" : "关闭") + "方形跳转线");
                    this.canvas.rebuild();
                }).checked(refreshing).size(50f);

                tt.button(Icon.playSmall, Styles.cleari, () -> {
                    if (state.isPaused()) state.set(State.playing);
                    else state.set(State.paused);
                    arcui.arcInfo(state.isPaused() ? "已暂停" : "已继续游戏");
                }).checked(state.isPaused()).size(50f);
            });
        });
        varTable.row();
            varTable.pane(t->{
                if(executor==null) return;
                for(var s : executor.vars){
                    if(s.name.startsWith("___")) continue;
                    String text = arcVarsText(s);
                    t.table(tt->{
                        tt.background(Tex.whitePane);

                        tt.table(tv->{
                            tv.labelWrap(s.name).width(100f);
                            tv.touchable = Touchable.enabled;
                            tv.tapped(()->{
                                Core.app.setClipboardText(s.name);
                                arcui.arcInfo("[cyan]复制变量名[white]\n " + s.name);
                            });
                        });
                        tt.table(tv->{
                            Label varPro = tv.labelWrap(text).width(200f).get();
                            tv.touchable = Touchable.enabled;
                            tv.tapped(()->{
                                Core.app.setClipboardText(varPro.getText().toString());
                                arcui.arcInfo("[cyan]复制变量属性[white]\n " + varPro.getText());
                            });
                            tv.update(()->{
                                if(counter + Time.delta>period && refreshing){
                                    varPro.setText(arcVarsText(s));
                                }
                            });
                        }).padLeft(20f);

                        tt.update(()->{
                            if(counter + Time.delta>period && refreshing){
                                tt.setColor(arcVarsColor(s));
                            }
                        });

                    }).padTop(10f).row();
                }
                t.table(tt->{
                    tt.background(Tex.whitePane);

                    tt.table(tv->{
                        Label varPro = tv.labelWrap(executor.textBuffer.toString()).width(300f).get();
                        tv.touchable = Touchable.enabled;
                        tv.tapped(()->{
                            Core.app.setClipboardText(varPro.getText().toString());
                            arcui.arcInfo("[cyan]复制信息版[white]\n " + varPro.getText());
                        });
                        tv.update(()->{
                            if(counter + Time.delta>period && refreshing){
                                varPro.setText(executor.textBuffer.toString());
                            }
                        });
                    }).padLeft(20f);

                    tt.update(()->{
                        if(counter + Time.delta>period && refreshing){
                            tt.setColor(Color.valueOf("#e600e6"));
                        }
                    });

                }).padTop(10f).row();
            }).width(400f).padLeft(20f);
    }

    public static String arcVarsText(Var s){
        return s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
    }

    public static Color arcVarsColor(Var s){
        if(s.constant && s.name.startsWith("@")) return Color.goldenrod;
        else if (s.constant) return Color.valueOf("00cc7e");
        else return typeColor(s,new Color());
    }

    private static Color typeColor(Var s, Color color){
        return color.set(
            !s.isobj ? Pal.place :
            s.objval == null ? Color.darkGray :
            s.objval instanceof String ? Pal.ammo :
            s.objval instanceof Content ? Pal.logicOperations :
            s.objval instanceof Building ? Pal.logicBlocks :
            s.objval instanceof Unit ? Pal.logicUnits :
            s.objval instanceof Team ? Pal.logicUnits :
            s.objval instanceof Enum<?> ? Pal.logicIo :
            Color.white
        );
    }

    private String typeName(Var s){
        return
            !s.isobj ? "number" :
            s.objval == null ? "null" :
            s.objval instanceof String ? "string" :
            s.objval instanceof Content ? "content" :
            s.objval instanceof Building ? "building" :
            s.objval instanceof Team ? "team" :
            s.objval instanceof Unit ? "unit" :
            s.objval instanceof Enum<?> ? "enum" :
            "unknown";
    }

    private void setup(){
        buttons.clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.flatt;
                    t.defaults().size(280f, 60f).left();

                    t.button("@schematic.copy", Icon.copy, style, () -> {
                        dialog.hide();
                        Core.app.setClipboardText(canvas.save());
                    }).marginLeft(12f);
                    t.row();
                    t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText().replace("\r\n", "\n"));
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                    t.row();
                     t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText().replace("\r\n", "\n"));
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                    t.row();
                    t.button("[orange]从蓝图导入", Icon.paste, style, () -> {
    dialog.hide();
    showSchematics();
}).marginLeft(12f);
t.row();
                    t.button("[orange]清空",Icon.trash,style,() -> canvas.clearAll()).marginLeft(12f);
                    t.row();
                    t.button("[orange]丢弃更改", Icon.cancel,style, () -> ui.showConfirm("确认丢弃?", () -> {
                        dispose = true;
                        dialog.hide();
                        hide();
                    })).marginLeft(12f);
                    t.row();
                    t.button("[orange]逻辑辅助器",Icon.settings,style,()-> {
                        Core.settings.put("logicSupport",!Core.settings.getBool("logicSupport"));
                        rebuildMain();
                    }).marginLeft(12f);
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        if(Core.graphics.isPortrait()) buttons.row();

        buttons.button("@variables", Icon.menu, () -> {
            BaseDialog dialog = new BaseDialog("@variables");
            dialog.hidden(() -> {
                if(!wasPaused && !net.active()){
                    state.set(State.paused);
                }
            });

            dialog.shown(() -> {
                if(!wasPaused && !net.active()){
                    state.set(State.playing);
                }
            });

            dialog.cont.pane(p -> {
                p.margin(10f).marginRight(16f);
                p.table(Tex.button, t -> {
                    t.defaults().fillX().height(45f);
                    for(var s : executor.vars){
                        if(s.constant) continue;

                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(" " + s.name + " ", Styles.outlineLabel){{
                            setColor(Pal.accent);
                        }}).padRight(pad);

                        t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                        t.table(Tex.pane, out -> {
                            float[] counter = {-1f};
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(140f).wrap().get();
                            label.update(() -> {
                                if(counter[0] < 0 || (counter[0] += Time.delta) >= period){
                                    String text = s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
                                    if(!label.textEquals(text)){
                                        label.setText(text);
                                        if(counter[0] >= 0f){
                                            label.actions(Actions.color(Pal.accent), Actions.color(Color.white, 0.2f));
                                        }
                                    }
                                    counter[0] = 0f;
                                }
                            });
                            label.act(1f);
                        }).padRight(pad);

                        t.add(new Image(Tex.whiteui, typeColor(s, new Color()).mul(mul))).update(i -> i.setColor(typeColor(s, i.color).mul(mul))).width(stub);

                        t.stack(new Image(Tex.whiteui, typeColor(s, new Color())){{
                            update(() -> setColor(typeColor(s, color)));
                        }}, new Label(() -> " " + typeName(s) + " "){{
                            setStyle(Styles.outlineLabel);
                        }});

                        t.row();

                        t.add().growX().colspan(6).height(4).row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("variables").disabled(b -> executor == null || executor.vars.length == 0);

        buttons.button("@add", Icon.add, () -> {
            BaseDialog dialog = new BaseDialog("@add");
            dialog.cont.table(table -> {
                table.background(Tex.button);
                table.pane(t -> {
                    for(Prov<LStatement> prov : LogicIO.allStatements){
                        LStatement example = prov.get();
                        if(example instanceof InvalidStatement || example.hidden() || (example.privileged() && !privileged) || (example.nonPrivileged() && privileged)) continue;

                        LCategory category = example.category();
                        Table cat = t.find(category.name);
                        if(cat == null){
                            t.table(s -> {
                                if(category.icon != null){
                                    s.image(category.icon, Pal.darkishGray).left().size(15f).padRight(10f);
                                }
                                s.add(category.localized()).color(Pal.darkishGray).left().tooltip(category.description());
                                s.image(Tex.whiteui, Pal.darkishGray).left().height(5f).growX().padLeft(10f);
                            }).growX().pad(5f).padTop(10f);

                            t.row();

                            cat = t.table(c -> {
                                c.top().left();
                            }).name(category.name).top().left().growX().fillY().get();
                            t.row();
                        }

                        TextButtonStyle style = new TextButtonStyle(Styles.flatt);
                        style.fontColor = category.color;
                        style.font = Fonts.outline;

                        cat.button(example.name(), style, () -> {
                            canvas.add(prov.get());
                            dialog.hide();
                        }).size(130f, 50f).self(c -> tooltip(c, "lst." + example.name())).top().left();

                        if(cat.getChildren().size % 3 == 0) cat.row();
                    }
                }).grow();
            }).fill().maxHeight(Core.graphics.getHeight() * 0.8f);
            dialog.addCloseButton();
            dialog.show();
        }).disabled(t -> canvas.statements.getChildren().size >= LExecutor.maxInstructions);
    }

    public void show(String code, LExecutor executor, boolean privileged, Cons<String> modified){
        this.executor = executor;
        this.privileged = privileged;
        canvas.statements.clearChildren();
        canvas.rebuild();
        canvas.privileged = privileged;
        try{
            canvas.load(code);
        }catch(Throwable t){
            Log.err(t);
            canvas.load("");
        }
        this.consumer = result -> {
            if(!result.equals(code)){
                modified.get(result);
            }
        };
        varsTable();
        show();
    }
    private void showSchematics() {
    BaseDialog dialog = new BaseDialog("[orange]从蓝图导入逻辑");
    
    // 主内容区域设置
    dialog.cont.pane(p -> {
        p.margin(15f);  // 增加外边距
        
        // 创建一个可滚动的内容区域
        Table schematicList = new Table();
        schematicList.left();
        
        // 添加搜索框 - 这部分不会被清除
        p.table(search -> {
            search.left();
            search.image(Icon.zoom).size(32f).padRight(8f);
            TextField searchField = search.field("", text -> {
                // 直接使用保存的引用来刷新列表
                refreshSchematicList(schematicList, text.toLowerCase(), dialog);
            }).growX().height(50f).get();
            searchField.setMessageText("搜索蓝图...");
        }).growX().pad(5f);
        
        p.row();
        
        // 添加蓝图列表的可滚动区域
        p.pane(schematicList).grow().maxHeight(Core.graphics.getHeight() * 0.7f);
        
        // 初始填充蓝图列表
        refreshSchematicList(schematicList, "", dialog);
        
    }).grow().maxHeight(Core.graphics.getHeight() * 0.85f);
    
    // 底部按钮
    dialog.buttons.button("@cancel", Icon.cancel, dialog::hide).size(210f, 64f);
    
    dialog.show();
}

// 刷新蓝图列表的辅助方法 - 现在只处理schematicList部分
private void refreshSchematicList(Table container, String searchQuery, BaseDialog parentDialog) {
    // 其余代码保持不变
    container.clear();
    boolean[] found = {false};
    
    // 使用表格布局，设置两列
    container.defaults().growX().left().pad(8f);
    
    schematics.all().each(schematic -> {
        boolean hasProcessor = schematic.tiles.contains(tile -> tile.block instanceof LogicBlock);
        boolean matchesSearch = searchQuery.isEmpty() || schematic.name().toLowerCase().contains(searchQuery);
        
        if(hasProcessor && matchesSearch) {
            found[0] = true;
            
            // 创建包含更多信息的按钮
            container.table(Tex.buttonEdge3, row -> {
                // 左侧图标区域
                row.table(iconSection -> {
                    iconSection.image(Icon.logicSmall).size(42f).pad(5f);
                }).size(60f, 90f).padRight(5f);
                
                // 蓝图信息区域
                row.table(infoSection -> {
                    // 蓝图名称 - 使用较大字体并允许换行
                    infoSection.add(schematic.name()).width(250f).wrap().growX().color(Pal.accent).left();
                    infoSection.row();
                    
                    // 蓝图尺寸信息
                    infoSection.add("[gray]尺寸: " + schematic.width + "x" + schematic.height).left();
                    infoSection.row();
                    
                    // 处理器数量
                    int processorCount = (int)schematic.tiles.count(tile -> tile.block instanceof LogicBlock);
                    infoSection.add("[gray]处理器: " + processorCount + " 个").left();
                }).growX().pad(10f);
                
                // 右侧导入按钮
                row.table(buttonSection -> {
                    buttonSection.button(Icon.download, Styles.cleari, () -> {
                        importLogicFromSchematic(schematic, container, parentDialog);
                    }).size(60f).right().tooltip("导入逻辑");
                }).size(70f, 90f);
                
            }).height(100f).fillX().pad(10f);
            
            container.row();
        }
    });
    
    if(!found[0]) {
        container.add("[lightgray]未找到包含处理器的蓝图").pad(30f).center();
    }
}

// 从蓝图导入逻辑的辅助方法
private void importLogicFromSchematic(Schematic schematic, Table container, BaseDialog parentDialog) {
    // 如果蓝图只有一个处理器，直接导入
    int processorCount = (int)schematic.tiles.count(tile -> tile.block instanceof LogicBlock);
    
    if(processorCount == 1) {
        // 直接查找并导入
        schematic.tiles.each(tile -> {
            if(tile.block instanceof LogicBlock) {
                importProcessorLogic(tile, schematic.name());
                parentDialog.hide();
                return;
            }
        });
    } else {
        // 多个处理器时，显示处理器选择对话框
        BaseDialog processorDialog = new BaseDialog("[orange]选择处理器");
        
        processorDialog.cont.pane(p -> {
            p.margin(15f);
            p.defaults().growX().left().pad(10f);
            
            AtomicInteger index = new AtomicInteger(0);
            
            schematic.tiles.each(tile -> {
                if(tile.block instanceof LogicBlock) {
                    int currentIndex = index.getAndIncrement();
                    
                    p.table(Tex.buttonEdge2, t -> {
                        t.add(tile.block.localizedName + " #" + (currentIndex + 1)).growX().left().color(Pal.accent);
                        t.add("[gray]位置: " + tile.x + ", " + tile.y).right().padLeft(20f);
                        
                        t.row();
                        
                        t.button("导入此处理器", Icon.download, Styles.cleart, () -> {
                            importProcessorLogic(tile, schematic.name());
                            processorDialog.hide();
                            parentDialog.hide();
                        }).height(50f).growX().pad(8f);
                    }).height(80f).fillX().pad(5f);
                    
                    p.row();
                }
            });
        }).grow().maxHeight(Core.graphics.getHeight() * 0.7f);
        
        processorDialog.addCloseButton();
        processorDialog.show();
    }
}

// 从蓝图的处理器瓦块导入逻辑
private void importProcessorLogic(Schematic.Stile tile, String schematicName) {
    try {
        if(tile.config instanceof byte[] data) {
            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
                // 读取版本号
                stream.read();
                // 读取代码字节长度
                int bytelen = stream.readInt();
                byte[] bytes = new byte[bytelen];
                stream.readFully(bytes);
                // 将字节转换为字符串
                String code = new String(bytes, StandardCharsets.UTF_8);
                // 直接加载到canvas
                canvas.load(code);
                arcui.arcInfo("[green]成功从蓝图导入逻辑: " + schematicName);
            }
        } else {
            arcui.arcInfo("[scarlet]蓝图中的处理器配置格式不正确");
        }
    } catch(Exception e) {
        ui.showException(e);
    }
}
}
