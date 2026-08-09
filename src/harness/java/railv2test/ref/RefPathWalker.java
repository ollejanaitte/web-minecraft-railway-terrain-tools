package railv2test.ref;

import java.util.ArrayList;
import java.util.List;

/**
 * TEST-ONLY ORACLE for the Phase 2 distance-based formation model
 * (doc/architecture/TRAIN_FORMATION_BOGIE_DESIGN.md, ADR-003).
 *
 * A path is an ordered list of entries, each a piece traversed in a
 * direction with a local metre range. The leader advances a GLOBAL path
 * distance; a follower is placed by walking BACKWARD by carSpacing.
 *
 * This is NOT the production solver; it is the reference the production
 * solver in Phase 2 must reproduce.
 */
public final class RefPathWalker {

    public static final class Entry {
        public final RefGeometry piece;
        public final int dir;        // +1 = start->end, -1 = end->start
        public final double startM;  // global distance at entry start
        public final double endM;    // global distance at entry end

        public Entry(RefGeometry piece, int dir, double startM, double endM) {
            this.piece = piece;
            this.dir = dir;
            this.startM = startM;
            this.endM = endM;
        }

        public double length() {
            return endM - startM;
        }
    }

    private final List<Entry> entries;

    public RefPathWalker() {
        this.entries = new ArrayList<>();
    }

    public void append(RefGeometry piece, int dir) {
        double startM = entries.isEmpty() ? 0.0 : entries.get(entries.size() - 1).endM;
        entries.add(new Entry(piece, dir, startM, startM + piece.lengthM()));
    }

    public List<Entry> entries() {
        return entries;
    }

    public double totalLength() {
        return entries.isEmpty() ? 0.0 : entries.get(entries.size() - 1).endM;
    }

    /**
     * Resolve a global distance to a local sample.
     * Clamps into the path range.
     */
    public RefSample resolve(double globalM) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("empty path");
        }
        double g = globalM < 0.0 ? 0.0 : (globalM > totalLength() ? totalLength() : globalM);
        for (Entry e : entries) {
            if (g <= e.endM || e == entries.get(entries.size() - 1)) {
                // local distance from the piece START, honoring traversal dir
                double local = e.dir == 1 ? (g - e.startM) : (e.endM - g);
                return e.piece.sampleByDistance(local);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /**
     * Place car k behind the leader (k=0 => leader).
     * targetDistance = leaderDistance - k * carSpacing, resolved by walking
     * backward across entries WITHOUT modulo wrap.
     */
    public RefSample placeFollower(double leaderDistanceM, int k, double carSpacingM) {
        double target = leaderDistanceM - (double) k * carSpacingM;
        return resolve(target);
    }
}
