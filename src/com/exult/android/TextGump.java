package com.exult.android;

public class TextGump extends Gump {
	protected String text;
	protected int curtop;		// Offset of top of current page.
	protected int curend;		// Offset past end of current page(s).
	protected int font;			// The shape in fonts.vga to use
	
	public TextGump(int shapenum) {
		super(shapenum);
		font = 4;
	}
	public void addText(String str) {	// Append text.
		
	}
	// Paint and return index pas end of displayed page.
	public int paintPage(Rectangle box, int start) {
		return 0;//+++++++FINISH
	}
					// Next page of book/scroll.
	boolean showNextPage() {
		if (curend > text.length())
			return false;
		curtop = curend;// Start next page or pair of pages.
		paint();			// Paint.  This updates curend.
		return true;
	}
}
