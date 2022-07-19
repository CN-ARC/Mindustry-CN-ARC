package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.arcModule.*;
import mindustry.arcModule.Marker.*;
import mindustry.arcModule.ui.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;

public class AuxilliaryTable extends Table{
    private boolean show = true;
    private boolean showMark = true;

    public MarkType markType = Marker.mark;
    public Element mobileHitter = new Element();

    private final BaseToolsTable[] toolsTables = new BaseToolsTable[]{
    new MapInfoTable(), new WaveInfoTable(), new AIToolsTable(), new ScriptTable(), new MobileToolTable()
    };

    public AuxilliaryTable(){
        setup();

        rebuild();

        mobileHitter.addListener(new ElementGestureListener(20, 0.4f, Marker.heatTime / 60f, 0.15f){
            @Override
            public boolean longPress(Element actor, float x, float y){
                Marker.mark(markType, Core.input.mouseWorld());

                mobileHitter.remove();

                return true;
            }

            @Override
            public void fling(InputEvent event, float velocityX, float velocityY, KeyCode button){
                mobileHitter.remove();
                ui.announce("[yellow]你已退出标记模式");
            }
        });

        mobileHitter.fillParent = true;

        if(!mobile){
            update(() -> {
                if(Core.input.keyTap(Binding.point) && !Core.scene.hasField()){
                    Marker.mark(markType, Core.input.mouseWorld());
                }
            });
        }
    }

    public void setup(){
        for(BaseToolsTable table : toolsTables){
            table.setup();
        }
    }

    public void toggle(){
        show = !show;
        rebuild();
    }

    void rebuild(){
        clear();

        table(Styles.black3, buttons -> {
            buttons.button("[acid]辅助器", clearLineNoneTogglet, this::toggle).size(80f, 40f).tooltip("关闭辅助器");

            if(show){
                for(BaseToolsTable table : toolsTables){
                    table.addButton(buttons);
                }
                buttons.button("♐",clearLineNoneTogglet,()->{showMark = !showMark;rebuild();}).checked(showMark).size(40f, 40f).tooltip("标记");
            }
        }).fillX();

        row();

        if(show){
            table(black1, body -> {
                for(BaseToolsTable table : toolsTables){
                    body.collapser(table, table::shown).padTop(3).left().row();
                }
                if(showMark){
                    body.table(t -> {
                        if(mobile){
                            t.button("♐ >", clearLineNonet, () -> {
                                ui.hudGroup.addChild(mobileHitter);
                                ui.announce("[cyan]你已进入标记模式,长按屏幕可进行一次标记(外划可以退出).");
                            }).height(40).width(70f).tooltip("开启手机标记");
                        }

                        for(MarkType type : Marker.markTypes){
                            t.button(type.tinyName(), clearLineNoneTogglet, () -> markType = type)
                                    .checked(b -> markType == type).size(40).tooltip(type.describe);
                        }
                        t.button("T", clearLineNoneTogglet, () -> Marker.teamMark = !Marker.teamMark)
                                .checked(b -> Marker.teamMark).size(40).tooltip("前缀添加/t");

                    }).left();
                }
            }).fillX().left();
        }
    }

}
