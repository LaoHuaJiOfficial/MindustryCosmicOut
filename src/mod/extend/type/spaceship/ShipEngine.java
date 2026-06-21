package mod.extend.type.spaceship;

import arc.struct.EnumSet;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.StatUnit;
import mod.content.ModStats;

public class ShipEngine extends Block implements ShipBlock {
    public float thrust = 10f;
    public float massPerSize = 3f;

    public ShipEngine(String name) {
        super(name);
        solid = true;
        update = true;
        destructible = true;
        group = BlockGroup.units;
    }

    @Override
    public float blockMass(){
        return size * size * massPerSize;
    }

    @Override
    public float blockThrust(){
        return thrust;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(ModStats.thrust, thrust, StatUnit.none);
        stats.add(ModStats.shipMass, blockMass(), StatUnit.none);
    }
}
