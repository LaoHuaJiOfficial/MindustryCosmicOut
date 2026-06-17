package mod.extend.type.pad;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.LaunchPayload;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.world.blocks.campaign.LaunchPad;
import mindustry.world.blocks.liquid.LiquidBlock;

import static mindustry.Vars.*;

public abstract class ModLaunchPad extends LaunchPad {
    public ModLaunchPad(String name) {
        super(name);
        launchTime = 60f;
    }

    public abstract class ModLaunchPadBuild extends LaunchPadBuild {
        protected float launchFillRatio() {
            return hasItems ? (float) items.total() / itemCapacity : 0f;
        }

        protected @Nullable Liquid displayLiquid() {
            return drawLiquid;
        }

        protected float displayLiquidRatio(Liquid liquid) {
            return liquids.get(liquid) / liquidCapacity;
        }

        @Override
        public void draw() {
            Liquid liquid = displayLiquid();
            if (hasLiquids && liquid != null) {
                Draw.color(bottomColor);
                Fill.square(x, y, size * tilesize / 2f - liquidPad);
                Draw.color();
                LiquidBlock.drawTiledFrames(block.size, x, y, liquidPad, liquidPad, liquidPad, liquidPad, liquid, displayLiquidRatio(liquid));
            }

            Draw.rect(region, x, y);

            if (lightRegion.found()) {
                Draw.color(lightColor);
                float progress = Math.min(launchFillRatio(), launchCounter / launchTime);

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

        protected void fireLaunchVisual() {
            consume();
            launchSound.at(x, y, 1f + Mathf.range(launchSoundPitchRand));
            LaunchPayload entity = LaunchPayload.create();
            entity.set(this);
            entity.lifetime(120f);
            entity.team(team);
            entity.add();
            Fx.launchPod.at(this);
            Effect.shake(3f, 3f, this);
            launchCounter = 0f;
        }

        protected boolean readyToLaunch() {
            return (launchCounter += edelta()) >= launchTime;
        }

        protected abstract void buildDestinationConfig(Table table);
    }
}
