package mod.extend.type.sectorpad;

import arc.Core;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;
import mod.extend.type.pad.PayloadLandingPadBase;

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
        int sources = 0;
        float perSecond = 0f;
        for (Sector other : state.getPlanet().sectors) {
            if (other == state.getSector() || !other.hasBase() || other.info.destination != state.getSector()) continue;
            float amount = SectorLogistics.get(other).getPayloadExport(unlockable);
            if (amount <= 0f) continue;
            sources++;
            perSecond += amount;
        }

        String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
        if (perSecond > 0f) {
            str += "\n" + Core.bundle.format("landing.import", unlockable.emoji(), (int) (perSecond * 60f));
        }
        return str;
    }

    public class PayloadLandingPadBuild extends PayloadLandingPadBase.PayloadLandingPadBuild {
    }
}
