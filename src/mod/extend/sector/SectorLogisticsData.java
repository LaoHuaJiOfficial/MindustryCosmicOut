package mod.extend.sector;

import arc.func.Cons;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Time;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;

import java.util.Arrays;

import static mindustry.Vars.*;

public class SectorLogisticsData {
    private static final float refreshPeriod = 60f;

    public ItemSeq items = new ItemSeq();
    public ObjectMap<Item, FlowStat> itemExport = new ObjectMap<>();
    public ObjectMap<Item, FlowStat> itemImport = new ObjectMap<>();
    public ObjectMap<Liquid, FlowStat> liquidExport = new ObjectMap<>();
    public ObjectMap<Liquid, FlowStat> liquidImport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadExport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadImport = new ObjectMap<>();
    public ObjectFloatMap<Item> itemImportTimers = new ObjectFloatMap<>();
    public ObjectFloatMap<Liquid> liquidImportTimers = new ObjectFloatMap<>();
    public ObjectFloatMap<UnlockableContent> payloadImportTimers = new ObjectFloatMap<>();

    public transient float[] itemImportRateCache;
    public transient float[] liquidImportRateCache;
    public transient ObjectFloatMap<UnlockableContent> payloadImportRateCache = new ObjectFloatMap<>();
    public transient Seq<UnlockableContent> payloadImportKeys = new Seq<>();

    private final transient Interval time = new Interval();

    public void handleItemExport(Item item, int amount) {
        if (item == null || amount <= 0) return;
        itemExport.get(item, FlowStat::new).tickCounter(amount);
    }

    public void handleItemImport(Item item, int amount) {
        if (item == null || amount <= 0) return;
        itemImport.get(item, FlowStat::new).tickCounter(amount);
    }

    public void handleLiquidExport(Liquid liquid, float amount) {
        if (liquid == null || amount <= 0f) return;
        liquidExport.get(liquid, FlowStat::new).tickCounter(amount);
    }

    public void handleLiquidImport(Liquid liquid, float amount) {
        if (liquid == null || amount <= 0f) return;
        liquidImport.get(liquid, FlowStat::new).tickCounter(amount);
    }

    public void handlePayloadExport(UnlockableContent content, int amount) {
        if (content == null || amount <= 0) return;
        payloadExport.get(content, FlowStat::new).tickCounter(amount);
    }

    public void handlePayloadImport(UnlockableContent content, int amount) {
        if (content == null || amount <= 0) return;
        payloadImport.get(content, FlowStat::new).tickCounter(amount);
    }

    public float getItemExport(Item item) {
        return itemExport.get(item, FlowStat::new).mean;
    }

    public float getLiquidExport(Liquid liquid) {
        return liquidExport.get(liquid, FlowStat::new).mean;
    }

    public float getPayloadExport(UnlockableContent content) {
        return payloadExport.get(content, FlowStat::new).mean;
    }

    public boolean hasItemExport(Item item) {
        FlowStat stat = itemExport.get(item);
        return stat != null && stat.mean > 0f;
    }

    public boolean hasLiquidExport(Liquid liquid) {
        FlowStat stat = liquidExport.get(liquid);
        return stat != null && stat.mean > 0f;
    }

    public boolean hasPayloadExport(UnlockableContent content) {
        FlowStat stat = payloadExport.get(content);
        return stat != null && stat.mean > 0f;
    }

    public boolean anyItemExports() {
        return hasFlow(itemExport);
    }

    public boolean anyLiquidExports() {
        return hasFlow(liquidExport);
    }

    public boolean anyPayloadExports() {
        return hasFlow(payloadExport);
    }

    public void refreshItemImportRates(Planet planet, Sector self) {
        if (itemImportRateCache == null || itemImportRateCache.length != content.items().size) {
            itemImportRateCache = new float[content.items().size];
        } else {
            Arrays.fill(itemImportRateCache, 0f);
        }

        eachItemSource(planet, self, sector -> {
            SectorLogisticsData source = SectorLogistics.get(sector);
            source.itemExport.each((item, stat) -> itemImportRateCache[item.id] += stat.mean);
        });
    }

    public void refreshLiquidImportRates(Planet planet, Sector self) {
        if (liquidImportRateCache == null || liquidImportRateCache.length != content.liquids().size) {
            liquidImportRateCache = new float[content.liquids().size];
        } else {
            Arrays.fill(liquidImportRateCache, 0f);
        }

        eachLiquidSource(planet, self, sector -> {
            SectorLogisticsData source = SectorLogistics.get(sector);
            source.liquidExport.each((liquid, stat) -> liquidImportRateCache[liquid.id] += stat.mean);
        });
    }

    public void refreshPayloadImportRates(Planet planet, Sector self) {
        payloadImportRateCache.clear();
        payloadImportKeys.clear();
        eachPayloadSource(planet, self, sector -> {
            SectorLogisticsData source = SectorLogistics.get(sector);
            source.payloadExport.each((content, stat) -> {
                payloadImportRateCache.increment(content, 0f, stat.mean);
                if (!payloadImportKeys.contains(content)) payloadImportKeys.add(content);
            });
        });
    }

    public float getItemImportRate(Planet planet, Sector self, Item item) {
        refreshItemImportRates(planet, self);
        return itemImportRateCache[item.id];
    }

    public float getLiquidImportRate(Planet planet, Sector self, Liquid liquid) {
        refreshLiquidImportRates(planet, self);
        return liquidImportRateCache[liquid.id];
    }

    public float getPayloadImportRate(Planet planet, Sector self, UnlockableContent content) {
        if (payloadImportRateCache.isEmpty() && anyPayloadSources(planet, self)) {
            refreshPayloadImportRates(planet, self);
        }
        return payloadImportRateCache.get(content, 0f);
    }

    public void eachItemSource(Planet planet, Sector self, Cons<Sector> cons) {
        for (Planet p : content.planets()) {
            for (Sector sector : p.sectors) {
                if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyItemExports()) {
                    cons.get(sector);
                }
            }
        }
    }

    public void eachLiquidSource(Planet planet, Sector self, Cons<Sector> cons) {
        for (Planet p : content.planets()) {
            for (Sector sector : p.sectors) {
                if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyLiquidExports()) {
                    cons.get(sector);
                }
            }
        }
    }

    public void eachPayloadSource(Planet planet, Sector self, Cons<Sector> cons) {
        for (Planet p : content.planets()) {
            for (Sector sector : p.sectors) {
                if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyPayloadExports()) {
                    cons.get(sector);
                }
            }
        }
    }

    public void flushAllStats() {
        flushStats(itemExport);
        flushStats(itemImport);
        flushStats(liquidExport);
        flushStats(liquidImport);
        flushStats(payloadExport);
        flushStats(payloadImport);
    }

    public boolean anyIncomingImports(Planet planet, Sector self) {
        refreshItemImportRates(planet, self);
        refreshLiquidImportRates(planet, self);
        refreshPayloadImportRates(planet, self);

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

        if (time.get(refreshPeriod)) {
            flushAllStats();

            Planet planet = sector.planet;
            itemImport.each((item, stat) -> stat.mean = Math.min(stat.mean, getItemImportRate(planet, sector, item)));
            liquidImport.each((liquid, stat) -> stat.mean = Math.min(stat.mean, getLiquidImportRate(planet, sector, liquid)));
            payloadImport.each((content, stat) -> stat.mean = Math.min(stat.mean, getPayloadImportRate(planet, sector, content)));
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

    public void syncItemImportTimers(Planet planet, Sector self, float batchAmount) {
        refreshItemImportRates(planet, self);
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

    public void syncLiquidImportTimers(Planet planet, Sector self, float batchAmount) {
        refreshLiquidImportRates(planet, self);
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

    public void syncPayloadImportTimers(Planet planet, Sector self, float batchAmount) {
        refreshPayloadImportRates(planet, self);
        for (UnlockableContent content : payloadImportKeys) {
            float rate = payloadImportRateCache.get(content, 0f);
            float importedPerFrame = rate / 60f;
            if (importedPerFrame > 0f) {
                float framesBetweenArrival = batchAmount / importedPerFrame;
                payloadImportTimers.increment(content, 0f, 1f / framesBetweenArrival * Time.delta);
            }
        }
    }

    public float[] getItemImportRates(Planet planet, Sector self) {
        if (itemImportRateCache == null) refreshItemImportRates(planet, self);
        return itemImportRateCache;
    }

    public float[] getLiquidImportRates(Planet planet, Sector self) {
        if (liquidImportRateCache == null) refreshLiquidImportRates(planet, self);
        return liquidImportRateCache;
    }

    private boolean anyPayloadSources(Planet planet, Sector self) {
        for (Planet p : content.planets()) {
            for (Sector sector : p.sectors) {
                if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyPayloadExports()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> boolean hasFlow(ObjectMap<T, FlowStat> map) {
        boolean[] found = {false};
        map.each((key, stat) -> {
            if (stat.mean > 0f) found[0] = true;
        });
        return found[0];
    }

    private static <T> void flushStats(ObjectMap<T, FlowStat> map) {
        map.each((key, stat) -> stat.flushCounter());
    }
}
