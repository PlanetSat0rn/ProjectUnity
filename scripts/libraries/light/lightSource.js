//light source, does all the calculations at placement and when any block has been placed.
//calc method: first raytrace, then if updated check if block placement area(x, y) intersects with each straight line from the source(aka the theta is the same).
const Integer = java.lang.Integer;
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
            //The strength of light, used by consumers
            lightStrength: 60,
            //The distance light does before dissapating
            lightLength: 50,
            //The absolute distance a light can reach, regardless of lightRepeaters
            maxLightLength: 5000,
            //the max reflections this light has, everything that affects this light is consided a reflection
            maxReflections: 128,
            lightColor: Color.white,
            //whether to scale lightStrength with the input power status
            scaleStatus: true,
            //whether to display angle configuration
            angleConfig: false,
            //The interval light is updated
            lightInterval: 20,
            //defaults
            dirs: [
                [1, 0],
                [1, 1],
                [0, 1],
                [-1, 1],
                [-1, 0],
                [-1, -1],
                [0, -1],
                [1, -1]
            ],
            update: true,
            rotate: true
            //end
        }, obj, {
            //start
            setBars() {
                this.super$setBars();
                this.bars.add("light", func(build => {
                    return new Bar(prov(() => Core.bundle.format("lightlib.light", build.lightPower())), prov(() => this.lightColor), floatp(() => {
                        return build.lightPower() / this.lightStrength;
                    }));
                }));
            },
            setStats(){
                this.super$setStats();
                this.stats.add(Stat.output, "@ @", Core.bundle.format("lightlib.light", this.lightStrength), StatUnit.perSecond.localized());
            }
            //end
        });
        const lightblock = extendContent(type, name, obj);
        lightblock.reflowTimer = lightblock.timers++;
        if(lightblock.size % 2 == 0) print("[scarlet]Even - sized light blocks are not supported! Continue anyways?[]");
        //lightblock.hasLevelFunction = (typeof objb["levelUp"] === "function");
        lightblock.hasCustomUpdate = (typeof objb["customUpdate"] === "function");
        lightblock.hasCustomRW = (typeof objb["customRead"] === "function");
        objb = Object.assign(objb, {
            //angle strengthPercentage lengthleft color
            _lightData: [0, 100, lightblock.lightLength, lightblock.lightColor],
            //array of tiles
            _ls: [],
            //array of _lightData for each _ls
            _lsData: [],
            _lCons: [],
            //only used in angleConfig
            _angle: 0,
            _strength: 0,
            _lightInit: false,
            setInit(a) {
                this._lightInit = a;
            },
            initDone() {
                return this._lightInit;
            },
            getAngleDeg() {
                return (lightblock.rotate) ? this.rotDeg() : this._angle * 45;
            },
            getAngle() {
                return (lightblock.rotate) ? this.rotation * 2 : this._angle;
            },
            setAngle(a) {
                this._angle = a;
                this.lightMarchStart(lightblock.lightLength, lightblock.maxLightLength);
            },
            addAngle(a) {
                this.setAngle((this._angle + a + 8) % 8);
            },
            lightData() {
                return this._lightData;
            },
            setLightData(d) {
                this._lightData = d;
            },
            setPower(a) {
                this._strength = a;
            },
            lightPower() {
                if(!this._lightInit) return this.targetStrength();
                return this._strength;
            },
            lpowerf() {
                return this.lightPower() / lightblock.lightStrength;
            },
            getPowerStatus() {
                if((!lightblock.hasPower) || this.power == null) return 1;
                return this.power.status;
            },
            targetStrength() {
                if(!this.cons.valid()) return 0;
                return (lightblock.scaleStatus) ? lightblock.lightStrength * this.getPowerStatus() : lightblock.lightStrength;
            },
            updateTile() {
                this.setPower(this.targetStrength());
                if(!this.initDone()) this.lightMarchStart(lightblock.lightLength, lightblock.maxLightLength);
                else if(lightblock.lightInterval <= 0 || this.timer.get(lightblock.reflowTimer, lightblock.lightInterval)) {
                    if(this.lightPower() > 1) this.lightMarchStart(lightblock.lightLength, lightblock.maxLightLength);
                    else this.clearCons();
                }
                if(lightblock.hasCustomUpdate) this.customUpdate();
                else this.super$updateTile();
            },
            read(stream, version) {
                this.super$read(stream, version);
                this._angle = stream.b();
                if(lightblock.hasCustomRW) this.customRead(stream, version);
            },
            write(stream) {
                this.super$write(stream);
                stream.b(this._angle);
                if(lightblock.hasCustomRW) this.customWrite(stream);
            },
            drawLight() {
                //TODO make light draw on beams
                Drawf.light(this.team, this.x, this.y, (this.lightPower() * 0.1 + 60) * this.getPowerStatus(), lightblock.lightColor, 0.8);
            },
            drawLightLasers() {
                if(this == null || !this.isAdded() || this.lightPower() <= 1) return;
                Draw.z(Layer.effect - 1);
                Draw.blend(Blending.additive);
                const w = 1 + Math.min(this.lightPower() / 1000, 10);
                Lines.stroke(w);
                //var now = null;
                //var next = null;
                for(var i = 0; i < this._ls.length; i++) {
                    if(this._lsData[i] == null) continue;
                    //print("Drawing Data: "+this._lsData[i]);
                    var a = this._lsData[i][1] / 100 * (this.lightPower() / lightblock.lightStrength);
                    Draw.color(this._lsData[i][3], a);
                    if(Core.settings.getBool("bloom")) Draw.z((a > 0.99) ? (Layer.effect - 1) : (Layer.bullet - 2));
                    //now = this._ls[i];
                    //next = this._ls[i+1];
                    if(i == this._ls.length - 1 || this._ls[i + 1] == null) {
                        //I'm sorry. okay?
                        //Draw.alpha(a);
                        //var scl = (this._lsData[i][0]%2==0)?Vars.tilesize:Vars.tilesize*1.414;
                        //Lines.lineAngle(this._ls[i].worldx(), this._ls[i].worldy(), this._lsData[i][0]*45, this._lsData[i][2]*scl);
                        /*Draw.alpha(a*0.5);
                        Lines.lineAngle(this._ls[i].worldx(), this._ls[i].worldy(), this._lsData[i][2]*Vars.tilesize, this._lsData[i][0]*45, 4);
                        Draw.alpha(a*0.25);
                        Lines.lineAngle(this._ls[i].worldx(), this._ls[i].worldy(), this._lsData[i][2]*Vars.tilesize + 4, this._lsData[i][0]*45, 2);
                        Draw.alpha(a*0.125);
                        Lines.lineAngle(this._ls[i].worldx(), this._ls[i].worldy(), this._lsData[i][2]*Vars.tilesize + 6, this._lsData[i][0]*45, 2);*/
                        if(this._lsData[i][2] < 1) continue;
                        Lines.line(this._ls[i].worldx(), this._ls[i].worldy(), this._ls[i].worldx() + (this._lsData[i][2] - 1) * Vars.tilesize * lightblock.dirs[this._lsData[i][0]][0], this._ls[i].worldy() + (this._lsData[i][2] - 1) * Vars.tilesize * lightblock.dirs[this._lsData[i][0]][1]);
                        //Draw.alpha(a*0.5);
                        const shift = (this._lsData[i][0] % 2 == 0) ? w / 2 : w / 2.8285;
                        Drawf.tri(this._ls[i].worldx() + ((this._lsData[i][2] - 1) * Vars.tilesize + shift) * lightblock.dirs[this._lsData[i][0]][0], this._ls[i].worldy() + ((this._lsData[i][2] - 1) * Vars.tilesize + shift) * lightblock.dirs[this._lsData[i][0]][1], w, (this._lsData[i][0] % 2 == 0) ? 8 : 11.313, this._lsData[i][0] * 45);
                    }
                    else {
                        if(this._lsData[i + 1] == null) {
                            //obstructed
                            Lines.line(this._ls[i].worldx(), this._ls[i].worldy(), this._ls[i + 1].worldx() - 4 * lightblock.dirs[this._lsData[i][0]][0], this._ls[i + 1].worldy() - 4 * lightblock.dirs[this._lsData[i][0]][1]);
                        }
                        else {
                            //light go brrrrrrrr
                            Lines.line(this._ls[i].worldx(), this._ls[i].worldy(), this._ls[i + 1].worldx(), this._ls[i + 1].worldy());
                        }
                    }
                }
                Draw.blend();
                Draw.color();
            },
            clearCons() {
                for(var i = 0; i < this._lCons.length; i++) {
                    this._lCons[i].removeSource(this);
                }
            },
            lightMarchStart(length, maxLength) {
                //idk
                this._lightData[0] = this.getAngle();
                this._lightData[1] = 100;
                //TODO make it more efficient
                for(var i = 0; i < this._lCons.length; i++) {
                    this._lCons[i].removeSource(this);
                }
                this._ls = [];
                this._lsData = [];
                this._lCons = [];
                this._ls.push(this.tile);
                this._lsData.push([this.getAngle(), 100, this._lightData[2], this._lightData[3]]);
                this.pointMarch(this.tile, this.lightData(), length, maxLength, 0, this);
                this.setInit(true);
                //print(this._ls.toString());
                //print(this._lsData.toString());
            },
            pointMarch(tile, ld, length, maxLength, num, source) {
                if(length <= 0 || maxLength <= 0 || ld[1] * source.lightPower() < 1) return;
                const dir = lightblock.dirs[ld[0]];
                var next = null;
                var next2 = null;
                var furthest = null;
                var i = 0;
                var hit = Vars.world.raycast(tile.x, tile.y, tile.x + length * dir[0], tile.y + length * dir[1], (x, y) => {
                    furthest = Vars.world.tile(x, y);
                    if(furthest == tile || furthest == null) return false;
                    i++;
                    if(!furthest.solid() || (furthest.block() == lightblock && tile == source.tile)) return false;
                    if(furthest.build == null) return true;
                    if(furthest.build.block.lightReflector) {
                        //print("Light reflector!");
                        var tr = furthest.build.calcReflection(ld[0]);
                        if(tr >= 0) next = [tr, ld[1], ld[2] - i, ld[3]];
                    }
                    else if(furthest.build.block.lightDivisor) {
                        var tr = furthest.build.calcReflection(ld[0]);
                        if(tr >= 0) {
                            next = [ld[0], ld[1] / 2, ld[2] - i, ld[3]];
                            next2 = [tr, ld[1] / 2, ld[2] - i, ld[3]];
                        }
                    }
                    else if(furthest.build.block.lightRepeater) {
                        var tl = furthest.build.calcLight(ld, i);
                        if(tl == null) return true;
                        next = [tl[0], tl[1], tl[2], tl[3]];
                    }
                    else if(furthest.build.block.consumesLight) {
                        furthest.build.addSource([source, ld]);
                        this._lCons.push(furthest.build);
                    }
                    return true;
                });
                if(!hit) return;
                if(next == null || num > lightblock.maxReflections) {
                    //the block hit is solid or a consumer
                    this._ls.push(furthest);
                    this._lsData.push(null); //obstructor
                }
                else if(next2 == null) {
                    //the block hit reflecc
                    this._ls.push(furthest);
                    this._lsData.push(next); //mirror
                    this.pointMarch(furthest, next, ld[2] - i, maxLength - i, ++num, source);
                }
                else {
                    //the light go S P L I T
                    //TODO
                    this._ls.push(furthest);
                    this._lsData.push(next); //mirror
                    this.pointMarch(furthest, next, ld[2] - i, maxLength - i, ++num, source);
                    this._ls.push(null); //cheaty yep
                    this._lsData.push(null);
                    this._ls.push(furthest);
                    this._lsData.push(next2); //mirror mirror on the wall
                    this.pointMarch(furthest, next2, ld[2] - i, maxLength - i, ++num, source);
                }
            }
        });
        if(lightblock.angleConfig) {
            lightblock.rotate = false;
            lightblock.configurable = true;
            lightblock.saveConfig = true;
            lightblock.lastConfig = new Integer(0);
            lightblock.config(Integer, (build, value) => {
                build.addAngle(value);
            });
            objb = Object.assign(objb, {
                config() {
                    return new Integer(this._angle);
                },
                buildConfiguration(table) {
                    table.button(Icon.leftOpen, Styles.clearTransi, 34, () => {
                        this.configure(new Integer(1));
                    }).size(40);
                    table.button(Icon.rightOpen, Styles.clearTransi, 34, () => {
                        this.configure(new Integer(-1));
                    }).size(40);
                }
            });
        }
        //Extend Building
        lightblock.buildType = ent => {
            ent = extendContent(build, lightblock, clone(objb));
            ent._angle = 0;
            ent._lightInit = false;
            Events.run(Trigger.draw, () => {
                if(ent != null) ent.drawLightLasers();
            });
            return ent;
        };
        return lightblock;
    }
}
