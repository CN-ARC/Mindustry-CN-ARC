package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.geom.Vec3;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.ARCVars;
import mindustry.arcModule.ui.dialogs.TeamSelectDialog;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.editor.BannedContentDialog;
import mindustry.game.*;
import mindustry.game.Rules.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.ui.*;
import mindustry.world.*;

import static arc.Core.*;
import static arc.util.Time.*;
import static mindustry.Vars.*;

public class CustomRulesDialog extends BaseDialog{
    Rules rules;
    private Table main;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;
    private BannedContentDialog<Block> bannedBlocks = new BannedContentDialog<>("@bannedblocks", ContentType.block, Block::canBeBuilt);
    private BannedContentDialog<UnitType> bannedUnits = new BannedContentDialog<>("@bannedunits", ContentType.unit, u -> !u.isHidden());

    private Seq<Team> teams;

    public boolean showRuleEditRule;
    public Seq<Table> categories;
    public Table current;
    public Seq<String> categoryNames;
    public String currentName = "";
    public String ruleSearch = "";
    public Seq<Runnable> additionalSetup; // for modding to easily add new rules

    public CustomRulesDialog(){
        this(false);
    }

    public CustomRulesDialog(boolean showRuleEditRule){
        super("@mode.custom");

        this.showRuleEditRule = showRuleEditRule;

        loadoutDialog = new LoadoutDialog();

        setFillParent(true);
        shown(this::setup);
        addCloseButton();

        teams = Seq.with(Team.baseTeams);

        additionalSetup = new Seq<>();
        categories = new Seq<>();
        categoryNames = new Seq<>();

        buttons.button("@edit", Icon.pencil, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);

            dialog.cont.table(Tex.button, t -> {
                var style = Styles.cleart;
                t.defaults().size(280f, 64f).pad(2f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@copied");

                    //hack: don't write the spawns, they just waste space
                    var spawns = rules.spawns;
                    rules.spawns = new Seq<>();
                    Core.app.setClipboardText(JsonIO.write(rules));
                    rules.spawns = spawns;
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try{
                        Rules newRules = JsonIO.read(Rules.class, Core.app.getClipboardText());
                        //objectives and spawns are considered to be map-specific; don't use them
                        newRules.spawns = rules.spawns;
                        newRules.objectives = rules.objectives;
                        rules = newRules;
                        refresh();
                    }catch(Throwable e){
                        Log.err(e);
                        ui.showErrorMessage("@rules.invaliddata");
                    }
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith("{")).marginLeft(12f).row();

                t.button("@settings.reset", Icon.refresh, style, () -> {
                    rules = resetter.get();
                    refresh();
                }).marginLeft(12f);
            });

            dialog.show();
        });
    }

    private <T extends UnlockableContent> void showBanned(String title, ContentType type, ObjectSet<T> set, Boolf<T> pred){
        BaseDialog bd = new BaseDialog(title);
        bd.addCloseButton();

        Runnable[] rebuild = {null};

        rebuild[0] = () -> {
            float previousScroll = bd.cont.getChildren().isEmpty() ? 0f : ((ScrollPane)bd.cont.getChildren().first()).getScrollY();
            bd.cont.clear();
            bd.cont.pane(t -> {
                t.margin(10f);

                if(set.isEmpty()){
                    t.add("@empty");
                }

                Seq<T> array = set.toSeq();
                array.sort();

                int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
                int i = 0;

                for(T con : array){
                    t.table(Tex.underline, b -> {
                        b.left().margin(4f);
                        b.image(con.uiIcon).size(iconMed).padRight(3);
                        b.add(con.localizedName).color(Color.lightGray).padLeft(3).growX().left().wrap();

                        b.button(Icon.cancel, Styles.emptyi, () -> {
                            set.remove(con);
                            rebuild[0].run();
                        }).size(70f).pad(-4f).padLeft(0f);
                    }).size(300f, 70f).padRight(5);

                    if(++i % cols == 0){
                        t.row();
                    }
                }
            }).get().setScrollYForce(previousScroll);
            bd.cont.row();
            bd.cont.button("@add", Icon.add, () -> {
                BaseDialog dialog = new BaseDialog("@add");
                dialog.cont.pane(t -> {
                    t.left().margin(14f);
                    int[] i = {0};
                    content.<T>getBy(type).each(b -> !set.contains(b) && pred.get(b), b -> {
                        int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                        t.button(new TextureRegionDrawable(b.uiIcon), Styles.flati, iconMed, () -> {
                            set.add(b);
                            rebuild[0].run();
                            dialog.hide();
                        }).size(60f).tooltip(b.localizedName);

                        if(++i[0] % cols == 0){
                            t.row();
                        }
                    });
                });

                dialog.addCloseButton();
                dialog.show();
            }).size(300f, 64f).disabled(b -> set.size == content.<T>getBy(type).count(pred));
        };

        bd.shown(rebuild[0]);
        bd.buttons.button("@addall", Icon.add, () -> {
            set.addAll(content.<T>getBy(type).select(pred));
            rebuild[0].run();
        }).size(180, 64f);

        bd.buttons.button("@clear", Icon.trash, () -> {
            set.clear();
            rebuild[0].run();
        }).size(180, 64f);

        bd.show();
    }

    void refresh(){
        setup();
        requestKeyboard();
        requestScroll();
    }

    public void show(Rules rules, Prov<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        show();
    }

    void setup(){
        cont.clear();
        cont.table(t -> {
            t.add("@search").padRight(10);
            var field = t.field(ruleSearch, text -> {
                ruleSearch = text.trim().replaceAll(" +", " ").toLowerCase();
                setupMain();
            }).grow().pad(8).get();
            field.setCursorPosition(ruleSearch.length());
            Core.scene.setKeyboardFocus(field);
            t.button(Icon.cancel, Styles.emptyi, () -> {
                ruleSearch = "";
                setupMain();
            }).padLeft(10f).size(35f);
        }).row();
        Cell<ScrollPane> paneCell = cont.pane(m -> main = m);

        setupMain();

        paneCell.scrollX(main.getPrefWidth() + 40f > graphics.getWidth());
    }

    void setupMain(){
        categories.clear();
        main.clear();
        main.left().defaults().fillX().left();
        main.row();

        category("waves");
        check("@rules.waves", b -> rules.waves = b, () -> rules.waves);
        check("@rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer, () -> rules.waves);
        check("@rules.wavesending", b -> rules.waveSending = b, () -> rules.waveSending, () -> rules.waves);
        check("@rules.waitForWaveToEnd", b -> rules.waitEnemies = b, () -> rules.waitEnemies, () -> rules.waves && rules.waveTimer);
        number("@rules.winWave", b -> rules.winWave = (int)b, () -> (int)rules.winWave);
        check("@rules.randomwaveai", b -> rules.randomWaveAI = b, () -> rules.randomWaveAI, () -> rules.waves);
        check("@rules.wavespawnatcores", b -> rules.wavesSpawnAtCores = b, () -> rules.wavesSpawnAtCores, () -> rules.waves);
        check("@rules.airUseSpawns", b -> rules.airUseSpawns = b, () -> rules.airUseSpawns, () -> rules.waves);
        numberi("@rules.wavelimit", f -> rules.winWave = f, () -> rules.winWave, () -> rules.waves, 0, Integer.MAX_VALUE);

        number("@rules.wavespacing", false, f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, () -> rules.waves && rules.waveTimer, 1, Float.MAX_VALUE);
        number("@rules.initialwavespacing", false, f -> rules.initialWaveSpacing = f * 60f, () -> rules.initialWaveSpacing / 60f, () -> rules.waves && rules.waveTimer, 0, Float.MAX_VALUE);
        number("@rules.dropzoneradius", false, f -> rules.dropZoneRadius = f * tilesize, () -> rules.dropZoneRadius / tilesize, () -> rules.waves);

        category("resourcesbuilding");
        check("@rules.alloweditworldprocessors", b -> rules.allowEditWorldProcessors = b, () -> rules.allowEditWorldProcessors);
        check("@rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        check("@rules.onlydepositcore", b -> rules.onlyDepositCore = b, () -> rules.onlyDepositCore);
        check("@rules.derelictrepair", b -> rules.derelictRepair = b, () -> rules.derelictRepair);
        check("@rules.reactorexplosions", b -> rules.reactorExplosions = b, () -> rules.reactorExplosions);
        check("@rules.schematic", b -> rules.schematicsAllowed = b, () -> rules.schematicsAllowed);
        check("@rules.coreincinerates", b -> rules.coreIncinerates = b, () -> rules.coreIncinerates);
        check("@rules.cleanupdeadteams", b -> rules.cleanupDeadTeams = b, () -> rules.cleanupDeadTeams);
        check("@rules.disableworldprocessors", b -> rules.disableWorldProcessors = b, () -> rules.disableWorldProcessors);
        number("@rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("@rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier, 0.001f, 50f);
        number("@rules.deconstructrefundmultiplier", false, f -> rules.deconstructRefundMultiplier = f, () -> rules.deconstructRefundMultiplier, () -> !rules.infiniteResources, 0f, 1f);
        number("@rules.blockhealthmultiplier", f -> rules.blockHealthMultiplier = f, () -> rules.blockHealthMultiplier);
        number("@rules.blockdamagemultiplier", f -> rules.blockDamageMultiplier = f, () -> rules.blockDamageMultiplier);
        number("@rules.solarmultiplier", f -> rules.solarMultiplier = f, () -> rules.solarMultiplier);

        if(Core.bundle.get("configure").toLowerCase().contains(ruleSearch)){
            current.button("@configure",
                () -> loadoutDialog.show(999999, rules.loadout,
                    i -> true,
                    () -> rules.loadout.clear().add(new ItemStack(Items.copper, 100)),
                    () -> {}, () -> {}
            )).left().width(300f).row();
        }

        if(Core.bundle.get("bannedblocks").toLowerCase().contains(ruleSearch)){
            current.button("@bannedblocks", () -> bannedBlocks.show(rules.bannedBlocks)).left().width(300f).row();
        }
        main.button("@bannedblocks", () -> showBanned("@bannedblocks", ContentType.block, rules.bannedBlocks, Block::canBeBuilt)).left().width(300f).row();
        main.button("@revealedblocks", () -> showBanned("@revealedblocks", ContentType.block, rules.revealedBlocks, b -> b.showUnlock() && (!b.isVanilla() || b.hasEmoji()))).left().width(300f).row();
        check("@rules.hidebannedblocks", b -> rules.hideBannedBlocks = b, () -> rules.hideBannedBlocks);
        check("@bannedblocks.whitelist", b -> rules.blockWhitelist = b, () -> rules.blockWhitelist);


        category("unit");
        check("@rules.unitcapvariable", b -> rules.unitCapVariable = b, () -> rules.unitCapVariable);
        check("@rules.unitpayloadsexplode", b -> rules.unitPayloadsExplode = b, () -> rules.unitPayloadsExplode);
        numberi("@rules.unitcap", f -> rules.unitCap = f, () -> rules.unitCap, -999, 999);
        number("@rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("@rules.unitcrashdamagemultiplier", f -> rules.unitCrashDamageMultiplier = f, () -> rules.unitCrashDamageMultiplier);
        number("@rules.unitminespeedmultiplier", f -> rules.unitMineSpeedMultiplier = f, () -> rules.unitMineSpeedMultiplier);
        number("@rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier, 0f, 50f);
        number("@rules.unitcostmultiplier", f -> rules.unitCostMultiplier = f, () -> rules.unitCostMultiplier);
        number("@rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);

        if(Core.bundle.get("bannedunits").toLowerCase().contains(ruleSearch)){
            current.button("@bannedunits", () -> bannedUnits.show(rules.bannedUnits)).left().width(300f).row();
        }
        check("@bannedunits.whitelist", b -> rules.unitWhitelist = b, () -> rules.unitWhitelist);


        category("enemy");
        check("@rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("@rules.corecapture", b -> rules.coreCapture = b, () -> rules.coreCapture);
        check("@rules.placerangecheck", b -> rules.placeRangeCheck = b, () -> rules.placeRangeCheck);
        check("@rules.polygoncoreprotection", b -> rules.polygonCoreProtection = b, () -> rules.polygonCoreProtection);
        number("@rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200), () -> !rules.polygonCoreProtection);


        category("environment");
        check("@rules.explosions", b -> rules.damageExplosions = b, () -> rules.damageExplosions);
        check("@rules.fire", b -> rules.fire = b, () -> rules.fire);
        check("@rules.fog", b -> rules.fog = b, () -> rules.fog);
        check("@rules.staticFog", b -> rules.staticFog = b, () -> rules.staticFog);
        check("@rules.lighting", b -> rules.lighting = b, () -> rules.lighting);

        check("@rules.limitarea", b -> rules.limitMapArea = b, () -> rules.limitMapArea, () -> !state.isGame());
        numberi("x", x -> rules.limitX = x, () -> rules.limitX, () -> rules.limitMapArea && !state.isGame(), 0, 10000);
        numberi("y", y -> rules.limitY = y, () -> rules.limitY, () -> rules.limitMapArea && !state.isGame(), 0, 10000);
        numberi("w", w -> rules.limitWidth = w, () -> rules.limitWidth, () -> rules.limitMapArea && !state.isGame(), 0, 10000);
        numberi("h", h -> rules.limitHeight = h, () -> rules.limitHeight, () -> rules.limitMapArea && !state.isGame(), 0, 10000);

        current.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.ambientLight));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.ambientlight");
        }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f).row();
        current.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.staticColor));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.staticColor");
        }, () -> ui.picker.show(rules.staticColor, rules.staticColor::set)).left().width(250f).row();
        current.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.dynamicColor));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.dynamicColor");
        }, () -> ui.picker.show(rules.dynamicColor, rules.dynamicColor::set)).left().width(250f).row();
        number("@rules.solarmultiplier", f -> rules.solarMultiplier = f, () -> rules.solarMultiplier);

        if(Core.bundle.get("rules.ambientlight").toLowerCase().contains(ruleSearch)){
            current.button(b -> {
                b.left();
                b.table(Tex.pane, in -> {
                    in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                        update(() -> setColor(rules.ambientLight));
                    }}).grow();
                }).margin(4).size(50f).padRight(10);
                b.add("@rules.ambientlight");
            }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f).row();
        }

        if(Core.bundle.get("rules.weather").toLowerCase().contains(ruleSearch)){
            current.button("@rules.weather", this::weatherDialog).width(250f).left().row();
        }

        title("@rules.title.arcExperimental");
        check("@rules.logicUnitBuild", b -> rules.logicUnitBuild = b, () -> rules.logicUnitBuild);
        check("@rules.coreDestroyClear",b->rules.coreDestroyClear = b,()->rules.coreDestroyClear);
        check("@rules.unitPayloadUpdate",b->rules.unitPayloadUpdate = b,()->rules.unitPayloadUpdate);
        check("@rules.showSpawns",b->rules.showSpawns = b,()->rules.showSpawns);
        check("允许控制单位", b -> rules.possessionAllowed = b, () -> rules.possessionAllowed);
        check("禁用重建", b -> rules.ghostBlocks = b, () -> rules.ghostBlocks);
        //main.button("@hiddenBuildItems", () -> showBanned("@hiddenBuildItems", ContentType.item, rules.hiddenBuildItems, Item::showUnlock)).left().width(300f).row();

        check("@rules.limitarea", b -> rules.limitMapArea = b, () -> rules.limitMapArea);
        numberi("x", x -> state.rules.limitX = x, () -> state.rules.limitX, () -> state.rules.limitMapArea, 0, 10000);
        numberi("y", y -> state.rules.limitY = y, () -> state.rules.limitY, () -> state.rules.limitMapArea, 0, 10000);
        numberi("w", w -> state.rules.limitWidth = w, () -> state.rules.limitWidth, () -> state.rules.limitMapArea, 0, 10000);
        numberi("h", h -> state.rules.limitHeight = h, () -> state.rules.limitHeight, () -> state.rules.limitMapArea, 0, 10000);
        check("@rules.disableOutsideArea",b -> rules.disableOutsideArea = b, () -> rules.disableOutsideArea);

        title("@rules.title.planet");

        category("planet");
        if(Core.bundle.get("rules.title.planet").toLowerCase().contains(ruleSearch)){
            current.table(Tex.button, t -> {
                t.margin(10f);
                var group = new ButtonGroup<>();
                var style = Styles.flatTogglet;

                t.defaults().size(140f, 50f);

            for(Planet planet : content.planets()){
                t.button(planet.localizedName, style, () -> {
                    planet.applyRules(rules);
                }).group(group).checked(b -> rules.planet == planet);

                    if(t.getChildren().size % 3 == 0){
                        t.row();
                    }
                }

                t.button("@rules.anyenv", style, () -> {
                    rules.env = Vars.defaultEnv;
                    rules.planet = Planets.sun;
                }).group(group).checked(b -> rules.planet == Planets.sun);
            }).left().fill(false).expand(false, false).row();
        }


        category("teams");
        //not sure where else to put this
        if(showRuleEditRule){
            check("@rules.allowedit", b -> rules.allowEditRules = b, () -> rules.allowEditRules);
        }
        main.button("所有队伍开启无限火力", () -> {
            for(Team team : Team.all){
                team.rules().cheat = true;
            }
            setup();
        }).width(256f).height(32f).row();
        main.button("所有队伍关闭无限火力", () -> {
            for(Team team : Team.all){
                team.rules().cheat = false;
            }
            setup();
        }).width(256f).height(32f).row();

        team("@rules.playerteam", t -> rules.defaultTeam = t, () -> rules.defaultTeam);
        team("@rules.enemyteam", t -> rules.waveTeam = t, () -> rules.waveTeam);

        main.button("更多队伍设置", Styles.flatBordert, () -> {
            new TeamSelectDialog(team -> {
                if(teams.contains(team)) teams.remove(team);
                else teams.add(team);
                setup();
            }, team -> teams.contains(team), false).show();
        }).marginLeft(14f).fillX().height(55f).row();

        for(Team team : teams){
            boolean[] shown = {false};
            Table wasCurrent = current;

            Table teamRules = new Table(); // just button and collapser in one table
            teamRules.button(team.coloredName(), Icon.downOpen, Styles.togglet, () -> {
                shown[0] = !shown[0];
            }).marginLeft(14f).width(260f).height(55f).update(t -> {
                ((Image)t.getChildren().get(1)).setDrawable(shown[0] ? Icon.upOpen : Icon.downOpen);
                t.setChecked(shown[0]);
            }).left().padBottom(2f).row();

            teamRules.collapser(c -> {
                c.left().defaults().fillX().left().pad(5);
                current = c;
                TeamRule teams = rules.teams.get(team);

                check("@rules.cheat", b -> teams.cheat = b, () -> teams.cheat);
                check("@rules.infiniteAmmo",b -> teams.infiniteAmmo = b, () -> teams.infiniteAmmo);
                check("@rules.aiCoreSpawn", b -> teams.aiCoreSpawn = b, () -> teams.aiCoreSpawn);

                number("@rules.blockhealthmultiplier", f -> teams.blockHealthMultiplier = f, () -> teams.blockHealthMultiplier);
                number("@rules.blockdamagemultiplier", f -> teams.blockDamageMultiplier = f, () -> teams.blockDamageMultiplier);

                check("@rules.rtsai", b -> teams.rtsAi = b, () -> teams.rtsAi, () -> team != rules.defaultTeam);
                numberi("@rules.rtsminsquadsize", f -> teams.rtsMinSquad = f, () -> teams.rtsMinSquad, () -> teams.rtsAi, 0, 100);
                numberi("@rules.rtsmaxsquadsize", f -> teams.rtsMaxSquad = f, () -> teams.rtsMaxSquad, () -> teams.rtsAi, 1, 1000);
                number("@rules.rtsminattackweight", f -> teams.rtsMinWeight = f, () -> teams.rtsMinWeight, () -> teams.rtsAi);

                //disallow on Erekir (this is broken for mods I'm sure, but whatever)
                check("@rules.buildai", b -> teams.buildAi = b, () -> teams.buildAi, () -> team != rules.defaultTeam && rules.env != Planets.erekir.defaultEnv && !rules.pvp);
                number("@rules.buildaitier", false, f -> teams.buildAiTier = f, () -> teams.buildAiTier, () -> teams.buildAi && rules.env != Planets.erekir.defaultEnv && !rules.pvp, 0, 1);

                number("@rules.extracorebuildradius", f -> teams.extraCoreBuildRadius = f * tilesize, () -> Math.min(teams.extraCoreBuildRadius / tilesize, 200), () -> !rules.polygonCoreProtection);

                check("@rules.infiniteresources", b -> teams.infiniteResources = b, () -> teams.infiniteResources);
                check("@rules.fillitems", b -> teams.fillItems = b, () -> teams.fillItems);
                number("@rules.buildspeedmultiplier", f -> teams.buildSpeedMultiplier = f, () -> teams.buildSpeedMultiplier, 0.001f, 50f);

                number("@rules.unitdamagemultiplier", f -> teams.unitDamageMultiplier = f, () -> teams.unitDamageMultiplier);
                number("@rules.unitcrashdamagemultiplier", f -> teams.unitCrashDamageMultiplier = f, () -> teams.unitCrashDamageMultiplier);
                number("@rules.unitminespeedmultiplier", f -> teams.unitMineSpeedMultiplier = f, () -> teams.unitMineSpeedMultiplier);
                number("@rules.unitbuildspeedmultiplier", f -> teams.unitBuildSpeedMultiplier = f, () -> teams.unitBuildSpeedMultiplier, 0.001f, 50f);
                number("@rules.unitcostmultiplier", f -> teams.unitCostMultiplier = f, () -> teams.unitCostMultiplier);
                number("@rules.unithealthmultiplier", f -> teams.unitHealthMultiplier = f, () -> teams.unitHealthMultiplier);

                if(!current.hasChildren()){
                    teamRules.clear();
                }else{
                    wasCurrent.add(teamRules).row();
                }

                current = wasCurrent;
            }, () -> shown[0]).growX().row();
        }


        additionalSetup.each(Runnable::run);

        for(var i = 0; i < categories.size; i++){
            addToMain(categories.get(i), Core.bundle.get("rules.title." + categoryNames.get(i)));
        }

        title("地图背景[lightgray]需要设置空地板");
        check("自定义背景", t -> {
            rules.planetBackground = t ? new PlanetParams(){{planet = Planets.sun;zoom=1f;camPos = new Vec3(1.2388899f, 1.6047299f, 2.4758825f);}} : null;
            setup();
        }, () -> rules.planetBackground != null);
        if (rules.planetBackground != null){
            main.table(Tex.button, t -> {
                t.margin(10f);
                var group = new ButtonGroup<>();
                var style = Styles.flatTogglet;

                t.defaults().size(140f, 50f);

                for(Planet planet : content.planets()){
                    t.button(planet.localizedName, style, () -> rules.planetBackground.planet = planet).group(group).checked(b -> rules.planetBackground.planet == planet);
                    if(t.getChildren().size % 3 == 0){
                        t.row();
                    }
                }
            }).left().fill(false).expand(false, false).row();
            number("放缩", f -> rules.planetBackground.zoom = f, () -> rules.planetBackground.zoom, 0.0001f, 999);
            number("位置x", f -> rules.planetBackground.camPos.x = f, () -> rules.planetBackground.camPos.x);
            number("位置y", f -> rules.planetBackground.camPos.y = f, () -> rules.planetBackground.camPos.y);
            number("位置z", f -> rules.planetBackground.camPos.z = f, () -> rules.planetBackground.camPos.z);
        }
    }

    public void category(String name){
        current = new Table();
        current.left().defaults().fillX().expandX().left().pad(5);
        currentName = name;
        categories.add(current);
        categoryNames.add(currentName);
    }

    void addToMain(Table category, String title){
        if(category.hasChildren()){
            main.add(title).color(Pal.accent).padTop(20).padRight(100f).padBottom(-3).fillX().left().pad(5).row();
            main.image().color(Pal.accent).height(3f).padRight(100f).padBottom(20).fillX().left().pad(5).row();
            main.add(category).row();
        }
    }

    public void team(String text, Cons<Team> cons, Prov<Team> prov){
        if(!Core.bundle.get(text.substring(1)).toLowerCase().contains(ruleSearch)) return;
        current.table(t -> {
            t.left();
            t.add(text).left().padRight(5);

            for(Team team : Team.baseTeams){
                t.button(Tex.whiteui, Styles.squareTogglei, 38f, () -> {
                    cons.get(team);
                }).pad(1f).checked(b -> prov.get() == team).size(60f).tooltip(team.coloredName()).with(i -> i.getStyle().imageUpColor = team.color);
            }
            t.button(Icon.add, Styles.squareTogglei, 38f, () -> {
                new TeamSelectDialog(cons, prov.get()).show();
            }).pad(1f).checked(b -> {
                return !Seq.with(Team.baseTeams).contains(prov.get());
            }).size(60f).tooltip("[acid]更多队伍选择");
        }).padTop(0).row();
    }

    public void number(String text, Floatc cons, Floatp prov){
        number(text, false, cons, prov, () -> true, 0, Float.MAX_VALUE);
    }

    public void number(String text, Floatc cons, Floatp prov, float min, float max){
        number(text, false, cons, prov, () -> true, min, max);
    }

    public void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition){
        number(text, integer, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    public void number(String text, Floatc cons, Floatp prov, Boolp condition){
        number(text, false, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    public void numberi(String text, Intc cons, Intp prov, int min, int max){
        numberi(text, cons, prov, () -> true, min, max);
    }

    public void numberi(String text, Intc cons, Intp prov, Boolp condition, int min, int max){
        if(!Core.bundle.get(text.substring(1)).toLowerCase().contains(ruleSearch)) return;
        var cell = current.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
                .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((prov.get()) + "", s -> cons.get(Strings.parseInt(s)))
                .update(a -> a.setDisabled(!condition.get()))
                .padRight(100f)
                .valid(f -> Strings.parseInt(f) >= -999999 && Strings.parseInt(f) <= 999999999).width(120f).left();
        }).padTop(0);
        ruleInfo(cell, text);
        current.row();
    }

    public void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition, float min, float max){
        if(!Core.bundle.get(text.substring(1)).toLowerCase().contains(ruleSearch)) return;
        var cell = current.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((integer ? (int)prov.get() : prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(50f)
            .update(a -> a.setDisabled(!condition.get()))
            //.valid(f -> Strings.canParsePositiveFloat(f) && Strings.parseFloat(f) >= min && Strings.parseFloat(f) <= max).width(120f).left();
            .valid(f ->  Strings.parseFloat(f) >= -Float.MAX_VALUE && Strings.parseFloat(f) <= Float.MAX_VALUE).width(120f).left();
        }).padTop(0);
        ruleInfo(cell, text);
        current.row();
    }

    public void check(String text, Boolc cons, Boolp prov){
        check(text, cons, prov, () -> true);
    }

    public void check(String text, Boolc cons, Boolp prov, Boolp condition){
        if(!Core.bundle.get(text.substring(1)).toLowerCase().contains(ruleSearch)) return;
        var cell = current.check(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get()));
        cell.get().left();
        ruleInfo(cell, text);
        current.row();
    }

    void title(String text){
        main.add(text).color(ARCVars.getThemeColor()).padTop(20).center().padBottom(-3);
        main.row();
        main.image().color(ARCVars.getThemeColor()).height(3f).padRight(100f).padBottom(20);
        main.row();
    }

    public void ruleInfo(Cell<?> cell, String text){
        if(Core.bundle.has(text.substring(1) + ".info")){
            if(mobile && !graphics.isPortrait()){ //disabled in portrait - broken and goes offscreen
                Table table = new Table();
                table.add(cell.get()).left().expandX().fillX();
                cell.clearElement();
                table.button(Icon.infoSmall, () -> ui.showInfo(text + ".info")).size(32f).right();
                cell.setElement(table).left().expandX().fillX();
            }else{
                cell.tooltip(text + ".info");
            }
        }
    }

    Cell<TextField> field(Table table, float value, Floatc setter){
        return table.field(Strings.autoFixed(value, 2), v -> setter.get(Strings.parseFloat(v)))
            .valid(Strings::canParsePositiveFloat)
            .size(90f, 40f).pad(2f);
    }

    void weatherDialog(){
        BaseDialog dialog = new BaseDialog("@rules.weather");
        Runnable[] rebuild = {null};

        dialog.cont.pane(base -> {

            rebuild[0] = () -> {
                base.clearChildren();
                int cols = Math.max(1, (int)(Core.graphics.getWidth() / Scl.scl(450)));
                int idx = 0;

                for(WeatherEntry entry : rules.weather){
                    base.top();
                    //main container
                    base.table(Tex.pane, c -> {
                        c.margin(0);

                        //icons to perform actions
                        c.table(Tex.whiteui, t -> {
                            t.setColor(Pal.gray);

                            t.top().left();
                            t.add(entry.weather.localizedName).left().padLeft(6);

                            t.add().growX();

                            ImageButtonStyle style = Styles.geni;
                            t.defaults().size(42f);

                            t.button(Icon.cancel, style, () -> {
                                rules.weather.remove(entry);
                                rebuild[0].run();
                            });
                        }).growX();

                        c.row();

                        //all the options
                        c.table(f -> {
                            f.marginLeft(4);
                            f.left().top();

                            f.defaults().padRight(4).left();

                            f.add("@rules.weather.duration");
                            field(f, entry.minDuration / toMinutes, v -> entry.minDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxDuration / toMinutes, v -> entry.maxDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.add("@rules.weather.frequency");
                            field(f, entry.minFrequency / toMinutes, v -> entry.minFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxFrequency / toMinutes, v -> entry.maxFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.check("@rules.weather.always", val -> entry.always = val).checked(cc -> entry.always).padBottom(4);

                            //intensity can't currently be customized

                        }).grow().left().pad(6).top();
                    }).width(410f).pad(3).top().left().fillY();

                    if(++idx % cols == 0){
                        base.row();
                    }
                }
            };

            rebuild[0].run();
        }).grow();

        dialog.addCloseButton();

        dialog.buttons.button("@add", Icon.add, () -> {
            BaseDialog add = new BaseDialog("@add");
            add.cont.pane(t -> {
                t.background(Tex.button);
                int i = 0;
                for(Weather weather : content.<Weather>getBy(ContentType.weather)){
                    if(weather.hidden) continue;

                    t.button(weather.localizedName, Styles.flatt, () -> {
                        rules.weather.add(new WeatherEntry(weather));
                        rebuild[0].run();

                        add.hide();
                    }).size(140f, 50f);
                    if(++i % 2 == 0) t.row();
                }
            });
            add.addCloseButton();
            add.show();
        }).width(170f);

        dialog.show();
    }
}
