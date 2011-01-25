package com.exult.android;

public class ShapeID extends GameSingletons {
	private short shapeNum;			// Shape #.
	private byte frameNum;			// Frame # within shape.
	private byte hasTrans;
	private ShapeFiles shapeFile;
	private ShapeFrame shape;
	private static ImageBuf.XformPalette xforms[];
	public static final int // Special pixels
		POISON_PIXEL = 0, PROTECT_PIXEL = 1, CURSED_PIXEL = 2,
		CHARMED_PIXEL = 3, HIT_PIXEL = 4, PARALYZE_PIXEL = 5, NPIXCOLORS = 6;
	private static byte specialPixels[]; 
	// Shape_info *info;

	private ShapeFrame cacheShape() {
		if (frameNum == -1) return null;

		if (hasTrans != 2) hasTrans = 0;
		if (shapeFile == ShapeFiles.SHAPES_VGA) {
			shape = shapeFile.getFile().getShape(shapeNum, frameNum);
			if (hasTrans != 2) 
				hasTrans = getInfo(shapeNum).hasTranslucency() ? (byte)1 : (byte)0;
		 
		} else if (shapeFile != null) {
			shape = shapeFile.getFile().getShape(shapeNum, frameNum);
			if (shapeFile == ShapeFiles.SPRITES_VGA)
				hasTrans = 1;
		} else {
			// std::cerr << "Error! Wrong ShapeFile!" << std::endl;
			return null;
		} 
		return shape;
	}
	public final void set(int shnum, int frnum, ShapeFiles shfile) {
		shapeNum = (short)shnum; frameNum = (byte)frnum;
		hasTrans = 0;
		shape = null;
		shapeFile = shfile;	
	}
	public ShapeID(int shnum, int frnum, ShapeFiles shfile) {
		set(shnum, frnum, shfile);
	}
	public ShapeID(int shnum, int frnum) {
		set(shnum, frnum, ShapeFiles.SHAPES_VGA);
	}
	public ShapeID() {
		set(0,0, null);
	}
	public final boolean isInvalid()
		{ return shapeNum == -1; }

	public final int getShapeNum()
		{ return shapeNum; }
	public final int getFrameNum()
		{ return frameNum; }
	public final ShapeFiles getShapeFile()
		{ return shapeFile; }
	public final ShapeFrame getShape()
		{ return (shape!=null)?shape:cacheShape(); }
	public final ShapeInfo getInfo() {
		return ShapesVgaFile.getInfo(shapeNum);
	}
	public static final ShapeInfo getInfo(int shnum) {
		return ShapesVgaFile.getInfo(shnum);
	}
	public final void set_translucent(int trans)
		{ hasTrans = (byte)trans; }
	public final boolean isTranslucent()
		{ if (shape==null) cacheShape(); return hasTrans!=0; }
					// Set to given shape.
	public final void setShape(int shnum, int frnum) {
		shapeNum = (short)shnum;
		frameNum = (byte)frnum;
		shape = null;
		//info = null;
	}
	public final void setShape(int shnum)	// Set shape, but keep old frame #.
		{ shapeNum = (short)shnum; shape = null; /* info = 0; */ }
	public final void setFrame(int frnum)	// Set to new frame.
		{ frameNum = (byte)frnum; shape = null; }
	public final void setFile(ShapeFiles shfile)	// Set to new flex
		{ shapeFile = shfile; shape = null; }
	public final int getNumFrames() {
		if (shapeFile != null)
			return shapeFile.getFile().getNumFrames(shapeNum);
		else
			return 0;
	}
	public void paintShapeTranslucent(int xoff, int yoff) {
		ShapeFrame s = getShape();
		if (s != null) {
			if (xforms != null) {
				s.paintRleTranslucent(gwin.getWin(), xoff, yoff, xforms);
			} else {
				s.paint(gwin.getWin(), xoff, yoff);
			}
		}
	}
	public void paintShape(int xoff, int yoff) {
		ShapeFrame s = getShape();
		if (s != null) {
			if (hasTrans != 0 && xforms != null) {
				s.paintRleTranslucent(gwin.getWin(), xoff, yoff, xforms);
			} else {
				s.paint(gwin.getWin(), xoff, yoff);
			}
		}
	}
	public void paintOutline(int xoff, int yoff, int pix) {
		ShapeFrame s = getShape();
		if (s != null) {
			s.paintRleOutline(gwin.getWin(), xoff, yoff, specialPixels[pix]);
		}
	}
	/*
	 * Load static/global data.
	 */
	public static void loadStatic() {
		EFile xf = fman.getFileObject(EFile.XFORMTBL, EFile.PATCH_XFORMS);
		int nxforms = 17;	// FOR NOW.
		if (xf != null) {
			int nobjs = xf.numberOfObjects();
			if (nobjs > nxforms)
				nobjs = nxforms;
			xforms = new ImageBuf.XformPalette[nobjs];
			for (int i = 0; i < nobjs; ++i) {
				xforms[nxforms - 1 - i] = new ImageBuf.XformPalette();
				byte data[] = xf.retrieve(i);
				if (data == null) {
					// No XForm data at all. Make this XForm into an
					// identity transformation.
					for (int j = 0; j < ImageBuf.XformPalette.NCOLORS; j++)
						xforms[nxforms - 1 - i].colors[j] = (byte)j;
				} else {
					System.arraycopy(data, 0, xforms[nxforms - 1 - i].colors, 0, 
										ImageBuf.XformPalette.NCOLORS);
				}
			}
			xf.close();
		} else {
			xforms = null;
		}
		specialPixels = new byte[NPIXCOLORS];
		// Determine some colors based on the default palette
		Palette pal = new Palette(gwin.getWin());
		pal.load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, 0);
			// Get a bright green.
		specialPixels[POISON_PIXEL] = (byte)pal.findColor(4, 63, 4);
			// Get a light gray.
		specialPixels[PROTECT_PIXEL] = (byte)pal.findColor(62, 62, 55);
			// Yellow for cursed.
		specialPixels[CURSED_PIXEL] = (byte)pal.findColor(62, 62, 5);
			// Light blue for charmed.
		specialPixels[CHARMED_PIXEL] = (byte)pal.findColor(30, 40, 63);
			// Red for hit in battle.
		specialPixels[HIT_PIXEL] = (byte)pal.findColor(63, 4, 4);
			// Purple for paralyze.
		specialPixels[PARALYZE_PIXEL] = (byte)pal.findColor(49, 27, 49);
	}
}
