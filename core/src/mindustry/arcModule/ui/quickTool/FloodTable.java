package mindustry.arcModule.ui.quickTool;

import arc.struct.Seq;
import mindustry.arcModule.ElementUtils;
import mindustry.arcreeper.CreeperNet;
import mindustry.arcreeper.CreeperTile;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;

import static mindustry.arcModule.DrawUtilities.arcDrawText;
import static mindustry.arcModule.ElementUtils.NCtextStyle;
import static mindustry.arcreeper.CreeperTile.creeperDrawTrans;


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
                tt.button("[cyan]透", NCtextStyle, () -> {
                    creeperDrawTrans = !creeperDrawTrans;
                    rebuild();
                }).tooltip("[cyan]水层半透明").size(30f);
                tt.button("[blue]效", NCtextStyle, () -> {
                    CreeperTile.playCreeperFx = !CreeperTile.playCreeperFx;
                    rebuild();
                }).tooltip("[cyan]显示ARCreeper相关特效").size(30f);
                tt.button("[blue]高", NCtextStyle, () -> {
                    CreeperTile.drawTileHeight = !CreeperTile.drawTileHeight;
                    rebuild();
                }).tooltip("[cyan]显示ARCreeper地形高度").size(30f);

                tt.row();
                tt.button("[gray]网-", NCtextStyle, () -> {
                    CreeperNet.setBrush(CreeperNet.none);
                }).tooltip("[gray]删除Creeper网络").size(30f);
                tt.button("[lightgray]网0", NCtextStyle, () -> {
                    CreeperNet.setBrush(CreeperNet.inactive);
                }).tooltip("[lightgray]绘制未激活网络").size(30f);

                tt.button("[blue]网1", NCtextStyle, () -> {
                    CreeperNet.setBrush(CreeperNet.active);
                }).tooltip("[blue]绘制激活网络").size(30f);

                tt.button("[cyan]C口", NCtextStyle, () -> {
                    CreeperNet.setBrush(CreeperNet.outlet);
                }).tooltip("[cyan]绘制C喷口网络").size(30f);

                tt.button("[pink]AC口", NCtextStyle, () -> {
                    CreeperNet.setBrush(CreeperNet.antiOutlet);
                }).tooltip("[pink]绘制AC喷口网络").size(30f);
            });

        });
    }
}