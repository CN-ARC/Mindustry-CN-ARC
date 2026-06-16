package mindustry.arcModule;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.Radar;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.line;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.*;

public class DrawUtilities {
    private static Vec2 vector = new Vec2();

    private static int arcCircleIndex = 0;
    static float iconSize = 0;

    private static final Effect effect120 = createBuildEffect(120f), effect180 = createBuildEffect(180f);

    public static float arcDrawText(String text, float scl, float dx, float dy, Color color, int halign) {
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(scl);
        layout.setText(font, text);

        float height = layout.height;

        font.setColor(color);
        font.draw(text, dx, dy + layout.height + 1, halign);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);

        return height;
    }

    public static float arcDrawText(String text, float scl, float dx, float dy, int halign) {
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(scl);
        layout.setText(font, text);

        float height = layout.height;

        font.draw(text, dx + layout.width / 2, dy + layout.height / 2, halign);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);

        return height;
    }

    private void arcFillTextMain(String text, float x1, float y1, float x2, float y2, float ratio) {

        Color color = ARCVars.getThemeColor();
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(Math.abs(x2 - x1) / text.length() / 2f / Scl.scl(1f) * ratio);
        layout.setText(font, text);

        font.setColor(color);
        float cx = (x1 + x2) / 2 * tilesize, cy = (y1 + y2) / 2 * tilesize;
        font.draw(text, cx, cy, Align.center);
        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }

    public static void arcFillTextHead(String text, float x1, float x2, float y, float ratio) {

        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(Math.abs(x2 - x1) / Math.max(text.length(), 1f) / 2f / Scl.scl(1f) * ratio);
        layout.setText(font, text);

        //font.setColor(Color.goldenrod);
        float cx = (x1 + x2) / 2 * tilesize, cy = y * tilesize - 5f;
        font.draw(text, cx, cy, Align.center);
        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }

    public static void arcDrawTextMain(String text, int x, int y) {

        Color color = ARCVars.getThemeColor();
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f / 3f / Scl.scl(1f));
        layout.setText(font, text);

        font.setColor(color);
        float dx = x * tilesize, dy = y * tilesize;
        font.draw(text, dx, dy + layout.height + 1, Align.center);
        dy -= 1f;
        Lines.stroke(2f, Color.darkGray);
        line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 2f, dy);
        Lines.stroke(1f, color);
        line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 2f, dy);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }

    public static void arcBuildEffect(Building build) {
        arcBuildEffect(build, build.x, build.y);
    }

    public static void arcBuildEffect(Building build, float x, float y) {
        if (build == null || !Core.settings.getBool("arcPlacementEffect")) return;
        if (build.block instanceof BaseTurret baseTurret && build.health > Core.settings.getInt("blockbarminhealth"))
            effect120.at(x, y, 0, new ARCEffectData(baseTurret.range, build.team.color));
        else if (build.block instanceof MendProjector mendProjector)
            effect120.at(x, y, 0, new ARCEffectData(mendProjector.range, Pal.heal));
        else if (build.block instanceof OverdriveProjector overdriveProjector)
            effect120.at(x, y, 0, new ARCEffectData(overdriveProjector.range, overdriveProjector.baseColor));
        else if (build.block instanceof Radar radar)
            effect120.at(x, y, 0, new ARCEffectData(radar.fogRadius * tilesize, build.team.color));
        else if (build.block instanceof LogicBlock logic)
            effect120.at(x, y, 0, new ARCEffectData(logic.range, logic.mapColor));
        else if (build.block instanceof CoreBlock block)
            effect180.at(x, y, 0, new ARCEffectData(block.fogRadius * tilesize, build.team.color));
    }

    public static class ARCEffectData {
        public final float range;
        public final Color color;

        public ARCEffectData(float range, Color color) {
            this.range = range;
            this.color = color;
        }
    }

    public static Effect createBuildEffect(float lifeTime) {
        return new Effect(lifeTime, e -> {
            ARCEffectData d = e.data();
            color(d.color);
            stroke((1.5f - e.fin()) * (d.range / 100));
            if (e.fin() < 0.7f) Lines.circle(e.x, e.y, (float) ((1 - Math.pow((0.7f - e.fin()) / 0.7f, 2f)) * d.range));
            else {
                Draw.alpha((1 - e.fin()) * 5f);
                Lines.circle(e.x, e.y, d.range);
            }
        });
    }

    public static void arcDashCircling(float x, float y, float radius) {
        arcDashCircling(x, y, radius, 0.5f);
    }

    public static void arcDashCircling(float x, float y, float radius, float speed) {
        arcDashCircle(x, y, radius, Time.time * speed);
    }

    public static void arcDashCircle(float x, float y, float radius) {
        arcDashCircle(x, y, radius, 0);
    }

    public static void arcDashCircle(float x, float y, float radius, float rotation) {
        float scaleFactor = 0.6f;
        int sides = 10 + (int) (radius * scaleFactor);
        if (sides % 2 == 1) sides++;

        vector.set(0, 0);

        for (int i = 0; i < sides; i += 2) {
            vector.set(radius, 0).rotate(360f / sides * i + 90 + rotation);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).rotate(360f / sides * (i + 1) + 90 + rotation);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void drawNSideRegion(float x, float y, int n, float range, float rotation, Color color, float fraction, TextureRegion region, boolean regionColor) {
        Draw.z(Layer.effect - 2f);
        Draw.color(color);

        Lines.stroke(2f);

        for (int i = 0; i < n; i++) {
            float frac = 360f * (1 - fraction * n) / n / 2;
            float rot = rotation + i * 360f / n + frac;
            if (!regionColor) {
                Draw.color(color);
                Lines.arc(x, y, range, 0.25f, rot, (int) (50 + range / 10));
                Draw.color();
            } else {
                Lines.arc(x, y, range, 0.25f, rot, (int) (50 + range / 10));
            }
            Draw.rect(region, x + range * Mathf.cos((float) Math.toRadians(rot - frac)), y + range * Mathf.sin((float) Math.toRadians(rot - frac)), 12f, 12f);
        }
        Draw.reset();
    }

    public static void arcAuxiliaryLine(float x, float y, float x2, float y2){
        arcAuxiliaryLine(x, y, x2, y2, 50f, 5f);
    }

    public static void arcAuxiliaryLine(float x, float y, float x2, float y2, float extendTiles, float dashTiles){
        float extend = extendTiles * tilesize;
        float dash = dashTiles * tilesize;

        // 下边：向左、向右延长
        arcAuxiliaryDash(x, y, -1f, 0f, extend, dash);
        arcAuxiliaryDash(x2, y, 1f, 0f, extend, dash);

        // 上边：向左、向右延长
        arcAuxiliaryDash(x, y2, -1f, 0f, extend, dash);
        arcAuxiliaryDash(x2, y2, 1f, 0f, extend, dash);

        // 左边：向下、向上延长
        arcAuxiliaryDash(x, y, 0f, -1f, extend, dash);
        arcAuxiliaryDash(x, y2, 0f, 1f, extend, dash);

        // 右边：向下、向上延长
        arcAuxiliaryDash(x2, y, 0f, -1f, extend, dash);
        arcAuxiliaryDash(x2, y2, 0f, 1f, extend, dash);

        // 四角
        arcAuxiliaryDash(x, y, -1f, -1f, extend, dash);
        arcAuxiliaryDash(x, y2, -1f, 1f, extend, dash);
        arcAuxiliaryDash(x2, y, 1f, -1f, extend, dash);
        arcAuxiliaryDash(x2, y2, 1f, 1f, extend, dash);
    }

    private static void arcAuxiliaryDash(float x, float y, float dx, float dy, float extend, float dash){
        if(extend <= 0f || dash <= 0f) return;

        float interval = dash * 2f;

        float phase = (Time.time / 5f) % interval;

        for(float end = phase; end <= extend + dash; end += interval){
            float start = end - dash;

            float drawStart = Math.max(start, 0f);
            float drawEnd = Math.min(end, extend);

            if(drawEnd <= drawStart) continue;

            line(
                    x + dx * drawStart,
                    y + dy * drawStart,
                    x + dx * drawEnd,
                    y + dy * drawEnd
            );
        }
    }

    /*
    public static void arcRegionCircle(float x, float y, float offset, float range, int size, Seq<TextureRegion> textureRegionSeq){
        if (textureRegionSeq.size == 0) return;
        arcCircleIndex = 0;
        iconSize = 6f + 2f * size;
        textureRegionSeq.each(textureRegion -> {
            arcCircleIndex += 1;
            for (int i = 0; i < 4; i++) {
                float rot = (i + ((float) arcCircleIndex) / textureRegionSeq.size) / 4 * 360f + Time.time * 0.5f;
                Draw.rect(textureRegion,
                        x + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                        y + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + BulletType.rangeChange + iconSize + 1f)),
                        iconSize, iconSize, -rot);
            }
        });
    }*/
}
