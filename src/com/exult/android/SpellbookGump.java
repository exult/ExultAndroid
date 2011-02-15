package com.exult.android;

/*
 *	Open spellbook.
 */
public class SpellbookGump extends Gump {
	/*
	 *	Defines in 'gumps.vga':
	 */
	private static int SPELLBOOK() {
		return (game.isBG() ? 43 : 38);
	}
	private static int SPELLS() {
		return (game.isBG() ? 33 : 28);		// First group of 9 spells.
	}
	private static int TURNINGPAGE() {
		return (game.isBG() ? 41 : 36);	// Animation?? (4 frames).
	}
	private static int BOOKMARK() {
		return (game.isBG() ? 42 : 37);	// Red ribbon, 5 frames.
	}
	private static int LEFTPAGE() {
		return (game.isBG() ? 44 : 39);	// At top-left of left page.
	}
	private static int RIGHTPAGE() {
		return (game.isBG() ? 45 : 40);	// At top-right of right page.
	}
	private static int SCROLLSPELLS = 66;		// First group of scroll spells (SI).
	/*
	 *	And in 'text.flx' (indices are offset from 0x500):
	 */
	private static int CIRCLE() {
		return ((game.isBG() ? 0x45 : 0x51));
	}
	private static int CIRCLENUM() {
		return ((game.isBG() ? 0x45 : 0x52));
	}
	private Rectangle area;
	private Rectangle tempRect = new Rectangle();
	private int page;			// Starting with 0 (= circle #).
	private int turning_page;		// 1 if turning forward, -1 backward,
					//   0 if not turning.
	private int turning_frame;		// Next frame to show.
	private short avail[] = new short[9*8];		// For each spell, # which the
					//   available reagents make possible.
	private SpellbookObject book;		// Book this shows.
	private GameObject bookOwner;	// Top-owner of book.
					// Page turners:
	private GumpWidget.Button leftpage, rightpage;
	private BookmarkButton bookmark;
	private GumpWidget.Button spells[] = new GumpWidget.Button[9*8];// .spell 'buttons'.
	private int spwidth, spheight;		// Dimensions of a spell shape.
	private void setAvail() {		// Set up counts.
		int i;				// Init.
		for (i = 0; i < 9*8; i++)
			avail[i] = 0;
		if (bookOwner == book)
			return;			// Nobody owns it.
		int reagent_counts[] = new int[SpellbookObject.NREAGENTS];	// Count reagents.
		int r;
		for (r = 0; r < SpellbookObject.NREAGENTS; r++)	// Count, by frame (frame==bit#).
			reagent_counts[r] = bookOwner.countObjects(
							SpellbookObject.REAGENTS, EConst.c_any_qual, r);
		boolean has_ring = SpellbookObject.hasRing(gwin.getMainActor());
		for (i = 0; i < 9*8; i++) {	// Now figure what's available.
			avail[i] = 10000;	// 'infinite'.
			if (has_ring)
				continue;
			int flags = book.getReagents(i);
						// Go through bits.
			for (r = 0; flags != 0; r++, flags = flags >> 1)
						// Take min. of req. reagent counts.
				if ((flags&1) != 0 && reagent_counts[r] < avail[i])
					avail[i] = (short)reagent_counts[r];
		}

	}
	 // Get circle, given a spell #.
	private static int getCircle(int spell)
		{ return spell/8; }
	/*
	 *	Get shape, frame for a given spell #.  There are 8 shapes, each
	 *	containing 9 frames, where frame # = spell circle #.
	 */
	private static int getSpellGumpShape(int spell) { // Spell # (as used in Usecode).
		return (spell < 0 || spell >= 0x48) ? -1 : spell%8;
	}
	private static int getSpellGumpFrame(int spell) { // Spell # (as used in Usecode).
		return (spell < 0 || spell >= 0x48) ? -1 : spell/8;
	}
	public SpellbookGump(SpellbookObject b) {
		super(SPELLBOOK());
		book = b;	
		area = new Rectangle(36, 28, 102, 66);
		addCheckMark(7, 54);
		// Where to paint page marks:
		final int lpagex = 38, rpagex = 142, lrpagey = 25;
						// Get book's top owner.
		bookOwner = book.getOutermost();
		setAvail();			// Figure spell counts.
		if (book.getBookmark() >= 0)	// Set to bookmarked page.
			page = getCircle(book.getBookmark());
		leftpage = new PageButton(this, lpagex, lrpagey, 0);
	 	rightpage = new PageButton(this, rpagex, lrpagey, 1);
		bookmark = new BookmarkButton(this);
						// Get dims. of a spell.
		ShapeFrame spshape = ShapeFiles.GUMPS_VGA.getShape(SPELLS(), 0);
		spwidth = spshape.getWidth();
		spheight = spshape.getHeight();
		bookmark.set();		// Set to correct position, frame.
		int vertspace = (area.h - 4*spheight)/4;
		int spells0 = SPELLS();
		for (int c = 0; c < 9; c++) {	// Add each spell.
			int spindex = c*8;
			int cflags = book.getCircle(c);
			for (int s = 0; s < 8; s++) {
				if ((cflags & (1<<s)) != 0 || cheat.inWizardMode()) {
					int spnum = spindex + s;
					spells[spnum] = new SpellButton(this,
						s < 4 ? area.x +
							spshape.getXLeft() + 1
						: area.x + area.w - 
							spshape.getXRight() - 2,
						area.y + spshape.getYAbove() +
							(spheight + vertspace)*(s%4),
						spnum,
						spells0 + spnum%8, spnum/8);
				} else
					spells[spindex + s] = null;
			}
		}
	}
	public void doSpell(int spell) {	// Perform spell.
		if (!book.canDoSpell(gwin.getMainActor(), spell))
			mouse.flashShape(Mouse.redx);
		else {
			SpellbookObject save_book = book;
			close();		// We've just been deleted!
						// Don't need to check again.
			save_book.doSpell(gwin.getMainActor(), spell, true, false);
						// Close all gumps so animations can
						//   start.
			gumpman.closeAllGumps(false);
		}
	}
	public void changePage(int delta) {	// Page forward/backward.
		if (delta > 0) {
			if (page == 8)
				return;
			turning_page = -1;
		} else if (delta < 0) {
			if (page == 0)
				return;
			turning_page = 1;
		}
		int nframes = ShapeFiles.GUMPS_VGA.getFile().getNumFrames(TURNINGPAGE());
		int i;
		turning_frame = turning_page == 1 ? 0 : nframes - 1;
		for (i = 0; i < nframes; i++) { // Animate
			if (i == nframes/2) {
				page += delta;	// Change page halfway through.
				bookmark.set();// Update bookmark for new page.
			}
			getDirty(tempRect);
			gwin.addDirty(tempRect);
			
			gwin.paintDirty();
			/*++++++++FINISH
			gwin.show();
			SDL_Delay(50);		// 1/20 sec.
			*/
		}
	paint();
	}
	public void selectSpell(int spell) {	// Set bookmark.
		if (spells[spell] != null) {
			book.setBookmark(spell);
			bookmark.set();	// Update bookmark's position/frame.
			paint();
		}
	}
	public GameObject getOwner() { // Get object this belongs to.
		return book;
	}
	@Override		// Is a given point on a button?
	public GumpWidget.Button onButton(int mx, int my) {
		GumpWidget.Button btn = super.onButton(mx, my);
		if (btn != null)
			return btn;
		else if (leftpage.onButton(mx, my) != null)
			return leftpage;
		else if (rightpage.onButton(mx, my) != null)
			return rightpage;
		int spindex = page*8;		// Index into list.
		for (int s = 0; s < 8; s++) {	// Check spells.
			GumpWidget.Button spell = spells[spindex + s];
			if (spell != null && spell.onButton(mx, my) != null)
				return spell;
		}
		if (bookmark.onButton(mx, my) != null)
			return bookmark;
		return null;
	}
			// Paint button.
	public void paintButton(GumpWidget.Button btn) {
		btn.paint();
	}
	@Override		// Paint it and its contents.
	public void paint() {
		final int numx = 1, numy = -4;// Where to draw numbers on spells,
									  //   with numx being the right edge.
		super.paint();			// Paint outside & checkmark.
		if (page > 0)			// Not the first?
			paintButton(leftpage);
		if (page < 8)			// Not the last?
			paintButton(rightpage);
		int spindex = page*8;		// Index into list.
		for (int s = 0; s < 8; s++) {	// Paint spells.
			if (spells[spindex + s] != null) {
				GumpWidget.Button spell = spells[spindex + s];
				paintButton(spell);
				if (game.isBG() && page == 0)	// No quantities for 0th circle in BG.
					continue;
				int num = avail[spindex + s];
				if (num > 0 || cheat.inWizardMode()) {
					String text;
					if ((num >= 1000 || cheat.inWizardMode()) && game.isSI())
						text = "#"; // # = infinity in SI's font 5
					else if (num > 99 || cheat.inWizardMode())
						text = "99";
					else
						text = String.format("%1$d", num);
					fonts.paintText(5, text,
							x + spell.x + numx - fonts.getTextWidth(5, text),
							y + spell.y + numy);
				}
			}
		}
		if (page > 0 ||	game.isSI()) {		// Paint circle.
			String circ = ItemNames.misc[CIRCLE()];
			String cnum = ItemNames.misc[CIRCLENUM() + page];
			fonts.paintText(5, cnum, x + 40 + 
				(44 - fonts.getTextWidth(5, cnum))/2, y + 20);
			fonts.paintText(5, circ, x + 92 +
				(44 - fonts.getTextWidth(5, circ))/2, y + 20);
		}
		if (book.getBookmark() >= 0)	// Bookmark?
			paintButton(bookmark);
		if (turning_page != 0) {		// Animate turning page.
			final int TPXOFF = 5, TPYOFF = 3;
			int nframes = ShapeFiles.GUMPS_VGA.getFile().getNumFrames(TURNINGPAGE());
			ShapeFrame fr = ShapeFiles.GUMPS_VGA.getShape(TURNINGPAGE(), turning_frame);
			int spritex = x + area.x + fr.getXLeft() + TPXOFF;
			int spritey = y + fr.getYAbove() + TPYOFF;
			fr.paint(gwin.getWin(), spritex, spritey);
			turning_frame += turning_page;
			if (turning_frame < 0 || turning_frame >= nframes)
				turning_page = 0;	// Last one.
		}
		gwin.setPainted();
	}
	/*
	 *	Abstract base class for spellbook, spell-scrolls:
	 */
	/* +++++++MAYBE LATER
	private abstract static class SpelltypeGump extends Gump {
		SpelltypeGump(int shnum) {
			super(shnum);
		}
		// Perform spell.
		abstract void doSpell(int spell);
						// Set bookmark.
		abstract void selectSpell(int spell);
	}
	*/
	/*
	 *	A 'page-turner' button.
	 */
	private static class PageButton extends GumpWidget.Button {
		int leftright;			// 0=left, 1=right.
		public PageButton(Gump par, int px, int py, int lr) {
			super(par, lr != 0 ? RIGHTPAGE() : LEFTPAGE(), px, py);
			leftright = lr;
		}
		@Override			// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) return false;
			((SpellbookGump) parent).changePage(leftright != 0 ? 1 : -1);
			return true;
		}
		@Override
		public boolean push(boolean button) 
			{return button;}
		@Override
		public void unpush(boolean button) {}
		};
	/*
	 * Bookmark.
	 */
	private static class BookmarkButton extends GumpWidget.Button {
		public BookmarkButton(Gump par) {
			super(par, BOOKMARK(), 0, 0);
		}
		@Override			// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) return false;
			SpellbookGump sgump = (SpellbookGump) parent;
			int bmpage = sgump.book.getBookmark()/8;	// Bookmark's page.
							// On a different, valid page?
			if (bmpage >= 0 && bmpage != sgump.page)
				sgump.changePage(bmpage - sgump.page);
			return true;
		}
		@Override
		public boolean push(boolean button) 
			{ return button; }
		@Override
		public void unpush(boolean button) {}
		void set() {			// Call this to set properly.
			SpellbookGump sgump = (SpellbookGump) parent;
			Rectangle area = sgump.area;
			int spwidth = sgump.spwidth;	// Spell width.
			SpellbookObject book = sgump.book;
			int page = sgump.page;		// Page (circle) we're on.
			int bmpage = book.getBookmark()/8;	// Bookmark's page.
			int s = book.getBookmark()%8;	// Get # within circle.
						// Which side for bookmark?
			boolean left = bmpage == page ? (s < 4) : bmpage < page;
						// Figure coords.
			x = left ? area.x + spwidth/2
					: area.x + area.w - spwidth/2 - 2;
			ShapeFrame bshape = getShape();
			x += bshape.getXLeft();
			y = area.y - 14 + bshape.getYAbove();
			setFrame((bmpage == page) ? (1 + s%4) : 0);
		}
	}
	/*
	 *	A spell button.
	 */
	private static class SpellButton extends GumpWidget.Button {
		int spell;			// Spell # (0 - 71).
		public SpellButton(Gump par, int px, int py, int sp, int shnum, int frnum) {
			super(par, shnum, px, py);
			spell = sp;
			setFrame(frnum);	// Frame # is circle.
		}
		@Override			// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) return false;
			((SpellbookGump) parent).selectSpell(spell);
			return true;
		}
		@Override
		public boolean push(boolean button) 
			{return button;}
		@Override
		public void unpush(boolean button) {}
		@Override
		public void doubleClicked(int x, int y) {
			((SpellbookGump) parent).doSpell(spell);
		}
	}
}
