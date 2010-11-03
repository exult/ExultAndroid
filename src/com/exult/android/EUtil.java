package com.exult.android;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

public class EUtil {
	private static TreeMap pathMap;
	private static byte buf2[] = new byte[2];
	private static byte buf4[] = new byte[4];
	public static final int Read2(byte buf[], int ind) {
		return ((int)buf[ind]&0xff) | ((int)buf[ind+1]&0xff)<<8;
	}
	public static final int Write2(byte buf[], int ind, short v) {
		buf[ind] = (byte)(v&0xff);
		buf[ind+1] = (byte)((v>>8)&0xff);
		return ind + 2;
	}
	public static final int Write2(byte buf[], int ind, int v) {
		buf[ind] = (byte)(v&0xff);
		buf[ind+1] = (byte)((v>>8)&0xff);
		return ind + 2;
	}
	public static final void Memcpy(byte dest[], int out, byte src[], int in, int cnt) {
		while (cnt > 0) {
			dest[out] = src[in];
			++out; ++in;
			--cnt;
		}
	}
	public static final int Read4(RandomAccessFile in) {
		try {
			int cnt = in.read(buf4);
			if (cnt != 4)
				return -1;	// Throw exception?
			return (((int)buf4[0]&0xff) | (((int)buf4[1]&0xff)<<8) | 
					(((int)buf4[2]&0xff)<<16) | (((int)buf4[3]&0xff)<<24));
		} catch (IOException e) {
			return -1;
		}
	}
	public static final int Read2(RandomAccessFile in) {
		try {
			int cnt = in.read(buf2);
			if (cnt != 2)
				return -1;	// Throw exception?
			return (((int)buf2[0]&0xff) | (((int)buf2[1]&0xff)<<8));
		} catch (IOException e) {
			return -1;
		}
	}
	private static String baseToUppercase(String str, int count) {
		if (count <= 0) return str;
		int todo = count, i;
						// Go backwards.
		for (i = str.length() - 1; i >= 0; --i) {
			int c = str.charAt(i);
			if (c == '/') {
				todo--;
				if (todo <= 0)
					break;
			}
		}
		if (todo > 0)
			return null;	// Didn't reach 'count' parts.
		String res = str.substring(0, i) + str.substring(i, str.length()).toUpperCase();
		return res;
	}

	public static final String U7exists(String nm) {
		String name = getSystemPath(nm);
		int uppercasecount = 0;
		do {
			if (new File(name).exists())
				return name; // found it!
		} while ((name = baseToUppercase(name, ++uppercasecount)) != null);
		return null;
	}
	public static final boolean isFlex(RandomAccessFile in) {
		int magic = 0;
		try {
			long pos = in.getFilePointer();
			long len = in.length();	// Check length.
			if (len >= 0x80L) {		// Has to be at least this long.
				in.seek(0x50);
				magic = EUtil.Read4(in);
			}
			in.seek(pos);
		} catch (IOException e) {}
		return (magic==0xffff1a00); 
	}
	public static final boolean isFlex(String nm) {
		RandomAccessFile file = null;
		String fname = U7exists(nm);
		if (fname != null) {
			try {
				file = new RandomAccessFile(fname, "r");
				return isFlex(file);
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
	public static boolean isSystemPathDefined(String path) {
		return pathMap != null && pathMap.containsKey(path);
	}
	public static String getSystemPath(String path) {
		String newPath;
		int pos, pos2;
		pos = path.indexOf('>');
		pos2 = path.indexOf('<');
		// If there is no separator, return the path as is
		if (pos == -1 || pos2 != 0) {
			newPath = path;
		} else {
			pos += 1;
			// See if we can translate this prefix
			String syspath = path.substring(0, pos);
			if (isSystemPathDefined(syspath)) {
				String newPrefix = (String)pathMap.get(syspath);
				newPath = newPrefix + path.substring(pos);
			} else {
				newPath = path;
			}
		}
		return newPath;
	}
	public static void addSystemPath(String key, String value) {
		if (pathMap == null)
			pathMap = new TreeMap();
		pathMap.put(key, value);
	}
	public static void initSystemPaths() {
		String base = "/sdcard/Games/exult/blackgate";	// FOR NOW.
		addSystemPath("<PATCH>", base + "/PATCH");
		addSystemPath("<STATIC>", base + "/STATIC");
		addSystemPath("<GAMEDAT>", base + "/GAMEDAT");
	}
}

