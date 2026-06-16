package mod.extend.starmap;

import arc.struct.Seq;
import mindustry.type.Planet;

public class StarMapPlanetData {
    public final Planet planet;
    public final int coord1, coord2, coord3;
    public float starmapSize;
    public Seq<StarMapPlanetData> links = new Seq<>();

    public StarMapPlanetData(Planet planet, int axis0, int axis120) {
        this(planet, axis0, axis120, 1.5f);
    }

    public StarMapPlanetData(Planet planet, int axis0, int axis120, float starmapSize) {
        this.planet = planet;
        this.coord1 = axis0;
        this.coord3 = axis120;
        this.coord2 = -axis0 - axis120;
        this.starmapSize = starmapSize;
    }

    public HexCoord hexCoord() {
        return HexCoord.from0And120(coord1, coord3);
    }
}
