package mod;

import mindustry.mod.ClassMap;
import mod.extend.type.cargopad.*;
import mod.extend.type.sectorpad.*;

public class ClassMapRegister {
    public static void load() {
        //ClassMap.classes.put("basic-shipyard-ShipyardBuild", Shipyard.ShipyardBuild.class);
        //ClassMap.classes.put("ship-core-ShipCoreBuild", ShipCore.ShipCoreBuild.class);
        //ClassMap.classes.put("ship-cargo-ShipCargoBuild", ShipCargo.ShipCargoBuild.class);

        ClassMap.classes.put("PlanetaryItemLaunchPad", PlanetaryItemLaunchPad.class);
        ClassMap.classes.put("PlanetaryItemLandingPad", PlanetaryItemLandingPad.class);
        ClassMap.classes.put("PlanetaryLiquidLaunchPad", PlanetaryLiquidLaunchPad.class);
        ClassMap.classes.put("PlanetaryLiquidLandingPad", PlanetaryLiquidLandingPad.class);
        ClassMap.classes.put("PlanetaryPayloadLaunchPad", PlanetaryPayloadLaunchPad.class);
        ClassMap.classes.put("PlanetaryPayloadLandingPad", PlanetaryPayloadLandingPad.class);

        ClassMap.classes.put("ItemLaunchPad", ItemLaunchPad.class);
        ClassMap.classes.put("ItemLandingPad", ItemLandingPad.class);
        ClassMap.classes.put("LiquidLaunchPad", LiquidLaunchPad.class);
        ClassMap.classes.put("LiquidLandingPad", LiquidLandingPad.class);
        ClassMap.classes.put("PayloadLaunchPad", PayloadLaunchPad.class);
        ClassMap.classes.put("PayloadLandingPad", PayloadLandingPad.class);
    }
}
