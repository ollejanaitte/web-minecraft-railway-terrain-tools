package net.minecraft.rail;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

public class RailCurveData {
	private final double controlX1;
	private final double controlY1;
	private final double controlZ1;
	private final double controlX2;
	private final double controlY2;
	private final double controlZ2;

	public RailCurveData(double controlX1, double controlY1, double controlZ1, double controlX2, double controlY2,
			double controlZ2) {
		this.controlX1 = controlX1;
		this.controlY1 = controlY1;
		this.controlZ1 = controlZ1;
		this.controlX2 = controlX2;
		this.controlY2 = controlY2;
		this.controlZ2 = controlZ2;
	}

	public double getControlX1() {
		return this.controlX1;
	}

	public double getControlY1() {
		return this.controlY1;
	}

	public double getControlZ1() {
		return this.controlZ1;
	}

	public double getControlX2() {
		return this.controlX2;
	}

	public double getControlY2() {
		return this.controlY2;
	}

	public double getControlZ2() {
		return this.controlZ2;
	}

	public Vec3 getPoint(double t, RailNode start, RailNode end) {
		double clampedT = clamp(t);
		double inverseT = 1.0D - clampedT;
		double startWeight = inverseT * inverseT * inverseT;
		double control1Weight = 3.0D * inverseT * inverseT * clampedT;
		double control2Weight = 3.0D * inverseT * clampedT * clampedT;
		double endWeight = clampedT * clampedT * clampedT;
		double x = start.getX() * startWeight + this.controlX1 * control1Weight + this.controlX2 * control2Weight
				+ end.getX() * endWeight;
		double y = start.getY() * startWeight + this.controlY1 * control1Weight + this.controlY2 * control2Weight
				+ end.getY() * endWeight;
		double z = start.getZ() * startWeight + this.controlZ1 * control1Weight + this.controlZ2 * control2Weight
				+ end.getZ() * endWeight;
		return new Vec3(x, y, z);
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setDouble("ControlX1", this.controlX1);
		nbt.setDouble("ControlY1", this.controlY1);
		nbt.setDouble("ControlZ1", this.controlZ1);
		nbt.setDouble("ControlX2", this.controlX2);
		nbt.setDouble("ControlY2", this.controlY2);
		nbt.setDouble("ControlZ2", this.controlZ2);
	}

	public static RailCurveData readFromNBT(NBTTagCompound nbt) {
		return new RailCurveData(nbt.getDouble("ControlX1"), nbt.getDouble("ControlY1"), nbt.getDouble("ControlZ1"),
				nbt.getDouble("ControlX2"), nbt.getDouble("ControlY2"), nbt.getDouble("ControlZ2"));
	}

	static double clamp(double value) {
		if (value < 0.0D) {
			return 0.0D;
		}
		return value > 1.0D ? 1.0D : value;
	}
}
