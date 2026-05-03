package mindustry.arcModule.media;

import arc.Core;
import arc.audio.AudioBus;
import arc.audio.Sound;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.Seq;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/** gpt-generated code, 后续需要整理，备注了 */
public class ArcSound extends Sound{
    private String name;

    private final Map<String, Sound> loaded = new HashMap<>();

    private long minInterval = 16L;
    private float falloffOffset = 0f;

    public ArcSound(){
        super();
    }

    public ArcSound(String name){
        super();
        this.name = ArcSounds.normalizeName(name);
    }

    public ArcSound(Fi file){
        super();
        load(file);
    }

    @Override
    public void load(Fi file){
        this.file = file;
        this.name = file == null ? "" : ArcSounds.normalizeName(file.nameWithoutExtension());
    }

    /**
     * Arc 原本的 Sound.play(...) 调用最终会进这里。
     * 这里不要直接播放，统一交给 ArcSounds.play(...)。
     */
    @Override
    public int play(float volume, float pitch, float pan, boolean loop, boolean checkFrame){
        return ArcSounds.play(name, volume, pitch, pan, loop, checkFrame);
    }

    /**
     * 真正播放 customSound 的内部函数。
     * 只允许 ArcSounds.play(...) 调用它，避免递归。
     */
    int playCustom(float volume, float pitch, float pan, boolean loop, boolean checkFrame){
        Sound selected = select();

        if(selected == null){
            return -1;
        }

        configure(selected);

        return selected.play(volume, pitch, pan, loop, checkFrame);
    }

    @Override
    public void stop(){
        for(Sound sound : loaded.values()){
            sound.stop();
        }
    }

    @Override
    public float getLength(){
        Sound selected = select();
        return selected == null ? 0f : selected.getLength();
    }

    @Override
    public void setBus(AudioBus bus){
        super.setBus(bus);

        for(Sound sound : loaded.values()){
            sound.setBus(bus);
        }
    }

    @Override
    public void setMinInterval(long interval){
        this.minInterval = interval;

        for(Sound sound : loaded.values()){
            sound.setMinInterval(interval);
        }
    }

    @Override
    public void setFalloffOffset(float falloffOffset){
        super.setFalloffOffset(falloffOffset);

        this.falloffOffset = falloffOffset;

        for(Sound sound : loaded.values()){
            sound.setFalloffOffset(falloffOffset);
        }
    }

    private Sound select(){
        if(name == null || name.isEmpty()){
            return null;
        }

        if(Core.audio == null || !Core.audio.initialized()){
            return null;
        }

        Seq<Fi> files = ArcSounds.files(name);

        if(files.size <= 0){
            return null;
        }

        retainOnly(files);

        Fi file = files.get(Mathf.random(files.size - 1));
        String key = file.absolutePath();

        Sound sound = loaded.get(key);

        if(sound == null){
            sound = Core.audio.newSound(file);
            loaded.put(key, sound);
        }

        return sound;
    }

    private void configure(Sound sound){
        // 使用独立 bus 播放，避免游戏暂停时 Core.audio.soundBus 被暂停/静音导致新音效无法发声。
        AudioBus customBus = ArcSounds.customSoundBus();
        sound.setBus(customBus == null ? bus : customBus);
        sound.setMinInterval(minInterval);
        sound.setFalloffOffset(falloffOffset);
    }

    private void retainOnly(Seq<Fi> files){
        HashSet<String> alive = new HashSet<>();

        for(Fi file : files){
            alive.add(file.absolutePath());
        }

        loaded.keySet().removeIf(key -> !alive.contains(key));
    }

    public String name(){
        return name;
    }

    @Override
    public String toString(){
        return "ArcSound: " + name;
    }
}