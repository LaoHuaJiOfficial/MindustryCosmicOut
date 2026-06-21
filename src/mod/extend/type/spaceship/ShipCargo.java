package mod.extend.type.spaceship;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.PayloadSeq;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.StatUnit;
import mod.content.ModStats;

public class ShipCargo extends Block implements ShipBlock {
    public float massPerSize = 2f;
    public int stackCapacity = 16;

    public ShipCargo(String name) {
        super(name);
        solid = true;
        update = true;
        sync = true;
        acceptsPayload = true;
        destructible = true;
        group = BlockGroup.transportation;
    }

    @Override
    public float blockMass(){
        return size * size * massPerSize;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(ModStats.stackCapacity, stackCapacity, StatUnit.none);
        stats.add(ModStats.shipMass, blockMass(), StatUnit.none);
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class ShipCargoBuild extends Building {
        public PayloadSeq stacks = new PayloadSeq();

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            if(stacks.total() >= stackCapacity) return false;
            if(payload instanceof BuildPayload bp){
                return bp.build.block != null;
            }
            return payload instanceof UnitPayload;
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            if(payload instanceof BuildPayload bp){
                stacks.add(bp.build.block, 1);
            }else if(payload instanceof UnitPayload up){
                stacks.add(up.unit.type, 1);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            stacks.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            stacks.read(read);
        }
    }
}
