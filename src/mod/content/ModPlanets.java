package mod.content;

import mindustry.content.Planets;
import mod.extend.starmap.StarMapPlanets;

public class ModPlanets {

    public static void load() {
        StarMapPlanets.clear();

        StarMapPlanets.register(Planets.sun, 0, 0);
        StarMapPlanets.register(Planets.erekir, 2, 0);
        StarMapPlanets.register(Planets.gier, 2, 3, 16f);
        StarMapPlanets.register(Planets.notva, -1, 2, 16f);
        StarMapPlanets.register(Planets.tantros, 3, 2);
        StarMapPlanets.register(Planets.serpulo, -4, 1);
    }
}