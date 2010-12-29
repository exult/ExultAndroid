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
		FACES_VGA.file = new VgaFile(EFile.FACES_VGA, EFile.PATCH_FACES);
		GUMPS_VGA.file = new VgaFile(EFile.GUMPS_VGA, EFile.PATCH_GUMPS);
		if (EUtil.U7exists(EFile.EXULT_FLX) == null)
			ExultActivity.fileFatal(EFile.EXULT_FLX);
		EXULT_FLX.file = new VgaFile(EFile.EXULT_FLX, EFile.BUNDLE_EXULT_FLX);
		//++++FINISH.
	}
}
