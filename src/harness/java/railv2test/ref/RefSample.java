package railv2test.ref;

/**
 * A rail sample: world position + orientation at a distance along a piece.
 * Matches the Phase -1 RailSample contract (doc/architecture/RAIL_GEOMETRY_DESIGN.md).
 */
public final class RefSample {
    public final double distanceM;
    public final double x;
    public final double y;
    public final double z;
    /** degrees, Minecraft convention: 0 = +Z, clockwise positive. */
    public final double yawDeg;
    /** degrees, +up positive. */
    public final double pitchDeg;
    /** degrees, positive = right-side down (cant/roll). */
    public final double rollDeg;
    /** piece id (from the owning fixture/geometry). */
    public final int pieceId;

    public RefSample(double distanceM, double x, double y, double z,
                     double yawDeg, double pitchDeg, double rollDeg, int pieceId) {
        this.distanceM = distanceM;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yawDeg = yawDeg;
        this.pitchDeg = pitchDeg;
        this.rollDeg = rollDeg;
        this.pieceId = pieceId;
    }

    public static double wrapYaw(double deg) {
        while (deg > 180.0) {
            deg -= 360.0;
        }
        while (deg <= -180.0) {
            deg += 360.0;
        }
        return deg;
    }
}
