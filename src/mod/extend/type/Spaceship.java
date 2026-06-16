package mod.extend.type;

import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.Nullable;
import mindustry.game.Schematic;
import mindustry.type.PayloadSeq;
import mindustry.type.Planet;
import mod.extend.starmap.HexCoord;
import mod.extend.starmap.HexPlanetMap;
import mod.extend.starmap.StarMapPlanets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static mindustry.Vars.content;

public class Spaceship {
    public enum Status {
        docked,
        traveling,
        arrived
    }

    public int id = -1;
    public String name = "Spaceship";
    public Status status = Status.docked;

    public @Nullable Planet departurePlanet, destinationPlanet;
    public HexCoord currentHex = new HexCoord();
    public Seq<HexCoord> route = new Seq<>();
    public int routeIndex;
    public int turnProgress;

    public PayloadSeq cargo = new PayloadSeq();
    public @Nullable Schematic structure;

    public float shipMass;
    public float engineThrust;
    public float hullPoints;

    public int hexPerTurn(){
        if(engineThrust <= 0f) return Integer.MAX_VALUE;
        return Math.max(1, (int)Math.ceil(shipMass / engineThrust));
    }

    public void launch(Planet departure, Planet destination){
        this.departurePlanet = departure;
        this.destinationPlanet = destination;
        this.route.clear();
        this.routeIndex = 0;
        this.turnProgress = 0;
        this.status = Status.traveling;

        HexCoord from = StarMapPlanets.get(departure).hexCoord();
        HexCoord to = StarMapPlanets.get(destination).hexCoord();
        this.route.addAll(HexCoord.line(from, to));
        this.currentHex.set(route.first());
    }

    public void turnUpdate(){
        if(status != Status.traveling || route.isEmpty() || destinationPlanet == null) return;

        if(routeIndex >= route.size - 1){
            arrive();
            return;
        }

        turnProgress++;
        if(turnProgress >= hexPerTurn()){
            turnProgress = 0;
            routeIndex = Math.min(routeIndex + 1, route.size - 1);
            HexCoord next = route.get(routeIndex);
            currentHex.a = next.a;
            currentHex.b = next.b;
            currentHex.c = next.c;

            if(routeIndex >= route.size - 1){
                arrive();
            }
        }
    }

    public void arrive(){
        status = Status.arrived;
        if(destinationPlanet != null){
            currentHex.set(StarMapPlanets.get(destinationPlanet).hexCoord());
        }
        SpaceshipManager.onSpaceshipArrived(this);
    }

    public @Nullable Planet planetAt(HexPlanetMap map){
        return map.planetAt(currentHex);
    }

    public void applyStats(SpaceshipStats stats){
        shipMass = stats.mass;
        engineThrust = stats.thrust;
        hullPoints = stats.hull;
        SpaceshipStats.copyPayload(stats.cargo, cargo);
        structure = stats.structure;

    }

    public SpaceshipSave toSave(){
        SpaceshipSave save = new SpaceshipSave();
        save.id = id;
        save.name = name;
        save.status = status.name();
        save.departure = departurePlanet == null ? null : departurePlanet.name;
        save.destination = destinationPlanet == null ? null : destinationPlanet.name;
        save.currentA = currentHex.a;
        save.currentB = currentHex.b;
        save.currentC = currentHex.c;
        save.route = route.map(h -> HexCoord.key(h)).toArray(String.class);
        save.routeIndex = routeIndex;
        save.turnProgress = turnProgress;
        save.shipMass = shipMass;
        save.engineThrust = engineThrust;
        save.hullPoints = hullPoints;

        if(!cargo.isEmpty()){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            cargo.write(new Writes(new DataOutputStream(stream)));
            save.cargoData = stream.toByteArray();
        }

        return save;
    }

    public void fromSave(SpaceshipSave save){
        id = save.id;
        name = save.name;
        status = Status.valueOf(save.status);
        departurePlanet = save.departure == null ? null : content.planet(save.departure);
        destinationPlanet = save.destination == null ? null : content.planet(save.destination);
        currentHex.a = save.currentA;
        currentHex.b = save.currentB;
        currentHex.c = save.currentC;
        route.clear();
        if(save.route != null){
            for(String key : save.route){
                String[] parts = key.split(",");
                if(parts.length == 3){
                    route.add(new HexCoord(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                }
            }
        }
        routeIndex = save.routeIndex;
        turnProgress = save.turnProgress;
        shipMass = save.shipMass;
        engineThrust = save.engineThrust;
        hullPoints = save.hullPoints;
        cargo.clear();
        if(save.cargoData != null && save.cargoData.length > 0){
            cargo.read(new Reads(new DataInputStream(new ByteArrayInputStream(save.cargoData))));
        }
    }

    public static class SpaceshipSave {
        public int id;
        public String name;
        public String status;
        public String departure, destination;
        public int currentA, currentB, currentC;
        public String[] route;
        public int routeIndex, turnProgress;
        public float shipMass, engineThrust, hullPoints;
        public byte[] cargoData;
    }
}
