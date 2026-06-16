package mod.extend;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.game.SectorInfo.ExportStat;
import mindustry.gen.Iconc;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.type.UnitType;
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
        SectorLogisticsData data = SectorLogistics.get(sector);

        displayExportStats(c, sector.info.production, "@sectors.production");

        Table itemExportTable = buildItemExportTable(sector.info.export);
        boolean hasLogisticsExport = hasSectorExport(data);
        if (itemExportTable != null || hasLogisticsExport) {
            c.add("@sectors.export").left().row();
            if (sector.info.destination != null && sector.info.destination.hasBase()) {
                c.add(formatSectorLink(sector.info.destination)).padLeft(10f).left().row();
            }
            if (itemExportTable != null) {
                c.add(itemExportTable).padLeft(10f).row();
            }
            if (hasLogisticsExport) {
                addLiquidFlows(c, data.liquidExport);
                addPayloadFlows(c, data.payloadExport);
            }
        }

        Table itemImportTable = buildItemExportTable(sector.info.imports);
        boolean hasLogisticsImport = sector.hasBase() && data.anyIncomingImports(sector.planet, sector);
        boolean hasItemImport = sector.hasBase() && itemImportTable != null;
        if (hasItemImport || hasLogisticsImport) {
            c.add("@sectors.import").left().row();
            appendMergedSectorImportSources(c, sector, hasItemImport, hasLogisticsImport);
            if (itemImportTable != null) {
                c.add(itemImportTable).padLeft(10f).row();
            }
            if (hasLogisticsImport) {
                addSectorLiquidImportFlows(c, sector, data);
                addSectorPayloadImportFlows(c, sector, data);
            }
        }

        var items = sector.items();
        if (sector.hasBase() && items.total > 0) {
            c.add("@sectors.stored").left().row();
            c.table(t -> {
                t.left();
                t.table(res -> {
                    int i = 0;
                    for (ItemStack stack : items) {
                        res.image(stack.item.uiIcon).padRight(3);
                        res.add(UI.formatAmount(Math.max(stack.amount, 0))).color(Color.lightGray);
                        if (++i % 4 == 0) res.row();
                    }
                }).padLeft(10f);
            }).left().row();
        }
    }

    public static void buildPlanetLogisticsStats(Table c, Planet planet) {
        c.defaults().left().padBottom(4);

        PlanetLogistics.refreshImportRates(planet);
        PlanetLogisticsData data = PlanetLogistics.get(planet);

        if (hasPlanetExport(data, planet)) {
            c.add("@sectors.export").left().row();
            appendPlanetDestination(c, planet);

            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = data.getSector(sector);
                if (!hasPlanetSectorExport(sectorData)) continue;
                appendSectorHeader(c, sector);
                addItemFlows(c, sectorData.itemExport);
                addLiquidFlows(c, sectorData.liquidExport);
                addPayloadFlows(c, sectorData.payloadExport);
            }
        }

        if (PlanetLogistics.hasBase(planet) && data.anyIncomingImports(planet)) {
            c.add("@sectors.import").left().row();
            appendPlanetImportSources(c, planet);

            boolean hasSectorImport = false;
            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = data.getSector(sector);
                if (!hasPlanetSectorImport(sectorData)) continue;
                hasSectorImport = true;
                appendSectorHeader(c, sector);
                addItemFlows(c, sectorData.itemImport);
                addLiquidFlows(c, sectorData.liquidImport);
                addPayloadFlows(c, sectorData.payloadImport);
            }

            if (!hasSectorImport) {
                addPlanetItemImportFlows(c, planet, data);
                addPlanetLiquidImportFlows(c, planet, data);
                addPlanetPayloadImportFlows(c, planet, data);
            }
        }

        if (PlanetLogistics.hasBase(planet) && data.items.total > 0) {
            c.add("@sectors.stored").left().row();
            c.table(t -> {
                t.left();
                t.table(res -> {
                    int[] i = {0};
                    data.items.each((item, amount) -> {
                        if (amount <= 0) return;
                        res.image(item.uiIcon).padRight(3);
                        res.add(UI.formatAmount(amount)).color(Color.lightGray);
                        if (++i[0] % 3 == 0) res.row();
                    });
                }).padLeft(10f);
            }).left().row();
        }
    }

    static Table buildItemExportTable(ObjectMap<Item, ExportStat> stats) {
        Table t = new Table().left();
        int i = 0;
        for (Item item : content.items()) {
            ExportStat stat = stats.get(item);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 1) continue;
            t.image(item.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 3 == 0) t.row();
        }
        return t.getChildren().any() ? t : null;
    }

    static void appendMergedSectorImportSources(Table c, Sector sector, boolean hasItemImport, boolean hasLogisticsImport) {
        Seq<Sector> shown = new Seq<>();
        if (hasItemImport) {
            sector.info.eachImport(sector.planet, other -> addSectorImportSource(shown, c, other));
        }
        if (hasLogisticsImport) {
            SectorLogisticsData data = SectorLogistics.get(sector);
            data.eachLiquidSource(sector.planet, sector, s -> addSectorImportSource(shown, c, s));
            data.eachPayloadSource(sector.planet, sector, s -> addSectorImportSource(shown, c, s));
        }
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
            t.image(item.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 3 == 0) t.row();
        }

        if (t.getChildren().any()) {
            c.defaults().left();
            c.add(name).row();
            builder.get(c);
            c.add(t).padLeft(10f).row();
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

    static boolean hasPlanetSectorImport(PlanetSectorLogisticsData data) {
        return hasFlow(data.itemImport) || hasFlow(data.liquidImport) || hasFlow(data.payloadImport);
    }

    static void appendSectorHeader(Table c, Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        c.add("[lightgray]" + prefix + sector.name()).padLeft(6f).left().row();
    }

    static void addPlanetItemImportFlows(Table parent, Planet planet, PlanetLogisticsData data) {
        Table t = buildFlowTable();
        int i = 0;
        for (Item item : content.items()) {
            int total = (int) (data.getItemImportRate(planet, item) * 60);
            if (total <= 0) continue;
            t.image(item.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addPlanetLiquidImportFlows(Table parent, Planet planet, PlanetLogisticsData data) {
        Table t = buildFlowTable();
        int i = 0;
        for (Liquid liquid : content.liquids()) {
            int total = (int) (data.getLiquidImportRate(planet, liquid) * 60);
            if (total <= 0) continue;
            t.image(liquid.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addPlanetPayloadImportFlows(Table parent, Planet planet, PlanetLogisticsData data) {
        data.refreshPayloadImportRates(planet);
        Table t = buildFlowTable();
        int i = 0;
        for (UnlockableContent content : data.payloadImportKeys) {
            int total = (int) (data.getPayloadImportRate(planet, content) * 60);
            if (total <= 0) continue;
            t.image(content.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addSectorLiquidImportFlows(Table parent, Sector sector, SectorLogisticsData data) {
        Table t = buildFlowTable();
        int i = 0;
        for (Liquid liquid : content.liquids()) {
            int total = (int) (data.getLiquidImportRate(sector.planet, sector, liquid) * 60);
            if (total <= 0) continue;
            t.image(liquid.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addSectorPayloadImportFlows(Table parent, Sector sector, SectorLogisticsData data) {
        data.refreshPayloadImportRates(sector.planet, sector);
        Table t = buildFlowTable();
        int i = 0;
        for (UnlockableContent content : data.payloadImportKeys) {
            int total = (int) (data.getPayloadImportRate(sector.planet, sector, content) * 60);
            if (total <= 0) continue;
            t.image(content.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static <T> boolean hasFlow(ObjectMap<T, FlowStat> map) {
        boolean[] found = {false};
        map.each((k, stat) -> {
            if (stat.mean > 0.01f) found[0] = true;
        });
        return found[0];
    }

    static void appendPlanetDestination(Table c, Planet planet) {
        Planet dest = PlanetLogistics.get(planet).destinationPlanet();
        if (dest == null) return;
        c.add(formatPlanetLink(dest)).padLeft(10f).left().row();
    }

    static void appendPlanetImportSources(Table c, Planet planet) {
        Seq<Planet> shown = new Seq<>();
        PlanetLogisticsData data = PlanetLogistics.get(planet);
        data.eachItemSource(planet, p -> addPlanetImportSource(shown, c, p));
        data.eachLiquidSource(planet, p -> addPlanetImportSource(shown, c, p));
        data.eachPayloadSource(planet, p -> addPlanetImportSource(shown, c, p));
    }

    static void addPlanetImportSource(Seq<Planet> shown, Table c, Planet source) {
        if (shown.contains(source)) return;
        shown.add(source);
        c.add(formatPlanetLink(source)).padLeft(10f).left().row();
    }

    static void addSectorImportSource(Seq<Sector> shown, Table c, Sector source) {
        if (shown.contains(source)) return;
        shown.add(source);
        c.add(formatSectorLink(source)).padLeft(10f).left().row();
    }

    static String formatPlanetLink(Planet planet) {
        return Iconc.rightOpen + " " + planet.localizedName;
    }

    static String formatSectorLink(Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        return Iconc.rightOpen + " " + prefix + sector.name();
    }

    static void addItemFlows(Table parent, ObjectMap<Item, FlowStat> stats) {
        Table t = buildFlowTable();
        int i = 0;
        for (Item item : content.items()) {
            FlowStat stat = stats.get(item);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 0) continue;
            t.image(item.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addLiquidFlows(Table parent, ObjectMap<Liquid, FlowStat> stats) {
        Table t = buildFlowTable();
        int i = 0;
        for (Liquid liquid : content.liquids()) {
            FlowStat stat = stats.get(liquid);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 0) continue;
            t.image(liquid.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addPayloadFlows(Table parent, ObjectMap<UnlockableContent, FlowStat> stats) {
        Table t = buildFlowTable();
        int i = 0;
        for (Block block : content.blocks()) {
            FlowStat stat = stats.get(block);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 0) continue;
            t.image(block.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        for (UnitType unit : content.units()) {
            FlowStat stat = stats.get(unit);
            if (stat == null) continue;
            int total = (int) (stat.mean * 60);
            if (total <= 0) continue;
            t.image(unit.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static Table buildFlowTable() {
        return new Table().left();
    }

    static void appendFlows(Table parent, Table t) {
        if (t.getChildren().any()) {
            parent.add(t).padLeft(10f).left().row();
        }
    }
}
