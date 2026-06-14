package mod.extend.type.cargopad;

import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.ctype.UnlockableContent;
import mindustry.type.PayloadSeq;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mod.extend.sector.SectorLogistics;

import static mindustry.Vars.*;

public class PayloadCargoLaunchPad extends CargoLaunchPad {
    public int stackCapacity = 16;

    public PayloadCargoLaunchPad(String name) {
        super(name);
        hasItems = false;
        acceptsPayload = true;
        update = true;
    }

    public class PayloadCargoLaunchPadBuild extends CargoLaunchPadBuild {
        public PayloadSeq stacks = new PayloadSeq();

        @Override
        protected float launchFillRatio() {
            return (float) stacks.total() / stackCapacity;
        }

        @Override
        public void updateTile() {
            if (!readyToLaunch() || stacks.total() < stackCapacity) return;

            for (Block block : content.blocks()) {
                int amount = stacks.get(block);
                if (amount > 0) SectorLogistics.handlePayloadExport(state.getSector(), block, amount);
            }
            for (UnitType unit : content.units()) {
                int amount = stacks.get(unit);
                if (amount > 0) SectorLogistics.handlePayloadExport(state.getSector(), unit, amount);
            }

            fireLaunchVisual();
            stacks.clear();
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload) {
            if (stacks.total() >= stackCapacity) return false;
            if (payload instanceof BuildPayload bp) {
                return bp.build.block != null;
            }
            return payload instanceof UnitPayload;
        }

        @Override
        public void handlePayload(Building source, Payload payload) {
            if (payload instanceof BuildPayload bp) {
                stacks.add(bp.build.block, 1);
            } else if (payload instanceof UnitPayload up) {
                stacks.add(up.unit.type, 1);
            }
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
