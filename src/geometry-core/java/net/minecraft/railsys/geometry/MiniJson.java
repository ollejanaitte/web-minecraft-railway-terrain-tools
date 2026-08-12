package net.minecraft.railsys.geometry;

/**
 * MiniJson — minimal, dependency-free JSON subset parser (Railsys clean-room).
 *
 * Only the features needed by the ModelPack prototype format (schema v1):
 * objects, arrays, strings, numbers, booleans and null. Used by
 * {@link RailModelPackParser} so the geometry-core has NO runtime dependency on
 * org.json (TeaVM/harness safe). Tolerates whitespace; throws on structural
 * errors, which the parser catches per asset.
 */
final class MiniJson {

	private final String s;
	private int i;

	private MiniJson(String s) {
		this.s = s;
		this.i = 0;
	}

	/** Parse a JSON value; returns Object (JSONObject-like map / list / String / Double / Boolean / null). */
	static Object parse(String text) {
		if (text == null) {
			throw new IllegalArgumentException("null json");
		}
		MiniJson j = new MiniJson(text);
		j.ws();
		Object v = j.value();
		j.ws();
		if (j.i < j.s.length()) {
			throw new IllegalArgumentException("trailing chars at " + j.i);
		}
		return v;
	}

	private Object value() {
		if (i >= s.length()) {
			throw new IllegalArgumentException("unexpected end");
		}
		char c = s.charAt(i);
		if (c == '{') {
			return object();
		}
		if (c == '[') {
			return array();
		}
		if (c == '"') {
			return string();
		}
		if (c == 't' || c == 'f') {
			return bool();
		}
		if (c == 'n') {
			expect("null");
			return null;
		}
		return number();
	}

	private java.util.Map<String, Object> object() {
		java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<String, Object>();
		i++; // {
		ws();
		if (peek('}')) {
			i++;
			return map;
		}
		while (true) {
			ws();
			if (peek('"')) {
				String key = string();
				ws();
				expect(':');
				ws();
				map.put(key, value());
				ws();
				if (peek(',')) {
					i++;
					continue;
				}
				if (peek('}')) {
					i++;
					return map;
				}
				throw new IllegalArgumentException("expected , or } at " + i);
			}
			throw new IllegalArgumentException("expected key at " + i);
		}
	}

	private java.util.List<Object> array() {
		java.util.ArrayList<Object> list = new java.util.ArrayList<Object>();
		i++; // [
		ws();
		if (peek(']')) {
			i++;
			return list;
		}
		while (true) {
			ws();
			list.add(value());
			ws();
			if (peek(',')) {
				i++;
				continue;
			}
			if (peek(']')) {
				i++;
				return list;
			}
			throw new IllegalArgumentException("expected , or ] at " + i);
		}
	}

	private String string() {
		i++; // "
		StringBuilder sb = new StringBuilder();
		while (i < s.length()) {
			char c = s.charAt(i++);
			if (c == '"') {
				return sb.toString();
			}
			if (c == '\\') {
				if (i >= s.length()) {
					break;
				}
				char e = s.charAt(i++);
				switch (e) {
					case '"': sb.append('"'); break;
					case '\\': sb.append('\\'); break;
					case '/': sb.append('/'); break;
					case 'n': sb.append('\n'); break;
					case 't': sb.append('\t'); break;
					case 'r': sb.append('\r'); break;
					case 'b': sb.append('\b'); break;
					case 'f': sb.append('\f'); break;
					default: sb.append(e); break;
				}
			} else {
				sb.append(c);
			}
		}
		throw new IllegalArgumentException("unterminated string");
	}

	private Object bool() {
		if (s.startsWith("true", i)) {
			i += 4;
			return Boolean.TRUE;
		}
		if (s.startsWith("false", i)) {
			i += 5;
			return Boolean.FALSE;
		}
		throw new IllegalArgumentException("bad bool at " + i);
	}

	private Object number() {
		int start = i;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9')) {
				i++;
			} else {
				break;
			}
		}
		if (start == i) {
			throw new IllegalArgumentException("bad number at " + i);
		}
		return Double.valueOf(s.substring(start, i));
	}

	private void expect(char c) {
		if (i >= s.length() || s.charAt(i) != c) {
			throw new IllegalArgumentException("expected '" + c + "' at " + i);
		}
		i++;
	}

	private void expect(String word) {
		if (!s.startsWith(word, i)) {
			throw new IllegalArgumentException("expected " + word + " at " + i);
		}
		i += word.length();
	}

	private boolean peek(char c) {
		return i < s.length() && s.charAt(i) == c;
	}

	private void ws() {
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				i++;
			} else {
				break;
			}
		}
	}

	// ---- typed accessors (object keys) ----

	static String optString(java.util.Map<String, Object> o, String key, String dflt) {
		Object v = o.get(key);
		return v == null ? dflt : String.valueOf(v);
	}

	static double optDouble(java.util.Map<String, Object> o, String key, double dflt) {
		Object v = o.get(key);
		if (v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		return dflt;
	}

	static int optInt(java.util.Map<String, Object> o, String key, int dflt) {
		Object v = o.get(key);
		if (v instanceof Number) {
			return (int) ((Number) v).doubleValue();
		}
		return dflt;
	}

	static boolean optBoolean(java.util.Map<String, Object> o, String key, boolean dflt) {
		Object v = o.get(key);
		if (v instanceof Boolean) {
			return ((Boolean) v).booleanValue();
		}
		return dflt;
	}

	static boolean has(java.util.Map<String, Object> o, String key) {
		return o.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	static java.util.List<Object> optArray(java.util.Map<String, Object> o, String key) {
		Object v = o.get(key);
		if (v instanceof java.util.List) {
			return (java.util.List<Object>) v;
		}
		return java.util.Collections.emptyList();
	}
}
