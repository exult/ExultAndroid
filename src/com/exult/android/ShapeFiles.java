package com.exult.android;

public enum ShapeFiles {
	SHAPES_VGA,
	GUMPS_VGA,
	PAPERDOL,	
	SPRITES_VGA,
	FACES_VGA,	
	EXULT_FLX,		
	GAME_FLX;
	
	private VgaFile file;
	ShapeFiles() {
		file = null;
	}
	public final VgaFile getFile() {
		return file;
	}
	public ShapeFrame getShape(int shapenum, int framenum) {
		return file.getShape(shapenum, framenum);
	}
	public static final void load() {
		SHAPES_VGA.file = new ShapesVgaFile(EFile.SHAPES_VGA, EFile.PATCH_SHAPES);
		//++++FINISH.
	}
}
