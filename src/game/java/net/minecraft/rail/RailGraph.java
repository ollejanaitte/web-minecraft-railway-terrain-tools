package net.minecraft.rail;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RailGraph {
	private final Map<Integer, RailNode> nodes = new HashMap<>();
	private final Map<Integer, RailSegment> segments = new HashMap<>();
	private final Map<Integer, Integer> switchTargetSegmentByNodeId = new HashMap<>();
	private final Map<Integer, Integer> trainTargetNodeByTrainId = new HashMap<>();
	private final Set<Integer> occupiedSegmentIds = new HashSet<>();
	private final Set<Integer> stationNodeIds = new HashSet<>();
	private int nextNodeId = 1;
	private int nextSegmentId = 1;

	public RailNode createNode(double x, double y, double z) {
		RailNode node = new RailNode(this.nextNodeId++, x, y, z);
		this.nodes.put(Integer.valueOf(node.getId()), node);
		return node;
	}

	public RailNode createOrReuseNode(double x, double y, double z, double reuseDistance) {
		RailNode nearestNode = this.findNearestNode(x, y, z, reuseDistance);
		return nearestNode != null ? nearestNode : this.createNode(x, y, z);
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

	public void setSwitchTargetSegment(int nodeId, int segmentId) {
		if (this.isValidSwitchTarget(nodeId, segmentId)) {
			this.switchTargetSegmentByNodeId.put(Integer.valueOf(nodeId), Integer.valueOf(segmentId));
		}
	}

	public int getSwitchTargetSegment(int nodeId) {
		Integer segmentId = this.switchTargetSegmentByNodeId.get(Integer.valueOf(nodeId));
		return segmentId != null ? segmentId.intValue() : -1;
	}

	public void clearSwitchTargetSegment(int nodeId) {
		this.switchTargetSegmentByNodeId.remove(Integer.valueOf(nodeId));
	}

	public boolean isValidSwitchTarget(int nodeId, int segmentId) {
		RailNode node = this.getNode(nodeId);
		RailSegment segment = this.getSegment(segmentId);
		return node != null && segment != null
				&& (segment.getStartNodeId() == nodeId || segment.getEndNodeId() == nodeId);
	}

	public RailNode findNearestNode(double x, double y, double z, double maxDistance) {
		RailNode nearestNode = null;
		double nearestDistanceSq = maxDistance * maxDistance;
		for (RailNode node : this.nodes.values()) {
			double dx = node.getX() - x;
			double dy = node.getY() - y;
			double dz = node.getZ() - z;
			double distanceSq = dx * dx + dy * dy + dz * dz;
			if (distanceSq <= nearestDistanceSq) {
				nearestDistanceSq = distanceSq;
				nearestNode = node;
			}
		}

		return nearestNode;
	}

	public List<RailSegment> getSegmentsConnectedToNode(int nodeId) {
		List<RailSegment> connectedSegments = new ArrayList<>();
		if (this.getNode(nodeId) == null) {
			return connectedSegments;
		}

		for (RailSegment segment : this.segments.values()) {
			if (segment.getStartNodeId() == nodeId || segment.getEndNodeId() == nodeId) {
				connectedSegments.add(segment);
			}
		}

		return connectedSegments;
	}

	public void setTrainTargetNode(int trainId, int nodeId) {
		if (trainId >= 0 && this.getNode(nodeId) != null) {
			this.trainTargetNodeByTrainId.put(Integer.valueOf(trainId), Integer.valueOf(nodeId));
		}
	}

	public int getTrainTargetNode(int trainId) {
		Integer nodeId = this.trainTargetNodeByTrainId.get(Integer.valueOf(trainId));
		return nodeId != null ? nodeId.intValue() : -1;
	}

	public void clearTrainTargetNode(int trainId) {
		this.trainTargetNodeByTrainId.remove(Integer.valueOf(trainId));
	}

	public void setSegmentOccupied(int segmentId, boolean occupied) {
		if (occupied) {
			this.occupiedSegmentIds.add(Integer.valueOf(segmentId));
		} else {
			this.occupiedSegmentIds.remove(Integer.valueOf(segmentId));
		}
	}

	public boolean isSegmentOccupied(int segmentId) {
		return this.occupiedSegmentIds.contains(Integer.valueOf(segmentId));
	}

	public void clearOccupiedSegments() {
		this.occupiedSegmentIds.clear();
	}

	public void addStationNode(int nodeId) {
		if (this.getNode(nodeId) != null) {
			this.stationNodeIds.add(Integer.valueOf(nodeId));
		}
	}

	public void removeStationNode(int nodeId) {
		this.stationNodeIds.remove(Integer.valueOf(nodeId));
	}

	public boolean isStationNode(int nodeId) {
		return this.stationNodeIds.contains(Integer.valueOf(nodeId));
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
		this.clearSwitchTargetSegment(id);
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
		this.removeSwitchTargetsForSegment(id);

		return true;
	}

	public void clear() {
		this.nodes.clear();
		this.segments.clear();
		this.switchTargetSegmentByNodeId.clear();
		this.trainTargetNodeByTrainId.clear();
		this.occupiedSegmentIds.clear();
		this.stationNodeIds.clear();
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

		NBTTagList switchList = new NBTTagList();
		for (Map.Entry<Integer, Integer> entry : this.switchTargetSegmentByNodeId.entrySet()) {
			NBTTagCompound switchTag = new NBTTagCompound();
			switchTag.setInteger("NodeId", entry.getKey().intValue());
			switchTag.setInteger("SegmentId", entry.getValue().intValue());
			switchList.appendTag(switchTag);
		}
		nbt.setTag("Switches", switchList);

		NBTTagList routeList = new NBTTagList();
		for (Map.Entry<Integer, Integer> entry : this.trainTargetNodeByTrainId.entrySet()) {
			NBTTagCompound routeTag = new NBTTagCompound();
			routeTag.setInteger("TrainId", entry.getKey().intValue());
			routeTag.setInteger("NodeId", entry.getValue().intValue());
			routeList.appendTag(routeTag);
		}
		nbt.setTag("TrainTargets", routeList);

		NBTTagList stationList = new NBTTagList();
		for (Integer nodeId : this.stationNodeIds) {
			NBTTagCompound stationTag = new NBTTagCompound();
			stationTag.setInteger("NodeId", nodeId.intValue());
			stationList.appendTag(stationTag);
		}
		nbt.setTag("Stations", stationList);
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

		NBTTagList switchList = nbt.getTagList("Switches", 10);
		for (int i = 0; i < switchList.tagCount(); ++i) {
			NBTTagCompound switchTag = switchList.getCompoundTagAt(i);
			int nodeId = switchTag.getInteger("NodeId");
			int segmentId = switchTag.getInteger("SegmentId");
			if (this.isValidSwitchTarget(nodeId, segmentId)) {
				this.switchTargetSegmentByNodeId.put(Integer.valueOf(nodeId), Integer.valueOf(segmentId));
			}
		}

		NBTTagList routeList = nbt.getTagList("TrainTargets", 10);
		for (int i = 0; i < routeList.tagCount(); ++i) {
			NBTTagCompound routeTag = routeList.getCompoundTagAt(i);
			int trainId = routeTag.getInteger("TrainId");
			int nodeId = routeTag.getInteger("NodeId");
			if (trainId >= 0 && this.getNode(nodeId) != null) {
				this.trainTargetNodeByTrainId.put(Integer.valueOf(trainId), Integer.valueOf(nodeId));
			}
		}

		NBTTagList stationList = nbt.getTagList("Stations", 10);
		for (int i = 0; i < stationList.tagCount(); ++i) {
			int nodeId = stationList.getCompoundTagAt(i).getInteger("NodeId");
			if (this.getNode(nodeId) != null) {
				this.stationNodeIds.add(Integer.valueOf(nodeId));
			}
		}
	}

	public void copyFrom(RailGraph other) {
		NBTTagCompound nbt = new NBTTagCompound();
		other.writeToNBT(nbt);
		this.readFromNBT(nbt);
	}

	private void removeSwitchTargetsForSegment(int segmentId) {
		Integer[] nodeIds = this.switchTargetSegmentByNodeId.keySet()
				.toArray(new Integer[this.switchTargetSegmentByNodeId.size()]);
		for (int i = 0; i < nodeIds.length; ++i) {
			if (this.switchTargetSegmentByNodeId.get(nodeIds[i]).intValue() == segmentId) {
				this.switchTargetSegmentByNodeId.remove(nodeIds[i]);
			}
		}
	}
}
