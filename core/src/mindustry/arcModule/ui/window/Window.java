package mindustry.arcModule.ui.window;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Tmp;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.ARCVars.getThemeColor;

@SuppressWarnings("unused")
public class Window {
    public WindowTable table;
    WindowManager manager;
    String title;
    Table cont;
    Drawable icon;
    public Image iconImage;
    public boolean removed = false, added = false, minSized = false, resizable = true, maxSizable = true, minSizable = true, closable = true;
    boolean cursorRestored = true, closeToRemove = true;
    float minWidth = 200, minHeight = 200;
    private final ObjectMap<Enum<WindowEvents>, Seq<Cons<Window>>> events = new ObjectMap<>();
    private static Window front = null;

    public Window() {
        this(arcui.WindowManager);
    }

    public Window(WindowManager manager) {
        this("Title", manager);
    }

    public Window(String title, WindowManager manager) {
        this(title, 600, 400, manager);
    }

    public Window(String title, float width, float height, WindowManager manager) {
        this(title, width, height, (TextureRegionDrawable) Core.atlas.getDrawable("error"), manager);
    }

    public Window(String title, float width, float height, TextureRegion icon, WindowManager manager) {
        this(title, width, height, new TextureRegionDrawable(icon), manager);
    }

    public Window(String title, float width, float height, Drawable icon, WindowManager manager) {
        iconImage = new Image(icon);
        iconImage.setScaling(Scaling.fit);
        this.manager = manager;
        this.title = title;
        this.icon = icon;
        table = new WindowTable(width, height);
    }

    public void add() {
        if (added && !closeToRemove) {
            table.visible = true;
            fire(WindowEvents.open);
            return;
        }
        added = true;
        fire(WindowEvents.open);
        manager.addWindow(this);
        front = this;
        fadeIn();
    }

    public void fadeIn() {
        table.actions(Actions.sequence(Actions.alpha(0), Actions.fadeIn(10f / 60f)));
    }

    public void fadeOut() {
        table.touchable = Touchable.disabled;
        table.actions(Actions.sequence(Actions.alpha(1), Actions.fadeOut(10f / 60f), Actions.remove()));
    }

    public Window setIcon(TextureRegion icon) {
        iconImage.setDrawable(icon);
        fire(WindowEvents.iconChanged);
        return this;
    }

    public Window setTitle(String title) {
        this.title = title;
        table.lastLabelWidth = 0;
        fire(WindowEvents.titleChanged);
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setBody(Table body) {
        cont.clear();
        cont.add(body).grow();
        fire(WindowEvents.bodyChanged);
    }

    public void posX(float x) {
        table.x = x;
    }

    public float posX() {
        return table.x;
    }

    public void posY(float y) {
        table.y = y;
    }

    public float posY() {
        return table.y;
    }

    public void pos(float x, float y) {
        table.x = x;
        table.y = y;
    }

    public void center() {
        pos((table.parent.getWidth() - table.getWidth()) / 2, (table.parent.getHeight() - table.getHeight()) / 2);
    }

    public boolean visible() {
        return table.visible && !removed;
    }

    public void remove() {
        if (removed) return;
        if (closeToRemove) {
            removed = true;
            fire(WindowEvents.close);
        }
        table.remove();
    }

    public void closeToRemove(boolean toggle) {
        closeToRemove = toggle;
    }

    public void setMinSize(float width, float height) {
        minWidth = width;
        minHeight = height;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isMaxSizable() {
        return maxSizable;
    }

    public void setMaxSizable(boolean maxSizable) {
        this.maxSizable = maxSizable;
        if (!maxSizable) table.cancelMaximize();
    }

    public boolean isMinSizable() {
        return minSizable;
    }

    public void setMinSizable(boolean minSizable) {
        this.minSizable = minSizable;
        if (!minSizable) table.cancelMinimize();
    }

    public boolean isClosable() {
        return closable;
    }

    public void setClosable(boolean closable) {
        this.closable = closable;
    }

    public boolean maximize(boolean maximize) {
        if (!maxSizable) return false;
        if (maximize) {
            return table.maximize();
        } else {
            return table.cancelMaximize();
        }
    }

    public boolean minimize(boolean minimize) {
        if (!minSizable) return false;
        if (minimize) {
            return table.minimize();
        } else {
            return table.cancelMinimize();
        }
    }

    public float getWidth() {
        return table.getWidth();
    }

    public float getHeight() {
        return table.getHeight();
    }

    public void setWidth(float width) {
        table.setWidth(width);
    }

    public void setHeight(float height) {
        table.setHeight(height);
    }

    public void setSize(float w, float h) {
        table.setSize(w, h);
    }

    public void update(Runnable r) {
        table.update(r);
    }

    public void addListener(Enum<WindowEvents> type, Cons<Window> listener){
        events.get(type, () -> new Seq<>(Cons.class)).add(listener);
    }

    public void fire(Enum<WindowEvents> type){
        Seq<Cons<Window>> listeners = events.get(type);

        if(listeners != null){
            int len = listeners.size;
            Cons<Window>[] items = listeners.items;
            for(int i = 0; i < len; i++){
                items[i].get(this);
            }
        }
    }

    private static class ResizeDirection {
        public static byte X = 1, Y = 2;
        public static byte FlipX = 4, FlipY = 8;
        public static byte RIGHT = X;
        public static byte TOP = Y;
        public static byte LEFT = (byte) (RIGHT | FlipX);
        public static byte BOTTOM = (byte) (TOP | FlipY);
        public static byte BOTTOM_LEFT = (byte) (BOTTOM | LEFT);
        public static byte BOTTOM_RIGHT = (byte) (BOTTOM | RIGHT);
        public static byte TOP_LEFT = (byte) (TOP | LEFT);
        public static byte TOP_RIGHT = (byte) (TOP | RIGHT);
    }

    public class WindowTable extends Table {
        private boolean resizing = false;
        private byte resizeDirection;
        private float lastX, lastY, lastHeight, lastWidth;
        private final Label layoutLabel = new Label("");
        private float lastLabelWidth = 0;
        String cachedTitle = "";

        public WindowTable(float width, float height) {
            setColor(new Color(127, 127, 127, 255));
            margin(0f);
            touchable = Touchable.childrenOnly;
            add(new Table(t -> {
                t.add(iconImage).size(12).pad(10, 12, 10, 12);
                t.table(tt -> {
                    tt.add("").update(l -> {
                        l.setText(calcString(title, l.getWidth()));
                        l.setColor((front == Window.this ? Color.white : Color.gray));
                    }).grow();
                    tt.addListener(new InputListener() {
                        float lastX, lastY;

                        @Override
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                            if (fillParent) return true;
                            Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                            lastX = v.x;
                            lastY = v.y;
                            toFront();
                            return true;
                        }

                        @Override
                        public void touchDragged(InputEvent event, float x, float y, int pointer) {
                            if (resizing || fillParent) return;
                            Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                            WindowTable.this.x += v.x - lastX;
                            WindowTable.this.y += v.y - lastY;
                            lastX = v.x;
                            lastY = v.y;
                        }
                    });
                }).grow();
                t.button("一", Styles.cleart, () -> {
                    if (minSized) {
                        cancelMinimize();
                    } else {
                        minimize();
                    }
                }).size(46, 28).pad(1).disabled(e -> !minSizable).right();
                t.button(Icon.copySmall, Styles.clearNonei, () -> {
                    if (fillParent) {
                        cancelMaximize();
                    } else {
                        maximize();
                    }
                }).size(46, 28).pad(1).disabled(e -> !maxSizable).right();
                t.button(Icon.cancelSmall, Styles.clearNonei, this::remove).size(46, 28).pad(1).disabled(e -> !closable).right();
                t.touchable = Touchable.enabled;
            }) {
                @Override
                public void draw() {
                    Draw.color(front == Window.this ? getThemeColor() : Color.white);
                    Fill.rect(x + width / 2, y + height / 2, width, height);
                    super.draw();
                    Draw.reset();
                }
            }).height(32).top().growX().row();
            WidgetGroup group = new WidgetGroup() {
                @Override
                public void layout() {
                    children.peek().setBounds(x, y, getWidth(), getHeight());
                }
            };
            add(group).grow();
            group.addChild(new Table(tt -> {
                tt.setClip(true);
                tt.setFillParent(true);
                tt.align(Align.left);
                tt.add().grow();
                cont = tt;
                tt.touchable = Touchable.enabled;
            }));
            addListener(new InputListener() {
                float lastX, lastY;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    if (fillParent || minSized || !resizable) return false;
                    Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                    lastX = v.x;
                    lastY = v.y;
                    toFront();
                    front = Window.this;
                    if (x < 7 && y < 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.BOTTOM_LEFT;
                    } else if (x > getWidth() - 7 && y < 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.BOTTOM_RIGHT;
                    } else if (x < 7 && y > getHeight() - 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.TOP_LEFT;
                    } else if (x > getWidth() - 7 && y > getHeight() - 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.TOP_RIGHT;
                    } else if (x < 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.LEFT;
                    } else if (x > getWidth() - 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.RIGHT;
                    } else if (y < 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.BOTTOM;
                    } else if (y > getHeight() - 7) {
                        resizing = true;
                        resizeDirection = ResizeDirection.TOP;
                    } else {
                        resizing = false;
                        return false;
                    }
                    Window.this.fire(WindowEvents.resizeStart);
                    touchable = Touchable.disabled;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    if (!resizing) return;
                    Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                    float deltaX = 0, deltaY = 0;
                    float transX = 0, transY = 0;
                    if ((resizeDirection & ResizeDirection.X) != 0) {
                        if ((resizeDirection & ResizeDirection.FlipX) == 0) {
                            deltaX = v.x - lastX;
                        } else {
                            deltaX = lastX - v.x;
                            transX = v.x - lastX;
                        }
                    }
                    if ((resizeDirection & ResizeDirection.Y) != 0) {
                        if ((resizeDirection & ResizeDirection.FlipY) == 0) {
                            deltaY = v.y - lastY;
                        } else {
                            deltaY = lastY - v.y;
                            transY = v.y - lastY;
                        }
                    }
                    if (getWidth() + deltaX < minWidth) {
                        transX = deltaX = 0;
                    } else {
                        lastX = v.x;
                    }
                    if (getHeight() + deltaY < minHeight) {
                        transY = deltaY = 0;
                    } else {
                        lastY = v.y;
                    }
                    WindowTable.this.x += transX;
                    WindowTable.this.y += transY;
                    setSize(getWidth() + deltaX, getHeight() + deltaY);
                    Window.this.fire(WindowEvents.resizing);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    resizing = false;
                    touchable = Touchable.enabled;
                    Window.this.fire(WindowEvents.resizeFinish);
                }

                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    if (fillParent || minSized || !resizable) return true;
                    if (x < 7 && y < 7 || x > getWidth() - 7 && y > getHeight() - 7) {
                        Core.graphics.cursor(arcui.resizeRightCursor);
                    } else if (x > getWidth() - 7 && y < 7 || x < 7 && y > getHeight() - 7) {
                        Core.graphics.cursor(arcui.resizeLeftCursor);
                    } else if (x < 7 || x > getWidth() - 7) {
                        Core.graphics.cursor(arcui.resizeHorizontalCursor);
                    } else if (y < 7 || y > getHeight() - 7) {
                        Core.graphics.cursor(arcui.resizeVerticalCursor);
                    } else if (!cursorRestored) {
                        Core.graphics.restoreCursor();
                        return cursorRestored = true;
                    }
                    cursorRestored = false;
                    return true;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                    if (pointer == -1 && !cursorRestored) {
                        Core.graphics.restoreCursor();
                        cursorRestored = true;
                    }
                }
            });
            setWidth(width);
            setHeight(height);
        }

        @Override
        public void draw() {
            super.draw();
            Draw.color(front == Window.this ? getThemeColor() : Color.gray);
            Lines.rect(x - 1, y - 1, width + 1, height + 1);
        }

        @Override
        public boolean remove() {
            if (closeToRemove) {
                if (removed) {
                    return super.remove();
                } else {
                    removed = true;
                    manager.removeWindow(Window.this);
                    fadeOut();
                    return true;
                }
            } else {
                visible = false;
                return false;
            }
        }

        private String calcString(String input, float width) {
            if (width == lastLabelWidth) return cachedTitle;
            lastLabelWidth = width;
            if (input.isEmpty()) return "";
            layoutLabel.setText("....");
            float p = layoutLabel.getPrefWidth();
            for (int i = 1 , l = input.length(); i < l; i++) {
                layoutLabel.setText(input.substring(0, i));
                if (layoutLabel.getPrefWidth() + p > width) return cachedTitle = (i - 1 > 0 ? input.substring(0, i - 1) : "") + "...";
            }
            return cachedTitle = input;
        }

        private void savePos() {
            lastX = x;
            lastY = y;
            lastWidth = getWidth();
            lastHeight = getHeight();
        }

        private void loadPos() {
            x = lastX;
            y = lastY;
            setWidth(lastWidth);
            setHeight(lastHeight);
        }

        private boolean maximize() {
            if (fillParent) return false;
            savePos();
            x = y = 0;
            setFillParent(true);
            Window.this.fire(WindowEvents.maximize);
            return true;
        }

        private boolean cancelMaximize() {
            if (!fillParent) return false;
            loadPos();
            setFillParent(false);
            Window.this.fire(WindowEvents.restoreSize);
            return true;
        }

        private boolean minimize() {
            if (minSized) return false;
            minSized = true;
            if (fillParent) {
                setFillParent(false);
            } else {
                savePos();
            }
            cont.visible = false;
            setHeight(32);
            setWidth(200);
            Window.this.fire(WindowEvents.minimize);
            return true;
        }

        private boolean cancelMinimize() {
            if (!minSized) return false;
            minSized = false;
            if (fillParent) {
                setFillParent(true);
            } else {
                loadPos();
            }
            cont.visible = true;
            Window.this.fire(WindowEvents.restoreSize);
            return true;
        }
    }
}

