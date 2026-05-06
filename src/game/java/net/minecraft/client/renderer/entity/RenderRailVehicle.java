package net.minecraft.client.renderer.entity;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityRailVehicle;
import net.minecraft.util.ResourceLocation;

public class RenderRailVehicle extends Render<EntityRailVehicle> {
	private static final double MAX_COUPLER_RENDER_DISTANCE_SQ = 400.0D;
	private static final boolean DEBUG_COUPLER_RENDER = false;
	private static final double BODY_HALF_LENGTH = 1.22D;
	private static final double BODY_HALF_WIDTH = 0.48D;
	private static final double BODY_HEIGHT = 0.92D;

	public RenderRailVehicle(RenderManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.0F;
	}

	public void doRender(EntityRailVehicle entity, double x, double y, double z, float entityYaw, float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, (float) z);
		GlStateManager.rotate(this.interpolateYaw(entity.prevRotationYaw, entity.rotationYaw, partialTicks), 0.0F, 1.0F,
				0.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		this.drawRailVehicleModel(entity);
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
		this.drawCouplerToNextCar(entity, x, y, z, partialTicks);
		super.doRender(entity, x, y, z, entityYaw, partialTicks);
	}

	private float interpolateYaw(float previousYaw, float currentYaw, float partialTicks) {
		float delta = currentYaw - previousYaw;
		while (delta < -180.0F) {
			delta += 360.0F;
		}

		while (delta >= 180.0F) {
			delta -= 360.0F;
		}

		return previousYaw + partialTicks * delta;
	}

	private void drawRailVehicleModel(EntityRailVehicle entity) {
		int bodyRed = entity.vehicleType == EntityRailVehicle.VehicleType.EXPRESS ? 220
				: (entity.vehicleType == EntityRailVehicle.VehicleType.FREIGHT ? 110 : (entity.isLeadCar ? 200 : 120));
		int bodyGreen = entity.vehicleType == EntityRailVehicle.VehicleType.EXPRESS ? 220
				: (entity.vehicleType == EntityRailVehicle.VehicleType.FREIGHT ? 90 : (entity.isLeadCar ? 45 : 40));
		int bodyBlue = entity.vehicleType == EntityRailVehicle.VehicleType.EXPRESS ? 235
				: (entity.vehicleType == EntityRailVehicle.VehicleType.FREIGHT ? 80 : (entity.isLeadCar ? 230 : 150));
		int roofRed = entity.isLeadCar ? 140 : 80;
		int roofGreen = entity.isLeadCar ? 30 : 25;
		int roofBlue = entity.isLeadCar ? 180 : 110;
		this.drawCuboid(-BODY_HALF_LENGTH, 0.18D, -BODY_HALF_WIDTH, BODY_HALF_LENGTH, BODY_HEIGHT, BODY_HALF_WIDTH,
				bodyRed, bodyGreen, bodyBlue, 255);
		this.drawCuboid(-0.98D, 0.88D, -0.36D, 0.98D, 1.14D, 0.36D, roofRed, roofGreen, roofBlue, 255);
		this.drawCuboid(1.19D, 0.25D, -0.44D, 1.29D, 0.88D, 0.44D, entity.isLeadCar ? 245 : 95,
				entity.isLeadCar ? 65 : 65, entity.isLeadCar ? 70 : 115, 255);
		this.drawCuboid(1.30D, 0.50D, -0.28D, 1.35D, 0.80D, 0.28D, 80, 210, 255, 255);
		if (entity.carIndex == 0) {
			this.drawCuboid(1.36D, 0.60D, -0.32D, 1.41D, 0.76D, -0.19D, 255, 245, 150, 255);
			this.drawCuboid(1.36D, 0.60D, 0.19D, 1.41D, 0.76D, 0.32D, 255, 245, 150, 255);
		}
		if (entity.trainLength > 1 && entity.carIndex == entity.trainLength - 1) {
			this.drawCuboid(-1.35D, 0.54D, -0.33D, -1.30D, 0.70D, -0.21D, 255, 20, 20, 255);
			this.drawCuboid(-1.35D, 0.54D, 0.21D, -1.30D, 0.70D, 0.33D, 255, 20, 20, 255);
		}
		this.drawCuboid(-1.20D, 0.38D, -0.49D, 0.92D, 0.72D, -0.53D, 70, 190, 230, 255);
		this.drawCuboid(-1.20D, 0.38D, 0.49D, 0.92D, 0.72D, 0.53D, 70, 190, 230, 255);
		this.drawCuboid(-0.82D, -0.04D, -0.58D, -0.38D, 0.18D, -0.42D, 20, 20, 20, 255);
		this.drawCuboid(0.38D, -0.04D, -0.58D, 0.82D, 0.18D, -0.42D, 20, 20, 20, 255);
		this.drawCuboid(-0.82D, -0.04D, 0.42D, -0.38D, 0.18D, 0.58D, 20, 20, 20, 255);
		this.drawCuboid(0.38D, -0.04D, 0.42D, 0.82D, 0.18D, 0.58D, 20, 20, 20, 255);
	}

	private void drawCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int red,
			int green, int blue, int alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		this.addFace(worldrenderer, minX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		this.addFace(worldrenderer, maxX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(worldrenderer, minX, minY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
		this.addFace(worldrenderer, maxX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(worldrenderer, minX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(worldrenderer, minX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
		tessellator.draw();
	}

	private void addFace(WorldRenderer worldrenderer, double x1, double y1, double z1, double x2, double y2, double z2,
			int red, int green, int blue, int alpha) {
		worldrenderer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(x2, y1, z1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(x1, y2, z2).color(red, green, blue, alpha).endVertex();
	}

	private void drawCouplerToNextCar(EntityRailVehicle entity, double x, double y, double z, float partialTicks) {
		EntityRailVehicle nextCar = this.findNextCar(entity);
		if (nextCar == null) {
			return;
		}

		double currentX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
		double currentY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
		double currentZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
		double nextX = nextCar.lastTickPosX + (nextCar.posX - nextCar.lastTickPosX) * (double) partialTicks;
		double nextY = nextCar.lastTickPosY + (nextCar.posY - nextCar.lastTickPosY) * (double) partialTicks;
		double nextZ = nextCar.lastTickPosZ + (nextCar.posZ - nextCar.lastTickPosZ) * (double) partialTicks;
		double dx = nextX - currentX;
		double dy = nextY - currentY;
		double dz = nextZ - currentZ;
		if (dx * dx + dy * dy + dz * dz > MAX_COUPLER_RENDER_DISTANCE_SQ) {
			return;
		}

		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		double startX = x;
		double startY = y + 0.85D;
		double startZ = z;
		double endX = x + dx;
		double endY = y + dy + 0.85D;
		double endZ = z + dz;
		if (DEBUG_COUPLER_RENDER) {
			this.drawCouplerLine(startX, startY, startZ - 0.18D, endX, endY, endZ - 0.18D, 255, 0, 0, 255);
			this.drawCouplerLine(startX, startY, startZ - 0.06D, endX, endY, endZ - 0.06D, 255, 0, 0, 255);
			this.drawCouplerLine(startX, startY, startZ + 0.06D, endX, endY, endZ + 0.06D, 255, 0, 0, 255);
			this.drawCouplerLine(startX, startY, startZ + 0.18D, endX, endY, endZ + 0.18D, 255, 0, 0, 255);
			double midX = (startX + endX) * 0.5D;
			double midY = (startY + endY) * 0.5D;
			double midZ = (startZ + endZ) * 0.5D;
			this.drawCuboid(midX - 0.16D, midY - 0.16D, midZ - 0.16D, midX + 0.16D, midY + 0.16D, midZ + 0.16D,
					255, 0, 0, 255);
		} else {
			this.drawCouplerLine(startX, startY, startZ - 0.12D, endX, endY, endZ - 0.12D, 35, 35, 35, 255);
			this.drawCouplerLine(startX, startY, startZ + 0.12D, endX, endY, endZ + 0.12D, 35, 35, 35, 255);
		}
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	private EntityRailVehicle findNextCar(EntityRailVehicle entity) {
		if (entity.worldObj == null || entity.trainId < 0) {
			return null;
		}

		int targetCarIndex = entity.carIndex + 1;
		for (int i = 0; i < entity.worldObj.loadedEntityList.size(); ++i) {
			Entity loadedEntity = (Entity) entity.worldObj.loadedEntityList.get(i);
			if (loadedEntity instanceof EntityRailVehicle && !loadedEntity.isDead) {
				EntityRailVehicle vehicle = (EntityRailVehicle) loadedEntity;
				if (vehicle.trainId == entity.trainId && vehicle.carIndex == targetCarIndex) {
					return vehicle;
				}
			}
		}

		return null;
	}

	private void drawCouplerLine(double x1, double y1, double z1, double x2, double y2, double z2, int red, int green,
			int blue, int alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(1, DefaultVertexFormats.POSITION_COLOR);
		worldrenderer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
	}

	protected ResourceLocation getEntityTexture(EntityRailVehicle entity) {
		return null;
	}
}
