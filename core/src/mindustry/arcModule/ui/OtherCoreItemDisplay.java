package mindustry.arcModule.ui;

import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.core.*;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;
import static mindustry.ui.Styles.*;

public class OtherCoreItemDisplay extends Table {
    public Seq<Teams.TeamData> teams = new Seq<>();

    private float lastUpd = 0f, fontScl = 0.8f;
    private boolean show = false, showStat = true, showItem = true, showUnit = true;

    private Table teamsTable;

    private TextButton.TextButtonStyle textStyle;


    public OtherCoreItemDisplay() {

        textStyle = new TextButton.TextButtonStyle(){{
            down = flatOver;
            up = pane;
            over = flatDownBase;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            checked = flatDown;
        }};

        teamsTable = new Table();
        rebuild();
    }

    void rebuild(){
        clear();
        if(!show){
        button("[red]+",textStyle, () -> {
            show = !show;
            rebuild();
        }).left().width(40f).fillY().get().left();}
        else{
            table(t -> {
                t.table(buttons -> {
                    buttons.button("[red]×",textStyle, () -> {
                        show = !show;
                        rebuild();
                    }).size(40f).row();

                    buttons.button("T",textStyle, () -> {
                        teamChangeMenu();
                    }).checked(gg->false).size(40f).row();

                    buttons.button(Blocks.worldProcessor.emoji(),textStyle, () -> {
                        showStat = !showStat;
                        teamsRebuild();
                    }).checked(a->showStat).size(40f).row();

                    buttons.button(content.items().get(0).emoji(),textStyle, () -> {
                        showItem = !showItem;
                        teamsRebuild();
                    }).checked(a->showItem).size(40f).row();
    
                    buttons.button(UnitTypes.mono.emoji(),textStyle, () -> {
                        showUnit = !showUnit;
                        teamsRebuild();
                    }).checked(a->showUnit).size(40f);
                }).left();
        
                teamsRebuild();
    
                t.add(teamsTable).left();
    
            }).left();
        }
    }

    private void teamChangeMenu(){
        BaseDialog dialog = new BaseDialog("队伍选择器");
        Table selectTeam = new Table().top();

        dialog.cont.pane(td -> {
            for(Team team : Team.all){
                if(team.id % 10 == 6){
                    td.row();
                    td.add("队伍：" + team.id + "~" + (team.id + 10));
                }
                ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
                button.getStyle().imageUpColor = team.color;
                button.margin(10f);
                button.resizeImage(40f);
                button.clicked(() -> {
                    if(teams.contains(team.data())) teams.remove(team.data());
                    else teams.add(team.data());
                    teamsRebuild();
                });
                button.update(() -> button.setChecked(teams.contains(team.data())));
                td.add(button);
            }
        });

        dialog.add(selectTeam).center();
        dialog.row();
        dialog.addCloseButton();
        dialog.show();
    }

    private void teamsRebuild(){
        teamsTable.clear();
        teamsTable.background(Styles.black6);
        teamsTable.update(() -> {
            if (Time.time - lastUpd > 120f){
                lastUpd = Time.time;
                teamsRebuild();
                return;
            }
        });
        teams.sort(teamData -> -teamData.cores.size);

        /**name + cores + units */
        teamsTable.label(() -> "").get().setFontScale(fontScl);
        for (Teams.TeamData team : teams) {
            if(team.team.id>6) teamsTable.label(() -> "[#" + team.team.color + "]#" + team.team.id).get().setFontScale(fontScl);
            else teamsTable.label(() -> "[#" + team.team.color + "]" + team.team.localized()).get().setFontScale(fontScl);
        }
        teamsTable.row();
        teamsTable.label(() -> Blocks.coreNucleus.emoji()).get().setFontScale(fontScl);
        for (Teams.TeamData team : teams) {
            teamsTable.label(() -> "[#" + team.team.color + "]" + UI.formatAmount(team.cores.size)).padRight(1).get().setFontScale(fontScl);
        }
        teamsTable.row();
        teamsTable.label(() -> UnitTypes.mono.emoji()).get().setFontScale(fontScl);
        for (Teams.TeamData team : teams) {
            teamsTable.label(() -> "[#" + team.team.color + "]" + UI.formatAmount(team.units.size)).padRight(1).get().setFontScale(fontScl);
        }
        teamsTable.row();
        teamsTable.label(() -> UnitTypes.gamma.emoji()).get().setFontScale(fontScl);
        for (Teams.TeamData team : teams) {
            teamsTable.label(() -> "[#" + team.team.color + "]" + team.players.size).padRight(1).get().setFontScale(fontScl);
        }
        teamsTable.row();

        if(showStat){
            teamsTable.image().color(getThemeColor()).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            teamsTable.image(Blocks.siliconSmelter.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + (state.rules.teams.get(team.team).cheat? "[green]+":"[red]×")).get().setFontScale(fontScl);
            }
            teamsTable.row();
            teamsTable.image(Blocks.arc.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + Strings.autoFixed(state.rules.teams.get(team.team).blockDamageMultiplier * state.rules.blockDamageMultiplier,2)).get().setFontScale(fontScl);
            }
            teamsTable.row();
            teamsTable.image(Blocks.titaniumWall.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + Strings.autoFixed(state.rules.teams.get(team.team).blockHealthMultiplier * state.rules.blockHealthMultiplier,2)).get().setFontScale(fontScl);
            }
            teamsTable.row();
            teamsTable.image(UnitTypes.corvus.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + Strings.autoFixed(state.rules.teams.get(team.team).unitDamageMultiplier * state.rules.unitDamageMultiplier,2)).get().setFontScale(fontScl);
            }
            teamsTable.row();
            teamsTable.image(UnitTypes.poly.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + Strings.autoFixed(state.rules.teams.get(team.team).buildSpeedMultiplier * state.rules.buildSpeedMultiplier,2)).get().setFontScale(fontScl);
            }
            teamsTable.row();
            teamsTable.image(Blocks.tetrativeReconstructor.uiIcon).size(15,15).left().get();
            for(Teams.TeamData team : teams){
                teamsTable.label(() -> "[#" + team.team.color + "]" + Strings.autoFixed(state.rules.teams.get(team.team).unitBuildSpeedMultiplier * state.rules.unitBuildSpeedMultiplier,2)).get().setFontScale(fontScl);
            }
            teamsTable.row();
        }

        if(showItem){
            teamsTable.image().color(getThemeColor()).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            boolean[] dispItems = new boolean[content.items().size];
            for(Item item : content.items()){            
                for(Teams.TeamData team : teams){
                    if(team.hasCore() && team.core().items.get(item) > 0) dispItems[content.items().indexOf(item)] = true;
                }
            }

            for(Item item : content.items()){            
                if(dispItems[content.items().indexOf(item)]){

                    //teamsTable.label(() -> item.emoji()).padRight(5f).left().get().setFontScale(fontScl);
                    teamsTable.image(item.uiIcon).size(15,15).left().get(); 
                    for(Teams.TeamData team : teams){
                        teamsTable.label(() -> "[#" + team.team.color + "]" + ((team.hasCore() && team.core().items.get(item) > 0) ? UI.formatAmount(team.core().items.get(item)) : "-")).get().setFontScale(fontScl);
                    }
                    teamsTable.row();

                }
            }
        }

        if(showUnit){
            teamsTable.image().color(getThemeColor()).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            boolean[] dispUnits = new boolean[content.units().size];
            for(UnitType unit : content.units()){            
                for(Teams.TeamData team : teams){
                    if(team.countType(unit) > 0) dispUnits[content.units().indexOf(unit)] = true;
                }
            }

            for(UnitType unit : content.units()){            
                if(dispUnits[content.units().indexOf(unit)]){

                    //teamsTable.label(() -> unit.emoji()).padRight(5f).left().get().setFontScale(fontScl);
                    teamsTable.image(unit.uiIcon).size(15,15).left().get(); 
                    for(Teams.TeamData team : teams){
                        teamsTable.label(() -> "[#" + team.team.color + "]" + (team.countType(unit) > 0 ? team.countType(unit) : "-")).get().setFontScale(fontScl);
                    }
                    teamsTable.row();

                }
            }
        }
    }

    public void updateTeamList() {
        teams = Vars.state.teams.getActive().copy();
    }

}