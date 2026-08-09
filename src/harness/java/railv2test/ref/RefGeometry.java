package railv2test.ref;

/**
 * Reference rail-geometry contract (test oracle).
 * Independent, high-precision implementation; NOT the production algorithm.
 * Any future production RailGeometry must be validated against this.
 */
public interface RefGeometry {
    double lengthM();

    RefSample sampleByDistance(double distanceM);

    int pieceId();
}
