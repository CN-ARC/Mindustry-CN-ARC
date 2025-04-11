package mindustry.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.Tmp;
import mindustry.arcModule.ARCVars;
import mindustry.gen.*;
import mindustry.input.DesktopInput;

import static mindustry.Vars.*;

public class Minimap extends Table {

    private float size;
    private Element map;

    // 添加一个自定义手势监听器类来跟踪拖动状态
    private static class DragTrackingGestureListener extends ElementGestureListener {
        private boolean isDragging = false;
        private float initialX, initialY;
        private static final float DRAG_THRESHOLD = 3f; // 拖动阈值

        // 跟踪拖动起点对应的世界坐标
        private float worldStartX, worldStartY;

        public boolean isDragging() {
            return isDragging;
        }

        @Override
        public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            super.touchDown(event, x, y, pointer, button);
            if (button == KeyCode.mouseMiddle) { // 改为中键
                initialX = x;
                initialY = y;
                isDragging = false;

                // 记录点击位置对应的世界坐标
                if (renderer.minimap.getRegion() != null) {
                    // 计算点击点在小地图上的相对位置
                    Element target = event.listenerActor;
                    float sx = (x - target.x) / target.getWidth();
                    float sy = (y - target.y) / target.getHeight();

                    // 将相对位置转换为世界坐标
                    var region = renderer.minimap.getRegion();
                    worldStartX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize;
                    worldStartY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;
                }
            }
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            super.touchUp(event, x, y, pointer, button);
            if (button == KeyCode.mouseMiddle) { // 改为中键
                // 如果拖动距离小，重置拖动状态
                if (Math.abs(x - initialX) < DRAG_THRESHOLD && Math.abs(y - initialY) < DRAG_THRESHOLD) {
                    isDragging = false;
                }
            }
        }

        @Override
        public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
            if (event.keyCode == KeyCode.mouseMiddle) { // 改为中键
                // 检查拖动距离是否超过阈值
                float dragDist = (float) Math.sqrt(Math.pow(x - initialX, 2) + Math.pow(y - initialY, 2));
                if (dragDist > DRAG_THRESHOLD) {
                    isDragging = true;

                    // 计算拖动的世界坐标差值
                    Element target = event.listenerActor;
                    float scaleX = (world.width() * tilesize) / target.getWidth();
                    float scaleY = (world.height() * tilesize) / target.getHeight();

                    // 计算拖动后的目标世界坐标
                    float worldTargetX = worldStartX + deltaX * scaleX;
                    float worldTargetY = worldStartY + deltaY * scaleY;
                    // 更新当前起点为最新相机位置，防止重复回拉
worldStartX = worldTargetX;
worldStartY = worldTargetY;


                    // 使用control.input.panCamera方法移动摄像机
                    control.input.panCamera(Tmp.v1.set(worldTargetX, worldTargetY));

                    event.handle(); // 标记事件已处理
                }
            }
        }
    }

    public Minimap() {
        background(Tex.pane);
        float margin = 5f;
        this.touchable = Touchable.enabled;

        add(new Element() {
            {
                setSize(Scl.scl(140f));

                // 添加自定义手势监听器来处理拖动
                DragTrackingGestureListener gestureListener = new DragTrackingGestureListener();
                addListener(gestureListener);

                // 添加右键点击事件处理器
                addListener(new ClickListener(KeyCode.mouseRight) {
                    @Override
                    public void clicked(InputEvent event, float cx, float cy) {
                        var region = renderer.minimap.getRegion();
                        if (region == null) return;
                        float sx = (cx - x) / width,
                              sy = (cy - y) / height,
                              scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                              scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;
                        control.input.panCamera(Tmp.v1.set(scaledX, scaledY));
                    }
                });
            }

            @Override
            public void act(float delta) {
                setPosition(Scl.scl(margin), Scl.scl(margin));
                super.act(delta);
            }

            @Override
            public void draw() {
                if (renderer.minimap.getRegion() == null) return;
                if (!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if (renderer.minimap.getTexture() != null) {
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, renderer.minimap.getZoom(), false);
                }

                clipEnd();
            }
        }).size(140f);

        margin(margin);

        // 保留滚轮缩放功能
        addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty) {
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
            if (e != null && e.isDescendantOf(this)) {
                requestScroll();
            } else if (hasScroll()) {
                Core.scene.setScrollFocus(null);
            }
        });
    }

    private void buildMap() {
        float margin = 5f;
        map = new Element() {
            {
                setSize(Scl.scl(size));

                // 添加自定义手势监听器来处理拖动
                DragTrackingGestureListener gestureListener = new DragTrackingGestureListener();
                addListener(gestureListener);

                // 为新地图元素添加右键点击事件处理器
                addListener(new ClickListener(KeyCode.mouseRight) {
                    @Override
                    public void clicked(InputEvent event, float cx, float cy) {
                        var region = renderer.minimap.getRegion();
                        if (region == null) return;
                        float sx = (cx - x) / width,
                              sy = (cy - y) / height,
                              scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                              scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;
                        control.input.panCamera(Tmp.v1.set(scaledX, scaledY));
                    }
                });

                // 修改左键点击事件处理器，检查是否处于拖动状态
                addListener(new ClickListener(KeyCode.mouseLeft) {
    @Override
    public void clicked(InputEvent event, float cx, float cy) {
        ui.minimapfrag.toggle();
        event.handle();
    }
});

            }

            @Override
            public void act(float delta) {
                setPosition(Scl.scl(margin), Scl.scl(margin));
                super.act(delta);
            }

            @Override
            public void draw() {
                if (renderer.minimap.getRegion() == null) return;
                if (!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if (renderer.minimap.getTexture() != null) {
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, 3f / renderer.minimap.getZoom(), false);
                }

                clipEnd();
            }
        };
        add(map).size(size);
    }
}
