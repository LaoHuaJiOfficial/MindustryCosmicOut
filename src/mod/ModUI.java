package mod;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.PlanetDialog;
import mod.extend.StarMapDialog;

import static mindustry.Vars.ui;
import static mindustry.ui.dialogs.PlanetDialog.Mode.look;

public class ModUI {
    public static StarMapDialog starmap;

    public static void init() {
        starmap = new StarMapDialog();

        ui.planet.shown(ModUI::insertStarMapButton);
        ui.planet.resized(ModUI::insertStarMapButton);
    }

    public static void insertStarMapButton() {
        PlanetDialog planet = ui.planet;
        Table buttons = planet.buttons;

        buttons.clearChildren();

        buttons.bottom();

        if(Core.graphics.isPortrait()){
            buttons.add(planet.sectorTop).colspan(2).fillX();
            buttons.row();
            buttons.table(t -> {
                t.button("@back", Icon.left, planet::hide).size(200f, 54f).pad(2).bottom();
                t.button("@techtree", Icon.tree, () -> ui.research.show()).size(200f, 54f).visible(() -> planet.mode == look).pad(2).bottom();
            }).pad(0).margin(0);
            buttons.row();
            buttons.button("Star Map", Icon.planet, () -> starmap.show()).size(0, 54f).visible(() -> planet.mode == look).pad(2).bottom().fillX();
        }else{
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
