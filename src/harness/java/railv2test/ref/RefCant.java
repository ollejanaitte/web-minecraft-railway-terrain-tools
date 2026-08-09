package railv2test.ref;

/**
 * Wraps a base geometry and adds a cant (roll) profile: roll is 0 at the
 * ends and ramps to {@code maxRollDeg} at the middle (simple cosine taper).
 * Produces the roll continuity used by the cant fixtures.
 */
public final class RefCant implements RefGeometry {

    private final RefGeometry base;
    private final double maxRollDeg;

    public RefCant(RefGeometry base, double maxRollDeg) {
        this.base = base;
        this.maxRollDeg = maxRollDeg;
    }

    @Override
    public double lengthM() {
        return base.lengthM();
    }

    @Override
    public int pieceId() {
        return base.pieceId();
    }

    @Override
    public RefSample sampleByDistance(double distanceM) {
        RefSample s = base.sampleByDistance(distanceM);
        double l = lengthM();
        double t = l <= 0.0 ? 0.0 : RefStraight.clamp01(distanceM / l);
        double roll = maxRollDeg * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * t));
        return new RefSample(s.distanceM, s.x, s.y, s.z, s.yawDeg, s.pitchDeg, roll, s.pieceId);
    }
}
