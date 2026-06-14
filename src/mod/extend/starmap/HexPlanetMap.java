package mod.extend.starmap;

import arc.struct.StringMap;
import arc.util.Nullable;
import mindustry.type.Planet;
import mod.extend.type.ModPlanet;

import static mindustry.Vars.content;

public class HexPlanetMap {
    public final StringMap map = new StringMap();

    public void rebuild(){
        map.clear();
        for(Planet planet : content.planets()){
            if(planet instanceof ModPlanet modPlanet){
                HexCoord hex = planetCoord(modPlanet);
                map.put(HexCoord.key(hex), planet.name);
            }
        }
    }

    public HexCoord planetCoord(ModPlanet planet){
        return HexCoord.from0And120(planet.coord1, planet.coord3);
    }

    public @Nullable Planet planetAt(@Nullable HexCoord hex){
        if(hex == null) return null;
        String name = map.get(HexCoord.key(hex));
        return name == null ? null : content.planet(name);
    }
}
