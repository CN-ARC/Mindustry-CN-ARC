package mindustry.arcModule.ui.quickTool;

import mindustry.arcModule.ElementUtils;
import mindustry.gen.Iconc;

import static mindustry.arcModule.ElementUtils.textStyle;

public class QuickToolTable extends ElementUtils.ToolTable {

    public HudSettingsTable hudSettingsTable = new HudSettingsTable();
    public AdvanceBuildTool advanceBuildTool = new AdvanceBuildTool();
    public ARCreeperTable ARCreeperTable = new ARCreeperTable();

    public QuickToolTable() {
        icon = String.valueOf(Iconc.list);
        rebuild();
        left();
    }

    @Override
    public void rebuild(){
        clear();
        table(t->{
            button((expand ? "":"[lightgray]") + icon, textStyle, () -> {
                expand = !expand;
                rebuild();
            }).right().size(40f);
        }).growX().row();
        if (expand) {
            buildTable();
        }
    }


    @Override
    protected void buildTable(){
        table(t -> {
            t.add(hudSettingsTable).growX().row();
            t.add(advanceBuildTool).growX().row();
            t.add(ARCreeperTable).growX();
        });
    }
}
