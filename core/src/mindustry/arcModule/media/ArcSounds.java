package mindustry.arcModule.media;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import mindustry.Vars;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static mindustry.arcModule.ARCVars.arcFolderName;

/** gpt-generated code, 后续需要整理，备注了 */
public class ArcSounds{
    public static final String folderName = "customSound";

    public static long scanIntervalMillis = 1000L;

    public static final String[] audioExtensions = {
            "wav", "ogg", "mp3", "flac"
    };

    private static final Map<String, ArcSound> sounds = new HashMap<>();
    private static final Map<String, Seq<Fi>> indexedFiles = new HashMap<>();

    private static long lastScanTime = -1L;

    private ArcSounds(){
    }

    public static ArcSound sound(String name){
        String key = normalizeName(name);

        ArcSound sound = sounds.get(key);
        if(sound == null){
            sound = new ArcSound(key);
            sounds.put(key, sound);
        }

        return sound;
    }

    public static int play(String name){
        return play(name, 1f, 1f, 0f, false, true);
    }

    public static int play(String name, float volume){
        return play(name, volume, 1f, 0f, false, true);
    }

    public static int play(String name, float volume, float pitch){
        return play(name, volume, pitch, 0f, false, true);
    }

    public static int play(String name, float volume, float pitch, float pan){
        return play(name, volume, pitch, pan, false, true);
    }

    public static int play(String name, float volume, float pitch, float pan, boolean loop){
        return play(name, volume, pitch, pan, loop, true);
    }

    /**
     * 所有 ArcSounds.play(...) 最终都走这里。
     * 留意volume实际上不生效，只是留作后续可能有额外音频控制备用--lc
     */
    public static int play(String name, float volume, float pitch, float pan, boolean loop, boolean checkFrame){
        if(!Core.settings.getBool("enableArcCustomSound")){
            return -1;
        }

        return sound(name).playCustom(Core.settings.getInt("ArcCustomSoundvol")/100f, pitch, pan, loop, checkFrame);
    }

    public static int loop(String name){
        return play(name, 1f, 1f, 0f, true, true);
    }

    public static int loop(String name, float volume){
        return play(name, volume, 1f, 0f, true, true);
    }

    public static int loop(String name, float volume, float pitch, float pan){
        return play(name, volume, pitch, pan, true, true);
    }

    public static void stop(String name){
        sound(name).stop();
    }

    public static void stopAll(){
        for(ArcSound sound : sounds.values()){
            sound.stop();
        }
    }

    public static void reload(){
        lastScanTime = -1L;
        refresh(true);
    }

    public static void clear(){
        stopAll();
        sounds.clear();
        indexedFiles.clear();
        lastScanTime = -1L;
    }

    static Seq<Fi> files(String name){
        refresh(false);

        Seq<Fi> files = indexedFiles.get(normalizeName(name));
        return files == null ? new Seq<>() : files;
    }

    static void refresh(boolean force){
        long now = System.currentTimeMillis();

        if(!force && lastScanTime >= 0L && now - lastScanTime < scanIntervalMillis){
            return;
        }

        lastScanTime = now;
        indexedFiles.clear();

        Fi root = root();

        if(root == null || !root.exists() || !root.isDirectory()){
            return;
        }

        Seq<Fi> all = root.findAll(ArcSounds::isSupportedAudio);

        for(Fi file : all){
            String key = normalizeName(file.nameWithoutExtension());

            Seq<Fi> list = indexedFiles.get(key);
            if(list == null){
                list = new Seq<>();
                indexedFiles.put(key, list);
            }

            list.add(file);
        }
    }

    /**
     * Mindustry/customSound
     */
    public static Fi root(){
        if(Vars.dataDirectory != null){
            return Vars.dataDirectory.child(arcFolderName).child(folderName);
        }

        if(Core.settings != null){
            return Core.settings.getDataDirectory().child(folderName);
        }

        return null;
    }

    public static boolean isSupportedAudio(Fi file){
        if(file == null || file.isDirectory()) return false;

        String ext = file.extension();

        for(String allowed : audioExtensions){
            if(ext.equalsIgnoreCase(allowed)){
                return true;
            }
        }

        return false;
    }

    public static String normalizeName(String name){
        if(name == null) return "";

        String result = name.replace('\\', '/');

        int slash = result.lastIndexOf('/');
        if(slash >= 0){
            result = result.substring(slash + 1);
        }

        int dot = result.lastIndexOf('.');
        if(dot >= 0){
            result = result.substring(0, dot);
        }

        return result.toLowerCase(Locale.ROOT);
    }
}