package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
import com.exult.android.DataUtils;
import java.io.InputStream;

import com.exult.android.ShapeInfo;
/*
 *	Information about effective HPs.
 *	This is meant to be stored in a totally ordered vector.
 */
public class EffectiveHpInfo extends BaseInfo.FrameInfo {
	private byte	hps;	
	
	public int getHps()
		{ return hps; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		frame = EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;
		quality = EUtil.ReadInt(txtin);
		if (quality < 0)
			quality = -1;
		else
			quality &= 255;
		hps = (byte)((int)EUtil.ReadInt(txtin) & 0xff);
		return true;
	}
}
