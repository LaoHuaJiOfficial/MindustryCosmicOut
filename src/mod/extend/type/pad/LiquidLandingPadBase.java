package mod.extend.type.pad;

import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.io.TypeIO;
import mindustry.type.Liquid;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.StatUnit;
import mod.content.ModStats;

import static mindustry.Vars.*;

public abstract class LiquidLandingPadBase extends ModLandingPad {
    static ObjectMap<Liquid, Seq<LiquidLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public float landingVolume = 100f;

    public LiquidLandingPadBase(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        acceptsItems = false;

        config(Liquid.class, (LiquidLandingPadBuild build, Liquid liquid) -> {
            if (!build.accessible() || liquid == null || !acceptLiquidConfig(liquid)) return;
            build.liquidConfig = liquid;
        });

        configClear((LiquidLandingPadBuild build) -> {
            if (!build.accessible()) return;
            build.liquidConfig = null;
        });
    }

    protected boolean acceptLiquidConfig(Liquid liquid) {
        return true;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(ModStats.landingVolume, landingVolume, StatUnit.liquidUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addLiquidBar((LiquidLandingPadBuild build) -> build.liquidConfig);
    }

    protected abstract void resetLiquidImportTimer(LiquidLandingPadBuild build, Liquid liquid);

    protected abstract void syncLiquidImportTimers();

    protected abstract void handleLiquidImport(Liquid liquid, float volume);

    protected abstract boolean canRequestLiquidImport(LiquidLandingPadBuild build, Liquid liquid);

    protected abstract String buildImportSourcesLabel(Liquid liquid);

    public class LiquidLandingPadBuild extends ModLandingPadBuild {
        public @Nullable Liquid liquidConfig;

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        @Override
        public void handleLanding() {
            if (liquidConfig == null) return;

            cooldown = 1f;
            arrivingLiquid = liquidConfig;
            arrivingTimer = 0f;
            liquidRemoved = 0f;
            landSound.at(x, y, 1f, landSoundVolume);

            if (state.isCampaign() && !isFake()) {
                resetLiquidImportTimer(this, liquidConfig);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;
                syncLiquidImportTimers();

                waiting.each((liquid, pads) -> {
                    pads.removeAll(l -> l.liquidConfig != liquid);
                    if (pads.size > 0) {
                        pads.sort(p -> p.priority);
                        var first = pads.first();
                        var head = pads.peek();
                        Call.landingPadLanded(first.tile);
                        var tmp = first.priority;
                        first.priority = head.priority;
                        head.priority = tmp;
                        pads.clear();
                    }
                });
            }
        }

        @Override
        public void updateTile() {
            updateTimers();

            if (arrivingLiquid != null) {
                updateArrivalParticles();
                updateArrivalLiquidConsume();

                if (arrivingTimer >= 1f) {
                    finishArrivalEffects();
                    liquids.add(arrivingLiquid, landingVolume);
                    if (!isFake()) {
                        handleLiquidImport(arrivingLiquid, landingVolume);
                    }
                    arrivingLiquid = null;
                    arrivingTimer = 0f;
                }
            }

            if (liquidConfig != null && liquids.get(liquidConfig) > 0.001f) {
                dumpLiquid(liquidConfig);
            }

            updateCooldown();

            if (liquidConfig != null && (isFake() || (state.isCampaign() && !legacyDisabled()))) {
                float stored = liquids.get(liquidConfig);
                boolean hasRoom = stored <= liquidCapacity - landingVolume + 0.001f;
                if (cooldown <= 0f && efficiency > 0f && hasRoom && !isLanding() && canRequestLiquidImport(this, liquidConfig)) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        Seq<LiquidLandingPadBuild> pads = waiting.get(liquidConfig, () -> new Seq<>(false));
                        if (!pads.contains(this)) pads.add(this);
                    }
                }
            }
        }

        @Override
        public void dumpLiquid(Liquid liquid, float scaling, int outputDir) {
            if (liquids.get(liquid) <= 0.001f) return;

            if (!net.client() && state.isCampaign() && team == state.rules.defaultTeam) {
                liquid.unlock();
            }

            for (int i = 0; i < proximity.size; i++) {
                incrementDump(proximity.size);
                Building other = proximity.get((i + cdump) % proximity.size);
                if (outputDir != -1 && (outputDir + rotation) % 4 != relativeTo(other)) continue;

                other = other.getLiquidDestination(this, liquid);
                if (other == null || !other.block.hasLiquids || other.liquids == null || !canDumpLiquid(other, liquid)) continue;

                transferLiquid(other, liquids.get(liquid) / scaling, liquid);
                if (liquids.get(liquid) <= 0.001f) break;
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(LiquidLandingPadBase.this, table, content.liquids(), () -> liquidConfig, this::configure);
        }

        @Override
        public void drawSelect() {
            drawItemSelection(liquidConfig);
        }

        @Override
        protected String buildImportDisplayLabel() {
            if (liquidConfig == null) return null;
            return buildImportSourcesLabel(liquidConfig);
        }

        @Override
        public @Nullable Object config() {
            return liquidConfig;
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            liquidConfig = TypeIO.readLiquid(read);
            arrivingLiquid = TypeIO.readLiquid(read);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            TypeIO.writeLiquid(write, liquidConfig);
            TypeIO.writeLiquid(write, arrivingLiquid);
        }
    }
}
