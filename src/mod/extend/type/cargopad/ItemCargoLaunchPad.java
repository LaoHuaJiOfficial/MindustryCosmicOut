package mod.extend.type.cargopad;

import mindustry.type.ItemSeq;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;

import static mindustry.Vars.*;

public class ItemCargoLaunchPad extends CargoLaunchPad {
    public ItemCargoLaunchPad(String name) {
        super(name);
    }

    public class ItemCargoLaunchPadBuild extends CargoLaunchPadBuild {
        @Override
        public void updateTile() {
            if (!readyToLaunch() || items.total() < itemCapacity) return;

            Sector dest = destination();
            items.each((item, amount) -> SectorLogistics.handleItemExport(state.getSector(), item, amount));

            if (dest != null && dest != state.getSector()) {
                SectorLogistics.refreshImportRates(dest.planet, dest);
            }

            if (state.isCampaign() && dest != null && dest != state.getSector()
                    && state.getPlanet().campaignRules.legacyLaunchPads) {
                ItemSeq exported = new ItemSeq();
                items.each(exported::add);
                SectorLogistics.addItems(dest, exported);
            }

            fireLaunchVisual();
            items.clear();
        }
    }
}
