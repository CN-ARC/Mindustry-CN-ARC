package mindustry.arcModule.ui.scratch;

import arc.struct.ObjectMap;
import arc.struct.Seq;

public class ScratchEvents {
    private static final ObjectMap<Event, Seq<Runnable>> events = new ObjectMap<>();

    public static void on(Event e, Runnable r) {
        events.get(e, Seq::new).add(r);
    }

    public static void fire(Event e) {
        Seq<Runnable> s = events.get(e);
        if (s != null) s.each(Runnable::run);
    }

    public enum Event {
        saveBegin, saveEnd, loadBegin, loadEnd, clear
    }
}