package mod.extend.type;

import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.units.UnitAssembler.YeetData;

public final class PayloadDirectedOutput {
    private PayloadDirectedOutput() {
    }

    public static void yeetPayload(Building build, Payload payload) {
        Vec2 spawn = new Vec2(build.x, build.y);
        float rot = payload.angleTo(spawn);
        Fx.payloadDeposit.at(payload.x(), payload.y(), rot, new YeetData(spawn, payload.content()));
    }

    public static void updatePayload(Payload payload, Vec2 payVector, float payRotation, float x, float y) {
        if (payload != null) {
            payload.set(x + payVector.x, y + payVector.y, payRotation);
        }
    }

    public static void drawPayload(Payload payload, Vec2 payVector, float payRotation, float x, float y) {
        updatePayload(payload, payVector, payRotation, x, y);
        Draw.z(Layer.blockOver);
        payload.draw();
    }

    public static boolean moveInPayload(Building build, Payload payload, Vec2 payVector, float[] payRotation, float payloadSpeed, float payloadRotateSpeed, boolean rotateTowardsBlock) {
        if (payload == null) return false;

        updatePayload(payload, payVector, payRotation[0], build.x, build.y);

        if (rotateTowardsBlock) {
            payRotation[0] = Angles.moveToward(payRotation[0], build.block.rotate ? build.rotdeg() : 90f, payloadRotateSpeed * build.delta());
        }
        payVector.approach(Vec2.ZERO, payloadSpeed * build.delta());

        return payVector.isZero(0.01f);
    }

    public static void moveOutPayload(Building build, Payload payload, Vec2 payVector, float[] payRotation, float payloadSpeed, float payloadRotateSpeed, Runnable onReleased) {
        if (payload == null) return;

        float tilesize = build.block.size * 8f;

        updatePayload(payload, payVector, payRotation[0], build.x, build.y);

        Tmp.v1.trns(build.rotdeg(), tilesize / 2f);
        payRotation[0] = Angles.moveToward(payRotation[0], build.rotdeg(), payloadRotateSpeed * build.delta());
        payVector.approach(Tmp.v1, payloadSpeed * build.delta());

        Building front = build.front();
        boolean canDump = front == null || !front.tile.solid();
        boolean canMove = front != null && (front.block.outputsPayload || front.block.acceptsPayload);

        if (canDump && !canMove) {
            PayloadBlock.pushOutput(payload, 1f - (payVector.dst(Tmp.v1) / (tilesize / 2f)));
        }

        if (payVector.within(Tmp.v1, 0.001f)) {
            payVector.clamp(-tilesize / 2f, -tilesize / 2f, tilesize / 2f, tilesize / 2f);

            if (canMove) {
                if (build.movePayload(payload)) {
                    onReleased.run();
                }
            } else if (canDump) {
                dumpPayload(payload, onReleased);
            }
        }
    }

    static void dumpPayload(Payload payload, Runnable onReleased) {
        float tx = Angles.trnsx(payload.rotation(), 0.1f);
        float ty = Angles.trnsy(payload.rotation(), 0.1f);
        payload.set(payload.x() + tx, payload.y() + ty, payload.rotation());

        if (payload.dump()) {
            onReleased.run();
        } else {
            payload.set(payload.x() - tx, payload.y() - ty, payload.rotation());
        }
    }
}
