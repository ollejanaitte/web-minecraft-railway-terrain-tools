package net.minecraft.railsys.placement;

import net.minecraft.entity.Entity;

/**
 * RailsysCamera — Phase 1-R9 client-side camera entity for screenshots.
 *
 * A standalone Entity (empty NBT/init) that can be set as the render view
 * entity so the client camera is placed/directed independently of the
 * player entity. Server position sync (S08PacketPlayerPosLook) only rewrites
 * the player entity, NOT this camera entity, so a held camera stays fixed
 * across frames — ideal for reproducible placement/asset comparison
 * screenshots in any (normal) world.
 *
 * Reset with {@code Minecraft.getMinecraft().setRenderViewEntity(player)}.
 */
public final class RailsysCamera extends Entity {

	private static RailsysCamera instance;

	public static RailsysCamera get() {
		if (instance == null) {
			instance = new RailsysCamera(net.minecraft.client.Minecraft.getMinecraft().theWorld);
		}
		return instance;
	}

	private RailsysCamera(net.minecraft.world.World worldIn) {
		super(worldIn);
		this.setSize(0.1F, 0.1F);
		this.noClip = true;
	}

	/**
	 * Reset the render view back to the local player (default camera).
	 * Safe no-op when mc is null or the player is unavailable.
	 */
	public static void reset(net.minecraft.client.Minecraft mc) {
		if (mc != null && mc.thePlayer != null) {
			mc.setRenderViewEntity(mc.thePlayer);
		}
	}
	public void place(double x, double y, double z, float yaw, float pitch) {
		this.setLocationAndAngles(x, y, z, yaw, pitch);
		this.prevPosX = this.lastTickPosX = x;
		this.prevPosY = this.lastTickPosY = y;
		this.prevPosZ = this.lastTickPosZ = z;
		this.prevRotationYaw = this.rotationYaw = yaw;
		this.prevRotationPitch = this.rotationPitch = pitch;
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tagCompund) {
	}

	@Override
	protected void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tagCompound) {
	}
}
