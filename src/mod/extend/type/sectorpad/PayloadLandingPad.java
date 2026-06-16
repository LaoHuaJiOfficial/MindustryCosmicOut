package mod.extend.type.sectorpad;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.graphics.Pal;
import mindustry.type.PayloadSeq;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.extend.sector.SectorLogistics;
import mod.extend.sector.SectorLogisticsData;

import static mindustry.Vars.*;

public class PayloadLandingPad extends SectorLandingPad {
    static ObjectMap<UnlockableContent, Seq<PayloadLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public int stackCapacity = 16;
    public int landingBatch = 1;

    public PayloadLandingPad(String name) {
        super(name);
        hasItems = false;
        acceptsItems = false;
        outputsPayload = true;
        update = true;

        config(Block.class, (PayloadLandingPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
        });

        config(UnitType.class, (PayloadLandingPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
        });

        configClear((PayloadLandingPadBuild build) -> {
            if (!build.accessible()) return;
            build.configBlock = null;
            build.unit = null;
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
        addBar("payload", (PayloadLandingPadBuild build) -> new Bar(
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

    public class PayloadLandingPadBuild extends SectorLandingPadBuild {
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
        public void handleLanding() {
            UnlockableContent config = payloadConfig();
            if (config == null) return;

            cooldown = 1f;
            arrivingPayload = config;
            arrivingTimer = 0f;
            liquidRemoved = 0f;
            landSound.at(x, y, 1f, landSoundVolume);

            if (state.isCampaign() && !isFake()) {
                logistics().resetPayloadImportTimer(config);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;

                logistics().syncPayloadImportTimers(state.getPlanet(), state.getSector(), landingBatch);

                waiting.each((content, pads) -> {
                    pads.removeAll(l -> l.payloadConfig() != content);
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

            if (stacks.total() > 0) {
                dumpStoredPayloads();
            }

            updateCooldown();

            UnlockableContent config = payloadConfig();
            if (config != null && (isFake() || (state.isCampaign() && !legacyDisabled()))) {
                SectorLogisticsData data = logistics();
                if (cooldown <= 0f && efficiency > 0f && stacks.total() < stackCapacity && !isLanding()
                        && (isFake() || (data.getPayloadImportRate(state.getPlanet(), state.getSector(), config) > 0f
                        && data.payloadImportTimer(config) >= 1f))) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        waiting.get(config, () -> new Seq<>(false)).add(this);
                    }
                }
            }
        }

        protected void dumpStoredPayloads() {
            if (stacks.isEmpty() || isLanding()) return;

            UnlockableContent config = payloadConfig();
            if (config != null && stacks.get(config) != stacks.total()) return;

            UnlockableContent content = config;
            if (content == null) {
                if (configBlock != null && stacks.contains(configBlock)) content = configBlock;
                else if (unit != null && stacks.contains(unit)) content = unit;
            }
            if (content == null || !stacks.contains(content)) return;

            Payload payload = payloadOf(content);
            if (payload == null) return;

            if (dumpPayload(payload)) {
                stacks.remove(content, 1);
            } else if (payload instanceof UnitPayload up) {
                up.unit.remove();
            }
        }

        protected Payload payloadOf(UnlockableContent content) {
            if (content instanceof Block b) return new BuildPayload(b, team);
            if (content instanceof UnitType type) return new UnitPayload(type.create(team));
            return null;
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PayloadLandingPad.this, table,
                    content.blocks().select(PayloadLandingPad.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PayloadLandingPad.this::canProduce).<UnlockableContent>as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);
        }

        @Override
        public void display(Table table) {
            super.display(table);
            if (!state.isCampaign() || net.client() || team != player.team() || isFake()) return;

            UnlockableContent config = payloadConfig();
            if (config == null) return;

            table.row();
            table.label(() -> {
                if (legacyDisabled()) return Core.bundle.get("landingpad.legacy.disabled");

                int sources = 0;
                float perSecond = 0f;
                for (Sector other : state.getPlanet().sectors) {
                    if (other == state.getSector() || !other.hasBase() || other.info.destination != state.getSector()) continue;
                    float amount = SectorLogistics.get(other).getPayloadExport(config);
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
