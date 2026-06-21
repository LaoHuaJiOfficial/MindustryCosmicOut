package mod.extend.starmap;

import arc.math.Mathf;
import arc.struct.Seq;

public class HexCoord {
    public int a = 0, b = 0, c = 0;

    static final int[][] neighbors = {
            {1, -1, 0}, {1, 0, -1}, {0, 1, -1},
            {-1, 1, 0}, {-1, 0, 1}, {0, -1, 1}
    };

    public HexCoord(){}

    public HexCoord(int a, int b, int c){
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static HexCoord from0And120(int axis0, int axis120){
        return new HexCoord(axis0, -axis0 - axis120, axis120);
    }

    public static String key(HexCoord hex){
        return hex.a + "," + hex.b + "," + hex.c;
    }

    public static String key(int a, int b, int c){
        return a + "," + b + "," + c;
    }

    public static int distance(int a, int b, int c){
        return (Math.abs(a) + Math.abs(b) + Math.abs(c)) / 2;
    }

    public static HexCoord roundCube(float a, float b, float c){
        int ai = Math.round(a);
        int bi = Math.round(b);
        int ci = Math.round(c);

        float aDiff = Math.abs(ai - a);
        float bDiff = Math.abs(bi - b);
        float cDiff = Math.abs(ci - c);

        if(aDiff > bDiff && aDiff > cDiff){
            ai = -bi - ci;
        }else if(bDiff > cDiff){
            bi = -ai - ci;
        }else{
            ci = -ai - bi;
        }

        return new HexCoord(ai, bi, ci);
    }

    public boolean valid(){
        return a + b + c == 0;
    }

    public boolean inGrid(int radius){
        return distance(a, b, c) <= radius;
    }

    public void set(HexCoord other){
        a = other.a;
        b = other.b;
        c = other.c;
    }

    public static Seq<HexCoord> line(HexCoord from, HexCoord to){
        int steps = distance(from.a, from.b, from.c, to.a, to.b, to.c);
        Seq<HexCoord> result = new Seq<>(steps + 1);

        for(int i = 0; i <= steps; i++){
            float t = steps == 0 ? 0f : i / (float)steps;
            result.add(roundCube(
                Mathf.lerp(from.a, to.a, t),
                Mathf.lerp(from.b, to.b, t),
                Mathf.lerp(from.c, to.c, t)
            ));
        }

        return result;
    }

    public static Seq<HexCoord> path(HexCoord from, HexCoord to){
        if(from.equals(to)) return Seq.with(new HexCoord(from.a, from.b, from.c));

        Seq<HexCoord> result = new Seq<>();
        HexCoord cur = new HexCoord(from.a, from.b, from.c);
        result.add(new HexCoord(cur.a, cur.b, cur.c));

        int maxSteps = distance(from.a, from.b, from.c, to.a, to.b, to.c) + 2;
        for(int step = 0; step < maxSteps && !cur.equals(to); step++){
            int bestDist = distance(cur.a, cur.b, cur.c, to.a, to.b, to.c);
            int nextA = cur.a, nextB = cur.b, nextC = cur.c;

            for(int[] dir : neighbors){
                int na = cur.a + dir[0];
                int nb = cur.b + dir[1];
                int nc = cur.c + dir[2];
                int dist = distance(na, nb, nc, to.a, to.b, to.c);
                if(dist < bestDist){
                    bestDist = dist;
                    nextA = na;
                    nextB = nb;
                    nextC = nc;
                }
            }

            if(nextA == cur.a && nextB == cur.b && nextC == cur.c) break;
            cur.a = nextA;
            cur.b = nextB;
            cur.c = nextC;
            result.add(new HexCoord(cur.a, cur.b, cur.c));
        }

        return result;
    }

    public static int distance(int a1, int b1, int c1, int a2, int b2, int c2){
        return (Math.abs(a1 - a2) + Math.abs(b1 - b2) + Math.abs(c1 - c2)) / 2;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof HexCoord other)) return false;
        return a == other.a && b == other.b && c == other.c;
    }

    @Override
    public int hashCode(){
        return a * 73856093 ^ b * 19349663 ^ c * 83492791;
    }
}
