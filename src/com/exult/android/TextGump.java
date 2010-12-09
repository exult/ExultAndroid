package com.exult.android;

public class TextGump extends Gump {
	protected String text;
	protected int textlen;
	protected int curtop;		// Offset of top of current page.
	protected int curend;		// Offset past end of current page(s).
	protected int font;			// The shape in fonts.vga to use
	
	public TextGump(int shapenum) {
		super(shapenum);
		font = 4;
	}
	public TextGump(int shapenum, int fontnum) {
		super(shapenum);
		font = fontnum;
	}
	public void addText(String str) {	// Append text.
		if (text == null)
			text = str;
		else {
			// Add a new-line marker if necessary.
			if (text.charAt(textlen- 1) != '*')
				text = text + '~';
			text = text + str;
		}
		textlen  = text.length();
	}
	// Paint and return index pas end of displayed page.
	public int paintPage(int boxX, int boxY, int boxW, int boxH, int start) {
		int vlead = 1;		// Extra inter-line spacing.
		int ypos = 0;
		int textheight = fonts.getTextHeight(font) + vlead;
		int ind = start;
		while (ind < textlen && text.charAt(ind) != '*' && 
										ypos + textheight <= boxH) {
			if (text.charAt(ind) == '~') {	// Empty paragraph?
				ypos += textheight;
				ind++;
				continue;
			}
						// Look for page break.
			int epage = text.indexOf('*', ind);
						// Look for line break.
			int eol = text.indexOf('~', ind);
			if (epage >= 0 && (eol < 0 || eol > epage))
				eol = epage;
			if (eol < 0)		// No end found?
				eol = textlen;
			char eolchr = text.charAt(eol);	// Save char. at EOL.
			int endoff = fonts.paintTextBox(gwin.getWin(),
					font, text, ind, eol, x + boxX,
					y + boxY + ypos, boxW, boxH - ypos, vlead, false, false);
			if (endoff > 0)	{	// All painted?
								// Value returned is height.
				ind = eol + ((eolchr == '~') ? 1 : 0);
				ypos += endoff;
			} else {			// Out of room.
				ind += -endoff;
				break;
			}
		}
		if (text.charAt(ind) == '*')		// Saw end of page?
			ind++;
		gwin.setPainted();		// Force blit.
		return (ind);		// Return offset past end.
	}
	// Next page of book/scroll.
	boolean showNextPage() {
		if (curend > text.length())
			return false;
		curtop = curend;// Start next page or pair of pages.
		paint();			// Paint.  This updates curend.
		return true;
	}
	public static class Book extends TextGump {
		public Book() {
			super(32);	//++++++FINISH:  game->get_shape("gumps/book")
		}
		public Book(int fontnum) {
			super(32, fontnum);//++++++FINISH:  game->get_shape("gumps/book")
		}
		public void paint() {
						// Paint the gump itself.
			paintShape(x, y);
						// Paint left page.
			curend = paintPage(36, 10, 122, 130, curtop);
						// Paint right page.
			curend = paintPage(174, 10, 122, 130, curend);
		}
	}
	public static class Scroll extends TextGump {
		public Scroll() {
			super(32);	//++++++FINISH:  game->get_shape("gumps/scroll")
		}
		public Scroll(int fontnum) {
			super(32, fontnum);//++++++FINISH:  game->get_shape("gumps/scroll")
		}
		public void paint() {	
			paintShape(x, y);	// The gump itself.
			curend = paintPage(52, 30, 142, 118, curtop);
		}
	}
}
