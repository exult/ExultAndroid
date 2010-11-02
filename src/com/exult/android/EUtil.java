package com.exult.android;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;

public class EUtil {
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
	public static final boolean U7exists(String nm) {
		return (new File(nm)).exists(); 
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
		if (EUtil.U7exists(nm)) {
			try {
				file = new RandomAccessFile(nm, "r");
				return isFlex(file);
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
}

