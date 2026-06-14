package mod.extend;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Iconc;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import mindustry.core.UI;
import mindustry.world.Block;
import mod.extend.sector.FlowStat;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.ModPlanet;

import static mindustry.Vars.*;

public class StarMapLogisticsUI {
    public static void buildStats(Table parent, Sector sector) {
        parent.defaults().left().padBottom(4);

        if (sector.preset != null && sector.preset.description != null) {
            parent.add(sector.preset.displayDescription()).width(260f).wrap().left().row();
        }

        if (sector.save != null) {
            parent.add(Core.bundle.get("sectors.time") + " [accent]" + sector.save.getPlayTime()).left().row();
        }

        if (sector.info.attempts > 0) {
            parent.add(Core.bundle.get("sectors.attempts") + " [accent]" + sector.info.attempts).left().row();
        }

        if (sector.info.waves && sector.hasBase()) {
            parent.add(Core.bundle.get("sectors.wave") + " [accent]" + sector.info.wave).left().row();
        }

        if (sector.isAttacked() || !sector.hasBase()) {
            parent.add(Core.bundle.get("sectors.threat") + " [accent]" + sector.displayThreat()).left().row();
        }

        if (sector.save != null && sector.info.resources.any()) {
            parent.add("@sectors.resources").left().row();
            parent.table(t -> {
                for (UnlockableContent uc : sector.info.resources) {
                    if (uc == null) continue;
                    t.image(uc.uiIcon).scaling(Scaling.fit).padRight(3).size(iconSmall);
                }
            }).padLeft(10f).left().row();
        }

        if (sector.isAttacked() && !sector.isBeingPlayed()) {
            parent.add(UI.formatIcons(Core.bundle.get("sector.lockdown"))).wrap().width(260f).padBottom(6f).row();
        }

        SectorLogisticsData data = SectorLogistics.get(sector);

        if (hasExport(data)) {
            parent.add("@sectors.export").left().row();
            appendDestination(parent, sector);
            addItemFlows(parent, data.itemExport);
            addLiquidFlows(parent, data.liquidExport);
            addPayloadFlows(parent, data.payloadExport);
        }

        if (sector.hasBase() && data.anyIncomingImports(sector.planet, sector)) {
            parent.add("@sectors.import").left().row();
            appendImportSources(parent, sector);
            addItemImportFlows(parent, sector, data);
            addLiquidImportFlows(parent, sector, data);
            addPayloadImportFlows(parent, sector, data);
        }

        if (sector.hasBase() && data.items.total > 0) {
            parent.add("@sectors.stored").left().row();
            parent.table(t -> {
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

    static boolean hasExport(SectorLogisticsData data) {
        return hasFlow(data.itemExport) || hasFlow(data.liquidExport) || hasFlow(data.payloadExport);
    }

    static void addItemImportFlows(Table parent, Sector sector, SectorLogisticsData data) {
        Planet planet = sector.planet;
        Table t = buildFlowTable();
        int i = 0;
        for (Item item : content.items()) {
            int total = (int) (data.getItemImportRate(planet, sector, item) * 60);
            if (total <= 0) continue;
            t.image(item.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addLiquidImportFlows(Table parent, Sector sector, SectorLogisticsData data) {
        Planet planet = sector.planet;
        Table t = buildFlowTable();
        int i = 0;
        for (Liquid liquid : content.liquids()) {
            int total = (int) (data.getLiquidImportRate(planet, sector, liquid) * 60);
            if (total <= 0) continue;
            t.image(liquid.uiIcon).padRight(3);
            t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
            if (++i % 2 == 0) t.row();
        }
        appendFlows(parent, t);
    }

    static void addPayloadImportFlows(Table parent, Sector sector, SectorLogisticsData data) {
        Planet planet = sector.planet;
        data.refreshPayloadImportRates(planet, sector);
        Table t = buildFlowTable();
        int i = 0;
        for (UnlockableContent content : data.payloadImportKeys) {
            int total = (int) (data.getPayloadImportRate(planet, sector, content) * 60);
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

    static void appendDestination(Table c, Sector sector) {
        Sector dest = sector.info.destination;
        if (dest == null) return;
        c.add(formatSectorLink(dest)).padLeft(10f).left().row();
    }

    static void appendImportSources(Table c, Sector sector) {
        Seq<Sector> shown = new Seq<>();
        SectorLogisticsData data = SectorLogistics.get(sector);
        data.eachItemSource(sector.planet, sector, s -> addImportSource(shown, c, s));
        data.eachLiquidSource(sector.planet, sector, s -> addImportSource(shown, c, s));
        data.eachPayloadSource(sector.planet, sector, s -> addImportSource(shown, c, s));
    }

    static void addImportSource(Seq<Sector> shown, Table c, Sector source) {
        if (shown.contains(source)) return;
        shown.add(source);
        c.add(formatSectorLink(source)).padLeft(10f).left().row();
    }

    static String formatSectorLink(Sector sector) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        if (sector.planet instanceof ModPlanet) {
            return Iconc.rightOpen + " " + prefix + sector.planet.localizedName;
        }
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
