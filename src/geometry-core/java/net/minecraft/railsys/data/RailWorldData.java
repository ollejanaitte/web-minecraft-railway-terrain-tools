package net.minecraft.railsys.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RailWorldData — the AUTHORITATIVE world-scoped store of confirmed production
 * rails (R12-F persistence model §3; R13 holds it in-memory, R23 adds the
 * save/reload backend).
 *
 * AUTHORITATIVE: rails (RailSegment records), the id issuer, retired id set,
 * schemaVersion.
 * CACHE: none stored here (frames/mesh/index are later-phase caches).
 * TRANSIENT: nothing here; placement preview state lives in the client
 * placement state (R10F F5).
 *
 * Future reference boundaries (R13 reserves slots, does NOT implement them):
 *   - RailNode / connections  → R16 (Rail Network)
 *   - Junction / route       → R17
 *   - InfrastructureConnector → R19
 * The class is structured so later phases can add those registries without
 * changing the rail identity model.
 */
public final class RailWorldData {

	public static final int SCHEMA_VERSION = 1;

	private final int schemaVersion;
	private final RailIdIssuer issuer = new RailIdIssuer();
	private final List<RailSegment> rails = new ArrayList<RailSegment>();
	private final Map<Long, RailSegment> byId = new LinkedHashMap<Long, RailSegment>();

	public RailWorldData() {
		this(SCHEMA_VERSION);
	}

	public RailWorldData(int schemaVersion) {
		if (schemaVersion <= 0) {
			throw new IllegalArgumentException("schemaVersion must be positive");
		}
		this.schemaVersion = schemaVersion;
	}

	public int schemaVersion() {
		return this.schemaVersion;
	}

	/** The world's stable-id issuer (only this store may issue rails). */
	public RailIdIssuer issuer() {
		return this.issuer;
	}

	/**
	 * Register a confirmed production segment. Duplicate id is rejected.
	 * Returns the registered segment.
	 */
	public RailSegment register(RailSegment seg) {
		if (seg == null) {
			throw new IllegalArgumentException("register requires a non-null RailSegment");
		}
		RailId id = seg.railId();
		if (id == null) {
			throw new IllegalArgumentException("register requires a segment with a stable id");
		}
		if (this.byId.containsKey(id.value())) {
			throw new IllegalArgumentException("duplicate RailId rejected: " + id);
		}
		if (this.issuer.isRetired(id)) {
			throw new IllegalArgumentException("retired RailId rejected: " + id);
		}
		this.byId.put(id.value(), seg);
		this.rails.add(seg);
		return seg;
	}

	/** Issue the next stable id (confirm-time boundary). */
	public RailId nextRailId() {
		return this.issuer.next();
	}

	public RailSegment get(RailId id) {
		return id == null ? null : this.byId.get(id.value());
	}

	public boolean contains(RailId id) {
		return id != null && this.byId.containsKey(id.value());
	}

	/** Delete: retire the id (not reused) and remove the segment record. */
	public RailSegment delete(RailId id) {
		RailSegment seg = id == null ? null : this.byId.remove(id.value());
		if (seg != null) {
			this.rails.remove(seg);
			seg.retire();
			this.issuer.retire(id);
		}
		return seg;
	}

	public int size() {
		return this.rails.size();
	}

	public List<RailSegment> rails() {
		return Collections.unmodifiableList(this.rails);
	}

	public boolean isEmpty() {
		return this.rails.isEmpty();
	}
}
