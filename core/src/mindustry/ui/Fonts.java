package mindustry.ui;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.assets.*;
import arc.files.*;
import arc.freetype.*;
import arc.freetype.FreeTypeFontGenerator.*;
import arc.freetype.FreetypeFontLoader.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.RFuncs;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;

import java.io.*;

public class Fonts{

    static String fontPack = "fonts/font.woff";
    private static final String mainFont = "fonts/font.woff";
    private static final ObjectSet<String> unscaled = ObjectSet.with("iconLarge", "logic");
    private static ObjectIntMap<String> unicodeIcons = new ObjectIntMap<>();
    private static IntMap<String> unicodeToName = new IntMap<>();
    public static ObjectMap<String, String> stringIcons = new ObjectMap<>();
    private static ObjectMap<String, TextureRegion> largeIcons = new ObjectMap<>();

    public static Font def, outline, icon, iconLarge, tech, logic;

    public static int getUnicode(String content){
        return unicodeIcons.get(content, 0);
    }

    public static String getUnicodeStr(String content){
        return stringIcons.get(content, "");
    }

    public static boolean hasUnicodeStr(String content){
        return stringIcons.containsKey(content);
    }

    /** Called from a static context to make the cursor appear immediately upon startup.*/
    public static void loadSystemCursors(){
        SystemCursor.arrow.set(RFuncs.customCursor("cursor", cursorScale()));
        SystemCursor.hand.set(RFuncs.customCursor("hand", cursorScale()));
        SystemCursor.ibeam.set(RFuncs.customCursor("ibeam", cursorScale()));

        Core.graphics.restoreCursor();
    }

    public static int cursorScale(){
        return 1;
    }

    public static void loadFonts(){
        largeIcons.clear();
        FreeTypeFontParameter param = fontParameter();

        if (Core.settings.getInt("fontSet") == 1) fontPack = "fonts/cute.woff";
        else if (Core.settings.getInt("fontSet") == 2) fontPack = "fonts/kai.woff";
        Core.assets.load("default", Font.class, new FreeTypeFontLoaderParameter(fontPack, param)).loaded = f -> Fonts.def = f;

        Core.assets.load("icon", Font.class, new FreeTypeFontLoaderParameter("fonts/icon.ttf", new FreeTypeFontParameter(){{
            size = 30;
            incremental = true;
            characters = "\0";
        }})).loaded = f -> Fonts.icon = f;

        Core.assets.load("iconLarge", Font.class, new FreeTypeFontLoaderParameter("fonts/icon.ttf", new FreeTypeFontParameter(){{
            size = 48;
            incremental = false;
            characters = "\0" + Iconc.all;
            borderWidth = 5f;
            borderColor = Color.darkGray;
        }})).loaded = f -> Fonts.iconLarge = f;

        Core.assets.load("logic", Font.class, new FreeTypeFontLoaderParameter("fonts/logic.ttf", new FreeTypeFontParameter(){{
            size = 16;
            //generated all at once, it's fast enough anyway
            incremental = false;
            //ASCII only
            characters = "\0ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*";
        }})).loaded = f -> Fonts.logic = f;
    }

    public static @Nullable String unicodeToName(int unicode){
        return unicodeToName.get(unicode, () -> Iconc.codeToName.get(unicode));
    }

    public static TextureRegion getLargeIcon(String name){
        return largeIcons.get(name, () -> {
            var region = new TextureRegion();
            int code = Iconc.codes.get(name, '\uF8D4');
            var glyph = iconLarge.getData().getGlyph((char)code);
            if(glyph == null) return Core.atlas.find(name);
            region.set(iconLarge.getRegion().texture);
            region.set(glyph.u, glyph.v2, glyph.u2, glyph.v);
            return region;
        });
    }

    public static void registerIcon(String name, String regionName, int ch, TextureRegion region){
        int size = (int)(Fonts.def.getData().lineHeight/Fonts.def.getData().scaleY);

        unicodeIcons.put(name, ch);
        stringIcons.put(name, ((char)ch) + "");
        unicodeToName.put(ch, regionName);

        Vec2 out = Scaling.fit.apply(region.width, region.height, size, size);

        Glyph glyph = new Glyph();
        glyph.id = ch;
        glyph.srcX = 0;
        glyph.srcY = 0;
        glyph.width = (int)out.x;
        glyph.height = (int)out.y;
        glyph.u = region.u;
        glyph.v = region.v2;
        glyph.u2 = region.u2;
        glyph.v2 = region.v;
        glyph.xoffset = 0;
        glyph.yoffset = -size;
        glyph.xadvance = size;
        glyph.kerning = null;
        glyph.fixedWidth = true;
        glyph.page = 0;
        Fonts.def.getData().setGlyph(ch, glyph);
        Fonts.outline.getData().setGlyph(ch, glyph);
    }

    public static void loadContentIcons(){
        Texture uitex = Core.atlas.find("logo").texture;

        try(var reader = Core.files.internal("icons/icons.properties").reader(Vars.bufferSize)){
            String line;
            while((line = reader.readLine()) != null){
                String[] split = line.split("=");
                String[] nametex = split[1].split("\\|");
                String character = split[0], texture = nametex[1];
                int ch = Integer.parseInt(character);
                TextureRegion region = Core.atlas.find(texture);

                if(region.texture != uitex){
                    continue;
                }

                registerIcon(nametex[0], texture, ch, region);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        stringIcons.put("alphachan", stringIcons.get("alphaaaa"));

        //TODO: mod emojis  can't work because most mod icons are not on the UI page!
        /*
        if(Vars.mods.list().contains(m -> m.shouldBeEnabled())){
            ContentType[] types = {ContentType.liquid, ContentType.item, ContentType.block, ContentType.status, ContentType.unit};
            int startChar = 0xE000 + 1;

            for(var type : types){
                for(var cont : Vars.content.getBy(type)){
                    if(!cont.isVanilla() && cont instanceof UnlockableContent u && u.uiIcon.found()){
                        int id = startChar;

                        registerIcon(u.name, u.uiIcon instanceof AtlasRegion atlas ? atlas.name : u.name, id, u.uiIcon);

                        startChar ++;
                    }
                }
            }
        }*/

        for(Team team : Team.baseTeams){
            team.emoji = stringIcons.get(team.name, "");
        }
    }

    public static void loadContentIconsHeadless(){
        try(var reader = Core.files.internal("icons/icons.properties").reader(Vars.bufferSize)){
            String line;
            while((line = reader.readLine()) != null){
                String[] split = line.split("=");
                String[] nametex = split[1].split("\\|");
                String character = split[0];
                int ch = Integer.parseInt(character);

                unicodeIcons.put(nametex[0], ch);
                stringIcons.put(nametex[0], ((char)ch) + "");
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        stringIcons.put("alphachan", stringIcons.get("alphaaaa"));

        for(Team team : Team.baseTeams){
            team.emoji = stringIcons.get(team.name, "");
        }
    }

    /** Called from a static context for use in the loading screen.*/
    public static void loadDefaultFont(){
        int max = Gl.getInt(Gl.maxTextureSize);

        UI.packer = new PixmapPacker(max >= 4096 ? 4096 : 2048, 2048, 2, true);
        Core.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(Core.files::internal));
        Core.assets.setLoader(Font.class, null, new FreetypeFontLoader(Core.files::internal){
            ObjectSet<FreeTypeFontParameter> scaled = new ObjectSet<>();

            @Override
            public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
                if(fileName.equals("outline")){
                    parameter.fontParameters.borderWidth = Scl.scl(2f);
                    parameter.fontParameters.spaceX -= parameter.fontParameters.borderWidth;
                }

                if(!scaled.contains(parameter.fontParameters) && !unscaled.contains(fileName)){
                    parameter.fontParameters.size = (int)(Scl.scl(parameter.fontParameters.size));
                    scaled.add(parameter.fontParameters);
                }

                parameter.fontParameters.magFilter = TextureFilter.linear;
                parameter.fontParameters.minFilter = TextureFilter.linear;
                parameter.fontParameters.packer = UI.packer;
                return super.loadSync(manager, fileName, file, parameter);
            }
        });

        FreeTypeFontParameter param = new FreeTypeFontParameter(){{
            borderColor = Color.darkGray;
            incremental = true;
            size = getFontSize();
        }};

        if (Core.settings.getInt("fontSet") == 1) fontPack = "fonts/cute.woff";
        else if (Core.settings.getInt("fontSet") == 2) fontPack = "fonts/kai.woff";
        Core.assets.load("outline", Font.class, new FreeTypeFontLoaderParameter(fontPack, param)).loaded = t -> Fonts.outline = t;

        String techPack = "fonts/tech.ttf";
        if (Core.settings.getInt("fontSet") == 1) techPack = "fonts/cuteTech.ttf";
        else if (Core.settings.getInt("fontSet") == 2) techPack = "fonts/kaiTech.ttf";

        Core.assets.load("tech", Font.class, new FreeTypeFontLoaderParameter(techPack, new FreeTypeFontParameter(){{
            size = getFontSize();
        }})).loaded = f -> {
            Fonts.tech = f;
            Fonts.tech.getData().down *= 1.5f;
        };
    }

    /** Merges the UI and font atlas together for better performance. */
    public static void mergeFontAtlas(TextureAtlas atlas){
        //grab all textures from the ui page, remove all the regions assigned to it, then copy them over to UI.packer and replace the texture in this atlas.

        //grab old UI texture and regions...
        Texture texture = atlas.find("logo").texture;

        Page page = UI.packer.getPages().first();

        Seq<AtlasRegion> regions = atlas.getRegions().select(t -> t.texture == texture);
        for(AtlasRegion region : regions){
            //get new pack rect
            page.setDirty(false);
            Rect rect = UI.packer.pack(region.name, atlas.getPixmap(region), region.splits, region.pads);

            //set new texture
            region.texture = UI.packer.getPages().first().getTexture();
            //set its new position
            region.set((int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);
            //add old texture
            atlas.getTextures().add(region.texture);
            //clear it
            region.pixmapRegion = null;
        }

        //remove old texture, it will no longer be used
        atlas.getTextures().remove(texture);
        texture.dispose();
        atlas.disposePixmap(texture);

        page.setDirty(true);
        page.updateTexture(TextureFilter.linear, TextureFilter.linear, false);
    }

    public static TextureRegionDrawable getGlyph(Font font, char glyph){
        Glyph found = font.getData().getGlyph(glyph);
        if(found == null){
            Log.warn("No icon found for glyph: @ (@)", glyph, (int)glyph);
            found = font.getData().getGlyph('F');
        }
        Glyph g = found;

        float size = Math.max(g.width, g.height);
        TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion(font.getRegion().texture, g.u, g.v2, g.u2, g.v)){
            @Override
            public void draw(float x, float y, float width, float height){
                Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
                float cx = x + width/2f - g.width/2f, cy = y + height/2f - g.height/2f;
                cx = (int)cx;
                cy = (int)cy;
                Draw.rect(region, cx + g.width/2f, cy + g.height/2f, g.width, g.height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
                width *= scaleX;
                height *= scaleY;
                Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
                float cx = x + width/2f - g.width/2f, cy = y + height/2f - g.height/2f;
                cx = (int)cx;
                cy = (int)cy;
                originX = g.width/2f;
                originY = g.height/2f;
                Draw.rect(region, cx + g.width/2f, cy + g.height/2f, g.width * scaleX, g.height * scaleY, originX, originY, rotation);
            }

            @Override
            public float imageSize(){
                return size;
            }
        };

        draw.setMinWidth(size);
        draw.setMinHeight(size);
        return draw;
    }

    static int getFontSize(){
        if (Core.settings.getInt("fontSize") < 5) Core.settings.put("fontSize",10);
        float multiplier = Core.settings.getInt("fontSize") / 10f;
        return (int) (18 * multiplier);
    }

    static FreeTypeFontParameter fontParameter(){
        return new FreeTypeFontParameter(){{
            size = getFontSize();
            shadowColor = Color.darkGray;
            shadowOffsetY = 2;
            incremental = true;
        }};
    }
}
