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
public class FrameFlagsInfo extends BaseInfo.FrameInfo {
	public static final int // enum Enum_Frame_power_flags
		fp_poison_safe = 0,
		fp_charm_safe = 1,
		fp_sleep_safe = 2,
		fp_paralysis_safe = 3,
		fp_curse_safe = 4,
		fp_power_safe = 5,
		fp_death_safe = 6,
		fp_cant_die = 7,
		fp_cold_immune = 8,
		fp_doesnt_eat = 9,
		fp_swamp_safe = 10,
		fp_force_usecode = 11,
		fp_infinite_reagents = 12;
	public static final int // enum Enum_Frame_Flags
		poison_safe = (1 << fp_poison_safe),
		charm_safe = (1 << fp_charm_safe),
		sleep_safe = (1 << fp_sleep_safe),
		paralysis_safe = (1 << fp_paralysis_safe),
		curse_safe = (1 << fp_curse_safe),
		power_safe = (1 << fp_power_safe),
		death_safe = (1 << fp_death_safe),
		cant_die = (1 << fp_cant_die),
		cold_immune = (1 << fp_cold_immune),
		doesnt_eat = (1 << fp_doesnt_eat),
		swamp_safe = (1 << fp_swamp_safe),
		force_usecode = (1 << fp_force_usecode),
		infinite_reagents = (1 << fp_infinite_reagents);
	private int	m_flags;	// Bit field with the relevant flags.
	
	public boolean getFlag(int tf)
		{ return (m_flags & (1 << tf)) != 0; }
	public int getFlags()
		{ return m_flags; }
	private boolean readNew(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		frame = EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;

		if (version >= 6)
			quality = EUtil.ReadInt(txtin);
		else
			quality = -1;
		if (quality < 0)
			quality = -1;
		else
			quality &= 0xff;
		final int size = 32;	// Bit count for m_flags.
		m_flags = DataUtils.readBitFlags(txtin, size); 
		//System.out.println("frameFlagsInfo for frame " + frame + ", quality = " + quality);
		info.setFrameFlagsInfo(addVectorInfo(this, info.getFrameFlagsInfo()));
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return (new FrameFlagsInfo()).readNew(in, version, patch, game, info);
	}
}
