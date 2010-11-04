package com.exult.android;

public enum ShapeFiles {
	SHAPES_VGA	(EFile.SHAPES_VGA);
	
	private String name;
	private VgaFile file;
	ShapeFiles(String nm) {
		name = nm;
		file = null;
	}
	public final VgaFile getFile() {
		return file;
	}
	public static final void load() {
		SHAPES_VGA.file = new VgaFile(SHAPES_VGA.name, null);
	}
}
