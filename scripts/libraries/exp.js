//EXP library by sk7725. Recommended for turrets, works with any block.
//the fields in Block are global, but does not matter if you set them for each block every calculation - just like Tmp.
//type: The block you are extending.
//build: the building you are extending.
//name: the name of your block.
//obj: what will override the block; add exp params here.
//objb: what will override the building.
const clone = obj => {
    if(obj === null || typeof(obj) !== 'object') return obj;
    var copy = obj.constructor();
    for(var attr in obj) {
        if(obj.hasOwnProperty(attr)) {
            copy[attr] = obj[attr];
        }
    };
    return copy;
}
module.exports = {
    extend(type, build, name, obj, objb) {
        if(obj == undefined) obj = {};
        if(objb == undefined) objb = {};
        obj = Object.assign({
            //start
            maxLevel: 20,
            level0Color: Pal.accent,
            levelMaxColor: Color.valueOf("fff4cc"),
            exp0Color: Color.valueOf("84ff00"),
            expMaxColor: Color.valueOf("90ff00"),
            expFields: [],
            hasLevelEffect: true,
            levelUpFx: Fx.upgradeCore,
            levelUpSound: Sounds.message,
            //type, field, start, intensity
            //below are legacy arrays
            linearInc: [],
            linearIncStart: [],
            linearIncMul: [],
            expInc: [],
            expIncStart: [],
            expIncMul: [],
            rootInc: [],
            rootIncMul: [],
            rootIncStart: [],
            boolInc: [],
            boolIncStart: [],
            boolIncMul: [],
            listInc: [],
            listIncStart: [],
            listIncMul: [],
            hasLevelFunction: false,
            hasCustomUpdate: false,
            forStats: new ObjectMap(),
            caches: []
            //end
        }, obj, {
            //start
            getLevel(exp) {
                return Math.min(Mathf.floorPositive(Mathf.sqrt(exp * 0.1)), this.maxLevel);
            },
            getRequiredEXP(lvl) {
                return lvl * lvl * 10;
            },
            getLvlf(exp) {
                var lvl = this.getLevel(exp);
                if(lvl >= this.maxLevel) return 1;
                var last = this.getRequiredEXP(lvl);
                var next = this.getRequiredEXP(lvl + 1);
                return (exp - last) / (next - last);
            },
            setEXPStats(build) {
                var exp = build.totalExp();
                var lvl = this.getLevel(exp);
                if(this.linearInc.length == 1) {
                    this[this.linearInc[0]] = Math.max(this.linearIncStart[0] + this.linearIncMul[0] * lvl, 0);
                } else if(this.linearInc.length > 0) {
                    this.linearEXP(tile, lvl);
                };
                if(this.expInc.length == 1) {
                    this[this.expInc[0]] = Math.max(this.expIncStart[0] + Mathf.pow(this.expIncMul[0], lvl), 0);
                } else if(this.expInc.length > 0) {
                    this.expEXP(tile, lvl);
                };
                if(this.rootInc.length == 1) {
                    this[this.rootInc[0]] = Math.max(this.rootIncStart[0] + Mathf.sqrt(this.rootIncMul[0] * lvl), 0);
                } else if(this.rootInc.length > 0) {
                    this.rootEXP(tile, lvl);
                };
                if(this.boolInc.length == 1) {
                    this[this.boolInc[0]] = (this.boolIncStart[0]) ? (lvl < this.boolIncMul[0]) : (lvl >= this.boolIncMul[0]);
                } else if(this.boolInc.length > 0) {
                    this.boolEXP(tile, lvl);
                };
                if(this.listInc.length > 0) {
                    this.listEXP(tile, lvl);
                };
            },
            linearEXP(tile, lvl) {
                for(var i = 0; i < this.linearInc.length; i++) {
                    this[this.linearInc[i]] = Math.max(this.linearIncStart[i] + this.linearIncMul[i] * lvl, 0);
                };
            },
            expEXP(tile, lvl) {
                for(var i = 0; i < this.expInc.length; i++) {
                    this[this.expInc[i]] = Math.max(this.expIncStart[i] + Mathf.pow(this.expIncMul[i], lvl), 0);
                };
            },
            rootEXP(tile, lvl) {
                for(var i = 0; i < this.rootInc.length; i++) {
                    this[this.rootInc[i]] = Math.max(this.rootIncStart[i] + Mathf.sqrt(this.rootIncMul[i] * lvl), 0);
                };
            },
            boolEXP(tile, lvl) {
                for(var i = 0; i < this.boolInc.length; i++) {
                    this[this.boolInc[i]] = (this.boolIncStart[i]) ? (lvl < this.boolIncMul[i]) : (lvl >= this.boolIncMul[i]);
                };
            },
            listEXP(tile, lvl) {
                for(var i = 0; i < this.listInc.length; i++) {
                    this[this.listInc[i]] = this.listIncMul[i][Math.min(lvl, this.listIncMul[i].length - 1)];
                };
            },
            setBars() {
                this.super$setBars();
                this.bars.add("level", func(build => {
                    return new Bar(prov(() => Core.bundle.get("explib.level") + " " + this.getLevel(build.totalExp())), prov(() => Tmp.c1.set(this.level0Color).lerp(this.levelMaxColor, this.getLevel(build.totalExp()) / this.maxLevel)), floatp(() => {
                        return this.getLevel(build.totalExp()) / this.maxLevel;
                    }));
                }));
                this.bars.add("exp", func(build => {
                    return new Bar(prov(() => (build.totalExp() < this.maxExp) ? Core.bundle.get("explib.exp") : Core.bundle.get("explib.max")), prov(() => Tmp.c1.set(this.exp0Color).lerp(this.expMaxColor, this.getLvlf(build.totalExp()))), floatp(() => {
                        return this.getLvlf(build.totalExp());
                    }));
                }));
            },
            isNumerator(stat) {
                return stat == Stat.inaccuracy || stat == Stat.shootRange;
            },
            setStats() {
                this.forStats.put("range", Stat.shootRange);
                this.forStats.put("inaccuracy", Stat.inaccuracy);
                this.forStats.put("reloadTime", Stat.reload);
                this.forStats.put("targetAir", Stat.targetsAir);
                this.forStats.put("targetGround", Stat.targetsGround);
                this.super$setStats();
                for(var i = 0; i < this.linearInc.length; i++) {
                    var temp = this.forStats.get(this.linearInc[i]);
                    if(temp) {
                        //fuck
                        if(this.isNumerator(temp) == true) this.stats.add(temp, Core.bundle.get("explib.linear.numer"), this.linearIncMul[i] > 0 ? "+" : "", (100 * this.linearIncMul[i] / this.linearIncStart[i]).toFixed(2));
                        else this.stats.add(temp, Core.bundle.get("explib.linear.denomin"), String(this.linearIncStart[i]), this.linearIncMul[i] > 0 ? "+" : "", String(this.linearIncStart[i]), this.linearIncMul[i]);
                    }
                };
                for(var i = 0; i < this.expInc.length; i++) {
                    var temp = this.forStats.get(this.expInc[i]);
                    if(temp) {
                        if(this.isNumerator(temp) == true) this.stats.add(temp, Core.bundle.get("explib.expo.numer"), this.expIncMul[i], String(this.expIncStart[i]));
                        else this.stats.add(temp, Core.bundle.get("explib.expo.denomin"), String(this.expIncStart[i]), String(this.expIncStart[i]), this.expIncMul[i]);
                    }
                };
                for(var i = 0; i < this.rootInc.length; i++) {
                    var temp = this.forStats.get(this.rootInc[i]);
                    if(temp) {
                        if(this.isNumerator(temp) == true) this.stats.add(temp, Core.bundle.get("explib.root.numer"), this.rootIncMul[i], String(this.rootIncStart[i]));
                        else this.stats.add(temp, Core.bundle.get("explib.root.denomin"), String(this.rootIncStart[i]), String(this.rootIncStart[i]), this.rootIncMul[i]);
                    }
                };
                for(var i = 0; i < this.boolInc.length; i++) {
                    var temp = this.forStats.get(this.boolInc[i]);
                    if(temp) this.stats.add(temp, Core.bundle.get("explib.bool"), String(this.boolIncMul[i]), !this.boolIncStart[i]);
                };
            }
            //end
        });
        const expblock = extendContent(type, name, obj);
        expblock.maxExp = expblock.getRequiredEXP(expblock.maxLevel);
        for(var i = 0; i < expblock.expFields.length; i++) {
            var tobj = expblock.expFields[i];
            if(tobj.type == undefined) tobj.type = "linear";
            expblock[tobj.type + "Inc"].push(tobj.field);
            expblock[tobj.type + "IncStart"].push((tobj.start == undefined) ? ((expblock[tobj.field] == undefined || expblock[tobj.field] == null) ? 0 : expblock[tobj.field]) : tobj.start);
            expblock[tobj.type + "IncMul"].push((tobj.intensity == undefined) ? 1 : tobj.intensity);
            if(tobj.cacheValue) expblock.caches.push(tobj.field);
        };
        expblock.hasLevelFunction = (typeof objb["levelUp"] === "function");
        expblock.hasCustomUpdate = (typeof objb["customUpdate"] === "function");
        expblock.hasCustomRW = (typeof objb["customRead"] === "function");
        expblock.hasCache = (expblock.caches.length > 0);
        var objc = clone(objb);
        objc = Object.assign(objc, {
            totalExp() {
                return this._exp;
            },
            totalLevel() {
                return expblock.getLevel(this._exp);
            },
            expf() {
                return expblock.getLvlf(this._exp);
            },
            levelf() {
                return this._exp / expblock.maxExp;
            },
            setExp(a) {
                this._exp = a;
            },
            incExp(a) {
                if(this._exp >= expblock.maxExp) return;
                this._exp += a;
                if(this._exp > expblock.maxExp) this._exp = expblock.maxExp;
                this._changedVal = true;
                if(!expblock.hasLevelEffect) return;
                if(expblock.getLevel(this._exp - a) != expblock.getLevel(this._exp)) {
                    expblock.levelUpFx.at(this.x, this.y, expblock.size);
                    expblock.levelUpSound.at(this.x, this.y);
                    if(expblock.hasLevelFunction) this.levelUp(expblock.getLevel(this._exp));
                };
            },

            updateCaches(){
                for(var i=0; i<expblock.caches.length; i++){
                    this["_cache_" + expblock.caches[i]] = expblock[expblock.caches[i]];
                }
            },
            getCache(fieldName){
                return this["_cache_" + fieldName];
            },

            updateTile() {
                expblock.setEXPStats(this);
                if(this._changedVal && expblock.hasCache) this.updateCaches();
                if(expblock.hasCustomUpdate) this.customUpdate();
                else this.super$updateTile();
            },
            read(stream, version) {
                this.super$read(stream, version);
                this._exp = stream.i();
                if(expblock.hasCustomRW) this.customRead(stream, version);
                if(expblock.hasCache){
                    expblock.setEXPStats(this);
                    this.updateCaches();
                }
            },
            write(stream) {
                this.super$write(stream);
                stream.i(this._exp);
                if(expblock.hasCustomRW) this.customWrite(stream);
            }
        });
        //Extend Building
        expblock.buildType = ent => {
            ent = extendContent(build, expblock, objc);
            ent._exp = 0;
            ent._changedVal = true;
            return ent;
        };
        return expblock;
    }
}
