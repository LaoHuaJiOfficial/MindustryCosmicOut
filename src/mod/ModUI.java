package mod;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.type.Sector;
import mindustry.ui.dialogs.PlanetDialog;
import mod.extend.PlanetLogisticsStatsDialog;
import mod.extend.SectorLogisticsStatsDialog;
import mod.extend.StarMapDialog;

import static mindustry.Vars.ui;
import static mindustry.ui.dialogs.PlanetDialog.Mode.look;

public class ModUI {
    public static StarMapDialog starmap;
    public static SectorLogisticsStatsDialog sectorLogisticsStats;
    public static PlanetLogisticsStatsDialog planetLogisticsStats;

    public static void init() {
        starmap = new StarMapDialog();
        sectorLogisticsStats = new SectorLogisticsStatsDialog();
        planetLogisticsStats = new PlanetLogisticsStatsDialog();

        ui.planet.shown(() -> {
            insertStarMapButton();

            Table stable = ui.planet.sectorTop;
            stable.update(() -> {
                Element result = stable.find(e -> e instanceof TextButton b && Core.bundle.get("stats").contentEquals(b.getText()));

                boolean first = true;
                for (var listener: result.getListeners()) {
                    if (listener instanceof ClickListener) {
                        if (first) {
                            first = false;
                            continue;
                        }
                        result.removeListener(listener);
                    }
                }
                result.clicked(() -> {
                    Sector sector = ui.planet.selected;
                    if (sector != null) sectorLogisticsStats.show(sector);
                });
            });
        });
        ui.planet.resized(ModUI::insertStarMapButton);
    }

    public static void insertStarMapButton() {
        PlanetDialog planet = ui.planet;
        Table buttons = planet.buttons;

        buttons.clearChildren();

        buttons.bottom();

        if (Core.graphics.isPortrait()) {
            buttons.add(planet.sectorTop).colspan(2).fillX();
            buttons.row();
            buttons.table(t -> {
                t.button("@back", Icon.left, planet::hide).size(200f, 54f).pad(2).bottom();
                t.button("@techtree", Icon.tree, () -> ui.research.show()).size(200f, 54f).visible(() -> planet.mode == look).pad(2).bottom();
            }).pad(0).margin(0);
            buttons.row();
            buttons.button("Star Map", Icon.planet, () -> starmap.show()).size(0, 54f).visible(() -> planet.mode == look).pad(2).bottom().fillX();
        } else {
            buttons.button("@back", Icon.left, planet::hide).size(200f, 54f).pad(2).bottom();
            buttons.add().growX();
            buttons.add(planet.sectorTop).minWidth(230f);
            buttons.add().growX();
            buttons.table(t -> {
                t.button("@techtree", Icon.tree, () -> ui.research.show()).size(200f, 54f).visible(() -> planet.mode == look).pad(2).row();
                t.button("Star Map", Icon.planet, () -> starmap.show()).size(200f, 54f).visible(() -> planet.mode == look).pad(2).bottom();
            }).bottom().pad(0).margin(0);
        }
    }
}
