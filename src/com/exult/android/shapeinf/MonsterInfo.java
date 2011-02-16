package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import com.exult.android.*;

public class MonsterInfo extends BaseInfo implements DataUtils.ReaderFunctor {
	private static Vector<EquipRecord> equip;	// ->equipment info.
	private static MonsterInfo default_info;	// For shapes not found.
	private byte strength;		// Attributes.
	private byte dexterity;
	private byte intelligence;
	private byte alignment;	// Default alignment.
	private byte combat;
	private byte armor;		// These are unarmed stats.
	private byte weapon;
	private byte reach;
	private byte flags;		// Defined below.
					// The following are bits corresponding
					//   to Weapon_data::Damage_type.
	private byte vulnerable, immune;
	private byte equip_offset;	// Offset in 'equip.dat' (1 based;
					//   if 0, there's none.)
	private short sfx;		// Sound used when attacking. We *need* better sfx packs.
	private boolean m_splits;			// For slimes.
	private boolean m_cant_die;
	private boolean m_cant_yell;		// Can't yell during combat.
	private boolean m_cant_bleed;

	private boolean m_poison_safe;		// Can't be poisoned.
	private boolean m_charm_safe;		// Can't be charmed.
	private boolean m_sleep_safe;		// Can't be put to sleep.
	private boolean m_paralysis_safe;		// Can't be paralyzed.
	private boolean m_curse_safe;		// Can't be cursed.
	private boolean m_power_safe;		// As above 4 items, plus return of flag 13.
	private boolean m_death_safe;		// Return of flag 14. Immune to death spells?
	private boolean m_int_b1;			// May give XP; but what does it do???

	private byte m_attackmode;		// Sets initial attack mode.
	private byte m_byte13;			// Unknown; Bits 3 through 7 of byte 13.

	private boolean m_can_teleport;
	private boolean m_can_summon;
	private boolean m_can_be_invisible;
	
	public static final int // enum 
		is_binary = 1, entry_size = 25;
	private boolean readNew(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		byte buf[] = new byte[entry_size-2];		// Entry length.
		try {
			in.read(buf);
		} catch (IOException e) {
			setInvalid(true);
			System.out.println("Error reading MONSTERS info");
			return false;
		}
		int ind = 0;
		if (buf[entry_size-3] == 0xff) {	// means delete entry.
			setInvalid(true);
			info.setMonsterInfo(null);
			return true;
		}
		m_sleep_safe = (buf[ind]&1) != 0;
		m_charm_safe = ((buf[ind] >> 1) & 1) != 0;
		strength = (byte)((int)(buf[ind++] >> 2) & 63);	// Byte 2.

		m_curse_safe = (buf[ind]&1) != 0;
		m_paralysis_safe = ((buf[ind] >> 1) & 1) != 0;
		dexterity = (byte)(((int)buf[ind++] >> 2) & 63);	// Byte 3.

		m_poison_safe = (buf[ind]&1) != 0;	// verified
		m_int_b1 = ((buf[ind] >> 1) & 1) != 0;	// What does this do?
		intelligence = (byte)(((int)buf[ind++] >> 2) & 63);	// Byte 4.

		alignment = (byte)(buf[ind] & 3);		// Byte 5.
		combat = (byte)(((int)buf[ind++] >> 2) & 63);

		m_splits = (buf[ind] & 1) != 0;	// Byte 6 (slimes).
		m_cant_die = (buf[ind] & 2) != 0;
		m_power_safe = (buf[ind] & 4) != 0;
		m_death_safe = (buf[ind] & 8) != 0;
		armor = (byte)(((int)buf[ind++] >> 4) & 15);

		ind++;				// Byte 7: Unknown.
		reach = (byte)(buf[ind] & 15);		// Byte 8 - weapon reach.
		weapon = (byte)((buf[ind++] >> 4) & 15);
		flags = buf[ind++];			// Byte 9.
		vulnerable = buf[ind++];	// Byte 10.
		immune = buf[ind++];		// Byte 11.
		m_cant_yell = (buf[ind] & (1<<5)) != 0;		// Byte 12.
		m_cant_bleed = (buf[ind] & (1<<6)) != 0;
		ind++;
		m_attackmode = (byte)((buf[ind] & 7)-1);
		if (m_attackmode < 0)		// Fixes invalid data saved by older
			m_attackmode = 2;		// versions of ES.
		m_byte13 = (byte)((buf[ind++])&~7);	// Byte 13: partly unknown.
		equip_offset = buf[ind++];		// Byte 14.
		m_can_teleport = (buf[ind] & 1) != 0;	// Exult's extra flags: byte 15.
		m_can_summon = (buf[ind] & 2) != 0;
		m_can_be_invisible = (buf[ind] & 4) != 0;
		ind++;
		ind++;		// Byte 16: Unknown (0).
		int sfx_delta = game == EConst.BLACK_GATE ? -1 : 0;
		sfx = (short)(buf[ind++] + sfx_delta);	// Byte 17.
		info.setMonsterInfo(this);
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {	
		return (new MonsterInfo()).readNew(in, version, patch, game, info);
	}
	public static MonsterInfo getDefault() {
		if (default_info == null) {
			default_info = new MonsterInfo();
			default_info.combat = 10;
			default_info.reach = 3;
			default_info.flags = (1<<(int) walk);
			default_info.m_attackmode = 2;
		}
		return default_info;
	}
	public static void reserveEquip(int cnt) {
		equip = new Vector<EquipRecord>(cnt);
	}
	public static void addEquip(EquipRecord eq)
		{ equip.add(eq); }
	public static int getEquipCnt()
		{ return equip.size(); }
	public static EquipRecord getEquip(int i)
		{ return equip.elementAt(i); }
	public boolean splits()
		{ return m_splits; }
	public boolean cantDie()
		{ return m_cant_die; }
	public boolean cantYell()
		{ return m_cant_yell; }
	public boolean cantBleed()
		{ return m_cant_bleed; }
	public boolean poisonSafe()
		{ return m_poison_safe; }
	public boolean charmSafe()
		{ return m_charm_safe; }
	public boolean sleepSafe()
		{ return m_sleep_safe; }
	public boolean paralysisSafe()
		{ return m_paralysis_safe; }
	public boolean curseSafe()
		{ return m_curse_safe; }
	public boolean powerSafe()
		{ return m_power_safe; }
	public boolean deathSafe()
		{ return m_death_safe; }
	public boolean get_int_b1()
		{ return m_int_b1; }
	byte getByte13()
		{ return m_byte13; }
	public byte getAttackmode()
		{ return m_attackmode; }
	public boolean canTeleport()
		{ return m_can_teleport; }
	public boolean canSummon()
		{ return m_can_summon; }
	public boolean canBeInvisible()
		{ return m_can_be_invisible; }
					// Get bits indicating
					//   Weapon_data::damage_type:
	public byte getVulnerable()
		{ return vulnerable; }
	public byte getImmune()
		{ return immune; }
	public final static int // enum Flags {
		fly = 0,
		swim = 1,
		walk = 2,
		ethereal = 3,		// Can walk through walls.
		no_body = 4,		// Don't create body.
					// 5:  gazer, hook only.
		start_invisible = 6,
		see_invisible = 7
		;
	public byte getFlags()	// Get above set of flags.
		{ return flags; }
	public boolean hasNoBody()	// No dead body?
		{ return ((flags>>no_body)&1)!= 0; }
	public int getStrength()	
		{ return strength; }
	public int getDexterity()
		{ return dexterity; }
	public int getIntelligence()
		{ return intelligence; }
	public int getAlignment()
		{ return alignment; }
	public int getCombat()
		{ return combat; }
	public int getArmor()
		{ return armor; }
	public int getWeapon()
		{ return weapon; }
	public int getReach()
		{ return reach; }
	public int getEquipOffset()
		{ return equip_offset; }
	public short getHitsfx()
		{ return sfx; }
	public static int getInfoFlag()
		{ return 8; }
	public int getBaseXpValue() {
		// This formula is exact.
		int expval = armor + weapon;
		expval += m_sleep_safe ? 1 : 0;
		expval += m_charm_safe ? 1 : 0;
		expval += m_curse_safe ? 1 : 0;
		expval += m_paralysis_safe ? 1 : 0;
		expval += m_poison_safe ? 1 : 0;
			// Don't know what this does, but it *does* add to XP.
		expval += m_int_b1 ? 1 : 0;
			// This prevents death from Death Bolt.
		expval += m_death_safe ? 1 : 0;
		expval += m_power_safe ? 8 : 0;
		expval += (flags & (1 << fly)) != 0 ? 1 : 0;
		expval += (flags & (1 << swim)) != 0 ? 1 : 0;
		expval += (flags & (1 << ethereal)) != 0 ? 2 : 0;
		expval += (flags & (1 << 5)) != 0 ? 2 : 0;	// No idea what this does.
		expval += (flags & (1 << see_invisible)) != 0 ? 2 : 0;
		expval += (flags & (1 << start_invisible)) != 0 ? 8 : 0;
		expval += m_splits ? 2 : 0;
		expval += reach > 5 ? 2 : (reach > 3 ? 1 : 0);
		return expval;
	}
	
	public static final class EquipElement {
		public int shapenum;	// What to create, or 0 for nothing.
		public int probability;	// 0-100:  probability of creation.
		public int quantity;		// # to create.
		public EquipElement(int shnum, int prob, int quant) {
			shapenum = shnum;
			probability = prob;
			quantity = quant;
		}
		public int getShapenum()
			{ return shapenum; }
		public int getProbability()
			{ return probability; }
		public int getQuantity()
			{ return quantity; }
	}
	public static final class EquipRecord {
		private EquipElement elements[];
		public EquipRecord() {
			elements = new EquipElement[10];
		}
		public int getNumElements() {
			return elements.length;
		}
							// Set i'th element.
		public void set(int i, int shnum, int prob, int quant)
			{ elements[i] = new EquipElement(shnum, prob, quant); }
		public EquipElement get(int i)
			{ return elements[i]; }
	}
}
