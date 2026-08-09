package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.DataWatcher;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.railv2.RailV2Course;
import net.minecraft.railv2.RailV2CourseMath;
import net.minecraft.railv2.RailV2Sample;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Phase 0.1 validation spike: full-scale bogie-anchored car.
 * Server: distance-based formation (leader advances a course distance;
 * followers at leaderDistance - k*carSpacing). Body pose derived from the
 * front/rear bogie anchors on the course. Client: reads synced carDistance,
 * rebuilds the deterministic course, and resolves bogie positions for
 * rendering.
 */
public class EntityRailV2Car extends Entity {
	public static final int SPAWN_OBJECT_TYPE = 81;

	public static final double CAR_HALF_LENGTH = 10.0D;
	public static final double CAR_WIDTH = 2.8D;
	public static final double CAR_HEIGHT = 3.8D;
	public static final double BOGIE_OFFSET = 7.0D;
	public static final double CAR_SPACING = 22.0D;

	private static final int DW_CAR_INDEX = 20;
	private static final int DW_TRAIN_LENGTH = 21;
	private static final int DW_CAR_DISTANCE = 22;
	private static final int DW_SPEED = 23;

	public int trainId = -1;
	public int carIndex = 0;
	public int trainLength = 1;
	public double carDistance = 0.0D;
	public double leaderDistance = 0.0D;
	public boolean isLead = true;
	public double speed = 0.0D;
	public int direction = 1;

	private static final RailV2Course COURSE = RailV2Course.INSTANCE;

	public EntityRailV2Car(World worldIn) {
		super(worldIn);
		this.noClip = true;
		this.setSize((float) CAR_WIDTH, (float) CAR_HEIGHT);
	}

	public EntityRailV2Car(World worldIn, int trainId, int carIndex, int trainLength, boolean isLead) {
		this(worldIn);
		this.trainId = trainId;
		this.carIndex = carIndex;
		this.trainLength = trainLength;
		this.isLead = isLead;
	}

	@Override
	protected void entityInit() {
		this.dataWatcher.addObject(DW_CAR_INDEX, Integer.valueOf(this.carIndex));
		this.dataWatcher.addObject(DW_TRAIN_LENGTH, Integer.valueOf(this.trainLength));
		this.dataWatcher.addObject(DW_CAR_DISTANCE, Float.valueOf((float) this.carDistance));
		this.dataWatcher.addObject(DW_SPEED, Float.valueOf((float) this.speed));
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (this.worldObj.isRemote) {
			this.readSync();
			this.applyPoseFromBogies();
			return;
		}

		if (this.isLead) {
			this.leaderDistance += (double) this.direction * this.speed;
			this.carDistance = this.leaderDistance;
			this.updateFollowers();
		} else {
			EntityRailV2Car lead = this.findLead();
			if (lead != null) {
				this.leaderDistance = lead.leaderDistance;
				this.speed = lead.speed;
				this.carDistance = lead.leaderDistance - (double) this.carIndex * CAR_SPACING;
			}
		}
		this.applyPoseFromBogies();
		this.updateSync();
	}

	private void updateFollowers() {
		if (this.worldObj == null || this.trainId < 0) {
			return;
		}
		for (int i = 0; i < this.worldObj.loadedEntityList.size(); i++) {
			Entity e = this.worldObj.loadedEntityList.get(i);
			if (e instanceof EntityRailV2Car && !e.isDead && e != this) {
				EntityRailV2Car car = (EntityRailV2Car) e;
				if (car.trainId == this.trainId) {
					car.leaderDistance = this.leaderDistance;
					car.speed = this.speed;
					car.direction = this.direction;
					car.carDistance = this.leaderDistance - (double) car.carIndex * CAR_SPACING;
				}
			}
		}
	}

	private EntityRailV2Car findLead() {
		if (this.worldObj == null || this.trainId < 0) {
			return null;
		}
		for (int i = 0; i < this.worldObj.loadedEntityList.size(); i++) {
			Entity e = this.worldObj.loadedEntityList.get(i);
			if (e instanceof EntityRailV2Car && !e.isDead && e != this) {
				EntityRailV2Car car = (EntityRailV2Car) e;
				if (car.trainId == this.trainId && car.isLead) {
					return car;
				}
			}
		}
		return null;
	}

	/** Compute body pose from front/rear bogie anchors on the course. */
	private void applyPoseFromBogies() {
		RailV2Sample front = COURSE.resolve(this.carDistance + BOGIE_OFFSET);
		RailV2Sample rear = COURSE.resolve(this.carDistance - BOGIE_OFFSET);
		double ratio = BOGIE_OFFSET / (BOGIE_OFFSET + BOGIE_OFFSET);
		double x = rear.x + (front.x - rear.x) * ratio;
		double y = (front.y + rear.y) * 0.5D + 0.5D;
		double z = rear.z + (front.z - rear.z) * ratio;
		double yaw = Math.toDegrees(Math.atan2(front.x - rear.x, front.z - rear.z));
		double pitch = Math.toDegrees(Math.atan2(front.y - rear.y, Math.hypot(front.x - rear.x, front.z - rear.z)));
		this.setPosition(x, y, z);
		this.rotationYaw = (float) RailV2CourseMath.wrapYaw(-yaw);
		this.rotationPitch = (float) pitch;
		this.prevRotationYaw = this.rotationYaw;
	}

	private void readSync() {
		this.carIndex = this.dataWatcher.getWatchableObjectInt(DW_CAR_INDEX);
		this.trainLength = this.dataWatcher.getWatchableObjectInt(DW_TRAIN_LENGTH);
		this.carDistance = (double) this.dataWatcher.getWatchableObjectFloat(DW_CAR_DISTANCE);
		this.speed = (double) this.dataWatcher.getWatchableObjectFloat(DW_SPEED);
	}

	private void updateSync() {
		this.dataWatcher.updateObject(DW_CAR_INDEX, Integer.valueOf(this.carIndex));
		this.dataWatcher.updateObject(DW_TRAIN_LENGTH, Integer.valueOf(this.trainLength));
		this.dataWatcher.updateObject(DW_CAR_DISTANCE, Float.valueOf((float) this.carDistance));
		this.dataWatcher.updateObject(DW_SPEED, Float.valueOf((float) this.speed));
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompund) {
		this.trainId = tagCompund.getInteger("TrainId");
		this.carIndex = tagCompund.getInteger("CarIndex");
		this.trainLength = tagCompund.getInteger("TrainLength");
		this.isLead = tagCompund.getBoolean("IsLead");
		this.carDistance = tagCompund.getDouble("CarDistance");
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		tagCompound.setInteger("TrainId", this.trainId);
		tagCompound.setInteger("CarIndex", this.carIndex);
		tagCompound.setInteger("TrainLength", this.trainLength);
		tagCompound.setBoolean("IsLead", this.isLead);
		tagCompound.setDouble("CarDistance", this.carDistance);
	}
}
