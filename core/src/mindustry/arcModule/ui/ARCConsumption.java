package mindustry.arcModule.ui;

import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.ui.Styles;

public class ARCConsumption extends Stack{

    public static Stack arcAdvancedStack(UnlockableContent item, int require, int obtained){
        return arcAdvancedStack(item.uiIcon, require, obtained, item);
    }

    /** Displays an item with a specified amount. */
    private static Stack arcAdvancedStack(TextureRegion region, int require, int obtained, @Nullable UnlockableContent content){
        Stack stack = new Stack();

        stack.add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f).scaling(Scaling.fit);
        }));

        if(require != 0){
            stack.add(new Table(t -> {
                t.left().bottom();
                t.add(require >= 1000 ? UI.formatAmount(require) : require + "").name("stack amount").style(Styles.outlineLabel).fontScale(0.7f);
                t.pack();
            }));
        }
        if(obtained != 0){
            stack.add(new Table(t -> {
                t.left().top();
                t.add(obtained >= 1000 ? UI.formatAmount(obtained) : obtained + "").name("stack amount").style(Styles.outlineLabel).fontScale(0.7f);
                t.pack();
            }));
        }

        return stack;
    }
}
