package bench.cubes;

public class TPolygon {
    public TObject owner;
    public int[] vertexList;
    public boolean isVisible;
    public boolean doubleSided = false;
    public int color_index;
    public int shade_index;
    public int color;
    public int max_distance;
    public int base_luminance = 8;
    private static TPoint p0;
    private static TPoint p1;
    private static TPoint p2;
    private static TPoint p3;
    private static TPoint u;
    private static TPoint c;
    private static TPoint v;
    private static TPoint n;
    private static TPoint l;

    static {
        p0 = new TPoint();
        p1 = new TPoint();
        p2 = new TPoint();
        p3 = new TPoint();
        u = new TPoint();
        c = new TPoint();
        v = new TPoint();
        n = new TPoint();
        l = new TPoint();
    }

    public TPolygon(TObject owner) {
        this.owner = owner;
    }

    public void shadeAndCull(TPoint light, TPoint viewpoint) {
        int light_length, normal_length, view_length, dot, alpha;

        // convert to world coordinates
        p0.x = owner.vertexList[vertexList[0]].x + owner.location.x;
        p0.y = owner.vertexList[vertexList[0]].y + owner.location.y;
        p0.z = owner.vertexList[vertexList[0]].z + owner.location.z;

        // convert to world coordinates
        p1.x = owner.vertexList[vertexList[1]].x + owner.location.x;
        p1.y = owner.vertexList[vertexList[1]].y + owner.location.y;
        p1.z = owner.vertexList[vertexList[1]].z + owner.location.z;

        // convert to world coordinates
        p2.x = owner.vertexList[vertexList[2]].x + owner.location.x;
        p2.y = owner.vertexList[vertexList[2]].y + owner.location.y;
        p2.z = owner.vertexList[vertexList[2]].z + owner.location.z;

        // u = vector from p0 to p1
        u.x = p1.x - p0.x;
        u.y = p1.y - p0.y;
        u.z = p1.z - p0.z;

        // v = vector from p0 to p2
        v.x = p2.x - p0.x;
        v.y = p2.y - p0.y;
        v.z = p2.z - p0.z;

        // l = vector from p0 to light
        l.x = light.x - p0.x;
        l.y = light.y - p0.y;
        l.z = light.z - p0.z;

        // c = vector from p0 to light
        c.x = viewpoint.x - p0.x;
        c.y = viewpoint.y - p0.y;
        c.z = viewpoint.z - p0.z;

        // n = u x v (cross product)
        n.x = -(u.y * v.z - u.z * v.y);
        n.y = u.x * v.z - u.z * v.x;
        n.z = -(u.x * v.y - u.y * v.x);

        light_length = l.distance(TPoint.ORIGIN);
        view_length = c.distance(TPoint.ORIGIN);
        normal_length = n.distance(TPoint.ORIGIN);

        // dot = n . l / |n| * |l| (normalized dot product)
        dot = (n.x * l.x + n.y * l.y + n.z * l.z) * 28 /
            (light_length == 0 || normal_length == 0 ? 1 : (light_length * normal_length));

        shade_index = base_luminance;
        if (dot > 0) shade_index += dot;
        if (shade_index > (ColorTable.MAX_SHADES - 1)) {
            shade_index = (ColorTable.MAX_SHADES - 1);
        }

        color = ColorTable.color[color_index][shade_index];

        // alpha = n . c / |n| * |c| (normalized dot product)
        alpha = (n.x * c.x + n.y * c.y + n.z * c.z) * 50 /
            (view_length == 0 || normal_length == 0
                ? 1
                : (view_length * normal_length));

        isVisible = doubleSided || alpha >= 0;
    }

    public void calcMaximumDistance(TPoint other) {
        int distance;
        int delta_x, delta_y, delta_z;

        max_distance = Integer.MIN_VALUE;
        for (int i=0; i<vertexList.length; i++) {
            p3 = owner.vertexList[vertexList[i]];
            p0.x = p3.x + owner.location.x - other.x;
            p0.y = p3.y + owner.location.y - other.y;
            p0.z = p3.z + owner.location.z - other.z;
            distance = TPoint.ORIGIN.distance(p0);

            if (distance > max_distance) max_distance = distance;
        }
    }
}
