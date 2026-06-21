package mod.extend.sector;

import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.Planet;

import static mindustry.Vars.content;

public class PlanetSectorLogisticsData {
    public @Nullable String destination;

    public ObjectMap<Item, FlowStat> itemExport = new ObjectMap<>();
    public ObjectMap<Item, FlowStat> itemImport = new ObjectMap<>();
    public ObjectMap<Liquid, FlowStat> liquidExport = new ObjectMap<>();
    public ObjectMap<Liquid, FlowStat> liquidImport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadExport = new ObjectMap<>();
    public ObjectMap<UnlockableContent, FlowStat> payloadImport = new ObjectMap<>();

    public @Nullable Planet destinationPlanet() {
        return destination == null ? null : content.planet(destination);
    }

    public void setDestination(@Nullable Planet planet) {
        destination = planet == null ? null : planet.name;
    }

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

    public boolean anyItemExports() {
        return hasFlow(itemExport);
    }

    public boolean anyLiquidExports() {
        return hasFlow(liquidExport);
    }

    public boolean anyPayloadExports() {
        return hasFlow(payloadExport);
    }

    public boolean anyItemImports() {
        return hasFlow(itemImport);
    }

    public boolean anyLiquidImports() {
        return hasFlow(liquidImport);
    }

    public boolean anyPayloadImports() {
        return hasFlow(payloadImport);
    }

    public void flushAllStats() {
        flushStats(itemExport);
        flushStats(itemImport);
        flushStats(liquidExport);
        flushStats(liquidImport);
        flushStats(payloadExport);
        flushStats(payloadImport);
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
