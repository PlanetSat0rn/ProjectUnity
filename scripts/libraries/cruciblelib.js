
//imports
importPackage(Packages.arc.graphics.gl);

const graphLib = require("libraries/graphlib");
const heatlib = require("libraries/heatlib");
//credit to younggam setting example of how to build new overlay resrouce(heat) and deltanedas for example of block graph system via phase router.
print("youndcha test2")
//ui test, if sucessful will be moved to seperate js file
importPackage(Packages.arc.util.pooling);
importPackage(Packages.arc.scene);
const StackedBarChart ={
	_prefHeight:100,
	_barStats: prov(()=>
		{
			return [
				{name: "default", weight:1.6, filled: 0.5, color: Color.valueOf("6bc7ff")},
				{name: "default2", weight:1, filled: 0.8, color: Color.valueOf("ea7a55")}
			];
		}
	),
	draw(){
		let font = Fonts.outline;
        let lay = Pools.obtain(GlyphLayout,prov(()=>{return new GlyphLayout();}));
		
		let data = this._barStats.get();
		let totalweight = 0;
		for(let i=0;i<data.length;i++){
			totalweight += data[i].weight;
		}
		let ypos = this.y;
		for(let i=0;i<data.length;i++){
			let ah =  this.height*(data[i].weight/totalweight) ;
			let aw =  this.width*data[i].filled;
			let text = data[i].name;
			let dark = data[i].color.cpy().mul(0.5);
			Draw.color(dark);
			Fill.rect(this.x + this.width*0.5,ypos+ah*0.5,this.width,ah);
			Draw.color(data[i].color);
			Fill.rect(this.x + aw*0.5,ypos+ah*0.5,aw,ah);
			lay.setText(font, text);
			font.setColor(Color.white);
			font.draw(text, this.x + this.width / 2.0 - lay.width / 2.0, ypos + ah / 2.0 + lay.height / 2.0 + 1);
			
			ypos += ah;
		}

        Pools.free(lay);
		
		
	},
	setSize(x,y){
		this.super$setSize(x,y);
	},
	getPrefHeight() {
        return this._prefHeight;
    },
	getPrefWidth() {
        return 180.0;
    },
	setPrefHeight(s) {
        this._prefHeight=s;
    },
	setBarStatsProv(s) {
        this._barStats=s;
    }
	
};


function getStackedBarChart(pheight, datafunction){
	let pp=  extend(Element,Object.create(StackedBarChart));
	//pp.setName("abcd");
	pp.setPrefHeight(pheight);
	pp.setBarStatsProv(datafunction);
	return pp;
	
}



const IconBar ={
	_prefHeight:60,
	_barStats: prov(()=>
		{
			return {
				value: 2,
				defaultmax: 3,
				defaultmin: 0,
				color:Color.red,
				steps:[
					{value:1.6, icon: null},
					{value:1, icon: null}
				]
			};
		}
	),
	draw(){
		let font = Fonts.outline;
        let lay = Pools.obtain(GlyphLayout,prov(()=>{return new GlyphLayout();}));
		let xstart = 20;
		let data = this._barStats.get();
		let maxVal = Math.max(data.defaultmax,data.value);
		let minVal = Math.min(data.defaultmin,data.value);
		for(let i=0;i<data.steps.length;i++){
			maxVal = Math.max(maxVal,data.steps[i].value);
			minVal = Math.min(minVal,data.steps[i].value);
		}
		
		let dpos = Mathf.map(data.value,minVal,maxVal,xstart,this.width-xstart);
		let ah = this.height;
		Lines.stroke(3);
		Draw.color(Color.gray);
		Lines.rect(this.x+xstart, this.y+ah*0.5-8,this.width-xstart,16);  //thermometer outline
		
		Draw.color(data.color);
		Fill.rect(this.x + xstart, this.y+ah*0.5,26,26,45);  //thermometer head
		Fill.rect(this.x + xstart+dpos*0.5, this.y+ah*0.5,dpos,16);  //thermometer fill
		
		let stepsize = Mathf.pow(10,Math.floor(Mathf.log(10.0,2*(maxVal-minVal))));
		if(maxVal-minVal <= stepsize){
			stepsize = stepsize*0.5; //5x
		}
		if(maxVal-minVal <= stepsize){
			stepsize = stepsize*0.4; //2x
		}
		if(maxVal-minVal <= stepsize){
			stepsize = stepsize*0.5; //1x
		}
		
		Draw.color(Color.white);
		
		for(let i=0;i<data.steps.length;i++){  //icon indicators
			let dpos = Mathf.map(data.steps[i].value,minVal,maxVal,this.x+xstart,this.x+this.width);
			Lines.line(dpos,this.y+ah*0.5,dpos,this.y+ah*0.5 + 12 );
			Draw.rect(data.steps[i].icon,dpos,this.y+ah*0.75);
		}
		Draw.color(Color.lightGray);
		let stsrt = (Math.floor(minVal/stepsize)+1)*stepsize;
		for(let i=stsrt;i<maxVal;i+=stepsize){  //step indicators
			let dpos = Mathf.map(i,minVal,maxVal,this.x+xstart,this.x+this.width);
			Lines.line(dpos,this.y+ah*0.5,dpos,this.y+ah*0.5 - 12 );
			
			let text = Strings.fixed(i,0)+"'C";
			if(i>=1000){
				text = Strings.fixed(i/1000,1)+"K'C";
			}
			lay.setText(font, text);
			font.setColor(Color.white);
			font.draw(text, dpos - lay.width / 2.0, this.y + (ah*0.25) + lay.height / 2.0 + 1);
		}

        Pools.free(lay);
		
		
	},
	setSize(x,y){
		this.super$setSize(x,y);
	},
	getPrefHeight() {
        return this._prefHeight;
    },
	getPrefWidth() {
        return 180.0;
    },
	setPrefHeight(s) {
        this._prefHeight=s;
    },
	setBarStatsProv(s) {
        this._barStats=s;
    }
	
};

function getIconBar(pheight, datafunction){
	let pp=  extend(Element,Object.create(IconBar));
	pp.setPrefHeight(pheight);
	pp.setBarStatsProv(datafunction);
	return pp;
	
}




function deepCopy(obj) {
    var clone = {};
    for (var i in obj) {
        if (typeof(obj[i]) == "object" && obj[i] != null) clone[i] = deepCopy(obj[i]);
        else clone[i] = obj[i];
    }
    return clone;
}

//dunno how to load this from a json
const crucibleMelts = [
	{name:"copper", meltpoint:750, meltspeed:0.1, evaporation: 0.02, evaporationTemp: 2100, priority: 1}, // irl: 1475K, halved cus its a low tier resource.
	{name:"lead", meltpoint:570, meltspeed:0.2, evaporation: 0.02, evaporationTemp: 1900, priority: 1},  //dont let it get too hot! c:
	{name:"titanium", meltpoint:1600, meltspeed:0.07, priority: 1},  // irl: 1940K
	{name:"sand", meltpoint:1000, meltspeed:0.25, priority: 1},  // irl: 1900K
	{name:"carbon", meltpoint:4000, meltspeed:0.01, evaporation: 0.01, evaporationTemp: 600, notItem:true, priority: 0},  // practically cant be melted, but burns instead (to avoid clogging the thing)
	{name:"coal", additive:true, additiveID:"carbon", additiveWeight: 0.5, priority: 0},  
	{name:"graphite", additive:true, additiveID:"carbon", additiveWeight: 1.0, priority: 0},    
	{name:"unity-nickel", meltpoint:1100, meltspeed:0.15, priority: 1},  // irl: 1728K
	{name:"unity-cupronickel", meltpoint:850, meltspeed:0.05, priority: 2},  // irl: 1300K
	{name:"metaglass", meltpoint:950, meltspeed:0.05, priority: 2},  // irl: 1300K
	{name:"silicon", meltpoint:900, meltspeed:0.2, priority: 2},  // irl: 1600K
	{name:"surge-alloy", meltpoint:1500, meltspeed:0.05, priority: 3},  
	{name:"thorium", meltpoint:1650, meltspeed:0.03, priority: 1},  // irl: 1950K
	{name:"unity-super-alloy", meltpoint:1800, meltspeed:0.02, priority: 4},  
	////APPEND NEW MELTS TO THE END TO AVOID AFFECTING SAVES
];
for(let inde = 0 ;inde<crucibleMelts.length;inde++){
	crucibleMelts[inde].id = inde;
}
function getMeltByName(name){
	for(let inde = 0 ;inde<crucibleMelts.length;inde++){
		if(crucibleMelts[inde].name == name){
			return crucibleMelts[inde];
		}
	}
}
function getMelt(item){
	return getMeltByName(item.name);
}
function getMeltItem(id){
	let hh = crucibleMelts[id];
	if(!hh.item && !hh.notItem){hh.item=Vars.content.getByName(ContentType.item,hh.name);}
	return hh.item;
}


const crucibleRecipes = [
	{name:"unity-cupronickel", 
		inputs:[
			{material:"unity-nickel", amount:0.8},
			{material:"copper",       amount:2.0, needsliquid: true}
		],
		alloyspeed: 0.6
	},
	{name:"silicon", 
		inputs:[
			{material:"sand",   amount:1.25, needsliquid: true},
			{material:"carbon", amount:0.25}
		],
		alloyspeed: 0.25
	},
	{name:"metaglass", 
		inputs:[
			{material:"sand", amount:0.66, needsliquid: true},
			{material:"lead", amount:0.66, needsliquid: true}
		],
		alloyspeed: 0.5
	},
	{name:"surge-alloy", 
		inputs:[
			{material:"silicon",  amount:1,   needsliquid: true},
			{material:"lead",     amount:2,   needsliquid: true},
			{material:"copper",   amount:1,   needsliquid: true},
			{material:"titanium", amount:1.5, needsliquid: true},
		],
		alloyspeed: 0.25
	},
	{name:"unity-super-alloy", 
		inputs:[
			{material:"unity-cupronickel",  amount:1,   needsliquid: true},
			{material:"silicon",     amount:1,   needsliquid: true},
			{material:"thorium",   amount:1,   needsliquid: true},
			{material:"titanium", amount:1, needsliquid: true},
		],
		alloyspeed: 0.20
	},
]


const _dirs = [
	{x: 1,y: 0},
    {x: 0,y: 1},
    {x: -1,y: 0},
    {x: 0,y: -1}
]

function sqrd(x) {
    return x * x;
}



const _CrucibleCommon = Object.assign(Object.create(graphLib.graphCommon),{
	_baseLiquidCapacity:6.0, //capacity of crucible
	_meltSpeed:0.8, 
	_doesCrafting:true, //whether this block does alloying
	_capacityTiling:true, //whether the block's capacity is affect by tiling

	getBaseLiquidCapacity() {
        return this._baseLiquidCapacity;
    },
    setBaseLiquidCapacity(v) {
        this._baseLiquidCapacity = v;
    },
	getMeltSpeed() {
        return this._meltSpeed;
    },
    setMeltSpeed(v) {
        this._meltSpeed = v;
    },
	setDoesCrafting(v) {
        this._doesCrafting = v;
    },
	getDoesCrafting(v) {
        return this._doesCrafting;
    },
	setCapacityTiling(v) {
        this._capacityTiling = v;
    },
	getCapacityTiling(v) {
        return this._capacityTiling;
    },

    setStats(table) {
		table.row();
		table.left();
        table.add("Crucible system").color(Pal.accent).fillX();
		table.row();
		table.left();
        table.add("[lightgray]" + Core.bundle.get("stat.unity.liquidCapacity") + ":[] ").left();
		table.add((this.getBaseLiquidCapacity())+" Units");
		
		table.row();
		table.left();
        table.add("[lightgray]" + Core.bundle.get("stat.unity.meltSpeed") + ":[] ").left();
		table.add(Strings.fixed(this.getMeltSpeed()*100,0)+"%");
		
		this.setStatsExt(table);
    },
	setStatsExt(table) {},
    otherStats(table) {},
});
// gets a conneciton point on its index, runs anticlockwise around the block.
/*
e.g:

1x1:      2x2:       3x3:
 1         32        543    
2▣0       4▣▣1      6▣▣▣2
 3        5▣▣0      7▣▣▣1   
           67       8▣▣▣0
		             9AB
*/
function getConnectSidePos(index, size, rotation) {
    let side = Mathf.floor(index / size);
    side = (side + rotation) % 4;
    let normal = _dirs[(side + 3) % 4]
    let tangent = _dirs[(side + 1) % 4]
    let originx = 0;
    let originy = 0;

    if (size > 1) {
        originx += Mathf.floor(size / 2);
        originy += Mathf.floor(size / 2);
        originy -= (size - 1);
        if (side > 0) {
            for (let i = 1; i <= side; i++) {
                originx += _dirs[i].x * (size - 1);
                originy += _dirs[i].y * (size - 1);
            }
        }
        originx += tangent.x * (index % size);
        originy += tangent.y * (index % size);
    }
    return {
        fromPos: {
            x: originx,
            y: originy
        },
        toPos: {
            x: originx + _dirs[side].x,
            y: originy + _dirs[side].y
        },
        dir: side

    }
}


const _CruciblePropsCommon = Object.assign(deepCopy(graphLib.graphProps),{
	tilingIndex:0,
	liquidcap:0,
	melter:true,
	
	containedAmCache:0,
	containChanged:true,
	
	//the actual smelting products.
	contains:[],
	addItem(item){
		return this.getNetwork().addItem(item);
	},
	updateExtension() {
		//hm
		
	},
	getContained(){
		if(this.getNetwork()){
			return this.getNetwork().contains;
		}
		return this.contains;
	},
	getVolumeContained(){
		if(!this.getNetwork()){return 0;}
		return this.getNetwork().getVolumeContained();
	},
	canContainMore(amount){
		if(!this.getNetwork()){return false;}
		return this.getNetwork().canContainMore(amount);
	},
	getTotalLiquidCapacity(){
		if(!this.getNetwork()){return 1;}
		return this.getNetwork().getLiquidCapacity();
	},
	getMelt(item){
		return getMelt(item);
	},
	
	getLiquidCapacity(){
		if(!this.liquidcap){return 0;}
		return this.liquidcap;
	},
	setLiquidCapacity(s){
		this.liquidcap=s;
	},
	getTileIndex(){
		if(!this.tilingIndex){return 0;}
		return this.tilingIndex;
	},
	setTileIndex(s){
		this.tilingIndex=s;
	},
	
	getStackedBars(){
		
		let that = this;
		return getStackedBarChart(200, prov(()=>{
			let cc = that.getContained();
			let data = [];
			if(!cc ||!cc.length){
				data.push(
					{name: "Empty", weight:1.0, filled: 1.0, color: Color.valueOf("555555")}
				);
			}else{
				let tv = that.getVolumeContained();
				let min = Math.min(1.0/cc.length,0.15);
				let remain = 1.0-(cc.length*min);
				for(let i = 0;i<cc.length;i++){
					let ccl = cc[i];
					let ml = crucibleMelts[cc[i].id];
					let itm = Vars.content.getByName(ContentType.item,ml.name);
					if(itm){
						data.push(
							{name: itm.toString()+" - "+Strings.fixed(ccl.volume,2)+" units", weight:min+(remain*ccl.volume/tv), filled:ccl.meltedRatio, color: itm.color}
						);
					}else{
						data.push(
							{name: ml.name+" - "+Strings.fixed(ccl.volume,2)+" units", weight:min+(remain*ccl.volume/tv), filled:ccl.meltedRatio, color: Color.valueOf("555555")}
						);
					}
				}
			}
			return data;
			
		}));
	},
	
	getIconBar(){
		
		let that = this;
		return getIconBar(96, prov(()=>{
			let cc = that.getContained();
			let temp = 0;
			if(that.getNetwork()){
				temp = that.getNetwork().getAverageTemp();
			}
			let tempcol = heatlib.getTempColor(temp);
			tempcol.mul(tempcol.a);
			tempcol.add(Color.gray);
			tempcol.a = 1.0;
			let data = {
				value: (temp-273),
				defaultmax: 500,
				defaultmin: 0,
				color: tempcol,
				steps:[],
			};
			if(temp<270){
				data.defaultmin = Math.max(-273.15,5*(temp-273));
				data.defaultmax = Math.max(20,500+ 5*(temp-273));
			}
			if(!cc ||!cc.length){
			}else{
				for(let i = 0;i<cc.length;i++){
					let ccl = cc[i];
					let ml = crucibleMelts[cc[i].id];
					let itm = Vars.content.getByName(ContentType.item,ml.name);
					if(itm){
						data.steps.push(
							{value:ml.meltpoint-273, icon: itm.icon(Cicon.xlarge)}
						);
					}
				}
			}
			return data;
			
		}));
	},
	
	
    display(table) {
        if (!this._network) {
            return;
        }
		
        table.row();
        table.table(
            cons(sub => {
                sub.clearChildren();
				sub.left();
				sub.label(prov(() => {
                    return "Crucible temperature:";
                })).color(Color.lightGray).growX();
				sub.row();
				sub.add(this.getIconBar()).padTop(3).growX();
				sub.row();
				sub.label(prov(() => {
                    return "Crucible contents:";
                })).color(Color.lightGray).growX();
				sub.row();
				sub.left();
				sub.add(this.getStackedBars()).padTop(3).growX();
				
				
                //todo display liquids.
            })
        ).left().growX();
    },

    
    drawSelect() {
        if (this._network) {
            this._network.connected.each(cons(building => {
                Drawf.selected(building.getBuild().tileX(), building.getBuild().tileY(), building.getBuild().block, Pal.accent);
            }));
        }
    },
	
	updateProps(graph,index) {
		
	},
	initStats(){
		this.tilingIndex=0;
		this.liquidcap=0;
		let tmp = [];
		this.contains =tmp;
		this.melter=true;
		this.containedAmCache=0;
		let tmp2 = [];
		this._propsList=tmp2;
	},
	applySaveState(graph,cache) {
		if(graph.contains.length==cache.length){return;}
		let tmp = [];
		graph.contains = tmp;
		let cc = graph.contains;
		for(let i =0;i<cache.length;i++){
			cc.push(cache[i]);
			cc[i].item = getMeltItem(cc[i].id);
		}
	},
	writeGlobal(stream) {
	},
	writeLocal(stream,graph) {
		let cc = graph.contains;
		if(!cc || !cc.length){
			stream.i(0);
			return;
		}
		stream.i(cc.length);
		for(let i =0;i<cc.length;i++){
			stream.i(cc[i].id);
			stream.f(cc[i].meltedRatio);
			stream.f(cc[i].volume);
		}
	},
	readGlobal(stream, revision) {
	},
	readLocal(stream, revision) {
		let len = stream.i();
		let save = [];
		for(let i =0;i<len;i++){
			let id = stream.i();
			let mratio = stream.f();
			let vol = stream.f();
			save.push({
				id:id,
				volume:vol,
				meltedRatio:mratio,
			});
		}
		return save;
	},
	displayBars(barsTable) {
        let net = this.getNetwork();
		if(!net){return;}
		net.getLiquidCapacity();
		net.getVolumeContained();

        barsTable.add(new Bar(
            prov(() => Core.bundle.get("stat.unity.liquidTotal") + ": " + Strings.fixed(net.getVolumeContained(), 1) + "/" + Strings.fixed(net.getLiquidCapacity(), 1)),
            prov(() => Pal.darkishGray),
            floatp(() => net.getVolumeContained() / net.getLiquidCapacity() ))).growX();
        barsTable.row();
    },

});


const _CrucibleMulticonnectorProps = Object.assign(Object.create(_CruciblePropsCommon),deepCopy(graphLib.graphMultiProps), {
	
	
});



const tilechkdirs = [
	{x:-1,y: 1},{x:0,y: 1},{x:1,y: 1},
	{x:-1,y: 0},/*[tile]*/ {x:1,y: 0},
	{x:-1,y:-1},{x:0,y:-1},{x:1,y:-1},
]


const crucibleGraph = { //this just uh manages the graphics lmAO
	totalVolume:0,
	contains: [],
	containedAmCache:0,
	containChanged:true,
	crafts:true,
	totalCapacity:0,
	color:null,
	
	getLiquidColor(){
		if(!this.color){
			return Color.clear;
		}
		return this.color;
	},
	getLiquidCapacity(){
		if(!this.totalCapacity){return 0;}
		return this.totalCapacity;
	},
	
	getVolumeContained(){
		if(this.containChanged){
			let totalcontained = 0;
			for(let i = 0;i<this.contains.length;i++){
				totalcontained += this.contains[i].volume;
			}
			this.containedAmCache=totalcontained;
		}
		return this.containedAmCache;
	},
	addItem(item){
		let meltprod = getMelt(item);
		if(!meltprod){return false;}
		if(!meltprod.additive){
			if(!meltprod.item){meltprod.item=item;}
			return this.addMeltItem(meltprod,1,false);
		}else{
			return this.addMeltItem(getMeltByName(meltprod.additiveID),meltprod.additiveWeight,false);
		}
		
	},
	getMelt(meltprod){
		for(let i = 0;i<this.contains.length;i++){
			if(this.contains[i].id === meltprod.id){
				return this.contains[i];
			}
		}
		return null;
	},
	getMeltFromID(id){
		for(let i = 0;i<this.contains.length;i++){
			if(this.contains[i].id === id){
				return this.contains[i];
			}
		}
		return null;
	},
	addMeltItem(meltprod, am,liquid){
		if(!am){return;}
		if(!this.contains || !this.contains.push){ 
			let tmp =[];
			this.contains = tmp;
		}
		if(!meltprod.item && !meltprod.notItem){meltprod.item=Vars.content.getByName(ContentType.item,meltprod.name);}
		let avalslot = null;
		let totalcontained = 0;
		for(let i = 0;i<this.contains.length;i++){
			if(this.contains[i].id === meltprod.id){
				avalslot = this.contains[i];
			}
			totalcontained += this.contains[i].volume;
		}
		if(totalcontained+am>this.getLiquidCapacity()){
			return false;
		}
		if(avalslot){
			if(!liquid){
				this.addSolidToSlot(avalslot,am);
			}else{
				this.addLiquidToSlot(avalslot,am);
			}
		}else{
			this.contains.push({
				id:meltprod.id,
				volume:am,
				meltedRatio:liquid?1:0,
				item:meltprod.item
			});
		}
		this.containChanged=true;
		return true;
	},
	canContainMore(amount){
		return this.getVolumeContained()+amount <=this.getLiquidCapacity();
	},
	getRemainingSpace(amount){
		return Math.max(0,this.getLiquidCapacity()-this.getVolumeContained());
	},
	addSolidToSlot(slot, am){
		if(am==0){return;}
		let melted = slot.meltedRatio*slot.volume;
		slot.volume = slot.volume+am;
		slot.meltedRatio = melted/slot.volume;
		if(slot.volume==0 || isNaN(slot.meltedRatio)){
			slot.meltedRatio = 0;
		}
		this.containChanged=true;
	},
	addLiquidToSlot(slot, am){
		if(am==0){return;}
		let melted = (slot.meltedRatio*slot.volume)+am;
		slot.volume = slot.volume+am;
		slot.meltedRatio = melted/slot.volume;
		if(slot.volume==0 || isNaN(slot.meltedRatio)){
			slot.meltedRatio = 0;
		}
		this.containChanged=true;
	},
	
	addMergeStats(building){
		let port = building.getPortOfNetwork(this);
		this.totalCapacity +=building.liquidcap;
		let cc = building._propsList[port] ;
		if(!cc || cc.length==0){return;}
		
		for(let i = 0;i<cc.length;i++){
			this.addMeltItem(crucibleMelts[cc[i].id], cc[i].volume*(1.0-cc[i].meltedRatio), false);
			this.addMeltItem(crucibleMelts[cc[i].id], cc[i].volume*cc[i].meltedRatio, true);
		}
		
	},
	copyGraphStatsFrom(graph) {
	},
	updateDirect(){

	},
	updateOnGraphChanged() {
		this.totalCapacity = 0;
		let capacumm=0;
		
		let hasCrafter = false;
		this.connected.each(cons(building => {
			let bitmask = 0;
			if(!building.getBuild().tile){
				building.tilingIndex = 0;
				return;
			}
			let capacitymul=[0, 0.1, 0.2, 0.5, 1.0];
			let directneighbour = 0;
			for(let i =0;i<tilechkdirs.length;i++){
				
				let tile = building.getBuild().tile.nearby(tilechkdirs[i].x, tilechkdirs[i].y);
				if(!tile || (tile.block().getIsNetworkConnector === undefined) ){
					continue;
				}
				let conbuild = tile.build.getGraphConnector(this.name);
				if (!conbuild || conbuild.getDead() || !this.canConnect(building,conbuild)) {
					continue;
				}
				if(i==1||i==3||i==4||i==6){
					directneighbour++;
				}
				bitmask += 1<<i;
			}
			building.tilingIndex = bitmask;
			building.liquidcap = (building.getBuild().block.size==1?capacitymul[directneighbour]:1.0)*building.getBlockData().getBaseLiquidCapacity();
			capacumm+=building.liquidcap;
			hasCrafter = hasCrafter||building.getBlockData().getDoesCrafting();
        }));
		this.totalCapacity = capacumm;
		this.crafts=hasCrafter;
		if(this.getVolumeContained()>this.totalCapacity){
			let decratio = this.totalCapacity/this.getVolumeContained();
			for(let i = 0;i<this.contains.length;i++){
				this.contains[i].volume *= decratio;	
			}
			this.containChanged=true;
		}
	},
	getAverageTemp(){
		let speed = 0;
		let count = 0;
		this.connected.each(cons(building => {
			if(!building.getBlockData().getDoesCrafting()){return;}
			let temp = building.getBuild().getGraphConnector("heat graph").getTemp();
			speed+=temp;
			count++;
		}));
		if(count==0){return 0;}
		return speed/=count;
	},
	getAverageTempDecay(meltpoint,meltspeed,tmpdep,cooldep){
		let speed = 0;
		let count = 0;
		this.connected.each(cons(building => {
			if(!building.getBlockData().getDoesCrafting()){return;}
			let temp = building.getBuild().getGraphConnector("heat graph").getTemp();
			if(temp>meltpoint){
				speed+=(1+(temp/meltpoint)*tmpdep)*meltspeed;
			}else{
				speed-=((1.0-(temp/meltpoint))*cooldep)*meltspeed;
			}
			count++;
		}));
		if(count==0){return 0;}
		return speed/=count;
	},
	getAverageMeltSpeed(ml,tmpdep,cooldep){
		return this.getAverageTempDecay(ml.meltpoint,ml.meltspeed,tmpdep,cooldep);
	},
	getAverageMeltSpeedIndex(index,tmpdep,cooldep){
		return this.getAverageMeltSpeed(crucibleMelts[index],tmpdep,cooldep)
	},
	updateColor(){
		let col = {r:0,g:0,b:0};
		let tliquid = 0;
		for(let i = 0;i<this.contains.length;i++){
			let itmcol = Color.valueOf("555555");
			if(this.contains[i].meltedRatio>0){
				let liquidvol = this.contains[i].meltedRatio*this.contains[i].volume;
				tliquid +=liquidvol;
				if(this.contains[i].item){
					itmcol = this.contains[i].item.color;
				}
				col.r +=itmcol.r*liquidvol;
				col.g +=itmcol.g*liquidvol;
				col.b +=itmcol.b*liquidvol;
			}
		}
		let invt = 1.0/tliquid;
		this.color =new Color(col.r*invt, col.g*invt, col.b*invt, Mathf.clamp(2.0*tliquid/this.getLiquidCapacity()));
	},

	updateGraph() {
		if(!this.contains){ return;}
		if(!this.crafts){
			this.removeEmptyMelts();
			this.updateColor();
			return;
		}
		let capacitymul = Math.sqrt(this.getLiquidCapacity()/15.0);
		//melting 
		for(let i = 0;i<this.contains.length;i++){
			let meltmul = Time.delta/this.contains[i].volume;
			let ml = crucibleMelts[this.contains[i].id];
			if(ml){
				this.contains[i].meltedRatio += meltmul*this.getAverageMeltSpeed(ml,0.002,0.5)*0.4*capacitymul;
				this.contains[i].meltedRatio = Mathf.clamp(this.contains[i].meltedRatio);
				
				if(ml.evaporationTemp){
					let evap = this.getAverageTempDecay(ml.evaporationTemp,ml.evaporation,0.0,1.0);
					if(evap>0){
						this.contains[i].volume-=evap;
						this.containChanged=true;
					}
				}
			}
		}
		//alloying
		for(let z=0;z<crucibleRecipes.length;z++){
			let valid = true;
			let inputslots = [];
			let maxcraftable = 9999999;
			for(let r =0;r<crucibleRecipes[z].inputs.length;r++){
				let found =false;
				for(let i = 0;i<this.contains.length;i++){
					let ingre = this.contains[i];
					let alyinput = crucibleRecipes[z].inputs[r];
					if(crucibleMelts[ingre.id].name == alyinput.material  && //actually such a mess
						(!alyinput.needsliquid ||ingre.meltedRatio>0)){
						found=true;
						inputslots[r]=i;
						maxcraftable = Math.min(maxcraftable,  (alyinput.needsliquid? ingre.meltedRatio:1)*ingre.volume/alyinput.amount );
						break;
					}
				}
				if(!found){
					valid=false;
					break;
				}
			}
			if(valid && maxcraftable>0){
				let craftam = Math.min(maxcraftable,crucibleRecipes[z].alloyspeed*Time.delta*0.2*capacitymul);
				if(craftam<=0){continue;}
				for(let r =0;r<crucibleRecipes[z].inputs.length;r++){
					let alyinput = crucibleRecipes[z].inputs[r];
					if(alyinput.needsliquid){
						this.addLiquidToSlot(this.contains[inputslots[r]],-alyinput.amount*craftam);
					}else{
						this.contains[inputslots[r]].volume-=alyinput.amount*craftam;
						this.containChanged=true;
					}
				}
				this.addMeltItem(getMeltByName(crucibleRecipes[z].name),craftam,true);
			}
		}
		//removing empty if any
		this.removeEmptyMelts();
		//updating color
		this.updateColor();
	},
	
	removeEmptyMelts(){
		for(let i = 0;i<this.contains.length;i++){
			if(this.contains[i].volume<=0){
				this.contains.splice(i, 1);
				i--;
			}
		}
	},
	
	killGraph(){
		let cc = this.contains;
		let graph = this;
		this.connected.each(cons(building => {
			let nc = [];
			let ratio = building.liquidcap/graph.totalCapacity;
			for(let i = 0;i<cc.length;i++){
				nc.push({
					id:cc[i].id,
					volume:cc[i].volume*ratio,
					meltedRatio:cc[i].meltedRatio,
					item:cc[i].item
				});
			}
			building._propsList[building.getPortOfNetwork(graph)] = nc;
		}));
		this.connected.clear();
	},
	mergeStats(graph){
		let cc = graph.contains;
		this.totalCapacity +=graph.totalCapacity;
		for(let i = 0;i<cc.length;i++){
			this.addMeltItem(crucibleMelts[cc[i].id], cc[i].volume*(1.0-cc[i].meltedRatio), false);
			this.addMeltItem(crucibleMelts[cc[i].id], cc[i].volume*cc[i].meltedRatio, true);
		}
	},
	
};



const _baseTypes = {
    crucibleConnector: {
        block: _CrucibleCommon,
        build: _CruciblePropsCommon,
		graph: crucibleGraph,
    },
	crucibleMultiConnector: {
        block: _CrucibleCommon,
        build: _CrucibleMulticonnectorProps,
		graph: crucibleGraph,
    },
}



for(let key in _baseTypes){
	graphLib.setGraphName(_baseTypes[key],"crucible graph");
}


const tileMap = [//not sure how to format this.
	39,39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,39,
	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,3,
	3,15,15,3,3,15,15,5,5,29,31,5,5,29,31,4,
	4,40,40,4,4,20,20,28,28,10,11,28,28,23,32,3,
	3,15,15,3,3,15,15,2,2,9,14,2,2,9,14,4,
	4,40,40,4,4,20,20,30,30,47,44,30,30,22,6,39,
	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,39,
	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,3,
	3,15,15,3,3,15,15,5,5,29,31,5,5,29,31,0,
	0,42,42,0,0,12,12,8,8,35,34,8,8,33,7,3,
	3,15,15,3,3,15,15,2,2,9,14,2,2,9,14,0,
	0,42,42,0,0,12,12,1,1,45,18,1,1,19,13
]


function _getRegion(region, tile) {
    if (!region) {
        print("oh no there is no texture");
        return;
    }
    let nregion = new TextureRegion(region);
    let tilew = (nregion.u2 - nregion.u)/12.0;
	let tileh = (nregion.v2 - nregion.v)/4.0;
	let tilex = (tile%12)/12.0;
	let tiley = Math.floor(tile/12)/4.0;
	
	nregion.u = Mathf.map(tilex,0,1,nregion.u,nregion.u2)+tilew*0.02;
	nregion.v = Mathf.map(tiley,0,1,nregion.v,nregion.v2)+tileh*0.02; //y is flipped h 
	nregion.u2 = nregion.u+tilew*0.96;
	nregion.v2 = nregion.v+tileh*0.96;
	nregion.width = 32;
	nregion.height = 32;
    return nregion;
}

function _drawTile(region, x, y, w, h, rot, tile) {
    Draw.rect(_getRegion(region, tile) , x, y, w, h, w * 0.5, h * 0.5, rot);
}



module.exports = {
	drawTile: _drawTile,
	getRegion:_getRegion,
	tileIndexMap: tileMap,
    crucibleGraph: crucibleGraph,
    heatProps: _CruciblePropsCommon,
    heatCommon: _CrucibleCommon,
    dirs: _dirs,
    getConnectSidePos: getConnectSidePos,
    baseTypes: _baseTypes,
	meltTypes: crucibleMelts
}