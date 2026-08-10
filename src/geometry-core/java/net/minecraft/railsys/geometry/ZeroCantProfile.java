package net.minecraft.railsys.geometry;

/** Default zero cant. */
public final class ZeroCantProfile implements CantProfile {

	public static final ZeroCantProfile INSTANCE = new ZeroCantProfile();

	private ZeroCantProfile() {
	}

	@Override
	public double rollDegAt(double distanceM, double lengthM) {
		return 0.0D;
	}
}
