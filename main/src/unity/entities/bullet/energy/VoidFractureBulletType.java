package unity.entities.bullet.energy;

import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.effects.*;
import unity.content.effects.SpecialFx.*;
import unity.gen.*;
import unity.util.*;

public class VoidFractureBulletType extends BulletType{
    public float length = 28f;
    public float delay = 30f;
    public float targetingRange = 320f;
    public float trueSpeed;
    public float nextLifetime = 10f;
    public int maxTargets = 15;
    public float spikesRange = 100f, spikesDamage = 200f;
    public Sound activeSound = UnitySounds.fractureShoot, spikesSound = UnitySounds.spaceFracture;

    private static float s;
    private static int in;

    public VoidFractureBulletType(float speed, float damage){
        super(4.3f, damage);
        drag = 0.11f;
        trueSpeed = speed;
        collides = false;
        collidesTiles = false;
        keepVelocity = false;
        layer = Layer.effect + 0.03f;

        despawnEffect = Fx.none;
        smokeEffect = Fx.none;
    }

    @Override
    public float range(){
        return trueSpeed * nextLifetime;
    }

    @Override
    public void init(){
        super.init();
        drawSize = (range() * 2f) + 30f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        if(b.data instanceof FractureData data){
            if(b.fdata <= 0f){
                if(data.target != null && !data.target.isValid()) data.target = null;
                if(data.target == null && b.timer(1, 5f)){
                    s = Float.MAX_VALUE;
                    Floatf<Healthc> score = h -> h.dst2(b) + Mathf.pow(Angles.angleDist(b.rotation(), b.angleTo(h)), 4f);

                    Units.nearbyEnemies(b.team, b.x, b.y, targetingRange, u -> {
                        float c = score.get(u);
                        if(u.isValid() && Angles.within(b.rotation(), b.angleTo(u), 45f) && c < s){
                            s = c;
                            data.target = u;
                        }
                    });
                    Vars.indexer.allBuildings(b.x, b.y, targetingRange, build -> {
                        if(build.team != b.team && Angles.within(b.rotation(), b.angleTo(build), 45f)){
                            float c = score.get(build);
                            if(c < s){
                                s = c;
                                data.target = build;
                            }
                        }
                    });
                }
                if(data.target != null){
                    b.rotation(Mathf.slerpDelta(b.rotation(), b.angleTo(data.target), 0.1f));
                }
                if(b.time >= delay){
                    b.time = 0f;
                    b.lifetime = nextLifetime;
                    b.fdata = 1f;
                    b.drag = 0f;
                    b.vel.trns(b.rotation(), trueSpeed);
                    data.x = b.x;
                    data.y = b.y;
                    activeSound.at(b.x, b.y, Mathf.random(0.9f, 1.1f));
                }
            }else{
                Utils.collideLineRawEnemy(b.team, b.lastX, b.lastY, b.x, b.y, 3f, (build, direct) -> {
                    if(direct){
                        if(data.collided.add(build.id)) build.damage(damage * b.damageMultiplier() * buildingDamageMultiplier);
                        if(build.block.absorbLasers){
                            Tmp.v1.set(b.x, b.y).sub(b.lastX, b.lastY).setLength2(build.dst2(b.lastX, b.lastY)).add(b.lastX, b.lastY);
                            b.x = Tmp.v1.x;
                            b.y = Tmp.v1.y;
                            b.vel.setZero();
                        }
                    }
                    return build.block.absorbLasers;
                }, unit -> {
                    if(data.collided.add(unit.id)) hitEntity(b, unit, unit.health);
                    return false;
                }, (ex, ey) -> hit(b, ex, ey), true);
            }
        }
    }

    @Override
    public void removed(Bullet b){
        super.removed(b);

        if(b.data instanceof FractureData data){
            VoidFractureData d = new VoidFractureData();
            d.x = data.x;
            d.y = data.y;
            d.x2 = b.x;
            d.y2 = b.y;

            //tmp.clear();
            in = 0;
            if(!b.hit){
                Utils.collideLineRawEnemy(b.team, d.x, d.y, d.x2, d.y2, spikesRange, (build, direct) -> {
                    if(direct && in < maxTargets){
                        float s = build.hitSize() / 2f;
                        in++;
                        build.damage(spikesDamage);
                        Vec2 v = Intersector.nearestSegmentPoint(d.x, d.y, d.x2, d.y2, build.x + Mathf.range(8f), build.y + Mathf.range(8f), Tmp.v1);
                        Tmp.v2.set(build).sub(v).setLength2(Math.max(v.dst2(build) - (s * s), 0f)).add(v);
                        hitEffect.at(Tmp.v2);
                        d.spikes.add(v.x, v.y, build.x, build.y);
                    }
                    return false;
                }, unit -> {
                    if(in < maxTargets){
                        float s = unit.hitSize / 2f;
                        in++;
                        unit.damage(spikesDamage);
                        unit.apply(status, statusDuration);
                        Vec2 v = Intersector.nearestSegmentPoint(d.x, d.y, d.x2, d.y2, unit.x + Mathf.range(8f), unit.y + Mathf.range(8f), Tmp.v1);
                        Tmp.v2.set(unit).sub(v).setLength2(Math.max(v.dst2(unit) - (s * s), 0f)).add(v);
                        hitEffect.at(Tmp.v2);
                        d.spikes.add(v.x, v.y, unit.x, unit.y);
                    }
                    return false;
                },
                h -> Intersector.distanceSegmentPoint(d.x, d.y, d.x2, d.y2, h.getX(), h.getY()), null, false);
                
                if(!d.spikes.isEmpty()) spikesSound.at((d.x + d.x2) / 2f, (d.y + d.y2) / 2f, Mathf.random(0.9f, 1.1f));
            }
            SpecialFx.voidFractureEffect.at((d.x + d.x2) / 2f, (d.y + d.y2) / 2f, 0f, d);
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof FractureData data){
            Draw.color(Color.black);
            if(b.fdata <= 0f){
                float in = Mathf.clamp(b.time / delay);
                Drawf.tri(b.x, b.y, 12f * in, length, b.rotation());
                Drawf.tri(b.x, b.y, 12f * in, length, b.rotation() + 180f);
            }else{
                Drawf.tri(b.x, b.y, 12f * b.fout(), length, b.rotation());
                Drawf.tri(b.x, b.y, 12f * b.fout(), length / 2f, b.rotation() + 180f);

                for(int i = 0; i < 3; i++){
                    float f = Mathf.lerp(12f, 3f, i / 2f);
                    float a = Mathf.lerp(0.25f, 1f, (i / 2f) * (i / 2f));

                    Draw.alpha(a);
                    Lines.stroke(f);
                    Lines.line(data.x, data.y, b.x, b.y, false);
                    Drawf.tri(b.x, b.y, f * 1.22f, f * 2f, b.angleTo(data.x, data.y) + 180f);
                    Drawf.tri(data.x, data.y, f * 1.22f, f * 2f, b.angleTo(data.x, data.y));
                }
            }
        }
    }

    @Override
    public void drawLight(Bullet b){

    }

    @Override
    public void init(Bullet b){
        super.init(b);
        b.data = new FractureData();
    }

    static class FractureData{
        Healthc target;
        float x, y;
        IntSet collided = new IntSet();
    }
}