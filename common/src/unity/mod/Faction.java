package unity.mod;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import unity.graphics.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
public enum Faction{
    monolith("monolith", Palettes.monolith);

    public static final Faction[] all = values();

    public final String name;
    public String localizedName;

    public final Color color;
    public TextureRegion icon;

    public static void init(){
        if(headless) return;
        for(Faction faction : all) faction.localizedName = Core.bundle.format("faction." + faction.name, faction.color);
    }

    public static void load(){
        if(headless) return;
        for(Faction faction : all) faction.icon = Core.atlas.find("unity-faction-" + faction.name + "-icon");
    }

    Faction(String name, Color color){
        this.name = name;
        this.color = color.cpy();
    }

    @Override
    public String toString(){
        return localizedName;
    }
}