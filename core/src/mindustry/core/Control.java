package mindustry.core;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.actions.Actions;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.DrawUtilities.arcBuildEffect;

/**
 * Control module.
 * Handles all input, saving and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener, Loadable{
    public Saves saves;
    public SoundControl sound;
    public InputHandler input;
    public AttackIndicators indicators;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false, backgroundPaused = false;
    private Seq<Building> toBePlaced = new Seq<>(false);

    public static boolean bossKeyPressing, bossKeyValid;
    private BaseDialog calcDialog;
    private final StringBuilder formula = new StringBuilder();
    private String resultString = "";

    public Control(){
        saves = new Saves();
        sound = new SoundControl();
        indicators = new AttackIndicators();

        Events.on(BuildDamageEvent.class, e -> {
            if(e.build.team == Vars.player.team()){
                indicators.add(e.build.tileX(), e.build.tileY());
            }
        });

        //show dialog saying that mod loading was skipped.
        Events.on(ClientLoadEvent.class, e -> {
            if(Vars.mods.skipModLoading() && Vars.mods.list().any()){
                Time.runTask(4f, () -> {
                    ui.showInfo("@mods.initfailed");
                });
            }
            checkAutoUnlocks();
        });

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, platform::updateRPC);
            }
        });

        Events.on(PlayEvent.class, event -> {
            player.team(netServer.assignTeam(player));
            player.add();

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(Mathf.zero(player.x) && Mathf.zero(player.y)){
                Building core = player.bestCore();
                if(core != null){
                    player.set(core);
                    camera.position.set(core);
                }
            }else{
                camera.position.set(player);
            }
        });

        Events.on(SaveLoadEvent.class, event -> {
            input.checkUnit();
        });

        Events.on(ResetEvent.class, event -> {
            player.reset();
            toBePlaced.clear();
            indicators.clear();

            hiscore = false;
            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {
            if(state.map.getHightScore() < state.wave){
                hiscore = true;
                state.map.setHighScore(state.wave);
            }

            Sounds.wave.play();
        });

        Events.on(GameOverEvent.class, event -> {
            state.stats.wavesLasted = state.wave;
            Effect.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.gameOver(event.winner);
        });

        //add player when world loads regardless
        Events.on(WorldLoadEvent.class, e -> {
            player.add();
            //make player admin on any load when hosting
            if(net.active() && net.server()){
                player.admin = true;
            }
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> app.post(() -> {
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.admin = true;
                }catch(IOException e){
                    ui.showException("@server.error", e);
                    state.set(State.menu);
                }
            }
        }));

        Events.on(UnlockEvent.class, e -> {
            if(e.content.showUnlock()){
                ui.hudfrag.showUnlock(e.content);
            }

            checkAutoUnlocks();

            if(e.content instanceof SectorPreset){
                for(TechNode node : TechTree.all){
                    if(!node.content.unlocked() && node.objectives.contains(o -> o instanceof SectorComplete sec && sec.preset == e.content) && !node.objectives.contains(o -> !o.complete())){
                        ui.hudfrag.showToast(new TextureRegionDrawable(node.content.uiIcon), iconLarge, bundle.get("available"));
                    }
                }
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            app.post(this::checkAutoUnlocks);

            if(!net.client() && e.sector.preset != null && e.sector.preset.isLastSector && e.initialCapture){
                Time.run(60f * 2f, () -> {
                    ui.campaignComplete.show(e.sector.planet);
                });
            }
        });

        //delete save on campaign game over
        Events.on(GameOverEvent.class, e -> {
            if(state.isCampaign() && !net.client() && !headless){

                //save gameover sate immediately
                if(saves.getCurrent() != null){
                    saves.getCurrent().save();
                }
            }
        });

        Events.run(Trigger.newGame, () -> {
            var core = player.bestCore();
            if(core == null) return;

            camera.position.set(core);
            player.set(core);

            float coreDelay = 0f;
            if(!settings.getBool("skipcoreanimation") && !state.rules.pvp){
                coreDelay = core.launchDuration();
                //delay player respawn so animation can play.
                player.deathTimer = Player.deathDelay - core.launchDuration();
                //TODO this sounds pretty bad due to conflict
                if(settings.getInt("musicvol") > 0){
                    //TODO what to do if another core with different music is already playing?
                    Music music = core.landMusic();
                    music.stop();
                    music.play();
                    music.setVolume(settings.getInt("musicvol") / 100f);
                }

                renderer.showLanding(core);
            }

            if(state.isCampaign()){
                if(state.rules.sector.info.importRateCache != null){
                    state.rules.sector.info.refreshImportRates(state.rules.sector.planet);
                }

                //don't run when hosting, that doesn't really work.
                if(state.rules.sector.planet.prebuildBase){
                    toBePlaced.clear();
                    float unitsPerTick = 2f;
                    float buildRadius = state.rules.enemyCoreBuildRadius * 1.5f;

                    //TODO if the save is unloaded or map is hosted, these blocks do not get built.
                    boolean anyBuilds = false;
                    for(var build : state.rules.defaultTeam.data().buildings.copy()){
                        if(!(build instanceof CoreBuild) && !build.block.privileged){
                            var ccore = build.closestCore();

                            if(ccore != null){
                                anyBuilds = true;

                                if(!net.active()){
                                    build.pickedUp();
                                    build.tile.remove();

                                    toBePlaced.add(build);

                                    Time.run(build.dst(ccore) / unitsPerTick + coreDelay, () -> {
                                        if(build.tile.build != build){
                                            placeLandBuild(build);

                                            toBePlaced.remove(build);
                                        }
                                    });
                                }else{
                                    //when already hosting, instantly build everything. this looks bad but it's better than a desync
                                    Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
                                    build.block.placeEffect.at(build.x, build.y, build.block.size);
                                    arcBuildEffect(build);
                                }
                            }
                        }
                    }

                    if(anyBuilds){
                        for(var ccore : state.rules.defaultTeam.data().cores){
                            Time.run(coreDelay, () -> {
                                Fx.coreBuildShockwave.at(ccore.x, ccore.y, buildRadius);
                            });
                        }
                    }
                }
            }
        });

        Events.on(SaveWriteEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> {
            state.set(State.playing);
        });
    }

    private void forcePlaceAll(){
        //force set buildings when a save is done or map is hosted, to prevent desyncs
        for(var build : toBePlaced){
            placeLandBuild(build);
        }

        toBePlaced.clear();
    }

    private void placeLandBuild(Building build){
        build.tile.setBlock(build.block, build.team, build.rotation, () -> build);
        build.dropped();

        Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
        build.block.placeEffect.at(build.x, build.y, build.block.size);
        arcBuildEffect(build);
    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").width;

        Core.input.setCatch(KeyCode.back, true);

        Core.settings.defaults(
        "ip", "localhost",
        "color-0", playerColors[8].rgba(),
        "name", "",
        "lastBuild", 0
        );

        createPlayer();

        saves.load();
    }

    /** Automatically unlocks things with no requirements and no locked parents. */
    public void checkAutoUnlocks(){
        if(net.client()) return;

        for(TechNode node : TechTree.all){
            if(!node.content.unlocked() && (node.parent == null || node.parent.content.unlocked()) && node.requirements.length == 0 && !node.objectives.contains(o -> !o.complete())){
                node.content.unlock();
            }
        }
    }

    void createPlayer(){
        player = Player.create();
        player.name = Core.settings.getString("name");

        String locale = Core.settings.getString("locale");
        if(locale.equals("default")){
            locale = Locale.getDefault().toString();
        }
        player.locale = locale;

        player.color.set(Core.settings.getInt("color-0"));

        if(mobile){
            input = new MobileInput();
        }else{
            input = new DesktopInput();
        }

        if(state.isGame()){
            player.add();
        }

        Events.on(ClientLoadEvent.class, e -> input.add());
    }

    public void setInput(InputHandler newInput){
        Block block = input.block;
        boolean added = Core.input.getInputProcessors().contains(input);
        input.remove();
        this.input = newInput;
        newInput.block = block;
        if(added){
            newInput.add();
        }
    }

    public void playMap(Map map, Rules rules){
        playMap(map, rules, false);
    }

    public void playMap(Map map, Rules rules, boolean playtest){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadMap(map, rules);
            state.rules = rules;
            if(playtest) state.playtestingMap = map;
            state.rules.sector = null;
            state.rules.editor = false;
            logic.play();
            if(settings.getBool("savecreate") && !world.isInvalidMap() && !playtest){
                control.saves.addSave(map.name() + " " + new SimpleDateFormat("MMM dd h:mm", Locale.getDefault()).format(new Date()));
            }
            Events.fire(Trigger.newGame);

            //booted out of map, resume editing
            if(world.isInvalidMap() && playtest){
                Dialog current = scene.getDialog();
                ui.editor.resumeAfterPlaytest(map);
                if(current != null){
                    current.update(current::toFront);
                }
            }
        });
    }

    public void playSector(Sector sector){
        playSector(sector, sector);
    }

    public void playSector(@Nullable Sector origin, Sector sector){
        playSector(origin, sector, new WorldReloader());
    }

    void playSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        ui.loadAnd(() -> {
            if(saves.getCurrent() != null && state.isGame()){
                control.saves.getCurrent().save();
                control.saves.resetSave();
            }

            if(sector.preset != null){
                sector.preset.quietUnlock();
            }

            ui.planet.hide();
            SaveSlot slot = sector.save;
            sector.planet.setLastSector(sector);
            if(slot != null && !clearSectors && (!(sector.planet.clearSectorOnLose || sector.info.hasWorldProcessor) || sector.info.hasCore)){

                try{
                    boolean hadNoCore = !sector.info.hasCore;
                    reloader.begin();
                    //pass in a sector context to make absolutely sure the correct sector is written; it may differ from what's in the meta due to remapping.
                    slot.load(world.makeSectorContext(sector));
                    slot.setAutosave(true);
                    state.rules.sector = sector;
                    state.rules.cloudColor = sector.planet.landCloudColor;

                    //if there is no base, simulate a new game and place the right loadout at the spawn position
                    if(state.rules.defaultTeam.cores().isEmpty() || hadNoCore){

                        if(sector.planet.clearSectorOnLose || sector.info.hasWorldProcessor){
                            playNewSector(origin, sector, reloader);
                        }else{
                            //no spawn set -> delete the sector save
                            if(sector.info.spawnPosition == 0){
                                //delete old save
                                sector.save = null;
                                slot.delete();
                                //play again
                                playSector(origin, sector, reloader);
                                return;
                            }

                            //set spawn for sector damage to use
                            Tile spawn = world.tile(sector.info.spawnPosition);
                            spawn.setBlock(sector.planet.defaultCore, state.rules.defaultTeam);

                            //add extra damage.
                            SectorDamage.apply(1f);

                            //reset wave so things are more fair
                            state.wave = 1;
                            //set up default wave time
                            state.wavetime = state.rules.initialWaveSpacing <= 0f ? (state.rules.waveSpacing * (sector.preset == null ? 2f : sector.preset.startWaveTimeMultiplier)) : state.rules.initialWaveSpacing;
                            state.wavetime *= sector.planet.campaignRules.difficulty.waveTimeMultiplier;
                            //reset captured state
                            sector.info.wasCaptured = false;

                            if(state.rules.sector.planet.allowWaves){
                                //re-enable waves
                                state.rules.waves = true;
                                //reset win wave??
                                state.rules.winWave = state.rules.attackMode ? -1 : sector.preset != null && sector.preset.captureWave > 0 ? sector.preset.captureWave : state.rules.winWave > state.wave ? state.rules.winWave : 30;
                            }

                            //if there's still an enemy base left, fix it
                            if(state.rules.attackMode){
                                //replace all broken blocks
                                for(var plan : state.rules.waveTeam.data().plans){
                                    Tile tile = world.tile(plan.x, plan.y);
                                    if(tile != null){
                                        tile.setBlock(plan.block, state.rules.waveTeam, plan.rotation);
                                        if(plan.config != null && tile.build != null){
                                            tile.build.configureAny(plan.config);
                                        }
                                    }
                                }
                                state.rules.waveTeam.data().plans.clear();
                            }

                            //kill all units, since they should be dead anyway
                            Groups.unit.clear();
                            Groups.fire.clear();
                            Groups.puddle.clear();

                            //reset to 0, so replaced cores don't count
                            state.rules.defaultTeam.data().unitCap = 0;
                            Schematics.placeLaunchLoadout(spawn.x, spawn.y);

                            //set up camera/player locations
                            player.set(spawn.x * tilesize, spawn.y * tilesize);
                            camera.position.set(player);

                            Events.fire(new SectorLaunchEvent(sector));
                            Events.fire(Trigger.newGame);

                            state.set(State.playing);
                            reloader.end();
                        }
                    }else{
                        state.set(State.playing);
                        reloader.end();
                    }

                }catch(SaveException e){
                    Log.err(e);
                    sector.save = null;
                    Time.runTask(10f, () -> ui.showErrorMessage("@save.corrupted"));
                    slot.delete();
                    playSector(origin, sector);
                }
                ui.planet.hide();
            }else{
                playNewSector(origin, sector, reloader);
            }
        });
    }

    public void playNewSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        reloader.begin();
        world.loadSector(sector);
        state.rules.sector = sector;
        //assign origin when launching
        sector.info.origin = origin;
        sector.info.destination = origin;
        logic.play();
        control.saves.saveSector(sector);
        Events.fire(new SectorLaunchEvent(sector));
        Events.fire(Trigger.newGame);
        reloader.end();
        state.set(State.playing);
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        //try to save when exiting
        if(saves != null && saves.getCurrent() != null && saves.getCurrent().isAutosave() && !net.client() && !state.isMenu() && !state.gameOver){
            try{
                SaveIO.save(control.saves.getCurrent().file);
                settings.forceSave();
                Log.info("Saved on exit.");
            }catch(Throwable t){
                Log.err(t);
            }
        }

        for(Music music : assets.getAll(Music.class, new Seq<>())){
            music.stop();
        }

        net.dispose();
    }

    @Override
    public void pause(){
        if(settings.getBool("backgroundpause", true) && !net.active()){
            backgroundPaused = true;
            wasPaused = state.is(State.paused);
            if(state.is(State.playing)) state.set(State.paused);
        }
    }

    @Override
    public void resume(){
        if(state.is(State.paused) && !wasPaused && settings.getBool("backgroundpause", true) && !net.active()){
            state.set(State.playing);
        }
        backgroundPaused = false;
    }

    @Override
    public void init(){
        platform.updateRPC();

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            Core.app.post(() -> Core.app.post(() -> {
                BaseDialog dialog = new BaseDialog("@confirm");
                dialog.setFillParent(true);

                float[] countdown = {60 * 31};
                Runnable exit = () -> {
                    Core.settings.put("uiscale", 100);
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                    Core.app.exit();
                };

                dialog.cont.label(() -> {
                    if(countdown[0] <= 0){
                        exit.run();
                    }
                    return Core.bundle.format("uiscale.reset", (int)((countdown[0] -= Time.delta) / 60f));
                }).pad(10f).expand().center();

                dialog.buttons.defaults().size(200f, 60f);
                dialog.buttons.button("@uiscale.cancel", exit);

                dialog.buttons.button("@ok", () -> {
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                });

                dialog.show();
            }));
        }

        bossKeyPressing = false;
        if (!app.isMobile()) Events.on(EventType.ClientLoadEvent.class, e -> initCalc());
    }

    private void initCalc() {
        calcDialog = new BaseDialog("");
        calcDialog.setBackground(Styles.black);
        calcDialog.shown(this::buildCalc);
        calcDialog.resized(this::buildCalc);
        calcDialog.update(() -> calcDialog.toFront());
    }

    private void buildCalc() {
        float width = Core.graphics.getWidth() * 0.9f;
        calcDialog.cont.clear();
        calcDialog.cont.label(() -> formula.toString()).width(width).get().setAlignment(Align.right);
        calcDialog.cont.row();
        calcDialog.cont.label(() -> resultString).width(width).get().setAlignment(Align.right);
        calcDialog.cont.row();
        HashMap<String, Runnable> map = new HashMap<>();
        map.put("AC", () -> {
            formula.setLength(0);
            resultString = "";
        });
        map.put("<-", () -> {
            if (formula.length() == 0) return;
            formula.deleteCharAt(formula.length() - 1);
        });
        String[][] buttons = new String[][]{
                {
                    "(", ")", "AC", "<-"
                },
                {
                    "7", "8", "9", "-"
                },
                {
                    "4", "5", "6", "+"
                },
                {
                    "1", "2", "3", "*"
                },
                {
                    "0", ".", "e", "/"
                },
                {
                    "&", "^", "|", "%"
                }
        };
        calcDialog.cont.table(t -> {
            for (String[] arr : buttons) {
                for (String button : arr) {
                    Runnable replace = map.get(button);
                    if (replace == null) {
                        t.button(button, () -> formula.append(button)).width(width / 4);
                    } else {
                        t.button(button, replace).width(width / 4);
                    }
                }
                t.row();
            }
        });
        calcDialog.cont.row();
        calcDialog.cont.button("计算", () -> resultString = Vars.mods.getScripts().runConsole(formula.toString())).width(width);
    }

    public void loadIcon(String path){
        mods.getScripts().runConsole("Vars.mods.getScripts().runConsole(" +
                "\"{let p=new Pixmap(Core.files.get(\\\"" + path + "\\\",Files.FileType.internal));" +
                "let s=Packages.arc.backend.sdl.jni.SDL.SDL_CreateRGBSurfaceFrom(p.pixels,p.width,p.height);" +
                "Packages.arc.backend.sdl.jni.SDL.SDL_SetWindowIcon(Core.app.window,s);" +
                "Packages.arc.backend.sdl.jni.SDL.SDL_FreeSurface(s);" +
                "p.dispose()}\"" +
                ")");//几把java 几把anuke
    }

    @Override
    public void update(){
        //this happens on Android and nobody knows why
        if(assets == null) return;

        saves.update();

        //update and load any requested assets
        try{
            assets.update();
        }catch(Exception ignored){
        }

        input.updateState();

        sound.update();

        if(Core.input.keyTap(Binding.fullscreen)){
            boolean full = settings.getBool("fullscreen");
            if(full){
                graphics.setWindowedMode(graphics.getWidth(), graphics.getHeight());
            }else{
                graphics.setFullscreen();
            }
            settings.put("fullscreen", !full);
        }

        if(Float.isNaN(Vars.player.x) || Float.isNaN(Vars.player.y)){
            player.set(0, 0);
            if(!player.dead()) player.unit().kill();
        }
        if(Float.isNaN(camera.position.x)) camera.position.x = world.unitWidth()/2f;
        if(Float.isNaN(camera.position.y)) camera.position.y = world.unitHeight()/2f;

        if (calcDialog != null && bossKeyValid && Vars.clientLoaded && Core.input.keyTap(Binding.bossKey)) {
            if (Core.input.keyDown(KeyCode.controlLeft)) {
                if (!bossKeyPressing) return;
                calcDialog.hide();
                loadIcon("icons/arc.png");
                bossKeyPressing = false;
            } else {
                if (bossKeyPressing) return;
                bossKeyPressing = true;
                loadIcon("icons/calc.png");
                settings.put("musicvol", 0);
                settings.put("sfxvol", 0);
                settings.put("ambientvol", 0);
                arcui.MusicDialog.vol = 0;
                arcui.MusicDialog.player.setVolume(0);
                arcui.MusicDialog.sounds.setVolume(0);
                calcDialog.show(scene, Actions.alpha(1));
                mods.getScripts().runConsole(
                        """
                            Packages.arc.backend.sdl.jni.SDL.SDL_SetWindowFullscreen(Core.app.window,0);
                            Packages.arc.backend.sdl.jni.SDL.SDL_SetWindowSize(Core.app.window,220,430);
                            Timer.schedule(()=>Packages.arc.backend.sdl.jni.SDL.SDL_MinimizeWindow(Core.app.window),0.15)
                            """
                );
            }
        }

        if(state.isGame()){
            input.update();
            if(!state.isPaused()){
                indicators.update();
            }

            //auto-update rpc every 5 seconds
            if(timer.get(0, 60 * 5)){
                platform.updateRPC();
            }

            //unlock core items
            var core = state.rules.defaultTeam.core();
            if(!net.client() && core != null && state.isCampaign()){
                core.items.each((i, a) -> i.unlock());
            }

            if(backgroundPaused && settings.getBool("backgroundpause") && !net.active()){
                state.set(State.paused);
            }

            //cannot launch while paused
            if(state.isPaused() && renderer.isCutscene()){
                state.set(State.playing);
            }

            if(!net.client() && Core.input.keyTap(Binding.pause) && !renderer.isCutscene() && !scene.hasDialog() && !scene.hasKeyboard() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.isPaused() ? State.playing : State.paused);
            }

            if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown() && !ui.minimapfrag.shown()){
                if(ui.chatfrag.shown()){
                    ui.chatfrag.hide();
                }else if(!ui.paused.isShown() && !scene.hasDialog()){
                    ui.paused.show();
                    if(!net.active()){
                        state.set(State.paused);
                    }
                }
            }

            if(!mobile && Core.input.keyTap(Binding.screenshot) && !scene.hasField() && !scene.hasKeyboard()){
                renderer.takeMapScreenshot();
            }

        }else{
            //this runs in the menu
            if(!state.isPaused()){
                Time.update();
            }

            if(!scene.hasDialog() && !scene.root.getChildren().isEmpty() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.back)){
                platform.hide();
            }
        }
    }
}
