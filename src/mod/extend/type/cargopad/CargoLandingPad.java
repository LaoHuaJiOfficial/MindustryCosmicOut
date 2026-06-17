package mod.extend.type.cargopad;

import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.pad.ModLandingPad;

import static mindustry.Vars.*;

public class CargoLandingPad extends ModLandingPad {
    public CargoLandingPad(String name) {
        super(name);
    }

    public class CargoLandingPadBuild extends ModLandingPadBuild {
        protected PlanetLogisticsData logistics() {
            return PlanetLogistics.get(state.getPlanet());
        }
    }
}
