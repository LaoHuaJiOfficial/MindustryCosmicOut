package mod.extend.type.pad;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.world.blocks.campaign.LandingPad;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.consumers.ConsumeLiquid;

import static mindustry.Vars.*;

public abstract class ModLandingPad extends LandingPad {
    public ModLandingPad(String name) {
        super(name);
    }

    @Override
    public void init() {
        super.init();
        if (consumeLiquidAmount < 0) removeConsumers(c -> c instanceof ConsumeLiquid);
    }

    @Override
    public void setBars() {
        super.setBars();
        if (consumeLiquidAmount < 0) removeBar("liquid-" + consumeLiquid.name);
    }

    public abstract class ModLandingPadBuild extends LandingPadBuild {
        public Liquid arrivingLiquid;
        public UnlockableContent arrivingPayload;

        protected boolean legacyDisabled() {
            return state.isCampaign() && state.getPlanet().campaignRules.legacyLaunchPads;
        }

        protected boolean isLanding() {
            return arriving != null || arrivingLiquid != null || arrivingPayload != null;
        }

        @Override
        public void draw() {
            if (consumeLiquid != null && consumeLiquidAmount > 0f) {
                Draw.color(bottomColor);
                Fill.square(x, y, size * tilesize / 2f - liquidPad);
                Draw.color();
                LiquidBlock.drawTiledFrames(block.size, x, y, liquidPad, liquidPad, liquidPad, liquidPad, consumeLiquid, liquids.get(consumeLiquid) / liquidCapacity);
            }

            Draw.rect(region, x, y);

            if (isLanding()) {
                float fin = Mathf.clamp(arrivingTimer), fout = 1f - fin;
                float alpha = Interp.pow5Out.apply(fin);
                float scale = (1f - alpha) * 1.3f + 1f;
                float cx = x;
                float cy = y + Interp.pow4In.apply(fout) * (100f + Mathf.randomSeedRange(id() + 2, 30f));
                float rotation = fout * (90f + Mathf.randomSeedRange(id(), 50f));

                Draw.z(Layer.effect + 0.001f);
                Draw.color(Pal.engine);

                float rad = 0.15f + Interp.pow5Out.apply(Mathf.slope(fin));

                Fill.light(cx, cy, 10, 25f * (rad + scale - 1f), Tmp.c2.set(Pal.engine).a(alpha), Tmp.c1.set(Pal.engine).a(0f));

                Draw.alpha(alpha);
                for (int i = 0; i < 4; i++) {
                    Drawf.tri(cx, cy, 6f, 40f * (rad + scale - 1f), i * 90f + rotation);
                }

                Draw.color();
                Draw.z(Layer.weather - 1);

                scale *= podRegion.scl();
                float rw = podRegion.width * scale, rh = podRegion.height * scale;

                Draw.alpha(alpha);
                Drawf.shadow(cx, cy, size * tilesize, fin);
                Draw.rect(podRegion, cx, cy, rw, rh, rotation);

                Tmp.v1.trns(225f, Interp.pow3In.apply(fout) * 250f);

                Draw.z(Layer.flyingUnit + 1);
                Draw.color(0, 0, 0, 0.22f * alpha);
                Draw.rect(podRegion, cx + Tmp.v1.x, cy + Tmp.v1.y, rw, rh, rotation);
            } else if (cooldown > 0f) {
                Drawf.shadow(x, y, size * tilesize, cooldown);
                Draw.alpha(cooldown);
                Draw.mixcol(Pal.accent, 1f - cooldown);
                Draw.z(Layer.block + 0.01f);
                Draw.rect(podRegion, x, y);
            }

            Draw.reset();
        }

        @Override
        public void drawLight() {
            Drawf.light(x, y, lightRadius, Pal.accent, Mathf.clamp(Math.max(cooldown, isLanding() ? arrivingTimer * 1.5f : 0f)));
        }

        protected void updateArrivalParticles() {
            if (headless) return;

            float tsize = Interp.pow5Out.apply(arrivingTimer);
            landParticleTimer += tsize * Time.delta / 2f;
            if (landParticleTimer >= 1f) {
                tile.getLinkedTiles(t -> {
                    if (Mathf.chance(0.1f)) {
                        Fx.podLandDust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f),
                                Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                    }
                });
                landParticleTimer = 0f;
            }
        }

        protected void updateArrivalLiquidConsume() {
            arrivingTimer += Time.delta / arrivalDuration;

            if (consumeLiquidAmount > 0) {
                float toRemove = Math.min(consumeLiquidAmount / arrivalDuration * Time.delta, consumeLiquidAmount - liquidRemoved);
                liquidRemoved += toRemove;
                liquids.remove(consumeLiquid, toRemove);
            }

            if (Mathf.chanceDelta(coolingEffectChance * Interp.pow5Out.apply(arrivingTimer))) {
                coolingEffect.at(this);
            }
        }

        protected void finishArrivalEffects() {
            if (consumeLiquidAmount > 0) {
                liquids.remove(consumeLiquid, consumeLiquidAmount - liquidRemoved);
            }
            landEffect.at(this);
            Effect.shake(3f, 3f, this);
        }

        protected void updateCooldown() {
            if (!isLanding()) {
                cooldown -= delta() / cooldownTime;
                cooldown = Mathf.clamp(cooldown);
            }
        }
    }
}
