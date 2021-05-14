package unity.entities.comp;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.gen.*;

@SuppressWarnings("unused")
@EntityDef({Unitc.class, MonolithSoulc.class})
@EntityComponent
@ExcludeGroups(Unitc.class)
abstract class MonolithSoulComp implements Unitc{
    static final Rect rec1 = new Rect();
    static final Rect rec2 = new Rect();

    static final float maxSize = 12f;
    static final Prov<UnitType> defaultType = () -> UnityUnitTypes.monolithSoul;

    float healAmount;

    @Import UnitController controller;
    @Import Team team;
    @Import float x, y, rotation, health, maxHealth, hitSize;
    @Import boolean dead;

    @Override
    public void update(){
        if(controller == null){
            kill();
        }else{
            health -= maxHealth / (5f * Time.toSeconds) * Time.delta;

            if(!dead && isPlayer()){
                Units.nearby(team, x, y, hitSize, unit -> {
                    hitbox(rec1);
                    unit.hitbox(rec2);

                    if(!unit.dead && rec1.overlaps(rec2)){
                        invoke(unit);

                        if(unit.getPlayer() == null){
                            unit.controller(getPlayer());
                        }
                    }
                });
            }
        }

        if(Mathf.chanceDelta(0.5f)){
            UnityFx.monolithSoul.at(x, y, rotation, hitSize * 2f);
        }

        if(dead){
            destroy();
        }
    }

    @Override
    @Replace
    public int cap(){
        return Integer.MAX_VALUE;
    }

    public void invoke(Unit unit){
        unit.heal(healAmount);
        kill();
    }
}
