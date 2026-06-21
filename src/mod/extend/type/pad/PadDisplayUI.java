package mod.extend.type.pad;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.world.blocks.campaign.LandingPad;
import mindustry.world.blocks.campaign.LaunchPad;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.sector.PlanetSectorLogisticsData;
import mod.extend.sector.SectorLogistics;
import mod.extend.starmap.HexCoord;
import mod.extend.starmap.StarMapPlanets;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static mindustry.Vars.*;

public final class PadDisplayUI {
    private static final MethodHandle launchBuildingDisplay;
    private static final MethodHandle landingBuildingDisplay;

    static {
        try {
            var launchLookup = MethodHandles.privateLookupIn(LaunchPad.LaunchPadBuild.class, MethodHandles.lookup());
            launchBuildingDisplay = launchLookup.findSpecial(
                    Building.class,
                    "display",
                    MethodType.methodType(void.class, Table.class),
                    LaunchPad.LaunchPadBuild.class
            );

            var landingLookup = MethodHandles.privateLookupIn(LandingPad.LandingPadBuild.class, MethodHandles.lookup());
            landingBuildingDisplay = landingLookup.findSpecial(
                    Building.class,
                    "display",
                    MethodType.methodType(void.class, Table.class),
                    LandingPad.LandingPadBuild.class
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void invokeBuildingDisplay(Building build, Table table) {
        try {
            if (build instanceof LaunchPad.LaunchPadBuild) {
                launchBuildingDisplay.invoke(build, table);
            } else if (build instanceof LandingPad.LandingPadBuild) {
                landingBuildingDisplay.invoke(build, table);
            } else {
                build.display(table);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean shouldShowCampaignLogistics(Building build) {
        if (!state.isCampaign() || net.client() || build.team != player.team()) return false;
        return !(build instanceof LandingPad.LandingPadBuild landing) || !landing.isFake();
    }

    public static void appendInfoRow(Table table, String text) {
        if (text == null || text.isEmpty()) return;
        table.row();
        table.label(() -> text).pad(4).wrap().width(200f).left();
    }

    public static void appendDestination(Table table, @Nullable Object dest) {
        appendInfoRow(table, formatDestination(dest));
    }

    public static void appendPathLength(Table table, @Nullable Planet from, @Nullable Planet to, int maxPath) {
        if (maxPath <= 0) return;
        appendInfoRow(table, formatPathLength(from, to, maxPath));
    }

    public static int planetPathSteps(@Nullable Planet from, @Nullable Planet to) {
        if (from == null || to == null || from == to) return 0;
        var fromData = StarMapPlanets.get(from);
        var toData = StarMapPlanets.get(to);
        if (fromData == null || toData == null) return 0;
        return Math.max(0, HexCoord.path(fromData.hexCoord(), toData.hexCoord()).size - 1);
    }

    public static String formatPathLength(@Nullable Planet from, @Nullable Planet to, int maxPath) {
        String label = Core.bundle.get("stat.maxpath");
        if (to == null || to == from) {
            return label + ": [accent]" + maxPath;
        }
        int steps = planetPathSteps(from, to);
        String stepColor = steps > maxPath ? "[red]" : "[accent]";
        return label + ": " + stepColor + steps + "[lightgray] / [accent]" + maxPath;
    }

    public static String formatDestination(@Nullable Object dest) {
        String label;
        if (dest instanceof Sector sector) {
            label = sector.hasBase() ? "[accent]" + sector.name() : Core.bundle.get("sectors.nonelaunch");
        } else if (dest instanceof Planet planet) {
            label = PlanetLogistics.hasBase(planet) ? "[accent]" + planet.localizedName : Core.bundle.get("sectors.nonelaunch");
        } else {
            label = Core.bundle.get("sectors.nonelaunch");
        }
        return Core.bundle.format("launch.destination", label);
    }

    public static String legacyDisabledLabel() {
        return Core.bundle.get("landingpad.legacy.disabled");
    }

    public static String formatImportWithSectors(UnlockableContent unlockable, Seq<Sector> sources, float perSecond, Planet contextPlanet) {
        String str = Core.bundle.format("landing.sources", formatSourcesLabel(sources, contextPlanet));
        if (perSecond > 0f) {
            str += "\n" + Core.bundle.format("landing.import", unlockable.emoji(), (int) (perSecond * 60f));
        }
        return str;
    }

    static String formatSourcesLabel(Seq<Sector> sources, Planet contextPlanet) {
        if (sources.isEmpty()) return Core.bundle.get("none");
        StringBuilder sb = new StringBuilder();
        for (Sector sector : sources) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(formatSectorName(sector, contextPlanet));
        }
        return sb.toString();
    }

    static String formatSectorName(Sector sector, Planet contextPlanet) {
        String ic = sector.iconChar();
        String prefix = ic == null || ic.isEmpty() ? "" : ic + " ";
        String name = prefix + sector.name();
        if (contextPlanet != null && sector.planet != contextPlanet) {
            name = sector.planet.localizedName + " / " + name;
        }
        return name;
    }

    public static ImportSources sectorItemSources(Sector self, Item item) {
        ImportSources result = new ImportSources();
        for (Sector other : self.planet.sectors) {
            if (other == self || !other.hasBase() || other.info.destination != self) continue;
            float amount = other.info.getExport(item);
            if (amount <= 0f) continue;
            result.add(other, amount);
        }
        return result;
    }

    public static ImportSources sectorLiquidSources(Sector self, Liquid liquid) {
        ImportSources result = new ImportSources();
        for (Sector other : self.planet.sectors) {
            if (other == self || !other.hasBase() || other.info.destination != self) continue;
            float amount = SectorLogistics.get(other).getLiquidExport(liquid);
            if (amount <= 0f) continue;
            result.add(other, amount);
        }
        return result;
    }

    public static ImportSources sectorPayloadSources(Sector self, UnlockableContent unlockable) {
        ImportSources result = new ImportSources();
        for (Sector other : self.planet.sectors) {
            if (other == self || !other.hasBase() || other.info.destination != self) continue;
            float amount = SectorLogistics.get(other).getPayloadExport(unlockable);
            if (amount <= 0f) continue;
            result.add(other, amount);
        }
        return result;
    }

    public static ImportSources planetaryItemSources(Planet self, Item item) {
        ImportSources result = new ImportSources();
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData planetData = PlanetLogistics.get(planet);
            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = planetData.getSector(sector);
                if (sectorData.destinationPlanet() != self) continue;
                float amount = sectorData.getItemExport(item);
                if (amount <= 0f) continue;
                result.add(sector, amount);
            }
        }
        return result;
    }

    public static ImportSources planetaryLiquidSources(Planet self, Liquid liquid) {
        ImportSources result = new ImportSources();
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData planetData = PlanetLogistics.get(planet);
            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = planetData.getSector(sector);
                if (sectorData.destinationPlanet() != self) continue;
                float amount = sectorData.getLiquidExport(liquid);
                if (amount <= 0f) continue;
                result.add(sector, amount);
            }
        }
        return result;
    }

    public static ImportSources planetaryPayloadSources(Planet self, UnlockableContent unlockable) {
        ImportSources result = new ImportSources();
        for (Planet planet : content.planets()) {
            if (planet == self || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData planetData = PlanetLogistics.get(planet);
            for (Sector sector : planet.sectors) {
                if (!sector.hasBase()) continue;
                PlanetSectorLogisticsData sectorData = planetData.getSector(sector);
                if (sectorData.destinationPlanet() != self) continue;
                float amount = sectorData.getPayloadExport(unlockable);
                if (amount <= 0f) continue;
                result.add(sector, amount);
            }
        }
        return result;
    }

    public static final class ImportSources {
        public final Seq<Sector> sectors = new Seq<>();
        public float perSecond;

        public void add(Sector sector, float amount) {
            if (!sectors.contains(sector)) sectors.add(sector);
            perSecond += amount;
        }
    }
}
