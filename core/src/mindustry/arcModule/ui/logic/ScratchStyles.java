package mindustry.arcModule.ui.logic;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.*;
import arc.math.Mat;
import arc.math.Mathf;
import arc.scene.style.NinePatchDrawable;
import arc.struct.FloatSeq;
import arc.util.Tmp;

public class ScratchStyles {
    private static final int quality = 256;
    private static final int lineColor = Color.green.rgba8888();//new Color(0, 0, 0, 0.7f).rgba8888();

    public static NinePatchDrawable createSwitchBackground(int left, int top, int bottom, Color c) {
        Pixmap p = new Pixmap(quality, quality);
        p.fill(lineColor);
        int color = c.rgba8888();
        p.fillRect(1, 1, quality - 1 - 1, quality - 1 - 1, color);
        p.fillRect(left - 1, bottom - 1, quality - left + 1, quality - top - bottom + 2, lineColor);
        p.fillRect(left, bottom, quality - left, quality - top - bottom, 0);
        NinePatch n = new NinePatch(new TextureRegion(new Texture(p)), left, 2, top, bottom);
        NinePatchDrawable d = new NinePatchDrawable(n);
        p.dispose();
        return d;
    }

    public static void drawInput(float x, float y, float w, float h, Color c) {
        Draw.color(c);
        float halfH = h / 2;
        Fill.circle(x + halfH, y + halfH, halfH);
        Fill.circle(x + w - halfH, y + halfH, halfH);
        Draw.color(Tmp.c1.set(c).lerp(Color.black, 0.3f));
        Lines.circle(x + halfH, y + halfH, halfH);
        Lines.circle(x + w - halfH, y + halfH, halfH);
        Draw.color(c);
        Fill.rect(x + w / 2, y + halfH, w - 2 * halfH, h);
        Draw.color(Tmp.c1.set(c).lerp(Color.black, 0.3f));
        Lines.line(x + halfH, y + 1, x + w - halfH, y + 1);
        Lines.line(x + halfH, y + h, x + w - halfH, y + h);
    }

    public static boolean withinInput(float x, float y, float w, float h, float x2, float y2) {
        float halfH = h / 2;
        return Mathf.within(x + halfH, y + halfH, x2, y2, halfH) ||
                Mathf.within(x + w - halfH, y + halfH, x2, y2, halfH) ||
                x2 > x + w / 2 && x2 < x + w - 2 * halfH && y2 > y + halfH && y2 < y + h;
    }

    public static void drawCond(float x, float y, float w, float h, Color c) {
        Draw.color(c);
        float halfH = h / 2;
        Fill.tri(x + halfH, y, x, y + halfH, x + halfH, y + h);
        Fill.tri(x + w - halfH, y, x + w, y + halfH, x + w - halfH, y + h);
        Fill.rect(x + w / 2, y + halfH, w - 2 * halfH, h);
        Draw.color(Tmp.c1.set(c).lerp(Color.black, 0.3f));
        Lines.beginLine();
        Lines.linePoint(x + halfH, y);
        Lines.linePoint(x, y + halfH);
        Lines.linePoint(x + halfH, y + h);
        Lines.linePoint(x + w - halfH, y + h);
        Lines.linePoint(x + w, y + halfH);
        Lines.linePoint(x + w - halfH, y);
        Lines.endLine();
    }
}
