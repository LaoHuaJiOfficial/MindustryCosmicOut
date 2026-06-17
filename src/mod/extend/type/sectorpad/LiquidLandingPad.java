package mod.extend.type.sectorpad;

import arc.Core;
import mindustry.type.Liquid;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.LiquidLandingPadBase;

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
        int sources = 0;
        float perSecond = 0f;
        for (Sector other : state.getPlanet().sectors) {
            if (other == state.getSector() || !other.hasBase() || other.info.destination != state.getSector()) continue;
            float amount = SectorLogistics.get(other).getLiquidExport(liquid);
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

    public class LiquidLandingPadBuild extends LiquidLandingPadBase.LiquidLandingPadBuild {
    }
}
