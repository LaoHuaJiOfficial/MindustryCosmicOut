package mod;

import arc.Events;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.mod.*;
import mod.content.ModBlocks;
import mod.content.ModPlanets;
import mod.extend.sector.SectorLogistics;
import mod.extend.type.SpaceshipManager;

public class CosmicOut extends Mod{

    public CosmicOut(){
        Events.on(EventType.ClientLoadEvent.class, evt -> {
            Vars.content.each(content -> {
                if (content instanceof UnlockableContent unlockableContent) {
                    unlockableContent.quietUnlock();
                    unlockableContent.shownPlanets.clear();
                }
            });
        });
    }

    @Override
    public void init() {
        ClassMapRegister.load();
        SectorLogistics.init();
        SpaceshipManager.init();
        ModUI.init();
    }

    @Override
    public void loadContent(){
        ModPlanets.load();
        ModBlocks.load();
    }
}
