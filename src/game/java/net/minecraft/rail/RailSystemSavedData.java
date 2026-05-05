package net.minecraft.rail;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class RailSystemSavedData extends WorldSavedData {
	public static final String DATA_NAME = "rail_system";
	private final RailGraph graph = new RailGraph();

	public RailSystemSavedData(String name) {
		super(name);
	}

	public RailGraph getGraph() {
		return this.graph;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		this.graph.readFromNBT(nbt);
	}

	public void writeToNBT(NBTTagCompound nbt) {
		this.graph.writeToNBT(nbt);
	}
}
