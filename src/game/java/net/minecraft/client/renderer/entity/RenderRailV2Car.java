package net.minecraft.client.renderer.entity;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityRailV2Car;
import net.minecraft.railv2.RailV2Course;
import net.minecraft.railv2.RailV2Sample;
import net.minecraft.util.ResourceLocation;

/**
 * Phase 0.1 validation spike: renders a full-scale car body aligned to the
 * front/rear bogie anchors on the course, plus bogie markers and a coupler
 * line to the next car. Body length = 20m, width 2.8m, height 3.8m.
 */
public class RenderRailV2Car extends Render<EntityRailV2Car> {
	private static final double HALF_LEN = EntityRailV2Car.CAR_HALF_LENGTH;
	private static final double HALF_W = EntityRailV2Car.CAR_WIDTH * 0.5D;
	private static final double HEIGHT = EntityRailV2Car.CAR_HEIGHT;
	private static final double BOGIE_OFFSET = EntityRailV2Car.BOGIE_OFFSET;

	public RenderRailV2Car(RenderManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.0F;
	}

	@Override
	public void doRender(EntityRailV2Car entity, double x, double y, double z, float entityYaw, float partialTicks) {
		double carDist = entity.carDistance;
		RailV2Sample front = RailV2Course.INSTANCE.resolve(carDist + BOGIE_OFFSET);
		RailV2Sample rear = RailV2Course.INSTANCE.resolve(carDist - BOGIE_OFFSET);

		double ratio = BOGIE_OFFSET / (BOGIE_OFFSET * 2.0D);
		double cx = rear.x + (front.x - rear.x) * ratio;
		double cy = (front.y + rear.y) * 0.5D + 0.5D;
		double cz = rear.z + (front.z - rear.z) * ratio;
		double dx = front.x - rear.x;
		double dz = front.z - rear.z;
		double yaw = Math.toDegrees(Math.atan2(dx, dz));

		// body
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (cx - x), (float) (cy - y), (float) (cz - z));
		GlStateManager.rotate((float) yaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		int bodyRed = entity.isLead ? 60 : 140;
		int bodyGreen = entity.isLead ? 120 : 60;
		int bodyBlue = entity.isLead ? 210 : 60;
		this.drawCuboid(-HALF_LEN, 0.0D, -HALF_W, HALF_LEN, HEIGHT, HALF_W, bodyRed, bodyGreen, bodyBlue, 255);
		// roof stripe for lead
		if (entity.isLead) {
			this.drawCuboid(-HALF_LEN + 0.5D, HEIGHT - 0.3D, -HALF_W + 0.4D, HALF_LEN - 0.5D, HEIGHT, HALF_W - 0.4D,
					220, 40, 40, 255);
		}
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();

		// bogie markers (world coords)
		this.drawBogieMarker(front.x - x, front.y - y + 0.5D, front.z - z, 0);
		this.drawBogieMarker(rear.x - x, rear.y - y + 0.5D, rear.z - z, 1);

		// coupler to next car
		this.drawCoupler(entity, x, y, z, partialTicks);

		super.doRender(entity, x, y, z, entityYaw, partialTicks);
	}

	private void drawBogieMarker(double ox, double oy, double oz, int idx) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) ox, (float) oy, (float) oz);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		int c = idx == 0 ? 255 : 40;
		this.drawCuboid(-0.9D, -0.3D, -0.9D, 0.9D, 0.3D, 0.9D, c, 200, 40, 255);
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	private void drawCoupler(EntityRailV2Car entity, double x, double y, double z, float partialTicks) {
		if (entity.trainLength <= 1 || entity.worldObj == null || entity.trainId < 0) {
			return;
		}
		EntityRailV2Car next = null;
		for (int i = 0; i < entity.worldObj.loadedEntityList.size(); i++) {
			Entity e = entity.worldObj.loadedEntityList.get(i);
			if (e instanceof EntityRailV2Car && !e.isDead) {
				EntityRailV2Car c = (EntityRailV2Car) e;
				if (c.trainId == entity.trainId && c.carIndex == entity.carIndex + 1) {
					next = c;
					break;
				}
			}
		}
		if (next == null) {
			return;
		}
		double sx = entity.posX;
		double sy = entity.posY + 1.2D;
		double sz = entity.posZ;
		double ex = next.posX;
		double ey = next.posY + 1.2D;
		double ez = next.posZ;
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		this.drawLine(sx - x, sy - y, sz - z, ex - x, ey - y, ez - z);
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(1, DefaultVertexFormats.POSITION_COLOR);
		wr.pos(x1, y1, z1).color(50, 50, 50, 255).endVertex();
		wr.pos(x2, y2, z2).color(50, 50, 50, 255).endVertex();
		tessellator.draw();
	}

	private void drawCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			int red, int green, int blue, int alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		this.addFace(wr, minX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		this.addFace(wr, maxX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(wr, minX, minY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
		this.addFace(wr, maxX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(wr, minX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		this.addFace(wr, minX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
		tessellator.draw();
	}

	private void addFace(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2,
			int red, int green, int blue, int alpha) {
		wr.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
		wr.pos(x2, y1, z1).color(red, green, blue, alpha).endVertex();
		wr.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
		wr.pos(x1, y2, z2).color(red, green, blue, alpha).endVertex();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRailV2Car entity) {
		return null;
	}
}
