package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mod.ModUI;
import mod.extend.sector.PlanetLogistics;
import mod.extend.type.pad.ModLaunchPad;

import static mindustry.Vars.*;

public final class CargoPadDestination {
    private CargoPadDestination() {
    }

    public static void addConfigButton(Table table, ModLaunchPad block, Runnable onDone) {
        table.button(Icon.upOpen, Styles.cleari, () -> {
            ModUI.starmap.showSectorSelect(state.getSector(), dest -> {
                if (!state.isCampaign() || dest == null) return;

                var data = PlanetLogistics.get(state.getPlanet());
                var prev = data.destinationPlanet(state.rules.sector);
                data.setDestination(state.rules.sector, dest.planet);
                PlanetLogistics.flushStats(state.rules.sector);
                PlanetLogistics.save(state.getPlanet());
                if (prev != null) PlanetLogistics.refreshImportRates(prev);
                PlanetLogistics.refreshImportRates(dest.planet);
            }, block.maxPath);
            onDone.run();
        }).size(40f);
    }
}
