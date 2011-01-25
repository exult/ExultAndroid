package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import com.exult.android.*;

public class AmmoInfo extends BaseInfo implements DataUtils.ReaderFunctor {
	private static AmmoInfo defaultInfo;	// For shapes not found.
	private int familyShape;		// I.e., burst-arrow's is 'arrow'.
	private int sprite;				// What the missile should look like.
	private byte damage;		// Extra damage points.
	private byte powers;		// Same as for weapons.
	private byte damageType;	// Same as for weapons.
	private boolean m_no_blocking;		// Can move through walls.
	private byte dropType;	// What to do to missile when it hits/misses
	private boolean m_autohit;			// Weapon always hits.
	private boolean m_lucky;			// Easier to hit with.
	private boolean m_returns;			// Boomerang, magic axe.
	private boolean homing;		// For Energy Mist/Death Vortex.
	private boolean m_explodes;		// Burst arrows.
	public static final int // Drop_types			// Determines what happens when the missile misses
		drop_normally = 0,
		never_drop = 1,
		always_drop = 2
		;	
	public static final int is_binary = 1, entry_size = 13; 
	public AmmoInfo getDefault() {
		if (defaultInfo == null) {
			defaultInfo = new AmmoInfo();
			defaultInfo.familyShape =
			defaultInfo.sprite = -1;
			defaultInfo.damage =
			defaultInfo.powers =
			defaultInfo.damageType =
			defaultInfo.dropType = 0;
			defaultInfo.m_no_blocking =
			defaultInfo.m_autohit =
			defaultInfo.m_lucky =
			defaultInfo.m_returns =
			defaultInfo.homing =
			defaultInfo.m_explodes = false;
		}
		return defaultInfo;
	}
	public int getFamilyShape()
		{ return familyShape; }
	public int getSpriteShape()
		{ return sprite; }
	public int getDamage()
		{ return damage; }
	public int getDamageType()
		{ return damageType; }
	public byte get_powers()
		{ return powers; }
	public boolean no_blocking()
		{ return m_no_blocking; }
	public byte get_drop_type()
		{ return dropType; }
	public boolean autohits()
		{ return m_autohit; }
	public boolean lucky()
		{ return m_lucky; }
	public boolean returns()
		{ return m_returns; }
	public boolean is_homing()
		{ return homing; }
	public boolean explodes()
		{ return m_explodes; }
	public static int get_info_flag()
		{ return 2; }
	public int getBaseStrength() {
		// ++++The strength values are utter guesses.
		int strength = damage;
			// These 4 get picked with about the same odds.
		strength += (powers & WeaponInfo.no_damage) != 0 ? 10 : 0;
		strength += (powers & WeaponInfo.sleep) != 0 ? 10 : 0;
		strength += (powers & WeaponInfo.paralyze) != 0 ? 10 : 0;
		strength += (powers & WeaponInfo.charm) != 0 ? 10 : 0;
			// These have slightly lower odds.
		strength += (powers & WeaponInfo.poison) != 0 ? 5 : 0;
		strength += (powers & WeaponInfo.curse) != 0 ? 5 : 0;
		strength += (powers & WeaponInfo.magebane) != 0 ? 5 : 0;
		strength += m_lucky ? 5 : 0;
		strength += damageType != WeaponInfo.normal_damage ? 5 : 0;
		if (m_autohit)
			strength *= 2;	// These are almost unfair...
		if (m_no_blocking)
			strength *= 2;	// ... and these get picked a lot more often.
		return strength;
	}
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		byte buf[] = new byte[entry_size-2];		// Entry length.
		try {
			in.read(buf);
		} catch (IOException e) {
			setInvalid(true);
			System.out.println("Error reading AMMO info");
			return false;
		}
		int ind = 0;
		if (buf[entry_size-3] == 0xff) {	// means delete entry.
			setInvalid(true);
			info.setWeaponInfo(null);
			return true;
		}		
		familyShape = EUtil.Read2(buf, ind);
		sprite = EUtil.Read2(buf, ind + 2);		// How the missile looks like
		ind += 4;
		damage = buf[ind++];
		byte flags0 = buf[ind++];
		m_lucky = ((flags0)&1) != 0;
		m_autohit = ((flags0>>1)&1) != 0;
		m_returns = ((flags0>>2)&1) != 0;
		m_no_blocking = ((flags0>>3)&1) != 0;
		homing = ((flags0>>4)&3)==3;
		dropType = (byte)(homing ? 0 : (flags0>>4)&3);
		m_explodes = ((flags0>>6)&1) != 0;
		ind++;			// 1 unknown.
		byte flags1 = buf[ind++];
		damageType = (byte)((flags1>>4)&15);
		powers = buf[ind++];
							// Last 2 unknown.
		return true;
	}
}
