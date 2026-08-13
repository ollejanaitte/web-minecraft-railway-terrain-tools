package net.minecraft.railsys.modelpack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * SafeZipReader — the R15 Safe Import Boundary for ModelPack ZIP input.
 *
 * Reads a ZIP byte[] defensively:
 *   - rejects path traversal ("../", "..\\", leading "/", backslash absolute)
 *   - rejects absolute paths and invalid normalization
 *   - enforces entry count and total uncompressed size caps (zip bomb guard)
 *   - enforces per-entry size cap
 *   - rejects duplicate entry names
 *   - rejects directories explicitly (ignored, not counted as entries)
 *   - never executes anything; only returns bytes (parse happens later)
 *
 * Pure data + java.util.zip (TeaVM-safe). Never throws for malformed input:
 * malformed entries are diagnosed via {@link ImportDiagnostic.Collector} and
 * skipped, so a broken ModelPack cannot crash the game.
 */
public final class SafeZipReader {

	/** Maximum number of accepted entries in a ModelPack. */
	public static final int MAX_ENTRIES = 4096;
	/** Maximum total uncompressed size accepted (128 MiB). */
	public static final long MAX_TOTAL_UNCOMPRESSED = 128L * 1024L * 1024L;
	/** Maximum per-entry uncompressed size (32 MiB). */
	public static final long MAX_ENTRY_UNCOMPRESSED = 32L * 1024L * 1024L;

	private SafeZipReader() {
	}

	/** One safe entry: normalized name + uncompressed bytes. */
	public static final class Entry {
		public final String name;
		public final byte[] data;

		Entry(String name, byte[] data) {
			this.name = name;
			this.data = data;
		}
	}

	/** Result of a safe read: accepted entries + diagnostics (never throws). */
	public static final class Result {
		public final List<Entry> entries;
		public final List<ImportDiagnostic> diagnostics;
		public final boolean rejected;
		public final long totalBytes;

		Result(List<Entry> entries, List<ImportDiagnostic> diags, boolean rejected, long total) {
			this.entries = entries;
			this.diagnostics = diags;
			this.rejected = rejected;
			this.totalBytes = total;
		}
	}

	/**
	 * Read a ZIP byte[] safely. Returns entries + diagnostics; sets rejected
	 * when a fatal (whole-pack) problem is found (e.g. malformed ZIP).
	 */
	public static Result read(byte[] zipBytes, ImportDiagnostic.Collector diag) {
		List<Entry> entries = new ArrayList<Entry>();
		List<String> seen = new ArrayList<String>();
		long total = 0L;
		boolean rejected = false;
		if (zipBytes == null || zipBytes.length == 0) {
			diag.reject("zip", "EMPTY_INPUT", "", "ModelPack bytes are empty");
			return new Result(entries, diag.snapshot(), true, 0L);
		}
		// Reject clearly non-ZIP input early (no PK signature).
		boolean sigOk = zipBytes.length >= 4 && zipBytes[0] == 'P' && zipBytes[1] == 'K';
		if (!sigOk) {
			diag.reject("zip", "MALFORMED_ZIP", "", "input is not a ZIP (bad signature)");
			return new Result(entries, diag.snapshot(), true, 0L);
		}
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
			int count = 0;
			ZipEntry e;
			while ((e = zis.getNextEntry()) != null) {
				count++;
				if (count > MAX_ENTRIES) {
					diag.reject("zip", "TOO_MANY_ENTRIES", e.getName(),
							"entry count exceeds " + MAX_ENTRIES);
					rejected = true;
					break;
				}
				if (e.isDirectory()) {
					continue;
				}
				String name = e.getName();
				String norm = normalize(name, diag);
				if (norm == null) {
					continue; // already diagnosed
				}
				if (seen.contains(norm)) {
					diag.reject("zip", "DUPLICATE_ENTRY", name, "duplicate entry name rejected");
					continue;
				}
				long size = e.getSize();
				// getSize() may be -1 when the size is only in the central
				// directory; the read-buffer cap enforces the real limit.
				if (size > MAX_ENTRY_UNCOMPRESSED) {
					diag.reject("zip", "ENTRY_TOO_LARGE", name,
							"entry uncompressed size " + size + " exceeds cap " + MAX_ENTRY_UNCOMPRESSED);
					continue;
				}
				if (size >= 0 && total + size > MAX_TOTAL_UNCOMPRESSED) {
					diag.reject("zip", "ZIP_BOMB", name,
							"total uncompressed size would exceed cap " + MAX_TOTAL_UNCOMPRESSED);
					rejected = true;
					break;
				}
				byte[] data = readEntry(zis, (int) Math.min(size, MAX_ENTRY_UNCOMPRESSED + 1L));
				if (data == null) {
					diag.reject("zip", "BAD_ENTRY_DATA", name, "could not inflate entry (malformed)");
					continue;
				}
				total += data.length;
				seen.add(norm);
				entries.add(new Entry(norm, data));
			}
		} catch (IOException e) {
			diag.reject("zip", "MALFORMED_ZIP", "", "ZIP stream error: " + e.getClass().getSimpleName());
			rejected = true;
		} finally {
			if (zis != null) {
				try {
					zis.close();
				} catch (IOException ignore) {
				}
			}
		}
		return new Result(entries, diag.snapshot(), rejected, total);
	}

	private static byte[] readEntry(InputStream in, int max) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
		byte[] buf = new byte[8192];
		int n;
		long total = 0L;
		while ((n = in.read(buf)) > 0) {
			total += n;
			if (total > MAX_ENTRY_UNCOMPRESSED) {
				return null;
			}
			bos.write(buf, 0, n);
		}
		return bos.toByteArray();
	}

	/**
	 * Normalize + validate an entry path. Returns a safe normalized name or
	 * null (after diagnosing a REJECT) when unsafe.
	 */
	public static String normalize(String raw, ImportDiagnostic.Collector diag) {
		if (raw == null || raw.isEmpty()) {
			diag.reject("zip", "EMPTY_NAME", "", "entry with empty name rejected");
			return null;
		}
		String s = raw.replace('\\', '/');
		if (s.startsWith("/") || s.startsWith("//") || s.length() > 2 && (s.charAt(1) == ':')) {
			diag.reject("zip", "ABSOLUTE_PATH", raw, "absolute path rejected");
			return null;
		}
		if (s.contains("..")) {
			for (int i = 0; i + 2 <= s.length(); i++) {
				if (s.startsWith("../", i) || (i + 2 == s.length() && s.substring(i).equals(".."))) {
					diag.reject("zip", "PATH_TRAVERSAL", raw, "path traversal '..' rejected");
					return null;
				}
			}
		}
		// collapse duplicate slashes and drop "./"
		StringBuilder sb = new StringBuilder();
		boolean prevSlash = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/') {
				if (prevSlash) {
					continue;
				}
				prevSlash = true;
			} else {
				prevSlash = false;
			}
			if (i == 0 && c == '/' ) {
				continue;
			}
			sb.append(c);
		}
		String out = sb.toString();
		// final component must not be "." or ".."
		if (out.equals(".") || out.equals("..") || out.endsWith("/..") || out.endsWith("/.")) {
			diag.reject("zip", "INVALID_PATH", raw, "invalid path component rejected");
			return null;
		}
		return out;
	}
}
