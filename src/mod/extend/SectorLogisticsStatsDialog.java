package mod.extend;

import mindustry.gen.Icon;
import mindustry.type.Sector;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.ui;

public class SectorLogisticsStatsDialog extends BaseDialog {
    Sector sector;

    public SectorLogisticsStatsDialog() {
        super("");
        addCloseButton();
    }

    public void show(Sector sector) {
        this.sector = sector;
        title.setText(sector.name());
        rebuild();
        super.show();
    }

    void rebuild() {
        cont.clear();
        cont.pane(c -> StarMapLogisticsUI.buildSectorStats(c, sector));

        buttons.clearChildren();
        addCloseButton();
        if (sector.hasBase()) {
            buttons.button("@sector.abandon", Icon.cancel, () -> ui.planet.abandonSectorConfirm(sector, this::hide));
        }
    }
}
