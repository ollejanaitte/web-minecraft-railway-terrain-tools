package net.minecraft.railv2;

/**
 * Phase 0.1 validation spike: rail geometry contract.
 * A piece provides position + yaw/pitch/roll at any distance along it.
 */
public interface RailV2Geometry {
	double lengthM();

	int pieceId();

	RailV2Sample sampleByDistance(double distanceM);
}
