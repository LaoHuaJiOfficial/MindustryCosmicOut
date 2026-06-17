package mod.extend.type.pad;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Iconc;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.PayloadSeq;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mod.extend.type.PayloadDirectedOutput;

import static mindustry.Vars.*;

public abstract class PayloadLaunchPadBase extends ModLaunchPad {
    public int payloadCapacity = 1;
    public int payloadLaunchCount = 1;
    public TextureRegion inRegion;

    public PayloadLaunchPadBase(String name) {
        super(name);
        hasItems = false;
        acceptsPayload = true;
        update = true;
        configurable = true;
        clipSize = 120;
        regionRotated1 = 1;
        selectionRows = selectionColumns = 8;

        config(Block.class, (PayloadLaunchPadBuild build, Block block) -> {
            if (!build.accessible() || !canProduce(block) || build.configBlock == block) return;
            build.configBlock = block;
            build.unit = null;
            build.clearPayload();
        });

        config(UnitType.class, (PayloadLaunchPadBuild build, UnitType unit) -> {
            if (!build.accessible() || !canProduce(unit) || build.unit == unit) return;
            build.unit = unit;
            build.configBlock = null;
            build.clearPayload();
        });

        configClear((PayloadLaunchPadBuild build) -> {
            if (!build.accessible()) return;
            build.configBlock = null;
            build.unit = null;
            build.clearPayload();
        });
    }

    @Override
    public void load() {
        super.load();
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
        return new TextureRegion[]{region};
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
        addBar("payload", (PayloadLaunchPadBuild build) -> new Bar(
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null || build.payloadseq.isEmpty() ? Iconc.cancel + "" : config.localizedName;
                },
                () -> {
                    UnlockableContent config = build.payloadConfig();
                    return config == null ? Color.clear : Pal.ammo;
                },
                build::launchFillRatio
        ));
    }

    protected abstract void exportPayload(UnlockableContent config, int count);

    public abstract class PayloadLaunchPadBuild extends ModLaunchPadBuild {
        public Block configBlock;
        public UnitType unit;
        public PayloadSeq payloadseq = new PayloadSeq();

        public boolean accessible() {
            return state.rules.editor || state.isCampaign() || team != state.rules.defaultTeam;
        }

        public @Nullable UnlockableContent payloadConfig() {
            return unit != null ? unit : configBlock;
        }

        public void clearPayload() {
            payloadseq.clear();
            launchCounter = 0f;
        }

        public boolean matchesPayload(Payload p) {
            UnlockableContent config = payloadConfig();
            return config != null && p.content() == config;
        }

        public boolean blends(int direction) {
            return PayloadBlock.blends(this, direction);
        }

        @Override
        protected float launchFillRatio() {
            if (payloadseq.isEmpty()) return 0f;
            UnlockableContent config = payloadConfig();
            if (config == null || payloadseq.get(config) < payloadLaunchCount) {
                return (float) payloadseq.total() / payloadCapacity;
            }
            return Mathf.clamp(launchCounter / launchTime);
        }

        @Override
        public void updateTile() {
            UnlockableContent config = payloadConfig();
            if (config == null || payloadseq.get(config) < payloadLaunchCount) {
                launchCounter = 0f;
                return;
            }

            if (!readyToLaunch()) return;

            exportPayload(config, payloadLaunchCount);
            fireLaunchVisual();
            payloadseq.remove(config, payloadLaunchCount);
            if (payloadseq.isEmpty()) launchCounter = 0f;
        }

        @Override
        public boolean acceptPayload(Building source, Payload p) {
            return payloadseq.total() < payloadCapacity && matchesPayload(p);
        }

        @Override
        public void handlePayload(Building source, Payload p) {
            PayloadDirectedOutput.yeetPayload(this, p);
            if (p instanceof BuildPayload bp) {
                payloadseq.add(bp.build.block, 1);
            } else if (p instanceof UnitPayload up) {
                payloadseq.add(up.unit.type, 1);
            }
            p.remove();
            launchCounter = 0f;
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

            Drawf.shadow(x, y, size * tilesize);
            Draw.rect(podRegion, x, y);

            Draw.reset();
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(PayloadLaunchPadBase.this, table,
                    content.blocks().select(PayloadLaunchPadBase.this::canProduce).<UnlockableContent>as()
                            .add(content.units().select(PayloadLaunchPadBase.this::canProduce).<UnlockableContent>as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);

            if (!state.isCampaign() || net.client()) {
                deselect();
                return;
            }

            table.row();
            buildDestinationConfig(table);
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
            payloadseq.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            unit = content.unit(read.s());
            configBlock = content.block(read.s());
            payloadseq.read(read);
        }
    }
}
