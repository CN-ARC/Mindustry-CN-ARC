package mindustry.arcModule.ui.scratch;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;

public class ScratchStyles {
    public static final Color selectedColor = Color.gold;
    public static void drawInput(float x, float y, float w, float h, Color c, boolean selected) {
        Color c1 = selected ? selectedColor : Tmp.c1.set(c).lerp(Color.black, 0.3f).cpy();
        Lines.stroke(selected ? 2 : 1);
        float halfH = h / 2;
        Draw.color(c);
        Fill.circle(x + halfH, y + halfH, halfH);
        Fill.circle(x + w - halfH, y + halfH, halfH);
        Draw.color(c1);
        Lines.circle(x + halfH, y + halfH, halfH);
        Lines.circle(x + w - halfH, y + halfH, halfH);
        Draw.color(c);
        Fill.rect(x + w / 2, y + halfH, w - 2 * halfH, h);
        Draw.color(c1);
        float ty = selected ? y : y + 1;
        Lines.line(x + halfH, ty, x + w - halfH, ty);
        Lines.line(x + halfH, y + h, x + w - halfH, y + h);
    }

    public static void drawInputSelected(float x, float y, float w, float h) {
        float halfH = h / 2;
        Draw.color(selectedColor);
        Lines.stroke(3);
        Lines.circle(x + halfH, y + halfH, halfH);
        Lines.circle(x + w - halfH, y + halfH, halfH);
        Lines.line(x + halfH, y + 1, x + w - halfH, y + 1);
        Lines.line(x + halfH, y + h, x + w - halfH, y + h);
    }

    public static void drawCond(float x, float y, float w, float h, Color c, boolean selected) {
        Draw.color(c);
        float halfH = h / 2;
        Fill.tri(x + halfH, y, x, y + halfH, x + halfH, y + h);
        Fill.tri(x + w - halfH, y, x + w, y + halfH, x + w - halfH, y + h);
        Fill.rect(x + w / 2, y + halfH, w - 2 * halfH, h);
        Draw.color(selected ? selectedColor : Tmp.c1.set(c).lerp(Color.black, 0.3f));
        Lines.stroke(selected ? 2 : 1);
        condPoint(x, y, w, h, halfH);
        Lines.endLine(true);
    }

    public static void drawCondSelected(float x, float y, float w, float h) {
        Draw.color(selectedColor);
        float halfH = h / 2;
        Lines.stroke(2);
        condPoint(x, y, w, h, halfH);
        Lines.endLine();
        Lines.line(x + halfH + 1, y - 1, x + w - halfH - 1, y - 1);
    }

    private static void condPoint(float x, float y, float w, float h, float halfH) {
        Lines.beginLine();
        Lines.linePoint(x + halfH, y);
        Lines.linePoint(x, y + halfH);
        Lines.linePoint(x + halfH, y + h);
        Lines.linePoint(x + w - halfH, y + h);
        Lines.linePoint(x + w, y + halfH);
        Lines.linePoint(x + w - halfH, y);
    }

    public static void drawBlock(float x, float y, float w, float h, Color c, boolean noBorder) {
        drawBlockHeader(x, y, w, h, c);
        drawBlockInner(x, y, w, h);
        if (!noBorder) drawBlockBorder(x, y, w, h, c);
    }

    public static void drawBlockInner(float x, float y, float w, float h) {
        Fill.polyBegin();
        Fill.polyPoint(x, y + h - 7);
        Fill.polyPoint(x + w, y + h - 7);
        Fill.polyPoint(x + w, y);
        Fill.polyPoint(x + 35, y);
        Fill.polyPoint(x + 30, y - 7);
        Fill.polyPoint(x + 15, y - 7);
        Fill.polyPoint(x + 10, y);
        Fill.polyPoint(x, y);
        Fill.polyEnd();
    }

    public static void drawBlockHeader(float x, float y, float w, float h, Color c) {
        Draw.color(c);
        Fill.quad(x, y + h, x + 10, y + h, x + 15, y + h - 7, x, y + h - 7);
        Fill.quad(x + 30, y + h - 7, x + 35, y + h, x + w, y + h, x + w, y + h - 7);
    }

    public static void drawBlockBorder(float x, float y, float w, float h, Color c) {
        Lines.stroke(1);
        Draw.color(Tmp.c1.set(c).lerp(Color.black, 0.3f));
        Lines.beginLine();
        drawBlockBorderTop(x, y, w, h);
        drawBlockBorderBottom(x, y);
        Lines.linePoint(x, y + h);
        Lines.endLine();
    }

    public static void drawBlockBorderTop(float x, float y, float w, float h) {
        Lines.linePoint(x, y + h);
        Lines.linePoint(x + 10, y + h);
        Lines.linePoint(x + 15, y + h - 7);
        Lines.linePoint(x + 30, y + h - 7);
        Lines.linePoint(x + 35, y + h);
        Lines.linePoint(x + w, y + h);
        Lines.linePoint(x + w, y);
    }

    public static void drawBlockBorderBottom(float x, float y) {
        Lines.linePoint(x + 35, y);
        Lines.linePoint(x + 30, y - 7);
        Lines.linePoint(x + 15, y - 7);
        Lines.linePoint(x + 10, y);
        Lines.linePoint(x, y);
    }

    public static void drawBorderBottom(float x, float y, float w) {
        Lines.line(x + w, y, x + 35, y);
        Lines.line(x + 35, y, x + 30, y - 7);
        Lines.line(x + 30, y - 7, x + 15, y - 7);
        Lines.line(x + 15, y - 7, x + 10, y);
        Lines.line(x + 10, y, x, y);
    }
}
