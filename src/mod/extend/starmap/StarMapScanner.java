package mod.extend.starmap;

import arc.files.Fi;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.type.Planet;

public class StarMapScanner {
    public static final String fileName = "starmap.hjson";

    public static void loadAll() {
        StarMapPlanets.clear();

        Vars.mods.eachEnabled(StarMapScanner::loadMod);

        Log.info("[CosmicOut] Loaded @ starmap entries from enabled mods.", StarMapPlanets.all.size);
    }

    public static void loadMod(Mods.LoadedMod mod) {
        Fi file = mod.root.child(fileName);
        if (!file.exists()) return;

        try {
            registerFrom(mod, file);
        } catch (Exception e) {
            Log.err("[CosmicOut] Failed to load @ from mod '@'", fileName, mod.name, e);
        }
    }

    public static void registerFrom(Mods.LoadedMod mod, Fi file) {
        Jval json = Jval.read(file.readString());
        Jval planets = json.get("planets");
        if (planets == null || !planets.isArray()) {
            Log.warn("[CosmicOut] @ in mod '@' has no planets array.", fileName, mod.name);
            return;
        }

        for (Jval entry : planets.asArray()) {
            registerEntry(mod, entry);
        }
    }

    public static void registerEntry(Mods.LoadedMod mod, Jval entry) {
        String name = entry.getString("name", null);
        if (name == null) {
            Log.warn("[CosmicOut] Skipping starmap entry without name in mod '@'.", mod.name);
            return;
        }

        Planet planet = Vars.content.planet(name);
        if (planet == null) {
            Log.warn("[CosmicOut] Unknown planet '@' in mod '@'.", name, mod.name);
            return;
        }

        int axis0 = entry.getInt("axis0", 0);
        int axis120 = entry.getInt("axis120", 0);
        float size = entry.getFloat("size", 1.5f);

        StarMapPlanets.register(planet, axis0, axis120, size);
    }
}
