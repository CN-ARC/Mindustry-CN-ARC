package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.Pathfinder.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.input.InputHandler;
import mindustry.type.ammo.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.input.*;

import static arc.graphics.g2d.Draw.*;
import static mindustry.Vars.*;

public class UnitType extends UnlockableContent{
    public static final float shadowTX = -12, shadowTY = -13;
    private static final Vec2 legOffset = new Vec2();

    //MI2 unit transparency
    private static float legTrans = 1f, unitTrans = 1f;


    /** Environmental flags that are *all* required for this unit to function. 0 = any environment */
    public int envRequired = 0;
    /** The environment flags that this unit can function in. If the env matches any of these, it will be enabled. */
    public int envEnabled = Env.terrestrial;
    /** The environment flags that this unit *cannot* function in. If the env matches any of these, it will explode or be disabled. */
    public int envDisabled = Env.scorching;

    /** movement speed (world units/t) */
    public float speed = 1.1f,
    /** multiplier for speed when boosting */
    boostMultiplier = 1f,
    /** body rotation speed in degrees/t */
    rotateSpeed = 5f,
    /** mech base rotation speed in degrees/t*/
    baseRotateSpeed = 5f,
    /** movement drag as fraction */
    drag = 0.3f,
    /** acceleration as fraction of speed */
    accel = 0.5f,
    /** size of one side of the hitbox square */
    hitSize = 6f,
    /** shake on each step for leg/mech units */
    stepShake = -1f,
    /** ripple / dust size for legged units */
    rippleScale = 1f,
    /** boosting rise speed as fraction */
    riseSpeed = 0.08f,
    /** how fast this unit falls when not boosting */
    fallSpeed = 0.018f,
    /** how many ticks it takes this missile to accelerate to full speed */
    missileAccelTime = 0f,
    /** raw health amount */
    health = 200f,
    /** incoming damage is reduced by this amount */
    armor = 0f,
    /** minimum range of any weapon; used for approaching targets. can be overridden by setting a value > 0. */
    range = -1,
    /** maximum range of any weapon */
    maxRange = -1f,
    /** range at which this unit can mine ores */
    mineRange = 70f,
    /** range at which this unit can build */
    buildRange = Vars.buildingRange,
    /** multiplier for damage this (flying) unit deals when crashing on enemy things */
    crashDamageMultiplier = 1f,
    /** a VERY ROUGH estimate of unit DPS; initialized in init() */
    dpsEstimate = -1,
    /** graphics clipping size; <0 to calculate automatically */
    clipSize = -1,
    /** multiplier for how slowly this unit drowns - higher numbers, slower drowning. */
    drownTimeMultiplier = 1f,
    /** fractional movement speed penalty for this unit when it is moving in the opposite direction that it is facing */
    strafePenalty = 0.5f,
    /** multiplier for cost of research in tech tree */
    researchCostMultiplier = 50,

    /** for ground units, the layer upon which this unit is drawn */
    groundLayer = Layer.groundUnit,
    /** Payload capacity of this unit in world units^2 */
    payloadCapacity = 8,
    /** building speed multiplier; <0 to disable. */
    buildSpeed = -1f,
    /** Minimum distance from this unit that weapons can target. Prevents units from firing "inside" the unit. */
    aimDst = -1f,
    /** Visual offset of build beam from front. */
    buildBeamOffset = 3.8f,
    /** WIP: Units of low priority will always be ignored in favor of those with higher priority, regardless of distance. */
    targetPriority = 0f,
    /** Elevation of shadow drawn under this (ground) unit. Visual only. */
    shadowElevation = -1f,
    /** Scale for length of shadow drawn under this unit. Does nothing if this unit has no shadow. */
    shadowElevationScl = 1f,
    /** backwards engine offset from center of unit */
    engineOffset = 5f,
    /** main engine radius */
    engineSize = 2.5f,
    /** layer of all engines (<0 for default) */
    engineLayer = -1f,
    /** visual backwards offset of items on unit */
    itemOffsetY = 3f,
    /** radius of light emitted, <0 for default */
    lightRadius = -1f,
    /** light color opacity*/
    lightOpacity = 0.6f,
    /** fog view radius in tiles. <0 for automatic radius. */
    fogRadius = -1f,

    /** horizontal offset of wave trail in naval units */
    waveTrailX = 4f,
    /** vertical offset of wave trail in naval units  */
    waveTrailY = -3f,
    /** width of all trails (including naval ones) */
    trailScl = 1f;

    /** if true, this unit counts as an enemy in the wave counter (usually false for support-only units) */
    public boolean isEnemy = true,
    /** If true, the unit is always at elevation 1. */
    flying = false,
    /** whether this unit tries to attack air units */
    targetAir = true,
    /** whether this unit tries to attack ground units */
    targetGround = true,
    /** if true, this unit will attempt to face its target when shooting/aiming at it */
    faceTarget = true,
    /** AI flag: if true, this flying unit circles around its target like a bomber */
    circleTarget = false,
    /** if true, this unit can boost into the air if a player/processors controls it*/
    canBoost = false,
    /** if false, logic processors cannot control this unit */
    logicControllable = true,
    /** if false, players cannot control this unit */
    playerControllable = true,
    /** if false, this unit cannot be moved into payloads */
    allowedInPayloads = true,
    /** if false, this unit cannot be hit by bullets or explosions*/
    hittable = true,
    /** if false, this unit does not take damage and cannot be kill() / destroy()-ed. */
    killable = true,
    /** if false, this unit is not targeted by anything. */
    targetable = true,
    /** if true, this unit can be hit/targeted when it has payloads (assuming hittable/targetable is false) */
    vulnerableWithPayloads = false,
    /** if true, this payload unit can pick up units */
    pickupUnits = true,
    /** if false, this unit does not physically collide with others. */
    physics = true,
    /** if true, this ground unit will drown in deep liquids. */
    canDrown = true,
    /** if false, this unit ignores the unit cap and can be spawned infinitely */
    useUnitCap = true,
    /** if true, this core unit will "dock" to other units, making it re-appear when "undocking". */
    coreUnitDock = false,
    /** if false, no falling "corpse" is created when this unit dies. */
    createWreck = true,
    /** if false, no scorch marks are created when this unit dies */
    createScorch = true,
    /** if true, this unit will be drawn under effects/bullets; this is a visual change only. */
    lowAltitude = false,
    /** if true, this unit will look at whatever it is building */
    rotateToBuilding = true,
    /** if true and this is a legged unit, this unit can walk over blocks. */
    allowLegStep = false,
    /** for legged units, setting this to false forces it to be on the ground physics layer. */
    legPhysicsLayer = true,
    /** if true, this unit cannot drown, and will not be affected by the floor under it. */
    hovering = false,
    /** if true, this unit can move in any direction regardless of rotation. if false, this unit can only move in the direction it is facing. */
    omniMovement = true,
    /** if true, the unit faces its moving direction before actually moving. */
    rotateMoveFirst = false,
    /** if true, this unit flashes when being healed */
    healFlash = true,
    /** whether the unit can heal blocks. Initialized in init() */
    canHeal = false,
    /** if true, all weapons will attack the same target. */
    singleTarget = false,
    /** if true, this unit will be able to have multiple targets, even if it only has one mirrored weapon. */
    forceMultiTarget = false,
    /** if false, this unit has no weapons that can attack. */
    canAttack = true,
    /** if true, this unit won't show up in the database or various other UIs. */
    hidden = false,
    /** if true, this unit is for internal use only and does not have a sprite generated. */
    internal = false,
    /** if true, this unit is detected as naval - do NOT assign this manually! Initialized in init() */
    naval = false,

    /** if true, this modded unit always has a -outline region generated for its base. Normally, outlines are ignored if there are no top = false weapons. */
    alwaysCreateOutline = false,
    /** if true, this unit has a square shadow. */
    squareShape = false,
    /** if true, this unit will draw its building beam towards blocks. */
    drawBuildBeam = true,
    /** if false, the team indicator/cell is not drawn. */
    drawCell = true,
    /** if false, carried items are not drawn. */
    drawItems = true,
    /** if false, the unit shield (usually seen in waves) is not drawn. */
    drawShields = true,
    /** if false, the unit body is not drawn. */
    drawBody = true;

    /** If `flying` and this is true, the unit can appear on the title screen */
    public boolean onTitleScreen = true;
    /** The default AI controller to assign on creation. */
    public Prov<? extends UnitController> defaultController = () -> !flying ? new GroundAI() : new FlyingAI();

    /** The default AI controller to assign on creation. */
    public Prov<? extends UnitController> aiController = () -> !flying ? new GroundAI() : new FlyingAI();
    /** Function that chooses AI controller based on unit entity. */
    public Func<Unit, ? extends UnitController> controller = u -> !playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? aiController.get() : new CommandAI();
    /** Creates a new instance of this unit class. */
    public Prov<? extends Unit> constructor;

    /** list of "abilities", which are various behaviors that update each frame */
    public Seq<Ability> abilities = new Seq<>();
    /** All weapons that this unit will shoot with. */
    public Seq<Weapon> weapons = new Seq<>();
    /** None of the status effects in this set can be applied to this unit. */
    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();

    /** color that this unit flashes when getting healed (if healFlash is true) */
    public Color healColor = Pal.heal;
    /** Color of light that this unit produces when lighting is enabled in the map. */
    public Color lightColor = Pal.powerLight;
    /** sound played when this unit explodes (*not* when it is shot down) */
    public Sound deathSound = Sounds.bang;
    /** effect that this unit emits when falling */
    public Effect fallEffect = Fx.fallSmoke;
    /** effect created at engine when unit falls. */
    public Effect fallEngineEffect = Fx.fallSmoke;
    /** effect created when this unit dies */
    public Effect deathExplosionEffect = Fx.dynamicExplosion;
    /** optional effect created when this tank moves */
    public @Nullable Effect treadEffect;
    /** extra (usually animated) visual parts */
    public Seq<DrawPart> parts = new Seq<>(DrawPart.class);
    /** list of engines, or "thrusters" */
    public Seq<UnitEngine> engines = new Seq<>();
    /** if false, the thruster is always displayed at its normal size regardless of elevation */
    public boolean useEngineElevation = true;
    /** override for all engine colors */
    public @Nullable Color engineColor = null;
    /** color for inner portions of engines */
    public Color engineColorInner = Color.white;
    /** length of engine trail (if flying) or wave trail (if naval) */
    public int trailLength = 0;
    /** override for engine trail color */
    public @Nullable Color trailColor;

    /** Function used for calculating cost of moving with ControlPathfinder. Does not affect "normal" flow field pathfinding. */
    public @Nullable PathCost pathCost;
    /** A sample of the unit that this type creates. Do not modify! */
    public @Nullable Unit sample;

    /** Flags to target based on priority. Null indicates that the closest target should be found. The closest enemy core is used as a fallback. */
    public BlockFlag[] targetFlags = {null};

    /** color for outline generated around sprites */
    public Color outlineColor = Pal.darkerMetal;
    /** thickness for sprite outline  */
    public int outlineRadius = 3;
    /** if false, no sprite outlines are generated */
    public boolean outlines = true;

    /** amount of items this unit can carry; <0 to determine based on hitSize. */
    public int itemCapacity = -1;
    /** amount of ammo this unit can hold (if the rule is enabled); <0 to determine based on weapon fire rate. */
    public int ammoCapacity = -1;
    /** ammo this unit uses, if that system is enabled. */
    public AmmoType ammoType = new ItemAmmoType(Items.copper);

    /** max hardness of ore that this unit can mine (<0 to disable) */
    public int mineTier = -1;
    /** mining speed in weird arbitrary units */
    public float mineSpeed = 1f;
    /** whether this unit can mine ores from floors/walls, respectively */
    public boolean mineWalls = false, mineFloor = true;
    /** if true, harder materials will take longer to mine */
    public boolean mineHardnessScaling = true;
    /** continuous sound emitted when mining. */
    public Sound mineSound = Sounds.minebeam;
    /** volume of mining sound. */
    public float mineSoundVolume = 0.6f;
    /** Target items to mine. Used in MinerAI */
    public Seq<Item> mineItems = Seq.with(Items.copper, Items.lead, Items.titanium, Items.thorium);

    //LEG UNITS

    /** number of legs this unit has (must have the correct type to function!) */
    public int legCount = 4;
    /** size of groups in which legs move. for example, insects (6 legs) usually move legs in groups of 3. */
    public int legGroupSize = 2;

    /** total length of a leg (both segments) */
    public float legLength = 10f,
    /** how fast individual legs move towards their destination (non-linear) */
    legSpeed = 0.1f,
    /** scale for how far in front (relative to unit velocity) legs try to place themselves; if legs lag behind a unit, increase this number */
    legForwardScl = 1f,
    /** leg offset from the center of the unit */
    legBaseOffset = 0f,
    /** scaling for space between leg movements */
    legMoveSpace = 1f,
    /** for legs without "joints", this is how much the second leg sprite is moved "back" by, so it covers the joint region (it's hard to explain without an image) */
    legExtension = 0,
    /** Higher values of this field make groups of legs move less in-sync with each other. */
    legPairOffset = 0,
    /** scaling for how far away legs *try* to be from the body (not their actual length); e.g. if set to 0.5, legs will appear somewhat folded */
    legLengthScl = 1f,
    /** if legStraightness > 0, this is the scale for how far away legs are from the body horizontally */
    legStraightLength = 1f,
    /** maximum length of an individual leg as fraction of real length */
    legMaxLength = 1.75f,
    /** minimum length of an individual leg as fraction of real length */
    legMinLength = 0f,
    /** splash damage dealt when a leg touches the ground */
    legSplashDamage = 0f,
    /** splash damage radius of legs */
    legSplashRange = 5,
    /** how straight the leg base/origin is (0 = circular, 1 = line) */
    baseLegStraightness = 0f,
    /** how straight the leg outward angles are (0 = circular, 1 = horizontal line) */
    legStraightness = 0f;

    /** If true, legs are locked to the base of the unit instead of being on an implicit rotating "mount". */
    public boolean lockLegBase = false;
    /** If true, legs always try to move around even when the unit is not moving (leads to more natural behavior) */
    public boolean legContinuousMove;
    /** TODO neither of these appear to do much */
    public boolean flipBackLegs = true, flipLegSide = false;

    //MECH UNITS

    /** screen shake amount for when this mech lands after boosting */
    public float mechLandShake = 0f;
    /** parameters for mech swaying animation */
    public float mechSideSway = 0.54f, mechFrontSway = 0.1f, mechStride = -1f;
    /** whether particles are created when this mech takes a step */
    public boolean mechStepParticles = false;
    /** color that legs change to when moving, to simulate depth */
    public Color mechLegColor = Pal.darkMetal;

    //TANK UNITS

    /** list of treads as rectangles in IMAGE COORDINATES, relative to the center. these are mirrored. */
    public Rect[] treadRects = {};
    /** number of frames of movement in a tread */
    public int treadFrames = 18;
    /** how much of a top part of a tread sprite is "cut off" relative to the pattern; this is corrected for */
    public int treadPullOffset = 0;

    //SEGMENTED / CRAWL UNITS (this is WIP content!)

    /** number of independent segments */
    public int segments = 0;
    /** magnitude of sine offset between segments */
    public float segmentMag = 2f,
    /** scale of sine offset between segments */
    segmentScl = 4f,
    /** index multiplier of sine offset between segments */
    segmentPhase = 5f,
    /** how fast each segment moves towards the next one */
    segmentRotSpeed = 1f,
    /** maximum difference between segment angles */
    segmentMaxRot = 30f,
    /** speed multiplier this unit will have when crawlSlowdownFrac is met. */
    crawlSlowdown = 0.5f,
    /** damage dealt to blocks under this tank/crawler every frame. */
    crushDamage = 0f,
    /** the fraction of solids under this block necessary for it to reach crawlSlowdown. */
    crawlSlowdownFrac = 0.55f;

    //MISSILE UNITS

    /** lifetime of this missile. */
    public float lifetime = 60f * 5f;
    /** ticks that must pass before this missile starts homing. */
    public float homingDelay = 10f;

    //REGIONS

    //(undocumented, you shouldn't need to use these, and if you do just check how they're drawn and copy that)
    public TextureRegion baseRegion, legRegion, region, previewRegion, shadowRegion, cellRegion, itemCircleRegion,
        softShadowRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion, treadRegion;
    public TextureRegion[] wreckRegions, segmentRegions, segmentOutlineRegions;
    public TextureRegion[][] treadRegions;

    protected float buildTime = -1f;
    protected @Nullable ItemStack[] totalRequirements, cachedRequirements, firstRequirements;

    public UnitType(String name){
        super(name);

        constructor = EntityMapping.map(this.name);
    }

    public UnitController createController(Unit unit){
        return controller.get(unit);
    }

    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.setType(this);
        unit.ammo = ammoCapacity; //fill up on ammo upon creation
        unit.elevation = flying ? 1f : 0;
        unit.heal();
        if(unit instanceof TimedKillc u){
            u.lifetime(lifetime);
        }
        return unit;
    }

    public Unit spawn(Team team, float x, float y){
        Unit out = create(team);
        out.set(x, y);
        out.add();
        return out;
    }

    public Unit spawn(float x, float y){
        return spawn(state.rules.defaultTeam, x, y);
    }

    public Unit spawn(Team team, Position pos){
        return spawn(team, pos.getX(), pos.getY());
    }

    public Unit spawn(Position pos){
        return spawn(state.rules.defaultTeam, pos);
    }

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    public boolean targetable(Unit unit, Team targeter){
        return targetable || (vulnerableWithPayloads && unit instanceof Payloadc p && p.hasPayload());
    }

    public boolean hittable(Unit unit){
        return hittable || (vulnerableWithPayloads && unit instanceof Payloadc p && p.hasPayload());
    }

    public void update(Unit unit){

    }

    public void updatePayload(Unit unit, @Nullable Unit unitHolder, @Nullable Building buildingHolder){

    }

    public void killed(Unit unit){}

    public void landed(Unit unit){}

    private String getStatustext(Unit unit){
        String statustext = "";
        int count = 0;
        if(unit.damageMultiplier() != 1f){
            statustext += " [red]伤[white]：" + String.format("%.1f", unit.damageMultiplier());
            count += 1;
        }
        if(unit.reloadMultiplier() != 1f){
            statustext += " [violet]攻速[white]：" + String.format("%.1f", unit.reloadMultiplier());
            count += 1;
        }
        if(unit.speedMultiplier() != 1f){
            statustext += " [cyan]移[white]：" + String.format("%.1f", unit.speedMultiplier());
            count += 1;
        }
        if(unit.healthMultiplier() != 1f){
            statustext += " [acid]血[white]：" + ((unit.healthMultiplier() == Float.POSITIVE_INFINITY) ? "Inf" : String.format("%.1f", unit.healthMultiplier()));
        }
        return statustext;
    }

    private String getInfStatusEffect(Unit unit){
        StringBuilder builder = new StringBuilder();
        for(StatusEffect eff : Vars.content.statusEffects()){
            if(unit.hasEffect(eff)){
                if(unit.getEffectTime(eff) <= 0f || unit.getEffectTime(eff) >= 100000f){
                    builder.append(eff.emoji()).append(" ");
                }
            }
        }
        return builder.toString();
    }

    private void arcdisplayStatus(Unit unit, Table bars){
        if(!getStatustext(unit).equals("")){
            bars.add(new Bar(() -> getStatustext(unit), () -> Pal.accent, () -> 1f));
            bars.row();
        }
        if(!getInfStatusEffect(unit).equals("")){
            bars.add(new Bar(() -> "[orange]永久[white]：" + getInfStatusEffect(unit), () -> Pal.accent, () -> 1f));
            bars.row();
        }

        for(StatusEffect eff : Vars.content.statusEffects()){
            if(unit.hasEffect(eff)){
                if(unit.getEffectTime(eff) > 0f && unit.getEffectTime(eff) < 100000f){
                    bars.add(new Bar(() -> eff.emoji() + eff.localizedName + ": " + UI.formatTime(unit.getEffectTime(eff)), () -> Pal.accent, () -> 1f));
                    bars.row();
                }
            }
        }
    }

    public void display(Unit unit, Table table){
        table.table(t -> {
        t.left();
        t.add(new Image(uiIcon)).size(iconMed).scaling(Scaling.fit);
        if(unit.team.id < 6){
            t.labelWrap("[#" + unit.team.color + "]" + localizedName).left().width(190f).padLeft(5);
        }else{
            t.labelWrap("[#" + unit.team.color + "]" + localizedName + "[" + unit.team.id + "]").left().width(190f).padLeft(5);
        }
    }).growX().left();
        table.row();

        table.table(bars -> {
            bars.defaults().growX().height(20f).pad(4);

            bars.add(new Bar(() -> {
                //"[yellow]盾[white]：" + UI.formatAmount((long)unit.shield()) +
                if(unit.shield() > 0){
                    return UI.formatAmount((long)unit.health) + "[gray]+[white]" + UI.formatAmount((long)unit.shield);
                }else if(unit.maxHealth == unit.health){
                    return UI.formatAmount((long)unit.health);
                }else{
                    return UI.formatAmount((long)unit.health) + "/" + UI.formatAmount((long)unit.maxHealth) + " (" + (int)(100 * unit.health / unit.maxHealth) + "%)";
                }
            }, () -> Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar(() -> ammoType.icon() + " " + Core.bundle.format("stat.ammoDetail", unit.ammo, ammoCapacity), () -> ammoType.barColor(), () -> unit.ammo / ammoCapacity));
                bars.row();
            }
            if(unit instanceof Payloadc payload && payload.payloadUsed() > 0){
                bars.add(new Bar("装载：" + String.format("%.2f", payload.payloadUsed() / 9) + "/" + String.format("%.2f", unit.type().payloadCapacity / 9), Pal.items, () -> payload.payloadUsed() / unit.type().payloadCapacity));
                bars.row();

                var count = new float[]{-1};
                bars.table().update(t -> {
                    if(count[0] != payload.payloadUsed()){
                        payload.contentInfo(t, 8 * 2, 270);
                        count[0] = payload.payloadUsed();
                    }
                }).growX().left().height(0f).pad(0f);
                bars.row();
            }
            arcdisplayStatus(unit, bars);
        }).growX();

        if(unit.controller() instanceof LogicAI ai){
            table.row();
            table.table(tt->{
                tt.add(Blocks.microProcessor.emoji() + " " + Core.bundle.get("units.processorcontrol")).growX().left();
                if(ai.controller != null && (Core.settings.getBool("mouseposition") || Core.settings.getBool("position"))){
                    //table.row();
                    tt.add("[lightgray](" + ai.controller.tileX() + ", " + ai.controller.tileY() + ")").growX().right();
                }
            }).growX().wrap().left();
            table.row();
            //table.label(() -> Iconc.settings + " " + (long)unit.flag + "").color(Color.lightGray).growX().wrap().left();
            if(net.active() && ai.controller != null && ai.controller.lastAccessed != null){
                table.row();
                table.add(Core.bundle.format("lastaccessed", ai.controller.lastAccessed)).growX().wrap().left();
            }
        }else if(net.active() && unit.lastCommanded != null){
            table.row();
            table.add(Core.bundle.format("lastcommanded", unit.lastCommanded)).growX().wrap().left();
        }

        table.row();
        table.table(t -> {
            t.add(Iconc.settings + " " + (long)unit.flag + "").color(Color.lightGray).growX();
            t.add(Fonts.getUnicodeStr(unit.type().name) + unit.team.data().countType(unit.type()) + "/" + Units.getStringCap(unit.team)).color(Color.lightGray).growX();
        }).growX();
        table.row();
    }

    /** @return whether this block supports a specific environment. */
    public boolean supportsEnv(int env){
        return (envEnabled & env) != 0 && (envDisabled & env) == 0 && (envRequired == 0 || (envRequired & env) == envRequired);
    }

    public boolean isBanned(){
        return state.rules.bannedUnits.contains(this);
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        //units require reconstructors being researched
        for(Block block : content.blocks()){
            if(block instanceof Reconstructor r){
                for(UnitType[] recipe : r.upgrades){
                    //result of reconstruction is this, so it must be a dependency
                    if(recipe[1] == this){
                        cons.get(block);
                    }
                }
            }
        }

        for(ItemStack stack : researchRequirements()){
            cons.get(stack.item);
        }
    }

    @Override
    public boolean isHidden(){
        return hidden;
    }

    @Override
    public void setStats(){

        stats.add(Stat.health, health);
        stats.add(Stat.armor, armor);
        stats.add(Stat.speed, speed * 60f / tilesize, StatUnit.tilesSecond);
        stats.add(Stat.rotateSpeed,rotateSpeed);
        stats.add(Stat.size, hitSize / tilesize, StatUnit.blocksSquared);
        stats.add(Stat.unitItemCapacity, itemCapacity);
        stats.add(Stat.unitrange, (int)(maxRange / tilesize), StatUnit.blocks);
        stats.add(Stat.ammoType, ammoType.icon());
        stats.add(Stat.ammoCapacity, ammoCapacity);

        if(abilities.any()){
            var unique = new ObjectSet<String>();

            for(Ability a : abilities){
                if(a.display && unique.add(a.localized())){
                    stats.add(Stat.abilities, a.localized());
                }
            }
        }

        stats.add(Stat.flying, flying);

        if(!flying){
            stats.add(Stat.canBoost, canBoost);
            if(canBoost){
                stats.add(Stat.boostMultiplier, boostMultiplier);
            }
        }
        if(drownTimeMultiplier != 1){
            stats.add(Stat.drownTimeMultiplier, drownTimeMultiplier);
        }

        if(mineTier >= 1){
            stats.add(Stat.uniMineTier,mineTier);
            stats.addPercent(Stat.mineSpeed, mineSpeed);
            stats.add(Stat.mineTier, StatValues.drillUnit(this));
            /*
            stats.add(Stat.mineTier, StatValues.blocks(b ->
                b.itemDrop != null &&
                (b instanceof Floor f && (((f.wallOre && mineWalls) || (!f.wallOre && mineFloor))) ||
                (!(b instanceof Floor) && mineWalls)) &&
                b.itemDrop.hardness <= mineTier && (!b.playerUnmineable || Core.settings.getBool("doubletapmine"))));*/
        }
        if(buildSpeed > 0){
            stats.addPercent(Stat.buildSpeed, buildSpeed);
        }
        if(sample instanceof Payloadc){
            stats.add(Stat.payloadCapacity, StatValues.squared(Mathf.sqrt(payloadCapacity / (tilesize * tilesize)), StatUnit.blocksSquared));
        }

        var reqs = getFirstRequirements();

        if(reqs != null){
            stats.add(Stat.buildCost, StatValues.items(reqs));
        }

        if(weapons.any()){
            stats.add(Stat.weapons, StatValues.weapons(this, weapons));
        }

        if(immunities.size > 0){
            var imm = immunities.toSeq().sort();
            //it's redundant to list wet for naval units
            if(naval){
                imm.remove(StatusEffects.wet);
            }
            stats.add(Stat.immunities, StatValues.statusEffects(imm));
        }
    }

    //never actually called; it turns out certain mods have custom weapons that do not need bullets.
    protected void validateWeapons(){
        for(int i = 0; i < weapons.size; i++){
            var wep = weapons.get(i);
            if(wep.bullet == Bullets.placeholder || wep.bullet == null){
                throw new RuntimeException("Unit: " + name + ": weapon #" + i + " ('" + wep.name + "') does not have a bullet defined. Make sure you have a bullet: (JSON) or `bullet = ` field in your unit definition.");
            }
        }
    }

    @CallSuper
    @Override
    public void init(){
        if(constructor == null) throw new IllegalArgumentException("no constructor set up for unit '" + name + "'");

        Unit example = constructor.get();

        allowLegStep = example instanceof Legsc;

        stats.useCategories = true;

        //water preset
        if(example instanceof WaterMovec){
            naval = true;
            canDrown = false;
            omniMovement = false;
            immunities.add(StatusEffects.wet);
            if(shadowElevation < 0f){
                shadowElevation = 0.11f;
            }
        }

        if(pathCost == null){
            pathCost =
                example instanceof WaterMovec ? ControlPathfinder.costNaval :
                allowLegStep ? ControlPathfinder.costLegs :
                hovering ? ControlPathfinder.costHover :
                ControlPathfinder.costGround;
        }

        if(flying){
            envEnabled |= Env.space;
        }

        if(lightRadius == -1){
            lightRadius = Math.max(60f, hitSize * 2.3f);
        }

        clipSize = Math.max(clipSize, lightRadius * 1.1f);
        singleTarget = weapons.size <= 1 && !forceMultiTarget;

        if(itemCapacity < 0){
            itemCapacity = Math.max(Mathf.round((int)(hitSize * 4f), 10), 10);
        }

        //assume slight range margin
        float margin = 4f;

        //set up default range
        if(range < 0){
            range = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                range = Math.min(range, weapon.range() - margin);
                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(maxRange < 0){
            maxRange = Math.max(0f, range);

            for(Weapon weapon : weapons){
                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(fogRadius < 0){
            //TODO depend on range?
            fogRadius = Math.max(58f * 3f, hitSize * 2f) / 8f;
        }

        if(weapons.isEmpty()){
            range = maxRange = mineRange;
        }

        if(mechStride < 0){
            mechStride = 4f + (hitSize -8f)/2.1f;
        }

        if(aimDst < 0){
            aimDst = weapons.contains(w -> !w.rotate) ? hitSize * 2f : hitSize / 2f;
        }

        if(stepShake < 0){
            stepShake = Mathf.round((hitSize - 11f) / 9f);
            mechStepParticles = hitSize > 15f;
        }

        if(engineSize > 0){
            engines.add(new UnitEngine(0f, -engineOffset, engineSize, -90f));
        }

        if(treadEffect == null){
            treadEffect = new Effect(50, e -> {
                color(Tmp.c1.set(e.color).mul(1.5f));
                Fx.rand.setSeed(e.id);
                for(int i = 0; i < 3; i++){
                    Fx.v.trns(e.rotation + Fx.rand.range(40f), Fx.rand.random(6f * e.finpow()));
                    Fill.circle(e.x + Fx.v.x + Fx.rand.range(4f), e.y + Fx.v.y + Fx.rand.range(4f), Math.min(e.fout(), e.fin() * e.lifetime / 8f) * hitSize / 28f * 3f * Fx.rand.random(0.8f, 1.1f) + 0.3f);
                }
            }).layer(Layer.debris);
        }

        for(Ability ab : abilities){
            ab.init(this);
        }

        //add mirrored weapon variants
        Seq<Weapon> mapped = new Seq<>();
        for(Weapon w : weapons){
            if(w.recoilTime < 0) w.recoilTime = w.reload;
            mapped.add(w);

            //mirrors are copies with X values negated
            if(w.mirror){
                Weapon copy = w.copy();
                copy.flip();
                mapped.add(copy);

                //since there are now two weapons, the reload and recoil time must be doubled
                w.recoilTime *= 2f;
                copy.recoilTime *= 2f;
                w.reload *= 2f;
                copy.reload *= 2f;

                w.otherSide = mapped.size - 1;
                copy.otherSide = mapped.size - 2;
            }
        }
        this.weapons = mapped;

        weapons.each(Weapon::init);

        canHeal = weapons.contains(w -> w.bullet.heals());

        canAttack = weapons.contains(w -> !w.noAttack);

        //dynamically create ammo capacity based on firing rate
        if(ammoCapacity < 0){
            float shotsPerSecond = weapons.sumf(w -> w.useAmmo ? 60f / w.reload : 0f);
            //duration of continuous fire without reload
            float targetSeconds = 35;

            ammoCapacity = Math.max(1, (int)(shotsPerSecond * targetSeconds));
        }

        //calculate estimated DPS for one target based on weapons
        if(dpsEstimate < 0){
            dpsEstimate = weapons.sumf(Weapon::dps);

            //suicide enemy
            if(weapons.contains(w -> w.bullet.killShooter)){
                //scale down DPS to be insignificant
                dpsEstimate /= 25f;
            }
        }

        //only do this after everything else was initialized
        sample = constructor.get();
    }

    @CallSuper
    @Override
    public void load(){
        super.load();

        for(var part : parts){
            part.load(name);
        }
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        previewRegion = Core.atlas.find(name + "-preview", name);
        legRegion = Core.atlas.find(name + "-leg");
        jointRegion = Core.atlas.find(name + "-joint");
        baseJointRegion = Core.atlas.find(name + "-joint-base");
        footRegion = Core.atlas.find(name + "-foot");
        treadRegion = Core.atlas.find(name + "-treads");
        itemCircleRegion = Core.atlas.find("ring-item");

        if(treadRegion.found()){
            treadRegions = new TextureRegion[treadRects.length][treadFrames];
            for(int r = 0; r < treadRects.length; r++){
                for(int i = 0; i < treadFrames; i++){
                    treadRegions[r][i] = Core.atlas.find(name + "-treads" + r + "-" + i);
                }
            }
        }
        legBaseRegion = Core.atlas.find(name + "-leg-base", name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        //when linear filtering is on, it's acceptable to use the relatively low-res 'particle' region
        softShadowRegion =
            squareShape ? Core.atlas.find("square-shadow") :
            hitSize <= 10f || (Core.settings != null && Core.settings.getBool("linear", true)) ?
                Core.atlas.find("particle") :
                Core.atlas.find("circle-shadow");

        outlineRegion = Core.atlas.find(name + "-outline");
        shadowRegion = fullIcon;

        wreckRegions = new TextureRegion[3];
        for(int i = 0; i < wreckRegions.length; i++){
            wreckRegions[i] = Core.atlas.find(name + "-wreck" + i);
        }

        segmentRegions = new TextureRegion[segments];
        segmentOutlineRegions = new TextureRegion[segments];
        for(int i = 0; i < segments; i++){
            segmentRegions[i] = Core.atlas.find(name + "-segment" + i);
            segmentOutlineRegions[i] = Core.atlas.find(name + "-segment-outline" + i);
        }

        clipSize = Math.max(region.width * 2f, clipSize);
    }

    public void getRegionsToOutline(Seq<TextureRegion> out){
        for(Weapon weapon : weapons){
            for(var part : weapon.parts){
                part.getOutlines(out);
            }
        }
        for(var part : parts){
            part.getOutlines(out);
        }
    }

    public boolean needsBodyOutline(){
        return alwaysCreateOutline;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        var toOutline = new Seq<TextureRegion>();
        getRegionsToOutline(toOutline);

        for(var region : toOutline){
            if(region instanceof AtlasRegion atlas){
                String regionName = atlas.name;
                Pixmap outlined = Pixmaps.outline(Core.atlas.getPixmap(region), outlineColor, outlineRadius);

                Drawf.checkBleed(outlined);

                packer.add(PageType.main, regionName + "-outline", outlined);
            }
        }

        if(outlines){
            Seq<TextureRegion> outlineSeq = Seq.with(region, jointRegion, footRegion, baseJointRegion, legRegion, treadRegion);
            if(Core.atlas.has(name + "-leg-base")){
                outlineSeq.add(legBaseRegion);
            }

            //note that mods with these regions already outlined will have *two* outlines made, which is... undesirable
            for(var outlineTarget : outlineSeq){
                if(!outlineTarget.found()) continue;

                makeOutline(PageType.main, packer, outlineTarget, alwaysCreateOutline && region == outlineTarget, outlineColor, outlineRadius);
            }

            if(sample instanceof Crawlc){
                for(int i = 0; i < segments; i++){
                    makeOutline(packer, segmentRegions[i], name + "-segment-outline" + i, outlineColor, outlineRadius);
                }
            }

            for(Weapon weapon : weapons){
                if(!weapon.name.isEmpty() && (minfo.mod == null || weapon.name.startsWith(minfo.mod.name)) && !(!weapon.top && packer.isOutlined(weapon.name))){
                    makeOutline(PageType.main, packer, weapon.region, !weapon.top, outlineColor, outlineRadius);
                }
            }
        }

        //TODO test
        if(sample instanceof Tankc){
            PixmapRegion pix = Core.atlas.getPixmap(treadRegion);

            for(int r = 0; r < treadRects.length; r++){
                Rect treadRect = treadRects[r];
                //slice is always 1 pixel wide
                Pixmap slice = pix.crop((int)(treadRect.x + pix.width/2f), (int)(treadRect.y + pix.height/2f), 1, (int)treadRect.height);
                int frames = treadFrames;
                for(int i = 0; i < frames; i++){
                    int pullOffset = treadPullOffset;
                    Pixmap frame = new Pixmap(slice.width, slice.height);
                    for(int y = 0; y < slice.height; y++){
                        int idx = y + i;
                        if(idx >= slice.height){
                            idx -= slice.height;
                            idx += pullOffset;
                            idx = Mathf.mod(idx, slice.height);
                        }

                        frame.setRaw(0, y, slice.getRaw(0, idx));
                    }

                    packer.add(PageType.main, name + "-treads" + r + "-" + i, frame);
                }
            }
        }
    }

    /** @return the time required to build this unit, as a value that takes into account reconstructors */
    public float getBuildTime(){
        getTotalRequirements();
        return buildTime;
    }

    /** @return all items needed to build this unit, including reconstructor steps. */
    public ItemStack[] getTotalRequirements(){
        if(totalRequirements == null){
            UnitType[] ret = {null};
            float[] timeret = {0f};
            ItemStack[] result = getRequirements(ret, timeret);

            //prevents stack overflow if requirements are circular and result != null
            totalRequirements = ItemStack.empty;

            if(result != null){
                ItemSeq total = new ItemSeq();

                total.add(result);
                if(ret[0] != null){
                    total.add(ret[0].getTotalRequirements());
                }
                totalRequirements = total.toArray();
            }

            for(var stack : totalRequirements){
                buildTime += stack.item.cost * stack.amount;
            }
        }
        return totalRequirements;
    }

    /** @return item requirements based on reconstructors or factories found; returns previous unit in array if provided */
    public @Nullable ItemStack[] getRequirements(@Nullable UnitType[] prevReturn, @Nullable float[] timeReturn){
        //find reconstructor
        var rec = (Reconstructor)content.blocks().find(b -> b instanceof Reconstructor re && re.upgrades.contains(u -> u[1] == this));

        if(rec != null && Structs.find(rec.consumers, i -> i instanceof ConsumeItems) instanceof ConsumeItems ci){
            if(prevReturn != null){
                prevReturn[0] = rec.upgrades.find(u -> u[1] == this)[0];
            }
            if(timeReturn != null){
                timeReturn[0] = rec.constructTime;
            }
            return ci.items;
        }else{
            //find a factory
            var factory = (UnitFactory)content.blocks().find(u -> u instanceof UnitFactory uf && uf.plans.contains(p -> p.unit == this));
            if(factory != null){

                var plan = factory.plans.find(p -> p.unit == this);
                if(timeReturn != null){
                    timeReturn[0] = plan.time;
                }
                return plan.requirements;
            }else{
                //find an assembler
                var assembler = (UnitAssembler)content.blocks().find(u -> u instanceof UnitAssembler a && a.plans.contains(p -> p.unit == this));
                if(assembler != null){
                    var plan = assembler.plans.find(p -> p.unit == this);

                    if(timeReturn != null){
                        timeReturn[0] = plan.time;
                    }
                    ItemSeq reqs = new ItemSeq();
                    for(var bstack : plan.requirements){
                        if(bstack.item instanceof Block block){
                            for(var stack : block.requirements){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }else if(bstack.item instanceof UnitType unit){
                            for(var stack : unit.getTotalRequirements()){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }
                    }
                    return reqs.toArray();
                }
            }
        }
        return null;
    }

    public @Nullable ItemStack[] getFirstRequirements(){
        if(firstRequirements == null){
            firstRequirements = getRequirements(null, null);
        }
        return firstRequirements;
    }

    @Override
    public ItemStack[] researchRequirements(){
        if(cachedRequirements != null){
            return cachedRequirements;
        }

        ItemStack[] stacks = getRequirements(null, null);

        if(stacks != null){
            ItemStack[] out = new ItemStack[stacks.length];
            for(int i = 0; i < out.length; i++){
                out[i] = new ItemStack(stacks[i].item, UI.roundAmount((int)(Math.pow(stacks[i].amount, 1.1) * researchCostMultiplier)));
            }

            //remove zero-requirements for automatic unlocks
            out = Structs.filter(ItemStack.class, out, stack -> stack.amount > 0);

            cachedRequirements = out;

            return out;
        }

        return super.researchRequirements();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    /** Sets up engines, mirroring the contents of the specified array. */
    public void setEnginesMirror(UnitEngine... array){
        for(var base : array){
            engines.add(base);

            var engine = base.copy();
            engine.x *= -1;
            engine.rotation = 180f - engine.rotation;
            if(engine.rotation < 0) engine.rotation += 360f;
            engines.add(engine);
        }
    }

    //region drawing

    public void draw(Unit unit){
        if(unit.inFogTo(Vars.player.team())) return;

        boolean isPayload = !unit.isAdded();

        //透明度
        initUnitTransp();
        initLegTransp();

        unitTrans = (float)Core.settings.getInt("unitTransparency") / 100f;
        legTrans = unitTrans;

        Mechc mech = unit instanceof Mechc ? (Mechc)unit : null;
        float z = unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);

        boolean draw_unit = (unit.maxHealth + unit.shield) > (float)Core.settings.getInt("minhealth_unitshown");
        boolean draw_minunithealthbar = (unit.maxHealth + unit.shield) > (float)Core.settings.getInt("minhealth_unithealthbarshown");

        if(!draw_unit){
            unitTrans = 0f;
            draw_minunithealthbar = false;
        }
        if(arcInfoControl(unit.team())) {
            //玩家操控的单位具有炫酷特效
            if (Core.settings.getInt("superUnitEffect") != 0 && unit.controller() == Vars.player) {
                unitTrans = 100f;
                draw_minunithealthbar = true;

                float curStroke = (float) Core.settings.getInt("playerEffectCurStroke") / 10f;
                Color effectcolor = getPlayerEffectColor();

                float sectorRad = 0.14f, rotateSpeed = 0.5f;
                int sectors = 5;

                Lines.stroke(Lines.getStroke() * curStroke);

                Draw.z(Layer.effect - 2f);
                Draw.color(effectcolor);

                Tmp.v1.trns(unit.rotation - 90, unit.x, unit.y).add(unit.x, unit.y);
                float rx = Tmp.v1.x, ry = Tmp.v1.y;

                if (curStroke > 0) {
                    for (int i = 0; i < sectors; i++) {
                        float rot = unit.rotation + i * 360f / sectors + Time.time * rotateSpeed;
                        Lines.arc(unit.x, unit.y, maxRange, sectorRad, rot);
                    }
                }
                Draw.reset();
            } else if (Core.settings.getInt("superUnitEffect") == 2 && unit.controller() instanceof Player) {
                unitTrans = 100f;
                draw_minunithealthbar = true;

                float curStroke = (float) Core.settings.getInt("playerEffectCurStroke") / 10f;
                Color effectcolor = unit.team.color;

                float sectorRad = 0.14f, rotateSpeed = 0.5f;
                int sectors = 5;

                Lines.stroke(Lines.getStroke() * curStroke);

                Draw.z(Layer.effect - 2f);
                Draw.color(effectcolor);

                Tmp.v1.trns(unit.rotation - 90, unit.x, unit.y).add(unit.x, unit.y);
                float rx = Tmp.v1.x, ry = Tmp.v1.y;

                if (curStroke > 0) {
                    for (int i = 0; i < sectors; i++) {
                        float rot = unit.rotation + i * 360f / sectors + Time.time * rotateSpeed;
                        Lines.arc(unit.x, unit.y, maxRange, sectorRad, rot, (int) (50 + maxRange / 10));
                    }
                }


                Draw.reset();

            } else if (Core.settings.getBool("alwaysShowPlayerUnit") && (unit.controller() instanceof Player || unit.controller().isBeingControlled(player.unit()))) {
                unitTrans = 100f;
                draw_minunithealthbar = true;
                Draw.color(unit.team.color);
                Draw.alpha((float) Core.settings.getInt("unitweapon_range") / 100f);
                Lines.dashCircle(unit.x, unit.y, maxRange);
            } else if (draw_minunithealthbar) {
                float alertRange = (float) Core.settings.getInt("unitAlertRange");
                boolean turretAlert = alertRange > 0 && (alertRange >= 30f ||
                        ((!player.unit().isNull() && player.unit().targetable(unit.team)) || (control.input.commandMode && control.input.selectedUnits.size > 0)));
                boolean canHitPlayer = player.unit().isFlying() ? targetAir : targetGround;
                boolean canHitCommand = control.input.commandMode && (control.input.selectedUnits.size > 0);
                boolean showHitPlayer = unit.team != player.team() && canHitPlayer && (player.unit().dst(unit.x, unit.y) <= (maxRange + (float) Core.settings.getInt("unitAlertRange") * tilesize));
                boolean showHitCommand = unit.team != player.team() && canHitCommand &&
                        Core.input.mouseWorld().dst(unit.x, unit.y) <= (maxRange + (float) Core.settings.getInt("unitAlertRange") * tilesize);

                Draw.color(unit.team.color);
                float unitRangeAlpha = turretAlert && (showHitPlayer || showHitCommand) ? 1f : Core.settings.getInt("unitweapon_range") / 100f;
                Draw.alpha(unitRangeAlpha);
                Lines.dashCircle(unit.x, unit.y, maxRange);
            }


            if (Core.settings.getInt("unitTargetType") > 0 && unit.controller() instanceof Player) {
                Draw.reset();
                Draw.z(Layer.effect);
                Color effectColor = unit.controller() == Vars.player ? getPlayerEffectColor() : unit.team.color;
                Draw.color(effectColor, 0.8f);
                Lines.stroke(1f);
                Lines.line(unit.x, unit.y, unit.aimX, unit.aimY);

                switch (Core.settings.getInt("unitTargetType")) {
                    case 1:
                        Lines.dashCircle(unit.aimX, unit.aimY, 8);
                        break;
                    case 2:
                        Drawf.target(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                        break;
                    case 3:
                        Drawf.target2(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                        break;
                    case 4:
                        Drawf.targetc(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                        break;
                    case 5:
                        Drawf.targetd(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                        break;
                }
            }

            if (!control.input.commandMode && Core.settings.getBool("alwaysShowUnitRTSAi") && unit.isCommandable() && arcInfoControl()) {
                Draw.z(Layer.effect);
                CommandAI ai = unit.command();
                //draw target line
                if (ai.targetPos != null) {
                    Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                    Draw.color(unit.team.color);
                    Drawf.limitLineColor(unit, lineDest, unit.hitSize / 2f, 3.5f, unit.team.color);

                    if (ai.attackTarget == null) {
                        Draw.color(unit.team.color);
                        Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f, unit.team.color);
                    }
                }

                //Drawf.square(unit.x, unit.y, unit.hitSize / 1.4f + 1f);

                if (ai.attackTarget != null) {
                    Draw.color(unit.team.color);
                    Drawf.target(ai.attackTarget.getX(), ai.attackTarget.getY(), 6f, unit.team.color);
                }
                Draw.color();
            }

        }
        if(unit.controller().isBeingControlled(player.unit())){
            drawControl(unit);
        }

        if(!isPayload && (unit.isFlying() || shadowElevation > 0)){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            drawShadow(unit);
        }

        Draw.z(z - 0.02f);

        if(mech != null){
            if(unitTrans > 0f) drawMech(mech);

            //side
            legOffset.trns(mech.baseRotation(), 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f/Mathf.PI, 1) * mechSideSway, 0f, unit.elevation));

            //front
            legOffset.add(Tmp.v1.trns(mech.baseRotation() + 90, 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 1f/Mathf.PI, 1) * mechFrontSway, 0f, unit.elevation)));

            unit.trns(legOffset.x, legOffset.y);
        }

        if(unit instanceof Tankc){
            drawTank((Unit & Tankc)unit);
        }

        if(unit instanceof Legsc && !isPayload){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc && unitTrans > 0f){
            drawPayload((Unit & Payloadc)unit);
        }
        if(unitTrans > 0f){
            drawSoftShadow(unit);

            Draw.z(z);

            if(unit instanceof Crawlc c){
                drawCrawl(c);
            }

            if(drawBody) drawOutline(unit);
            drawWeaponOutlines(unit);
            if(engineLayer > 0) Draw.z(engineLayer);
            if(trailLength > 0 && !naval && (unit.isFlying() || !useEngineElevation)){
                drawTrail(unit);
            }
            if(engines.size > 0) drawEngines(unit);
            Draw.z(z);
            if(drawBody) drawBody(unit);
            if(drawCell) drawCell(unit);
            drawWeapons(unit);
            if(drawItems) drawItems(unit);
            drawLight(unit);
        }

        if(unit.shieldAlpha > 0 && drawShields){
            drawShield(unit);
        }

        if(mech != null){
            unit.trns(-legOffset.x, -legOffset.y);
        }

        //TODO how/where do I draw under?
        if(parts.size > 0){
            for(int i = 0; i < parts.size; i++){
                var part = parts.get(i);

                WeaponMount first = unit.mounts.length > part.weaponIndex ? unit.mounts[part.weaponIndex] : null;
                if(first != null){
                    DrawPart.params.set(first.warmup, first.reload / weapons.first().reload, first.smoothReload, first.heat, first.recoil, first.charge, unit.x, unit.y, unit.rotation);
                }else{
                    DrawPart.params.set(0f, 0f, 0f, 0f, 0f, 0f, unit.x, unit.y, unit.rotation);
                }

                if(unit instanceof Scaled s){
                    DrawPart.params.life = s.fin();
                }

                part.draw(DrawPart.params);
            }
        }

        if(!isPayload){
            for(Ability a : unit.abilities){
                Draw.reset();
                a.draw(unit);
            }
        }

        Draw.reset();

        //display healthbar by MI2
        Draw.z(Layer.shields + 6f);
        float y_corr = 0f ;
        if (unit.hitSize<30f && unit.hitSize>20f && unit.controller().isBeingControlled(player.unit())) y_corr = 2f;
        if(Core.settings.getBool("unitHealthBar")){
            if(draw_minunithealthbar && (unit.health < unit.maxHealth || unit.shield > 0)){
                Draw.reset();
                Lines.stroke(4f);
                Draw.color(unit.team.color, 0.5f);
                Lines.line(unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr, unit.x + unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr);
                Lines.stroke(2f);
                Draw.color(Pal.health, 0.8f);
                Lines.line(
                        unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr,
                        unit.x + unit.hitSize() * (Math.min(Mathf.maxZero(unit.health), unit.maxHealth) * 1.2f / unit.maxHealth - 0.6f), unit.y + (unit.hitSize() / 2f) + y_corr);
                Lines.stroke(2f);
                if(unit.shield > 0){
                    for(int didgt = 1; didgt <= Mathf.digits((int)(unit.shield / unit.maxHealth)) + 1; didgt++){
                        Draw.color(Pal.shield, 0.8f);
                        float shieldAmountScale = unit.shield / (unit.maxHealth * Mathf.pow(10f, (float)didgt - 1f));
                        if(didgt > 1){
                            Lines.line(unit.x - unit.hitSize() * 0.6f,
                                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr,
                                    unit.x + unit.hitSize() * ((Mathf.ceil((shieldAmountScale - Mathf.floor(shieldAmountScale)) * 10f) - 1f + 0.0001f) * 1.2f * (1f / 9f) - 0.6f),
                                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr);
                            //(s-1)*(1/9)because line(0) will draw length of 1
                        }else{
                            Lines.line(unit.x - unit.hitSize() * 0.6f,
                                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr,
                                    unit.x + unit.hitSize() * ((shieldAmountScale - Mathf.floor(shieldAmountScale) - 0.001f) * 1.2f - 0.6f),
                                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr);
                        }
                    }
                }
                Draw.reset();
            }

            float index = 0f;
            float iconSize = Mathf.ceil(unit.hitSize() / 4f);
            for(StatusEffect eff : Vars.content.statusEffects()){
                if(unit.hasEffect(eff)){
                    Draw.rect(eff.uiIcon,
                            unit.x - unit.hitSize() * 0.6f + 0.5f * iconSize * Mathf.mod(index, 4f),
                            unit.y + (unit.hitSize() / 2f) + 3f + 4f * Mathf.floor(index / 4f),
                            4f, 4f);
                    index++;
                }
            }

            index = 0f;
            if(unit instanceof Payloadc payload && payload.payloads().any()){
                for(Payload p : payload.payloads()){
                    Draw.rect(p.icon(),
                            unit.x - unit.hitSize() * 0.6f + 0.5f * iconSize * index,
                            unit.y + (unit.hitSize() / 2f) - 4f,
                            4f, 4f);
                    index++;
                }
                Draw.reset();
            }

        }

        //display logicAI info by MI2
        if(unit.controller() instanceof LogicAI logicai){
            Draw.reset();
            if(Core.settings.getBool("unitLogicMoveLine") && Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) <= 1200f){
                Lines.stroke(1f);
                Draw.color(0.2f, 0.2f, 1f, 0.9f);
                Lines.dashLine(unit.x, unit.y, logicai.moveX, logicai.moveY, (int)(Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) / 8));
                Lines.dashCircle(logicai.moveX, logicai.moveY, logicai.moveRad);
                Draw.reset();
            }

            //logicai timers
            if(Core.settings.getBool("unitLogicTimerBars")){

                Lines.stroke(2f);
                Draw.color(Pal.heal);
                Lines.line(unit.x - (unit.hitSize() / 2f), unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f), unit.y + unit.hitSize() * (logicai.controlTimer / logicai.logicControlTimeout - 0.5f));

                Lines.stroke(2f);
                Draw.color(Pal.items);
                Lines.line(unit.x - (unit.hitSize() / 2f) - 1f, unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f) - 1f, unit.y + unit.hitSize() * (logicai.itemTimer / logicai.transferDelay - 0.5f));

                Lines.stroke(2f);
                Draw.color(Pal.items);
                Lines.line(unit.x - (unit.hitSize() / 2f) - 1.5f, unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f) - 1.5f, unit.y + unit.hitSize() * (logicai.payTimer / logicai.transferDelay - 0.5f));

                Draw.reset();
            }
        }

        if(Core.settings.getBool("unithitbox")){
            Draw.color(unit.team.color, Color.black, Mathf.absin(Time.time, 4f, 1f));
            Lines.poly(unit.x, unit.y, 6, unit.hitSize());
        }

    }



    public <T extends Unit & Payloadc> void drawPayload(T unit){
        if(unit.hasPayload()){
            Payload pay = unit.payloads().first();
            pay.set(unit.x, unit.y, unit.rotation);
            pay.draw();
        }
    }

    public void drawShield(Unit unit){
        float alpha = unit.shieldAlpha();
        float radius = unit.hitSize() * 1.3f;
        Fill.light(unit.x, unit.y, Lines.circleVertices(radius), radius,
            Color.clear,
            Tmp.c2.set(unit.team.color).lerp(Color.white, Mathf.clamp(unit.hitTime() / 2f)).a(0.7f * alpha)
        );
    }

    public void drawControl(Unit unit){
        Draw.z(unit.isFlying() ? Layer.flyingUnitLow : Layer.groundUnit - 2);

        Draw.color(Pal.accent, Color.white, Mathf.absin(4f, 0.3f));
        Lines.poly(unit.x, unit.y, 4, unit.hitSize + 1.5f);

        Draw.reset();
    }

    public void drawShadow(Unit unit){
        float e = Mathf.clamp(unit.elevation, shadowElevation, 1f) * shadowElevationScl * (1f - unit.drownTime);
        float x = unit.x + shadowTX * e, y = unit.y + shadowTY * e;
        Floor floor = world.floorWorld(x, y);

        float dest = floor.canShadow ? 1f : 0f;
        //yes, this updates state in draw()... which isn't a problem, because I don't want it to be obvious anyway
        unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }

    public void drawSoftShadow(Unit unit){
        drawSoftShadow(unit, 1f);
    }

    public void drawSoftShadow(Unit unit, float alpha){
        drawSoftShadow(unit.x, unit.y, unit.rotation, alpha);
    }

    public void drawSoftShadow(float x, float y, float rotation, float alpha){
        Draw.color(0, 0, 0, 0.4f * alpha);
        float rad = 1.6f;
        float size = Math.max(region.width, region.height) * Draw.scl;
        Draw.rect(softShadowRegion, x, y, size * rad * Draw.xscl, size * rad * Draw.yscl, rotation - 90);
        Draw.color();
    }

    public void drawItems(Unit unit){
        applyColor(unit);

        //draw back items
        if(unit.item() != null && unit.itemTime > 0.01f){
            float size = (itemSize + Mathf.absin(Time.time, 5f, 1f)) * unit.itemTime;

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time, 5f, 0.1f));
            Draw.alpha(unitTrans);
            Draw.rect(unit.item().fullIcon,
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            size, size, unit.rotation);
            Draw.mixcol();
            Draw.alpha(unitTrans);

            size = (3f + Mathf.absin(Time.time, 5f, 1f)) * unit.itemTime + 0.5f;
            Draw.color(Pal.accent);
            Draw.rect(itemCircleRegion,
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY), size * 2, size * 2);

            if(Core.settings.getBool("unitItemCarried") || (unit.isLocal() && !renderer.pixelator.enabled())){
                Fonts.outline.draw(unit.stack.amount + "",
                unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
                unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY) - 3,
                Pal.accent, 0.25f * unit.itemTime / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }

    public void drawTrail(Unit unit){
        if(unit.trail == null){
            unit.trail = new Trail(trailLength);
        }
        Trail trail = unit.trail;
        trail.draw(trailColor == null ? unit.team.color : trailColor, (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * (useEngineElevation ? unit.elevation : 1f)) * trailScl);
    }

    public void drawEngines(Unit unit){
        if((useEngineElevation ? unit.elevation : 1f) <= 0.0001f) return;

        for(var engine : engines){
            engine.draw(unit);
        }

        Draw.color();
    }

    public void drawWeapons(Unit unit){
        applyColor(unit);

        for(WeaponMount mount : unit.mounts){
            mount.weapon.draw(unit, mount);
        }

        Draw.reset();
    }

    public void drawWeaponOutlines(Unit unit){
        applyColor(unit);
        applyOutlineColor(unit);

        for(WeaponMount mount : unit.mounts){
            if(!mount.weapon.top){
                //apply layer offset, roll it back at the end
                float z = Draw.z();
                Draw.z(z + mount.weapon.layerOffset);

                mount.weapon.drawOutline(unit, mount);

                Draw.z(z);
            }
        }

        Draw.reset();
    }

    public void drawOutline(Unit unit){
        Draw.reset();
        Draw.alpha(unitTrans); //

        if(Core.atlas.isFound(outlineRegion)){
            applyColor(unit);
            applyOutlineColor(unit);
            Draw.alpha(unitTrans);
            Draw.rect(outlineRegion, unit.x, unit.y, unit.rotation - 90);
            Draw.reset();
        }
    }

    public void drawBody(Unit unit){
        applyColor(unit);

        Draw.alpha(unitTrans); //

        Draw.rect(region, unit.x, unit.y, unit.rotation - 90);

        Draw.reset();
    }

    public void drawCell(Unit unit){
        applyColor(unit);

        Draw.color(cellColor(unit));

        Draw.alpha(unitTrans); //

        Draw.rect(cellRegion, unit.x, unit.y, unit.rotation - 90);
        Draw.reset();
    }

    public Color cellColor(Unit unit){
        float f = Mathf.clamp(unit.healthf());
        return Tmp.c1.set(Color.black).lerp(unit.team.color, f + Mathf.absin(Time.time, Math.max(f * 5f, 1f), 1f - f));
    }

    public void drawLight(Unit unit){
        if(lightRadius > 0){
            Drawf.light(unit.x, unit.y, lightRadius, lightColor, lightOpacity);
        }
    }

    public <T extends Unit & Tankc> void drawTank(T unit){
        Draw.rect(treadRegion, unit.x, unit.y, unit.rotation - 90);

        if(treadRegion.found()){
            int frame = (int)(unit.treadTime()) % treadFrames;
            for(int i = 0; i < treadRects.length; i ++){
                var region = treadRegions[i][frame];
                var treadRect = treadRects[i];
                float xOffset = -(treadRect.x + treadRect.width/2f);
                float yOffset = -(treadRect.y + treadRect.height/2f);

                for(int side : Mathf.signs){
                    Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90);
                    Draw.rect(region, unit.x + Tmp.v1.x / 4f, unit.y + Tmp.v1.y / 4f, treadRect.width / 4f, region.height / 4f, unit.rotation - 90);
                }
            }
        }
    }

    public <T extends Unit & Legsc> void drawLegs(T unit){
        applyColor(unit);
        Tmp.c3.set(Draw.getMixColor());

        Leg[] legs = unit.legs();

        float ssize = footRegion.width * Draw.scl * 1.5f;
        float rotation = unit.baseRotation();
        float invDrown = 1f - unit.drownTime;

        if(footRegion.found()){
            for(Leg leg : legs){
                Drawf.shadow(leg.base.x, leg.base.y, ssize, invDrown);
            }
        }

        //legs are drawn front first
        for(int j = legs.length - 1; j >= 0; j--){
            int i = (j % 2 == 0 ? j/2 : legs.length - 1 - j/2);
            Leg leg = legs[i];
            boolean flip = i >= legs.length/2f;
            int flips = Mathf.sign(flip);

            Vec2 position = unit.legOffset(legOffset, i).add(unit);

            Tmp.v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension);

            if(footRegion.found() && leg.moving && shadowElevation > 0){
                float scl = shadowElevation * invDrown;
                float elev = Mathf.slope(1f - leg.stage) * scl;
                Draw.color(Pal.shadow);
                Draw.alpha(legTrans); //
                Draw.rect(footRegion, leg.base.x + shadowTX * elev, leg.base.y + shadowTY * elev, position.angleTo(leg.base));
                Draw.color();
            }
            Draw.alpha(legTrans); //

            Draw.mixcol(Tmp.c3, Tmp.c3.a);

            if(footRegion.found()){
                Draw.rect(footRegion, leg.base.x, leg.base.y, position.angleTo(leg.base));
            }

            Lines.stroke(legRegion.height * Draw.scl * flips);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);

            Lines.stroke(legBaseRegion.height * Draw.scl * flips);
            Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, false);

            if(jointRegion.found()){
                Draw.alpha(unitTrans);
                Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
            }
        }

        //base joints are drawn after everything else
        if(baseJointRegion.found()){
            Draw.alpha(unitTrans);
            for(int j = legs.length - 1; j >= 0; j--){
                //TODO does the index / draw order really matter?
                Vec2 position = unit.legOffset(legOffset, (j % 2 == 0 ? j/2 : legs.length - 1 - j/2)).add(unit);
                Draw.rect(baseJointRegion, position.x, position.y, rotation);
            }
        }

        if(baseRegion.found()){
            Draw.alpha(unitTrans);
            Draw.rect(baseRegion, unit.x, unit.y, rotation - 90);
        }

        Draw.reset();
    }

    //TODO
    public void drawCrawl(Crawlc crawl){
        Unit unit = (Unit)crawl;
        applyColor(unit);

        //change to 2 TODO
        for(int p = 0; p < 2; p++){
            TextureRegion[] regions = p == 0 ? segmentOutlineRegions : segmentRegions;

            for(int i = 0; i < segments; i++){
                float trns = Mathf.sin(crawl.crawlTime() + i * segmentPhase, segmentScl, segmentMag);

                //at segment 0, rotation = segmentRot, but at the last segment it is rotation
                float rot = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));
                float tx = Angles.trnsx(rot, trns), ty = Angles.trnsy(rot, trns);

                //shadow
                Draw.color(0f, 0f, 0f, 0.2f);
                //Draw.rect(regions[i], unit.x + tx + 2f, unit.y + ty - 2f, rot - 90);

                applyColor(unit);

                //TODO merge outlines?
                Draw.rect(regions[i], unit.x + tx, unit.y + ty, rot - 90);
            }
        }
    }

    public void drawMech(Mechc mech){
        Unit unit = (Unit)mech;

        Draw.reset();

        float e = unit.elevation;

        float sin = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f / Mathf.PI, 1f), 0f, e);
        float extension = Mathf.lerp(mech.walkExtend(false), 0, e);
        float boostTrns = e * 2f;

        Floor floor = unit.isFlying() ? Blocks.air.asFloor() : unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.mixcol(Tmp.c1.set(mechLegColor).lerp(Color.white, Mathf.clamp(unit.hitTime)), Math.max(Math.max(0, i * extension / mechStride), unit.hitTime));
            Draw.alpha(unitTrans); //
            Draw.rect(legRegion,
            unit.x + Angles.trnsx(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            unit.y + Angles.trnsy(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            legRegion.width * i * Draw.scl,
            legRegion.height * Draw.scl - Math.max(-sin * i, 0) * legRegion.height * 0.5f * Draw.scl,
            mech.baseRotation() - 90 + 35f*i*e);
        }

        Draw.mixcol(Color.white, unit.hitTime);

        if(unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }else{
            Draw.color(Color.white);
        }

        Draw.alpha(unitTrans); //

        Draw.rect(baseRegion, unit, mech.baseRotation() - 90);

        Draw.mixcol();
    }

    public void applyOutlineColor(Unit unit){
        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.8f), unit.drownTime * 0.9f);
        }
    }

    public void applyColor(Unit unit){
        Draw.color();
        if(healFlash){
            Tmp.c1.set(Color.white).lerp(healColor, Mathf.clamp(unit.healTime - unit.hitTime));
        }
        Draw.mixcol(Tmp.c1, Math.max(unit.hitTime, !healFlash ? 0f : Mathf.clamp(unit.healTime)));

        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.mixcol(Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }
    }

    public static void initUnitTransp(){
        unitTrans = (float)Core.settings.getInt("unitTransparency") / 100f;
    }

    public static void initLegTransp(){
        legTrans = (float)Core.settings.getInt("unitTransparency") / 100f;
    }
    //endregion

    public static class UnitEngine implements Cloneable{
        public float x, y, radius, rotation;

        public UnitEngine(float x, float y, float radius, float rotation){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
        }

        public UnitEngine(){
        }

        public void draw(Unit unit){
            UnitType type = unit.type;
            float scale = type.useEngineElevation ? unit.elevation : 1f;

            if(scale <= 0.0001f) return;

            float rot = unit.rotation - 90;
            Color color = type.engineColor == null ? unit.team.color : type.engineColor;

            Tmp.v1.set(x, y).rotate(rot);
            float ex = Tmp.v1.x, ey = Tmp.v1.y;

            //engine outlines (cursed?)
            /*float z = Draw.z();
            Draw.z(z - 0.0001f);
            Draw.color(type.outlineColor);
            Fill.circle(
            unit.x + ex,
            unit.y + ey,
            (type.outlineRadius * Draw.scl + radius + Mathf.absin(Time.time, 2f, radius / 4f)) * scale
            );
            Draw.z(z);*/

            Draw.color(color);
            Draw.alpha(unitTrans);
            Fill.circle(
            unit.x + ex,
            unit.y + ey,
            (radius + Mathf.absin(Time.time, 2f, radius / 4f)) * scale
            );
            Draw.color(type.engineColorInner);
            Draw.alpha(unitTrans);
            Fill.circle(
            unit.x + ex - Angles.trnsx(rot + rotation, 1f),
            unit.y + ey - Angles.trnsy(rot + rotation, 1f),
            (radius + Mathf.absin(Time.time, 2f, radius / 4f)) / 2f  * scale
            );
        }

        public UnitEngine copy(){
            try{
                return (UnitEngine)clone();
            }catch(CloneNotSupportedException awful){
                throw new RuntimeException("fantastic", awful);
            }
        }
    }

}
