package mindustry.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.Tmp;
import mindustry.arcModule.ARCVars;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.DesktopInput;

import static mindustry.Vars.*;

public class Minimap extends Table{

    private float size;
    private Element map;
    private static class DragTrackingGestureListener extends ElementGestureListener {
        private boolean isDragging = false;
        private float initialX, initialY;
        private float worldStartX, worldStartY;

        public boolean isDragging() {
            return isDragging;
        }

        @Override
        public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            super.touchDown(event, x, y, pointer, button);
            if (button == KeyCode.mouseMiddle) {
                initialX = x;
                initialY = y;
                isDragging = false;
                if (renderer.minimap.getRegion() != null) {
                    Element target = event.listenerActor;
                    float sx = (x - target.x) / target.getWidth();
                    float sy = (y - target.y) / target.getHeight();
                    var region = renderer.minimap.getRegion();
                    worldStartX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize;
                    worldStartY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;
                }
            }
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            super.touchUp(event, x, y, pointer, button);
            if (button == KeyCode.mouseMiddle) {
                    isDragging = false;
            }
        }

        @Override
        public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
            if (event.keyCode == KeyCode.mouseMiddle) {
                float dragDist = (float) Math.sqrt(Math.pow(x - initialX, 2) + Math.pow(y - initialY, 2));
                    isDragging = true;
                    Element target = event.listenerActor;
                    float scaleX = (world.width() * tilesize) / target.getWidth();
                    float scaleY = (world.height() * tilesize) / target.getHeight();
                    float worldTargetX = worldStartX + deltaX * scaleX;
                    float worldTargetY = worldStartY + deltaY * scaleY;
                    worldStartX = worldTargetX;
                    worldStartY = worldTargetY;
                    control.input.panCamera(Tmp.v1.set(worldTargetX, worldTargetY));
                    event.handle();
            }
        }
    }

    public Minimap(){
        background(Tex.pane);
        float margin = 5f;
        this.touchable = Touchable.enabled;

        add(new Element(){
            {
                setSize(Scl.scl(140f));
                DragTrackingGestureListener gestureListener = new DragTrackingGestureListener();
                addListener(gestureListener);

                addListener(new ClickListener(KeyCode.mouseRight){
                    @Override
                    public void clicked(InputEvent event, float cx, float cy){
                        var region = renderer.minimap.getRegion();
                        if(region == null) return;

                        float
                        sx = (cx - x) / width,
                        sy = (cy - y) / height, 
                        scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                        scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;
                        control.input.panCamera(Tmp.v1.set(scaledX, scaledY));
                    }
                });
            }
            @Override
            public void act(float delta){
                setPosition(Scl.scl(margin), Scl.scl(margin));

               super.act(delta);
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;
                if(!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, renderer.minimap.getZoom(), false);
                }

                clipEnd();
            }
        }).size(140f);

        margin(margin);

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }
        });
        update(() -> {
            if ((float) ARCVars.getMinimapSize() != size) {
                size = (float) ARCVars.getMinimapSize();
                clearChildren();
                buildMap();
            }

            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(this)){
                requestScroll();
            }else if(hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });

    }

    private void buildMap() {
        float margin = 5f;
        map = new Element(){
            {
                setSize(Scl.scl(size));
                DragTrackingGestureListener gestureListener = new DragTrackingGestureListener();
                addListener(gestureListener);

                addListener(new ClickListener(KeyCode.mouseRight){
                    @Override
                    public void clicked(InputEvent event, float cx, float cy) {
                        var region = renderer.minimap.getRegion();
                        if(region == null) return;
 
                        float
                        sx = (cx - x) / width,
                        sy = (cy - y) / height,
                        scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                        scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;

                        control.input.panCamera(Tmp.v1.set(scaledX, scaledY));
                    }
                });
                addListener(new ClickListener(KeyCode.mouseLeft) {
                    @Override
                    public void clicked(InputEvent event, float cx, float cy) {
                        ui.minimapfrag.toggle();
                        event.handle();
                    }
                });
            }

            @Override
            public void act(float delta){
                setPosition(Scl.scl(margin), Scl.scl(margin));

                super.act(delta);
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;
                if(!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, 3f / renderer.minimap.getZoom(), false);
                }

                clipEnd();
            }
        };
        add(map).size(size);
    }
}
