package com.terradue.ssegrid.sagaext;

import java.io.*;
import java.util.regex.*;

/** Class to encapsulate the filtration for Find.
 * Really needs to be a real
 * data structure to allow complex things like
 *		-n "*.html" -a \( -size < 0 -o mtime < 5 \).
 */
public class FindFilter implements FilenameFilter {
	boolean sizeSet;
	int size;
	String name;
	Pattern nameRE;

	public FindFilter() {
	}
	
	public FindFilter(String name) {
		setNameFilter(name);
	}

	void setSizeFilter(String sizeFilter) {
		size = Integer.parseInt(sizeFilter);
		sizeSet = true;
	}

	/** Convert the given shell wildcard pattern into internal form (an RE) */
	void setNameFilter(String nameFilter) {
		name = nameFilter;
		StringBuffer sb = new StringBuffer('^');
		for (int i = 0; i < nameFilter.length(); i++) {
			char c = nameFilter.charAt(i);
			switch(c) {
				case '.':	sb.append("\\."); break;
				case '*':	sb.append(".*"); break;
				case '?':	sb.append('.'); break;
				default:	sb.append(c); break;
			}
		}
		sb.append('$');
		try {
			nameRE = Pattern.compile(sb.toString());
		} catch (PatternSyntaxException ex) {
			System.err.println("Error: RE " + sb.toString() +
				" didn't compile: " + ex);
		}
	}

	/** Do the filtering. For now, only filter on name */
	public boolean accept(File dir, String fileName) {
		File f = new File(dir, fileName);

		if (nameRE != null) {
			return nameRE.matcher(fileName).matches();
		}

		// TODO size handling.

		// Catchall
		return false;
	}
}
