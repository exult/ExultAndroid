package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import com.exult.android.*;

public class WeaponInfo extends BaseInfo implements DataUtils.ReaderFunctor {
	// Actor frames to show when attacking with weapon:
	public final static int // Actor_frames
		reach = 0,
		raise = 1,
		fast_swing = 2,
		slow_swing = 3
		;
		// The weapon kind:
	public final static int // Weapon_uses
		melee = 0,
		poor_thrown = 1,
		good_thrown = 2,
		ranged = 3
		;
		// Type of ammo used by weapon:
	public final static int // Weapon_ammo
		self_ammo = -1,
		quality_ammo = -2,
		quantity_ammo = -3
		;	
	// Special weapon/ammo powers.
	public final static int //Powers
		sleep = 1,
		charm = 2,
		curse = 4,
		poison = 8,
		paralyze = 16,
		magebane = 32,		// Takes away mana.
		unknown = 64,		// This enters XP formula, but can't
						// figure out *what* it does.
		no_damage = 128		// Weapon/missile causes no damage;
						// also puts Draygan to sleep.
		;
	// Type of damage.  These also are the
	//   bit #'s in Monster_info's 
	//   immune and vulerable fields.
	public final static int // Damage_type
		normal_damage = 0,
		fire_damage = 1,
		magic_damage = 2,
		lightning_damage = 3,
		ethereal_damage = 4,
		sonic_damage = 5
		;
	private static WeaponInfo defaultInfo;	// For shapes not found.
	private byte damage;			// Damage points (positive).
	private byte powers;		// Poison, sleep, charm. flags.
	private byte damageType;	// See Damage_type above.
	private byte actorFrames;	// Frames for NPC when using (from
					//   Actor_frames above).  Low 2 bits
					//   are for 'strike', next 2 are for
					//   shooting/throwing.
	private short ammo;			// Shape # of ammo. consumed, or
					//   -1 = weapon is ammo.
					//   -2 = consummes weapon quality.
					//   -3 = consumes weapon quantity if ranged.
	private short projectile;		// Projectile shape, or
					//	-1 = no projectile shown.
					//	-3 = use weapon shape as sprite shape.
	private boolean m_autohit;			// Weapon always hits.
	private boolean m_lucky;			// Easier to hit with.
	private boolean m_explodes;		// Explodes on impact.
	private boolean m_no_blocking;		// Can move through walls.
	private boolean m_delete_depleted;	// Delete ammo == -2 weapon when quality reaches 0.
	private boolean m_returns;			// Boomerang, magic axe.
	private boolean m_need_target;		// If false, can be used to attack a tile.
	private short missileSpeed;	// # of steps taken by the missile each cycle.
	private short rotationSpeed;	// Added to frame # each cycle (misslies only).
	private int usecode;			// Usecode function, or 0.
	private byte uses;		// 0 = hand-hand, 1 = poor throwable,
					//   2 = good throwable, 3 = missile-firing.
	private byte range;		// Distance weapon can be used.
	private short sfx, hitsfx;		// Sound when using/hit, or -1.
	public WeaponInfo() {
	}
	public static final int is_binary = 1, entry_size = 21;
	public static  WeaponInfo getDefault() {
		if (defaultInfo == null) {
			defaultInfo = new WeaponInfo();
		defaultInfo.missileSpeed = 1;
		defaultInfo.damage = 1;
		defaultInfo.ammo = -1;
		defaultInfo.projectile = -3;
		defaultInfo.range = 4;
		defaultInfo.uses = 2;
		defaultInfo.actorFrames = 3;
		defaultInfo.powers = 0;
		defaultInfo.damageType = 0;
		defaultInfo.m_autohit =
		defaultInfo.m_lucky =
		defaultInfo.m_explodes =
		defaultInfo.m_no_blocking =
		defaultInfo.m_delete_depleted =
		defaultInfo.m_returns =
		defaultInfo.m_need_target = false;
		defaultInfo.rotationSpeed = 0;
		defaultInfo.usecode = 0;
		defaultInfo.sfx =
		defaultInfo.hitsfx = -1;
		}
	return defaultInfo;
	}
	public int getDamage() 
		{ return damage; }
	public int getDamageType() 
		{ return damageType; }
	public byte getPowers() 
		{ return powers; }
	public byte getActorFrames(boolean projectile) 
		{ return projectile ? (byte)(actorFrames&3) : (byte)((actorFrames>>2)&3); }
	public int getAmmoConsumed() 
		{ return ammo; }
	public int getAmmo() 			// Raw value, for map-editor.
		{ return ammo; }
	public boolean usesCharges() 
		{ return ammo == -2; }
	public boolean usesAmmoOnRanged() 
		{ return ammo != -1; }
	public boolean isThrown() 
		// Figured this out from printing out values:
		{ return ammo == -3 && uses != 0 && uses != 3; }
	public boolean returns() 
		{ return m_returns; }
	public boolean explodes() 
		{ return m_explodes; }
	public boolean noBlocking() 
		{ return m_no_blocking; }
	public boolean autohits() 
		{ return m_autohit; }
	public boolean lucky() 
		{ return m_lucky; }
	public boolean deleteDepleted() 
		{ return m_delete_depleted; }
	public boolean needsTarget() 
		{ return m_need_target; }
	public byte getUses() 
		{ return uses; }
	public int getRange() 			// Raw # (for map-editor).
		{ return range; }
	public int getStrikingTange() 
		{ return uses < 3 ? range : 0; }
	public int getProjectileRange() 	// Guess for thrown weapons.
		{ return uses == 3 ? range : -1; }
	public int getProjectile() 
		{ return projectile; }
	public int getMissileSpeed() 
		{ return missileSpeed; }
	public int getRotationSpeed() 
		{ return rotationSpeed; }
	public int getUsecode() 
		{ return usecode; }
	public int getSfx() 			// Return sound-effects #, or -1.
		{ return sfx; }
	public int getHitsfx() 
		{ return hitsfx; }
	public static int getInfoFlag()
		{ return 1; }
	public int getBaseStrength() {
		if (m_explodes && uses == melee)
			return -50;		// Avoid hand-held explosives at all costs.
		else if (usecode == 0x689)
			// Causes death in BG. In SI, weapons set with this function
			// also get picked very often even though the function does
			// not exits.
			return 5000;
		else if (m_explodes)	// These are safer, and seem to be preferred.
			return 3000;
		else {
			int strength;
			if ((powers & no_damage) != 0)
				strength = 0;	// Start from zero.
			else
				strength = damage;
				// These don't kill, but disable.
			strength += (powers & sleep) != 0 ? 25 : 0;
			strength += (powers & paralyze) != 0 ? 25 : 0;
				// Charm is slightly worse than the above two.
			strength += (powers & charm) != 0 ? 20 : 0;
			strength += (powers & poison) != 0 ? 10 : 0;
			strength += (powers & curse) != 0 ? 5 : 0;
			strength += m_lucky ? 5 : 0;
			strength += damageType != normal_damage ? 10 : 0;
			if (m_autohit)
				strength *= 2;
				// Magebane power is too situation-specific.
				// Maybe give bonus for lightning damage, as it ignores armor?
			return strength;
		}
	}
	public int getBaseXpValue() {
		// This formula is exact.
		int expval = damage;
		expval += 2*EUtil.bitcount(powers);
		expval += m_explodes ? 1 : 0;
		expval += m_autohit ? 10 : 0;
		expval += m_no_blocking ? 1 : 0;
		return expval;
	}
	private boolean readNew(InputStream in, int version, 
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
		ammo = (short)EUtil.Read2(buf, ind);		// This is ammo family, or a neg. #.
		ind += 2;
						// Shape to strike with, or projectile
						//   shape if shoot/throw.
		projectile = (short)EUtil.Read2(buf, ind);	// What a projectile fired will look like.
		ind += 2;
		damage = buf[ind++];
		byte flags0 = buf[ind++];
		m_lucky = (flags0&1) != 0;
		m_explodes = ((flags0>>1)&1) != 0;
		m_no_blocking = ((flags0>>2)&1) != 0;
		m_delete_depleted = ((flags0>>3)&1) != 0;
		damageType = (byte)((flags0>>4)&15);
		range = buf[ind++];
		m_autohit = (range&1) != 0;
		uses = (byte)((range>>1)&3);		// Throwable, etc.:
		range = (byte) (range>>3);
		byte flags1 = buf[ind++];
		m_returns = (flags1&1) != 0;
		m_need_target = ((flags1>>1)&1) != 0;
		missileSpeed = (short)((flags1>>2)&3);
		rotationSpeed = (short)((flags1>>4)&15);
		byte flags2 = buf[ind++];
		actorFrames = (byte)(flags2&15);
		int speed =  (flags2>>5)&7;
		if (missileSpeed != 0)
			missileSpeed = 4;
		else
			missileSpeed = (short)(speed == 0 ? 3 : (speed < 3 ? 2 : 1)); 
		powers = buf[ind++];
		ind++;				// Skip (0).
		usecode = EUtil.Read2(buf, ind);
		ind += 2;
						// BG:  Subtract 1 from each sfx.
		int sfxDelta = game == EConst.BLACK_GATE ? -1 : 0;
		sfx = (short)(EUtil.Read2(buf, ind) + sfxDelta);
		ind += 2;
		hitsfx = (short)(EUtil.Read2(buf, ind) + sfxDelta);
		ind += 2;
		/*	Don't (seems to be unused).
		if (hitsfx == 123 && game == SERPENT_ISLE)	// SerpentIsle:  Does not sound right.
			hitsfx = 61;		// Sounds more like a weapon.
		*/
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		return (new WeaponInfo()).readNew(in, version, patch, game, info);
	}
}
