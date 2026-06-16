package mod.extend.type.cargopad;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.type.Planet;
import mindustry.world.blocks.ItemSelection;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;

import static mindustry.Vars.*;

public class PlanetaryItemLandingPad extends CargoLandingPad {
    static ObjectMap<Item, Seq<PlanetaryItemLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public PlanetaryItemLandingPad(String name) {
        super(name);
    }

    public class PlanetaryItemLandingPadBuild extends CargoLandingPadBuild {
        @Override
        public void handleLanding() {
            if (config == null) return;

            cooldown = 1f;
            arriving = config;
            arrivingTimer = 0f;
            liquidRemoved = 0f;
            landSound.at(x, y, 1f, landSoundVolume);

            if (state.isCampaign() && !isFake()) {
                logistics().resetItemImportTimer(config);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;

                logistics().syncItemImportTimers(state.getPlanet(), itemCapacity);

                waiting.each((item, pads) -> {
                    pads.removeAll(l -> l.config != item);
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

            if (arriving != null) {
                updateArrivalParticles();
                updateArrivalLiquidConsume();

                if (arrivingTimer >= 1f) {
                    finishArrivalEffects();
                    items.set(arriving, itemCapacity);
                    if (!isFake()) {
                        produced(arriving, itemCapacity);
                        PlanetLogistics.handleItemImport(state.getPlanet(), arriving, itemCapacity);
                    }
                    arriving = null;
                    arrivingTimer = 0f;
                }
            }

            if (items.total() > 0) {
                dumpAccumulate(config == null || items.get(config) != items.total() ? null : config);
            }

            updateCooldown();

            if (config != null && (isFake() || (state.isCampaign() && !legacyDisabled()))) {
                PlanetLogisticsData data = logistics();
                if (cooldown <= 0f && efficiency > 0f && items.total() == 0 && !isLanding()
                        && (isFake() || (data.getItemImportRate(state.getPlanet(), config) > 0f
                        && data.itemImportTimer(config) >= 1f))) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        waiting.get(config, () -> new Seq<>(false)).add(this);
                    }
                }
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryItemLandingPad.this, table, content.items(), () -> config, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public void display(Table table) {
            super.display(table);
            if (!state.isCampaign() || net.client() || team != player.team() || isFake()) return;

            table.row();
            table.label(() -> {
                if (legacyDisabled() || config == null) return legacyDisabled() ? Core.bundle.get("landingpad.legacy.disabled") : "";

                int sources = 0;
                float perSecond = 0f;
                for (Planet planet : content.planets()) {
                    if (planet == state.getPlanet() || !PlanetLogistics.hasBase(planet)) continue;
                    PlanetLogisticsData otherData = PlanetLogistics.get(planet);
                    if (otherData.destinationPlanet() != state.getPlanet()) continue;
                    float amount = otherData.getItemExport(config);
                    if (amount <= 0f) continue;
                    sources++;
                    perSecond += amount;
                }

                String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
                if (perSecond > 0f) {
                    str += "\n" + Core.bundle.format("landing.import", config.emoji(), (int) (perSecond * 60f));
                }
                return str;
            }).pad(4).wrap().width(200f).left();
        }
    }
}
