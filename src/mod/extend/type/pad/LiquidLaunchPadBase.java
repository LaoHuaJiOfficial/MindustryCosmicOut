package mod.extend.type.pad;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.io.TypeIO;
import mindustry.type.Liquid;
import mindustry.world.blocks.ItemSelection;

import static mindustry.Vars.*;

public abstract class LiquidLaunchPadBase extends ModLaunchPad {
    public float launchVolume = 100f;

    public LiquidLaunchPadBase(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        configurable = true;

        config(Liquid.class, (LiquidLaunchPadBuild build, Liquid liquid) -> {
            if (!build.accessible() || liquid == null || !acceptLiquidConfig(liquid)) return;
            build.liquidConfig = liquid;
        });

        configClear((LiquidLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.liquidConfig = null;
        });
    }

    protected boolean acceptLiquidConfig(Liquid liquid) {
        return true;
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addLiquidBar((LiquidLaunchPadBuild build) -> build.liquidConfig);
    }

    protected abstract void exportLiquid(Liquid liquid, float volume);

    public abstract class LiquidLaunchPadBuild extends ModLaunchPadBuild {
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

            exportLiquid(liquidConfig, launchVolume);
            fireLaunchVisual();
            liquids.remove(liquidConfig, launchVolume);
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(LiquidLaunchPadBase.this, table, content.liquids(), () -> liquidConfig, this::configure);

            if (!state.isCampaign() || net.client()) {
                deselect();
                return;
            }

            table.row();
            buildDestinationConfig(table);
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
