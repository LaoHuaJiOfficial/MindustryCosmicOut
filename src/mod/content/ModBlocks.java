package mod.content;

import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import mod.extend.type.cargopad.*;
import mod.extend.type.sectorpad.*;
import mod.extend.type.spaceship.ShipCargo;
import mod.extend.type.spaceship.ShipCore;
import mod.extend.type.spaceship.ShipEngine;
import mod.extend.type.spaceship.ShipHull;
import mod.extend.type.spaceship.Shipyard;

public class ModBlocks {
    public static Block
            basicShipyard, shipCore, shipHull, shipEngine, shipCargo,
            itemCargoLaunchPad, liquidCargoLaunchPad, payloadCargoLaunchPad,
            itemCargoLandingPad, liquidCargoLandingPad, payloadCargoLandingPad,
            liquidLaunchPad, liquidLandingPad, payloadLaunchPad, payloadLandingPad;

    public static void load() {
        /*
        basicShipyard = new Shipyard("basic-shipyard") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 3;
            alwaysUnlocked = true;
        }};

        shipCore = new ShipCore("ship-core") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 2;
            alwaysUnlocked = true;
        }};

        shipHull = new ShipHull("ship-hull") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 1;
            alwaysUnlocked = true;
        }};

        shipEngine = new ShipEngine("ship-engine") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 1;
            thrust = 15f;
            alwaysUnlocked = true;
        }};

        shipCargo = new ShipCargo("ship-cargo") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 1;
            stackCapacity = 8;
            alwaysUnlocked = true;
        }};

         */

        itemCargoLaunchPad = new PlanetaryItemLaunchPad("item-cargo-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            itemCapacity = 100;
            alwaysUnlocked = true;
        }};

        liquidCargoLaunchPad = new PlanetaryLiquidLaunchPad("liquid-cargo-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            liquidCapacity = 1200f;
            launchVolume = 1000f;
            alwaysUnlocked = true;
        }};

        payloadCargoLaunchPad = new PlanetaryPayloadLaunchPad("payload-cargo-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            stackCapacity = 16;
            alwaysUnlocked = true;
        }};

        itemCargoLandingPad = new PlanetaryItemLandingPad("item-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            itemCapacity = 100;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        liquidCargoLandingPad = new PlanetaryLiquidLandingPad("liquid-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            liquidCapacity = 1200f;
            landingVolume = 1000f;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        payloadCargoLandingPad = new PlanetaryPayloadLandingPad("payload-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            stackCapacity = 16;
            landingBatch = 1;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        liquidLaunchPad = new LiquidLaunchPad("liquid-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            liquidCapacity = 1200f;
            launchVolume = 1000f;
            alwaysUnlocked = true;
        }};

        liquidLandingPad = new LiquidLandingPad("liquid-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            liquidCapacity = 1200f;
            landingVolume = 1000f;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        payloadLaunchPad = new PayloadLaunchPad("payload-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            stackCapacity = 16;
            alwaysUnlocked = true;
        }};

        payloadLandingPad = new PayloadLandingPad("payload-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 4;
            stackCapacity = 16;
            landingBatch = 1;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};
    }
}
