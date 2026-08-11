package net.minecraft.railsys.persist;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.placement.RailsysPlacementState;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.World;

/**
 * RailsysWorldRailData — WorldRailData schema v2 (Railsys Production).
 *
 * Persists the confirmed rail (placement anchors + asset id) in the world
 * save so it is restored after save/reload. Uses the SAME anchor -> geometry
 * -> RailPath pipeline as placement (no separate geometry serialization).
 *
 * NBT layout:
 *   version: int = 2
 *   hasRail: byte
 *   assetId: string
 *   anchorA_x/y/z, anchorA_yaw, anchorA_pitch, anchorA_handle
 *   anchorB_x/y/z, anchorB_yaw, anchorB_pitch, anchorB_handle
 */
public class RailsysWorldRailData extends WorldSavedData {
	private static final Logger logger = LogManager.getLogger();
	public static final String DATA_NAME = "railsys_world_rail_v2";
	public static final int SCHEMA_VERSION = 2;

	private boolean hasRail = false;
	private String assetId = "railsys.straight_1435_wood";
	private double ax, ay, az, ayaw, apitch, ahandle;
	private double bx, by, bz, byaw, bpitch, bhandle;

	public RailsysWorldRailData(String name) {
		super(name);
	}

	public static RailsysWorldRailData get(World world) {
		if (world == null || world.isRemote) {
			return null;
		}
		net.minecraft.world.storage.MapStorage storage = world.getMapStorage();
		if (storage == null) {
			return null;
		}
		if (!net.minecraft.world.storage.MapStorage.storageProviders.containsKey(RailsysWorldRailData.class)) {
			net.minecraft.world.storage.MapStorage.storageProviders.put(RailsysWorldRailData.class,
					RailsysWorldRailData::new);
		}
		RailsysWorldRailData data = (RailsysWorldRailData) storage.loadData(RailsysWorldRailData.class, DATA_NAME);
		if (data == null) {
			data = new RailsysWorldRailData(DATA_NAME);
			storage.setData(DATA_NAME, data);
			data.markDirty();
		}
		return data;
	}

	/** Snapshot current confirmed rail into this saved data. */
	public void captureFromState(RailsysPlacementState st) {
		if (st.hasConfirmed() && st.getConfirmedAnchorA() != null && st.getConfirmedAnchorB() != null) {
			AnchorDefinition a = st.getConfirmedAnchorA();
			AnchorDefinition b = st.getConfirmedAnchorB();
			this.hasRail = true;
			this.assetId = st.getConfirmedAssetId();
			this.ax = a.x; this.ay = a.y; this.az = a.z;
			this.ayaw = a.yawDeg; this.apitch = a.pitchDeg; this.ahandle = a.lengthH_m;
			this.bx = b.x; this.by = b.y; this.bz = b.z;
			this.byaw = b.yawDeg; this.bpitch = b.pitchDeg; this.bhandle = b.lengthH_m;
			this.markDirty();
		}
	}

	/** Restore the saved rail into the placement/render state. */
	public void restoreInto(World world) {
		if (!this.hasRail) {
			return;
		}
		try {
			AnchorDefinition a = new AnchorDefinition(ax, ay, az, ayaw, apitch, ahandle > 0.0D ? ahandle : 1.0D, 0.0D);
			AnchorDefinition b = new AnchorDefinition(bx, by, bz, byaw, bpitch, bhandle > 0.0D ? bhandle : 1.0D, 0.0D);
			RailPath path = this.buildPath(a, b);
			if (path == null) {
				logger.warn("[RAILSYS] restore: cannot rebuild path from saved anchors");
				return;
			}
			RailsysPlacementState.getInstance().restore(path, a, b, this.assetId);
			RailsysRenderManager.setRenderPath(path);
			RailsysRenderManager.setActiveAsset(this.assetId);
			logger.info("[RAILSYS] restored rail length=" + path.totalLength() + " asset=" + this.assetId);
		} catch (RuntimeException e) {
			logger.error("[RAILSYS] restore failed (corrupt?): " + e.getMessage());
		}
	}

	private RailPath buildPath(AnchorDefinition a, AnchorDefinition b) {
		try {
			boolean curve = Math.abs(a.yawDeg - b.yawDeg) > 1.0D || Math.abs(a.pitchDeg - b.pitchDeg) > 1.0D;
			if (curve) {
				HorizontalBezierGeometry g = HorizontalBezierGeometry.fromAnchors(a, b, 700);
				return RailPath.of(new RailPiece(g));
			}
			StraightGeometry g = new StraightGeometry(a.x, a.y, a.z, b.x, b.y, b.z, 701);
			return RailPath.of(new RailPiece(g));
		} catch (RuntimeException e) {
			logger.warn("[RAILSYS] buildPath failed: " + e.getMessage());
			return null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.hasRail = nbt.getInteger("version") == SCHEMA_VERSION && nbt.getInteger("hasRail") == 1;
		if (!this.hasRail) {
			return;
		}
		this.assetId = nbt.getString("assetId");
		this.ax = nbt.getDouble("anchorA_x"); this.ay = nbt.getDouble("anchorA_y"); this.az = nbt.getDouble("anchorA_z");
		this.ayaw = nbt.getDouble("anchorA_yaw"); this.apitch = nbt.getDouble("anchorA_pitch");
		this.ahandle = nbt.getDouble("anchorA_handle");
		this.bx = nbt.getDouble("anchorB_x"); this.by = nbt.getDouble("anchorB_y"); this.bz = nbt.getDouble("anchorB_z");
		this.byaw = nbt.getDouble("anchorB_yaw"); this.bpitch = nbt.getDouble("anchorB_pitch");
		this.bhandle = nbt.getDouble("anchorB_handle");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("version", SCHEMA_VERSION);
		nbt.setInteger("hasRail", this.hasRail ? 1 : 0);
		if (!this.hasRail) {
			return;
		}
		nbt.setString("assetId", this.assetId);
		nbt.setDouble("anchorA_x", this.ax); nbt.setDouble("anchorA_y", this.ay); nbt.setDouble("anchorA_z", this.az);
		nbt.setDouble("anchorA_yaw", this.ayaw); nbt.setDouble("anchorA_pitch", this.apitch);
		nbt.setDouble("anchorA_handle", this.ahandle);
		nbt.setDouble("anchorB_x", this.bx); nbt.setDouble("anchorB_y", this.by); nbt.setDouble("anchorB_z", this.bz);
		nbt.setDouble("anchorB_yaw", this.byaw); nbt.setDouble("anchorB_pitch", this.bpitch);
		nbt.setDouble("anchorB_handle", this.bhandle);
	}
}
