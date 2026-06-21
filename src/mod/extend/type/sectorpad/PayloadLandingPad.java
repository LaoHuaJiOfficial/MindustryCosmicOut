package mod.extend.type.sectorpad;

import mindustry.ctype.UnlockableContent;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.PayloadLandingPadBase;
import mod.extend.type.pad.PadDisplayUI;

import static mindustry.Vars.*;

public class PayloadLandingPad extends PayloadLandingPadBase {
    public PayloadLandingPad(String name) {
        super(name);
    }

    @Override
    protected void resetPayloadImportTimer(PayloadLandingPadBase.PayloadLandingPadBuild build, UnlockableContent unlockable) {
        SectorLogistics.get(state.getSector()).resetPayloadImportTimer(unlockable);
    }

    @Override
    protected void syncPayloadImportTimers() {
        SectorLogistics.get(state.getSector()).syncPayloadImportTimers(state.getPlanet(), state.getSector(), 1);
    }

    @Override
    protected void handlePayloadImport(UnlockableContent unlockable, int amount) {
        SectorLogistics.handlePayloadImport(state.getSector(), unlockable, amount);
    }

    @Override
    protected boolean canRequestPayloadImport(PayloadLandingPadBase.PayloadLandingPadBuild build, UnlockableContent unlockable) {
        if (build.isFake()) return true;
        SectorLogisticsData data = SectorLogistics.get(state.getSector());
        return data.getPayloadImportRate(state.getPlanet(), state.getSector(), unlockable) > 0f
                && data.payloadImportTimer(unlockable) >= 1f;
    }

    @Override
    protected String buildImportSourcesLabel(UnlockableContent unlockable) {
        PadDisplayUI.ImportSources sources = PadDisplayUI.sectorPayloadSources(state.getSector(), unlockable);
        return PadDisplayUI.formatImportWithSectors(unlockable, sources.sectors, sources.perSecond, state.getPlanet());
    }

    public class PayloadLandingPadBuild extends PayloadLandingPadBase.PayloadLandingPadBuild {
    }
}
