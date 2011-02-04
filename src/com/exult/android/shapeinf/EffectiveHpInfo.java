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
public class EffectiveHpInfo extends BaseInfo implements Comparable<EffectiveHpInfo> {
	private short	frame;
	private short	quality;
	private byte	hps;	
	public int getFrame()
		{ return frame; }
	public int getQuality() 
		{ return quality; }
	public int getHps()
		{ return hps; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		frame = (short)EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;
		quality = (short)EUtil.ReadInt(txtin);
		if (quality < 0)
			quality = -1;
		else
			quality &= 255;
		hps = (byte)((int)EUtil.ReadInt(txtin) & 0xff);
		return true;
	}
	@Override
	public int compareTo(EffectiveHpInfo i2) {
		int v = frame - i2.frame;
		if (v == 0)
			v = quality - i2.quality;
		return v;
	}
}
