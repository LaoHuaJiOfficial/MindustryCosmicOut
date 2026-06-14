package mod;

import mindustry.mod.ClassMap;
import mod.extend.type.cargopad.*;
import mod.extend.type.spaceship.ShipCargo;
import mod.extend.type.spaceship.ShipCore;
import mod.extend.type.spaceship.Shipyard;

public class ClassMapRegister {
    public static void load() {
        ClassMap.classes.put("basic-shipyard-ShipyardBuild", Shipyard.ShipyardBuild.class);
        ClassMap.classes.put("ship-core-ShipCoreBuild", ShipCore.ShipCoreBuild.class);
        ClassMap.classes.put("ship-cargo-ShipCargoBuild", ShipCargo.ShipCargoBuild.class);

        ClassMap.classes.put("item-cargo-launch-pad-ItemCargoLaunchPadBuild", ItemCargoLaunchPad.ItemCargoLaunchPadBuild.class);
        ClassMap.classes.put("liquid-cargo-launch-pad-LiquidCargoLaunchPadBuild", LiquidCargoLaunchPad.LiquidCargoLaunchPadBuild.class);
        ClassMap.classes.put("payload-cargo-launch-pad-PayloadCargoLaunchPadBuild", PayloadCargoLaunchPad.PayloadCargoLaunchPadBuild.class);
        ClassMap.classes.put("item-cargo-landing-pad-ItemCargoLandingPadBuild", ItemCargoLandingPad.ItemCargoLandingPadBuild.class);
        ClassMap.classes.put("liquid-cargo-landing-pad-LiquidCargoLandingPadBuild", LiquidCargoLandingPad.LiquidCargoLandingPadBuild.class);
        ClassMap.classes.put("payload-cargo-landing-pad-PayloadCargoLandingPadBuild", PayloadCargoLandingPad.PayloadCargoLandingPadBuild.class);
    }
}
