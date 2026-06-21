package mod.extend.type.spaceship;

import arc.struct.EnumSet;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.StatUnit;
import mod.content.ModStats;

public class ShipHull extends Block implements ShipBlock {
    public float structurePoints = 10f;
    public float massPerSize = 2f;

    public ShipHull(String name) {
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.units;
    }

    @Override
    public float blockMass(){
        return size * size * massPerSize;
    }

    @Override
    public float blockHull(){
        return structurePoints;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(ModStats.hull, structurePoints, StatUnit.none);
        stats.add(ModStats.shipMass, blockMass(), StatUnit.none);
    }
}
