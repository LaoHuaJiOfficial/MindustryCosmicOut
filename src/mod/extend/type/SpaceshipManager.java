package mod.extend.type;

import arc.Core;
import arc.Events;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.game.EventType;
import mindustry.type.Sector;
import mod.extend.type.Spaceship.SpaceshipSave;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

public class SpaceshipManager {
    public static ObjectMap<Integer, Spaceship> spaceships = new ObjectMap<>();
    public static int spaceshipId = 0;

    public static void init() {
        loadSpaceshipData();

        Events.on(EventType.TurnEvent.class, e -> {
            if(net.client()) return;
            updateAll();
        });

        Events.on(EventType.SaveWriteEvent.class, evt -> {
            if(!net.client() && state.isCampaign()){
                saveSpaceshipData();
            }
        });

        Events.on(EventType.SaveLoadEvent.class, evt -> {
            if(!net.client() && state.isCampaign() && state.getSector() != null){
                handleSectorLoad(state.getSector());
            }
        });
    }

    public static int registerNewSpaceship(Spaceship spaceship){
        spaceship.id = ++spaceshipId;
        spaceships.put(spaceship.id, spaceship);
        saveSpaceshipData();
        return spaceship.id;
    }

    public static void updateSpaceshipStat(Spaceship spaceship){
        if(spaceship == null) return;
        spaceships.put(spaceship.id, spaceship);
        saveSpaceshipData();
    }

    public static void onSpaceshipArrived(Spaceship spaceship){
        saveSpaceshipData();
    }

    public static void updateAll(){
        spaceships.each((id, spaceship) -> spaceship.turnUpdate());
        saveSpaceshipData();
    }

    public static void saveSpaceshipData() {
        Core.settings.put("cosmic-out-spaceship-id", spaceshipId);

        Seq<SpaceshipSave> saves = new Seq<>();
        //instance.spaceships.each(ship -> saves.add(ship.toSave()));
        //Core.settings.putJson("cosmic-out-spaceships", saves, Seq.class, SpaceshipSave.class);
    }

    public static void loadSpaceshipData() {
        spaceshipId = Core.settings.getInt("cosmic-out-spaceship-id", 0);
        spaceships.clear();

        Seq<SpaceshipSave> saves = Core.settings.getJson("cosmic-out-spaceships", Seq.class, SpaceshipSave.class, Seq::new);
        if(saves == null) return;

        for(SpaceshipSave save : saves){
            Spaceship ship = new Spaceship();
            ship.fromSave(save);
            spaceships.put(ship.id, ship);
            spaceshipId = Math.max(spaceshipId, ship.id);
        }
    }

    public static void handleSectorLoad(Sector sector) {
        loadSpaceshipData();
    }

    public static Spaceship get(int id){
        return spaceships.get(id);
    }


}
