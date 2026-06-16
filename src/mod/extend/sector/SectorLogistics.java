package mod.extend.sector;

import arc.Core;
import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.*;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;

import static mindustry.Vars.*;

public class SectorLogistics {
    private static final ObjectMap<Sector, SectorLogisticsData> cache = new ObjectMap<>();

    public static void init() {
        Events.on(ResetEvent.class, e -> cache.clear());

        Events.on(SaveWriteEvent.class, e -> {
            flushAllStats();
            saveAll();
        });

        Events.on(SectorLoseEvent.class, e -> unload(e.sector));

        Events.on(TurnEvent.class, e -> {
            if (net.client()) return;
            for (Planet planet : content.planets()) {
                refreshImportRates(planet);
            }
        });

        Events.run(Trigger.update, () -> {
            if (!state.isCampaign() || state.rules.sector == null || net.client()) return;
            get(state.rules.sector).update(state.rules.sector);
        });
    }

    public static SectorLogisticsData get(Sector sector) {
        if (sector == null) return new SectorLogisticsData();
        return cache.get(sector, () -> load(sector));
    }

    public static void refreshImportRates(Planet planet, Sector sector) {
        if (planet == null || sector == null) return;
        SectorLogisticsData data = get(sector);
        data.refreshLiquidImportRates(planet, sector);
        data.refreshPayloadImportRates(planet, sector);
    }

    public static void refreshImportRates(Planet planet) {
        if (planet == null) return;
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) refreshImportRates(planet, sector);
        }
    }

    public static void handleLiquidExport(Sector sector, Liquid liquid, float amount) {
        get(sector).handleLiquidExport(liquid, amount);
    }

    public static void handleLiquidImport(Sector sector, Liquid liquid, float amount) {
        get(sector).handleLiquidImport(liquid, amount);
    }

    public static void handlePayloadExport(Sector sector, UnlockableContent content, int amount) {
        get(sector).handlePayloadExport(content, amount);
    }

    public static void handlePayloadImport(Sector sector, UnlockableContent content, int amount) {
        get(sector).handlePayloadImport(content, amount);
    }

    public static void save(Sector sector) {
        if (sector == null) return;
        Core.settings.putJson(settingsKey(sector), get(sector));
    }

    public static void saveAll() {
        cache.each((sector, data) -> save(sector));
    }

    public static void flushAllStats() {
        cache.each((sector, data) -> data.flushAllStats());
    }

    public static void flushStats(Sector sector) {
        if (sector == null) return;
        get(sector).flushAllStats();
    }

    public static void unload(Sector sector) {
        if (sector == null) return;
        save(sector);
        cache.remove(sector);
    }

    public static void setDestination(Sector from, @Nullable Sector dest) {
        if (from == null || !state.isCampaign()) return;

        Sector prev = from.info.destination;
        from.info.destination = dest;
        from.saveInfo();
        flushStats(from);
        if (prev != null) {
            prev.info.refreshImportRates(from.planet);
            refreshImportRates(from.planet, prev);
        }
        if (dest != null) {
            refreshImportRates(from.planet, dest);
        }
        save(from);
    }

    private static SectorLogisticsData load(Sector sector) {
        return Core.settings.getJson(settingsKey(sector), SectorLogisticsData.class, SectorLogisticsData::new);
    }

    private static String settingsKey(Sector sector) {
        return sector.planet.name + "-s-" + sector.id + "-sector-logistics";
    }
}
