package com.exult.android;
import java.io.IOException;

public class FlexFile extends EFile {
	public static final int EXULT_FLEX_MAGIC2 = 0x0000cc00;
	public static final int orig = 0;	// Original U7 version.
	public static final int exultV2 = 1;// Exult extension for IFIX files.
	byte title[];	// 80 bytes
	int	magic1;
	int	count;
	int	magic2;
	int	padding[];	// 9 words
	class  Reference {
		int offset;
		int size;
		byte buf[];
		Reference() {
			offset = 0; size = 0;
			buf = null;
		}		
	}
	Reference objects[];
	public FlexFile(String fname, String id) {
		super(fname, id);
		try {
			title = new byte[80];
			file.seek(0);
			file.read(title);
			magic1 = EUtil.Read4(file);
			count = EUtil.Read4(file);
			magic2 = EUtil.Read4(file);
			if (magic1!=0xffff1a00L)
					// Not a flex file.
				;	// Throw exception?
			padding = new int[9];
			for (int i=0; i<9; i++)
				padding[i] = EUtil.Read4(file);
			file.seek(128);	// Should already be there.
			objects = new Reference[count];
			for (int c = 0; c < count; c++) {
				Reference f = new Reference();
				f.offset = EUtil.Read4(file);
				f.size = EUtil.Read4(file);
				objects[c] = f;
			}
		} catch (IOException e) {
		}
	}
	public int getVers() {
		return (magic2&~0xff) == EXULT_FLEX_MAGIC2 ? exultV2 : orig;
	}
	public int numberOfObjects() {
		return objects.length;
	}
	public void close() {
		super.close();
		int cnt = objects.length;
		for (int i = 0; i < cnt; ++i)
			objects[i] = null;
	}
	public byte [] retrieve(int objnum) {
		if (objnum < 0 || objnum >= objects.length)
			return null;
		Reference ref = objects[objnum];
		if (ref.buf == null)
			try {
				file.seek(ref.offset);
				ref.buf = new byte[ref.size];
				file.read(ref.buf);
			} catch (IOException e) {
			}
		return ref.buf;
	}
	public String getArchiveType() {
		return "FLEX";
	}
}
