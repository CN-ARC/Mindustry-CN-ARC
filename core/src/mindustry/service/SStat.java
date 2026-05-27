package mindustry.service;

import static mindustry.Vars.*;

public enum SStat{
    unitTypesBuilt,
    unitsBuilt,
    attacksWon,
    pvpsWon,
    timesLaunched,
    blocksDestroyed,
    itemsLaunched,
    reactorsOverheated,
    maxUnitActive,
    unitsDestroyed,
    bossesDefeated,
    maxPlayersServer,
    mapsPublished,
    maxWavesSurvived,
    blocksBuilt,
    maxProduction,
    sectorsControlled,

    mapsMade,
    schematicsCreated,
    bouldersDeconstructed,
    totalCampaignItems,

    arcUnitsBuilt,
    arcUnitsDestroyed,
    arcBlocksBuilt,
    arcBlocksDestroyed,
    arcPlayTime,
    arcMapsPlayed,
    arcReactorsOverheated
    ;

    public int get(){
        return service.getStat(name(), 0);
    }

    public void max(int amount){
        if(amount > get()){
            set(amount);
        }
    }

    public void set(int amount){
        service.setStat(name(), amount);
        service.storeStats();

        for(Achievement a : Achievement.all){
            a.checkCompletion();
        }
    }

    public void add(int amount){
        set(get() + amount);
    }

    public void add(){
        add(1);
    }
}
