package com.exult.android;
import com.exult.android.shapeinf.*;
import java.io.RandomAccessFile;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.IOException;
import java.util.Vector;

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
	private SFXInfo sfxinf;
	private AnimationInfo aniinf;
	private ExplosionInfo explosion;
	private BodyInfo body;
	//+++++++++FINISH private PaperdollNpc npcpaperdoll;
	// These vectors should be totally ordered by the strict-weak
	// order operator defined for the classes.
	//+++++++FINISH private vector<Paperdoll_item> objpaperdoll;
	private Vector<EffectiveHpInfo> hpinf;
	private Vector<FrameNameInfo> nameinf;
	private Vector<FrameFlagsInfo> frflagsinf;
	private Vector<FrameUsecodeInfo> frucinf;
	private Vector<WarmthInfo> warminf;
	private Vector<ContentRules> cntrules;
	private short gumpShape;		// From container.dat.
	private short gumpFont;		// From container.dat v2+.
	private short monsterFood;
	private short shapeFlags;
	private byte mountainTop;
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
	public int getExplosionSprite() {
		return explosion != null ? explosion.getSprite() : 5;
	}
	public int getExplosionSfx() {
		return explosion != null ? explosion.getSfx() : -1;
	}
	public int getBodyShape() {
		return body != null ? body.getBodyShape() : 400;
	}
	public int getBodyFrame() {
		return body != null ? body.getBodyFrame() : 3;
	}
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
	public MonsterInfo getMonsterInfo() {
		return monstinf; 
	}
	public MonsterInfo getMonsterInfoSafe() {
		return monstinf != null ? monstinf
				: MonsterInfo.getDefault();
	}
	public void setMonsterInfo(MonsterInfo m) {
		monstinf = m;
	}
/*

bool has_npc_paperdoll_info() {
{ return npcpaperdoll != 0; }
Paperdoll_npc *get_npc_paperdoll() {
{ return npcpaperdoll; }
Paperdoll_npc *get_npc_paperdoll_safe(bool sex) {;
*/
	public boolean hasSfxInfo() {
		return sfxinf != null; 
	}
	public SFXInfo getSfxInfo() {
		return sfxinf; 
	}
	boolean hasExplosionInfo() {
		return explosion != null; 
	}
	public ExplosionInfo getExplosionInfo() {
		return explosion; 
	}
	public boolean hasAnimationInfo() {
		return aniinf != null; 
	}
	public AnimationInfo getAnimationInfo() {
		return aniinf; 
	}
	public AnimationInfo getAnimationInfoSafe(int nframes) {
		if (aniinf == null)
			aniinf = AnimationInfo.createFromTfa(0, nframes);
		return aniinf; 
	}
	public BodyInfo getBodyInfo() {
		return body; 
	}
/*++++++++FINISH
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
	public Vector<ContentRules> getContentRules()
		{ return cntrules; }
	public void setContentRules(Vector<ContentRules> i) {
		cntrules = i;
	}
	public boolean isShapeAccepted(int shape) {
		ContentRules inf = BaseInfo.OneKeyInfo.searchSingleWildCard(cntrules, shape);
		return inf != null && inf.acceptsShape();
	}
	public Vector<EffectiveHpInfo> getEffectiveHpInfo()
		{ return hpinf; }
	public void setEffectiveHpInfo(Vector<EffectiveHpInfo> i) {
		hpinf = i;
	}
	int getEffectiveHps(int frame, int quality) {
		EffectiveHpInfo inf = BaseInfo.FrameInfo.searchDoubleWildCards(hpinf, 
																frame, quality);
		return inf != null ? inf.getHps() : 0;	// Default to indesctructible.
	}
	public Vector<FrameNameInfo> getFrameNameInfo()
		{ return nameinf; }
	public void setFrameNameInfo(Vector<FrameNameInfo> i) {
		nameinf = i;
	}
	public FrameNameInfo getFrameName(int frame, int quality) {
		return BaseInfo.FrameInfo.searchDoubleWildCards(nameinf, frame, quality);
	}
	public Vector<FrameUsecodeInfo> getFrameUsecodeInfo() {
		return frucinf;
	}
	public void setFrameUsecodeInfo(Vector<FrameUsecodeInfo> i) {
		frucinf = i;
	}
	FrameUsecodeInfo getFrameUsecode(int frame, int quality) {
		return BaseInfo.FrameInfo.searchDoubleWildCards(frucinf, frame, quality);
	}
	public Vector<FrameFlagsInfo> getFrameFlagsInfo()
		{ return frflagsinf; }
	public void setFrameFlagsInfo(Vector<FrameFlagsInfo> i) {
		frflagsinf = i;
	}
	public int getObjectFlags(int frame, int qual) {
		FrameFlagsInfo inf = BaseInfo.FrameInfo.searchDoubleWildCards(frflagsinf,
				frame, qual);
		return inf != null ? inf.getFlags() : 0;	// Default to no flags.
	}
	public Vector<WarmthInfo> getWarmthInfo()
		{ return warminf; }
	public void setWarmthInfo(Vector<WarmthInfo> i) {
		warminf = i;
	}
	public int getObjectWarmth(int frame) {
		WarmthInfo inf = BaseInfo.OneKeyInfo.searchSingleWildCard(warminf, frame);
		return inf != null ? inf.getWarmth() : 0;	// Default to no warmth.
	}
	public int getMonsterFood() {
		return monsterFood; 
	}
	public int getMountainTopType()
		{ return mountainTop; }
	public int getBargeType()
		{ return bargeType; }
	public int getFieldType()
		{ return fieldType; }
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
	public final byte getActorFlags() {
		return actorFlags;
	}
	public final boolean getActorFlag(int tf) {
		return (actorFlags & (1 << tf)) != 0;
	}
	public final boolean isColdImmune()
		{ return getActorFlag(cold_immune); }
	public final boolean doesNotEat()
		{ return getActorFlag(doesnt_eat); }
	public final boolean canTeleport()
		{ return getActorFlag(teleports); }
	public final boolean canSummon()
		{ return getActorFlag(summons); }
	public final boolean canBeInvisible()
		{ return getActorFlag(turns_invisible); }
	public final boolean survivesArmageddon()
		{ return getActorFlag(armageddon_safe); }
	public final boolean quakeOnWalk()
		{ return getActorFlag(quake_walk); }
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
	public int getWeaponOffset(int frame) {
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
	//	Read in Exult text data.
	private static void readShapeInfoTextDataFile(ShapeInfo info[], 
						DataUtils.IDReaderFunctor idReader, int game) {
		final String sections[] = {
			"explosions", "shape_sfx", "animation",
			"usecode_events", "mountain_tops", "monster_food", "actor_flags",
			"effective_hps", "lightweight_object", "warmth_data",
			"quantity_frames", "locked_containers", "content_rules",
			"volatile_explosive", "framenames", "altready", "barge_type",
			"frame_powers", "is_jawbone", "is_mirror", "field_type", 
			"frame_usecode" };
		
		final DataUtils.BaseReader readers[] = {
				new DataUtils.FunctorMultidataReader(info, 
						new ExplosionInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new SFXInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new AnimationInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(usecode_events), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new IntReaderFunctor(IntReaderFunctor.mountain_tops), 
								null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new IntReaderFunctor(IntReaderFunctor.monster_food), 
								null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new ActorFlagsFunctor(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new EffectiveHpInfo(), null, idReader, false),		
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(lightweight), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,		
						new WarmthInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(quantity_frames), null, idReader, false),		
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(locked), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
								new ContentRules(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(is_volatile), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new FrameNameInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new AltReadyFunctor(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new IntReaderFunctor(IntReaderFunctor.barge_type), 
								null, idReader, false),		
				new DataUtils.FunctorMultidataReader(info,
						new FrameFlagsInfo(), null, idReader, false),
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(jawbone), null, idReader, false),		
				new DataUtils.FunctorMultidataReader(info, 
						new ShapeFlagsReader(mirror), null, idReader, false),	
				new DataUtils.FunctorMultidataReader(info,
						new IntReaderFunctor(IntReaderFunctor.field_type), 
								null, idReader, false),
				new DataUtils.FunctorMultidataReader(info,
						new FrameUsecodeInfo(), null, idReader, false)
		};
		assert(sections.length == readers.length);
		int flxres = game == EConst.BLACK_GATE ?
				EFile.EXULT_BG_FLX_SHAPE_INFO_TXT : EFile.EXULT_SI_FLX_SHAPE_INFO_TXT;
		try {
			DataUtils.readTextDataFile("shape_info", readers, sections, game, flxres);
		} catch (IOException e) {
			ExultActivity.fatal("Failed to read \"shape_info\" data");
		}
	}
	public static void read(VgaFile vgafile, ShapeInfo info[], int game) {
		int i;
		int num_shapes = vgafile.getNumShapes();
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
		if (game == EConst.BLACK_GATE || game == EConst.SERPENT_ISLE) try {
			// Animation data at the end of BG and SI TFA.DAT
			tfa.seek(3*1024);
			byte buf [] = new byte[512];
			tfa.read(buf);
			int ind = 0;
			for (i = 0; i < 512; i++, ind++) {
				int val = (int)buf[ind]&0xff;
				int shape = 2*i;
				while (val != 0) {
					if ((val&0xf) != 0) {
						//System.out.println("Anim. data for shape " + shape + " is " + (val&0xf));
						info[shape].aniinf = AnimationInfo.createFromTfa(
								val&0xf, vgafile.getNumFrames(shape));
					}
					val >>= 4;
					shape++;
				}
			}
		} catch (IOException e) { }

		// Load data about drawing the weapon in an actor's hand
		RandomAccessFile wihh = EUtil.U7open2(EFile.PATCH_WIHH, EFile.WIHH);
		
		if (wihh != null) {
			int offsets[] = new int[EConst.c_max_shapes];
			int cnt = num_shapes;
			for (i = 0; i < cnt; i++)
				offsets[i] = EUtil.Read2(wihh)&0xffff;
			for (i = 0; i < cnt; i++) {
				// A zero offset means there is no record
				if(offsets[i] == 0)
					info[i].weaponOffsets = null;
				else try {
					wihh.seek(offsets[i]);
					// There are two bytes per frame: 64 total
					info[i].weaponOffsets = new byte[64];
					for(int j = 0; j < 32; j++) {
						byte x = (byte)(wihh.read());
						byte y = (byte)(wihh.read());
						// Set x/y to 255 if weapon is not to be drawn
						// In the file x/y are either 64 or 255:
						// I am assuming that they mean the same
						if(x > 63 || y > 63)
							x = y = (byte)255;
						info[i].weaponOffsets[j * 2] = x;
						info[i].weaponOffsets[j * 2 + 1] = y;
					}
				} catch (IOException e) {
					ExultActivity.fileFatal(EFile.WIHH);
				}
			}
		}
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
			wihh.close();
		} catch (IOException e) { }
		
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
		// Text files.
		readShapeInfoTextDataFile(info, idReader, game);
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
	static class IntReaderFunctor implements DataUtils.ReaderFunctor {
		static final int mountain_tops = 0, monster_food = 1, barge_type = 2,
						 field_type = 3;
		int type;
		IntReaderFunctor(int ty) {
			type = ty;
		}
		public boolean read(InputStream in, int version, 
							boolean patch, int game, ShapeInfo info) {
			PushbackInputStream txtin = (PushbackInputStream)in;
			int val = EUtil.ReadInt(txtin);
			switch (type) {
			case mountain_tops:		info.mountainTop = (byte)val; break;
			case monster_food:	info.monsterFood = (short)val; break;
			case barge_type:	info.bargeType = (byte)val; break;
			case field_type:	info.fieldType = (byte)val; break;
			}
			return true;
		}
	}
	static class AltReadyFunctor implements DataUtils.ReaderFunctor {
		public boolean read(InputStream in, int version, 
				boolean patch, int game, ShapeInfo info) {
			PushbackInputStream txtin = (PushbackInputStream)in;
			info.altReady1 = (byte)EUtil.ReadInt(txtin);
			info.altReady2 = (byte)EUtil.ReadInt(txtin);
			return true;
		}
	}
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
	// Read in one flag in 'shape_flags'
	static class ShapeFlagsReader implements DataUtils.ReaderFunctor {
		int bit;
		ShapeFlagsReader(int b) {
			bit = b;
		}
		public boolean read(InputStream in, int version, 
						boolean patch, int game, ShapeInfo info) {	
			//System.out.println("ShapesFlagReader");
			boolean biton = EUtil.ReadInt((PushbackInputStream)in, 1) != 0;
			//System.out.println("ShapesFlagReader: read " + biton);
			if (biton)
				info.shapeFlags |= (1 << bit);
			else
				info.shapeFlags &= ~(1 << bit);
			return true;
		}
	}
	static class ActorFlagsFunctor implements DataUtils.ReaderFunctor {
		public boolean read(InputStream in, int version, 
						boolean patch, int game, ShapeInfo info) {	
			info.actorFlags = (byte)DataUtils.readBitFlags((PushbackInputStream)in, 8);
			//System.out.println("ActorFlagsFunctor: " + info.actorFlags);
			// We already have monster data by this point.
			MonsterInfo minf= info.monstinf;
			if (minf != null) {
				// Deprecating old Monster_info based flags for these powers:
				if (minf.canTeleport())
					info.actorFlags |= ShapeInfo.teleports;
				if (minf.canSummon())
					info.actorFlags |= ShapeInfo.summons;
				if (minf.canBeInvisible())
					info.actorFlags |= ShapeInfo.turns_invisible;
			}
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
