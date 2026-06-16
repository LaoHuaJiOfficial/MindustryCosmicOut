package mod.extend.starmap;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.type.Planet;

public class StarMapPlanets {
    public static final Seq<StarMapPlanetData> all = new Seq<>();
    private static final ObjectMap<Planet, StarMapPlanetData> byPlanet = new ObjectMap<>();

    public static void clear() {
        all.clear();
        byPlanet.clear();
    }

    public static StarMapPlanetData register(Planet planet, int axis0, int axis120) {
        return register(planet, axis0, axis120, 1.5f);
    }

    public static StarMapPlanetData register(Planet planet, int axis0, int axis120, float starmapSize) {
        StarMapPlanetData data = new StarMapPlanetData(planet, axis0, axis120, starmapSize);
        all.add(data);
        byPlanet.put(planet, data);
        return data;
    }

    public static @Nullable StarMapPlanetData get(Planet planet) {
        if (planet == null) return null;
        return byPlanet.get(planet);
    }

    public static boolean has(Planet planet) {
        return planet != null && byPlanet.containsKey(planet);
    }

    public static boolean isStarmapPlanet(Planet planet) {
        return has(planet) && planet.parent != null;
    }
}
