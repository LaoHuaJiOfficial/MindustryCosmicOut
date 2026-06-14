package mod.extend.type;

import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Schematic;
import mindustry.gen.Building;
import mindustry.type.PayloadSeq;
import mod.extend.type.spaceship.ShipBlock;
import mod.extend.type.spaceship.ShipCargo;
import mod.extend.type.spaceship.ShipCore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class SpaceshipStats {
    public float mass;
    public float thrust;
    public float hull;
    public PayloadSeq cargo = new PayloadSeq();
    public Schematic structure;
    public ShipCore.ShipCoreBuild core;

    public static SpaceshipStats fromYard(Seq<Building> buildings){
        SpaceshipStats stats = new SpaceshipStats();

        for(Building build : buildings){
            if(build.block instanceof ShipBlock shipBlock){
                stats.mass += shipBlock.blockMass();
                stats.thrust += shipBlock.blockThrust();
                stats.hull += shipBlock.blockHull();
            }

            if(build instanceof ShipCargo.ShipCargoBuild cargo){
                copyPayload(cargo.stacks, stats.cargo);
            }

            if(build instanceof ShipCore.ShipCoreBuild core){
                stats.core = core;
            }
        }

        return stats;
    }

    public static void copyPayload(PayloadSeq from, PayloadSeq to){
        if(from.isEmpty()){
            to.clear();
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        from.write(new Writes(new DataOutputStream(stream)));
        to.clear();
        to.read(new Reads(new DataInputStream(new ByteArrayInputStream(stream.toByteArray()))));
    }
}
