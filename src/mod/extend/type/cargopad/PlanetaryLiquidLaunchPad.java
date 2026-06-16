package mod.extend.type.cargopad;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.io.TypeIO;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.ui.Styles;
import mindustry.world.blocks.ItemSelection;
import mod.ModUI;
import mod.extend.sector.PlanetLogistics;

import static mindustry.Vars.*;

public class PlanetaryLiquidLaunchPad extends CargoLaunchPad {
    public float launchVolume = 100f;

    public PlanetaryLiquidLaunchPad(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        configurable = true;

        config(Liquid.class, (PlanetaryLiquidLaunchPadBuild build, Liquid liquid) -> {
            if (!build.accessible() || liquid == null) return;
            build.liquidConfig = liquid;
        });

        configClear((PlanetaryLiquidLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.liquidConfig = null;
        });
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addLiquidBar((PlanetaryLiquidLaunchPadBuild build) -> build.liquidConfig);
    }

    public class PlanetaryLiquidLaunchPadBuild extends CargoLaunchPadBuild {
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

            PlanetLogistics.handleLiquidExport(state.getPlanet(), liquidConfig, launchVolume);
            fireLaunchVisual();
            liquids.remove(liquidConfig, launchVolume);
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryLiquidLaunchPad.this, table, content.liquids(), () -> liquidConfig, this::configure);

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
