package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class Shape {
	private WeakReference raw;			// Entire shape from file.
	private ShapeFrame frames[];		// List of ->'s to frames.
	private int numFrames;				// # of frames (not counting reflects).
	private boolean modified;
	private boolean fromPatch;
	/*
	 *	Resize list upwards.
	 */
	private void enlarge(int newsize)
	{
		ShapeFrame newframes[] = new ShapeFrame[newsize];
		System.arraycopy(frames, 0, newframes, 0, frames.length);
		frames = newframes;
	}
	private void createFramesList(int nframes) {
		numFrames = nframes;
		frames = new ShapeFrame[nframes];
	}
	private ShapeFrame read(RandomAccessFile files[], boolean patch_flags[], int counts[], 
			int shnum, int frnum, int srcnum) {
		RandomAccessFile shpfile = null;
					// Figure offset in "shapes.vga".
		int shapeoff = 0x80 + shnum*8;
		int shapelen = 0;
	
		// Check backwards for the shape file to use.
		int i = counts.length - 1;
		if (srcnum < 0) {
			for ( ; i >= 0; --i) {
				if (shnum < counts[i]) {
					RandomAccessFile ds = files[i];
					try {
						ds.seek(shapeoff);
					} catch (IOException e) {
						continue;
					}
								// Get location, length.
					int s = EUtil.Read4(ds);
					shapelen = EUtil.Read4(ds);
					if (s > 0 && shapelen> 0) {
						shapeoff = s;
						shpfile = ds;
						fromPatch = patch_flags[i];
						break;
					}
				}
			}
		} else if (shnum < counts[srcnum]) {
			RandomAccessFile ds = files[srcnum];
			try {
				ds.seek(shapeoff);
			} catch (IOException e) {
				return null;
			}
						// Get location, length.
			int s = EUtil.Read4(ds);
			shapelen = EUtil.Read4(ds);
			if (s > 0 && shapelen> 0) {
				shapeoff = s;
				shpfile = ds;
				fromPatch = patch_flags[i];
			}
		}
		// The shape was not found anywhere, so leave.
		if (shpfile == null) {
		// std::cerr << "Shape num out of range: " << shapenum << std::endl;
		return null;
		}
		if (shapelen == 0)
			return null;		// Empty shape.
					// Read it in and get frame count.
		ShapeFrame frame = new ShapeFrame();
		int nframes;
		try {
			byte data[];
			if (raw == null || (data = (byte[])raw.get()) == null) {
				data = new byte[shapelen];
				raw = new WeakReference(data);
				shpfile.seek(shapeoff);
				shpfile.read(data);
			}
			nframes = frame.read(data, shapelen, frnum);
		} catch (IOException e) {
			return null;
		}
		if (numFrames== 0)		// 1st time?
			createFramesList(nframes);
		if (!frame.isRle())
			frnum &= 31;		// !!Guessing.
		if (frnum >= nframes &&	// Compare against #frames in file.
				(frnum&32) != 0) {		// Reflection desired?
			return (reflect(files, patch_flags, counts, shnum, frnum&0x1f));
		}
		return storeFrame(frame, frnum);
	}
	/*
	 *	Store frame that was read.
	 *
	 *	Output:	->frame, or 0 if not valid.
	 */
	private ShapeFrame storeFrame
		(
		ShapeFrame frame,		// Frame that was read.
		int frnum				// It's frame #.
		)
		{
		if (frnum < 0 || frnum >= frames.length) {			// Something fishy?
			//cerr << "Shape::store_frame:  framenum < 0 ("
			//	 << framenum << " >= " << (unsigned int)(frames_size)
			//	 << ")" << endl;
			return (null);
		}
		if (frames == null) {	// First one?
			frames = new ShapeFrame[numFrames];
			}
		frames[frnum] = frame;
		return (frame);
	}
	/*
	 *	Load all frames for a single shape.  (Assumes RLE-type shape.)
	 */
	void load(RandomAccessFile shapeSource) throws IOException {
		ShapeFrame frame = new ShapeFrame();
		int shapelen = EUtil.Read4(shapeSource);
		byte data[] = new byte[shapelen];
		shapeSource.seek(0);
		shapeSource.read(data);
						// Read frame 0 & get frame count.
		createFramesList(frame.read(data, shapelen, 0));
		storeFrame(frame, 0);
						// Get the rest.
		for (int i = 1; i < frames.length; i++) {
			frame = new ShapeFrame();
			frame.read(data, shapelen, i);
			storeFrame(frame, i);
		}
	}
	/*
	 *	Create the reflection of a shape.
	 */
	private ShapeFrame reflect(RandomAccessFile files[], boolean patch_flags[], int counts[], 
							int shnum, int frnum) {
						// Get normal frame.
		ShapeFrame normal = get(files, patch_flags, counts, shnum, frnum, -1);
		if (normal == null)
			return (null);
						// Reflect it.
		ShapeFrame reflected = normal.reflect();
		if (reflected == null)
			return (null);
		frnum |= 32;			// Put back 'reflect' flag.
		if (frnum >= frames.length - 1)// Expand list if necessary.
			enlarge(frnum + 1);
		frames[frnum] = reflected;	// Store new frame.
		return reflected;
	}
	public Shape() {
		frames = null;
		numFrames =  0;
		modified = false;
		fromPatch = false;
	}
	/*
	 * Create with given # of frames.
	 */
	public Shape(int n) {
		modified = false;
		fromPatch = false;
		createFramesList(n);
	}
	
	public int getNumFrames() {
	 	return numFrames; 
	}
	public ShapeFrame getFrame(int framenum) {
	 	return 0 <= framenum && framenum < frames.length
					? frames[framenum] : null; 
	}
	public ShapeFrame get(RandomAccessFile files[], boolean patch_flags[], int counts[], 
			int shnum, int frnum, int srcnum) { 
		return (frames != null && frnum < frames.length &&
				frames[frnum] != null) ? frames[frnum] : 
				read(files, patch_flags, counts, shnum, frnum, srcnum); 
	}
}
