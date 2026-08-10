package net.minecraft.railsys.path;

import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;

/**
 * PathSample — the result of resolving a global path distance.
 *
 * <p>Carries the resolved piece entry (piece id + index), the local distance
 * within that piece, the traversal direction, the NATIVE geometry sample and
 * local frame, plus travel-adjusted heading/tangent.
 *
 * <p>Travel conventions:
 * <ul>
 *   <li>FORWARD (+1): travel heading == native yaw/pitch; travel tangent == native tangent.</li>
 *   <li>REVERSE (-1): travel heading = yaw+180 (wrapped), pitch negated;
 *       travel tangent = negated native tangent.</li>
 * </ul>
 * The {@code sample} / {@code frame} always reflect the NATIVE geometry so that
 * boundary continuity across pieces can be compared consistently; the
 * travel-adjusted values are provided for pose/vehicle use.
 */
public final class PathSample {

	public final double globalDistanceM;
	public final int entryIndex;
	public final int pieceId;
	public final RailPiece piece;
	public final double localDistanceM;
	public final int travelDirection;
	/** Native geometry sample at local distance. */
	public final RailSample sample;
	/** Travel-adjusted tangent (native * travelDirection). */
	public final double travelTx;
	public final double travelTy;
	public final double travelTz;
	/** Travel-adjusted yaw (native + 180 when reversed, wrapped). */
	public final double travelYawDeg;
	/** Travel-adjusted pitch (negated when reversed). */
	public final double travelPitchDeg;
	/** Native geometry local frame (for boundary continuity checks). */
	public final RailLocalFrame frame;

	private PathSample(double globalDistanceM, int entryIndex, RailPathEntry entry, double localDistanceM,
			RailSample sample, RailLocalFrame frame) {
		this.globalDistanceM = globalDistanceM;
		this.entryIndex = entryIndex;
		this.pieceId = entry.pieceId();
		this.piece = entry.piece();
		this.localDistanceM = localDistanceM;
		this.travelDirection = entry.direction();
		this.sample = sample;
		this.frame = frame;
		if (this.travelDirection == RailPathEntry.FORWARD) {
			this.travelTx = sample.tx;
			this.travelTy = sample.ty;
			this.travelTz = sample.tz;
			this.travelYawDeg = sample.yawDeg;
			this.travelPitchDeg = sample.pitchDeg;
		} else {
			this.travelTx = -sample.tx;
			this.travelTy = -sample.ty;
			this.travelTz = -sample.tz;
			this.travelYawDeg = RailMath.wrapYaw(sample.yawDeg + 180.0D);
			this.travelPitchDeg = -sample.pitchDeg;
		}
	}

	static PathSample create(double globalDistanceM, int entryIndex, RailPathEntry entry, double localDistanceM,
			RailSample sample, RailLocalFrame frame) {
		return new PathSample(globalDistanceM, entryIndex, entry, localDistanceM, sample, frame);
	}

	@Override
	public String toString() {
		return "PathSample{g=" + globalDistanceM + " entry=" + entryIndex
				+ " pieceId=" + pieceId + " local=" + localDistanceM
				+ " dir=" + travelDirection + " yaw=" + travelYawDeg + " pitch=" + travelPitchDeg + "}";
	}
}
