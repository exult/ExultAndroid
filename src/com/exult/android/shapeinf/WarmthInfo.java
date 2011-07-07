package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
/*
 *	Information about shapes accepted/rejected by containers.
 *	This is meant to be stored in a totally ordered vector.
 */
public class WarmthInfo extends BaseInfo.OneKeyInfo {
	// Key is the frame.
	private byte	warmth;	
	public final int getFrame()
		{ return keyval; }
	public int getWarmth()
		{ return warmth; }
	private boolean readNew(InputStream in, int version, boolean patch, int game,
				ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;	
		keyval = (short) EUtil.ReadInt(txtin);
		if (keyval < 0)
			keyval = -1;
		else
			keyval &= 0xff;
		warmth = (byte)(EUtil.ReadInt(txtin) & 0xff);
		info.setWarmthInfo(addVectorInfo(this, info.getWarmthInfo()));
		return true;
	}

	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return new WarmthInfo().readNew(in, version, patch, game, info);
	}
}
