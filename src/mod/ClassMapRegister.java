package mod;

import mindustry.mod.ClassMap;
import mod.extend.type.cargopad.*;
import mod.extend.type.sectorpad.*;
import mod.extend.type.spaceship.ShipCargo;
import mod.extend.type.spaceship.ShipCore;
import mod.extend.type.spaceship.Shipyard;

public class ClassMapRegister {
    public static void load() {
        ClassMap.classes.put("basic-shipyard-ShipyardBuild", Shipyard.ShipyardBuild.class);
        ClassMap.classes.put("ship-core-ShipCoreBuild", ShipCore.ShipCoreBuild.class);
        ClassMap.classes.put("ship-cargo-ShipCargoBuild", ShipCargo.ShipCargoBuild.class);

        ClassMap.classes.put("item-cargo-launch-pad-PlanetaryItemLaunchPadBuild", PlanetaryItemLaunchPad.PlanetaryItemLaunchPadBuild.class);
        ClassMap.classes.put("liquid-cargo-launch-pad-PlanetaryLiquidLaunchPadBuild", PlanetaryLiquidLaunchPad.PlanetaryLiquidLaunchPadBuild.class);
        ClassMap.classes.put("payload-cargo-launch-pad-PlanetaryPayloadLaunchPadBuild", PlanetaryPayloadLaunchPad.PlanetaryPayloadLaunchPadBuild.class);
        ClassMap.classes.put("item-cargo-landing-pad-PlanetaryItemLandingPadBuild", PlanetaryItemLandingPad.PlanetaryItemLandingPadBuild.class);
        ClassMap.classes.put("liquid-cargo-landing-pad-PlanetaryLiquidLandingPadBuild", PlanetaryLiquidLandingPad.PlanetaryLiquidLandingPadBuild.class);
        ClassMap.classes.put("payload-cargo-landing-pad-PlanetaryPayloadLandingPadBuild", PlanetaryPayloadLandingPad.PlanetaryPayloadLandingPadBuild.class);

        ClassMap.classes.put("liquid-launch-pad-LiquidLaunchPadBuild", LiquidLaunchPad.LiquidLaunchPadBuild.class);
        ClassMap.classes.put("liquid-landing-pad-LiquidLandingPadBuild", LiquidLandingPad.LiquidLandingPadBuild.class);
        ClassMap.classes.put("payload-launch-pad-PayloadLaunchPadBuild", PayloadLaunchPad.PayloadLaunchPadBuild.class);
        ClassMap.classes.put("payload-landing-pad-PayloadLandingPadBuild", PayloadLandingPad.PayloadLandingPadBuild.class);
    }
}
