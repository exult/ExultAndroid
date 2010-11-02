package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Vector;

public class VgaFile {
	private RandomAccessFile shapeSources[];
	private boolean patchFlags[];
	private int shapeCnts[];
	private Shape shapes[];
	private boolean flex;	// False if this is a single-shape file.
	
	public VgaFile(
		String nm,				// Path to file.
		String nm2				// Patch file, or null.
		) {
		flex = true;
		load(nm, nm2);
	}

	public VgaFile() {
		flex = true;
	}
	private void reset() {
	// Not sure if this is needed.	
	}
	/*
	 *	Open file.
	 */
	private RandomAccessFile U7load
		(
		String resource,
		Vector<RandomAccessFile> tmpSources,
		boolean tmpPatch[]
		) {
		RandomAccessFile source = null;
		if (EUtil.U7exists(resource)) {
			try {
				source = new RandomAccessFile(resource, "r");
			} catch (IOException e) {
				return null;
			}
			tmpSources.add(source);
			tmpPatch[tmpSources.size() - 1] = resource.regionMatches(0, "<PATCH>", 0, 7);
		}
		return source;
	}
	private boolean load(String sources[]) {
		reset();
		int count = sources.length;
		int num_shapes = 0;
		Vector<RandomAccessFile> tmpSources = new Vector(count);
		boolean tmpPatch[] = new boolean[count];
		int tmpCnts[] = new int[count];
		
		boolean is_good = true;
		if (!EUtil.U7exists(sources[0]))
			is_good = false;
		for (int i = 0; i < count; ++i) {
			RandomAccessFile source = U7load(sources[i], tmpSources, tmpPatch);
			if (source != null) {
				flex = EUtil.isFlex(source);
				if (flex) {
					int cnt = 0;
					try {
						source.seek(0x54);	// Get # of shapes.
						cnt = EUtil.Read4(source);
						num_shapes = num_shapes > cnt ? num_shapes : cnt;
						tmpCnts[tmpSources.size()-1] = cnt;
					} catch (IOException e) {
						is_good = false;
					}
				}
			}
		}
		if (tmpSources.size() == 0)
			return false;		// ++++++throw exception?
		if (!flex) {			// Just one shape, which we preload.
			num_shapes = 1;
			tmpCnts[0] = 1;
			shapes = new Shape[1];
			shapes[0] = new Shape();
			is_good = true;
			try {
				shapes[0].load((RandomAccessFile)tmpSources.lastElement());
			} catch (IOException e) {
				is_good = false;
			}
		} else {
			shapes = new Shape[num_shapes];
			for (int i = 0; i < num_shapes; ++i)
				shapes[i] = new Shape();
		}
		count = tmpSources.size();
		shapeSources = new RandomAccessFile[count];
		for (int i = 0; i < count; ++i)
			shapeSources[i] = (RandomAccessFile)tmpSources.elementAt(i);
		patchFlags = new boolean[count];
		System.arraycopy(tmpPatch, 0, patchFlags, 0, count);
		shapeCnts = new int[count];
		System.arraycopy(tmpCnts, 0, shapeCnts, 0, count);
		return is_good;
	}
	private boolean load(String nm, String nm2) {
		String src[] = new String[nm2 != null ? 2 : 1];
		src[0] = nm;
		if (nm2 != null)
			src[1] = nm2;
		return load(src);
	}
	public ShapeFrame getShape(int shapenum, int framenum) {
		ShapeFrame r;
		r = (shapes[shapenum].get(shapeSources, patchFlags, shapeCnts, shapenum, framenum, -1));
		return r;
	}
	public ShapeFrame getShape(int shapenum) {
		return getShape(shapenum, 0);
	}
	public int getNumFrames(int shapenum) {
		getShape(shapenum, 0);	// Force it into memory.
		return shapes[shapenum].getNumFrames();
	}
}
