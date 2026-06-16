package mod.extend.type.cargopad;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.graphics.Pal;
import mindustry.type.PayloadSeq;
import mindustry.type.Planet;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.ModUI;
import mod.extend.sector.PlanetLogistics;

import static mindustry.Vars.*;

public class PlanetaryPayloadLaunchPad extends CargoLaunchPad {
    public int stackCapacity = 16;

    public PlanetaryPayloadLaunchPad(String name) {
        super(name);
        hasItems = false;
        acceptsPayload = true;
        update = true;
        configurable = true;
        selectionRows = selectionColumns = 8;

        config(Block.class, (PlanetaryPayloadLaunchPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
            build.stacks.clear();
        });

        config(UnitType.class, (PlanetaryPayloadLaunchPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
            build.stacks.clear();
        });

        configClear((PlanetaryPayloadLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.configBlock = null;
            build.unit = null;
            build.stacks.clear();
        });
    }

    public boolean canProduce(Block block) {
        return block.isVisible() && block.size < size && !(block instanceof CoreBlock) && !state.rules.isBanned(block) && block.environmentBuildable();
    }

    public boolean canProduce(UnitType unit) {
        return !unit.isHidden() && !unit.isBanned() && unit.supportsEnv(state.rules.env);
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addBar("payload", (PlanetaryPayloadLaunchPadBuild build) -> new Bar(
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null || build.stacks.total() <= 0 ? Iconc.cancel + "" : config.localizedName;
                },
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null ? Color.clear : Pal.ammo;
                },
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null ? 0f : (float) build.stacks.get(config) / stackCapacity;
                }
        ));
    }

    public class PlanetaryPayloadLaunchPadBuild extends CargoLaunchPadBuild {
        public Block configBlock;
        public UnitType unit;
        public PayloadSeq stacks = new PayloadSeq();

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        public @Nullable UnlockableContent payloadConfig() {
            return unit != null ? unit : configBlock;
        }

        @Override
        protected float launchFillRatio() {
            UnlockableContent config = payloadConfig();
            if (config == null) return 0f;
            return (float) stacks.get(config) / stackCapacity;
        }

        @Override
        public void updateTile() {
            UnlockableContent config = payloadConfig();
            if (config == null || !readyToLaunch() || stacks.get(config) < stackCapacity) return;

            PlanetLogistics.handlePayloadExport(state.getPlanet(), config, stackCapacity);

            fireLaunchVisual();
            stacks.clear();
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload) {
            if (stacks.total() >= stackCapacity) return false;
            if (payload instanceof BuildPayload bp) {
                return configBlock != null && bp.build.block == configBlock;
            }
            if (payload instanceof UnitPayload up) {
                return unit != null && up.unit.type == unit;
            }
            return false;
        }

        @Override
        public void handlePayload(Building source, Payload payload) {
            if (payload instanceof BuildPayload bp && configBlock != null && bp.build.block == configBlock) {
                stacks.add(configBlock, 1);
            } else if (payload instanceof UnitPayload up && unit != null && up.unit.type == unit) {
                stacks.add(unit, 1);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryPayloadLaunchPad.this, table,
                    content.blocks().select(PlanetaryPayloadLaunchPad.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PlanetaryPayloadLaunchPad.this::canProduce).<UnlockableContent>as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);

            if (!state.isCampaign() || net.client()) {
                deselect();
                return;
            }

            table.row();
            table.button(Icon.upOpen, Styles.cleari, () -> {
                ModUI.starmap.showSectorSelect(state.getSector(), dest -> {
                    if (!state.isCampaign() || dest == null) return;

                    Planet prev = PlanetLogistics.get(state.getPlanet()).destinationPlanet();
                    PlanetLogistics.get(state.getPlanet()).setDestination(dest.planet);
                    PlanetLogistics.flushStats(state.rules.sector);
                    PlanetLogistics.save(state.getPlanet());
                    if (prev != null) PlanetLogistics.refreshImportRates(prev);
                    PlanetLogistics.refreshImportRates(dest.planet);
                });
                deselect();
            }).size(40f);
        }

        @Override
        public @Nullable Object config() {
            return unit == null ? configBlock : unit;
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            write.s(configBlock == null ? -1 : configBlock.id);
            stacks.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) {
                unit = content.unit(read.s());
                configBlock = content.block(read.s());
            }
            stacks.read(read);
        }
    }
}
