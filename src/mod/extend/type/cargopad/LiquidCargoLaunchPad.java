package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.io.TypeIO;
import mindustry.type.Liquid;
import mindustry.type.Sector;
import mindustry.world.blocks.ItemSelection;
import mod.ModUI;
import mod.extend.sector.SectorLogistics;

import static mindustry.Vars.*;

public class LiquidCargoLaunchPad extends CargoLaunchPad {
    public float launchVolume = 100f;

    public LiquidCargoLaunchPad(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        configurable = true;

        config(Liquid.class, (LiquidCargoLaunchPadBuild build, Liquid liquid) -> {
            if (!build.accessible() || liquid == null) return;
            build.liquidConfig = liquid;
        });

        configClear((LiquidCargoLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.liquidConfig = null;
        });
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addLiquidBar((LiquidCargoLaunchPadBuild build) -> build.liquidConfig);
    }

    public class LiquidCargoLaunchPadBuild extends CargoLaunchPadBuild {
        public @Nullable Liquid liquidConfig;

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        @Override
        protected float launchFillRatio() {
            if (liquidConfig == null) return 0f;
            return liquids.get(liquidConfig) / launchVolume;
        }

        @Override
        protected @Nullable Liquid displayLiquid() {
            return liquidConfig;
        }

        @Override
        public void updateTile() {
            if (liquidConfig == null || !readyToLaunch()) return;

            if (liquids.get(liquidConfig) < launchVolume) return;

            SectorLogistics.handleLiquidExport(state.getSector(), liquidConfig, launchVolume);
            fireLaunchVisual();
            liquids.remove(liquidConfig, launchVolume);
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(LiquidCargoLaunchPad.this, table, content.liquids(), () -> liquidConfig, this::configure);
            CargoPadUI.buildDestinationButton(table, () -> {
                ModUI.starmap.showSectorSelect(state.getSector(), dest -> {
                    if (!state.isCampaign() || dest == null) return;
                    Sector prev = state.rules.sector.info.destination;
                    state.rules.sector.info.destination = dest;
                    state.rules.sector.saveInfo();
                    SectorLogistics.flushStats(state.getSector());
                    if (prev != null) SectorLogistics.refreshImportRates(prev.planet, prev);
                    SectorLogistics.refreshImportRates(dest.planet, dest);
                });
                deselect();
            });
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquidConfig != null && liquid == liquidConfig && liquids.get(liquid) < liquidCapacity;
        }

        @Override
        public @Nullable Object config() {
            return liquidConfig;
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);

            liquidConfig = TypeIO.readLiquid(read);
        }

        @Override
        public void write(Writes write) {
            super.write(write);

            TypeIO.writeLiquid(write, liquidConfig);
        }
    }
}
