package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
import com.exult.android.DataUtils;
/*
 *	Information about frame names.
 *	This is meant to be stored in a totally ordered vector.
 */
public class FrameFlagsInfo extends BaseInfo implements Comparable<FrameFlagsInfo> {
	private short			frame;		// Frame for which this applies or -1 for any.
	private short			quality;	// Quality for which this applies or -1 for any.
	private int	m_flags;	// Bit field with the relevant flags.
	
	int get_frame()
		{ return frame; }
	public int get_quality()
		{ return quality; }
	public boolean get_flag(int tf)
		{ return (m_flags & (1 << tf)) != 0; }
	public int get_flags()
		{ return m_flags; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		frame = (short)EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;

		if (version >= 6)
			quality = (short)EUtil.ReadInt(txtin);
		else
			quality = -1;
		if (quality < 0)
			quality = -1;
		else
			quality &= 0xff;
		final int size = 32;	// Bit count for m_flags.
		m_flags = DataUtils.readBitFlags(txtin, size); 
		return true;
	}
	@Override
	public int compareTo(FrameFlagsInfo i2) {
		int v = frame - i2.frame;
		if (v == 0)
			v = quality - i2.quality;
		return v;
	}
}
