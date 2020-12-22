package unity.ai;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import unity.entities.comp.*;

import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class WormAI extends FlyingAI{
    @Override
    public void updateMovement(){
        if(worm().isHead()){
            if(target != null && worm().hasWeapons() && command() == UnitCommand.attack){
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
            updateRotation();
        }
    }

    @Override
    protected void moveTo(Position target, float circleLength){
        if(worm().isHead()){
            super.moveTo(target, circleLength);
            updateRotation();
        }
    }

    @Override
    protected void moveTo(Position target, float circleLength, float smooth){
        if(worm().isHead()){
            super.moveTo(target, circleLength, smooth);
            updateRotation();
        }
    }

    @Override
    protected void updateWeapons(){
        if(!worm().isHead()){
            worm().isShooting = worm().head().isShooting();

            for(WeaponMount mount : worm().mounts){
                Weapon weapon = mount.weapon;

                Vec2 to = Predict.intercept(worm(), Core.input.mouseWorld(), weapon.bullet.speed);
                mount.aimX = to.x;
                mount.aimY = to.y;

                mount.shoot = worm().head().isShooting();
                mount.rotate = true;

                worm().aimX = mount.aimX;
                worm().aimY = mount.aimY;
            }
        }else{
            super.updateWeapons();
        }
    }

    protected void updateRotation(){
        if(!unit.vel.isZero(0.001f)){
            unit.rotation(Mathf.slerpDelta(unit.rotation(), unit.prefRotation(), unit.type.rotateSpeed / 60f));

            Wormc child = worm().child();
            if(child != null){
                Tmp.v1.trns(unit.rotation(), -worm().segmentOffset() / 2f).add(worm());
                Tmp.v2.trns(child.rotation(), child.segmentOffset() / 2f).add(child);
                Tmp.v3.set(Tmp.v2).sub(Tmp.v1);

                unit.vel.add(Tmp.v3);
            }
        }
    }

    protected <T extends Unit & Wormc> T worm(){
        return (T)unit;
    }
}
