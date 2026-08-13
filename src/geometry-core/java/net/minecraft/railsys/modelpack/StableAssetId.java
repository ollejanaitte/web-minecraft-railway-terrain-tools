package net.minecraft.railsys.modelpack;

import java.security.MessageDigest;

/**
 * StableAssetId — deterministic, collision-resistant asset ID generation
 * (R15-09).
 *
 * Format: "<packId>:<railId>" where both parts are sanitized (lowercased,
 * non-alphanumeric -> '_', trimmed). A stable hash suffix is appended when the
 * source includes version/encoding so identical names from different packs stay
 * unique while the ID remains deterministic for the same input.
 */
public final class StableAssetId {

	private StableAssetId() {
	}

	/** Sanitize an id segment: lowercase, [a-z0-9._-] kept, others -> '_'. */
	public static String sanitize(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
				sb.append(c);
			} else if (c >= 'A' && c <= 'Z') {
				sb.append((char) (c + 32));
			} else if (c == ' ') {
				sb.append('_');
			} else {
				sb.append('_');
			}
		}
		String out = sb.toString();
		while (out.contains("__")) {
			out = out.replace("__", "_");
		}
		return out;
	}

	/** 8-hex deterministic digest of a string (stable across runs). */
	public static String shortHash(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(s.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				sb.append(String.format("%02x", d[i] & 0xFF));
			}
			return sb.toString();
		} catch (Exception e) {
			int h = s.hashCode();
			return Integer.toHexString(h);
		}
	}

	/**
	 * Build a stable asset id: packId:railId, both sanitized. When
	 * uniqueVersion is non-empty a '-h<8>' suffix is appended (version-aware).
	 */
	public static String assetId(String packId, String railId, String uniqueVersion) {
		String base = sanitize(packId) + ":" + sanitize(railId);
		if (uniqueVersion != null && !uniqueVersion.isEmpty()) {
			base = base + "-h" + shortHash(sanitize(packId) + "|" + sanitize(railId) + "|" + uniqueVersion);
		}
		return base;
	}
}
