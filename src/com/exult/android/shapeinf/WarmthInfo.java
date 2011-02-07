package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
/*
 *	Information about shapes accepted/rejected by containers.
 *	This is meant to be stored in a totally ordered vector.
 */
public class WarmthInfo extends BaseInfo implements Comparable<WarmthInfo> {
	private short	frame;
	private byte	warmth;	
	public final int getFrame()
		{ return frame; }
	public int getWarmth()
		{ return warmth; }
	private boolean readNew(InputStream in, int version, boolean patch, int game,
				ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;	
		frame = (short) EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;
		warmth = (byte)(EUtil.ReadInt(txtin) & 0xff);
		//++++++++FINISH: insert into vector.
		//+++info.setWarmthInfo(addVectorInfo(this, info.getWarmthInfo()));
		return true;
	}

	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return new WarmthInfo().readNew(in, version, patch, game, info);
	}
	@Override
	public int compareTo(WarmthInfo i2) {
		return frame - i2.frame;
	}
}
