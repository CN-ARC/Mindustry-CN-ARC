package mindustry.ui;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.*;

public class GridImage extends Element{
    private int imageWidth, imageHeight;
    private static final Color gridLight = Color.valueOf("9ba3aa");
    private static final Color gridDark = Color.valueOf("59616a");

    public GridImage(int w, int h){
        this.imageWidth = w;
        this.imageHeight = h;
    }

    @Override
    public void draw(){
        float xspace = (getWidth() / imageWidth);
        float yspace = (getHeight() / imageHeight);
        float s = 1f;

        int minspace = 10;

        int jumpx = (int)(Math.max(minspace, xspace) / xspace);
        int jumpy = (int)(Math.max(minspace, yspace) / yspace);

        for(int x = 0; x <= imageWidth; x += jumpx){
            Fill.crect((int)(this.x + xspace * x - s), y - s, 2, getHeight() + (x == imageWidth ? 1 : 0));
        }

        for(int y = 0; y <= imageHeight; y += jumpy){
            Fill.crect(x - s, (int)(this.y + y * yspace - s), getWidth(), 2);
        }
    }

    /**
     * 编辑器专用彩色网格：
     * - 普通格线：浅灰 / 深灰交替
     * - 每 10 格：cyan / acid 交替
     */
    public void drawEditorGrid(){
        if(imageWidth <= 0 || imageHeight <= 0 || getWidth() <= 0f || getHeight() <= 0f){
            return;
        }

        final int minorStep = 10;
        final int majorStep = 100;
        final float xspace = getWidth() / imageWidth;
        final float yspace = getHeight() / imageHeight;
        final float s = 1f;
        final int minspace = 10;

        // 保留原 GridImage 的缩放降采样逻辑，避免缩小时线太密。
        final int jumpx = Math.max(1, (int)(Math.max(minspace, xspace) / xspace));
        final int jumpy = Math.max(1, (int)(Math.max(minspace, yspace) / yspace));

        // ---------- 普通竖线：浅灰 / 深灰交替 ----------
        for(int gx = 0; gx <= imageWidth; gx += jumpx){
            // 主线后续单独画，避免先被灰线覆盖。
            if(gx % minorStep == 0) continue;

            Draw.color(((gx / jumpx) & 1) == 0 ? gridLight : gridDark);

            Fill.crect(
                    (int)(this.x + xspace * gx - s),
                    this.y - s,
                    2f,
                    getHeight() + (gx == imageWidth ? 1f : 0f)
            );
        }

        // ---------- 普通横线：浅灰 / 深灰交替 ----------
        for(int gy = 0; gy <= imageHeight; gy += jumpy){
            if(gy % minorStep == 0) continue;

            Draw.color(((gy / jumpy) & 1) == 0 ? gridLight : gridDark);

            Fill.crect(
                    this.x - s,
                    (int)(this.y + yspace * gy - s),
                    getWidth(),
                    2f
            );
        }

        // ---------- 每 10 格的竖向主线：cyan / acid 交替 ----------
        // 0 格为 cyan，10 格为 acid，20 格重新 cyan。
        for(int gx = 0; gx <= imageWidth; gx += minorStep){
            Draw.color(((gx / minorStep) & 1) == 0 ? Color.cyan : Color.acid);
            if (gx % majorStep !=0 ) Draw.alpha(0.8f);

            Fill.crect(
                    (int)(this.x + xspace * gx - s),
                    this.y - s,
                    2f,
                    getHeight() + (gx == imageWidth ? 1f : 0f)
            );
        }

        // ---------- 每 10 格的横向主线：cyan / acid 交替 ----------
        for(int gy = 0; gy <= imageHeight; gy += minorStep){
            Draw.color(((gy / minorStep) & 1) == 0 ? Color.cyan : Color.acid);
            if (gy % majorStep !=0 ) Draw.alpha(0.8f);

            Fill.crect(
                    this.x - s,
                    (int)(this.y + yspace * gy - s),
                    getWidth(),
                    2f
            );
        }

        Draw.reset();
    }

    public void setImageSize(int w, int h){
        this.imageWidth = w;
        this.imageHeight = h;
    }
}
