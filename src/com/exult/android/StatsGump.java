package com.exult.android;

public class StatsGump extends Gump {
	private static final short textx = 123;
	private static final short texty[] = {17, 26, 35, 46, 55, 67, 76, 86, 95, 104};
	//	Some gump shape numbers:
	private static final int ASLEEP = 0, POISONED = 1, CHARMED = 2, HUNGRY = 3,
			  PROTECTED = 4, CURSED = 5, PARALYZED = 6;
	private Actor actor;
	public StatsGump(Actor a, int initx, int inity) {
		super(game.getShape("gumps/statsdisplay"));
		actor = a;
		addCheckMark(6, 136);
	}
	public static StatsGump create(Actor npc, int x, int y) {
		return new StatsGump(npc, x, y);
	}
	public ContainerGameObject getContainer() {
		return actor;
	}
	public void paint() {
		GumpManager gman = gumpman;
					// Area to print name in.
		final int namex = 30, namey = 6, namew = 95;
		/* ++++++++++++FINISH
		if (gwin.getMainActor().getFlag(GameObject.freeze)) {
			int frame = act.getTemperatureZone();
			if (getNumFrames() < frame+1)	// Prevent problems in BG.
				frame = getNumFrames()-1;
			if (frame < 0)
				frame = 0;
			setFrame(frame);
		}
		*/
					// Paint the gump itself.
		paintShape(x, y);
					// Paint red "checkmark".
		paintElems();
					// Show statistics.
		String nm = actor.getName();
		fonts.paintText(2, nm, x + namex +
				(namew - fonts.getTextWidth(2, nm))/2, y + namey);
		gman.paintNum(actor.getEffectiveProp(Actor.strength),
						x + textx, y + texty[0]);
		gman.paintNum(actor.getEffectiveProp(Actor.dexterity),
						x + textx, y + texty[1]);
		gman.paintNum(actor.getEffectiveProp(Actor.intelligence),
						x + textx, y + texty[2]);
		gman.paintNum(actor.getEffectiveProp(Actor.combat),
						x + textx, y + texty[3]);
		gman.paintNum(actor.getProperty(Actor.magic),
						x + textx, y + texty[4]);
		gman.paintNum(actor.getProperty(Actor.health),
						x + textx, y + texty[5]);
		gman.paintNum(actor.getProperty(Actor.mana),
						x + textx, y + texty[6]);
		gman.paintNum(actor.getProperty(Actor.exp),
						x + textx, y + texty[7]);
		gman.paintNum(actor.getLevel(), x + textx, y + texty[8]);
		gman.paintNum(actor.getProperty(Actor.training),
						x + textx, y + texty[9]);
					// Now show atts. at bottom.
		final int attsy = 130, attsx0 = 29;
		int attsx = attsx0;
		if (actor.getFlag(GameObject.asleep))
			attsx += showAtts(x + attsx, y + attsy, ASLEEP);
		if (actor.getFlag(GameObject.poisoned))
			attsx += showAtts(x + attsx, y + attsy, POISONED);
		if (actor.getFlag(GameObject.charmed))
			attsx += showAtts(x + attsx, y + attsy, CHARMED);
		if (actor.getProperty((int) Actor.food_level) <= 4)
			attsx += showAtts(x + attsx, y + attsy, HUNGRY);
		if (actor.getFlag(GameObject.protection))
			attsx += showAtts(x + attsx, y + attsy, PROTECTED);
		if (actor.getFlag(GameObject.cursed))
			attsx += showAtts(x + attsx, y + attsy, CURSED);
		if (actor.getFlag(GameObject.paralyzed))
			attsx += showAtts(x + attsx, y + attsy, PARALYZED);
	}/*
	 *	Show one of the atts.
	 *
	 *	Output:	Amount to increment x-pos for the next one.
	 */
	private static int showAtts
		(
		int x, int y,			// Pos. on screen.
		int framenum
		) {
		int shnum = game.getShape("gumps/statatts");
		ShapeFrame s = ShapeFiles.GUMPS_VGA.getShape(shnum, framenum);
		s.paint(gwin.getWin(), x + s.getXLeft(), y + s.getYBelow());
		return s.getWidth() + 2;
	}
}
