package mod.extend.type.cargopad;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.pad.LiquidLandingPadBase;

import static mindustry.Vars.*;

public class PlanetaryLiquidLandingPad extends LiquidLandingPadBase {
    public PlanetaryLiquidLandingPad(String name) {
        super(name);
    }

    @Override
    protected boolean acceptLiquidConfig(Liquid liquid) {
        return liquid.isOnPlanet(state.getPlanet());
    }

    @Override
    protected void resetLiquidImportTimer(LiquidLandingPadBase.LiquidLandingPadBuild build, Liquid liquid) {
        PlanetLogistics.get(state.getPlanet()).resetLiquidImportTimer(liquid);
    }

    @Override
    protected void syncLiquidImportTimers() {
        PlanetLogistics.get(state.getPlanet()).syncLiquidImportTimers(state.getPlanet(), landingVolume);
    }

    @Override
    protected void handleLiquidImport(Liquid liquid, float volume) {
        PlanetLogistics.handleLiquidImport(state.getPlanet(), liquid, volume);
    }

    @Override
    protected boolean canRequestLiquidImport(LiquidLandingPadBase.LiquidLandingPadBuild build, Liquid liquid) {
        if (build.isFake()) return true;
        PlanetLogisticsData data = PlanetLogistics.get(state.getPlanet());
        return data.getLiquidImportRate(state.getPlanet(), liquid) > 0f
                && data.liquidImportTimer(liquid) >= 1f;
    }

    @Override
    protected String buildImportSourcesLabel(Liquid liquid) {
        int sources = 0;
        float perSecond = 0f;
        for (Planet planet : content.planets()) {
            if (planet == state.getPlanet() || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData otherData = PlanetLogistics.get(planet);
            if (otherData.destinationPlanet() != state.getPlanet()) continue;
            float amount = otherData.getLiquidExport(liquid);
            if (amount <= 0f) continue;
            sources++;
            perSecond += amount;
        }

        String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
        if (perSecond > 0f) {
            str += "\n" + Core.bundle.format("landing.import", liquid.emoji(), (int) (perSecond * 60f));
        }
        return str;
    }

    public class PlanetaryLiquidLandingPadBuild extends LiquidLandingPadBase.LiquidLandingPadBuild {
        protected PlanetLogisticsData logistics() {
            return PlanetLogistics.get(state.getPlanet());
        }
    }
}
