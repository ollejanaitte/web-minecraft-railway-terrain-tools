package net.minecraft.rail;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

public class RailSegment {
	private static final int CURVE_LENGTH_SAMPLES = 32;
	private final int id;
	private final int startNodeId;
	private final int endNodeId;
	private double length;
	private final RailSegmentType type;
	private RailCurveData curveData;

	public RailSegment(int id, int startNodeId, int endNodeId, double length, RailSegmentType type) {
		this.id = id;
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.length = length;
		this.type = type;
	}

	public int getId() {
		return this.id;
	}

	public int getStartNodeId() {
		return this.startNodeId;
	}

	public int getEndNodeId() {
		return this.endNodeId;
	}

	public double getLength() {
		return this.length;
	}

	public RailSegmentType getType() {
		return this.type;
	}

	public RailCurveData getCurveData() {
		return this.curveData;
	}

	public void setCurveData(RailCurveData data) {
		this.curveData = data;
	}

	public boolean hasCurveData() {
		return this.curveData != null;
	}

	public Vec3 getPoint(double t, RailNode start, RailNode end) {
		double clampedT = RailCurveData.clamp(t);
		if (this.type == RailSegmentType.CURVE && this.curveData != null) {
			return this.curveData.getPoint(clampedT, start, end);
		}

		double inverseT = 1.0D - clampedT;
		double x = start.getX() * inverseT + end.getX() * clampedT;
		double y = start.getY() * inverseT + end.getY() * clampedT;
		double z = start.getZ() * inverseT + end.getZ() * clampedT;
		return new Vec3(x, y, z);
	}

	public void recalculateLength(RailNode start, RailNode end) {
		if (start == null || end == null) {
			this.length = 0.0D;
			return;
		}

		if (this.type != RailSegmentType.CURVE || this.curveData == null) {
			this.length = Math.sqrt(start.distanceSqTo(end));
			return;
		}

		double totalLength = 0.0D;
		Vec3 previousPoint = this.getPoint(0.0D, start, end);
		for (int i = 1; i <= CURVE_LENGTH_SAMPLES; ++i) {
			Vec3 point = this.getPoint((double) i / (double) CURVE_LENGTH_SAMPLES, start, end);
			totalLength += previousPoint.distanceTo(point);
			previousPoint = point;
		}

		this.length = totalLength;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("Id", this.id);
		nbt.setInteger("StartNodeId", this.startNodeId);
		nbt.setInteger("EndNodeId", this.endNodeId);
		nbt.setDouble("Length", this.length);
		nbt.setString("Type", this.type.name());
		if (this.curveData != null) {
			NBTTagCompound curveTag = new NBTTagCompound();
			this.curveData.writeToNBT(curveTag);
			nbt.setTag("CurveData", curveTag);
		}
	}

	public static RailSegment readFromNBT(NBTTagCompound nbt) {
		RailSegmentType segmentType = RailSegmentType.STRAIGHT;
		try {
			segmentType = RailSegmentType.valueOf(nbt.getString("Type"));
		} catch (IllegalArgumentException exception) {
			segmentType = RailSegmentType.STRAIGHT;
		}

		RailSegment segment = new RailSegment(nbt.getInteger("Id"), nbt.getInteger("StartNodeId"),
				nbt.getInteger("EndNodeId"), nbt.getDouble("Length"), segmentType);
		if (nbt.hasKey("CurveData", 10)) {
			segment.setCurveData(RailCurveData.readFromNBT(nbt.getCompoundTag("CurveData")));
		}
		return segment;
	}
}
