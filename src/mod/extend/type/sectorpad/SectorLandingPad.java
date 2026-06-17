package mod.extend.type.sectorpad;

import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.ModLandingPad;

import static mindustry.Vars.*;

public class SectorLandingPad extends ModLandingPad {
    public SectorLandingPad(String name) {
        super(name);
    }

    public class SectorLandingPadBuild extends ModLandingPadBuild {
        protected SectorLogisticsData logistics() {
            return SectorLogistics.get(state.getSector());
        }
    }
}
