package net.minecraft.railsys.geometry;

/**
 * Future cant integration point. Phase 1.1: data model / no-op only; no physics, no RTM UI.
 * rollDeg positive = right rail lower.
 */
public interface CantProfile {

	double rollDegAt(double distanceM, double lengthM);
}
