package mod.extend;

import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.dialogs.BaseDialog;

public class PlanetLogisticsStatsDialog extends BaseDialog {
    Planet planet;

    public PlanetLogisticsStatsDialog() {
        super("");
        addCloseButton();
    }

    public void show(Planet planet, Sector sector) {
        this.planet = planet;
        title.setText(planet.localizedName);
        rebuild();
        super.show();
    }

    void rebuild() {
        cont.clear();
        cont.pane(c -> StarMapLogisticsUI.buildPlanetLogisticsStats(c, planet));
    }
}
