package mod.extend;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Nullable;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.game.SectorInfo.ExportStat;
import mindustry.gen.Iconc;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mod.extend.sector.FlowStat;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.sector.PlanetSectorLogisticsData;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;

import static mindustry.Vars.*;

public class StarMapLogisticsUI {
    public static void buildSectorStats(Table c, Sector sector) {
        c.defaults().padBottom(5);

        if (sector.preset != null && sector.preset.description != null) {
            c.add(sector.preset.displayDescription()).width(420f).wrap().left().row();
        }

        if (sector.save != null) {
            c.add(Core.bundle.get("sectors.time") + " [accent]" + sector.save.getPlayTime()).left().row();
        }

        if (sector.info.attempts > 0) {
            c.add(Core.bundle.get("sectors.attempts") + " [accent]" + sector.info.attempts).left().row();
        }

        if (sector.info.waves && sector.hasBase()) {
            c.add(Core.bundle.get("sectors.wave") + " [accent]" + sector.info.wave).left().row();
        }

        if (sector.isAttacked() || !sector.hasBase()) {
            c.add(Core.bundle.get("sectors.threat") + " [accent]" + sector.displayThreat()).left().row();
        }

        if (sector.save != null && sector.info.resources.any()) {
            c.add("@sectors.resources").left().row();
            c.table(t -> {
                for (UnlockableContent uc : sector.info.resources) {
                    if (uc == null) continue;
                    t.image(uc.uiIcon).scaling(Scaling.fit).padRight(3).size(iconSmall);
                }
            }).padLeft(10f).left().row();
        }

        if (sector.isAttacked() && !sector.isBeingPlayed()) {
            c.add(UI.formatIcons(Core.bundle.get("sector.lockdown"))).wrap().fillX().padBottom(10f).row();
        }

        SectorLogistics.flushStats(sector);
        SectorLogistics.refreshImportRates(sector.planet, sector);
        if (hasSectorLogisticsContent(sector)) {
            borderedTable(c, t -> buildSectorLogisticsContent(t, sector));
        }

        if (PlanetLogistics.hasBase(sector.planet) && hasPlanetLogisticsContent(sector.planet)) {
            c.add("[accent]" + sector.planet.localizedName).padTop(8f).left().row();
            buildPlanetLogisticsStats(c, sector.planet, true, sector);
        }
    }

    static void buildSectorLogisticsContent(Table c, Sector sector) {
        SectorLogisticsData data = SectorLogistics.get(sector);

        displayExportStats(c, sector.info.production, "@sectors.production");

        Table itemExportTable = buildItemExportTable(sector.info.export);
        boolean hasLogisticsExport = hasSectorExport(data);
        if (itemExportTable != null || hasLogisticsExport) {
            appendSectionHeader(c, "@sectors.export", sector.info.destination != null && sector.info.destination.hasBase()
                    ? formatSectorLink(sector.info.destination) : null);
            Table flows = buildSectorExportFlowTable(sector.info.export, data);
            if (flows != null) c.add(flows).padLeft(4f).left().row();
        }

        Table itemImportTable = buildItemExportTable(sector.info.imports);
        boolean hasLogisticsImport = sector.hasBase() && data.anyIncomingImports(sector.planet, sector);
        boolean hasItemImport = sector.hasBase() && itemImportTable != null;
        if (hasItemImport || hasLogisticsImport) {
            Seq<Sector> sources = collectSectorImportSources(sector, hasItemImport, hasLogisticsImport);
            String route = sources.size == 1 ? formatImportSectorLink(sources.first()) : null;
            appendSectionHeader(c, "@sectors.import", route);
            if (sources.size > 1) {
                for (Sector source : sources) {
                    c.add(formatImportSectorLink(source)).padLeft(4f).left().row();
                }
            }
            Table flows = buildSectorImportFlowTable(sector.info.imports, sector, data, hasLogisticsImport);
            if (flows != null) c.add(flows).padLeft(4f).left().row();
        }

        var items = sector.items();
        if (sector.hasBase() && items.total > 0) {
            appendStoredItems(c, items);
        }
    }

    public static void buildPlanetLogisticsStats(Table c, Planet planet) {
        buildPlanetLogisticsStats(c, planet, false, null);
    }

    static void buildPlanetLogisticsStats(Table c, Planet planet, boolean embedded, @Nullable Sector contextSector) {
        c.defaults().left().padBottom(4);

        PlanetLogistics.refreshImportRates(planet);
        PlanetLogisticsData data = PlanetLogistics.get(planet);

        if (hasPlanetExport(data, planet)) {
            c.add("@sectors.export").left().row();
            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = data.getSector(sector);
                Planet dest = data.destinationPlanet(sector);
                boolean hasExport = hasPlanetSectorExport(sectorData);
                if (!hasExport) continue;
                borderedTable(c, t -> {
                    appendSectorExportHeader(t, sector, dest, contextSector);
                    addAllFlows(t, sectorData.itemExport, sectorData.liquidExport, sectorData.payloadExport);
                });
            }
        }

        if (PlanetLogistics.hasBase(planet) && data.anyIncomingImports(planet)) {
            c.add("@sectors.import").left().row();
            appendPlanetImportBySource(c, planet);
        }

        if (!embedded) {
            ItemSeq stored = collectPlanetItems(planet);
            if (PlanetLogistics.hasBase(planet) && stored.total > 0) {
                appendPlanetStoredItems(c, stored);
            }
        }
    }

    static void appendSectionHeader(Table c, String label, String route) {
        c.table(t -> {
            t.left().defaults().left();
            t.add(label).left();
            if (route != null) t.add(route).padLeft(6f).left();
        }).left().row();
    }

    static void appendStoredItems(Table c, ItemSeq items) {
        c.add("@sectors.stored").padTop(2f).left().row();
        Table stored = buildStoredTable(items);
        if (stored != null) c.add(stored).padLeft(4f).left().row();
    }

    static void appendPlanetStoredItems(Table c, ItemSeq items) {
        c.add("@sectors.stored").left().row();
        Table stored = buildStoredTable(items);
        if (stored != null) borderedTable(c, t -> t.add(stored).padLeft(4f).left());
    }

    static ItemSeq collectPlanetItems(Planet planet) {
        ItemSeq total = new ItemSeq();
        if (planet == null) return total;
        for (Sector sector : planet.sectors) {
            if (!sector.hasBase() || sector.isFrozen()) continue;
            sector.items().each((item, amount) -> {
                int value = Math.max(amount, 0);
                if (value > 0) total.add(item, value);
            });
        }
        return total;
    }

    static Seq<Sector> collectSectorImportSources(Sector sector, boolean hasItemImport, boolean hasLogisticsImport) {
        Seq<Sector> shown = new Seq<>();
        if (hasItemImport) {
            sector.info.eachImport(sector.planet, other -> addSectorImportSource(shown, other, sector));
        }
        if (hasLogisticsImport) {
            SectorLogisticsData data = SectorLogistics.get(sector);
            data.eachLiquidSource(sector.planet, sector, s -> addSectorImportSource(shown, s, sector));
            data.eachPayloadSource(sector.planet, sector, s -> addSectorImportSource(shown, s, sector));
        }
        return shown;
    }

    static void addSectorImportSource(Seq<Sector> shown, Sector source, Sector self) {
        if (source == self || source == null || shown.contains(source)) return;
        shown.add(source);
    }

    static Table buildStoredTable(ItemSeq items) {
        Table res = new Table().left();
        int i = 0;
        for (Item item : content.items()) {
            int amount = items.get(item);
            if (amount <= 0) continue;
            res.image(item.uiIcon).scaling(Scaling.fit).padRight(2).size(iconSmall);
            res.add(UI.formatAmount(amount)).color(Color.lightGray).padRight(6f).left();
            if (++i % 4 == 0) res.row();
        }
        return res.getChildren().any() ? res : null;
    }

    static Table buildItemExportTable(ObjectMap<Item, ExportStat> stats) {
        Table t = new Table().left();
        int i = 0;
        for (Item item : content.items()) {
            ExportStat stat = stats.get(item);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 1) continue;
            t.image(item.uiIcon).scaling(Scaling.fit).padRight(3).size(iconSmall);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 3 == 0) t.row();
        }
        return t.getChildren().any() ? t : null;
    }

    static void displayExportStats(Table c, ObjectMap<Item, ExportStat> stats, String name) {
        displayExportStats(c, stats, name, t -> {});
    }

    static void displayExportStats(Table c, ObjectMap<Item, ExportStat> stats, String name, arc.func.Cons<Table> builder) {
        Table t = new Table().left();
        int i = 0;
        for (Item item : content.items()) {
            ExportStat stat = stats.get(item);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 1) continue;
            t.image(item.uiIcon).scaling(Scaling.fit).padRight(3).size(iconSmall);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 3 == 0) t.row();
        }

        if (t.getChildren().any()) {
            c.defaults().left();
            c.add(name).left().row();
            builder.get(c);
            c.add(t).padLeft(4f).left().row();
        }
    }

    static boolean hasSectorExport(SectorLogisticsData data) {
        return hasFlow(data.liquidExport) || hasFlow(data.payloadExport);
    }

    static boolean hasPlanetExport(PlanetLogisticsData data, Planet planet) {
        for (Sector sector : planet.sectors) {
            if (sector.hasBase() && hasPlanetSectorExport(data.getSector(sector))) return true;
        }
        return false;
    }

    static boolean hasPlanetSectorExport(PlanetSectorLogisticsData data) {
        return hasFlow(data.itemExport) || hasFlow(data.liquidExport) || hasFlow(data.payloadExport);
    }

    static void appendPlanetImportBySource(Table c, Planet planet) {
        forEachImportSourcePlanet(planet, source -> {
            if (countSectorsWithBase(source) > 1) {
                for (Sector sourceSector : exportingSectors(source, planet)) {
                    PlanetSectorLogisticsData sectorData = PlanetLogistics.get(source).getSector(sourceSector);
                    borderedTable(c, t -> {
                        t.add(formatImportPlanetSectorLink(source, sourceSector)).padLeft(4f).left().row();
                        addAllFlows(t, sectorData.itemExport, sectorData.liquidExport, sectorData.payloadExport);
                    });
                }
            } else {
                borderedTable(c, t -> {
                    t.add(formatImportPlanetLink(source)).padLeft(4f).left().row();
                    appendSourcePlanetExportFlows(t, source, planet);
                });
            }
        });
    }

    static void forEachImportSourcePlanet(Planet self, arc.func.Cons<Planet> cons) {
        Seq<Planet> shown = new Seq<>();
        PlanetLogisticsData data = PlanetLogistics.get(self);
        data.eachItemSource(self, p -> addImportSourcePlanet(shown, p, cons));
        data.eachLiquidSource(self, p -> addImportSourcePlanet(shown, p, cons));
        data.eachPayloadSource(self, p -> addImportSourcePlanet(shown, p, cons));
    }

    static void addImportSourcePlanet(Seq<Planet> shown, Planet source, arc.func.Cons<Planet> cons) {
        if (shown.contains(source)) return;
        shown.add(source);
        cons.get(source);
    }

    static int countSectorsWithBase(Planet planet) {
        int count = 0;
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) count++;
        }
        return count;
    }

    static Seq<Sector> exportingSectors(Planet source, Planet dest) {
        Seq<Sector> result = new Seq<>();
        PlanetLogisticsData data = PlanetLogistics.get(source);
        for (Sector sector : source.sectors) {
            if (!sector.hasBase()) continue;
            PlanetSectorLogisticsData sectorData = data.getSector(sector);
            if (sectorData.destinationPlanet() != dest || !hasPlanetSectorExport(sectorData)) continue;
            result.add(sector);
        }
        return result;
    }

    static void appendSourcePlanetExportFlows(Table t, Planet source, Planet dest) {
        PlanetLogisticsData data = PlanetLogistics.get(source);
        ObjectMap<Item, FlowStat> items = new ObjectMap<>();
        ObjectMap<Liquid, FlowStat> liquids = new ObjectMap<>();
        ObjectMap<UnlockableContent, FlowStat> payloads = new ObjectMap<>();
        for (Sector sector : source.sectors) {
            if (!sector.hasBase()) continue;
            PlanetSectorLogisticsData sectorData = data.getSector(sector);
            if (sectorData.destinationPlanet() != dest) continue;
            mergeFlowMap(items, sectorData.itemExport);
            mergeFlowMap(liquids, sectorData.liquidExport);
            mergeFlowMap(payloads, sectorData.payloadExport);
        }
        addAllFlows(t, items, liquids, payloads);
    }

    static void appendSectorHeader(Table c, Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        c.add("[lightgray]" + prefix + sector.name()).left().row();
    }

    static void appendSectorExportHeader(Table c, Sector sector, Planet dest, @Nullable Sector contextSector) {
        if (contextSector == sector) {
            if (dest != null) c.add(formatPlanetLink(dest)).left().row();
            return;
        }
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        if (dest != null) {
            c.add("[lightgray]" + prefix + sector.name() + "[] " + formatPlanetLink(dest)).left().row();
        } else {
            appendSectorHeader(c, sector);
        }
    }

    static void borderedTable(Table parent, arc.func.Cons<Table> builder) {
        parent.table(Styles.grayPanel, t -> {
            t.left();
            t.defaults().left().padBottom(2);
            t.margin(6f);
            builder.get(t);
        }).fillX().left().padBottom(4).row();
    }

    static boolean hasSectorLogisticsContent(Sector sector) {
        SectorLogisticsData data = SectorLogistics.get(sector);
        if (buildItemExportTable(sector.info.production) != null) return true;
        if (buildItemExportTable(sector.info.export) != null || hasSectorExport(data)) return true;
        if (sector.hasBase() && (buildItemExportTable(sector.info.imports) != null || data.anyIncomingImports(sector.planet, sector))) {
            return true;
        }
        return sector.hasBase() && sector.items().total > 0;
    }

    static <T> boolean hasFlow(ObjectMap<T, FlowStat> map) {
        boolean[] found = {false};
        map.each((k, stat) -> {
            if (stat.mean > 0.01f) found[0] = true;
        });
        return found[0];
    }

    static boolean hasPlanetLogisticsContent(Planet planet) {
        if (!PlanetLogistics.hasBase(planet)) return false;
        PlanetLogisticsData data = PlanetLogistics.get(planet);
        if (hasPlanetExport(data, planet)) return true;
        if (data.anyIncomingImports(planet)) return true;
        return collectPlanetItems(planet).total > 0;
    }

    static String formatPlanetLink(Planet planet) {
        return Iconc.rightOpen + " " + planet.localizedName;
    }

    static String formatImportPlanetLink(Planet planet) {
        return Iconc.leftOpen + " " + planet.localizedName;
    }

    static String formatImportPlanetSectorLink(Planet planet, Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        return Iconc.leftOpen + " " + planet.localizedName + " | " + prefix + sector.name();
    }

    static String formatImportSectorLink(Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        return Iconc.leftOpen + " " + prefix + sector.name();
    }

    static String formatSectorLink(Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        return Iconc.rightOpen + " " + prefix + sector.name();
    }

    static Table buildSectorExportFlowTable(ObjectMap<Item, ExportStat> itemExports, SectorLogisticsData data) {
        Table t = buildFlowTable();
        int i = appendItemExportEntries(t, itemExports);
        i = appendFlowMapEntries(t, data.liquidExport, content.liquids(), liquid -> liquid.uiIcon, i);
        appendPayloadMapEntries(t, data.payloadExport, i);
        return t.getChildren().any() ? t : null;
    }

    static Table buildSectorImportFlowTable(ObjectMap<Item, ExportStat> itemImports, Sector sector, SectorLogisticsData data, boolean hasLogisticsImport) {
        Table t = buildFlowTable();
        int i = appendItemExportEntries(t, itemImports);
        if (!hasLogisticsImport) return t.getChildren().any() ? t : null;
        for (Liquid liquid : content.liquids()) {
            int total = (int) (data.getLiquidImportRate(sector.planet, sector, liquid) * 60);
            if (total <= 0) continue;
            appendFlowEntry(t, liquid.uiIcon, total, i++);
        }
        data.refreshPayloadImportRates(sector.planet, sector);
        for (UnlockableContent payload : data.payloadImportKeys) {
            int total = (int) (data.getPayloadImportRate(sector.planet, sector, payload) * 60);
            if (total <= 0) continue;
            appendFlowEntry(t, payload.uiIcon, total, i++);
        }
        return t.getChildren().any() ? t : null;
    }

    static void addAllFlows(Table parent, ObjectMap<Item, FlowStat> items, ObjectMap<Liquid, FlowStat> liquids, ObjectMap<UnlockableContent, FlowStat> payloads) {
        Table t = buildCombinedFlowTable(items, liquids, payloads);
        if (t != null) parent.add(t).padLeft(4f).left().row();
    }

    static Table buildCombinedFlowTable(ObjectMap<Item, FlowStat> items, ObjectMap<Liquid, FlowStat> liquids, ObjectMap<UnlockableContent, FlowStat> payloads) {
        Table t = buildFlowTable();
        int i = appendFlowMapEntries(t, items, content.items(), item -> item.uiIcon, 0);
        i = appendFlowMapEntries(t, liquids, content.liquids(), liquid -> liquid.uiIcon, i);
        appendPayloadMapEntries(t, payloads, i);
        return t.getChildren().any() ? t : null;
    }

    static int appendItemExportEntries(Table t, ObjectMap<Item, ExportStat> stats) {
        int i = 0;
        for (Item item : content.items()) {
            ExportStat stat = stats.get(item);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 1) continue;
            appendFlowEntry(t, item.uiIcon, total, i++);
        }
        return i;
    }

    static <T> int appendFlowMapEntries(Table t, ObjectMap<T, FlowStat> stats, Iterable<T> contents, arc.func.Func<T, arc.graphics.g2d.TextureRegion> icon, int start) {
        int i = start;
        for (T entry : contents) {
            FlowStat stat = stats.get(entry);
            if (stat == null || stat.mean <= 0f) continue;
            appendFlowEntry(t, icon.get(entry), (int) (stat.mean * 60), i++);
        }
        return i;
    }

    static void appendPayloadMapEntries(Table t, ObjectMap<UnlockableContent, FlowStat> stats, int start) {
        int i = start;
        for (Block block : content.blocks()) {
            FlowStat stat = stats.get(block);
            if (stat == null || stat.mean <= 0f) continue;
            appendFlowEntry(t, block.uiIcon, (int) (stat.mean * 60), i++);
        }
        for (UnitType unit : content.units()) {
            FlowStat stat = stats.get(unit);
            if (stat == null || stat.mean <= 0f) continue;
            appendFlowEntry(t, unit.uiIcon, (int) (stat.mean * 60), i++);
        }
    }

    static <T> void mergeFlowMap(ObjectMap<T, FlowStat> into, ObjectMap<T, FlowStat> from) {
        from.each((key, stat) -> {
            if (stat.mean <= 0f) return;
            FlowStat target = into.get(key);
            if (target == null) {
                target = new FlowStat();
                target.mean = stat.mean;
                into.put(key, target);
            } else {
                target.mean += stat.mean;
            }
        });
    }

    static void appendFlowEntry(Table t, arc.graphics.g2d.TextureRegion icon, int perMinute, int index) {
        t.image(icon).scaling(Scaling.fit).padRight(2).size(iconSmall);
        t.add(UI.formatAmount(perMinute) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(6f).left();
        if ((index + 1) % 3 == 0) t.row();
    }

    static Table buildFlowTable() {
        return new Table().left();
    }
}
