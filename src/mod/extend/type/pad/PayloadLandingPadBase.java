package mod.extend.type.pad;

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
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.extend.type.PayloadDirectedOutput;

import static mindustry.Vars.*;

public abstract class PayloadLandingPadBase extends ModLandingPad {
    static ObjectMap<UnlockableContent, Seq<PayloadLandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static {
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public float payloadSpeed = 0.7f, payloadRotateSpeed = 5f;
    public TextureRegion outRegion;

    public PayloadLandingPadBase(String name) {
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

        config(Block.class, (PayloadLandingPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
            build.clearOutputPayload();
        });

        config(UnitType.class, (PayloadLandingPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
            build.clearOutputPayload();
        });

        configClear((PayloadLandingPadBuild build) -> {
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
        addBar("payload", (PayloadLandingPadBuild build) -> new Bar(
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

    protected abstract void resetPayloadImportTimer(PayloadLandingPadBuild build, UnlockableContent content);

    protected abstract void syncPayloadImportTimers();

    protected abstract void handlePayloadImport(UnlockableContent content, int amount);

    protected abstract boolean canRequestPayloadImport(PayloadLandingPadBuild build, UnlockableContent content);

    protected abstract String buildImportSourcesLabel(UnlockableContent content);

    public class PayloadLandingPadBuild extends ModLandingPadBuild {
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
                resetPayloadImportTimer(this, config);
            }
        }

        @Override
        public void updateTimers() {
            if (state.isCampaign() && lastUpdateId != state.updateId) {
                lastUpdateId = state.updateId;
                syncPayloadImportTimers();

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
                            handlePayloadImport(arrivingPayload, 1);
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
                if (cooldown <= 0f && efficiency > 0f && payload == null && !isLanding() && canRequestPayloadImport(this, config)) {
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

                TextureRegion region = payloadConfig().fullIcon;
                scale *= region.scl();
                float rw = region.width * scale, rh = region.height * scale;

                Draw.color();
                Draw.alpha(alpha);
                Draw.z(Layer.weather - 1.01f);
                Draw.rect(region, cx, cy, rw, rh, rotation);
            }

            Draw.scl(scl);
            if (payload != null) {
                PayloadDirectedOutput.drawPayload(payload, payVector, payRotation, x, y);
            }
            Draw.reset();
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PayloadLandingPadBase.this, table,
                    content.blocks().select(PayloadLandingPadBase.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PayloadLandingPadBase.this::canProduce).<UnlockableContent>as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);
        }

        @Override
        public void drawSelect() {
            drawItemSelection(payloadConfig());
        }

        @Override
        protected String buildImportDisplayLabel() {
            UnlockableContent config = payloadConfig();
            if (config == null) return null;
            return buildImportSourcesLabel(config);
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
