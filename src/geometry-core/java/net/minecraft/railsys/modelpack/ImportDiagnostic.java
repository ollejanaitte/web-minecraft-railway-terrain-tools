package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.List;

/**
 * ImportDiagnostic — a single structured diagnostic from a ModelPack import.
 *
 * Clean-room Railsys type. Each diagnostic records the entry/phase involved,
 * a stable code, a human-readable message and the severity. Diagnostics are
 * how the Safe Import Boundary reports REJECT reasons in a diagnosable way.
 */
public final class ImportDiagnostic {

	public enum Severity {
		INFO, WARN, REJECT, SKIP
	}

	public final Severity severity;
	public final String phase;   // "zip", "json", "mqo", "texture", "registry"
	public final String code;    // stable machine code e.g. "PATH_TRAVERSAL"
	public final String entry;   // zip entry / asset id involved (may be "")
	public final String message;

	public ImportDiagnostic(Severity severity, String phase, String code, String entry, String message) {
		this.severity = severity;
		this.phase = phase;
		this.code = code;
		this.entry = entry == null ? "" : entry;
		this.message = message == null ? "" : message;
	}

	public boolean isReject() {
		return severity == Severity.REJECT;
	}

	@Override
	public String toString() {
		return "[" + severity + "][" + phase + "][" + code + "] " + entry + ": " + message;
	}

	/** Collects diagnostics as an immutable snapshot. */
	public static final class Collector {
		private final List<ImportDiagnostic> list = new ArrayList<ImportDiagnostic>();

		public void info(String phase, String code, String entry, String msg) {
			list.add(new ImportDiagnostic(Severity.INFO, phase, code, entry, msg));
		}

		public void warn(String phase, String code, String entry, String msg) {
			list.add(new ImportDiagnostic(Severity.WARN, phase, code, entry, msg));
		}

		public void reject(String phase, String code, String entry, String msg) {
			list.add(new ImportDiagnostic(Severity.REJECT, phase, code, entry, msg));
		}

		public void skip(String phase, String code, String entry, String msg) {
			list.add(new ImportDiagnostic(Severity.SKIP, phase, code, entry, msg));
		}

		public List<ImportDiagnostic> snapshot() {
			return new ArrayList<ImportDiagnostic>(list);
		}

		public boolean hasReject() {
			for (ImportDiagnostic d : list) {
				if (d.isReject()) {
					return true;
				}
			}
			return false;
		}
	}
}
