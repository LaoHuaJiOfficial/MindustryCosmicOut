package mod.extend.type.cargopad;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Call;
import mindustry.type.PayloadSeq;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;

import static mindustry.Vars.*;

public class PayloadCargoLandingPad extends CargoLandingPad {
    static ObjectMap<UnlockableContent, Seq<PayloadCargoLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public int stackCapacity = 16;
    public int landingBatch = 1;

    public PayloadCargoLandingPad(String name) {
        super(name);
        hasItems = false;
        acceptsItems = false;
        update = true;

        config(UnlockableContent.class, (PayloadCargoLandingPadBuild build, UnlockableContent content) -> {
            if (!build.accessible() || content == null || !content.unlockedNow()) return;
            build.payloadConfig = content;
        });

        configClear((PayloadCargoLandingPadBuild build) -> {
            if (!build.accessible()) return;
            build.payloadConfig = null;
        });
    }

    public class PayloadCargoLandingPadBuild extends CargoLandingPadBuild {
        public @Nullable UnlockableContent payloadConfig;
        public PayloadSeq stacks = new PayloadSeq();

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        @Override
        public void handleLanding() {
            if (payloadConfig == null) return;

            cooldown = 1f;
            arrivingPayload = payloadConfig;
            arrivingTimer = 0f;
            liquidRemoved = 0f;
            landSound.at(x, y, 1f, landSoundVolume);

            if (state.isCampaign() && !isFake()) {
                logistics().resetPayloadImportTimer(payloadConfig);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;

                logistics().syncPayloadImportTimers(state.getPlanet(), state.getSector(), landingBatch);

                waiting.each((content, pads) -> {
                    pads.removeAll(l -> l.payloadConfig != content);
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

            if (arrivingPayload != null) {
                updateArrivalParticles();
                updateArrivalLiquidConsume();

                if (arrivingTimer >= 1f) {
                    finishArrivalEffects();
                    int room = stackCapacity - stacks.total();
                    int amount = Math.min(landingBatch, room);
                    if (amount > 0) {
                        stacks.add(arrivingPayload, amount);
                        if (!isFake()) {
                            SectorLogistics.handlePayloadImport(state.getSector(), arrivingPayload, amount);
                        }
                    }
                    arrivingPayload = null;
                    arrivingTimer = 0f;
                }
            }

            updateCooldown();

            if (payloadConfig != null && (isFake() || (state.isCampaign() && !legacyDisabled()))) {
                SectorLogisticsData data = logistics();
                if (cooldown <= 0f && efficiency > 0f && stacks.total() < stackCapacity && !isLanding()
                        && (isFake() || (data.getPayloadImportRate(state.getPlanet(), state.getSector(), payloadConfig) > 0f
                        && data.payloadImportTimer(payloadConfig) >= 1f))) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        waiting.get(payloadConfig, () -> new Seq<>(false)).add(this);
                    }
                }
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            CargoPadUI.buildPayloadTable(PayloadCargoLandingPad.this, table, CargoPadUI.payloadOptions(), () -> payloadConfig, this::configure, selectionColumns);
        }

        @Override
        public void display(Table table) {
            super.display(table);
            if (!state.isCampaign() || net.client() || team != player.team() || isFake() || payloadConfig == null) return;

            table.row();
            table.label(() -> {
                if (legacyDisabled()) return Core.bundle.get("landingpad.legacy.disabled");

                int sources = 0;
                float perSecond = 0f;
                for (Planet planet : content.planets()) {
                    for (Sector other : planet.sectors) {
                        if (other == state.getSector() || !other.hasBase() || other.info.destination != state.getSector()) continue;
                        float amount = SectorLogistics.get(other).getPayloadExport(payloadConfig);
                        if (amount <= 0f) continue;
                        sources++;
                        perSecond += amount;
                    }
                }

                String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
                if (perSecond > 0f) {
                    str += "\n" + Core.bundle.format("landing.import", payloadConfig.emoji(), (int) (perSecond * 60f));
                }
                return str;
            }).pad(4).wrap().width(200f).left();
        }

        @Override
        public @Nullable Object config() {
            return payloadConfig;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            stacks.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            stacks.read(read);
        }
    }
}
