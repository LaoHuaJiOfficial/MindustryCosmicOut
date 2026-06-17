package mod.extend.type.sectorpad;

import arc.Core;
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
        protected void buildDestinationConfig(Table table) {
            SectorPadDestination.addConfigButton(table, this::deselect);
        }

        @Override
        public void display(Table table) {
            super.display(table);

            if (!state.isCampaign() || net.client() || team != player.team()) return;

            table.row();
            table.label(() -> {
                Sector dest = state.rules.sector == null ? null : state.rules.sector.info.destination;
                return Core.bundle.format("launch.destination",
                        dest == null || !dest.hasBase() ? Core.bundle.get("sectors.nonelaunch") :
                                "[accent]" + dest.name());
            }).pad(4).wrap().width(200f).left();
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
