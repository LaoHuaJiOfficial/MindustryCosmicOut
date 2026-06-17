package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;
import mod.extend.type.pad.ModLaunchPad;

import static mindustry.Vars.*;

public class CargoLaunchPad extends ModLaunchPad {
    public CargoLaunchPad(String name) {
        super(name);
    }

    public class CargoLaunchPadBuild extends ModLaunchPadBuild {
        protected @Nullable Planet destination() {
            return state.isCampaign() ? PlanetLogistics.get(state.getPlanet()).destinationPlanet() : null;
        }

        @Override
        protected void buildDestinationConfig(Table table) {
            CargoPadDestination.addConfigButton(table, this::deselect);
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
