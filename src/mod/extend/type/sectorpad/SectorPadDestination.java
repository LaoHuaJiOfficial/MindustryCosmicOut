package mod.extend.type.sectorpad;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mod.extend.sector.SectorLogistics;

import static mindustry.Vars.*;

public final class SectorPadDestination {
    private SectorPadDestination() {
    }

    public static void addConfigButton(Table table, Runnable onDone) {
        table.button(Icon.upOpen, Styles.cleari, () -> {
            ui.planet.showSelect(state.rules.sector, other -> {
                if (state.isCampaign() && other.planet == state.rules.sector.planet) {
                    SectorLogistics.setDestination(state.rules.sector, other);
                }
            });
            onDone.run();
        }).size(40f);
    }
}
