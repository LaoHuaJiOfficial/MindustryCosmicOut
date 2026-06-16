package mod.extend.sector;

import arc.func.Cons;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;

import java.util.Arrays;

import static mindustry.Vars.*;

public class PlanetLogisticsData {
    private static final float refreshPeriod = 60f;

    public @Nullable String destination;
    public ItemSeq items = new ItemSeq();
    public ObjectMap<String, PlanetSectorLogisticsData> sectors = new ObjectMap<>();

    public ObjectFloatMap<Item> itemImportTimers = new ObjectFloatMap<>();
    public ObjectFloatMap<Liquid> liquidImportTimers = new ObjectFloatMap<>();
    public ObjectFloatMap<UnlockableContent> payloadImportTimers = new ObjectFloatMap<>();

    public transient float[] itemImportRateCache;
    public transient float[] liquidImportRateCache;
    public transient ObjectFloatMap<UnlockableContent> payloadImportRateCache = new ObjectFloatMap<>();
    public transient Seq<UnlockableContent> payloadImportKeys = new Seq<>();

    private final transient Interval time = new Interval();

    public PlanetSectorLogisticsData getSector(Sector sector) {
        if (sector == null) return new PlanetSectorLogisticsData();
        return sectors.get(String.valueOf(sector.id), PlanetSectorLogisticsData::new);
    }

    public void eachSector(Cons<PlanetSectorLogisticsData> cons) {
        sectors.each((id, data) -> cons.get(data));
    }

    public @Nullable Planet destinationPlanet() {
        return destination == null ? null : content.planet(destination);
    }

    public void setDestination(@Nullable Planet planet) {
        destination = planet == null ? null : planet.name;
    }

    public void handleItemExport(Sector sector, Item item, int amount) {
        getSector(sector).handleItemExport(item, amount);
    }

    public void handleItemImport(Sector sector, Item item, int amount) {
        getSector(sector).handleItemImport(item, amount);
    }

    public void handleLiquidExport(Sector sector, Liquid liquid, float amount) {
        getSector(sector).handleLiquidExport(liquid, amount);
    }

    public void handleLiquidImport(Sector sector, Liquid liquid, float amount) {
        getSector(sector).handleLiquidImport(liquid, amount);
    }

    public void handlePayloadExport(Sector sector, UnlockableContent content, int amount) {
        getSector(sector).handlePayloadExport(content, amount);
    }

    public void handlePayloadImport(Sector sector, UnlockableContent content, int amount) {
        getSector(sector).handlePayloadImport(content, amount);
    }

    public float getItemExport(Item item) {
        float[] total = {0f};
        eachSector(data -> total[0] += data.getItemExport(item));
        return total[0];
    }

    public float getLiquidExport(Liquid liquid) {
        float[] total = {0f};
        eachSector(data -> total[0] += data.getLiquidExport(liquid));
        return total[0];
    }

    public float getPayloadExport(UnlockableContent content) {
        float[] total = {0f};
        eachSector(data -> total[0] += data.getPayloadExport(content));
        return total[0];
    }

    public boolean hasItemExport(Item item) {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.getItemExport(item) > 0f) found[0] = true;
        });
        return found[0];
    }

    public boolean hasLiquidExport(Liquid liquid) {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.getLiquidExport(liquid) > 0f) found[0] = true;
        });
        return found[0];
    }

    public boolean hasPayloadExport(UnlockableContent content) {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.getPayloadExport(content) > 0f) found[0] = true;
        });
        return found[0];
    }

    public boolean anyItemExports() {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.anyItemExports()) found[0] = true;
        });
        return found[0];
    }

    public boolean anyLiquidExports() {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.anyLiquidExports()) found[0] = true;
        });
        return found[0];
    }

    public boolean anyPayloadExports() {
        boolean[] found = {false};
        eachSector(data -> {
            if (data.anyPayloadExports()) found[0] = true;
        });
        return found[0];
    }

    public void refreshItemImportRates(Planet self) {
        if (itemImportRateCache == null || itemImportRateCache.length != content.items().size) {
            itemImportRateCache = new float[content.items().size];
        } else {
            Arrays.fill(itemImportRateCache, 0f);
        }

        eachItemSource(self, source -> {
            PlanetLogisticsData data = PlanetLogistics.get(source);
            data.eachSector(sectorData -> sectorData.itemExport.each((item, stat) -> itemImportRateCache[item.id] += stat.mean));
        });
    }

    public void refreshLiquidImportRates(Planet self) {
        if (liquidImportRateCache == null || liquidImportRateCache.length != content.liquids().size) {
            liquidImportRateCache = new float[content.liquids().size];
        } else {
            Arrays.fill(liquidImportRateCache, 0f);
        }

        eachLiquidSource(self, source -> {
            PlanetLogisticsData data = PlanetLogistics.get(source);
            data.eachSector(sectorData -> sectorData.liquidExport.each((liquid, stat) -> liquidImportRateCache[liquid.id] += stat.mean));
        });
    }

    public void refreshPayloadImportRates(Planet self) {
        payloadImportRateCache.clear();
        payloadImportKeys.clear();

        eachPayloadSource(self, source -> {
            PlanetLogisticsData data = PlanetLogistics.get(source);
            data.eachSector(sectorData -> sectorData.payloadExport.each((content, stat) -> {
                payloadImportRateCache.increment(content, 0f, stat.mean);
                if (!payloadImportKeys.contains(content)) payloadImportKeys.add(content);
            }));
        });
    }

    public float getItemImportRate(Planet self, Item item) {
        refreshItemImportRates(self);
        return itemImportRateCache[item.id];
    }

    public float getLiquidImportRate(Planet self, Liquid liquid) {
        refreshLiquidImportRates(self);
        return liquidImportRateCache[liquid.id];
    }

    public float getPayloadImportRate(Planet self, UnlockableContent content) {
        if (payloadImportRateCache.isEmpty() && anyPayloadSources(self)) {
            refreshPayloadImportRates(self);
        }
        return payloadImportRateCache.get(content, 0f);
    }

    public void eachItemSource(Planet self, Cons<Planet> cons) {
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData data = PlanetLogistics.get(planet);
            if (data.destinationPlanet() == self && data.anyItemExports()) cons.get(planet);
        }
    }

    public void eachLiquidSource(Planet self, Cons<Planet> cons) {
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData data = PlanetLogistics.get(planet);
            if (data.destinationPlanet() == self && data.anyLiquidExports()) cons.get(planet);
        }
    }

    public void eachPayloadSource(Planet self, Cons<Planet> cons) {
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData data = PlanetLogistics.get(planet);
            if (data.destinationPlanet() == self && data.anyPayloadExports()) cons.get(planet);
        }
    }

    public void flushAllStats() {
        sectors.each((id, data) -> data.flushAllStats());
    }

    public void flushSectorStats(Sector sector) {
        getSector(sector).flushAllStats();
    }

    public boolean anyIncomingImports(Planet self) {
        refreshItemImportRates(self);
        refreshLiquidImportRates(self);
        refreshPayloadImportRates(self);

        for (Item item : content.items()) {
            if (itemImportRateCache[item.id] > 0.01f) return true;
        }
        for (Liquid liquid : content.liquids()) {
            if (liquidImportRateCache[liquid.id] > 0.01f) return true;
        }
        for (UnlockableContent uc : payloadImportKeys) {
            if (payloadImportRateCache.get(uc, 0f) > 0.01f) return true;
        }
        return false;
    }

    public void update(Sector sector) {
        if (net.client() || sector == null) return;

        Planet planet = sector.planet;
        PlanetSectorLogisticsData data = getSector(sector);

        if (time.get(refreshPeriod)) {
            data.flushAllStats();

            data.itemImport.each((item, stat) -> stat.mean = Math.min(stat.mean, getItemImportRate(planet, item)));
            data.liquidImport.each((liquid, stat) -> stat.mean = Math.min(stat.mean, getLiquidImportRate(planet, liquid)));
            data.payloadImport.each((content, stat) -> stat.mean = Math.min(stat.mean, getPayloadImportRate(planet, content)));
        }
    }

    public void resetItemImportTimer(Item item) {
        if (item == null) return;
        itemImportTimers.put(item, 0f);
    }

    public float itemImportTimer(Item item) {
        return itemImportTimers.get(item, 0f);
    }

    public void resetLiquidImportTimer(Liquid liquid) {
        if (liquid == null) return;
        liquidImportTimers.put(liquid, 0f);
    }

    public float liquidImportTimer(Liquid liquid) {
        return liquidImportTimers.get(liquid, 0f);
    }

    public void resetPayloadImportTimer(UnlockableContent content) {
        if (content == null) return;
        payloadImportTimers.put(content, 0f);
    }

    public float payloadImportTimer(UnlockableContent content) {
        return payloadImportTimers.get(content, 0f);
    }

    public void syncItemImportTimers(Planet self, float batchAmount) {
        refreshItemImportRates(self);
        float[] imports = itemImportRateCache;
        for (Item item : content.items()) {
            float importedPerFrame = imports[item.id] / 60f;
            if (importedPerFrame > 0f) {
                float framesBetweenArrival = batchAmount / importedPerFrame;
                itemImportTimers.increment(item, 0f, 1f / framesBetweenArrival * Time.delta);
            } else {
                itemImportTimers.put(item, 0f);
            }
        }
    }

    public void syncLiquidImportTimers(Planet self, float batchAmount) {
        refreshLiquidImportRates(self);
        float[] imports = liquidImportRateCache;
        for (Liquid liquid : content.liquids()) {
            float importedPerFrame = imports[liquid.id] / 60f;
            if (importedPerFrame > 0f) {
                float framesBetweenArrival = batchAmount / importedPerFrame;
                liquidImportTimers.increment(liquid, 0f, 1f / framesBetweenArrival * Time.delta);
            } else {
                liquidImportTimers.put(liquid, 0f);
            }
        }
    }

    public void syncPayloadImportTimers(Planet self, float batchAmount) {
        refreshPayloadImportRates(self);
        for (UnlockableContent content : payloadImportKeys) {
            float rate = payloadImportRateCache.get(content, 0f);
            float importedPerFrame = rate / 60f;
            if (importedPerFrame > 0f) {
                float framesBetweenArrival = batchAmount / importedPerFrame;
                payloadImportTimers.increment(content, 0f, 1f / framesBetweenArrival * Time.delta);
            }
        }
    }

    private boolean anyPayloadSources(Planet self) {
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData data = PlanetLogistics.get(planet);
            if (data.destinationPlanet() == self && data.anyPayloadExports()) return true;
        }
        return false;
    }
}
