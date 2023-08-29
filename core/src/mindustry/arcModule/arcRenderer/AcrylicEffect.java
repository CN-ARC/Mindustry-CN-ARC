package mindustry.arcModule.arcRenderer;

import arc.fx.FxProcessor;
import arc.fx.filters.BlurFilter;
import arc.fx.filters.GaussianBlurFilter;
import arc.fx.util.PingPongBuffer;
import arc.graphics.Gl;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.FrameBuffer;

public class AcrylicEffect implements Effect {
    private final FrameBuffer fb = new FrameBuffer();
    private final FrameBuffer fb2 = new FrameBuffer();
    private final BlurFilter bf = new BlurFilter();
    private Pixmap pix;

    public AcrylicEffect() {
    }

    @Override
    public void draw(float x, float y, float w, float h) {
        Draw.flush();
        Draw.reset();
        int dx = (int) x, dy = (int) y, dw = (int) w, dh = (int) h;
        if (fb.getWidth() != (int) w || fb.getHeight() != (int) h) {
            fb.resize(dw, dh);
            fb2.resize(dw, dh);
            fb.resize(dw, dh);
            pix = new Pixmap(dw, dh);
        }
        fb.begin();
        Fill.crect(0, 0, w, h);
        fb.end();
        bf.render(fb, fb2);
        Draw.rect(new TextureRegion(fb2.getTexture()), x, y, w, h);
    }
}
