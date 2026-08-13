package net.minecraft.railsys.junction;

/**
 * SwitchRoute — the route a switch junction commits to (R17).
 *
 * THROUGH: continue on the main-out segment (straight continuation).
 * BRANCH:  continue on a chosen branch segment.
 * UNKNOWN: no committed route (invalid input / uninitialised).
 */
public enum SwitchRoute {
	THROUGH,
	BRANCH,
	UNKNOWN
}
