package mindustry.world.blocks.sandbox;

import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerSource extends PowerNode{
    public float powerProduction = 10000f;

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
        drawDisabled = true;
        //TODO maybe don't?
        envEnabled = Env.any;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.basePowerGeneration, powerProduction * 60f, StatUnit.powerSecond);
    }

    public class PowerSourceBuild extends PowerNodeBuild{
        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            if(!allowUpdate()){
                enabled = false;
            }
        }

        @Override
        public float getPowerProduction(){
            return enabled ? powerProduction : 0f;
        }
    }

}
