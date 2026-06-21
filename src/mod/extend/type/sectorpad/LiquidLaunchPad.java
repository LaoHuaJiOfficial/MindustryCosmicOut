package mod.extend.type.sectorpad;

import arc.scene.ui.layout.Table;
import mindustry.type.Liquid;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.type.pad.LiquidLaunchPadBase;

import static mindustry.Vars.*;

public class LiquidLaunchPad extends LiquidLaunchPadBase {
    public LiquidLaunchPad(String name) {
        super(name);
    }

    @Override
    protected void exportLiquid(Liquid liquid, float volume) {
        SectorLogistics.handleLiquidExport(state.getSector(), liquid, volume);

        Sector dest = state.isCampaign() && state.rules.sector != null ? state.rules.sector.info.destination : null;
        if (dest != null && dest != state.getSector()) {
            SectorLogistics.refreshImportRates(dest.planet, dest);
        }
    }

    public class LiquidLaunchPadBuild extends LiquidLaunchPadBase.LiquidLaunchPadBuild {
        @Override
        protected void buildDestinationConfig(Table table) {
            SectorPadDestination.addConfigButton(table, this::deselect);
        }

        @Override
        protected Object logisticsDestination() {
            return state.isCampaign() && state.rules.sector != null ? state.rules.sector.info.destination : null;
        }
    }
}
