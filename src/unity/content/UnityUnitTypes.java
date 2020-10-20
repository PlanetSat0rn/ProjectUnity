package unity.content;

import arc.func.*;
import arc.math.Mathf;
import arc.graphics.Color;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.gen.*;
import mindustry.entities.bullet.*;
import mindustry.graphics.*;
import mindustry.content.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;
import unity.units.*;

import static mindustry.type.ItemStack.*;

public class UnityUnitTypes implements ContentList{
	private static Prov<? extends Unit>[] constructors = new Prov[]{CopterUnit::new, WormSegmentUnit::new, WormDefaultUnit::new};
	private static final int[] classIDs = new int[constructors.length];
	
	public static UnitType
	//flying units
	anthophila, caelifera, lepidoptera, schistocerca, vespula,
	
	//worm units
	arcnelidia, devourer;

	public static int getClassId(int index){
		return classIDs[index];
	}

	@Override
	public void load(){
		//region air units
		
		for(int i = 0, j = 0, len = EntityMapping.idMap.length; i < len; i++){
			if(EntityMapping.idMap[i] == null){
				classIDs[j] = i;
				EntityMapping.idMap[i] = constructors[j++];
				if(j >= constructors.length) break;
			}
		}
		
		EntityMapping.nameMap.put("anthophila", CopterUnit::new);
		anthophila = new CopterUnitType("anthophila", 3){{
			speed = 4f;
			drag = 0.07f;
			accel = 0.03f;
			fallSpeed = 0.005f;
			health = 450;
			engineSize = 0f;
			flying = true;
			hitSize = 15f;
			range = 165f;
			weapons.add(new Weapon(name + "-gun"){{
				x = 4.25f;
				y = 14f;
				reload = 15;
				shootSound = Sounds.shootBig;
				bullet = Bullets.standardThoriumBig;
			}});
			
			weapons.add(new Weapon(name + "-tesla"){{
				x = 7.75f;
				y = 8.25f;
				reload = 30f;
				shots = 5;
				shootSound = Sounds.spark;
				bullet = new LightningBulletType(){{
					damage = 15f;
				}};
			}});
			fallRotateSpeed = 2f;
			addRotor(0f, -13f, 0.6f, 4, 29, 0);
			for(int i = 0; i < 2; i++) addRotor(13f * Mathf.signs[i], 3f, 1f, 3, 29 * Mathf.signs[i], i * 180);
		}};
		
		EntityMapping.nameMap.put("caelifera", CopterUnit::new);
		caelifera = new CopterUnitType("caelifera", 1){{
			speed = 5f;
			drag = 0.08f;
			accel = 0.04f;
			fallSpeed = 0.005f;
			health = 75;
			engineSize = 0f;
			flying = true;
			hitSize = 12f;
			range = 140f;
			weapons.add(new Weapon(name + "-gun"){{
				reload = 6f;
				x = 5.25f;
				y = 6.5f;
				shootSound = Sounds.pew;
				ejectEffect = Fx.shellEjectSmall;
				bullet = new BasicBulletType(5f, 7f){{
					lifetime = 30f;
					shrinkY = 0.2f;
				}};
			}});
			
			weapons.add(new Weapon(name + "-launcher"){{
				reload = 30f;
				x = 4.5f;
				y = 0.5f;
				shootSound = Sounds.shootSnap;
				ejectEffect = Fx.shellEjectMedium;
				bullet = new MissileBulletType(3f, 1f){{
					speed = 3f;
					lifetime = 50f;
					splashDamage = 40f;
					splashDamageRadius = 6f;
					drag = -0.01f;
				}};
			}});
			addRotor(0f, 6f, 1f, 4, 29, 0);
		}};
		
		EntityMapping.nameMap.put("lepidoptera", CopterUnit::new);
		lepidoptera = new CopterUnitType("lepidoptera", 8){{
			speed = 3f;
			drag = 0.07f;
			accel = 0.03f;
			fallSpeed = 0.003f;
			health = 9500;
			engineSize = 0f;
			flying = true;
			hitSize = 45f;
			range = 300f;
			lowAltitude = true;
			weapons.add(new Weapon(name + "-gun"){{
				x = 14f;
				y = 27f;
				shootSound = Sounds.shootBig;
				ejectEffect = Fx.shellEjectBig;
				reload = 10f;
				bullet = Bullets.standardThoriumBig;
			}});
			
			weapons.add(new Weapon(name + "-launcher"){{
				x = 17f;
				y = 14f;
				shootSound = Sounds.shootSnap;
				ejectEffect = Fx.shellEjectMedium;
				shots = 2;
				spacing = 2f;
				reload = 20f;
				bullet = new MissileBulletType(6f, 1f){{
					width = 8f;
					height = 14f;
					trailColor = Color.valueOf("e58956");
					weaveScale = 2f;
					weaveMag = -2f;
					lifetime = 50f;
					drag = -0.01f;
					splashDamage = 48f;
					splashDamageRadius = 12f;
					frontColor = Color.valueOf("ffd2ae");
					backColor = Color.valueOf("e58956");
				}};
			}});
			
			weapons.add(new Weapon(name + "-gun-big"){{
				rotate = true;
				rotateSpeed = 3f;
				x = 8f;
				y = 3f;
				shootSound = Sounds.shotgun;
				ejectEffect = Fx.none;
				shots = 3;
				spacing = 15f;
				shotDelay = 0f;
				reload = 45f;
				bullet = new ShrapnelBulletType(){{
					toColor = Color.valueOf("ffd2ae");
					damage = 150f;
					keepVelocity = false;
					length = 150f;
				}};
			}});
			fallRotateSpeed = 0.8f;
			for(int i = 0; i < 2; i++){
				for(int j = 0; j < 2; j++){
					addRotor(Mathf.signs[i] * 22.5f, 21.25f, 1f, 3, 19 * Mathf.signs[i] * Mathf.signs[j], 0);
					addRotor(Mathf.signs[i] * 17.25f, 1f, 0.8f, 2, 23 * Mathf.signs[i] * Mathf.signs[j], 0);
				}
			}
		}};
		
		EntityMapping.nameMap.put("schistocerca", CopterUnit::new);
		schistocerca = new CopterUnitType("schistocerca", 2){{
			speed = 4.5f;
			drag = 0.07f;
			accel = 0.03f;
			fallSpeed = 0.005f;
			health = 150;
			engineSize = 0f;
			flying = true;
			hitSize = 13f;
			range = 165f;
			weapons.add(new Weapon(name + "-gun"){{
				x = 1.5f;
				y = 11f;
				shootSound = Sounds.pew;
				ejectEffect = Fx.shellEjectSmall;
				reload = 8f;
				bullet = new BasicBulletType(4f, 5f){{
					lifetime = 36;
					shrinkY = 0.2f;
				}};
			}});
			
			weapons.add(new Weapon(name + "-gun"){{
				x = 4f;
				y = 8.75f;
				shootSound = Sounds.shootSnap;
				ejectEffect = Fx.shellEjectSmall;
				reload = 12f;
				bullet = new BasicBulletType(4f, 8f){{
					width = 7f;
					height = 9f;
					lifetime = 36f;
					shrinkY = 0.2f;
				}};
			}});
			
			weapons.add(new Weapon(name + "-gun-big"){{
				x = 6.75f;
				y = 5.75f;
				shootSound = Sounds.shootBig;
				ejectEffect = Fx.shellEjectMedium;
				reload = 30f;
				bullet = Bullets.standardIncendiaryBig;
			}});
			for(int i = 00; i < 2; i++) addRotor(0f, 6.5f, 1f, 3, 29 * Mathf.signs[i], 0);
		}};
		
		EntityMapping.nameMap.put("vespula", CopterUnit::new);
		vespula = new CopterUnitType("vespula", 4){{
			speed = 3.5f;
			drag = 0.07f;
			accel = 0.03f;
			fallSpeed = 0.003f;
			health = 4000;
			engineSize = 0f;
			flying = true;
			hitSize = 30f;
			range = 165f;
			lowAltitude = true;
			weapons.add(new Weapon(name + "-gun-big"){{
				x = 8.25f;
				y = 9.5f;
				reload = 12f;
				shootSound = Sounds.shootBig;
				bullet = Bullets.standardDenseBig;
			}});
				
			weapons.add(new Weapon(name + "-gun"){{
				x = 6.5f;
				y = 21.5f;
				reload = 20f;
				shots = 4;
				shotDelay = 2f;
				shootSound = Sounds.shootSnap;
				bullet = Bullets.standardThorium;
			}});
				
			weapons.add(new Weapon(name + "-laser-gun"){{
				x = 13.5f;
				y = 15.5f;
				reload = 60f;
				shootSound = Sounds.laser;
				bullet = new LaserBulletType(240f){{
					sideAngle = 45f;
					length = 200f;
				}};
			}});
			
			for(int i = 0, index = 0; i < 2; i++){
				for(int j = 0; j < 2; j++) addRotor(15f * Mathf.signs[i], 6.75f, 1f, 4, 29 * Mathf.signs[i] * Mathf.signs[j], j * 180);
			}
		}};
		((UnitFactory) Blocks.airFactory).plans.add(new UnitPlan(caelifera, 60f * 25, with(Items.silicon, 15, Items.titanium, 25)));
		
		//endregion
		//region worm units
		
		EntityMapping.nameMap.put("arcnelidia", WormDefaultUnit::new);
		arcnelidia = new WormUnitType("arcnelidia"){{
			setTypeID(3);
			segmentOffset = 23f;
			hitSize = 17f;
			health = 800;
			speed = 4f;
			accel = 0.035f;
			drag = 0.007f;
			rotateSpeed = 3.2f;
			engineSize = -1f;
			faceTarget = false;
			armor = 5f;
			flying = true;
			visualElevation = 0.8f;
			range = 210f;
			LightningBulletType archnelidiaBolt = new LightningBulletType(){{
				damage = 23f;
				lightningColor = Pal.surge;
				lightningLength = 24;
				lightningLengthRand = 3;
			}};
			weapons.add(new Weapon(){{
				reload = 90f;
				rotateSpeed = 50f;
				mirror = true;
				rotate = true;
				ignoreRotation = true;
				minShootVelocity = 2.1f;
				bullet = archnelidiaBolt;
			}});
			segWeapSeq.add(new Weapon(){{
				x = 0f;
				shots = 4;
				reload = 70f;
				rotateSpeed = 50f;
				mirror = false;
				ignoreRotation = true;
				bullet = archnelidiaBolt;
			}});
		}};

		/*
		EntityMapping.nameMap.put("devourer", WormDefaultUnit::new);
		devourer=new WormUnitType("devourer", 45) {{
			
		}};
		*/

		//reconstructors
		((Reconstructor) Blocks.additiveReconstructor).upgrades.add(new UnitType[]{caelifera, schistocerca});
		((Reconstructor) Blocks.multiplicativeReconstructor).upgrades.add(new UnitType[]{schistocerca, anthophila});
		((Reconstructor) Blocks.exponentialReconstructor).upgrades.add(new UnitType[]{anthophila, vespula});
		((Reconstructor) Blocks.tetrativeReconstructor).upgrades.add(new UnitType[]{vespula, lepidoptera});
	}
}