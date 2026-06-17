package mod.extend.type.cargopad;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.type.PayloadSeq;
import mindustry.type.Planet;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.extend.type.PayloadDirectedOutput;

import static mindustry.Vars.*;

public class PlanetaryPayloadLandingPad extends CargoLandingPad {
    static ObjectMap<UnlockableContent, Seq<PlanetaryPayloadLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public float payloadSpeed = 0.7f, payloadRotateSpeed = 5f;
    public TextureRegion outRegion;

    public PlanetaryPayloadLandingPad(String name) {
        super(name);
        hasItems = false;
        acceptsItems = false;
        outputsPayload = true;
        update = true;
        rotate = true;
        commandable = true;
        clipSize = 120;
        regionRotated1 = 1;
        selectionRows = selectionColumns = 8;

        config(Block.class, (PlanetaryPayloadLandingPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
            build.clearOutputPayload();
        });

        config(UnitType.class, (PlanetaryPayloadLandingPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
            build.clearOutputPayload();
        });

        configClear((PlanetaryPayloadLandingPadBuild build) -> {
            if (!build.accessible()) return;
            build.configBlock = null;
            build.unit = null;
            build.clearOutputPayload();
        });
    }

    @Override
    public void load() {
        super.load();
        TextureRegion found = Core.atlas.find(name + "-out");
        if (!found.found() && minfo.mod != null) found = Core.atlas.find(minfo.mod.name + "-factory-out-" + size);
        if (!found.found()) found = Core.atlas.find("factory-out-" + size);
        outRegion = found;
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, outRegion};
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
        addBar("payload", (PlanetaryPayloadLandingPadBuild build) -> new Bar(
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null || build.payload == null ? Iconc.cancel + "" : config.localizedName;
                },
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null ? Color.clear : Pal.ammo;
                },
                () -> build.payload != null ? 1f : 0f
        ));
    }

    public class PlanetaryPayloadLandingPadBuild extends CargoLandingPadBuild {
        public Block configBlock;
        public UnitType unit;
        public @Nullable Payload payload;
        public @Nullable Vec2 commandPos;
        public Vec2 payVector = new Vec2();
        public float payRotation, scl;

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        public @Nullable UnlockableContent payloadConfig() {
            return unit != null ? unit : configBlock;
        }

        public void clearOutputPayload() {
            payload = null;
            scl = 0f;
            payVector.setZero();
        }

        @Override
        public Vec2 getCommandPosition() {
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target) {
            commandPos = target;
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

                logistics().syncPayloadImportTimers(state.getPlanet(), 1);

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
                    if (arrivingPayload != null && payload == null) {
                        payload = payloadOf(arrivingPayload);
                        if (payload != null) {
                            if (payload instanceof UnitPayload up) {
                                Unit u = up.unit;
                                if (commandPos != null && u.isCommandable()) {
                                    u.command().commandPosition(commandPos);
                                }
                                Events.fire(new UnitCreateEvent(u, this));
                            }
                            payVector.setZero();
                            payRotation = rotdeg();
                            scl = 0f;
                        }
                        if (!isFake()) {
                            PlanetLogistics.handlePayloadImport(state.getPlanet(), arrivingPayload, 1);
                        }
                    }
                    arrivingPayload = null;
                    arrivingTimer = 0f;
                }
            }

            if (payload != null) {
                updatePayloadOutput();
            }

            updateCooldown();

            UnlockableContent config = payloadConfig();
            if (config != null && (isFake() || (state.isCampaign() && !legacyDisabled()))) {
                PlanetLogisticsData data = logistics();
                if (cooldown <= 0f && efficiency > 0f && payload == null && !isLanding()
                        && (isFake() || (data.getPayloadImportRate(state.getPlanet(), config) > 0f
                        && data.payloadImportTimer(config) >= 1f))) {
                    if (isFake()) {
                        Call.landingPadLanded(tile);
                    } else {
                        waiting.get(config, () -> new Seq<>(false)).add(this);
                    }
                }
            }
        }

        protected void updatePayloadOutput() {
            if (isLanding() || payload == null) return;

            if (payVector.isZero(0.01f)) {
                scl = 0f;
            } else {
                scl = Mathf.lerpDelta(scl, 1f, 0.1f);
            }

            float[] rot = {payRotation};
            PayloadDirectedOutput.moveOutPayload(this, payload, payVector, rot, payloadSpeed, payloadRotateSpeed, this::releaseOutputPayload);
            payRotation = rot[0];
        }

        protected void releaseOutputPayload() {
            payload = null;
        }

        protected Payload payloadOf(UnlockableContent content) {
            if (content instanceof Block b) return new BuildPayload(b, team);
            if (content instanceof UnitType type) return new UnitPayload(type.create(team));
            return null;
        }

        @Override
        public void onRemoved() {
            super.onRemoved();
            if (payload != null) payload.dump();
        }

        @Override
        public void draw() {
            super.draw();

            Draw.z(Layer.block);
            if (outRegion.found()) Draw.rect(outRegion, x, y, rotdeg());

            if (isLanding() && payloadConfig() != null) {
                float fin = Mathf.clamp(arrivingTimer), fout = 1f - fin;
                float alpha = Interp.pow5Out.apply(fin);
                float scale = (1f - alpha) * 1.3f + 1f;
                float cx = x;
                float cy = y + Interp.pow4In.apply(fout) * (100f + Mathf.randomSeedRange(id() + 2, 30f));
                float rotation = fout * (90f + Mathf.randomSeedRange(id(), 50f)) + rotdeg() - 90f;

                TextureRegion icon = payloadConfig().fullIcon;
                scale *= icon.scl();
                float rw = icon.width * scale, rh = icon.height * scale;

                Draw.color();
                Draw.alpha(alpha);
                Draw.z(Layer.weather - 1.01f);
                Draw.rect(icon, cx, cy, rw, rh, rotation);
            }

            Draw.scl(scl);
            if (payload != null) {
                PayloadDirectedOutput.drawPayload(payload, payVector, payRotation, x, y);
            }
            Draw.reset();
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryPayloadLandingPad.this, table,
                    content.blocks().select(PlanetaryPayloadLandingPad.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PlanetaryPayloadLandingPad.this::canProduce).<UnlockableContent>as()),
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
                for (Planet planet : content.planets()) {
                    if (planet == state.getPlanet() || !PlanetLogistics.hasBase(planet)) continue;
                    PlanetLogisticsData otherData = PlanetLogistics.get(planet);
                    if (otherData.destinationPlanet() != state.getPlanet()) continue;
                    float amount = otherData.getPayloadExport(config);
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
        public void write(Writes write) {
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            write.s(configBlock == null ? -1 : configBlock.id);
            TypeIO.writeVecNullable(write, commandPos);
            TypeIO.writeVecNullable(write, payVector);
            write.f(payRotation);
            write.f(scl);
            Payload.write(payload, write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            unit = content.unit(read.s());
            configBlock = content.block(read.s());
            commandPos = TypeIO.readVecNullable(read);
            payVector = TypeIO.readVecNullable(read);
            payRotation = read.f();
            scl = read.f();
            payload = Payload.read(read);
        }
    }
}
