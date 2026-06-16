package mod.extend.sector;

import arc.func.Cons;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Time;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;

import java.util.Arrays;

import static mindustry.Vars.*;

public class SectorLogisticsData {
    private static final float refreshPeriod = 60f;

    public ObjectMap<Liquid, FlowStat> liquidExport = new ObjectMap<>();
    public ObjectMap<Liquid, FlowStat> liquidImport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadExport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadImport = new ObjectMap<>();
    public ObjectFloatMap<Liquid> liquidImportTimers = new ObjectFloatMap<>();
    public ObjectFloatMap<UnlockableContent> payloadImportTimers = new ObjectFloatMap<>();

    public transient float[] liquidImportRateCache;
    public transient ObjectFloatMap<UnlockableContent> payloadImportRateCache = new ObjectFloatMap<>();
    public transient Seq<UnlockableContent> payloadImportKeys = new Seq<>();

    private final transient Interval time = new Interval();

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

    public float getLiquidExport(Liquid liquid) {
        return liquidExport.get(liquid, FlowStat::new).mean;
    }

    public float getPayloadExport(UnlockableContent content) {
        return payloadExport.get(content, FlowStat::new).mean;
    }

    public boolean anyLiquidExports() {
        return hasFlow(liquidExport);
    }

    public boolean anyPayloadExports() {
        return hasFlow(payloadExport);
    }

    public void refreshLiquidImportRates(Planet planet, Sector self) {
        if (liquidImportRateCache == null || liquidImportRateCache.length != content.liquids().size) {
            liquidImportRateCache = new float[content.liquids().size];
        } else {
            Arrays.fill(liquidImportRateCache, 0f);
        }

        eachLiquidSource(planet, self, source -> {
            SectorLogisticsData data = SectorLogistics.get(source);
            data.liquidExport.each((liquid, stat) -> liquidImportRateCache[liquid.id] += stat.mean);
        });
    }

    public void refreshPayloadImportRates(Planet planet, Sector self) {
        payloadImportRateCache.clear();
        payloadImportKeys.clear();
        eachPayloadSource(planet, self, source -> {
            SectorLogisticsData data = SectorLogistics.get(source);
            data.payloadExport.each((content, stat) -> {
                payloadImportRateCache.increment(content, 0f, stat.mean);
                if (!payloadImportKeys.contains(content)) payloadImportKeys.add(content);
            });
        });
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

    public void eachLiquidSource(Planet planet, Sector self, Cons<Sector> cons) {
        for (Sector sector : planet.sectors) {
            if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyLiquidExports()) {
                cons.get(sector);
            }
        }
    }

    public void eachPayloadSource(Planet planet, Sector self, Cons<Sector> cons) {
        for (Sector sector : planet.sectors) {
            if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyPayloadExports()) {
                cons.get(sector);
            }
        }
    }

    public void flushAllStats() {
        flushStats(liquidExport);
        flushStats(liquidImport);
        flushStats(payloadExport);
        flushStats(payloadImport);
    }

    public boolean anyIncomingImports(Planet planet, Sector self) {
        refreshLiquidImportRates(planet, self);
        refreshPayloadImportRates(planet, self);

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
            liquidImport.each((liquid, stat) -> stat.mean = Math.min(stat.mean, getLiquidImportRate(planet, sector, liquid)));
            payloadImport.each((content, stat) -> stat.mean = Math.min(stat.mean, getPayloadImportRate(planet, sector, content)));
        }
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

    private boolean anyPayloadSources(Planet planet, Sector self) {
        for (Sector sector : planet.sectors) {
            if (sector.hasBase() && sector != self && sector.info.destination == self && SectorLogistics.get(sector).anyPayloadExports()) {
                return true;
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
