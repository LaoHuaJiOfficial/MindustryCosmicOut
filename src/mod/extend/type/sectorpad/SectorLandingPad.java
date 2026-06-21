package mod.extend.type.sectorpad;

import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.ModLandingPad;
import mod.extend.type.pad.PadDisplayUI;

import static mindustry.Vars.*;

public class SectorLandingPad extends ModLandingPad {
    public SectorLandingPad(String name) {
        super(name);
    }

    public class SectorLandingPadBuild extends ModLandingPadBuild {
        protected SectorLogisticsData logistics() {
            return SectorLogistics.get(state.getSector());
        }

        @Override
        protected String buildImportDisplayLabel() {
            if (config == null) return null;
            PadDisplayUI.ImportSources sources = PadDisplayUI.sectorItemSources(state.getSector(), config);
            return PadDisplayUI.formatImportWithSectors(config, sources.sectors, sources.perSecond, state.getPlanet());
        }
    }
}
