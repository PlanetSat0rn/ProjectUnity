const Integer = java.lang.Integer;

const loaderBlock = extendContent(Block, "loader-block", {
	/*load(){
		this.region = Core.atlas.white();
	},*/

	init(){
		Core.app.post(run(() => {
			// Air Factory
			try{
				const airFac = Blocks.airFactory;
				
				airFac.consumes.remove(ConsumeType.item);

				const caelifera = new UnitFactory.UnitPlan(
					Vars.content.getByName(ContentType.unit, "unity-caelifera"),
					60 * 25,
					ItemStack.with(
						Items.silicon, 15,
						Items.titanium, 25
					)
				);

				var newPlan = [];
				for(var i = 0; i < airFac.plans.length; i++){
					newPlan.push(airFac.plans[i]);
				};
				newPlan.push(caelifera);
				airFac.plans = newPlan;

				airFac.config(Integer, (tile, i) => {
					tile.currentPlan = (Math.floor(i) < 0 || Math.floor(i) >= airFac.plans.length) ? -1 : Math.floor(i);
					tile.progress = 0;
				});

				for(var i = 0; i < airFac.plans.length; i++){
					for(var j = 0; j < airFac.plans[i].requirements.length; j++){
						var stack = airFac.plans[i].requirements[j];
						airFac.capacities[stack.item.id] = Math.max(airFac.capacities[stack.item.id], stack.amount * 2);
						airFac.itemCapacity = Math.max(airFac.itemCapacity, stack.amount * 2);
					}
				};

				airFac.consumes.add(extendContent(ConsumeItemDynamic, func(e => {
					return e.currentPlan != -1 ? (airFac.plans[e.currentPlan]).requirements : ItemStack.empty
				}), {}));
			}catch(e){
				print(e);
			};

			// Naval Factory
			try{
				const NavalFac = Blocks.navalFactory;

				NavalFac.consumes.remove(ConsumeType.item);

				const amphibi = new UnitFactory.UnitPlan(
					Vars.content.getByName(ContentType.unit, "unity-amphibi-naval"),
					60 * 25,
					ItemStack.with(
						Items.silicon, 15,
						Items.metaglass, 30,
						Items.titanium, 25
					)
				);

				var newPlan = [];
				for(var i = 0; i < NavalFac.plans.length; i++){
					newPlan.push(NavalFac.plans[i]);
				};
				newPlan.push(amphibi);
				NavalFac.plans = newPlan;

				NavalFac.config(Integer, (tile, i) => {
					tile.currentPlan = (Math.floor(i) < 0 || Math.floor(i) >= NavalFac.plans.length) ? -1 : Math.floor(i);
					tile.progress = 0;
				});

				for(var i = 0; i < NavalFac.plans.length; i++){
					for(var j = 0; j < NavalFac.plans[i].requirements.length; j++){
						var stack = NavalFac.plans[i].requirements[j];
						NavalFac.capacities[stack.item.id] = Math.max(NavalFac.capacities[stack.item.id], stack.amount * 2);
						NavalFac.itemCapacity = Math.max(NavalFac.itemCapacity, stack.amount * 2);
					}
				};

				NavalFac.consumes.add(extendContent(ConsumeItemDynamic, func(e => {
					return e.currentPlan != -1 ? (NavalFac.plans[e.currentPlan]).requirements : ItemStack.empty
				}), {}));
			}catch(e){
				print(e);
			};

			// Additive Reconstructor
			try{
				const addReconstructor = Blocks.additiveReconstructor;

				var newUpgrades = [];
				for(var i = 0; i < addReconstructor.upgrades.length; i++){
					newUpgrades.push(addReconstructor.upgrades[i]);
				}
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-caelifera"),
						Vars.content.getByName(ContentType.unit, "unity-schistocerca"),
					]
				);
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-amphibi-naval"),
						Vars.content.getByName(ContentType.unit, "unity-craber-naval")
					]
				);
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-electron"),
						Vars.content.getByName(ContentType.unit, "unity-neutron")
					]
				);
				addReconstructor.upgrades = newUpgrades;
			}catch(e){
				print(e);
			};

			// Multiplicative Reconstructor
			try{
				const mulReconstructor = Blocks.multiplicativeReconstructor;

				var newUpgrades = [];
				for(var i = 0; i < mulReconstructor.upgrades.length; i++){
					newUpgrades.push(mulReconstructor.upgrades[i]);
				}
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-schistocerca"),
						Vars.content.getByName(ContentType.unit, "unity-anthophila")
					]
				);
				mulReconstructor.upgrades = newUpgrades;
			}catch(e){
				print(e);
			};

			// Exponential Reconstructor
			try{
				const expReconstructor = Blocks.exponentialReconstructor;

				var newUpgrades = [];
				for(var i = 0; i < expReconstructor.upgrades.length; i++){
					newUpgrades.push(expReconstructor.upgrades[i]);
				}
				newUpgrades.push(
					[
						UnitTypes.bryde,
						Vars.content.getByName(ContentType.unit, "unity-rexed")
					]
				);
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-anthophila"),
						Vars.content.getByName(ContentType.unit, "unity-vespula"),
					]
				);
				expReconstructor.upgrades = newUpgrades;
			}catch(e){
				print(e);
			};

			// Tetrative Reconstructor
			try{
				const tetraReconstructor = Blocks.tetrativeReconstructor;

				var newUpgrades = [];
				for(var i = 0; i < tetraReconstructor.upgrades.length; i++){
					newUpgrades.push(tetraReconstructor.upgrades[i]);
				}
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-rexed"),
						Vars.content.getByName(ContentType.unit, "unity-storm")
					]
				);
				newUpgrades.push(
					[
						UnitTypes.arkyid,
						Vars.content.getByName(ContentType.unit, "unity-gummy")
					]
				);
				newUpgrades.push(
					[
						Vars.content.getByName(ContentType.unit, "unity-vespula"),
						Vars.content.getByName(ContentType.unit, "unity-lepidoptera")
					]
				);
				tetraReconstructor.upgrades = newUpgrades;
			}catch(e){
				print(e);
			};
		}));
	},

	isHidden(){
		return true;
	}
});

this.global.loader = {};
