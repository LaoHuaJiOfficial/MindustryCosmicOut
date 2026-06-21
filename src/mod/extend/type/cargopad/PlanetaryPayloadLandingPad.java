package mod.extend.type.cargopad;

import mindustry.ctype.UnlockableContent;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.pad.PayloadLandingPadBase;
import mod.extend.type.pad.PadDisplayUI;

import static mindustry.Vars.*;

public class PlanetaryPayloadLandingPad extends PayloadLandingPadBase {
    public PlanetaryPayloadLandingPad(String name) {
        super(name);
    }

    @Override
    protected void resetPayloadImportTimer(PayloadLandingPadBase.PayloadLandingPadBuild build, UnlockableContent unlockable) {
        PlanetLogistics.get(state.getPlanet()).resetPayloadImportTimer(unlockable);
    }

    @Override
    protected void syncPayloadImportTimers() {
        PlanetLogistics.get(state.getPlanet()).syncPayloadImportTimers(state.getPlanet(), 1);
    }

    @Override
    protected void handlePayloadImport(UnlockableContent unlockable, int amount) {
        PlanetLogistics.handlePayloadImport(state.getPlanet(), unlockable, amount);
    }

    @Override
    protected boolean canRequestPayloadImport(PayloadLandingPadBase.PayloadLandingPadBuild build, UnlockableContent unlockable) {
        if (build.isFake()) return true;
        PlanetLogisticsData data = PlanetLogistics.get(state.getPlanet());
        return data.getPayloadImportRate(state.getPlanet(), unlockable) > 0f
                && data.payloadImportTimer(unlockable) >= 1f;
    }

    @Override
    protected String buildImportSourcesLabel(UnlockableContent unlockable) {
        PadDisplayUI.ImportSources sources = PadDisplayUI.planetaryPayloadSources(state.getPlanet(), unlockable);
        return PadDisplayUI.formatImportWithSectors(unlockable, sources.sectors, sources.perSecond, state.getPlanet());
    }

    public class PlanetaryPayloadLandingPadBuild extends PayloadLandingPadBase.PayloadLandingPadBuild {
    }
}
