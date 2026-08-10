package net.minecraft.railsys.path;

import net.minecraft.railsys.geometry.RailGeometry;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;

/**
 * RailPiece — a single rail geometry as used on a RailPath / in a RailNetwork.
 *
 * <p>Identity: stable int piece id = geometry.pieceId() (no duplicated id that
 * could drift). The geometry remains the source of truth for length, sampling
 * and endpoint pose; the piece adds endpoint / connection bookkeeping and an
 * optional metadata hook. Zero / non-finite length pieces are rejected at
 * construction (mirrors Phase 0.6 invalid-state policy).
 */
public final class RailPiece {

	private final RailGeometry geometry;
	private final RailEndpoint start;
	private final RailEndpoint end;
	private final double length;
	private Object metadata;

	public RailPiece(RailGeometry geometry) {
		if (geometry == null) {
			throw new IllegalArgumentException("RailPiece geometry must not be null");
		}
		this.geometry = geometry;
		this.length = geometry.lengthM();
		if (!RailMath.isFinite(this.length) || this.length < RailMath.EPS) {
			throw new IllegalArgumentException(
					"RailPiece zero/non-finite length for pieceId=" + geometry.pieceId());
		}
		this.start = new RailEndpoint(this, RailEndpoint.Side.START);
		this.end = new RailEndpoint(this, RailEndpoint.Side.END);
	}

	public int pieceId() {
		return this.geometry.pieceId();
	}

	public RailGeometry geometry() {
		return this.geometry;
	}

	public double lengthM() {
		return this.length;
	}

	public RailEndpoint start() {
		return this.start;
	}

	public RailEndpoint end() {
		return this.end;
	}

	public Object metadata() {
		return this.metadata;
	}

	public void setMetadata(Object metadata) {
		this.metadata = metadata;
	}

	/** Sample at local distance (delegates to geometry; clamp + NaN policy preserved). */
	public RailSample sampleByDistance(double localM) {
		return this.geometry.sampleByDistance(localM);
	}

	public RailValidationResult validate() {
		if (this.geometry == null) {
			return RailValidationResult.invalid("missing geometry");
		}
		if (!RailMath.isFinite(this.length) || this.length < RailMath.EPS) {
			return RailValidationResult.invalid("zero/non-finite length");
		}
		return RailValidationResult.ok();
	}

	@Override
	public String toString() {
		return "RailPiece{pieceId=" + pieceId() + " length=" + length + "}";
	}
}
