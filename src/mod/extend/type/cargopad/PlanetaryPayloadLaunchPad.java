package mod.extend.type.cargopad;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.type.PayloadSeq;
import mindustry.type.Planet;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.ModUI;
import mod.extend.sector.PlanetLogistics;
import mod.extend.type.PayloadDirectedOutput;

import static mindustry.Vars.*;

public class PlanetaryPayloadLaunchPad extends CargoLaunchPad {
    public float payloadSpeed = 0.7f, payloadRotateSpeed = 5f;
    public TextureRegion topRegion, inRegion;

    public PlanetaryPayloadLaunchPad(String name) {
        super(name);
        hasItems = false;
        acceptsPayload = true;
        update = true;
        configurable = true;
        rotate = true;
        clipSize = 120;
        regionRotated1 = 1;
        selectionRows = selectionColumns = 8;

        config(Block.class, (PlanetaryPayloadLaunchPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
            build.clearPayload();
        });

        config(UnitType.class, (PlanetaryPayloadLaunchPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
            build.clearPayload();
        });

        configClear((PlanetaryPayloadLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.configBlock = null;
            build.unit = null;
            build.clearPayload();
        });
    }

    @Override
    public void load() {
        super.load();
        topRegion = findFactoryRegion("-top");
        inRegion = findFactoryRegion("-in");
    }

    protected TextureRegion findFactoryRegion(String suf) {
        TextureRegion found = Core.atlas.find(name + suf);
        if (!found.found() && minfo.mod != null) found = Core.atlas.find(minfo.mod.name + "-factory" + suf + "-" + size);
        if (!found.found()) found = Core.atlas.find("factory" + suf + "-" + size);
        return found;
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, topRegion};
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
        removeBar("progress");
        addBar("payload", (PlanetaryPayloadLaunchPadBuild build) -> new Bar(
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null || build.payload == null ? Iconc.cancel + "" : config.localizedName;
                },
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null ? Color.clear : Pal.ammo;
                },
                () -> build.launchFillRatio()
        ));
    }

    public class PlanetaryPayloadLaunchPadBuild extends CargoLaunchPadBuild {
        public Block configBlock;
        public UnitType unit;
        public @Nullable Payload payload;
        public Vec2 payVector = new Vec2();
        public float payRotation;

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        public @Nullable UnlockableContent payloadConfig() {
            return unit != null ? unit : configBlock;
        }

        public void clearPayload() {
            payload = null;
            payVector.setZero();
            launchCounter = 0f;
        }

        public boolean matchesPayload(Payload p) {
            UnlockableContent config = payloadConfig();
            return config != null && p.content() == config;
        }

        protected Payload payloadOf(UnlockableContent content) {
            if (content instanceof Block b) return new BuildPayload(b, team);
            if (content instanceof UnitType type) return new UnitPayload(type.create(team));
            return null;
        }

        public boolean blends(int direction) {
            return PayloadBlock.blends(this, direction);
        }

        @Override
        protected float launchFillRatio() {
            if (payload == null) return 0f;
            if (!payVector.isZero(0.01f)) {
                float max = block.size * tilesize / 2f;
                return 1f - Mathf.clamp(payVector.len() / max);
            }
            return Mathf.clamp(launchCounter / launchTime);
        }

        @Override
        public void updateTile() {
            if (payload != null && !payVector.isZero(0.01f)) {
                payload.update(null, this);
                float[] rot = {payRotation};
                boolean arrived = PayloadDirectedOutput.moveInPayload(this, payload, payVector, rot, payloadSpeed, payloadRotateSpeed, true);
                payRotation = rot[0];

                if (!arrived) {
                    launchCounter = 0f;
                    return;
                }

                PayloadDirectedOutput.yeetPayload(this, payload);
                payVector.setZero();
                payRotation = rotdeg();
                launchCounter = 0f;
            }

            if (payload == null) return;

            payload.update(null, this);
            PayloadDirectedOutput.updatePayload(payload, payVector, payRotation, x, y);

            if (!readyToLaunch()) return;

            UnlockableContent content = payload.content();
            if (content == null) return;

            PlanetLogistics.handlePayloadExport(state.getPlanet(), content, 1);

            fireLaunchVisual();
            payload.remove();
            clearPayload();
        }

        @Override
        public boolean acceptPayload(Building source, Payload p) {
            return payload == null && matchesPayload(p);
        }

        @Override
        public void handlePayload(Building source, Payload p) {
            payload = p;
            payVector.set(source).sub(this).clamp(-size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f, size * tilesize / 2f);
            payRotation = p.rotation();
            launchCounter = 0f;
        }

        @Override
        public void onRemoved() {
            super.onRemoved();
            if (payload != null) payload.dump();
        }

        @Override
        public void draw() {
            var liquid = displayLiquid();
            if (hasLiquids && liquid != null) {
                Draw.color(bottomColor);
                Fill.square(x, y, size * tilesize / 2f - liquidPad);
                Draw.color();
                LiquidBlock.drawTiledFrames(block.size, x, y, liquidPad, liquidPad, liquidPad, liquidPad, liquid, displayLiquidRatio(liquid));
            }

            Draw.rect(region, x, y);

            for (int i = 0; i < 4; i++) {
                if (blends(i) && inRegion.found()) {
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            if (topRegion.found()) Draw.rect(topRegion, x, y);

            if (lightRegion.found()) {
                Draw.color(lightColor);
                float progress = launchFillRatio();

                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < lightSteps; j++) {
                        float alpha = Mathf.curve(progress, (float) j / lightSteps, (j + 1f) / lightSteps);
                        float offset = -(j - 1f) * lightStep;

                        Draw.color(Pal.metalGrayDark, lightColor, alpha);
                        Draw.rect(lightRegion, x + Geometry.d8edge(i).x * offset, y + Geometry.d8edge(i).y * offset, i * 90);
                    }
                }

                Draw.reset();
            }

            if (payload != null) {
                PayloadDirectedOutput.drawPayload(payload, payVector, payRotation, x, y);
            } else {
                Drawf.shadow(x, y, size * tilesize);
                Draw.rect(podRegion, x, y);
            }

            Draw.reset();
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PlanetaryPayloadLaunchPad.this, table,
                    content.blocks().select(PlanetaryPayloadLaunchPad.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PlanetaryPayloadLaunchPad.this::canProduce).<UnlockableContent>as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);

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
        public @Nullable Object config() {
            return unit == null ? configBlock : unit;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            write.s(configBlock == null ? -1 : configBlock.id);
            write.f(payRotation);
            TypeIO.writeVecNullable(write, payVector);
            Payload.write(payload, write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            unit = content.unit(read.s());
            configBlock = content.block(read.s());
            payRotation = read.f();
            payVector = TypeIO.readVecNullable(read);
            payload = Payload.read(read);
        }
    }
}
