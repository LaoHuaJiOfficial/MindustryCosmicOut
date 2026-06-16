package mod.extend.starmap;

import arc.struct.StringMap;
import arc.util.Nullable;
import mindustry.type.Planet;

import static mindustry.Vars.content;

public class HexPlanetMap {
    public final StringMap map = new StringMap();

    public void rebuild() {
        map.clear();
        for (StarMapPlanetData data : StarMapPlanets.all) {
            HexCoord hex = data.hexCoord();
            map.put(HexCoord.key(hex), data.planet.name);
        }
    }

    public HexCoord planetCoord(StarMapPlanetData data) {
        return data.hexCoord();
    }

    public @Nullable HexCoord planetCoord(Planet planet) {
        StarMapPlanetData data = StarMapPlanets.get(planet);
        return data == null ? null : data.hexCoord();
    }

    public @Nullable Planet planetAt(@Nullable HexCoord hex) {
        if (hex == null) return null;
        String name = map.get(HexCoord.key(hex));
        return name == null ? null : content.planet(name);
    }
}
