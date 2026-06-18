package mod.content;

import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.campaign.LandingPad;
import mindustry.world.blocks.campaign.LaunchPad;
import mindustry.world.meta.BuildVisibility;
import mod.extend.type.cargopad.*;
import mod.extend.type.sectorpad.*;
import mod.extend.type.spaceship.ShipCargo;
import mod.extend.type.spaceship.ShipCore;
import mod.extend.type.spaceship.ShipEngine;
import mod.extend.type.spaceship.ShipHull;
import mod.extend.type.spaceship.Shipyard;

import static mindustry.type.ItemStack.with;

public class ModBlocks {
    public static Block
            basicShipyard, shipCore, shipHull, shipEngine, shipCargo,
            itemCargoLaunchPad, liquidCargoLaunchPad, payloadCargoLaunchPad,
            itemCargoLandingPad, liquidCargoLandingPad, payloadCargoLandingPad,
            itemLaunchPad, liquidLaunchPad, payloadLaunchPad,
            itemLandingPad, liquidLandingPad, payloadLandingPad;

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
            size = 5;
            itemCapacity = 100;
            launchTime = 60f * 20;
            alwaysUnlocked = true;
        }};

        liquidCargoLaunchPad = new PlanetaryLiquidLaunchPad("liquid-cargo-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 5;
            liquidCapacity = 1200f;
            launchVolume = 1000f;
            alwaysUnlocked = true;
        }};

        payloadCargoLaunchPad = new PlanetaryPayloadLaunchPad("payload-cargo-launch-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 5;
            alwaysUnlocked = true;
        }};

        itemCargoLandingPad = new PlanetaryItemLandingPad("item-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 5;
            itemCapacity = 100;
            consumeLiquidAmount = -1;
            alwaysUnlocked = true;
        }};

        liquidCargoLandingPad = new PlanetaryLiquidLandingPad("liquid-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 5;
            liquidCapacity = 1200f;
            landingVolume = 1000f;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        payloadCargoLandingPad = new PlanetaryPayloadLandingPad("payload-cargo-landing-pad") {{
            requirements(Category.effect, BuildVisibility.shown, ItemStack.with());
            size = 5;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = 100f;
            alwaysUnlocked = true;
        }};

        itemLaunchPad = new ItemLaunchPad("item-launch-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, with());
            size = 3;
            itemCapacity = 40;
            launchTime = 60f * 15;
            liquidCapacity = 30f;
            hasPower = true;
            lightSteps = 1;
            lightStep = 0f;
            drawLiquid = Liquids.oil;
            consumeLiquid(Liquids.oil, 6f/60f);
            consumePower(8f);
            alwaysUnlocked = true;
        }};

        itemLandingPad = new ItemLandingPad("item-landing-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, with());
            size = 3;
            itemCapacity = 120;
            consumeLiquidAmount = -1;
            alwaysUnlocked = true;
        }};

        liquidLaunchPad = new LiquidLaunchPad("liquid-launch-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, ItemStack.with());
            size = 3;
            launchTime = 60f * 15;
            liquidCapacity = 1200f;
            launchVolume = 1000f;
            lightSteps = 1;
            lightStep = 0f;
            consumePower(8f);
            alwaysUnlocked = true;
        }};

        liquidLandingPad = new LiquidLandingPad("liquid-landing-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, ItemStack.with());
            size = 3;
            liquidCapacity = 1200f;
            landingVolume = 1000f;
            consumeLiquid = Liquids.water;
            consumeLiquidAmount = -1f;
            alwaysUnlocked = true;
        }};

        payloadLaunchPad = new PayloadLaunchPad("payload-launch-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, ItemStack.with());
            size = 3;
            launchTime = 60f * 15;
            lightSteps = 1;
            lightStep = 0f;
            consumePower(8f);
            alwaysUnlocked = true;
        }};

        payloadLandingPad = new PayloadLandingPad("payload-landing-pad") {{
            requirements(Category.effect, BuildVisibility.notLegacyLaunchPadOnly, ItemStack.with());
            size = 3;
            consumeLiquidAmount = -1f;
            alwaysUnlocked = true;
        }};
    }
}
