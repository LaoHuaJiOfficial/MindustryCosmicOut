package mod.extend.type.sectorpad;

import mindustry.type.Liquid;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.LiquidLandingPadBase;
import mod.extend.type.pad.PadDisplayUI;

import static mindustry.Vars.*;

public class LiquidLandingPad extends LiquidLandingPadBase {
    public LiquidLandingPad(String name) {
        super(name);
    }

    @Override
    protected boolean acceptLiquidConfig(Liquid liquid) {
        return liquid.isOnPlanet(state.getPlanet());
    }

    @Override
    protected void resetLiquidImportTimer(LiquidLandingPadBase.LiquidLandingPadBuild build, Liquid liquid) {
        SectorLogistics.get(state.getSector()).resetLiquidImportTimer(liquid);
    }

    @Override
    protected void syncLiquidImportTimers() {
        SectorLogistics.get(state.getSector()).syncLiquidImportTimers(state.getPlanet(), state.getSector(), landingVolume);
    }

    @Override
    protected void handleLiquidImport(Liquid liquid, float volume) {
        SectorLogistics.handleLiquidImport(state.getSector(), liquid, volume);
    }

    @Override
    protected boolean canRequestLiquidImport(LiquidLandingPadBase.LiquidLandingPadBuild build, Liquid liquid) {
        if (build.isFake()) return true;
        SectorLogisticsData data = SectorLogistics.get(state.getSector());
        return data.getLiquidImportRate(state.getPlanet(), state.getSector(), liquid) > 0f
                && data.liquidImportTimer(liquid) >= 1f;
    }

    @Override
    protected String buildImportSourcesLabel(Liquid liquid) {
        PadDisplayUI.ImportSources sources = PadDisplayUI.sectorLiquidSources(state.getSector(), liquid);
        return PadDisplayUI.formatImportWithSectors(liquid, sources.sectors, sources.perSecond, state.getPlanet());
    }

    public class LiquidLandingPadBuild extends LiquidLandingPadBase.LiquidLandingPadBuild {
    }
}
