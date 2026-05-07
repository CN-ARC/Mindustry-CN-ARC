package mindustry.arcreeper;

import mindustry.io.*;
import java.io.*;

public final class CreeperSave{
    private static final String chunkName = "arc-creeper";
    private static boolean registered = false;

    private CreeperSave(){
    }

    public static void init(){
        if(registered) return;
        registered = true;

        SaveVersion.addCustomChunk(chunkName, new SaveFileReader.CustomChunk(){
            @Override
            public void write(DataOutput stream) throws IOException{
                CreeperCore.creeperTile.writeSnapshot(new arc.util.io.Writes(stream));
            }

            @Override
            public void read(DataInput stream) throws IOException{
                CreeperCore.creeperTile.readSnapshot(new arc.util.io.Reads(stream));
            }

            @Override
            public boolean shouldWrite(){
                return CreeperCore.enabled();
            }

            @Override
            public boolean writeNet(){
                return true;
            }
        });
    }
}
