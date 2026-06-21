package mod.extend.type.spaceship;

import arc.math.geom.Rect;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockGroup;
import mod.content.ModStats;
import mod.extend.type.SpaceshipStats;

import static mindustry.Vars.indexer;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class Shipyard extends Block {
    public int yardWidth = 12, yardHeight = 7;

    public Shipyard(String name) {
        super(name);

        sync = true;
        solid = true;
        update = true;
        rotate = true;
        rotateDraw = false;
        commandable = true;
        quickRotate = false;

        group = BlockGroup.units;
        flags = EnumSet.of(BlockFlag.unitAssembler);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(ModStats.yardSize, "@x@", yardWidth, yardHeight);
    }

    public Rect getRect(Rect rect, float x, float y, int rotation){
        Tmp.v1.trns(rotation * 90, ((yardWidth + size) / 2f) * tilesize).add(x, y);

        if (rotation % 2 == 0) {
            rect.setCentered(Tmp.v1.x, Tmp.v1.y, yardWidth * tilesize, yardHeight * tilesize);
        } else {
            rect.setCentered(Tmp.v1.x, Tmp.v1.y, yardHeight * tilesize, yardWidth * tilesize);
        }

        return rect;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        x *= tilesize;
        y *= tilesize;
        x += (int) offset;
        y += (int) offset;

        Rect rect = getRect(Tmp.r1, x, y, rotation);

        Drawf.dashRect(valid ? Pal.accent : Pal.remove, rect);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        Rect rect = getRect(Tmp.r1, tile.worldx() + offset, tile.worldy() + offset, rotation).grow(0.1f);
        return
                !indexer.getFlagged(team, BlockFlag.unitAssembler).contains(b -> b != tile.build && b.block instanceof Shipyard shipyard && shipyard.getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect)) &&
                        !team.data().getBuildings(ConstructBlock.get(size)).contains(b -> b != tile.build && ((ConstructBlock.ConstructBuild)b).current instanceof Shipyard shipyard && shipyard.getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect));
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class ShipyardBuild extends Building {

        public Rect yardRect(){
            return getRect(Tmp.r1, x, y, rotation);
        }

        public Seq<Building> buildingsInYard(){
            Seq<Building> out = new Seq<>();
            Rect rect = yardRect();
            int tx0 = (int)((rect.x - tilesize / 2f) / tilesize);
            int ty0 = (int)((rect.y - tilesize / 2f) / tilesize);
            int tx1 = (int)((rect.x + rect.width + tilesize / 2f) / tilesize);
            int ty1 = (int)((rect.y + rect.height + tilesize / 2f) / tilesize);

            for(int tx = tx0; tx <= tx1; tx++){
                for(int ty = ty0; ty <= ty1; ty++){
                    Tile tile = world.tile(tx, ty);
                    if(tile == null || tile.build == null || tile.build.team != team) continue;
                    if(!out.contains(tile.build)){
                        out.add(tile.build);
                    }
                }
            }
            return out;
        }

        public boolean contains(Building build){
            return yardRect().contains(build.x, build.y);
        }

        public SpaceshipStats scanStats(){
            return SpaceshipStats.fromYard(buildingsInYard());
        }

        @Override
        public void draw() {
            super.draw();

            Rect rect = getRect(Tmp.r1, x, y, rotation);

            Drawf.dashRect(Pal.accent, rect);
        }
    }
}
