package mod.extend.sector;

import arc.Core;
import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.*;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import static mindustry.Vars.*;

public class PlanetLogistics {
    private static final ObjectMap<Planet, PlanetLogisticsData> cache = new ObjectMap<>();
    private static @Nullable Planet lastPlayedPlanet;
    private static @Nullable Sector lastPlayedSector;

    public static void init() {
        Events.on(ResetEvent.class, e -> {
            cache.clear();
            lastPlayedPlanet = null;
            lastPlayedSector = null;
        });

        Events.on(SaveWriteEvent.class, e -> {
            flushAllStats();
            if (state.isCampaign() && state.rules.sector != null && state.rules.sector.isBeingPlayed()) {
                syncCoreToStorage(state.getPlanet());
            }
            saveAll();
        });

        Events.on(SectorLoseEvent.class, e -> {
            if (!hasBase(e.sector.planet)) unload(e.sector.planet);
            else save(e.sector.planet);
        });

        Events.on(WorldLoadEvent.class, e -> {
            if (lastPlayedSector != null && lastPlayedSector != state.rules.sector) {
                flushStats(lastPlayedSector);
                save(lastPlayedSector.planet);
            } else if (lastPlayedPlanet != null && lastPlayedPlanet != state.getPlanet()) {
                flushStats(lastPlayedPlanet);
                save(lastPlayedPlanet);
            }

            if (!state.isCampaign() || state.getPlanet() == null) {
                lastPlayedPlanet = null;
                lastPlayedSector = null;
                return;
            }

            mergeStorageToCore(state.getPlanet());
            lastPlayedPlanet = state.getPlanet();
            lastPlayedSector = state.rules.sector;
        });

        Events.on(TurnEvent.class, e -> {
            if (net.client()) return;
            runLegacyTurn();
            for (Planet planet : content.planets()) {
                if (hasBase(planet)) refreshImportRates(planet);
            }
        });

        Events.run(Trigger.update, () -> {
            if (!state.isCampaign() || state.rules.sector == null || net.client()) return;
            get(state.rules.sector.planet).update(state.rules.sector);
        });
    }

    public static PlanetLogisticsData get(Planet planet) {
        if (planet == null) return new PlanetLogisticsData();
        return cache.get(planet, () -> load(planet));
    }

    public static void refreshImportRates(Planet planet) {
        if (planet == null) return;
        PlanetLogisticsData data = get(planet);
        data.refreshItemImportRates(planet);
        data.refreshLiquidImportRates(planet);
        data.refreshPayloadImportRates(planet);
    }

    public static void handleItemExport(Planet planet, Item item, int amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handleItemExport(sector, item, amount);
    }

    public static void handleItemImport(Planet planet, Item item, int amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handleItemImport(sector, item, amount);
    }

    public static void handleLiquidExport(Planet planet, Liquid liquid, float amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handleLiquidExport(sector, liquid, amount);
    }

    public static void handleLiquidImport(Planet planet, Liquid liquid, float amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handleLiquidImport(sector, liquid, amount);
    }

    public static void handlePayloadExport(Planet planet, UnlockableContent content, int amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handlePayloadExport(sector, content, amount);
    }

    public static void handlePayloadImport(Planet planet, UnlockableContent content, int amount) {
        Sector sector = activeSector(planet);
        if (sector != null) get(planet).handlePayloadImport(sector, content, amount);
    }

    public static void addItems(Planet planet, ItemSeq stacks) {
        if (planet == null || stacks == null) return;

        if (isBeingPlayed(planet)) {
            CoreBuild core = state.rules.defaultTeam.core();
            if (core != null) {
                int cap = core.storageCapacity;
                stacks.each((item, amount) -> core.items.add(item, Math.min(cap - core.items.get(item), amount)));
            }
        } else if (hasBase(planet)) {
            PlanetLogisticsData data = get(planet);
            int cap = storageCapacity(planet);
            stacks.each((item, amount) -> data.items.add(item, Math.min(cap - data.items.get(item), amount)));
            data.items.checkNegative();
            save(planet);
        }
    }

    public static void mergeStorageToCore(Planet planet) {
        PlanetLogisticsData data = get(planet);
        if (data.items.total <= 0) return;

        CoreBuild core = state.rules.defaultTeam.core();
        if (core == null) return;

        int cap = core.storageCapacity;
        data.items.each((item, amount) -> core.items.add(item, Math.min(cap - core.items.get(item), amount)));
        data.items.clear();
        save(planet);
    }

    public static void syncCoreToStorage(Planet planet) {
        CoreBuild core = state.rules.defaultTeam.core();
        if (core == null) return;

        PlanetLogisticsData data = get(planet);
        data.items.clear();
        for (int i = 0; i < core.items.length(); i++) {
            data.items.set(content.item(i), core.items.get(i));
        }
    }

    public static void save(Planet planet) {
        if (planet == null) return;
        Core.settings.putJson(settingsKey(planet), get(planet));
    }

    public static void saveAll() {
        cache.each((planet, data) -> save(planet));
    }

    public static void flushAllStats() {
        cache.each((planet, data) -> data.flushAllStats());
    }

    public static void flushStats(Planet planet) {
        if (planet == null) return;
        get(planet).flushAllStats();
    }

    public static void flushStats(Sector sector) {
        if (sector == null) return;
        get(sector.planet).flushSectorStats(sector);
    }

    public static void unload(Planet planet) {
        if (planet == null) return;
        save(planet);
        cache.remove(planet);
    }

    public static boolean hasBase(Planet planet) {
        if (planet == null) return false;
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) return true;
        }
        return false;
    }

    public static boolean isBeingPlayed(Planet planet) {
        return state.isCampaign() && state.getPlanet() == planet && state.rules.sector != null && state.rules.sector.isBeingPlayed();
    }

    public static @Nullable Sector representativeSector(Planet planet) {
        if (planet == null) return null;
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) return sector;
        }
        if (planet.startSector >= 0 && planet.startSector < planet.sectors.size) {
            return planet.sectors.get(planet.startSector);
        }
        return planet.sectors.isEmpty() ? null : planet.sectors.first();
    }

    public static int storageCapacity(Planet planet) {
        Sector sector = representativeSector(planet);
        return sector == null ? 0 : sector.info.storageCapacity;
    }

    private static @Nullable Sector activeSector(Planet planet) {
        if (!state.isCampaign() || state.rules.sector == null || state.rules.sector.planet != planet) return null;
        return state.rules.sector;
    }

    private static void runLegacyTurn() {
        int newSecondsPassed = (int) (turnDuration / 60);

        for (Planet planet : content.planets()) {
            if (!planet.campaignRules.legacyLaunchPads || !hasBase(planet) || isBeingPlayed(planet)) continue;

            PlanetLogisticsData data = get(planet);
            for (Sector sector : planet.sectors) {
                PlanetSectorLogisticsData sectorData = data.getSector(sector);
                Planet dest = sectorData.destinationPlanet();
                if (dest == null || !hasBase(dest)) continue;

                ItemSeq exported = new ItemSeq();
                sectorData.itemExport.each((item, stat) -> exported.add(item, (int) (stat.mean * newSecondsPassed)));
                addItems(dest, exported);
            }
        }
    }

    private static PlanetLogisticsData load(Planet planet) {
        PlanetLogisticsData data = Core.settings.getJson(settingsKey(planet), PlanetLogisticsData.class, PlanetLogisticsData::new);
        data.migrateLegacyDestination(planet);
        return data;
    }

    private static String settingsKey(Planet planet) {
        return planet.name + "-logistics";
    }
}
