package mod.extend.type.cargopad;

import mindustry.type.Liquid;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.pad.LiquidLandingPadBase;
import mod.extend.type.pad.PadDisplayUI;

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
        PadDisplayUI.ImportSources sources = PadDisplayUI.planetaryLiquidSources(state.getPlanet(), liquid);
        return PadDisplayUI.formatImportWithSectors(liquid, sources.sectors, sources.perSecond, state.getPlanet());
    }

    public class PlanetaryLiquidLandingPadBuild extends LiquidLandingPadBase.LiquidLandingPadBuild {
        protected PlanetLogisticsData logistics() {
            return PlanetLogistics.get(state.getPlanet());
        }
    }
}
