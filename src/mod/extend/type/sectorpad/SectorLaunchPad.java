package mod.extend.type.sectorpad;

import arc.scene.ui.layout.Table;
import mindustry.type.Sector;
import mod.extend.type.pad.ModLaunchPad;

import static mindustry.Vars.*;

public class SectorLaunchPad extends ModLaunchPad {
    public SectorLaunchPad(String name) {
        super(name);
    }

    public class SectorLaunchPadBuild extends ModLaunchPadBuild {
        protected Sector destination() {
            return state.isCampaign() && state.rules.sector != null ? state.rules.sector.info.destination : null;
        }

        @Override
        protected Object logisticsDestination() {
            return destination();
        }

        @Override
        protected void buildDestinationConfig(Table table) {
            SectorPadDestination.addConfigButton(table, this::deselect);
        }

        @Override
        public void buildConfiguration(Table table) {
            if (!state.isCampaign() || net.client()) {
                deselect();
                return;
            }

            buildDestinationConfig(table);
        }
    }
}
