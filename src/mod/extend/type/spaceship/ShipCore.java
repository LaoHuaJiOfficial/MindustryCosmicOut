package mod.extend.type.spaceship;

import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockGroup;
import mod.content.ModPlanets;
import mod.extend.type.ModPlanet;
import mod.extend.type.Spaceship;
import mod.extend.type.SpaceshipManager;
import mod.extend.type.SpaceshipStats;

import static mindustry.Vars.content;
import static mindustry.Vars.indexer;
import static mindustry.Vars.ui;

public class ShipCore extends Block implements ShipBlock {
    public float mass = 20f;

    public ShipCore(String name) {
        super(name);
        solid = true;
        update = true;
        sync = true;
        destructible = true;
        configurable = true;
        commandable = true;
        group = BlockGroup.units;
        flags = EnumSet.of(BlockFlag.core);
    }

    @Override
    public float blockMass(){
        return mass;
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class ShipCoreBuild extends Building {

    }
}
