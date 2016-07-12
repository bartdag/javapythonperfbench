package com.infobart.bytesperf;

public class StringUtil {

	public final static char ESCAPE_CHAR = '\\';

	public static String escape(String original) {
		return original.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
	}

	public static String unescape(String escaped) {
		boolean escaping = false;
		StringBuilder newString = new StringBuilder();

		for (char c : escaped.toCharArray()) {
			if (!escaping) {
				if (c == ESCAPE_CHAR) {
					escaping = true;
				} else {
					newString.append(c);
				}
			} else {
				if (c == 'n') {
					newString.append('\n');
				} else if (c == 'r') {
					newString.append('\r');
				} else {
					newString.append(c);
				}
				escaping = false;
			}
		}

		return newString.toString();

	}
}
