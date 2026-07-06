package mindustry.arcModule;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.IntMap;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.ByteBufferOutput;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.net.Net;
import mindustry.net.Packet;
import mindustry.net.Packets;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;
import static mindustry.arcModule.TimeControl.*;

public class ReplayController {
    public static final int version = 2;
    private Writes writes;
    private Reads reads;
    private long startTime, allTime;
    private long lastTime, nextTime;
    private long length, skip = 0;
    private Thread thread;
    private final Fi dir = Vars.dataDirectory.child("replays");
    private final ByteBuffer tmpBuf = ByteBuffer.allocate(32768);
    private final Writes tmpWr = new Writes(new ByteBufferOutput(tmpBuf));
    private boolean recording = false, recordEnabled = false;
    private final Table controller = new Table();
    private final IntMap<Integer> map = new IntMap<>();
    private ReplayData now = null;
    private BaseDialog dialog = null;
    public boolean writing = false;

    public ReplayController() {
        dir.mkdirs();
        thread = new Thread(() -> {
            while (true) {
                if (reads == null) {
                    try {
                        synchronized (thread) {
                            thread.wait();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    readNextPacket();
                } catch (Exception e) {
                    closeReads();
                    reads = null;
                    net.disconnect();
                    ARCVars.replaying = false;
                    Core.app.post(() -> logic.reset());
                }
            }
        }, "Replay Controller");
        thread.setPriority(3);
        thread.setDaemon(true);
        thread.start();
        WidgetGroup g = new WidgetGroup();
        g.addChild(controller);
        g.setFillParent(true);
        g.touchable = Touchable.childrenOnly;
        g.visible(() -> ARCVars.replaying);
        controller.setFillParent(true);
        Events.on(EventType.ClientLoadEvent.class, e -> {
            Core.scene.add(g);
            dialog = new BaseDialog("回放统计");
            dialog.shown(() -> {
                dialog.cont.clear();
                if (now == null) {
                    dialog.cont.add("未加载回放!");
                    return;
                }
                dialog.cont.add("回放版本:" + now.version).row();
                dialog.cont.add("回放创建时间:" + now.time).row();
                dialog.cont.add("服务器ip:" + now.ip).row();
                dialog.cont.add("玩家名:" + now.name).row();
                int secs = (int) (length / 1000000000);
                dialog.cont.add("回放长度:" + (secs / 3600) + ":" + (secs / 60 % 60) + ":" + (secs % 60)).row();
                //! to fix 这里有问题
                //dialog.cont.pane(t -> map.keys().toArray().each(b -> t.add(Net.newPacket((byte) b).getClass().getSimpleName() + " " + map.get(b)).row())).growX().row();
            });
            dialog.addCloseButton();
            controller.table(t -> {
                t.setBackground(Styles.black3);
                t.table(tt -> {
                    //tt.button("快进10s", () -> skip = timeEscaped() + 10000000000L);//TODO bug
                    tt.button("倍率x2", () -> changeGameSpeed(2f));
                    tt.button("倍率/2", () -> changeGameSpeed(0.5f));
                    tt.button("暂停回放", () -> setGameSpeed(0f));
                    tt.button("恢复原速", () -> setGameSpeed(1f));
                    tt.button("回放信息", this::showInfo);
                }).row();
                t.label(() -> "当前倍率:" + gameSpeed).row();
                t.label(() -> {
                    int secs = (int) (length / 1000000000);
                    int escaped = (int) (timeEscaped() / 1000000000);
                    return (escaped / 3600) + ":" + (escaped / 60 % 60) + ":" + (escaped % 60) + "/" + (secs / 3600) + ":" + (secs / 60 % 60) + ":" + (secs % 60);
                });
            }).growX().top().padTop(150).row();
            controller.add().grow();
        });
        Events.run(EventType.Trigger.update, () -> {
            if (state.getState() == GameState.State.menu && !netClient.isConnecting()) {
                closeReads();
                reads = null;
                ARCVars.replaying = false;
                stopPlay();
            }
        });
    }

    private void closeReads() {
        try {
            reads.close();
        } catch (Exception ignored) {
        }
    }

    private static class ReplayData {
        int version;
        Date time;
        String ip;
        String name;
        ReplayData(int version, Date time, String ip, String name) {
            this.version = version;
            this.time = time;
            this.ip = ip;
            this.name = name;
        }
    }

    public void createReplay(String ip) {
        if (!recordEnabled || ARCVars.replaying) return;
        stop();
        try {
            writes = new Writes(new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new FileOutputStream(dir.child(new Date().getTime() + ".mrep").file())))));
        } catch (Exception e) {
            Log.err("创建回放出错!", e);
            return;
        }
        boolean anonymous = Core.settings.getBool("anonymous", false);
        writes.i(version);
        writes.l(new Date().getTime());
        writes.str(anonymous ? "anonymous" : ip);
        writes.str(anonymous ? "anonymous" : Vars.player.name.trim());
        recording = true;
        startTime = Time.nanos();
    }

    public void stop() {
        recording = false;
        try {
            writes.close();
        } catch (Exception ignored) {
        }
    }

    public void writePacket(Packet p) {
        if (!recording || p instanceof Packets.WorldStream) return;
        try {
            byte id = Net.getPacketId(p);
            try {
                writes.l(Time.nanos() - startTime);
                writes.b(id);
                tmpBuf.position(0);
                writing = true;
                p.write(tmpWr);
                writing = false;
                int l = tmpBuf.position();
                writes.s(l);
                writes.b(tmpBuf.array(), 0, l);
            } catch (Exception e) {
                net.disconnect();
                Core.app.post(() -> ui.showException("录制出错!", e));
            }
        } catch (Exception ignored) {
        }
    }

    synchronized private long timeEscaped() {
        long escaped = (long) ((Time.nanos() - lastTime) * gameSpeed);
        allTime += escaped;
        lastTime = Time.nanos();
        return allTime;
    }

    private void readNextPacket() {
        long escaped = timeEscaped();
        if (escaped < nextTime && skip == 0) {
            Thread.yield();
            return;
        }
        nextTime = reads.l();
        Packet p = Net.newPacket(reads.b());
        int l = reads.us();
        p.read(reads, l);
        Core.app.post(() -> net.handleClientReceived(p));
        if (skip != 0 && escaped > skip) skip = 0;
    }

    public void shouldRecord(boolean should) {
        recordEnabled = should;
    }

    public boolean shouldRecord() {
        return recordEnabled;
    }

    public Reads createReads(File input) {
        closeReads();
        try {
            return new Reads(new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(input)), 32768)));
        } catch (Exception e) {
            Core.app.post(() -> ui.showException("读取回放失败!", e));
        }
        return null;
    }

    public void startPlay(File input) {
        gameSpeed = 1f;
        Reads r = createReads(input);
        if (r == null) return;
        int version = r.i();
        Date time = new Date(r.l());
        String ip = r.str();
        String name = r.str();
        Log.info("version: @, time: @, ip: @, name: @", version, time, ip, name);
        now = new ReplayData(version, time, ip, name);
        while (true) {
            try {
                long l = r.l();
                byte id = r.b();
                r.skip(r.us());
                map.put(id, map.get(id, 0) + 1);
                length = l;
            } catch (Exception e) {
                break;
            }
        }
        r = createReads(input);
        r.skip(12);
        r.str();
        r.str();
        ARCVars.replaying = true;
        reads = r;
        logic.reset();
        net.reset();
        try {
            Field f = net.getClass().getDeclaredField("active");
            f.setAccessible(true);
            f.set(net, true);
        } catch (Exception e) {
            ui.showException(e);
        }
        Packets.Connect c = new Packets.Connect();
        c.addressTCP = ip;
        net.handleClientReceived(c);
        netClient.beginConnecting();
        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
            reads = null;
        });
        nextTime = allTime = skip = 0;
        lastTime = Time.nanos();
        synchronized (thread) {
            thread.notify();
        }
    }

    public void stopPlay() {
        reads = null;
        ARCVars.replaying = false;
    }


    public void showInfo() {
        if(dialog != null) dialog.show();
    }
}
