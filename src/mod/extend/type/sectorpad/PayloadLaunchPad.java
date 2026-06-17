package mod.extend.type.sectorpad;

import arc.scene.ui.layout.Table;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.type.pad.PayloadLaunchPadBase;

import static mindustry.Vars.*;

public class PayloadLaunchPad extends PayloadLaunchPadBase {
    public PayloadLaunchPad(String name) {
        super(name);
        rotate = true;
    }

    @Override
    protected void exportPayload(UnlockableContent config, int count) {
        SectorLogistics.handlePayloadExport(state.getSector(), config, count);

        Sector dest = state.isCampaign() && state.rules.sector != null ? state.rules.sector.info.destination : null;
        if (dest != null && dest != state.getSector()) {
            SectorLogistics.refreshImportRates(dest.planet, dest);
        }
    }

    public class PayloadLaunchPadBuild extends PayloadLaunchPadBase.PayloadLaunchPadBuild {
        @Override
        protected void buildDestinationConfig(Table table) {
            SectorPadDestination.addConfigButton(table, this::deselect);
        }
    }
}
