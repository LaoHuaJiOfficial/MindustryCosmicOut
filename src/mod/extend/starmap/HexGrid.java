package mod.extend.starmap;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.graphics.Pal;

public class HexGrid {
    public float size = Scl.scl(64f);
    public float pad = 2f;
    public float lineWidth = Scl.scl(3f);
    public float rotation = 0f;
    public Color centerColor = Pal.accent;
    public Color gridColor = Pal.gray;

    public float axis0WorldStep(){
        return size * 1.5f;
    }

    public float axis120WorldStep(){
        return size * Mathf.sqrt3;
    }

    public HexCoord worldToHex(float gx, float gy){
        float a = gx / axis0WorldStep();
        float c = gy / axis120WorldStep() - a * 0.5f;
        float b = -a - c;
        return HexCoord.roundCube(a, b, c);
    }

    public void hexToWorld(HexCoord hex, float offsetX, float offsetY, Vec2 out){
        out.set(
            axis0WorldStep() * hex.a + offsetX,
            axis120WorldStep() * (hex.c + hex.a * 0.5f) + offsetY
        );
    }

    public HexCoord localToHex(float localX, float localY, float panX, float panY, float viewWidth, float viewHeight){
        float offsetX = panX + viewWidth / 2f, offsetY = panY + viewHeight / 2f;
        return worldToHex(localX - offsetX, localY - offsetY);
    }

    public HexRange getVisibleRange(float viewWidth, float viewHeight, float scaleX, float scaleY, float offsetX, float offsetY){
        float cx = viewWidth / 2f, cy = viewHeight / 2f;
        if(scaleX <= 0f) scaleX = 1f;
        if(scaleY <= 0f) scaleY = 1f;

        float margin = size * 2f;
        float minGx = Float.POSITIVE_INFINITY, maxGx = Float.NEGATIVE_INFINITY;
        float minGy = Float.POSITIVE_INFINITY, maxGy = Float.NEGATIVE_INFINITY;

        float[][] corners = {{0f, 0f}, {viewWidth, 0f}, {viewWidth, viewHeight}, {0f, viewHeight}};
        for(float[] corner : corners){
            float lx = cx + (corner[0] - cx) / scaleX;
            float ly = cy + (corner[1] - cy) / scaleY;
            float gx = lx - offsetX;
            float gy = ly - offsetY;
            minGx = Math.min(minGx, gx);
            maxGx = Math.max(maxGx, gx);
            minGy = Math.min(minGy, gy);
            maxGy = Math.max(maxGy, gy);
        }

        minGx -= margin;
        maxGx += margin;
        minGy -= margin;
        maxGy += margin;

        HexCoord[] samples = {
            worldToHex(minGx, minGy),
            worldToHex(maxGx, minGy),
            worldToHex(minGx, maxGy),
            worldToHex(maxGx, maxGy),
            worldToHex(minGx, (minGy + maxGy) / 2f),
            worldToHex(maxGx, (minGy + maxGy) / 2f),
            worldToHex((minGx + maxGx) / 2f, minGy),
            worldToHex((minGx + maxGx) / 2f, maxGy)
        };

        int aMin = Integer.MAX_VALUE, aMax = Integer.MIN_VALUE;
        int cMin = Integer.MAX_VALUE, cMax = Integer.MIN_VALUE;
        for(HexCoord hex : samples){
            aMin = Math.min(aMin, hex.a);
            aMax = Math.max(aMax, hex.a);
            cMin = Math.min(cMin, hex.c);
            cMax = Math.max(cMax, hex.c);
        }

        int hexMargin = Math.max(1, Math.round(pad));
        return new HexRange(aMin - hexMargin, aMax + hexMargin, cMin - hexMargin, cMax + hexMargin);
    }

    public void draw(float viewWidth, float viewHeight, float scaleX, float scaleY, float offsetX, float offsetY, @Nullable HexCoord hoverHex, float alpha){
        HexRange range = getVisibleRange(viewWidth, viewHeight, scaleX, scaleY, offsetX, offsetY);

        Draw.sort(true);
        Draw.z(0f);

        for(int c = range.cMin; c <= range.cMax; c++){
            for(int a = range.aMin; a <= range.aMax; a++){
                int b = -a - c;
                if(hoverHex != null && hoverHex.a == a && hoverHex.b == b && hoverHex.c == c) continue;

                hexToWorld(new HexCoord(a, b, c), offsetX, offsetY, Tmp.v1);

                Lines.stroke(lineWidth, gridColor);
                Draw.alpha(alpha * 0.15f);
                Lines.poly(Tmp.v1.x, Tmp.v1.y, 6, size, rotation);
            }
        }

        if(hoverHex != null){
            hexToWorld(hoverHex, offsetX, offsetY, Tmp.v1);
            Draw.color(centerColor, alpha * 0.3f);
            Fill.poly(Tmp.v1.x, Tmp.v1.y, 6, size, rotation);
            Lines.stroke(lineWidth * 2f, centerColor);
            Draw.alpha(alpha * 0.95f);
            Lines.poly(Tmp.v1.x, Tmp.v1.y, 6, size, rotation);
        }

        Draw.sort(false);
        Draw.reset();
    }
}
