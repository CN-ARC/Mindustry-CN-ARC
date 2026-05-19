package mindustry.arcModule;

import arc.*;
import arc.files.*;
import arc.math.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.Saves.SaveSlot;
import mindustry.io.*;

import static mindustry.Vars.*;

/**
 * ARC rotating save backup.
 *
 * Saves to arcBackup_{id}.msav only when the current map game time differs from
 * the previous backup slot. This uses the save meta "tick" value instead of
 * serialized map bytes, so server/headless saves can be compared without writing
 * a temporary save first.
 */
public class ARCBackup {
    private static final String prefix = "[gray]arcBackup_[]";
    private static final String indexKey = "arcBackupIndex";
    private boolean loaded = false;
    private static float timer = 0f;

    public void init(){
        if(loaded) return;
        loaded = true;

        Events.on(EventType.WorldLoadEvent.class, e -> reset());
        Events.on(EventType.ResetEvent.class, e -> reset());
        Events.run(EventType.Trigger.update, ARCBackup::update);
    }

    public void reset(){
        timer = 0f;
    }

    public static void update(){
        if(!shouldBackupInThisRuntime()) return;
        if(!state.isGame() || state.gameOver || world.isGenerating()){
            timer = 0f;
            return;
        }

        if(state.isPaused()) return;

        timer += Time.delta / 60f;
        if(timer < intervalSeconds()) return;

        timer = 0f;
        tryBackup();
    }

    public static void tryBackup(){
        int slots = slotCount();
        int id = Mathf.mod(Core.settings.getInt(indexKey, 0), slots);
        int previousId = Mathf.mod(id - 1, slots);

        Fi previous = fileFor(previousId);
        Fi target = fileFor(id);

        try{
            double currentTime = currentMapTime();
            Float previousTime = previous.exists() ? readMapTime(previous) : null;

            if(previousTime != null && Math.abs(currentTime - previousTime) <= 1f){
                Log.info("ARC backup skipped; map time @ is unchanged from @.", currentTime, previous.nameWithoutExtension());
                return;
            }

            Log.info("ARC backup saving to @...", target.nameWithoutExtension());
            SaveIO.save(target);
            markSaveSlot(target, id);
            Core.settings.put(indexKey, Mathf.mod(id + 1, slots));
            Core.settings.forceSave();
            refreshLoadedSlot(target);
            Log.info("ARC backup completed: @.", target.nameWithoutExtension());
        }catch(Throwable t){
            Log.err("ARC backup failed.", t);
        }
    }

    private static boolean shouldBackupInThisRuntime(){
        int mode = Core.settings.getInt("arcSaveMode", 0);
        if(mode <= 0) return false;

        // Remote clients must not write rotating backups for a server world.
        if(net != null && net.client()) return false;

        // 1 = local only; 2 = local + server. Treat a hosted game as server-side.
        boolean server = headless || (net != null && net.server());
        return server ? mode >= 2 : mode >= 1;
    }

    private static int slotCount(){
        // arcBackupSlot is configured as 0..10 and is used as the highest slot id.
        // 0 therefore means a single file: arcBackup_0.msav.
        return Mathf.clamp(Core.settings.getInt("arcBackupSlot", 0), 0, 10) + 1;
    }

    private static int intervalSeconds(){
        return Core.settings.getInt("arcBackupInterval", 30);
    }

    private static Fi fileFor(int id){
        return saveDirectory.child(prefix + id + "." + saveExtension);
    }

    private static void markSaveSlot(Fi file, int id){
        String name = prefix + id;
        Core.settings.put("save-" + file.nameWithoutExtension() + "-name", name);
        Core.settings.put("save-" + file.nameWithoutExtension() + "-autosave", false);
    }

    private static void refreshLoadedSlot(Fi file){
        if(headless || control == null || control.saves == null) return;

        try{
            SaveSlot slot = control.saves.getSaveSlots().find(s -> s.file.equals(file));
            if(slot == null){
                slot = control.saves.new SaveSlot(file);
                control.saves.getSaveSlots().add(slot);
            }
            slot.meta = SaveIO.getMeta(file);
        }catch(Throwable t){
            Log.err("Failed to refresh ARC backup save slot.", t);
        }
    }

    private static double currentMapTime(){
        return state.tick;
    }

    private static Float readMapTime(Fi file){
        try{
            return readMapTimeRaw(file);
        }catch(Throwable first){
            Fi backup = SaveIO.backupFileFor(file);
            if(backup.exists()){
                try{
                    return readMapTimeRaw(backup);
                }catch(Throwable second){
                    Log.err("Failed to read ARC backup map time from @ and its backup.", file, second);
                    return null;
                }
            }

            Log.err("Failed to read ARC backup map time from @.", file, first);
            return null;
        }
    }

    private static Float readMapTimeRaw(Fi file){
        SaveMeta meta = SaveIO.getMeta(file);
        float tick = meta.tags.getFloat("tick", -1f);
        return tick < 0f ? null : tick;
    }
}
