package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.rail.RailGraph;
import net.minecraft.rail.RailNode;
import net.minecraft.rail.RailSegment;
import net.minecraft.rail.RailSystemManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityRailVehicle extends Entity {
	public static enum VehicleType {
		DEFAULT,
		EXPRESS,
		FREIGHT
	}

	public static final int SPAWN_OBJECT_TYPE = 80;
	private static final double YAW_LOOK_AHEAD_PROGRESS = 0.02D;
	private static final int DW_TRAIN_ID = 5;
	private static final int DW_CAR_INDEX = 6;
	private static final int DW_TRAIN_LENGTH = 7;
	private static final int DW_CAR_SPACING = 8;
	private static final int DW_IS_LEAD_CAR = 9;
	private static final int DW_TARGET_SPEED = 10;
	private static final int DW_SPEED = 11;
	private static final int DW_SEGMENT_ID = 12;
	private static final int DW_PROGRESS = 13;
	private static final int STATION_DWELL_TICKS = 60;
	public int segmentId = -1;
	public double progress = 0.0D;
	public double speed = 0.0D;
	public double targetSpeed = 0.0D;
	public double maxSpeed = 0.02D;
	public double acceleration = 0.0005D;
	public int trainId = -1;
	public int carIndex = 0;
	public int trainLength = 1;
	public double carSpacing = 0.2D;
	public boolean isLeadCar = true;
	public boolean forward = true;
	public boolean stopAtDistanceEnabled = false;
	public double traveledDistance = 0.0D;
	public double stopAtDistance = -1.0D;
	public int stationDwellTicks = 0;
	public VehicleType vehicleType = VehicleType.DEFAULT;
	private double stationResumeTargetSpeed = 0.02D;

	public EntityRailVehicle(World worldIn) {
		super(worldIn);
		this.noClip = true;
		this.preventEntitySpawning = false;
		this.setSize(1.4F, 1.4F);
	}

	public EntityRailVehicle(World worldIn, int segmentId) {
		this(worldIn);
		this.segmentId = segmentId;
		this.updateRailPosition();
		this.prevRotationYaw = this.rotationYaw;
	}

	protected void entityInit() {
		this.dataWatcher.addObject(DW_TRAIN_ID, Integer.valueOf(this.trainId));
		this.dataWatcher.addObject(DW_CAR_INDEX, Integer.valueOf(this.carIndex));
		this.dataWatcher.addObject(DW_TRAIN_LENGTH, Integer.valueOf(this.trainLength));
		this.dataWatcher.addObject(DW_CAR_SPACING, Float.valueOf((float) this.carSpacing));
		this.dataWatcher.addObject(DW_IS_LEAD_CAR, Byte.valueOf((byte) (this.isLeadCar ? 1 : 0)));
		this.dataWatcher.addObject(DW_TARGET_SPEED, Float.valueOf((float) this.targetSpeed));
		this.dataWatcher.addObject(DW_SPEED, Float.valueOf((float) this.speed));
		this.dataWatcher.addObject(DW_SEGMENT_ID, Integer.valueOf(this.segmentId));
		this.dataWatcher.addObject(DW_PROGRESS, Float.valueOf((float) this.progress));
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
		this.updateRailMotionDataWatcher();
	}

	public void setTrainData(int trainId, int carIndex, int trainLength, double carSpacing, boolean isLeadCar) {
		this.trainId = trainId;
		this.carIndex = carIndex;
		this.trainLength = trainLength;
		this.carSpacing = carSpacing;
		this.isLeadCar = isLeadCar;
		this.updateTrainDataWatcher();
	}

	public void setTargetSpeed(double targetSpeed) {
		this.targetSpeed = this.clampSpeed(targetSpeed);
		if (this.targetSpeed > 0.0D && this.stopAtDistanceEnabled) {
			this.stopAtDistanceEnabled = false;
		}
		this.updateRailMotionDataWatcher();
	}

	public void setInitialStopDistance(double stopAtDistance) {
		this.stopAtDistanceEnabled = stopAtDistance >= 0.0D;
		this.stopAtDistance = stopAtDistance;
		this.traveledDistance = 0.0D;
	}

	public void onUpdate() {
		super.onUpdate();
		double lastRailX = this.posX;
		double lastRailY = this.posY;
		double lastRailZ = this.posZ;
		if (this.worldObj.isRemote) {
			this.readTrainDataWatcher();
		}

		if (this.segmentId < 0) {
			return;
		}

		if (this.worldObj.isRemote) {
			this.updateRailPosition();
			return;
		}

		if (this.isLeadCar) {
			this.updateRiderControls();
			this.updateSpeedTowardsTarget();
			this.updateLeadProgress();
		} else {
			EntityRailVehicle leadCar = this.findLeadCar();
			if (leadCar != null) {
				this.segmentId = leadCar.segmentId;
				this.trainLength = leadCar.trainLength;
				this.carSpacing = leadCar.carSpacing;
				this.forward = leadCar.forward;
				this.speed = leadCar.speed;
				this.targetSpeed = leadCar.targetSpeed;
				double targetProgress = leadCar.progress - this.carSpacing * (double) this.carIndex;
				while (targetProgress < 0.0D) {
					targetProgress += 1.0D;
				}
				while (targetProgress > 1.0D) {
					targetProgress -= 1.0D;
				}
				this.progress = targetProgress;
			}
		}

		this.updateRailPosition();
		if (this.isLeadCar) {
			this.updateInitialStopDistance(lastRailX, lastRailY, lastRailZ);
		}
		this.updateRailMotionDataWatcher();
	}

	private void updateRiderControls() {
		if (this.riddenByEntity instanceof EntityLivingBase) {
			EntityLivingBase rider = (EntityLivingBase) this.riddenByEntity;
			if (rider.moveForward > 0.05F) {
				this.setTargetSpeed(this.targetSpeed + this.acceleration);
			} else if (rider.moveForward < -0.05F) {
				this.setTargetSpeed(this.targetSpeed - this.acceleration);
			}
		}
	}

	private void updateSpeedTowardsTarget() {
		if (this.stationDwellTicks > 0) {
			--this.stationDwellTicks;
			this.speed = 0.0D;
			if (this.stationDwellTicks == 0) {
				this.setTargetSpeed(this.stationResumeTargetSpeed);
			}
			return;
		}

		this.targetSpeed = this.clampSpeed(this.targetSpeed);
		if (this.speed < this.targetSpeed) {
			this.speed = Math.min(this.speed + this.acceleration, this.targetSpeed);
		} else if (this.speed > this.targetSpeed) {
			this.speed = Math.max(this.speed - this.acceleration, this.targetSpeed);
		}
		this.speed = this.clampSpeed(this.speed);
	}

	private void updateLeadProgress() {
		if (this.forward) {
			this.progress += this.speed;
		} else {
			this.progress -= this.speed;
		}

		if (this.progress > 1.0D || this.progress < 0.0D) {
			this.moveToNextSegment();
		}
	}

	private void updateInitialStopDistance(double lastRailX, double lastRailY, double lastRailZ) {
		if (!this.stopAtDistanceEnabled || this.stopAtDistance < 0.0D || this.speed <= 0.0D) {
			return;
		}

		double dx = this.posX - lastRailX;
		double dy = this.posY - lastRailY;
		double dz = this.posZ - lastRailZ;
		this.traveledDistance += Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (this.traveledDistance >= this.stopAtDistance) {
			this.speed = 0.0D;
			this.targetSpeed = 0.0D;
			this.stopAtDistanceEnabled = false;
		}
	}

	private void moveToNextSegment() {
		RailGraph graph = RailSystemManager.getGraphForWorld(this.worldObj);
		RailSegment currentSegment = graph.getSegment(this.segmentId);
		if (currentSegment == null) {
			this.progress = this.clampProgress(this.progress);
			this.speed = 0.0D;
			return;
		}

		int transitionNodeId = this.forward ? currentSegment.getEndNodeId() : currentSegment.getStartNodeId();
		RailSegment nextSegment = this.findConnectedSegment(graph, currentSegment, transitionNodeId);
		if (nextSegment == null) {
			this.progress = this.forward ? 1.0D : 0.0D;
			this.speed = 0.0D;
			this.targetSpeed = 0.0D;
			this.updateRailMotionDataWatcher();
			return;
		}

		if (graph.isSegmentOccupied(nextSegment.getId())) {
			this.progress = this.forward ? 1.0D : 0.0D;
			this.speed = 0.0D;
			this.targetSpeed = 0.0D;
			this.updateRailMotionDataWatcher();
			return;
		}

		// Phase 6-B/6-C: move through a connected segment, with switch routing taking priority.
		graph.setSegmentOccupied(this.segmentId, false);
		this.segmentId = nextSegment.getId();
		graph.setSegmentOccupied(this.segmentId, true);
		if (nextSegment.getStartNodeId() == transitionNodeId) {
			this.forward = true;
			this.progress = 0.0D;
		} else {
			this.forward = false;
			this.progress = 1.0D;
		}

		if (graph.isStationNode(transitionNodeId)) {
			this.stationDwellTicks = STATION_DWELL_TICKS;
			this.stationResumeTargetSpeed = this.targetSpeed > 0.0D ? this.targetSpeed : 0.02D;
			this.speed = 0.0D;
			this.targetSpeed = 0.0D;
		}
	}

	private RailSegment findConnectedSegment(RailGraph graph, RailSegment currentSegment, int transitionNodeId) {
		if (graph.getNode(transitionNodeId) == null) {
			return null;
		}

		int switchTargetSegmentId = graph.getSwitchTargetSegment(transitionNodeId);
		if (switchTargetSegmentId != currentSegment.getId()
				&& graph.isValidSwitchTarget(transitionNodeId, switchTargetSegmentId)) {
			return graph.getSegment(switchTargetSegmentId);
		}

		RailSegment routeSegment = this.findRouteSegment(graph, currentSegment, transitionNodeId,
				graph.getTrainTargetNode(this.trainId));
		if (routeSegment != null) {
			return routeSegment;
		}

		for (RailSegment segment : graph.getSegmentsConnectedToNode(transitionNodeId)) {
			if (segment != null && segment.getId() != currentSegment.getId()) {
				return segment;
			}
		}

		return null;
	}

	private RailSegment findRouteSegment(RailGraph graph, RailSegment currentSegment, int transitionNodeId,
			int targetNodeId) {
		RailNode targetNode = graph.getNode(targetNodeId);
		if (targetNode == null) {
			return null;
		}

		RailSegment bestSegment = null;
		double bestDistanceSq = Double.MAX_VALUE;
		for (RailSegment segment : graph.getSegmentsConnectedToNode(transitionNodeId)) {
			if (segment == null || segment.getId() == currentSegment.getId()) {
				continue;
			}

			int otherNodeId = segment.getStartNodeId() == transitionNodeId ? segment.getEndNodeId()
					: segment.getStartNodeId();
			RailNode otherNode = graph.getNode(otherNodeId);
			if (otherNode == null) {
				continue;
			}

			double distanceSq = otherNode.distanceSqTo(targetNode);
			if (distanceSq < bestDistanceSq) {
				bestDistanceSq = distanceSq;
				bestSegment = segment;
			}
		}

		return bestSegment;
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
		if (!this.worldObj.isRemote && this.isLeadCar) {
			graph.setSegmentOccupied(this.segmentId, true);
		}
	}

	private double getAheadProgress() {
		double aheadProgress = this.progress + (this.forward ? YAW_LOOK_AHEAD_PROGRESS : -YAW_LOOK_AHEAD_PROGRESS);
		if (aheadProgress < 0.0D) {
			return 0.0D;
		}
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

	private double clampSpeed(double speed) {
		if (speed < 0.0D) {
			return 0.0D;
		}
		return speed > this.maxSpeed ? this.maxSpeed : speed;
	}

	private double wrapProgress(double progress) {
		while (progress < 0.0D) {
			progress += 1.0D;
		}

		while (progress > 1.0D) {
			progress -= 1.0D;
		}

		return progress;
	}

	private EntityRailVehicle findLeadCar() {
		if (this.worldObj == null || this.trainId < 0) {
			return null;
		}

		for (int i = 0; i < this.worldObj.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) this.worldObj.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle != this && !vehicle.isDead && vehicle.trainId == this.trainId && vehicle.carIndex == 0) {
					return vehicle;
				}
			}
		}

		return null;
	}

	private void updateTrainDataWatcher() {
		this.dataWatcher.updateObject(DW_TRAIN_ID, Integer.valueOf(this.trainId));
		this.dataWatcher.updateObject(DW_CAR_INDEX, Integer.valueOf(this.carIndex));
		this.dataWatcher.updateObject(DW_TRAIN_LENGTH, Integer.valueOf(this.trainLength));
		this.dataWatcher.updateObject(DW_CAR_SPACING, Float.valueOf((float) this.carSpacing));
		this.dataWatcher.updateObject(DW_IS_LEAD_CAR, Byte.valueOf((byte) (this.isLeadCar ? 1 : 0)));
		this.updateRailMotionDataWatcher();
	}

	private void updateRailMotionDataWatcher() {
		this.dataWatcher.updateObject(DW_TARGET_SPEED, Float.valueOf((float) this.targetSpeed));
		this.dataWatcher.updateObject(DW_SPEED, Float.valueOf((float) this.speed));
		this.dataWatcher.updateObject(DW_SEGMENT_ID, Integer.valueOf(this.segmentId));
		this.dataWatcher.updateObject(DW_PROGRESS, Float.valueOf((float) this.progress));
	}

	private void readTrainDataWatcher() {
		this.trainId = this.dataWatcher.getWatchableObjectInt(DW_TRAIN_ID);
		this.carIndex = this.dataWatcher.getWatchableObjectInt(DW_CAR_INDEX);
		this.trainLength = this.dataWatcher.getWatchableObjectInt(DW_TRAIN_LENGTH);
		this.carSpacing = (double) this.dataWatcher.getWatchableObjectFloat(DW_CAR_SPACING);
		this.isLeadCar = this.dataWatcher.getWatchableObjectByte(DW_IS_LEAD_CAR) != 0;
		this.targetSpeed = (double) this.dataWatcher.getWatchableObjectFloat(DW_TARGET_SPEED);
		this.speed = (double) this.dataWatcher.getWatchableObjectFloat(DW_SPEED);
		this.segmentId = this.dataWatcher.getWatchableObjectInt(DW_SEGMENT_ID);
		this.progress = (double) this.dataWatcher.getWatchableObjectFloat(DW_PROGRESS);
	}

	public boolean canBeCollidedWith() {
		return !this.isDead;
	}

	public boolean canBePushed() {
		return false;
	}

	public boolean interactFirst(EntityPlayer playerIn) {
		if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer
				&& this.riddenByEntity != playerIn) {
			return true;
		}

		if (!this.worldObj.isRemote) {
			playerIn.mountEntity(this);
		}

		return true;
	}

	public void updateRiderPosition() {
		if (this.riddenByEntity != null) {
			double yawRadians = (double) this.rotationYaw * Math.PI / 180.0D;
			double offsetX = -Math.sin(yawRadians) * 0.05D;
			double offsetZ = Math.cos(yawRadians) * 0.05D;
			this.riddenByEntity.setPosition(this.posX + offsetX,
					this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ + offsetZ);
		}
	}

	public double getMountedYOffset() {
		return 1.55D;
	}

	protected void readEntityFromNBT(NBTTagCompound tagCompund) {
		this.segmentId = tagCompund.getInteger("SegmentId");
		this.progress = this.clampProgress(tagCompund.getDouble("Progress"));
		this.speed = tagCompund.hasKey("Speed") ? tagCompund.getDouble("Speed") : 0.0D;
		this.targetSpeed = tagCompund.hasKey("TargetSpeed") ? tagCompund.getDouble("TargetSpeed") : 0.0D;
		this.maxSpeed = tagCompund.hasKey("MaxSpeed") ? tagCompund.getDouble("MaxSpeed") : 0.02D;
		this.acceleration = tagCompund.hasKey("Acceleration") ? tagCompund.getDouble("Acceleration") : 0.0005D;
		this.trainId = tagCompund.hasKey("TrainId") ? tagCompund.getInteger("TrainId") : -1;
		this.carIndex = tagCompund.hasKey("CarIndex") ? tagCompund.getInteger("CarIndex") : 0;
		this.trainLength = tagCompund.hasKey("TrainLength") ? tagCompund.getInteger("TrainLength") : 1;
		this.carSpacing = tagCompund.hasKey("CarSpacing") ? tagCompund.getDouble("CarSpacing") : 0.2D;
		this.isLeadCar = !tagCompund.hasKey("IsLeadCar") || tagCompund.getBoolean("IsLeadCar");
		this.forward = !tagCompund.hasKey("Forward") || tagCompund.getBoolean("Forward");
		this.stopAtDistanceEnabled = tagCompund.hasKey("StopAtDistanceEnabled")
				&& tagCompund.getBoolean("StopAtDistanceEnabled");
		this.traveledDistance = tagCompund.hasKey("TraveledDistance") ? tagCompund.getDouble("TraveledDistance") : 0.0D;
		this.stopAtDistance = tagCompund.hasKey("StopAtDistance") ? tagCompund.getDouble("StopAtDistance") : -1.0D;
		this.stationDwellTicks = tagCompund.hasKey("StationDwellTicks") ? tagCompund.getInteger("StationDwellTicks") : 0;
		this.stationResumeTargetSpeed = tagCompund.hasKey("StationResumeTargetSpeed")
				? tagCompund.getDouble("StationResumeTargetSpeed") : 0.02D;
		if (tagCompund.hasKey("VehicleType")) {
			try {
				this.vehicleType = VehicleType.valueOf(tagCompund.getString("VehicleType"));
			} catch (IllegalArgumentException ex) {
				this.vehicleType = VehicleType.DEFAULT;
			}
		}
		this.updateTrainDataWatcher();
	}

	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		tagCompound.setInteger("SegmentId", this.segmentId);
		tagCompound.setDouble("Progress", this.progress);
		tagCompound.setDouble("Speed", this.speed);
		tagCompound.setDouble("TargetSpeed", this.targetSpeed);
		tagCompound.setDouble("MaxSpeed", this.maxSpeed);
		tagCompound.setDouble("Acceleration", this.acceleration);
		tagCompound.setInteger("TrainId", this.trainId);
		tagCompound.setInteger("CarIndex", this.carIndex);
		tagCompound.setInteger("TrainLength", this.trainLength);
		tagCompound.setDouble("CarSpacing", this.carSpacing);
		tagCompound.setBoolean("IsLeadCar", this.isLeadCar);
		tagCompound.setBoolean("Forward", this.forward);
		tagCompound.setBoolean("StopAtDistanceEnabled", this.stopAtDistanceEnabled);
		tagCompound.setDouble("TraveledDistance", this.traveledDistance);
		tagCompound.setDouble("StopAtDistance", this.stopAtDistance);
		tagCompound.setInteger("StationDwellTicks", this.stationDwellTicks);
		tagCompound.setDouble("StationResumeTargetSpeed", this.stationResumeTargetSpeed);
		tagCompound.setString("VehicleType", this.vehicleType.name());
	}
}
