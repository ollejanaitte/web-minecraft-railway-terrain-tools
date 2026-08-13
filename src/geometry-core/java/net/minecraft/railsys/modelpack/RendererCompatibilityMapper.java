package net.minecraft.railsys.modelpack;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.railsys.modelpack.RailsysInternalAsset.RendererBehaviour;

/**
 * RendererCompatibilityMapper — maps an RTM renderer script reference to a
 * Railsys-native {@link RendererBehaviour} (R15-08).
 *
 * The RTM renderer JavaScript is NEVER executed. The script path is treated as
 * a spec fact identifying a known renderer pattern; the mapping below is the
 * Railsys allowlist. Unknown patterns map to FALLBACK_STATIC (base + 2 rails)
 * so appearance is never silently broken.
 */
public final class RendererCompatibilityMapper {

	private static final Map<String, RendererBehaviour> PATTERNS = new HashMap<String, RendererBehaviour>();

	static {
		// Static parts renderers (base + railL/railR + optional sideL/sideR).
		PATTERNS.put("scripts/renderrailnb.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb2.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_1067mm.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_750.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_bu.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_dss.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_tram.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailnb_y-schwelle.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailrille.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailrille_1067mm.js", RendererBehaviour.STATIC_PARTS);
		PATTERNS.put("scripts/renderrailbvg.js", RendererBehaviour.STATIC_PARTS);
		// Renderers with switch/tongue parts (Zunge*) — recognized, R17/R18.
		PATTERNS.put("scripts/renderrail_nb_sb.js", RendererBehaviour.STATIC_SWITCH_META);
	}

	private RendererCompatibilityMapper() {
	}

	/** Map a renderer script path (lowercased, slashes normalized) to a behaviour. */
	public static RendererBehaviour map(String rendererPath) {
		if (rendererPath == null || rendererPath.isEmpty()) {
			return RendererBehaviour.STATIC_PARTS;
		}
		String norm = rendererPath.replace('\\', '/').toLowerCase();
		RendererBehaviour b = PATTERNS.get(norm);
		if (b == null) {
			// Fall back on basename match for robustness.
			int slash = norm.lastIndexOf('/');
			String base = slash >= 0 ? norm.substring(slash + 1) : norm;
			for (Map.Entry<String, RendererBehaviour> e : PATTERNS.entrySet()) {
				String k = e.getKey();
				String kb = k.substring(k.lastIndexOf('/') + 1);
				if (kb.equals(base)) {
					return e.getValue();
				}
			}
			return RendererBehaviour.FALLBACK_STATIC;
		}
		return b;
	}
}
