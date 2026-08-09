package railv2test.ref;

/**
 * Reference straight rail piece. Optionally constant gradient (pitch).
 * start (s) to end (e), each with absolute world coords.
 * Yaw from the horizontal projection; pitch from gradient.
 */
public final class RefStraight implements RefGeometry {

    private final RefVec3 start;
    private final RefVec3 end;
    private final double length;
    private final int pieceId;

    public RefStraight(RefVec3 start, RefVec3 end, int pieceId) {
        this.start = start;
        this.end = end;
        this.length = start.distanceTo(end);
        this.pieceId = pieceId;
    }

    @Override
    public double lengthM() {
        return length;
    }

    @Override
    public int pieceId() {
        return pieceId;
    }

    @Override
    public RefSample sampleByDistance(double distanceM) {
        double t = length <= 0.0 ? 0.0 : clamp01(distanceM / length);
        double x = start.x + (end.x - start.x) * t;
        double y = start.y + (end.y - start.y) * t;
        double z = start.z + (end.z - start.z) * t;
        double yaw = RefSample.wrapYaw(Math.toDegrees(Math.atan2(end.x - start.x, end.z - start.z)));
        double horiz = Math.hypot(end.x - start.x, end.z - start.z);
        double pitch = Math.toDegrees(Math.atan2(end.y - start.y, horiz));
        return new RefSample(distanceM, x, y, z, yaw, pitch, 0.0, pieceId);
    }

    static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
