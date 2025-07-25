package mindustry.server;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Timer;
import arc.util.CommandHandler.*;
import arc.util.Timer.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.arcModule.TimeControl;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.maps.Maps.*;
import mindustry.mod.Mods.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.net.*;
import mindustry.type.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static arc.util.ColorCodes.*;
import static arc.util.Log.*;
import static mindustry.Vars.*;

public class ServerControl implements ApplicationListener{
    protected static String[] tags = {"&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", ""};
    protected static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"),
        autosaveDate = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH-mm-ss");

    /** Global instance of ServerControl, initialized when the server is created. Should never be null on a dedicated server. */
    public static ServerControl instance;

    public final CommandHandler handler = new CommandHandler("");
    public final Fi logFolder = Core.settings.getDataDirectory().child("logs/");

    private final Interval autosaveCount = new Interval();

    public Runnable serverInput = () -> {
        Scanner scan = new Scanner(System.in);
        while(scan.hasNext()){
            String line = scan.nextLine();
            Core.app.post(() -> handleCommandString(line));
        }
    };

    /** The file to which the logs are currently being written. */
    public Fi currentLogFile;

    /** Whether the server is currently waiting for the next map to be loaded. */
    public boolean inGameOverWait;

    /** The last gamemode loaded on this server. */
    public Gamemode lastMode;

    private Task lastTask;
    private Thread socketThread;
    private ServerSocket serverSocket;
    private PrintWriter socketOutput;
    private String suggested;
    private boolean autoPaused = false;

    public Cons<GameOverEvent> gameOverListener = event -> {
        if(state.rules.waves){
            info("Game over! Reached wave @ with @ players online on map @.", state.wave, Groups.player.size(), Strings.capitalize(state.map.plainName()));
        }else{
            info("Game over! Team @ is victorious with @ players online on map @.", event.winner.name, Groups.player.size(), Strings.capitalize(state.map.plainName()));
        }

        //set the next map to be played
        Map map = maps.getNextMap(lastMode, state.map);
        if(map != null){
            Call.infoMessage((state.rules.pvp
                    ? "[accent]The " + event.winner.coloredName() + " team is victorious![]\n" : "[scarlet]Game over![]\n")
                    + "\nNext selected map: [accent]" + map.name() + "[white]"
                    + (map.hasTag("author") ? " by[accent] " + map.author() + "[white]" : "") + "." +
                    "\nNew game begins in " + Config.roundExtraTime.num() + " seconds.");

            state.gameOver = true;
            Call.updateGameOver(event.winner);

            info("Selected next map to be @.", map.plainName());

            play(() -> world.loadMap(map, map.applyRules(lastMode)));
        }else{
            netServer.kickAll(KickReason.gameover);
            state.set(State.menu);
            net.closeServer();
        }
    };

    public ServerControl(String[] args){
        setup(args);
        instance = this;
    }

    protected void setup(String[] args){
        Core.settings.defaults(
            "bans", "",
            "admins", "",
            "shufflemode", "custom",
            "globalrules", "{reactorExplosions: false, logicUnitBuild: false}"
        );

        //update log level
        Config.debug.set(Config.debug.bool());

        try{
            lastMode = Gamemode.valueOf(Core.settings.getString("lastServerMode", "survival"));
        }catch(Exception e){ //handle enum parse exception
            lastMode = Gamemode.survival;
        }

        logger = (level1, text) -> {
            //err has red text instead of reset.
            if(level1 == LogLevel.err) text = text.replace(reset, lightRed + bold);

            String result = bold + lightBlack + "[" + dateTime.format(LocalDateTime.now()) + "] " + reset + format(tags[level1.ordinal()] + " " + text + "&fr");
            System.out.println(result);

            if(Config.logging.bool()){
                logToFile("[" + dateTime.format(LocalDateTime.now()) + "] " + formatColors(tags[level1.ordinal()] + " " + text + "&fr", false));
            }

            if(socketOutput != null){
                try{
                    socketOutput.println(formatColors(text + "&fr", false));
                }catch(Throwable e1){
                    err("Error occurred logging to socket: @", e1.getClass().getSimpleName());
                }
            }
        };

        formatter = (text, useColors, arg) -> {
            text = Strings.format(text.replace("@", "&fb&lb@&fr"), arg);
            return useColors ? addColors(text) : removeColors(text);
        };

        Time.setDeltaProvider(TimeControl.deltaProvider);
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, maxDeltaServer));

        registerCommands();

        Core.app.post(() -> {
            //try to load auto-update save if possible
            if(Config.autoUpdate.bool()){
                Fi fi = saveDirectory.child("autosavebe." + saveExtension);
                if(fi.exists()){
                    try{
                        SaveIO.load(fi);
                        info("Auto-save loaded.");
                        state.set(State.playing);
                        netServer.openServer();
                    }catch(Throwable e){
                        err(e);
                    }
                }
            }

            Seq<String> commands = new Seq<>();

            if(args.length > 0){
                commands.addAll(Strings.join(" ", args).split(","));
                info("Found @ command-line arguments to parse.", commands.size);
            }

            if(!Config.startCommands.string().isEmpty()){
                String[] startup = Strings.join(" ", Config.startCommands.string()).split(",");
                info("Found @ startup commands.", startup.length);
                commands.addAll(startup);
            }

            for(String s : commands){
                CommandResponse response = handler.handleMessage(s);
                if(response.type != ResponseType.valid){
                    err("Invalid command argument sent: '@': @", s, response.type.name());
                    err("Argument usage: &lb<command-1> <command1-args...>,<command-2> <command-2-args2...>");
                }
            }
        });

        customMapDirectory.mkdirs();

        if(Version.build == -1){
            warn("&lyYour server is running a custom build, which means that client checking is disabled.");
            warn("&lyIt is highly advised to specify which version you're using by building with gradle args &lb&fb-Pbuildversion=&lr<build>");
        }

        //set up default shuffle mode
        try{
            maps.setShuffleMode(ShuffleMode.valueOf(Core.settings.getString("shufflemode")));
        }catch(Exception e){
            maps.setShuffleMode(ShuffleMode.all);
        }

        Events.on(GameOverEvent.class, event -> {
            if(!inGameOverWait && gameOverListener != null){
                gameOverListener.get(event);
            }
        });

        //reset autosave on world load
        Events.on(WorldLoadEvent.class, e -> {
            autosaveCount.reset(0, Config.autosaveSpacing.num() * 60);
        });

        //autosave periodically
        Events.run(Trigger.update, () -> {
            if(state.isPlaying() && Config.autosave.bool()){
                if(autosaveCount.get(Config.autosaveSpacing.num() * 60)){
                    int max = Config.autosaveAmount.num();

                    //use map file name to make sure it can be saved
                    String mapName = (state.map.file == null ? "unknown" : state.map.file.nameWithoutExtension()).replace(" ", "_");
                    String date = autosaveDate.format(LocalDateTime.now());

                    Seq<Fi> autosaves = saveDirectory.findAll(f -> f.name().startsWith("auto_"));
                    autosaves.sort(f -> -f.lastModified());

                    //delete older saves
                    if(autosaves.size >= max){
                        for(int i = max - 1; i < autosaves.size; i++){
                            autosaves.get(i).delete();
                        }
                    }

                    String fileName = "auto_" + mapName + "_" + date + "." + saveExtension;
                    Fi file = saveDirectory.child(fileName);
                    info("Autosaving...");

                    try{
                        SaveIO.save(file);
                        info("Autosave completed.");
                    }catch(Throwable e){
                        err("Autosave failed.", e);
                    }
                }
            }

            if(state.isGame()){ //run this only if the server's actually hosting
                if(Config.autoPause.bool()){
                    if(Groups.player.isEmpty()){
                        autoPaused = true;
                        state.set(State.paused);
                    }else if(autoPaused){
                        autoPaused = false;
                        state.set(State.playing);
                    }
                }else if(autoPaused && Vars.state.isPaused()){ //unpause when the config is disabled
                    state.set(State.playing);
                    autoPaused = false;
                }
            }
        });

        Events.run(Trigger.socketConfigChanged, () -> {
            toggleSocket(false);
            toggleSocket(Config.socketInput.bool());
        });

        Events.on(ResetEvent.class, e -> {
            autoPaused = false;
        });

        Events.on(PlayEvent.class, e -> {
            try{
                JsonValue value = JsonIO.json.fromJson(null, Core.settings.getString("globalrules"));
                JsonIO.json.readFields(state.rules, value);
            }catch(Throwable t){
                err("Error applying custom rules, proceeding without them.", t);
            }
        });

        //autosave settings once a minute
        float saveInterval = 60;
        Timer.schedule(() -> {
            netServer.admins.forceSave();
            Core.settings.forceSave();
        }, saveInterval, saveInterval);

        if(!mods.orderedMods().isEmpty()){
            info("@ mods loaded.", mods.orderedMods().size);
        }

        int unsupported = mods.list().count(l -> !l.enabled());

        if(unsupported > 0){
            Log.err("There were errors loading @ mod(s):", unsupported);
            for(LoadedMod mod : mods.list().select(l -> !l.enabled())){
                Log.err("- @ &ly(" + mod.state + ")", mod.meta.name);
            }
        }

        toggleSocket(Config.socketInput.bool());

        Events.on(ServerLoadEvent.class, e -> {
            if(serverInput != null){
                Thread thread = new Thread(serverInput, "Server Controls");
                thread.setDaemon(true);
                thread.start();
            }

            info("Server loaded. Type @ for help.", "'help'");
        });
    }

    protected void registerCommands(){
        handler.register("help", "[command]", "Display the command list, or get help for a specific command.", arg -> {
            if(arg.length > 0){
                Command command = handler.getCommandList().find(c -> c.text.equalsIgnoreCase(arg[0]));
                if(command == null){
                    err("Command " + arg[0] + " not found!");
                }else{
                    info(command.text + ":");
                    info("  &b&lb " + command.text + (command.paramText.isEmpty() ? "" : " &lc&fi") + command.paramText + "&fr - &lw" + command.description);
                }
            }else{
                info("Commands:");
                for(Command command : handler.getCommandList()){
                    info("  &b&lb " + command.text + (command.paramText.isEmpty() ? "" : " &lc&fi") + command.paramText + "&fr - &lw" + command.description);
                }
            }
        });

        handler.register("version", "Displays server version info.", arg -> {
            info("Version: Mindustry @-@ @ / build @", Version.number, Version.modifier, Version.type, Version.build + (Version.revision == 0 ? "" : "." + Version.revision));
            info("Java Version: @", OS.javaVersion);
        });

        handler.register("exit", "Exit the server application.", arg -> {
            info("Shutting down server.");
            net.dispose();
            Core.app.exit();
        });

        handler.register("stop", "Stop hosting the server.", arg -> {
            net.closeServer();
            cancelPlayTask();
            state.set(State.menu);
            info("Stopped server.");
        });

        handler.register("host", "[mapname] [mode]", "Open the server. Will default to survival and a random map if not specified.", arg -> {
            if(state.isGame()){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            cancelPlayTask();

            Gamemode preset = Gamemode.survival;

            if(arg.length > 1){
                try{
                    preset = Gamemode.valueOf(arg[1]);
                }catch(IllegalArgumentException e){
                    err("No gamemode '@' found.", arg[1]);
                    return;
                }
            }

            Map result;
            if(arg.length > 0){
                result = maps.all().find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));

                if(result == null){
                    err("No map with name '@' found.", arg[0]);
                    return;
                }
            }else{
                result = maps.getShuffleMode().next(preset, state.map);
                if(result != null){
                    info("Randomized next map to be @.", result.plainName());
                }
            }

            info("Loading map...");

            logic.reset();
            if(result != null){
                lastMode = preset;
                Core.settings.put("lastServerMode", lastMode.name());
                try{
                    world.loadMap(result, result.applyRules(lastMode));
                    state.rules = result.applyRules(preset);
                    logic.play();

                    info("Map loaded.");

                    netServer.openServer();
                }catch(MapException e){
                    err("@: @", e.map.plainName(), e.getMessage());
                }
            }
        });

        handler.register("maps", "[all/custom/default]", "Display available maps. Displays only custom maps by default.", arg -> {
            boolean custom = arg.length == 0 || arg[0].equals("custom") || arg[0].equals("all");
            boolean def = arg.length > 0 && (arg[0].equals("default") || arg[0].equals("all"));

            if(!maps.all().isEmpty()){
                Seq<Map> all = new Seq<>();

                if(custom) all.addAll(maps.customMaps());
                if(def) all.addAll(maps.defaultMaps());

                if(all.isEmpty()){
                    info("No custom maps loaded. &fiTo display built-in maps, use the \"@\" argument.", "all");
                }else{
                    info("Maps:");

                    for(Map map : all){
                        String mapName = map.plainName().replace(' ', '_');
                        if(map.custom){
                            info("  @ (@): &fiCustom / @x@", mapName, map.file.name(), map.width, map.height);
                        }else{
                            info("  @: &fiDefault / @x@", mapName, map.width, map.height);
                        }
                    }
                }
            }else{
                info("No maps found.");
            }
            info("Map directory: &fi@", customMapDirectory.file().getAbsoluteFile().toString());
        });

        handler.register("reloadmaps", "Reload all maps from disk.", arg -> {
            int beforeMaps = maps.all().size;
            maps.reload();
            if(maps.all().size > beforeMaps){
                info("@ new map(s) found and reloaded.", maps.all().size - beforeMaps);
            }else if(maps.all().size < beforeMaps){
                info("@ old map(s) deleted.", beforeMaps - maps.all().size);
            }else{
                info("Maps reloaded.");
            }
        });

        handler.register("status", "Display server status.", arg -> {
            if(state.isMenu()){
                info("Status: &rserver closed");
            }else{
                info("Status:");
                info("  Playing on map &fi@ / Wave @", Strings.capitalize(state.map.plainName()), state.wave);

                if(state.rules.waves){
                    info("  @ seconds until next wave.", (int)(state.wavetime / 60));
                }
                info("  @ units / @ enemies", Groups.unit.size(), state.enemies);

                info("  @ FPS, @ MB used.", Core.graphics.getFramesPerSecond(), Core.app.getJavaHeap() / 1024 / 1024);

                if(Groups.player.size() > 0){
                    info("  Players: @", Groups.player.size());
                    for(Player p : Groups.player){
                        info("    @ @ / @", p.admin() ? "&r[A]&c" : "&b[P]&c", p.plainName(), p.uuid());
                    }
                }else{
                    info("  No players connected.");
                }
            }
        });

        handler.register("mods", "Display all loaded mods.", arg -> {
            if(!mods.list().isEmpty()){
                info("Mods:");
                for(LoadedMod mod : mods.list()){
                    info("  @ &fi@ " + (mod.enabled() ? "" : " &lr(" + mod.state + ")"), mod.meta.displayName, mod.meta.version);
                }
            }else{
                info("No mods found.");
            }
            info("Mod directory: &fi@", modDirectory.file().getAbsoluteFile().toString());
        });

        handler.register("mod", "<name...>", "Display information about a loaded plugin.", arg -> {
            LoadedMod mod = mods.list().find(p -> p.meta.name.equalsIgnoreCase(arg[0]));
            if(mod != null){
                info("Name: @", mod.meta.displayName);
                info("Internal Name: @", mod.name);
                info("Version: @", mod.meta.version);
                info("Author: @", mod.meta.author);
                info("Path: @", mod.file.path());
                info("Description: @", mod.meta.description);
            }else{
                info("No mod with name '@' found.", arg[0]);
            }
        });

        handler.register("js", "<script...>", "Run arbitrary Javascript.", arg -> {
            info("&fi&lw&fb" + mods.getScripts().runConsole(arg[0]));
        });

        handler.register("say", "<message...>", "Send a message to all players.", arg -> {
            if(!state.isGame()){
                err("Not hosting. Host a game first.");
                return;
            }

            Call.sendMessage("[scarlet][[Server]:[] " + arg[0]);

            info("&fi&lcServer: &fr@", "&lw" + arg[0]);
        });

        handler.register("pause", "<on/off>", "Pause or unpause the game.", arg -> {
            if(state.isMenu()){
                err("Cannot pause without a game running.");
                return;
            }
            boolean pause = arg[0].equals("on");
            autoPaused = false;
            state.set(pause ? State.paused : State.playing);
            info(pause ? "Game paused." : "Game unpaused.");
        });

        handler.register("rules", "[remove/add] [name] [value...]", "List, remove or add global rules. These will apply regardless of map.", arg -> {
            String rules = Core.settings.getString("globalrules");
            JsonValue base = JsonIO.json.fromJson(null, rules);

            if(arg.length == 0){
                info("Rules:\n@", JsonIO.print(rules));
            }else if(arg.length == 1){
                err("Invalid usage. Specify which rule to remove or add.");
            }else{
                if(!(arg[0].equals("remove") || arg[0].equals("add"))){
                    err("Invalid usage. Either add or remove rules.");
                    return;
                }

                boolean remove = arg[0].equals("remove");
                if(remove){
                    if(base.has(arg[1])){
                        info("Rule '@' removed.", arg[1]);
                        base.remove(arg[1]);
                    }else{
                        err("Rule not defined, so not removed.");
                        return;
                    }
                }else{
                    if(arg.length < 3){
                        err("Missing last argument. Specify which value to set the rule to.");
                        return;
                    }

                    try{
                        JsonValue value = new JsonReader().parse(arg[2]);
                        value.name = arg[1];

                        JsonValue parent = new JsonValue(ValueType.object);
                        parent.addChild(value);

                        JsonIO.json.readField(state.rules, value.name, parent);
                        if(base.has(value.name)){
                            base.remove(value.name);
                        }
                        base.addChild(arg[1], value);
                        info("Changed rule: @", value.toString().replace("\n", " "));
                    }catch(Throwable e){
                        err("Error parsing rule JSON: @", e.getMessage());
                    }
                }

                Core.settings.put("globalrules", base.toString());
                Call.setRules(state.rules);
            }
        });

        handler.register("fillitems", "[team]", "Fill the core with items.", arg -> {
            if(!state.isGame()){
                err("Not playing. Host first.");
                return;
            }

            Team team = arg.length == 0 ? Team.sharded : Structs.find(Team.all, t -> t.name.equals(arg[0]));

            if(team == null){
                err("No team with that name found.");
                return;
            }

            if(state.teams.cores(team).isEmpty()){
                err("That team has no cores.");
                return;
            }

            for(Item item : content.items()){
                state.teams.cores(team).first().items.set(item, state.teams.cores(team).first().storageCapacity);
            }

            info("Core filled.");
        });

        handler.register("playerlimit", "[off/somenumber]", "Set the server player limit.", arg -> {
            if(arg.length == 0){
                info("Player limit is currently @.", netServer.admins.getPlayerLimit() == 0 ? "off" : netServer.admins.getPlayerLimit());
                return;
            }
            if(arg[0].equals("off")){
                netServer.admins.setPlayerLimit(0);
                info("Player limit disabled.");
                return;
            }

            if(Strings.canParsePositiveInt(arg[0]) && Strings.parseInt(arg[0]) > 0){
                int lim = Strings.parseInt(arg[0]);
                netServer.admins.setPlayerLimit(lim);
                info("Player limit is now &lc@.", lim);
            }else{
                err("Limit must be a number above 0.");
            }
        });

        handler.register("config", "[name] [value...]", "Configure server settings.", arg -> {
            if(arg.length == 0){
                info("All config values:");
                for(Config c : Config.all){
                    info("&lk| @: @", c.name, "&lc&fi" + c.get());
                    info("&lk| | &lw" + c.description);
                    info("&lk|");
                }
                return;
            }

            Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(arg[0]));

            if(c != null){
                if(arg.length == 1){
                    info("'@' is currently @.", c.name, c.get());
                }else{
                    if(arg[1].equals("default")){
                        c.set(c.defaultValue);
                    }else if(c.isBool()){
                        c.set(arg[1].equals("on") || arg[1].equals("true"));
                    }else if(c.isNum()){
                        try{
                            c.set(Integer.parseInt(arg[1]));
                        }catch(NumberFormatException e){
                            err("Not a valid number: @", arg[1]);
                            return;
                        }
                    }else if(c.isString()){
                        c.set(arg[1].replace("\\n", "\n"));
                    }

                    info("@ set to @.", c.name, c.get());
                    Core.settings.forceSave();
                }
            }else{
                err("Unknown config: '@'. Run the command with no arguments to get a list of valid configs.", arg[0]);
            }
        });

        handler.register("subnet-ban", "[add/remove] [address]", "Ban a subnet. This simply rejects all connections with IPs starting with some string.", arg -> {
            if(arg.length == 0){
                info("Subnets banned: @", netServer.admins.getSubnetBans().isEmpty() ? "<none>" : "");
                for(String subnet : netServer.admins.getSubnetBans()){
                    info("&lw\t" + subnet);
                }
            }else if(arg.length == 1){
                err("You must provide a subnet to add or remove.");
            }else{
                if(arg[0].equals("add")){
                    if(netServer.admins.getSubnetBans().contains(arg[1])){
                        err("That subnet is already banned.");
                        return;
                    }

                    netServer.admins.addSubnetBan(arg[1]);
                    info("Banned @**", arg[1]);
                }else if(arg[0].equals("remove")){
                    if(!netServer.admins.getSubnetBans().contains(arg[1])){
                        err("That subnet isn't banned.");
                        return;
                    }

                    netServer.admins.removeSubnetBan(arg[1]);
                    info("Unbanned @**", arg[1]);
                }else{
                    err("Incorrect usage. Provide add/remove as the second argument.");
                }
            }
        });

        handler.register("whitelist", "[add/remove] [ID]", "Add/remove players from the whitelist using their ID.", arg -> {
            if(arg.length == 0){
                Seq<PlayerInfo> whitelist = netServer.admins.getWhitelisted();

                if(whitelist.isEmpty()){
                    info("No whitelisted players found.");
                }else{
                    info("Whitelist:");
                    whitelist.each(p -> info("- Name: @ / UUID: @", p.plainLastName(), p.id));
                }
            }else{
                if(arg.length == 2){
                    PlayerInfo info = netServer.admins.getInfoOptional(arg[1]);

                    if(info == null){
                        err("Player ID not found. You must use the ID displayed when a player joins a server.");
                    }else{
                        if(arg[0].equals("add")){
                            netServer.admins.whitelist(arg[1]);
                            info("Player '@' has been whitelisted.", info.plainLastName());
                        }else if(arg[0].equals("remove")){
                            netServer.admins.unwhitelist(arg[1]);
                            info("Player '@' has been un-whitelisted.", info.plainLastName());
                        }else{
                            err("Incorrect usage. Provide add/remove as the second argument.");
                        }
                    }
                }else{
                    err("Incorrect usage. Provide an ID to add or remove.");
                }
            }
        });

        //TODO should be a config, not a separate command.
        handler.register("shuffle", "[none/all/custom/builtin]", "Set map shuffling mode.", arg -> {
            if(arg.length == 0){
                info("Shuffle mode current set to '@'.", maps.getShuffleMode());
            }else{
                try{
                    ShuffleMode mode = ShuffleMode.valueOf(arg[0]);
                    Core.settings.put("shufflemode", mode.name());
                    maps.setShuffleMode(mode);
                    info("Shuffle mode set to '@'.", arg[0]);
                }catch(Exception e){
                    err("Invalid shuffle mode.");
                }
            }
        });

        handler.register("nextmap", "<mapname...>", "Set the next map to be played after a game-over. Overrides shuffling.", arg -> {
            Map res = maps.all().find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));
            if(res != null){
                maps.setNextMapOverride(res);
                info("Next map set to '@'.", res.plainName());
            }else{
                err("No map '@' found.", arg[0]);
            }
        });

        handler.register("kick", "<username...>", "Kick a person by name.", arg -> {
            if(!state.isGame()){
                err("Not hosting a game yet. Calm down.");
                return;
            }

            Player target = Groups.player.find(p -> p.name().equals(arg[0]));

            if(target != null){
                Call.sendMessage("[scarlet]" + target.name() + "[scarlet] has been kicked by the server.");
                target.kick(KickReason.kick);
                info("It is done.");
            }else{
                info("Nobody with that name could be found...");
            }
        });

        handler.register("ban", "<type-id/name/ip> <username/IP/ID...>", "Ban a person.", arg -> {
            if(arg[0].equals("id")){
                netServer.admins.banPlayerID(arg[1]);
                info("Banned.");
            }else if(arg[0].equals("name")){
                Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1]));
                if(target != null){
                    netServer.admins.banPlayer(target.uuid());
                    info("Banned.");
                }else{
                    err("No matches found.");
                }
            }else if(arg[0].equals("ip")){
                netServer.admins.banPlayerIP(arg[1]);
                info("Banned.");
            }else{
                err("Invalid type.");
            }

            for(Player player : Groups.player){
                if(netServer.admins.isIDBanned(player.uuid())){
                    Call.sendMessage("[scarlet]" + player.name + " has been banned.");
                    player.con.kick(KickReason.banned);
                }
            }
        });

        handler.register("bans", "List all banned IPs and IDs.", arg -> {
            Seq<PlayerInfo> bans = netServer.admins.getBanned();

            if(bans.size == 0){
                info("No ID-banned players have been found.");
            }else{
                info("Banned players [ID]:");
                for(PlayerInfo info : bans){
                    info(" @ / Last known name: '@'", info.id, info.plainLastName());
                }
            }

            Seq<String> ipbans = netServer.admins.getBannedIPs();

            if(ipbans.size == 0){
                info("No IP-banned players have been found.");
            }else{
                info("Banned players [IP]:");
                for(String string : ipbans){
                    PlayerInfo info = netServer.admins.findByIP(string);
                    if(info != null){
                        info("  '@' / Last known name: '@' / ID: '@'", string, info.plainLastName(), info.id);
                    }else{
                        info("  '@' (No known name or info)", string);
                    }
                }
            }
        });

        handler.register("unban", "<ip/ID>", "Completely unban a person by IP or ID.", arg -> {
            if(netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])){
                info("Unbanned player: @", arg[0]);
            }else{
                err("That IP/ID is not banned!");
            }
        });

        handler.register("pardon", "<ID>", "Pardons a votekicked player by ID and allows them to join again.", arg -> {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);

            if(info != null){
                info.lastKicked = 0;
                netServer.admins.kickedIPs.remove(info.lastIP);
                info("Pardoned player: @", info.plainLastName());
            }else{
                err("That ID can't be found.");
            }
        });

        handler.register("admin", "<add/remove> <username/ID...>", "Make an online user admin", arg -> {
            if(!state.isGame()){
                err("Open the server first.");
                return;
            }

            if(!(arg[0].equals("add") || arg[0].equals("remove"))){
                err("Second parameter must be either 'add' or 'remove'.");
                return;
            }

            boolean add = arg[0].equals("add");

            PlayerInfo target;
            Player playert = Groups.player.find(p -> p.plainName().equalsIgnoreCase(Strings.stripColors(arg[1])));
            if(playert != null){
                target = playert.getInfo();
            }else{
                target = netServer.admins.getInfoOptional(arg[1]);
                playert = Groups.player.find(p -> p.getInfo() == target);
            }

            if(target != null){
                if(add){
                    netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
                }else{
                    netServer.admins.unAdminPlayer(target.id);
                }
                if(playert != null) playert.admin = add;
                info("Changed admin status of player: @", target.plainLastName());
            }else{
                err("Nobody with that name or ID could be found. If adding an admin by name, make sure they're online; otherwise, use their UUID.");
            }
        });

        handler.register("admins", "List all admins.", arg -> {
            Seq<PlayerInfo> admins = netServer.admins.getAdmins();

            if(admins.size == 0){
                info("No admins have been found.");
            }else{
                info("Admins:");
                for(PlayerInfo info : admins){
                    info(" &lm @ /  ID: '@' / IP: '@'", info.plainLastName(), info.id, info.lastIP);
                }
            }
        });

        handler.register("players", "List all players currently in game.", arg -> {
            if(Groups.player.size() == 0){
                info("No players are currently in the server.");
            }else{
                info("Players: @", Groups.player.size());
                for(Player user : Groups.player){
                    info(" @&lm @ / ID: @ / IP: @", user.admin ? "&r[A]&c" : "&b[P]&c", user.plainName(), user.uuid(), user.ip());
                }
            }
        });

        handler.register("runwave", "Trigger the next wave.", arg -> {
            if(!state.isGame()){
                err("Not hosting. Host a game first.");
            }else{
                logic.runWave();
                info("Wave spawned.");
            }
        });

        handler.register("loadautosave", "Loads the last auto-save.", arg -> {
            if(state.isGame()){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            Fi newestSave = saveDirectory.findAll(f -> f.name().startsWith("auto_")).min(Fi::lastModified);

            if(newestSave == null){
                err("No auto-saves found! Type `config autosave true` to enable auto-saves.");
                return;
            }

            if(!SaveIO.isSaveValid(newestSave)){
                err("No (valid) save data found for slot.");
                return;
            }

            Core.app.post(() -> {
                try{
                    SaveIO.load(newestSave);
                    state.rules.sector = null;
                    info("Save loaded.");
                    state.set(State.playing);
                    netServer.openServer();
                }catch(Throwable t){
                    err("Failed to load save. Outdated or corrupt file.");
                }
            });
        });

        handler.register("load", "<slot>", "Load a save from a slot.", arg -> {
            if(state.isGame()){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            Fi file = saveDirectory.child(arg[0] + "." + saveExtension);

            if(!SaveIO.isSaveValid(file)){
                err("No (valid) save data found for slot.");
                return;
            }

            Core.app.post(() -> {
                try{
                    SaveIO.load(file);
                    state.rules.sector = null;
                    info("Save loaded.");
                    state.set(State.playing);
                    netServer.openServer();
                }catch(Throwable t){
                    err("Failed to load save. Outdated or corrupt file.");
                }
            });
        });

        handler.register("save", "<slot>", "Save game state to a slot.", arg -> {
            if(!state.isGame()){
                err("Not hosting. Host a game first.");
                return;
            }

            Fi file = saveDirectory.child(arg[0] + "." + saveExtension);

            Core.app.post(() -> {
                SaveIO.save(file);
                info("Saved to @.", file);
            });
        });

        handler.register("saves", "List all saves in the save directory.", arg -> {
            info("Save files: ");
            for(Fi file : saveDirectory.list()){
                if(file.extension().equals(saveExtension)){
                    info("| @", file.nameWithoutExtension());
                }
            }
        });

        handler.register("gameover", "Force a game over.", arg -> {
            if(state.isMenu()){
                err("Not playing a map.");
                return;
            }

            info("Core destroyed.");
            inGameOverWait = false;
            Events.fire(new GameOverEvent(state.rules.waveTeam));
        });

        handler.register("info", "<IP/UUID/name...>", "Find player info(s). Can optionally check for all names or IPs a player has had.", arg -> {
            ObjectSet<PlayerInfo> infos = netServer.admins.findByName(arg[0]);

            if(infos.size > 0){
                info("Players found: @", infos.size);

                int i = 0;
                for(PlayerInfo info : infos){
                    info("[@] Trace info for player '@' / UUID @ / RAW @", i++, info.plainLastName(), info.id, info.lastName);
                    info("  all names used: @", info.names);
                    info("  IP: @", info.lastIP);
                    info("  all IPs used: @", info.ips);
                    info("  times joined: @", info.timesJoined);
                    info("  times kicked: @", info.timesKicked);
                }
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("search", "<name...>", "Search players who have used part of a name.", arg -> {
            ObjectSet<PlayerInfo> infos = netServer.admins.searchNames(arg[0]);

            if(infos.size > 0){
                info("Players found: @", infos.size);

                int i = 0;
                for(PlayerInfo info : infos){
                    info("- [@] '@' / @", i++, info.plainLastName(), info.id);
                }
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("gc", "Trigger a garbage collection. Testing only.", arg -> {
            int pre = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            System.gc();
            int post = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            info("@ MB collected. Memory usage now at @ MB.", pre - post, post);
        });

        handler.register("yes", "Run the last suggested incorrect command.", arg -> {
            if(suggested == null){
                err("There is nothing to say yes to.");
            }else{
                handleCommandString(suggested);
            }
        });

        handler.register("dos-ban", "[add/remove] [ip]", "Add or remove a DOS ban.", arg -> {
            if(arg.length == 0){
                info("DOS bans: @", netServer.admins.dosBlacklist.isEmpty() ? "<none>" : "");

                netServer.admins.dosBlacklist.forEach(address -> {
                    info("&lw\t" + address);
                });
                return;
            }else if(arg.length == 1){
                err("Expected either zero or two parameters, but only got one parameter.");
                return;
            }

            String action = arg[0].toLowerCase();
            String ip = arg[1];

            if(action.equals("add")){
                netServer.admins.blacklistDos(ip);
                info("Dos banned: @", ip);
                return;
            }else if(action.equals("remove")){
                netServer.admins.unBlacklistDos(ip);
                info("Removed dos ban: @", ip);
                return;
            }

            err("Unrecognized action: @", action);
        });

        mods.eachClass(p -> p.registerServerCommands(handler));
    }

    public void handleCommandString(String line){
        CommandResponse response = handler.handleMessage(line);

        if(response.type == ResponseType.unknownCommand){

            int minDst = 0;
            Command closest = null;

            for(Command command : handler.getCommandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < minDst)){
                    minDst = dst;
                    closest = command;
                }
            }

            if(closest != null && !closest.text.equals("yes")){
                err("Command not found. Did you mean \"" + closest.text + "\"?");
                suggested = line.replace(response.runCommand, closest.text);
            }else{
                err("Invalid command. Type 'help' for help.");
            }
        }else if(response.type == ResponseType.fewArguments){
            err("Too few command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.manyArguments){
            err("Too many command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.valid){
            suggested = null;
        }
    }

    /**
     * Cancels the world load timer task, if it is scheduled. Can be useful for stopping a server or hosting a new game.
     */
    public void cancelPlayTask(){
        if(lastTask != null) lastTask.cancel();
    }

    /**
     * Resets the world state, starts a new game.
     * @param run What task to run to load a new world.
     */
    public void play(Runnable run){
        play(true, run);
    }

    /**
     * Resets the world state, starts a new game.
     * @param wait Whether to wait for {@link Config#roundExtraTime} seconds before starting a new game.
     * @param run What task to run to load a new world.
     */
    public void play(boolean wait, Runnable run){
        inGameOverWait = true;
        cancelPlayTask();

        Runnable reload = () -> {
            try{
                WorldReloader reloader = new WorldReloader();
                reloader.begin();

                run.run();

                state.rules = state.map.applyRules(lastMode);
                logic.play();

                reloader.end();
                inGameOverWait = false;
            }catch(MapException e){
                err("@: @", e.map.plainName(), e.getMessage());
                net.closeServer();
            }
        };

        if(wait){
            lastTask = Timer.schedule(reload, Config.roundExtraTime.num());
        }else{
            reload.run();
        }
    }

    public void logToFile(String text){
        if(currentLogFile != null && currentLogFile.length() > Config.maxLogLength.num()){
            currentLogFile.writeString("[End of log file. Date: " + dateTime.format(LocalDateTime.now()) + "]\n", true);
            currentLogFile = null;
        }

        for(String value : values){
            text = text.replace(value, "");
        }

        if(currentLogFile == null){
            int i = 0;
            while(logFolder.child("log-" + i + ".txt").length() >= Config.maxLogLength.num()){
                i++;
            }

            currentLogFile = logFolder.child("log-" + i + ".txt");
        }

        currentLogFile.writeString(text + "\n", true);
    }

    public void toggleSocket(boolean on){
        if(on && socketThread == null){
            socketThread = new Thread(() -> {
                try{
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(Config.socketInputAddress.string(), Config.socketInputPort.num()));
                    while(true){
                        Socket client = serverSocket.accept();
                        info("&lkReceived command socket connection: &fi@", serverSocket.getLocalSocketAddress());
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        socketOutput = new PrintWriter(client.getOutputStream(), true);
                        String line;
                        while(client.isConnected() && (line = in.readLine()) != null){
                            String result = line;
                            Core.app.post(() -> handleCommandString(result));
                        }
                        info("&lkLost command socket connection: &fi@", serverSocket.getLocalSocketAddress());
                        socketOutput = null;
                    }
                }catch(BindException b){
                    err("Command input socket already in use. Is another instance of the server running?");
                }catch(IOException e){
                    if(!e.getMessage().equals("Socket closed") && !e.getMessage().equals("Connection reset")){
                        err("Terminating socket server.");
                        err(e);
                    }
                }
            });
            socketThread.setDaemon(true);
            socketThread.start();
        }else if(socketThread != null){
            socketThread.interrupt();
            try{
                serverSocket.close();
            }catch(IOException e){
                err(e);
            }
            socketThread = null;
            socketOutput = null;
        }
    }
}
