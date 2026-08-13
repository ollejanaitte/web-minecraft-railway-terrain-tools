package net.minecraft.railsys.network;

/**
 * ConnectionId — stable Production RailConnection identity (R16-08).
 *
 * Issued by a {@link ProductionRailNetwork} connection issuer (per-network
 * monotonically increasing positive id). Serialization format is REPLACEABLE
 * (R12-F style); this fixes only the VALUE semantics (positive long + prefix).
 * Retired on removal, never reused.
 */
public final class ConnectionId {

	public static final String PREFIX = "conn-";

	private final long value;

	private ConnectionId(long value) {
		if (value <= 0L) {
			throw new IllegalArgumentException("ConnectionId must be positive: " + value);
		}
		this.value = value;
	}

	/** Factory used by the network issuer. */
	static ConnectionId of(long value) {
		return new ConnectionId(value);
	}

	/** Probe factory for validation/tests only (never registered). */
	public static ConnectionId probe(long value) {
		return new ConnectionId(value <= 0L ? 1L : value);
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
		return this == o || (o instanceof ConnectionId && ((ConnectionId) o).value == this.value);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.value);
	}

	public static ConnectionId parse(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			throw new IllegalArgumentException("malformed ConnectionId: " + s);
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			throw new IllegalArgumentException("malformed ConnectionId: " + s);
		}
		try {
			long v = Long.parseLong(num);
			return new ConnectionId(v);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("malformed ConnectionId: " + s, e);
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
