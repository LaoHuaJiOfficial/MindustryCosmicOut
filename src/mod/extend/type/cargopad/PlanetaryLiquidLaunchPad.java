package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;
import mod.extend.type.pad.LiquidLaunchPadBase;
import mod.extend.type.pad.ModLaunchPad;

import static mindustry.Vars.*;

public class PlanetaryLiquidLaunchPad extends LiquidLaunchPadBase {
    public PlanetaryLiquidLaunchPad(String name) {
        super(name);
    }

    @Override
    protected void exportLiquid(Liquid liquid, float volume) {
        PlanetLogistics.handleLiquidExport(state.getPlanet(), liquid, volume);
    }

    public class PlanetaryLiquidLaunchPadBuild extends LiquidLaunchPadBase.LiquidLaunchPadBuild {
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
