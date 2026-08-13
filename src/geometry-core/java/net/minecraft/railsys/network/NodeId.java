package net.minecraft.railsys.network;

/**
 * NodeId — stable Production RailNode identity (R16-07).
 *
 * Issued by a {@link ProductionRailNetwork} node issuer (per-network
 * monotonically increasing positive id). Serialization format is
 * REPLACEABLE (R12-F style); this fixes only the VALUE semantics
 * (positive long + prefix). Retired on removal, never reused.
 */
public final class NodeId {

	public static final String PREFIX = "node-";

	private final long value;

	private NodeId(long value) {
		if (value <= 0L) {
			throw new IllegalArgumentException("NodeId must be positive: " + value);
		}
		this.value = value;
	}

	/** Factory used by the network issuer. */
	static NodeId of(long value) {
		return new NodeId(value);
	}

	/** Probe factory for validation/tests only (never registered). */
	public static NodeId probe(long value) {
		return new NodeId(value <= 0L ? 1L : value);
	}

	public long value() {
		return this.value;
	}

	@Override
	public String toString() {
		return PREFIX + this.value;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof NodeId && ((NodeId) o).value == this.value);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.value);
	}

	/** Parse "node-<n>". Rejects malformed/non-positive. */
	public static NodeId parse(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			throw new IllegalArgumentException("malformed NodeId: " + s);
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			throw new IllegalArgumentException("malformed NodeId: " + s);
		}
		try {
			long v = Long.parseLong(num);
			return new NodeId(v);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("malformed NodeId: " + s, e);
		}
	}

	public static boolean isValid(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			return false;
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			return false;
		}
		try {
			return Long.parseLong(num) > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
