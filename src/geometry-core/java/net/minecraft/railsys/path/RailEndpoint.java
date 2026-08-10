package net.minecraft.railsys.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;

/**
 * RailEndpoint — one end of a RailPiece (START at geometry s=0, END at geometry
 * s=length). Position, native tangent and heading are DERIVED from the owning
 * geometry at construction; there is no independent coordinate state that could
 * drift out of sync with the geometry (single source of truth).
 *
 * <p>Travel direction convention: the {@code native} tangent / yaw / pitch
 * always point along the geometry's natural start→end direction. A piece
 * traversed in reverse (path direction -1) negates the tangent for travel use
 * (see {@link PathSample} / {@link RailPathEntry}).
 */
public final class RailEndpoint {

	public enum Side {
		START, END
	}

	private final RailPiece piece;
	private final Side side;
	private final double x, y, z;
	private final double tx, ty, tz;
	private final double yawDeg;
	private final double pitchDeg;
	private final List<RailConnection> connections = new ArrayList<>();

	RailEndpoint(RailPiece piece, Side side) {
		this.piece = piece;
		this.side = side;
		RailSample s = side == Side.START
				? piece.geometry().sampleByDistance(0.0D)
				: piece.geometry().sampleByDistance(piece.lengthM());
		this.x = s.x;
		this.y = s.y;
		this.z = s.z;
		this.tx = s.tx;
		this.ty = s.ty;
		this.tz = s.tz;
		this.yawDeg = s.yawDeg;
		this.pitchDeg = s.pitchDeg;
	}

	public RailPiece piece() {
		return this.piece;
	}

	public Side side() {
		return this.side;
	}

	public int pieceId() {
		return this.piece.pieceId();
	}

	/** Stable deterministic identity: (pieceId &lt;&lt; 1) | side. Unique per network. */
	public long id() {
		return ((long) this.piece.pieceId() << 1) | (this.side == Side.END ? 1L : 0L);
	}

	public double x() {
		return this.x;
	}

	public double y() {
		return this.y;
	}

	public double z() {
		return this.z;
	}

	public double tx() {
		return this.tx;
	}

	public double ty() {
		return this.ty;
	}

	public double tz() {
		return this.tz;
	}

	public double yawDeg() {
		return this.yawDeg;
	}

	public double pitchDeg() {
		return this.pitchDeg;
	}

	/** Unmodifiable view of the connections attached to this endpoint. */
	public List<RailConnection> connections() {
		return Collections.unmodifiableList(this.connections);
	}

	void addConnection(RailConnection c) {
		if (!this.connections.contains(c)) {
			this.connections.add(c);
		}
	}

	void removeConnection(RailConnection c) {
		this.connections.remove(c);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RailEndpoint)) {
			return false;
		}
		RailEndpoint e = (RailEndpoint) o;
		return this.piece.pieceId() == e.piece.pieceId() && this.side == e.side;
	}

	@Override
	public int hashCode() {
		return (this.piece.pieceId() * 31) ^ this.side.hashCode();
	}

	@Override
	public String toString() {
		return "RailEndpoint{pieceId=" + piece.pieceId() + " side=" + side
				+ " pos=(" + x + "," + y + "," + z + ")}";
	}
}
