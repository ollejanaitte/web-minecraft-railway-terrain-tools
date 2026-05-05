package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.rail.RailGraph;
import net.minecraft.rail.RailNode;
import net.minecraft.rail.RailSegment;
import net.minecraft.rail.RailSystemManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityRailVehicle extends Entity {
	public static final int SPAWN_OBJECT_TYPE = 80;
	private static final double YAW_LOOK_AHEAD_PROGRESS = 0.02D;
	public int segmentId = -1;
	public double progress = 0.0D;
	public double speed = 0.01D;

	public EntityRailVehicle(World worldIn) {
		super(worldIn);
		this.noClip = true;
		this.preventEntitySpawning = false;
		this.setSize(0.9F, 0.6F);
	}

	public EntityRailVehicle(World worldIn, int segmentId) {
		this(worldIn);
		this.segmentId = segmentId;
		this.updateRailPosition();
		this.prevRotationYaw = this.rotationYaw;
	}

	protected void entityInit() {
	}

	public void setRailSegment(int segmentId) {
		this.segmentId = segmentId;
		this.progress = 0.0D;
		this.updateRailPosition();
		this.prevRotationYaw = this.rotationYaw;
	}

	public void setRailProgress(double progress) {
		this.progress = this.clampProgress(progress);
		this.updateRailPosition();
	}

	public void onUpdate() {
		super.onUpdate();
		if (this.segmentId < 0) {
			return;
		}

		this.progress += this.speed;
		if (this.progress > 1.0D) {
			this.progress = 0.0D;
		} else if (this.progress < 0.0D) {
			this.progress = 1.0D;
		}

		this.updateRailPosition();
	}

	private void updateRailPosition() {
		RailGraph graph = RailSystemManager.getGraphForWorld(this.worldObj);
		RailSegment segment = graph.getSegment(this.segmentId);
		if (segment == null) {
			return;
		}

		RailNode start = graph.getNode(segment.getStartNodeId());
		RailNode end = graph.getNode(segment.getEndNodeId());
		if (start == null || end == null) {
			return;
		}

		Vec3 point = segment.getPoint(this.progress, start, end);
		Vec3 ahead = segment.getPoint(this.getAheadProgress(), start, end);
		this.setPosition(point.xCoord, point.yCoord + 0.5D, point.zCoord);
		this.updateRailYaw(point, ahead);
	}

	private double getAheadProgress() {
		double aheadProgress = this.progress + YAW_LOOK_AHEAD_PROGRESS;
		return aheadProgress > 1.0D ? 1.0D : aheadProgress;
	}

	private void updateRailYaw(Vec3 current, Vec3 ahead) {
		double dx = ahead.xCoord - current.xCoord;
		double dz = ahead.zCoord - current.zCoord;
		if (dx * dx + dz * dz <= 1.0E-6D) {
			dx = this.posX - this.prevPosX;
			dz = this.posZ - this.prevPosZ;
		}

		if (dx * dx + dz * dz > 1.0E-6D) {
			this.rotationYaw = (float) (-Math.atan2(dz, dx) * 180.0D / Math.PI);
			while (this.rotationYaw - this.prevRotationYaw < -180.0F) {
				this.prevRotationYaw -= 360.0F;
			}

			while (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
				this.prevRotationYaw += 360.0F;
			}
		}
	}

	private double clampProgress(double progress) {
		if (progress < 0.0D) {
			return 0.0D;
		}
		return progress > 1.0D ? 1.0D : progress;
	}

	public boolean canBeCollidedWith() {
		return false;
	}

	public boolean canBePushed() {
		return false;
	}

	protected void readEntityFromNBT(NBTTagCompound tagCompund) {
		this.segmentId = tagCompund.getInteger("SegmentId");
		this.progress = this.clampProgress(tagCompund.getDouble("Progress"));
		this.speed = tagCompund.hasKey("Speed") ? tagCompund.getDouble("Speed") : 0.01D;
	}

	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		tagCompound.setInteger("SegmentId", this.segmentId);
		tagCompound.setDouble("Progress", this.progress);
		tagCompound.setDouble("Speed", this.speed);
	}
}
