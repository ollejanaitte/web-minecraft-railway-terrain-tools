package net.minecraft.railsys.data;

/**
 * RailId — stable production rail identity.
 *
 * R12 Production Rail Data Model §3 (frozen):
 *   - issued at CONFIRM (never during preview);
 *   - opaque per-world unique id;
 *   - retired on delete, never reused;
 *   - serialization format is REPLACEABLE (R12-F) — this class only fixes the
 *     VALUE semantics (positive long + prefix), not any wire encoding.
 *
 * AUTHORITATIVE identity data. Never derived; never a debug counter.
 */
public final class RailId {

	public static final String PREFIX = "rail-";

	private final long value;

	private RailId(long value) {
		if (value <= 0L) {
			throw new IllegalArgumentException("RailId must be positive: " + value);
		}
		this.value = value;
	}

	/** Factory used by the issuer (kept package-private: ids come from a world issuer). */
	static RailId of(long value) {
		return new RailId(value);
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
		return this == o || (o instanceof RailId && ((RailId) o).value == this.value);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.value);
	}

	/** Parse a string form "rail-<n>". Rejects malformed / non-positive. */
	public static RailId parse(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			throw new IllegalArgumentException("malformed RailId: " + s);
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			throw new IllegalArgumentException("malformed RailId: " + s);
		}
		long v;
		try {
			v = Long.parseLong(num);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("malformed RailId: " + s, e);
		}
		return new RailId(v);
	}

	/** True if the string is a structurally valid RailId (no throw). */
	public static boolean isValid(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			return false;
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			return false;
		}
		try {
			long v = Long.parseLong(num);
			return v > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
