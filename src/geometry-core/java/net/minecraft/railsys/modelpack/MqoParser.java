package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MqoParser — parses the MQO text subset needed by R15 (clean-room, original
 * implementation; MQO format facts are treated as specification).
 *
 * Supports:
 *   - Metasequoia Document / Format Text Ver 1.1 header
 *   - Material N { "name" shader(...) col(...) ... tex("rel\path.png") }
 *   - Object "name" { ... vertex N { x y z ... } face M { ... } }
 *   - faces: "<n> V(i j k [l]) M(mat) UV(u v ...)" (3 or 4 vertices)
 *   - object transform: scale/rotation/translation
 *
 * Deliberately skips: Thumbnail { binary }, Scene { }, lighting blocks.
 * Unknown/unsupported syntax produces a SKIP/WARN diagnostic, never a crash.
 * Vertex/face counts are capped (defensive) — extreme meshes are rejected.
 */
public final class MqoParser {

	public static final int MAX_VERTICES = 65536;
	public static final int MAX_FACES = 131072;

	private MqoParser() {
	}

	/** A single MQO face: vertex indices + optional per-vertex UV. */
	public static final class Face {
		public final int[] verts;
		public final int material;
		public final float[] uv; // flat [u0,v0,u1,v1,...] or empty

		Face(int[] verts, int material, float[] uv) {
			this.verts = verts;
			this.material = material;
			this.uv = uv;
		}
	}

	/** A material: name + texture path (may be empty). */
	public static final class Material {
		public final String name;
		public final String texture;

		Material(String name, String texture) {
			this.name = name;
			this.texture = texture;
		}
	}

	/** An object: name + vertices + faces + transform. */
	public static final class Object3D {
		public final String name;
		public final float[][] vertices; // [n][3]
		public final List<Face> faces;
		public final double scale;
		public final double rotX, rotY, rotZ;
		public final double tx, ty, tz;

		Object3D(String name, float[][] v, List<Face> f, double scale,
				double rx, double ry, double rz, double tx, double ty, double tz) {
			this.name = name;
			this.vertices = v;
			this.faces = f;
			this.scale = scale;
			this.rotX = rx; this.rotY = ry; this.rotZ = rz;
			this.tx = tx; this.ty = ty; this.tz = tz;
		}
	}

	/** Parsed MQO model. */
	public static final class Model {
		public final List<Material> materials;
		public final List<Object3D> objects;
		public final List<ImportDiagnostic> diagnostics;

		Model(List<Material> m, List<Object3D> o, List<ImportDiagnostic> d) {
			this.materials = m;
			this.objects = o;
			this.diagnostics = d;
		}
	}

	public static Model parse(String text, ImportDiagnostic.Collector diag) {
		List<Material> materials = new ArrayList<Material>();
		List<Object3D> objects = new ArrayList<Object3D>();
		if (text == null) {
			diag.reject("mqo", "EMPTY_MQO", "", "MQO text is null");
			return new Model(materials, objects, diag.snapshot());
		}
		String[] lines = text.split("\n");
		int i = 0;
		// Header
		boolean headerOk = false;
		while (i < lines.length) {
			String t = lines[i].trim();
			if (t.startsWith("Metasequoia Document")) {
				headerOk = true;
				i++;
				continue;
			}
			if (t.startsWith("Format Text Ver")) {
				i++;
				continue;
			}
			break;
		}
		if (!headerOk) {
			diag.warn("mqo", "NO_MQO_HEADER", "", "missing 'Metasequoia Document' header");
		}
		// Skip Thumbnail block
		while (i < lines.length) {
			String t = lines[i].trim();
			if (t.startsWith("Thumbnail")) {
				// skip until matching '}' that closes thumbnail (may span lines)
				i++;
				int depth = 1;
				while (i < lines.length && depth > 0) {
					String s = lines[i];
					for (int c = 0; c < s.length(); c++) {
						char ch = s.charAt(c);
						if (ch == '{') depth++;
						else if (ch == '}') depth--;
					}
					i++;
				}
				continue;
			}
			if (t.startsWith("Scene")) {
				i = skipBraceBlock(lines, i);
				continue;
			}
			if (t.startsWith("Material")) {
				i = parseMaterial(lines, i, materials, diag);
				continue;
			}
			if (t.startsWith("Object")) {
				Object3D obj = parseObject(lines, i, diag);
				if (obj != null) {
					objects.add(obj);
				}
				i = skipBraceBlock(lines, i); // consume remaining object lines if not consumed
				continue;
			}
			if (t.startsWith("Eof")) {
				break;
			}
			if (t.isEmpty() || t.startsWith("//")) {
				i++;
				continue;
			}
			// unknown top-level block
			diag.warn("mqo", "UNKNOWN_BLOCK", t.substring(0, Math.min(t.length(), 24)),
					"unknown MQO block skipped");
			i++;
		}
		return new Model(materials, objects, diag.snapshot());
	}

	private static int skipBraceBlock(String[] lines, int i) {
		int depth = 0;
		boolean started = false;
		while (i < lines.length) {
			String s = lines[i];
			for (int c = 0; c < s.length(); c++) {
				char ch = s.charAt(c);
				if (ch == '{') { depth++; started = true; }
				else if (ch == '}') { depth--; if (started && depth <= 0) return i + 1; }
			}
			i++;
		}
		return i;
	}

	private static int parseMaterial(String[] lines, int i, List<Material> out,
			ImportDiagnostic.Collector diag) {
		// "Material 3 {" ... "}"; each line: "name" shader(...) ... tex("path")
		int depth = 0;
		boolean started = false;
		while (i < lines.length) {
			String t = lines[i].trim();
			if (t.startsWith("Material")) {
				// header line, no brace yet
			} else if (t.isEmpty()) {
				i++;
				continue;
			}
			// count braces on this line
			int open = count(t, '{');
			int close = count(t, '}');
			started = started || open > 0;
			if (started) {
				String name = firstStringLiteral(t);
				String tex = textureFromLine(t);
				if (name != null) {
					out.add(new Material(name, tex));
				}
			}
			depth += open - close;
			if (started && depth <= 0) {
				return i + 1;
			}
			i++;
		}
		diag.warn("mqo", "UNCLOSED_MATERIAL", "", "material block not closed");
		return i;
	}

	private static Object3D parseObject(String[] lines, int i, ImportDiagnostic.Collector diag) {
		// "Object "name" {"
		String header = lines[i].trim();
		String name = firstStringLiteral(header);
		if (name == null) {
			diag.warn("mqo", "OBJECT_NO_NAME", header.substring(0, Math.min(24, header.length())), "object without name");
			return null;
		}
		int depth = 0;
		boolean started = false;
		float[][] verts = new float[0][3];
		List<Face> faces = new ArrayList<Face>();
		double scale = 1.0, rx = 0, ry = 0, rz = 0, tx = 0, ty = 0, tz = 0;
		// find block start (line may contain '{')
		boolean headerBrace = false;
		for (int c = 0; c < header.length(); c++) {
			if (header.charAt(c) == '{') {
				depth++;
				started = true;
				headerBrace = true;
			}
		}
		if (!headerBrace) {
			i++; // header has no brace; body starts next line
		} else {
			i++; // header brace counted; advance to body
		}
		while (i < lines.length) {
			String t = lines[i].trim();
			if (started) {
				if (t.startsWith("vertex")) {
					// "vertex 152 {"
					int vcount = parseIntToken(t);
					if (vcount <= 0 || vcount > MAX_VERTICES) {
						diag.reject("mqo", "TOO_MANY_VERTICES", name,
								"vertex count " + vcount + " exceeds cap " + MAX_VERTICES);
						verts = new float[0][3];
					} else {
						verts = new float[vcount][3];
						// consume vertex lines until '}'
						int v = 0;
						int j = i + 1;
						while (j < lines.length && v < vcount) {
							String vl = lines[j].trim();
							if (vl.startsWith("}")) {
								break;
							}
							String[] parts = vl.trim().split("[ \t]+");
							if (parts.length >= 3) {
								verts[v][0] = parseFloat(parts[0]);
								verts[v][1] = parseFloat(parts[1]);
								verts[v][2] = parseFloat(parts[2]);
								v++;
							}
							j++;
						}
						i = j;
					}
				} else if (t.startsWith("face")) {
					// "face 12 {"
					int fcount = parseIntToken(t);
					if (fcount <= 0 || fcount > MAX_FACES) {
						diag.reject("mqo", "TOO_MANY_FACES", name,
								"face count " + fcount + " exceeds cap " + MAX_FACES);
					} else {
						int j = i + 1;
						int f = 0;
						while (j < lines.length && f < fcount) {
							String fl = lines[j].trim();
							if (fl.startsWith("}")) {
								break;
							}
							Face face = parseFace(fl, diag, name);
							if (face != null) {
								faces.add(face);
								f++;
							}
							j++;
						}
						i = j;
					}
				} else if (t.startsWith("scale")) {
					String[] p = t.split("[ \t]+");
					if (p.length >= 4) scale = parseFloat(p[1]);
				} else if (t.startsWith("rotation")) {
					String[] p = t.split("[ \t]+");
					if (p.length >= 4) { rx = parseFloat(p[1]); ry = parseFloat(p[2]); rz = parseFloat(p[3]); }
				} else if (t.startsWith("translation")) {
					String[] p = t.split("[ \t]+");
					if (p.length >= 4) { tx = parseFloat(p[1]); ty = parseFloat(p[2]); tz = parseFloat(p[3]); }
				}
			}
			// vertex/face/... blocks are consumed internally (their braces are
			// balanced within the block reader), so only count braces that are
			// NOT part of a consumed inner block. We count braces only when the
			// line is the object header or a bare close (no inner block text).
			if (!t.startsWith("vertex") && !t.startsWith("face")) {
				int open = count(t, '{');
				int close = count(t, '}');
				started = started || open > 0;
				depth += open - close;
				if (started && depth <= 0) {
					return new Object3D(name, verts, faces, scale, rx, ry, rz, tx, ty, tz);
				}
			}
			i++;
		}
		diag.warn("mqo", "UNCLOSED_OBJECT", name, "object block not closed");
		return new Object3D(name, verts, faces, scale, rx, ry, rz, tx, ty, tz);
	}

	private static Face parseFace(String line, ImportDiagnostic.Collector diag, String obj) {
		// "4 V(6 4 2 0) M(0) UV(0.59 0.96 0.66 0.96 ...)"
		int vCount = 0;
		try {
			vCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
		} catch (RuntimeException e) {
			diag.warn("mqo", "BAD_FACE", obj, "face line unparsable: " + line.substring(0, Math.min(40, line.length())));
			return null;
		}
		if (vCount < 3 || vCount > 4) {
			diag.warn("mqo", "UNSUPPORTED_FACE", obj, "face with " + vCount + " verts skipped");
			return null;
		}
		int vi = line.indexOf("V(");
		int mi = line.indexOf("M(");
		if (vi < 0 || mi < 0) {
			diag.warn("mqo", "BAD_FACE", obj, "face missing V()/M()");
			return null;
		}
		int[] verts = new int[vCount];
		int vstart = vi + 2;
		int idx = 0;
		// V(...) indices are space-separated (no commas in this pack).
		int paren = line.indexOf(')', vstart);
		if (paren < 0) {
			paren = line.length();
		}
		String vbody = line.substring(vstart, paren);
		String[] vtoks = vbody.split("[ \t,]+");
		for (int k = 0; k < vtoks.length && idx < vCount; k++) {
			String tok = vtoks[k];
			if (tok.isEmpty()) {
				continue;
			}
			try {
				verts[idx++] = Integer.parseInt(tok);
			} catch (RuntimeException e) {
				diag.warn("mqo", "BAD_FACE", obj, "bad vertex index token: " + tok);
				return null;
			}
		}
		if (idx != vCount) {
			diag.warn("mqo", "BAD_FACE", obj, "vertex count mismatch in face");
			return null;
		}
		int material = 0;
		int mstart = mi + 2;
		int mend = line.indexOf(')', mstart);
		if (mend < 0) {
			mend = line.length();
		}
		try {
			material = Integer.parseInt(line.substring(mstart, mend).trim());
		} catch (RuntimeException e) {
			material = 0;
		}
		float[] uv = new float[0];
		int ui = line.indexOf("UV(");
		if (ui >= 0) {
			int ustart = ui + 3;
			int uend = line.indexOf(')', ustart);
			if (uend < 0) {
				uend = line.length();
			}
			String[] uvTok = line.substring(ustart, uend).trim().split("[ \t]+");
			if (uvTok.length >= vCount * 2) {
				uv = new float[vCount * 2];
				for (int k = 0; k < vCount * 2 && k < uvTok.length; k++) {
					uv[k] = parseFloat(uvTok[k]);
				}
			}
		}
		return new Face(verts, material, uv);
	}

	private static String textureFromLine(String line) {
		int ti = line.indexOf("tex(");
		if (ti < 0) {
			return "";
		}
		int q1 = line.indexOf('"', ti);
		if (q1 < 0) {
			return "";
		}
		int q2 = line.indexOf('"', q1 + 1);
		if (q2 < 0) {
			return "";
		}
		String t = line.substring(q1 + 1, q2).replace('\\', '/');
		while (t.contains("//")) {
			t = t.replace("//", "/");
		}
		return t;
	}

	private static String firstStringLiteral(String t) {
		int q1 = t.indexOf('"');
		if (q1 < 0) {
			return null;
		}
		int q2 = t.indexOf('"', q1 + 1);
		if (q2 < 0) {
			return null;
		}
		return t.substring(q1 + 1, q2);
	}

	private static int count(String s, char c) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				n++;
			}
		}
		return n;
	}

	private static int parseIntToken(String t) {
		try {
			int sp = t.indexOf(' ');
			if (sp < 0) {
				sp = t.indexOf('\t');
			}
			if (sp < 0) {
				return -1;
			}
			String rest = t.substring(sp + 1).trim();
			// take digits only (stop at '{' etc.)
			int end = 0;
			while (end < rest.length() && (rest.charAt(end) >= '0' && rest.charAt(end) <= '9')) {
				end++;
			}
			return end > 0 ? Integer.parseInt(rest.substring(0, end)) : -1;
		} catch (RuntimeException e) {
			return -1;
		}
	}

	private static float parseFloat(String s) {
		try {
			return Float.parseFloat(s.trim());
		} catch (RuntimeException e) {
			return 0.0F;
		}
	}
}
