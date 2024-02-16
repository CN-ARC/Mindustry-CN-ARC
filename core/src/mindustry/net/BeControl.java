package mindustry.net;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.*;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.arcModule.ARCVars;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.io.SaveIO;
import mindustry.net.Administration.Config;
import mindustry.net.Packets.KickReason;
import mindustry.ui.Bar;
import mindustry.ui.dialogs.BaseDialog;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;

/** Handles control of bleeding edge builds. */
public class BeControl{
    private static final int updateInterval = 60;

    private boolean checkUpdates = true;
    private boolean updateAvailable = false;
    private String updateUrl;
    private String steamUrl;
    private String mobileUrl;
    private int updateBuild;
    public static String gitDownloadURL = "https://gh.tinylake.top/";

    private String patronURL = "https://afdian.net/a/Mindustry-CN-ARC";
    private String directDesktopURL,directMobileURL,directSteamURL;

    private Table beTable,upTable;
    private TextField URLField;
    private TextField upField;
    private Label commitLabel;

    /** @return whether this is a bleeding edge build. */
    public boolean active(){
        return false;
    }

    public BeControl(){/*
        if(Version.arcBuild != -1) checkUpdate(u -> {
            if(u && Core.settings.getBool("showUpdateDialog", true)) {
                Events.on(EventType.ClientLoadEvent.class, e -> {
                    ui.showConfirm("检测到新版学术!\n打开更新列表?", this::BeControlTable);
                    Timer.schedule(() -> arcui.LabelController.start("[violet]检测到新版学术!"), 5);
                });
            }
        });
        if(active()){
            Timer.schedule(() -> {
                if((Vars.clientLoaded || headless) && checkUpdates && !mobile){
                    checkUpdate(t -> {});
                }
            }, updateInterval, updateInterval);
        }

        if(OS.hasProp("becopy")){
            try{
                Fi dest = Fi.get(OS.prop("becopy"));
                Fi self = Fi.get(BeControl.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                
                for(Fi file : self.parent().findAll(f -> !f.equals(self))) file.delete();

                self.copyTo(dest);
            }catch(Throwable e){
                e.printStackTrace();
            }
        }*/
    }

    public void BeControlTable(){
        BaseDialog beDialog = new BaseDialog("自动更新设置");

        beDialog.cont.table(t -> {
            t.table(a->beTable = a);
            if(!mobile || Core.graphics.isPortrait()) {
                t.table().width(20f);
                t.pane(tt -> {
                    tt.add("更新日志").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
                    tt.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
                    commitLabel = tt.labelWrap("加载中...").width(500f).get();
                });
            }
        }
        );


        buildTable();

        beDialog.cont.row();
        beDialog.cont.button("支持作者",()->donateDialog()).padTop(10f).width(200f);

        beDialog.addCloseButton();

        beDialog.show();
        if(!mobile || Core.graphics.isPortrait())  getCommits();
    }

    private void donateDialog(){
        Dialog dl = new BaseDialog("学术支持板");
        dl.cont.table(t->{
            t.labelWrap("开发和运营我们的mdt社区需要资金，如果觉得对你有帮助的话，欢迎[orange]提供赞助[]啊~~\n\uE805 提供的赞助将作为[orange]学术端公共资金[] \uE805").width(400f).padBottom(10f).row();
            t.add("[violet]用途说明").padTop(10f).width(500f).row();
            t.image().color(Color.violet).width(500f).padTop(10f).padBottom(10f).row();
            t.labelWrap("\uE829 [acid]部分开发使用的商业软件及服务[]PS.如语音、字体包等，如atri包因为音质不好放弃了，需要更换较好的音质来源\n" +
                    "\uE829 [acid]pvp联赛策划及奖品[]PS.好吧我承认了，之前联赛因为赞助不够所以一直没啥实际奖品。如果赞助够的话会补发前两届的！款式我已经大概确定了，为定制的mdt周边\n" +
                    "\uE829 [acid]小型活动所需[]PS.如果有剩余的话，会举办赏金地图杯等小联赛\n" +
                    "\uE829 [acid]其他[]\nPS.如果金额较大会在群里说明\n\n" +
                    "如果赞助的金额较大也可获得mdt周边哦~").width(400f).left();
            t.row();
            t.add("[violet]支持说明").padTop(10f).width(500f).row();
            t.image().color(Color.violet).width(500f).padTop(10f).padBottom(10f).row();
            t.labelWrap(patronURL).width(400f).left();
            t.button("♐", () -> {
                if (!Core.app.openURI(patronURL)) {
                    ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                    Core.app.setClipboardText(patronURL);
                }
            }).width(50f);
        }).width(400f);
        dl.addCloseButton();
        dl.show();
    }

    private void buildTable(){
        beTable.clear();

        beTable.table(t->{
            t.add(updateAvailable?("[violet]！！！发现了新版本：" + updateBuild +"，你的版本为：" + ARCVars.arcVersion) : ("你已是最新版本，不需要更新！版本号" + ARCVars.arcVersion));
        });

        beTable.row();
        beTable.add("镜像设置").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
        beTable.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

        beTable.table(t->{
            t.add("下载镜像站，留空表示直连github(通常需要梯子)\n你可选择自行输入或者直接点下面的可选按钮\n输入完成后需要点击刷新按钮以更新");
            t.row();
            t.table(tt->{
                URLField = tt.field(gitDownloadURL, text->{
                    gitDownloadURL = text;
                    updateUrl = gitDownloadURL + directDesktopURL;
                    steamUrl = gitDownloadURL + directSteamURL;
                    mobileUrl = gitDownloadURL + directMobileURL;
                }).width(250f).get();
                tt.button(Icon.refreshSmall, this::buildTable);
            });
            t.row();
            t.table(tt->{
                tt.button("wz镜像",()->{
                    gitDownloadURL = "https://gh.tinylake.top/";
                    updateUrl = gitDownloadURL + directDesktopURL;
                    steamUrl = gitDownloadURL + directSteamURL;
                    mobileUrl = gitDownloadURL + directMobileURL;
                    buildTable();
                }).height(50f).width(150f);
                tt.button("A",()->{
                    gitDownloadURL = "https://gh.api.99988866.xyz/";
                    updateUrl = gitDownloadURL + directDesktopURL;
                    steamUrl = gitDownloadURL + directSteamURL;
                    mobileUrl = gitDownloadURL + directMobileURL;
                    buildTable();
                }).height(50f).width(50f);
                tt.button("B",()->{
                    gitDownloadURL = "https://ghproxy.com/";
                    updateUrl = gitDownloadURL + directDesktopURL;
                    steamUrl = gitDownloadURL + directSteamURL;
                    mobileUrl = gitDownloadURL + directMobileURL;
                    buildTable();
                }).height(50f).width(50f);
            });
        });
        if(!mobile || Core.graphics.isPortrait()) {
            beTable.row();
            beTable.add("PC端").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            beTable.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

            beTable.table(t -> {
                t.table(tt -> {
                    tt.field(updateUrl, text -> {
                        updateUrl = text;
                    }).width(300f);
                    tt.button("♐", () -> {
                        if (!Core.app.openURI(updateUrl)) {
                            ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                            Core.app.setClipboardText(updateUrl);
                        }
                    }).width(50f);
                });
                t.row();
                t.button("PC-自动下载安装", () -> {
                    boolean[] cancel = {false};
                    float[] progress = {0};
                    int[] length = {0};
                    Fi file = bebuildDirectory.child("Mindustry CN-ARC-" + updateBuild + ".jar");
                    Fi fileDest = OS.hasProp("becopy") ?
                            Fi.get(OS.prop("becopy")) :
                            Fi.get(BeControl.class.getProtectionDomain().getCodeSource().getLocation().getPath());

                    BaseDialog dialog = new BaseDialog("@be.updating");
                    download(updateUrl, file, i -> length[0] = i, v -> progress[0] = v, () -> cancel[0], () -> {
                        try {
                            Runtime.getRuntime().exec(OS.isMac ?
                                    new String[]{javaPath, "-XstartOnFirstThread", "-DlastBuild=" + Version.arcBuild, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath()} :
                                    new String[]{javaPath, "-DlastBuild=" + Version.arcBuild, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath()}
                            );
                            System.exit(0);
                        } catch (IOException e) {
                            ui.showException(e);
                        }
                    }, e -> {
                        dialog.hide();
                        ui.showException(e);
                    });

                    dialog.cont.add(new Bar(() -> length[0] == 0 ? Core.bundle.get("be.updating") : (int) (progress[0] * length[0]) / 1024 / 1024 + "/" + length[0] / 1024 / 1024 + " MB", () -> Pal.accent, () -> progress[0])).width(400f).height(70f);
                    dialog.buttons.button("@cancel", Icon.cancel, () -> {
                        cancel[0] = true;
                        dialog.hide();
                    }).size(210f, 64f);
                    dialog.setFillParent(false);
                    dialog.show();
                }).width(300f);
            });

            beTable.row();
            beTable.add("steam端").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            beTable.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

            beTable.table(t -> {
                t.table(tt -> {
                    tt.field(steamUrl, text -> {
                        steamUrl = text;
                    }).width(300f);
                    tt.button("♐", () -> {
                        if (!Core.app.openURI(steamUrl)) {
                            ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                            Core.app.setClipboardText(steamUrl);
                        }
                    }).width(50f);
                });
            });
        }
        else {
            beTable.row();
            beTable.add("检测到手机竖屏状态，已隐藏更新日志及其他端下载链接。\n如需显示请横屏后重新打开本窗口");
        }
        beTable.row();
        beTable.add("PE端").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
        beTable.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

        beTable.table(t->{
            t.table(tt->{
                tt.field(mobileUrl,text->{
                    mobileUrl = text;
                }).width(300f);
                tt.button("♐",()-> {
                    if(!Core.app.openURI(mobileUrl)){
                        ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                        Core.app.setClipboardText(mobileUrl);
                    }
                }).width(50f);
            });
        });
    }

    /** asynchronously checks for updates. */
    public void checkUpdate(Boolc done){
        Http.get("https://api.github.com/repos/Jackson11500/Mindustry-CN-ARC-Builds/releases/latest")
        .error(e -> {
            //don't log the error, as it would clog output if there is no internet. make sure it's handled to prevent infinite loading.
            Core.app.post(() -> done.get(false));
        })
        .submit(res -> {
            Jval val = Jval.read(res.getResultAsString());
            int newBuild = Strings.parseInt(val.getString("tag_name", "0"));
            if(newBuild > Version.arcBuild){
                Jval asset = val.get("assets").asArray().find(v -> v.getString("name", "").startsWith("Mindustry-CN-ARC-Desktop"));
                directDesktopURL = asset.getString("browser_download_url", "");
                String url = gitDownloadURL + "/" + asset.getString("browser_download_url", "");
                updateAvailable = true;
                updateBuild = newBuild;
                updateUrl = url;

                Jval steamAsset = val.get("assets").asArray().find(v -> v.getString("name", "").startsWith("Mindustry-CN-ARC-Steam"));
                directSteamURL = steamAsset.getString("browser_download_url", "");
                steamUrl = gitDownloadURL + "/" + asset.getString("browser_download_url", "");

                Jval mobileAsset = val.get("assets").asArray().find(v -> v.getString("name", "").startsWith("Mindustry-CN-ARC-Android"));
                directMobileURL = mobileAsset.getString("browser_download_url", "");
                mobileUrl = gitDownloadURL + "/" + mobileAsset.getString("browser_download_url", "");

                Core.app.post(() -> done.get(true));
            }else{
                Core.app.post(() -> done.get(false));
            }
        });
    }

    /** 加载commits */
    public void getCommits(){
        //upTable.clear();
        StringBuilder commits = new StringBuilder();
        Http.get("https://api.github.com/repos/CN-ARC/Mindustry-CN-ARC/commits").submit(res -> {
            Jval val = Jval.read(res.getResultAsString());
            Jval.JsonArray list =  val.asArray();
            
            // 抛回主线程处理提交
            Core.app.post(() -> {
                list.each(commit->{
                    String time = commit.get("commit").get("author").getString("date");
                    String author = commit.get("commit").get("author").getString("name");
                    String content = commit.get("commit").getString("message");
                    
                    commits.append("[#008000]").append(time);
                    for(int i=time.length();i<30;i++)
                        commits.append(" ");
                    commits.append("[#1E90FF]").append(author);
                    commits.append("\n");
                    commits.append("[white]").append(content);
                    commits.append("\n");
                });
                commitLabel.setText(commits.toString());
            });
        });
    }

    /** @return whether a new update is available */
    public boolean isUpdateAvailable(){
        return updateAvailable;
    }

    /** shows the dialog for updating the game on desktop, or a prompt for doing so on the server */
    public void showUpdateDialog(){
        if(!updateAvailable) return;

        if(!headless){
            checkUpdates = false;
            ui.showCustomConfirm("！！！发现了新版本：" + updateBuild, "@be.update.confirm", "@ok", "@be.ignore", () -> {
                try{
                    if(mobile){
                        if(!Core.app.openURI(mobileUrl)){
                            ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                            Core.app.setClipboardText(mobileUrl);
                        }
                        return;
                    }
                    else{
                        boolean[] cancel = {false};
                        float[] progress = {0};
                        int[] length = {0};
                        Fi file = bebuildDirectory.child("Mindustry CN-ARC-" + updateBuild + ".jar");
                        Fi fileDest = OS.hasProp("becopy") ?
                                Fi.get(OS.prop("becopy")) :
                                Fi.get(BeControl.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                        BaseDialog dialog = new BaseDialog("@be.updating");
                        download(updateUrl, file, i -> length[0] = i, v -> progress[0] = v, () -> cancel[0], () -> {
                            try{
                                Runtime.getRuntime().exec(OS.isMac ?
                                        new String[]{javaPath, "-XstartOnFirstThread", "-DlastBuild=" + Version.arcBuild, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath()} :
                                        new String[]{javaPath, "-DlastBuild=" + Version.arcBuild, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath()}
                                );
                                System.exit(0);
                            }catch(IOException e){
                                ui.showException(e);
                            }
                        }, e -> {
                            dialog.hide();
                            ui.showException(e);
                        });

                        dialog.cont.add(new Bar(() -> length[0] == 0 ? Core.bundle.get("be.updating") : (int)(progress[0] * length[0]) / 1024/ 1024 + "/" + length[0]/1024/1024 + " MB", () -> Pal.accent, () -> progress[0])).width(400f).height(70f);
                        dialog.buttons.button("@cancel", Icon.cancel, () -> {
                            cancel[0] = true;
                            dialog.hide();
                        }).size(210f, 64f);
                        dialog.setFillParent(false);
                        dialog.show();
                    }

                }catch(Exception e){
                    ui.showException(e);
                }
            }, () -> checkUpdates = false);
            }else{
                Log.info("&lcA new update is available: &lyBleeding Edge build @", updateBuild);
                if(Config.autoUpdate.bool()){
                    Log.info("&lcAuto-downloading next version...");

                    try{
                        //download new file from github
                        Fi source = Fi.get(BeControl.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                        Fi dest = source.sibling("server-be-" + updateBuild + ".jar");

                        download(updateUrl, dest,
                        len -> Core.app.post(() -> Log.info("&ly| Size: @ MB.", Strings.fixed((float)len / 1024 / 1024, 2))),
                        progress -> {},
                        () -> false,
                        () -> Core.app.post(() -> {
                            Log.info("&lcSaving...");
                            SaveIO.save(saveDirectory.child("autosavebe." + saveExtension));
                            Log.info("&lcAutosaved.");
                            netServer.kickAll(KickReason.serverRestarting);
                            Threads.sleep(32);
                            Log.info("&lcVersion downloaded, exiting. Note that if you are not using a auto-restart script, the server will not restart automatically.");
                            //replace old file with new
                            dest.copyTo(source);
                            dest.delete();
                            System.exit(2); //this will cause a restart if using the script
                        }),
                        Throwable::printStackTrace);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                checkUpdates = false;
            }

    }

    private void download(String furl, Fi dest, Intc length, Floatc progressor, Boolp canceled, Runnable done, Cons<Throwable> error){
        mainExecutor.submit(() -> {
            try{
                HttpURLConnection con = (HttpURLConnection)new URL(furl).openConnection();
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                OutputStream out = dest.write(false, 4096);

                byte[] data = new byte[4096];
                long size = con.getContentLength();
                long counter = 0;
                length.get((int)size);
                int x;
                while((x = in.read(data, 0, data.length)) >= 0 && !canceled.get()){
                    counter += x;
                    progressor.get((float)counter / (float)size);
                    out.write(data, 0, x);
                }
                out.close();
                in.close();
                if(!canceled.get()) done.run();
            }catch(Throwable e){
                error.get(e);
            }
        });
    }
}
