package com.exult.android;
import com.exult.android.shapeinf.*;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SpellbookObject extends IregGameObject { 
	public static final int REAGENTS = 842;		// Shape #.
	public static final int NREAGENTS = 11;		// Total # reagents.
	/*
	 *	Flags for required reagents.  Bits match frame #.
	 */
	static final int bp = 1;			// Black pearl.
	static final int bm = 2;			// Blood moss.
	static final int ns = 4;			// Nightshade.
	static final int mr = 8;			// Mandrake root.
	static final int gr = 16;			// Garlic.
	static final int gn = 32;			// Ginseng.
	static final int ss = 64;			// Spider silk.
	static final int sa = 128;			// Sulphuras ash.
	static final int bs = 256;			// Blood spawn.
	static final int sc = 512;			// Serpent scales.
	static final int wh = 1024;			// Worm's hart.
	static short bg_reagents[] = {
		0, 0, 0, 0, 0, 0, 0, 0,		// Linear spells require no reagents.
		// Circle 1:
		gr|gn|mr, gr|gn, ns|ss, gr|ss, sa|ss, sa, ns, gr|gn,
		// Circle 2:
		bm|sa, bp|mr, bp|sa, mr|sa, gr|gn|mr, gr|gn|sa, bp|bm|mr,
				bm|ns|mr|sa|bp|ss,
		// Circle 3:
		gr|ns|sa, gr|gn|ss, ns|mr|bm, sa|gn|gr|mr, ns|ss, ns|mr,
			bp|ns|bm, ns|ss|bp,
		// Circle 4:
		ss|mr, bp|sa|mr, mr|bp|bm, gr|mr|ns|sa, mr|bp|bm, bm|sa,
			bm|mr|ns|ss|sa, bm|sa,
		// Circle 5:
		bp|ns|ss, mr|gr|bm, gr|bp|sa|ss, bm|bp|mr|sa, gn|ss|mr|gr,
			ns|bm, bp|sa|ss, gn|ns|ss,
		// Circle 6:
		gr|mr|ns, sa|ss|bm|gn|ns|mr, bp|mr|ss|sa, sa|bp|bm, mr|ns|sa|bm,
			ns|ss|bp, gn|ss|bp, bm|sa|mr,
		// Circle 7:
		mr|ss, bp|ns|sa, bm|bp|mr|ss|sa, bp|mr|ss|sa, bm|mr|ns|sa,
			bp|ns|ss|mr, bp|gn|mr, gr|gn|mr|ss,
		// Circle 8:
		bp|bm|gr|gn|mr|ns|ss|sa, bm|mr|ns|sa, gr|gn|mr|ns|bm, mr|ns|bm|bp,
		gr|gn|ss|sa, bm|gr|mr, bp|mr|ns, bm|gr|mr
		};
	static short si_reagents[] = {
		// Circle 1:
		gr|gn|mr, gr|gn, ns|ss, gr|ss, sa|ss, sa, ns, bp|bm|mr,
		// Circle 2:
		gr|gn, bm|sa, ns|sa, bp|sa|wh, mr|sa, gr|gn|ss, gr|gn|mr, gr|gn|sa,
		// Circle 3:
		gr|gn|wh,gr|ns|sa, bp|mr, bp|gr, gr|gn|mr|sa, ns|ss, bp|ns|ss, bp|mr|sa|sa,
		// Circle 4:
		bm|mr, gr|ss, mr|sa, sa|bm|gr|mr|ss|sc, gr|mr|ns|sa, bm|sa, bp|ss, bm|sa,
		// Circle 5:
		mr|ss, bp|gr|ss|sa, bm|bp|mr|sa, gr|gn|mr|ss, bm|ns, gn|ns|ss, sa|bm|mr|ns|ss, 
						bp|gr|mr|sa,
		// Circle 6:
		bp|ns|ss, gr|mr|ns, gr|mr|ns, bp|wh|ss|sa, bp|wh|mr|ss|sa, 
						bm|bp|wh|sa, bm|gn|sa, mr|sa|ss|sc,
		// Circle 7:
		bp|mr|ss|sa, bm|mr|ns|sa, gr|gn, bp|gn|mr, bm|ns|sa, gr|gn|mr|ss, 
							bp|bm|mr|ss, bp|mr|sa,
		// Circle 8:
		wh|ss, bs|bp|ns|sa, bm|bp|mr|ss|sa, bm|bp|mr, bm|gr|ss|wh|sc, 
					bm|bp|gr|ss|wh|sc, gr|mr|sa, bp|bs|mr|ns,
		// Circle 9:
		bm|mr|ns|sa, bm|bs|gr|gn|mr|ns, bp|bm|mr|ns, bm|bs|bp|ns|sa, 
				bp|gr|mr|ss|sa, bm|gr|mr|ss, bm|gr|mr, ns|sa|wh|sc|bs
		};
	private short reagents[];	// .appropriate table.
	private byte circles[] = new byte[9];	// Spell-present flags for each circle.
	private int bookmark;			// Spell # that bookmark is on, or -1.
	
					// Create from ireg. data.
	public SpellbookObject(int shapenum, int framenum, int shapex,
						int shapey, int lft, byte c[], byte bmark) {
		super(shapenum, framenum, shapex, shapey, lft);
		bookmark = bmark == 255 ? -1 : bmark;
		System.arraycopy(c, 0, circles, 0, circles.length);
		reagents = game.isSI() ? si_reagents : bg_reagents;
	}
	public final int getBookmark() {
		return bookmark;
	}
	public final void setBookmark(int s) {
		bookmark = s;
	}
	public final int getCircle(int c) {
		return (int)circles[c]&0xffff;
	}
	public final int getReagents(int i) {
		return (int)reagents[i]&0xffff;
	}
	public boolean hasSpell(int spell) {	// Has a spell.
		int circle = spell/8;
		int num = spell%8;		// # within circle.
		return (circles[circle] & (1<<num))!=0;
	}
	public boolean addSpell(int spell) {	// Add a spell.
		int circle = spell/8;
		int num = spell%8;		// # within circle.
		if ((circles[circle] & (1<<num)) != 0)
			return false;		// Already have it.
		circles[circle] |= (1<<num);
		return true;
	}
	public boolean removeSpell(int spell) {	// Remove a spell.
		int circle = spell/8;
		int num = spell%8;		// # within circle.
		if ((circles[circle] & (1<<num)) == 0)
			return false;		// Already does not have it.
		circles[circle] ^= (1<<num);
		return true;
	}
	public void clearSpells() {	// Empties spellbook.
		Arrays.fill(circles, (byte)0);
	}
	public static boolean hasRing(Actor act) {	// Has ring-o-reagents?
		return act.checkGearPowers(FrameFlagsInfo.infinite_reagents);
	}
					// Can we do this spell?
	public boolean canDoSpell(Actor act, int spell) {
		if (cheat.inWizardMode())
			return true;		// Cheating.
		int circle = spell/8;		// Circle spell is in.
		byte cflags = circles[circle];
		if ((cflags & (1<<(spell%8))) == 0)
			return false;		// We don't have that spell.
		int mana = act.getProperty(Actor.mana);
		// ++++TAG: Need to de-hard-code cost.
		int cost = circle + (game.isSI() ? 1 : 0);
		int level = act.getLevel();
		if ((mana < cost) || (level < circle))
				// Not enough mana or not yet at required level?
			return false;
		if (hasRing(act))		// Ring of reagents (SI)?
			return true;
						// Figure what we used.
		int flags = (int)reagents[spell]&0xffff;
						// Go through bits.
		for (int r = 0; flags != 0; r++, flags = flags >> 1) {
						// Need 1 of each required reagent.
			if ((flags&1) != 0 && 
			    act.countObjects(REAGENTS, EConst.c_any_qual, r) == 0)
				return false;	// Missing.
		}
		return true;
	}
	public boolean canDoSpell(Actor act)	// Can we do bookmarked spell?
		{ return bookmark >= 0 ? canDoSpell(act, bookmark) : false; }
					// Do the spell.
	public boolean doSpell(Actor act, int spell, boolean can_do,
						boolean in_combat) {
		if (can_do || canDoSpell(act, spell)) {
		int circle = spell/8;	// Figure/subtract mana.
		if (cheat.inWizardMode())
			circle = 0;
		int mana = act.getProperty(Actor.mana);
		// ++++TAG: Need to de-hard-code cost.
		act.setProperty(Actor.mana, mana-circle-(game.isSI() ? 1 : 0));
					// Figure what we used.
		int flags = (int)reagents[spell]&0xffff;

		if (!cheat.inWizardMode() && !hasRing(act)) {
					// Go through bits.
			for (int r = 0; flags != 0; r++, flags = flags >> 1)
					// Remove 1 of each required reagent.
				if ((flags&1) != 0)
					act.removeQuantity(1, 
						REAGENTS, EConst.c_any_qual, r);
			}
		executeSpell(act, spell, in_combat);
		return true;
		}
	return false;
	}
					// Do bookmarked spell.
	public boolean doSpell(Actor act, boolean in_combat)
		{ return bookmark >= 0 ?
		 	doSpell(act, bookmark, false, in_combat) : false; }
	private static int getUsecode(int spell) {
		return 0x640 + spell;
	}
	public static void executeSpell(Actor act, int spell, 
						boolean in_combat) {
		act.beginCasting(859);	// ++++TAG: Need to de-hard-code.

		// We use intercept_item for spells cast from readied spellbook
		// while in combat.
		// First, save current.
		GameObject oldtarg = ucmachine.getInterceptClickOnItem();
		Tile oldtile = ucmachine.getInterceptClickOnTile();
		if (in_combat)	// Use caster's target if for combat.
			ucmachine.interceptClickOnItem(act.getTarget());
		else	// Otherwise, disable intercept for gump casting.
			ucmachine.interceptClickOnItem(null);

		ucmachine.callUsecode(getUsecode(spell), act, 
			in_combat ? UsecodeMachine.weapon :
				    UsecodeMachine.double_click);

		// Restore previous intercept_item.
		ucmachine.restoreIntercept(oldtarg, oldtile);
	}
	@Override				// Run usecode function.
	public void activate(int event) {
		//+++++FINISH gumpman.addGump(this, getInfo().getGumpShape(), false);
	}
	@Override				// Write out to IREG file.
	public void writeIreg(OutputStream out) throws IOException {
		byte buf[] = new byte[24];		// 18-byte entry.
		int ind = writeCommonIreg(18, buf);
		System.arraycopy(circles, 0, buf, ind, 5);	// Store the way U7 does it.
		ind += 5;
		buf[ind++] = (byte)((getLift()&15)<<4);	// Low bits?++++++
		System.arraycopy(circles, 5, buf, ind, 4); // Rest of spell circles.
		ind += 4;
		buf[ind++] = 0;			// 3 unknowns.
		buf[ind++] = 0;
		buf[ind++] = 0;
		buf[ind++] = (byte)(bookmark >= 0 ? bookmark : 255);
		out.write(buf, 0, ind);
						// Write scheduled usecode.
		GameMap.writeScheduled(out, this, false);	
	}
						// Get size of IREG. 
	@Override			// Returns -1 if can't write to buffer
	public int getIregSize() {
		// These shouldn't ever happen, but you never know
		if (gumpman.findGump(this) != null || UsecodeScript.find(this) != null)
			return -1;
		return 14 + getCommonIregSize();
	}
}
