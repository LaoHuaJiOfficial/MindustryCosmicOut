package mod.extend.type.cargopad;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.io.TypeIO;
import mindustry.type.Liquid;
import mindustry.type.Planet;
import mindustry.world.blocks.ItemSelection;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;

import static mindustry.Vars.*;

public class PlanetaryLiquidLandingPad extends CargoLandingPad {
    static ObjectMap<Liquid, Seq<PlanetaryLiquidLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public float landingVolume = 100f;

    public PlanetaryLiquidLandingPad(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        acceptsItems = false;

        config(Liquid.class, (PlanetaryLiquidLandingPadBuild build, Liquid liquid) -> {
            if (!build.accessible() || liquid == null || !liquid.isOnPlanet(state.getPlanet())) return;
            build.liquidConfig = liquid;
        });

        configClear((PlanetaryLiquidLandingPadBuild build) -> {
            if (!build.accessible()) return;
            build.liquidConfig = null;
        });
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        addLiquidBar((PlanetaryLiquidLandingPadBuild build) -> build.liquidConfig);
    }

    public class PlanetaryLiquidLandingPadBuild extends CargoLandingPadBuild {
        public @Nullable Liquid liquidConfig;

        @Override
        public void handleLanding() {
            if (liquidConfig == null) return;

            cooldown = 1f;
            arrivingLiquid = liquidConfig;
            arrivingTimer = 0f;
            liquidRemoved = 0f;
            landSound.at(x, y, 1f, landSoundVolume);

            if (state.isCampaign() && !isFake()) {
                logistics().resetLiquidImportTimer(liquidConfig);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;

                logistics().syncLiquidImportTimers(state.getPlanet(), landingVolume);

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
        public void draw() {
            super.draw();
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
                        PlanetLogistics.handleLiquidImport(state.getPlanet(), arrivingLiquid, landingVolume);
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
                PlanetLogisticsData data = logistics();
                float stored = liquids.get(liquidConfig);
                boolean hasRoom = stored <= liquidCapacity - landingVolume + 0.001f;
                if (cooldown <= 0f && efficiency > 0f && hasRoom && !isLanding()
                        && (isFake() || (data.getLiquidImportRate(state.getPlanet(), liquidConfig) > 0f
                        && data.liquidImportTimer(liquidConfig) >= 1f))) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        Seq<PlanetaryLiquidLandingPadBuild> pads = waiting.get(liquidConfig, () -> new Seq<>(false));
                        if (!pads.contains(this)) pads.add(this);
                    }
                }
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryLiquidLandingPad.this, table, content.liquids(), () -> liquidConfig, this::configure);
        }

        @Override
        public void display(Table table) {
            super.display(table);
            if (!state.isCampaign() || net.client() || team != player.team() || isFake() || liquidConfig == null) return;

            table.row();
            table.label(() -> {
                if (legacyDisabled()) return Core.bundle.get("landingpad.legacy.disabled");

                int sources = 0;
                float perSecond = 0f;
                for (Planet planet : content.planets()) {
                    if (planet == state.getPlanet() || !PlanetLogistics.hasBase(planet)) continue;
                    PlanetLogisticsData otherData = PlanetLogistics.get(planet);
                    if (otherData.destinationPlanet() != state.getPlanet()) continue;
                    float amount = otherData.getLiquidExport(liquidConfig);
                    if (amount <= 0f) continue;
                    sources++;
                    perSecond += amount;
                }

                String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
                if (perSecond > 0f) {
                    str += "\n" + Core.bundle.format("landing.import", liquidConfig.emoji(), (int) (perSecond * 60f));
                }
                return str;
            }).pad(4).wrap().width(200f).left();
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
