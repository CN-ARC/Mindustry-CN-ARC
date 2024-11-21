package mindustry.arcModule.ui.scratch.block;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.GL20;
import arc.graphics.Gl;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import mindustry.arcModule.ui.scratch.ScratchDraw;
import mindustry.arcModule.ui.scratch.ScratchType;

public class FakeBlock extends ScratchBlock {
    private ScratchBlock real;
    public FakeBlock() {
        super(ScratchType.fake, Color.black.cpy().mulA(0.3f), emptyInfo);
    }

    public void setReal(ScratchBlock real) {
        this.real = real;
    }

    @Override
    public float getWidth() {
        return real.getWidth();
    }

    @Override
    public float getHeight() {
        return real.getHeight();
    }

    @Override
    public float getPrefWidth() {
        return real.getPrefWidth();
    }

    @Override
    public float getPrefHeight() {
        return real.getPrefHeight();
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        return null;
    }

    @Override
    public void draw() {
        if (real instanceof TriggerBlock) {
            ScratchDraw.drawTriggerBlock(x, y, width, elemColor, true);
        } else {
            ScratchDraw.drawBlock(x, y, width, height, elemColor, true);
        }
    }
}
