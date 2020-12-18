package unity.ai;

import arc.math.geom.*;
import mindustry.ai.types.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import unity.entities.comp.*;

import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class WormAI extends FlyingAI{
    @Override
    public void updateMovement(){
        if(worm().isHead()){
            if(target != null && (unit.hasWeapons() || worm().headDamage() > 0f) && command() == UnitCommand.attack){
                attack(120f);
            }

            if(target == null && command() == UnitCommand.attack && state.rules.waves && unit.team == state.rules.defaultTeam){
                moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 120f);
            }

            if(command() == UnitCommand.rally){
                moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
            }
        }else{
            worm().updateMovement();
        }
    }

    @Override
    protected void attack(float circleLength){
        if(worm().isHead()){
            super.attack(circleLength);
            if(!unit.vel.isZero(0.1f)){
                unit.rotation(unit.vel.angle());
            }
        }
    }

    @Override
    protected void moveTo(Position target, float circleLength){
        if(worm().isHead()){
            super.moveTo(target, circleLength);
            if(!unit.vel.isZero(0.1f)){
                unit.rotation(unit.vel.angle());
            }
        }
    }

    @Override
    protected void moveTo(Position target, float circleLength, float smooth){
        if(worm().isHead()){
            super.moveTo(target, circleLength, smooth);
            if(!unit.vel.isZero(0.1f)){
                unit.rotation(unit.vel.angle());
            }
        }
    }

    protected <T extends Unit & Wormc> T worm(){
        return (T)unit;
    }
}