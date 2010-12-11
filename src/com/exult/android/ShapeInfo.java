package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.TreeMap;

public class ShapeInfo {
	private byte tfa[] = new byte[3];
	// 3D dimensions in tiles:
	private byte dims[] = new byte[3];		//   (x, y, z)
	private byte weight, volume;	// From "wgtvol.dat".
	private byte shpdims[] = new byte[2];	// From "shpdims.dat".
	private byte weaponOffsets[];	// From "wihh.dat": pixel offsets
	/*
	//   for drawing weapon in hand
	private ArmorInfo armor;		// From armor.dat.
	private WeaponInfo weapon;		// From weapon.dat, if a weapon.
	private AmmoInfo ammo;		// From ammo.dat, if ammo.
	private MonsterInfo monstinf;		// From monster.dat.
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
	/*
	public int getArmor() {
		return armor != null ? armor.prot : 0;
	}
	public int getArmorImmunity() {
		return armor != null? armor.immune : 0;
	}
	public int getExplosionSprite() {
		return explosion != null ? explosion.sprite : 5;
	}
	int getExplosionSfx() {
		return explosion != null ? explosion.sfxnum : -1;
	}

int get_body_shape() {;
int get_body_frame() {;

bool has_weapon_info() {
{ return weapon != 0; }
Weapon_info *get_weapon_info_safe() {;
Weapon_info *get_weapon_info() {
{ return weapon; }
Weapon_info *set_weapon_info(bool tf);

bool has_ammo_info() {
{ return ammo != 0; }
Ammo_info *get_ammo_info_safe() {;
Ammo_info *get_ammo_info() {
{ return ammo; }
Ammo_info *set_ammo_info(bool tf);

bool has_armor_info() {
{ return armor != 0; }
Armor_info *get_armor_info() {
{ return armor; }
Armor_info *set_armor_info(bool tf);

bool has_monster_info() {
{ return monstinf != 0; }
Monster_info *get_monster_info_safe() {;
Monster_info *get_monster_info() {
{ return monstinf; }
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
bool is_object_allowed(int frame, int spot)
{ return get_item_paperdoll(frame, spot) != 0; }

bool has_content_rules() {;
private vector<Content_rules>& get_content_rules()
{ return cntrules; }
private vector<Content_rules>& set_content_rules(bool tf);
void clean_invalid_content_rules();
void clear_content_rules();
void add_content_rule(Content_rules& add);
bool is_shape_accepted(int shape);

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

int get_monster_food() {
{ return monster_food; }
void set_monster_food(int sh)
{
if (monster_food != (short)sh)
{
modified_flags |= monster_food_flag;
monster_food = (short)sh;
}
}

int get_mountain_top_type() {
{ return mountain_top; }
void set_mountain_top(int sh)
{
if (mountain_top != (unsigned char)sh)
{
modified_flags |= mountain_top_flag;
mountain_top = (unsigned char)sh;
}
}

int get_barge_type() {
{ return barge_type; }
void set_barge_type(int sh)
{
if (barge_type != (unsigned char)sh)
{
modified_flags |= barge_type_flag;
barge_type = (unsigned char)sh;
}
}

int get_field_type() {
{ return field_type; }
void set_field_type(int sh)
{
if (field_type != (char)sh)
{
modified_flags |= field_type_flag;
field_type = (char)sh;
}
}

int get_gump_shape() {
{ return gump_shape; }
int get_gump_font() {
{ return gump_font; }
void set_gump_data(int sh, int fnt)
{
if (gump_shape != (short)sh || gump_font != (short)sh)
{
modified_flags |= gump_shape_flag;
gump_shape = (short) sh;
gump_font = (short) fnt;
}
}
*/
	public short getShapeFlags() {
		return shapeFlags; }
	/*
void set_shape_flags(unsigned short flags)
{
if (shape_flags != flags)
{
int diff = (shape_flags ^ flags) * usecode_events_flag;
modified_flags |= diff;
shape_flags = flags;
}
}
*/
	public boolean getShapeFlag(int tf) { 
		return (shapeFlags & (1 << tf)) != 0;
	}
/*
void set_shape_flag(int tf, int mod)
{
if (!(shape_flags & (1U << tf)))
{
modified_flags |= (1U << mod);
shape_flags |= (1U << tf);
}
}
void clear_shape_flag(int tf, int mod)
{
if (shape_flags & (1U << tf))
{
modified_flags |= (1U << mod);
shape_flags &= ~(1U << tf);
}
}
*/
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
	public static void read(int num_shapes, ShapeInfo info[]) {
		int i, cnt;
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
		/*
		 * 	CLOSE all the files.
		 */
		try {
			shpdims.close();
			wgtvol.close();
			tfa.close();
		} catch (IOException e) { }
		
		//++++++++++LOTS MORE
	}
}
