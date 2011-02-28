package com.exult.android;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

public class VgaFile {
	protected RandomAccessFile shapeSources[];
	protected boolean patchFlags[];
	protected int shapeCnts[];
	protected Shape shapes[];
	protected boolean flex;	// False if this is a single-shape file.
	
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
	protected void reset() {
	// Not sure if this is needed.	
	}
	public int getNumShapes() {
		return shapes.length;
	}
	/*
	 *	Open file.
	 */
	protected RandomAccessFile U7load
		(
		String resource,
		Vector<RandomAccessFile> tmpSources,
		boolean tmpPatch[]
		) {
		RandomAccessFile source = null;
		String fname = EUtil.U7exists(resource);
		if (fname != null) {
			try {
				source = new RandomAccessFile(fname, "r");
			} catch (IOException e) {
				return null;
			}
			tmpSources.add(source);
			tmpPatch[tmpSources.size() - 1] = resource.regionMatches(0, "<PATCH>", 0, 7);
		}
		return source;
	}
	protected boolean load(String sources[]) {
		reset();
		int count = sources.length;
		int num_shapes = 0;
		Vector<RandomAccessFile> tmpSources = new Vector<RandomAccessFile>(count);
		boolean tmpPatch[] = new boolean[count];
		int tmpCnts[] = new int[count];
		
		boolean is_good = true;
		if (EUtil.U7exists(sources[0]) == null)
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
	protected boolean load(String nm, String nm2) {
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
	
	/*
	 *	A shape file just has one shape with multiple frames.  They're all
	 *	read in during construction.
	 */
	public static class ShapeFile extends Shape {
		public ShapeFile(byte data[]) {
			super.load(data);
		}
		public ShapeFile(String nm) {
			try {
				load(nm);
			} catch (IOException e) {
				System.out.println("ERROR loading " + nm);
			}
		}
		public ShapeFile(ShapeFrame fr) {
			super(fr);
		}
		/*
		ShapeFile(const char *nm);
		
		Shape_file(DataSource* out);
		*/
		public void load(String nm) throws IOException {
			RandomAccessFile src = EUtil.U7open(nm, true);
			super.load(src);
		}
		public void load(RandomAccessFile src) throws IOException { 
			super.load(src);
		}
		/* UNUSED
		private int getSize() {
			int size = 4;
			for (int i=0; i<numFrames; i++)
				size += frames[i].getSize() + 4 + 8;
			return size;
		} */
		//	This only works on RLE shapes.
		public void save(OutputStream out) throws IOException {
			int offsets[] = new int[numFrames];
			int size;
			offsets[0] = 4 + numFrames * 4;
			int i;	// Blame MSVC
			for (i=1; i<numFrames; i++)
				offsets[i] = offsets[i-1] + frames[i-1].getSize() + 8;
			System.out.println("numFrames = " + numFrames);
			size = offsets[numFrames-1] + frames[numFrames-1].getSize() + 8;
			EUtil.Write4(out, size);
			for (i=0; i<numFrames; i++)
				EUtil.Write4(out, offsets[i]);
			for (i=0; i<numFrames; i++) {
				EUtil.Write2(out, frames[i].getXRight());
				EUtil.Write2(out, frames[i].getXLeft());
				EUtil.Write2(out, frames[i].getYAbove());
				EUtil.Write2(out, frames[i].getYBelow());
				System.out.println("frames[i] size = " + frames[i].getSize());
				out.write((frames[i].getData()), 0, frames[i].getSize());
			}
		}
	}
}
