package mod.extend.type.cargopad;

import mindustry.type.ItemSeq;
import mindustry.type.Planet;
import mod.extend.sector.PlanetLogistics;

import static mindustry.Vars.*;

public class PlanetaryItemLaunchPad extends CargoLaunchPad {
    public PlanetaryItemLaunchPad(String name) {
        super(name);
    }

    public class PlanetaryItemLaunchPadBuild extends CargoLaunchPadBuild {
        @Override
        public void updateTile() {
            if (!readyToLaunch() || items.total() < itemCapacity) return;

            Planet dest = destination();
            items.each((item, amount) -> PlanetLogistics.handleItemExport(state.getPlanet(), item, amount));

            if (dest != null && dest != state.getPlanet()) {
                PlanetLogistics.refreshImportRates(dest);
            }

            if (state.isCampaign() && dest != null && dest != state.getPlanet()
                    && state.getPlanet().campaignRules.legacyLaunchPads) {
                ItemSeq exported = new ItemSeq();
                items.each(exported::add);
                PlanetLogistics.addItems(dest, exported);
            }

            fireLaunchVisual();
            items.clear();
        }
    }
}
