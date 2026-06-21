package mod.content;

import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

public final class ModStats {
    public static final Stat
            maxPath = new Stat("maxpath", StatCat.items),
            launchVolume = new Stat("launchvolume", StatCat.liquids),
            landingVolume = new Stat("landingvolume", StatCat.liquids),
            payloadLaunchCount = new Stat("payloadlaunchcount", StatCat.items),
            thrust = new Stat("thrust", StatCat.function),
            hull = new Stat("hull", StatCat.function),
            shipMass = new Stat("shipmass", StatCat.general),
            stackCapacity = new Stat("stackcapacity", StatCat.items),
            yardSize = new Stat("yardsize", StatCat.general);

    private ModStats() {}
}
