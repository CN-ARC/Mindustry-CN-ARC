package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.arcModule.ui.RStyles.*;
import static mindustry.content.UnitTypes.vela;

public class ScriptTable extends BaseToolsTable{
    private boolean boost = false;

    public ScriptTable(){
        super(UnitTypes.gamma.uiIcon);

        if(Vars.mobile){
            Events.run(EventType.Trigger.update, () -> {
                if(!Vars.player.dead() && Vars.player.unit().type.canBoost && boost){
                    Vars.player.boosting = true;
                }
            });
        }
    }

    @Override
    protected void setup(){
        defaults().size(40);

        scriptButton(Blocks.buildTower.uiIcon, "在建造列表加入被摧毁建筑", () -> Vars.player.buildDestroyedBlocks());

        scriptButton(Blocks.message.uiIcon, "锁定上个标记点", () -> {
            Marker.lockonLastMark();
        });

        scriptButton(Icon.modeAttack, "自动攻击", () -> {
            boolean at = Core.settings.getBool("autotarget");
            Core.settings.put("autotarget", !at);
        }, b -> Core.settings.getBool("autotarget"));


        scriptButton(vela.uiIcon, "助推", () -> boost = !boost).checked(boost);
    }

    private void scriptButton(Drawable region, String describe, Runnable runnable, Boolf<ImageButton> checked){
        scriptButton(region, clearLineNoneTogglei, describe, runnable).checked(checked);
    }

    private Cell<ImageButton> scriptButton(TextureRegion region, String describe, Runnable runnable){
        return scriptButton(new TextureRegionDrawable(region), clearLineNonei, describe, runnable);
    }

    private Cell<ImageButton> scriptButton(Drawable icon, ImageButtonStyle style, String describe, Runnable runnable){
        Cell<ImageButton> cell = button(icon, style, 30, runnable);

        ElementUtils.tooltip(cell.get(), describe);

        return cell;
    }

}
