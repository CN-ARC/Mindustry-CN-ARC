package mindustry.ui.fragments;

import arc.*;
import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.arcModule.ARCVars;
import mindustry.arcModule.ui.RStyles;
import mindustry.arcModule.ui.window.Window;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.MenuRenderer;
import mindustry.graphics.Pal;
import mindustry.service.GameService;
import mindustry.ui.Fonts;
import mindustry.ui.MobileButton;
import mindustry.ui.Styles;

import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.gen.Tex.discordBanner;
import static mindustry.ui.Styles.cleart;

public class MenuFragment{
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;
    private Seq<MenuButton> customButtons = new Seq<>();
    public Seq<MenuButton> desktopButtons = null;
    Label textLabel;
    float tx, ty, base;
    String[] labels = { "[yellow]学术端!" };
    float period = 75f;
    float varSize = 0.8f;
    String text = labels[0];
    static long arcNewsLastUpdate = 0;
    static boolean haveNewerNews = false;

    Fi arcBackground;
    String arcBackgroundPath = Core.settings.getString("arcBackgroundPath");
    Seq<Fi> arcBGList;

    Image img = new Image();

    int arcBackgroundIndex = 0;

    public void build(Group parent){
        renderer = new MenuRenderer();

        if (!Core.settings.getBool("arcDisableModWarning")){
            arcui.aboutcn_arc.show();
        }
        Core.settings.put("locale", "zh_CN");

        Group group = new WidgetGroup();
        group.setFillParent(true);
        group.visible(() -> !ui.editor.isShown());
        //parent.addChild(group);

        WidgetGroup textGroup = new WidgetGroup();
        //parent.addChild(textGroup);

        parent = group;

        if (arcBackgroundPath != null && Core.files.absolute(arcBackgroundPath).exists() && Core.files.absolute(arcBackgroundPath).list().length >=1){
            arcBackgroundIndex = (int) (Math.random() * Core.files.absolute(arcBackgroundPath).list().length);
            nextBackGroundImg();
            if (arcBGList.size == 0) {
                parent.fill((x, y, w, h) -> renderer.render());
            } else {
                group.addChild(img);
                img.setFillParent(true);
            }
        } else {
            parent.fill((x, y, w, h) -> renderer.render());
        }

        parent.fill(c -> {
            c.pane(Styles.noBarPane, cont -> {
                container = cont;
                cont.name = "menu container";

                if(!mobile){
                    c.left();
                    buildDesktop();
                    Events.on(ResizeEvent.class, event -> buildDesktop());
                }else{
                    buildMobile();
                    Events.on(ResizeEvent.class, event -> buildMobile());
                }
            }).with(pane -> {
                pane.setOverscroll(false, false);
            }).grow();
        });

        parent.fill(c -> c.bottom().right().button(Icon.discord, new ImageButtonStyle(){{
            up = discordBanner;
        }}, ui.discord::show).marginTop(9f).marginLeft(10f).tooltip("@discord").size(84, 45).name("discord"));

        parent.fill(c -> {
            c.bottom().right().button("提交反馈", Icon.github, () -> {
                String link = "https://docs.qq.com/form/page/DTllxbXlCc0lJb1ps";
                if (!Core.app.openURI(link)) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText(link);
                }
            }).size(200, 60).tooltip("发现了bug/提交功能建议?\n点击这里提交反馈").with(b -> {
                TextButton.TextButtonStyle s = new TextButton.TextButtonStyle(b.getStyle());
                s.fontColor = b.color;
                b.setStyle(s);
            }).update(b -> b.color.fromHsv(Time.time % 360,1,1)).row();
            /*c.bottom().right().button("检查更新", Icon.refresh, () -> {
                ui.loadfrag.show();
                becontrol.checkUpdate(result -> {
                    ui.loadfrag.hide();
                    becontrol.BeControlTable();
                });
            }).size(200, 60).name("检查更新").update(t -> {
                t.getLabel().setColor(becontrol.isUpdateAvailable() ? Tmp.c1.set(Color.white).lerp(Pal.accent, Mathf.absin(5f, 1f)) : Color.white);
            });*/
        });

        parent.fill(c -> c.bottom().left().table(t -> {
            t.background(Tex.buttonEdge3);
            t.button("\uE83D", cleart, this::nextBackGroundImg).width(50f);
        }).visible(() -> Core.settings.getString("arcBackgroundPath", "").length() != 0).left().width(100));

        String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[cyan]") + Version.combined();
        String arcversionText = "\n[cyan]ARC version:" + Version.arcBuild;
        parent.fill((x, y, w, h) -> {
            TextureRegion logo = Core.atlas.find("logo");
            float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
            float logoscl = Scl.scl(1) * logo.scale;
            float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
            float logoh = logow * (float)logo.height / logo.width;

            float fx = (int)(width / 2f);
            float fy = (int)(height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
            if(Core.settings.getBool("macnotch") ){
                fy -= Scl.scl(macNotchHeight);
            }

            tx = width / 2f + logow * 0.35f;
            ty = fy - logoh / 2f - Scl.scl(2f) + logoh * 0.15f;
            base = logoh * 0.03f;

            Draw.color();
            Draw.rect(logo, fx, fy, logow, logoh);

            Fonts.outline.setColor(Color.white);
            Fonts.outline.draw(versionText+arcversionText, fx, fy - logoh/2f - Scl.scl(2f), Align.center);
        }).touchable = Touchable.disabled;

        textGroup.setTransform(true);
        textGroup.setRotation(20);
        textGroup.addChild(textLabel = new Label(""));
        textGroup.visible(() -> Core.settings.getBool("menuFloatText", true));
        textLabel.setAlignment(Align.center);
        textGroup.update(() -> {
            textGroup.x = tx;
            textGroup.y = ty;
            textLabel.setFontScale((base == 0 ? 1f : base) * Math.abs(Time.time % period / period - 0.5f) * varSize + 1);
            textLabel.setText(text);
        });
        Events.on(EventType.ClientLoadEvent.class, event -> Core.app.post(() -> Http.get("https://cn-arc.github.io/labels?t=" + Time.millis())
                .timeout(5000)
                .error(e -> {
                    Log.err("获取最新主页标语失败!加载本地标语", e);
                    labels = Core.files.internal("labels").readString("UTF-8").replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
                    Core.app.post(this::randomLabel);
                })
                .submit(result -> {
                    labels = result.getResultAsString().replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
                    Core.app.post(this::randomLabel);
                })
        ));

        arcNewsLastUpdate = Core.settings.getLong("arcNewsLastUpdate", 0);
        Events.on(EventType.ClientLoadEvent.class, event -> Timer.schedule(MenuFragment::fetchArcNews, 0, 300));

        parent.fill(c -> c.top().left().table(t -> {
            t.background(Tex.buttonEdge4);
            t.button("学术日报", cleart, MenuFragment::showArcNews).left().update(b -> b.setColor(haveNewerNews ? Tmp.c1.set(Color.white).lerp(Color.cyan, Mathf.absin(5f, 1f)) : Color.white)).growX();
        }).left().width(100));

        Core.app.post(() -> Http.get("https://cn-arc.github.io/classes.json?t=" + Time.millis()).timeout(10000).error(Log::err).submit(r -> {
            try {
                JsonValue j = new JsonReader().parse(r.getResultAsString());
                if (j.getLong("lastUpdate", 0) > Core.settings.getLong("archotfixtime", 0)) {
                    Http.get("https://cn-arc.github.io/classes.zip").timeout(20000).error(Log::err).submit(r2 -> {
                        try {
                            ZipInputStream zip = new ZipInputStream(r2.getResultAsStream());
                            ZipEntry file;
                            Fi root = dataDirectory.child("arcvars");
                            root.emptyDirectory();
                            byte[] buffer = new byte[1024];
                            while ((file = zip.getNextEntry()) != null) {
                                Fi f = root.child(file.getName());
                                if (file.isDirectory()) {
                                    f.mkdirs();
                                } else {
                                    f.parent().mkdirs();
                                    int len;
                                    while ((len = zip.read(buffer)) > 0) {
                                        f.writeBytes(buffer, 0, len, true);
                                    }
                                }
                            }
                            Core.settings.put("archotfixtime", j.getLong("lastUpdate", 0));
                        } catch (Exception e) {
                            Log.err(e);
                        }
                    });
                }
            } catch (Exception e) {
                Log.err(e);
            }
        }));
    }

    private void nextBackGroundImg(){
        arcBGList = Core.files.absolute(arcBackgroundPath).findAll(f -> !f.isDirectory() && (f.extEquals("png") || f.extEquals("jpg") || f.extEquals("jpeg")));
        if (arcBGList.size == 0) return;
        arcBackgroundPath = Core.settings.getString("arcBackgroundPath");
        arcBackgroundIndex += 1;
        arcBackgroundIndex = arcBackgroundIndex % arcBGList.size;
        new Thread(() -> {
            try{
                arcBackground = arcBGList.get(arcBackgroundIndex);
                Core.app.post(() -> img.setDrawable(new TextureRegion(new Texture(arcBackground))));
            } catch (Exception e) {
                Core.app.post(() -> ui.showException("背景图片无效:" + arcBGList.get(arcBackgroundIndex).path(), e));
            }
        }).start();
    }

    public static void fetchArcNews() {
        Http.get("https://cn-arc.github.io/news?t=" + Time.millis())
                .timeout(5000)
                .error(e -> {})
                .submit(result -> {
                    try {
                        String s = result.getResultAsString();
                        long last = Long.parseLong(s.substring(0, s.indexOf('\n')));
                        if (arcNewsLastUpdate < last) {
                            arcNewsLastUpdate = last;
                            haveNewerNews = true;
                            if (Core.settings.getBool("autoArcNews", false)) Core.app.post(MenuFragment::showArcNews);
                        }
                    } catch (Exception ignored) {
                    }
                });
    }

    public static void showArcNews() {
        Core.settings.put("arcNewsLastUpdate", arcNewsLastUpdate);
        haveNewerNews = false;
        Http.get("https://cn-arc.github.io/news?t=" + Time.millis(), result -> {
            String s = result.getResultAsString();
            Core.app.post(() -> {
                try {
                    String[] news = s.replace("\r", "").split("\n");
                    boolean haveNews = false;
                    Table t = new Table();
                    ScrollPane p = new ScrollPane(t);
                    t.table(t2 -> t2.check("有更新时自动显示", b -> Core.settings.put("autoArcNews", b)).checked(Core.settings.getBool("autoArcNews", false))).growX().row();
                    for (int i = 0; i < news.length; i += 3) {
                        haveNews = true;
                        int idx = news[i + 1].indexOf(' ');
                        int id = i;
                        t.button(b -> {
                            b.clearChildren();
                            b.add(idx == -1 ? news[id + 1].substring(0, 10) + "..." : news[id + 1].substring(0, idx)).padLeft(5);
                            b.add().grow();
                            b.add(formatTimeElapsed(Time.millis() - Long.parseLong(news[id]))).padRight(5);
                        }, RStyles.flatt, () -> {
                            Window w = new Window(idx == -1 ? news[id + 1] : news[id + 1].substring(0, idx), 600, 400, Icon.book, arcui.WindowManager);
                            Table content = new Table();
                            ScrollPane pane = new ScrollPane(content);
                            content.table(t2 -> {
                                t2.image().color(ARCVars.getThemeColor()).height(3).growX().row();
                                t2.add(formatTimeElapsed(Time.millis() - Long.parseLong(news[id]))).align(Align.left).row();
                                t2.table(t3 -> {
                                    String n = (idx == -1 ? news[id + 1] : news[id + 1].substring(idx + 1)).replace("\\n", "\n");
                                    StringBuilder sb = new StringBuilder();
                                    Table there = new Table();
                                    for (int ptr = 0, l = n.length(); ptr < l; ptr++) {
                                        char c = n.charAt(ptr);
                                        Table finalThere = there;
                                        switch (c) {
                                            case '\\' -> {
                                                if (ptr + 1 < l) {
                                                    sb.append(n.charAt(ptr + 1));
                                                    ptr++;
                                                }
                                            }
                                            case '{' -> {
                                                int left = 0, right = 0;
                                                for (int ptr2 = ptr; ptr2 < l; ptr2++) {
                                                    switch (n.charAt(ptr2)) {
                                                        case '{' -> left++;
                                                        case '}' -> right++;
                                                    }
                                                    if (left == right) {
                                                        String sub = n.substring(ptr + 1, ptr2);
                                                        int index = sub.indexOf(':');
                                                        if (index == -1) {
                                                            sb.append(n, ptr, ptr2 + 1);
                                                            ptr = ptr2;
                                                            break;
                                                        }
                                                        String cont = sub.substring(index + 1);
                                                        boolean found = true;
                                                        String out = "";
                                                        switch (sub.substring(0, index)) {
                                                            case "image" -> {
                                                                if (sb.length() != 0) there.add(sb.toString());
                                                                Image img = there.image(Icon.refresh.getRegion()).size(64).get();
                                                                img.setScaling(Scaling.fit);
                                                                Http.get(cont, r -> {
                                                                    byte[] b = r.getResult();
                                                                    Core.app.post(() -> {
                                                                        Pixmap pix = new Pixmap(b);
                                                                        finalThere.getCell(img).size(0).grow();
                                                                        img.setDrawable(new TextureRegion(new Texture(pix)));
                                                                        pix.dispose();
                                                                    });
                                                                });
                                                            }
                                                            case "eval" -> {
                                                                sb.append(mods.getScripts().runConsole(cont));
                                                                there.add(sb.toString());
                                                            }
                                                            default -> {
                                                                found = false;
                                                                sb.append(n, ptr, ptr2 + 1);
                                                            }
                                                        }
                                                        if (found) {
                                                            sb.setLength(0);
                                                            System.out.println(out);
                                                            ptr = ptr2;
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                            case '\n' -> {
                                                if (sb.length() != 0) there.add(sb.toString());
                                                sb.setLength(0);
                                                t3.add(there).row();
                                                there = new Table();
                                            }
                                            default -> sb.append(c);
                                        }
                                    }
                                    if (sb.length() != 0) there.add(sb.toString());
                                    t3.add(there);
                                }).growX().row();
                                t2.add(news[id + 2]).align(Align.right);
                            }).pad(5).growX().row();
                            w.setBody(new Table(t2 -> {
                                t2.setBackground(Styles.black3);
                                t2.add(pane).grow();
                            }));
                            w.add();
                        }).minHeight(40).growX().row();
                    }
                    if (!haveNews) {
                        t.add("这里什么都没有");
                    }
                    Window w = new Window("学术日报", 600, 400, Icon.book.getRegion(), arcui.WindowManager);
                    w.setBody(new Table(t2 -> t2.add(p).grow()) {{
                        setBackground(Styles.black3);
                    }});
                    w.add();
                } catch (Exception e) {
                    Log.err(e);
                }
            });
        });
    }

    public static String formatTimeElapsed(long milliseconds) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);

        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return seconds + "秒前";
        }
    }

    private void randomLabel(){
        Timer.schedule(() -> text = "[yellow]" + labels[new Rand().random(0, labels.length - 1)], 0.11f);
    }

    private void buildMobile(){
        container.clear();
        container.name = "buttons";
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        container.defaults().size(size).pad(5).padTop(4f);

        initAchievement();

        MobileButton
                play = new MobileButton(Icon.play, "@campaign", () -> checkPlay(ui.planet::show)),
                custom = new MobileButton(Icon.rightOpenOut, "@customgame", () -> checkPlay(ui.custom::show)),
                maps = new MobileButton(Icon.download, "@loadgame", () -> checkPlay(ui.load::show)),
                join = new MobileButton(Icon.add, "@joingame", () -> checkPlay(ui.join::show)),
                editor = new MobileButton(Icon.terrain, "@editor", () -> checkPlay(ui.maps::show)),
                tools = new MobileButton(Icon.settings, "@settings", ui.settings::show),
                mods = new MobileButton(Icon.book, "@mods", ui.mods::show),
                exit = new MobileButton(Icon.exit, "@quit", () -> Core.app.exit()),
                cn_arc = new MobileButton(Icon.info,"@aboutcn_arc.button",  arcui.aboutcn_arc::show),
                //mindustrywiki = new MobileButton(Icon.book, "@mindustrywiki.button", ui.mindustrywiki::show),
                updatedialog = new MobileButton(Icon.info,"@updatedialog.button",  arcui.updatedialog::show),
                database = new MobileButton(Icon.book, "@database",  ui.database::show),
                achievements = new MobileButton(Icon.star, "@achievements",  arcui.achievements::show);

        play.clicked(this::randomLabel);
        custom.clicked(this::randomLabel);
        maps.clicked(this::randomLabel);
        join.clicked(this::randomLabel);
        editor.clicked(this::randomLabel);
        tools.clicked(this::randomLabel);
        mods.clicked(this::randomLabel);
        cn_arc.clicked(this::randomLabel);
        updatedialog.clicked(this::randomLabel);
        database.clicked(this::randomLabel);
        achievements.clicked(this::randomLabel);

        Seq<MobileButton> customs = customButtons.map(b -> new MobileButton(b.icon, b.text, b.runnable == null ? () -> {} : b.runnable));

        if(!Core.graphics.isPortrait()){
            container.marginTop(60f);
            if(Core.settings.getInt("changelogreaded") == ARCVars.changeLogRead){
                container.add(play);
                container.add(join);
                container.add(custom);
                container.add(maps);
                // add odd custom buttons
                for(int i = 1; i < customs.size; i += 2){
                    customs.get(i).clicked(this::randomLabel);
                    container.add(customs.get(i));
                }
                container.row();
                container.add(editor);
            }
            container.add(tools);
            container.add(mods);
            container.add(achievements);
            // add even custom buttons (before the exit button)
            for(int i = 0; i < customs.size; i += 2){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
            }
            container.row();
            container.add(cn_arc);
            container.add(updatedialog);
            container.add(database);
            if(!ios) container.add(exit);
        }else{
            container.marginTop(0f);
            if(Core.settings.getInt("changelogreaded") == ARCVars.changeLogRead){
                container.add(play);
                container.add(maps);
                container.row();
                container.add(custom);
                container.add(join);
                container.row();
                container.add(editor);
            }
            container.add(tools);
            container.row();
            container.add(mods);
            // add custom buttons
            for(int i = 0; i < customs.size; i++){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
                if(i % 2 == 0) container.row();
            }
            if(!ios) container.add(exit);
            container.row();
            container.add(cn_arc);
            container.add(database);
            container.row();
            container.add(achievements);
            container.add(updatedialog);
        }
    }

    void initAchievement(){
        service = new GameService(){
            @Override
            public boolean enabled(){ return true; }

            @Override
            public void completeAchievement(String name){
                Core.settings.put("achievement." + name, true);
                //TODO draw the sprite of the achievement
                ui.hudfrag.showToast(Core.atlas.getDrawable("error"), Core.bundle.get("achievement.unlocked") +"\n"+ Core.bundle.get("achievement."+name+".name"));
            }

            @Override
            public boolean isAchieved(String name){
                return Core.settings.getBool("achievement." + name, false);
            }

            @Override
            public int getStat(String name, int def) {
                return Core.settings.getInt("achievementstat." + name, def);
            }

            @Override
            public void setStat(String name, int amount) {
                Core.settings.put("achievementstat." + name, amount);
            }
        };

        service.init();
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float width = 230f;
        Drawable background = Styles.black6;

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);
            t.name = "buttons";

            if(desktopButtons == null){
                if(Core.settings.getInt("changelogreaded") != ARCVars.changeLogRead) {
                    desktopButtons = Seq.with(
                            new MenuButton("@database.button", Icon.menu,
                                    new MenuButton("@schematics", Icon.paste, ui.schematics::show),
                                    new MenuButton("@database", Icon.book, ui.database::show),
                                    new MenuButton("@about.button", Icon.info, ui.about::show),
                                    new MenuButton("@updatedialog.button", Icon.distribution, arcui.updatedialog::show)
                            ),
                            new MenuButton("@settings", Icon.settings, ui.settings::show),
                            new MenuButton("@aboutcn_arc.button", Icon.info, arcui.aboutcn_arc::show)
                    );
                } else {
                    desktopButtons = Seq.with(
                            new MenuButton("@play", Icon.play,
                                    new MenuButton("@campaign", Icon.play, () -> checkPlay(ui.planet::show)),
                                    new MenuButton("@joingame", Icon.add, () -> checkPlay(ui.join::show)),
                                    new MenuButton("@customgame", Icon.terrain, () -> checkPlay(ui.custom::show)),
                                    new MenuButton("@loadgame", Icon.download, () -> checkPlay(ui.load::show))
                            ),
                            new MenuButton("@database.button", Icon.menu,
                                    new MenuButton("@schematics", Icon.paste, ui.schematics::show),
                                    new MenuButton("@database", Icon.book, ui.database::show),
                                    new MenuButton("@about.button", Icon.info, ui.about::show)
                            ),
                            new MenuButton("@editor", Icon.terrain, () -> checkPlay(ui.maps::show)), steam ? new MenuButton("@workshop", Icon.steam, platform::openWorkshop) : null,
                            new MenuButton("@mods", Icon.book, ui.mods::show),
                            new MenuButton("@settings", Icon.settings, ui.settings::show)
                    );
                }
            }

            buttons(t, desktopButtons.toArray(MenuButton.class));
            buttons(t, customButtons.toArray(MenuButton.class));
            buttons(t, new MenuButton("@quit", Icon.exit, Core.app::exit));
        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.name = "submenu";
            t.color.a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();
    }

    private void checkPlay(Runnable run){
        if(!mods.hasContentErrors()){
            run.run();
        }else{
            ui.showInfo("@mod.noerrorplay");
        }
    }

    private void fadeInMenu(){
        submenu.clearActions();
        submenu.actions(Actions.alpha(1f, 0.15f, Interp.fade));
    }

    private void fadeOutMenu(){
        //nothing to fade out
        if(submenu.getChildren().isEmpty()){
            return;
        }

        submenu.clearActions();
        submenu.actions(Actions.alpha(1f), Actions.alpha(0f, 0.2f, Interp.fade), Actions.run(() -> submenu.clearChildren()));
    }

    private void buttons(Table t, MenuButton... buttons){
        for(MenuButton b : buttons){
            if(b == null) continue;
            Button[] out = {null};
            out[0] = t.button(b.text, b.icon, Styles.flatToggleMenut, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    fadeOutMenu();
                }else{
                    if(b.submenu != null && b.submenu.any()){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        fadeInMenu();
                        //correctly offset the button
                        submenu.add().height((Core.graphics.getHeight() - Core.scene.marginTop - Core.scene.marginBottom - out[0].getY(Align.topLeft)) / Scl.scl(1f));
                        submenu.row();
                        buttons(submenu, b.submenu.toArray());
                    }else{
                        currentMenu = null;
                        fadeOutMenu();
                        randomLabel();
                        b.runnable.run();
                    }
                }
            }).marginLeft(11f).get();
            out[0].update(() -> out[0].setChecked(currentMenu == out[0]));
            t.row();
        }
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Drawable icon, Runnable callback){
        addButton(new MenuButton(text, icon, callback));
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Runnable callback){
        addButton(text, Styles.none, callback);
    }

    /**
     * Adds a custom button to the menu.
     * If {@link MenuButton#submenu} is null or the player is on mobile, {@link MenuButton#runnable} is invoked on click.
     * Otherwise, {@link MenuButton#submenu} is shown.
     */
    public void addButton(MenuButton button){
        customButtons.add(button);
    }

    /** Represents a menu button definition. */
    public static class MenuButton{
        public final Drawable icon;
        public final String text;
        /** Runnable ran when the button is clicked. Ignored on desktop if {@link #submenu} is not null. */
        public final Runnable runnable;
        /** Submenu shown when this button is clicked. Used instead of {@link #runnable} on desktop. */
        public final @Nullable Seq<MenuButton> submenu;

        /** Constructs a simple menu button, which behaves the same way on desktop and mobile. */
        public MenuButton(String text, Drawable icon, Runnable runnable){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = null;
        }

        /** Constructs a button that runs the runnable when clicked on mobile or shows the submenu on desktop. */
        public MenuButton(String text, Drawable icon, Runnable runnable, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = submenu != null ? Seq.with(submenu) : null;
        }

        /** Comstructs a desktop-only button; used internally. */
        MenuButton(String text, Drawable icon, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = () -> {};
            this.submenu = submenu != null ? Seq.with(submenu) : null;
        }
    }
}
