package com.exult.android;
import com.exult.android.shapeinf.*;
import java.io.RandomAccessFile;
import java.io.InputStream;
import java.io.IOException;

public final class ShapeInfo {
	private byte tfa[] = new byte[3];
	// 3D dimensions in tiles:
	private byte dims[] = new byte[3];		//   (x, y, z)
	private byte weight, volume;	// From "wgtvol.dat".
	private byte shpdims[] = new byte[2];	// From "shpdims.dat".
	private byte weaponOffsets[];	// From "wihh.dat": pixel offsets
	//   for drawing weapon in hand
	private ArmorInfo armor;		// From armor.dat.
	private WeaponInfo weapon;		// From weapon.dat, if a weapon.
	private AmmoInfo ammo;		// From ammo.dat, if ammo.
	private MonsterInfo monstinf;		// From monster.dat.
	/*
	private SFXInfo sfxinf;
	private AnimationInfo aniinf;
	private ExplosionInfo explosion;
	private BodyInfo body;
	private PaperdollNpc npcpaperdoll;
	// These vectors should be totally ordered by the strict-weak
	// order operator defined for the classes.
	private vector<Paperdoll_item> objpaperdoll;
	private vector<Effective_hp_info> hpinf;
	private vector<Frame_name_info> nameinf;
	private vector<Frame_flags_info> frflagsinf;
	private vector<Frame_usecode_info> frucinf;
	private vector<Warmth_info> warminf;
	private vector<Content_rules> cntrules;
	*/
	private short gumpShape;		// From container.dat.
	private short gumpFont;		// From container.dat v2+.
	private short monsterFood;
	private short shapeFlags;
	private byte mountainFop;
	private byte bargeType;
	private byte actorFlags;
	private byte fieldType;
	private byte readyType;	// From "ready.dat": where item can be worn.
	private byte altReady1;	// Alternate spot where item can be worn.
	private byte altReady2;	// Second alternate spot where item can be worn.
	private boolean spellFlag;		// Flagged as epsll in 'ready.dat'.
	boolean occludesFlag;		// Flagged in 'occlude.dat'.  Roof.
	private void setTfaData() {	// Set fields from tfa.
		dims[0] = (byte)(1 + (tfa[2]&7));
		dims[1] = (byte)(1 + ((tfa[2]>>3)&7));
		dims[2] = (byte)((tfa[0] >> 5)&7);
	}
	// Set/clear tfa bit.
	private void setTfa(int i, int bit, boolean tf) {
		tfa[i] = (byte)(tf ?
				(tfa[i]|(1<<bit)) : (tfa[i]&~(1<<bit))); 
	}
	// Enum Actor_flags
	public static final int cold_immune = 0;
	public static final int doesnt_eat = 1;
	public static final int teleports = 2;
	public static final int summons = 3;
	public static final int turns_invisible = 4;
	public static final int armageddon_safe = 5;
	public static final int quake_walk = 6;
	// Enum Shape_flags
	public static final int usecode_events = 0;
	public static final int is_body = 1;
	public static final int lightweight = 2;
	public static final int quantity_frames = 3;
	public static final int locked = 4;
	public static final int is_volatile = 5;
	public static final int jawbone = 6;
	public static final int mirror = 7;

	// Enum Mountain_tops
	public static final int not_mountain_top = 0;
	public static final int normal_mountain_top = 1;
	public static final int snow_mountain_top = 2;

	// Enum Barge_types
	public static final int barge_generic = 0;
	public static final int barge_raft = 1;
	public static final int barge_seat = 2;
	public static final int barge_sails = 3;
	public static final int barge_wheel = 4;
	public static final int barge_draftanimal = 5;
	public static final int barge_turtle = 6;

	// Enum Field_types
	public static final int no_field = -1;
	public static final int fire_field = 0;
	public static final int sleep_field = 1;
	public static final int poison_field = 2;
	public static final int caltrops = 3;

	public ShapeInfo() {
		gumpShape = -1; gumpFont = -1; monsterFood = -1;
		fieldType = -1;
		readyType = -1; altReady1 = -1; altReady2 = -1; 
		tfa[0] = tfa[1] = tfa[2] = shpdims[0] = shpdims[1] = 0;
		dims[0] = dims[1] = dims[2] = 0;
	}
	public int getWeight() {		// Get weight, volume.
		return weight; 
	}
	public int getVolume() { 
		return volume; 
	}
	public int getArmor() {
		return armor != null ? armor.getProt() : 0;
	}
	public int getArmorImmunity() {
		return armor != null? armor.getImmune() : 0;
	}
	/*
	public int getExplosionSprite() {
		return explosion != null ? explosion.sprite : 5;
	}
	int getExplosionSfx() {
		return explosion != null ? explosion.sfxnum : -1;
	}

int get_body_shape() {;
int get_body_frame() {;
*/
	public boolean hasWeaponInfo() {
		return weapon != null; 
	}
	public WeaponInfo getWeaponInfo()
		{ return weapon; }
	public void setWeaponInfo(WeaponInfo i) {
		weapon = i;
	}
	boolean hasAmmoInfo() 
		{ return ammo != null; }
	public AmmoInfo getAmmoInfo() {
		return ammo; 
	}
	public void setAmmoInfo(AmmoInfo i) {
		ammo = i;
	}
	public boolean hasArmorInfo() {
		return armor != null;
	}
	public ArmorInfo getArmorInfo()
		{ return armor; }
	public void setArmorInfo(ArmorInfo i) {
		armor = i;
	}
/*++++++++++FINISH

Armor_info *get_armor_info() {
{ return armor; }
Armor_info *set_armor_info(bool tf);

bool has_monster_info() {
{ return monstinf != 0; }
	public MonsterInfo getMonsterInfoafe()
*/
	public MonsterInfo getMonsterInfo() {
		return monstinf; 
	}
	public void setMonsterInfo(MonsterInfo m) {
		monstinf = m;
	}
/*
Monster_info *set_monster_info(bool tf);

bool has_npc_paperdoll_info() {
{ return npcpaperdoll != 0; }
Paperdoll_npc *get_npc_paperdoll() {
{ return npcpaperdoll; }
Paperdoll_npc *set_npc_paperdoll_info(bool tf);
Paperdoll_npc *get_npc_paperdoll_safe(bool sex) {;

bool has_sfx_info() {
{ return sfxinf != 0; }
SFX_info *get_sfx_info() {
{ return sfxinf; }
SFX_info *set_sfx_info(bool tf);

bool has_explosion_info() {
{ return explosion != 0; }
Explosion_info *get_explosion_info() {
{ return explosion; }
Explosion_info *set_explosion_info(bool tf);

bool has_animation_info() {
{ return aniinf != 0; }
Animation_info *get_animation_info() {
{ return aniinf; }
Animation_info *get_animation_info_safe(int shnum, int nframes);
Animation_info *set_animation_info(bool tf);

bool has_body_info() {
{ return body != 0; }
Body_info *get_body_info() {
{ return body; }
Body_info *set_body_info(bool tf);

bool has_paperdoll_info() {;
private vector<Paperdoll_item>& get_paperdoll_info()
{ return objpaperdoll; }
private vector<Paperdoll_item>& set_paperdoll_info(bool tf);
void clean_invalid_paperdolls();
void clear_paperdoll_info();
void add_paperdoll_info(Paperdoll_item& add);
Paperdoll_item *get_item_paperdoll(int frame, int spot);
*/
	public final boolean isObjectAllowed(int frame, int spot) {
		return true;// +++++FINISHreturn get_item_paperdoll(frame, spot) != 0; 
	}
/*
bool has_content_rules() {;
private vector<Content_rules>& get_content_rules()
{ return cntrules; }
private vector<Content_rules>& set_content_rules(bool tf);
void clean_invalid_content_rules();
void clear_content_rules();
void add_content_rule(Content_rules& add);
*/
	public boolean isShapeAccepted(int shape) {
		return true;//+++++++FINISH
	}
/*
bool has_effective_hp_info() {;
private vector<Effective_hp_info>& get_effective_hp_info()
{ return hpinf; }
private vector<Effective_hp_info>& set_effective_hp_info(bool tf);
void clean_invalid_hp_info();
void clear_effective_hp_info();
void add_effective_hp_info(Effective_hp_info& add);
int get_effective_hps(int frame, int quality);

bool has_frame_name_info() {;
private vector<Frame_name_info>& get_frame_name_info()
{ return nameinf; }
private vector<Frame_name_info>& set_frame_name_info(bool tf);
void clean_invalid_name_info();
void clear_frame_name_info();
void add_frame_name_info(Frame_name_info& add);
Frame_name_info *get_frame_name(int frame, int quality);

bool has_frame_usecode_info() {;
private vector<Frame_usecode_info>& get_frame_usecode_info()
{ return frucinf; }
private vector<Frame_usecode_info>& set_frame_usecode_info(bool tf);
void clean_invalid_usecode_info();
void clear_frame_usecode_info();
void add_frame_usecode_info(Frame_usecode_info& add);
Frame_usecode_info *get_frame_usecode(int frame, int quality);
/*
bool has_frame_flags() {;
private vector<Frame_flags_info>& get_frame_flags()
{ return frflagsinf; }
private vector<Frame_flags_info>& set_frame_flags(bool tf);
void clean_invalid_frame_flags();
void clear_frame_flags();
void add_frame_flags(Frame_flags_info& add);
int get_object_flags(int frame, int qual);
int has_object_flag(int frame, int qual, int p)
{ return (get_object_flags(frame, qual)&(1 << p)) != 0; }

bool has_warmth_info() {;
private vector<Warmth_info>& get_warmth_info()
{ return warminf; }
private vector<Warmth_info>& set_warmth_info(bool tf);
void clean_invalid_warmth_info();
void clear_warmth_info();
void add_warmth_info(Warmth_info& add);
int get_object_warmth(int frame);
*/
	public int getMonsterFood() {
		return monsterFood; 
	}
/*

int get_mountain_top_type() {
{ return mountain_top; }

int get_barge_type() {
{ return barge_type; }

int get_field_type() {
{ return field_type; }
*/
	public int getGumpShape() { 
		return gumpShape;
	}
	public int getGumpFont() {
		return gumpFont; }
	public short getShapeFlags() {
		return shapeFlags; }
	public boolean getShapeFlag(int tf) { 
		return (shapeFlags & (1 << tf)) != 0;
	}
	public final boolean hasUsecodeEvents() {
		return getShapeFlag(usecode_events); }
	public final boolean isBodyShape() {
		return getShapeFlag(is_body); }
	public final boolean isLightweight() {
		return getShapeFlag(lightweight); }
	public final boolean hasQuantityFrames() {
		return getShapeFlag(quantity_frames); }
	public final boolean isContainerLocked() {
		return getShapeFlag(locked); }
	public final boolean isExplosive() {
		return getShapeFlag(is_volatile); }
	public final boolean isJawbone() {
		return getShapeFlag(jawbone); }
	public final boolean isMirror() {
		return getShapeFlag(mirror); }
/*
unsigned byte get_actor_flags() {
{ return actor_flags; }
void set_actor_flags(byte flags)
{
if (actor_flags != flags)
{
modified_flags |= actor_flags_flag;
actor_flags = flags;
}
}
bool get_actor_flag(int tf) {
{ return (actor_flags & (1 << tf)) != 0; }
void set_actor_flag(int tf)
{
if (!(actor_flags & (1 << tf)))
{
modified_flags |= actor_flags_flag;
actor_flags |= (1U << tf);
}
}
void clear_actor_flag(int tf)
{
if (actor_flags & (1 << tf))
{
modified_flags |= actor_flags_flag;
actor_flags &= ~(1U << tf);
}
}

bool is_cold_immune() {
{ return get_actor_flag(cold_immune); }
bool does_not_eat() {
{ return get_actor_flag(doesnt_eat); }
bool can_teleport() {
{ return get_actor_flag(teleports); }
bool can_summon() {
{ return get_actor_flag(summons); }
bool can_be_invisible() {
{ return get_actor_flag(turns_invisible); }
bool survives_armageddon() {
{ return get_actor_flag(armageddon_safe); }
bool quake_on_walk() {
{ return get_actor_flag(quake_walk); }
*/
	// Get tile dims., flipped for
	//   reflected (bit 5) frames.
	public int get3dXtiles(int framenum)
		{ return dims[(framenum >> 5)&1]; }
	public int get3dYtiles(int framenum)
		{ return dims[1 ^ ((framenum >> 5)&1)]; }
	public int get3dHeight()		// Height (in lifts?).
		{ return dims[2]; }
	public boolean hasSfx()			// Has a sound effect (guessing).
		{ return (tfa[0] & (1<<0)) != 0; }
	public boolean hasStrangeMovement()	// Slimes, sea monsters.
		{ return (tfa[0] & (1<<1)) != 0; }
	public boolean isAnimated()
		{ return (tfa[0] & (1<<2)) != 0; }
	public boolean isSolid()			// Guessing.  Means can't walk through.
		{ return (tfa[0] & (1<<3)) != 0; }
	public boolean isWater()			// Guessing.
		{ return (tfa[0] & (1<<4)) != 0; }
	public boolean isPoisonous()		// Swamps.  Applies to tiles.
		{ return (tfa[1] & (1<<4)) != 0; }
	public boolean isField()			// Applies to Game_objects??
		{ return (tfa[1] & (1<<4)) != 0; }
	public boolean isDoor()
		{ return (tfa[1] & (1<<5)) != 0; }
	public boolean isBargePart()
		{ return (tfa[1] & (1<<6)) != 0; }
	public boolean isTransparent()		// ??
		{ return (tfa[1] & (1<<7)) != 0; }
	public boolean isLightSource()
		{ return (tfa[2] & (1<<6)) != 0; }
	public boolean hasTranslucency()
		{ return (tfa[2] & (1<<7)) != 0; }

	public boolean isXobstacle()		// Obstacle in x-dir.???
		{ return (shpdims[1] & 1) != 0; }
	public boolean isYobstacle()		// Obstacle in y-dir.???
		{ return (shpdims[0] & 1) != 0; }
	/*
	 *	TFA[1][b0-b3] seems to indicate object types:
	 */
	// Enum Shape_class 
	public static final int unusable = 0;		// Trees.
	public static final int quality = 2;
	public static final int quantity = 3;		// Can have more than 1:  coins, arrs.
	public static final int has_hp = 4;	    // Breakable items (if hp != 0, that is)
	public static final int quality_flags = 5;	// Item quality is set of flags:
	// Bit 3 = okay-to-take.
	public static final int container = 6;
	public static final int hatchable = 7;		// Eggs, traps, moongates.
	public static final int spellbook = 8;
	public static final int barge = 9;
	public static final int virtue_stone = 11;
	public static final int monster = 12;		// Non-human's.
	public static final int human = 13;		// Human NPC's.
	public static final int building = 14;		// Roof, window, mountain.

	public int getShapeClass()
		{ return (int) (tfa[1]&15); }
	public boolean isNpc() {
		int c = getShapeClass();
		return c == human || c == monster;
	}
	public boolean hasQuantity()
		{ return getShapeClass() == quantity; }
	public boolean hasQualityFlags()	// Might be more...
		{ return getShapeClass() == quality_flags; }
	public boolean hasQuality() {
		int c = getShapeClass();
		return (c == 2 || c == 6 || c == 7 || c == 11 || c == 12 || c == 13);
	}
	public boolean occludes()
		{ return occludesFlag; }
	public byte getReadyType()
		{ return readyType; }
	public boolean isSpell()
		{ return spellFlag; }
	public byte getAltReady1()
		{ return altReady1; }
	public byte getAltReady2()
		{ return altReady2; }

// Returns x<<8 + y, as bytes.
// Sets x to 255 if there is no weapon offset
int get_weapon_offset(int frame)
{
	int x, y;
	if (weaponOffsets == null) {
		x = y = 255;
	} else {
		// x could be 255 (see read_info())
		x = weaponOffsets[frame * 2]&0xff;
		y = weaponOffsets[frame * 2 + 1]&0xff;
	}
	return (x<<8)|y;
	}
	public int getRotatedFrame(int curframe, int quads) {
		// Seat is a special case.
		if (bargeType == barge_seat) {
			int dir = curframe%4;	// Current dir (0-3).
			return (curframe - dir) + (dir + quads)%4;
		} else if (isBargePart())		// Piece of a barge?
			switch (quads) {
			case 1:
				return (curframe^32)^((curframe&32) != 0 ? 3 : 1);
			case 2:
				return curframe^2;
			case 3:
				return (curframe^32)^((curframe&32) != 0 ? 1 : 3);
			default:
				return curframe;
				}
		else
						// Reflect.  Bit 32==horizontal.
			return curframe ^ ((quads%2)<<5);
	}
	public static void read(int num_shapes, ShapeInfo info[], int game) {
		int i;
		ShapeInfo s;
		// ShapeDims

		// Starts at 0x96'th shape.
		RandomAccessFile shpdims = EUtil.U7open2(EFile.PATCH_SHPDIMS, EFile.SHPDIMS);
		if (shpdims != null) try {
			for (i = EConst.c_first_obj_shape; i < num_shapes; i++) {
				s = info[i];
				if (s == null)
					info[i] = s = new ShapeInfo();
				s.shpdims[0] = shpdims.readByte();
				s.shpdims[1] = shpdims.readByte();
			}
		} catch (IOException e) { }
		// WGTVOL
		RandomAccessFile wgtvol = EUtil.U7open2(EFile.PATCH_WGTVOL, EFile.WGTVOL);
		if (wgtvol != null) try {
			for (i = 0; i < num_shapes; i++) {
				s = info[i];
				if (s == null)
					info[i] = s = new ShapeInfo();
				s.weight = wgtvol.readByte();
				s.volume = wgtvol.readByte();
			}
		} catch (IOException e) { }
		
		// TFA
		RandomAccessFile tfa = EUtil.U7open2(EFile.PATCH_TFA, EFile.TFA);
		if (tfa != null) try {
			for (i = 0; i < num_shapes; i++) {
				s = info[i];
				if (s == null)
					info[i] = s = new ShapeInfo();
				tfa.read(s.tfa);
				s.setTfaData();
			}
		} catch (IOException e) { }
		// Get 'equip.dat'.
		InputStream equip = EUtil.U7openStream2(EFile.PATCH_EQUIP, EFile.EQUIP);
		if (equip != null) {
				// Get # entries (with Exult extension).
			int num_recs = DataUtils.ReadCount(equip);
			MonsterInfo.reserveEquip(num_recs);
			for (i = 0; i < num_recs; i++) {
				MonsterInfo.EquipRecord erec = new MonsterInfo.EquipRecord();
						// 10 elements/record.
				for (int elem = 0; elem < 10; elem++) {
					int shnum = EUtil.Read2(equip);
					int prob = EUtil.Read1(equip);
					int quant = EUtil.Read1(equip);
					EUtil.Read2(equip);
					erec.set(elem, shnum, prob, quant);
					}
				MonsterInfo.addEquip(erec);
			}
		} 
		/*
		 * 	CLOSE all the files.
		 */
		try {
			shpdims.close();
			wgtvol.close();
			tfa.close();
			equip.close();
		} catch (IOException e) { }
		
		//++++++++++LOTS MORE
		DataUtils.IDReaderFunctor idReader = new DataUtils.IDReaderFunctor();
		DataUtils.FunctorMultidataReader armorinf = 
			new DataUtils.FunctorMultidataReader(
					info, new ArmorInfo(), null, idReader, false);
		armorinf.read(EFile.ARMOR, false, game);
		armorinf.read(EFile.PATCH_ARMOR, true, game);
		
		DataUtils.FunctorMultidataReader weaponinf = 
			new DataUtils.FunctorMultidataReader(
					info, new WeaponInfo(), null, idReader, false);
		weaponinf.read(EFile.WEAPONS, false, game);
		weaponinf.read(EFile.PATCH_WEAPONS, true, game);
		
		DataUtils.FunctorMultidataReader ammoinf = 
			new DataUtils.FunctorMultidataReader(
					info, new AmmoInfo(), null, idReader, false);
		ammoinf.read(EFile.AMMO, false, game);
		ammoinf.read(EFile.PATCH_AMMO, true, game);
		
		DataUtils.FunctorMultidataReader monstinf = 
			new DataUtils.FunctorMultidataReader(
					info, new MonsterInfo(), null, idReader, false);
		monstinf.read(EFile.MONSTERS, false, game);
		monstinf.read(EFile.PATCH_MONSTERS, true, game);
		
		DataUtils.FunctorMultidataReader gump = 
			new DataUtils.FunctorMultidataReader(
				info, new GumpReaderFunctor(), null, idReader, true);
		if (game == EConst.BLACK_GATE || game == EConst.SERPENT_ISLE)
			gump.read(game, game == EConst.BLACK_GATE
				? EFile.EXULT_BG_FLX_CONTAINER_DAT
				: EFile.EXULT_SI_FLX_CONTAINER_DAT);
		else
			gump.read(EFile.CONTAINER, false, game);
		gump.read(EFile.PATCH_CONTAINER, true, game);

		DataUtils.FunctorMultidataReader ready =
			new DataUtils.FunctorMultidataReader(
					info, new ReadyTypeFunctor(), null, idReader, false);
		ready.read(EFile.READY, false, game);
		ready.read(EFile.PATCH_READY, true, game);
		//+++++Read text files?
		// Ensure valid ready spots for all shapes.
		byte defready = (byte) (game == EConst.BLACK_GATE
								? Ready.backpack : Ready.rhand);
		int cnt = info.length;
		for (i = 0; i < cnt; ++i) {
			if (info[i].readyType < 0)
				info[i].readyType = defready;
		}
	}
	/*
	 * Readers
	 */
	static class GumpReaderFunctor implements DataUtils.ReaderFunctor {
		public boolean read(InputStream in, int version, 
							boolean patch, int game, ShapeInfo info) {
			info.gumpShape = (short) EUtil.Read2(in);
			if (version >= 2)
				info.gumpFont = (short)EUtil.Read2(in);
			else
				info.gumpFont = -1;
			return true;
		}
	}
	// A few custom post-read functors.
	static class ReadyTypeFunctor implements DataUtils.ReaderFunctor {
		public boolean read(InputStream in, int version, 
								boolean patch, int game, ShapeInfo info) {
			info.readyType = (byte)EUtil.Read1(in);
			
			try { in.skip(6); } catch (IOException e) {}
			int ready = info.readyType;
			info.spellFlag = (ready&1) != 0;
			ready >>= 3;
			int spot = game == EConst.BLACK_GATE ? Ready.spotFromBG(ready)
			                               : Ready.spotFromSI(ready);
			info.readyType = (byte)(spot&0xff);
			//System.out.println("readyType = " + info.readyType);
					// Init alternate spots.
			switch (spot) {
			case Ready.lfinger:
				info.altReady1 = Ready.rfinger;
				break;
			case Ready.lhand:
				info.altReady1 = Ready.rhand;
				info.altReady2 = Ready.belt;
				break;
			case Ready.both_hands:
				info.altReady1 = Ready.back_2h;
				break;
			}
			return true;
		}
	}

}
