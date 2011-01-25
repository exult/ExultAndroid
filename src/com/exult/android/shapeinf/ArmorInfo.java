package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import com.exult.android.*;

public class ArmorInfo extends BaseInfo implements DataUtils.ReaderFunctor {
	private byte prot;		// Protection value.
	private byte immune;		// Weapon_data::damage_type bits.	
	public static final int is_binary = 1, entry_size = 10;
	public int getProt() {
		return ((int)prot)&0xff;
	}
	public int getImmune() {
		return ((int)immune)&0xff;
	}
	static int getInfoFlag() {
		return 4;
	}
	public int getBaseStrength() {
		// ++++The strength values are utter guesses.
		int strength = prot;
		if (immune != 0)	// Double strength for any immunities? Give bonus for each?
			strength *= 2;
		return strength;
	}
	public int getBaseXpValue() {
		return ((int)prot)&0xff;
	}
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		byte buf[] = new byte[entry_size-2];		// Entry length.
		try {
			in.read(buf);
		} catch (IOException e) {
			setInvalid(true);
			System.out.println("Error reading ARMOR info");
			return false;
		}
		int ind = 0;
		if (buf[entry_size-3] == 0xff) {	// means delete entry.
			setInvalid(true);
			info.setWeaponInfo(null);
			return true;
		}	
		prot = buf[ind++];			// Protection value.
		ind++;				// Unknown.
		immune = buf[ind++];		// Immunity flags.
						// Last 5 are unknown/unused.
		return true;
	}
}
