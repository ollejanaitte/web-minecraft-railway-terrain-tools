package net.minecraft.rail;

import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;

public class RailSystemManager {
	private static final RailGraph GLOBAL_GRAPH = new RailGraph();
	private static RailSystemSavedData lastMirroredData;

	private RailSystemManager() {
	}

	public static RailGraph getGlobalGraph() {
		return GLOBAL_GRAPH;
	}

	public static void clearGlobalGraph() {
		GLOBAL_GRAPH.clear();
		lastMirroredData = null;
	}

	public static RailGraph getGraphForWorld(World world) {
		RailSystemSavedData data = getSavedData(world);
		if (data != null) {
			if (data != lastMirroredData) {
				GLOBAL_GRAPH.copyFrom(data.getGraph());
				lastMirroredData = data;
			}
			return data.getGraph();
		}

		return GLOBAL_GRAPH;
	}

	public static void markDirty(World world) {
		RailSystemSavedData data = getSavedData(world);
		if (data != null) {
			GLOBAL_GRAPH.copyFrom(data.getGraph());
			data.markDirty();
		}
	}

	private static RailSystemSavedData getSavedData(World world) {
		if (world == null || world.isRemote) {
			return null;
		}

		MapStorage storage = world.getMapStorage();
		if (storage == null) {
			return null;
		}

		if (!MapStorage.storageProviders.containsKey(RailSystemSavedData.class)) {
			MapStorage.storageProviders.put(RailSystemSavedData.class, RailSystemSavedData::new);
		}

		RailSystemSavedData data = (RailSystemSavedData) storage.loadData(RailSystemSavedData.class,
				RailSystemSavedData.DATA_NAME);
		if (data == null) {
			data = new RailSystemSavedData(RailSystemSavedData.DATA_NAME);
			storage.setData(RailSystemSavedData.DATA_NAME, data);
			data.markDirty();
		}

		return data;
	}

	/*
	 * Temporary Phase 5-C usage example:
	 *
	 * RailGraph graph = RailSystemManager.getGlobalGraph();
	 * RailNode a = graph.createNode(0.0D, 64.0D, 0.0D);
	 * RailNode b = graph.createNode(10.0D, 64.0D, 0.0D);
	 * RailSegment s = graph.createSegment(a.getId(), b.getId(), RailSegmentType.STRAIGHT);
	 *
	 * Phase 5-D+: move this global graph to WorldSavedData/NBT-backed world storage.
	 */
}
