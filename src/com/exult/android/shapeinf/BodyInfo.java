package com.exult.android.shapeinf;
import com.exult.android.*;
import java.io.PushbackInputStream;

import java.io.InputStream;

import com.exult.android.ShapeInfo;

public class BodyInfo extends BaseInfo {
	private int		bshape;			// Body shape.
	private int		bframe;			// Body frame.
	public int getBodyShape()
		{ return bshape; }
	public int getBodyFrame()
		{ return bframe; }
	public static int getInfoFlag()
		{ return 0x100; }
	private boolean readNew(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream) in;
		bshape = EUtil.ReadInt(txtin);
		if (bshape == -0xff) {	// means delete entry.
			setInvalid(true);
		return true;
		}
		bframe = EUtil.ReadInt(txtin);
		//System.out.println("BodyInfo: shape = " + bshape + ", frame = " + bframe);
		info.setBodyInfo(this);
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return (new BodyInfo()).readNew(in, version, patch, game, info);
	}
}
