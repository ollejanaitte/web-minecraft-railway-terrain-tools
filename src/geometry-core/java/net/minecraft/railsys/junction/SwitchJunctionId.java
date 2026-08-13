package net.minecraft.railsys.junction;

/**
 * SwitchJunctionId — stable identity for a production switch junction (R17).
 *
 * Issued by the owning {@link SwitchNetwork} junction issuer (per-network
 * monotonic positive id). Serialization format is REPLACEABLE (R12-F style);
 * only VALUE semantics are fixed (positive long + prefix). Retired on removal,
 * never reused.
 */
public final class SwitchJunctionId {

	public static final String PREFIX = "sw-";

	private final long value;

	private SwitchJunctionId(long value) {
		if (value <= 0L) {
			throw new IllegalArgumentException("SwitchJunctionId must be positive: " + value);
		}
		this.value = value;
	}

	static SwitchJunctionId of(long value) {
		return new SwitchJunctionId(value);
	}

	/** Probe factory for validation/tests only (never registered). */
	public static SwitchJunctionId probe(long value) {
		return new SwitchJunctionId(value <= 0L ? 1L : value);
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
		return this == o || (o instanceof SwitchJunctionId && ((SwitchJunctionId) o).value == this.value);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.value);
	}

	public static SwitchJunctionId parse(String s) {
		if (s == null || !s.startsWith(PREFIX)) {
			throw new IllegalArgumentException("malformed SwitchJunctionId: " + s);
		}
		String num = s.substring(PREFIX.length());
		if (num.isEmpty()) {
			throw new IllegalArgumentException("malformed SwitchJunctionId: " + s);
		}
		try {
			long v = Long.parseLong(num);
			return new SwitchJunctionId(v);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("malformed SwitchJunctionId: " + s, e);
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
