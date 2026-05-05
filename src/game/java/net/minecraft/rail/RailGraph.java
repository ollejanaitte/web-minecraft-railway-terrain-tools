package net.minecraft.rail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RailGraph {
	private final Map<Integer, RailNode> nodes = new HashMap<>();
	private final Map<Integer, RailSegment> segments = new HashMap<>();
	private int nextNodeId = 1;
	private int nextSegmentId = 1;

	public RailNode createNode(double x, double y, double z) {
		RailNode node = new RailNode(this.nextNodeId++, x, y, z);
		this.nodes.put(Integer.valueOf(node.getId()), node);
		return node;
	}

	public RailSegment createSegment(int startNodeId, int endNodeId, RailSegmentType type) {
		if (startNodeId == endNodeId) {
			return null;
		}

		RailNode startNode = this.getNode(startNodeId);
		RailNode endNode = this.getNode(endNodeId);
		if (startNode == null || endNode == null) {
			return null;
		}

		RailSegmentType segmentType = type == null ? RailSegmentType.STRAIGHT : type;
		double length = Math.sqrt(startNode.distanceSqTo(endNode));
		RailSegment segment = new RailSegment(this.nextSegmentId++, startNodeId, endNodeId, length, segmentType);
		this.segments.put(Integer.valueOf(segment.getId()), segment);
		startNode.connectTo(endNodeId);
		endNode.connectTo(startNodeId);
		return segment;
	}

	public RailSegment createCurveSegment(int startNodeId, int endNodeId, RailCurveData curveData) {
		if (curveData == null) {
			return null;
		}

		RailSegment segment = this.createSegment(startNodeId, endNodeId, RailSegmentType.CURVE);
		if (segment == null) {
			return null;
		}

		segment.setCurveData(curveData);
		segment.recalculateLength(this.getNode(startNodeId), this.getNode(endNodeId));
		return segment;
	}

	public RailNode getNode(int id) {
		return this.nodes.get(Integer.valueOf(id));
	}

	public RailSegment getSegment(int id) {
		return this.segments.get(Integer.valueOf(id));
	}

	public Collection<RailNode> getNodes() {
		return Collections.unmodifiableCollection(this.nodes.values());
	}

	public Collection<RailSegment> getSegments() {
		return Collections.unmodifiableCollection(this.segments.values());
	}

	public boolean removeNode(int id) {
		RailNode node = this.nodes.get(Integer.valueOf(id));
		if (node == null) {
			return false;
		}

		Integer[] segmentIds = this.segments.keySet().toArray(new Integer[this.segments.size()]);
		for (int i = 0; i < segmentIds.length; ++i) {
			RailSegment segment = this.getSegment(segmentIds[i].intValue());
			if (segment != null && (segment.getStartNodeId() == id || segment.getEndNodeId() == id)) {
				this.removeSegment(segment.getId());
			}
		}

		this.nodes.remove(Integer.valueOf(id));
		return true;
	}

	public boolean removeSegment(int id) {
		RailSegment segment = this.segments.remove(Integer.valueOf(id));
		if (segment == null) {
			return false;
		}

		RailNode startNode = this.getNode(segment.getStartNodeId());
		RailNode endNode = this.getNode(segment.getEndNodeId());
		if (startNode != null) {
			startNode.disconnectFrom(segment.getEndNodeId());
		}
		if (endNode != null) {
			endNode.disconnectFrom(segment.getStartNodeId());
		}

		return true;
	}

	public void clear() {
		this.nodes.clear();
		this.segments.clear();
		this.nextNodeId = 1;
		this.nextSegmentId = 1;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("NextNodeId", this.nextNodeId);
		nbt.setInteger("NextSegmentId", this.nextSegmentId);
		NBTTagList nodeList = new NBTTagList();
		for (RailNode node : this.nodes.values()) {
			NBTTagCompound nodeTag = new NBTTagCompound();
			node.writeToNBT(nodeTag);
			nodeList.appendTag(nodeTag);
		}
		nbt.setTag("Nodes", nodeList);

		NBTTagList segmentList = new NBTTagList();
		for (RailSegment segment : this.segments.values()) {
			NBTTagCompound segmentTag = new NBTTagCompound();
			segment.writeToNBT(segmentTag);
			segmentList.appendTag(segmentTag);
		}
		nbt.setTag("Segments", segmentList);
	}

	public void readFromNBT(NBTTagCompound nbt) {
		this.clear();
		int maxNodeId = 0;
		NBTTagList nodeList = nbt.getTagList("Nodes", 10);
		for (int i = 0; i < nodeList.tagCount(); ++i) {
			RailNode node = RailNode.readFromNBT(nodeList.getCompoundTagAt(i));
			this.nodes.put(Integer.valueOf(node.getId()), node);
			if (node.getId() > maxNodeId) {
				maxNodeId = node.getId();
			}
		}

		int maxSegmentId = 0;
		NBTTagList segmentList = nbt.getTagList("Segments", 10);
		for (int i = 0; i < segmentList.tagCount(); ++i) {
			RailSegment segment = RailSegment.readFromNBT(segmentList.getCompoundTagAt(i));
			if (this.nodes.containsKey(Integer.valueOf(segment.getStartNodeId()))
					&& this.nodes.containsKey(Integer.valueOf(segment.getEndNodeId()))) {
				this.segments.put(Integer.valueOf(segment.getId()), segment);
				if (segment.getId() > maxSegmentId) {
					maxSegmentId = segment.getId();
				}
			}
		}

		this.nextNodeId = Math.max(nbt.getInteger("NextNodeId"), maxNodeId + 1);
		this.nextSegmentId = Math.max(nbt.getInteger("NextSegmentId"), maxSegmentId + 1);
	}

	public void copyFrom(RailGraph other) {
		NBTTagCompound nbt = new NBTTagCompound();
		other.writeToNBT(nbt);
		this.readFromNBT(nbt);
	}
}
