package mindustry.arcreeper;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.io.SaveFileReader;
import mindustry.io.SaveVersion;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class SporeSave{
    private static final String chunkName = "arc-spores";
    private static final short version = 1;

    private static boolean registered;

    private SporeSave(){
    }

    public static void init(){
        if(registered) return;
        registered = true;

        SaveVersion.addCustomChunk(chunkName, new SaveFileReader.CustomChunk(){
            @Override
            public void write(DataOutput stream) throws IOException{
                Writes write = new Writes(stream);

                write.s(version);
                write.i(SporeCore.generation);
                write.i(SporeCore.nextId());

                int count = 0;
                for(Spore spore : SporeCore.all()){
                    if(!spore.removed){
                        count++;
                    }
                }

                write.i(count);

                for(Spore spore : SporeCore.all()){
                    if(!spore.removed){
                        spore.write(write);
                    }
                }
            }

            @Override
            public void read(DataInput stream) throws IOException{
                Reads read = new Reads(stream);

                short fileVersion = read.s();
                int generation = read.i();
                int nextId = read.i();
                int count = read.i();

                SporeCore.resetLocal();
                SporeCore.setSaveState(generation, nextId);

                for(int i = 0; i < count; i++){
                    Spore spore = new Spore();
                    spore.read(read);

                    if(fileVersion >= 1 && !spore.removed){
                        SporeCore.addLocal(spore);
                    }
                }
            }

            @Override
            public boolean shouldWrite(){
                return CreeperCore.enabled() || SporeCore.hasAny();
            }

            @Override
            public boolean writeNet(){
                return true;
            }
        });
    }
}