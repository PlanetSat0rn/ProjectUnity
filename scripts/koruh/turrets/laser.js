const lib = this.global.unity.exp;
const powerLaser = Color.valueOf("F9DBB1");

const laser = extend(BulletType, {
    getDamage(b){
        return this.damage + (b.owner.totalLevel() * 10);
    },

    getColor(b){
        return Tmp.c1.set(powerLaser).lerp(Pal.lancerLaser, b.owner.totalLevel() / 10);
    },

    collision(other, x, y){
        this.hit(this.base(), x, y);
        if(other instanceof Healthc){
            var h = other;
            h.damage(this.getDamage(this));
        }else if(other instanceof Unit) {
            var unit = other;
            unit.impulse(Tmp.v3.set(unit).sub(this.x, this.y).nor().scl(this.knockback * 80.0));
            unit.apply(this.status, this.statusDuration);
        }if(!this.pierce){
			this.remove();
        }else{
            this.collided.add(other.id());
        }
    },

    init(b){
        if(!b) return;
        this.super$init(b);

        var target = Damage.linecast(b, b.x, b.y, b.rotation(), this.length);
        b.data = target;

        if(target instanceof Hitboxc){
            var hit = target;

            hit.collision(b, hit.x, hit.y);
            b.collision(hit, hit.x, hit.y);
            b.owner.incExp(1.5);
        }else if(target instanceof Building){
            var tile = target;

            if(tile.collide(b)){
                tile.collision(b);
                this.hit(b, tile.x, tile.y);
                b.owner.incExp(2);
            }
        }else{
            b.data = new Vec2().trns(b.rotation(), this.length).add(b.x, b.y);
        }
    },

    range(){
        return this.length;
    },

    draw(b){
        if(b.data instanceof Position){
            var data = b.data;
            Tmp.v1.set(data);

            Draw.color(this.getColor(b));
            Draw.alpha(0.4);
            //this looks horrible without bloom
            //Drawf.laser(b.team, Core.atlas.find("laser"), Core.atlas.find("laser-end"), b.x, b.y, Tmp.v1.x, Tmp.v1.y, this.width * b.fout());
            Lines.stroke(b.fout()*2.9);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.alpha(1);
            Lines.stroke(b.fout()*1.8);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.color(Color.white);
            Lines.stroke(b.fout());
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);
            Draw.reset();

            Drawf.light(Team.derelict, b.x, b.y, Tmp.v1.x, Tmp.v1.y, 15 * b.fout() + 5, Color.white, 0.6);
        }
    }
});
laser.damage = 30;
laser.lifetime = 18;
laser.speed = 0.0001;
laser.despawnEffect = Fx.none;
laser.pierce = true;
laser.hitSize = 0;
laser.status = StatusEffects.shocked;
laser.statusDuration = 3 * 60;
laser.width = 0.7;
laser.length = 150;
laser.hittable = false;
laser.hitEffect = Fx.hitLiquid;
laser.shootEffect = Fx.hitLiquid;

const laserTurret = lib.extend(PowerTurret, PowerTurret.PowerTurretBuild, "laser-turret", {
    maxLevel: 10,
    expFields: [
        {
            type: "linear",
            field: "reloadTime",
            start: 35,
            intensity: -2
        },
        {
            type: "bool",
            field: "targetAir",
            start: false,
            intensity: 5
        }
    ],
    upgrades: [
        {
            block: "unity-charge-laser-turret",
            min: 10
        },
        {
            block: "unity-frost-laser-turret",
            min: 10
        }
    ]
}, {});
laserTurret.shootType = laser;