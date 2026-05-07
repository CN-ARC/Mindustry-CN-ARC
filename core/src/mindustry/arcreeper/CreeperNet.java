package mindustry.arcreeper;


import arc.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.net.*;
import mindustry.net.Packets.*;

import java.io.*;

public final class CreeperNet{
    private static boolean registered = false;

    private CreeperNet(){
    }

    public static void init(){
        if(registered) return;
        registered = true;

        Net.registerPacket(ArcCreeperSnapshotRequestPacket::new);
        Net.registerPacket(ArcCreeperSnapshotStream::new);

        Vars.net.handleServer(ArcCreeperSnapshotRequestPacket.class, CreeperNet::handleSnapshotRequest);
        Vars.net.handleClient(ArcCreeperSnapshotStream.class, CreeperNet::handleSnapshotStream);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(Vars.net.client() && CreeperCore.enabled()){
                requestSnapshot();
            }
        });

        Events.on(EventType.PlayerConnectionConfirmed.class, e -> {
            if(Vars.net.server() && CreeperCore.enabled() && e.player != null && e.player.con != null){
                sendSnapshot(e.player.con);
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

    public static class ArcCreeperSnapshotStream extends Streamable{
    }
}