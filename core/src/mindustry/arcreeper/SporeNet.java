package mindustry.arcreeper;

import arc.Events;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustry.net.Packet;

public final class SporeNet{
    private static boolean registered;

    private SporeNet(){
    }

    public static void init(){
        if(registered) return;
        registered = true;

        Net.registerPacket(ArcSporeSpawnPacket::new);
        Net.registerPacket(ArcSporeStatePacket::new);
        Net.registerPacket(ArcSporeRemovePacket::new);
        Net.registerPacket(ArcSporeSnapshotPacket::new);
        Net.registerPacket(ArcSporeSnapshotRequestPacket::new);
        Net.registerPacket(ArcSporeClearPacket::new);

        Vars.net.handleClient(ArcSporeSpawnPacket.class, SporeNet::handleSpawn);
        Vars.net.handleClient(ArcSporeStatePacket.class, SporeNet::handleState);
        Vars.net.handleClient(ArcSporeRemovePacket.class, SporeNet::handleRemove);
        Vars.net.handleClient(ArcSporeSnapshotPacket.class, SporeNet::handleSnapshot);
        Vars.net.handleClient(ArcSporeClearPacket.class, SporeNet::handleClear);

        Vars.net.handleServer(ArcSporeSnapshotRequestPacket.class, SporeNet::handleSnapshotRequest);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(Vars.net.client()){
                requestSnapshot();
            }
        });
    }

    public static void sendSpawn(Spore spore){
        ArcSporeSpawnPacket packet = new ArcSporeSpawnPacket();
        packet.state = new SporeState(spore);
        Vars.net.send(packet, true);
    }

    public static void sendState(Spore spore){
        ArcSporeStatePacket packet = new ArcSporeStatePacket();
        packet.state = new SporeState(spore);
        Vars.net.send(packet, false);
    }

    public static void sendRemove(Spore spore, byte reason){
        ArcSporeRemovePacket packet = new ArcSporeRemovePacket();
        packet.state = new RemoveState(spore, reason);
        Vars.net.send(packet, true);
    }

    public static void sendClear(int generation){
        ArcSporeClearPacket packet = new ArcSporeClearPacket();
        packet.generation = generation;
        Vars.net.send(packet, true);
    }

    public static void requestSnapshot(){
        if(!Vars.net.client()) return;
        Vars.net.send(new ArcSporeSnapshotRequestPacket(), true);
    }

    public static void sendSnapshot(NetConnection con){
        ArcSporeSnapshotPacket packet = new ArcSporeSnapshotPacket();
        packet.generation = SporeCore.generation();
        packet.nextId = SporeCore.nextId();

        for(Spore spore : SporeCore.all()){
            if(!spore.removed){
                packet.states.add(new SporeState(spore));
            }
        }

        con.send(packet, true);
    }

    public static void sendSnapshotToAll(){
        ArcSporeSnapshotPacket packet = new ArcSporeSnapshotPacket();
        packet.generation = SporeCore.generation();
        packet.nextId = SporeCore.nextId();

        for(Spore spore : SporeCore.all()){
            if(!spore.removed){
                packet.states.add(new SporeState(spore));
            }
        }

        Vars.net.send(packet, true);
    }

    private static void handleSpawn(ArcSporeSpawnPacket packet){
        SporeCore.applyRemoteSpawn(packet.state);
    }

    private static void handleState(ArcSporeStatePacket packet){
        SporeCore.applyRemoteState(packet.state);
    }

    private static void handleRemove(ArcSporeRemovePacket packet){
        SporeCore.applyRemoteRemove(packet.state);
    }

    private static void handleSnapshot(ArcSporeSnapshotPacket packet){
        SporeCore.applySnapshot(packet.generation, packet.nextId, packet.states);
    }

    private static void handleClear(ArcSporeClearPacket packet){
        SporeCore.applyRemoteClear(packet.generation);
    }

    private static void handleSnapshotRequest(NetConnection con, ArcSporeSnapshotRequestPacket packet){
        if(!Vars.net.server()) return;
        sendSnapshot(con);
    }

    public static class SporeState{
        public int generation;
        public int id;

        public float startX, startY;
        public float targetX, targetY;
        public float x, y;

        public float speed;
        public float health;
        public float maxHealth;
        public float creeperAmount;
        public int releaseRadius;

        public float rotate;

        public SporeState(){
        }

        public SporeState(Spore spore){
            generation = spore.generation;
            id = spore.id;

            startX = spore.startX;
            startY = spore.startY;
            targetX = spore.targetX;
            targetY = spore.targetY;
            x = spore.x;
            y = spore.y;

            speed = spore.speed;
            health = spore.health;
            maxHealth = spore.maxHealth;
            creeperAmount = spore.creeperAmount;
            releaseRadius = spore.releaseRadius;

            rotate = spore.rotate;
        }

        public void write(Writes write){
            write.i(generation);
            write.i(id);

            write.f(startX);
            write.f(startY);
            write.f(targetX);
            write.f(targetY);
            write.f(x);
            write.f(y);

            write.f(speed);
            write.f(health);
            write.f(maxHealth);
            write.f(creeperAmount);
            write.i(releaseRadius);

            write.f(rotate);
        }

        public void read(Reads read){
            generation = read.i();
            id = read.i();

            startX = read.f();
            startY = read.f();
            targetX = read.f();
            targetY = read.f();
            x = read.f();
            y = read.f();

            speed = read.f();
            health = read.f();
            maxHealth = read.f();
            creeperAmount = read.f();
            releaseRadius = read.i();

            rotate = read.f();
        }

        public void apply(Spore spore){
            spore.generation = generation;
            spore.id = id;

            spore.startX = startX;
            spore.startY = startY;
            spore.targetX = targetX;
            spore.targetY = targetY;
            spore.x = x;
            spore.y = y;

            spore.speed = speed;
            spore.health = health;
            spore.maxHealth = maxHealth;
            spore.creeperAmount = creeperAmount;
            spore.releaseRadius = releaseRadius;

            spore.rotate = rotate;
            spore.removed = false;
        }

        public void applyMutable(Spore spore){
            spore.x = x;
            spore.y = y;
            spore.health = health;
            spore.maxHealth = maxHealth;
        }
    }

    public static class RemoveState{
        public int generation;
        public int id;
        public byte reason;

        public float x, y;
        public float targetX, targetY;

        public float creeperAmount;
        public int releaseRadius;

        public RemoveState(){
        }

        public RemoveState(Spore spore, byte reason){
            this.generation = spore.generation;
            this.id = spore.id;
            this.reason = reason;

            this.x = spore.x;
            this.y = spore.y;
            this.targetX = spore.targetX;
            this.targetY = spore.targetY;

            this.creeperAmount = spore.creeperAmount;
            this.releaseRadius = spore.releaseRadius;
        }

        public void write(Writes write){
            write.i(generation);
            write.i(id);
            write.b(reason);

            write.f(x);
            write.f(y);
            write.f(targetX);
            write.f(targetY);

            write.f(creeperAmount);
            write.i(releaseRadius);
        }

        public void read(Reads read){
            generation = read.i();
            id = read.i();
            reason = read.b();

            x = read.f();
            y = read.f();
            targetX = read.f();
            targetY = read.f();

            creeperAmount = read.f();
            releaseRadius = read.i();
        }
    }

    public static class ArcSporeSpawnPacket extends Packet{
        public SporeState state = new SporeState();

        @Override
        public boolean allow(boolean server){
            return !server;
        }

        @Override
        public void write(Writes write){
            state.write(write);
        }

        @Override
        public void read(Reads read){
            state = new SporeState();
            state.read(read);
        }
    }

    public static class ArcSporeStatePacket extends Packet{
        public SporeState state = new SporeState();

        @Override
        public boolean allow(boolean server){
            return !server;
        }

        @Override
        public void write(Writes write){
            state.write(write);
        }

        @Override
        public void read(Reads read){
            state = new SporeState();
            state.read(read);
        }
    }

    public static class ArcSporeRemovePacket extends Packet{
        public RemoveState state = new RemoveState();

        @Override
        public boolean allow(boolean server){
            return !server;
        }

        @Override
        public void write(Writes write){
            state.write(write);
        }

        @Override
        public void read(Reads read){
            state = new RemoveState();
            state.read(read);
        }
    }

    public static class ArcSporeSnapshotPacket extends Packet{
        public int generation;
        public int nextId;
        public Seq<SporeState> states = new Seq<>();

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
            write.i(generation);
            write.i(nextId);
            write.i(states.size);

            for(SporeState state : states){
                state.write(write);
            }
        }

        @Override
        public void read(Reads read){
            generation = read.i();
            nextId = read.i();

            int count = read.i();
            states = new Seq<>(count);

            for(int i = 0; i < count; i++){
                SporeState state = new SporeState();
                state.read(read);
                states.add(state);
            }
        }
    }

    public static class ArcSporeSnapshotRequestPacket extends Packet{
        @Override
        public boolean allow(boolean server){
            return server;
        }

        @Override
        public int getPriority(){
            return priorityHigh;
        }
    }

    public static class ArcSporeClearPacket extends Packet{
        public int generation;

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
            write.i(generation);
        }

        @Override
        public void read(Reads read){
            generation = read.i();
        }
    }
}