package mindustry.arcreeper;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.net.*;

import static mindustry.Vars.*;

public class updateARCreeperDialog extends BaseDialog{
    private static final String mapsPageUrl = "https://github.com/CN-ARC/ARCreeper_CP/tree/main/maps";
    private static final String apiBaseUrl = "https://api.github.com/repos/CN-ARC/ARCreeper_CP";
    private static final String mapsApiUrl = apiBaseUrl + "/contents/maps?ref=main";
    private static final String contentsApiPrefix = apiBaseUrl + "/contents/";
    private static final String commitsApiPrefix = apiBaseUrl + "/commits?path=";
    private static final String githubApiVersion = "2022-11-28";

    private final Runnable refreshMaps;
    private final Seq<RemoteMap> remoteMaps = new Seq<>();
    private final Table list = new Table();

    private Label status;
    private boolean busy = false;

    public updateARCreeperDialog(Runnable refreshMaps){
        super("更新ARCreeperMaps");
        this.refreshMaps = refreshMaps;

        build();
        addCloseButton();

        loadRemoteMaps();
    }

    private void build(){
        cont.clear();

        cont.table(top -> {
            top.defaults().height(54f).pad(4f);

            top.button("打开网站", Icon.link, () -> Core.app.openURI(mapsPageUrl))
                    .width(210f);

            top.button("一键更新地图", Icon.upload, this::downloadAll)
                    .width(240f);
        }).growX();

        cont.row();

        status = new Label("正在读取 ARCreeper_CP/maps ...");
        status.setWrap(true);
        cont.add(status).growX().pad(8f);

        cont.row();

        list.margin(8f);

        ScrollPane pane = new ScrollPane(list);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabledX(true);

        cont.add(pane)
                .width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) - 80f, 920f))
                .height(Core.graphics.isPortrait() ? 430f : 520f)
                .pad(8f);

        rebuildList();
    }

    private void loadRemoteMaps(){
        setStatus("正在读取：" + mapsPageUrl);

        Http.get(mapsApiUrl)
                .timeout(20000)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", githubApiVersion)
                .header("User-Agent", "Mindustry-ARCreeper")
                .error(e -> Core.app.post(() -> {
                    Log.err(e);
                    setStatus("[scarlet]读取地图列表失败：[] " + e.getMessage());
                    rebuildList();
                }))
                .submit(res -> {
                    Seq<RemoteMap> result = new Seq<>();

                    try{
                        Jval json = Jval.read(res.getResultAsString());

                        for(Jval item : json.asArray()){
                            String type = item.getString("type", "");
                            String name = item.getString("name", "");
                            String path = item.getString("path", "maps/" + name);
                            String htmlUrl = item.getString("html_url", "");
                            String rawDownloadUrl = item.getString("download_url", "");

                            if(!"file".equals(type)) continue;
                            if(!name.endsWith("." + mapExtension)) continue;

                            result.add(new RemoteMap(name, path, contentsRawApiUrl(path), rawDownloadUrl, htmlUrl));
                        }

                        result.sortComparing(RemoteMap::displayName);

                        Core.app.post(() -> {
                            remoteMaps.clear();
                            remoteMaps.addAll(result);

                            setStatus(remoteMaps.isEmpty() ? "[yellow]没有读取到地图文件。" : "已读取到 " + remoteMaps.size + " 张地图。");
                            rebuildList();

                            loadUpdateTimes(result, 0);
                        });
                    }catch(Throwable e){
                        Core.app.post(() -> {
                            Log.err(e);
                            setStatus("[scarlet]解析地图列表失败：[] " + e.getMessage());
                            rebuildList();
                        });
                    }
                });
    }

    private void loadUpdateTimes(Seq<RemoteMap> maps, int index){
        if(index >= maps.size){
            return;
        }

        RemoteMap map = maps.get(index);
        String url = commitsApiPrefix + encodeQueryValue(map.path) + "&per_page=1";

        Http.get(url)
                .timeout(15000)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", githubApiVersion)
                .header("User-Agent", "Mindustry-ARCreeper")
                .error(e -> Core.app.post(() -> {
                    map.updated = "读取失败";
                    rebuildList();
                    loadUpdateTimes(maps, index + 1);
                }))
                .submit(res -> {
                    String updated;

                    try{
                        Jval json = Jval.read(res.getResultAsString());

                        if(json.asArray().isEmpty()){
                            updated = "未知";
                        }else{
                            Jval first = json.asArray().first();
                            String date = first.get("commit").get("committer").getString("date", "");
                            updated = formatDate(date);
                        }
                    }catch(Throwable e){
                        updated = "读取失败";
                    }

                    String finalUpdated = updated;
                    Core.app.post(() -> {
                        map.updated = finalUpdated;
                        rebuildList();
                        loadUpdateTimes(maps, index + 1);
                    });
                });
    }

    private void rebuildList(){
        list.clear();
        list.defaults().pad(6f);

        list.add("[accent]地图名").left().growX();
        list.add("[accent]更新时间").left().width(190f);
        list.add("[accent]下载").width(130f);
        list.row();

        if(remoteMaps.isEmpty()){
            list.add("暂无地图，或正在读取中。").colspan(3).padTop(24f);
            return;
        }

        for(RemoteMap map : remoteMaps){
            list.add(map.displayName()).left().growX().wrap().width(480f);
            list.add(map.updated).left().width(190f);

            list.button("下载", Icon.upload, () -> downloadOne(map))
                    .width(130f)
                    .height(46f)
                    .disabled(busy);

            list.row();
        }
    }

    private void downloadOne(RemoteMap map){
        if(busy){
            ui.showInfo("已有地图正在下载/导入。");
            return;
        }

        busy = true;
        rebuildList();
        setStatus("正在下载：" + map.displayName());

        downloadRemoteMap(map, file -> {
            Core.app.post(() -> ui.loadAnd(() -> {
                try{
                    importDownloadedMap(file);
                    refreshMaps.run();

                    busy = false;
                    setStatus("已更新：" + map.displayName());
                    rebuildList();
                    ui.showInfo("地图已更新：" + map.displayName());
                }catch(Throwable e){
                    busy = false;
                    Log.err(e);
                    setStatus("[scarlet]导入失败：[] " + map.displayName() + "：" + e.getMessage());
                    rebuildList();
                    ui.showException("导入 ARCreeper 地图失败", e);
                }
            }));
        }, e -> Core.app.post(() -> {
            busy = false;
            Log.err(e);
            setStatus("[scarlet]下载失败：[] " + map.displayName() + "：" + e.getMessage());
            rebuildList();
            ui.showException("下载 ARCreeper 地图失败", e);
        }));
    }

    private void downloadAll(){
        if(busy){
            ui.showInfo("已有地图正在下载/导入。");
            return;
        }

        if(remoteMaps.isEmpty()){
            ui.showInfo("还没有读取到可下载的地图。");
            return;
        }

        ui.showConfirm("@confirm", "将下载并导入全部 ARCreeper_CP/maps 地图；如果本地已有同名地图，会直接替换。", () -> {
            busy = true;
            rebuildList();
            setStatus("正在下载全部地图...");

            Seq<RemoteMap> mapsToDownload = remoteMaps.copy();
            Seq<Fi> downloaded = new Seq<>();

            downloadNext(mapsToDownload, downloaded, 0);
        });
    }

    private void downloadNext(Seq<RemoteMap> mapsToDownload, Seq<Fi> downloaded, int index){
        if(index >= mapsToDownload.size){
            Core.app.post(() -> ui.loadAnd(() -> {
                int imported = 0;

                try{
                    for(Fi file : downloaded){
                        importDownloadedMap(file);
                        imported++;
                    }

                    refreshMaps.run();

                    busy = false;
                    setStatus("全部更新完成，共导入 " + imported + " 张地图。");
                    rebuildList();
                    ui.showInfo("ARCreeperMaps 更新完成，共导入 " + imported + " 张地图。");
                }catch(Throwable e){
                    busy = false;
                    Log.err(e);
                    setStatus("[scarlet]导入全部地图失败：[] " + e.getMessage());
                    rebuildList();
                    ui.showException("导入 ARCreeper 地图失败", e);
                }
            }));

            return;
        }

        RemoteMap map = mapsToDownload.get(index);
        setStatusPost("正在下载：" + (index + 1) + "/" + mapsToDownload.size + "  " + map.displayName());

        downloadRemoteMap(map, file -> {
            downloaded.add(file);
            downloadNext(mapsToDownload, downloaded, index + 1);
        }, e -> Core.app.post(() -> {
            busy = false;
            Log.err(e);
            setStatus("[scarlet]下载失败：[] " + map.displayName() + "：" + e.getMessage());
            rebuildList();
            ui.showException("下载 ARCreeper 地图失败", e);
        }));
    }

    private void downloadRemoteMap(RemoteMap map, Cons<Fi> success, Cons<Throwable> failure){
        Fi file = tempFile(map);

        // 优先走 api.github.com 的 Contents API raw media type，避免 raw.githubusercontent.com 连接超时。
        downloadToFile(map, map.apiDownloadUrl, file, true, success, e -> {
            // 兜底：如果 API raw 下载失败，再尝试 GitHub 返回的 download_url。
            // 这个通常仍然会指向 raw.githubusercontent.com，所以这里只作为兜底，不作为主路径。
            if(map.rawDownloadUrl != null && !map.rawDownloadUrl.isEmpty()){
                Log.err(e);
                setStatusPost("API 下载失败，正在尝试备用 Raw 下载：" + map.displayName());

                downloadToFile(map, map.rawDownloadUrl, file, false, success, failure);
            }else{
                failure.get(e);
            }
        });
    }

    private void downloadToFile(RemoteMap map, String url, Fi dest, boolean githubApiRaw, Cons<Fi> success, Cons<Throwable> failure){
        mainExecutor.submit(() -> {
            HttpURLConnection con = null;

            try{
                dest.parent().mkdirs();
                if(dest.exists()){
                    dest.delete();
                }

                con = (HttpURLConnection)new URL(url).openConnection();
                con.setRequestMethod("GET");
                con.setInstanceFollowRedirects(true);
                con.setConnectTimeout(30000);
                con.setReadTimeout(60000);
                con.setRequestProperty("User-Agent", "Mindustry-ARCreeper");

                if(githubApiRaw){
                    con.setRequestProperty("Accept", "application/vnd.github.raw+json");
                    con.setRequestProperty("X-GitHub-Api-Version", githubApiVersion);
                }

                int code = con.getResponseCode();

                if(code < 200 || code >= 300){
                    String message = responseMessage(con);
                    String detail = readStreamLimited(con.getErrorStream(), 512);

                    throw new IOException("HTTP " + code +
                            (message == null || message.isEmpty() ? "" : " " + message) +
                            (detail == null || detail.isEmpty() ? "" : " - " + detail));
                }

                int total = con.getContentLength();
                long counter = 0L;
                long lastStatus = 0L;

                try(InputStream input = new BufferedInputStream(con.getInputStream());
                    OutputStream output = dest.write(false, 4096)){

                    byte[] buffer = new byte[4096];
                    int read;

                    while((read = input.read(buffer)) != -1){
                        output.write(buffer, 0, read);
                        counter += read;

                        if(total > 0 && Time.millis() - lastStatus > 250L){
                            lastStatus = Time.millis();
                            int percent = (int)(counter * 100L / total);
                            setStatusPost("正在下载：" + map.displayName() + "  " + percent + "%");
                        }
                    }
                }

                if(counter <= 0L){
                    throw new IOException("下载结果为空：" + map.fileName);
                }

                success.get(dest);
            }catch(Throwable e){
                try{
                    if(dest.exists()){
                        dest.delete();
                    }
                }catch(Throwable ignored){
                }

                failure.get(e);
            }finally{
                if(con != null){
                    con.disconnect();
                }
            }
        });
    }

    private Fi tempFile(RemoteMap map){
        Fi dir = dataDirectory.child("tmp").child("arcreeper-maps");
        dir.mkdirs();
        return dir.child(map.safeLocalFileName());
    }

    private void importDownloadedMap(Fi file){
        maps.tryCatchMapError(() -> {
            if(MapIO.isImage(file)){
                ui.showErrorMessage("@editor.errorimage");
                return;
            }

            Map map = MapIO.createMap(file, true);

            String name = map.tags.get("name", () -> {
                String result = "unknown";
                int number = 0;

                while(maps.byName(result + number++) != null);

                return result + number;
            });

            if(name == null){
                ui.showErrorMessage("@editor.errorname");
                return;
            }

            Map conflict = maps.all().find(m -> m.name().equalsIgnoreCase(name));

            if(conflict != null){
                maps.removeMap(conflict);
            }

            maps.importMap(map.file);
        });
    }

    private void setStatus(String text){
        if(status != null){
            status.setText(text);
        }
    }

    private void setStatusPost(String text){
        Core.app.post(() -> setStatus(text));
    }

    private static String contentsRawApiUrl(String path){
        return contentsApiPrefix + encodePath(path) + "?ref=main";
    }

    private static String encodePath(String path){
        StringBuilder out = new StringBuilder();

        String[] parts = path.split("/");
        for(int i = 0; i < parts.length; i++){
            if(i > 0) out.append("/");
            out.append(encodeQueryValue(parts[i]));
        }

        return out.toString();
    }

    private static String encodeQueryValue(String text){
        try{
            return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        }catch(Throwable e){
            return text;
        }
    }

    private static String readStreamLimited(InputStream stream, int max){
        if(stream == null){
            return "";
        }

        try(InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()){
            byte[] buffer = new byte[256];
            int remaining = max;
            int read;

            while(remaining > 0 && (read = input.read(buffer, 0, Math.min(buffer.length, remaining))) != -1){
                output.write(buffer, 0, read);
                remaining -= read;
            }

            return output.toString("UTF-8").replace('\n', ' ').replace('\r', ' ');
        }catch(Throwable e){
            return "";
        }
    }

    private static String responseMessage(HttpURLConnection con){
        try{
            return con.getResponseMessage();
        }catch(Throwable e){
            return "";
        }
    }

    private String formatDate(String date){
        if(date == null || date.length() < 10){
            return "未知";
        }

        return date.replace("T", " ").replace("Z", " UTC");
    }

    private static class RemoteMap{
        final String fileName;
        final String path;
        final String apiDownloadUrl;
        final String rawDownloadUrl;
        final String htmlUrl;
        String updated = "读取中";

        RemoteMap(String fileName, String path, String apiDownloadUrl, String rawDownloadUrl, String htmlUrl){
            this.fileName = fileName;
            this.path = path;
            this.apiDownloadUrl = apiDownloadUrl;
            this.rawDownloadUrl = rawDownloadUrl;
            this.htmlUrl = htmlUrl;
        }

        String displayName(){
            String ext = "." + mapExtension;
            return fileName.endsWith(ext) ? fileName.substring(0, fileName.length() - ext.length()) : fileName;
        }

        String safeLocalFileName(){
            return path.replace("/", "_").replace("\\", "_");
        }
    }
}