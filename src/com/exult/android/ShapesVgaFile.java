package com.exult.android;
import java.util.TreeMap;

/*
 * Represents THE 'shapes.vga' file.
 */
public class ShapesVgaFile extends VgaFile {
	private boolean infoRead;
	private static ShapeInfo info[];
	private static ShapeInfo zinfo = new ShapeInfo();	//A fake one (all 0's).
	public ShapesVgaFile(
		String nm,				// Path to file.
		String nm2				// Patch file, or null.
		) {
		super(nm, nm2);
		info = new ShapeInfo[shapes.length];
		int gameType = GameSingletons.game.getGameType();
		ShapeInfo.read(this, info, gameType);
		zinfo.setReadyType((byte) (gameType == EConst.BLACK_GATE
								? Ready.backpack : Ready.rhand));
	}
	public static ShapeInfo getInfo(int shapenum) {
		ShapeInfo s = shapenum >= 0 ? info[shapenum] : zinfo;
		return s;
	}
}
