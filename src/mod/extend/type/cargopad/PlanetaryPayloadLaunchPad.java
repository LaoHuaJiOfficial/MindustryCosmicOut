package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;
import mod.extend.type.pad.ModLaunchPad;
import mod.extend.type.pad.PayloadLaunchPadBase;

import static mindustry.Vars.*;

public class PlanetaryPayloadLaunchPad extends PayloadLaunchPadBase {
    public PlanetaryPayloadLaunchPad(String name) {
        super(name);
    }

    @Override
    protected void exportPayload(UnlockableContent config, int count) {
        PlanetLogistics.handlePayloadExport(state.getPlanet(), config, count);
    }

    public class PlanetaryPayloadLaunchPadBuild extends PayloadLaunchPadBase.PayloadLaunchPadBuild {
        @Override
        protected void buildDestinationConfig(Table table) {
            CargoPadDestination.addConfigButton(table, (ModLaunchPad) block, this::deselect);
        }

        @Override
        protected Object logisticsDestination() {
            return state.isCampaign() && state.rules.sector != null
                    ? PlanetLogistics.get(state.getPlanet()).destinationPlanet(state.rules.sector) : null;
        }
    }
}
