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

public class SectorLogistics {
    private static final ObjectMap<Sector, SectorLogisticsData> cache = new ObjectMap<>();
    private static @Nullable Sector lastPlayedSector;

    public static void init() {
        Events.on(ResetEvent.class, e -> {
            cache.clear();
            lastPlayedSector = null;
        });

        Events.on(SaveWriteEvent.class, e -> {
            flushAllStats();
            if (state.isCampaign() && state.rules.sector != null && state.rules.sector.isBeingPlayed()) {
                syncCoreToStorage(state.rules.sector);
            }
            saveAll();
        });

        Events.on(SectorLoseEvent.class, e -> unload(e.sector));

        Events.on(WorldLoadEvent.class, e -> {
            if (lastPlayedSector != null && lastPlayedSector != state.rules.sector) {
                flushStats(lastPlayedSector);
                save(lastPlayedSector);
            }
            if (!state.isCampaign() || state.rules.sector == null) return;
            mergeStorageToCore(state.rules.sector);
            lastPlayedSector = state.rules.sector;
        });

        Events.on(TurnEvent.class, e -> {
            if (net.client()) return;
            runLegacyTurn();
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
        data.refreshItemImportRates(planet, sector);
        data.refreshLiquidImportRates(planet, sector);
        data.refreshPayloadImportRates(planet, sector);
    }

    public static void refreshImportRates(Planet planet) {
        if (planet == null) return;
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) refreshImportRates(planet, sector);
        }
    }

    public static void handleItemExport(Sector sector, Item item, int amount) {
        get(sector).handleItemExport(item, amount);
    }

    public static void handleItemImport(Sector sector, Item item, int amount) {
        get(sector).handleItemImport(item, amount);
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

    public static void addItems(Sector sector, ItemSeq stacks) {
        if (sector == null || stacks == null) return;

        if (sector.isBeingPlayed()) {
            CoreBuild core = state.rules.defaultTeam.core();
            if (core != null) {
                int cap = core.storageCapacity;
                stacks.each((item, amount) -> core.items.add(item, Math.min(cap - core.items.get(item), amount)));
            }
        } else if (sector.hasBase()) {
            SectorLogisticsData data = get(sector);
            int cap = sector.info.storageCapacity;
            stacks.each((item, amount) -> data.items.add(item, Math.min(cap - data.items.get(item), amount)));
            data.items.checkNegative();
            save(sector);
        }
    }

    public static void mergeStorageToCore(Sector sector) {
        SectorLogisticsData data = get(sector);
        if (data.items.total <= 0) return;

        CoreBuild core = state.rules.defaultTeam.core();
        if (core == null) return;

        int cap = core.storageCapacity;
        data.items.each((item, amount) -> core.items.add(item, Math.min(cap - core.items.get(item), amount)));
        data.items.clear();
        save(sector);
    }

    public static void syncCoreToStorage(Sector sector) {
        CoreBuild core = state.rules.defaultTeam.core();
        if (core == null) return;

        SectorLogisticsData data = get(sector);
        data.items.clear();
        for (int i = 0; i < core.items.length(); i++) {
            data.items.set(content.item(i), core.items.get(i));
        }
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

    private static void runLegacyTurn() {
        int newSecondsPassed = (int) (turnDuration / 60);

        for (Planet planet : content.planets()) {
            if (!planet.campaignRules.legacyLaunchPads) continue;

            for (Sector sector : planet.sectors) {
                if (!sector.hasBase() || sector.isBeingPlayed() || sector.isAttacked()) continue;
                if (sector.info.destination == null) continue;

                Sector dest = sector.info.destination;
                if (!dest.hasBase() || dest.planet != planet) continue;

                ItemSeq exported = new ItemSeq();
                get(sector).itemExport.each((item, stat) -> exported.add(item, (int) (stat.mean * newSecondsPassed)));
                addItems(dest, exported);
            }
        }
    }

    private static SectorLogisticsData load(Sector sector) {
        return Core.settings.getJson(settingsKey(sector), SectorLogisticsData.class, SectorLogisticsData::new);
    }

    private static String settingsKey(Sector sector) {
        return sector.planet.name + "-s-" + sector.id + "-logistics";
    }
}
