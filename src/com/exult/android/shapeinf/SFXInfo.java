package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.EUtil;
import com.exult.android.ShapeInfo;

import com.exult.android.ShapeInfo;

public class SFXInfo extends BaseInfo {
	private int		sfxnum;
	private boolean	random;			// sfx in range are to be randomly chosen.
	private int		range;			// # of sequential sfx to be used.
	private int		chance;			// % chance of playing the SFX.
	private int		extra;			// For grandfather clock.
	
	public int getSfx()
		{ return sfxnum; }
	public boolean playSequentially()
		{ return !random; }
	public boolean playRandomly()
		{ return random; }
	public int getChance()
		{ return chance; }
	public boolean playHourlyTicks() 
		{ return extra > -1; }
	public int getExtraSfx() 
		{ return extra; }
	public int get_sfx_range() 
		{ return range; }
	public boolean time_to_play() 
		{ return EUtil.rand()%100 < chance; }
	public int getNextSfx(int last) {	//++++NOTE: SB int& last
		if (range > 1) {
			if (random)
				return sfxnum + (EUtil.rand() % range);
			else {
				last = (last + 1) % range;
				return sfxnum + last;
			}
		}
		return sfxnum;
	}
	public static int getInfoFlag()
		{ return 0x20; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream) in;
		sfxnum = EUtil.ReadInt(txtin);
		if (sfxnum == -0xff) {	// means delete entry.
			setInvalid(true);
			return true;
		}
		if (version >= 2) {
			chance = EUtil.ReadInt(txtin, 100);
			if (chance < 1 || chance > 100)
				chance = 100;
			range = EUtil.ReadInt(txtin, 1);
			if (range < 1)
				range = 1;		// Sensible default.
			random = EUtil.ReadInt(txtin, 0) != 0;
			extra = EUtil.ReadInt(txtin, -1);
		}
		return true;
	}

}
