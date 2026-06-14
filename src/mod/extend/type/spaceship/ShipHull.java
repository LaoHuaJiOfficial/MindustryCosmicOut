package mod.extend.type.spaceship;

import arc.struct.EnumSet;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

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
}
