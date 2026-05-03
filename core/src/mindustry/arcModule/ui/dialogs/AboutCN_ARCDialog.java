package mindustry.arcModule.ui.dialogs;

import arc.Core;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.arcModule.ARCVars;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.cleart;

public class AboutCN_ARCDialog extends BaseDialog {
    Seq<String> changelogs = new Seq<>();

    public AboutCN_ARCDialog() {
        super("@aboutcn_arc.button");

        shown(() -> {
            changelogs = Seq.with(Core.files.internal("arcchangelog").readString("UTF-8").split("\n"));
            Core.app.post(this::setup);
        });

        shown(this::setup);
        onResize(this::setup);

    }

    void setup() {
        cont.clear();
        buttons.clear();

        Table header = new Table();
        ScrollPane p = new ScrollPane(header);
        Table about = new Table();
        ScrollPane pane = new ScrollPane(about);


        header.add("[cyan]CN-ARC Client by PVP学术交流群");
        header.row();
        header.row();
        header.add("共同制作:");
        header.row();
        header.add("v3制作团队：[violet]Lucky_Clover[white]、blac8、[orange]squirrel[]、[blue]xkldklp[white]、[yellow]miner");
        header.row();
        header.add("主负责人：[violet]Lucky_Clover").row();
        header.add("公开发行版本：" + ARCVars.arcVersion).row();
        header.add("提议更新或反馈bug，请加入[yellow]猫猫的数据库[white]").row();
        header.add("讨论学术端更新|提议源码|共同编辑，请加入[yellow]mindustry PVP交流群[white]:931790051[orange]不欢迎萌新，仅限大佬加入").row();
        header.row();
        header.table(t -> {
            t.add("相关链接").height(30f);
            t.button("[cyan]更新日志", cleart, () -> {
                if (!Core.app.openURI("https://github.com/CN-ARC/Mindustry-CN-ARC")) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText("https://github.com/CN-ARC/Mindustry-CN-ARC");
                }
            }).width(100f).height(30f);
            t.button("[cyan]数据库1群", cleart, () -> {
                if (!Core.app.openURI("https://jq.qq.com/?_wv=1027&k=SIlwZafb")) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText("https://jq.qq.com/?_wv=1027&k=SIlwZafb");
                }
            }).width(100f).height(30f);
            t.button("[cyan]数据库2群", cleart, () -> {
                if (!Core.app.openURI("https://jq.qq.com/?_wv=1027&k=OTtencaZ")) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText("https://jq.qq.com/?_wv=1027&k=OTtencaZ");
                }
            }).width(100f).height(30f);
        });
        header.row();
        header.table(t -> {
            t.add("共同编辑").height(30f);
            t.button("[cyan]成就翻译", cleart, () -> {
                String link = "https://docs.qq.com/doc/DVHBUd3d4eWdNQWpo";
                if (!Core.app.openURI(link)) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText(link);
                }
            }).width(100f).height(30f);
            t.button("[cyan]功能建议", cleart, () -> {
                String link = "https://docs.qq.com/form/page/DTllxbXlCc0lJb1ps";
                if (!Core.app.openURI(link)) {
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText(link);
                }
            }).width(100f).height(30f);
        });
        cont.add(p).height(300).growX().pad(3);

        cont.row();
        for (String log : changelogs) {
            if (!log.startsWith("[cyan]请仔细阅读学术端")) {
                about.add(log).left();
            } else {
                about.add(log).left();
            }
            about.row();
        }
        cont.add(pane).grow().pad(3);
        cont.row();


        addCloseButton();
    }
}
