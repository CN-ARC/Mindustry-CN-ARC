package mindustry.arcreeper;

import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.world.Tile;

public class CreeperNetCombat {
    public static final float defaultNetHealth = 100f;
    public static final float damaged1Threshold = 67f;
    public static final float damaged2Threshold = 33f;
    public static final float autoHealRate = 8f;
    public static final float directDamageScale = 1f;

    private float[] netHealth = new float[0];
    private float[] netCharge = new float[0];
    private boolean[] netPowered = new boolean[0];
    private final IntSeq netPowerQueue = new IntSeq();

    public void init(){
        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());
        syncFromTileStates();
    }

    public void reset(){
        for(int i = 0; i < netHealth.length; i++){
            netHealth[i] = 0f;
        }
        for(int i = 0; i < netCharge.length; i++){
            netCharge[i] = 0f;
        }
        for(int i = 0; i < netPowered.length; i++){
            netPowered[i] = false;
        }
    }

    public void onStateChanged(Tile tile, int oldState, int newState){
        CreeperCore.creeperTile.markNetHeightsDirty();

        if(tile == null) return;

        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());

        int index = tile.array();
        if(index < 0 || index >= netHealth.length) return;

        if(!CreeperTile.hasNetState(newState)){
            netHealth[index] = 0f;
            netCharge[index] = 0f;
            netPowered[index] = false;
            return;
        }

        if(newState == CreeperTile.netStateInactive){
            netCharge[index] = 0f;
            netPowered[index] = false;
            if(!CreeperTile.hasNetState(oldState)){
                netHealth[index] = 0f;
            }
            return;
        }

        if(CreeperTile.isOutletState(newState)){
            netHealth[index] = defaultNetHealth;
            netCharge[index] = 0f;
            netPowered[index] = true;
            return;
        }

        if(!CreeperTile.isDegradableNetState(oldState) && netHealth[index] <= 0f){
            netHealth[index] = defaultHealthForState(newState);
        }
        netCharge[index] = 0f;
    }

    public void update(){
        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());
        updatePoweredNetMap();

        float delta = Vars.state.rules.creeperFlowInterval;
        float activationTime = 5f * Math.max(Vars.state.rules.creeperNetActivationScale, 0.0001f);
        float disconnectDecayRate = (defaultNetHealth / 2.5f) * Math.max(Vars.state.rules.creeperNetDecayScale, 0.0001f);

        Vars.world.tiles.eachTile(tile -> {
            int state = stateOf(tile);
            if(!CreeperTile.hasNetState(state)) return;

            int index = tile.array();
            if(index < 0 || index >= netHealth.length) return;

            if(CreeperTile.isOutletState(state)){
                updatePulseSource(tile, index, delta, activationTime, true);
                return;
            }

            if(state == CreeperTile.netStateInactive){
                netCharge[index] = 0f;
                return;
            }

            if(!CreeperTile.isDegradableNetState(state)) return;

            if(netPowered[index]){
                netHealth[index] = Math.min(defaultNetHealth, netHealth[index] + autoHealRate * delta);
                updatePulseSource(tile, index, delta, activationTime, false);
            }else if(isDisconnectedBoundaryTile(tile)){
                netHealth[index] = Math.max(0f, netHealth[index] - disconnectDecayRate * delta);
                netCharge[index] = 0f;
            }else{
                netCharge[index] = 0f;
            }

            applyStateFromHealth(tile, index);
        });
    }

    public float damageAt(float wx, float wy, float damage){
        if(damage <= 0f) return 0f;

        Tile tile = Vars.world.tileWorld(wx, wy);
        return damageTile(tile, damage);
    }

    public float damageTile(Tile tile, float damage){
        float scale = Math.max(Vars.state.rules.creeperNetDamageScale, 0.0001f);
        return damageHealth(tile, damage * directDamageScale / scale);
    }

    public float damageHealth(Tile tile, float amount){
        if(tile == null) return 0f;

        int state = stateOf(tile);
        if(!CreeperTile.isDegradableNetState(state)) return 0f;

        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());

        int index = tile.array();
        if(index < 0 || index >= netHealth.length) return 0f;

        float applied = Math.min(netHealth[index], Math.max(0f, amount));
        netHealth[index] = Math.max(0f, netHealth[index] - applied);
        applyStateFromHealth(tile, index);
        return applied;
    }

    public float healthOf(Tile tile){
        if(tile == null) return 0f;
        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());
        int index = tile.array();
        return index < 0 || index >= netHealth.length ? 0f : netHealth[index];
    }

    public float chargeOf(Tile tile){
        if(tile == null) return 0f;
        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());
        int index = tile.array();
        return index < 0 || index >= netCharge.length ? 0f : netCharge[index];
    }

    public void writeSnapshotData(Writes write, Tile tile){
        write.f(healthOf(tile));
        write.f(chargeOf(tile));
    }

    public void readSnapshotData(Tile tile, int state, Reads read){
        readSnapshotData(tile, state, read.f(), read.f());
    }

    public void readSnapshotData(Tile tile, int state, float health, float charge){
        if(tile == null) return;

        ensureRuntimeArrays(Vars.world.width() * Vars.world.height());

        int index = tile.array();
        if(index < 0 || index >= netHealth.length) return;

        if(!CreeperTile.hasNetState(state)){
            netHealth[index] = 0f;
            netCharge[index] = 0f;
            netPowered[index] = false;
            return;
        }

        netHealth[index] = Math.max(0f, Math.min(defaultNetHealth, health));
        netCharge[index] = Math.max(0f, charge);
        netPowered[index] = false;

        if(CreeperTile.isOutletState(state)){
            netHealth[index] = defaultNetHealth;
            netCharge[index] = 0f;
        }else if(state == CreeperTile.netStateInactive){
            netHealth[index] = 0f;
        }else if(netHealth[index] <= 0f){
            netHealth[index] = defaultHealthForState(state);
        }
    }

    private void updatePulseSource(Tile tile, int index, float delta, float activationTime, boolean alwaysPulse){
        if(!alwaysPulse && !netPowered[index]){
            netCharge[index] = 0f;
            return;
        }

        netCharge[index] += delta;
        while(netCharge[index] >= activationTime){
            netCharge[index] -= activationTime;
            pulseAdjacentInactive(tile);
        }
    }

    private void updatePoweredNetMap(){
        for(int i = 0; i < netPowered.length; i++){
            netPowered[i] = false;
        }

        netPowerQueue.clear();

        Vars.world.tiles.eachTile(tile -> {
            if(tile == null) return;
            if(!CreeperTile.isOutletState(stateOf(tile))) return;

            int index = tile.array();
            if(index < 0 || index >= netPowered.length || netPowered[index]) return;

            netPowered[index] = true;
            netPowerQueue.add(index);
        });

        while(!netPowerQueue.isEmpty()){
            int current = netPowerQueue.pop();
            Tile tile = Vars.world.tiles.geti(current);
            if(tile == null) continue;

            for(Point2 point : Geometry.d4){
                Tile other = tile.nearby(point);
                if(other == null) continue;

                int otherIndex = other.array();
                if(otherIndex < 0 || otherIndex >= netPowered.length || netPowered[otherIndex]) continue;
                if(!CreeperTile.isBoostedNetState(stateOf(other))) continue;

                netPowered[otherIndex] = true;
                netPowerQueue.add(otherIndex);
            }
        }
    }

    private void pulseAdjacentInactive(Tile tile){
        for(Point2 point : Geometry.d4){
            Tile other = tile.nearby(point);
            if(other == null) continue;
            if(stateOf(other) != CreeperTile.netStateInactive) continue;

            other.setCreeperNet(CreeperTile.netStateActive);
        }
    }

    private boolean isDisconnectedBoundaryTile(Tile tile){
        if(tile == null) return false;
        if(netPowered[tile.array()]) return false;

        for(Point2 point : Geometry.d4){
            Tile other = tile.nearby(point);
            if(other == null) return true;

            int otherState = stateOf(other);
            int otherIndex = other.array();
            boolean otherDisconnectedCore =
                CreeperTile.isDegradableNetState(otherState) &&
                otherIndex >= 0 &&
                otherIndex < netPowered.length &&
                !netPowered[otherIndex];

            if(!otherDisconnectedCore){
                return true;
            }
        }

        return false;
    }

    private void applyStateFromHealth(Tile tile, int index){
        float health = netHealth[index];
        int nextState;

        if(health <= 0f){
            netHealth[index] = 0f;
            netCharge[index] = 0f;
            nextState = CreeperTile.netStateInactive;
        }else if(health < damaged2Threshold){
            nextState = CreeperTile.netStateDamaged2;
        }else if(health < damaged1Threshold){
            nextState = CreeperTile.netStateDamaged1;
        }else{
            nextState = CreeperTile.netStateActive;
        }

        if(tile.getCreeperNetState() != nextState){
            tile.setCreeperNet(nextState);
        }
    }

    private void syncFromTileStates(){
        Vars.world.tiles.eachTile(tile -> {
            if(tile == null) return;
            int state = stateOf(tile);
            int index = tile.array();
            if(index < 0 || index >= netHealth.length) return;

            if(!CreeperTile.hasNetState(state)){
                netHealth[index] = 0f;
                netCharge[index] = 0f;
            }else if(CreeperTile.isOutletState(state)){
                netHealth[index] = defaultNetHealth;
                netCharge[index] = 0f;
            }else if(state == CreeperTile.netStateInactive){
                netHealth[index] = 0f;
                netCharge[index] = 0f;
            }else if(netHealth[index] <= 0f){
                netHealth[index] = defaultHealthForState(state);
            }
        });
    }

    private int stateOf(Tile tile){
        return tile == null ? CreeperTile.netStateNone : CreeperTile.sanitizeNetState(tile.getCreeperNetState());
    }

    private float defaultHealthForState(int state){
        return switch(state){
            case CreeperTile.netStateDamaged2 -> damaged2Threshold;
            case CreeperTile.netStateDamaged1 -> damaged1Threshold;
            default -> defaultNetHealth;
        };
    }

    private void ensureRuntimeArrays(int size){
        if(netHealth.length != size){
            netHealth = new float[size];
        }
        if(netCharge.length != size){
            netCharge = new float[size];
        }
        if(netPowered.length != size){
            netPowered = new boolean[size];
        }
    }
}
