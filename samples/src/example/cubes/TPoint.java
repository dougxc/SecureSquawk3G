package example.cubes;

public class TPoint {
    public static final TPoint ORIGIN = new TPoint();
    public int x;
    public int y;
    public int z;

    public TPoint() {
    }

    public TPoint(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

    public final int distance(TPoint p) {
        return IntegerMath.sqrt(
            (p.x - x) * (p.x - x)
            + (p.y - y) * (p.y - y)
            + (p.z - z) * (p.z - z)
        );
    }

    public void rotate(int pitch, int yaw, int roll) {
        int tmp;

        tmp = IntegerMath.multiCos(x , roll) - IntegerMath.multiSin(y , roll);
        y = IntegerMath.multiSin(x , roll) + IntegerMath.multiCos(y , roll);
        x = tmp;

        tmp = IntegerMath.multiSin(z , yaw) + IntegerMath.multiCos(x , yaw);
        z = IntegerMath.multiCos(z , yaw) - IntegerMath.multiSin(x , yaw);
        x = tmp;

        tmp = IntegerMath.multiCos(y , pitch) - IntegerMath.multiSin(z , pitch);
        z = IntegerMath.multiSin(y , pitch) + IntegerMath.multiCos(z , pitch);
        y = tmp;
    }

    public final void assignFrom(TPoint p) {
        x = p.x;
        y = p.y;
        z = p.z;
    }

    public final void invert() {
        x = -x;
        y = -y;
        z = -z;
    }

    public final void translate(TPoint p) {
        x += p.x;
        y += p.y;
        z += p.z;
    }

    public final void scaleUp(int factor) {
        x *= factor;
        y *= factor;
        z *= factor;
    }

    public final void scaleDown(int factor) {
        x /= factor;
        y /= factor;
        z /= factor;
    }

    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
