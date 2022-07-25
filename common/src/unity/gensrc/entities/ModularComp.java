package unity.gensrc.entities;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import unity.annotations.Annotations.*;
import unity.entities.prop.*;
import unity.entities.type.*;
import unity.gen.entities.*;
import unity.mod.*;
import unity.parts.*;
import unity.parts.PanelDoodadType.*;
import unity.parts.types.*;
import unity.util.*;

import java.util.*;

import static arc.graphics.g2d.Draw.color;
import static mindustry.Vars.*;


//Ok we need to essentially replace nearly every usage of UnitType bc the stats dont come from there anymore

@SuppressWarnings("unused")
@EntityComponent
abstract class ModularComp implements Unitc, Factionc, ElevationMovec{
    @Import
    UnitType type;
    @Import
    boolean dead;
    @Import
    float health, maxHealth, rotation, armor, drownTime;
    @Import
    int id;
    @Import
    UnitController controller;
    @Import
    WeaponMount[] mounts;
    @Import
    float elevation;
    @Import
    public transient Seq<Unit> controlling;
    @Import
    public transient Floor lastDrownFloor;

    transient ModularConstruct construct;
    transient boolean constructLoaded = false;
    public transient Seq<PanelDoodad> doodadlist = new Seq<>();
    public byte[] constructdata;

    //visuals
    public transient float driveDist = 0;
    public transient float clipsize = 0;
    //stat
    public transient float enginepower = 0;
    public transient float speed = 0;
    public transient float rotateSpeed = 0;
    public transient float massStat = 0;
    public transient float weaponrange = 0;
    public transient int itemcap = 0;

    @Override
    public Faction faction(){
        return Faction.youngcha;
    }

    @Override
    public void add(){
        if(ModularConstruct.cache.containsKey(this)){
            construct = new ModularConstruct(ModularConstruct.cache.get(this));
        }else{
            if(constructdata != null){
                construct = new ModularConstruct(constructdata);
            }else{
                String templateStr = ((PUUnitTypeCommon)type).prop(ModularProps.class).templates.random();
                ModularConstructBuilder test = new ModularConstructBuilder(3, 3);
                test.set(Base64.getDecoder().decode(templateStr.trim().replaceAll("[\\t\\n\\r]+", "")));
                construct = new ModularConstruct(test.exportCropped());
            }
        }
        constructdata = Arrays.copyOf(construct.data, construct.data.length);

        var statmap = new ModularUnitStatMap();
        ModularConstructBuilder.getStats(construct.parts, statmap);
        applyStatMap(statmap);
        if(construct != ModularConstruct.test){
            constructLoaded = true;
            if(!headless){
                UnitDoodadGenerator.initDoodads(construct.parts.length, doodadlist, construct);
            }
            int w = construct.parts.length;
            int h = construct.parts[0].length;
            int maxx = 0, minx = 256;
            int maxy = 0, miny = 256;

            for(int j = 0; j < h; j++){
                for(int i = 0; i < w; i++){
                    if(construct.parts[i][j] != null){
                        maxx = Math.max(i, maxx);
                        minx = Math.min(i, minx);
                        maxy = Math.max(j, maxy);
                        miny = Math.min(j, miny);
                    }
                }
            }
            clipsize = Mathf.dst((maxy - miny), (maxx - minx)) * ModularPartType.partSize * 6;
            hitSize(((maxy - miny) + (maxx - minx)) * 0.5f * ModularPartType.partSize);
        }
    }

    public void applyStatMap(ModularUnitStatMap statmap){
        if(construct.parts.length == 0){
            return;
        }
        float power = statmap.getOrCreate("power").getFloat("value");
        float powerUse = statmap.getOrCreate("powerusage").getFloat("value");
        float eff = Mathf.clamp(power / powerUse, 0, 1);

        float hRatio = Mathf.clamp(this.health / this.maxHealth);
        this.maxHealth = statmap.getOrCreate("health").getFloat("value");
        if(savedHp <= 0){
            this.health = hRatio * this.maxHealth;
        }else{
            this.health = savedHp;
            savedHp = -1;
        }
        var weapons = statmap.stats.getList("weapons");
        mounts = new WeaponMount[weapons.length()];
        weaponrange = 0;

        int weaponslots = Math.round(statmap.getValue("weaponslots"));
        int weaponslotsused = Math.round(statmap.getValue("weaponslotuse"));

        for(int i = 0; i < weapons.length(); i++){
            var weapon = getWeaponFromStat(weapons.getMap(i));
            weapon.reload *= 1f / eff;
            if(weaponslotsused > weaponslots){
                weapon.reload *= 4f * (weaponslotsused - weaponslots);
            }
            if(!headless){
                weapon.load();
            }
            mounts[i] = weapon.mountType.get(weapon);
            ModularPart mpart = weapons.getMap(i).get("part");
            weaponrange = Math.max(weaponrange, weapon.bullet.range + Mathf.dst(mpart.cx, mpart.cy) * ModularPartType.partSize);
        }

        int abilityslots = Math.round(statmap.getValue("abilityslots"));
        int abilityslotsused = Math.round(statmap.getValue("abilityslotuse"));

        if(abilityslotsused <= abilityslots){
            var abilitiesStats = statmap.stats.getList("abilities");
            var newAbilities = new Ability[abilitiesStats.length()];
            for(int i = 0; i < abilitiesStats.length(); i++){
                var abilityStat = abilitiesStats.getMap(i);
                Ability ability = abilityStat.get("ability");
                newAbilities[i] = ability.copy();
            }
            abilities(newAbilities);
        }

        this.massStat = statmap.getOrCreate("mass").getFloat("value");


        float wheelspd = statmap.getOrCreate("wheel").getFloat("nominal speed", 0);
        float wheelcap = statmap.getOrCreate("wheel").getFloat("weight capacity", 0);
        speed = eff * Mathf.clamp(wheelcap / this.massStat, 0, 1) * wheelspd;
        rotateSpeed = Mathf.clamp(10f * speed / (float)Math.max(construct.parts.length, construct.parts[0].length), 0, 5);

        armor = statmap.getValue("armour", "realValue");
        itemcap = (int)statmap.getValue("itemcapacity");
    }

    @Replace
    public void setType(UnitType type){
        this.type = type;
        if(controller == null) controller(type.createController(self())); //for now
        //instead of type != YoungchaUnitTypes.modularUnitSmall
        if(!(type instanceof PUUnitTypeCommon u && u.hasProp(ModularProps.class))){
            this.maxHealth = type.health;
            drag(type.drag);
            this.armor = type.armor;
            hitSize(type.hitSize);
            hovering(type.hovering);
            if(controller == null) controller(type.createController(self()));
            if(mounts().length != type.weapons.size) setupWeapons(type);
            if(abilities().length != type.abilities.size){
                //hacky shrink
                abilities(type.abilities.map(Ability::copy).shrink());
            }
        }
    }

    @Replace
    public void setupWeapons(UnitType type){
        if(!(type instanceof PUUnitTypeCommon u && u.hasProp(ModularProps.class))){
            mounts = new WeaponMount[type.weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = type.weapons.get(i).mountType.get(type.weapons.get(i));
            }
        }
    }

    public void initWeapon(Weapon w){
        if(w.recoilTime < 0) w.recoilTime = w.reload;
    }

    public Weapon getWeaponFromStat(ValueMap weaponStat){
        Weapon weapon = weaponStat.get("weapon");
        initWeapon(weapon);
        return weapon;
    }


    ///replaces ==================================================================================================================================


    @Override
    public void update(){
        if(construct != null && constructdata == null){
            Log.info("uh constructdata died");
            constructdata = Arrays.copyOf(construct.data, construct.data.length);
        }
        float velLen = this.vel().len();

        if(construct != null && velLen > 0.01f && elevation < 0.01){
            driveDist += velLen;
            DrawTransform dt = new DrawTransform(new Vec2(this.x(), this.y()), rotation);
            float dustVel = 0;
            if(moving){
                dustVel = velLen - speed;
            }
            Vec2 nv = vel().cpy().nor().scl(dustVel * 40);//
            Vec2 nvt = new Vec2();
            final Vec2 pos = new Vec2();
            construct.partlist.each(part -> {
                boolean b = part.getY() - 1 < 0 || (construct.parts[part.getX()][part.getY() - 1] != null && construct.parts[part.getX()][part.getY() - 1].type instanceof ModularWheelType);
                if(!(Mathf.random() > 0.1 || !(part.type instanceof ModularWheelType)) && b){
                    pos.set(part.cx * ModularPartType.partSize, part.ay * ModularPartType.partSize);
                    dt.transform(pos);
                    nvt.set(nv.x + Mathf.range(3), nv.y + Mathf.range(3));
                    Tile t = Vars.world.tileWorld(pos.x, pos.y);
                    if(t != null){
                        //TODO what the heck
                        new Effect(70, e -> {
                            color(e.color);
                            Vec2 v = (Vec2)e.data;
                            Fill.circle(e.x + e.finpow() * v.x, e.y + e.finpow() * v.y, (7f - e.fin() * 7f) * 0.5f);
                        }).layer(Layer.debris).at(pos.x, pos.y, 0, t.floor().mapColor, nvt);
                    }

                }
            });

        }
        moving = false;
    }

    @Replace
    public int itemCapacity(){
        return itemcap;
    }

    @Replace
    public boolean hasWeapons(){
        return mounts.length > 0;
    }

    @Replace
    public float range(){
        return weaponrange;
    }

    @Replace(value = 5)
    @Override
    public float clipSize(){
        if(isBuilding()){
            return state.rules.infiniteResources ? Float.MAX_VALUE : Math.max(clipsize, type.region.width) + buildingRange + tilesize * 4.0F;
        }
        if(mining()){
            return clipsize + type.mineRange;
        }
        return clipsize;
    }

    @Replace
    @Override
    public float speed(){
        float strafePenalty = isGrounded() || !isPlayer() ? 1.0F : Mathf.lerp(1.0F, type.strafePenalty, Angles.angleDist(vel().angle(), rotation) / 180.0F);
        float boost = Mathf.lerp(1.0F, type.canBoost ? type.boostMultiplier : 1.0F, elevation);
        return speed * strafePenalty * boost * floorSpeedMultiplier();
    }

    @Replace
    public float mass(){
        return massStat == 0 ? type.hitSize * type.hitSize : massStat;
    }

    transient boolean moving = false;

    @Replace
    public void rotateMove(Vec2 vec){
        moveAt(Tmp.v2.trns(rotation, vec.len()));
        moving = vec.len2() > 0.1;
        if(!vec.isZero()){
            rotation = Angles.moveToward(rotation, vec.angle(), rotateSpeed * Math.max(Time.delta, 1));
        }
    }

    @Replace
    public void lookAt(float angle){
        rotation = Angles.moveToward(rotation, angle, rotateSpeed * Time.delta * speedMultiplier());
    }

    transient float savedHp = -1;

    @Override
    public void read(Reads read){
        savedHp = health;
    }

    @Replace
    public void updateDrowning(){
        Floor floor = drownFloor();
        if(floor != null && floor.isLiquid && floor.drownTime > 0){
            lastDrownFloor = floor;
            drownTime += Time.delta / floor.drownTime / type.drownTimeMultiplier / (hitSize() / 8f);
            if(Mathf.chanceDelta(0.05F)){
                floor.drownUpdateEffect.at(x(), y(), hitSize(), floor.mapColor);
            }
            if(drownTime >= 0.999F && !net.client()){
                kill();
                Events.fire(new UnitDrownEvent(self()));
            }
        }else{
            drownTime -= Time.delta / 50.0F;
        }
        drownTime = Mathf.clamp(drownTime);
    }

}