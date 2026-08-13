package net.minecraft.railsys.data;

import java.util.HashSet;
import java.util.Set;

/**
 * RailIdIssuer — world-scoped issuer of stable {@link RailId}.
 *
 * R12 Production Rail Data Model §3 (frozen):
 *   - ids are issued ONLY at CONFIRM (preview has no stable id);
 *   - ids are world-monotonic and never reused within a session;
 *   - a retired id is recorded so persistence/network references to it can be
 *     detected as dangling;
 *   - id source = world-monotonic counter (the world store owns the issuer).
 *
 * AUTHORITATIVE. One issuer per world (see RailWorldData).
 */
public final class RailIdIssuer {

	private long nextValue = 1L;
	private final Set<Long> retired = new HashSet<>();

	public RailIdIssuer() {
	}

	/** Issue the next stable id (at CONFIRM). Never returns a retired/used value. */
	public RailId next() {
		long v = this.nextValue++;
		return RailId.of(v);
	}

	/** Mark an id retired (delete). Retired ids are not re-issued. */
	public void retire(RailId id) {
		if (id == null) {
			throw new IllegalArgumentException("retire requires a non-null RailId");
		}
		this.retired.add(id.value());
	}

	/** True if the id has been retired (dangling reference check). */
	public boolean isRetired(RailId id) {
		return id != null && this.retired.contains(id.value());
	}

	/** Number of issued ids so far (for tests / reporting). */
	public long issuedCount() {
		return this.nextValue - 1L;
	}

	/** Number of retired ids. */
	public int retiredCount() {
		return this.retired.size();
	}
}
