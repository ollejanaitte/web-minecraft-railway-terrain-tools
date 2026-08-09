package railv2test.ref;

/** Minimal immutable 3D vector (test reference only; no game dependency). */
public final class RefVec3 {
    public final double x;
    public final double y;
    public final double z;

    public RefVec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distanceTo(RefVec3 o) {
        double dx = x - o.x;
        double dy = y - o.y;
        double dz = z - o.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
