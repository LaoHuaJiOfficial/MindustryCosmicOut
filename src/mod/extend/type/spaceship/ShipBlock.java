package mod.extend.type.spaceship;

public interface ShipBlock {
    default float blockMass(){
        return 0f;
    }

    default float blockThrust(){
        return 0f;
    }

    default float blockHull(){
        return 0f;
    }
}
