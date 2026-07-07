package mindustry.ui.dialogs;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.Elem;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.arcModule.ARCVars;
import mindustry.arcModule.RFuncs;
import mindustry.arcModule.media.ArcSounds;
import mindustry.arcModule.ui.AdvanceToolTable;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.io.*;
import java.util.zip.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class SettingsMenuDialog extends BaseDialog{
    public SettingsTable graphics;
    public SettingsTable game;
    public SettingsTable sound;
    public SettingsTable arc;
    public SettingsTable forcehide;
    public SettingsTable specmode;
    public SettingsTable cheating;
    public SettingsTable dev;
    public SettingsTable main;

    private Table prefs;
    private Table menu;
    private BaseDialog dataDialog;
    private BaseDialog planetDataDialog;
    private Planet planet = Planets.serpulo;
    private Seq<SettingsCategory> categories = new Seq<>();

    public SettingsMenuDialog(){
        super(bundle.get("settings", "Settings"));
        addCloseButton();


        cont.add(main = new SettingsTable());
        shouldPause = true;

        shown(() -> {
            back();
            rebuildMenu();
        });

        int[] lastRebuildSize = {Core.graphics.getWidth(), Core.graphics.getHeight()};
        onResize(() -> {
            if(lastRebuildSize[0] != Core.graphics.getWidth() || lastRebuildSize[1] != Core.graphics.getHeight()){
                graphics.rebuild();
                sound.rebuild();
                game.rebuild();
                dev.rebuild();
                updateScrollFocus();
                lastRebuildSize[0] = Core.graphics.getWidth();
                lastRebuildSize[1] = Core.graphics.getHeight();
            }
        });

        cont.clearChildren();
        cont.remove();
        buttons.remove();

        menu = new Table(Tex.button);

        game = new SettingsTable();
        graphics = new SettingsTable();
        sound = new SettingsTable();
        arc = new SettingsTable();
        forcehide = new SettingsTable();
        specmode = new SettingsTable();
        cheating = new SettingsTable();
        dev = new SettingsTable();

        prefs = new Table();
        prefs.top();
        prefs.margin(14f);

        rebuildMenu();

        prefs.clearChildren();
        prefs.add(menu);

        planetDataDialog = new BaseDialog("@settings.data");
        planetDataDialog.addCloseButton();

        planetDataDialog.cont.table(Tex.button, t -> {
            t.defaults().size(280f, 60f).left();
            TextButtonStyle style = Styles.flatt;

            t.button(bundle.format("settings.planetselect", "[#" + planet.iconColor + "]" + planet.localizedName), Icon.planet, style, () -> {
                BaseDialog dialog = new BaseDialog("");
                dialog.cont.pane(p -> {
                    p.background(Tex.button);
                    int i = 0;

                    for(var plan : content.planets()){
                        if(plan.generator == null || plan.sectors.size == 0 || !plan.accessible) continue;

                        p.button(plan.localizedName, Styles.flatTogglet, () -> {
                            planet = plan;
                            dialog.hide();
                        }).size(110f, 45f).checked(planet == plan);

                        if(++i % 4 == 0){
                            p.row();
                        }
                    }
                });
                dialog.setFillParent(false);
                dialog.addCloseButton();
                dialog.show();
            }).marginLeft(4).get().getLabel().setText(() -> bundle.format("settings.planetselect", "[#" + planet.iconColor + "]" + planet.localizedName));

            t.row();

            t.button("@settings.clearplanetresearch", Icon.trash, style, () -> {
                ui.showConfirm("@confirm", bundle.format("settings.clearplanetresearch.confirm", planet.localizedName), () -> {
                    universe.clearLoadoutInfo();
                    for(TechNode node : TechTree.all){
                        if(node.rootNode == planet.techTree){
                            node.reset();
                        }
                    }
                    content.each(c -> {
                        if(c instanceof UnlockableContent u && u.databaseTabs.contains(planet)){
                            u.clearUnlock();
                        }
                    });
                    settings.remove("unlocks");
                });
            }).marginLeft(4);

            t.row();

            t.button("@settings.clearplanetcampaignsaves", Icon.trash, style, () -> {
                ui.showConfirm("@confirm", bundle.format("settings.clearplanetcampaignsaves.confirm", planet.localizedName), () -> {
                    planet.clearStats();
                    boolean any = false;
                    for(var sec : planet.sectors){
                        sec.clearInfo();
                        if(sec.save != null){
                            any = true;
                            sec.save.delete();
                            sec.save = null;
                        }
                    }
                    if(any){
                        planet.reloadMeshAsync();
                    }

                    for(var slot : control.saves.getSaveSlots().copy()){
                        if(slot.isSector() && slot.getSector().planet == planet){
                            slot.delete();
                        }
                    }
                });
            }).marginLeft(4);

            t.row();
        });

        dataDialog = new BaseDialog("@settings.data");
        dataDialog.addCloseButton();

        dataDialog.cont.table(Tex.button, t -> {
            t.defaults().size(280f, 60f).left();
            TextButtonStyle style = Styles.flatt;

            t.button("@settings.cleardata", Icon.trash, style, () -> ui.showConfirm("@confirm", "@settings.clearall.confirm", () -> {
                ObjectMap<String, Object> map = new ObjectMap<>();
                for(String value : Core.settings.keys()){
                    if(value.contains("usid") || value.contains("uuid")){
                        map.put(value, Core.settings.get(value, null));
                    }
                }
                Core.settings.clear();
                Core.settings.putAll(map);

                for(Fi file : dataDirectory.list()){
                    file.deleteDirectory();
                }

                Core.app.exit();
            })).marginLeft(4);

            t.row();

            t.button("@settings.clearplanetdata", Icon.trash, style, () -> planetDataDialog.show()).marginLeft(4).row();

            t.button("@settings.clearsaves", Icon.trash, style, () -> {
                ui.showConfirm("@confirm", "@settings.clearsaves.confirm", () -> {
                    control.saves.deleteAll();
                });
            }).marginLeft(4);

            t.row();

            t.button("@settings.clearresearch", Icon.trash, style, () -> {
                ui.showConfirm("@confirm", "@settings.clearresearch.confirm", () -> {
                    universe.clearLoadoutInfo();
                    for(TechNode node : TechTree.all){
                        node.reset();
                    }
                    content.each(c -> {
                        if(c instanceof UnlockableContent u){
                            u.clearUnlock();
                        }
                    });
                    settings.remove("unlocks");
                });
            }).marginLeft(4);

            t.row();

            t.button("@settings.clearcampaignsaves", Icon.trash, style, () -> {
                ui.showConfirm("@confirm", "@settings.clearcampaignsaves.confirm", () -> {
                    for(var planet : content.planets()){
                        planet.clearStats();
                        boolean any = false;
                        for(var sec : planet.sectors){
                            sec.clearInfo();
                            if(sec.save != null){
                                any = true;
                                sec.save.delete();
                                sec.save = null;
                            }
                        }
                        if(any){
                            planet.reloadMeshAsync();
                        }
                    }

                    for(var slot : control.saves.getSaveSlots().copy()){
                        if(slot.isSector()){
                            slot.delete();
                        }
                    }
                });
            }).marginLeft(4);

            t.row();

            t.button("@data.export", Icon.upload, style, () -> {
                FileChooser.export("mindustry-data-export", "zip", this::exportData);
            }).marginLeft(4);

            t.row();

            t.button("@data.import", Icon.download, style, () -> ui.showConfirm("@confirm", "@data.import.confirm", () -> FileChooser.open("zip").submit(file -> {
                try{
                    importData(file);
                    mapPreviewDirectory.deleteDirectory();
                    control.saves.resetSave();
                    state = new GameState();
                    Core.app.exit();
                }catch(IllegalArgumentException e){
                    ui.showErrorMessage("@data.invalid");
                }catch(Exception e){
                    Log.err(e);
                    if(e.getMessage() == null || !e.getMessage().contains("too short")){
                        ui.showException(e);
                    }else{
                        ui.showErrorMessage("@data.invalid");
                    }
                }
            }))).marginLeft(4);

            if(!mobile){
                t.row();
                t.button("@data.openfolder", Icon.folder, style, () -> Core.app.openFolder(Core.settings.getDataDirectory().absolutePath())).marginLeft(4);
            }

            t.row();

            t.button("@crash.export", Icon.upload, style, () -> {
                if(settings.getDataDirectory().child("crashes").list().length == 0 && !settings.getDataDirectory().child("last_log.txt").exists()){
                    ui.showInfo("@crash.none");
                }else{
                    FileChooser.export("logs", "txt", file -> file.writeString(getLogs()));
                }
            }).marginLeft(4);
        });

        row();
        pane(prefs).grow().top();
        row();
        add(buttons).fillX();

        addSettings();
    }

    @Override
    public void closeOnBack() {
        ArcSounds.play("returnTitle");
        super.closeOnBack();
    }

    String getLogs(){
        Fi log = settings.getDataDirectory().child("last_log.txt");

        StringBuilder out = new StringBuilder();
        for(Fi fi : settings.getDataDirectory().child("crashes").list()){
            out.append(fi.name()).append("\n\n").append(fi.readString()).append("\n");
        }

        if(log.exists()){
            out.append("\nlast log:\n").append(log.readString());
        }

        return out.toString();
    }

    /** Adds a custom settings category, with the icon being the specified region. */
    public void addCategory(String name, @Nullable String region, Cons<SettingsTable> builder){
        categories.add(new SettingsCategory(name, region == null ? null : new TextureRegionDrawable(atlas.find(region)), builder));
    }

    /** Adds a custom settings category, for use in mods. The specified consumer should add all relevant mod settings to the table. */
    public void addCategory(String name, @Nullable Drawable icon, Cons<SettingsTable> builder){
        categories.add(new SettingsCategory(name, icon, builder));
    }

    /** Adds a custom settings category, for use in mods. The specified consumer should add all relevant mod settings to the table. */
    public void addCategory(String name, Cons<SettingsTable> builder){
        addCategory(name, (Drawable)null, builder);
    }

    public Seq<SettingsCategory> getCategories(){
        return categories;
    }

    void rebuildMenu(){
        menu.clearChildren();

        TextButtonStyle style = Styles.flatt;

        float marg = 8f, isize = iconMed;

        menu.defaults().size(300f, 60f);
        menu.button("@settings.game", Icon.settings, style, isize, () -> {
            visible(0);
            ArcSounds.play("settingTabGame");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.graphics", Icon.image, style, isize, () -> {
            visible(1);
            ArcSounds.play("settingTabGraphics");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.sound", Icon.volumeUp, style, isize, () -> {
            visible(2);
            ArcSounds.play("settingTabSound");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.arc", Icon.star, style, isize, () -> {
            visible(3);
            ArcSounds.play("settingTabArc");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.forcehide", Icon.eyeSmall, style, isize, () -> {
            visible(4);
            ArcSounds.play("settingTabForcehide");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.specmode", Icon.info, style, isize, () -> {
            visible(5);
            ArcSounds.play("settingTabSpecmode");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.cheating", Icon.lock, style, isize, () -> {
            visible(6);
            ArcSounds.play("settingTabCheating");
        }).marginLeft(marg).row();

        menu.row();

        menu.button("@settings.language", Icon.chat, style, isize, ui.language::show).marginLeft(marg).row();

        if(!mobile || Core.settings.getBool("keyboard")){
            menu.button("@settings.controls", Icon.move, style, isize, () -> {
                ui.controls.show();
                ArcSounds.play("settingTabControl");
            }).marginLeft(marg).row();
        }
        menu.button("@settings.data", Icon.save, style, isize, () -> {
            dataDialog.show();
            ArcSounds.play("attention");
        }).marginLeft(marg).row();

        menu.button("@settings.dev", Icon.fileCode, style, isize, () -> visible(3)).marginLeft(marg).row();

        int i =  8;
        for(var cat : categories){
            int index = i;
            if(cat.icon == null){
                menu.button(cat.name, style, () -> visible(index)).marginLeft(marg).row();
            }else{
                menu.button(cat.name, cat.icon, style, isize, () -> visible(index)).with(b -> ((Image)b.getChildren().get(1)).setScaling(Scaling.fit)).marginLeft(marg).row();
            }
            i++;
        }

    }

    void addSettings(){
        sound.addCategory("SoundSetting");
        sound.checkPref("alwaysmusic", false);
        sound.sliderPref("musicvol", 100, 0, 100, 1, i -> i + "%");
        sound.sliderPref("sfxvol", 100, 0, 100, 1, i -> i + "%");
        sound.sliderPref("ambientvol", 100, 0, 100, 1, i -> i + "%");
        sound.sliderPref("arcvol", settings.getInt("musicvol"), 0, 100, 1, i -> i + "%");
        sound.addCategory("arcCustomSound");
        sound.checkPref("enableArcCustomSound", false);
        sound.sliderPref("ArcCustomSoundvol", 100, 0, 100, 1, i -> i + "%");

        game.addCategory("arcCNet");
        game.stringInput("arcNetProxy", "");
        game.addCategory("arcCSave");
        game.checkPref("savecreate", true);
        game.checkPref("save_more_map", false);
        game.sliderPref("saveinterval", 60, 10, 5 * 120, 10, i -> Core.bundle.format("setting.seconds", i));

        game.addCategory("arcCAssist");
        game.checkPref("autotarget", true);
        game.checkPref("keyboard", false, val -> {
            control.setInput(val ? new DesktopInput() : new MobileInput());
            input.setUseKeyboard(val);
        });
        if(Core.settings.getBool("keyboard")){
            control.setInput(new DesktopInput());
            input.setUseKeyboard(true);
        }

        //the issue with touchscreen support on desktop is that:
        //1) I can't test it
        //2) the SDL backend doesn't support multitouch
        /*else{
            game.checkPref("touchscreen", false, val -> control.setInput(!val ? new DesktopInput() : new MobileInput()));
            if(Core.settings.getBool("touchscreen")){
                control.setInput(new MobileInput());
            }
        }*/
        if(!mobile){
            game.checkPref("crashreport", true);
        }
        game.checkPref("communityservers", true, val -> {
            defaultServers.clear();
            if(val){
                JoinDialog.fetchServers();
            }
        });

        game.checkPref("savecreate", true);
        game.checkPref("blockreplace", true);
        game.checkPref("conveyorpathfinding", true);
        game.checkPref("shiftCopyIcon", true);

            game.checkPref("backgroundpause", true);
            game.checkPref("buildautopause", false);
            game.checkPref("distinctcontrolgroups", true);

            game.checkPref("doubletapmine", false);
            game.checkPref("commandmodehold", true);

            if (!ios) {
                game.checkPref("modcrashdisable", true);
            }

            if (steam) {
                game.sliderPref("playerlimit", 16, 2, 32, i -> {
                    platform.updateLobby();
                    return i + "";
                });

            }


        game.addCategory("arcCHint");
        game.checkPref("console", false);
        game.checkPref("hints", true);
        game.checkPref("logichints", true);
        graphics.sliderPref("uiEdgePadding", 0, 0, 100, s -> s + "px", s -> {
            if(ui != null){
                ui.updateMargins();
                Core.scene.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
            }
        });

        graphics.addCategory("arcCOverview");

        graphics.sliderPref("fpscap", 240, 10, 245, 5, s -> (s > 240 ? Core.bundle.get("setting.fpscap.none") : Core.bundle.format("setting.fpscap.text", s)));
        int[] lastUiScale = {settings.getInt("uiscale", 100)};

        graphics.sliderPref("uiscale", 100, 25, 300, 5, s -> {
            //if the user changed their UI scale, but then put it back, don't consider it 'changed'
            Core.settings.put("uiscalechanged", s != lastUiScale[0]);
            return s + "%";
        });

        graphics.sliderPref("screenshake", 4, 0, 8, i -> (i / 4f) + "x");

        graphics.sliderPref("bloomintensity", 6, 0, 16, i -> (int)(i/4f * 100f) + "%");
        graphics.sliderPref("bloomblur", 2, 1, 16, i -> i + "x");

        graphics.sliderPref("fpscap", 240, 10, 245, 5, s -> {
            if(ios){
                Core.graphics.setPreferredFPS(s > 240 ? 0 : s);
            }
            return (s > 240 ? Core.bundle.get("setting.fpscap.none") : Core.bundle.format("setting.fpscap.text", s));
        });

        if(ios){
            int value = Core.settings.getInt("fpscap", 240);
            Core.graphics.setPreferredFPS(value > 240 ? 0 : value);
        }

        graphics.sliderPref("maxmagnificationmultiplierpercent", 100, 100, 200, 25, s -> {
            if(ui.settings != null){
                Core.settings.put("maxzoomingamemultiplier", (float)s / 100.0f);
            }
            return s + "%";
        });

        graphics.sliderPref("minmagnificationmultiplierpercent", 100, 100, 300, 25, s -> {
            if(ui.settings != null){
                Core.settings.put("minzoomingamemultiplier", (float)s / 100.0f);
            }
            return s + "%";
        });

        if(!mobile){
            graphics.checkPref("vsync", true, b -> Core.graphics.setVSync(b));
            graphics.checkPref("fullscreen", false, b -> Core.graphics.setFullscreen(b));

            Core.graphics.setVSync(Core.settings.getBool("vsync"));

            if(Core.settings.getBool("fullscreen")){
                Core.app.post(() -> Core.graphics.setFullscreen(true));
            }
        }else if(!ios){
            graphics.checkPref("landscape", false, b -> {
                if(b){
                    platform.beginForceLandscape();
                }else{
                    platform.endForceLandscape();
                }
            });

            if(Core.settings.getBool("landscape")){
                platform.beginForceLandscape();
            }
        }

        graphics.addCategory("arcCgamewindow");
        graphics.checkPref("fps", false);
        graphics.checkPref("override_boss_shown", false);

        graphics.checkPref("displayselection", true);
        graphics.checkPref("effects", true);
        graphics.checkPref("atmosphere", true);
        graphics.checkPref("drawlight", true);
        graphics.checkPref("destroyedblocks", true);
        graphics.checkPref("playerchat", true);
        if(!mobile){
            graphics.checkPref("coreitems", true);
        }
        graphics.checkPref("minimap", !mobile);
            graphics.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            graphics.checkPref("minimapTools", !mobile);
            if(!mobile){
            graphics.checkPref("detach-camera", false);
        }graphics.checkPref("position", false);

        graphics.checkPref("showpings", true);
        graphics.checkPref("showotherbuildplans", true);
        graphics.checkPref("mouseposition", false);
        graphics.sliderPref("chatopacity", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");

        graphics.addCategory("arcCgameview");
        graphics.checkPref("blockstatus", false);
        graphics.checkPref("playerchat", true);
        graphics.checkPref("alwaysshowdropzone", false);
        graphics.checkPref("showFlyerSpawn", false);
        graphics.checkPref("showFlyerSpawnLine", false);
        graphics.sliderPref("lasersopacity", 100, 0, 100, 5, s -> {
            if (ui.settings != null) {
                Core.settings.put("preferredlaseropacity", s);
            }
            return s + "%";
        });
        graphics.sliderPref("unitlaseropacity", 100, 0, 100, 5, s -> s + "%");
        graphics.sliderPref("bridgeopacity", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
        graphics.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
        graphics.checkPref("playerindicators", true);
        graphics.checkPref("indicators", true);

        graphics.addCategory("arcCGraphicsOther");
        graphics.checkPref("smoothcamera", true);
        graphics.sliderPref("screenshake", 4, 0, 8, i -> (i / 4f) + "x");
        graphics.checkPref("skipcoreanimation", false);
        if (!mobile) {
            Core.settings.put("swapdiagonal", false);
        }

        arc.addCategory("arcHudToolbox");
        arc.sliderPref("AuxiliaryTable", 0, 0, 3, 1, s -> {
            if (s == 0) {
                return "关闭";
            } else if (s == 1) {
                return "左上-右";
            } else if (s == 2) {
                return "左上-下";
            } else if (s == 3) {
                return "右上-下";
            } else {
                return "";
            }
        });
        arc.checkPref("showAdvanceToolTable", false);
        arc.checkPref("arcSpecificTable", true);
        arc.checkPref("logicSupport", true);
        arc.checkPref("powerStatistic", true);
        arc.sliderPref("arccoreitems", 3, 0, 3, 1, s -> {
            if (s == 0) {
                return "不显示";
            } else if (s == 1) {
                return "资源状态";
            } else if (s == 2) {
                return "兵种状态";
            } else {
                return "显示资源和兵种";
            }
        });
        arc.sliderPref("statisticsInterval", 0, 0, 120, 10, s -> {
            if (s == 0) {
                return "不统计";
            } else {
                return "每 " + s + " s";
            }
        });
        arc.sliderPref("arcCoreItemsCol", 5, 4, 15, 1, i -> i + "列");
        arc.checkPref("showQuickToolTable", settings.getBool("showFloatingSettings"));
        arc.sliderPref("quickHudSize", 0, 0, 10, 1, i -> i + "");
        arc.sliderPref("arcDetailInfo", 1, 0, 1, 1, s -> {
            if (s == 0) {
                return "详细模式";
            } else if (s == 1) {
                return "简略模式";
            } else {
                return s + "";
            }
        });
        arc.checkPref("hoveredTileInfo", false);

        arc.addCategory("arcAddBlockInfo");
        arc.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
        arc.sliderPref("mend_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
        arc.checkPref("blockdisabled", false);
        arc.checkPref("blockBars", false);
        arc.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");
        arc.checkPref("blockBars_mend", false);
        arc.checkPref("arcdrillmode", false);
        arc.checkPref("arcDrillProgress", false);
        arc.checkPref("arcchoiceuiIcon", false);
        arc.checkPref("hidedisplays", false);
        arc.checkPref("arcPlacementEffect", false);

        arc.addCategory("arcMassDriverInfo");
        arc.sliderPref("msLineAlpha", settings.getInt("mass_driver_line_alpha", 0), 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");
        arc.checkPref("msShootingDraw", false);
        arc.sliderPref("msLineInterval", settings.getInt("mass_driver_line_interval", 40), 8, 400, 8, i -> i / 8f + "格");
        arc.stringInput("msLineColor", settings.getString("mass_driver_line_color", "ff8c66"));

        arc.addCategory("arcAddTurretInfo");
        arc.checkPref("showTurretAmmo", false);
        arc.checkPref("showTurretAmmoAmount", false);
        arc.checkPref("arcTurretPlacementItem", false);
        arc.checkPref("arcTurretPlaceCheck", false);
        arc.sliderPref("turretShowRange", 0, 0, 3, 1, s -> {
            if (s == 0) {
                return "关闭";
            } else if (s == 1) {
                return "仅对地";
            } else if (s == 2) {
                return "仅对空";
            } else if (s == 3) {
                return "全部";
            } else {
                return "";
            }
        });
        arc.checkPref("turretForceShowRange", false);
        arc.sliderPref("turretAlertRange", 0, 0, 30, 1, i -> i > 0 ? i + "格" : "关闭");
        arc.checkPref("blockWeaponTargetLine", false);
        arc.checkPref("blockWeaponTargetLineWhenIdle", false);

        arc.addCategory("arcAddUnitInfo");
        arc.checkPref("alwaysShowPlayerUnit", false);

        arc.sliderPref("unitTransparency", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
        arc.sliderPref("unitDrawMinHealth", settings.getInt("minhealth_unitshown",0), 0, 2500, 50, i -> i + "[red]HP");

        arc.checkPref("unitHealthBar", false);
        arc.checkPref("drawWeaponRecharge", false);
        arc.sliderPref("unitBarDrawMinHealth", settings.getInt("minhealth_unithealthbarshown",0), 0, 2500, 100, i -> i + "[red]HP");


        arc.sliderPref("unitWeaponRange", settings.getInt("unitAlertRange",0), 0, 30, 1, s -> {
            if (s == 0) {
                return "关闭";
            } else if (s == 30) {
                return "一直开启";
            } else {
                return s + "格";
            }
        });
        arc.sliderPref("unitWeaponRangeAlpha", settings.getInt("unitweapon_range",0), 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

        arc.checkPref("unitWeaponTargetLine", false);
        arc.checkPref("showminebeam", true);
        arc.checkPref("unitItemCarried", false);
        arc.checkPref("unithitbox", false);
        arc.checkPref("unitLogicMoveLine", false);
        arc.checkPref("unitLogicTimerBars", false);
        arc.checkPref("arcBuildInfo",false);
        arc.checkPref("unitbuildplan", false);
        arc.checkPref("payloadpreview", false);

        arc.addCategory("arcRTSSupporter");
        arc.checkPref("arcCommandTable", true);
        arc.checkPref("alwaysShowUnitRTSAi", false);
        arc.sliderPref("rtsWoundUnit", 0, 0, 100, 2, s -> s + "%");

        arc.addCategory("arcBackup");
        arc.sliderPref("arcSaveMode", settings.getInt("arcSaveMode",0), 0, 2, 1, s -> switch (s) {
            case 0 -> "关闭";
            case 1 -> "仅本地";
            default -> "本地与服务器";
        });
        arc.sliderPref("arcBackupSlot", 0, 0, 10, 1, s -> s + "");
        arc.sliderPref("arcBackupInterval", 30, 30, 900, 30, s -> s + "s");

        arc.addCategory("arcShareinfo");
        arc.checkPref("arcPlayerList", true);
        arc.checkPref("ShowInfoPopup", true);
        arc.checkPref("arcShareWaveInfo", false);
        arc.checkPref("arcAlwaysTeamColor", false);
        arc.checkPref("arcSelfName", false);
        //arc.stringInput("arcDisablePacket", "^(.*\\.)?mindustry\\.top(:.*)?$");

        arc.addCategory("arcPlayerEffect");
        arc.stringInput("playerEffectColor", "ffd37f");
        arc.sliderPref("unitTargetType", 0, 0, 5, 1, s -> {
            if (s == 0) {
                return "关闭";
            } else if (s == 1) {
                return "虚圆";
            } else if (s == 2) {
                return "攻击";
            } else if (s == 3) {
                return "攻击去边框";
            } else if (s == 4) {
                return "圆十字";
            } else if (s == 5) {
                return "十字";
            } else {
                return s + "";
            }
        });
        arc.sliderPref("superUnitEffect", 0, 0, 2, 1, s -> {
            if (s == 0) {
                return "关闭";
            } else if (s == 1) {
                return "独一无二";
            } else if (s == 2) {
                return "全部玩家";
            } else {
                return s + "";
            }
        });
        arc.sliderPref("playerEffectCurStroke", 0, 1, 30, 1, i -> (float) i / 10f + "Pixel(s)");

        arc.addCategoryS("雷达扫描设置 [lightgray](PC按键，手机辅助器)");
        arc.sliderPref("radarMode", 0, 0, 30, 1, s -> {
            if (s == 0) return "关闭";
            else if (s == 30) return "一键开关";
            else {
                return "[lightgray]x[white]" + Strings.autoFixed(s * 0.2f, 1) + "倍搜索";
            }
        });
        arc.sliderPref("radarSize", 0, 0, 50, 1, s -> {
            if (s == 0) return "固定大小";
            else {
                return "[lightgray]x[white]" + Strings.autoFixed(s * 0.1f, 1) + "倍";
            }
        });

        arc.addCategory("developerMode");
        if (steam) arc.stringInput("arcSteamOverride", "");
        arc.checkPref("arcDisableModWarning", false);
        arc.sliderPref("menuFlyersCount", 0, -15, 50, 5, i -> i + "");
        arc.checkPref("menuFlyersRange", false);
        arc.checkPref("menuFlyersFollower", false);
        arc.checkPref("menuFloatText", true);
        arc.checkPref("showUpdateDialog", true);
        arc.checkPref("arcInfSchem", false);

        //////////forcehide
        forcehide.addCategory("arcCDisplayBlock");
        forcehide.sliderPref("blockRenderLevel", 2, 0, 2, 1, s -> {
            if (s == 0) {
                return "隐藏全部建筑";
            } else if (s == 1) {
                return "只显示建筑状态";
            } else if (s == 2) {
                return "全部显示";
            } else {
                return s + "";
            }
        });
        forcehide.checkPref("displayblock", true);
        forcehide.addCategory("arcCDisplayEffect");
        forcehide.checkPref("bulletShow", true);
        forcehide.checkPref("drawlight", true);
        forcehide.checkPref("effects", true);
        forcehide.checkPref("bloom", true, val -> renderer.toggleBloom(val));
        forcehide.sliderPref("bloomintensity", 6, 0, 16, i -> (int) (i / 4f * 100f) + "%");
        forcehide.sliderPref("bloomblur", 2, 1, 16, i -> i + "x");
        forcehide.checkPref("forceEnableDarkness", true);
        forcehide.checkPref("destroyedblocks", true);
        forcehide.checkPref("showweather", true);
        forcehide.checkPref("animatedwater", true);

        if(Shaders.shield != null){
            forcehide.checkPref("animatedshields", true);
            forcehide.checkPref("staticShieldsBorder", false);
        }

            forcehide.checkPref("atmosphere", !mobile);

        graphics.checkPref("pixelate", false, val -> {
            if(val){
                Events.fire(Trigger.enablePixelation);
            }
        });

        //iOS (and possibly Android) devices do not support linear filtering well, so disable it
        graphics.checkPref("linear", !mobile, b -> {
            for(Texture tex : Core.atlas.getTextures()){
                TextureFilter filter = b ? TextureFilter.linear : TextureFilter.nearest;
                tex.setFilter(filter, filter);
            }
        });

        if(Core.settings.getBool("linear")){
            for(Texture tex : Core.atlas.getTextures()){
                TextureFilter filter = TextureFilter.linear;
                tex.setFilter(filter, filter);
            }
        }

        forcehide.checkPref("pixelate", false, val -> {
            if (val) {
                Events.fire(Trigger.enablePixelation);
            }
        });

        ARCVars.limitUpdate = settings.getBool("limitupdate", false);
        forcehide.checkPref("limitupdate", false, v -> {
            settings.put("limitupdate", false);
            if (ARCVars.limitUpdate) {
                ARCVars.limitUpdate = false;
                return;
            }
            ui.showConfirm("确认开启限制更新", "此功能可以大幅提升fps，但会导致视角外的一切停止更新\n在服务器里会造成不同步\n强烈不建议在单人开启\n\n[darkgray]在帧数和体验里二选一", () -> {
                ARCVars.limitUpdate = true;
                settings.put("limitupdate", true);
            });
        });
        ARCVars.limitDst = settings.getInt("limitdst", 10);
        forcehide.sliderPref("limitdst", 10, 0, 100, 1, s -> {
            ARCVars.limitDst = s * 8;
            return s + "格";
        });

        //////////specmode
        specmode.addCategory("moreContent");
        specmode.checkPref("modMode", false);
        specmode.sliderPref("itemSelectionHeight", 4, 4, 12, i -> i + "行");
        specmode.sliderPref("itemSelectionWidth", 4, 4, 12, i -> i + "列");
        specmode.sliderPref("blockInventoryWidth", 3, 3, 16, i -> i + "");
        specmode.sliderPref("editorBrush", 4, 3, 12, i -> i + "");

        specmode.addCategory("personalized");
        specmode.checkPref("colorizedContent", false);
        specmode.sliderPref("fontSet", 0, 0, 2, 1, s -> {
            if (s == 0) {
                return "原版字体";
            } else if (s == 1) return "[violet]LC[white]の[cyan]萌化字体包";
            else if (s == 2) return "[violet]9527[white]の[cyan]楷体包";
            else {
                return s + "";
            }
        });
        specmode.sliderPref("fontSize", 10, 5, 25, 1, i -> "x " + Strings.fixed(i * 0.1f, 1));
        specmode.stringInput("themeColor", "ffd37f");
        //specmode.stringInput("arcBackgroundPath", ""); 使用默认路径
        if (!OS.isAndroid && !OS.isIos) {
            specmode.stringInput("arcCursorPath", "");
            specmode.buttonInput("[cyan]查看当前指针样式", () -> new BaseDialog("指针样式") {{
                shown(() -> {
                    addCloseButton();

                    cont.add("[orange]将鼠标悬停在这些框框上面，预览指针样式 (这些名字就是自定义指针文件名)").row();
                    cont.add("[cyan]图片中心是指针中心").row();
                    cont.button("[orange]重载指针", () -> {
                        RFuncs.cursorChecked = false;
                        RFuncs.cachedCursor = null;
                        ui.drillCursor = RFuncs.customCursor("drill", Fonts.cursorScale());
                        ui.unloadCursor = RFuncs.customCursor("unload", Fonts.cursorScale());
                        ui.targetCursor = RFuncs.customCursor("target", Fonts.cursorScale());
                        ARCVars.arcui.resizeHorizontalCursor = RFuncs.customCursor("resizeHorizontal", Fonts.cursorScale());
                        ARCVars.arcui.resizeVerticalCursor = RFuncs.customCursor("resizeVertical", Fonts.cursorScale());
                        ARCVars.arcui.resizeLeftCursor = RFuncs.customCursor("resizeLeft", Fonts.cursorScale());
                        ARCVars.arcui.resizeRightCursor = RFuncs.customCursor("resizeRight", Fonts.cursorScale());
                        Fonts.loadSystemCursors();
                    }).growX().row();
                    cont.table(root -> {
                        root.table(t -> t.add("cursor").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(Graphics.Cursor.SystemCursor.arrow));
                        root.table(t -> t.add("hand").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand));
                        root.table(t -> t.add("ibeam").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(Graphics.Cursor.SystemCursor.ibeam));
                    }).growX().row();
                    cont.table(root -> {
                        root.table(t -> t.add("drill").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ui.drillCursor));
                        root.table(t -> t.add("unload").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ui.unloadCursor));
                        root.table(t -> t.add("target").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ui.targetCursor));
                    }).growX().row();
                    cont.table(root -> {
                        root.table(t -> t.add("resizeHorizontal").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ARCVars.arcui.resizeHorizontalCursor));
                        root.table(t -> t.add("resizeVertical").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ARCVars.arcui.resizeVerticalCursor));
                        root.table(t -> t.add("resizeLeft").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ARCVars.arcui.resizeLeftCursor));
                        root.table(t -> t.add("resizeRight").pad(10)).height(80).growX().pad(10).touchable(Touchable.enabled).get().background(Styles.grayPanel).hovered(() -> Core.graphics.cursor(ARCVars.arcui.resizeRightCursor));
                    }).growX();
                });
            }}.show());
        }
        specmode.checkPref("yuanshen", false, b -> {
            if (b) {
                dataDirectory.child("yuanshen").writeString("原神，启动！");
            } else {
                dataDirectory.child("yuanshen").delete();
            }
        });
        specmode.checkPref("xibaoOnKick", false);
        specmode.addCategory("specGameMode");
        specmode.checkPref("autoSelSchematic", false);
        specmode.checkPref("researchViewer", false);
        specmode.checkPref("bossKeyValid",false);
        specmode.checkPref("arcShareMedia",true);
        specmode.checkPref("rotateCanvas",false);
        specmode.checkPref("developMode", false);
        //////////cheating
        cheating.addCategory("arcWeakCheat");
        cheating.checkPref("forceIgnoreAttack", false);
        cheating.checkPref("allBlocksReveal", false, b -> AdvanceToolTable.allBlocksReveal = b);
        cheating.checkPref("worldCreator", false, b -> AdvanceToolTable.worldCreator = b);
        cheating.checkPref("overrideSkipWave", false);
        cheating.checkPref("forceConfigInventory", false);
        cheating.addCategory("arcStrongCheat");
        cheating.checkPref("showOtherTeamResource", false);
        cheating.checkPref("showOtherTeamState", false);
        cheating.checkPref("selectTeam", false);
        cheating.checkPref("playerNeedShooting", false);
        cheating.checkPref("otherCheat", false);
        if (OS.isMac) {
            graphics.checkPref("macnotch", false);
        }

        if(!mobile){
            Core.settings.put("swapdiagonal", false);
        }

        dev.checkPref("console", false);
        dev.checkPref("drawhitboxes", false);
        dev.checkPref("showperformance", false);

        if(!ios){
            dev.checkPref("modcrashdisable", true);
        }
    }

    public void exportData(Fi file) throws IOException{
        Seq<Fi> files = new Seq<>();
        files.add(Core.settings.getSettingsFile());
        files.addAll(customMapDirectory.list());
        files.addAll(saveDirectory.list());
        files.addAll(modDirectory.list());
        files.addAll(schematicDirectory.list());
        files.addAll(assetCacheDirectory.list()); //important for saves
        String base = Core.settings.getDataDirectory().path();

        //add directories
        for(Fi other : files.copy()){
            Fi parent = other.parent();
            while(!files.contains(parent) && !parent.equals(settings.getDataDirectory())){
                files.add(parent);
            }
        }

        try(OutputStream fos = file.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
            for(Fi add : files){
                String path = add.path().substring(base.length());
                if(add.isDirectory()) path += "/";
                //fix trailing / in path
                path = path.startsWith("/") ? path.substring(1) : path;
                zos.putNextEntry(new ZipEntry(path));
                if(!add.isDirectory()){
                    try(var stream = add.read()){
                        Streams.copy(stream, zos);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public void importData(Fi file){
        Fi dest = Core.files.local("zipdata.zip");
        file.copyTo(dest);
        Fi zipped = new ZipFi(dest);

        Fi base = Core.settings.getDataDirectory();
        if(!zipped.child("settings.bin").exists()){
            throw new IllegalArgumentException("Not valid save data.");
        }

        //delete old saves so they don't interfere
        saveDirectory.deleteDirectory();

        //clear old assets cache
        assetCacheDirectory.deleteDirectory();

        //purge existing tmp data, keep everything else
        tmpDirectory.deleteDirectory();

        zipped.walk(f -> f.copyTo(base.child(f.path())));
        dest.delete();

        //clear old data
        settings.clear();
        //load data so it's saved on exit
        settings.load();
    }

    private void back(){
        rebuildMenu();
        prefs.clearChildren();
        prefs.add(menu);
    }

    private void visible(int index){
        prefs.clearChildren();

        Seq<Table> tables = new Seq<>();

        tables.addAll(game, graphics, sound, arc, forcehide, specmode, cheating, dev);

        for(var custom : categories){
            tables.add(custom.table);
        }

        prefs.add(tables.get(index));
    }

    @Override
    public void addCloseButton(){
        buttons.button("@back", Icon.left, () -> {
            if(prefs.getChildren().first() != menu){
                back();
            }else{
                ArcSounds.play("confirmSetting");
                hide();
            }
        }).size(210f, 64f);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                if(prefs.getChildren().first() != menu){
                    back();
                }else{
                    hide();
                }
            }
        });
    }

    public interface StringProcessor{
        String get(int i);
    }

    public static class SettingsCategory{
        public String name;
        public @Nullable Drawable icon;
        public Cons<SettingsTable> builder;
        public SettingsTable table;

        public SettingsCategory(String name, Drawable icon, Cons<SettingsTable> builder){
            this.name = name;
            this.icon = icon;
            this.builder = builder;

            table = new SettingsTable();
            builder.get(table);
        }
    }

    public static class SettingsTable extends Table{
        protected Seq<Setting> list = new Seq<>();

        public SettingsTable(){
            left();
        }

        public Seq<Setting> getSettings(){
            return list;
        }

        public void pref(Setting setting){
            list.add(setting);
            rebuild();
        }

        public SliderSetting sliderPref(String name, int def, int min, int max, StringProcessor s){
            return sliderPref(name, def, min, max, 1, s);
        }

        public SliderSetting sliderPref(String name, int def, int min, int max, int step, StringProcessor s){
            return sliderPref(name, def, min, max, step, s, null);
        }

        public SliderSetting sliderPref(String name, int def, int min, int max, StringProcessor s, Intc changed){
            return sliderPref(name, def, min, max, 1, s, changed);
        }

        public SliderSetting sliderPref(String name, int def, int min, int max, int step, StringProcessor s, Intc changed){
            SliderSetting res;
            list.add(res = new SliderSetting(name, def, min, max, step, s, changed));
            settings.defaults(name, def);
            rebuild();
            return res;
        }

        public void checkPref(String name, boolean def){
            list.add(new CheckSetting(name, def, null));
            settings.defaults(name, def);
            rebuild();
        }

        public void checkPref(String name, boolean def, Boolc changed){
            list.add(new CheckSetting(name, def, changed));
            settings.defaults(name, def);
            rebuild();
        }

        public void addCategory(String name){
            list.add(new Divider(name, bundle.get("category." + name + ".name")));
            rebuild();
        }

        public void addCategoryS(String name){
            list.add(new Divider(name, name));
            rebuild();
        }

        public void buttonInput(String text, Runnable callback) {
            list.add(new ButtonFakeSetting(text, callback));
            rebuild();
        }

        public void stringInput(String name, String def){
            list.add(new StringSetting(name, def, def));
            settings.defaults(name, def);
            rebuild();
        }

        public void textPref(String name, String def){
            list.add(new TextSetting(name, def, null));
            settings.defaults(name, def);
            rebuild();
        }

        public void textPref(String name, String def, Cons<String> changed){
            list.add(new TextSetting(name, def, changed));
            settings.defaults(name, def);
            rebuild();
        }

        public void areaTextPref(String name, String def){
            list.add(new AreaTextSetting(name, def, null));
            settings.defaults(name, def);
            rebuild();
        }

        public void areaTextPref(String name, String def, Cons<String> changed){
            list.add(new AreaTextSetting(name, def, changed));
            settings.defaults(name, def);
            rebuild();
        }

        public void rebuild(){
            clearChildren();

            for(Setting setting : list){
                setting.add(this);
            }

            button(bundle.get("settings.reset", "Reset to Defaults"), () -> {
                for(Setting setting : list){
                    if(setting.name == null || setting.title == null) continue;
                    settings.remove(setting.name);
                }
                rebuild();
            }).margin(14).width(240f).pad(6);
        }

        public abstract static class Setting{
            public String name;
            public String title;
            public @Nullable String description;

            public Setting(String name){
                this.name = name;
                String winkey = "setting." + name + ".name.windows";
                title = OS.isWindows && bundle.has(winkey) ? bundle.get(winkey) : bundle.get("setting." + name + ".name", name);
                description = bundle.getOrNull("setting." + name + ".description");
            }

            public abstract void add(SettingsTable table);

            public void addDesc(Element elem){
                ui.addDescTooltip(elem, description);
            }
        }

        public static class CheckSetting extends Setting{
            boolean def;
            Boolc changed;

            public CheckSetting(String name, boolean def, Boolc changed){
                super(name);
                this.def = def;
                this.changed = changed;
            }

            @Override
            public void add(SettingsTable table){
                Button box = new Button(Styles.grayt);
                box.background(Styles.grayPanel);
                box.margin(10f);

                box.add(new Image()).update(i -> i.setDrawable(box.isOver() ? (box.isChecked() ? Tex.checkOnOver : Tex.checkOver) : box.isChecked() ? Tex.checkOn : Tex.checkOff))
                    .size(32f).padRight(8f).padLeft(-4f);

                box.add(title);

                box.update(() -> box.setChecked(settings.getBool(name)));

                box.clicked(() -> {
                    settings.put(name, box.isChecked());
                    if(changed != null){
                        changed.get(box.isChecked());
                    }
                });

                box.left();
                addDesc(table.add(box).minWidth(Math.min(500f, Core.graphics.getWidth() / 1.2f / Scl.scl(1f))).fillX().height(45f).left().padTop(7f).get());
                table.row();
            }
        }

        public static class SliderSetting extends Setting{
            int def, min, max, step;
            StringProcessor sp;
            Intc changed;

            public SliderSetting(String name, int def, int min, int max, int step, StringProcessor s, Intc changed){
                super(name);
                this.def = def;
                this.min = min;
                this.max = max;
                this.step = step;
                this.sp = s;
                this.changed = changed;
            }

            @Override
            public void add(SettingsTable table){
                Slider slider = new Slider(min, max, step, false);

                slider.setValue(settings.getInt(name));

                Label value = new Label("", Styles.outlineLabel);
                Table content = new Table();
                content.add(title, Styles.outlineLabel).left().growX().wrap();
                content.add(value).padLeft(10f).right();
                content.margin(3f, 33f, 3f, 33f);
                content.touchable = Touchable.disabled;

                slider.changed(() -> {
                    settings.put(name, (int)slider.getValue());
                    value.setText(sp.get((int)slider.getValue()));
                    if(changed != null) changed.get((int)slider.getValue());
                });

                slider.change();

                addDesc(table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f / Scl.scl(1f), 500f)).left().padTop(4f).get());
                table.row();
            }
        }

        public static class Divider extends Setting {

            Divider(String name, String title) {
                super(name);
                this.title = title;
            }

            @Override
            public void add(SettingsTable table) {
                table.add(title).color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
                table.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            }
        }

        public static class StringSetting extends Setting {
            String def, value;

            StringSetting(String name, String def, String value) {
                super(name);
                this.def = def;
                this.value = value;
            }

            @Override
            public void add(SettingsTable table) {
                value = settings.getString(name);
                Table field = new Table();
                field.add(bundle.get("setting."+name+".name"));
                field.field(value, text -> {
                    settings.put(name, text);
                    value = text;
                }).growX().padLeft(30);
                table.add(field).growX().pad(10).padTop(15).padBottom(4).row();
            }
        }

        public static class TextSetting extends Setting{
            String def;
            Cons<String> changed;

            public TextSetting(String name, String def, Cons<String> changed){
                super(name);
                this.def = def;
                this.changed = changed;
            }

            @Override
            public void add(SettingsTable table){
                TextField field = new TextField();

                field.update(() -> field.setText(settings.getString(name)));

                field.changed(() -> {
                    settings.put(name, field.getText());
                    if(changed != null){
                        changed.get(field.getText());
                    }
                });

                Table prefTable = table.table().left().padTop(3f).get();
                prefTable.add(field);
                prefTable.label(() -> title);
                addDesc(prefTable);
                table.row();
            }
        }

        public static class AreaTextSetting extends TextSetting{
            public AreaTextSetting(String name, String def, Cons<String> changed){
                super(name, def, changed);
            }

            @Override
            public void add(SettingsTable table){
                TextArea area = new TextArea("");
                area.setPrefRows(5);

                area.update(() -> {
                    area.setText(settings.getString(name));
                    area.setWidth(table.getWidth());
                });

                area.changed(() -> {
                    settings.put(name, area.getText());
                    if(changed != null){
                        changed.get(area.getText());
                    }
                });

                addDesc(table.label(() -> title).left().padTop(3f).get());
                table.row().add(area).left();
                table.row();
            }
        }

        public static class ButtonFakeSetting extends Setting {
            Button button;
            public ButtonFakeSetting(String text, Runnable callback) {
                super("fake");
                button = Elem.newButton(text, callback);
            }

            @Override
            public void add(SettingsTable table) {
                table.row().add(button).growX().height(48).row();
            }
        }
    }
}
