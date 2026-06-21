package mod;

import arc.Events;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.mod.*;
import mindustry.ui.dialogs.PlanetDialog;
import mindustry.world.meta.Env;
import mod.extend.sector.PlanetLogistics;
import mod.extend.starmap.StarMapScanner;
import mod.extend.sector.SectorLogistics;
import mod.extend.type.SpaceshipManager;

public class CosmicOut extends Mod{

    public CosmicOut(){
        Events.on(EventType.ModContentLoadEvent.class, e -> StarMapScanner.loadAll());

        Events.on(EventType.ClientLoadEvent.class, evt -> {
            Vars.content.each(content -> {
                if (content instanceof UnlockableContent unlockableContent) {
                    unlockableContent.quietUnlock();
                    unlockableContent.shownPlanets.clear();
                }
            });

            PlanetDialog.debugSelect = true;

            Planets.erekir.allowLaunchToNumbered = true;
            Planets.erekir.ruleSetter = r -> {
                r.waves = false;
                r.infiniteResources = true;
                r.waveTeam = Team.malis;
                r.placeRangeCheck = false;
                r.showSpawns = true;
                r.fog = true;
                r.staticFog = true;
                r.lighting = false;
                r.coreDestroyClear = true;
                r.onlyDepositCore = true;
            };

            Planets.serpulo.ruleSetter = r -> {
                r.waves = false;
                r.infiniteResources = true;
                r.waveTeam = Team.crux;
                r.placeRangeCheck = false;
                r.showSpawns = false;
                r.coreDestroyClear = true;
            };

            Planets.gier.accessible = true;
            Planets.notva.accessible = true;

            Planets.gier.ruleSetter = r -> {
                r.env = Env.terrestrial;
                r.waves = false;
                r.infiniteResources = true;
            };
            Planets.notva.ruleSetter = r -> {
                r.env = Env.terrestrial;
                r.waves = false;
                r.infiniteResources = true;
            };
        });
    }

    @Override
    public void init() {
        PlanetLogistics.init();
        SectorLogistics.init();
        SpaceshipManager.init();
        ModUI.init();
    }

    @Override
    public void loadContent() {
        ClassMapRegister.load();
    }
}
