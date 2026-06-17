package mod.extend.type.cargopad;

import arc.Core;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.pad.PayloadLandingPadBase;

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
        int sources = 0;
        float perSecond = 0f;
        for (Planet planet : content.planets()) {
            if (planet == state.getPlanet() || !PlanetLogistics.hasBase(planet)) continue;
            PlanetLogisticsData otherData = PlanetLogistics.get(planet);
            if (otherData.destinationPlanet() != state.getPlanet()) continue;
            float amount = otherData.getPayloadExport(unlockable);
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

    public class PlanetaryPayloadLandingPadBuild extends PayloadLandingPadBase.PayloadLandingPadBuild {
    }
}
