package mindustry.arcModule.ui.scratch;

import arc.graphics.Color;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class ScratchStyles {
    public static ImageButton.ImageButtonStyle flatImage;
    public static TextButton.TextButtonStyle flatText;
    public static Label.LabelStyle grayFont, grayOutline;
    public static TextField.TextFieldStyle clearField;
    public static void init() {
        flatImage = new ImageButton.ImageButtonStyle(Styles.cleari) {{
            up = Styles.black3;
            over = Styles.black6;
            down = Styles.black8;
            disabled = Styles.black9;
        }};
        flatText = new TextButton.TextButtonStyle(Styles.defaultt) {{
            up = Styles.black3;
            over = Styles.black6;
            down = Styles.black8;
            disabled = Styles.black9;
        }};
        grayFont = new Label.LabelStyle(Styles.defaultLabel) {{
            fontColor = Color.gray;
        }};
        grayOutline = new Label.LabelStyle(Styles.outlineLabel) {{
            fontColor = Color.gray;
        }};
        clearField = new TextField.TextFieldStyle(Styles.defaultField) {{
            focusedBackground = null;
            disabledBackground = null;
            background = null;
            invalidBackground = null;
            font = Fonts.outline;
            fontColor = Color.gray;
        }};
    }
}
