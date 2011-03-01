package com.exult.android;
import com.exult.android.shapeinf.*;
import java.util.HashSet;

public class MonsterActor extends NpcActor {
	private static HashSet<MonsterActor> inWorld =	// All monsters in the world.
		new HashSet<MonsterActor>();
	Animator animator;		// For wounded men.
	private void equip(MonsterInfo inf, boolean temporary) {
		// Get equipment.
		int equip_offset = inf.getEquipOffset();
		int equip_cnt = MonsterInfo.getEquipCnt();
		if (equip_offset == 0 || equip_offset - 1 >= equip_cnt)
			return;
		MonsterInfo.EquipRecord rec = MonsterInfo.getEquip(equip_offset - 1);
		int elem_cnt = rec.getNumElements();
		for (int i = 0; i < elem_cnt; ++i) {
			 		// Give equipment.
			MonsterInfo.EquipElement elem = rec.get(i);
			if (elem.shapenum == 0 || 1 + EUtil.rand()%100 > elem.probability)
				continue;// You lose.
			int frnum = (elem.shapenum == 377) ? 
				getInfo().getMonsterFood() : 0;
			if (frnum < 0)	// Food.
				frnum = EUtil.rand()%32;
			ShapeInfo einfo = ShapeID.getInfo(elem.shapenum);
			WeaponInfo winfo = einfo.getWeaponInfo();
			if (einfo.hasQuality() && winfo != null && winfo.usesCharges())
				createQuantity(1, elem.shapenum, elem.quantity,
					frnum, temporary);
			else
				createQuantity(elem.quantity,
					elem.shapenum, EConst.c_any_qual, frnum, temporary);
			int ammo = winfo != null ? winfo.getAmmoConsumed() : -1;
			if (ammo >= 0)		// Weapon requires ammo.
				createQuantity(5 + EUtil.rand()%25, ammo, EConst.c_any_qual, 0,
							temporary);
			}
	}
	public MonsterActor(String nm, int shapenum, int num, int uc) {
		super(nm, shapenum, num, uc);				// Check for animated shape.
		ShapeInfo info = getInfo();
		if (info.isAnimated() || info.hasSfx())
			animator = Animator.create(this);
	}
	public static void deleteAll() {
		inWorld.clear();
	}
	public boolean moveAside(Actor forActor, int dir) {
		return false;	// Monsters don't move aside.
	}
	public static HashSet<MonsterActor> getAll() {
		return inWorld;
	}
	/*
	 *	Render.
	 */
	public void paint() {
		// Animate first
		if (animator != null)			// Be sure animation is on.
			animator.wantAnimation();
		super.paint();		// Draw on screen.
	}
	/*
	 *	Step onto an adjacent tile.
	 *
	 *	Output:	0 if blocked.
	 *		Dormant is set if off screen.
	 */
	public boolean step
		(
		Tile t,			// Tile to step onto.
		int frame,			// New frame #.
		boolean force
		) {
		// If move not allowed do I remove or change destination?
		// I'll do nothing for now
		/* ++++++FINISH
		if (!gwin.emulateIsMoveAllowed(t.tx, t.ty))
			return false;
		*/
		if (getFlag(GameObject.paralyzed) || getMap() != gmap)
			return false;
						// Get old chunk.
		MapChunk olist = getChunk();
						// Get chunk.
		int cx = t.tx/EConst.c_tiles_per_chunk, cy = t.ty/EConst.c_tiles_per_chunk;
						// Get .new chunk.
		MapChunk nlist = gmap.getChunk(cx, cy);
						// Blocked?
		if (!areaAvailable(t, null, force ? EConst.MOVE_ALL : 0)) {
			if (schedule != null)		// Tell scheduler.
				schedule.setBlocked(t);
			stop();
			if (!gwin.addDirty(this))
				dormant = true;	// Off-screen.
			return false;		// Done.
		}
						// Check for scrolling.
		gwin.scrollIfNeeded(this, t);
		addDirty(false);			// Set to repaint old area.
						// Move it.
						// Get rel. tile coords.
		int tx = t.tx%EConst.c_tiles_per_chunk, ty = t.ty%EConst.c_tiles_per_chunk;
		movef(olist, nlist, tx, ty, frame, t.tz);
		if (!addDirty(true) &&
						// And > a screenful away?
		    distance(gwin.getCameraActor()) > 1 + EConst.c_screen_tile_size) {
						// No longer on screen.
			stop();
			dormant = true;
			return false;
		}
		// ++++++FINISH quake_on_walk();
		return true;			// Add back to queue for next time.
	}
	/*
	 *	Remove an object from its container, or from the world.
	 *	The object is deleted.
	 */
	public void removeThis() {
		inWorld.remove(this);			// Remove from list.
		super.removeThis();
	}
	/*
	 *	Move (teleport) to a new spot.
	 */
	public void move(int newtx, int newty, int newlift, int newmap) {
		super.move(newtx, newty, newlift, newmap);
		inWorld.add(this);			// Insure it's in global list.
	}
	/*
	 *	Add an object.
	 *
	 *	Output:	1, meaning object is completely contained in this,
	 *		0 if not enough space.
	 */
	public boolean add
		(
		GameObject obj,
		boolean dont_check,			// 1 to skip volume check.
		boolean combine,			// True to try to combine obj.  MAY
								//   cause obj to be deleted.
		boolean noset		// True to prevent actors from setting sched. weapon.
		) {
						// Try to add to 'readied' spot.
		if (super.add(obj, true, combine, noset))
			return (true);		// Successful.
						// Just add anything.
		return super.add(obj, true, combine, false);
	}
	/*
	 *	Create an instance of a monster.
	 */
	public static MonsterActor create(int shnum) {
		// Get usecode for shape.
		int ucnum = ucmachine.getShapeFun(shnum);
		/* +++++++FINISH
		if (shnum == 529)		// Slime?
			return new Slime_actor("", shnum, -1, ucnum);
		else */
			return new MonsterActor("", shnum, -1, ucnum);
	}
	/*
	 *	Create an instance of a monster and initialize from monstinf.dat.
	 */
	public static MonsterActor create(int shnum, Tile pos, int sched, int align) {
		return create(shnum, pos, sched, align, true, true);
	}
	public static MonsterActor create(int shnum, Tile pos) {
		return create(shnum, pos, -1, Actor.neutral, true, true);
	}
	public static MonsterActor create
		(
		int shnum,			// Shape to use.
		Tile pos,			// Where to place it.  If pos is null or pos.tx < 0,
							//   it's not placed in the world.
		int sched,			// Schedule type.
		int align,			// Alignment.
		boolean temporary,
		boolean equipment
		) {
						// Get 'monsters.dat' info.
		MonsterInfo inf = ShapeID.getInfo(shnum).getMonsterInfo();
		if (inf == null)
			inf = MonsterInfo.getDefault();
		MonsterActor monster = create(shnum);
		monster.setAlignment(align == Actor.neutral
							? inf.getAlignment() : align);
		// Movement flags
		int mflags = inf.getFlags();
		if (((mflags >> MonsterInfo.fly)&1) != 0)
			monster.setTypeFlag(Actor.tf_fly);

		if (((mflags >> MonsterInfo.swim)&1) != 0)
			monster.setTypeFlag(Actor.tf_swim);

		if (((mflags >> MonsterInfo.walk)&1) != 0)
			monster.setTypeFlag(Actor.tf_walk);

		if (((mflags >> MonsterInfo.ethereal)&1) != 0)
			monster.setTypeFlag(Actor.tf_ethereal);

		if (((mflags >> MonsterInfo.start_invisible)&1) != 0)
			monster.setFlag(GameObject.invisible);

		int str = randomizeInitialStat(inf.getStrength());
		monster.setProperty(Actor.strength, str);
						// Max. health = strength.
		monster.setProperty(Actor.health, str);
		monster.setProperty(Actor.dexterity,
				randomizeInitialStat(inf.getDexterity()));
		monster.setProperty(Actor.intelligence,
				randomizeInitialStat(inf.getIntelligence()));
		monster.setProperty(Actor.combat,
				randomizeInitialStat(inf.getCombat()));

		final short monster_mode_odds[] = {
			20, 45, 70, 100,		// These are slightly off, but
			50, 100, 0, 0,		// are good enough that no one
			35, 70, 100, 0,		// will notice the difference
			35, 55, 70, 100,		// without serious statistics.
			50, 100, 0, 0};
		final short monster_modes[] = {
			nearest, random, flee, nearest,		// noncombatants
			weakest, nearest, nearest, nearest,	// opportunists
			nearest, random, nearest, nearest,	// unpredictable
			flank, defend, weakest, strongest,	// tacticians
			berserk, nearest, nearest, nearest};	// berserkers

		int prob = EUtil.rand()%100;
		int i;
		for (i = 0; i < 3; i++)
			if (prob < monster_mode_odds[inf.getAttackmode()*4 + i])
				break;
		monster.setAttackMode((int) monster_modes[inf.getAttackmode()*4 + i], false);

		// Set temporary
		if (temporary) 
			monster.setFlag (GameObject.is_temporary);
		monster.setInvalid();		// Place in world.
		if (pos != null && pos.tx >= 0)
			monster.move(pos.tx, pos.ty, pos.tz);
		if (equipment) {
			monster.equip(inf, temporary);	// Get equipment.
			if (sched == Schedule.combat)
				monster.readyBestWeapon();
		}
		if (sched < 0)			// Set sched. AFTER equipping.
			sched = Schedule.loiter;
		monster.setScheduleType(sched);
		return (monster);
	}
	private static int randomizeInitialStat(int val) {
		if (val > 7)
			return val + EUtil.rand()%5 + EUtil.rand()%5 -4;
		else if (val > 0)
			return EUtil.rand()%val + EUtil.rand()%val + 1;
		else
			return 1;
	}
}
