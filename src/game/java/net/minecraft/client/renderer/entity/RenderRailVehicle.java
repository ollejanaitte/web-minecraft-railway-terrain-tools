package net.minecraft.client.renderer.entity;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.entity.item.EntityRailVehicle;
import net.minecraft.util.ResourceLocation;

public class RenderRailVehicle extends Render<EntityRailVehicle> {
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
		this.drawRailVehicleModel();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
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

	private void drawRailVehicleModel() {
		this.drawCuboid(-1.45D, 0.20D, -0.55D, 1.45D, 1.00D, 0.55D, 180, 35, 210, 255);
		this.drawCuboid(-1.15D, 0.95D, -0.42D, 1.15D, 1.25D, 0.42D, 120, 25, 160, 255);
		this.drawCuboid(1.42D, 0.28D, -0.50D, 1.52D, 0.95D, 0.50D, 235, 60, 70, 255);
		this.drawCuboid(1.53D, 0.55D, -0.32D, 1.58D, 0.88D, 0.32D, 80, 210, 255, 255);
		this.drawCuboid(-1.46D, 0.42D, -0.56D, 1.10D, 0.78D, -0.60D, 70, 190, 230, 255);
		this.drawCuboid(-1.46D, 0.42D, 0.56D, 1.10D, 0.78D, 0.60D, 70, 190, 230, 255);
		this.drawCuboid(-0.95D, -0.05D, -0.67D, -0.45D, 0.22D, -0.48D, 20, 20, 20, 255);
		this.drawCuboid(0.45D, -0.05D, -0.67D, 0.95D, 0.22D, -0.48D, 20, 20, 20, 255);
		this.drawCuboid(-0.95D, -0.05D, 0.48D, -0.45D, 0.22D, 0.67D, 20, 20, 20, 255);
		this.drawCuboid(0.45D, -0.05D, 0.48D, 0.95D, 0.22D, 0.67D, 20, 20, 20, 255);
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

	protected ResourceLocation getEntityTexture(EntityRailVehicle entity) {
		return null;
	}
}
