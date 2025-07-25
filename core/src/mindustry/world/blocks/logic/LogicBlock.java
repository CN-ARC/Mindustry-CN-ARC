package mindustry.world.blocks.logic;

import arc.Core;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.Label;
import arc.scene.ui.layout.*;
import arc.struct.Bits;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import arc.util.pooling.*;
import mindustry.ai.types.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.io.TypeIO.Boxed;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.meta.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.logic.LogicDialog.*;

public class LogicBlock extends Block{
    public static final int maxByteLen = 1024 * 100;
    private static final int maxLinks = 6000;
    public static final int maxNameLength = 32;

    public int maxInstructionScale = 5;
    public int instructionsPerTick = 1;
    //privileged only
    public int maxInstructionsPerTick = 40;
    public float range = 8 * 10;

    public LogicBlock(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        group = BlockGroup.logic;
        schematicPriority = 5;
        ignoreResizeConfig = true;

        //universal, no real requirements
        envEnabled = Env.any;

        config(byte[].class, (LogicBuild build, byte[] data) -> {
            if(!accessible()) return;

            build.readCompressed(data, true);
        });

        config(String.class, (LogicBuild build, String data) -> {
            if(!accessible() || !privileged) return;

            if(data != null && data.length() < maxNameLength){
                build.tag = data;
            }
        });

        config(Character.class, (LogicBuild build, Character data) -> {
            if(!accessible() || !privileged) return;

            build.iconTag = data;
        });

        config(Integer.class, (LogicBuild entity, Integer pos) -> {
            if(!accessible()) return;

            //if there is no valid link in the first place, nobody cares
            if(!entity.validLink(world.build(pos))) return;
            var lbuild = world.build(pos);
            int x = lbuild.tileX(), y = lbuild.tileY();

            LogicLink link = entity.links.find(l -> l.x == x && l.y == y);

            if(link != null){
                entity.links.remove(link);
                //disable when unlinking
                if(lbuild.block.autoResetEnabled && lbuild.lastDisabler == entity){
                    lbuild.enabled = true;
                }
            }else{
                entity.links.remove(l -> world.build(l.x, l.y) == lbuild);
                entity.links.add(new LogicLink(x, y, entity.findLinkName(lbuild.block), true));
            }

            entity.updateCode(entity.code, true, null);
        });
    }

    @Override
    public boolean checkForceDark(Tile tile){
        return !accessible();
    }

    public boolean accessible(){
        return !privileged || state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors || ui.logic.editing;
        //return !privileged || state.rules.editor|| state.playtestingMap != null || ARCVars.arcInfoControl;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }

    public static String getLinkName(Block block){
        String name = block.name;
        if(name.contains("-")){
            String[] split = name.split("-");
            //filter out 'large' at the end of block names
            if(split.length >= 2 && (split[split.length - 1].equals("large") || Strings.canParseFloat(split[split.length - 1]))){
                name = split[split.length - 2];
            }else{
                name = split[split.length - 1];
            }
        }
        return name;
    }

    public static byte[] compress(String code, Seq<LogicLink> links){
        return compress(code.getBytes(charset), links);
    }

    public static byte[] compress(byte[] bytes, Seq<LogicLink> links){
        try{
            var baos = new ByteArrayOutputStream();
            var stream = new DataOutputStream(new DeflaterOutputStream(baos));

            //current version of config format
            stream.write(1);

            //write string data
            stream.writeInt(bytes.length);
            stream.write(bytes);

            stream.writeInt(links.size);
            for(LogicLink link : links){
                stream.writeUTF(link.name);
                stream.writeShort(link.x);
                stream.writeShort(link.y);
            }

            stream.close();

            return baos.toByteArray();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        if(!privileged){
            stats.add(Stat.linkRange, range / 8, StatUnit.blocks);
            stats.add(Stat.instructions, instructionsPerTick * 60, StatUnit.perSecond);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(privileged) return;

        Drawf.circles(x*tilesize + offset, y*tilesize + offset, range);
    }

    @Override
    public Object pointConfig(Object config, Cons<Point2> transformer){
        if(config instanceof byte[] data){

            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))){
                //discard version for now
                stream.read();

                int bytelen = stream.readInt();

                if(bytelen > maxByteLen) throw new RuntimeException("Logic data too long or malformed! Length: " + bytelen);

                byte[] bytes = new byte[bytelen];
                stream.readFully(bytes);

                int total = Math.min(stream.readInt(), maxLinks);

                Seq<LogicLink> links = new Seq<>();

                for(int i = 0; i < total; i++){
                    String name = stream.readUTF();
                    short x = stream.readShort(), y = stream.readShort();

                    transformer.get(Tmp.p1.set(x, y));
                    links.add(new LogicLink(Tmp.p1.x, Tmp.p1.y, name, true));
                }

                return compress(bytes, links);
            }catch(IOException e){
                Log.err(e);
            }
        }
        return config;
    }

    public static class LogicLink{
        public boolean valid;
        public int x, y;
        public String name;
        public Building lastBuild;

        public LogicLink(int x, int y, String name, boolean valid){
            this.x = x;
            this.y = y;
            this.name = name;
            this.valid = valid;
        }

        public LogicLink copy(){
            return new LogicLink(x, y, name, valid);
        }
    }

    public class LogicBuild extends Building implements Ranged, LReadable, LWritable{
        /** logic "source code" as list of asm statements */
        public String code = "";
        public LExecutor executor = new LExecutor();
        public float accumulator = 0;
        public Seq<LogicLink> links = new Seq<>();
        public boolean checkedDuplicates = false;
        //dynamic only for privileged processors
        public int ipt = instructionsPerTick;
        /** Display name, for convenience. This is currently only available for world processors. */
        public @Nullable String tag;
        public char iconTag;

        Table settingTable = new Table();
        boolean showSettingTable = false;
        boolean showContent = false;

        float counter = 0f;

        boolean linkSimplify = false;

        /** Block of code to run after load. */
        public @Nullable Runnable loadBlock;

        {
            executor.privileged = privileged;
            executor.build = this;
        }

        public void readCompressed(byte[] data, boolean relative){
            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))){
                int version = stream.read();

                int bytelen = stream.readInt();
                if(bytelen > maxByteLen) throw new IOException("Malformed logic data! Length: " + bytelen);
                byte[] bytes = new byte[bytelen];
                stream.readFully(bytes);

                links.clear();

                int total = Math.min(stream.readInt(), maxLinks);

                if(version == 0){
                    //old version just had links, ignore those

                    for(int i = 0; i < total; i++){
                        stream.readInt();
                    }
                }else{
                    for(int i = 0; i < total; i++){
                        String name = stream.readUTF();
                        short x = stream.readShort(), y = stream.readShort();

                        if(relative){
                            x += tileX();
                            y += tileY();
                        }

                        Building build = world.build(x, y);

                        if(build != null){
                            String bestName = getLinkName(build.block);
                            if(!name.startsWith(bestName)){
                                name = findLinkName(build.block);
                            }
                        }

                        links.add(new LogicLink(x, y, name, false));
                    }
                }

                updateCode(new String(bytes, charset));
            }catch(Exception ignored){
                //invalid logic doesn't matter here
            }
        }

        public String findLinkName(Block block){
            String bname = getLinkName(block);
            Bits taken = new Bits(links.size);
            int max = 1;

            for(LogicLink others : links){
                if(others.name.startsWith(bname)){

                    String num = others.name.substring(bname.length());
                    try{
                        int val = Integer.parseInt(num);
                        taken.set(val);
                        max = Math.max(val, max);
                    }catch(NumberFormatException ignored){
                        //ignore failed parsing, it isn't relevant
                    }
                }
            }

            int outnum = 0;

            for(int i = 1; i < max + 2; i++){
                if(!taken.get(i)){
                    outnum = i;
                    break;
                }
            }

            return bname + outnum;
        }

        public void updateCode(String str){
            updateCode(str, false, null);
        }

        public void updateCode(String str, boolean keep, Cons<LAssembler> assemble){
            if(str != null){
                code = str;

                try{
                    //create assembler to store extra variables
                    LAssembler asm = LAssembler.assemble(str, privileged);

                    //store connections
                    for(LogicLink link : links){
                        link.valid = validLink(world.build(link.x, link.y));
                        if(link.valid){
                            asm.putConst(link.name, world.build(link.x, link.y));
                        }
                    }

                    //store link objects
                    executor.links = new Building[links.count(l -> l.valid)];
                    executor.linkIds.clear();

                    int index = 0;
                    for(LogicLink link : links){
                        if(link.valid){
                            Building build = world.build(link.x, link.y);
                            executor.links[index ++] = build;
                            if(build != null) executor.linkIds.add(build.id);
                        }
                    }

                    asm.putConst("@links", executor.links.length);
                    asm.putConst("@ipt", instructionsPerTick);

                    Object oldUnit = null;

                    if(keep){
                        oldUnit = executor.unit.objval;
                        //store any older variables
                        for(LVar var : executor.vars){
                            if(!var.constant){
                                LVar dest = asm.getVar(var.name);
                                if(dest != null && !dest.constant){
                                    dest.set(var);
                                }
                            }
                        }
                    }

                    //inject any extra variables
                    if(assemble != null){
                        assemble.get(asm);

                        if(oldUnit == null && asm.getVar("@unit") != null && asm.getVar("@unit").objval instanceof Unit u){
                            oldUnit = u;
                        }
                    }

                    asm.getVar("@this").setconst(this);
                    asm.putConst("@thisx", World.conv(x));
                    asm.putConst("@thisy", World.conv(y));

                    executor.load(asm);
                    executor.unit.objval = oldUnit;
                    executor.unit.isobj = true;
                }catch(Exception e){
                    //handle malformed code and replace it with nothing
                    executor.load(LAssembler.assemble(code = "", privileged));
                }
            }
        }

        //editor-only processors cannot be damaged or destroyed
        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public boolean displayable(){
            return accessible();
        }

        @Override
        public void damage(float damage){
            if(!privileged){
                super.damage(damage);
            }
        }

        @Override
        public void removeFromProximity(){
            super.removeFromProximity();

            for(var link : executor.links){
                if(!link.enabled && link.lastDisabler == this){
                    link.enabled = true;
                }
            }
        }

        @Override
        public Cursor getCursor(){
            return !accessible() ? SystemCursor.arrow : super.getCursor();
        }

        //logic blocks cause write problems when picked up
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){
            //load up code from read()
            if(loadBlock != null){
                loadBlock.run();
                loadBlock = null;
            }

            executor.team = team;

            if(!checkedDuplicates){
                checkedDuplicates = true;
                var removal = new IntSet();
                var removeLinks = new Seq<LogicLink>();
                for(var link : links){
                    var build = world.build(link.x, link.y);
                    if(build != null){
                        if(!removal.add(build.id)){
                            removeLinks.add(link);
                        }
                    }
                }
                links.removeAll(removeLinks);
            }

            //check for previously invalid links to add after configuration
            boolean changed = false, updates = true;

            while(updates){
                updates = false;

                for(int i = 0; i < links.size; i++){
                    LogicLink l = links.get(i);

                    var cur = world.build(l.x, l.y);

                    boolean valid = validLink(cur);
                    if(l.lastBuild == null) l.lastBuild = cur;
                    if(valid != l.valid || l.lastBuild != cur){
                        l.lastBuild = cur;
                        changed = true;
                        l.valid = valid;
                        if(valid){

                            //this prevents conflicts
                            l.name = "";
                            //finds a new matching name after toggling
                            l.name = findLinkName(cur.block);

                            //remove redundant links
                            links.removeAll(o -> world.build(o.x, o.y) == cur && o != l);

                            //break to prevent concurrent modification
                            updates = true;
                            break;
                        }
                    }
                }
            }

            if(changed){
                updateCode(code, true, null);
            }

            if(!privileged){
                ipt = instructionsPerTick;
            }

            if(state.rules.disableWorldProcessors && privileged) return;

            if(enabled && executor.initialized()){
                accumulator += edelta() * ipt;

                if(accumulator > maxInstructionScale * ipt) accumulator = maxInstructionScale * ipt;

                while(accumulator >= 1f){
                    executor.runOnce();
                    accumulator --;
                    if(executor.yield){
                        executor.yield = false;
                        break;
                    }
                }
            }
        }

        @Override
        public boolean readable(LExecutor exec){
            return exec.privileged || (this.team == exec.team && !this.block.privileged);
        }

        @Override
        public void read(LVar position, LVar output){
            if(position.isobj && position.objval instanceof String varName){
                LVar ret = executor.optionalVar(varName);
                if(ret == null){
                    output.setnum(Double.NaN);
                    return;
                }
                if(output.constant) return;
                output.set(ret);
            }
        }

        @Override
        public boolean writable(LExecutor exec){
            return exec.privileged || (this.team == exec.team && !this.block.privileged);
        }

        @Override
        public void write(LVar position, LVar value){
            if(position.isobj && position.objval instanceof String varName){
                LVar at = executor.optionalVar(varName);
                if(at == null || at.constant) return;
                at.set(value);
            }
        }

        @Override
        public byte[] config(){
            return compress(code, relativeConnections());
        }

        public Seq<LogicLink> relativeConnections(){
            var copy = new Seq<LogicLink>(links.size);
            for(var l : links){
                var c = l.copy();
                c.x -= tileX();
                c.y -= tileY();
                copy.add(c);
            }
            return copy;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();

            if(!privileged){
                Drawf.circles(x, y, range);
            }

            for(LogicLink l : links){
                Building build = world.build(l.x, l.y);
                if(validLink(build)){
                    Drawf.square(build.x, build.y, build.block.size * tilesize / 2f + 1f, Pal.place);
                }
            }

            //draw top text on separate layer
            //draw link order
            int i = 0;
            for(LogicLink l : links){
                Building build = world.build(l.x, l.y);
                if(validLink(build)){
                    build.block.drawPlaceText(linkSimplify ? i++ + "" : l.name + "[" + i++ + "]", build.tileX(), build.tileY(), true);
                }
            }
        }

        @Override
        public void drawSelect(){
            if(!accessible()) return;

            Groups.unit.each(u -> u.controller() instanceof LogicAI ai && ai.controller == this, unit -> {
                Drawf.square(unit.x, unit.y, unit.hitSize, unit.rotation + 45);
            });

            //draw tag over processor (world processor only)
            if(!(renderer.pixelate || !privileged || tag == null || tag.isEmpty())){
                Font font = Fonts.outline;
                GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                boolean ints = font.usesIntegerPositions();
                font.getData().setScale(1 / 4f / Scl.scl(1f));
                font.setUseIntegerPositions(false);

                l.setText(font, tag, Color.white, 90f, Align.left, true);
                float offset = 1f;

                //Draw.color(0f, 0f, 0f, 0.1f);
                //Fill.rect(x, y + tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
                Draw.color();
                font.setColor(1f, 1f, 1f, 0.5f);
                font.draw(tag, x - l.width/2f, y + tilesize + 2f - offset, 90f, Align.left, true);
                font.setUseIntegerPositions(ints);

                font.getData().setScale(1f);

                Pools.free(l);
            }

            if(iconTag != 0){
                TextureRegion icon = Fonts.getLargeIcon(Fonts.unicodeToName(iconTag));
                if(icon.found()){
                    Draw.alpha(0.5f);

                    Draw.rect(icon, x, y, tilesize, tilesize / icon.ratio());

                    Draw.color();
                }
            }
        }

        public boolean validLink(Building other){
            return other != null && other.isValid() && (privileged || (!other.block.privileged && other.team == team && other.within(this, range + other.block.size*tilesize/2f))) && !(other instanceof ConstructBuild);
        }

        private void rebuildSettingTable() {
            settingTable.clear();
            if (!showSettingTable) return;
            settingTable.setColor(Color.lightGray);
            settingTable.update(() -> {
                counter += Time.delta;
                if (counter > period && refreshing) {
                    counter = 0;
                }
            });
            settingTable.table(t -> {
                t.button(Icon.copy, Styles.cleari, () -> {
                    Core.app.setClipboardText(code);
                    arcui.arcInfo("已复制逻辑");
                }).size(40);
                t.button(Icon.download, Styles.cleari, () -> {
                    updateCode(Core.app.getClipboardText().replace("\r\n", "\n"));
                    arcui.arcInfo("已导入逻辑(仅单机生效)");
                }).size(40);
                t.button(Icon.trash, Styles.cleari, () -> {
                    code = "";
                    updateCode(code);
                    arcui.arcInfo("已清除逻辑(仅单机生效)");
                }).size(40);
                t.button(Icon.chatSmall, Styles.cleari, () -> {
                    linkSimplify = !linkSimplify;
                    arcui.arcInfo(linkSimplify ? "仅显示linkindex" : "显示方块名和linkindex");
                }).size(40);
                t.button(Icon.info, Styles.cleari, () -> {
                    showContent = !showContent;
                    rebuildSettingTable();
                }).size(40);
            });
            if (showContent && !code.isEmpty()) {
                settingTable.row();
                settingTable.pane(t -> {
                    for (var s : executor.vars) {
                        if(s.name.startsWith("___")) continue;
                        String text = arcVarsText(s);
                        t.table(tt -> {
                            tt.labelWrap(s.name + "").color(arcVarsColor(s)).width(100f);
                            Label label = tt.labelWrap(" : " + text).width(200f).get();
                            tt.update(() -> {
                                if (counter + Time.delta > period && refreshing) {
                                    label.setText(arcVarsText(s));
                                }
                            });
                        });
                        t.row();
                    }
                }).maxHeight(400f);

            }
        }

        @Override
        public boolean shouldShowConfigure(Player player){
            return player == Vars.player;
        }

        @Override
        public void buildConfiguration(Table table){
            if (Core.settings.getBool("logicSupport")){
                table.table(t -> {
                    t.button(Icon.pencil, Styles.cleari, this::showEditDialog).size(40);
                    t.button(Icon.settings, Styles.cleari, () -> {
                        showSettingTable = !showSettingTable;
                        rebuildSettingTable();
                    }).size(40);
                });
            }else{
                table.button(Icon.pencil, Styles.cleari, this::showEditDialog).size(40);
            }
            table.row();
            table.add(settingTable);
        }

        public void showEditDialog(){
            showEditDialog(false);
        }

        public void showEditDialog(boolean forceEditor){
            ui.logic.show(code, executor, privileged, code -> {
                boolean prev = state.rules.editor;
                //this is a hack to allow configuration to work correctly in the editor for privileged processors
                if(forceEditor) state.rules.editor = true;
                configure(compress(code, relativeConnections()));
                state.rules.editor = prev;
            });
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                deselect();
                return false;
            }

            if(validLink(other)){
                configure(other.pos());
                return false;
            }

            return super.onConfigureBuildTapped(other);
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            byte[] compressed = compress(code, links);
            write.i(compressed.length);
            write.b(compressed);

            //write only the non-constant variables
            int count = Structs.count(executor.vars, v -> (!v.constant || v == executor.unit) && !(v.isobj && v.objval == null));

            write.i(count);
            for(int i = 0; i < executor.vars.length; i++){
                LVar v = executor.vars[i];

                //null is the default variable value, so waste no time serializing that
                if(v.isobj && v.objval == null) continue;

                //skip constants
                if(v.constant && v != executor.unit) continue;

                //write the name and the object value
                write.str(v.name);

                Object value = v.isobj ? v.objval : v.numval;
                TypeIO.writeObject(write, value);
            }

            //no memory
            write.i(0);

            if(privileged){
                write.s(Mathf.clamp(ipt, 1, maxInstructionsPerTick));
            }

            TypeIO.writeString(write, tag);
            write.s(iconTag);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                int compl = read.i();
                byte[] bytes = new byte[compl];
                read.b(bytes);
                readCompressed(bytes, false);
            }else{
                code = read.str();
                links.clear();
                short total = read.s();
                for(int i = 0; i < total; i++){
                    read.i();
                }
            }

            int varcount = read.i();

            //variables need to be temporarily stored in an array until they can be used
            String[] names = new String[varcount];
            Object[] values = new Object[varcount];

            for(int i = 0; i < varcount; i++){
                String name = read.str();
                Object value = TypeIO.readObjectBoxed(read, true);

                names[i] = name;
                values[i] = value;
            }

            int memory = read.i();
            //skip memory, it isn't used anymore
            read.skip(memory * 8);

            loadBlock = () -> updateCode(code, false, asm -> {
                //load up the variables that were stored
                for(int i = 0; i < varcount; i++){
                    LVar var = asm.getVar(names[i]);
                    if(var != null && (!var.constant || var.name.equals("@unit"))){
                        var value = values[i];
                        if(value instanceof Boxed<?> boxed) value = boxed.unbox();

                        if(value instanceof Number num){
                            var.numval = num.doubleValue();
                            var.isobj = false;
                        }else{
                            var.objval = value;
                            var.isobj = true;
                        }
                    }
                }
            });

            if(privileged && revision >= 2){
                ipt = Mathf.clamp(read.s(), 1, maxInstructionsPerTick);
            }

            if(revision >= 3){
                tag = TypeIO.readString(read);
                iconTag = (char)read.us();
            }

        }
    }
}
