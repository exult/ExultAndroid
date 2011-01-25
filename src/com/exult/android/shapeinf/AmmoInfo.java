package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import com.exult.android.*;

public class AmmoInfo  extends BaseInfo implements DataUtils.ReaderFunctor {
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
	//+++++++FINISH public static AmmoInfo getDefault();
	int getFamilyShape()
		{ return familyShape; }
	int getSpriteShape()
		{ return sprite; }
	int getDamage()
		{ return damage; }
	int getDamageType()
		{ return damageType; }
	byte get_powers()
		{ return powers; }
	boolean no_blocking()
		{ return m_no_blocking; }
	byte get_drop_type()
		{ return dropType; }
	boolean autohits()
		{ return m_autohit; }
	boolean lucky()
		{ return m_lucky; }
	boolean returns()
		{ return m_returns; }
	boolean is_homing()
		{ return homing; }
	boolean explodes()
		{ return m_explodes; }
	static int get_info_flag()
		{ return 2; }
	//+++++++FINISH int getBaseStrength();
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		byte buf[] = new byte[entry_size-2];		// Entry length.
			try {
				in.read(buf);
			} catch (IOException e) {
				setInvalid(true);
				System.out.println("Error reading WEAPONS info");
				return false;
			}
			int ind = 0;
			if (buf[entry_size-3] == 0xff) {	// means delete entry.
				setInvalid(true);
				info.setWeaponInfo(null);
				return true;
			}		
		return false;//+++++++++FINISH
	}
}
