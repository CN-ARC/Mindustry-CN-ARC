package mindustry.net;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.net.*;

import static mindustry.Vars.*;

/** Handles control of bleeding edge builds. */
public class BeControl{
    private static final int updateInterval = 60;

    private boolean checkUpdates = true;
    private boolean updateAvailable = false;
    private String updateUrl;
    private String steamUrl;
    private String mobileUrl;
    private int updateBuild;
    public static String gitDownloadURL = "https://gh.tinylake.tk//";
    private String directDesktopURL,directMobileURL,directSteamURL;

    private Table beTable,upTable;
    private TextField URLField;
    private TextField upField;
    private Label commitLabel;

    /** @return whether this is a bleeding edge build. */
    public boolean active(){
        return Version.type.equals("bleeding-edge");
    }

    public BeControl(){
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
        }
    }

    private void BeControlTable(){
        BaseDialog beDialog = new BaseDialog("自动更新设置");

        beDialog.cont.table(t -> {
            t.table(a->beTable = a);
            if(!mobile || Core.graphics.isPortrait()) {
                t.table().width(20f);
                t.pane(tt -> {
                    tt.add("更新日志").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
                    tt.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
                    commitLabel = tt.labelWrap("加载中...").width(500f).get();
                });
            }
        }
        );


        buildTable();

        beDialog.addCloseButton();

        beDialog.show();
        if(!mobile || Core.graphics.isPortrait())  getCommits();
    }

    private void buildTable(){
        beTable.clear();

        beTable.table(t->{
            t.add(updateAvailable?("[violet]！！！发现了新版本：" + updateBuild +"，你的版本为：" + arcVersion) : ("你已是最新版本，不需要更新！版本号" + arcVersion));
        });

        beTable.row();
        beTable.add("镜像设置").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
        beTable.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

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
                tt.button(Icon.refreshSmall,()->buildTable());
            });
            t.row();
            t.table(tt->{
                tt.button("wz镜像",()->{
                    gitDownloadURL = "https://gh.tinylake.tk//";
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
        if(!mobile || !Core.graphics.isPortrait()) {
            beTable.row();
            beTable.add("PC端").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            beTable.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

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
            beTable.add("steam端").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            beTable.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

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
        beTable.add("PE端").color(getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
        beTable.image().color(getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();

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
            done.get(false);
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
                directSteamURL = asset.getString("browser_download_url", "");
                steamUrl = gitDownloadURL + "/" + asset.getString("browser_download_url", "");

                Jval mobileAsset = val.get("assets").asArray().find(v -> v.getString("name", "").startsWith("Mindustry-CN-ARC-Android"));
                directMobileURL = mobileAsset.getString("browser_download_url", "");
                mobileUrl = gitDownloadURL + "/" + mobileAsset.getString("browser_download_url", "");

                Core.app.post(() -> {
                    BeControlTable();
                    done.get(true);
                });
            }else{
                Core.app.post(() -> {BeControlTable();done.get(false);});
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
                /*
                upTable.table(upt->{
                    upt.add(time).color(Color.valueOf("#008000")).width(270f).left();
                    upt.add(author).color(Color.valueOf("#1E90FF")).width(80f).padLeft(10f);
                }).fillX().row();
                upTable.add(content).color(Color.white).padBottom(3f).left();
                upTable.row();*/
            });
            commitLabel.setText(commits.toString());
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
