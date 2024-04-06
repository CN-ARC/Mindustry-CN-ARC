package mindustry.arcModule.ui.utils;

import arc.func.Boolf;
import arc.scene.event.EventListener;
import arc.scene.event.SceneEvent;

public class DelayedInitListener implements EventListener {
    private Boolf<SceneEvent> listener = null;

    public DelayedInitListener setListener(Boolf<SceneEvent> l) {
        listener = l;
        return this;
    }

    public void remove() {
        listener = null;
    }

    @Override
    public boolean handle(SceneEvent event) {
        return listener != null && listener.get(event);
    }
}
