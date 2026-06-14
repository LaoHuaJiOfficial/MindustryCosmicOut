package mod.extend.type;

import arc.struct.Seq;
import mindustry.mod.Mod;
import mindustry.type.Planet;
import mod.extend.starmap.HexCoord;

public class ModPlanet extends Planet {
    public int coord1, coord2, coord3;
    public float starmapSize = 1.5f;

    public Seq<ModPlanet> links = Seq.with();

    public ModPlanet(String name, Planet parent, float radius, int axis0, int axis120) {
        super(name, parent, radius);
        this.coord1 = axis0;
        this.coord3 = axis120;
        this.coord2 = -axis0 - axis120;
    }

    public HexCoord hexCoord(){
        return HexCoord.from0And120(coord1, coord3);
    }
}
