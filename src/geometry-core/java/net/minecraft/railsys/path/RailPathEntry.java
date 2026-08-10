package net.minecraft.railsys.path;

/**
 * RailPathEntry — one traversal of one RailPiece inside a RailPath.
 *
 * <p>A piece may appear more than once (even adjacent) with different
 * directions; the geometry itself is never rewritten. For a FORWARD entry the
 * piece is traversed start→end and global distance maps to local distance
 * {@code g - globalStart}. For a REVERSE entry the piece is traversed end→start
 * and local distance maps to {@code globalEnd - g}; the travel tangent is the
 * negated native tangent (see {@link PathSample}).
 */
public final class RailPathEntry {

	public static final int FORWARD = 1;
	public static final int REVERSE = -1;

	private final RailPiece piece;
	private final int direction;
	private final double globalStart;
	private final double globalEnd;

	public RailPathEntry(RailPiece piece, int direction, double globalStart, double globalEnd) {
		this.piece = piece;
		this.direction = direction == REVERSE ? REVERSE : FORWARD;
		this.globalStart = globalStart;
		this.globalEnd = globalEnd;
	}

	public RailPiece piece() {
		return this.piece;
	}

	public int pieceId() {
		return this.piece.pieceId();
	}

	/** +1 start→end, -1 end→start. */
	public int direction() {
		return this.direction;
	}

	/** Global path distance at the entry start. */
	public double globalStart() {
		return this.globalStart;
	}

	/** Global path distance at the entry end. */
	public double globalEnd() {
		return this.globalEnd;
	}

	/** Traversed length in metres (== piece length). */
	public double length() {
		return this.globalEnd - this.globalStart;
	}

	/** The endpoint at which travel on this entry begins. */
	public RailEndpoint entryEndpoint() {
		return this.direction == FORWARD ? this.piece.start() : this.piece.end();
	}

	/** The endpoint at which travel on this entry ends. */
	public RailEndpoint exitEndpoint() {
		return this.direction == FORWARD ? this.piece.end() : this.piece.start();
	}

	/** Local distance in metres for a global distance inside this entry. */
	public double localDistance(double globalM) {
		return this.direction == FORWARD ? (globalM - this.globalStart) : (this.globalEnd - globalM);
	}

	@Override
	public String toString() {
		return "RailPathEntry{pieceId=" + piece.pieceId() + " dir=" + direction
				+ " global=[" + globalStart + "," + globalEnd + "]}";
	}
}
