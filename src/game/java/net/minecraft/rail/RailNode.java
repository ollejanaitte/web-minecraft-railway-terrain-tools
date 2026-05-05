package net.minecraft.rail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RailNode {
	private final int id;
	private final double x;
	private final double y;
	private final double z;
	private final List<Integer> connectedNodeIds = new ArrayList<>();

	public RailNode(int id, double x, double y, double z) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getId() {
		return this.id;
	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public double getZ() {
		return this.z;
	}

	public void connectTo(int nodeId) {
		if (nodeId != this.id && !this.connectedNodeIds.contains(Integer.valueOf(nodeId))) {
			this.connectedNodeIds.add(Integer.valueOf(nodeId));
		}
	}

	public void disconnectFrom(int nodeId) {
		this.connectedNodeIds.remove(Integer.valueOf(nodeId));
	}

	public List<Integer> getConnectedNodeIds() {
		return Collections.unmodifiableList(this.connectedNodeIds);
	}

	public double distanceSqTo(RailNode other) {
		if (other == null) {
			return 0.0D;
		}

		double dx = other.x - this.x;
		double dy = other.y - this.y;
		double dz = other.z - this.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("Id", this.id);
		nbt.setDouble("X", this.x);
		nbt.setDouble("Y", this.y);
		nbt.setDouble("Z", this.z);
		NBTTagList connectedNodes = new NBTTagList();
		for (int i = 0; i < this.connectedNodeIds.size(); ++i) {
			NBTTagCompound connectedNode = new NBTTagCompound();
			connectedNode.setInteger("NodeId", this.connectedNodeIds.get(i).intValue());
			connectedNodes.appendTag(connectedNode);
		}
		nbt.setTag("ConnectedNodeIds", connectedNodes);
	}

	public static RailNode readFromNBT(NBTTagCompound nbt) {
		RailNode node = new RailNode(nbt.getInteger("Id"), nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
		NBTTagList connectedNodes = nbt.getTagList("ConnectedNodeIds", 10);
		for (int i = 0; i < connectedNodes.tagCount(); ++i) {
			int nodeId = connectedNodes.getCompoundTagAt(i).getInteger("NodeId");
			node.connectTo(nodeId);
		}
		return node;
	}
}
