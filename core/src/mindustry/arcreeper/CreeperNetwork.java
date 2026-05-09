package mindustry.arcreeper;


import arc.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.*;
import mindustry.game.*;
import mindustry.net.*;
import mindustry.world.Tile;

import java.io.*;

public final class CreeperNetwork {
    private static boolean registered = false;

    private CreeperNetwork(){
    }

    public static void init(){
        if(registered) return;
        registered = true;

        /*
         * 原 ARCreeper snapshot 网络同步。
         */
        Net.registerPacket(ArcCreeperSnapshotRequestPacket::new);
        Net.registerPacket(ArcCreeperSnapshotStream::new);

        /*
         * 新增 netStat 运行时增量同步。
         * 注意：只同步 Tile.netStat，不同步 Tile.netHealth。
         */
        Net.registerPacket(ArcCreeperNetSnapshotRequestPacket::new);
        Net.registerPacket(ArcCreeperNetSnapshotStream::new);
        Net.registerPacket(ArcCreeperNetStatRequestPacket::new);
        Net.registerPacket(ArcCreeperNetStatPacket::new);

        Vars.net.handleServer(ArcCreeperSnapshotRequestPacket.class, CreeperNetwork::handleSnapshotRequest);
        Vars.net.handleClient(ArcCreeperSnapshotStream.class, CreeperNetwork::handleSnapshotStream);

        Vars.net.handleServer(ArcCreeperNetStatRequestPacket.class, CreeperNetwork::handleNetStatRequest);
        Vars.net.handleClient(ArcCreeperNetStatPacket.class, CreeperNetwork::handleNetStat);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(Vars.net.client() && CreeperCore.enabled()){
                requestSnapshot();
                requestNetSnapshot();
            }
        });

        Events.on(EventType.PlayerConnectionConfirmed.class, e -> {
            if(Vars.net.server() && CreeperCore.enabled() && e.player != null && e.player.con != null){
                sendSnapshot(e.player.con);
                sendNetSnapshot(e.player.con);
            }
        });
    }

    public static void requestSnapshot(){
        if(!Vars.net.client()) return;

        Vars.net.send(new ArcCreeperSnapshotRequestPacket(), true);
    }

    private static void handleSnapshotRequest(NetConnection con, ArcCreeperSnapshotRequestPacket packet){
        if(!Vars.net.server()) return;
        if(!CreeperCore.enabled()) return;

        sendSnapshot(con);
    }

    public static void sendSnapshot(NetConnection con){
        byte[] bytes = CreeperCore.creeperTile.writeSnapshotBytes();

        ArcCreeperSnapshotStream stream = new ArcCreeperSnapshotStream();
        stream.stream = new ByteArrayInputStream(bytes);

        con.sendStream(stream);
    }

    public static void sendSnapshotToAll(){
        if(!Vars.net.server()) return;

        for(NetConnection con : Vars.net.getConnections()){
            sendSnapshot(con);
        }
    }

    private static void handleSnapshotStream(ArcCreeperSnapshotStream packet){
        if(packet.stream == null) return;

        byte[] bytes = readStreamBytes(packet.stream);
        CreeperCore.creeperTile.readSnapshotBytes(bytes);
    }

    private static byte[] readStreamBytes(ByteArrayInputStream stream){
        byte[] bytes = new byte[stream.available()];

        int offset = 0;
        while(offset < bytes.length){
            int read = stream.read(bytes, offset, bytes.length - offset);
            if(read <= 0) break;
            offset += read;
        }

        return bytes;
    }

    public static class ArcCreeperSnapshotRequestPacket extends Packet{
        @Override
        public boolean allow(boolean server){
            return server;
        }

        @Override
        public int getPriority(){
            return priorityHigh;
        }
    }
    /*
     * netStat runtime sync
     */

    public static void requestSetNetStat(Tile tile, int stat){
        if(tile == null) return;
        if(!Vars.net.client()) return;

        ArcCreeperNetStatRequestPacket packet = new ArcCreeperNetStatRequestPacket();
        packet.pos = tile.pos();
        packet.stat = CreeperNet.sanitize(stat);

        Vars.net.send(packet, true);
    }

    public static void sendNetStat(Tile tile){
        if(tile == null) return;
        if(!Vars.net.server()) return;

        ArcCreeperNetStatPacket packet = new ArcCreeperNetStatPacket();
        packet.pos = tile.pos();
        packet.stat = CreeperNet.state(tile);

        Vars.net.send(packet, true);
    }

    public static void sendNetStat(NetConnection con, Tile tile){
        if(con == null || tile == null) return;
        if(!Vars.net.server()) return;

        ArcCreeperNetStatPacket packet = new ArcCreeperNetStatPacket();
        packet.pos = tile.pos();
        packet.stat = CreeperNet.state(tile);

        con.send(packet, true);
    }

    private static void handleNetStatRequest(NetConnection con, ArcCreeperNetStatRequestPacket packet){
        if(!Vars.net.server()) return;
        if(!CreeperCore.enabled()) return;
        if(!canRemoteSet(con)) return;

        Tile tile = Vars.world.tile(packet.pos);
        if(tile == null) return;

        /*
         * 服务端是权威。
         * 服务端落地后会通过 CreeperNetwork.sendNetStat(tile) 广播给所有客户端。
         */
        CreeperNet.setAuthoritative(tile, packet.stat);
    }

    private static void handleNetStat(ArcCreeperNetStatPacket packet){
        if(Vars.net.server()) return;
        if(!CreeperCore.enabled()) return;

        Tile tile = Vars.world.tile(packet.pos);
        if(tile == null) return;

        /*
         * 客户端收到服务端状态，只本地应用，不再发包。
         */
        CreeperNet.applyLocal(tile, packet.stat);
    }

    private static boolean canRemoteSet(NetConnection con){
        if(con == null || con.player == null) return false;

        /*
         * 避免普通客户端随意改 Tile.netStat。
         * 逻辑处理器、服务端内部逻辑不会走这个请求入口。
         */
        return con.player.admin || Vars.state.isEditor();
    }

    /*
     * Packets
     */

    public static void requestNetSnapshot(){
        if(!Vars.net.client()) return;

        Vars.net.send(new ArcCreeperNetSnapshotRequestPacket(), true);
    }

    private static void handleNetSnapshotRequest(NetConnection con, ArcCreeperNetSnapshotRequestPacket packet){
        if(!Vars.net.server()) return;
        if(!CreeperCore.enabled()) return;
        if(con == null) return;

        sendNetSnapshot(con);
    }

    public static void sendNetSnapshot(NetConnection con){
        if(con == null) return;

        byte[] bytes = CreeperNet.writeSnapshotBytes();

        ArcCreeperNetSnapshotStream stream = new ArcCreeperNetSnapshotStream();
        stream.stream = new ByteArrayInputStream(bytes);

        con.sendStream(stream);
    }

    private static void handleNetSnapshotStream(ArcCreeperNetSnapshotStream packet){
        if(packet.stream == null) return;

        byte[] bytes = readStreamBytes(packet.stream);
        CreeperNet.readSnapshotBytes(bytes);
    }

    public static class ArcCreeperSnapshotStream extends Streamable{
    }

    public static class ArcCreeperNetStatRequestPacket extends Packet{
        public int pos;
        public int stat;

        @Override
        public boolean allow(boolean server){
            return server;
        }

        @Override
        public int getPriority(){
            return priorityHigh;
        }

        @Override
        public void write(Writes write){
            write.i(pos);
            write.i(stat);
        }

        @Override
        public void read(Reads read){
            pos = read.i();
            stat = read.i();
        }
    }

    public static class ArcCreeperNetStatPacket extends Packet{
        public int pos;
        public int stat;

        @Override
        public boolean allow(boolean server){
            return !server;
        }

        @Override
        public int getPriority(){
            return priorityHigh;
        }

        @Override
        public void write(Writes write){
            write.i(pos);
            write.i(stat);
        }

        @Override
        public void read(Reads read){
            pos = read.i();
            stat = read.i();
        }
    }

    public static class ArcCreeperNetSnapshotRequestPacket extends Packet{
        @Override
        public boolean allow(boolean server){
            return server;
        }

        @Override
        public int getPriority(){
            return priorityHigh;
        }
    }

    public static class ArcCreeperNetSnapshotStream extends Streamable{
    }
}