package net.minecraft.railsys.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;

/**
 * RailPath — an ordered traversal of RailPieces forming one continuous railway.
 * The external API is the physical global path distance s [m]; a caller never
 * needs to know which piece owns the sample.
 *
 * <h3>Boundary ownership (frozen Phase 0.6 contract)</h3>
 * An internal piece boundary at global distance {@code B} is owned by the
 * <b>earlier piece</b> (the sample is its exit sample at full local distance);
 * the final boundary (end of the whole path) is owned by the last piece.
 * Implemented as an upper-bound binary search plus an exact-boundary step-back
 * ({@code starts[idx] == s} → step back). No magic epsilon is used by the
 * resolver.
 *
 * <h3>Reverse traversal</h3>
 * {@link #reverse()} returns a new path with the same pieces in reverse order,
 * each with direction -1. The geometry is never rewritten; a REVERSE entry
 * maps global distance to local distance {@code globalEnd - g} and the travel
 * tangent / yaw / pitch are adjusted by {@link PathSample}.
 *
 * <h3>Error / clamp policy (mirrors Phase 1.1 geometry)</h3>
 * s &lt; 0 → clamp 0; s &gt; total → clamp total; NaN/Inf → IllegalStateException.
 * Empty paths, zero-length pieces and disconnected consecutive entries are
 * rejected at construction (silent gaps are forbidden).
 */
public final class RailPath {

	/** Frozen Phase 0.6 join tolerance (position). */
	public static final double POSITION_TOLERANCE_M = RailConnection.POSITION_TOLERANCE_M;
	/** Frozen Phase 0.6 heading continuity tolerance (degrees). */
	public static final double ANGLE_TOLERANCE_DEG = RailConnection.ANGLE_TOLERANCE_DEG;
	/** Documented exact boundary ownership rule. */
	public static final String BOUNDARY_OWNERSHIP_RULE = "earlier-piece-owns-internal-boundary";

	private final RailPathEntry[] entries;
	private final double[] starts; // starts[i] = cumulative distance before entry i, length n+1
	private final double totalLength;

	private RailPath(List<RailPiece> pieces, int[] dirs) {
		int n = pieces.size();
		if (n == 0) {
			throw new IllegalArgumentException("RailPath must contain at least one piece (empty path rejected)");
		}
		if (dirs.length != n) {
			throw new IllegalArgumentException("dirs length must equal piece count");
		}
		RailPathEntry[] es = new RailPathEntry[n];
		double[] st = new double[n + 1];
		st[0] = 0.0D;
		double acc = 0.0D;
		for (int i = 0; i < n; i++) {
			RailPiece p = pieces.get(i);
			if (p == null) {
				throw new IllegalArgumentException("RailPath contains a null piece at index " + i);
			}
			double len = p.lengthM();
			if (!RailMath.isFinite(len) || len < RailMath.EPS) {
				throw new IllegalArgumentException(
						"RailPath zero/non-finite length piece rejected, pieceId=" + p.pieceId());
			}
			int d = dirs[i] == RailPathEntry.REVERSE ? RailPathEntry.REVERSE : RailPathEntry.FORWARD;
			double next = acc + len;
			es[i] = new RailPathEntry(p, d, acc, next);
			acc = next;
			st[i + 1] = acc;
		}
		this.entries = es;
		this.starts = st;
		this.totalLength = acc;

		StringBuilder issues = new StringBuilder();
		for (int i = 0; i + 1 < n; i++) {
			RailValidationResult r = checkContinuity(es[i], es[i + 1]);
			if (!r.valid) {
				if (issues.length() > 0) {
					issues.append("; ");
				}
				issues.append(r.reason);
			}
		}
		if (issues.length() > 0) {
			throw new IllegalArgumentException("disconnected RailPath (silent gaps forbidden): " + issues);
		}
	}

	/** Path of all-forward pieces. */
	public static RailPath of(List<RailPiece> pieces) {
		int n = pieces.size();
		int[] dirs = new int[n];
		for (int i = 0; i < n; i++) {
			dirs[i] = RailPathEntry.FORWARD;
		}
		return new RailPath(pieces, dirs);
	}

	public static RailPath of(RailPiece... pieces) {
		List<RailPiece> list = new ArrayList<>();
		for (RailPiece p : pieces) {
			list.add(p);
		}
		return of(list);
	}

	public static Builder builder() {
		return new Builder();
	}

	public int entryCount() {
		return this.entries.length;
	}

	public RailPathEntry entry(int i) {
		return this.entries[i];
	}

	public List<RailPathEntry> entries() {
		return Collections.unmodifiableList(java.util.Arrays.asList(this.entries));
	}

	public double totalLength() {
		return this.totalLength;
	}

	/** Cumulative global distance before entry i (i in 0..entryCount). */
	public double cumulativeStart(int i) {
		return this.starts[i];
	}

	public double[] cumulativeStarts() {
		return this.starts.clone();
	}

	public RailPiece firstPiece() {
		return this.entries[0].piece();
	}

	public RailPiece lastPiece() {
		return this.entries[this.entries.length - 1].piece();
	}

	/**
	 * Resolve a global path distance to a {@link PathSample}. Out-of-range s is
	 * clamped to [0, totalLength]; non-finite s throws IllegalStateException.
	 */
	public PathSample resolve(double globalM) {
		if (!RailMath.isFinite(globalM)) {
			throw new IllegalStateException("Non-finite path distance=" + globalM);
		}
		double g = RailMath.clamp(globalM, 0.0D, this.totalLength);
		int idx = resolveIndex(g);
		RailPathEntry e = this.entries[idx];
		double local = e.localDistance(g);
		RailSample sample = e.piece().sampleByDistance(local);
		RailLocalFrame frame = e.piece().geometry().frameAt(local);
		return PathSample.create(g, idx, e, local, sample, frame);
	}

	/** Convenience alias of {@link #resolve(double)}. */
	public PathSample sampleByDistance(double globalM) {
		return resolve(globalM);
	}

	/**
	 * Reverse traversal path: same pieces in reverse order, each traversed with
	 * direction -1. Forward path total L and reverse path total L satisfy
	 * forward.resolve(s) and reverse.resolve(L - s) at the same world position
	 * with opposite travel tangents.
	 */
	public RailPath reverse() {
		int n = this.entries.length;
		List<RailPiece> pieces = new ArrayList<>(n);
		int[] dirs = new int[n];
		for (int i = 0; i < n; i++) {
			RailPathEntry e = this.entries[n - 1 - i];
			pieces.add(e.piece());
			dirs[i] = -e.direction();
		}
		return new RailPath(pieces, dirs);
	}

	/** Re-run the continuity checks; a constructed path is always valid by construction. */
	public RailValidationResult validate() {
		for (int i = 0; i + 1 < this.entries.length; i++) {
			RailValidationResult r = checkContinuity(this.entries[i], this.entries[i + 1]);
			if (!r.valid) {
				return r;
			}
		}
		return RailValidationResult.ok();
	}

	/**
	 * Upper-bound binary search over piece start offsets with the frozen
	 * earlier-piece boundary rule: find the smallest i with
	 * {@code starts[i+1] >= s}. At s exactly equal to an internal start this
	 * selects the earlier piece; the final boundary falls to the last piece.
	 */
	private int resolveIndex(double s) {
		int lo = 0;
		int hi = this.entries.length - 1;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (this.starts[mid + 1] >= s) {
				hi = mid;
			} else {
				lo = mid + 1;
			}
		}
		return lo;
	}

	private static RailValidationResult checkContinuity(RailPathEntry exit, RailPathEntry next) {
		RailEndpoint a = exit.exitEndpoint();
		RailEndpoint b = next.entryEndpoint();
		RailValidationResult v = RailConnection.validate(a, b);
		if (!v.valid) {
			return v;
		}
		double dot = a.tx() * b.tx() + a.ty() * b.ty() + a.tz() * b.tz();
		double magA = RailMath.hypot3(a.tx(), a.ty(), a.tz());
		double magB = RailMath.hypot3(b.tx(), b.ty(), b.tz());
		double cos = magA < RailMath.EPS || magB < RailMath.EPS ? 0.0D : dot / (magA * magB);
		cos = RailMath.clamp(cos, -1.0D, 1.0D);
		double ang = Math.toDegrees(Math.acos(cos));
		if (ang > ANGLE_TOLERANCE_DEG) {
			return RailValidationResult.invalid(
					"direction discontinuity " + a.pieceId() + " -> " + b.pieceId() + " angle=" + ang,
					v.positionErrorM, ang);
		}
		return RailValidationResult.ok();
	}

	@Override
	public String toString() {
		return "RailPath{entries=" + entries.length + " total=" + totalLength + "}";
	}

	public static final class Builder {

		private final List<RailPiece> pieces = new ArrayList<>();
		private final List<Integer> dirs = new ArrayList<>();

		public Builder append(RailPiece piece, int direction) {
			this.pieces.add(piece);
			this.dirs.add(direction == RailPathEntry.REVERSE ? RailPathEntry.REVERSE : RailPathEntry.FORWARD);
			return this;
		}

		public Builder forward(RailPiece piece) {
			return append(piece, RailPathEntry.FORWARD);
		}

		public Builder reverse(RailPiece piece) {
			return append(piece, RailPathEntry.REVERSE);
		}

		public RailPath build() {
			int n = this.pieces.size();
			int[] d = new int[n];
			for (int i = 0; i < n; i++) {
				d[i] = this.dirs.get(i);
			}
			return new RailPath(this.pieces, d);
		}
	}
}
