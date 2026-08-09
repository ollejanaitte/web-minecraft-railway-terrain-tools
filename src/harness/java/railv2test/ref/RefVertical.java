package railv2test.ref;

/**
 * Reference vertical curve: horizontal straight with a cubic Bezier in Y/Z
 * (vertical profile) so pitch transitions smoothly from pitch0 to pitch1.
 * X is linear; Z linear; Y from a cubic Bezier on (yStart, yStart+dy0,
 * yEnd-dy1, yEnd) where dy0/dy1 are endpoint slope handles.
 */
public final class RefVertical implements RefGeometry {

    private final RefVec3 start;
    private final RefVec3 end;
    private final double dy0;
    private final double dy1;
    private final int pieceId;
    private final double length;

    public RefVertical(RefVec3 start, RefVec3 end, double dy0, double dy1, int pieceId) {
        this.start = start;
        this.end = end;
        this.dy0 = dy0;
        this.dy1 = dy1;
        this.pieceId = pieceId;
        this.length = start.distanceTo(end);
    }

    private double yAt(double t) {
        double u = clamp01(t);
        double uu = 1.0 - u;
        double w0 = uu * uu * uu;
        double w1 = 3.0 * uu * uu * u;
        double w2 = 3.0 * uu * u * u;
        double w3 = u * u * u;
        double y0 = start.y;
        double y1 = start.y + dy0;
        double y2 = end.y - dy1;
        double y3 = end.y;
        return w0 * y0 + w1 * y1 + w2 * y2 + w3 * y3;
    }

    private double dyDt(double t) {
        double u = clamp01(t);
        double y0 = start.y;
        double y1 = start.y + dy0;
        double y2 = end.y - dy1;
        double y3 = end.y;
        return 3.0 * (1 - u) * (1 - u) * (y1 - y0)
                + 6.0 * (1 - u) * u * (y2 - y1)
                + 3.0 * u * u * (y3 - y2);
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
        double z = start.z + (end.z - start.z) * t;
        double y = yAt(t);
        double yaw = RefSample.wrapYaw(Math.toDegrees(Math.atan2(end.x - start.x, end.z - start.z)));
        double horiz = Math.hypot(end.x - start.x, end.z - start.z);
        double horizStep = horiz * (1.0 / length);
        double pitch = Math.toDegrees(Math.atan2(dyDt(t) / length, horizStep));
        return new RefSample(distanceM, x, y, z, yaw, pitch, 0.0, pieceId);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
