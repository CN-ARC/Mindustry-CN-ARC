package mindustry.arcModule.ui.quickTool;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import mindustry.arcModule.ElementUtils;
import mindustry.arcreeper.CreeperTile;
import mindustry.game.EventType;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;

import static mindustry.arcModule.DrawUtilities.arcDrawText;
import static mindustry.arcModule.ElementUtils.NCtextStyle;


public class FloodTable extends ElementUtils.ToolTable {
    protected Seq<HudSettingsTable.Setting> list = new Seq<>();

    public FloodTable() {
        icon = String.valueOf(Iconc.itemSporePod);
        rebuild();
    }

    @Override
    protected void buildTable() {
        table(t -> {
            t.setBackground(Styles.black6);
            t.table(tt->{
                tt.button("[blue]显", NCtextStyle, () -> {
                    CreeperTile.creeperDrawType++;
                    CreeperTile.creeperDrawType %= 3;
                    rebuild();
                }).tooltip("[cyan]水层显示模式").size(30f);
                tt.button("[blue]效", NCtextStyle, () -> {
                    CreeperTile.playCreeperFx = !CreeperTile.playCreeperFx;
                    rebuild();
                }).tooltip("[cyan]显示ARCreeper相关特效").size(30f);
                tt.button("[blue]高", NCtextStyle, () -> {
                    CreeperTile.drawTileHeight = !CreeperTile.drawTileHeight;
                    rebuild();
                }).tooltip("[cyan]显示ARCreeper地形高度").size(30f);
            });

        });
    }
}